package com.ticketwave.dto.ticket;

import com.ticketwave.domain.TicketStatus;

public record ValidateTicketResponse(
        String qrCode,
        String eventName,
        String seat,
        TicketStatus status,
        boolean valid,
        String message
) {
}