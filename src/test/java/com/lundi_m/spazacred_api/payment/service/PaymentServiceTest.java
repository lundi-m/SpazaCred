package com.lundi_m.spazacred_api.payment.service;

import com.lundi_m.spazacred_api.audit.service.AuditLogService;
import com.lundi_m.spazacred_api.auth.entity.Role;
import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import com.lundi_m.spazacred_api.credit.entity.TransactionStatus;
import com.lundi_m.spazacred_api.credit.repository.CreditAccountRepository;
import com.lundi_m.spazacred_api.credit.repository.CreditTransactionRepository;
import com.lundi_m.spazacred_api.credit.service.CreditAccountService;
import com.lundi_m.spazacred_api.customer.entity.Customer;
import com.lundi_m.spazacred_api.customer.repository.CustomerRepository;
import com.lundi_m.spazacred_api.exception.CreditAccountNotFoundException;
import com.lundi_m.spazacred_api.exception.CustomerNotFoundException;
import com.lundi_m.spazacred_api.exception.PaymentExceedsOutstandingBalanceException;
import com.lundi_m.spazacred_api.payment.dto.PaymentResponse;
import com.lundi_m.spazacred_api.payment.dto.RecordPaymentRequest;
import com.lundi_m.spazacred_api.payment.entity.Payment;
import com.lundi_m.spazacred_api.payment.entity.PaymentAllocation;
import com.lundi_m.spazacred_api.payment.entity.PaymentMethod;
import com.lundi_m.spazacred_api.payment.repository.PaymentAllocationRepository;
import com.lundi_m.spazacred_api.payment.repository.PaymentRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    private static final Long CUSTOMER_ID = 1L;
    private static final Long ACCOUNT_ID = 10L;

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentAllocationRepository paymentAllocationRepository;
    @Mock private CreditTransactionRepository creditTransactionRepository;
    @Mock private CreditAccountRepository creditAccountRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private CreditAccountService creditAccountService;
    @Mock private AuditLogService auditLogService;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private Clock clock;

    @InjectMocks private PaymentService paymentService;

    private CreditAccount account;
    private SpazaCredUser staff;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer("Thabo", null, "Mokoena", "0821234567",
                null, "9001015009087", "12 Main Road");
        customer.setId(CUSTOMER_ID);

        account = new CreditAccount(customer, money("5000.00"));
        account.setId(ACCOUNT_ID);

        staff = new SpazaCredUser("Sipho", null, "Dlamini", "sipho@shop.test", "hash", Role.EMPLOYEE);
        staff.setId(7L);

        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-10-15T08:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Africa/Johannesburg"));
        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(new SpazaCredUserDetails(staff));
        lenient().when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
        lenient().when(creditAccountRepository.findByCustomerIdForUpdate(CUSTOMER_ID))
                .thenReturn(Optional.of(account));
        lenient().when(paymentRepository.nextReferenceSequence()).thenReturn(1L);
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(100L);
            return payment;
        });
        lenient().when(paymentAllocationRepository.save(any(PaymentAllocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void allocatesToTheOldestDebtFirst() {
        CreditTransaction debtA = debt(1L, "CRT-2026-000001", "500.00", LocalDate.of(2026, 10, 25));
        CreditTransaction debtB = debt(2L, "CRT-2026-000002", "700.00", LocalDate.of(2026, 11, 4));
        CreditTransaction debtC = debt(3L, "CRT-2026-000003", "300.00", LocalDate.of(2026, 11, 14));
        givenOutstandingDebts(debtA, debtB, debtC);
        givenBalance("1500.00");

        PaymentResponse response = pay("800.00");

        assertThat(response.allocations()).hasSize(2);
        assertThat(response.allocations().get(0).creditTransactionId()).isEqualTo(1L);
        assertThat(response.allocations().get(0).amountAllocated()).isEqualByComparingTo("500.00");
        assertThat(response.allocations().get(1).creditTransactionId()).isEqualTo(2L);
        assertThat(response.allocations().get(1).amountAllocated()).isEqualByComparingTo("300.00");
    }

    @Test
    void theAllocationsSavedAddUpToThePayment() {
        CreditTransaction debtA = debt(1L, "CRT-2026-000001", "500.00", LocalDate.of(2026, 10, 25));
        CreditTransaction debtB = debt(2L, "CRT-2026-000002", "700.00", LocalDate.of(2026, 11, 4));
        CreditTransaction debtC = debt(3L, "CRT-2026-000003", "300.00", LocalDate.of(2026, 11, 14));
        givenOutstandingDebts(debtA, debtB, debtC);
        givenBalance("1500.00");

        pay("800.00");

        ArgumentCaptor<PaymentAllocation> captor = ArgumentCaptor.forClass(PaymentAllocation.class);
        verify(paymentAllocationRepository, times(2)).save(captor.capture());

        BigDecimal totalAllocated = captor.getAllValues().stream()
                .map(PaymentAllocation::getAmountAllocated)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalAllocated).isEqualByComparingTo("800.00");
    }

    @Test
    void onlyAllocatesWhatIsStillOwedOnAPartiallyPaidDebt() {
        CreditTransaction debtA = debt(1L, "CRT-2026-000001", "500.00", LocalDate.of(2026, 10, 25));
        CreditTransaction debtB = debt(2L, "CRT-2026-000002", "700.00", LocalDate.of(2026, 11, 4));
        givenAlreadyPaid(debtA, "200.00");
        givenOutstandingDebts(debtA, debtB);
        givenBalance("1000.00");

        PaymentResponse response = pay("600.00");

        assertThat(response.allocations()).hasSize(2);
        assertThat(response.allocations().get(0).creditTransactionId()).isEqualTo(1L);
        assertThat(response.allocations().get(0).amountAllocated()).isEqualByComparingTo("300.00");
        assertThat(response.allocations().get(1).creditTransactionId()).isEqualTo(2L);
        assertThat(response.allocations().get(1).amountAllocated()).isEqualByComparingTo("300.00");
    }

    @Test
    void skipsADebtThatIsAlreadyFullyPaid() {
        CreditTransaction debtA = debt(1L, "CRT-2026-000001", "500.00", LocalDate.of(2026, 10, 25));
        CreditTransaction debtB = debt(2L, "CRT-2026-000002", "700.00", LocalDate.of(2026, 11, 4));
        givenAlreadyPaid(debtA, "500.00");
        givenOutstandingDebts(debtA, debtB);
        givenBalance("700.00");

        PaymentResponse response = pay("300.00");

        assertThat(response.allocations()).hasSize(1);
        assertThat(response.allocations().getFirst().creditTransactionId()).isEqualTo(2L);
        assertThat(response.allocations().getFirst().amountAllocated()).isEqualByComparingTo("300.00");
    }

    @Test
    void payingTheWholeBalanceClearsEveryDebt() {
        CreditTransaction debtA = debt(1L, "CRT-2026-000001", "500.00", LocalDate.of(2026, 10, 25));
        CreditTransaction debtB = debt(2L, "CRT-2026-000002", "700.00", LocalDate.of(2026, 11, 4));
        CreditTransaction debtC = debt(3L, "CRT-2026-000003", "300.00", LocalDate.of(2026, 11, 14));
        givenOutstandingDebts(debtA, debtB, debtC);
        givenBalance("1500.00");

        PaymentResponse response = pay("1500.00");

        assertThat(response.allocations()).hasSize(3);
        assertThat(response.allocations().get(0).amountAllocated()).isEqualByComparingTo("500.00");
        assertThat(response.allocations().get(1).amountAllocated()).isEqualByComparingTo("700.00");
        assertThat(response.allocations().get(2).amountAllocated()).isEqualByComparingTo("300.00");
    }

    @Test
    void rejectsAPaymentOneCentAboveTheBalance() {
        givenBalance("1500.00");

        assertThatThrownBy(() -> pay("1500.01"))
                .isInstanceOf(PaymentExceedsOutstandingBalanceException.class);

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(paymentAllocationRepository, auditLogService);
    }

    @Test
    void rejectsAnUnknownCustomer() {
        when(customerRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pay("100.00"))
                .isInstanceOf(CustomerNotFoundException.class);

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void rejectsACustomerWithoutACreditAccount() {
        when(creditAccountRepository.findByCustomerIdForUpdate(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pay("100.00"))
                .isInstanceOf(CreditAccountNotFoundException.class);

        verifyNoInteractions(paymentRepository);
    }


    private PaymentResponse pay(String amount) {
        return paymentService.recordPayment(CUSTOMER_ID,
                new RecordPaymentRequest(money(amount), PaymentMethod.CASH));
    }

    private CreditTransaction debt(Long id, String reference, String amount, LocalDate dueDate) {
        CreditTransaction transaction =
                new CreditTransaction(reference, account, staff, money(amount), "groceries", dueDate);
        transaction.setId(id);
        lenient().when(paymentAllocationRepository.sumActiveAllocationsByTransactionId(id))
                .thenReturn(money("0.00"));
        return transaction;
    }

    private void givenAlreadyPaid(CreditTransaction debt, String paid) {
        when(paymentAllocationRepository.sumActiveAllocationsByTransactionId(debt.getId()))
                .thenReturn(money(paid));
    }

    private void givenOutstandingDebts(CreditTransaction... debts) {
        when(creditTransactionRepository
                .findByCreditAccount_IdAndStatusOrderByDueDateAscCreatedAtAscIdAsc(
                        ACCOUNT_ID, TransactionStatus.ACTIVE))
                .thenReturn(List.of(debts));
    }

    private void givenBalance(String balance) {
        when(creditAccountService.getOutstandingBalance(ACCOUNT_ID)).thenReturn(money(balance));
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
