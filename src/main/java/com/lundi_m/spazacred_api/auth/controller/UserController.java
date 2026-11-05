package com.lundi_m.spazacred_api.auth.controller;

import com.lundi_m.spazacred_api.auth.dto.CreateUserRequest;
import com.lundi_m.spazacred_api.auth.dto.UserResponse;
import com.lundi_m.spazacred_api.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Create a staff user",
            description = """
                Creates a new staff user account.

                Requires ADMIN role.
                The temporary password must contain at least 8 characters.
                The user must change their temporary password when required
                by the account's password policy.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @Operation(
            summary = "List staff users",
            description = """
                Retrieves a paginated list of staff users.

                Requires ADMIN role.
                Supports pagination and sorting.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<Page<UserResponse>> listUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable));
    }
}
