package com.lundi_m.spazacred_api.audit.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record AuditLogResponse(
        Long id,
        String action,
        String entityType,
        Long entityId,
        Long userId,
        String userEmail,
        Map<String, Object> details,
        LocalDateTime createdAt
) {}
