-- V5: idempotency keys for TST-020 idempotency and replay (Wave 17).
-- See com.techcombank.qe.sut.capability.ledger.IdempotencyService.
--
-- The UNIQUE constraint on idempotency_key is what makes I5 (true concurrency:
-- one wins, the rest are served the stored response) enforceable rather than
-- merely intended. Two concurrent inserts cannot both succeed; the loser
-- catches the violation and reads the winner's stored response. A
-- check-then-insert in application code could not promise that.
--
-- payload_hash exists for I4: a reused key carrying a DIFFERENT payload must
-- conflict rather than silently replay someone else's result, which is the
-- more dangerous of the two failures.
--
-- response_body is stored verbatim so a replay is BYTE-identical (I2), not
-- merely equivalent -- re-serialising would risk key reordering or whitespace
-- drift.
--
-- NOTE: this table has NO foreign key to account, so it is NOT reached by
-- AbstractLedgerIntegrationTest's TRUNCATE ... CASCADE. It must be added to
-- that TRUNCATE list explicitly or keys leak between tests.
--
-- idempotency_key_format's CHECK constrains keys to the declared hyphenated
-- short form used throughout this suite's fixtures (e.g. "idem-0001"): a
-- literal "idem-" prefix followed by 1-58 characters from
-- [A-Za-z0-9-]. What this DOES guarantee: the key cannot be, say, a bare
-- UUID or an arbitrary client-supplied string outside that shape. What it
-- does NOT guarantee: the character class still permits long digit runs
-- inside the suffix (e.g. "idem-1234567890123"), so this constraint alone
-- does not enforce this corpus's separate "gate check 5" convention (no
-- 13-19-digit run that could be mistaken for an epoch-millis timestamp or a
-- PAN -- see AggregatorService/MessageLog for that convention applied to
-- correlation ids). No key written by this SUT today violates gate check 5,
-- but that is a property of what callers happen to send, not one this CHECK
-- constraint enforces on their behalf.
CREATE TABLE idempotency_key (
    id             BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    payload_hash   VARCHAR(64) NOT NULL,
    response_body  TEXT        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT idempotency_key_format CHECK (idempotency_key ~ '^idem-[A-Za-z0-9-]{1,58}$')
);

CREATE INDEX idempotency_key_expires_at_idx ON idempotency_key (expires_at);
