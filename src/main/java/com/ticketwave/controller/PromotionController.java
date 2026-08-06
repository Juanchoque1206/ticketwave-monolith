package com.ticketwave.controller;

import com.ticketwave.dto.promotion.PromotionRequest;
import com.ticketwave.dto.promotion.PromotionResponse;
import com.ticketwave.service.PromotionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@Tag(name = "Promotions", description = "National and venue-specific discount codes")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PromotionResponse> create(@Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<PromotionResponse>> listActive() {
        return ResponseEntity.ok(promotionService.listActive());
    }
}