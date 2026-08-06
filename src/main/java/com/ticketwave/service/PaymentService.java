package com.ticketwave.service;

import com.ticketwave.domain.Payment;
import com.ticketwave.domain.PaymentProvider;
import com.ticketwave.domain.PaymentStatus;
import com.ticketwave.domain.TicketOrder;
import com.ticketwave.dto.payment.CreatePaymentRequest;
import com.ticketwave.dto.payment.PaymentResponse;
import com.ticketwave.exception.PaymentException;
import com.ticketwave.exception.ResourceNotFoundException;
import com.ticketwave.repository.PaymentRepository;
import com.ticketwave.repository.TicketOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TicketOrderRepository orderRepository;
    private final TicketOrderService orderService;

    public PaymentService(PaymentRepository paymentRepository,
                          TicketOrderRepository orderRepository,
                          TicketOrderService orderService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @Transactional
    public PaymentResponse create(CreatePaymentRequest request) {
        TicketOrder order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (request.provider() == null) {
            throw new PaymentException("A payment provider is required");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setProvider(request.provider());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(order.getEvent().getBasePrice().multiply(BigDecimal.valueOf(order.getQuantity())));
        payment = paymentRepository.save(payment);

        boolean succeeded = payWithProvider(request.provider(), payment.getAmount());
        if (!succeeded) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentException("Payment failed with provider " + request.provider());
        }

        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setProviderTransactionId("TXN-" + UUID.randomUUID());
        payment.setPaidAt(LocalDateTime.now());
        payment = paymentRepository.save(payment);

        orderService.confirmOrder(order.getId(), payment);
        return toResponse(payment);
    }

    private boolean payWithProvider(PaymentProvider provider, BigDecimal amount) {
        return switch (provider) {
            case STRIPE -> simulateProviderCall("stripe", amount);
            case PAYPAL -> simulateProviderCall("paypal", amount);
        };
    }

    private boolean simulateProviderCall(String providerName, BigDecimal amount) {
        // Placeholder for real Stripe/PayPal SDK integration.
        return true;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getForOrder(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order"));
        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrder().getId(), payment.getProvider(),
                payment.getStatus(), payment.getAmount(), payment.getProviderTransactionId(),
                payment.getPaidAt(), null);
    }
}