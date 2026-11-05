package com.lundi_m.spazacred_api.report.controller;

import com.lundi_m.spazacred_api.report.dto.OutstandingReportRow;
import com.lundi_m.spazacred_api.report.dto.OverdueReportRow;
import com.lundi_m.spazacred_api.report.dto.PaymentReportResponse;
import com.lundi_m.spazacred_api.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    @Operation(
            summary = "Generate overdue transactions report",
            description = """
                Returns a paginated report of outstanding credit transactions
                that are overdue by at least the specified number of days.

                The minimum overdue period defaults to 1 day.
                Requires EMPLOYEE, SHOP_OWNER or ADMIN role.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/overdue")
    public ResponseEntity<Page<OverdueReportRow>> overdue(
            @Parameter(
                    description = "Minimum number of days a transaction must be overdue",
                    example = "7"
            )
            @RequestParam(defaultValue = "1") int minDaysOverdue,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reportService.overdue(minDaysOverdue, pageable));
    }

    @Operation(
            summary = "Generate outstanding balances report",
            description = """
                Returns a paginated report of customers with their credit account
                status, credit limit, outstanding balance and available credit.

                Requires EMPLOYEE, SHOP_OWNER or ADMIN role.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/outstanding-by-customer")
    public ResponseEntity<Page<OutstandingReportRow>> outstandingByCustomer(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reportService.outstandingByCustomer(pageable));
    }

    @Operation(
            summary = "Generate payments report",
            description = """
                Returns a payment report for the specified date range.

                Both date filters are optional. When supplied, dates must use
                ISO format (yyyy-MM-dd). The response includes total payments
                collected, payment totals grouped by method, and a paginated
                list of individual payments.

                Requires EMPLOYEE, SHOP_OWNER or ADMIN role.
                """
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/payments")
    public ResponseEntity<PaymentReportResponse> payments(
            @Parameter(description = "Start date (inclusive)", example = "2026-11-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "End date (inclusive)", example = "2026-11-30")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reportService.payments(from, to, pageable));
    }
}
