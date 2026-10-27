package com.lundi_m.spazacred_api.credit.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdjustLimitRequest(
        @NotNull(message = "new limit is required")
        @DecimalMin(value = "0.00", message = "credit limit cannot be negative")
        @Digits(
                integer = 17,
                fraction = 2,
                message = "amount must have no more than 2 decimal places"
        )
        BigDecimal newLimit,

        @NotBlank(message = "a reason is required for limit changes")
        String reason
)
{}
