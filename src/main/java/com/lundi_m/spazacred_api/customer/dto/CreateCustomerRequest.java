package com.lundi_m.spazacred_api.customer.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateCustomerRequest(

        @NotBlank(message = "first name is required")
        String firstName,

        String middleName,

        @NotBlank(message = "last name is required")
        String lastName,

        @NotBlank(message = "phone is required")
        @Pattern(regexp = "^\\+?[0-9]{9,15}$",
                message = "phone number must be 9 to 15 digits, optionally starting with +")
        String phone,

        @Email(message = "email must be a valid mail address")
        String email,

        @NotBlank(message = "ID number is required")
        @Pattern(regexp = "^[0-9]{13}$", message = "ID number must be exactly 13 digits")
        String idNumber,

        @NotBlank(message = "address is required")
        String address,

        @NotNull(message = "credit limit is required")
        @DecimalMin(value = "0.00", message = "credit limit cannot be negative")
        @Digits(
                integer = 17,
                fraction = 2,
                message = "amount must have no more than 2 decimal places"
        )
        BigDecimal creditLimit
) {}