package com.lundi_m.spazacred_api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(

        @Schema(
                description = "JWT authentication token",
                example = "eyJhbGciOiJIUzI1NiJ9..."
        )
        String token,

        @Schema(
                description = "Authenticated user's assigned role",
                example = "SHOP_OWNER"
        )
        String role,

        @Schema(
                description = "Indicates whether the user must change their password before accessing normal application endpoints",
                example = "false"
        )
        boolean mustChangePassword

) {}
