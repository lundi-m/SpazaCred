package com.lundi_m.spazacred_api.credit.service;

import com.lundi_m.spazacred_api.audit.service.AuditLogService;
import com.lundi_m.spazacred_api.credit.entity.AccountStatus;
import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import com.lundi_m.spazacred_api.credit.repository.CreditAccountRepository;
import com.lundi_m.spazacred_api.credit.repository.CreditTransactionRepository;
import com.lundi_m.spazacred_api.payment.repository.PaymentAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OverdueAccountService {

    private final CreditAccountRepository creditAccountRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final AuditLogService auditLogService;
    private final Clock clock;

    @Value("${credit.overdue-grace-days}")
    private int graceDays;

    @Transactional(readOnly = true)
    public List<Long> findAccountIdsToSuspend() {
        return creditTransactionRepository.findActiveAccountIdsWithDebtDueBefore(cutoffDate());
    }

    @Transactional
    public boolean suspendIfStillOverdue(Long accountId) {
        CreditAccount account = creditAccountRepository.findByIdForUpdate(accountId).orElse(null);
        if (account == null || account.getStatus() != AccountStatus.ACTIVE) {
            return false;
        }

        List<CreditTransaction> overdue =
                creditTransactionRepository.findOverdueByAccountId(accountId, cutoffDate());
        if (overdue.isEmpty()) {
            return false;
        }

        BigDecimal overdueAmount = BigDecimal.ZERO;
        for (CreditTransaction transaction : overdue) {
            BigDecimal paid = paymentAllocationRepository
                    .sumActiveAllocationsByTransactionId(transaction.getId());
            overdueAmount = overdueAmount.add(transaction.getAmount().subtract(paid));
        }
        long oldestOverdueDays =
                ChronoUnit.DAYS.between(overdue.getFirst().getDueDate(), LocalDate.now(clock));

        account.setStatus(AccountStatus.SUSPENDED);

        auditLogService.recordSystemAction(
                "ACCOUNT_AUTO_SUSPENDED",
                "CreditAccount",
                account.getId(),
                Map.of(
                        "reason", "OVERDUE_DEBT",
                        "graceDays", graceDays,
                        "overdueTransactions", overdue.size(),
                        "overdueAmount", overdueAmount,
                        "oldestOverdueDays", oldestOverdueDays
                )
        );

        return true;
    }

    private LocalDate cutoffDate() {
        return LocalDate.now(clock).minusDays(graceDays);
    }
}
