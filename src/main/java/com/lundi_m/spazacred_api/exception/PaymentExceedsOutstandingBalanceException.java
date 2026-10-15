package com.lundi_m.spazacred_api.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class PaymentExceedsOutstandingBalanceException extends ApiException{
    public PaymentExceedsOutstandingBalanceException(BigDecimal amount, BigDecimal outstanding) {
        super(HttpStatus.CONFLICT, String.format(
                "Payment of %s exceeds the outstanding balance of %s", amount, outstanding
        ));
    }
}
