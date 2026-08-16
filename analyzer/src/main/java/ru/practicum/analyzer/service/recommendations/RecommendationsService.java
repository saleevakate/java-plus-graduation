package ru.practicum.analyzer.service.recommendations;

import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.List;

public interface RecommendationsService {

    List<RecommendedEventProto> getRecommendationsForUser(Long userId, Integer maxResults);

    List<RecommendedEventProto> getSimilarEvents(Long eventId, Long userId, Integer maxResults);

    List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds);
}
