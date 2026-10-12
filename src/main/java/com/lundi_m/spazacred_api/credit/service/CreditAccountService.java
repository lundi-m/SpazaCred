package com.lundi_m.spazacred_api.credit.service;

import com.lundi_m.spazacred_api.audit.dto.AdjustLimitRequest;
import com.lundi_m.spazacred_api.audit.dto.CreditAccountResponse;
import com.lundi_m.spazacred_api.audit.service.AuditLogService;
import com.lundi_m.spazacred_api.credit.entity.AccountStatus;
import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.repository.CreditAccountRepository;
import com.lundi_m.spazacred_api.credit.repository.CreditTransactionRepository;
import com.lundi_m.spazacred_api.exception.CreditAccountNotFoundException;
import com.lundi_m.spazacred_api.exception.InvalidAccountStatusTransitionException;
import com.lundi_m.spazacred_api.payment.repository.PaymentAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CreditAccountService {

    private final CreditAccountRepository creditAccountRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;

    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public BigDecimal getOutstandingBalance(Long accountId) {
        BigDecimal totalTransactions = creditTransactionRepository.sumActiveAmountsByAccountId(accountId);
        BigDecimal totalAllocated = paymentAllocationRepository.sumActiveAllocationsByAccountId(accountId);

        return totalTransactions.subtract(totalAllocated);
    }

    @Transactional(readOnly = true)
    public CreditAccountResponse getAccount(Long accountId) {
        CreditAccount account = findAccountOrThrow(accountId);
        return toResponse(account);
    }

    @Transactional
    public CreditAccountResponse adjustCreditLimit(Long customerId, AdjustLimitRequest request) {
        CreditAccount account = findAccountOrThrow(customerId);
        BigDecimal oldLimit = account.getCreditLimit();

        account.setCreditLimit(request.newLimit());

        String details = String.format("{\"oldLimit\":%s,\"newLimit\":%s, \"reason\":\"%s\"}",
                oldLimit, request.newLimit(), request.reason());

        auditLogService.record(null,
                "LIMIT_ADJUSTED",
                "CreditAccount",
                account.getId(),
                details
        );

        return toResponse(account);
    }

    @Transactional
    public CreditAccountResponse suspendAccount(Long customerId) {
        CreditAccount account = findAccountOrThrow(customerId);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountStatusTransitionException(
                    "Only an ACTIVE account can be suspended (current status: " + account.getStatus() + ")"
            );
        }

        account.setStatus(AccountStatus.SUSPENDED);
        auditLogService.record(null,
                "ACCOUNT_SUSPENDED",
                "CreditAccount",
                account.getId(),
                null
        );

        return toResponse(account);
    }

    @Transactional
    public CreditAccountResponse reactivateAccount(Long customerId) {
        CreditAccount account = findAccountOrThrow(customerId);
        if (account.getStatus() != AccountStatus.SUSPENDED) {
            throw new InvalidAccountStatusTransitionException(
                    "Only an SUSPENDED account can be reactivated (current status: " + account.getStatus() + ")"
            );
        }

        account.setStatus(AccountStatus.ACTIVE);
        auditLogService.record(null,
                "ACCOUNT_REACTIVATED",
                "CreditAccount",
                account.getId(),
                null
        );

        return toResponse(account);
    }

    @Transactional
    public CreditAccountResponse closeAccount(Long customerId) {
        CreditAccount account = findAccountOrThrow(customerId);
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new InvalidAccountStatusTransitionException("Account is already closed");
        }

        BigDecimal outstanding = getOutstandingBalance(account.getId());
        if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidAccountStatusTransitionException(
                    "Cannot close an account with an outstanding balance of " + outstanding
            );
        }

        account.setStatus(AccountStatus.CLOSED);
        auditLogService.record(null,
                "ACCOUNT_CLOSED",
                "CreditAccount",
                account.getId(),
                null
        );

        return toResponse(account);
    }

    private CreditAccount findAccountOrThrow(Long customerId) {
        return creditAccountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CreditAccountNotFoundException(customerId)
        );
    }

    private CreditAccountResponse toResponse(CreditAccount account) {
        BigDecimal outstanding = getOutstandingBalance(account.getId());
        BigDecimal available = account.getCreditLimit().subtract(outstanding);

        return new CreditAccountResponse(
                account.getId(),
                account.getCustomer().getId(),
                account.getCreditLimit(),
                account.getStatus().name(),
                outstanding,
                available
        );
    }
}
