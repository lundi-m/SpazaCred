package com.lundi_m.spazacred_api.credit.service;

import com.lundi_m.spazacred_api.audit.service.AuditLogService;
import com.lundi_m.spazacred_api.auth.entity.Role;
import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.credit.dto.VoidTransactionRequest;
import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import com.lundi_m.spazacred_api.credit.entity.TransactionStatus;
import com.lundi_m.spazacred_api.credit.repository.CreditTransactionRepository;
import com.lundi_m.spazacred_api.customer.entity.Customer;
import com.lundi_m.spazacred_api.exception.CreditTransactionNotFoundException;
import com.lundi_m.spazacred_api.exception.TransactionNotVoidableException;
import com.lundi_m.spazacred_api.payment.repository.PaymentAllocationRepository;
import com.lundi_m.spazacred_api.security.CurrentUserProvider;
import com.lundi_m.spazacred_api.security.SpazaCredUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreditTransactionVoidTest {

    private static final Long TRANSACTION_ID = 5L;

    @Mock private CreditTransactionRepository creditTransactionRepository;
    @Mock private PaymentAllocationRepository paymentAllocationRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private CurrentUserProvider currentUserProvider;

    @InjectMocks private CreditTransactionService creditTransactionService;

    private SpazaCredUser staff;
    private CreditTransaction transaction;

    @BeforeEach
    void setUp() {
        Customer customer = new Customer("Thabo", null, "Mokoena", "0821234567",
                null, "9001015009087", "12 Main Road");
        customer.setId(1L);
        CreditAccount account = new CreditAccount(customer, new BigDecimal("2000.00"));
        account.setId(10L);

        staff = new SpazaCredUser("Nomsa", null, "Khumalo", "nomsa@shop.test", "hash", Role.SHOP_OWNER);
        staff.setId(7L);

        transaction = new CreditTransaction("CRT-2026-000005", account, staff,
                new BigDecimal("500.00"), "groceries", LocalDate.of(2026, 11, 1));
        transaction.setId(TRANSACTION_ID);

        lenient().when(currentUserProvider.getCurrentUser()).thenReturn(new SpazaCredUserDetails(staff));
        lenient().when(creditTransactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.of(transaction));
    }

    @Test
    void voidsATransactionThatHasNoActivePayments() {
        when(paymentAllocationRepository.existsByCreditTransaction_IdAndReversedAtIsNull(TRANSACTION_ID))
                .thenReturn(false);

        creditTransactionService.voidTransaction(TRANSACTION_ID, new VoidTransactionRequest("Entered twice"));

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.VOIDED);
        assertThat(transaction.getVoidedBy()).isEqualTo(staff);
        assertThat(transaction.getVoidedAt()).isNotNull();

        Map<String, Object> expectedDetails = Map.of("reason", "Entered twice");
        verify(auditLogService).record(eq(staff), eq("TRANSACTION_VOIDED"), eq("CreditTransaction"),
                eq(TRANSACTION_ID), eq(expectedDetails));
    }

    @Test
    void refusesToVoidATransactionThatHasActivePayments() {
        when(paymentAllocationRepository.existsByCreditTransaction_IdAndReversedAtIsNull(TRANSACTION_ID))
                .thenReturn(true);

        assertThatThrownBy(() ->
                creditTransactionService.voidTransaction(TRANSACTION_ID, new VoidTransactionRequest("x")))
                .isInstanceOf(TransactionNotVoidableException.class);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.ACTIVE);
        verifyNoInteractions(auditLogService);
    }

    @Test
    void refusesToVoidTheSameTransactionTwice() {
        transaction.voidTransaction(staff);

        assertThatThrownBy(() ->
                creditTransactionService.voidTransaction(TRANSACTION_ID, new VoidTransactionRequest("x")))
                .isInstanceOf(TransactionNotVoidableException.class);

        verifyNoInteractions(auditLogService);
    }

    @Test
    void rejectsAnUnknownTransaction() {
        when(creditTransactionRepository.findById(TRANSACTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                creditTransactionService.voidTransaction(TRANSACTION_ID, new VoidTransactionRequest("x")))
                .isInstanceOf(CreditTransactionNotFoundException.class);
    }
}
