package com.lundi_m.spazacred_api.payment.repository;

import com.lundi_m.spazacred_api.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Page<Payment> findByCustomer_Id(Long customerId, Pageable pageable);

    @Query(value = "SELECT nextval('payment_ref_seq')", nativeQuery = true)
    Long nextReferenceSequence();
}
