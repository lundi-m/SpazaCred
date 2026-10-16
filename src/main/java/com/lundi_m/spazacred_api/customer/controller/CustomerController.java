package com.lundi_m.spazacred_api.customer.controller;

import com.lundi_m.spazacred_api.credit.dto.AccountStatusChangeRequest;
import com.lundi_m.spazacred_api.customer.dto.CreateCustomerRequest;
import com.lundi_m.spazacred_api.customer.dto.CustomerResponse;
import com.lundi_m.spazacred_api.customer.dto.UpdateCustomerRequest;
import com.lundi_m.spazacred_api.customer.service.CustomerService;
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

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> findFiltered(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(customerService.findFiltered(search, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable Long id,
                                                           @Valid @RequestBody UpdateCustomerRequest request) {
        return ResponseEntity.ok(customerService.updateCustomer(id, request));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<CustomerResponse> deactivateCustomer(@PathVariable Long id,
                                                               @Valid @RequestBody AccountStatusChangeRequest request) {
        customerService.deactivateCustomer(id, request);
        return ResponseEntity.noContent().build();
    }
}
