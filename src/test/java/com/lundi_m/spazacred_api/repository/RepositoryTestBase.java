package com.lundi_m.spazacred_api.repository;

import com.lundi_m.spazacred_api.TestContainersConfiguration;
import com.lundi_m.spazacred_api.auth.entity.Role;
import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.auth.repository.SpazaCredUserRepository;
import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import com.lundi_m.spazacred_api.credit.repository.CreditAccountRepository;
import com.lundi_m.spazacred_api.credit.repository.CreditTransactionRepository;
import com.lundi_m.spazacred_api.customer.entity.Customer;
import com.lundi_m.spazacred_api.customer.repository.CustomerRepository;
import com.lundi_m.spazacred_api.payment.entity.Payment;
import com.lundi_m.spazacred_api.payment.entity.PaymentAllocation;
import com.lundi_m.spazacred_api.payment.entity.PaymentMethod;
import com.lundi_m.spazacred_api.payment.repository.PaymentAllocationRepository;
import com.lundi_m.spazacred_api.payment.repository.PaymentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@Import(TestContainersConfiguration.class)
@Transactional
public abstract class RepositoryTestBase {

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    @Autowired protected SpazaCredUserRepository userRepository;
    @Autowired protected CustomerRepository customerRepository;
    @Autowired protected CreditAccountRepository creditAccountRepository;
    @Autowired protected CreditTransactionRepository creditTransactionRepository;
    @Autowired protected PaymentRepository paymentRepository;
    @Autowired protected PaymentAllocationRepository paymentAllocationRepository;
    @Autowired protected EntityManager entityManager;

    protected SpazaCredUser staff;

    @BeforeEach
    void createStaffUser() {
        int n = COUNTER.getAndIncrement();
        staff = userRepository.save(new SpazaCredUser(
                "Test", null, "Staff", "staff" + n + "@test.local", "hash", Role.EMPLOYEE));
    }

    protected Customer customer(String firstName, String lastName) {
        int n = COUNTER.getAndIncrement();
        return customerRepository.save(new Customer(firstName, null, lastName,
                String.format("08%08d", n), null, String.format("%013d", n), "1 Test Street"));
    }

    protected CreditAccount account(Customer customer, String limit) {
        return creditAccountRepository.save(new CreditAccount(customer, new BigDecimal(limit)));
    }

    protected CreditTransaction debt(CreditAccount account, String amount, LocalDate dueDate) {
        int n = COUNTER.getAndIncrement();
        return creditTransactionRepository.save(new CreditTransaction(
                String.format("CRT-TEST-%06d", n), account, staff,
                new BigDecimal(amount), "groceries", dueDate));
    }

    protected CreditTransaction voided(CreditTransaction debt) {
        debt.voidTransaction(staff);
        return creditTransactionRepository.save(debt);
    }

    protected Payment payment(Customer customer, String amount, PaymentMethod method) {
        int n = COUNTER.getAndIncrement();
        return paymentRepository.save(new Payment(
                String.format("PAY-TEST-%06d", n), customer, staff, new BigDecimal(amount), method));
    }

    protected PaymentAllocation allocate(Payment payment, CreditTransaction debt, String amount) {
        return paymentAllocationRepository.save(new PaymentAllocation(payment, debt, new BigDecimal(amount)));
    }

    protected PaymentAllocation reversed(PaymentAllocation allocation) {
        allocation.reverse(staff);
        return paymentAllocationRepository.save(allocation);
    }
}
