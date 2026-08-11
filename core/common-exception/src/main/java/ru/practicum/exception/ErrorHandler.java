package ru.practicum.exception;

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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class ErrorHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFoundException(final NotFoundException e) {
        log.error("Resource not found: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Запрашиваемый ресурс не найден",
                        "404"
                ));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflictException(final ConflictException e) {
        log.error("Conflict: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Конфликт данных",
                        "409"
                ));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidationException(final ValidationException e) {
        log.error("Validation error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Ошибка валидации данных",
                        "400"
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getAllErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.error("Validation failed: {}", errorMessage, e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        getStackTrace(e),
                        errorMessage,
                        "Ошибка валидации данных",
                        "400"
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationException(final ConstraintViolationException e) {
        String errorMessage = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        log.error("Constraint violation: {}", errorMessage, e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        getStackTrace(e),
                        errorMessage,
                        "Ошибка валидации данных",
                        "400"
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolationException(final DataIntegrityViolationException e) {
        log.error("Data integrity violation: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Нарушение целостности данных",
                        "409"
                ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(final MissingServletRequestParameterException e) {
        log.error("Missing parameter: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Отсутствует обязательный параметр запроса",
                        "400"
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(final MethodArgumentTypeMismatchException e) {
        log.error("Type mismatch: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Некорректный тип параметра",
                        "400"
                ));
    }

    @ExceptionHandler(UserServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleUserServiceUnavailable(final UserServiceUnavailableException e) {
        log.error("User service unavailable: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Сервис пользователей временно недоступен",
                        "503"
                ));
    }

    @ExceptionHandler(EventServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleEventServiceUnavailable(final EventServiceUnavailableException e) {
        log.error("Event service unavailable: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Сервис событий временно недоступен",
                        "503"
                ));
    }

    @ExceptionHandler(RequestServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleRequestServiceUnavailable(final RequestServiceUnavailableException e) {
        log.error("Request service unavailable: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Сервис заявок временно недоступен",
                        "503"
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleException(final Exception e) {
        log.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(
                        getStackTrace(e),
                        e.getMessage(),
                        "Непредвиденная ошибка сервера",
                        "500"
                ));
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
