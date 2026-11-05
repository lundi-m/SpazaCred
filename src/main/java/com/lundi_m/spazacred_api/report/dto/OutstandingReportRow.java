package com.lundi_m.spazacred_api.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record OutstandingReportRow(

        @Schema(description = "Customer ID", example = "1")
        Long customerId,

        @Schema(description = "Customer's full name", example = "Thabo Mokoena")
        String customerName,

        @Schema(description = "Customer's contact number", example = "+27821234567")
        String phone,

        @Schema(description = "Current credit account status",
                example = "ACTIVE",
                allowableValues = {"ACTIVE", "SUSPENDED", "CLOSED"})
        String accountStatus,

        @Schema(description = "Customer's credit limit", example = "5000.00")
        BigDecimal creditLimit,

        @Schema(description = "Total outstanding balance", example = "1200.00")
        BigDecimal outstandingBalance,

        @Schema(description = "Credit still available to the customer", example = "3800.00")
        BigDecimal availableCredit
) {}
