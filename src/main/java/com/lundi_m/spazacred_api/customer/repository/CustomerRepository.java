package com.lundi_m.spazacred_api.customer.repository;

import com.lundi_m.spazacred_api.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByIdNumber(String idNumber);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    @Query("""
        SELECT c FROM Customer c
        WHERE (CAST(:search AS string) IS NULL OR
               LOWER(c.firstName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
               LOWER(c.middleName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
               LOWER(c.lastName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
               c.phone LIKE CONCAT('%', CAST(:search AS string), '%') OR
               c.idNumber LIKE CONCAT('%', CAST(:search AS string), '%') OR
               LOWER(c.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
        """)
    Page<Customer> findFiltered(
            @Param("search") String search,
            Pageable pageable
    );
}
