package com.lundi_m.spazacred_api.auth.dto;

import com.lundi_m.spazacred_api.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank String firstName,
        String middleName,
        @NotBlank
        String lastName,
        @Email @NotBlank
        String email,
        @NotBlank @Size(min = 8, message = "password must be at least 8 characters")
        String temporaryPassword,
        @NotNull
        Role role

) {}
