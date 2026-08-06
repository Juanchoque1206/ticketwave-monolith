package com.ticketwave.dto.promotion;

import com.ticketwave.domain.PromotionScope;
import com.ticketwave.domain.PromotionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PromotionResponse(
        UUID id,
        String code,
        String name,
        PromotionType type,
        BigDecimal value,
        PromotionScope scope,
        UUID venueId,
        boolean active,
        int maxUsage,
        int usedCount,
        LocalDateTime validFrom,
        LocalDateTime validUntil
) {
}