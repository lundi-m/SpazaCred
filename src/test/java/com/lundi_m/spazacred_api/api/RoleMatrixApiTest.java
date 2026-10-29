package com.lundi_m.spazacred_api.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoleMatrixApiTest extends ApiTestBase {

    record Endpoint(HttpMethod method, String path, String body, boolean shopOwnerAllowed) {
        @Override
        public String toString() {
            return method + " " + path;
        }
    }

    static Stream<Endpoint> restrictedEndpoints() {
        String reason = "{\"reason\":\"test\"}";
        String newLimit = "{\"newLimit\":100.00,\"reason\":\"test\"}";
        String newUser = """
                {"firstName":"New","lastName":"Staff","email":"matrix@test.local","temporaryPassword":"Temp12345","role":"EMPLOYEE"}""";

        return Stream.of(
                new Endpoint(HttpMethod.PATCH, "/api/v1/customers/999999/deactivate", reason, true),
                new Endpoint(HttpMethod.PATCH, "/api/v1/customers/999999/account/limit", newLimit, true),
                new Endpoint(HttpMethod.PATCH, "/api/v1/customers/999999/account/suspend", reason, true),
                new Endpoint(HttpMethod.PATCH, "/api/v1/customers/999999/account/reactivate", reason, true),
                new Endpoint(HttpMethod.PATCH, "/api/v1/customers/999999/account/close", reason, true),
                new Endpoint(HttpMethod.POST, "/api/v1/credit-transactions/999999/void", reason, true),
                new Endpoint(HttpMethod.POST, "/api/v1/payments/999999/void", reason, true),
                new Endpoint(HttpMethod.GET, "/api/v1/users", null, false),
                new Endpoint(HttpMethod.POST, "/api/v1/users", newUser, false),
                new Endpoint(HttpMethod.GET, "/api/v1/audit-logs", null, false)
        );
    }

    @ParameterizedTest(name = "employee is forbidden: {0}")
    @MethodSource("restrictedEndpoints")
    void anEmployeeIsForbidden(Endpoint endpoint) throws Exception {
        call(endpoint, bearer(employee))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @ParameterizedTest(name = "shop owner follows the rules: {0}")
    @MethodSource("restrictedEndpoints")
    void aShopOwnerIsLetThroughExactlyWhereTheRulesSay(Endpoint endpoint) throws Exception {
        int status = statusOf(endpoint, bearer(owner));

        if (endpoint.shopOwnerAllowed()) {
            assertThat(status).isNotIn(401, 403);
        } else {
            assertThat(status).isEqualTo(403);
        }
    }

    @ParameterizedTest(name = "admin is let through: {0}")
    @MethodSource("restrictedEndpoints")
    void anAdminIsLetThroughEverywhere(Endpoint endpoint) throws Exception {
        assertThat(statusOf(endpoint, bearer(admin))).isNotIn(401, 403);
    }

    @ParameterizedTest(name = "no token is unauthorized: {0}")
    @MethodSource("restrictedEndpoints")
    void noTokenMeansUnauthorized(Endpoint endpoint) throws Exception {
        call(endpoint, null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void theLowestRoleCanStillDoTheEverydayWork() throws Exception {
        String token = bearer(employee);

        mockMvc.perform(get("/api/v1/customers").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/overdue").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/outstanding-by-customer").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/payments").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/customers").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Thabo","lastName":"Mokoena","phone":"0821234567",
                                 "idNumber":"9001015009087","address":"12 Main Road","creditLimit":500.00}"""))
                .andExpect(status().isCreated());
    }


    private ResultActions call(Endpoint endpoint, String authorization) throws Exception {
        MockHttpServletRequestBuilder request = request(endpoint.method(), endpoint.path());
        if (authorization != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        if (endpoint.body() != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(endpoint.body());
        }
        return mockMvc.perform(request);
    }

    private int statusOf(Endpoint endpoint, String authorization) throws Exception {
        return call(endpoint, authorization).andReturn().getResponse().getStatus();
    }
}