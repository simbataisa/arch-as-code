-- V3: reservation counter for TST-023 concurrent limit and contention
-- (Wave 17). See com.techcombank.qe.sut.capability.reservation.ReservationService.
--
-- A reservation is a releasable hold against a per-account declared limit --
-- deliberately not a token bucket. TokenBucket (TST-031) regenerates capacity
-- over wall-clock time and forgets every admission; TST-023's I3 (rollback
-- returns exactly its own amount) and I4 (double release rejected) both need a
-- durable, identity-keyed row whose state can be inspected and transitioned.
--
-- account_limit carries the declared limit L as fixture data. L is a business
-- limit read from the SUT's own tables, not a service SLO, which is why no
-- NFR threshold_ref accompanies TST-023's assertions.
--
-- expires_at supports I6 (no reservation outlives its TTL); the window_tz
-- column supports I5 (window boundaries use the declared timezone, never the
-- server's). Both are stored per-account so a test can declare them rather
-- than duplicate a literal.

CREATE TABLE account_limit (
    account_id      BIGINT PRIMARY KEY REFERENCES account(id),
    declared_limit  BIGINT      NOT NULL,
    ttl_seconds     BIGINT      NOT NULL DEFAULT 60,
    window_tz       VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    CONSTRAINT declared_limit_positive CHECK (declared_limit > 0),
    CONSTRAINT ttl_seconds_positive    CHECK (ttl_seconds > 0)
);

CREATE TABLE reservation (
    id          BIGSERIAL PRIMARY KEY,
    account_id  BIGINT      NOT NULL REFERENCES account(id),
    amount      BIGINT      NOT NULL,
    state       VARCHAR(16) NOT NULL DEFAULT 'held',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT reservation_amount_positive CHECK (amount > 0),
    CONSTRAINT reservation_state_known     CHECK (state IN ('held', 'released', 'expired'))
);

-- Partial index: utilisation only ever sums held rows, so released/expired
-- rows never need to be scanned.
CREATE INDEX reservation_account_held_idx
    ON reservation (account_id) WHERE state = 'held';

CREATE INDEX reservation_expires_at_idx
    ON reservation (expires_at) WHERE state = 'held';
