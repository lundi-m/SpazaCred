package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

public class DuplicatePhoneException extends ApiException {
    public DuplicatePhoneException(String phone) {
        super(HttpStatus.CONFLICT, "A customer with phone number " + phone + " already exists");
    }
}
