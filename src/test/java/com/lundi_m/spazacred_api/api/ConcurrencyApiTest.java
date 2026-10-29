package com.lundi_m.spazacred_api.api;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class ConcurrencyApiTest extends CommittedDataTestBase {


    @Test
    void simultaneousPurchasesNeverGoOverTheLimit() throws Exception {
        long customerId = createCustomer("1000.00");

        List<Integer> statuses = inParallel(5,
                () -> statusOf(buyRequest(customerId, "300.00", 10)));

        assertThat(statuses).containsOnly(201, 409);
        assertThat(count(statuses, 201)).isEqualTo(3);
        assertThat(activePurchaseTotal()).isEqualByComparingTo("900.00");
    }

    @Test
    void simultaneousPaymentsCannotPayMoreThanIsOwed() throws Exception {
        long customerId = createCustomer("5000.00");
        buy(customerId, "300.00", 10);

        List<Integer> statuses = inParallel(5,
                () -> statusOf(payRequest(customerId, "100.00")));

        assertThat(statuses).containsOnly(201, 409);
        assertThat(count(statuses, 201)).isEqualTo(3);
        assertThat(activeAllocationTotal()).isEqualByComparingTo("300.00");
        account(customerId).andExpect(jsonPath("$.outstandingBalance").value(0.0));
    }

    @Test
    void twoLargePaymentsAgainstASmallBalanceLetOnlyOneThrough() throws Exception {
        long customerId = createCustomer("5000.00");
        buy(customerId, "500.00", 10);

        List<Integer> statuses = inParallel(2,
                () -> statusOf(payRequest(customerId, "400.00")));

        assertThat(statuses).containsOnly(201, 409);
        assertThat(count(statuses, 201)).isEqualTo(1);
        assertThat(activeAllocationTotal()).isEqualByComparingTo("400.00");
    }


    @Test
    void simultaneousCreatesWithTheSamePhoneLeaveExactlyOneCustomer() throws Exception {
        List<Integer> statuses = inParallel(4, () -> {
            int n = nextNumber();
            String body = """
                    {"firstName":"Race","lastName":"Condition","phone":"0845550000","idNumber":"%013d",
                     "address":"1 Race Road","creditLimit":100.00}""".formatted(n);
            return statusOf(mockMvc.perform(post("/api/v1/customers")
                    .header(AUTH, bearer(employee)).contentType(JSON).content(body)));
        });

        assertThat(statuses).containsOnly(201, 409);
        assertThat(count(statuses, 201)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE phone = '0845550000'", Long.class)).isEqualTo(1L);
    }


    @Test
    void aPaymentIsVoidedExactlyOnceEvenWhenRequestsRace() throws Exception {
        long customerId = createCustomer("5000.00");
        buy(customerId, "500.00", 10);
        long paymentId = pay(customerId, "200.00");

        List<Integer> statuses = inParallel(4, () -> statusOf(voidPaymentRequest(paymentId)));

        assertThat(statuses).containsOnly(200, 409);
        assertThat(count(statuses, 200)).isEqualTo(1);
        assertThat(auditCount("PAYMENT_VOIDED")).isEqualTo(1);
    }

    @Test
    void voidingAPurchaseWhileItIsBeingPaidNeverLeavesPaymentsOnAVoidedPurchase() throws Exception {

        for (int round = 0; round < 15; round++) {
            long customerId = createCustomer("5000.00");
            long purchaseId = buy(customerId, "500.00", 10);

            List<Callable<Integer>> tasks = List.of(
                    () -> statusOf(voidPurchaseRequest(purchaseId)),
                    () -> statusOf(payRequest(customerId, "100.00")));
            List<Integer> statuses = inParallel(tasks);

            assertThat(activeAllocationsOnVoidedPurchases())
                    .as("round %d, statuses %s", round, statuses).isZero();
            assertThat(statuses.get(0) == 200 && statuses.get(1) == 201)
                    .as("round %d: the void and the payment both succeeded", round).isFalse();
        }
    }

    private BigDecimal activePurchaseTotal() {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM credit_transactions WHERE status = 'ACTIVE'",
                BigDecimal.class);
    }

    private BigDecimal activeAllocationTotal() {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount_allocated), 0) FROM payment_allocations WHERE reversed_at IS NULL",
                BigDecimal.class);
    }

    private long activeAllocationsOnVoidedPurchases() {
        Long count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM payment_allocations a
            JOIN credit_transactions t ON t.id = a.credit_transaction_id
            WHERE a.reversed_at IS NULL
              AND t.status = 'VOIDED'
            """, Long.class);

        return count != null ? count : 0L;
    }

    private long auditCount(String action) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = ?",
                Long.class,
                action
        );

        return count != null ? count : 0L;
    }
}