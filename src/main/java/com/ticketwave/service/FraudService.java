package com.ticketwave.service;

import com.ticketwave.domain.AppUser;
import com.ticketwave.dto.fraud.FraudReportResponse;
import com.ticketwave.exception.FraudRiskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class FraudService {

    private static final Logger log = LoggerFactory.getLogger(FraudService.class);
    private static final String ATTEMPT_KEY = "fraud:attempts:";
    private static final int MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public FraudService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public FraudReportResponse evaluate(AppUser user, String ipAddress) {
        String key = user != null ? ATTEMPT_KEY + user.getId() : ATTEMPT_KEY + "anon:" + ipAddress;
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, WINDOW);
        }

        String duplicateSignal = user != null
                ? redisTemplate.opsForValue().get("fraud:dup:" + user.getId())
                : null;
        boolean duplicate = duplicateSignal != null;

        boolean blocked = attempts != null && attempts > MAX_ATTEMPTS || duplicate;
        String riskLevel = attempts != null && attempts > MAX_ATTEMPTS ? "HIGH" : "LOW";

        if (blocked) {
            log.warn("Fraud risk detected for user={} ip={} attempts={} duplicate={}", user, ipAddress, attempts, duplicate);
        }
        return new FraudReportResponse(user != null ? user.getId().toString() : null,
                ipAddress, riskLevel, "order_attempt_rate", blocked, duplicate,
                blocked ? "Blocked due to suspicious activity" : "OK");
    }

    public void guard(AppUser user, String ipAddress) {
        FraudReportResponse report = evaluate(user, ipAddress);
        if (report.blocked()) {
            throw new FraudRiskException(report.message());
        }
    }

    public void markOrder(UUID orderId, AppUser user) {
        String key = "fraud:dup:" + user.getId();
        redisTemplate.opsForValue().setIfAbsent(key, orderId.toString(), Duration.ofMinutes(5));
    }
}