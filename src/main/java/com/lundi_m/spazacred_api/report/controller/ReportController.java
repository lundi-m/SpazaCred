package com.lundi_m.spazacred_api.report.controller;

import com.lundi_m.spazacred_api.report.dto.OutstandingReportRow;
import com.lundi_m.spazacred_api.report.dto.OverdueReportRow;
import com.lundi_m.spazacred_api.report.dto.PaymentReportResponse;
import com.lundi_m.spazacred_api.report.service.ReportService;
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

    @GetMapping("/overdue")
    public ResponseEntity<Page<OverdueReportRow>> overdue(
            @RequestParam(defaultValue = "1") int minDaysOverdue,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reportService.overdue(minDaysOverdue, pageable));
    }

    @GetMapping("/outstanding-by-customer")
    public ResponseEntity<Page<OutstandingReportRow>> outstandingByCustomer(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reportService.outstandingByCustomer(pageable));
    }

    @GetMapping("/payments")
    public ResponseEntity<PaymentReportResponse> payments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(reportService.payments(from, to, pageable));
    }
}
