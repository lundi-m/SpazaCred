package com.lundi_m.spazacred_api.repository;

import com.lundi_m.spazacred_api.credit.entity.AccountStatus;
import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import com.lundi_m.spazacred_api.credit.entity.TransactionStatus;
import com.lundi_m.spazacred_api.customer.entity.Customer;
import com.lundi_m.spazacred_api.payment.entity.Payment;
import com.lundi_m.spazacred_api.payment.entity.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreditTransactionRepositoryTest extends RepositoryTestBase {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    private Customer customer;
    private CreditAccount account;

    @BeforeEach
    void createCustomerAndAccount() {
        customer = customer("Thabo", "Mokoena");
        account = account(customer, "5000.00");
    }

    @Test
    void sumsOnlyTheActiveTransactionsOfThatAccount() {
        debt(account, "500.00", TODAY.plusDays(10));
        debt(account, "250.50", TODAY.plusDays(20));
        voided(debt(account, "999.00", TODAY.plusDays(5)));
        CreditAccount otherAccount = account(customer("Other", "Person"), "1000.00");
        debt(otherAccount, "100.00", TODAY);

        assertThat(creditTransactionRepository.sumActiveAmountsByAccountId(account.getId()))
                .isEqualByComparingTo("750.50");
    }

    @Test
    void theSumIsZeroNotNullWhenThereAreNoTransactions() {
        assertThat(creditTransactionRepository.sumActiveAmountsByAccountId(account.getId()))
                .isEqualByComparingTo("0");
    }

    @Test
    void activeDebtsComeBackOldestDueDateFirstThenInCreationOrder() {
        CreditTransaction late = debt(account, "100.00", TODAY.plusDays(30));
        CreditTransaction early = debt(account, "100.00", TODAY.plusDays(5));
        CreditTransaction tiedFirst = debt(account, "100.00", TODAY.plusDays(10));
        CreditTransaction tiedSecond = debt(account, "100.00", TODAY.plusDays(10));
        voided(debt(account, "100.00", TODAY.plusDays(1)));   // never returned

        List<CreditTransaction> result = creditTransactionRepository
                .findByCreditAccount_IdAndStatusOrderByDueDateAscCreatedAtAscIdAsc(
                        account.getId(), TransactionStatus.ACTIVE);

        assertThat(result).extracting(CreditTransaction::getId)
                .containsExactly(early.getId(), tiedFirst.getId(), tiedSecond.getId(), late.getId());
    }

    @Test
    void overdueMeansDueBeforeTodayWithMoneyStillOwed() {
        Payment payment = payment(customer, "5000.00", PaymentMethod.CASH);

        debt(account, "100.00", TODAY);
        CreditTransaction yesterday = debt(account, "100.00", TODAY.minusDays(1));
        CreditTransaction fullyPaid = debt(account, "200.00", TODAY.minusDays(10));
        allocate(payment, fullyPaid, "200.00");
        CreditTransaction partlyPaid = debt(account, "300.00", TODAY.minusDays(3));
        allocate(payment, partlyPaid, "100.00");
        CreditTransaction paymentReversed = debt(account, "400.00", TODAY.minusDays(2));
        reversed(allocate(payment, paymentReversed, "400.00"));
        voided(debt(account, "500.00", TODAY.minusDays(20)));

        List<CreditTransaction> overdue =
                creditTransactionRepository.findOverdueByAccountId(account.getId(), TODAY);

        assertThat(overdue).extracting(CreditTransaction::getId)
                .containsExactly(partlyPaid.getId(), paymentReversed.getId(), yesterday.getId());
    }

    @Test
    void aDebtExactlyOnTheCutoffIsNotYetACandidate() {
        LocalDate cutoff = TODAY.minusDays(7);
        debt(account, "100.00", cutoff);

        assertThat(creditTransactionRepository.findActiveAccountIdsWithDebtDueBefore(cutoff))
                .doesNotContain(account.getId());
    }

    @Test
    void aDebtOlderThanTheCutoffMakesTheAccountACandidate() {
        LocalDate cutoff = TODAY.minusDays(7);
        debt(account, "100.00", cutoff.minusDays(1));

        assertThat(creditTransactionRepository.findActiveAccountIdsWithDebtDueBefore(cutoff))
                .contains(account.getId());
    }

    @Test
    void anAccountWithSeveralOverdueDebtsIsListedOnce() {
        LocalDate cutoff = TODAY.minusDays(7);
        debt(account, "100.00", cutoff.minusDays(1));
        debt(account, "100.00", cutoff.minusDays(5));

        assertThat(creditTransactionRepository.findActiveAccountIdsWithDebtDueBefore(cutoff))
                .containsOnlyOnce(account.getId());
    }

    @Test
    void suspendedAccountsAreNotCandidates() {
        LocalDate cutoff = TODAY.minusDays(7);
        debt(account, "100.00", cutoff.minusDays(1));
        account.setStatus(AccountStatus.SUSPENDED);

        assertThat(creditTransactionRepository.findActiveAccountIdsWithDebtDueBefore(cutoff))
                .doesNotContain(account.getId());
    }

    @Test
    void paidAndVoidedOldDebtsAreNotCandidates() {
        LocalDate cutoff = TODAY.minusDays(7);
        Payment payment = payment(customer, "1000.00", PaymentMethod.CASH);
        CreditTransaction paid = debt(account, "100.00", cutoff.minusDays(3));
        allocate(payment, paid, "100.00");
        voided(debt(account, "100.00", cutoff.minusDays(4)));

        assertThat(creditTransactionRepository.findActiveAccountIdsWithDebtDueBefore(cutoff))
                .doesNotContain(account.getId());
    }


    @Test
    void theReportIncludesDebtsDueOnTheCutoffDayOldestFirst() {
        LocalDate cutoff = TODAY.minusDays(1);
        debt(account, "100.00", TODAY);
        CreditTransaction onCutoff = debt(account, "100.00", cutoff);
        CreditTransaction older = debt(account, "100.00", cutoff.minusDays(4));

        Page<CreditTransaction> page = creditTransactionRepository
                .findOverduePage(cutoff, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(CreditTransaction::getId)
                .containsExactly(older.getId(), onCutoff.getId());
        assertThat(page.getContent().getFirst().getCreditAccount().getCustomer().getFirstName())
                .isEqualTo("Thabo");
    }

    @Test
    void theReportCountsAllRowsAcrossPages() {
        LocalDate cutoff = TODAY.minusDays(1);
        debt(account, "100.00", TODAY.minusDays(2));
        debt(account, "100.00", TODAY.minusDays(3));
        debt(account, "100.00", TODAY.minusDays(4));

        Page<CreditTransaction> firstPage = creditTransactionRepository
                .findOverduePage(cutoff, PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void referenceSequenceKeepsIncreasing() {
        Long first = creditTransactionRepository.nextReferenceSequence();
        Long second = creditTransactionRepository.nextReferenceSequence();

        assertThat(second).isGreaterThan(first);
    }
}