package com.lundi_m.spazacred_api.credit.controller;

import com.lundi_m.spazacred_api.credit.dto.AdjustLimitRequest;
import com.lundi_m.spazacred_api.credit.dto.CreditAccountResponse;
import com.lundi_m.spazacred_api.credit.dto.AccountStatusChangeRequest;
import com.lundi_m.spazacred_api.credit.dto.CreditAssessmentResponse;
import com.lundi_m.spazacred_api.credit.service.CreditAccountService;
import com.lundi_m.spazacred_api.credit.service.CreditAssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customers/{customerId}/account")
public class CreditAccountController {

    private final CreditAccountService accountService;
    private final CreditAssessmentService assessmentService;

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
    public ResponseEntity<CreditAccountResponse> suspend(@PathVariable Long customerId,
                                                         @Valid @RequestBody AccountStatusChangeRequest request) {
        return ResponseEntity.ok(accountService.suspendAccount(customerId, request));
    }

    @PatchMapping("/reactivate")
    public ResponseEntity<CreditAccountResponse> reactivate(@PathVariable Long customerId,
                                                            @Valid @RequestBody AccountStatusChangeRequest request) {
        return ResponseEntity.ok(accountService.reactivateAccount(customerId, request));
    }

    @PatchMapping("/close")
    public ResponseEntity<CreditAccountResponse> close(@PathVariable Long customerId,
                                                       @Valid @RequestBody AccountStatusChangeRequest request) {
        return ResponseEntity.ok(accountService.closeAccount(customerId, request));
    }

    @GetMapping("/eligibility")
    public ResponseEntity<CreditAssessmentResponse> assess(@PathVariable Long customerId) {
        return ResponseEntity.ok(assessmentService.assess(customerId));
    }
}
