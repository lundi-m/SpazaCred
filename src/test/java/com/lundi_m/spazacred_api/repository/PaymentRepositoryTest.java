package com.lundi_m.spazacred_api.repository;

import com.lundi_m.spazacred_api.customer.entity.Customer;
import com.lundi_m.spazacred_api.payment.dto.PaymentMethodTotal;
import com.lundi_m.spazacred_api.payment.entity.Payment;
import com.lundi_m.spazacred_api.payment.entity.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRepositoryTest extends RepositoryTestBase {

    private Customer customer;

    @BeforeEach
    void createCustomer() {
        customer = customer("Thabo", "Mokoena");
    }

    @Test
    void listsCompletedPaymentsInTheWindowNewestFirst() {
        Payment first = payment(customer, "100.00", PaymentMethod.CASH);
        Payment second = payment(customer, "200.00", PaymentMethod.EFT);
        Payment third = payment(customer, "300.00", PaymentMethod.CARD);

        Page<Payment> page = paymentRepository.findCompletedBetween(anHourAgo(), inAnHour(), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Payment::getId)
                .containsExactly(third.getId(), second.getId(), first.getId());
        assertThat(page.getContent().getFirst().getCustomer().getFirstName()).isEqualTo("Thabo");
    }

    @Test
    void theCountCoversAllPagesNotJustTheCurrentOne() {
        payment(customer, "100.00", PaymentMethod.CASH);
        payment(customer, "100.00", PaymentMethod.CASH);
        payment(customer, "100.00", PaymentMethod.CASH);

        Page<Payment> page = paymentRepository.findCompletedBetween(anHourAgo(), inAnHour(), PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void voidedPaymentsAreLeftOut() {
        Payment kept = payment(customer, "100.00", PaymentMethod.CASH);
        Payment gone = payment(customer, "200.00", PaymentMethod.CASH);
        gone.voidPayment(staff);
        paymentRepository.save(gone);

        Page<Payment> page = paymentRepository.findCompletedBetween(anHourAgo(), inAnHour(), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Payment::getId).containsExactly(kept.getId());
    }

    @Test
    void paymentsOutsideTheWindowAreLeftOut() {
        payment(customer, "100.00", PaymentMethod.CASH);
        LocalDateTime now = LocalDateTime.now();

        assertThat(paymentRepository.findCompletedBetween(
                now.minusDays(3), now.minusDays(2), PageRequest.of(0, 10)).getContent()).isEmpty();
        assertThat(paymentRepository.findCompletedBetween(
                now.plusDays(2), now.plusDays(3), PageRequest.of(0, 10)).getContent()).isEmpty();
    }

    @Test
    void theWindowIncludesItsStartAndExcludesItsEnd() {
        Payment payment = payment(customer, "100.00", PaymentMethod.CASH);
        LocalDateTime boundary = payment.getCreatedAt().truncatedTo(ChronoUnit.SECONDS);

        assertThat(paymentRepository.findCompletedBetween(
                boundary, boundary.plusHours(1), PageRequest.of(0, 10)).getContent()).hasSize(1);
        assertThat(paymentRepository.findCompletedBetween(
                boundary.minusHours(1), boundary, PageRequest.of(0, 10)).getContent()).isEmpty();
    }

    @Test
    void summarisesCompletedPaymentsPerMethod() {
        payment(customer, "100.00", PaymentMethod.CASH);
        payment(customer, "250.50", PaymentMethod.CASH);
        payment(customer, "75.00", PaymentMethod.EFT);
        Payment voidedCard = payment(customer, "999.00", PaymentMethod.CARD);
        voidedCard.voidPayment(staff);
        paymentRepository.save(voidedCard);

        List<PaymentMethodTotal> totals = paymentRepository.summarizeCompletedBetween(anHourAgo(), inAnHour());

        assertThat(totals).extracting(PaymentMethodTotal::method)
                .containsExactly(PaymentMethod.CASH, PaymentMethod.EFT);
        assertThat(totals).extracting(PaymentMethodTotal::count).containsExactly(2L, 1L);
        assertThat(totals.get(0).total()).isEqualByComparingTo("350.50");
        assertThat(totals.get(1).total()).isEqualByComparingTo("75.00");
    }

    @Test
    void aWindowWithNoPaymentsHasAnEmptySummary() {
        payment(customer, "100.00", PaymentMethod.CASH);
        LocalDateTime now = LocalDateTime.now();

        assertThat(paymentRepository.summarizeCompletedBetween(now.minusDays(3), now.minusDays(2))).isEmpty();
    }

    @Test
    void referenceSequenceKeepsIncreasing() {
        Long first = paymentRepository.nextReferenceSequence();
        Long second = paymentRepository.nextReferenceSequence();

        assertThat(second).isGreaterThan(first);
    }

    private LocalDateTime anHourAgo() {
        return LocalDateTime.now().minusHours(1);
    }

    private LocalDateTime inAnHour() {
        return LocalDateTime.now().plusHours(1);
    }
}