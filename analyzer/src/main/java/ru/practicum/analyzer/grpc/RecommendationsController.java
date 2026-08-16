package ru.practicum.analyzer.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.service.recommendations.RecommendationsService;
import ru.practicum.ewm.stats.proto.*;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationsService recommendationsService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Запрос рекомендаций для пользователя: userId={}, maxResults={}",
                    request.getUserId(), request.getMaxResults());

            var recommendations = recommendationsService.getRecommendationsForUser(
                    request.getUserId(),
                    request.getMaxResults()
            );

            for (var rec : recommendations) {
                responseObserver.onNext(rec);
            }

            responseObserver.onCompleted();
            log.info("Отправлено {} рекомендаций для пользователя {}", recommendations.size(), request.getUserId());

        } catch (Exception e) {
            log.error("Ошибка получения рекомендаций для пользователя {}", request.getUserId(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Запрос похожих мероприятий: eventId={}, userId={}, maxResults={}",
                    request.getEventId(), request.getUserId(), request.getMaxResults());

            var similarEvents = recommendationsService.getSimilarEvents(
                    request.getEventId(),
                    request.getUserId(),
                    request.getMaxResults()
            );

            for (var event : similarEvents) {
                responseObserver.onNext(event);
            }

            responseObserver.onCompleted();
            log.info("Отправлено {} похожих мероприятий для eventId={}",
                    similarEvents.size(), request.getEventId());

        } catch (Exception e) {
            log.error("Ошибка получения похожих мероприятий для eventId={}", request.getEventId(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Запрос количества взаимодействий для {} мероприятий", request.getEventIdsCount());

            var interactions = recommendationsService.getInteractionsCount(request.getEventIdsList());

            for (var interaction : interactions) {
                responseObserver.onNext(interaction);
            }

            responseObserver.onCompleted();
            log.info("Отправлены данные о взаимодействиях для {} мероприятий", interactions.size());

        } catch (Exception e) {
            log.error("Ошибка получения количества взаимодействий", e);
            responseObserver.onError(e);
        }
    }
}
