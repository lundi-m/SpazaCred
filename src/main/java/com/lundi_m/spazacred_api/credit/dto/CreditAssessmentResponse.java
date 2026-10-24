package com.lundi_m.spazacred_api.credit.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreditAssessmentResponse(
        Long customerId,
        boolean eligible,
        List<String> reasons,
        String accountStatus,
        BigDecimal creditLimit,
        BigDecimal outstandingBalance,
        BigDecimal availableCredit,
        int overdueTransactionCount,
        BigDecimal overdueAmount,
        Long oldestOverdueDays
) {}
