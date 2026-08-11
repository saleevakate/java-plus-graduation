package ru.practicum.controller.event;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
}
