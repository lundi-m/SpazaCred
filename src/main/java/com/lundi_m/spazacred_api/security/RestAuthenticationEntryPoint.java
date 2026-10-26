package com.lundi_m.spazacred_api.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String AUTH_ERROR_ATTRIBUTE = "spazacred.auth.error";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object reason = request.getAttribute(AUTH_ERROR_ATTRIBUTE);

        if ("TOKEN_EXPIRED".equals(reason)) {
            SecurityErrorWriter.write(response, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED",
                    "Your session has expired. Please log in again");
        } else if ("TOKEN_INVALID".equals(reason)) {
            SecurityErrorWriter.write(response, HttpStatus.UNAUTHORIZED, "TOKEN_INVALID",
                    "Your login token is not valid. Please log in again");
        } else {
            SecurityErrorWriter.write(response, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                    "Authentication is required. Send your login token in the Authorization header");
        }
    }
}
