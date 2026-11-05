package com.lundi_m.spazacred_api.credit.controller;

import com.lundi_m.spazacred_api.credit.dto.CreatePurchaseRequest;
import com.lundi_m.spazacred_api.credit.dto.CreditTransactionResponse;
import com.lundi_m.spazacred_api.credit.dto.VoidTransactionRequest;
import com.lundi_m.spazacred_api.credit.service.CreditTransactionService;
import io.swagger.v3.oas.annotations.Operation;
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
public class CreditTransactionController {

    private final CreditTransactionService creditTransactionService;

    @Operation(
            summary = "Record a credit purchase",
            description = """
                Records a new purchase against the customer's credit account.

                Requires EMPLOYEE, SHOP_OWNER or ADMIN role.
                Only active customers with active credit accounts can make
                purchases on credit.
                The purchase is rejected if it would exceed the credit limit.
                The due date defaults to 30 days if omitted.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/customers/{customerId}/credit-transactions")
    public ResponseEntity<CreditTransactionResponse> createPurchase(@PathVariable Long customerId,
                                                                    @Valid @RequestBody CreatePurchaseRequest request) {
        CreditTransactionResponse response = creditTransactionService.createPurchase(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get customer purchase history",
            description = """
                Retrieves the customer's credit purchase history.

                Requires EMPLOYEE, SHOP_OWNER or ADMIN role.
                Supports pagination and sorting.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/v1/customers/{customerId}/credit-transactions")
    public ResponseEntity<Page<CreditTransactionResponse>> getHistory(@PathVariable Long customerId,
                                                                      Pageable pageable) {
        return ResponseEntity.ok(creditTransactionService.getHistory(customerId, pageable));
    }

    @Operation(
            summary = "Void a credit purchase",
            description = """
                Voids an existing credit purchase and reverses its financial effect.

                Requires SHOP_OWNER or ADMIN role.
                A reason is mandatory and recorded in the audit log.
                A purchase with active payment allocations cannot be voided.
                Void the associated payment first.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/credit-transactions/{transactionId}/void")
    public ResponseEntity<CreditTransactionResponse> voidTransaction(
            @PathVariable Long transactionId,
            @Valid @RequestBody VoidTransactionRequest request) {
        return ResponseEntity.ok(creditTransactionService.voidTransaction(transactionId, request));
    }
}
