package com.lundi_m.spazacred_api.payment.controller;

import com.lundi_m.spazacred_api.payment.dto.PaymentResponse;
import com.lundi_m.spazacred_api.payment.dto.RecordPaymentRequest;
import com.lundi_m.spazacred_api.payment.dto.VoidPaymentRequest;
import com.lundi_m.spazacred_api.payment.service.PaymentService;
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
public class PaymentController {

    private final PaymentService paymentService;


    @Operation(
            summary = "Record a customer payment",
            description = """
                Records a payment against a customer's outstanding balance.
                Requires EMPLOYEE, SHOP_OWNER or ADMIN role.

                Payments are automatically allocated to the customer's oldest
                outstanding purchases first. A payment cannot exceed the
                customer's total outstanding balance.

                Suspended accounts can still receive payments.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/customers/{customerId}/payments")
    public ResponseEntity<PaymentResponse> recordPayment(@PathVariable Long customerId,
                                                         @Valid @RequestBody RecordPaymentRequest request) {
        PaymentResponse response = paymentService.recordPayment(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get customer payment history",
            description = """
                Retrieves a paginated list of payments recorded for a customer.
                Requires EMPLOYEE, SHOP_OWNER or ADMIN role.
                Supports pagination and sorting.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/api/v1/customers/{customerId}/payments")
    public ResponseEntity<Page<PaymentResponse>> getPayments(@PathVariable Long customerId,
                                                       Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPayments(customerId, pageable));
    }

    @Operation(
            summary = "Void a payment",
            description = """
                Voids an existing payment.
                Requires SHOP_OWNER or ADMIN role.
                A reason must be provided.

                Voiding a payment reverses its allocations, restoring the
                outstanding balances of the purchases to which it was allocated.
                The payment record is retained for audit purposes.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/api/v1/payments/{paymentId}/void")
    public ResponseEntity<PaymentResponse> voidPayment(@PathVariable Long paymentId,
                                                       @Valid @RequestBody VoidPaymentRequest request) {
        return ResponseEntity.ok(paymentService.voidPayment(paymentId, request));
    }
}
