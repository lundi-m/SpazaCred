package com.lundi_m.spazacred_api.credit.service;

import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.credit.dto.AdjustLimitRequest;
import com.lundi_m.spazacred_api.credit.dto.CreditAccountResponse;
import com.lundi_m.spazacred_api.audit.service.AuditLogService;
import com.lundi_m.spazacred_api.credit.dto.AccountStatusChangeRequest;
import com.lundi_m.spazacred_api.credit.entity.AccountStatus;
import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.repository.CreditAccountRepository;
import com.lundi_m.spazacred_api.credit.repository.CreditTransactionRepository;
import com.lundi_m.spazacred_api.exception.CreditAccountNotFoundException;
import com.lundi_m.spazacred_api.exception.InvalidAccountStatusTransitionException;
import com.lundi_m.spazacred_api.payment.repository.PaymentAllocationRepository;
import com.lundi_m.spazacred_api.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CreditAccountService {

    private final CreditAccountRepository creditAccountRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final AuditLogService auditLogService;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    public BigDecimal getOutstandingBalance(Long accountId) {
        BigDecimal totalTransactions = creditTransactionRepository.sumActiveAmountsByAccountId(accountId);
        BigDecimal totalAllocated = paymentAllocationRepository.sumActiveAllocationsByAccountId(accountId);

        return totalTransactions.subtract(totalAllocated);
    }

    @Transactional(readOnly = true)
    public CreditAccountResponse getAccount(Long customerId) {
        CreditAccount account = findAccountOrThrow(customerId);
        return toResponse(account);
    }

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'ADMIN')")
    @Transactional
    public CreditAccountResponse adjustCreditLimit(Long customerId, AdjustLimitRequest request) {
        SpazaCredUser currentUser = currentUserProvider.getCurrentUser().getUser();

        CreditAccount account = findAccountOrThrow(customerId);
        BigDecimal oldLimit = account.getCreditLimit();

        account.setCreditLimit(request.newLimit());

        Map<String, Object> details = Map.of(
                "oldLimit", oldLimit,
                "newLimit", request.newLimit(),
                "reason", request.reason()
        );

        auditLogService.record(
                currentUser,
                "LIMIT_ADJUSTED",
                "CreditAccount",
                account.getId(),
                details
        );

        return toResponse(account);
    }

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'ADMIN')")
    @Transactional
    public CreditAccountResponse suspendAccount(Long customerId, AccountStatusChangeRequest request) {
        SpazaCredUser currentUser = currentUserProvider.getCurrentUser().getUser();

        CreditAccount account = findAccountOrThrow(customerId);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountStatusTransitionException(
                    "Only an ACTIVE account can be suspended (current status: " + account.getStatus() + ")"
            );
        }

        account.setStatus(AccountStatus.SUSPENDED);
        Map<String, Object> details = Map.of("reason", request.reason());
        auditLogService.record(
                currentUser,
                "ACCOUNT_SUSPENDED",
                "CreditAccount",
                account.getId(),
                details
        );

        return toResponse(account);
    }

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'ADMIN')")
    @Transactional
    public CreditAccountResponse reactivateAccount(Long customerId, AccountStatusChangeRequest request) {
        SpazaCredUser currentUser = currentUserProvider.getCurrentUser().getUser();

        CreditAccount account = findAccountOrThrow(customerId);
        if (account.getStatus() != AccountStatus.SUSPENDED) {
            throw new InvalidAccountStatusTransitionException(
                    "Only an SUSPENDED account can be reactivated (current status: " + account.getStatus() + ")"
            );
        }

        account.setStatus(AccountStatus.ACTIVE);
        Map<String, Object> details = Map.of("reason", request.reason());
        auditLogService.record(
                currentUser,
                "ACCOUNT_REACTIVATED",
                "CreditAccount",
                account.getId(),
                details
        );

        return toResponse(account);
    }

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'ADMIN')")
    @Transactional
    public CreditAccountResponse closeAccount(Long customerId, AccountStatusChangeRequest request) {
        SpazaCredUser currentUser = currentUserProvider.getCurrentUser().getUser();

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
        Map<String, Object> details = Map.of("reason", request.reason());
        auditLogService.record(
                currentUser,
                "ACCOUNT_CLOSED",
                "CreditAccount",
                account.getId(),
                details
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
