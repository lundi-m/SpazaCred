package com.lundi_m.spazacred_api.auth.service;

import com.lundi_m.spazacred_api.auth.dto.ChangePasswordRequest;
import com.lundi_m.spazacred_api.auth.dto.LoginRequest;
import com.lundi_m.spazacred_api.auth.dto.LoginResponse;
import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.auth.repository.SpazaCredUserRepository;
import com.lundi_m.spazacred_api.security.CurrentUserProvider;
import com.lundi_m.spazacred_api.security.JwtService;
import com.lundi_m.spazacred_api.security.SpazaCredUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SpazaCredUserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUserProvider;

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

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = currentUserProvider.getCurrentUser().getUser().getId();
        SpazaCredUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User no longer exists"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current Password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
    }
}
