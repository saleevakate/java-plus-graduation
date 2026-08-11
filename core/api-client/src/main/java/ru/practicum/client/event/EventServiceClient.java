package ru.practicum.client.event;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
}
