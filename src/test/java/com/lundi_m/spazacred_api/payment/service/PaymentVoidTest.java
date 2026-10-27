package com.lundi_m.spazacred_api.payment.service;

import com.lundi_m.spazacred_api.audit.service.AuditLogService;
import com.lundi_m.spazacred_api.auth.entity.Role;
import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.credit.entity.AccountStatus;
import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import com.lundi_m.spazacred_api.credit.repository.CreditAccountRepository;
import com.lundi_m.spazacred_api.credit.repository.CreditTransactionRepository;
import com.lundi_m.spazacred_api.credit.service.OverdueAccountService;
import com.lundi_m.spazacred_api.customer.entity.Customer;
import com.lundi_m.spazacred_api.payment.repository.PaymentAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Captor;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentVoidTest {
    private static final Long ACCOUNT_ID = 10L;
    private static final LocalDate CUTOFF = LocalDate.of(2026, 10, 8);

    @Mock private CreditAccountRepository creditAccountRepository;
    @Mock private CreditTransactionRepository creditTransactionRepository;
    @Mock private PaymentAllocationRepository paymentAllocationRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private Clock clock;

    @Captor private ArgumentCaptor<Map<String, Object>> detailsCaptor;

    @InjectMocks private OverdueAccountService overdueAccountService;

    private CreditAccount account;
    private CreditTransaction overdueDebt;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(overdueAccountService, "graceDays", 7);

        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-10-15T08:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Africa/Johannesburg"));

        Customer customer = new Customer("Thabo", null, "Mokoena", "0821234567",
                null, "9001015009087", "12 Main Road");
        customer.setId(1L);
        account = new CreditAccount(customer, new BigDecimal("2000.00"));
        account.setId(ACCOUNT_ID);

        SpazaCredUser staff = new SpazaCredUser("Sipho", null, "Dlamini", "sipho@shop.test", "hash", Role.EMPLOYEE);
        staff.setId(7L);

        overdueDebt = new CreditTransaction("CRT-2026-000001", account, staff,
                new BigDecimal("500.00"), "groceries", LocalDate.of(2026, 10, 1));
        overdueDebt.setId(5L);

        lenient().when(creditAccountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.of(account));
    }

    @Test
    void suspendsAnActiveAccountThatIsStillOverdue() {
        when(creditTransactionRepository.findOverdueByAccountId(ACCOUNT_ID, CUTOFF))
                .thenReturn(List.of(overdueDebt));
        when(paymentAllocationRepository.sumActiveAllocationsByTransactionId(5L))
                .thenReturn(new BigDecimal("100.00"));

        boolean suspended = overdueAccountService.suspendIfStillOverdue(ACCOUNT_ID);

        assertThat(suspended).isTrue();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.SUSPENDED);

        verify(auditLogService).recordSystemAction(eq("ACCOUNT_AUTO_SUSPENDED"), eq("CreditAccount"),
                eq(ACCOUNT_ID), detailsCaptor.capture());
        Map<String, Object> details = detailsCaptor.getValue();
        assertThat(details.get("reason")).isEqualTo("OVERDUE_DEBT");
        assertThat(details.get("graceDays")).isEqualTo(7);
        assertThat(details.get("overdueTransactions")).isEqualTo(1);
        assertThat((BigDecimal) details.get("overdueAmount")).isEqualByComparingTo("400.00");
        assertThat(details.get("oldestOverdueDays")).isEqualTo(14L);
    }

    @Test
    void theCutoffIsTodayMinusTheGracePeriod() {
        overdueAccountService.suspendIfStillOverdue(ACCOUNT_ID);

        verify(creditTransactionRepository).findOverdueByAccountId(ACCOUNT_ID, CUTOFF);
    }

    @Test
    void doesNothingIfTheDebtWasPaidAfterTheJobFoundTheAccount() {
        when(creditTransactionRepository.findOverdueByAccountId(ACCOUNT_ID, CUTOFF)).thenReturn(List.of());

        boolean suspended = overdueAccountService.suspendIfStillOverdue(ACCOUNT_ID);

        assertThat(suspended).isFalse();
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void skipsAnAccountThatIsAlreadySuspended() {
        account.setStatus(AccountStatus.SUSPENDED);

        boolean suspended = overdueAccountService.suspendIfStillOverdue(ACCOUNT_ID);

        assertThat(suspended).isFalse();
        verifyNoInteractions(creditTransactionRepository, auditLogService);
    }

    @Test
    void skipsAnAccountThatNoLongerExists() {
        when(creditAccountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.empty());

        assertThat(overdueAccountService.suspendIfStillOverdue(ACCOUNT_ID)).isFalse();

        verifyNoInteractions(auditLogService);
    }

    @Test
    void findsCandidatesUsingTheSameCutoff() {
        when(creditTransactionRepository.findActiveAccountIdsWithDebtDueBefore(CUTOFF))
                .thenReturn(List.of(10L, 11L));

        assertThat(overdueAccountService.findAccountIdsToSuspend()).containsExactly(10L, 11L);
    }
}

