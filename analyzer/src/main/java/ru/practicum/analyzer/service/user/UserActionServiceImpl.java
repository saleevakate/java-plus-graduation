package ru.practicum.analyzer.service.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.UserActionHistory;
import ru.practicum.analyzer.repository.UserActionHistoryRepository;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionServiceImpl implements UserActionService {

    private static final double VIEW_WEIGHT = 1.0;
    private static final double REGISTER_WEIGHT = 3.0;
    private static final double LIKE_WEIGHT = 5.0;

    private final UserActionHistoryRepository repository;

    @Override
    @Transactional
    public void processUserAction(UserActionAvro event) {
        Long userId = event.getUserId();
        Long eventId = event.getEventId();
        ActionTypeAvro actionType = event.getActionType();
        Instant timestamp = event.getTimestamp();

        double weight = getActionWeight(actionType);

        Optional<UserActionHistory> existing = repository.findByUserIdAndEventId(userId, eventId);

        if (existing.isPresent()) {
            UserActionHistory history = existing.get();
            if (weight > history.getWeight()) {
                history.setWeight(weight);
                history.setLastActionTime(timestamp);
                repository.save(history);
                log.info("Обновлен вес для userId={}, eventId={}: {}", userId, eventId, weight);
            } else {
                log.debug("Вес для userId={}, eventId={} не изменился", userId, eventId);
            }
        } else {
            UserActionHistory history = UserActionHistory.builder()
                    .userId(userId)
                    .eventId(eventId)
                    .weight(weight)
                    .lastActionTime(timestamp)
                    .build();
            repository.save(history);
            log.info("Сохранено новое действие: userId={}, eventId={}, weight={}", userId, eventId, weight);
        }
    }

    private double getActionWeight(ActionTypeAvro actionType) {
        switch (actionType) {
            case VIEW:
                return VIEW_WEIGHT;
            case REGISTER:
                return REGISTER_WEIGHT;
            case LIKE:
                return LIKE_WEIGHT;
            default:
                return 0.0;
        }
    }
}
