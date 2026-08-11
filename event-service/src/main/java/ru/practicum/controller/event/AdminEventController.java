package ru.practicum.controller.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.UpdateEventAdminRequest;
import ru.practicum.dto.event.param_objects.AdminEventsFilter;
import ru.practicum.service.event.EventService;

import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AdminEventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventFullDto>> getAdminEvents(@ModelAttribute AdminEventsFilter filter) {
        log.info("GET /admin/events");
        return ResponseEntity.ok(eventService.getAdminEvents(filter));
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventFullDto> updateAdminEvent(
            @Positive @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventAdminRequest request) {
        log.info("PATCH /admin/events/{}", eventId);
        return ResponseEntity.ok(eventService.updateAdminEvent(eventId, request));
    }

    @GetMapping("/exists/{eventId}")
    public ResponseEntity<Boolean> eventExists(@PathVariable Long eventId) {
        log.info("GET /admin/events/exists/{}", eventId);
        return ResponseEntity.ok(eventService.eventExists(eventId));
    }

    @GetMapping("/owner/{userId}/{eventId}")
    public ResponseEntity<Boolean> isEventOwner(
            @PathVariable Long userId,
            @PathVariable Long eventId) {
        log.info("GET /admin/events/owner/{}/{}", userId, eventId);
        return ResponseEntity.ok(eventService.isEventOwner(userId, eventId));
    }

    @GetMapping("/published/{eventId}")
    public ResponseEntity<Boolean> isEventPublished(@PathVariable Long eventId) {
        log.info("GET /admin/events/published/{}", eventId);
        return ResponseEntity.ok(eventService.isEventPublished(eventId));
    }

    @GetMapping("/limit/{eventId}")
    public ResponseEntity<Integer> getParticipantLimit(@PathVariable Long eventId) {
        log.info("GET /admin/events/limit/{}", eventId);
        return ResponseEntity.ok(eventService.getParticipantLimit(eventId));
    }
}
