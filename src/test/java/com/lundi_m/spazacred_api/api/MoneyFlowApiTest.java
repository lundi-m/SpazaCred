package com.lundi_m.spazacred_api.api;

import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MoneyFlowApiTest extends ApiFlowTestBase {

    private static final String REASON = "{\"reason\":\"test\"}";


    @Test
    void theDesignExamplePaysTheOldestDebtsFirst() throws Exception {
        long customerId = createCustomer("5000.00");
        long a = buy(customerId, "500.00", 10);
        long b = buy(customerId, "700.00", 20);
        buy(customerId, "300.00", 30);

        payRequest(customerId, "800.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.allocations.length()").value(2))
                .andExpect(jsonPath("$.allocations[0].creditTransactionId").value(a))
                .andExpect(jsonPath("$.allocations[0].amountAllocated").value(500.0))
                .andExpect(jsonPath("$.allocations[1].creditTransactionId").value(b))
                .andExpect(jsonPath("$.allocations[1].amountAllocated").value(300.0));

        account(customerId)
                .andExpect(jsonPath("$.outstandingBalance").value(700.0))
                .andExpect(jsonPath("$.availableBalance").value(4300.0));
    }

    @Test
    void theDueDateDecidesTheOrderNotTheOrderOfPurchase() throws Exception {
        long customerId = createCustomer("5000.00");
        long late = buy(customerId, "300.00", 30);
        long early = buy(customerId, "500.00", 10);
        long middle = buy(customerId, "700.00", 20);

        payRequest(customerId, "800.00")
                .andExpect(jsonPath("$.allocations[0].creditTransactionId").value(early))
                .andExpect(jsonPath("$.allocations[0].amountAllocated").value(500.0))
                .andExpect(jsonPath("$.allocations[1].creditTransactionId").value(middle))
                .andExpect(jsonPath("$.allocations[1].amountAllocated").value(300.0));
    }


    @Test
    void thePurchaseLimitIsEnforcedToTheLastCent() throws Exception {
        long customerId = createCustomer("2000.00");
        buy(customerId, "1500.00", 10);

        buyRequest(customerId, "600.00", 10)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").isNotEmpty());
        buyRequest(customerId, "500.01", 10)
                .andExpect(status().isConflict());
        buyRequest(customerId, "500.00", 10)
                .andExpect(status().isCreated());

        account(customerId).andExpect(jsonPath("$.availableBalance").value(0.0));
    }

    @Test
    void aPaymentCannotExceedWhatIsOwed() throws Exception {
        long customerId = createCustomer("2000.00");
        buy(customerId, "500.00", 10);

        payRequest(customerId, "500.01").andExpect(status().isConflict());
        payRequest(customerId, "500.00").andExpect(status().isCreated());
        payRequest(customerId, "1.00").andExpect(status().isConflict());
    }

    @ParameterizedTest(name = "amount {0} is rejected")
    @ValueSource(strings = {"0", "-5.00", "10.005"})
    void badAmountsAreRejectedOnPurchasesAndPayments(String amount) throws Exception {
        long customerId = createCustomer("2000.00");
        buy(customerId, "500.00", 10);

        buyRequest(customerId, amount, 10)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
        payRequest(customerId, amount)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void aPastDueDateIsRejected() throws Exception {
        long customerId = createCustomer("2000.00");

        buyRequest(customerId, "100.00", -3)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.dueDate").exists());
    }

    @Test
    void anUnknownCustomerCannotBuyOrPay() throws Exception {
        buyRequest(999_999L, "10.00", 5).andExpect(status().isNotFound());
        payRequest(999_999L, "10.00").andExpect(status().isNotFound());
    }


    @Test
    void voidingAPaymentPutsTheMoneyBackOnTheDebts() throws Exception {
        long customerId = createCustomer("5000.00");
        long a = buy(customerId, "500.00", 10);
        buy(customerId, "700.00", 20);
        long paymentId = pay(customerId, "600.00");
        account(customerId).andExpect(jsonPath("$.outstandingBalance").value(600.0));

        voidPayment(paymentId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VOIDED"))
                .andExpect(jsonPath("$.allocations.length()").value(0));
        account(customerId).andExpect(jsonPath("$.outstandingBalance").value(1200.0));

        voidPayment(paymentId).andExpect(status().isConflict());

        voidPurchase(a).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VOIDED"));

        account(customerId).andExpect(jsonPath("$.outstandingBalance").value(700.0));
        voidPurchase(a).andExpect(status().isConflict());
    }

    @Test
    void aPurchaseThatHasPaymentsCannotBeVoided() throws Exception {
        long customerId = createCustomer("5000.00");
        long a = buy(customerId, "500.00", 10);
        pay(customerId, "100.00");

        voidPurchase(a).andExpect(status().isConflict());
    }


    @Test
    void aSuspendedAccountStillTakesPaymentsButNotPurchases() throws Exception {
        long customerId = createCustomer("1000.00");
        buy(customerId, "100.00", 10);

        ownerPatch("/api/v1/customers/{id}/account/suspend", customerId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        buyRequest(customerId, "50.00", 10).andExpect(status().isConflict());
        payRequest(customerId, "100.00").andExpect(status().isCreated());

        ownerPatch("/api/v1/customers/{id}/account/reactivate", customerId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        buyRequest(customerId, "50.00", 10).andExpect(status().isCreated());
    }

    @Test
    void anAccountCanOnlyBeClosedOnceItIsPaidOff() throws Exception {
        long customerId = createCustomer("1000.00");
        buy(customerId, "100.00", 10);

        ownerPatch("/api/v1/customers/{id}/account/close", customerId).andExpect(status().isConflict());

        pay(customerId, "100.00");
        ownerPatch("/api/v1/customers/{id}/account/close", customerId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        buyRequest(customerId, "10.00", 10).andExpect(status().isConflict());
        ownerPatch("/api/v1/customers/{id}/account/reactivate", customerId).andExpect(status().isConflict());
    }

    @Test
    void eligibilityFollowsTheAccountState() throws Exception {
        long customerId = createCustomer("1000.00");

        mockMvc.perform(get("/api/v1/customers/{id}/account/eligibility", customerId)
                        .header(AUTH, bearer(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.reasons.length()").value(0));

        ownerPatch("/api/v1/customers/{id}/account/suspend", customerId);

        mockMvc.perform(get("/api/v1/customers/{id}/account/eligibility", customerId)
                        .header(AUTH, bearer(employee)))
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.reasons[?(@ == 'Account is SUSPENDED')]").isNotEmpty());
    }


    @Test
    void everyActionLeavesAnAuditRowNamingTheRealUser() throws Exception {
        long customerId = createCustomer("1000.00");
        long purchaseId = buy(customerId, "100.00", 10);
        long paymentId = pay(customerId, "50.00");
        voidPayment(paymentId);
        voidPurchase(purchaseId);

        mockMvc.perform(get("/api/v1/audit-logs").header(AUTH, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.action == 'CUSTOMER_CREATED' && @.userEmail == '%s')]"
                        .formatted(employee.getEmail())).isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.action == 'PURCHASE_CREATED')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.action == 'PAYMENT_RECORDED')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.action == 'PAYMENT_VOIDED' && @.userEmail == '%s')]"
                        .formatted(owner.getEmail())).isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.action == 'TRANSACTION_VOIDED')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.userId == null)]").isEmpty());
    }

    @Test
    void aLimitChangeIsAuditedWithOldNewAndReason() throws Exception {
        long customerId = createCustomer("2000.00");

        mockMvc.perform(patch("/api/v1/customers/{id}/account/limit", customerId)
                        .header(AUTH, bearer(owner)).contentType(JSON)
                        .content("{\"newLimit\":3000.00,\"reason\":\"Paid on time\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditLimit").value(3000.0));

        mockMvc.perform(get("/api/v1/audit-logs").param("action", "LIMIT_ADJUSTED")
                        .header(AUTH, bearer(admin)))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userEmail").value(owner.getEmail()))
                .andExpect(jsonPath("$.content[0].details.reason").value("Paid on time"))
                .andExpect(jsonPath("$.content[0].details.oldLimit").value(2000.0))
                .andExpect(jsonPath("$.content[0].details.newLimit").value(3000.0));
    }

    @Test
    void aBackwardsAuditDateRangeIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("from", "2026-11-01")
                        .param("to", "2026-10-01")
                        .header(AUTH, bearer(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }


    @Test
    void theReportsReflectWhatHappened() throws Exception {
        long customerId = createCustomer("5000.00");
        long oldest = buy(customerId, "500.00", 10);
        buy(customerId, "250.00", 20);
        pay(customerId, "100.00");
        backdate(oldest, 10);

        String token = bearer(employee);

        mockMvc.perform(get("/api/v1/reports/overdue").header(AUTH, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].transactionId").value(oldest))
                .andExpect(jsonPath("$.content[?(@.daysOverdue >= 10)]").isNotEmpty())
                .andExpect(jsonPath("$.content[0].paid").value(100.0))
                .andExpect(jsonPath("$.content[0].remaining").value(400.0));

        mockMvc.perform(get("/api/v1/reports/overdue").param("minDaysOverdue", "30").header(AUTH, token))
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(get("/api/v1/reports/outstanding-by-customer").header(AUTH, token))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].customerId").value(customerId))
                .andExpect(jsonPath("$.content[0].outstandingBalance").value(650.0));

        LocalDate today = LocalDate.now();
        mockMvc.perform(get("/api/v1/reports/payments")
                        .param("from", today.minusDays(1).toString())
                        .param("to", today.plusDays(1).toString())
                        .header(AUTH, token))
                .andExpect(jsonPath("$.paymentCount").value(1))
                .andExpect(jsonPath("$.totalCollected").value(100.0))
                .andExpect(jsonPath("$.byMethod[0].method").value("CASH"))
                .andExpect(jsonPath("$.payments.content.length()").value(1));
    }

    @Test
    void voidedPaymentsDisappearFromThePaymentsReport() throws Exception {
        long customerId = createCustomer("5000.00");
        buy(customerId, "500.00", 10);
        pay(customerId, "100.00");
        long second = pay(customerId, "50.50");
        voidPayment(second);

        LocalDate today = LocalDate.now();
        mockMvc.perform(get("/api/v1/reports/payments")
                        .param("from", today.minusDays(1).toString())
                        .param("to", today.plusDays(1).toString())
                        .header(AUTH, bearer(employee)))
                .andExpect(jsonPath("$.paymentCount").value(1))
                .andExpect(jsonPath("$.totalCollected").value(100.0));
    }

    @Test
    void reportsIgnoreASortParameterInsteadOfFailing() throws Exception {
        mockMvc.perform(get("/api/v1/reports/overdue")
                        .param("sort", "nonsense").header(AUTH, bearer(employee)))
                .andExpect(status().isOk());
    }

    @Test
    void theReportDateRangeMustBeTheRightWayRound() throws Exception {
        mockMvc.perform(get("/api/v1/reports/payments")
                        .param("from", "2026-11-01")
                        .param("to", "2026-10-01")
                        .header(AUTH, bearer(employee)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }


    private ResultActions voidPayment(long paymentId) throws Exception {
        return mockMvc.perform(post("/api/v1/payments/{id}/void", paymentId)
                .header(AUTH, bearer(owner)).contentType(JSON).content(REASON));
    }

    private ResultActions voidPurchase(long transactionId) throws Exception {
        return mockMvc.perform(post("/api/v1/credit-transactions/{id}/void", transactionId)
                .header(AUTH, bearer(owner)).contentType(JSON).content(REASON));
    }

    private ResultActions ownerPatch(String path, long customerId) throws Exception {
        return mockMvc.perform(patch(path, customerId)
                .header(AUTH, bearer(owner)).contentType(JSON).content(REASON));
    }

    private void backdate(long transactionId, int daysAgo) {
        CreditTransaction transaction = creditTransactionRepository.findById(transactionId).orElseThrow();
        transaction.setDueDate(LocalDate.now().minusDays(daysAgo));
        creditTransactionRepository.saveAndFlush(transaction);
    }
}