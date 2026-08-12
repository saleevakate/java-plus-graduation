package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.client.event.EventServiceClient;
import ru.practicum.client.user.UserServiceClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.location.PrivateEventsFilter;
import ru.practicum.exception.NotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationServiceImpl implements LocationService {

    private final EventServiceClient eventServiceClient;
    private final UserServiceClient userServiceClient;

    @Override
    public EventFullDto getEventWithDistance(Long eventId, Double lat, Double lon) {
        log.info("Получение события с дистанцией: eventId={}, lat={}, lon={}", eventId, lat, lon);
        return eventServiceClient.getEventWithDistance(eventId, lat, lon);
    }

    @Override
    public List<EventShortDto> getUserEventsByCoordinates(Long userId, PrivateEventsFilter filter,
                                                          Integer page, Integer size) {
        log.info("Поиск событий рядом: userId={}, lat={}, lon={}, radius={}",
                userId, filter.lat(), filter.lon(), filter.radiusMeters());

        try {
            if (!userServiceClient.userExists(userId)) {
                throw new NotFoundException("Пользователь с id=" + userId + " не найден");
            }
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при проверке пользователя: {}", e.getMessage());
            throw new RuntimeException("Сервис пользователей временно недоступен");
        }

        return eventServiceClient.getEventsNearby(userId, filter.lat(), filter.lon(), filter.radiusMeters(), page, size);
    }
}
