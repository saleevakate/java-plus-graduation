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
                log.error("Request service unavailable for getUserRequests: {}", userId, cause);
                return List.of();
            }

            @Override
            public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
                log.error("Request service unavailable for addParticipationRequest: userId={}, eventId={}", userId, eventId, cause);
                throw new RuntimeException("Request service temporarily unavailable");
            }

            @Override
            public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
                log.error("Request service unavailable for cancelRequest: userId={}, requestId={}", userId, requestId, cause);
                throw new RuntimeException("Request service temporarily unavailable");
            }

            @Override
            public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
                log.error("Request service unavailable for getEventRequests: userId={}, eventId={}", userId, eventId, cause);
                return List.of();
            }

            @Override
            public EventRequestStatusUpdateResult updateRequestStatuses(Long userId, Long eventId, EventRequestStatusUpdateRequest request) {
                log.error("Request service unavailable for updateRequestStatuses: userId={}, eventId={}", userId, eventId, cause);
                throw new RuntimeException("Request service temporarily unavailable");
            }

            @Override
            public Integer getConfirmedRequestsCount(Long eventId) {
                log.error("Request service unavailable for getConfirmedRequestsCount: {}", eventId, cause);
                return 0;
            }
        };
    }
}
