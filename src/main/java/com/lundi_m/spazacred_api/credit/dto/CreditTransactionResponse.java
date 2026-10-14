package com.lundi_m.spazacred_api.credit.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreditTransactionResponse(
        Long id,
        String referenceNumber,
        Long customerId,
        BigDecimal amount,
        String description,
        LocalDate dueDate,
        String status,
        LocalDateTime createdAt
) {
}
