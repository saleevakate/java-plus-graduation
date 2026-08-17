package ru.practicum.aggregator.starter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.aggregator.service.SimilarityService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private static final String USER_ACTIONS_TOPIC = "stats.user-actions.v1";
    private static final String SIMILARITY_TOPIC = "stats.events-similarity.v1";

    private final Consumer<String, UserActionAvro> consumer;
    private final Producer<String, EventSimilarityAvro> producer;
    private final SimilarityService similarityService;

    @Value("${spring.kafka.poll.timeout:1000}")
    private long pollTimeout;

    public void start() {
        try {
            consumer.subscribe(List.of(USER_ACTIONS_TOPIC));
            log.info("Aggregator подписался на топик: {}", USER_ACTIONS_TOPIC);

            while (true) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(Duration.ofMillis(pollTimeout));

                if (records.isEmpty()) {
                    continue;
                }

                log.info("Получено {} действий пользователей", records.count());

                for (var record : records) {
                    try {
                        UserActionAvro event = record.value();
                        log.info("Обработка: userId={}, eventId={}, actionType={}",
                                event.getUserId(), event.getEventId(), event.getActionType());

                        List<EventSimilarityAvro> similarities = similarityService.processUserAction(event);

                        for (EventSimilarityAvro similarity : similarities) {
                            String key = similarity.getEventA() + "-" + similarity.getEventB();

                            ProducerRecord<String, EventSimilarityAvro> producerRecord = new ProducerRecord<>(
                                    SIMILARITY_TOPIC,
                                    key,
                                    similarity
                            );

                            producer.send(producerRecord, (metadata, exception) -> {
                                if (exception != null) {
                                    log.error("Ошибка отправки сходства в топик {}", SIMILARITY_TOPIC, exception);
                                } else {
                                    log.info("Сходство отправлено: eventA={}, eventB={}, score={}, offset={}",
                                            similarity.getEventA(), similarity.getEventB(),
                                            similarity.getScore(), metadata.offset());
                                }
                            });
                        }

                        producer.flush();

                    } catch (Exception e) {
                        log.error("Ошибка обработки действия пользователя", e);
                    }
                }

                try {
                    consumer.commitSync();
                    log.debug("Смещения зафиксированы");
                } catch (Exception e) {
                    log.error("Ошибка фиксации смещений", e);
                }
            }

        } catch (WakeupException e) {
            log.info("Получен сигнал завершения работы");
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий", e);
        } finally {
            try {
                producer.flush();
                log.info("Все сообщения отправлены");
            } finally {
                try {
                    consumer.commitSync();
                    log.info("Смещения зафиксированы");
                } catch (Exception e) {
                    log.error("Ошибка фиксации смещений при завершении", e);
                } finally {
                    log.info("Закрываем consumer");
                    consumer.close();
                    log.info("Закрываем producer");
                    producer.close();
                }
            }
        }
    }
}
