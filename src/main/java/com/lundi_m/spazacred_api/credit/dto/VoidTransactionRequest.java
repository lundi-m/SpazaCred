package com.lundi_m.spazacred_api.credit.dto;

import jakarta.validation.constraints.NotBlank;

public record VoidTransactionRequest(
        @NotBlank(message = "reason is required to void a transaction")
        String reason
) {}
