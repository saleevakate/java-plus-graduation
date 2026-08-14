package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.location.PrivateEventsFilter;
import ru.practicum.service.LocationService;

import java.util.List;

@RestController
@RequestMapping("/location")
@RequiredArgsConstructor
@Slf4j
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/events/{eventId}/distance")
    public ResponseEntity<EventFullDto> getEventWithDistance(
            @PathVariable Long eventId,
            @RequestParam Double lat,
            @RequestParam Double lon) {
        log.info("GET /location/events/{}/distance?lat={}&lon={}", eventId, lat, lon);
        return ResponseEntity.ok(locationService.getEventWithDistance(eventId, lat, lon));
    }

    @GetMapping("/users/{userId}/events/nearby")
    public ResponseEntity<List<EventShortDto>> getUserEventsByCoordinates(
            @PathVariable Long userId,
            @RequestParam Double lat,
            @RequestParam Double lon,
            @RequestParam Double radiusMeters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /location/users/{}/events/nearby", userId);
        PrivateEventsFilter filter = new PrivateEventsFilter(radiusMeters, lat, lon);
        return ResponseEntity.ok(locationService.getUserEventsByCoordinates(userId, filter, page, size));
    }
}
