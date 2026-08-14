package ru.practicum.dto.stats;

public record EndpointHitInfo(
        String app,
        String uri,
        String ip,
        String timestamp
) {
}