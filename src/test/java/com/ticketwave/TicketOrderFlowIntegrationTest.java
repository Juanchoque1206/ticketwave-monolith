package com.ticketwave;

import com.ticketwave.domain.*;
import com.ticketwave.repository.*;
import com.ticketwave.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class TicketOrderFlowIntegrationTest {

    @Autowired
    private TicketOrderService orderService;
    @Autowired
    private TicketService ticketService;
    @Autowired
    private TicketOrderRepository orderRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PromotionRepository promotionRepository;

    @Test
    @Transactional
    void reservation_confirmationAndEmission_flow() {
        AppUser user = createUser();
        Event event = createEvent();

        TicketOrderService.AuthenticationContext ctx =
                new TicketOrderService.AuthenticationContext(user.getUsername(), "127.0.0.1");

        TicketOrder order = new TicketOrder();
        order.setUser(user);
        order.setEvent(event);
        order.setQuantity(2);
        order.setReservedAt(LocalDateTime.now());
        order.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        order.setStatus(OrderStatus.PENDING);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setProvider(PaymentProvider.STRIPE);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(new BigDecimal("300.00"));

        order = orderRepository.save(order);
        var confirmed = orderService.confirmOrder(order.getId(), payment);
        assertNotNull(confirmed);
    }

    private AppUser createUser() {
        AppUser user = new AppUser();
        user.setUsername("tester-" + UUID.randomUUID());
        user.setEmail("tester-" + UUID.randomUUID() + "@mail.com");
        user.setPassword("$2a$10$dummyhash");
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    private Event createEvent() {
        Event event = new Event();
        event.setName("Test Event");
        event.setCity("Lima");
        event.setVenue("Test Venue");
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setBasePrice(new BigDecimal("100.00"));
        event.setTotalCapacity(100);
        event.setStatus(EventStatus.PUBLISHED);
        return eventRepository.save(event);
    }
}