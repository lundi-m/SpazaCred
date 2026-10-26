package com.lundi_m.spazacred_api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        boolean passwordChangeRequired = authentication != null
                && authentication.getPrincipal() instanceof SpazaCredUserDetails details
                && details.getUser().isMustChangePassword();

        if (passwordChangeRequired) {
            SecurityErrorWriter.write(response, HttpStatus.FORBIDDEN, "PASSWORD_CHANGE_REQUIRED",
                    "You must change your temporary password before using the API. Use POST /api/v1/auth/change-password");
        } else {
            SecurityErrorWriter.write(response, HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "You do not have permission to perform this action");
        }
    }
}
