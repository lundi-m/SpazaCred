package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends ApiException{
    public PaymentNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "No payment found with ID " + id);
    }
}
