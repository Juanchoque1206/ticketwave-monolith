package com.ticketwave.dto.promotion;

import com.ticketwave.domain.PromotionScope;
import com.ticketwave.domain.PromotionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PromotionRequest(
        String code,
        String name,
        PromotionType type,
        BigDecimal value,
        PromotionScope scope,
        UUID venueId,
        int maxUsage,
        LocalDateTime validFrom,
        LocalDateTime validUntil
) {
}