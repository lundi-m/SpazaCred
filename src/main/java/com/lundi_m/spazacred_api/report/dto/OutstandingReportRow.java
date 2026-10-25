package com.lundi_m.spazacred_api.report.dto;

import java.math.BigDecimal;

public record OutstandingReportRow(
        Long customerId,
        String customerName,
        String phone,
        String accountStatus,
        BigDecimal creditLimit,
        BigDecimal outstandingBalance,
        BigDecimal availableCredit
) {}
