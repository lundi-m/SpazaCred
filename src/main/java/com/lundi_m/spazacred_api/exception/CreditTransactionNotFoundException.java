package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

public class CreditTransactionNotFoundException extends ApiException{
    public CreditTransactionNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "No credit transaction found with ID " + id);
    }
}
