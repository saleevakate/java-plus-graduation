package ru.practicum.analyzer.processor;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.analyzer.service.user.UserActionService;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionProcessor {

    private static final String USER_ACTIONS_TOPIC = "stats.user-actions.v1";

    private final Consumer<String, UserActionAvro> userActionConsumer;
    private final UserActionService userActionService;

    @Value("${spring.kafka.poll.timeout:1000}")
    private long pollTimeout;

    @PostConstruct
    public void init() {
        new Thread(this::process).start();
    }

    public void process() {
        try {
            userActionConsumer.subscribe(List.of(USER_ACTIONS_TOPIC));
            log.info("UserActionProcessor подписался на топик: {}", USER_ACTIONS_TOPIC);

            while (true) {
                ConsumerRecords<String, UserActionAvro> records = userActionConsumer.poll(Duration.ofMillis(pollTimeout));

                if (records.isEmpty()) {
                    continue;
                }

                log.info("Получено {} действий пользователей", records.count());

                for (var record : records) {
                    try {
                        UserActionAvro event = record.value();
                        userActionService.processUserAction(event);
                    } catch (Exception e) {
                        log.error("Ошибка обработки действия пользователя", e);
                    }
                }

                try {
                    userActionConsumer.commitSync();
                    log.debug("Смещения зафиксированы");
                } catch (Exception e) {
                    log.error("Ошибка фиксации смещений", e);
                }
            }

        } catch (Exception e) {
            log.error("Ошибка в UserActionProcessor", e);
        } finally {
            try {
                userActionConsumer.commitSync();
            } catch (Exception e) {
                log.error("Ошибка фиксации смещений при завершении", e);
            } finally {
                userActionConsumer.close();
            }
        }
    }
}
