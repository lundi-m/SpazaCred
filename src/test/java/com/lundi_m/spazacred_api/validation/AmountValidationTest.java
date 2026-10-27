package com.lundi_m.spazacred_api.validation;

import com.lundi_m.spazacred_api.credit.dto.CreatePurchaseRequest;
import com.lundi_m.spazacred_api.payment.dto.RecordPaymentRequest;
import com.lundi_m.spazacred_api.payment.entity.PaymentMethod;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class AmountValidationTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.005", "0.001", "99999999999999999999"})
    void purchaseRejectsAmountsTheDatabaseCannotStore(String amount) {
        CreatePurchaseRequest request =
                new CreatePurchaseRequest(new BigDecimal(amount), "test", null);

        Set<ConstraintViolation<CreatePurchaseRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.01", "10", "10.50", "99999999999999999.99"})
    void purchaseAcceptsValidAmounts(String amount) {
        CreatePurchaseRequest request =
                new CreatePurchaseRequest(new BigDecimal(amount), "test", null);

        Set<ConstraintViolation<CreatePurchaseRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.005", "0.001", "99999999999999999999"})
    void paymentRejectsAmountsTheDatabaseCannotStore(String amount) {
        RecordPaymentRequest request =
                new RecordPaymentRequest(new BigDecimal(amount), PaymentMethod.CASH);

        Set<ConstraintViolation<RecordPaymentRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
    }
}
