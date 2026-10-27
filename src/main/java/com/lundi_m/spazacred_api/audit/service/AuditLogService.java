package com.lundi_m.spazacred_api.audit.service;

import com.lundi_m.spazacred_api.audit.dto.AuditLogResponse;
import com.lundi_m.spazacred_api.audit.entity.AuditLog;
import com.lundi_m.spazacred_api.audit.repository.AuditLogRepository;
import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.exception.InvalidDateRangeException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.lundi_m.spazacred_api.audit.repository.AuditLogSpecifications.*;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(SpazaCredUser user, String action, String entityType,
                       Long entityId, Map<String, Object> details) {
        Objects.requireNonNull(user, "A user is required; use recordSystemAction for automated actions");
        auditLogRepository.save(new AuditLog(user, action, entityType, entityId, details));
    }

    @Transactional
    public void recordSystemAction(String action, String entityType,
                                   Long entityId, Map<String, Object> details) {
        auditLogRepository.save(new AuditLog(null, action, entityType, entityId, details));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(String entityType, Long entityId, Long userId,
                                         String action, LocalDate from, LocalDate to,
                                         Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidDateRangeException();
        }

        List<Specification<AuditLog>> filters = new ArrayList<>();
        if (StringUtils.hasText(entityType)) filters.add(hasEntityType(entityType));
        if (entityId != null) filters.add(hasEntityId(entityId));
        if (userId != null) filters.add(performedBy(userId));
        if (StringUtils.hasText(action)) filters.add(hasAction(action));
        if (from != null) filters.add(createdFrom(from));
        if (to != null) filters.add(createdTo(to));

        return auditLogRepository.findAll(Specification.allOf(filters), pageable)
                .map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        SpazaCredUser user = log.getUser();
        return new AuditLogResponse(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                user != null ? user.getId() : null,
                user != null ? user.getEmail() : null,
                log.getDetails(),
                log.getCreatedAt()
        );
    }
}