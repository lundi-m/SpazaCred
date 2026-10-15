package com.lundi_m.spazacred_api.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentResponse(
        Long id,
        String referenceNumber,
        Long customerId,
        BigDecimal amount,
        String paymentMethod,
        String status,
        LocalDateTime createdAt,
        List<PaymentAllocationResponse> allocations
) {
}
