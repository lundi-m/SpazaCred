package com.lundi_m.spazacred_api.credit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreditTransactionResponse(

        @Schema(description = "Unique transaction identifier", example = "1")
        Long id,

        @Schema(description = "Unique purchase reference number", example = "CRT-2026-000001")
        String referenceNumber,

        @Schema(description = "Customer identifier", example = "1")
        Long customerId,

        @Schema(description = "Purchase amount", example = "500.00")
        BigDecimal amount,

        @Schema(description = "Description of the purchase", example = "Groceries")
        String description,

        @Schema(description = "Payment due date", example = "2026-12-04")
        LocalDate dueDate,

        @Schema(description = "Current transaction status", example = "ACTIVE")
        String status,

        @Schema(description = "Date and time the purchase was recorded", example = "2026-11-04T14:30:00")
        LocalDateTime createdAt
) {}
