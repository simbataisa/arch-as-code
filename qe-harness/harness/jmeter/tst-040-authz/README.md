# TST-040 -- AuthZ Matrix and Token Lifecycle (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter. Canonical archetype:
[authn-authz-token-lifecycle.md](../../../../knowledge-base/testing/archetypes/authn-authz-token-lifecycle.md)
(TST-040, catalog IDs SEC-010/SEC-006/SEC-002/SEC-005/SEC-011/SEC-001/MOB-003).

| ID | Invariant |
|---|---|
| I1 | Every authorisation-matrix cell's outcome matches its `expected_verdict` under the three-outcome oracle (`allow`/`deny`/`error`) |
| I2 | Measured maximum accepted `exp` offset stays within the declared clock-skew tolerance |

These two are a deliberately simplified subset of the canonical archetype's own I1-I8 (which
also cover a direct-to-service gateway-bypass check, revocation-propagation latency,
refresh-token rotation, client-bound token replay, mTLS peer identity, and entitlement-change
propagation): the reference SUT's authz capability (Task 9) is a single service with no
fronting gateway to bypass, no mTLS, no client-bound tokens, and no entitlement-change event
source, so I2 (the archetype's own gateway-bypass check), I5-I8 do not apply to it. This
module's own I1 corresponds to the archetype's I1; this module's I2 corresponds to the
archetype's own clock-skew half of I3 (the token-lifecycle negative-path cases -- expired,
wrong-audience, tampered-signature, algorithm-confusion -- are exercised in-process by
`reference-sut`'s own `TokenLifecycleTest`/`AuthzMatrixTest`, Task 9, not restated here).

Defect proof: with the `authz-missing-marker` defect active (see below) this module MUST
report I1 failed.

## The three-outcome oracle (I1's whole point)

`assert-authz.groovy`'s `classify(status, decisionHeader, body, endpoint)` never collapses to
a two-outcome `status < 400 ? allow : deny` form:

- **`allow`** -- a `2xx` status **and** the response body carries the expected resource
  payload (`ProtectedController`'s own `{"resource": "<name>"}` shape).
- **`deny`** -- a `401` or `403` status **and** the response carries an explicit
  `X-Authz-Decision: deny` header (`AuthzDecisionFilter`, Task 9).
- **`error`** -- anything else: a bare `401`/`403` with no decision marker, any `5xx`, an
  unparseable or wrong `2xx` body, or a connect/read failure. **A `401`/`403` with no marker
  is deliberately `error`, never `deny`** -- see `AuthzDecisionFilter`'s own Javadoc in
  `reference-sut`: a denial-shaped status code with no explicit decision marker means
  something went wrong in the SUT's own security handling (a crashed policy layer, a
  misconfigured filter chain), not that the SUT made a deliberate, correct denial. Scoring
  it as a clean deny would let exactly that failure mode pass a matrix sweep undetected --
  the fail-open-vs-fail-closed confusion [SEC-010 Attribute-Based Access
  Control](../../../../knowledge-base/patterns/security/attribute-based-access-control.md)
  exists to prevent.

Every matrix cell is scored against its own `expected_verdict`, read directly from
`authz-matrix.csv`, **never against a second live code path**: this reference SUT has no
separate gateway hop to bypass (the full archetype's own I2), so there is no second path to
compare against here in the first place, and comparing two paths that happened to agree on
the same wrong answer would prove nothing about correctness even if there were.
`Tst040ModuleTest`'s `classifiesBareForbiddenAsErrorNotDeny` pins exactly this: with
`authz-missing-marker` active, every deny cell's `401`/`403` classifies `error` instead of
`deny`, so I1 fails and the whole run reports `FAILED` -- proving this module catches the
defect rather than quietly absorbing a markerless denial into a passing deny count.

## What this module drives

`plan.jmx` runs the 12-cell matrix (`authz-matrix.csv`: `reader`/`writer`/`admin`/`anonymous`
x `/protected/read`/`/protected/write`/`/protected/admin`) against the reference SUT's authz
capability (Task 9):

1. **setUp Thread Group** (`Mint Role Tokens and Zero Tallies`, 1 thread, 1 loop) mints one
   access token per known role (`reader`, `writer`, `admin`) via `POST /auth/token`, storing
   each in `props` (JMeter's cross-thread shared store) -- once here, not once per cell, so
   the 12-cell sweep below spends its calls entirely on the authorization decision itself.
   Also zeroes `tst040_cell_keys`, the same "clear tallies before load" idiom TST-031/TST-035's
   own setUp Thread Groups use, so a second run in the same SUT process (this module's own
   JUnit fixture running clean-then-defect) never mixes its cell results with a previous run's
   leftovers.
2. **Main Thread Group** (`Authz Matrix Sweep`, 1 thread, 12 loops -- one per CSV row, via a
   **CSV Data Set Config** with `recycle=false`/`stopThread=true`) resolves each row's
   `Authorization` header (`resolve-bearer-header`: `anonymous` sends no bearer credential at
   all; any other role looks up the token setUp already minted for it), calls
   `GET ${endpoint}`, and records the RAW response (status, the `X-Authz-Decision` header
   value if present, and the body) into `props`, keyed by `role|endpoint` --
   **`record-cell-result` performs no classification of its own**. Correctness here is a
   fixed 12-cell matrix, not a load/throughput invariant, so a single thread is deliberate
   (the same reasoning TST-035's own single-thread `Fault Load` Thread Group documents).
3. **TearDown Thread Group** (`Evaluate Authz Invariants`, 1 thread, 1 loop) runs only after
   every cell has been recorded -- `assert-authz.groovy` is the **one and only place** the
   three-outcome classifier is implemented, reads every cell back from `props`, evaluates I1,
   runs its own clock-skew sweep for I2, and calls `EvidenceEmitter` to write one fragment to
   `traceability/runs/`.

Keeping classification entirely inside `assert-authz.groovy` -- rather than also classifying
inline in `record-cell-result` -- means there is exactly one implementation of the oracle to
keep correct; a second, inline copy in `plan.jmx` risked drifting out of sync with the real
one and silently under- or over-scoring cells.

## Clock-skew measurement (I2): a genuine measurement, not an assertion against a literal

`assert-authz.groovy` presents progressively staler tokens -- offset `0, 1, 2, ...` seconds
past `exp` -- against `GET /protected/read`, using tokens minted by the reference SUT's own
`POST /_test/token/expired` (see "The clock-skew HTTP door" below), and records the **largest
offset still accepted** as `maxAcceptedOffset`, stopping at the first rejection. I2 then
asserts that *measured* quantity against `app.authz.clock-skew-seconds`
(`reference-sut/src/main/resources/application.properties`, default `5`) -- the same property
`JwtService` wires straight into JJWT's own `clockSkewSeconds(...)`, so this checks the real,
running validator's behaviour, never a duplicated literal. This is the same sweep-to-
first-rejection shape `TokenLifecycleTest#expiredTokenIsNotAcceptedBeyondDeclaredSkew`
(Task 9) uses in-process; this module runs the equivalent measurement over real HTTP, from a
separate process, per the archetype's own instruction that the harness "mints an otherwise
valid token at an offset in a monotonically increasing sweep past `exp`, records the largest
offset still accepted".

A run against the clean SUT typically measures `maxAcceptedOffset` a little *below* the
declared `5`s (`4`s was observed repeatedly in this environment), not exactly at it: the sweep
mints each token via one HTTP round-trip and validates it via a second, so real network and
processing latency between mint and validation eats a small, variable sliver out of the
nominal 5-second window before the validator ever sees the token. This is expected and
correct -- it is exactly what "measuring the real accepted offset" rather than "asserting a
hardcoded number" is supposed to surface -- and I2's own assertion
(`maxAcceptedOffset <= declaredClockSkewToleranceSeconds`) is written the right direction for
it: a measured ceiling *below* the declared tolerance still passes; a measured ceiling *above*
it (the Failure Taxonomy's "clock-skew tolerance too wide" defect) is what would fail I2.

`declaredClockSkewToleranceSeconds` (`5L`) and `sweepMaxSeconds` (`30L`, comfortably above the
declared tolerance so the sweep always finds a real rejection) are hardcoded literals in
`assert-authz.groovy`, not read from any HTTP surface -- the same limitation
`assert-ratelimit.groovy` documents for its own `configuredLimitRps`: this reference SUT
exposes no endpoint that reads `app.authz.clock-skew-seconds` back to an external caller, so
this value must be kept in sync by hand if that property ever changes. `Tst040ModuleTest`'s
`measuresRatherThanAssertsClockSkewTolerance` pins that the emitted I2 description reports the
*measured* offset (it asserts the description text contains "accepted exp offset"), not that
any particular number appears in it.

### The clock-skew HTTP door

`JwtService#mintExpiredAccessToken` (Task 9) is a plain Java method with no HTTP door of its
own -- deliberately: a real client must never be able to ask this SUT to mint a token with an
arbitrary expiry. This module runs as a separate process against an already-running
`docker compose up` container (the same "harness modules can only reach in-process state over
HTTP" constraint `RateLimitResetController`'s own Javadoc documents for TST-031), so unlike the
in-process `TokenLifecycleTest`, it has no way to call that method directly.
`TokenExpiryTestController` (`POST /_test/token/expired`, a Task 19 follow-up addition to
`reference-sut`) is that HTTP door -- the same `_test`-prefixed, test-control-only convention
`DefectController` and `RateLimitResetController` already use -- scoped narrowly to exactly
the one capability the sweep needs (mint an otherwise-valid, already-expired token for a known
role) rather than exposing the signing key or a general-purpose expiry override.

## Running it

```
make up PROFILES=core          # from qe-harness/, brings up postgres + reference-sut
./bin/run-module.sh TST-040    # from qe-harness/
```

Exits non-zero exactly when the emitted fragment's result is `failed` (see
`bin/run-jmeter.sh`).

## Defect proof

The defect is injected on the running SUT over HTTP, not via a process environment variable
(see `DefectController`/`DefectFlags` in `reference-sut`, and `ModuleRunner`'s own Javadoc for
why):

```
curl -X POST http://localhost:8080/_test/defect/authz-missing-marker   # 204
./bin/run-module.sh TST-040                                            # must report I1 FAILED
curl -X DELETE http://localhost:8080/_test/defect                      # 204, always clears it
```

With `authz-missing-marker` active, `AuthzDecisionFilter` returns immediately without setting
`X-Authz-Decision` on any response (confirmed directly: `curl` against `/protected/admin` with
the defect active returns a bare `401`, no `X-Authz-Decision` header at all; clearing the
defect and repeating the identical call returns `401` with `X-Authz-Decision: deny`) -- so
every deny cell in the matrix classifies `error` instead of `deny`, I1 fails, and the run
reports `FAILED`. `Tst040ModuleTest`'s `classifiesBareForbiddenAsErrorNotDeny` drives this
exact sequence via `ModuleRunner`, which performs the HTTP defect activate/clear itself.
