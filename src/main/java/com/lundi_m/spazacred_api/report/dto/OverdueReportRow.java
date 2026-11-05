package com.lundi_m.spazacred_api.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
public record OverdueReportRow(

        @Schema(description = "Credit transaction ID", example = "12")
        Long transactionId,

        @Schema(description = "Credit transaction reference number",
                example = "CR-2026-000012")
        String referenceNumber,

        @Schema(description = "Customer ID", example = "1")
        Long customerId,

        @Schema(description = "Customer's full name", example = "Thabo Mokoena")
        String customerName,

        @Schema(description = "Customer's contact number", example = "+27821234567")
        String phone,

        @Schema(description = "Transaction due date", example = "2026-10-20")
        LocalDate dueDate,

        @Schema(description = "Number of days the transaction is overdue",
                example = "15")
        long daysOverdue,

        @Schema(description = "Original transaction amount", example = "1000.00")
        BigDecimal amount,

        @Schema(description = "Amount already paid towards the transaction",
                example = "300.00")
        BigDecimal paid,

        @Schema(description = "Remaining unpaid amount", example = "700.00")
        BigDecimal remaining
) {}
