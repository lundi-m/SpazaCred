package com.lundi_m.spazacred_api.report.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentReportRow(
        Long paymentId,
        String referenceNumber,
        Long customerId,
        String customerName,
        BigDecimal amount,
        String paymentMethod,
        LocalDateTime createdAt
) {}
