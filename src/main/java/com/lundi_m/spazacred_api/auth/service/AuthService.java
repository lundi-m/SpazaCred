package com.lundi_m.spazacred_api.auth.service;

import com.lundi_m.spazacred_api.auth.dto.LoginRequest;
import com.lundi_m.spazacred_api.auth.dto.LoginResponse;
import com.lundi_m.spazacred_api.security.JwtService;
import com.lundi_m.spazacred_api.security.SpazaCredUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        SpazaCredUserDetails userDetails = (SpazaCredUserDetails) authentication.getPrincipal();
        assert userDetails != null;
        String token = jwtService.generateToken(userDetails);

        return new LoginResponse(
                token,
                userDetails.getUser().getRole().name(),
                userDetails.getUser().isMustChangePassword()
        );
    }
}
