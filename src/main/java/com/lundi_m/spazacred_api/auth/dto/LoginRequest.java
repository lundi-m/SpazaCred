package com.lundi_m.spazacred_api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @Email
        @NotBlank
        @Schema(
                description = "Registered user email address",
                example = "admin@spazacred.co.za"
        )
        String email,

        @NotBlank
        @Schema(
                description = "User account password",
                example = "ChangeMe123!"
        )
        String password

) {}