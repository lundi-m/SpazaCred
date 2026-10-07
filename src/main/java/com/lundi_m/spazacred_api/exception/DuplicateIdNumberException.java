package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

public class DuplicateIdNumberException extends ApiException {
    public DuplicateIdNumberException(String idNumber) {
        super(HttpStatus.CONFLICT, "A customer with ID number " + idNumber + " already exists");
    }
}
