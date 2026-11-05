package com.lundi_m.spazacred_api.credit.controller;

import com.lundi_m.spazacred_api.credit.dto.AdjustLimitRequest;
import com.lundi_m.spazacred_api.credit.dto.CreditAccountResponse;
import com.lundi_m.spazacred_api.credit.dto.AccountStatusChangeRequest;
import com.lundi_m.spazacred_api.credit.dto.CreditAssessmentResponse;
import com.lundi_m.spazacred_api.credit.service.CreditAccountService;
import com.lundi_m.spazacred_api.credit.service.CreditAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

    @Operation(
            summary = "Get customer credit account",
            description = """
                Retrieves the customer's credit account, including its credit limit,
                account status, outstanding balance and available credit.

                Requires EMPLOYEE, SHOP_OWNER or ADMIN role.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<CreditAccountResponse> getAccount(@PathVariable Long customerId) {
        return ResponseEntity.ok(accountService.getAccount(customerId));
    }

    @Operation(
            summary = "Adjust credit limit",
            description = """
                Changes the customer's credit limit.

                Requires SHOP_OWNER or ADMIN role.
                A new limit and a reason are mandatory.
                The reason is recorded in the audit log.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/limit")
    public ResponseEntity<CreditAccountResponse> adjustLimit(@PathVariable Long customerId,
                                                             @Valid @RequestBody AdjustLimitRequest request) {
        return ResponseEntity.ok(accountService.adjustCreditLimit(customerId, request));
    }

    @Operation(
            summary = "Suspend credit account",
            description = """
                Suspends the customer's credit account.

                Requires SHOP_OWNER or ADMIN role.
                A reason is mandatory and recorded in the audit log.
                Suspended accounts can still receive payments but cannot
                take new credit.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/suspend")
    public ResponseEntity<CreditAccountResponse> suspend(@PathVariable Long customerId,
                                                         @Valid @RequestBody AccountStatusChangeRequest request) {
        return ResponseEntity.ok(accountService.suspendAccount(customerId, request));
    }

    @Operation(
            summary = "Reactivate credit account",
            description = """
                Reactivates a suspended credit account.

                Requires SHOP_OWNER or ADMIN role.
                A reason is mandatory and recorded in the audit log.
                An account that remains overdue may be suspended again
                by the scheduled overdue job.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/reactivate")
    public ResponseEntity<CreditAccountResponse> reactivate(@PathVariable Long customerId,
                                                            @Valid @RequestBody AccountStatusChangeRequest request) {
        return ResponseEntity.ok(accountService.reactivateAccount(customerId, request));
    }

    @Operation(
            summary = "Close credit account",
            description = """
                Permanently closes the customer's credit account.

                Requires SHOP_OWNER or ADMIN role.
                A reason is mandatory.
                The account must have a zero outstanding balance before
                it can be closed. Closed accounts cannot be reactivated.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/close")
    public ResponseEntity<CreditAccountResponse> close(@PathVariable Long customerId,
                                                       @Valid @RequestBody AccountStatusChangeRequest request) {
        return ResponseEntity.ok(accountService.closeAccount(customerId, request));
    }

    @Operation(
            summary = "Assess credit eligibility",
            description = """
                Checks whether the customer is eligible to take new credit.
                Returns the assessment result and reasons where the customer
                is not eligible.

                Requires EMPLOYEE, SHOP_OWNER or ADMIN role.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/eligibility")
    public ResponseEntity<CreditAssessmentResponse> assess(@PathVariable Long customerId) {
        return ResponseEntity.ok(assessmentService.assess(customerId));
    }
}
