package com.ticketwave.controller;

import com.ticketwave.dto.payment.CreatePaymentRequest;
import com.ticketwave.dto.payment.PaymentResponse;
import com.ticketwave.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Integration with external payment providers (Stripe/PayPal)")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(request));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getForOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getForOrder(orderId));
    }
}