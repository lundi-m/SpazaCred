package com.lundi_m.spazacred_api.payment.controller;

import com.lundi_m.spazacred_api.payment.dto.PaymentResponse;
import com.lundi_m.spazacred_api.payment.dto.RecordPaymentRequest;
import com.lundi_m.spazacred_api.payment.dto.VoidPaymentRequest;
import com.lundi_m.spazacred_api.payment.service.PaymentService;
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

    @PostMapping("/api/v1/customers/{customerId}/payments")
    public ResponseEntity<PaymentResponse> recordPayment(@PathVariable Long customerId,
                                                         @Valid @RequestBody RecordPaymentRequest request) {
        PaymentResponse response = paymentService.recordPayment(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/customers/{customerId}/payments")
    public ResponseEntity<Page<PaymentResponse>> getPayments(@PathVariable Long customerId,
                                                       Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPayments(customerId, pageable));
    }

    @PostMapping("/api/v1/payments/{paymentId}")
    public ResponseEntity<PaymentResponse> voidPayment(@PathVariable Long paymentId,
                                                       @Valid @RequestBody VoidPaymentRequest request) {
        return ResponseEntity.ok(paymentService.voidPayment(paymentId, request));
    }
}
