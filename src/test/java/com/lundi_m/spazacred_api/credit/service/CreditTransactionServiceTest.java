package com.lundi_m.spazacred_api.credit.service;

import com.lundi_m.spazacred_api.audit.service.AuditLogService;
import com.lundi_m.spazacred_api.auth.entity.Role;
import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.credit.dto.CreatePurchaseRequest;
import com.lundi_m.spazacred_api.credit.entity.AccountStatus;
import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import com.lundi_m.spazacred_api.credit.repository.CreditAccountRepository;
import com.lundi_m.spazacred_api.credit.repository.CreditTransactionRepository;
import com.lundi_m.spazacred_api.customer.entity.Customer;
import com.lundi_m.spazacred_api.exception.CreditLimitExceededException;
import com.lundi_m.spazacred_api.exception.CustomerInactiveException;
import com.lundi_m.spazacred_api.exception.InvalidAccountStatusTransitionException;
import com.lundi_m.spazacred_api.security.CurrentUserProvider;
import com.lundi_m.spazacred_api.security.SpazaCredUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreditTransactionServiceTest {

    private static final Long CUSTOMER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;

    @Mock private CreditTransactionRepository creditTransactionRepository;
    @Mock private CreditAccountRepository creditAccountRepository;
    @Mock private CreditAccountService creditAccountService;
    @Mock private AuditLogService auditLogService;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private Clock clock;

    @InjectMocks private CreditTransactionService creditTransactionService;

    private Customer customer;
    private CreditAccount account;

    @BeforeEach
    void setUp() {
        customer = new Customer("Thabo", null, "Mokoena", "0821234567",
                null, "9001015009087", "12 Main Road");
        customer.setId(CUSTOMER_ID);

        account = new CreditAccount(customer, money("2000.00"));
        account.setId(ACCOUNT_ID);

        SpazaCredUser staff =
                new SpazaCredUser("Sipho", null, "Dlamini", "sipho@shop.test", "hash", Role.EMPLOYEE);
        staff.setId(7L);

        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-10-15T08:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Africa/Johannesburg"));
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(new SpazaCredUserDetails(staff));
        lenient().when(creditAccountRepository.findByCustomerIdForUpdate(CUSTOMER_ID))
                .thenReturn(Optional.of(account));
        lenient().when(creditTransactionRepository.nextReferenceSequence()).thenReturn(42L);
        lenient().when(creditTransactionRepository.save(any(CreditTransaction.class)))
                .thenAnswer(invocation -> {
                    CreditTransaction transaction = invocation.getArgument(0);
                    transaction.setId(200L);
                    return transaction;
                });
    }

    @Test
    void acceptsAPurchaseThatReachesTheLimitExactly() {
        givenBalance("1500.00");

        buy("500.00", null);

        assertThat(savedTransaction().getAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void rejectsAPurchaseThatWouldExceedTheLimit() {
        givenBalance("1500.00");

        assertThatThrownBy(() -> buy("600.00", null))
                .isInstanceOf(CreditLimitExceededException.class);

        verify(creditTransactionRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void rejectsAPurchaseOneCentOverTheLimit() {
        givenBalance("1500.00");

        assertThatThrownBy(() -> buy("500.01", null))
                .isInstanceOf(CreditLimitExceededException.class);
    }

    @Test
    void defaultsTheDueDateToThirtyDaysFromToday() {
        givenBalance("0.00");

        buy("100.00", null);

        assertThat(savedTransaction().getDueDate()).isEqualTo(LocalDate.of(2026, 11, 14));
    }

    @Test
    void usesTheDueDateTheStaffMemberChose() {
        givenBalance("0.00");

        buy("100.00", LocalDate.of(2026, 10, 22));

        assertThat(savedTransaction().getDueDate()).isEqualTo(LocalDate.of(2026, 10, 22));
    }

    @Test
    void buildsTheReferenceNumberFromTheClockYearAndTheSequence() {
        givenBalance("0.00");

        buy("100.00", null);

        assertThat(savedTransaction().getReferenceNumber()).isEqualTo("CRT-2026-000042");
    }

    @Test
    void rejectsPurchasesOnASuspendedAccount() {
        account.setStatus(AccountStatus.SUSPENDED);

        assertThatThrownBy(() -> buy("100.00", null))
                .isInstanceOf(InvalidAccountStatusTransitionException.class);

        verify(creditTransactionRepository, never()).save(any());
    }

    @Test
    void rejectsPurchasesForADeactivatedCustomer() {
        customer.setActive(false);

        assertThatThrownBy(() -> buy("100.00", null))
                .isInstanceOf(CustomerInactiveException.class);

        verify(creditTransactionRepository, never()).save(any());
    }


    private void buy(String amount, LocalDate dueDate) {
        creditTransactionService.createPurchase(CUSTOMER_ID,
                new CreatePurchaseRequest(money(amount), "groceries", dueDate));
    }

    private CreditTransaction savedTransaction() {
        ArgumentCaptor<CreditTransaction> captor = ArgumentCaptor.forClass(CreditTransaction.class);
        verify(creditTransactionRepository).save(captor.capture());
        return captor.getValue();
    }

    private void givenBalance(String balance) {
        when(creditAccountService.getOutstandingBalance(ACCOUNT_ID)).thenReturn(money(balance));
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
