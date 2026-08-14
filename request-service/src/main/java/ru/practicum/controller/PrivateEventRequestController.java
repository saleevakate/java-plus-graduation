package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.participation.EventRequestStatusUpdateRequest;
import ru.practicum.dto.participation.EventRequestStatusUpdateResult;
import ru.practicum.dto.participation.ParticipationRequestDto;
import ru.practicum.service.event.EventRequestService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@Slf4j
public class PrivateEventRequestController {

    private final EventRequestService eventRequestService;

    @GetMapping
    public ResponseEntity<List<ParticipationRequestDto>> getEventRequests(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        log.info("GET /users/{}/events/{}/requests", userId, eventId);
        return ResponseEntity.ok(eventRequestService.getEventRequests(userId, eventId));
    }

    @PatchMapping
    public ResponseEntity<EventRequestStatusUpdateResult> updateRequestStatuses(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody(required = false) EventRequestStatusUpdateRequest updateRequest) {
        log.info("PATCH /users/{}/events/{}/requests: {}", userId, eventId, updateRequest);
        return ResponseEntity.status(HttpStatus.OK)
                .body(eventRequestService.updateRequestStatuses(userId, eventId, updateRequest));
    }
}
