package com.lundi_m.spazacred_api.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class CurrentUserProvider {

    public SpazaCredUserDetails getCurrentUser() {
        return (SpazaCredUserDetails) Objects.requireNonNull(SecurityContextHolder.getContext()
                .getAuthentication()).getPrincipal();
    }
}
