package com.lundi_m.spazacred_api.credit.repository;

import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

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
}
