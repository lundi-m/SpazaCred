package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

public class CustomerInactiveException extends ApiException{
    public CustomerInactiveException(Long customerId) {
        super(HttpStatus.CONFLICT, "Customer " + customerId + " is deactivated");
    }
}
