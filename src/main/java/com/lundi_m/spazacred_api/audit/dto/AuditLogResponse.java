package com.lundi_m.spazacred_api.audit.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

public record AuditLogResponse(

        @Schema(description = "Unique audit log entry ID", example = "101")
        Long id,

        @Schema(
                description = "Action performed",
                example = "CUSTOMER_CREATED"
        )
        String action,

        @Schema(
                description = "Type of entity affected by the action",
                example = "CUSTOMER"
        )
        String entityType,

        @Schema(
                description = "ID of the affected entity",
                example = "25"
        )
        Long entityId,

        @Schema(
                description = "ID of the staff user who performed the action. Null for system-generated actions.",
                example = "3",
                nullable = true
        )
        Long userId,

        @Schema(
                description = "Email address of the staff user who performed the action. Null for system-generated actions.",
                example = "admin@spazacred.co.za",
                nullable = true
        )
        String userEmail,

        @Schema(
                description = "Additional action-specific details stored with the audit entry",
                example = """
                        {
                          "customerName": "Thabo Mokoena",
                          "reason": "Customer requested account closure"
                        }
                        """
        )
        Map<String, Object> details,

        @Schema(
                description = "Date and time the action was recorded",
                example = "2026-11-04T14:30:00"
        )
        LocalDateTime createdAt
) {}
