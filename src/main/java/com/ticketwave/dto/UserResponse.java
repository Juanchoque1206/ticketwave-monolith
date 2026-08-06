package com.ticketwave.dto;

import com.ticketwave.domain.Role;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String fullName,
        String city,
        Role role
) {
}