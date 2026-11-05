package com.lundi_m.spazacred_api.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record VoidPaymentRequest(

        @Schema(
                description = "Reason for voiding the payment",
                example = "Payment was recorded with an incorrect amount."
        )
        @NotBlank(message = "a reason is required to void a payment")
        String reason
) {}
