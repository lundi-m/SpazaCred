package com.lundi_m.spazacred_api.customer.dto;

import java.math.BigDecimal;

public record CustomerResponse(
        Long id,
        String firstName,
        String middleName,
        String lastName,
        String phone,
        String idNumber,
        String email,
        String address,
        boolean active,
        BigDecimal creditLimit,
        String accountStatus
) {}
