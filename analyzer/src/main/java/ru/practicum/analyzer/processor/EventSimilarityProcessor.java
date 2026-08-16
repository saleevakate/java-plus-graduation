package ru.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.service.event.EventSimilarityService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityProcessor {

    private static final String SIMILARITY_TOPIC = "stats.events-similarity.v1";

    private final Consumer<String, EventSimilarityAvro> eventSimilarityConsumer;
    private final EventSimilarityService eventSimilarityService;

    @Value("${spring.kafka.poll.timeout:1000}")
    private long pollTimeout;

    @PostConstruct
    public void init() {
        new Thread(this::process).start();
    }

    public void process() {
        try {
            eventSimilarityConsumer.subscribe(List.of(SIMILARITY_TOPIC));
            log.info("EventSimilarityProcessor подписался на топик: {}", SIMILARITY_TOPIC);

            while (true) {
                ConsumerRecords<String, EventSimilarityAvro> records = eventSimilarityConsumer.poll(Duration.ofMillis(pollTimeout));

                if (records.isEmpty()) {
                    continue;
                }

                log.info("Получено {} сходств мероприятий", records.count());

                for (var record : records) {
                    try {
                        EventSimilarityAvro event = record.value();
                        eventSimilarityService.processEventSimilarity(event);
                    } catch (Exception e) {
                        log.error("Ошибка обработки сходства мероприятий", e);
                    }
                }

                try {
                    eventSimilarityConsumer.commitSync();
                    log.debug("Смещения зафиксированы");
                } catch (Exception e) {
                    log.error("Ошибка фиксации смещений", e);
                }
            }

        } catch (Exception e) {
            log.error("Ошибка в EventSimilarityProcessor", e);
        } finally {
            try {
                eventSimilarityConsumer.commitSync();
            } catch (Exception e) {
                log.error("Ошибка фиксации смещений при завершении", e);
            } finally {
                eventSimilarityConsumer.close();
            }
        }
    }
}
