package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

public class PaymentNotVoidableException extends ApiException{
    public PaymentNotVoidableException(String message) {
        super(HttpStatus.CONFLICT ,message);
    }
}
