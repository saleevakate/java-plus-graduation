package ru.practicum.config;

import feign.RequestInterceptor;
import feign.Retryer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@Slf4j
@Configuration
public class FeignConfig {

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 3);
    }

    @Bean
    public RequestInterceptor requestIdInterceptor() {
        return template -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // Передаем X-Request-Id для трассировки
                String requestId = request.getHeader("X-Request-Id");
                if (requestId == null || requestId.isBlank()) {
                    requestId = UUID.randomUUID().toString();
                }
                template.header("X-Request-Id", requestId);

                // Передаем Authorization для Basic Auth
                String auth = request.getHeader("Authorization");
                if (auth != null) {
                    template.header("Authorization", auth);
                }
            }

            // Источник запроса
            template.header("X-Source-Service", "unknown");
        };
    }
}
