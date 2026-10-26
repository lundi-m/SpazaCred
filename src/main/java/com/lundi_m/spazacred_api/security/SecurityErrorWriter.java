package com.lundi_m.spazacred_api.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor
public class SecurityErrorWriter {

    public static void write(HttpServletResponse response, HttpStatus status,
                             String code, String message) throws IOException {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("code", code);
        body.put("message", message);
        body.put("fieldErrors", null);

        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(response.getWriter(), body);
    }
}
