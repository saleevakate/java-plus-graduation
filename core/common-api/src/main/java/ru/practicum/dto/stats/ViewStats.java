package ru.practicum.dto.stats;

public record ViewStats(
        String app,
        String uri,
        long hits
) {
}
