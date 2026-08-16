package ru.practicum.analyzer.service.recommendations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.model.UserActionHistory;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.analyzer.repository.UserActionHistoryRepository;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationsServiceImpl implements RecommendationsService {

    private final UserActionHistoryRepository userActionHistoryRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    @Override
    public List<RecommendedEventProto> getRecommendationsForUser(Long userId, Integer maxResults) {
        log.info("Генерация рекомендаций для userId={}, maxResults={}", userId, maxResults);

        List<Long> viewedEvents = userActionHistoryRepository.findEventIdsByUserId(userId);

        if (viewedEvents.isEmpty()) {
            log.info("Пользователь {} не взаимодействовал ни с одним мероприятием", userId);
            return List.of();
        }

        Map<Long, Double> candidateScores = new HashMap<>();

        for (Long viewedEventId : viewedEvents) {
            List<EventSimilarity> similarities = eventSimilarityRepository.findByEventAOrEventB(viewedEventId);

            for (EventSimilarity sim : similarities) {
                Long otherEventId = sim.getEventA().equals(viewedEventId) ? sim.getEventB() : sim.getEventA();

                if (viewedEvents.contains(otherEventId)) {
                    continue;
                }

                candidateScores.merge(otherEventId, sim.getScore(), Double::sum);
            }
        }

        List<RecommendedEventProto> result = candidateScores.entrySet().stream()
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(maxResults)
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        log.info("Сгенерировано {} рекомендаций для пользователя {}", result.size(), userId);
        return result;
    }

    @Override
    public List<RecommendedEventProto> getSimilarEvents(Long eventId, Long userId, Integer maxResults) {
        log.info("Поиск похожих мероприятий для eventId={}, userId={}, maxResults={}",
                eventId, userId, maxResults);

        List<Long> viewedEvents = userActionHistoryRepository.findEventIdsByUserId(userId);

        List<EventSimilarity> similarities = eventSimilarityRepository.findByEventAOrEventB(eventId);

        List<RecommendedEventProto> result = similarities.stream()
                .map(sim -> {
                    Long otherEventId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
                    return Map.entry(otherEventId, sim.getScore());
                })
                .filter(entry -> !viewedEvents.contains(entry.getKey()))
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .limit(maxResults)
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        log.info("Найдено {} похожих мероприятий для eventId={}", result.size(), eventId);
        return result;
    }

    @Override
    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        log.info("Получение количества взаимодействий для {} мероприятий", eventIds.size());

        if (eventIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Double> weightSums = new HashMap<>();

        for (Long eventId : eventIds) {
            List<UserActionHistory> histories = userActionHistoryRepository.findAll()
                    .stream()
                    .filter(h -> h.getEventId().equals(eventId))
                    .toList();

            double sum = histories.stream()
                    .mapToDouble(UserActionHistory::getWeight)
                    .sum();

            weightSums.put(eventId, sum);
        }

        List<RecommendedEventProto> result = weightSums.entrySet().stream()
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .collect(Collectors.toList());

        log.info("Получены данные о взаимодействиях для {} мероприятий", result.size());
        return result;
    }
}
