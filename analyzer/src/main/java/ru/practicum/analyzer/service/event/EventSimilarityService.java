package ru.practicum.analyzer.service.event;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

public interface EventSimilarityService {

    void processEventSimilarity(EventSimilarityAvro event);
}
