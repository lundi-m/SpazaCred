package com.lundi_m.spazacred_api.repository;

import com.lundi_m.spazacred_api.audit.entity.AuditLog;
import com.lundi_m.spazacred_api.audit.repository.AuditLogRepository;
import com.lundi_m.spazacred_api.auth.entity.Role;
import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.lundi_m.spazacred_api.audit.repository.AuditLogSpecifications.*;
import static org.assertj.core.api.Assertions.assertThat;

class AuditLogRepositoryTest extends RepositoryTestBase {

    @Autowired private AuditLogRepository auditLogRepository;

    @Test
    void detailsSurviveTheJsonRoundTripIncludingQuotesAndUnicode() {
        String reason = "He said \"pay Friday\" – Zoë's shop";
        AuditLog saved = auditLogRepository.save(new AuditLog(staff, "ACCOUNT_SUSPENDED", "CreditAccount",
                10L, Map.of("reason", reason, "graceDays", 7)));
        entityManager.flush();
        entityManager.clear();

        AuditLog loaded = auditLogRepository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getDetails()).containsEntry("reason", reason).containsEntry("graceDays", 7);
    }

    @Test
    void aSystemActionIsStoredWithoutAUser() {
        AuditLog saved = auditLogRepository.save(new AuditLog(null, "ACCOUNT_AUTO_SUSPENDED", "CreditAccount",
                10L, Map.of("reason", "OVERDUE_DEBT")));
        entityManager.flush();
        entityManager.clear();

        assertThat(auditLogRepository.findById(saved.getId()).orElseThrow().getUser()).isNull();
    }

    @Test
    void filtersByEntityTypeAndEntityId() {
        AuditLog recorded = log(staff, "PAYMENT_RECORDED", "Payment", 1L);
        AuditLog voided = log(staff, "PAYMENT_VOIDED", "Payment", 1L);
        AuditLog otherPayment = log(staff, "PAYMENT_RECORDED", "Payment", 2L);
        AuditLog account = log(staff, "LIMIT_ADJUSTED", "CreditAccount", 1L);

        List<AuditLog> found = find(Specification.allOf(List.of(hasEntityType("Payment"), hasEntityId(1L))));

        assertThat(found).extracting(AuditLog::getId)
                .containsExactlyInAnyOrder(recorded.getId(), voided.getId())
                .doesNotContain(otherPayment.getId(), account.getId());
    }

    @Test
    void filtersByActionAndByTheUserWhoActed() {
        SpazaCredUser admin = userRepository.save(new SpazaCredUser(
                "Other", null, "Admin", "other-admin@test.local", "hash", Role.ADMIN));
        AuditLog byStaff = log(staff, "PAYMENT_VOIDED", "Payment", 1L);
        AuditLog byAdmin = log(admin, "LIMIT_ADJUSTED", "CreditAccount", 2L);

        assertThat(find(performedBy(admin.getId()))).extracting(AuditLog::getId)
                .contains(byAdmin.getId()).doesNotContain(byStaff.getId());
        assertThat(find(hasAction("PAYMENT_VOIDED"))).extracting(AuditLog::getId)
                .contains(byStaff.getId()).doesNotContain(byAdmin.getId());
    }

    @Test
    void theDateRangeCoversTheWholeEndDay() {
        AuditLog saved = log(staff, "PAYMENT_RECORDED", "Payment", 1L);
        LocalDate day = saved.getCreatedAt().toLocalDate();

        assertThat(find(createdFrom(day))).extracting(AuditLog::getId).contains(saved.getId());
        assertThat(find(createdTo(day))).extracting(AuditLog::getId).contains(saved.getId());
        assertThat(find(createdFrom(day.plusDays(1)))).extracting(AuditLog::getId).doesNotContain(saved.getId());
        assertThat(find(createdTo(day.minusDays(1)))).extracting(AuditLog::getId).doesNotContain(saved.getId());
    }

    private AuditLog log(SpazaCredUser user, String action, String entityType, Long entityId) {
        return auditLogRepository.save(new AuditLog(user, action, entityType, entityId, Map.of("reason", "test")));
    }

    private List<AuditLog> find(Specification<AuditLog> specification) {
        return auditLogRepository.findAll(specification, PageRequest.of(0, 50)).getContent();
    }
}