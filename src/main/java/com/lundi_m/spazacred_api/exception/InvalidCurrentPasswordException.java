package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

public class InvalidCurrentPasswordException extends ApiException{
    public InvalidCurrentPasswordException() {
        super(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "The current password is incorrect");
    }
}
