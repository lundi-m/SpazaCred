ALTER TABLE payments
    ADD COLUMN voided_by BIGINT REFERENCES users(id);

UPDATE payments
SET status = 'COMPLETED'
WHERE status = 'ACTIVE';

ALTER TABLE payments
    ALTER COLUMN status SET DEFAULT 'COMPLETED';

ALTER TABLE payments
    DROP CONSTRAINT chk_payment_status;

ALTER TABLE payments
    ADD CONSTRAINT chk_payment_status CHECK (status IN ('COMPLETED', 'VOIDED'));