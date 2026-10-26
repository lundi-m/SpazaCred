package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(HttpStatus status, String code, String message) {
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                code, message, null);
    }

    public static ErrorResponse ofFieldErrors(Map<String, String> fieldErrors) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(),
                "VALIDATION_FAILED", "Validation failed", fieldErrors);
    }
}