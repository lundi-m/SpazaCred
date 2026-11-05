package com.lundi_m.spazacred_api.customer.controller;

import com.lundi_m.spazacred_api.credit.dto.AccountStatusChangeRequest;
import com.lundi_m.spazacred_api.customer.dto.CreateCustomerRequest;
import com.lundi_m.spazacred_api.customer.dto.CustomerResponse;
import com.lundi_m.spazacred_api.customer.dto.UpdateCustomerRequest;
import com.lundi_m.spazacred_api.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;


    @Operation(
            summary = "Create a customer",
            description = """
                Creates a new customer and their associated credit account.
                Requires EMPLOYEE, SHOP_OWNER or ADMIN role.
                Phone number and ID number must be unique.
                The supplied credit limit is assigned to the new credit account.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get customer by ID",
            description = """
                Retrieves a customer's details, including their credit limit
                and credit account status.
                Requires EMPLOYEE, SHOP_OWNER or ADMIN role.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    @Operation(
            summary = "Find customers",
            description = """
                Returns a paginated list of customers.
                An optional search term can be used to filter customers.
                Requires EMPLOYEE, SHOP_OWNER or ADMIN role.
                Supports pagination and sorting.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> findFiltered(
            @Parameter(description = "Optional search term for filtering customers",
                    example = "John")
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(customerService.findFiltered(search, pageable));
    }

    @Operation(
            summary = "Update customer details",
            description = """
                Updates a customer's personal and contact details.
                Requires EMPLOYEE, SHOP_OWNER or ADMIN role.
                This operation does not change the customer's credit limit
                or credit account status.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @Operation(
            summary = "Deactivate a customer",
            description = """
                Deactivates a customer.
                Requires SHOP_OWNER or ADMIN role.
                A reason must be supplied.
                Customers with an outstanding balance cannot be deactivated.
                Deactivation does not delete the customer's records.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateCustomer(@PathVariable Long id,
                                                               @Valid @RequestBody AccountStatusChangeRequest request) {
        customerService.deactivateCustomer(id, request);
        return ResponseEntity.noContent().build();
    }
}
