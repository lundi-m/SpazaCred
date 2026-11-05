package com.lundi_m.spazacred_api.audit.controller;

import com.lundi_m.spazacred_api.audit.dto.AuditLogResponse;
import com.lundi_m.spazacred_api.audit.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Operation(
            summary = "Search audit logs",
            description = """
                Returns a paginated list of audit log entries.
                Requires ADMIN role.

                Results can be filtered by entity type, entity ID, user ID,
                action and date range.

                By default, results are sorted by creation date in descending
                order, showing the most recent audit entries first.

                All filters are optional.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> search(

            @Parameter(description = "Filter by affected entity type",
                    example = "CUSTOMER")
            @RequestParam(required = false) String entityType,

            @Parameter(description = "Filter by affected entity ID",
                    example = "25")
            @RequestParam(required = false) Long entityId,

            @Parameter(description = "Filter by staff user ID who performed the action",
                    example = "3")
            @RequestParam(required = false) Long userId,

            @Parameter(description = "Filter by action performed",
                    example = "CUSTOMER_CREATED")
            @RequestParam(required = false) String action,

            @Parameter(description = "Filter entries from this date (inclusive)",
                    example = "2026-11-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Filter entries up to this date (inclusive)",
                    example = "2026-11-30")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {

        return ResponseEntity.ok(
                auditLogService.search(entityType, entityId, userId, action, from, to, pageable)
        );
    }
}