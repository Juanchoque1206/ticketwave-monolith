package com.ticketwave.controller;

import com.ticketwave.dto.ticket.TicketResponse;
import com.ticketwave.dto.ticket.ValidateTicketResponse;
import com.ticketwave.service.TicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Management of issued digital tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.get(id));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<TicketResponse>> listByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ticketService.listByOrder(orderId));
    }

    @PostMapping("/validate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ValidateTicketResponse> validate(@RequestBody com.ticketwave.dto.ticket.ValidateTicketRequest request) {
        return ResponseEntity.ok(ticketService.validate(request.qrCode()));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<TicketResponse> refund(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.refund(id));
    }
}