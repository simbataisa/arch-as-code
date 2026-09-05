# Wave 17 — QE Harness Coverage Expansion (Design)

**Status:** Draft — awaiting review
**Date:** 2026-09-03
**Owner:** @qe-lead
**Gating reviewers:** @tester-qe (oracle fidelity, defect specificity), @devsecops-engineer (broker in CI, dependency surface)
**Predecessor:** Wave 16 — QE Harness Reference Implementation (`2026-08-24-wave-16-qe-harness-design.md`)

---

## 1. Goal

Wave 16 built a runnable harness covering 7 of the 24 test archetypes. The other 17 answer
HTTP `501`. Wave 17 implements 8 of them and completes one entire archetype family.

The headline outcome is not the count. It is that `GET /_capabilities` stops overstating what
the harness can prove: `CapabilityRegistry.java:17` hardcodes 7 implemented archetypes, and
`TST-043` sits in that list while implementing **none** of its own I1–I6 invariants. Wave 17
raises real coverage and corrects the record.

Coverage by family, before and after:

| Family | Before | After |
|---|---|---|
| A Correctness | 1/6 | 3/6 (+TST-020, TST-023) |
| B Messaging | 1/5 | **5/5 — complete** |
| C Load | 1/4 | 2/4 (+TST-034) |
| D Resilience | 1/2 | 1/2 |
| E Data | 1/3 | 2/3 (+TST-037) |
| F Security | 1/2 | 1/2 |
| G Observability/Client | 1 partial /2 | 1 partial /2 |

Family B completing matters more than the arithmetic: `TST-030` was already done, so four
modules close a family, and the broker topology they share becomes the substrate every future
messaging archetype reuses.

## 2. Scope

**In scope — 8 archetype modules:**

| Slice | Archetypes |
|---|---|
| A (existing SUT) | TST-034 Blended Journey, TST-037 Read-Model Convergence, TST-023 Concurrent Limit |
| B (broker) | TST-026 Transform/Route, TST-027 Ordering, TST-028 Fan-out/Fan-in, TST-029 Delivery/DLQ |
| C (post-broker) | TST-020 Idempotency & Replay |

**In scope — integrity fixes (Phase 0):**

1. **Stale module counts and per-family framing.** `qe-harness/README.md:147` claims "One
   archetype per family, so all four tools and three of the four oracle types are exercised"
   and carries a 7-row table; `knowledge-base/testing/README.md:16` says "7 of the 24". Wave 17
   makes it 15 modules across 7 families. **Note:** the adjacent `golden-dataset` sentence in
   `qe-harness/README.md` is *not* stale — it says that oracle is not implemented as any family
   representative's primary oracle, landing with `TST-022`/`TST-038`, and since neither is in
   Wave 17's scope that remains true. Leave it alone.
2. `knowledge-base/testing/archetypes/data-protection-masking-tokenisation.md` (TST-041)
   contains **one NUL byte** at offset 48027, line 618 — `file` reports it as `data` rather
   than text, and grep silently returns nothing against it. It sits **inside a Groovy string
   literal**: `join("<NUL>")`. This is a semantics decision, not a deletion — `tr -d '\0'`
   yields `join("")`, an empty separator, which lets a marker match across two adjacent values
   and arguably inverts line 617's stated intent. The replacement must be chosen deliberately.
   (Archetype docs are slug-named, not `TST-0NN.md`.)
3. **`TST-025`'s `primary_tool` conflict only.** Three locust-only rows (`BSP-003`, `BSP-010`,
   `SEC-009`) move to `jmeter`. The two `jmeter` rows must **not** be touched: `SEC-010` also
   carries `TST-040` and `BSP-019` also carries `TST-032`, so editing them changes those
   archetypes' best-fit sets — and TST-040 is an implemented `jmeter` module, so flipping
   `SEC-010` would break check 2 for a working module. **`TST-036` is deferred**: its lone
   dissenter `MOB-006` (`k6`) also carries `TST-043`, an implemented `k6` module, so no
   mechanical edit fixes it — the row must lose `TST-036` or be split, which needs an owner.
   Both conflicts are latent (neither archetype has a `modules.yml` row), so neither blocks
   this wave.
4. `TST-043` re-labelled `coverage: partial` with an honest `partial_reason`. It ships 4
   substitute server-side HTTP invariants in place of its 6 real client-side ones.

**In scope — CI wiring:** `validate-testing-coverage.py` and `render-testing-coverage.py --check`
into `.gitlab-ci.yml`. Deferred by Wave 15 and still manual-only. Wave 17 mutates the very
coverage data these scripts validate, so wiring them here protects this wave's own output.

**Out of scope, deliberately:**

- `TST-017`–`TST-019` are reserved headroom, not archetypes. Do not backfill. (`TST-016` is the
  harness itself.)
- `TST-043`'s real client-side invariants — I1/I2/I6 need an offline client, I3/I4 a rendered
  DOM, I5 `k6/browser` against a real page. No such application exists in this repo.
- `TST-025` and `TST-036` **modules**. Their corpus conflict is fixed in Phase 0, but the
  modules themselves wait for a later wave.
- New harness tools. JMeter, Gatling+Karate, k6 and Locust are the toolchain; all 8 modules
  are JMeter (see §7, check 2).

## 3. Decisions Taken During Brainstorming

1. **Both cheap and expensive slices in one wave**, sequenced cheap-first, so Phases 0–1 ship
   as a coherent wave even if the broker work overruns.
2. **`TST-020` moves last.** Its I7 is "dedup survives broker redelivery" — unsatisfiable
   before the broker exists. Sequencing it after Phase 2 buys full I1–I7 coverage instead of a
   partial.
3. **Topology as Spring `@Bean Declarables`, not a mounted `definitions.json`.** Less compose
   surface; lands in code where it is unit-testable.
4. **No catch-all `#` binding on `qe.route`.** A catch-all would make TST-026's I2 ("zero
   messages on the default route") trivially true and therefore worthless.
5. **`partial` + `partial_reason` over an overstated `full`.** This is the rule the wave rests
   on, and the exact sin Phase 0 corrects on `TST-043`.
6. **`TST-029` I2 gets a real broker restart, gated out of CI** — Toxiproxy severance would
   prove reconnection, not durable-queue survival, and dressing one up as the other is the
   failure mode this whole harness exists to prevent.
7. **Two uncited bounds become application config, not NFR amendments** (§7.1). The governed
   NFR spine stays untouched; traceability is a §11 follow-up if the board wants it.
8. **Migrations split `V3`/`V4`/`V5`, one per capability**, rather than a single `V3` carrying
   three tables. A monolithic migration would couple three otherwise-independent tasks — the
   reservation task could not commit without also creating the outbox and idempotency tables it
   never touches. Note `idempotency_key` must be added to
   `AbstractLedgerIntegrationTest.resetLedgerFixture()`'s TRUNCATE list explicitly: the
   existing `CASCADE` only reaches tables with an FK to `account`, so a standalone table would
   silently leak state between tests.

## 4. Architecture

**Deliberately no new harness architecture.** Wave 16's module pattern is the contract:

- `traceability/modules.yml` is the single binding — archetype → tool, path, coverage
- exactly one `assert-*.groovy` per module (the runner requires precisely one)
- oracles consumed from `harness/common`; all four exist, so **no archetype is oracle-blocked**
- evidence to `traceability/runs/`, shape fixed by `evidence.schema.json`
- one paired defect flag per module, proving the assertions fail when the invariant breaks

Eight repetitions of a proven unit. A JMeter sibling adds zero build-system work — `pom.xml`
globs `*/plan.jmx` and `bin/run-module.sh` dispatches off `modules.yml`.

**The one genuinely new architectural element** is promoting the `messaging` compose profile
from declared-but-never-started to a live RabbitMQ topology with a producer/consumer/DLQ path
in the SUT. `docker-compose.yml:136-147` declares `rabbitmq:3.13-management-alpine` with a
healthcheck and no `environment`, `volumes` or `depends_on`; the SUT has no AMQP dependency and
no messaging Java code at all. This is the wave's main risk and its main durable asset.

### 4.1 Phasing

| Phase | Content | Rationale |
|---|---|---|
| 0 | Four integrity fixes + CI wiring | Cheap; unblocks later waves; protects this wave's data |
| 1 | TST-034, TST-037, TST-023 on the existing SUT | No new infra; ships independently |
| 2 | Broker topology, then TST-026/027/028/029 | One shared infra investment over four archetypes |
| 3 | TST-020 against the real broker | Full I1–I7 rather than partial |

## 5. Components

### 5.1 The per-module unit

Eight repetitions, ~400–500 new lines each, following `TST-021` exactly:

| Artefact | Role |
|---|---|
| `traceability/modules.yml` row | binding: archetype → tool / path / coverage / defect flag |
| `harness/jmeter/tst-0NN-<name>/plan.jmx` | setUp (reset+seed) / main / tearDown thread groups |
| `.../assert-*.groovy` | evaluates I1..In, calls `EvidenceEmitter` — exactly one per module |
| `.../README.md` | invariant table, defect proof, run instructions |
| `harness/jmeter/src/test/java/.../Tst0NNModuleTest.java` | 2 tests: clean → PASSED, defect → that invariant FAILED |

Reused unchanged: `bin/run-module.sh`, `bin/run-jmeter.sh`, `support/ModuleRunner.java`,
`harness/common` oracles and emitter.

### 5.2 New SUT surface

| Phase | Component | Detail |
|---|---|---|
| 1 | `capability/reservation` (new) | `POST /reservations`, `POST /reservations/{id}/release`, `GET /reservations/utilisation`, TTL sweeper, declared-TZ window. `ratelimit/TokenBucket.java` is a rate limiter, not a reservation counter — this is new, not an extension |
| 1 | `capability/reporting` (extend) | `GET /reporting/lag` returning **both p95 and p99** (TST-037 I2 forbids the mean), `POST /reporting/refresh` doing `REFRESH MATERIALIZED VIEW CONCURRENTLY` plus `report_refresh_timestamp` update |
| 1 | `harness/common/ProfileResolver` (new) | Sibling to `ThresholdResolver`; the **first code in the repo to read `profiles/*.yml`**. Also populates `mixed.yml`'s `blend_ref` (currently `null`) and adds per-journey tagged metrics |
| 1 | `POST /_test/seed` | `SyntheticDataSeeder` has no HTTP trigger today; modules need reseeding without a container restart |
| 2 | `capability/messaging` (new) | Spring AMQP, topology as `@Bean Declarables`, producer, consumer, resequencer, aggregator, DLQ handling |
| 2 | `GET /messaging/dlq/depth`, `/messaging/published-log`, `/messaging/emissions` | Harness-side ground truth — the harness must not trust the broker's own accounting |
| 3 | `POST /transfers` idempotency | `Idempotency-Key` header, payload hash + stored response, 409 on mismatch, TTL exposure |

**One `V3` migration** carries three tables: `outbox` (with `published_count` for TST-037 I4 —
no outbox exists today), `reservation`, `idempotency_key`.

`V2__reporting_view.sql` already provides the `account_balance_report` materialized view with a
unique index (enabling `REFRESH CONCURRENTLY`) and the `report_refresh_timestamp` companion
table. Only the HTTP surface exposing lag is missing.

### 5.3 Broker topology

Minimal but sufficient for all four Slice B archetypes:

| Object | Type | Serves |
|---|---|---|
| `qe.in` | direct exchange | publish entry point |
| `qe.route` | topic exchange, bindings `pay.domestic.*` / `pay.intl.*` | TST-026 I2 — **no catch-all binding** |
| `qe.q.route.domestic`, `qe.q.route.intl` | queues | TST-026 |
| `qe.q.unroutable` | queue on **`qe.route`**'s alternate-exchange | TST-026 I2 verdict, TST-029 I1 negative path. The alternate exchange must sit on `qe.route`, not `qe.in`: I2 is about `qe.route`'s bindings, and an unmatched `pay.*` key would otherwise be dropped by the broker rather than parked where a depth can be read |
| `qe.q.sequence` | queue, `x-single-active-consumer: true` | TST-027, scope declared `per_key` |
| `qe.fanout` → `qe.q.branch.{a,b,c}` + `qe.q.aggregate` | fanout | TST-028 I1/I3/I4 |
| `qe.q.work` | `x-dead-letter-exchange: qe.dlx` + delivery limit | TST-029 I1/I3/I6 |
| `qe.dlx` → `qe.q.dlq` | DLX + DLQ | TST-029 I1/I3/I5 |
| `qe.q.retry.{1,2,3}` | per-queue `x-message-ttl` → DLX back to `qe.q.work` | TST-029 I4 backoff ladder |

All queues `durable: true` for TST-029 I2.

**No hard `depends_on`.** `reference-sut` is in compose profile `["core"]`; `broker` is in
`["messaging"]`. Adding `depends_on: broker: {condition: service_healthy}` would make
`docker compose --profile core up` fail outright, and since `reference-sut`'s healthcheck hits
`/_capabilities`, a broker-connection failure at startup would mark the container unhealthy and
break all seven existing modules. So the AMQP connection must be **lazy**: `--profile core`
boots unchanged with no broker, and messaging modules require `PROFILES="core messaging"`.
Broker config arrives as `SPRING_RABBITMQ_*` env vars on the compose service — matching how the
datasource is already supplied, since neither config file declares `spring.datasource.*` at all.
`spring-boot-starter-amqp` is BOM-managed by Boot 3.5.16, so it takes no explicit `<version>`
(the `spring-boot-starter-aop` precedent); `org.testcontainers:rabbitmq` likewise from
testcontainers-bom 1.21.4, test-scoped.

### 5.4 Defect flags

Eight new entries in `DefectFlags.KNOWN_FLAGS` plus one `isActive` branch each, following the
`TransferService.java:46` shape. Each breaks **exactly one** invariant:

| Archetype | Flag | Breaks |
|---|---|---|
| TST-020 | `idempotency-key-ignored` | I1 only (I2/I4 stay structurally intact) |
| TST-023 | `reservation-overcommit` | I1/I2 only |
| TST-034 | `journey-starved` | I3 only (I1/I2 hold) |
| TST-037 | `outbox-published-count-stale` | I4 only |
| TST-026 | `route-default-fallthrough` | I2 |
| TST-027 | `resequencer-emits-on-arrival` | I1 |
| TST-028 | `aggregate-emitted-incomplete` | I1 |
| TST-029 | `dlq-bypass-drop` | I1 |

### 5.5 Fixtures

`SyntheticDataSeeder` takes a caller seed and inserts one account per `SyntheticNames.NAMES`
entry plus 30 transfers × 2 ledger entries. Extensions needed: more accounts for realistic
TST-034 contention; an `idempotency_key` fixture set; `report_refresh_timestamp` rows and outbox
entries; message fixtures including a **Vietnamese-diacritic field** (TST-026 I6 asserts
byte-identical diacritic survival) and enum-domain values for I3.

## 6. Run Flow

Unchanged pipeline: `bin/run-module.sh` reads `modules.yml` → `bin/run-jmeter.sh` → `plan.jmx`
runs setUp/main/tearDown → the single `assert-*.groovy` evaluates invariants → `EvidenceEmitter`
writes `traceability/runs/<ts>-TST-0NN.json` → gate check 7 validates it against the schema.

Defect proof is a second pass over the same pipeline: `ModuleRunner` activates the flag over
HTTP → `DefectController` → `DefectFlags.isActive` → the service takes its broken branch → the
module runs → the assertion must report that one invariant FAILED → flag cleared.

**Messaging flow:** publish → `qe.in` → `qe.route` matches `pay.domestic.*` / `pay.intl.*`;
anything else diverts via alternate-exchange to `qe.q.unroutable`, whose depth is TST-026 I2's
verdict. The resequencer takes a deliberately **shuffled** publish order into `qe.q.sequence`
under single-active-consumer, and TST-027 I1 compares the emissions log against `sorted(seq)`.
Fan-out sprays three branch queues into an aggregate, correlated per window. `qe.q.work`
dead-letters past its delivery limit, with a retry ladder whose **jitter must actually vary the
TTLs** or TST-029 I4's `distinct_intervals > 1` fails against its own backoff.

## 7. Evidence and Gate Compliance

`evidence.schema.json` is `additionalProperties: false` throughout — no improvised keys.

- `archetype` matches `^TST-0[2-4][0-9]$` — all 8 fit
- `module` must be `jmeter` for all 8
- `oracle` is `contract-schema` for **TST-026 only**; `invariant-assertion` for the other 7
- `thresholds[].result: not-evaluated` **requires** a `reason` — the normal smoke-mode path

The traceability gate is **7 checks**, not 6 (`scripts/validate-harness-coverage.py:4`):

| # | Check | Wave 17 obligation |
|---|---|---|
| 1 | Archetype doc exists with `Catalog ID` | All 8 verified clean (0 NUL bytes) |
| 2 | `tool` == corpus best-fit | `jmeter` for all 8, computed and verified |
| 3 | `path` exists on disk | Create 8 module directories |
| 4 | `coverage ∈ {full, partial}`, partial needs reason | 2 partials (TST-027, TST-037) + the `TST-043` relabel |
| 5 | No 13–19 consecutive digits under `qe-harness/` except `traceability/runs/` | **Slice B hazard** — hyphenated correlation IDs (`corr-a1b2-c3d4`), ISO-8601 not epoch-millis |
| 6 | `threshold_ref` → `NFR-NNN#anchor` that resolves | ✅ resolved — see §7.1 |
| 7 | Evidence validates against schema | Correct enums per above |

Also enforced: no duplicate `modules.yml` archetype entries. And `CapabilityRegistry.IMPLEMENTED`
must extend 7 → 15, or `/_capabilities` keeps reporting the wrong thing.
`render-harness-coverage.py` must be re-run so `harness-coverage.md` is not stale.

**Three existing tests fail by design and must be rewritten**, not deleted — all in
`CapabilityRegistryTest`: `waveSixteenImplementsExactlySevenCapabilities` (asserts `7` and the
exact set), `seventeenArchetypesRemainDeclared` (asserts `17`), and
`statusOfDeclaredButUnimplementedIsDeclared` (asserts `TST-022` is `declared` — safe, since
Wave 17 does not implement TST-022). `CapabilityControllerTest` must also be checked in case it
probes one of the eight. Wave 17 keeps every guard, restated for the new set.

### 7.1 Threshold provenance — how check 6 is satisfied

Check 6 validates **only** `qe-harness/profiles/_nfr-thresholds.yml`; a bound declared anywhere
else is invisible to it. Auditing the five NFR docs against Wave 17's four threshold-shaped
needs:

| Need | Anchor | Resolution |
|---|---|---|
| TST-034 per-journey p95 | ✅ `NFR-002#end-to-end-budgets-per-tier-customer-facing` | Already in use by three entries. Add per-journey entries sharing this one ref with differing `name`/`value` (e.g. `p95_latency_t1_ms: 500`) |
| TST-023 utilisation | ✅ N/A | I2's bound `L` is a per-account declared limit read from the SUT's own data — a fixture value, not an SLO. **No `threshold_ref`; citing one would be wrong** |
| TST-037 convergence bound | ❌ none | The corpus states lag only in **message counts** (Kafka, 1,000/10,000 msg). NFR-002's 50/80 ms "database write (sync replicated)" row is the *synchronous* write-path ack inside one request's budget — citing it for an asynchronous projection would be a category error |
| TST-029 DLQ depth | ❌ none | The entire NFR corpus contains **one** DLQ mention, in NFR-004's runbook prose, and it is not a threshold |

Wave 16's inherited constraints forbid modifying any NFR row, so the two uncited bounds are
declared as **application configuration**, not NFR citations:
`app.readmodel.convergence-bound-ms` and `app.messaging.dlq-alert-depth` in
`application.properties`. This follows the established `app.recon.freshness-window-seconds=300`
precedent — one declared value, read by both the service and its test, never duplicated as a
literal — and `application.yml`'s own rule that operational configuration "are resilience
configuration, not performance thresholds — no `NFR-*` citation is required". Neither bound
enters `_nfr-thresholds.yml`, so check 6 has nothing new to resolve and the governed NFR spine
is untouched.

**Accepted trade-off:** these two bounds are not traceable to an NFR SLO. If the architecture
board later wants that traceability, it is a governed NFR amendment in its own right — adding a
`### Read-model convergence budgets` section to NFR-002 and a DLQ-depth section to NFR-003 —
and is recorded in §11 as a follow-up rather than smuggled into this wave.

## 8. Failure Handling

Four outcomes. Conflating any two is the failure mode that matters most:

| Result | Means |
|---|---|
| `failed` | The SUT genuinely violates the invariant — the harness working as designed |
| `passed` | The invariant held under real exercise |
| `not-evaluated` | Not exercised this run; **requires a `reason`** |
| `not-implemented` | Declared, no module — today's 501s |

Reporting `passed` for something never exercised is the sin. `partial` + `partial_reason`
whenever an invariant is unreached; never `full`.

**`coverage` and per-run `result` are different claims, and must not be conflated.**
`modules.yml`'s `coverage` is a *static* statement about what the module implements;
an evidence file's per-invariant `result` is about one run. So:

- **TST-027 and TST-037 are `coverage: partial`** — I5 in each case is genuinely not
  implemented and never will be by this wave (no partitions in RabbitMQ; no CDC connector).
- **TST-029 is `coverage: full`** — I2's restart path *is* implemented; it simply emits
  `not-evaluated` with a reason on CI runs, and `passed`/`failed` on full runs.

Final tally across all 15 bound modules: **12 `full`, 3 `partial`** (TST-027, TST-037, and
TST-043 after its Phase 0 relabel).

**Every wait is bounded and declared.** TST-037 I6 makes exceeding the convergence bound a hard
FAIL, not an indefinite wait — convergence polling gets an explicit deadline that fails on
expiry. No unbounded retries; no sleeps tuned until green.

**Broker failure modes:**

- **Broker absent** — Spring AMQP retries connections by default and would hang. Connection
  attempts are capped so the module fails fast with a legible message.
- **TST-029 I2** requires restarting the broker process. Implemented as a real
  `docker compose restart broker`, gated to local/full runs, emitting `not-evaluated` with
  reason `"restart path exercised in full runs only"` in CI.
- **CI gains a container.** The broker's `rabbitmq-diagnostics ping` healthcheck must gate
  module start via `depends_on: condition: service_healthy`, or the first messaging module
  races the broker and fails for reasons unrelated to the SUT.

**Concurrency paths:**

- TST-020 I5 rests on a DB unique constraint on `idempotency_key`; the constraint violation is
  **caught and converted** into "serve stored response", never surfaced as a 500. I4's
  payload-hash mismatch returns 409.
- TST-023 I4 (double-release rejected) means release is state-checked and rejects unknown or
  already-released IDs. Silent success would make the invariant unfalsifiable. I6's TTL sweeper
  stops reservations outliving their window.

**Flakiness policy:** deterministic seeds via `SyntheticDataSeeder`'s existing seed parameter;
every deadline declared in the profile rather than hardcoded in the plan; and **no
retry-on-flake in the gate path** — a module needing retries to pass is not measuring what it
claims to.

## 9. Success Criteria

- `GET /_capabilities` truthfully reports **15** implemented archetypes
- **8/8** defect injections prove **specific**, not merely sensitive — each breaks its own
  invariant while the others stay passing
- The 7-check traceability gate is green across all 15 bound modules, not only the new ones
- 16 new JUnit tests pass (2 per module)
- `harness-coverage.md` regenerated and committed
- Family B is 5/5
- Every `partial` carries a `partial_reason`
- **CI stage green on a real GitLab runner with the broker running** — also the first real
  exercise of Wave 16's never-run `qe-harness` stage
- `validate-testing-coverage.py` and `render-testing-coverage.py --check` running in CI

**Run strategy:** CI runs smoke only — bounded, with most thresholds legitimately
`not-evaluated` plus reasons. `mixed.yml` holds 14,400s with a `smoke_mode_overrides` of 20s;
`soak.yml` runs 43,200s (86,400 for T0). Full-duration runs are manual or scheduled, never in
the MR path.

## 10. Risks

| Risk | Detail | Mitigation |
|---|---|---|
| ~~Gate check 6 anchors unverified~~ — **resolved** | Audited: two of four needs have anchors, two have none anywhere in the corpus | Declared as application config instead of NFR citations (§7.1). Governed NFR spine untouched; traceability recorded as a §11 follow-up |
| TST-026 needs a new harness dependency | It is the only one of the eight using the `contract-schema` oracle, `ContractSchema` **has never been called by anything**, and `com.networknt:json-schema-validator` is absent from `testPlanLibraries` — which runs with `downloadLibraryDependencies=false`, so transitives are not resolved | Its task carries the pom edit and must enumerate the dependency's transitives by hand; sequenced first in Slice B so the cost surfaces early |
| Broker in CI is untested ground | Wave 16's `qe-harness` stage has never run on a real runner; Phase 2 adds RabbitMQ to it | Healthcheck gating; Phase 2 sequenced after Phase 1 ships |
| TST-034's profile parsing is unprecedented | No code in the repo reads a profile file today; `blend_ref` is `null` | `ProfileResolver` built as a first-class shared component with its own tests, not inline plan logic |
| Digit-run rule (check 5) trips Slice B | Correlation IDs, sequence numbers and epoch-millis all tend to 13+ digits | Hyphenated short IDs and ISO-8601 mandated in §6; verified by the gate itself |
| Two archetypes land `partial` | TST-027 I5 (RabbitMQ has no partitions), TST-037 I5 (no CDC connector) | Declared up front with reasons; gate check 4 supports this explicitly |
| Scope creep from new SUT subsystems | Slice A proved less cheap than first assessed — TST-023 needs a new capability, TST-037 an outbox | Phasing; Phases 0–1 ship independently of Phase 2 |

## 11. Out-of-Scope Follow-Ups

Recorded so the next wave inherits them rather than rediscovering them:

1. The remaining **9 archetypes**: TST-022, TST-024, TST-025, TST-032, TST-033, TST-036,
   TST-038, TST-041, TST-042.
2. **TST-042 needs real SUT instrumentation** — `reference-sut/pom.xml` has no actuator,
   Micrometer or OTel dependency, and the `observability` compose profile (otel-collector,
   prometheus) is declared but never started.
3. **TST-043's real client-side invariants** — needs an application outside this repo.
4. `TST-002`/`NFR-004` numeric mismatch (0.01% vs 0.1% error-rate criteria) — needs a human
   decision, not a guess.
5. `TST-040` invariant I5 (refresh-token rotation/reuse) has zero coverage.
6. `check-compliance-rows.py` has a hardcoded `~/Documents/Arch-As-Code` default path.
7. `mkdocs.yml` points at `knowledge-base/technology-radar.md`, which does not exist
   (`domains/payments/technology-radar.md` does); 5 broken links remain under
   `domains/payments/dab/`.
8. `.markdownlint.json` is still absent while CI's `validate:markdown-lint` job references it.
9. `.trivyignore`'s license allowlist is repo-wide rather than path-scoped.
10. Making test evidence a formal DAB submission gate — needs EA Board / DAB chair approval.
11. **NFR traceability for two bounds.** If the board wants `app.readmodel.convergence-bound-ms`
    and `app.messaging.dlq-alert-depth` traceable to the NFR spine, that is a governed
    amendment: a `### Read-model convergence budgets (asynchronous projections)` section in
    NFR-002 (slug `read-model-convergence-budgets-asynchronous-projections`) and a
    `### Dead-letter queue depth alert thresholds` section in NFR-003 (slug
    `dead-letter-queue-depth-alert-thresholds`), then `_nfr-thresholds.yml` entries citing them.
    Keep headings ASCII — NFR-003's `Tết` heading is a live example of a slug that can never be
    cited, since check 6's anchor pattern is `[a-z0-9-]+` only.
12. **`TST-036`'s `primary_tool` conflict.** `MOB-006` carries both `TST-036` (`k6`) and
    `TST-043`, an implemented `k6` module, so no mechanical edit resolves it. The row must lose
    `TST-036` or be split — an owner's decision, deferred out of Wave 17.
13. **`.gitlab-ci.yml`'s header comment** lists five stages and omits `qe-harness` — already
    stale before this wave.
