package com.ticketwave.service;

import com.ticketwave.domain.AppUser;
import com.ticketwave.domain.Role;
import com.ticketwave.dto.UserResponse;
import com.ticketwave.exception.ResourceNotFoundException;
import com.ticketwave.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getProfile(Authentication authentication) {
        AppUser user = findByUsername(authentication.getName());
        return toResponse(user);
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse getUser(UUID id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user);
    }

    public AppUser findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getCity(), user.getRole());
    }
}