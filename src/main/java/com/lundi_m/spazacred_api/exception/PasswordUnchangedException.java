package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

public class PasswordUnchangedException extends ApiException{
    public PasswordUnchangedException() {
        super(HttpStatus.BAD_REQUEST, "PASSWORD_UNCHANGED",
                "The new password must be different from the current password");
    }
}
