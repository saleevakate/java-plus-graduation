package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.participation.ParticipationRequestDto;
import ru.practicum.dto.participation.ParticipationStatus;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Request;
import ru.practicum.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;

    // Пока заглушки, потом заменим на Feign
    private boolean userExists(Long userId) {
        // TODO: заменить на вызов user-service через Feign
        return true;
    }

    private boolean eventExists(Long eventId) {
        // TODO: заменить на вызов event-service через Feign
        return true;
    }

    private boolean isEventOwner(Long userId, Long eventId) {
        // TODO: заменить на вызов event-service через Feign
        return false;
    }

    private boolean isEventPublished(Long eventId) {
        // TODO: заменить на вызов event-service через Feign
        return true;
    }

    private int getParticipantLimit(Long eventId) {
        // TODO: заменить на вызов event-service через Feign
        return 0;
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Получение заявок пользователя: userId={}", userId);
        validateUser(userId);

        List<Request> requests = requestRepository.findAllByRequesterId(userId);
        log.debug("Найдено {} заявок", requests.size());

        return requests.stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        log.info("Добавление заявки: userId={}, eventId={}", userId, eventId);

        validateUser(userId);
        validateEvent(eventId);

        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Нельзя добавить повторный запрос");
        }

        if (isEventOwner(userId, eventId)) {
            throw new ConflictException("Владелец события не может создать заявку");
        }

        if (!isEventPublished(eventId)) {
            throw new ConflictException("Нельзя участвовать в неопубликованном событии");
        }

        int participantLimit = getParticipantLimit(eventId);
        if (participantLimit > 0) {
            int currentParticipants = requestRepository.countByEventId(eventId);
            if (currentParticipants >= participantLimit) {
                throw new ConflictException("В событии больше нет свободных мест");
            }
        }

        Request request = Request.builder()
                .requesterId(userId)
                .eventId(eventId)
                .created(LocalDateTime.now())
                .status(participantLimit == 0 ? ParticipationStatus.CONFIRMED.name() : ParticipationStatus.PENDING.name())
                .build();

        Request saved = requestRepository.save(request);
        log.info("Заявка создана с id={}", saved.getId());

        return requestMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена заявки: userId={}, requestId={}", userId, requestId);

        validateUser(userId);

        Request request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Заявка не найдена или принадлежит другому пользователю"));

        request.setStatus(ParticipationStatus.CANCELED.name());
        Request canceled = requestRepository.save(request);
        log.info("Заявка {} отменена", requestId);

        return requestMapper.toDto(canceled);
    }

    private void validateUser(Long userId) {
        if (!userExists(userId)) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }

    private void validateEvent(Long eventId) {
        if (!eventExists(eventId)) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено");
        }
    }
}
