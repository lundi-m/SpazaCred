package com.lundi_m.spazacred_api.repository;

import com.lundi_m.spazacred_api.customer.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerRepositoryTest extends RepositoryTestBase {

    private Customer thabo;
    private Customer nomsa;

    @BeforeEach
    void createCustomers() {
        thabo = customerRepository.save(new Customer("Thabo", null, "Mokoena", "0821112222",
                "thabo@shop.test", "9001015009087", "12 Main Road"));
        nomsa = customerRepository.save(new Customer("Nomsa", "Grace", "Khumalo", "0833334444",
                null, "8505050123084", "4 Hill Street"));
    }

    @Test
    void existsChecksFindExistingValuesOnly() {
        assertThat(customerRepository.existsByPhone("0821112222")).isTrue();
        assertThat(customerRepository.existsByPhone("0800000000")).isFalse();
        assertThat(customerRepository.existsByEmail("thabo@shop.test")).isTrue();
        assertThat(customerRepository.existsByEmail("nobody@shop.test")).isFalse();
        assertThat(customerRepository.existsByIdNumber("8505050123084")).isTrue();
        assertThat(customerRepository.existsByIdNumber("0000000000000")).isFalse();
    }

    @Test
    void theUpdateDuplicateChecksIgnoreTheCustomerBeingUpdated() {
        assertThat(customerRepository.existsByPhoneAndIdNot("0821112222", thabo.getId())).isFalse();
        assertThat(customerRepository.existsByEmailAndIdNot("thabo@shop.test", thabo.getId())).isFalse();

        assertThat(customerRepository.existsByPhoneAndIdNot("0821112222", nomsa.getId())).isTrue();
        assertThat(customerRepository.existsByEmailAndIdNot("thabo@shop.test", nomsa.getId())).isTrue();
    }

    @Test
    void searchingWithoutATermReturnsEveryone() {
        Page<Customer> page = customerRepository.findFiltered(null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({
            "MOKO,    Mokoena",
            "kHuMaLo, Khumalo",
            "grace,   Khumalo",
            "0833,    Khumalo",
            "85050,   Khumalo",
            "thabo@,  Mokoena"
    })
    void searchMatchesExactlyOneCustomer(String term, String expectedLastName) {
        Page<Customer> page = customerRepository.findFiltered(term.trim(), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Customer::getLastName).containsExactly(expectedLastName);
    }

    @Test
    void searchingForSomethingUnknownReturnsNobody() {
        assertThat(customerRepository.findFiltered("zzzz", PageRequest.of(0, 10)).getContent()).isEmpty();
    }
}