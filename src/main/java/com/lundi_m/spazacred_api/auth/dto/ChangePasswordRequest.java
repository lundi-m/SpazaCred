package com.lundi_m.spazacred_api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,

        @NotBlank
        @Size(min = 8, message = "new password must be at least 8 characters")
        String newPassword
) {}
