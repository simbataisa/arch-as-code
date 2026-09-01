"""TST-039 Data Quality Reconciliation load/correctness driver (Task 21).

Flow, run headlessly by `bin/run-locust.sh` (see that script and this
module's README):

  1. `on_test_start` (once, before any simulated user starts): clean up any
     `ACC-99xxxx` rows a PRIOR run left behind (`recompute.reset_seeded_state`),
     `POST /recon/seed-defects` (creates the real completeness/accuracy/
     timeliness divergence -- see `DefectSeeder`), then independently
     recompute the true defect set straight from the ledger tables
     (`recompute.recompute_expected_defects`) -- BEFORE any concurrent user
     ever calls `GET /recon/report`, so the baseline this run scores against
     is never influenced by anything the report itself said.
  2. Every simulated `ReconUser` repeatedly calls `GET /recon/report` (the
     concurrency this archetype needs -- data-quality reconciliation must
     stay correct while the reporting endpoint is under load, not only when
     called once, serially) and scores each response against that same
     independently-recomputed baseline, accumulating tp/fp/fn per dimension.
  3. `on_test_stop` (once, after every user has stopped): builds one
     invariant per dimension (`I1` completeness, `I2` accuracy, `I3`
     timeliness -- `failed` if that dimension's accumulated `fp` or `fn` is
     ever above zero) and emits one evidence fragment via `emitter.py`.
"""

from __future__ import annotations

import os
import sys
from pathlib import Path

# tst_039_recon/locustfile.py -> parent = tst_039_recon, parent.parent = harness/locust.
# Locust imports this file directly by path; explicit sys.path entries (rather
# than relying on however Locust's own loader happens to set sys.path) is what
# makes `import emitter` (one level up) and `from tst_039_recon.recompute import
# ...` (this package, from its parent) both resolve regardless of that loader's
# behaviour or the caller's own cwd.
_TST_039_DIR = Path(__file__).resolve().parent
_LOCUST_DIR = _TST_039_DIR.parent
for _p in (str(_LOCUST_DIR), str(_TST_039_DIR)):
    if _p not in sys.path:
        sys.path.insert(0, _p)

import requests
from locust import HttpUser, between, events, task

from emitter import emit_fragment
from tst_039_recon.recompute import (
    connect,
    fetch_reported_defects,
    recompute_expected_defects,
    reset_seeded_state,
    score_dimensions,
)

DIMENSIONS = ("completeness", "accuracy", "timeliness")
INVARIANT_IDS = {"completeness": "I1", "accuracy": "I2", "timeliness": "I3"}
INVARIANT_DESCRIPTIONS = {
    "completeness": "no false negatives/positives in completeness detection",
    "accuracy": "no false negatives/positives in accuracy detection",
    "timeliness": "no false negatives/positives in timeliness detection",
}

# Populated by on_test_start, read by every ReconUser task and by on_test_stop.
_state: dict[str, object] = {"expected": None, "totals": None}


def _fresh_totals() -> dict[str, dict[str, int]]:
    return {dimension: {"tp": 0, "fp": 0, "fn": 0} for dimension in DIMENSIONS}


@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    base_url = environment.host or os.environ.get("SUT_BASE_URL", "http://localhost:8080")

    conn = connect()
    try:
        # Idempotency: DefectSeeder's fixed seed (42) always writes the same
        # ACC-99xxxx refs, so a re-run against an already-seeded database
        # would otherwise fail outright on account.account_ref's UNIQUE
        # constraint.
        reset_seeded_state(conn)

        response = requests.post(f"{base_url}/recon/seed-defects", timeout=10)
        response.raise_for_status()

        # Independent recomputation, straight from the ledger tables --
        # never from the seed-defects response above, and never from
        # GET /recon/report. This is the baseline every ReconUser task
        # scores its own report call against.
        _state["expected"] = recompute_expected_defects(conn)
    finally:
        conn.close()

    _state["totals"] = _fresh_totals()


class ReconUser(HttpUser):
    """Repeatedly polls GET /recon/report under concurrency, scoring each
    response against the independently-recomputed baseline on_test_start
    already captured."""

    wait_time = between(0.1, 0.5)

    @task
    def poll_report(self):
        expected = _state["expected"]
        if expected is None:
            # on_test_start hasn't run yet (e.g. a stray request during
            # ramp-up); nothing to score against yet.
            return

        with self.client.get("/recon/report", catch_response=True) as response:
            if response.status_code != 200:
                response.failure(f"GET /recon/report -> {response.status_code}")
                return
            reported = fetch_reported_defects(response.json())
            scores = score_dimensions(expected, reported)
            totals = _state["totals"]
            for dimension in DIMENSIONS:
                for outcome in ("tp", "fp", "fn"):
                    totals[dimension][outcome] += scores[dimension][outcome]
            response.success()


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    totals = _state["totals"] or _fresh_totals()

    invariants = []
    for dimension in DIMENSIONS:
        outcome = totals[dimension]
        result = "failed" if (outcome["fp"] > 0 or outcome["fn"] > 0) else "passed"
        invariants.append({
            "id": INVARIANT_IDS[dimension],
            "description": INVARIANT_DESCRIPTIONS[dimension],
            "result": result,
        })

    runs_dir = Path(
        os.environ.get(
            "EVIDENCE_OUTPUT_DIR",
            Path(__file__).resolve().parents[3] / "traceability" / "runs",
        )
    )
    fragment = {
        "archetype": os.environ.get("QE_ARCHETYPE", "TST-039"),
        "module": "locust",
        "service_name": "reference-sut",
        "tier": "T0",
        "oracle": "confusion-matrix",
        "environment": os.environ.get("QE_ENVIRONMENT", "local-compose"),
        "invariants": invariants,
    }
    sut_defect = os.environ.get("QE_SUT_DEFECT")
    if sut_defect:
        fragment["sut_defect"] = sut_defect

    out = emit_fragment(fragment, runs_dir)
    print(f"tst_039_recon: wrote {out} (totals={totals})")
