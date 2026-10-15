package com.lundi_m.spazacred_api.payment.dto;

import java.math.BigDecimal;

public record PaymentAllocationResponse(
        Long creditTransactionId,
        String transactionReferenceNumber,
        BigDecimal amountAllocated
) {}
