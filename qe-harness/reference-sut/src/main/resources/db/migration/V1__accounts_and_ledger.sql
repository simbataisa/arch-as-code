-- V1: accounts and ledger tables for the reference SUT's synthetic dataset
-- (Task 6, Wave 16). See com.techcombank.qe.sut.data.SyntheticDataSeeder.
--
-- The account_ref_format CHECK constraint enforces the synthetic-identifier
-- rule at the database level, so no code path -- including an injected
-- defect -- can write a PAN-shaped identifier.

CREATE TABLE account (
    id           BIGSERIAL PRIMARY KEY,
    account_ref  VARCHAR(16) NOT NULL UNIQUE,
    party_name   VARCHAR(64) NOT NULL,
    CONSTRAINT account_ref_format CHECK (account_ref ~ '^ACC-[0-9]{6}$')
);

CREATE TABLE ledger_entry (
    id            BIGSERIAL PRIMARY KEY,
    transfer_ref  UUID        NOT NULL,
    account_id    BIGINT      NOT NULL REFERENCES account(id),
    amount_minor  BIGINT      NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT amount_nonzero CHECK (amount_minor <> 0)
);

CREATE INDEX ledger_entry_transfer_ref_idx ON ledger_entry (transfer_ref);
