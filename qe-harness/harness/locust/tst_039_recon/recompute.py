"""TST-039 reconciliation: independent recomputation (Task 21, Wave 16).

This is deliberately NOT built on top of `GET /recon/report` -- that endpoint
is exactly what this module scores, and a "recomputation" that quietly
delegated back to it could never detect the report lying about itself (see
the `recon-false-clean` defect flag, which makes
`com.techcombank.qe.sut.capability.recon.ReconService#report` discard every
genuinely-detected defect before responding). Instead,
`recompute_expected_defects` reads `account`/`ledger_entry`/
`account_balance_report`/`report_refresh_timestamp` DIRECTLY off the same
Postgres database the reference SUT itself writes to -- the same
"bypass the HTTP API, query the database straight" pattern
`qe-harness/harness/jmeter/tst-021-ledger/assert-trial-balance.groovy`
already uses for its own I2/I3 checks (its `LEDGER_JDBC_URL`/`_USER`/
`_PASSWORD`; see `qe-harness/docker-compose.yml`'s own comment on why
postgres's port is published to the host at all). The three SQL queries
below mirror `ReconService`'s three private query methods field-for-field --
authored independently in Python, not delegated to anything the SUT's own
Java code computed.

This is *why TST-039 uses Locust, not JMeter*: the confusion-matrix scoring
this module does needs a genuinely independent, directly-recomputed baseline
to diff the report against, not just an assertion on the report's own shape
or fields.
"""

from __future__ import annotations

from datetime import datetime, timedelta, timezone
from typing import Any

import psycopg2

# app.recon.freshness-window-seconds
# (reference-sut/src/main/resources/application.properties): the *declared*
# SLA ReconService/DefectSeeder are actually configured with. No HTTP
# surface exposes this value back to an external caller -- the same
# limitation qe-harness/harness/jmeter/tst-040-authz/assert-authz.groovy and
# tst-031-ratelimit/assert-ratelimit.groovy already document for their own
# hardcoded declared-config literals -- so this must be kept in sync by hand
# if that property ever changes. See this module's README.md.
DECLARED_FRESHNESS_WINDOW_SECONDS = 300

# ACC-99xxxx is DefectSeeder's own namespace (see its javadoc): every
# account it seeds, across every dimension plus the untouched control
# account, starts with this prefix. Used only to clean up a PRIOR run's
# seeded rows before re-seeding -- ReconService's own detection queries
# below never filter by this prefix, so a genuinely independent
# recomputation is never scoped to "only the accounts we know we seeded".
SEEDED_ACCOUNT_REF_PREFIX = "ACC-99"


def connect(dsn: str | None = None):
    """Open a direct connection to the same Postgres database the reference
    SUT itself uses (see qe-harness/docker-compose.yml's `postgres` service,
    port 5432 published to the host, and reference-sut's own
    SPRING_DATASOURCE_URL). `dsn=None` lets psycopg2 read the standard libpq
    `PG*` environment variables (`PGHOST`/`PGPORT`/`PGDATABASE`/`PGUSER`/
    `PGPASSWORD`), which `bin/run-locust.sh` sets with the same defaults
    `bin/run-jmeter.sh`'s `LEDGER_JDBC_*` variables use.
    """
    return psycopg2.connect(dsn) if dsn else psycopg2.connect()


def reset_seeded_state(conn) -> None:
    """Delete any accounts (and their dependent rows) left over from a PRIOR
    `POST /recon/seed-defects` call, so this module's own run can call that
    endpoint again without tripping `account.account_ref`'s UNIQUE
    constraint -- `DefectSeeder#seed` always writes the same four
    `ACC-99xxxx` refs for its fixed seed (42), so a second seed call against
    an already-seeded database fails outright unless this cleanup runs
    first. Deletion order respects the schema's FK constraints
    (`ledger_entry`/`report_refresh_timestamp` -> `account`, both default
    RESTRICT, no ON DELETE CASCADE); the materialized view is refreshed
    afterwards so it never keeps a stale row for a now-deleted account_id.
    """
    with conn.cursor() as cur:
        cur.execute(
            "DELETE FROM ledger_entry WHERE account_id IN "
            "(SELECT id FROM account WHERE account_ref LIKE %s)",
            (f"{SEEDED_ACCOUNT_REF_PREFIX}%",),
        )
        cur.execute(
            "DELETE FROM report_refresh_timestamp WHERE account_id IN "
            "(SELECT id FROM account WHERE account_ref LIKE %s)",
            (f"{SEEDED_ACCOUNT_REF_PREFIX}%",),
        )
        cur.execute(
            "DELETE FROM account WHERE account_ref LIKE %s",
            (f"{SEEDED_ACCOUNT_REF_PREFIX}%",),
        )
        cur.execute("REFRESH MATERIALIZED VIEW account_balance_report")
    conn.commit()


def recompute_expected_defects(
    conn, freshness_window_seconds: int = DECLARED_FRESHNESS_WINDOW_SECONDS
) -> dict[str, set[str]]:
    """Independently recompute which `account_ref`s are genuinely defective in
    each of the three reconciliation dimensions, straight from
    `ledger_entry`/`account_balance_report`/`report_refresh_timestamp` --
    never from `GET /recon/report` and never from `POST /recon/seed-defects`'s
    own response. Mirrors `ReconService`'s `completeness()`/`accuracy()`/
    `timeliness()` query-for-query.
    """
    with conn.cursor() as cur:
        # Completeness: an account with ledger activity but no
        # account_balance_report row at all.
        cur.execute(
            "SELECT DISTINCT a.account_ref FROM account a "
            "JOIN ledger_entry l ON l.account_id = a.id "
            "LEFT JOIN account_balance_report r ON r.account_id = a.id "
            "WHERE r.account_id IS NULL"
        )
        completeness = {row[0] for row in cur.fetchall()}

        # Accuracy: a report row whose stored balance no longer equals a
        # freshly-recomputed SUM(amount_minor) for that account.
        cur.execute(
            "SELECT a.account_ref, r.balance_minor, "
            "  COALESCE((SELECT SUM(l.amount_minor) FROM ledger_entry l "
            "            WHERE l.account_id = a.id), 0) AS actual "
            "FROM account a "
            "JOIN account_balance_report r ON r.account_id = a.id"
        )
        accuracy = {ref for ref, reported, actual in cur.fetchall() if reported != actual}

        # Timeliness: a report_refresh_timestamp row older than the
        # declared freshness window.
        cur.execute(
            "SELECT a.account_ref, t.refreshed_at "
            "FROM account a "
            "JOIN report_refresh_timestamp t ON t.account_id = a.id"
        )
        threshold = datetime.now(timezone.utc) - timedelta(seconds=freshness_window_seconds)
        timeliness = {ref for ref, refreshed_at in cur.fetchall() if refreshed_at < threshold}

    return {"completeness": completeness, "accuracy": accuracy, "timeliness": timeliness}


def fetch_reported_defects(report: dict[str, Any]) -> dict[str, set[str]]:
    """Translate `GET /recon/report`'s JSON body (`{"completeness":
    {"checked": n, "defects": [...]}, "accuracy": {...}, "timeliness":
    {...}}`, per `ReconReport`/`DimensionResult`) into the same
    `{dimension: set(account_ref)}` shape `score_dimensions` expects.
    """
    return {
        dimension: set(report[dimension]["defects"])
        for dimension in ("completeness", "accuracy", "timeliness")
    }


def score_dimensions(
    expected: dict[str, set[str]], reported: dict[str, set[str]]
) -> dict[str, dict[str, int]]:
    """Per-dimension confusion-matrix scoring.

    `expected` is this module's own independently-recomputed defect set (see
    `recompute_expected_defects` above) -- never taken from `GET
    /recon/report` and never merely echoed from `POST /recon/seed-defects`'s
    response. `reported` is what `GET /recon/report` actually claims.

    A false negative (`fn`) here -- an account genuinely defective per
    direct ledger recomputation, but absent from the report -- is exactly
    the failure shape the `recon-false-clean` defect produces. Independent
    recomputation is why TST-039 uses Locust, not JMeter: this scoring needs
    a genuinely computed, independent baseline to diff the report against,
    not just an assertion on the report's own shape.
    """
    scores: dict[str, dict[str, int]] = {}
    for dimension in ("completeness", "accuracy", "timeliness"):
        exp = expected.get(dimension, set())
        rep = reported.get(dimension, set())
        scores[dimension] = {
            "tp": len(exp & rep),
            "fp": len(rep - exp),
            "fn": len(exp - rep),
        }
    return scores
