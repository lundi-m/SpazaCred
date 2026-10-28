ALTER TABLE credit_transactions
    ADD CONSTRAINT chk_credit_transactions_void_details
    CHECK (
        (status = 'ACTIVE'
            AND voided_at IS NULL
            AND voided_by IS NULL)
        OR
        (status = 'VOIDED'
            AND voided_at IS NOT NULL
            AND voided_by IS NOT NULL)
    );

ALTER TABLE payments
    ADD CONSTRAINT chk_payments_void_details
    CHECK (
        (status = 'COMPLETED'
            AND voided_at IS NULL
            AND voided_by IS NULL)
        OR
        (status = 'VOIDED'
            AND voided_at IS NOT NULL
            AND voided_by IS NOT NULL)
    );

ALTER TABLE payment_allocations
    ADD CONSTRAINT chk_payment_allocations_reversal_details
    CHECK (
        (reversed_at IS NULL AND reversed_by IS NULL)
        OR
        (reversed_at IS NOT NULL AND reversed_by IS NOT NULL)
    );