package ru.practicum.client.request;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.config.FeignConfig;
import ru.practicum.dto.participation.EventRequestStatusUpdateRequest;
import ru.practicum.dto.participation.EventRequestStatusUpdateResult;
import ru.practicum.dto.participation.ParticipationRequestDto;


import java.util.List;

@FeignClient(
        name = "request-service",
        configuration = FeignConfig.class,
        fallbackFactory = RequestServiceClientFallbackFactory.class
)
public interface RequestServiceClient {

    @GetMapping("/users/{userId}/requests")
    List<ParticipationRequestDto> getUserRequests(@PathVariable("userId") Long userId);

    @PostMapping("/users/{userId}/requests")
    ParticipationRequestDto addParticipationRequest(@PathVariable("userId") Long userId,
                                                    @RequestParam("eventId") Long eventId);

    @PostMapping("/users/{userId}/requests/{requestId}/cancel")
    ParticipationRequestDto cancelRequest(@PathVariable("userId") Long userId,
                                          @PathVariable("requestId") Long requestId);

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    List<ParticipationRequestDto> getEventRequests(@PathVariable("userId") Long userId,
                                                   @PathVariable("eventId") Long eventId);

    @PostMapping("/users/{userId}/events/{eventId}/requests")
    EventRequestStatusUpdateResult updateRequestStatuses(@PathVariable("userId") Long userId,
                                                         @PathVariable("eventId") Long eventId,
                                                         @RequestBody EventRequestStatusUpdateRequest request);

    @GetMapping("/requests/count/{eventId}")
    Integer getConfirmedRequestsCount(@PathVariable("eventId") Long eventId);
}
