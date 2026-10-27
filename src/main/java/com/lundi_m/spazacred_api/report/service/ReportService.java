package com.lundi_m.spazacred_api.report.service;

import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import com.lundi_m.spazacred_api.credit.repository.CreditAccountRepository;
import com.lundi_m.spazacred_api.credit.repository.CreditTransactionRepository;
import com.lundi_m.spazacred_api.credit.service.CreditAccountService;
import com.lundi_m.spazacred_api.customer.entity.Customer;
import com.lundi_m.spazacred_api.exception.InvalidDateRangeException;
import com.lundi_m.spazacred_api.payment.dto.PaymentMethodTotal;
import com.lundi_m.spazacred_api.payment.repository.PaymentAllocationRepository;
import com.lundi_m.spazacred_api.payment.repository.PaymentRepository;
import com.lundi_m.spazacred_api.report.dto.OutstandingReportRow;
import com.lundi_m.spazacred_api.report.dto.OverdueReportRow;
import com.lundi_m.spazacred_api.report.dto.PaymentReportResponse;
import com.lundi_m.spazacred_api.report.dto.PaymentReportRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final CreditTransactionRepository creditTransactionRepository;
    private final CreditAccountRepository creditAccountRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final CreditAccountService creditAccountService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<OverdueReportRow> overdue(int minDaysOverdue, Pageable pageable) {
        LocalDate today = LocalDate.now(clock);
        LocalDate cutoff = today.minusDays(Math.max(minDaysOverdue, 1));

        return creditTransactionRepository.findOverduePage(cutoff, withoutSort(pageable))
                .map(transaction -> toOverdueRow(transaction, today));
    }

    @Transactional(readOnly = true)
    public Page<OutstandingReportRow> outstandingByCustomer(Pageable pageable) {
        return creditAccountRepository.findWithOutstandingBalance(withoutSort(pageable))
                .map(account -> {
                    BigDecimal outstanding = creditAccountService.getOutstandingBalance(account.getId());
                    Customer customer = account.getCustomer();
                    return new OutstandingReportRow(
                            customer.getId(),
                            fullName(customer),
                            customer.getPhone(),
                            account.getStatus().name(),
                            account.getCreditLimit(),
                            outstanding,
                            account.getCreditLimit().subtract(outstanding)
                    );
                });
    }

    @Transactional(readOnly = true)
    public PaymentReportResponse payments(LocalDate from, LocalDate to, Pageable pageable) {
        LocalDate start = from != null ? from : LocalDate.now(clock).withDayOfMonth(1);
        LocalDate end = to != null ? to : LocalDate.now(clock);
        if (start.isAfter(end)) {
            throw new InvalidDateRangeException();
        }

        LocalDateTime fromTime = start.atStartOfDay();
        LocalDateTime toTime = end.plusDays(1).atStartOfDay();

        List<PaymentMethodTotal> byMethod = paymentRepository.summarizeCompletedBetween(fromTime, toTime);
        long paymentCount = byMethod.stream().mapToLong(PaymentMethodTotal::count).sum();
        BigDecimal totalCollected = byMethod.stream()
                .map(PaymentMethodTotal::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Page<PaymentReportRow> rows = paymentRepository
                .findCompletedBetween(fromTime, toTime, withoutSort(pageable))
                .map(payment -> new PaymentReportRow(
                        payment.getId(),
                        payment.getReferenceNumber(),
                        payment.getCustomer().getId(),
                        fullName(payment.getCustomer()),
                        payment.getAmount(),
                        payment.getPaymentMethod().name(),
                        payment.getCreatedAt()
                ));

        return new PaymentReportResponse(start, end, paymentCount, totalCollected, byMethod, rows);
    }

    private OverdueReportRow toOverdueRow(CreditTransaction transaction, LocalDate today) {
        BigDecimal paid = paymentAllocationRepository.sumActiveAllocationsByTransactionId(transaction.getId());
        Customer customer = transaction.getCreditAccount().getCustomer();
        return new OverdueReportRow(
                transaction.getId(),
                transaction.getReferenceNumber(),
                customer.getId(),
                fullName(customer),
                customer.getPhone(),
                transaction.getDueDate(),
                ChronoUnit.DAYS.between(transaction.getDueDate(), today),
                transaction.getAmount(),
                paid,
                transaction.getAmount().subtract(paid)
        );
    }

    private Pageable withoutSort(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    private String fullName(Customer customer) {
        String middleName = customer.getMiddleName();

        if (middleName == null || middleName.isBlank()) {
            return customer.getFirstName() + " " + customer.getLastName();
        }

        return customer.getFirstName() + " " + middleName + " " + customer.getLastName();
    }
}

