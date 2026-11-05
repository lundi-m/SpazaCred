package com.lundi_m.spazacred_api.credit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record CreditAccountResponse(
        @Schema(description = "Unique credit account identifier", example = "1")
        Long id,

        @Schema(description = "Customer identifier", example = "1")
        Long customerId,

        @Schema(description = "Customer's approved credit limit", example = "5000.00")
        BigDecimal creditLimit,

        @Schema(description = "Current account status", example = "ACTIVE")
        String status,

        @Schema(description = "Total outstanding amount from active purchases", example = "1200.00")
        BigDecimal outstandingBalance,

        @Schema(description = "Remaining credit available to the customer", example = "3800.00")
        BigDecimal availableBalance
) {}
