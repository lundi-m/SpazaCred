package com.lundi_m.spazacred_api.payment.repository;

import com.lundi_m.spazacred_api.payment.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {

    @Query("""
        SELECT COALESCE(SUM(a.amountAllocated), 0)
        FROM PaymentAllocation a
        WHERE a.creditTransaction.creditAccount.id = :accountId
            AND a.reversedAt IS NULL
    """)
    BigDecimal sumActiveAllocationsByAccountId(@Param("accountId") Long accountId);

    @Query("""
        SELECT COALESCE(SUM(a.amountAllocated), 0)
        FROM PaymentAllocation a
        WHERE a.creditTransaction.id = :transactionId
            AND a.reversedAt IS NULL
    """)
    BigDecimal sumActiveAllocationsByTransactionId(@Param("transactionId") Long transactionId);

    boolean existsByCreditTransaction_IdAndReversedAtIsNull(Long creditTransactionId);

    List<PaymentAllocation> findByPayment_IdAndReversedAtIsNull(Long paymentId);
}
