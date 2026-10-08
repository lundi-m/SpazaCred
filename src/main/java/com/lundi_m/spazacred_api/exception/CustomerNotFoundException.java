package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

public class CustomerNotFoundException extends ApiException {
    public CustomerNotFoundException(Long customerId) {
        super(HttpStatus.NOT_FOUND, "No customer found with ID " + customerId);
    }
}
