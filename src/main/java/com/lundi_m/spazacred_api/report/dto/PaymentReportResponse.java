package com.lundi_m.spazacred_api.report.dto;

import com.lundi_m.spazacred_api.payment.dto.PaymentMethodTotal;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PaymentReportResponse(

        @Schema(description = "Start date of the report period",
                example = "2026-11-01")
        LocalDate from,

        @Schema(description = "End date of the report period",
                example = "2026-11-30")
        LocalDate to,

        @Schema(description = "Number of payments recorded in the report period",
                example = "45")
        long paymentCount,

        @Schema(description = "Total amount collected during the report period",
                example = "25000.00")
        BigDecimal totalCollected,

        @Schema(description = "Payment totals grouped by payment method")
        List<PaymentMethodTotal> byMethod,

        @Schema(description = "Paginated list of individual payments")
        Page<PaymentReportRow> payments
) {}
