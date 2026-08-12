package ru.practicum.service;

import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.location.PrivateEventsFilter;

import java.util.List;

public interface LocationService {

    EventFullDto getEventWithDistance(Long eventId, Double lat, Double lon);

    List<EventShortDto> getUserEventsByCoordinates(Long userId, PrivateEventsFilter filter,
                                                   Integer page, Integer size);
}
