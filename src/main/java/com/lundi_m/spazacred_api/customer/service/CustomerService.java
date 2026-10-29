package com.lundi_m.spazacred_api.customer.service;

import com.lundi_m.spazacred_api.audit.service.AuditLogService;
import com.lundi_m.spazacred_api.auth.entity.SpazaCredUser;
import com.lundi_m.spazacred_api.credit.dto.AccountStatusChangeRequest;
import com.lundi_m.spazacred_api.credit.entity.CreditAccount;
import com.lundi_m.spazacred_api.credit.repository.CreditAccountRepository;
import com.lundi_m.spazacred_api.credit.service.CreditAccountService;
import com.lundi_m.spazacred_api.customer.dto.CreateCustomerRequest;
import com.lundi_m.spazacred_api.customer.dto.CustomerResponse;
import com.lundi_m.spazacred_api.customer.dto.UpdateCustomerRequest;
import com.lundi_m.spazacred_api.customer.entity.Customer;
import com.lundi_m.spazacred_api.customer.repository.CustomerRepository;
import com.lundi_m.spazacred_api.exception.*;
import com.lundi_m.spazacred_api.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CreditAccountRepository creditAccountRepository;
    private final CreditAccountService accountService;
    private final AuditLogService auditLogService;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        SpazaCredUser currentUser = currentUserProvider.getCurrentUser().getUser();

        if (customerRepository.existsByIdNumber(request.idNumber())) {
            throw new DuplicateIdNumberException(request.idNumber());
        }

        String email = request.email() == null
                ? null
                : request.email().trim();

        if (email != null && !email.isBlank()
                && customerRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        if (customerRepository.existsByPhone(request.phone())) {
            throw new DuplicatePhoneException(request.phone());
        }

        Customer customer = new Customer(
                request.firstName(),
                request.middleName(),
                request.lastName(),
                request.phone(),
                request.email(),
                request.idNumber(),
                request.address()
        );
        customer = customerRepository.save(customer);
        auditLogService.record(
                currentUser,
                "CUSTOMER_CREATED",
                "Customer",
                customer.getId(),
                null
        );

        CreditAccount account = new CreditAccount(customer, request.creditLimit());
        account = creditAccountRepository.save(account);

        return toResponse(customer, account);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long id) {
        Customer customer = findCustomerOrThrow(id);
        CreditAccount account = findAccountOrThrow(id);
        return toResponse(customer, account);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> findFiltered(String search, Pageable pageable) {
        String searchTerm = (search == null || search.isBlank())
                ? null
                : search.trim();

        Page<Customer> customers = searchTerm == null
                ? customerRepository.findAll(pageable)
                : customerRepository.findFiltered(searchTerm, pageable);

        return customers.map(customer -> {
            CreditAccount account = findAccountOrThrow(customer.getId());
            return toResponse(customer, account);
        });
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        SpazaCredUser currentUser = currentUserProvider.getCurrentUser().getUser();

        Customer customer = findCustomerOrThrow(id);

        if (request.email() != null && customerRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new DuplicateEmailException(request.email());
        }

        if (customerRepository.existsByPhoneAndIdNot(request.phone(), id)) {
            throw new DuplicatePhoneException(request.phone());
        }

        customer.setFirstName(request.firstName());
        customer.setMiddleName(request.middleName());
        customer.setLastName(request.lastName());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setAddress(request.address());

        CreditAccount account = findAccountOrThrow(id);
        auditLogService.record(
                currentUser,
                "CUSTOMER_UPDATED",
                "Customer",
                id,
                null
        );
        return toResponse(customer, account);
    }

    @PreAuthorize("hasAnyRole('SHOP_OWNER', 'ADMIN')")
    @Transactional
    public void deactivateCustomer(Long id, AccountStatusChangeRequest request) {
        SpazaCredUser currentUser = currentUserProvider.getCurrentUser().getUser();

        Customer customer = findCustomerOrThrow(id);
        CreditAccount account = findAccountOrThrow(id);

        if (accountService.getOutstandingBalance(account.getId()).compareTo(BigDecimal.ZERO) > 0) {
            throw new OutstandingBalanceException(id);
        }

        if (!customer.isActive()) {
            throw new CustomerInactiveException(customer.getId());
        }

        customer.setActive(false);
        Map<String, Object> details = Map.of("reason", request.reason());
        auditLogService.record(
                currentUser,
                "CUSTOMER_DEACTIVATED",
                "Customer",
                customer.getId(),
                details
        );
    }

    private Customer findCustomerOrThrow(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    private CreditAccount findAccountOrThrow(Long customerId) {
        return creditAccountRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new IllegalStateException(
                        "Customer " + customerId + " has no credit account"
                ));
    }

    private CustomerResponse toResponse(Customer customer, CreditAccount account) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getMiddleName(),
                customer.getLastName(),
                customer.getPhone(),
                customer.getIdNumber(),
                customer.getEmail(),
                customer.getAddress(),
                customer.isActive(),
                account.getCreditLimit(),
                account.getStatus().name()
        );
    }
}
