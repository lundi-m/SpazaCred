package com.lundi_m.spazacred_api.credit.dto;

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
        BigDecimal amount,

        String description,

        @FutureOrPresent(message = "due date cannot be in the past")
        LocalDate dueDate
) {
}