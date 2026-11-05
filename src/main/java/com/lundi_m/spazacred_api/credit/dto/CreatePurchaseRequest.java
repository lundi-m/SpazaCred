package com.lundi_m.spazacred_api.credit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePurchaseRequest(

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        @Digits(
                integer = 17,
                fraction = 2,
                message = "amount must have no more than 2 decimal places"
        )
        @Schema(
                description = "Purchase amount. Must be greater than zero and have at most 2 decimal places.",
                example = "500.00"
        )
        BigDecimal amount,

        @Schema(
                description = "Description of the goods purchased",
                example = "Groceries"
        )
        String description,

        @FutureOrPresent(message = "due date cannot be in the past")
        @Schema(
                description = "Payment due date. Optional; defaults to 30 days from the purchase date.",
                example = "2026-12-04",
                type = "string",
                format = "date"
        )
        LocalDate dueDate
) {}
