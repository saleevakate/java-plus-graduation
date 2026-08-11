package ru.practicum.client.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.dto.participation.EventRequestStatusUpdateRequest;
import ru.practicum.dto.participation.EventRequestStatusUpdateResult;
import ru.practicum.dto.participation.ParticipationRequestDto;

import java.util.List;

@Slf4j
@Component
public class RequestServiceClientFallbackFactory implements FallbackFactory<RequestServiceClient> {

    @Override
    public RequestServiceClient create(Throwable cause) {
        return new RequestServiceClient() {
            @Override
            public List<ParticipationRequestDto> getUserRequests(Long userId) {
                log.error("Сервис запросов недоступен для getUserRequests: {}", userId, cause);
                return List.of();
            }

            @Override
            public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
                log.error("Сервис запросов недоступен для addParticipationRequest: userId={}, eventId={}", userId, eventId, cause);
                throw new RuntimeException("Сервис запросов временно недоступен");
            }

            @Override
            public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
                log.error("Сервис запросов недоступен для cancelRequest: userId={}, requestId={}", userId, requestId, cause);
                throw new RuntimeException("Сервис запросов временно недоступен");
            }

            @Override
            public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
                log.error("Сервис запросов недоступен для getEventRequests: userId={}, eventId={}", userId, eventId, cause);
                return List.of();
            }

            @Override
            public EventRequestStatusUpdateResult updateRequestStatuses(Long userId, Long eventId, EventRequestStatusUpdateRequest request) {
                log.error("Сервис запросов недоступен для updateRequestStatuses: userId={}, eventId={}", userId, eventId, cause);
                throw new RuntimeException("Сервис запросов временно недоступен");
            }

            @Override
            public Integer getConfirmedRequestsCount(Long eventId) {
                log.error("Сервис запросов недоступен для getConfirmedRequestsCount: {}", eventId, cause);
                return 0;
            }
        };
    }
}
