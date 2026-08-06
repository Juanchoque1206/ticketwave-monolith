package com.ticketwave.service;

import com.ticketwave.domain.*;
import com.ticketwave.dto.event.EventResponse;
import com.ticketwave.dto.order.CreateOrderRequest;
import com.ticketwave.dto.order.OrderResponse;
import com.ticketwave.exception.BusinessRuleException;
import com.ticketwave.exception.OrderStateException;
import com.ticketwave.exception.ResourceNotFoundException;
import com.ticketwave.repository.TicketOrderRepository;
import com.ticketwave.repository.TicketRepository;
import com.ticketwave.util.PriceCalculator;
import com.ticketwave.util.QrCodeGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TicketOrderService {

    private final TicketOrderRepository orderRepository;
    private final EventService eventService;
    private final UserService userService;
    private final PromotionService promotionService;
    private final FraudService fraudService;
    private final NotificationService notificationService;
    private final TicketRepository ticketRepository;
    private final long orderTtlMinutes;

    public TicketOrderService(TicketOrderRepository orderRepository,
                              EventService eventService,
                              UserService userService,
                              PromotionService promotionService,
                              FraudService fraudService,
                              NotificationService notificationService,
                              TicketRepository ticketRepository,
                              @Value("${ticketwave.order-ttl-minutes:15}") long orderTtlMinutes) {
        this.orderRepository = orderRepository;
        this.eventService = eventService;
        this.userService = userService;
        this.promotionService = promotionService;
        this.fraudService = fraudService;
        this.notificationService = notificationService;
        this.ticketRepository = ticketRepository;
        this.orderTtlMinutes = orderTtlMinutes;
    }

    @Transactional
    public OrderResponse createReservation(AuthenticationContext ctx, CreateOrderRequest request) {
        AppUser user = userService.findByUsername(ctx.username());
        fraudService.guard(user, ctx.ipAddress());

        Event event = eventService.getEntity(request.eventId());
        EventResponse capacity = eventService.reserveCapacity(event.getId(), request.quantity());
        if (capacity.availableCount() < request.quantity()) {
            eventService.releaseCapacity(event.getId(), request.quantity());
            throw new BusinessRuleException("Not enough capacity available");
        }

        TicketOrder order = new TicketOrder();
        order.setUser(user);
        order.setEvent(event);
        order.setQuantity(request.quantity());
        order.setReservedAt(LocalDateTime.now());
        order.setExpiresAt(LocalDateTime.now().plusMinutes(orderTtlMinutes));
        order.setStatus(OrderStatus.PENDING);

        BigDecimal subtotal = event.getBasePrice().multiply(BigDecimal.valueOf(request.quantity()));
        BigDecimal discount = BigDecimal.ZERO;
        if (request.promotionCode() != null && !request.promotionCode().isBlank()) {
            Promotion promotion = promotionService.findByCode(request.promotionCode());
            discount = promotionService.discountFor(promotion, event, request.quantity(), subtotal);
            promotionService.incrementUsage(promotion);
        }
        order = orderRepository.save(order);

        fraudService.markOrder(order.getId(), user);
        return toResponse(order, discount);
    }

    @Transactional
    public OrderResponse confirmOrder(UUID orderId, Payment payment) {
        TicketOrder order = getPendingOrder(orderId);
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        List<Ticket> tickets = emitTickets(order);
        notificationService.send(order.getUser(), NotificationType.PAYMENT_RECEIVED,
                "Payment received", "Your payment for order " + orderId + " was received.");

        return toResponse(order, BigDecimal.ZERO, tickets.stream().map(Ticket::getId).toList());
    }

    @Transactional
    public List<Ticket> emitTickets(TicketOrder order) {
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < order.getQuantity(); i++) {
            Ticket ticket = new Ticket();
            ticket.setOrder(order);
            ticket.setEvent(order.getEvent());
            ticket.setPrice(order.getEvent().getBasePrice());
            ticket.setSeat("Row-" + (i + 1));
            ticket.setStatus(TicketStatus.EMITTED);
            ticket.setIssuedAt(LocalDateTime.now());
            ticket.setQrCode(QrCodeGenerator.generate(
                    order.getId().toString(), UUID.randomUUID().toString(), order.getEvent().getId().toString()));
            tickets.add(ticketRepository.save(ticket));
        }
        return tickets;
    }

    @Transactional
    public void cancelOrder(UUID orderId) {
        TicketOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderStateException("Only pending orders can be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        eventService.releaseCapacity(order.getEvent().getId(), order.getQuantity());
        notificationService.send(order.getUser(), NotificationType.ORDER_CANCELLED,
                "Order cancelled", "Your order " + orderId + " has been cancelled.");
    }

    @Transactional
    public void expireOrder(UUID orderId) {
        TicketOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getStatus() == OrderStatus.PENDING && PriceCalculator.isExpired(order)) {
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);
            eventService.releaseCapacity(order.getEvent().getId(), order.getQuantity());
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        TicketOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toResponse(order, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrdersForUser(AuthenticationContext ctx) {
        AppUser user = userService.findByUsername(ctx.username());
        return orderRepository.findByUserId(user.getId())
                .stream().map(o -> toResponse(o, BigDecimal.ZERO)).toList();
    }

    private TicketOrder getPendingOrder(UUID orderId) {
        TicketOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new OrderStateException("Order is not pending");
        }
        if (PriceCalculator.isExpired(order)) {
            throw new OrderStateException("Order has expired");
        }
        return order;
    }

    private OrderResponse toResponse(TicketOrder order, BigDecimal discount) {
        List<UUID> ticketIds = ticketRepository.findByOrderId(order.getId())
                .stream().map(Ticket::getId).toList();
        return toResponse(order, discount, ticketIds);
    }

    private OrderResponse toResponse(TicketOrder order, BigDecimal discount, List<UUID> ticketIds) {
        BigDecimal subtotal = order.getEvent().getBasePrice().multiply(BigDecimal.valueOf(order.getQuantity()));
        return new OrderResponse(order.getId(), order.getEvent().getId(), order.getEvent().getName(),
                order.getStatus(), order.getQuantity(), subtotal.subtract(discount), discount,
                order.getReservedAt(), order.getExpiresAt(), ticketIds);
    }

    public record AuthenticationContext(String username, String ipAddress) {
    }
}