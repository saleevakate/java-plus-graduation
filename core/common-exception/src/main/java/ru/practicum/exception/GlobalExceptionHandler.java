package ru.practicum.exception;

import feign.FeignException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFoundException(final NotFoundException e) {
        log.error("Ресурс не найден: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Запрашиваемый ресурс не найден",
                        HttpStatus.NOT_FOUND.name(),
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflictException(final ConflictException e) {
        log.error("Конфликт: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Конфликт данных",
                        HttpStatus.CONFLICT.name(),
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidationException(final ValidationException e) {
        log.error("Ошибка валидации: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Ошибка валидации данных",
                        HttpStatus.BAD_REQUEST.name(),
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerMethodValidationException(final HandlerMethodValidationException e) {
        log.error("Ошибка валидации параметров запроса: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        getStackTrace(e),
                        "Невалидные параметры запроса",
                        "Ошибка валидации данных",
                        HttpStatus.BAD_REQUEST.name(),
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.error("Ошибка валидации: {}", errorMessage, e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        getStackTrace(e),
                        errorMessage,
                        "Ошибка валидации данных",
                        HttpStatus.BAD_REQUEST.name(),
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationException(final ConstraintViolationException e) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        log.error("Нарушение ограничений: {}", errorMessage, e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        getStackTrace(e),
                        errorMessage,
                        "Ошибка валидации данных",
                        HttpStatus.BAD_REQUEST.name(),
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolationException(final DataIntegrityViolationException e) {
        log.error("Нарушение целостности данных: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Нарушение целостности данных",
                        HttpStatus.CONFLICT.name(),
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(final MissingServletRequestParameterException e) {
        log.error("Отсутствует параметр запроса: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Отсутствует обязательный параметр запроса",
                        HttpStatus.BAD_REQUEST.name(),
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(final MethodArgumentTypeMismatchException e) {
        log.error("Несоответствие типа: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Некорректный тип параметра",
                        HttpStatus.BAD_REQUEST.name(),
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiError> handleFeignException(FeignException e) {
        log.error("Ошибка при вызове внешнего сервиса: {}", e.getMessage(), e);

        if (e.status() == 400) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ApiError(
                            getStackTrace(e),
                            e.getMessage(),
                            "Ошибка валидации данных в сервисе событий",
                            HttpStatus.BAD_REQUEST.name(),
                            LocalDateTime.now()
                    ));
        }

        if (e.status() == 404) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiError(
                            getStackTrace(e),
                            e.getMessage(),
                            "Ресурс не найден в сервисе событий",
                            HttpStatus.NOT_FOUND.name(),
                            LocalDateTime.now()
                    ));
        }

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError(
                        getStackTrace(e),
                        "Сервис событий временно недоступен",
                        "Внешний сервис временно недоступен",
                        HttpStatus.SERVICE_UNAVAILABLE.name(),
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(final Exception e) {
        log.error("Непредвиденная ошибка: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Непредвиденная ошибка сервера",
                        HttpStatus.INTERNAL_SERVER_ERROR.name(),
                        LocalDateTime.now()
                ));
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
