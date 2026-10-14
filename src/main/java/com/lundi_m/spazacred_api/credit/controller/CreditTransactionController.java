package com.lundi_m.spazacred_api.credit.controller;

import com.lundi_m.spazacred_api.credit.dto.CreatePurchaseRequest;
import com.lundi_m.spazacred_api.credit.dto.CreditTransactionResponse;
import com.lundi_m.spazacred_api.credit.dto.VoidTransactionRequest;
import com.lundi_m.spazacred_api.credit.service.CreditTransactionService;
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

    @PostMapping("/api/v1/customers/{customerId}/credit-transactions")
    public ResponseEntity<CreditTransactionResponse> createPurchase(@PathVariable Long customerId,
                                                                    @Valid @RequestBody CreatePurchaseRequest request) {
        CreditTransactionResponse response = creditTransactionService.createPurchase(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/customers/{customerId}/credit-transactions")
    public ResponseEntity<Page<CreditTransactionResponse>> getHistory(@PathVariable Long customerId,
                                                                      Pageable pageable) {
        return ResponseEntity.ok(creditTransactionService.getHistory(customerId, pageable));
    }

    @PostMapping("/api/v1/credit-transactions/{transactionId}/void")
    public ResponseEntity<CreditTransactionResponse> voidTransaction(
            @PathVariable Long transactionId,
            @Valid @RequestBody VoidTransactionRequest request) {
        return ResponseEntity.ok(creditTransactionService.voidTransaction(transactionId, request));
    }
}
