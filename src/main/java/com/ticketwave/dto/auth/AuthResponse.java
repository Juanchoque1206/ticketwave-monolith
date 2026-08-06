package com.ticketwave.dto.auth;

import com.ticketwave.domain.Role;
import com.ticketwave.dto.UserResponse;

import java.util.UUID;

public record AuthResponse(
        String token,
        String tokenType,
        UserResponse user
) {

    public static AuthResponse of(String token, UUID id, String username, String email,
                                  String fullName, String city, Role role) {
        return new AuthResponse(token, "Bearer",
                new UserResponse(id, username, email, fullName, city, role));
    }
}