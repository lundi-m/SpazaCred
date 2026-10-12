package com.lundi_m.spazacred_api.audit.dto;

import java.math.BigDecimal;

public record CreditAccountResponse(
        Long id,
        Long customerId,
        BigDecimal creditLimit,
        String status,
        BigDecimal outstandingBalance,
        BigDecimal availableBalance
) {}
