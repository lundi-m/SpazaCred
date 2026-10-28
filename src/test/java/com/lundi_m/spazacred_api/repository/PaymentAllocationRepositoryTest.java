package com.lundi_m.spazacred_api.repository;

import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import com.lundi_m.spazacred_api.customer.entity.Customer;
import com.lundi_m.spazacred_api.payment.entity.Payment;
import com.lundi_m.spazacred_api.payment.entity.PaymentAllocation;
import com.lundi_m.spazacred_api.payment.entity.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentAllocationRepositoryTest extends RepositoryTestBase {

    private static final LocalDate DUE = LocalDate.of(2026, 11, 1);

    private Customer customer;
    private CreditAccount account;
    private CreditTransaction debtA;
    private CreditTransaction debtB;

    @BeforeEach
    void createDebts() {
        customer = customer("Thabo", "Mokoena");
        account = account(customer, "5000.00");
        debtA = debt(account, "500.00", DUE);
        debtB = debt(account, "700.00", DUE.plusDays(10));
    }

    @Test
    void sumsIgnoreReversedAllocations() {
        Payment first = payment(customer, "800.00", PaymentMethod.CASH);
        allocate(first, debtA, "500.00");
        allocate(first, debtB, "300.00");

        Payment second = payment(customer, "200.00", PaymentMethod.CASH);
        reversed(allocate(second, debtB, "200.00"));

        assertThat(paymentAllocationRepository.sumActiveAllocationsByAccountId(account.getId()))
                .isEqualByComparingTo("800.00");
        assertThat(paymentAllocationRepository.sumActiveAllocationsByTransactionId(debtA.getId()))
                .isEqualByComparingTo("500.00");
        assertThat(paymentAllocationRepository.sumActiveAllocationsByTransactionId(debtB.getId()))
                .isEqualByComparingTo("300.00");
    }

    @Test
    void theSumsAreZeroNotNullWhenNothingWasPaid() {
        assertThat(paymentAllocationRepository.sumActiveAllocationsByAccountId(account.getId()))
                .isEqualByComparingTo("0");
        assertThat(paymentAllocationRepository.sumActiveAllocationsByTransactionId(debtA.getId()))
                .isEqualByComparingTo("0");
    }

    @Test
    void anActiveAllocationExistsOnlyWhileItIsNotReversed() {
        assertThat(paymentAllocationRepository
                .existsByCreditTransaction_IdAndReversedAtIsNull(debtA.getId())).isFalse();

        PaymentAllocation allocation = allocate(payment(customer, "100.00", PaymentMethod.CASH), debtA, "100.00");
        assertThat(paymentAllocationRepository
                .existsByCreditTransaction_IdAndReversedAtIsNull(debtA.getId())).isTrue();

        reversed(allocation);
        assertThat(paymentAllocationRepository
                .existsByCreditTransaction_IdAndReversedAtIsNull(debtA.getId())).isFalse();
    }

    @Test
    void findsOnlyTheActiveAllocationsOfAPayment() {
        Payment payment = payment(customer, "800.00", PaymentMethod.CASH);
        PaymentAllocation active = allocate(payment, debtA, "500.00");
        reversed(allocate(payment, debtB, "300.00"));

        List<PaymentAllocation> found =
                paymentAllocationRepository.findByPayment_IdAndReversedAtIsNull(payment.getId());

        assertThat(found).containsExactly(active);
    }
}