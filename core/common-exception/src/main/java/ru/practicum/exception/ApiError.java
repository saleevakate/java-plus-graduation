package ru.practicum.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ApiError(
        String error,
        String message,
        String reason,
        String status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timestamp
) {
    public ApiError(String error, String message, String reason, String status) {
        this(error, message, reason, status, LocalDateTime.now());
    }
}
