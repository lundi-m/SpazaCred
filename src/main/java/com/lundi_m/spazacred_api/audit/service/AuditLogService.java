package com.lundi_m.spazacred_api.audit.service;

import com.lundi_m.spazacred_api.audit.entity.AuditLog;
import com.lundi_m.spazacred_api.audit.repository.AuditLogRepository;
import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(SpazaCredUser user, String action, String entityType, Long entityId, Map<String, Object> detailsJson) {
        AuditLog log = new AuditLog(user, action, entityType, entityId, detailsJson);
        auditLogRepository.save(log);
    }
}
