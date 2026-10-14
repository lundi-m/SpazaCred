package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class CreditLimitExceededException extends ApiException {
    public CreditLimitExceededException(BigDecimal outstanding, BigDecimal amount, BigDecimal limit) {
        super(HttpStatus.CONFLICT, String.format(
                "Purchase of %s would bring outstanding balance %s, exceeding the credit limit of %s",
                amount, outstanding.add(amount), limit
        ));
    }
}
