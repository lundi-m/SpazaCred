package com.lundi_m.spazacred_api.credit.service;

import com.lundi_m.spazacred_api.audit.service.AuditLogService;
import com.lundi_m.spazacred_api.auth.entity.Role;
import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.credit.dto.AccountStatusChangeRequest;
import com.lundi_m.spazacred_api.credit.dto.AdjustLimitRequest;
import com.lundi_m.spazacred_api.credit.dto.CreditAccountResponse;
import com.lundi_m.spazacred_api.credit.entity.AccountStatus;
import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.repository.CreditAccountRepository;
import com.lundi_m.spazacred_api.credit.repository.CreditTransactionRepository;
import com.lundi_m.spazacred_api.customer.entity.Customer;
import com.lundi_m.spazacred_api.exception.CreditAccountNotFoundException;
import com.lundi_m.spazacred_api.exception.InvalidAccountStatusTransitionException;
import com.lundi_m.spazacred_api.payment.repository.PaymentAllocationRepository;
import com.lundi_m.spazacred_api.security.CurrentUserProvider;
import com.lundi_m.spazacred_api.security.SpazaCredUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreditAccountServiceTest {

    private static final Long CUSTOMER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;

    @Mock private CreditAccountRepository creditAccountRepository;
    @Mock private CreditTransactionRepository creditTransactionRepository;
    @Mock private PaymentAllocationRepository paymentAllocationRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private CurrentUserProvider currentUserProvider;

    @InjectMocks private CreditAccountService creditAccountService;

    private CreditAccount account;
    private SpazaCredUser staff;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer("Thabo", null, "Mokoena", "0821234567",
                null, "9001015009087", "12 Main Road");
        customer.setId(CUSTOMER_ID);

        account = new CreditAccount(customer, money("2000.00"));
        account.setId(ACCOUNT_ID);

        staff = new SpazaCredUser("Nomsa", null, "Khumalo", "nomsa@shop.test", "hash", Role.SHOP_OWNER);
        staff.setId(7L);

        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(new SpazaCredUserDetails(staff));
        lenient().when(creditAccountRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.of(account));
        lenient().when(creditTransactionRepository.sumActiveAmountsByAccountId(ACCOUNT_ID))
                .thenReturn(money("0.00"));
        lenient().when(paymentAllocationRepository.sumActiveAllocationsByAccountId(ACCOUNT_ID))
                .thenReturn(money("0.00"));
    }

    @Test
    void outstandingBalanceIsPurchasesMinusPayments() {
        givenBalance("1500.00", "800.00");

        assertThat(creditAccountService.getOutstandingBalance(ACCOUNT_ID)).isEqualByComparingTo("700.00");
    }

    @Test
    void availableBalanceIsTheLimitMinusWhatIsOwed() {
        givenBalance("1500.00", "800.00");

        CreditAccountResponse response = creditAccountService.getAccount(CUSTOMER_ID);

        assertThat(response.outstandingBalance()).isEqualByComparingTo("700.00");
        assertThat(response.availableBalance()).isEqualByComparingTo("1300.00");
    }

    @Test
    void anUnknownCustomerHasNoAccount() {
        when(creditAccountRepository.findByCustomerId(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> creditAccountService.getAccount(CUSTOMER_ID))
                .isInstanceOf(CreditAccountNotFoundException.class);
    }

    @Test
    void adjustingTheLimitChangesItAndRecordsOldNewAndReason() {
        CreditAccountResponse response = creditAccountService.adjustCreditLimit(CUSTOMER_ID,
                new AdjustLimitRequest(money("3000.00"), "Paid on time for six months"));

        assertThat(response.creditLimit()).isEqualByComparingTo("3000.00");

        Map<String, Object> expectedDetails = Map.of(
                "oldLimit", money("2000.00"),
                "newLimit", money("3000.00"),
                "reason", "Paid on time for six months");
        verify(auditLogService).record(eq(staff), eq("LIMIT_ADJUSTED"), eq("CreditAccount"),
                eq(ACCOUNT_ID), eq(expectedDetails));
    }

    @Test
    void suspendsAnActiveAccountAndRecordsTheReason() {
        CreditAccountResponse response = creditAccountService.suspendAccount(CUSTOMER_ID, reason("Missed payments"));

        assertThat(response.status()).isEqualTo("SUSPENDED");
        verify(auditLogService).record(eq(staff), eq("ACCOUNT_SUSPENDED"), eq("CreditAccount"),
                eq(ACCOUNT_ID), eq(reasonDetails("Missed payments")));
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"SUSPENDED", "CLOSED"})
    void onlyAnActiveAccountCanBeSuspended(AccountStatus status) {
        account.setStatus(status);

        assertThatThrownBy(() -> creditAccountService.suspendAccount(CUSTOMER_ID, reason("x")))
                .isInstanceOf(InvalidAccountStatusTransitionException.class);

        assertThat(account.getStatus()).isEqualTo(status);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void reactivatesASuspendedAccount() {
        account.setStatus(AccountStatus.SUSPENDED);

        CreditAccountResponse response = creditAccountService.reactivateAccount(CUSTOMER_ID, reason("Paid up"));

        assertThat(response.status()).isEqualTo("ACTIVE");
        verify(auditLogService).record(eq(staff), eq("ACCOUNT_REACTIVATED"), eq("CreditAccount"),
                eq(ACCOUNT_ID), eq(reasonDetails("Paid up")));
    }

    @ParameterizedTest
    @EnumSource(value = AccountStatus.class, names = {"ACTIVE", "CLOSED"})
    void onlyASuspendedAccountCanBeReactivated(AccountStatus status) {
        account.setStatus(status);

        assertThatThrownBy(() -> creditAccountService.reactivateAccount(CUSTOMER_ID, reason("x")))
                .isInstanceOf(InvalidAccountStatusTransitionException.class);

        assertThat(account.getStatus()).isEqualTo(status);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void closesAnAccountThatOwesNothing() {
        CreditAccountResponse response = creditAccountService.closeAccount(CUSTOMER_ID, reason("Customer moved"));

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        assertThat(response.status()).isEqualTo("CLOSED");
        verify(auditLogService).record(eq(staff), eq("ACCOUNT_CLOSED"), eq("CreditAccount"),
                eq(ACCOUNT_ID), eq(reasonDetails("Customer moved")));
    }

    @Test
    void aSuspendedAccountCanBeClosedToo() {
        account.setStatus(AccountStatus.SUSPENDED);

        creditAccountService.closeAccount(CUSTOMER_ID, reason("Customer moved"));

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
    }

    @Test
    void cannotCloseWhileMoneyIsOwed() {
        givenBalance("500.00", "0.00");

        assertThatThrownBy(() -> creditAccountService.closeAccount(CUSTOMER_ID, reason("x")))
                .isInstanceOf(InvalidAccountStatusTransitionException.class);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void cannotCloseAnAccountTwice() {
        account.setStatus(AccountStatus.CLOSED);

        assertThatThrownBy(() -> creditAccountService.closeAccount(CUSTOMER_ID, reason("x")))
                .isInstanceOf(InvalidAccountStatusTransitionException.class);
    }


    private void givenBalance(String purchases, String payments) {
        when(creditTransactionRepository.sumActiveAmountsByAccountId(ACCOUNT_ID)).thenReturn(money(purchases));
        when(paymentAllocationRepository.sumActiveAllocationsByAccountId(ACCOUNT_ID)).thenReturn(money(payments));
    }

    private AccountStatusChangeRequest reason(String text) {
        return new AccountStatusChangeRequest(text);
    }

    private Map<String, Object> reasonDetails(String text) {
        return Map.of("reason", text);
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
