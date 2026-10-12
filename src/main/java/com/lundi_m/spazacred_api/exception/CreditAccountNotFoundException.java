package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

public class CreditAccountNotFoundException extends ApiException{
    public CreditAccountNotFoundException(Long customerId) {
        super(HttpStatus.NOT_FOUND, "No credit account found for customer " + customerId);
    }
}
