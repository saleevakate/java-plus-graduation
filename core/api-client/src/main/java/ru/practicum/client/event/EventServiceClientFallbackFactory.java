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
                log.error("Сервис событий недоступен для getEventById: {}", eventId, cause);
                throw new RuntimeException("Сервис событий временно недоступен");
            }

            @Override
            public Boolean eventExists(Long eventId) {
                log.error("Сервис событий недоступен для eventExists: {}", eventId, cause);
                return false;
            }

            @Override
            public Boolean isEventOwner(Long userId, Long eventId) {
                log.error("Сервис событий недоступен для isEventOwner: userId={}, eventId={}", userId, eventId, cause);
                return false;
            }

            @Override
            public Boolean isEventPublished(Long eventId) {
                log.error("Сервис событий недоступен для isEventPublished: {}", eventId, cause);
                return false;
            }

            @Override
            public Integer getParticipantLimit(Long eventId) {
                log.error("Сервис событий недоступен для getParticipantLimit: {}", eventId, cause);
                return 0;
            }

            @Override
            public List<EventShortDto> getEventsByIds(List<Long> eventIds) {
                log.error("Сервис событий недоступен для getEventsByIds: {}", eventIds, cause);
                return List.of();
            }

            @Override
            public EventFullDto getEventWithDistance(Long eventId, Double lat, Double lon) {
                log.error("Сервис событий недоступен для getEventWithDistance: eventId={}, lat={}, lon={}",
                        eventId, lat, lon, cause);
                throw new RuntimeException("Сервис событий временно недоступен");
            }

            @Override
            public List<EventShortDto> getEventsNearby(Long userId, Double lat, Double lon, Double radiusMeters, Integer page, Integer size) {
                log.error("Сервис событий недоступен для getEventsNearby: userId={}, page={}, size={}",
                        userId, page, size, cause);
                return List.of();
            }
        };
    }
}
