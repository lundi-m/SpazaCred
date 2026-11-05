package com.lundi_m.spazacred_api.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentResponse(

        @Schema(description = "Unique payment ID", example = "1")
        Long id,

        @Schema(description = "Unique payment reference number",
                example = "PAY-2026-000001")
        String referenceNumber,

        @Schema(description = "ID of the customer who made the payment",
                example = "1")
        Long customerId,

        @Schema(description = "Total payment amount", example = "800.00")
        BigDecimal amount,

        @Schema(description = "Payment method used", example = "CASH")
        String paymentMethod,

        @Schema(description = "Current payment status",
                example = "ACTIVE",
                allowableValues = {"ACTIVE", "VOIDED"})
        String status,

        @Schema(description = "Date and time the payment was recorded",
                example = "2026-11-04T14:30:00")
        LocalDateTime createdAt,

        @Schema(description = "List of purchases to which this payment was allocated")
        List<PaymentAllocationResponse> allocations
) {}
