package com.lundi_m.spazacred_api.payment.dto;

import com.lundi_m.spazacred_api.payment.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RecordPaymentRequest(

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "payment method is required")
        PaymentMethod paymentMethod
) {}
