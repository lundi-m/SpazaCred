ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO users (first_name, last_name, email, password_hash, role, enabled, must_change_password, created_at, updated_at)
VALUES('System', 'Admin', 'admin@spazacred.local',
'$2a$10$fowkz.gDHpALFbxb7uBWbeDzyBhbCLAs8fkTxTnsmGegbXPIzZU3i',
'ADMIN', TRUE, TRUE, now(), now());