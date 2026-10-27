package com.lundi_m.spazacred_api.payment.service;

import com.lundi_m.spazacred_api.audit.service.AuditLogService;
import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import com.lundi_m.spazacred_api.credit.entity.TransactionStatus;
import com.lundi_m.spazacred_api.credit.repository.CreditAccountRepository;
import com.lundi_m.spazacred_api.credit.repository.CreditTransactionRepository;
import com.lundi_m.spazacred_api.credit.service.CreditAccountService;
import com.lundi_m.spazacred_api.customer.entity.Customer;
import com.lundi_m.spazacred_api.customer.repository.CustomerRepository;
import com.lundi_m.spazacred_api.exception.*;
import com.lundi_m.spazacred_api.payment.dto.*;
import com.lundi_m.spazacred_api.payment.entity.Payment;
import com.lundi_m.spazacred_api.payment.entity.PaymentAllocation;
import com.lundi_m.spazacred_api.payment.entity.PaymentStatus;
import com.lundi_m.spazacred_api.payment.repository.*;
import com.lundi_m.spazacred_api.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository paymentAllocationRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final CreditAccountRepository creditAccountRepository;
    private final CustomerRepository customerRepository;
    private final CreditAccountService creditAccountService;
    private final AuditLogService auditLogService;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    @Transactional
    public PaymentResponse recordPayment(Long customerId, RecordPaymentRequest request) {
        SpazaCredUser currentUser = currentUserProvider.getCurrentUser().getUser();

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        CreditAccount account = creditAccountRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new CreditAccountNotFoundException(customerId));

        BigDecimal outstanding = creditAccountService.getOutstandingBalance(account.getId());
        if (request.amount().compareTo(outstanding) > 0) {
            throw new PaymentExceedsOutstandingBalanceException(request.amount(), outstanding);
        }

        String referenceNumber = generateReferenceNumber();
        Payment payment = new Payment(
                referenceNumber,
                customer,
                currentUser,
                request.amount(),
                request.paymentMethod());
        payment = paymentRepository.save(payment);

        List<PaymentAllocationResponse> allocationResponses = allocate(payment, account, request.amount());

        Map<String, Object> details = Map.of(
                "referenceNumber", referenceNumber,
                "amount", request.amount() ,
                "paymentMethod", request.paymentMethod()
        );
        auditLogService.record(
                currentUser,
                "PAYMENT_RECORDED",
                "Payment",
                payment.getId(),
                details
        );

        return toResponse(payment, customerId, allocationResponses);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPayments(Long customerId, Pageable pageable) {
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException(customerId);
        }

        return paymentRepository.findByCustomer_Id(customerId, pageable)
                .map(payment -> toResponse(payment, customerId, allocationsFor(payment)));
    }

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'ADMIN')")
    @Transactional
    public PaymentResponse voidPayment(Long paymentId, VoidPaymentRequest request) {
        SpazaCredUser currentUser = currentUserProvider.getCurrentUser().getUser();

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() == PaymentStatus.VOIDED) {
            throw new PaymentNotVoidableException("Payment " + paymentId + " is already voided");
        }

        List<PaymentAllocation> activeAllocations = paymentAllocationRepository
                .findByPayment_IdAndReversedAtIsNull(paymentId);

        activeAllocations.forEach(allocation -> allocation
                .reverse(currentUser));

        payment.voidPayment(currentUser);

        auditLogService.record(
                currentUser,
                "PAYMENT_VOIDED",
                "Payment",
                payment.getId(),
                Map.of("reason", request.reason())
        );
        return toResponse(payment, payment.getCustomer().getId(), allocationsFor(payment));
    }

    private List<PaymentAllocationResponse> allocate(Payment payment,
                                                     CreditAccount account,
                                                     BigDecimal amountToAllocate)  {
        List<CreditTransaction> outstandingTransactions = creditTransactionRepository
                .findByCreditAccount_IdAndStatusOrderByDueDateAscCreatedAtAscIdAsc(
                        account.getId(),
                        TransactionStatus.ACTIVE
                );

        List<PaymentAllocationResponse> responses = new ArrayList<>();
        BigDecimal remainingToAllocate = amountToAllocate;

        for (CreditTransaction transaction : outstandingTransactions) {
            if (remainingToAllocate.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal alreadyAllocated = paymentAllocationRepository
                    .sumActiveAllocationsByTransactionId(transaction.getId());

            BigDecimal transactionRemaining = transaction.getAmount().subtract(alreadyAllocated);

            if (transactionRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal amountForThisTransaction = remainingToAllocate.min(transactionRemaining);

            PaymentAllocation allocation = new PaymentAllocation(payment, transaction, amountForThisTransaction);
            paymentAllocationRepository.save(allocation);

            responses.add(new PaymentAllocationResponse(
                    transaction.getId(), transaction.getReferenceNumber(), amountForThisTransaction)
            );

            remainingToAllocate = remainingToAllocate.subtract(amountForThisTransaction);
        }

        return responses;

    }

    private List<PaymentAllocationResponse> allocationsFor(Payment payment) {
        return paymentAllocationRepository.findByPayment_IdAndReversedAtIsNull(payment.getId()).stream()
                .map(a -> new PaymentAllocationResponse(
                        a.getCreditTransaction().getId(),
                        a.getCreditTransaction().getReferenceNumber(),
                        a.getAmountAllocated()))
                .toList();
    }

    private String generateReferenceNumber() {
        Long seq = paymentRepository.nextReferenceSequence();
        return String.format("PAY-%d-%06d", Year.now(clock).getValue(), seq);
    }

    private PaymentResponse toResponse(Payment payment, Long customerId, List<PaymentAllocationResponse> allocations) {
        return new PaymentResponse(
                payment.getId(),
                payment.getReferenceNumber(),
                customerId,
                payment.getAmount(),
                payment.getPaymentMethod().name(),
                payment.getStatus().name(),
                payment.getCreatedAt(),
                allocations
        );
    }
}
