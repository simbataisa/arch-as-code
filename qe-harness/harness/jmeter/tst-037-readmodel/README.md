# TST-037 -- Read-Model Convergence & CDC Lag (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.
Coverage: **partial** -- see `partial_reason` in `traceability/modules.yml`.

| ID | Invariant | Asserted here |
|---|---|---|
| I1 | The read model converges inside the declared bound | yes |
| I2 | Lag is asserted at p95 **and** p99, never the mean | yes |
| I3 | A replayed projection equals the incremental one, field by field | yes |
| I4 | Every outbox row has published_count = 1 | yes |
| I5 | No loss or duplication across a connector restart | **no -- not implemented** |
| I6 | Exceeding the bound is a hard FAIL, never an indefinite wait | yes |

I5 needs a CDC connector this repository does not contain. It is reported `not-evaluated` with
a reason rather than substituted -- a substitute server-side check would be a different
invariant wearing I5's name, which is the failure mode `TST-043`'s honest relabelling exists to
warn about.

The convergence bound is `app.readmodel.convergence-bound-ms`, returned by `GET /reporting/lag`
alongside the measurement, so this module asserts against the SUT's declared configuration
rather than a literal of its own. It carries **no** `threshold_ref`: the NFR corpus states lag
only in message counts, so citing an NFR row would fabricate provenance (design spec 7.1).

Defect proof: with `outbox-published-count-stale` active this module MUST report I4 failed and
I1/I2 still passed.

## What this module drives

1. **setUp Thread Group** (`Seed Ledger Activity`, 1 thread, 1 loop) truncates and seeds via
   JDBC, then enqueues outbox rows.
2. **Main Thread Group** (`Refresh and Sample Lag`, 4 threads x 3 loops) alternates
   `POST /reporting/refresh` with `GET /reporting/lag`, keeping the maximum observed p95 and
   p99 in `props`. **I6 is structural, not a timer**: the plan never waits for convergence, it
   samples a bounded number of times and fails if the bound is still breached -- an indefinite
   wait is the behaviour I6 forbids.
3. **TearDown Thread Group** (`Verify Convergence`, 1 thread, 1 loop) calls
   `POST /reporting/refresh` once more, reads `GET /reporting/lag` and `GET /reporting/outbox`,
   compares a replayed projection against the incremental one for I3, then
   `assert-readmodel.groovy` evaluates I1-I4 and I6, and emits I5 as `not-evaluated`.

## Running it

```
make up PROFILES=core
./bin/run-module.sh TST-037
```

## Defect proof

```
curl -X POST http://localhost:8080/_test/defect/outbox-published-count-stale   # 204
./bin/run-module.sh TST-037                                                    # must report I4 FAILED
curl -X DELETE http://localhost:8080/_test/defect                              # 204
```

With the defect active, `ReportingService.publishPending` sets `published_at` but never
increments `published_count`, so `GET /reporting/outbox` reports a miscounted row and I4 alone
fails. Convergence is untouched, which is what makes the proof specific.
