package com.lundi_m.spazacred_api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserResponse(

        @Schema(description = "Unique user identifier", example = "1")
        Long id,

        @Schema(description = "Staff member's first name", example = "Thabo")
        String firstName,

        @Schema(description = "Staff member's middle name", example = "Kabelo")
        String middleName,

        @Schema(description = "Staff member's last name", example = "Mokoena")
        String lastName,

        @Schema(description = "Staff member's email address", example = "thabo.mokoena@gmail.com")
        String email,

        @Schema(description = "Assigned staff role", example = "EMPLOYEE")
        String role,

        @Schema(description = "Whether the user account is enabled", example = "true")
        boolean enabled

) {}
