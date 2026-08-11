package ru.practicum.client.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;


import java.util.List;

@Slf4j
@Component
public class EventServiceClientFallbackFactory implements FallbackFactory<EventServiceClient> {

    @Override
    public EventServiceClient create(Throwable cause) {
        return new EventServiceClient() {
            @Override
            public EventFullDto getEventById(Long eventId) {
                log.error("Event service unavailable for getEventById: {}", eventId, cause);
                throw new RuntimeException("Event service temporarily unavailable");
            }

            @Override
            public Boolean eventExists(Long eventId) {
                log.error("Event service unavailable for eventExists: {}", eventId, cause);
                return false;
            }

            @Override
            public Boolean isEventOwner(Long userId, Long eventId) {
                log.error("Event service unavailable for isEventOwner: userId={}, eventId={}", userId, eventId, cause);
                return false;
            }

            @Override
            public Boolean isEventPublished(Long eventId) {
                log.error("Event service unavailable for isEventPublished: {}", eventId, cause);
                return false;
            }

            @Override
            public Integer getParticipantLimit(Long eventId) {
                log.error("Event service unavailable for getParticipantLimit: {}", eventId, cause);
                return 0;
            }

            @Override
            public List<EventShortDto> getEventsByIds(List<Long> eventIds) {
                log.error("Event service unavailable for getEventsByIds: {}", eventIds, cause);
                return List.of();
            }
        };
    }
}
