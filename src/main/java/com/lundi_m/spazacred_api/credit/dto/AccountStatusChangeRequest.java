package com.lundi_m.spazacred_api.credit.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountStatusChangeRequest(
        @NotBlank(message = "a reason is required for this action")
        String reason
) {}
