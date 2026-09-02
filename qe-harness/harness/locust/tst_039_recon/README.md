# TST-039 -- Data Quality Reconciliation (Locust)

Oracle: confusion-matrix. Best-fit tool per TST-010: Locust.

This is the second non-JMeter harness module, and the first pure-Python one -- a standalone
project (`qe-harness/harness/locust/`) entirely outside the Maven reactor, with its own
`pyproject.toml`/`requirements.txt` and its own `pytest` suite.

## Why Locust, not JMeter

`GET /recon/report` (Task 12's `ReconService`) already does genuine independent detection --
it queries `ledger_entry`/`account_balance_report`/`report_refresh_timestamp` directly and
never trusts its own seeder. But that only proves the report is *usually* honest; it says
nothing about whether the report itself can be trusted to *report what it found* -- exactly the
failure the `recon-false-clean` defect models (every dimension is still genuinely computed, so
`checked` counts stay honest, then `defects` is discarded before the response is built).

Scoring that requires a baseline this module computes **itself**, independently of anything the
report says. `recompute.py`'s `recompute_expected_defects` reads `account`/`ledger_entry`/
`account_balance_report`/`report_refresh_timestamp` directly off the same Postgres database the
reference SUT itself writes to -- the same "bypass the HTTP API, query the database straight"
pattern `qe-harness/harness/jmeter/tst-021-ledger/assert-trial-balance.groovy` already uses for
its own I2/I3 checks -- then diffs that against `GET /recon/report`'s claims as a per-dimension
confusion matrix (`score_dimensions`: `tp`/`fp`/`fn`). A false negative there is a real detection
failure, never an artefact of the check trusting the report's own bookkeeping. Building this
stateful seed-then-poll-under-concurrency workflow (one seed call, many concurrent scored
`GET /recon/report` polls, one final scored fragment) is naturally a Locust `User`/task,
not a JMeter assertion per request.

## Invariants

| ID | Description | Fails when |
|---|---|---|
| I1 | no false negatives/positives in completeness detection | any polled report's completeness `defects` disagrees with the independently-recomputed set |
| I2 | no false negatives/positives in accuracy detection | same, for the accuracy dimension |
| I3 | no false negatives/positives in timeliness detection | same, for the timeliness dimension |

## Files

- `emitter.py` (one level up, `qe-harness/harness/locust/`) -- the shared Python evidence
  emitter, mirroring the JVM `EvidenceEmitter`/`RunFragment` field-for-field so a fragment this
  module writes validates against the exact same `evidence.schema.json` a JVM module's fragment
  does.
- `recompute.py` -- the independent-recomputation baseline and the confusion-matrix scorer
  (`connect`, `reset_seeded_state`, `recompute_expected_defects`, `fetch_reported_defects`,
  `score_dimensions`).
- `locustfile.py` -- `on_test_start` seeds + independently recomputes the baseline once;
  `ReconUser.poll_report` polls `GET /recon/report` under concurrency, scoring each response;
  `on_test_stop` emits one evidence fragment.

## Running everything at once

```bash
cd qe-harness && docker compose --profile core up -d --wait   # postgres + reference-sut
./bin/run-module.sh TST-039
```

`bin/run-locust.sh` (Task 21's own addition -- see its own header comment for why it did not
already exist) creates/reuses a venv at `qe-harness/harness/locust/.venv` (`.gitignore`'d),
installs the pinned `requirements.txt`, then runs Locust headlessly (5 users, 10s) against
`SUT_BASE_URL` (default `http://localhost:8080`), writing one evidence fragment to
`traceability/runs/`. Exits non-zero if that fragment's `result` is `failed`.

## Running the pytest suite directly

```bash
cd qe-harness/harness/locust
python3.13 -m venv .venv && .venv/bin/pip install -r requirements.txt
.venv/bin/python -m pytest tests/ -v
```

Pure unit tests of `emitter.py`/`recompute.py` -- neither test calls a live SUT.

## Defect proof (manual)

The task brief's original Step 5 (`SUT_DEFECT=recon-false-clean ./bin/run-module.sh TST-039`)
is **wrong**: `SUT_DEFECT` is a shell environment variable on this process, and the SUT is a
separate, already-running Docker container -- setting it here has zero effect on that container.
Every module that needs to toggle a defect does so via a direct HTTP call to the SUT's own
test-control door (`DefectController`), exactly like `Tst030ContractRunner` does for TST-030's
own defect proof. The corrected sequence:

```bash
curl -X POST http://localhost:8080/_test/defect/recon-false-clean   # 204

cd qe-harness && ./bin/run-module.sh TST-039; echo "exit=$?"
# -> run-locust.sh: TST-039 -> failed (.../traceability/runs/...-TST-039.json)
# -> exit=1
# fragment's "result" is "failed"; every invariant (I1/I2/I3) is "failed" with a
# false-negative count above zero (confirmed against the real reference SUT: 121/121/121
# across 121 concurrent polls in one run)

curl -X DELETE http://localhost:8080/_test/defect                    # 204, always clears it
```

`locustfile.py` never toggles the defect itself (same convention `bin/run-jmeter.sh`/
`bin/run-gatling-karate.sh` follow for their own modules) -- toggling stays a manual/CI step
around `./bin/run-module.sh TST-039`, not something the runner or the locustfile does on its
own.

`on_test_start` calls `reset_seeded_state` before re-seeding, specifically so this exact
sequence -- and any other repeated `./bin/run-module.sh TST-039` invocation against the same,
already-seeded Postgres database -- stays idempotent: `DefectSeeder`'s fixed seed (`42`) always
writes the same four `ACC-99xxxx` account refs, so a second `POST /recon/seed-defects` against
an already-seeded database would otherwise fail outright on `account.account_ref`'s UNIQUE
constraint.
