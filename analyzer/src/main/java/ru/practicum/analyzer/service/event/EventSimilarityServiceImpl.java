package ru.practicum.analyzer.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.EventSimilarity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSimilarityServiceImpl implements EventSimilarityService {

    private final EventSimilarityRepository repository;

    @Override
    @Transactional
    public void processEventSimilarity(EventSimilarityAvro event) {
        Long eventA = event.getEventA();
        Long eventB = event.getEventB();
        Double score = event.getScore();
        Instant timestamp = event.getTimestamp();

        Optional<EventSimilarity> existing = repository.findByEventAAndEventB(eventA, eventB);

        if (existing.isPresent()) {
            EventSimilarity similarity = existing.get();
            similarity.setScore(score);
            similarity.setUpdatedAt(timestamp);
            repository.save(similarity);
            log.info("Обновлено сходство: eventA={}, eventB={}, score={}", eventA, eventB, score);
        } else {
            EventSimilarity similarity = EventSimilarity.builder()
                    .eventA(eventA)
                    .eventB(eventB)
                    .score(score)
                    .updatedAt(timestamp)
                    .build();
            repository.save(similarity);
            log.info("Сохранено новое сходство: eventA={}, eventB={}, score={}", eventA, eventB, score);
        }
    }
}
