package com.lundi_m.spazacred_api.auth.repository;

import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpazaCredRepository extends JpaRepository<SpazaCredUser, Long> {

    Optional<SpazaCredUser> findByEmail(String email);
}
