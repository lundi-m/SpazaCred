package com.lundi_m.spazacred_api.api;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class ApiFlowTestBase extends ApiTestBase {

    protected static final MediaType JSON = MediaType.APPLICATION_JSON;
    protected static final String AUTH = HttpHeaders.AUTHORIZATION;

    private static final AtomicInteger FLOW_COUNTER = new AtomicInteger(5_000_000);

    protected int nextNumber() {
        return FLOW_COUNTER.getAndIncrement();
    }

    protected long createCustomer(String creditLimit) throws Exception {
        return createCustomer("Flow", "Customer", null, creditLimit);
    }

    protected long createCustomer(String firstName, String lastName, String email, String creditLimit)
            throws Exception {
        int n = nextNumber();
        String emailJson = email == null ? "null" : "\"" + email + "\"";
        String body = """
                {"firstName":"%s","lastName":"%s","phone":"07%08d","idNumber":"%013d",
                 "email":%s,"address":"1 Flow Street","creditLimit":%s}"""
                .formatted(firstName, lastName, n, n, emailJson, creditLimit);

        String response = mockMvc.perform(post("/api/v1/customers")
                        .header(AUTH, bearer(employee)).contentType(JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(response);
    }

    protected ResultActions buyRequest(long customerId, String amount, int dueInDays) throws Exception {
        String dueDate = LocalDate.now().plusDays(dueInDays).toString();
        return mockMvc.perform(post("/api/v1/customers/{id}/credit-transactions", customerId)
                .header(AUTH, bearer(employee)).contentType(JSON)
                .content("""
                        {"amount":%s,"description":"groceries","dueDate":"%s"}""".formatted(amount, dueDate)));
    }

    protected long buy(long customerId, String amount, int dueInDays) throws Exception {
        String response = buyRequest(customerId, amount, dueInDays)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(response);
    }

    protected ResultActions payRequest(long customerId, String amount) throws Exception {
        return mockMvc.perform(post("/api/v1/customers/{id}/payments", customerId)
                .header(AUTH, bearer(employee)).contentType(JSON)
                .content("""
                        {"amount":%s,"paymentMethod":"CASH"}""".formatted(amount)));
    }

    protected long pay(long customerId, String amount) throws Exception {
        String response = payRequest(customerId, amount)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(response);
    }

    protected ResultActions account(long customerId) throws Exception {
        return mockMvc.perform(get("/api/v1/customers/{id}/account", customerId)
                .header(AUTH, bearer(employee)));
    }

    protected long idOf(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }
}