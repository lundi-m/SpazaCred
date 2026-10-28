package com.lundi_m.spazacred_api.repository;

import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import com.lundi_m.spazacred_api.customer.entity.Customer;
import com.lundi_m.spazacred_api.payment.entity.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CreditAccountRepositoryTest extends RepositoryTestBase {

    private static final LocalDate DUE = LocalDate.of(2026, 11, 1);

    @Test
    void findsAnAccountByCustomerAndById() {
        Customer customer = customer("Thabo", "Mokoena");
        CreditAccount account = account(customer, "2000.00");

        assertThat(creditAccountRepository.findByCustomerId(customer.getId())).contains(account);
        assertThat(creditAccountRepository.findByCustomerIdForUpdate(customer.getId())).contains(account);
        assertThat(creditAccountRepository.findByIdForUpdate(account.getId())).contains(account);
        assertThat(creditAccountRepository.findByCustomerId(999_999L)).isEmpty();
        assertThat(creditAccountRepository.findByIdForUpdate(999_999L)).isEmpty();
    }

    @Test
    void theOutstandingListHasOnlyAccountsThatStillOweMoneyOrderedByName() {

        Customer zulu = customer("Zodwa", "Zulu");
        debt(account(zulu, "1000.00"), "500.00", DUE);

        Customer adams = customer("Ayanda", "Adams");
        CreditAccount adamsAccount = account(adams, "1000.00");
        CreditTransaction adamsDebt = debt(adamsAccount, "300.00", DUE);
        allocate(payment(adams, "100.00", PaymentMethod.CASH), adamsDebt, "100.00");

        Customer ebrahim = customer("Elias", "Ebrahim");
        CreditAccount ebrahimAccount = account(ebrahim, "1000.00");
        CreditTransaction ebrahimDebt = debt(ebrahimAccount, "150.00", DUE);
        reversed(allocate(payment(ebrahim, "150.00", PaymentMethod.EFT), ebrahimDebt, "150.00"));

        Customer bhengu = customer("Bongani", "Bhengu");
        CreditAccount bhenguAccount = account(bhengu, "1000.00");
        CreditTransaction bhenguDebt = debt(bhenguAccount, "200.00", DUE);
        allocate(payment(bhengu, "200.00", PaymentMethod.CARD), bhenguDebt, "200.00");

        voided(debt(account(customer("Cebo", "Cele"), "1000.00"), "400.00", DUE));

        account(customer("Dumisani", "Dube"), "1000.00");

        Page<CreditAccount> page = creditAccountRepository.findWithOutstandingBalance(PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(account -> account.getCustomer().getLastName())
                .containsExactly("Adams", "Ebrahim", "Zulu");
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void theOutstandingListCountsAllRowsAcrossPages() {
        debt(account(customer("Ayanda", "Adams"), "1000.00"), "100.00", DUE);
        debt(account(customer("Bongani", "Bhengu"), "1000.00"), "100.00", DUE);
        debt(account(customer("Cebo", "Cele"), "1000.00"), "100.00", DUE);

        Page<CreditAccount> firstPage = creditAccountRepository.findWithOutstandingBalance(PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
    }
}