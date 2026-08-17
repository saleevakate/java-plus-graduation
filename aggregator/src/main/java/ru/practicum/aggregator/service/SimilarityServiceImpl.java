package ru.practicum.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SimilarityServiceImpl implements SimilarityService {

    private final Map<Long, Map<Long, Double>> eventVectors = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();
    private final Map<Long, Double> sumSquares = new ConcurrentHashMap<>();
    private static final double VIEW_WEIGHT = 1.0;
    private static final double REGISTER_WEIGHT = 3.0;
    private static final double LIKE_WEIGHT = 5.0;

    @Override
    public List<EventSimilarityAvro> processUserAction(UserActionAvro event) {
        Long userId = event.getUserId();
        Long eventId = event.getEventId();
        ActionTypeAvro actionType = event.getActionType();
        Instant timestamp = event.getTimestamp();
        log.info("Обработка действия: userId={}, eventId={}, actionType={}", userId, eventId, actionType);
        double weight = getActionWeight(actionType);
        Map<Long, Double> userVector = eventVectors.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());
        Double previousWeight = userVector.get(userId);

        if (previousWeight != null && weight <= previousWeight) {
            log.debug("Вес для userId={}, eventId={} не изменился: {}", userId, eventId, previousWeight);
            return new ArrayList<>();
        }

        if (previousWeight != null) {
            updateMinSumsForEvent(eventId, userId, previousWeight, weight);
        } else {
            addNewUserToMinSums(eventId, userId, weight);
        }

        userVector.put(userId, weight);
        double oldSq = previousWeight != null ? previousWeight * previousWeight : 0.0;
        double newSq = weight * weight;
        sumSquares.merge(eventId, newSq - oldSq, Double::sum);
        return recalculateSimilarities(eventId, timestamp);
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

    private void updateMinSumsForEvent(Long eventId, Long userId, double oldWeight, double newWeight) {
        Map<Long, Double> userVectors = getUserVectorsForUser(userId);

        for (Map.Entry<Long, Double> entry : userVectors.entrySet()) {
            Long otherEventId = entry.getKey();
            if (otherEventId.equals(eventId)) {
                continue;
            }
            double otherWeight = entry.getValue();
            if (otherWeight == 0) {
                continue;
            }

            double oldMin = Math.min(oldWeight, otherWeight);
            double newMin = Math.min(newWeight, otherWeight);
            double delta = newMin - oldMin;

            if (delta != 0) {
                addToMinSum(eventId, otherEventId, delta);
            }
        }
    }

    private void addNewUserToMinSums(Long eventId, Long userId, double weight) {
        Map<Long, Double> userVectors = getUserVectorsForUser(userId);

        for (Map.Entry<Long, Double> entry : userVectors.entrySet()) {
            Long otherEventId = entry.getKey();
            if (otherEventId.equals(eventId)) {
                continue;
            }

            double otherWeight = entry.getValue();
            if (otherWeight == 0) {
                continue;
            }
            double min = Math.min(weight, otherWeight);
            addToMinSum(eventId, otherEventId, min);
        }
    }

    private Map<Long, Double> getUserVectorsForUser(Long userId) {
        Map<Long, Double> result = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : eventVectors.entrySet()) {
            Long eventId = entry.getKey();
            Double weight = entry.getValue().get(userId);
            if (weight != null && weight > 0) {
                result.put(eventId, weight);
            }
        }
        return result;
    }

    private void addToMinSum(Long eventA, Long eventB, double value) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        minWeightsSums
                .computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .merge(second, value, Double::sum);
    }

    private double getMinSum(Long eventA, Long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        Map<Long, Double> innerMap = minWeightsSums.get(first);
        if (innerMap == null) {
            return 0.0;
        }
        return innerMap.getOrDefault(second, 0.0);
    }

    private List<EventSimilarityAvro> recalculateSimilarities(Long eventId, Instant timestamp) {
        List<EventSimilarityAvro> result = new ArrayList<>();
        Double sA = sumSquares.get(eventId);

        if (sA == null || sA <= 0) {
            return result;
        }

        double sqrtSA = Math.sqrt(sA);
        Set<Long> otherEvents = new HashSet<>();
        Map<Long, Double> asFirst = minWeightsSums.get(eventId);

        if (asFirst != null) {
            otherEvents.addAll(asFirst.keySet());
        }

        for (Map.Entry<Long, Map<Long, Double>> entry : minWeightsSums.entrySet()) {
            Long first = entry.getKey();
            if (entry.getValue().containsKey(eventId)) {
                otherEvents.add(first);
            }
        }

        for (Long otherEventId : otherEvents) {
            if (otherEventId.equals(eventId)) {
                continue;
            }
            Double sB = sumSquares.get(otherEventId);
            if (sB == null || sB <= 0) {
                continue;
            }
            double sqrtSB = Math.sqrt(sB);
            double sMin = getMinSum(eventId, otherEventId);
            double score = sMin / (sqrtSA * sqrtSB);

            EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                    .setEventA(Math.min(eventId, otherEventId))
                    .setEventB(Math.max(eventId, otherEventId))
                    .setScore(score)
                    .setTimestamp(timestamp)
                    .build();

            result.add(similarity);
        }
        log.info("Пересчитано {} сходств для eventId={}", result.size(), eventId);
        return result;
    }
}
