package com.lundi_m.spazacred_api.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record PaymentAllocationResponse(

        @Schema(description = "ID of the credit transaction receiving the allocation",
                example = "12")
        Long creditTransactionId,

        @Schema(description = "Reference number of the allocated purchase",
                example = "CR-2026-000012")
        String transactionReferenceNumber,

        @Schema(description = "Amount allocated to this purchase",
                example = "500.00")
        BigDecimal amountAllocated
) {}