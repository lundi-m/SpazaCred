package com.lundi_m.spazacred_api.credit.repository;

import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditAccountRepository extends JpaRepository<CreditAccount, Long> {

    Optional<CreditAccount> findByCustomerId(Long customerID);

}
