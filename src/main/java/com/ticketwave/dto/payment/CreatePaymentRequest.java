package com.ticketwave.dto.payment;

import com.ticketwave.domain.PaymentProvider;

import java.util.UUID;

public record CreatePaymentRequest(
        UUID orderId,
        PaymentProvider provider
) {
}