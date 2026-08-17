package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    public List<RecommendedEventProto> getRecommendationsForUser(Long userId, Integer maxResults) {
        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            var iterator = analyzerStub.getRecommendationsForUser(request);
            List<RecommendedEventProto> result = new ArrayList<>();
            iterator.forEachRemaining(result::add);

            log.info("Получены рекомендации для пользователя: userId={}, count={}", userId, result.size());
            return result;

        } catch (Exception e) {
            log.error("Ошибка получения рекомендаций для пользователя: userId={}", userId, e);
            return List.of();
        }
    }

    public List<RecommendedEventProto> getSimilarEvents(Long eventId, Long userId, Integer maxResults) {
        try {
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            var iterator = analyzerStub.getSimilarEvents(request);
            List<RecommendedEventProto> result = new ArrayList<>();
            iterator.forEachRemaining(result::add);

            log.info("Получены похожие события: eventId={}, count={}", eventId, result.size());
            return result;

        } catch (Exception e) {
            log.error("Ошибка получения похожих событий: eventId={}", eventId, e);
            return List.of();
        }
    }

    public List<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addAllEventIds(eventIds)
                    .build();

            var iterator = analyzerStub.getInteractionsCount(request);
            List<RecommendedEventProto> result = new ArrayList<>();
            iterator.forEachRemaining(result::add);

            log.info("Получены данные о взаимодействиях для {} мероприятий", eventIds.size());
            return result;

        } catch (Exception e) {
            log.error("Ошибка получения данных о взаимодействиях", e);
            return List.of();
        }
    }
}
