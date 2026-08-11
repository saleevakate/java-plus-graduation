package ru.practicum.controller.event;

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
    public ResponseEntity<List<EventShortDto>> getEventsByFilter(@ModelAttribute @Valid PublicEventsFilter filter) {
        log.info("GET /events");
        // TODO: получить app, uri, ip, timestamp из запроса
        String app = "ewm-event-service";
        String uri = "/events";
        String ip = "127.0.0.1";
        String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        );

        return ResponseEntity.ok(eventService.getPublishedEvents(filter, app, uri, ip, timestamp));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventFullDto> getEventById(@PathVariable Long eventId) {
        log.info("GET /events/{}", eventId);
        return ResponseEntity.ok(eventService.getEventById(eventId));
    }
}
