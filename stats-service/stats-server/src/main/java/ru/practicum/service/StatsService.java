package ru.practicum.service;

import ru.practicum.dto.stats.EndpointHit;
import ru.practicum.dto.stats.ViewStats;

import java.util.List;

public interface StatsService {
    EndpointHit saveEndpointHit(EndpointHit endpointHit);

    List<ViewStats> getViewStats(String start, String end, List<String> uris, Boolean unique);
}
