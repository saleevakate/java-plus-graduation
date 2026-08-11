package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.event.EventServiceClient;
import ru.practicum.client.user.UserServiceClient;
import ru.practicum.dto.participation.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Request;
import ru.practicum.repository.RequestRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventRequestServiceImpl implements EventRequestService {

    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;
    private final UserServiceClient userServiceClient;
    private final EventServiceClient eventServiceClient;

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("Получение запросов на участие userId: {}, eventId: {}", userId, eventId);
        validateUser(userId);
        validateEventOwner(userId, eventId);

        return requestRepository.findAllByEventId(eventId).stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatuses(Long userId, Long eventId,
                                                                EventRequestStatusUpdateRequest updateRequest) {
        log.info("Обновление статусов запросов к событию id: {}, {}", eventId, updateRequest);

        if (updateRequest == null) {
            throw new ConflictException("Переданные параметры не должны быть пустыми");
        }

        validateUser(userId);
        validateEventOwner(userId, eventId);

        List<Request> requests = requestRepository.findAllByIdIn(updateRequest.requestIds());
        if (requests.size() != updateRequest.requestIds().size()) {
            throw new NotFoundException("Некоторые запросы не были найдены.");
        }

        // Проверяем, что все запросы относятся к событию
        if (requests.stream().anyMatch(request -> !request.getEventId().equals(eventId))) {
            throw new ConflictException("Запрос не относится к указанному событию.");
        }

        // Проверяем, что все запросы в статусе PENDING
        if (requests.stream().anyMatch(request ->
                !ParticipationStatus.PENDING.name().equals(request.getStatus()))) {
            throw new ConflictException("Запрос должен иметь статус PENDING.");
        }

        List<Request> confirmed = new ArrayList<>();
        List<Request> rejected = new ArrayList<>();

        if (updateRequest.status() == RequestStatusAction.REJECTED) {
            requests.forEach(request -> request.setStatus(ParticipationStatus.REJECTED.name()));
            rejected.addAll(requests);
        } else {
            confirmRequests(eventId, requests, confirmed, rejected);
        }

        requestRepository.saveAll(requests);
        log.info("Новые статусы успешно сохранены");

        return new EventRequestStatusUpdateResult(
                confirmed.stream().map(requestMapper::toDto).toList(),
                rejected.stream().map(requestMapper::toDto).toList());
    }

    private void confirmRequests(Long eventId, List<Request> requests, List<Request> confirmed,
                                 List<Request> rejected) {
        log.debug("Подтверждение запросов");

        int limit = eventServiceClient.getParticipantLimit(eventId);
        int confirmedCount = requestRepository.countByEventId(eventId);

        if (limit != 0 && confirmedCount >= limit) {
            throw new ConflictException("Внимание: лимит участников!");
        }

        for (Request request : requests) {
            if (limit != 0 && confirmedCount >= limit) {
                throw new ConflictException("Внимание: лимит участников!");
            }
            request.setStatus(ParticipationStatus.CONFIRMED.name());
            confirmedCount++;
            confirmed.add(request);
        }

        // Если лимит достигнут, отклоняем все остальные PENDING заявки
        if (limit != 0 && confirmedCount >= limit) {
            List<Request> others = requestRepository.findAllByEventId(eventId).stream()
                    .filter(req -> ParticipationStatus.PENDING.name().equals(req.getStatus()))
                    .toList();
            others.forEach(req -> req.setStatus(ParticipationStatus.REJECTED.name()));
            requestRepository.saveAll(others);
            rejected.addAll(others);
        }
        log.debug("Запросы успешно приняты");
    }

    private void validateUser(Long userId) {
        try {
            if (!userServiceClient.userExists(userId)) {
                throw new NotFoundException("Пользователь с id=" + userId + " не найден");
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке пользователя: {}", e.getMessage());
            throw new RuntimeException("Сервис пользователей недоступен");
        }
    }

    private void validateEventOwner(Long userId, Long eventId) {
        try {
            if (!eventServiceClient.isEventOwner(userId, eventId)) {
                throw new ConflictException("Пользователь не является владельцем события");
            }
        } catch (Exception e) {
            log.error("Ошибка при проверке владельца события: {}", e.getMessage());
            throw new RuntimeException("Сервис событий недоступен");
        }
    }
}
