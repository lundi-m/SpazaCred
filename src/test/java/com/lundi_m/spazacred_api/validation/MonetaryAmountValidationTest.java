package com.lundi_m.spazacred_api.validation;

import com.lundi_m.spazacred_api.credit.dto.AdjustLimitRequest;
import com.lundi_m.spazacred_api.credit.dto.CreatePurchaseRequest;
import com.lundi_m.spazacred_api.customer.dto.CreateCustomerRequest;
import com.lundi_m.spazacred_api.payment.dto.RecordPaymentRequest;
import com.lundi_m.spazacred_api.payment.entity.PaymentMethod;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MonetaryAmountValidationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();

    private static final Validator VALIDATOR =
            VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void purchaseAmountWithThreeDecimalPlacesIsRejected() {
        CreatePurchaseRequest request = new CreatePurchaseRequest(
                new BigDecimal("10.005"),
                "Test purchase",
                null
        );

        assertHasPrecisionViolation(request, "amount");
    }

    @Test
    void paymentAmountWithThreeDecimalPlacesIsRejected() {
        RecordPaymentRequest request = new RecordPaymentRequest(
                new BigDecimal("10.005"),
                PaymentMethod.CASH
        );

        assertHasPrecisionViolation(request, "amount");
    }

    @Test
    void adjustedCreditLimitWithThreeDecimalPlacesIsRejected() {
        AdjustLimitRequest request = new AdjustLimitRequest(
                new BigDecimal("10.005"),
                "Test adjustment"
        );

        assertHasPrecisionViolation(request, "newLimit");
    }

    @Test
    void initialCreditLimitWithThreeDecimalPlacesIsRejected() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Lerato",
                null,
                "Mokoena",
                "0821234567",
                "lerato.test@gmail.com",
                "9001015009087",
                "12 Main Road",
                new BigDecimal("10.005")
        );

        assertHasPrecisionViolation(request, "creditLimit");
    }

    @Test
    void purchaseAmountWithTwoDecimalPlacesIsAccepted() {
        CreatePurchaseRequest request = new CreatePurchaseRequest(
                new BigDecimal("10.00"),
                "Test purchase",
                null
        );

        assertNoViolations(request);
    }

    @Test
    void paymentAmountWithTwoDecimalPlacesIsAccepted() {
        RecordPaymentRequest request = new RecordPaymentRequest(
                new BigDecimal("10.00"),
                PaymentMethod.CASH
        );

        assertNoViolations(request);
    }

    @Test
    void adjustedCreditLimitWithTwoDecimalPlacesIsAccepted() {
        AdjustLimitRequest request = new AdjustLimitRequest(
                new BigDecimal("10.00"),
                "Test adjustment"
        );

        assertNoViolations(request);
    }

    @Test
    void initialCreditLimitWithTwoDecimalPlacesIsAccepted() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "Lerato",
                null,
                "Mokoena",
                "0821234567",
                "lerato.test@gmail.com",
                "9001015009087",
                "12 Main Road",
                new BigDecimal("10.00")
        );

        assertNoViolations(request);
    }

    private void assertHasPrecisionViolation(Object request, String propertyName) {
        Set<? extends ConstraintViolation<?>> violations =
                VALIDATOR.validate(request);

        boolean hasPrecisionViolation = violations.stream()
                .anyMatch(violation ->
                        violation.getPropertyPath().toString().equals(propertyName)
                                && violation.getMessage().contains(
                                "no more than 2 decimal places"
                        )
                );

        assertTrue(
                hasPrecisionViolation,
                "Expected a two-decimal precision violation on " + propertyName
        );
    }

    private void assertNoViolations(Object request) {
        assertTrue(VALIDATOR.validate(request).isEmpty(), "Expected the request to pass validation");
    }
}
