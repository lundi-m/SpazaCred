package com.lundi_m.spazacred_api.credit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(
                description = "New credit limit. Maximum 2 decimal places.",
                example = "5000.00"
        )
        BigDecimal newLimit,

        @NotBlank(message = "a reason is required for limit changes")
        @Schema(
                description = "Reason for changing the credit limit",
                example = "Customer has demonstrated a consistent repayment history."
        )
        String reason

) {}
