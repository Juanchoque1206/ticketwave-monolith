package com.ticketwave.controller;

import com.ticketwave.dto.event.EventRequest;
import com.ticketwave.dto.event.EventResponse;
import com.ticketwave.dto.event.EventSearchRequest;
import com.ticketwave.service.EventService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/events")
@Tag(name = "Test Events", description = "Public test endpoints for events (no authentication)")
public class TestEventController {

    private final EventService eventService;

    public TestEventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> list(
            @ParameterObject @PageableDefault(page = 1, size = 10, sort = "eventDate") Pageable pageable) {
        EventSearchRequest filters = new EventSearchRequest(null, null, null, null, null);
        return ResponseEntity.ok(eventService.search(filters, pageable));
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request));
    }
}
