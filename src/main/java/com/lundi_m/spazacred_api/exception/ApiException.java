package com.lundi_m.spazacred_api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ApiException extends RuntimeException{

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String message) {
        this(status, status.name(), message);
    }

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
