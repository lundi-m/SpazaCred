package com.lundi_m.spazacred_api.report.dto;

import com.lundi_m.spazacred_api.payment.dto.PaymentMethodTotal;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PaymentReportResponse(
        LocalDate from,
        LocalDate to,
        long paymentCount,
        BigDecimal totalCollected,
        List<PaymentMethodTotal> byMethod,
        Page<PaymentReportRow> payments
) {}
