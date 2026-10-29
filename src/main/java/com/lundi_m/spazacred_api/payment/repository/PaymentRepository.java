package com.lundi_m.spazacred_api.payment.repository;

import com.lundi_m.spazacred_api.payment.dto.PaymentMethodTotal;
import com.lundi_m.spazacred_api.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByCustomer_Id(Long customerId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :paymentId")
    Optional<Payment> findByIdForUpdate(@Param("paymentId") Long paymentId);

    @Query(value = "SELECT nextval('payment_ref_seq')", nativeQuery = true)
    Long nextReferenceSequence();

    @Query(value = """
        SELECT p FROM Payment p
        JOIN FETCH p.customer
        WHERE p.status = com.lundi_m.spazacred_api.payment.entity.PaymentStatus.COMPLETED
            AND p.createdAt >= :from AND p.createdAt < :to
        ORDER BY p.createdAt DESC, p.id DESC""",
            countQuery = """
        SELECT COUNT(p) FROM Payment p
        WHERE p.status = com.lundi_m.spazacred_api.payment.entity.PaymentStatus.COMPLETED
            AND p.createdAt >= :from AND p.createdAt < :to
    """)
    Page<Payment> findCompletedBetween(@Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to,
                                       Pageable pageable
    );

    @Query("""
        SELECT new com.lundi_m.spazacred_api.payment.dto.PaymentMethodTotal(
            p.paymentMethod, COUNT(p), SUM(p.amount))
        FROM Payment p
        WHERE p.status = com.lundi_m.spazacred_api.payment.entity.PaymentStatus.COMPLETED
            AND p.createdAt >= :from AND p.createdAt < :to
        GROUP BY p.paymentMethod
        ORDER BY p.paymentMethod
    """)
    List<PaymentMethodTotal> summarizeCompletedBetween(@Param("from") LocalDateTime from,
                                                       @Param("to") LocalDateTime to
    );
}
