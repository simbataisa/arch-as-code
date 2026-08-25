# TST-035 -- Fault Injection and Graceful Degradation (JMeter + Toxiproxy)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.

Canonical archetype:
[fault-injection-degradation.md](../../../../knowledge-base/testing/archetypes/fault-injection-degradation.md)
(TST-035, catalog IDs RES-001/RES-002/RES-003/RES-004/RES-006/RES-007/RES-010/RES-012).

| ID | Invariant |
|---|---|
| I1 | Downstream failure never yields a `5xx` response |
| I2 | Degraded response matches the declared shape |
| I3 | Breaker recovers after the fault is removed |

These three are a deliberately simplified subset of the canonical archetype's own I1-I9
(which also cover half-open probe bounds, timeout-budget hierarchies across multiple hops,
health-check aggregation, leader election under partition, thundering-herd recovery, and
regulated-function availability across degradation tiers): the reference SUT's breaker
capability (Task 11) is a single `DownstreamClient` in front of one downstream dependency,
with no bulkhead, no multi-hop timeout waterfall, no composed health check, and no
leader-election or multi-tier degradation of its own, so I5-I9 do not apply to it. I1
corresponds to the archetype's I4 (the fallback's disclosure obligation, restated
negatively) plus its own no-`5xx` guarantee; I2 is I4's shape half; I3 folds the
archetype's I1 (breaker opens within threshold) and I3 (probes bounded, reopens on
failure) into one black-box, HTTP-observable outcome -- see "Why I3 is checked by polling,
not by reading breaker state" below for why this module cannot assert the finer-grained
version directly.

Defect proof: with the `breaker-disabled` defect active (see below) this module MUST
report I1 failed.

## What this module drives

`plan.jmx` runs three phases against the reference SUT's circuit-breaker capability
(`GET /quotes/{id}`, guarded by `DownstreamClient`'s Resilience4j `@CircuitBreaker`, Task
11) while a Toxiproxy toxic breaks its downstream dependency:

1. **setUp Thread Group** (`Inject Downstream Fault`, 1 thread, 1 loop) first calls
   `toxic-control.groovy` (parameters `inject`) to add a `reset_peer` toxic to the
   `downstream` Toxiproxy proxy (`qe-harness/toxiproxy/proxies.json`, Task 14) fronting
   `downstream-stub` -- confirmed empirically (direct `curl` against the running compose
   stack) to reset the TCP connection immediately, giving the same instant, deterministic
   failure `BreakerBehaviourTest`'s own in-process `blackhole()` stub has, with no
   read-timeout wait needed to observe it. It then reads `HarnessConfig.smokeMode()`
   (Task 15) once, writes the resulting load-iteration count into `props` (JMeter's
   cross-thread shared store), and zeroes this run's tallies -- same reason TST-031's
   plan.jmx zeroes its own props: a second run in the same SUT process (this module's own
   JUnit fixture running clean-then-defect-then-restore) must never add its tallies on top
   of a previous run's leftovers.
2. **Main Thread Group** (`Fault Load`, 1 thread x 8-12 loops depending on smoke mode)
   fires `GET /quotes/Q1` sequentially while the fault injected above is still active.
   A single thread is deliberate: I1-I3 are correctness checks, not throughput/concurrency
   invariants, so there is nothing here that needs TST-021's multi-thread contention or
   TST-031's rate-shaping timer. `tally-response` (a `JSR223PostProcessor`) tallies every
   response into `props` -- total calls, `5xx` count, and shape-check violations (any `200`
   response whose `degraded`/`source` pairing does not match `DegradedResponse`'s own two
   constructors: `degraded=true` must pair with `source="cache"`, `degraded=false` with
   `source="live"`).
3. **TearDown Thread Group** (`Restore Downstream and Verify Recovery`, 1 thread, 1 loop)
   runs only after every `Fault Load` thread has finished, and runs two samplers in a fixed
   order:
   - `restore-downstream` (`toxic-control.groovy`, parameters `remove`) -- **always first,
     and unconditional**. See "Guaranteed restore" below.
   - `assert-degradation` -- evaluates I1/I2 against the tallies above, then performs I3's
     own recovery poll (now that the fault is genuinely gone), and calls `EvidenceEmitter`
     to write one fragment to `traceability/runs/`.

## Guaranteed restore

**This is the module's single most important correctness property**, the fault-injection
analogue of Task 16's defect-flag cleanup-on-failure guarantee: a fault left injected on
Toxiproxy would corrupt every subsequent module run against this same long-lived
`docker compose` container, not just this run's own result.

`restore-downstream` is the first sampler in the TearDown Thread Group, before
`assert-degradation` -- not because JMeter enforces an assertion-failure barrier there (it
does not; a `JSR223Sampler` marking itself unsuccessful, or a `2xx`/`5xx` HTTP response, is
just a sample result, and does not stop the thread group the way a `stopthread`/`stoptest`
`on_sample_error` setting would), but so that removal never depends on `assert-degradation`
having run cleanly first. `Fault Load`'s own `on_sample_error=continue` means nothing in
this plan ever aborts the TearDown Thread Group outright, so `restore-downstream` runs
every time, whether the run that preceded it was clean, defect-active, or genuinely failed
an invariant.

`Tst035ModuleTest#restoresTheProxyEvenWhenAssertionsFail` proves this directly rather than
assuming it: it runs the module with `breaker-disabled` active (which makes I1 fail for
real -- every call while the fault is active surfaces as a genuine `500`, not a degraded
`200`, confirmed by `reportsFailureAgainstTheBreakerDisabledDefect`), then asserts the
`downstream` proxy's own `toxics` list is empty afterward, via a small `ToxiproxyProbe`
test helper reading Toxiproxy's control API directly. This was verified empirically
against the real stack (`make up PROFILES="core resilience"`, direct `curl` against
`:8474`), not just inferred from the plan's structure.

## Why I3 is checked by polling, not by reading breaker state

The reference SUT does not expose a Spring Boot Actuator endpoint (confirmed empirically:
`GET /actuator/health` returns `404`), so unlike `BreakerBehaviourTest` -- which is
in-process and can read `CircuitBreakerRegistry` directly -- this module has no way to
observe the breaker's own OPEN/CLOSED/HALF_OPEN state from outside the JVM. I3 is instead
checked the same way any black-box HTTP client would: poll `GET /quotes/Q1` after removing
the fault until a response reports `degraded=false`, within a bounded budget (15s full run,
8s smoke run -- both wide margins over `application.yml`'s 2-second
`waitDurationInOpenState`, the same rationale `BreakerBehaviourTest`'s own 20-second
`awaitClosed()` budget uses against that identical 2-second wait duration). This is
strictly weaker than the canonical archetype's own I1/I3 (which measure the actual
state-transition timing and the half-open probe count), but it is the correct-for-this-tool
approximation: an external caller only ever observes the *response*, never the breaker's
internal state machine, and "the response eventually stops being degraded" is exactly what
a real caller of this API would mean by "the breaker recovered."

## Investigation: does breaker state leak between runs? (the TST-031 lesson, applied here)

TST-031's own follow-up fix (`RateLimitResetController`, Task 17) exists because its
`TokenBucket` accrues state continuously with no reset, so a prior run's leftover burst
budget could silently corrupt a later run's rate measurement -- confirmed empirically by
running it three times back-to-back against a long-lived container. Before finishing this
module, the same question was asked of the circuit breaker: could a prior run leave the
breaker OPEN (or otherwise mid-transition) in a way that corrupts this module's own I1-I3?

**Finding: no reset endpoint is needed, and this was verified, not assumed.**

`Tst035ModuleTest`'s three tests were run **six times back-to-back** (two full passes
through `assertsDegradedResponseRatherThanFiveHundred` /
`reportsFailureAgainstTheBreakerDisabledDefect` / `restoresTheProxyEvenWhenAssertionsFail`,
`mvn -q -pl jmeter test -Dtest=Tst035ModuleTest`, then immediately re-run without tearing
the compose stack down) against the same long-lived `reference-sut` container that had
already been running for over 50 minutes and had already been exercised by ad-hoc manual
fault-injection probes beforehand. All eighteen individual test executions passed, every
time, with no flakiness of the kind TST-031 exhibited.

The reason this category of bug does not reproduce here is structural, not luck:

- Resilience4j's breaker is configured with `automaticTransitionFromOpenToHalfOpenEnabled:
  true` and a short `waitDurationInOpenState` (2s) -- unlike `TokenBucket`, which never
  changes state without a call arriving, the breaker self-transitions out of OPEN on a
  background schedule regardless of whether any request arrives. It cannot get
  *permanently* stuck the way an un-reset token bucket's burst budget can silently persist
  indefinitely.
- Every one of I1-I3 is a **boolean correctness check** (no `5xx`, correct response shape,
  eventual recovery), not a **quantitative measurement** like TST-031's admitted-rate
  number. TST-031's bug mattered because leftover bucket state changed the *measured rate*.
  Here, whether the breaker happens to start a run already OPEN (from a prior run's
  leftover state) or freshly CLOSED makes no observable difference to this module's own
  checks: both a leftover-OPEN breaker and a freshly-CLOSED one produce the exact same
  degraded response the instant a real call is attempted against the still-active fault
  (either the call fails and falls back, or it is short-circuited straight to the same
  fallback) -- confirmed directly with manual `curl` probes against the running stack
  before writing `plan.jmx` (see the task's own working notes: the very first call after
  injecting the toxic already returned `degraded=true`, regardless of prior breaker state).
- Because I3 itself is "eventually observe `degraded=false`", any run that reports I3
  PASSED has, by construction, already observed the breaker admit a real call successfully
  -- which is only possible from CLOSED or a successfully-probing HALF_OPEN, i.e. exactly
  the state a well-behaved run should leave behind for the next one.

No `/_test/reset/...` endpoint (the TST-031 pattern) was added to `reference-sut` for this
capability, and none of the shared harness fixtures (`ModuleRunner`, `run-jmeter.sh`) were
touched.

## Running it

```
make up PROFILES="core resilience"   # from qe-harness/, brings up postgres + reference-sut +
                                      # toxiproxy + downstream-stub -- TST-035 is the one
                                      # module that needs `resilience`, not just `core`
./bin/run-module.sh TST-035          # from qe-harness/
```

Exits non-zero exactly when the emitted fragment's result is `failed` (see
`bin/run-jmeter.sh`). Set `HARNESS_SMOKE_MODE=true` in the environment to get the shorter
load-iteration count and recovery-poll budget; I1-I3 are asserted identically either way
(see "Why I3 is checked by polling" above -- there is no NFR-cited performance threshold
in this module to record `not-evaluated`, unlike TST-031's `peak_rps`; the invariants here
are all correctness, not throughput, so smoke mode only shortens wall-clock time).

## Defect proof

The defect is injected on the running SUT over HTTP, not via a process environment
variable (see `DefectController`/`DefectFlags` in `reference-sut`, and `ModuleRunner`'s own
javadoc for why):

```
curl -X POST http://localhost:8080/_test/defect/breaker-disabled            # 204
curl -X POST http://localhost:8474/proxies/downstream/toxics \
     -H 'Content-Type: application/json' \
     -d '{"name":"manual-probe","type":"reset_peer","stream":"downstream","toxicity":1.0,"attributes":{"timeout":0}}'
HARNESS_SMOKE_MODE=true ./bin/run-module.sh TST-035                          # must report I1 FAILED
curl -X DELETE http://localhost:8474/proxies/downstream/toxics/manual-probe # 204, always clears it
curl -X DELETE http://localhost:8080/_test/defect                           # 204, always clears it
```

With `breaker-disabled` active, `DownstreamClient.fallback` stops masking the downstream
failure and instead rethrows, so `QuoteController` surfaces it as a genuine `500` instead of
the declared degraded `200` -- this is the capability's proof that it fails for the right
reason. `Tst035ModuleTest`'s `reportsFailureAgainstTheBreakerDisabledDefect` test drives
this exact sequence via `ModuleRunner`, which performs the HTTP defect activate/clear
itself; `plan.jmx`'s own `toxic-control.groovy` performs the Toxiproxy toxic add/remove.
