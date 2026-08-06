package com.ticketwave.controller;

import com.ticketwave.dto.order.CreateOrderRequest;
import com.ticketwave.dto.order.OrderResponse;
import com.ticketwave.service.TicketOrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Ticket Orders", description = "Unified reservation + purchase flow (TicketOrder)")
public class TicketOrderController {

    private final TicketOrderService orderService;

    public TicketOrderController(TicketOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> reserve(@Valid @RequestBody CreateOrderRequest request,
                                                 Principal principal,
                                                 HttpServletRequest httpRequest) {
        TicketOrderService.AuthenticationContext ctx =
                new TicketOrderService.AuthenticationContext(principal.getName(), clientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createReservation(ctx, request));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> myOrders(Authentication authentication,
                                                        HttpServletRequest httpRequest) {
        TicketOrderService.AuthenticationContext ctx =
                new TicketOrderService.AuthenticationContext(authentication.getName(), clientIp(httpRequest));
        return ResponseEntity.ok(orderService.listOrdersForUser(ctx));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> get(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable UUID orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}