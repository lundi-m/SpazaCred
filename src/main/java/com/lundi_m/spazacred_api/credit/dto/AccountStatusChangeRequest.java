package com.lundi_m.spazacred_api.credit.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AccountStatusChangeRequest(

        @NotBlank(message = "a reason is required for this action")
        @Schema(
                description = "Reason for the account status change",
                example = "Account suspended due to repeated overdue payments."
        )
        String reason

) {}
