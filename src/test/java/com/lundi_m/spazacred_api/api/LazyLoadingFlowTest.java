package com.lundi_m.spazacred_api.api;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LazyLoadingFlowTest extends CommittedDataTestBase {

    @Test
    void everyReadEndpointWorksWithNoTransactionAroundTheRequest() throws Exception {
        long customerId = createCustomer("Thabo", "Mokoena", "thabo@shop.test", "5000.00");
        long purchaseId = buy(customerId, "500.00", 10);
        long paymentId = pay(customerId, "200.00");
        String staffToken = bearer(employee);
        String adminToken = bearer(admin);

        ok("/api/v1/customers", staffToken);
        ok("/api/v1/customers?search=Thabo", staffToken);
        ok("/api/v1/customers/" + customerId, staffToken);
        ok("/api/v1/customers/" + customerId + "/account", staffToken);
        ok("/api/v1/customers/" + customerId + "/account/eligibility", staffToken);
        ok("/api/v1/customers/" + customerId + "/credit-transactions", staffToken);
        ok("/api/v1/customers/" + customerId + "/payments", staffToken);
        ok("/api/v1/reports/overdue", staffToken);
        ok("/api/v1/reports/outstanding-by-customer", staffToken);
        ok("/api/v1/reports/payments", staffToken);
        ok("/api/v1/audit-logs", adminToken);
        ok("/api/v1/audit-logs?entityType=Payment&action=PAYMENT_RECORDED", adminToken);
        ok("/api/v1/users", adminToken);

        statusOf(voidPaymentRequest(paymentId));
        voidPaymentRequest(paymentId);
        voidPurchaseRequest(purchaseId).andExpect(status().isOk());

        ok("/api/v1/customers/" + customerId + "/payments", staffToken);
        ok("/api/v1/customers/" + customerId + "/credit-transactions", staffToken);
    }

    private void ok(String path, String token) throws Exception {
        mockMvc.perform(get(path).header(AUTH, token))
                .andExpect(status().isOk());
    }
}