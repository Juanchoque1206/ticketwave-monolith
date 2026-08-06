package com.ticketwave.dto.payment;

import com.ticketwave.domain.PaymentProvider;
import com.ticketwave.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        PaymentProvider provider,
        PaymentStatus status,
        BigDecimal amount,
        String providerTransactionId,
        LocalDateTime paidAt,
        String checkoutUrl
) {
}