package com.lundi_m.spazacred_api.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateCustomerRequest(

        @Schema(description = "Customer's first name", example = "Thabo")
        @NotBlank(message = "first name is required")
        String firstName,

        @Schema(description = "Customer's middle name, if applicable", example = "Kabelo")
        String middleName,

        @Schema(description = "Customer's last name", example = "Mokoena")
        @NotBlank(message = "last name is required")
        String lastName,

        @Schema(description = "Contact phone number. 9 to 15 digits, optionally starting with +",
                example = "+27821234567")
        @NotBlank(message = "phone is required")
        @Pattern(regexp = "^\\+?[0-9]{9,15}$",
                message = "phone number must be 9 to 15 digits, optionally starting with +")
        String phone,

        @Schema(description = "Customer's email address", example = "thabo.mokoena@gmail.com")
        @Email(message = "email must be a valid mail address")
        String email,

        @Schema(description = "Customer's residential or contact address",
                example = "123 Main Street, Polokwane")
        @NotBlank(message = "address is required")
        String address
) {}
