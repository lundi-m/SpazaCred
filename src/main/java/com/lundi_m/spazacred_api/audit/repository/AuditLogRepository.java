package com.lundi_m.spazacred_api.audit.repository;

import com.lundi_m.spazacred_api.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
