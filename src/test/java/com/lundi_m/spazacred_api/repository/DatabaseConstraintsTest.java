package com.lundi_m.spazacred_api.repository;

import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import com.lundi_m.spazacred_api.customer.entity.Customer;
import com.lundi_m.spazacred_api.payment.entity.Payment;
import com.lundi_m.spazacred_api.payment.entity.PaymentMethod;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseConstraintsTest extends RepositoryTestBase {

    @Autowired private JdbcTemplate jdbcTemplate;

    private Customer customer;
    private CreditAccount account;
    private CreditTransaction debt;
    private Payment payment;

    @BeforeEach
    void createParentRows() {
        customer = customer("Thabo", "Mokoena");
        account = account(customer, "2000.00");
        debt = debt(account, "500.00", LocalDate.of(2026, 11, 1));
        payment = payment(customer, "100.00", PaymentMethod.CASH);
        entityManager.flush();
    }


    @Test
    void controlAValidPurchaseRowIsAccepted() {
        assertThat(insertPurchase("CRT-RAW-1", account.getId(), staff.getId(), "10.00", "ACTIVE", null, null))
                .isEqualTo(1);
    }

    @Test
    void controlAValidPaymentRowIsAccepted() {
        assertThat(insertPayment("PAY-RAW-1", customer.getId(), staff.getId(), "10.00",
                "CASH", "COMPLETED", null)).isEqualTo(1);
    }

    @Test
    void controlAValidAllocationRowIsAccepted() {
        assertThat(insertAllocation(payment.getId(), debt.getId(), "10.00", null, null)).isEqualTo(1);
    }


    @Test
    void aPurchaseMustHaveACreator() {
        assertRejected(() -> insertPurchase("CRT-RAW-2", account.getId(), null, "10.00", "ACTIVE", null, null));
    }

    @Test
    void aPaymentMustHaveARecorder() {
        assertRejected(() -> insertPayment("PAY-RAW-2", customer.getId(), null, "10.00",
                "CASH", "COMPLETED", null));
    }

    @Test
    void aVoidedPurchaseMustSayWhoVoidedIt() {
        assertRejected(() -> insertPurchase("CRT-RAW-3", account.getId(), staff.getId(), "10.00",
                "VOIDED", LocalDateTime.now(), null));
    }

    @Test
    void anActivePurchaseCannotCarryVoidDetails() {
        assertRejected(() -> insertPurchase("CRT-RAW-4", account.getId(), staff.getId(), "10.00",
                "ACTIVE", null, staff.getId()));
    }

    @Test
    void aVoidedPaymentMustSayWhoVoidedIt() {
        assertRejected(() -> insertPayment("PAY-RAW-3", customer.getId(), staff.getId(), "10.00",
                "VOIDED", "VOIDED", LocalDateTime.now()));
    }

    @Test
    void anAllocationReversalNeedsBothTheTimeAndThePerson() {
        assertRejected(() -> insertAllocation(payment.getId(), debt.getId(), "10.00", LocalDateTime.now(), null));
    }

    @Test
    void anAllocationCannotNameAReverserWithoutAReversalTime() {
        assertRejected(() -> insertAllocation(payment.getId(), debt.getId(), "10.00", null, staff.getId()));
    }


    @ParameterizedTest
    @ValueSource(strings = {"0", "-5.00"})
    void aPurchaseAmountMustBePositive(String amount) {
        assertRejected(() -> insertPurchase("CRT-RAW-5", account.getId(), staff.getId(), amount, "ACTIVE", null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5.00"})
    void aPaymentAmountMustBePositive(String amount) {
        assertRejected(() -> insertPayment("PAY-RAW-5", customer.getId(), staff.getId(), amount,
                "CASH", "COMPLETED", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5.00"})
    void anAllocationAmountMustBePositive(String amount) {
        assertRejected(() -> insertAllocation(payment.getId(), debt.getId(), amount, null, null));
    }

    @Test
    void aCreditLimitCannotBeNegative() {
        Customer withoutAccount = customer("No", "Account");

        assertRejected(() -> insertAccount(withoutAccount.getId(), "-1.00", "ACTIVE"));
    }

    @Test
    void anAccountStatusMustBeOneOfTheKnownOnes() {
        Customer withoutAccount = customer("No", "Account");

        assertRejected(() -> insertAccount(withoutAccount.getId(), "100.00", "FROZEN"));
    }

    @Test
    void aPurchaseStatusMustBeOneOfTheKnownOnes() {
        assertRejected(() -> insertPurchase("CRT-RAW-6", account.getId(), staff.getId(), "10.00", "PAID", null, null));
    }

    @Test
    void aPaymentMethodMustBeOneOfTheKnownOnes() {
        assertRejected(() -> insertPayment("PAY-RAW-6", customer.getId(), staff.getId(), "10.00",
                "BITCOIN", "COMPLETED", null));
    }

    @Test
    void twoCustomersCannotShareAPhoneNumber() {
        assertRejected(() -> insertCustomer(customer.getPhone(), null, "7777777777777"));
    }

    @Test
    void twoCustomersCannotShareAnIdNumber() {
        assertRejected(() -> insertCustomer("0849999999", null, customer.getIdNumber()));
    }

    @Test
    void twoCustomersCannotShareAnEmail() {
        customerRepository.saveAndFlush(new Customer("A", null, "B", "0841111111",
                "dup@test.local", "1111111111111", "x"));

        assertRejected(() -> insertCustomer("0842222222", "dup@test.local", "2222222222222"));
    }

    @Test
    void aCustomerCannotHaveTwoAccounts() {
        assertRejected(() -> insertAccount(customer.getId(), "100.00", "ACTIVE"));
    }

    @Test
    void twoPurchasesCannotShareAReferenceNumber() {
        insertPurchase("CRT-RAW-7", account.getId(), staff.getId(), "10.00", "ACTIVE", null, null);

        assertRejected(() -> insertPurchase("CRT-RAW-7", account.getId(), staff.getId(), "20.00", "ACTIVE", null, null));
    }

    @Test
    void aPaymentCannotBeAllocatedToTheSameDebtTwice() {
        insertAllocation(payment.getId(), debt.getId(), "10.00", null, null);

        assertRejected(() -> insertAllocation(payment.getId(), debt.getId(), "20.00", null, null));
    }

    @Test
    void aPurchaseNeedsARealAccount() {
        assertRejected(() -> insertPurchase("CRT-RAW-8", 999_999L, staff.getId(), "10.00", "ACTIVE", null, null));
    }


    @Test
    void anAuditRowMayHaveNoUserForSystemActions() {
        assertThat(insertAudit(null, "ACCOUNT_AUTO_SUSPENDED", 1L)).isEqualTo(1);
    }

    @Test
    void anAuditRowMustSayWhichEntityItIsAbout() {
        assertRejected(() -> insertAudit(staff.getId(), "PAYMENT_VOIDED", null));
    }


    @Test
    void postgresSilentlyRoundsAThirdDecimalPlace() {
        insertPurchase("CRT-RAW-9", account.getId(), staff.getId(), "10.005", "ACTIVE", null, null);

        BigDecimal stored = jdbcTemplate.queryForObject(
                "SELECT amount FROM credit_transactions WHERE reference_number = ?", BigDecimal.class, "CRT-RAW-9");

        assertThat(stored).isEqualByComparingTo("10.01");
    }


    private void assertRejected(ThrowingCallable statement) {
        assertThatThrownBy(statement).isInstanceOf(DataIntegrityViolationException.class);
    }

    private int insertPurchase(String reference, Long accountId, Long createdBy, String amount,
                               String status, LocalDateTime voidedAt, Long voidedBy) {
        return jdbcTemplate.update("""
                INSERT INTO credit_transactions
                    (reference_number, credit_account_id, created_by, amount, due_date, status,
                     created_at, voided_at, voided_by)
                VALUES (?, ?, ?, ?, DATE '2026-11-01', ?, now(), ?, ?)
                """, reference, accountId, createdBy, new BigDecimal(amount), status, voidedAt, voidedBy);
    }

    private int insertPayment(String reference, Long customerId, Long recordedBy, String amount,
                              String method, String status, LocalDateTime voidedAt) {
        return jdbcTemplate.update("""
                INSERT INTO payments
                    (reference_number, customer_id, recorded_by, amount, payment_method, status,
                     created_at, voided_at, voided_by)
                VALUES (?, ?, ?, ?, ?, ?, now(), ?, ?)
                """, reference, customerId, recordedBy, new BigDecimal(amount), method, status, voidedAt, null);
    }

    private int insertAllocation(Long paymentId, Long debtId, String amount,
                                 LocalDateTime reversedAt, Long reversedBy) {
        return jdbcTemplate.update("""
                INSERT INTO payment_allocations
                    (payment_id, credit_transaction_id, amount_allocated, created_at, reversed_at, reversed_by)
                VALUES (?, ?, ?, now(), ?, ?)
                """, paymentId, debtId, new BigDecimal(amount), reversedAt, reversedBy);
    }

    private void insertAccount(Long customerId, String limit, String status) {
        jdbcTemplate.update("""
                INSERT INTO credit_accounts (customer_id, credit_limit, status, created_at, updated_at)
                VALUES (?, ?, ?, now(), now())
                """, customerId, new BigDecimal(limit), status);
    }

    private void insertCustomer(String phone, String email, String idNumber) {
        jdbcTemplate.update("""
                INSERT INTO customers
                    (first_name, last_name, phone, email, id_number, address, created_at, updated_at)
                VALUES ('Raw', 'Insert', ?, ?, ?, 'x', now(), now())
                """, phone, email, idNumber);
    }

    private int insertAudit(Long userId, String action, Long entityId) {
        return jdbcTemplate.update("""
                INSERT INTO audit_logs (user_id, action, entity_type, entity_id, created_at)
                VALUES (?, ?, 'CreditAccount', ?, now())
                """, userId, action, entityId);
    }
}