package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends ApiException{
    public DuplicateEmailException(String email) {
        super(HttpStatus.CONFLICT, email + " already exists");
    }
}
