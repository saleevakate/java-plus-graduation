package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.exception.ValidationDataException;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class StatsClient {

    private static final String STATS_SERVICE_ID = "stats-server";

    private final DiscoveryClient discoveryClient;
    private final RestClient restClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClient(DiscoveryClient discoveryClient, RestClient restClient) {
        this.discoveryClient = discoveryClient;
        this.restClient = restClient;
    }

    @Retryable(
            retryFor = {RestClientException.class, IllegalStateException.class},
            backoff = @Backoff(delay = 3000, maxDelay = 5000)
    )
    private URI getServiceUri(String path, Object... uriVariables) {
        List<ServiceInstance> instances = discoveryClient.getInstances(STATS_SERVICE_ID);

        if (instances == null || instances.isEmpty()) {
            log.error("Сервис '{}' не найден в Eureka", STATS_SERVICE_ID);
            throw new IllegalStateException(
                    String.format("Сервис статистики '%s' временно недоступен", STATS_SERVICE_ID)
            );
        }

        ServiceInstance instance = instances.get(0);
        String host = instance.getHost();
        int port = instance.getPort();

        log.debug("Найден экземпляр '{}' на {}:{}", STATS_SERVICE_ID, host, port);

        return UriComponentsBuilder
                .newInstance()
                .scheme("http")
                .host(host)
                .port(port)
                .path(path)
                .buildAndExpand(uriVariables)
                .toUri();
    }

    public EndpointHit saveHit(EndpointHit hit) throws RestClientException {
        log.debug("Сохранение hit: app={}, uri={}", hit.app(), hit.uri());

        URI uri = getServiceUri("/hit");

        var response = restClient.post()
                .uri(uri)
                .body(hit)
                .retrieve()
                .toEntity(EndpointHit.class);

        if (response.getStatusCode().value() != HttpStatus.CREATED.value()) {
            log.error("Ошибка сохранения hit. Ожидался 201, получен {}", response.getStatusCode());
            throw new RestClientException(
                    "Ожидался код ответа 201, но получен " + response.getStatusCode()
            );
        }

        return response.getBody();
    }

    public List<ViewStats> getHits(String start, String end, List<String> uris, Boolean unique)
            throws RestClientException {

        validateDates(start, end);

        URI uri = UriComponentsBuilder
                .fromUri(getServiceUri("/stats"))
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("uris", uris)
                .queryParam("unique", unique)
                .build()
                .toUri();

        log.debug("Запрос статистики на {}", uri);

        var response = restClient.get()
                .uri(uri)
                .retrieve()
                .toEntity(new ParameterizedTypeReference<List<ViewStats>>() {
                });

        if (response.getStatusCode().value() != HttpStatus.OK.value()) {
            log.error("Ошибка получения статистики. Ожидался 200, получен {}", response.getStatusCode());
            throw new RestClientException(
                    "Ожидался код ответа 200, но получен " + response.getStatusCode()
            );
        }

        return response.getBody();
    }

    private void validateDates(String start, String end) {
        try {
            LocalDateTime startDateTime = LocalDateTime.parse(start, formatter);
            LocalDateTime endDateTime = LocalDateTime.parse(end, formatter);

            if (startDateTime.isAfter(LocalDateTime.now())) {
                throw new ValidationDataException("Дата начала не может быть позже текущего времени");
            }
            if (startDateTime.isAfter(endDateTime)) {
                throw new ValidationDataException("Дата начала не может быть позже даты конца");
            }
        } catch (java.time.format.DateTimeParseException e) {
            throw new ValidationDataException(
                    "Неверный формат даты. Ожидается: yyyy-MM-dd HH:mm:ss"
            );
        }
    }
}
