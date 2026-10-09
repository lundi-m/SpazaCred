package com.lundi_m.spazacred_api.credit.repository;

import com.lundi_m.spazacred_api.credit.entity.CreditTransaction;
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
            AND t.status = com.lundi_m.spazacred_api.credit.entity.AccountStatus.ACTIVE
    """)
    BigDecimal sumActiveAmountsByAccountId(@Param("accountId") Long accountId);
}
