package com.lundi_m.spazacred_api.payment.dto;

import com.lundi_m.spazacred_api.payment.entity.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record PaymentMethodTotal(

        @Schema(description = "Payment method used", example = "CASH")
        PaymentMethod method,

        @Schema(description = "Number of payments recorded using this method",
                example = "25")
        Long count,

        @Schema(description = "Total amount received using this method",
                example = "12500.00")
        BigDecimal total
) {}
