package com.lundi_m.spazacred_api.auth.dto;

public record UserResponse(
        Long id,
        String firstName,
        String middleName,
        String lastName,
        String email,
        String role,
        boolean enabled
) {}
