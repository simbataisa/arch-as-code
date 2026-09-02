-- V2: reporting view for TST-039 reconciliation (Task 12, Wave 16). See
-- com.techcombank.qe.sut.capability.recon.{ReconService,DefectSeeder}.
--
-- account_balance_report is a genuine PostgreSQL MATERIALIZED VIEW over
-- ledger_entry -- a physically-stored snapshot of every account's balance,
-- refreshed on demand via REFRESH MATERIALIZED VIEW, and therefore able to
-- drift out of sync with ledger_entry (its source of truth) between
-- refreshes. That drift -- not anything a defect class fabricates out of
-- thin air -- is exactly the reconciliation surface TST-039 exercises:
-- completeness (an account with ledger activity but no row here yet) and
-- accuracy (a row whose balance_minor no longer matches ledger_entry's true
-- sum for that account) are both genuine consequences of "this snapshot was
-- taken at some point in the past and ledger_entry has moved on since".
--
-- balance_minor is explicitly CAST to BIGINT: Postgres's SUM(bigint)
-- naturally returns NUMERIC, but ledger_entry.amount_minor is BIGINT and
-- ReconService compares against a same-shape recomputation of it, so both
-- sides of every comparison stay plain 64-bit integers rather than mixing
-- NUMERIC and BIGINT.
CREATE MATERIALIZED VIEW account_balance_report AS
SELECT
    account_id,
    CAST(SUM(amount_minor) AS BIGINT) AS balance_minor
FROM ledger_entry
GROUP BY account_id;

-- Required for REFRESH MATERIALIZED VIEW CONCURRENTLY, and doubles as the
-- natural lookup key ReconService's completeness/accuracy queries join on.
CREATE UNIQUE INDEX account_balance_report_account_id_idx
    ON account_balance_report (account_id);

-- report_refresh_timestamp is a plain table, deliberately NOT a column
-- inside the materialized view itself. A single REFRESH MATERIALIZED VIEW
-- statement recomputes every row of the view in one query execution, so a
-- `refreshed_at` column populated via e.g. `now()` in the view's own SELECT
-- would necessarily be identical across every row after any refresh --
-- there is no way for Postgres's native REFRESH to leave one account's row
-- with an older timestamp than its neighbours. Real reporting/ETL
-- pipelines that need per-partition freshness tracking solve this the same
-- way this table does: a companion bookkeeping table, updated by whichever
-- job last confirmed a given partition (here, an account) fresh.
-- ReconService's timeliness check reads this table directly as the sole
-- authority on "when was this account's report row last confirmed fresh" --
-- it never trusts anything DefectSeeder claims to have seeded.
CREATE TABLE report_refresh_timestamp (
    account_id   BIGINT PRIMARY KEY REFERENCES account(id),
    refreshed_at TIMESTAMPTZ NOT NULL
);
