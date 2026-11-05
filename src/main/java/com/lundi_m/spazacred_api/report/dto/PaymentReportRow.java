package com.lundi_m.spazacred_api.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
public record PaymentReportRow(

        @Schema(description = "Payment ID", example = "25")
        Long paymentId,

        @Schema(description = "Payment reference number",
                example = "PAY-2026-000025")
        String referenceNumber,

        @Schema(description = "Customer ID", example = "1")
        Long customerId,

        @Schema(description = "Customer's full name", example = "Thabo Mokoena")
        String customerName,

        @Schema(description = "Payment amount", example = "800.00")
        BigDecimal amount,

        @Schema(description = "Payment method", example = "CASH")
        String paymentMethod,

        @Schema(description = "Date and time the payment was recorded",
                example = "2026-11-04T14:30:00")
        LocalDateTime createdAt
) {}
