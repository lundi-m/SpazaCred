ALTER TABLE payment_allocations
    ADD COLUMN reversed_by BIGINT REFERENCES users(id);