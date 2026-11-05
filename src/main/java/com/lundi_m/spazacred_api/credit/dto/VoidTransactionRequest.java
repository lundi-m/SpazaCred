package com.lundi_m.spazacred_api.credit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record VoidTransactionRequest(

        @NotBlank(message = "reason is required to void a transaction")
        @Schema(
                description = "Reason for voiding the purchase",
                example = "Purchase was recorded against the wrong customer."
        )
        String reason
) {}