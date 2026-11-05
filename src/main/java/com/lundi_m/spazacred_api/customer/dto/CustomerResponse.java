package com.lundi_m.spazacred_api.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record CustomerResponse(

        @Schema(description = "Unique customer ID", example = "1")
        Long id,

        @Schema(description = "Customer's first name", example = "Thabo")
        String firstName,

        @Schema(description = "Customer's middle name", example = "Kabelo")
        String middleName,

        @Schema(description = "Customer's last name", example = "Mokoena")
        String lastName,

        @Schema(description = "Customer's phone number", example = "+27821234567")
        String phone,

        @Schema(description = "South African ID number", example = "9001015009087")
        String idNumber,

        @Schema(description = "Customer's email address", example = "thabo.mokoena@gmail.com")
        String email,

        @Schema(description = "Customer's address", example = "123 Main Street, Polokwane")
        String address,

        @Schema(description = "Whether the customer is active", example = "true")
        boolean active,

        @Schema(description = "Customer's credit limit", example = "5000.00")
        BigDecimal creditLimit,

        @Schema(description = "Current credit account status",
                example = "ACTIVE",
                allowableValues = {"ACTIVE", "SUSPENDED", "CLOSED"})
        String accountStatus
) {}