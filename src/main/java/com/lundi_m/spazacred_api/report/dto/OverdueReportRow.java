package com.lundi_m.spazacred_api.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OverdueReportRow(
        Long transactionId,
        String referenceNumber,
        Long customerId,
        String customerName,
        String phone,
        LocalDate dueDate,
        long daysOverdue,
        BigDecimal amount,
        BigDecimal paid,
        BigDecimal remaining
) {}
