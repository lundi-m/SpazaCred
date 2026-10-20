package com.lundi_m.spazacred_api.auth.dto;

public record LoginResponse(
        String token,
        String role,
        boolean mustChangePassword
) {}
