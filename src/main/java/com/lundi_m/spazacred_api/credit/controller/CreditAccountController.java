package com.lundi_m.spazacred_api.credit.controller;

import com.lundi_m.spazacred_api.audit.dto.AdjustLimitRequest;
import com.lundi_m.spazacred_api.audit.dto.CreditAccountResponse;
import com.lundi_m.spazacred_api.credit.service.CreditAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customer/{customerId}/account")
public class CreditAccountController {

    private final CreditAccountService accountService;

    @GetMapping
    public ResponseEntity<CreditAccountResponse> getAccount(@PathVariable Long customerId) {
        return ResponseEntity.ok(accountService.getAccount(customerId));
    }

    @PatchMapping("/limit")
    public ResponseEntity<CreditAccountResponse> adjustLimit(@PathVariable Long customerId,
                                                             @Valid @RequestBody AdjustLimitRequest request) {
        return ResponseEntity.ok(accountService.adjustCreditLimit(customerId, request));
    }

    @PatchMapping("/suspend")
    public ResponseEntity<CreditAccountResponse> suspend(@PathVariable Long customerId) {
        return ResponseEntity.ok(accountService.suspendAccount(customerId));
    }

    @PatchMapping("/reactivate")
    public ResponseEntity<CreditAccountResponse> reactivate(@PathVariable Long customerId) {
        return ResponseEntity.ok(accountService.suspendAccount(customerId));
    }

    @PatchMapping("/close")
    public ResponseEntity<CreditAccountResponse> close(@PathVariable Long customerId) {
        return ResponseEntity.ok(accountService.closeAccount(customerId));
    }
}
