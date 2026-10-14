package com.lundi_m.spazacred_api.credit.service;

import com.lundi_m.spazacred_api.audit.service.AuditLogService;
import com.lundi_m.spazacred_api.credit.dto.CreatePurchaseRequest;
import com.lundi_m.spazacred_api.credit.dto.CreditTransactionResponse;
import com.lundi_m.spazacred_api.credit.dto.VoidTransactionRequest;
import com.lundi_m.spazacred_api.credit.entity.AccountStatus;
import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import com.lundi_m.spazacred_api.credit.entity.TransactionStatus;
import com.lundi_m.spazacred_api.credit.repository.CreditAccountRepository;
import com.lundi_m.spazacred_api.credit.repository.CreditTransactionRepository;
import com.lundi_m.spazacred_api.exception.*;
import com.lundi_m.spazacred_api.payment.repository.PaymentAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

@Service
@RequiredArgsConstructor
public class CreditTransactionService {

    private static final int DEFAULT_CREDIT_TERM_DAYS = 30;

    private final CreditTransactionRepository creditTransactionRepository;
    private final CreditAccountRepository creditAccountRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;

    private final CreditAccountService creditAccountService;
    private final AuditLogService auditLogService;

    @Transactional
    public CreditTransactionResponse createPurchase(Long customerId, CreatePurchaseRequest request) {
        CreditAccount account = creditAccountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidAccountStatusTransitionException(
                    "Only an ACTIVE account can make purchases (current status: " + account.getStatus() + ")"
            );
        }

        BigDecimal outstanding = creditAccountService.getOutstandingBalance(account.getId());
        if (outstanding.add(request.amount()).compareTo(account.getCreditLimit()) > 0) {
            throw new CreditLimitExceededException(outstanding, request.amount(), account.getCreditLimit());
        }

        LocalDate dueDate = request.dueDate() != null
                ? request.dueDate() : LocalDate.now().plusDays(DEFAULT_CREDIT_TERM_DAYS);

        String referenceNumber = generateReferenceNumber();

        CreditTransaction transaction = new CreditTransaction(
                referenceNumber,
                account,
                null,
                request.amount(),
                request.description(),
                dueDate
        );
        transaction = creditTransactionRepository.save(transaction);

        String details = String.format("{\"referenceNumber\":%s,\"amount\":%s, \"dueDate\":\"%s\"}",
                referenceNumber, request.amount(), request.dueDate());
        auditLogService.record(
                null,
                "PURCHASE_CREATED",
                "CreditTransaction",
                transaction.getId(),
                details
        );

        return toResponse(transaction, customerId);
    }

    @Transactional
    public Page<CreditTransactionResponse> getHistory(Long customerId, Pageable pageable) {
        CreditAccount account = creditAccountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        return creditTransactionRepository.findByCreditAccount_Id(account.getId(), pageable)
                .map(transaction -> toResponse(transaction, customerId));
    }

    @Transactional
    public CreditTransactionResponse voidTransaction(Long transactionId, VoidTransactionRequest request) {
        CreditTransaction transaction = creditTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new CreditTransactionNotFoundException(transactionId));

        if (transaction.getStatus() == TransactionStatus.VOIDED) {
            throw new TransactionNotVoidableException("Transaction " + transactionId + " is already voided");
        }

        boolean hasActiveAllocations = paymentAllocationRepository.
                existsByCreditTransaction_IdAndReversedAtIsNull(transactionId);

        if (hasActiveAllocations) {
            throw new TransactionNotVoidableException(
                    "Cannot void transaction " + transactionId +
                            " - it has active payments allocations: void the payment(s) first"
            );
        }

        transaction.voidTransaction(null);

        String details = String.format("{\"referenceNumber\":%s\"}", request.reason());
        auditLogService.record(
                null,
                "TRANSACTION_VOIDED",
                "CreditTransaction",
                transaction.getId(),
                details
        );

        return toResponse(transaction, transaction.getCreditAccount().getCustomer().getId());
    }

    private String generateReferenceNumber() {
        Long seq = creditTransactionRepository.nextReferenceSequence();
        return String.format("CRT-%d-%06d", Year.now().getValue(), seq);
    }

    private CreditTransactionResponse toResponse(CreditTransaction transaction, Long customerId) {
        return new CreditTransactionResponse(
                transaction.getId(),
                transaction.getReferenceNumber(),
                customerId,
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getDueDate(),
                transaction.getStatus().name(),
                transaction.getCreatedAt()
        );
    }
}
