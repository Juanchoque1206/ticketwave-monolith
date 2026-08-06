package com.ticketwave.dto.order;

import com.ticketwave.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID eventId,
        String eventName,
        OrderStatus status,
        int quantity,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        LocalDateTime reservedAt,
        LocalDateTime expiresAt,
        List<UUID> ticketIds
) {
}