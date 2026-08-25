# TST-031 -- Rate Limit, Throttle & Breakpoint (JMeter, smoke-aware)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter. Canonical archetype:
[rate-limit-breakpoint.md](../../../../knowledge-base/testing/archetypes/rate-limit-breakpoint.md)
(TST-031, catalog IDs RES-008/RES-009/RES-011).

| ID | Invariant |
|---|---|
| I1 | Admitted rate never exceeds the configured rate limit |
| I2 | Every rejection carries a `Retry-After` header |
| I3 | No `5xx` response at any load stage |

These three are a deliberately simplified subset of the canonical archetype's own I1-I6
(which also cover queueing, per-client vs. global limits, and priority-class shedding):
the reference SUT's rate-limit capability (Task 8) is a single-instance token bucket with
no queue and no priority classes, so I4 (shedding) and I5 (queue budget) do not apply to it,
and I2's "measured globally, not per instance" clause is moot with exactly one instance.

Defect proof: with the `ratelimit-leaky` defect active (see below) this module MUST report
I1 failed.

## Smoke mode

Correctness is never conditional on smoke mode: I1-I3 are evaluated identically whether
`HARNESS_SMOKE_MODE` is set or not (see `assert-ratelimit.groovy` -- none of the three
`InvariantAssertion.check` calls reads it). Only the ramp's *hold duration per step* and the
emitted `peak_rps` *threshold* change, both taken from
[`profiles/stress.yml`](../../../profiles/stress.yml):

| | `step_hold_seconds` | `peak_rps` threshold |
|---|---|---|
| Full run | 300 (`load_shape.step_hold_seconds`) | evaluated (`passed`/`failed`) |
| Smoke run (`HARNESS_SMOKE_MODE=true`) | 20 (`smoke_mode_overrides.step_hold_seconds`) | `not-evaluated`, reason `smoke-mode` |

This is the one property this module exists to prove: **smoke mode degrades what is
measured, never whether correctness is checked.** `Tst031ModuleTest`'s
`smokeModeStillAssertsCorrectnessInvariants` and `smokeModeRecordsThresholdsNotEvaluatedWithAReason`
pin exactly this split; `reportsFailureAgainstTheLeakyDefect` pins that a real correctness
violation still fails the run even with smoke mode active, so smoke mode can never be used to
launder a genuine defect into a green result.

## What this module drives

`plan.jmx` runs three phases against the reference SUT's rate-limit capability
(`GET /rate-limited/ping`, guarded by `RateLimitFilter`/`TokenBucket`, Task 8):

1. **setUp Thread Group** (`Compute Ramp Shape`, 1 thread, 1 loop) reads
   `HarnessConfig.smokeMode()` (Task 15) once and writes the resulting
   `step_hold_seconds`, plus a run-start timestamp and zeroed tally counters, into `props`
   (JMeter's cross-thread shared store). Every later reference to the ramp shape --
   the main Thread Group's own `duration`, and each of the Throughput Shaping Timer's three
   step durations -- reads this same `props` value via `${__groovy(props.get(...),)}`
   rather than re-deriving it, so the scheduler's cutoff and the Timer's own schedule can
   never drift apart. JMeter guarantees this setUp Thread Group finishes before the main
   Thread Group starts, and `${__groovy(...)}` is evaluated lazily on first access (not at
   file-load time), so this ordering is safe.
2. **Main Thread Group** (`Rate Limit Load`, 25 threads, duration-scheduled) fires
   `GET /rate-limited/ping` continuously. A **Throughput Shaping Timer**
   (`kg.apc.jmeter.timers.VariableThroughputTimer` -- the plugin the archetype's own §5
   canonical harness names) paces the offered rate through three equivalence classes from
   the archetype's §3, each held for `step_hold_seconds`:
   - **below-limit** (5 rps) -- offered rate under the configured limit; every request
     should be admitted.
   - **at-limit** (10 rps) -- offered rate exactly at the configured limit (the reference
     SUT's `app.ratelimit.permits-per-second`, `reference-sut/src/main/resources/application.properties`).
   - **above-limit** (20 rps) -- offered rate double the limit; this is the step I1 actually
     exercises.

   A `JSR223PostProcessor` (`tally-accept-reject`) tallies every response into `props`,
   keyed by which step it landed in (derived from elapsed wall time since the setUp
   Thread Group's recorded start) -- the same `step_<idx>_accepted`/`rejected` idiom the
   archetype's own canonical harness uses, and for the same reason: a JMeter *variable* is
   scoped per-thread and would silently undercount (each of the 25 threads would see only
   its own slice), while `props` is process-wide and reflects the SUT's real aggregate rate.
3. **TearDown Thread Group** (`Evaluate Rate Limit Invariants`, 1 thread, 1 loop) runs only
   after every `Rate Limit Load` thread has finished -- asserting mid-run would understate
   whatever step is still in progress. `assert-ratelimit.groovy` reads the tallies,
   evaluates I1-I3, decides threshold evaluation from `HarnessConfig.smokeMode()`, and calls
   `EvidenceEmitter` to write one fragment to `traceability/runs/`.

### Why a plain ThreadGroup, not a Concurrency Thread Group

The archetype's own §5 canonical harness names a **Concurrency Thread Group** for the open
workload model. This module uses a plain core-JMeter `ThreadGroup` instead: a fixed pool of
25 threads, well above the peak offered rate (20 rps) given the reference SUT's sub-10ms
response times, so the Throughput Shaping Timer's own pacing -- not thread availability --
governs the offered rate at every step. Adding `kg.apc:jmeter-plugins-casutg` (the plugin
that ships Concurrency Thread Group) for this module's small, fixed target rates was not
worth a second external plugin dependency; see "Pinned versions" below for the one plugin
this module does add and why it needed care.

### Why I1 excludes a fixed warm-up window per step

`TokenBucket`'s capacity equals the configured rate (see its own javadoc). Since the
at-limit step (offered rate == refill rate) never drains whatever slack the bucket entered
it with, a bucket that starts the run full stays full through the below-limit and at-limit
steps too, and spends that whole capacity's worth of slack in one near-instant burst the
moment the above-limit step begins -- this is the bucket's designed burst absorption, not a
defect. An early version of this module tried to absorb that burst with a wider numeric
tolerance on the whole-step average instead (`configuredLimitRps / stepHoldSeconds`), which
turned out to sit almost exactly on the real measured value (confirmed empirically: I1 failed
against a clean SUT, no defect active, because the measured average landed a hair above that
razor-thin ceiling) -- correct in theory, but with no margin left for ordinary measurement
noise (HTTP round-trip jitter, thread-scheduling imprecision in the Timer's own pacing).

`tally-accept-reject` (plan.jmx) fixes this by excluding a short, fixed warm-up window
(`tst031_warmup_seconds`, 2s) at the **start of every step** from the accepted tally I1's rate
uses -- long enough to drain any carried-over burst (at 20 rps offered against a 10-token
bucket, that burst is spent in well under a second), short enough to stay a small fraction of
even the smoke-mode 20s hold. `assert-ratelimit.groovy` then divides by the *remaining*
window (`stepHoldSeconds - warmupSeconds`), not the full step, and applies only a measurement-
noise tolerance (10% of the configured rate) on top -- I2/I3 still cover the warm-up window
itself (a missing `Retry-After` or a `5xx` is a real defect at any point in the run, burst or
not). The `ratelimit-leaky` defect's violation (admitted rate roughly double the limit,
sustained for the entire above-limit step, not just its first couple of seconds) is far
outside this tolerance either way.

## Running it

```
make up PROFILES=core          # from qe-harness/, brings up postgres + reference-sut
./bin/run-module.sh TST-031    # from qe-harness/
```

Exits non-zero exactly when the emitted fragment's result is `failed` (see
`bin/run-jmeter.sh`). Set `HARNESS_SMOKE_MODE=true` in the environment to get the shortened
smoke-mode ramp (20s/step instead of 300s/step) with thresholds recorded `not-evaluated`.

## Defect proof

The defect is injected on the running SUT over HTTP, not via a process environment
variable (see `DefectController`/`DefectFlags` in `reference-sut`, and `ModuleRunner`'s own
javadoc for why):

```
curl -X POST http://localhost:8080/_test/defect/ratelimit-leaky   # 204
HARNESS_SMOKE_MODE=true ./bin/run-module.sh TST-031                # must report FAILED (I1)
curl -X DELETE http://localhost:8080/_test/defect                  # 204, always clears it
```

With `ratelimit-leaky` active, `TokenBucket.tryAcquire()` skips its capacity check entirely
and always admits (see that class's own javadoc) -- so during the above-limit step, the
admitted rate tracks the full 20 rps offered rate, roughly double the configured limit plus
tolerance, and I1 fails. `Tst031ModuleTest`'s `reportsFailureAgainstTheLeakyDefect` test
drives this exact sequence via `ModuleRunner`, which performs the HTTP activate/clear itself.

## Pinned versions

`kg.apc:jmeter-plugins-tst:2.6` (Throughput Shaping Timer) and its own
`kg.apc:jmeter-plugins-cmn-jmeter:0.3` dependency are added to `harness/jmeter/pom.xml`'s
`testPlanLibraries`, with `downloadLibraryDependencies` set `false` on the same
`jmeter-maven-plugin` configuration. Without that flag, the plugin's default transitive
resolution would also pull `jmeter-plugins-cmn-jmeter`'s own declared
`org.apache.jmeter:jorphan:2.13` / `ApacheJMeter_core:2.13` dependencies into the same `lib/`
directory as the real 5.6.2 engine jars this reactor is pinned to -- an unnecessary
classpath-collision risk avoided by listing every needed jar explicitly instead (see that
pom's own comment for detail). `org.yaml:snakeyaml:2.4` is also listed explicitly:
`assert-ratelimit.groovy` is the first JSR223 assertion script in this reactor to call
`ThresholdResolver` (Task 4), which depends on it, from inside the forked JMeter JVM.
