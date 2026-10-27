package com.lundi_m.spazacred_api.domain;

import com.lundi_m.spazacred_api.auth.entity.Role;
import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import com.lundi_m.spazacred_api.credit.entity.TransactionStatus;
import com.lundi_m.spazacred_api.customer.entity.Customer;
import com.lundi_m.spazacred_api.payment.entity.Payment;
import com.lundi_m.spazacred_api.payment.entity.PaymentAllocation;
import com.lundi_m.spazacred_api.payment.entity.PaymentMethod;
import com.lundi_m.spazacred_api.payment.entity.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FinancialRecordsRequireUserTest {

    private final Customer customer = new Customer("Thabo", null, "Mokoena", "0821234567",
            null, "9001015009087", "12 Main Road");
    private final CreditAccount account = new CreditAccount(customer, new BigDecimal("2000.00"));
    private final SpazaCredUser staff =
            new SpazaCredUser("Sipho", null, "Dlamini", "sipho@shop.test", "hash", Role.EMPLOYEE);

    @Test
    void aPurchaseCannotBeCreatedWithoutAUser() {
        assertThatThrownBy(() -> new CreditTransaction("CRT-1", account, null,
                new BigDecimal("10.00"), "x", LocalDate.of(2026, 11, 1)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void aPaymentCannotBeRecordedWithoutAUser() {
        assertThatThrownBy(() -> new Payment("PAY-1", customer, null,
                new BigDecimal("10.00"), PaymentMethod.CASH))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void voidingATransactionNeedsAUserAndChangesNothingWithout() {
        CreditTransaction transaction = new CreditTransaction("CRT-1", account, staff,
                new BigDecimal("10.00"), "x", LocalDate.of(2026, 11, 1));

        assertThatThrownBy(() -> transaction.voidTransaction(null))
                .isInstanceOf(NullPointerException.class);

        assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.ACTIVE);
        assertThat(transaction.getVoidedAt()).isNull();
    }

    @Test
    void voidingAPaymentNeedsAUserAndChangesNothingWithout() {
        Payment payment = new Payment("PAY-1", customer, staff, new BigDecimal("10.00"), PaymentMethod.CASH);

        assertThatThrownBy(() -> payment.voidPayment(null))
                .isInstanceOf(NullPointerException.class);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(payment.getVoidedAt()).isNull();
    }

    @Test
    void reversingAnAllocationNeedsAUserAndChangesNothingWithout() {
        CreditTransaction transaction = new CreditTransaction("CRT-1", account, staff,
                new BigDecimal("10.00"), "x", LocalDate.of(2026, 11, 1));
        Payment payment = new Payment("PAY-1", customer, staff, new BigDecimal("10.00"), PaymentMethod.CASH);
        PaymentAllocation allocation = new PaymentAllocation(payment, transaction, new BigDecimal("10.00"));

        assertThatThrownBy(() -> allocation.reverse(null))
                .isInstanceOf(NullPointerException.class);

        assertThat(allocation.getReversedAt()).isNull();
    }
}
