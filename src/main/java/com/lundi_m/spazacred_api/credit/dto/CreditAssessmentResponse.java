package com.lundi_m.spazacred_api.credit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record CreditAssessmentResponse(

        @Schema(description = "Customer identifier", example = "1")
        Long customerId,

        @Schema(description = "Whether the customer is eligible to take new credit", example = "true")
        boolean eligible,

        @Schema(description = "Reasons explaining the eligibility result",
                example = "[\"Customer has overdue transactions\"]")
        List<String> reasons,

        @Schema(description = "Current credit account status", example = "ACTIVE")
        String accountStatus,

        @Schema(description = "Approved credit limit", example = "5000.00")
        BigDecimal creditLimit,

        @Schema(description = "Current outstanding balance", example = "1200.00")
        BigDecimal outstandingBalance,

        @Schema(description = "Credit currently available to the customer", example = "3800.00")
        BigDecimal availableCredit,

        @Schema(description = "Number of overdue purchases", example = "2")
        int overdueTransactionCount,

        @Schema(description = "Total outstanding amount across overdue purchases", example = "700.00")
        BigDecimal overdueAmount,

        @Schema(description = "Age in days of the oldest overdue purchase", example = "12")
        Long oldestOverdueDays
) {}
