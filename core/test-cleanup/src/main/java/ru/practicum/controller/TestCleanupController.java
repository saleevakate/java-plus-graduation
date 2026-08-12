package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;

@RestController
@RequestMapping("/test-cleanup")
@RequiredArgsConstructor
@Slf4j
public class TestCleanupController {

    private final DataSource dataSource;

    @DeleteMapping
    public ResponseEntity<String> cleanDatabase() {
        log.info("Начинается очистка базы данных...");

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new ClassPathResource("cleanup.sql")
            );
            log.info("База данных успешно очищена!");
            return ResponseEntity.ok("База данных успешно очищена!");
        } catch (Exception e) {
            log.error("Ошибка при очистке базы данных: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Ошибка при очистке базы данных: " + e.getMessage());
        }
    }
}
