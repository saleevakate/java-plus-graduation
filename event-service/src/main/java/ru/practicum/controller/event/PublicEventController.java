package ru.practicum.controller.event;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.param_objects.PublicEventsFilter;
import ru.practicum.service.event.EventService;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicEventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getEventsByFilter(
            @ModelAttribute @Valid PublicEventsFilter filter,
            HttpServletRequest request) {
        log.info("GET /events");
        return ResponseEntity.ok(eventService.getPublishedEvents(filter, request));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> getEventById(
            @PathVariable Long eventId,
            HttpServletRequest request) {
        log.info("GET /events/{}", eventId);
        return ResponseEntity.ok(eventService.getEventById(eventId, request));
    }

    @GetMapping("/{eventId}/distance")
    public ResponseEntity<EventFullDto> getEventWithDistance(
            @Positive @PathVariable Long eventId,
            @RequestParam @NotNull @Min(-90) @Max(90) Double lat,
            @RequestParam @NotNull @Min(-180) @Max(180) Double lon) {
        log.info("GET /events/{}/distance?lat={}&lon={}", eventId, lat, lon);
        return ResponseEntity.ok(eventService.getEventWithDistance(eventId, lat, lon));
    }
}
