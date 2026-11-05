package com.lundi_m.spazacred_api.auth.controller;

import com.lundi_m.spazacred_api.auth.dto.ChangePasswordRequest;
import com.lundi_m.spazacred_api.auth.dto.LoginRequest;
import com.lundi_m.spazacred_api.auth.dto.LoginResponse;
import com.lundi_m.spazacred_api.auth.service.AuthService;
 import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Authenticate a user",
            description = """
                Authenticates a user using their email address and password.

                This endpoint is public and does not require a JWT.
                Returns a JWT authentication token and the user's role.

                If mustChangePassword is true, the user must change
                their password before accessing normal application endpoints.
                """
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }


    @Operation(
            summary = "Change user password",
            description = """
                Changes the authenticated user's password.

                Requires a valid JWT.
                The current password must be provided for verification.
                The new password must contain at least 8 characters.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.noContent().build();
    }
}
