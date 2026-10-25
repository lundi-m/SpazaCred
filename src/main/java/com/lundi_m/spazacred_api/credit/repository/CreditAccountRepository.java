package com.lundi_m.spazacred_api.credit.repository;

import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditAccountRepository extends JpaRepository<CreditAccount, Long> {

    Optional<CreditAccount> findByCustomerId(Long customerID);

    @Query(value = """
        SELECT a FROM CreditAccount a
        JOIN FETCH a.customer c
        WHERE COALESCE((
                SELECT SUM(t.amount) FROM CreditTransaction t
                WHERE t.creditAccount.id = a.id
                    AND t.status = com.lundi_m.spazacred_api.credit.entity.TransactionStatus.ACTIVE), 0)
            > COALESCE((
                SELECT SUM(x.amountAllocated) FROM PaymentAllocation x
                WHERE x.creditTransaction.creditAccount.id = a.id
                    AND x.reversedAt IS NULL), 0)
        ORDER BY c.lastName ASC, c.firstName ASC, a.id ASC""",
            countQuery = """
        SELECT COUNT(a) FROM CreditAccount a
        WHERE COALESCE((
                SELECT SUM(t.amount) FROM CreditTransaction t
                WHERE t.creditAccount.id = a.id
                    AND t.status = com.lundi_m.spazacred_api.credit.entity.TransactionStatus.ACTIVE), 0)
            > COALESCE((
                SELECT SUM(x.amountAllocated) FROM PaymentAllocation x
                WHERE x.creditTransaction.creditAccount.id = a.id
                    AND x.reversedAt IS NULL), 0)
    """)
    Page<CreditAccount> findWithOutstandingBalance(Pageable pageable);

}
