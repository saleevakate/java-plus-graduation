package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@RestController
@RequestMapping("/test-cleanup")
@RequiredArgsConstructor
@Slf4j
public class TestCleanupController {

    @Qualifier("userDataSource")
    private final DataSource userDataSource;

    @Qualifier("eventDataSource")
    private final DataSource eventDataSource;

    @Qualifier("requestDataSource")
    private final DataSource requestDataSource;

    @Qualifier("statsDataSource")
    private final DataSource statsDataSource;

    @DeleteMapping
    public ResponseEntity<String> cleanDatabase() {
        log.info("=== НАЧАЛО ОЧИСТКИ ВСЕХ БАЗ ДАННЫХ ===");

        try {
            cleanUserDb();
            cleanEventDb();
            cleanRequestDb();
            cleanStatsDb();

            log.info("=== ВСЕ БАЗЫ ДАННЫХ УСПЕШНО ОЧИЩЕНЫ ===");
            return ResponseEntity.ok("Все базы данных успешно очищены!");

        } catch (Exception e) {
            log.error("!!! ОШИБКА ПРИ ОЧИСТКЕ БАЗ ДАННЫХ: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Ошибка при очистке баз данных: " + e.getMessage());
        }
    }

    private void cleanUserDb() throws SQLException {
        log.info("Очистка user-db...");
        try (Connection connection = userDataSource.getConnection()) {
            log.debug("Подключение к user-db установлено");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("cleanup-user.sql"));
            log.info("user-db успешно очищена");
        } catch (Exception e) {
            log.error("Ошибка при очистке user-db: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void cleanEventDb() throws SQLException {
        log.info("Очистка event-db...");
        try (Connection connection = eventDataSource.getConnection()) {
            log.debug("Подключение к event-db установлено");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("cleanup-event.sql"));
            log.info("event-db успешно очищена");
        } catch (Exception e) {
            log.error("Ошибка при очистке event-db: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void cleanRequestDb() throws SQLException {
        log.info("Очистка request-db...");
        try (Connection connection = requestDataSource.getConnection()) {
            log.debug("Подключение к request-db установлено");
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("cleanup-request.sql"));
            log.info("request-db успешно очищена");
        } catch (Exception e) {
            log.error("Ошибка при очистке request-db: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void cleanStatsDb() throws SQLException {
        log.info("Очистка stats-db...");
        try (Connection connection = statsDataSource.getConnection()) {
            log.debug("Подключение к stats-db установлено");
            connection.createStatement().execute("TRUNCATE TABLE endpoint_hits CASCADE");
            log.info("stats-db успешно очищена");
        } catch (Exception e) {
            log.error("Ошибка при очистке stats-db: {}", e.getMessage(), e);
            throw e;
        }
    }
}
