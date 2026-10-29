package com.lundi_m.spazacred_api.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerApiTest extends ApiFlowTestBase {

    private static final String CUSTOMERS = "/api/v1/customers";


    @Test
    void createsACustomerTogetherWithItsCreditAccount() throws Exception {
        mockMvc.perform(post(CUSTOMERS).header(AUTH, bearer(employee)).contentType(JSON)
                        .content(customerJson("0821234567", "9001015009087", "thabo@shop.test", "500.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.phone").value("0821234567"))
                .andExpect(jsonPath("$.idNumber").value("9001015009087"))
                .andExpect(jsonPath("$.email").value("thabo@shop.test"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.creditLimit").value(500.0))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    @Test
    void aZeroCreditLimitIsAllowed() throws Exception {
        mockMvc.perform(post(CUSTOMERS).header(AUTH, bearer(employee)).contentType(JSON)
                        .content(customerJson("0821234567", "9001015009087", null, "0.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creditLimit").value(0.0));
    }

    @Test
    void everyMissingRequiredFieldIsReportedAtOnce() throws Exception {
        mockMvc.perform(post(CUSTOMERS).header(AUTH, bearer(employee)).contentType(JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.lastName").exists())
                .andExpect(jsonPath("$.fieldErrors.phone").exists())
                .andExpect(jsonPath("$.fieldErrors.idNumber").exists())
                .andExpect(jsonPath("$.fieldErrors.address").exists())
                .andExpect(jsonPath("$.fieldErrors.creditLimit").exists());
    }

    @ParameterizedTest(name = "bad {0} is rejected")
    @MethodSource("invalidCustomers")
    void aBadValueIsRejectedAgainstItsField(String field, String json) throws Exception {
        mockMvc.perform(post(CUSTOMERS).header(AUTH, bearer(employee)).contentType(JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors." + field).exists());
    }

    @Test
    void aDuplicatePhoneIsAConflict() throws Exception {
        create("0821234567", "9001015009087", "a@shop.test");

        mockMvc.perform(post(CUSTOMERS).header(AUTH, bearer(employee)).contentType(JSON)
                        .content(customerJson("0821234567", "8505050123084", "b@shop.test", "100.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").isNotEmpty());
    }

    @Test
    void aDuplicateIdNumberIsAConflict() throws Exception {
        create("0821234567", "9001015009087", "a@shop.test");

        mockMvc.perform(post(CUSTOMERS).header(AUTH, bearer(employee)).contentType(JSON)
                        .content(customerJson("0839999999", "9001015009087", "b@shop.test", "100.00")))
                .andExpect(status().isConflict());
    }

    @Test
    void aDuplicateEmailIsAConflict() throws Exception {
        create("0821234567", "9001015009087", "same@shop.test");

        mockMvc.perform(post(CUSTOMERS).header(AUTH, bearer(employee)).contentType(JSON)
                        .content(customerJson("0839999999", "8505050123084", "same@shop.test", "100.00")))
                .andExpect(status().isConflict());
    }

    @Test
    void severalCustomersWithoutAnEmailAreNotDuplicatesOfEachOther() throws Exception {
        createCustomer("Ayanda", "Adams", null, "100.00");
        createCustomer("Bongani", "Bhengu", null, "100.00");
        createCustomer("Cebo", "Cele", null, "100.00");
    }


    @Test
    void brokenJsonIsAMalformedRequest() throws Exception {
        mockMvc.perform(post(CUSTOMERS).header(AUTH, bearer(employee)).contentType(JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void anEmptyBodyIsAMalformedRequest() throws Exception {
        mockMvc.perform(post(CUSTOMERS).header(AUTH, bearer(employee)).contentType(JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void aWrongContentTypeIsRejected() throws Exception {
        mockMvc.perform(post(CUSTOMERS).header(AUTH, bearer(employee))
                        .contentType("text/plain").content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void aMethodThatDoesNotExistIsNotAllowed() throws Exception {
        mockMvc.perform(delete(CUSTOMERS + "/1").header(AUTH, bearer(employee)))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void anUnknownPathIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/nothing-here").header(AUTH, bearer(employee)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ENDPOINT_NOT_FOUND"));
    }


    @Test
    void readsACustomerById() throws Exception {
        long id = createCustomer("Thabo", "Mokoena", "thabo@shop.test", "250.00");

        mockMvc.perform(get(CUSTOMERS + "/{id}", id).header(AUTH, bearer(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.lastName").value("Mokoena"))
                .andExpect(jsonPath("$.email").value("thabo@shop.test"));
    }

    @Test
    void anUnknownCustomerIsNotFound() throws Exception {
        mockMvc.perform(get(CUSTOMERS + "/999999").header(AUTH, bearer(employee)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").isNotEmpty());
    }

    @Test
    void aNonNumericIdIsABadParameter() throws Exception {
        mockMvc.perform(get(CUSTOMERS + "/abc").header(AUTH, bearer(employee)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }


    @Test
    void listsAllCustomersWhenThereIsNoSearchTerm() throws Exception {
        createCustomer("Thabo", "Mokoena", "thabo@shop.test", "100.00");
        createCustomer("Nomsa", "Khumalo", null, "100.00");

        mockMvc.perform(get(CUSTOMERS).header(AUTH, bearer(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.lastName == 'Mokoena')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.lastName == 'Khumalo')]").isNotEmpty());
    }

    @Test
    void searchFindsPartOfANameInAnyCase() throws Exception {
        createCustomer("Thabo", "Mokoena", "thabo@shop.test", "100.00");
        createCustomer("Nomsa", "Khumalo", null, "100.00");

        mockMvc.perform(get(CUSTOMERS).param("search", "MOKO").header(AUTH, bearer(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.lastName == 'Mokoena')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.lastName == 'Khumalo')]").isEmpty());
    }

    @Test
    void searchFindsAnEmail() throws Exception {
        createCustomer("Thabo", "Mokoena", "thabo@shop.test", "100.00");
        createCustomer("Nomsa", "Khumalo", null, "100.00");

        mockMvc.perform(get(CUSTOMERS).param("search", "thabo@").header(AUTH, bearer(employee)))
                .andExpect(jsonPath("$.content[?(@.lastName == 'Mokoena')]").isNotEmpty())
                .andExpect(jsonPath("$.content[?(@.lastName == 'Khumalo')]").isEmpty());
    }

    @Test
    void aKnownSortFieldWorks() throws Exception {
        createCustomer("Thabo", "Mokoena", null, "100.00");

        mockMvc.perform(get(CUSTOMERS).param("sort", "lastName,asc").header(AUTH, bearer(employee)))
                .andExpect(status().isOk());
    }

    @Test
    void anUnknownSortFieldIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                        .param("sort", "unknownField,asc")
                        .header(AUTH, bearer(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void updatesACustomerButNeverTheIdNumber() throws Exception {
        long id = createCustomer("Thabo", "Mokoena", null, "100.00");

        String response = mockMvc.perform(put(CUSTOMERS + "/{id}", id)
                        .header(AUTH, bearer(employee)).contentType(JSON)
                        .content("""
                                {"firstName":"Thabiso","lastName":"Mokoena-Dlamini","phone":"0829998888",
                                 "address":"9 New Road","idNumber":"1111111111111"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Thabiso"))
                .andExpect(jsonPath("$.lastName").value("Mokoena-Dlamini"))
                .andExpect(jsonPath("$.phone").value("0829998888"))
                .andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(response, "$.idNumber")).isNotEqualTo("1111111111111");
    }

    @Test
    void keepingYourOwnPhoneOrNoEmailIsNotADuplicate() throws Exception {
        long first = createCustomer("Ayanda", "Adams", null, "100.00");
        createCustomer("Bongani", "Bhengu", null, "100.00");

        String phone = JsonPath.read(mockMvc.perform(get(CUSTOMERS + "/{id}", first)
                .header(AUTH, bearer(employee))).andReturn().getResponse().getContentAsString(), "$.phone");

        mockMvc.perform(put(CUSTOMERS + "/{id}", first).header(AUTH, bearer(employee)).contentType(JSON)
                        .content(updateJson("Ayanda", "Adams", phone)))
                .andExpect(status().isOk());
    }

    @Test
    void takingSomeoneElsePhoneIsAConflict() throws Exception {
        long first = createCustomer("Ayanda", "Adams", null, "100.00");
        long second = createCustomer("Bongani", "Bhengu", null, "100.00");

        String secondPhone = JsonPath.read(mockMvc.perform(get(CUSTOMERS + "/{id}", second)
                .header(AUTH, bearer(employee))).andReturn().getResponse().getContentAsString(), "$.phone");

        mockMvc.perform(put(CUSTOMERS + "/{id}", first).header(AUTH, bearer(employee)).contentType(JSON)
                        .content(updateJson("Ayanda", "Adams", secondPhone)))
                .andExpect(status().isConflict());
    }

    @Test
    void updatingAnUnknownCustomerIsNotFound() throws Exception {
        mockMvc.perform(put(CUSTOMERS + "/999999").header(AUTH, bearer(employee)).contentType(JSON)
                        .content(updateJson("No", "Body", "0821234567")))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatingWithoutAnAddressIsRejected() throws Exception {

        long id = createCustomer("Thabo", "Mokoena", null, "100.00");

        mockMvc.perform(put(CUSTOMERS + "/{id}", id).header(AUTH, bearer(employee)).contentType(JSON)
                        .content("""
                                {"firstName":"Thabo","lastName":"Mokoena","phone":"0821234567"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.address").exists());
    }


    @Test
    void deactivationNeedsAReason() throws Exception {
        long id = createCustomer("500.00");

        mockMvc.perform(patch(CUSTOMERS + "/{id}/deactivate", id)
                        .header(AUTH, bearer(owner)).contentType(JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.reason").exists());
    }

    @Test
    void aCustomerWhoOwesNothingCanBeDeactivatedAndThenCannotBuy() throws Exception {
        long id = createCustomer("500.00");

        mockMvc.perform(patch(CUSTOMERS + "/{id}/deactivate", id)
                        .header(AUTH, bearer(owner)).contentType(JSON).content("{\"reason\":\"Moved away\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(CUSTOMERS + "/{id}", id).header(AUTH, bearer(employee)))
                .andExpect(jsonPath("$.active").value(false));
        buyRequest(id, "10.00", 5)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").isNotEmpty());
    }

    @Test
    void aCustomerInDebtCannotBeDeactivated() throws Exception {
        long id = createCustomer("500.00");
        buy(id, "100.00", 5);

        mockMvc.perform(patch(CUSTOMERS + "/{id}/deactivate", id)
                        .header(AUTH, bearer(owner)).contentType(JSON).content("{\"reason\":\"Moved away\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").isNotEmpty());
    }

    static Stream<Arguments> invalidCustomers() {
        return Stream.of(
                Arguments.of("phone", customerJson("abc", "9001015009087", null, "100.00")),
                Arguments.of("idNumber", customerJson("0821234567", "123456789012", null, "100.00")),
                Arguments.of("email", customerJson("0821234567", "9001015009087", "not-an-email", "100.00")),
                Arguments.of("creditLimit", customerJson("0821234567", "9001015009087", null, "-1")),
                Arguments.of("creditLimit", customerJson("0821234567", "9001015009087", null, "10.005")));
    }


    private void create(String phone, String idNumber, String email) throws Exception {
        mockMvc.perform(post(CUSTOMERS).header(AUTH, bearer(employee)).contentType(JSON)
                        .content(customerJson(phone, idNumber, email, "100.00")))
                .andExpect(status().isCreated());
    }

    private static String customerJson(String phone, String idNumber, String email, String creditLimit) {
        String emailJson = email == null ? "null" : "\"" + email + "\"";
        return """
                {"firstName":"Thabo","lastName":"Mokoena","phone":"%s","idNumber":"%s",
                 "email":%s,"address":"12 Main Road","creditLimit":%s}"""
                .formatted(phone, idNumber, emailJson, creditLimit);
    }

    private static String updateJson(String firstName, String lastName, String phone) {
        return """
                {"firstName":"%s","lastName":"%s","phone":"%s","address":"12 Main Road"}"""
                .formatted(firstName, lastName, phone);
    }
}