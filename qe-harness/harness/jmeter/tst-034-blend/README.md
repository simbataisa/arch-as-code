# TST-034 -- Blended Journey Workload (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.

| ID | Invariant |
|---|---|
| I1 | Every constituent journey meets its **own** tier budget, never a blended figure |
| I2 | Each journey's actual share is within tolerance of its declared share |
| I3 | No journey is starved -- every journey keeps a non-zero count in every sub-window |
| I4 | Errors are attributed per journey, not pooled |
| I5 | Steady state is reached before measurement begins |

Defect proof: with `journey-starved` active this module MUST report I3 failed and I1 passed.

This is the **first module in the repository to read a profile file**. The blend comes from
`profiles/mixed.yml`'s `blend_ref: wave17-core-mix` via `ProfileResolver` (Wave 17), not from
literals in `plan.jmx` -- so the declared mix and the asserted mix cannot drift apart. Per
invariant I1, each journey's p95 is asserted against **its own tier's** NFR-002 budget
(`p95_latency_t0_ms`, `p95_latency_t1_ms`, `p95_latency_t2_ms`), resolved through
`ThresholdResolver`, never against a single blended number.

## What this module drives

1. **setUp Thread Group** (`Seed Blend Fixture`, 1 thread, 1 loop) calls
   `POST /_test/seed?seed=42&accounts=20` (Wave 17) so the blend has real contention surface
   rather than the two-account ledger fixture, and zeroes the per-journey tallies in `props`.
2. **Main Thread Group** (`Blended Load`, 20 threads, duration-scheduled) drives all four
   journeys through a **Throughput Controller** per journey, its percentage taken from the
   declared blend. A `JSR223 PreProcessor` selects the journey for each iteration; each
   sampler's `JSR223 PostProcessor` records latency and outcome **tagged by journey name** into
   `props` -- the cross-thread aggregation pattern `tst-031-ratelimit` established. I5's
   steady-state window is skipped by discarding samples from the first sub-window.
   `HARNESS_SMOKE_MODE=true` selects `smoke_mode_overrides.hold_seconds` (20s) instead of
   `hold_seconds` (14,400s): a four-hour blend can never run in an MR pipeline.
3. **TearDown Thread Group** (`Verify Blend`, 1 thread, 1 loop) runs `assert-blend.groovy`,
   which resolves the declared blend and the three tier thresholds, then evaluates I1-I5.

## Running it

```
make up PROFILES=core
HARNESS_SMOKE_MODE=true ./bin/run-module.sh TST-034   # 20s hold, thresholds not-evaluated
./bin/run-module.sh TST-034                           # full 4h hold -- never in CI
```

## Defect proof

```
curl -X POST http://localhost:8080/_test/defect/journey-starved   # 204
HARNESS_SMOKE_MODE=true ./bin/run-module.sh TST-034               # must report I3 FAILED
curl -X DELETE http://localhost:8080/_test/defect                 # 204
```

With `journey-starved` active, `TransferService.transfer` sleeps before taking its locks, so the
transfer journey's throughput collapses and its observed share falls below tolerance. The ledger
stays balanced and per-journey latency attribution keeps working, so I1 and I4 still pass --
which is what makes the proof specific.
