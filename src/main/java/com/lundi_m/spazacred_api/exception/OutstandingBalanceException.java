package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

public class OutstandingBalanceException extends ApiException {
    public OutstandingBalanceException(Long id) {
        super(HttpStatus.CONFLICT,
                "Customer deactivate customer " + id + " with an outstanding balance"
        );
    }
}
