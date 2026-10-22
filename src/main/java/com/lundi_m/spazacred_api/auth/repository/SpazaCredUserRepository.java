package com.lundi_m.spazacred_api.auth.repository;

import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpazaCredUserRepository extends JpaRepository<SpazaCredUser, Long> {

    Optional<SpazaCredUser> findByEmail(String email);
}
