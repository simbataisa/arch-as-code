-- V4: transactional outbox for TST-037 read-model convergence (Wave 17).
-- See com.techcombank.qe.sut.capability.reporting.ReportingService.
--
-- V2 already supplies account_balance_report (a real MATERIALIZED VIEW) and
-- report_refresh_timestamp (its per-account freshness bookkeeping). What
-- TST-037 additionally needs is I4's evidence surface: "every outbox row is
-- published exactly once". published_count is a counter rather than a boolean
-- precisely so double-publication is observable, not just non-publication --
-- a boolean flag would make the two failures indistinguishable.
--
-- No FK to account: the aggregate_ref is a business reference (ACC-000001),
-- not an id, so an outbox row survives its aggregate. This means the table is
-- NOT reached by AbstractLedgerIntegrationTest's TRUNCATE ... CASCADE, so any
-- test touching it must truncate it explicitly.

CREATE TABLE outbox (
    id              BIGSERIAL PRIMARY KEY,
    event_type      VARCHAR(64) NOT NULL,
    aggregate_ref   VARCHAR(16) NOT NULL,
    published_count INTEGER     NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ,
    CONSTRAINT outbox_published_count_sane CHECK (published_count >= 0)
);

CREATE INDEX outbox_pending_idx ON outbox (id) WHERE published_at IS NULL;
