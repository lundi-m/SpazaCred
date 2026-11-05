package com.lundi_m.spazacred_api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank
        @Schema(
                description = "User's current password",
                example = "ChangeMe123!"
        )
        String currentPassword,

        @NotBlank
        @Size(
                min = 8,
                message = "new password must be at least 8 characters"
        )
        @Schema(
                description = "New password (minimum 8 characters)",
                example = "MyNewPassword123!"
        )
        String newPassword

) {}
