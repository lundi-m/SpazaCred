package com.lundi_m.spazacred_api.audit.repository;

import com.lundi_m.spazacred_api.audit.entity.AuditLog;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

@NoArgsConstructor
public final class AuditLogSpecifications {

    public static Specification<AuditLog> hasEntityType(String entityType) {
        return (root, query, cb) -> cb.equal(root.get("entityType"), entityType);
    }

    public static Specification<AuditLog> hasEntityId(Long entityId) {
        return (root, query, cb) -> cb.equal(root.get("entityId"), entityId);
    }

    public static Specification<AuditLog> performedBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<AuditLog> hasAction(String action) {
        return (root, query, cb) -> cb.equal(root.get("action"), action);
    }

    public static Specification<AuditLog> createdFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(
                root.<LocalDateTime>get("createdAt"), from.atStartOfDay());
    }

    public static Specification<AuditLog> createdTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThan(
                root.<LocalDateTime>get("createdAt"), to.plusDays(1).atStartOfDay());
    }
}
