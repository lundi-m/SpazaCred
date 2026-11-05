package com.lundi_m.spazacred_api.auth.dto;

import com.lundi_m.spazacred_api.auth.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(

        @NotBlank
        @Schema(
                description = "Staff member's first name",
                example = "Thabo"
        )
        String firstName,

        @Schema(
                description = "Staff member's middle name (optional)",
                example = "Kabelo"
        )
        String middleName,

        @NotBlank
        @Schema(
                description = "Staff member's last name",
                example = "Mokoena"
        )
        String lastName,

        @Email
        @NotBlank
        @Schema(
                description = "Unique email address for the staff account",
                example = "thabo.mokoena@gmail.com"
        )
        String email,

        @NotBlank
        @Size(min = 8, message = "password must be at least 8 characters")
        @Schema(
                description = "Temporary password (minimum 8 characters)",
                example = "TempPass123!"
        )
        String temporaryPassword,

        @NotNull
        @Schema(
                description = "Role assigned to the staff member",
                example = "EMPLOYEE"
        )
        Role role

) {}
