package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

public class InvalidDateRangeException extends ApiException{
    public InvalidDateRangeException() {
        super(HttpStatus.BAD_REQUEST, "'from' date must not be after 'to' date");
    }
}
