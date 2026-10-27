package com.lundi_m.spazacred_api.audit.service;

import com.lundi_m.spazacred_api.audit.entity.AuditLog;
import com.lundi_m.spazacred_api.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private AuditLogService auditLogService;

    @Test
    void recordingAnActionRequiresAUser() {
        assertThatThrownBy(() ->
                auditLogService.record(null, "CUSTOMER_CREATED", "Customer", 1L, Map.of()))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void systemActionsAreSavedWithoutAUser() {
        auditLogService.recordSystemAction("ACCOUNT_AUTO_SUSPENDED", "CreditAccount", 10L,
                Map.of("reason", "OVERDUE_DEBT"));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        assertThat(captor.getValue().getUser()).isNull();
        assertThat(captor.getValue().getAction()).isEqualTo("ACCOUNT_AUTO_SUSPENDED");
    }
}