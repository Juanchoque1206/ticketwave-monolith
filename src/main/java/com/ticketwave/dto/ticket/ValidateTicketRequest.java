package com.ticketwave.dto.ticket;

import jakarta.validation.constraints.NotBlank;

public record ValidateTicketRequest(
        @NotBlank String qrCode
) {
}