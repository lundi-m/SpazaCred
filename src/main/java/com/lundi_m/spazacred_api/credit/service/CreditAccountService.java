package com.lundi_m.spazacred_api.credit.service;

import com.lundi_m.spazacred_api.credit.repository.CreditAccountRepository;
import com.lundi_m.spazacred_api.credit.repository.CreditTransactionRepository;
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

    @Transactional(readOnly = true)
    public BigDecimal getOutstandingBalance(Long accountId) {
        BigDecimal totalTransactions = creditTransactionRepository.sumActiveAmountsByAccountId(accountId);
        BigDecimal totalAllocated = paymentAllocationRepository.sumActiveAllocationsByAccountId(accountId);

        return totalTransactions.subtract(totalAllocated);
    }
}
