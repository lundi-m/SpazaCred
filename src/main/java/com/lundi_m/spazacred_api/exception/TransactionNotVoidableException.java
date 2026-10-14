package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

public class TransactionNotVoidableException extends ApiException{
    public TransactionNotVoidableException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
