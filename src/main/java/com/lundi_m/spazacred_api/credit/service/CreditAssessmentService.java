package com.lundi_m.spazacred_api.credit.service;

import com.lundi_m.spazacred_api.credit.dto.CreditAssessmentResponse;
import com.lundi_m.spazacred_api.credit.entity.AccountStatus;
import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import com.lundi_m.spazacred_api.credit.repository.CreditAccountRepository;
import com.lundi_m.spazacred_api.credit.repository.CreditTransactionRepository;
import com.lundi_m.spazacred_api.exception.CreditAccountNotFoundException;
import com.lundi_m.spazacred_api.payment.repository.PaymentAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditAssessmentService {

    private final CreditAccountRepository creditAccountRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final CreditAccountService creditAccountService;

    @Transactional(readOnly = true)
    public CreditAssessmentResponse assess(Long customerId) {
        CreditAccount account = creditAccountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CreditAccountNotFoundException(customerId));

        LocalDate today = LocalDate.now();
        List<CreditTransaction> overdue =
                creditTransactionRepository.findOverdueByAccountId(account.getId(), today);

        BigDecimal overdueAmount = BigDecimal.ZERO;
        for (CreditTransaction transaction : overdue) {
            BigDecimal paid = paymentAllocationRepository
                    .sumActiveAllocationsByTransactionId(transaction.getId());
            overdueAmount = overdueAmount.add(transaction.getAmount().subtract(paid));
        }

        Long oldestOverdueDays = overdue.isEmpty()
                ? null
                : ChronoUnit.DAYS.between(overdue.getFirst().getDueDate(), today);

        BigDecimal outstanding = creditAccountService.getOutstandingBalance(account.getId());
        BigDecimal available = account.getCreditLimit().subtract(outstanding);

        List<String> reasons = new ArrayList<>();
        if (!account.getCustomer().isActive()) {
            reasons.add("Customer is deactivated");
        }
        if (account.getStatus() != AccountStatus.ACTIVE) {
            reasons.add("Account is " + account.getStatus());
        }
        if (!overdue.isEmpty()) {
            reasons.add(overdue.size() + " overdue transaction(s) totalling " + overdueAmount);
        }
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            reasons.add("No available credit");
        }

        return new CreditAssessmentResponse(
                customerId,
                reasons.isEmpty(),
                reasons,
                account.getStatus().name(),
                account.getCreditLimit(),
                outstanding,
                available,
                overdue.size(),
                overdueAmount,
                oldestOverdueDays
        );
    }
}
