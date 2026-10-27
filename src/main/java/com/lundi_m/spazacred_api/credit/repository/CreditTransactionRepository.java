package com.lundi_m.spazacred_api.credit.repository;

import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import com.lundi_m.spazacred_api.credit.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM CreditTransaction t
        WHERE t.creditAccount.id = :accountId
            AND t.status = com.lundi_m.spazacred_api.credit.entity.TransactionStatus.ACTIVE
    """)
    BigDecimal sumActiveAmountsByAccountId(@Param("accountId") Long accountId);

    @Query(value = "SELECT nextval('credit_transaction_ref_seq')", nativeQuery = true)
    Long nextReferenceSequence();

    Page<CreditTransaction> findByCreditAccount_Id(Long creditAccount, Pageable pageable);

    List<CreditTransaction> findByCreditAccount_IdAndStatusOrderByDueDateAscCreatedAtAscIdAsc(
            Long creditAccountId, TransactionStatus status
    );

    @Query("""
        SELECT t FROM CreditTransaction t
        WHERE t.creditAccount.id = :accountId
            AND t.status = com.lundi_m.spazacred_api.credit.entity.TransactionStatus.ACTIVE
            AND t.dueDate < :today
            AND t.amount > COALESCE((
                SELECT SUM(a.amountAllocated)
                FROM PaymentAllocation a
                WHERE a.creditTransaction.id = t.id
                    AND a.reversedAt IS NULL), 0)
        ORDER BY t.dueDate ASC
    """)
    List<CreditTransaction> findOverdueByAccountId(@Param("accountId") Long accountId,
                                                   @Param("today") LocalDate today
    );

    @Query(value = """
        SELECT t FROM CreditTransaction t
        JOIN FETCH t.creditAccount a
        JOIN FETCH a.customer c
        WHERE t.status = com.lundi_m.spazacred_api.credit.entity.TransactionStatus.ACTIVE
            AND t.dueDate <= :cutoff
            AND t.amount > COALESCE((
                SELECT SUM(x.amountAllocated)
                FROM PaymentAllocation x
                WHERE x.creditTransaction.id = t.id
                    AND x.reversedAt IS NULL), 0)
        ORDER BY t.dueDate ASC, t.id ASC""",
            countQuery = """
        SELECT COUNT(t) FROM CreditTransaction t
        WHERE t.status = com.lundi_m.spazacred_api.credit.entity.TransactionStatus.ACTIVE
            AND t.dueDate <= :cutoff
            AND t.amount > COALESCE((
                SELECT SUM(x.amountAllocated)
                FROM PaymentAllocation x
                WHERE x.creditTransaction.id = t.id
                    AND x.reversedAt IS NULL), 0)
    """)
    Page<CreditTransaction> findOverduePage(@Param("cutoff") LocalDate cutoff, Pageable pageable);

    @Query("""
    SELECT DISTINCT t.creditAccount.id
    FROM CreditTransaction t
    WHERE t.creditAccount.status = com.lundi_m.spazacred_api.credit.entity.AccountStatus.ACTIVE
        AND t.status = com.lundi_m.spazacred_api.credit.entity.TransactionStatus.ACTIVE
        AND t.dueDate < :cutoff
        AND t.amount > COALESCE((
            SELECT SUM(a.amountAllocated)
            FROM PaymentAllocation a
            WHERE a.creditTransaction.id = t.id
                AND a.reversedAt IS NULL), 0)
    """)
    List<Long> findActiveAccountIdsWithDebtDueBefore(@Param("cutoff") LocalDate cutoff);
}
