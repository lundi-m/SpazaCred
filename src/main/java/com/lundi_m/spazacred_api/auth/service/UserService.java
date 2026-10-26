package com.lundi_m.spazacred_api.auth.service;

import com.lundi_m.spazacred_api.audit.service.AuditLogService;
import com.lundi_m.spazacred_api.auth.dto.CreateUserRequest;
import com.lundi_m.spazacred_api.auth.dto.UserResponse;
import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.auth.repository.SpazaCredUserRepository;
import com.lundi_m.spazacred_api.exception.DuplicateEmailException;
import com.lundi_m.spazacred_api.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SpazaCredUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final CurrentUserProvider currentUserProvider;

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        SpazaCredUser currentUser = currentUserProvider.getCurrentUser().getUser();

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new DuplicateEmailException(request.email());
        }

        SpazaCredUser user = new SpazaCredUser(
                request.firstName(),
                request.middleName(),
                request.lastName(),
                request.email(),
                passwordEncoder.encode(request.temporaryPassword()),
                request.role()
        );
        user.setMustChangePassword(true);
        user = userRepository.save(user);

        auditLogService.record(
                currentUser,
                "USER_CREATED",
                "SpazaCredUser",
                user.getId(),
                Map.of("role", request.role().name())
        );

        return toResponse(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(Pageable pageable){
        return userRepository.findAll(pageable)
                .map(this::toResponse);
    }

    private UserResponse toResponse(SpazaCredUser user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getMiddleName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                user.isEnabled()
        );
    }
    }
