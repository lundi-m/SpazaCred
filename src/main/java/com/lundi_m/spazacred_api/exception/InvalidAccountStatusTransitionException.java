package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

public class InvalidAccountStatusTransitionException extends ApiException{
    public InvalidAccountStatusTransitionException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
