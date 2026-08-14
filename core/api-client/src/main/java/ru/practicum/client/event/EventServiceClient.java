package ru.practicum.client.event;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.config.FeignConfig;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;

import java.util.List;

@FeignClient(
        name = "event-service",
        configuration = FeignConfig.class,
        fallbackFactory = EventServiceClientFallbackFactory.class
)
public interface EventServiceClient {

    @GetMapping("/events/{eventId}")
    EventFullDto getEventById(@PathVariable("eventId") Long eventId);

    @GetMapping("/events/exists/{eventId}")
    Boolean eventExists(@PathVariable("eventId") Long eventId);

    @GetMapping("/events/owner/{userId}/{eventId}")
    Boolean isEventOwner(@PathVariable("userId") Long userId,
                         @PathVariable("eventId") Long eventId);

    @GetMapping("/events/published/{eventId}")
    Boolean isEventPublished(@PathVariable("eventId") Long eventId);

    @GetMapping("/events/limit/{eventId}")
    Integer getParticipantLimit(@PathVariable("eventId") Long eventId);

    @PostMapping("/events/batch")
    List<EventShortDto> getEventsByIds(@RequestBody List<Long> eventIds);

    //методы дополнительной функциональности
    @GetMapping("/events/{eventId}/distance")
    EventFullDto getEventWithDistance(
            @PathVariable("eventId") Long eventId,
            @RequestParam("lat") Double lat,
            @RequestParam("lon") Double lon
    );

    @GetMapping("/users/{userId}/events/nearby")
    List<EventShortDto> getEventsNearby(
            @PathVariable("userId") Long userId,
            @RequestParam("lat") Double lat,
            @RequestParam("lon") Double lon,
            @RequestParam("radiusMeters") Double radiusMeters,
            @RequestParam("page") Integer page,
            @RequestParam("size") Integer size
    );
}
