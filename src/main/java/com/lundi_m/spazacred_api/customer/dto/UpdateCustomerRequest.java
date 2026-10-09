package com.lundi_m.spazacred_api.customer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record UpdateCustomerRequest (
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

        String address
){}
