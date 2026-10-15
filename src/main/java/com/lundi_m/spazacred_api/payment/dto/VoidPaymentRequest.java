package com.lundi_m.spazacred_api.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record VoidPaymentRequest(
        @NotBlank(message = "a reason is required to void a payment")
        String reason
) {}
