package com.lundi_m.spazacred_api.payment.dto;

import com.lundi_m.spazacred_api.payment.entity.PaymentMethod;

import java.math.BigDecimal;

public record PaymentMethodTotal(
        PaymentMethod method,
        Long count,
        BigDecimal total
) {}
