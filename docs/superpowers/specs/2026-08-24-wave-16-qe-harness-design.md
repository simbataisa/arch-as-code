# Wave 16 — QE Harness Reference Implementation (Design)

**Status:** Draft — awaiting review
**Date:** 2026-08-24
**Owner:** @qe-lead
**Gating reviewers:** @devsecops-engineer (dependency surface), @tester-qe (oracle fidelity)
**Predecessor:** Wave 15 — Testing Knowledge Base (`2026-08-12-testing-knowledge-base-design.md`)

---

## 1. Goal

Build the runnable test harness that the Wave 15 testing corpus explicitly points at but
does not provide, together with a bundled synthetic reference service to run it against.

`knowledge-base/testing/README.md` states the gap in its own words:

> This is not a test harness. There is no Maven or Gradle project here, no `package.json`,
> and nothing runs in CI from this folder. Every code fragment — JMeter JMX snippets, Karate
> features, k6 scripts — is a worked example to copy into the QE team's own harness
> repository, not a runnable artefact maintained here.

Wave 16 produces that repository. The QE team currently has 24 archetypes of doctrine they
can read and nothing they can clone and run.

## 2. Scope

**In scope**

- One new top-level directory, `qe-harness/`, holding the harness and the reference SUT.
- A Java 21 + Spring Boot reference system-under-test with a capability registry
  enumerating all 24 archetypes; 7 implemented, 17 declared.
- 7 harness modules — one per archetype family (A–G) — each in the tool the Wave 15
  tool-selection matrix names as its best fit.
- A shared oracle and evidence library, plus a language-neutral evidence schema with
  emitters for the JVM, Python, and JavaScript halves of the harness.
- A traceability gate, `scripts/validate-harness-coverage.py`.
- A new `qe-harness` stage in `.gitlab-ci.yml` with four jobs, one of which is a blocking
  dependency-and-licence scan.
- Registration of the harness entry document as catalog row `TST-016`.

**Out of scope**

- The remaining 17 archetype modules. They are declared in the capability registry and
  land in Waves 17+ against a proven template.
- `TST-043`'s offline-sync invariants. They require a client application; there is none.
  See §5.3.
- Any change to the 24 archetype documents, the 9 strategy documents, the 5 tooling
  documents, or any pattern, NFR, or reference-architecture row. Wave 16 implements the
  corpus; it does not amend it.
- Extraction of `qe-harness/` into a separate repository. Considered and rejected in §4.1.

## 3. Decisions Taken During Brainstorming

Recorded here because each one closes off an alternative that a reader may otherwise
reopen.

| # | Decision | Rejected alternative and why |
|---|---|---|
| D1 | Harness lives in this repository, as a new top-level directory | A separate repository loses mechanical traceability — no gate could prove the harness stays in step with the 24 archetypes as they evolve. See §4.1. |
| D2 | A bundled containerised reference SUT | Config-driven targeting at a team's own environment means nothing runs out of the box and the harness's own correctness stays unverifiable. Stub-only means the contention archetypes can never be genuinely demonstrated. |
| D3 | 7 modules, one per family, selected for toolchain spread | Selecting for demonstration power alone yields JMeter ×5 + Locust ×1, leaving Gatling+Karate and k6 entirely unproven — and leaving `TST-012`'s headline claim unexecuted. See §5.1. |
| D4 | Java 21 + Spring Boot for the SUT | Aligns with 3 of 4 tools (JMeter, Gatling, Karate are JVM) and gives real JDBC transactions and row-level locking, which `TST-021` and `TST-023` need in order to fail for genuine reasons. |
| D5 | SUT designed for all 24, implemented for 7 | Building all 24 SUT behaviours is more than one wave. Declaring them makes Wave 17+ mechanical and makes a missing capability fail loudly rather than look like a pass. |
| D6 | Three-state results everywhere: `passed` / `failed` / `not-evaluated` | Two-state results force smoke-mode and unresolvable-threshold runs to masquerade as passes. |

## 4. Architecture

### 4.1 Placement and the traceability argument

`qe-harness/` sits alongside `knowledge-base/`, `governance/`, and `domains/`. The
documentation-only claim in `knowledge-base/testing/README.md` is rescoped to
`knowledge-base/` specifically — which is already true and remains true.

In-repo placement is justified by exactly one thing: `scripts/validate-harness-coverage.py`
can mechanically assert that the harness and the corpus agree. If that gate did not exist,
a git submodule would be the better answer. The gate is therefore not optional polish; it
is the reason for the placement.

### 4.2 Directory layout

```text
qe-harness/
├── README.md                 # TST-016 — governed entry document, clone-and-run
├── Makefile                  # single façade: make up · verify · run ARCH=TST-021 · run-all
├── docker-compose.yml        # SUT + infra, split by compose profile
├── reference-sut/            # Java 21 + Spring Boot (Maven)
│   ├── pom.xml
│   └── src/main/java/.../capability/    # one package per archetype capability
├── harness/
│   ├── pom.xml               # JVM reactor: common + jmeter + gatling-karate
│   ├── common/               # synthetic data factory, config, oracles, evidence emitter
│   ├── jmeter/               # TST-021, TST-031, TST-035, TST-040
│   ├── gatling-karate/       # TST-030
│   ├── k6/                   # npm — TST-043
│   └── locust/               # pip — TST-039
├── profiles/
│   ├── _nfr-thresholds.yml   # machine-readable projection of NFR targets, with citations
│   └── <profile>.yml         # the 8 TST-002 performance profiles as config
└── traceability/
    ├── evidence.schema.json  # language-neutral test_acceptance_criteria schema
    ├── harness-coverage.md   # generated
    └── runs/                 # generated run evidence
```

### 4.3 Build topology

Three dependency trees, behind one façade:

- **Maven reactor** — `reference-sut`, `harness/common`, `harness/jmeter`,
  `harness/gatling-karate`
- **npm** — `harness/k6`
- **pip** — `harness/locust`

The `Makefile` is the single entry point, so `clone && make up && make run-all` holds
regardless of language. The build systems are deliberately **not** unified: wrapping npm
and pip inside Maven produces brittle plumbing that hides real failures behind wrapper
plugin errors.

### 4.4 Compose profiles

CI pipeline time must track actual scope, so infrastructure is profile-gated:

| Profile | Contains | Needed by |
|---|---|---|
| `core` | Postgres, SUT, OAuth2 issuer | TST-021, TST-030, TST-031, TST-039, TST-040, TST-043 |
| `resilience` | Toxiproxy + a downstream stub | TST-035 |
| `observability` | OTel collector, Prometheus | Declared for `TST-042` in Waves 17+; not started in Wave 16 |
| `messaging` | Broker | Declared for `TST-027`–`TST-029` in Waves 17+; not started in Wave 16 |

Six of the seven modules need only `core`, so the common CI path is one Postgres, one SUT,
and one issuer. `resilience` is the sole addition, and only for `TST-035`.

## 5. Components

### 5.1 Module selection

One archetype per family, each in its declared best-fit tool:

| Family | Archetype | Tool | Oracle |
|---|---|---|---|
| A — Correctness & State | TST-021 Ledger & Monetary Invariant | JMeter | invariant-assertion |
| B — Messaging & Integration | TST-030 Contract & Schema Compatibility | Gatling + Karate | contract-schema |
| C — Load & Capacity | TST-031 Rate Limit, Throttle & Breakpoint | JMeter | invariant-assertion |
| D — Resilience | TST-035 Fault Injection & Graceful Degradation | JMeter | invariant-assertion |
| E — Data | TST-039 Data Quality & Reconciliation | Locust | confusion-matrix |
| F — Security | TST-040 AuthN/AuthZ Matrix & Token Lifecycle | JMeter | invariant-assertion |
| G — Observability & Client | TST-043 Client Experience & Perf Budget | k6 | invariant-assertion |

All four tools are exercised, and three of the four oracle types
(`invariant-assertion`, `contract-schema`, `confusion-matrix`) are implemented.
`golden-dataset` is not — no family-representative archetype uses it as primary oracle.
It lands with `TST-022` or `TST-038` in a later wave. Recorded here so the omission is
deliberate rather than discovered.

`TST-039` uses Locust rather than JMeter because the archetype document itself justifies
that choice (`TST-039` §6, independent recomputation per dimension). The harness follows
the corpus where the corpus deliberately departs from the default.

### 5.2 Reference SUT capabilities

`GET /_capabilities` returns the map of all 24 archetype IDs to
`implemented | declared`. Declared-but-unimplemented capabilities answer `501` with the
archetype ID in the body.

| Archetype | Capability | Why it exhibits real behaviour |
|---|---|---|
| TST-021 | Double-entry ledger: `POST /accounts`, `POST /transfers`, `GET /ledger/trial-balance` | One DB transaction per transfer against Postgres, so a concurrent transfer storm can genuinely break the trial balance if the code is wrong |
| TST-030 | `/v1` and `/v2` transfer APIs, OpenAPI schema at `/openapi.json`, one deliberate breaking-change fixture (v2 removes a field) | The test catches a real removed field |
| TST-031 | Token-bucket limiter returning `429` + `Retry-After` | Real bucket state under real concurrency; the breakpoint is discovered, not configured |
| TST-035 | Downstream-dependent endpoint behind a Resilience4j breaker with a declared degraded response | Toxiproxy injects the fault at the network layer, so the breaker reacts to genuine latency |
| TST-039 | Source table, derived reporting view, reconciliation endpoint, seeded with deliberate defects across completeness / accuracy / timeliness | Scoring a confusion matrix requires real divergence to score |
| TST-040 | Spring Security resource server; role × endpoint matrix; token issue / refresh / revoke / expiry; explicit authorisation-decision marker header | Real token validation and a real expiry clock, which the clock-skew measurement depends on |
| TST-043 | Cache headers, ETag / conditional requests, compression, payload-size budget | Server-side contributions to client experience are measurable without a frontend |

`TST-040`'s decision marker is not incidental. The Wave 15 archetype classifies a bare
`403` carrying no decision marker as `error`, not `deny`. The SUT must therefore emit the
marker, or the harness cannot distinguish a correct denial from an unhandled failure.

### 5.3 TST-043 scoped limitation

`TST-043` covers client experience, offline sync, and perf budget. Offline-sync
invariants require a client application, and Wave 16 builds none. This module implements
perf budget, cache correctness, conditional requests, and compression only.

The traceability output records `TST-043` as **partial**, never `passed`. A green tick
that overstates coverage is worse than an explicit partial.

### 5.4 Shared library and the language split

`harness/common/` (JVM) carries:

- **Synthetic data factory** — deterministic seeds, reproducible runs
- **Config resolution** — profile → target URL, credentials, thresholds
- **Oracle assertion library** — `InvariantAssertion`, `GoldenDataset`, `ConfusionMatrix`,
  `ContractSchema`
- **Evidence emitter** — writes the `test_acceptance_criteria` block

The evidence emitter is what makes the harness more than a collection of scripts. Every
run emits the exact `test_acceptance_criteria` contract that `TST-001` defines and that
DAB submissions require, closing the loop from pattern to archetype to harness run to
governance evidence.

`common/` is JVM, so Locust and k6 cannot use it. The evidence block therefore has a
language-neutral JSON schema (`traceability/evidence.schema.json`) plus three thin
emitters, roughly 50 lines each. Drift between them is caught by a gate that validates
all three outputs against the one schema — not by code review, which will not catch it
reliably.

### 5.5 Synthetic data constraints

Enforced by gate, not by convention:

- Account identifiers of the form `ACC-000001`
- Amounts in minor units
- Party names drawn from a fixed synthetic list
- **No 13–19 digit numeric strings anywhere in the tree**, so nothing can be mistaken
  for a PAN
- No PII or PHI of any kind, in seed data, fixtures, or test names

Wave 15 carried the PAN constraint as a manual check. Here it becomes a gate, because
seed data is precisely where a well-meaning contributor pastes something real.

## 6. Run Flow

```text
make up            → compose starts only the profiles the selected modules need
                   → deterministic seed loads synthetic data into Postgres
make run ARCH=…    → resolve module, resolve profile config and thresholds
                   → execute tool, oracle assertions fire
                   → evidence emitter writes traceability/runs/<ts>-<TST-id>.json
make verify        → traceability gate, evidence-schema validation, synthetic-data gate
                   → render traceability/harness-coverage.md
```

## 7. Threshold Resolution and Its Limit

Wave 15 forbade hardcoded thresholds; archetypes cite `NFR-*` rows. The harness needs
actual numbers, so `profiles/_nfr-thresholds.yml` holds a machine-readable projection,
each entry carrying a `threshold_ref` such as `NFR-003#p99-latency`.

**What the gate proves:** the citation resolves — the `NFR-*` row and its anchor exist —
reusing `scripts/validate-internal-links.py`'s anchor machinery.

**What the gate does not prove:** that the number matches the NFR document's prose. Doing
so requires parsing Markdown for numeric values, which is brittle enough to produce false
confidence. A human owns number accuracy; the gate owns the citation. This limit is
stated in `qe-harness/README.md` so no reader infers more than the gate checks.

## 8. CI Design

New `qe-harness` stage in `.gitlab-ci.yml`:

| Job | Work | Gating |
|---|---|---|
| `harness:build` | Maven reactor, `npm ci`, pip install; dependencies cached | blocking |
| `harness:scan` | CVE scan across all three dependency trees, plus licence check | **blocking** |
| `harness:verify` | traceability gate, evidence-schema validation, synthetic-data gate; no compose needed | blocking |
| `harness:run` | compose up, execute the 7 modules against clean and defective SUT, publish reports | blocking on MR, plus nightly |

**Rules.** These jobs run only on merge requests touching `qe-harness/**`, plus a nightly
schedule. A Markdown-only merge request — which is what fifteen waves of this repository
have consisted of — pays nothing. Slowing the documentation pipeline to accommodate the
harness would be a poor trade.

**`harness:scan` is blocking by design.** Wave 16 introduces a third-party dependency tree
into an architecture repository for the first time. Maven, npm, and pip lockfiles bring
CVE exposure and licence obligations that `knowledge-base/` has never carried. That new
risk surface gets a gate, not a report. `@devsecops-engineer` signs off before merge, in
the same way `@infosec-architect` gated `TST-040` and `TST-041` in Wave 15.

### 8.1 Smoke mode — the deliberate refusal

`TST-031` and `TST-035` run in **smoke mode** in CI. A shared GitLab runner cannot produce
meaningful latency or throughput figures, so in CI these modules assert only their
correctness invariants — `429` carries `Retry-After`, the breaker opens, the degraded
response has the declared shape — and record every performance threshold as
`not-evaluated`.

Full-load runs happen on a dedicated environment via the manual or nightly job.

This is the most common way a performance harness misleads: a green CI badge implying
performance was validated on a noisy shared container. The evidence block says
`not-evaluated`, in writing, on every such run.

## 9. Failure Handling

Every result is `passed`, `failed`, or `not-evaluated`. The third state is load-bearing
and mirrors the `TST-040` design decision that a bare `403` without a decision marker is
`error` rather than `deny`.

| Condition | Result | Behaviour |
|---|---|---|
| SUT not reachable | `failed`, once | Health-check gate fails fast with one clear message, rather than cascading connection-refused assertion failures |
| Capability returns `501` | `not-implemented` | Distinct from `failed`; expected for the 17 declared archetypes |
| Threshold unresolvable | `not-evaluated` | Never silently `passed` |
| Smoke mode | `not-evaluated` for perf assertions | Correctness assertions still evaluated normally |
| Infra setup flake | retry | Bounded retry **on setup only** |
| Assertion failure | `failed` | **Never retried.** Retrying an assertion launders flakiness into green |

## 10. Self-Verification — Defect Injection

Each of the 7 modules ships with a deliberate SUT defect behind a flag, and CI asserts the
module **fails** against its defect and **passes** against the clean SUT. Seven pairs,
fourteen runs.

| Module | Defect flag | Defect |
|---|---|---|
| TST-021 | `SUT_DEFECT=ledger-unbalanced` | Credit leg omitted under concurrency |
| TST-030 | `SUT_DEFECT=schema-drift` | Response field silently renamed |
| TST-031 | `SUT_DEFECT=ratelimit-leaky` | Bucket admits above configured rate |
| TST-035 | `SUT_DEFECT=breaker-disabled` | Downstream failure surfaces as `500` instead of the degraded response |
| TST-039 | `SUT_DEFECT=recon-false-clean` | Reconciliation reports clean despite seeded defects |
| TST-040 | `SUT_DEFECT=authz-missing-marker` | Denials return bare `403` with no decision marker |
| TST-043 | `SUT_DEFECT=cache-headers-absent` | `Cache-Control` and `ETag` omitted |

A test that has never been observed to fail demonstrates only that it compiles. For a
harness whose purpose is to be copied into other teams' repositories, shipping tests of
unknown sensitivity would be worse than shipping nothing.

## 11. Catalog Registration

One new row, `TST-016` — the next free ID, since `TST-015` is the Coverage Matrix and
`TST-016`–`TST-019` is reserved headroom.

| Field | Value |
|---|---|
| `id` | TST-016 |
| `title` | QE Harness Reference Implementation |
| `category` | testing |
| `status` | Approved on merge |
| `owner` | @qe-lead |
| `path` | `qe-harness/README.md` |
| `spine_or_radii` | radii |
| `target_wave` | 16 |

This is the first catalog row whose `path` sits outside `knowledge-base/`. The catalog
audit script must accept that; verify before relying on it.

Coverage: `TST-016` is a `governs` row in `_testing-coverage.yml` — it constrains how
other rows are tested rather than being under test itself.

## 12. Success Criteria

1. `git clone && make up && make verify && make run-all` succeeds from cold on a clean
   machine.
2. All 7 modules pass against the clean SUT and fail against their injected defect.
3. `GET /_capabilities` enumerates all 24 archetypes: 7 `implemented`, 17 `declared`.
4. Traceability gate green — every module maps to a real archetype, each module's tool
   matches the archetype's declared best fit, no hardcoded thresholds, no PAN-shaped
   strings.
5. Evidence output from all three languages validates against
   `traceability/evidence.schema.json`.
6. `TST-016` registered; `TST-043` recorded as **partial**, not `passed`.
7. `harness:scan` green, with `@devsecops-engineer` sign-off on the dependency surface.
8. Markdown-only merge requests show no added pipeline time.
9. All existing Wave 15 gates still green: catalog audit, coverage validation, coverage
   render `--check`, internal links.

## 13. Global Constraints

- **Synthetic data only.** No PII, no PHI, no PAN-shaped strings (13–19 digit numerics),
  in seed data, fixtures, test names, or documentation.
- **No hardcoded performance thresholds.** Every numeric target carries a `threshold_ref`
  citing an existing `NFR-*` row and anchor.
- **No modification** of the 24 archetype documents, 9 strategy documents, 5 tooling
  documents, or any pattern, NFR, or reference-architecture row.
- **Assertions are never retried.** Bounded retry applies to infrastructure setup only.
- **Three-state results.** `passed` / `failed` / `not-evaluated`; two-state reporting is a
  defect.
- **Each module's tool must match** the archetype's declared best fit in `TST-010`.
- **Documentation-only claim** in `knowledge-base/testing/README.md` rescoped to
  `knowledge-base/`, not deleted.
- **Markdown-only merge requests must not incur harness CI time.**

## 14. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Dependency tree introduces CVEs into an architecture repo | New, previously absent risk class | `harness:scan` blocking; `@devsecops-engineer` gate before merge |
| Three toolchains in one wave | Integration cost discovered late | This is the reason for toolchain-spread selection — discovering it now is the point |
| Reference SUT over-built relative to Wave 16's needs | Wasted effort | Declared-not-implemented (D5) caps SUT work at the 7 exercised capabilities plus a registry |
| Smoke mode misread as performance validation | False confidence — the highest-severity risk here | `not-evaluated` in every evidence block; stated in `README.md`; full-load path documented |
| `TST-043` partial coverage read as complete | Overstated coverage | Traceability output records `partial`; success criterion 6 makes it explicit |
| Catalog audit rejects a path outside `knowledge-base/` | Registration blocked | Verify `audit-catalog-consistency.py` behaviour before Task 1, not after |
| Harness drifts from the corpus as archetypes evolve | The whole in-repo rationale collapses | The traceability gate is the mitigation, and is therefore not optional |

## 15. Out-of-Scope Follow-Ups

Recorded so they are not lost, and explicitly not part of Wave 16:

- Remaining 17 archetype modules (Waves 17+).
- `golden-dataset` oracle implementation, arriving with `TST-022` or `TST-038`.
- `TST-043` offline-sync invariants, which need a client application.
- Pre-existing repository debt, untouched here: absent `.markdownlint.json` referenced by
  CI; `check-compliance-rows.py` hardcoded `~/Documents/Arch-As-Code` default;
  `technology-radar.md` canonical-path mismatch in `mkdocs.yml`; 5 broken links under
  `domains/payments/dab/`.
