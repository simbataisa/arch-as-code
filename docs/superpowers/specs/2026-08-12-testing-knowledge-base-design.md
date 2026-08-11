# Testing Knowledge Base — Design Specification

Status: Draft | Last Reviewed: 2026-08-12 | Owner: @qe-lead
Scope: new `knowledge-base/testing/` catalog category for the IT Quality Engineering team
Related: NFR-001, NFR-002, NFR-003, NFR-004, TPL-001, TPL-002, INT-015, EIP-020, OBS-009, BP-005

---

## 1. Problem Statement

The knowledge base holds 191 Approved catalog rows — 130 design patterns, 20 reference
architectures, 5 NFR spine docs, 13 principles, 11 best practices — but carries no guidance
on how to *verify* any of them. The consequences today:

- Every squad invents its own test approach per pattern. Two teams implementing
  `RES-002 Circuit Breaker` test it differently, and neither result is comparable.
- Performance targets exist (`NFR-002` defines per-tier P50/P95/P99; `TPL-001` requires a
  declared `sustained_rps` / `peak_rps`) but nothing defines the *test profile* that proves
  those numbers were met. A DAB can declare 5000 RPS at P95 200 ms with no evidence standard.
- Soak, spike, and mixed-workload testing are named in conversation but nowhere defined, so
  duration, load shape, and pass criteria vary per engineer.
- Declared `failure_modes` in the `nfr_acceptance_criteria` block have no corresponding
  resilience test obligation, so a service can declare FM1/FM2 and never exercise them.
- Performance tooling is fragmented across JMeter, Gatling/Karate, Locust, and k6 with no
  selection rule, so results are not reproducible across squads — most often because
  JMeter's closed workload model and Gatling/k6's open model are silently mixed.
- QE has no way to answer "which patterns are untested?" because there is no coverage record.

## 2. Goals and Non-Goals

### Goals

1. Give the IT QE team a normative, catalog-governed testing corpus covering all 191 existing
   catalog rows across six disciplines: functional, performance, resilience, contract,
   security, and data quality.
2. Define the performance test family once, normatively — baseline, load, stress/breakpoint,
   spike, soak, mixed/blended, scalability step-ramp, failover-under-load — with thresholds
   *derived from* the NFR spine rather than restated.
3. Make coverage provable and machine-checkable, not asserted in prose.
4. Standardise tool selection across JMeter (primary), Gatling + Karate, k6, and Locust.
5. Create a traceability hook between declared NFRs and executed tests.

### Non-Goals

- No runnable test harness. No Maven/Gradle project, no `package.json`, no dependency
  management, no CI test execution. This repository stays documentation-only.
- No change to the DAB process itself. Making test evidence a DAB gate is a separate
  governance decision requiring EA Board and DAB chair approval; this spec supplies the
  artifacts such a gate would cite, and stops there.
- No modification to existing pattern documents. Cross-links flow one way: testing docs link
  to patterns, patterns are left untouched.
- No production data. See §7.

## 3. Approach Decisions

Five decisions, taken during brainstorming, that shape everything below.

| # | Decision | Rejected alternative | Rationale |
|---|---|---|---|
| D1 | Archetype library plus coverage matrix | One testing doc per pattern (~130 docs) | ~70% of test strategy text is shared across patterns of the same shape. Archetypes capture the shape once; the matrix proves coverage. |
| D2 | Documentation with inline code snippets | Runnable harness, or committed template files | Matches the existing architecture-as-code convention and keeps CI unchanged. QE copies snippets into their own harness repository. |
| D3 | Full catalog integration under a new `TST-` prefix | Standalone folder with no catalog rows | Uncatalogued docs are invisible to catalog audits, DAB citation rules, and coverage reporting. |
| D4 | All six disciplines in scope | Functional plus performance only | Requested explicitly. Sequenced into waves so security lands last, under InfoSec ownership. |
| D5 | JMeter canonical recipe per archetype, plus a tool-fit table and four deep tooling guides | All four tools in every archetype (~96 recipes) | Four implementations of one scenario is a 4x maintenance surface for marginal value. JMeter is the primary tool; the other three get full guides. |

## 4. Structure

```
knowledge-base/testing/
  README.md                                      (no Catalog ID — index page)
  strategy/
    test-strategy-standard.md                    TST-001   ** SPINE **
    performance-test-standard.md                 TST-002
    workload-modelling.md                        TST-003
    test-data-management.md                      TST-004
    environments-quality-gates.md                TST-005
    resilience-test-standard.md                  TST-006
    contract-integration-test-standard.md        TST-007
    security-test-standard.md                    TST-008
    data-quality-test-standard.md                TST-009
  tooling/
    tool-selection-matrix.md                     TST-010
    jmeter.md                                    TST-011
    gatling-karate.md                            TST-012
    k6.md                                        TST-013
    locust.md                                    TST-014
  coverage/
    _testing-coverage.yml                        (machine-readable, 191 rows)
    coverage-matrix.md                           TST-015   (generated)
  archetypes/
    ...24 documents...                           TST-020 … TST-043

knowledge-base/templates/
  test-archetype-template.md                     TPL-005   (new)

scripts/
  validate-testing-coverage.py                   (new)
  render-testing-coverage.py                     (new)
```

### Conventions inherited from the repository

- Filenames carry no ID prefix. The Catalog ID lives in the document header, matching
  `knowledge-base/patterns/resilience/circuit-breaker.md` (`Catalog ID: RES-002`).
- Category `README.md` files carry no Catalog ID and open with an index table, matching
  `knowledge-base/principles/README.md`.
- Every Approved document carries the five-line header: `Status`, `Last Reviewed`, `Owner`,
  `Catalog ID` plus `Spine`/`Radii` class, and `Tier Applicability`.
- Every document contains at least one Mermaid diagram (`governance/standards/diagram-standards.md`).
- Every document completes the Ring 0/1/2 Compliance Mapping table, using the existing
  `⚠️ (working summary — pending Legal review)` marker for Ring 2 items.

### Catalog impact

| Measure | Before | After |
|---|---|---|
| Approved catalog rows | 191 | 231 |
| Categories | 16 | 17 (`testing` added) |
| Spine docs | 6 | 7 (`TST-001` added) |
| `templates` category rows | 4 | 5 (`TPL-005` added) |
| Files created | — | 44 |
| Existing files edited | — | 4 |

Registration touches exactly four existing files:

1. `governance/standards/_catalog-inventory.yml` — append 40 rows (39 `TST-*`, 1 `TPL-005`).
2. `governance/standards/enterprise-architecture-catalog.md` — append 40 table rows in ID
   order; update the line 5 coverage sentence (191→231, 16→17, 6→7 spine, 185→224 radii);
   add a `testing` row and update `templates` in the §5 category summary table and its
   `**Total**` row; add a `testing` subsection to the §3 taxonomy; add `TST-001` to the §2.2
   spine list.
3. `mkdocs.yml` — add a `Testing (QE)` section under `Architecture Knowledge Base`.
4. `.gitlab/CODEOWNERS` — `knowledge-base/testing/` → `@qe-lead`, with `@sre-lead` as
   co-owner on `strategy/performance-test-standard.md`, `strategy/workload-modelling.md`,
   and `tooling/`; `@infosec-architect` co-owner on `strategy/security-test-standard.md`
   and the two Family F archetypes.

## 5. The 24 Archetypes

Grouped into seven families by shared test method — two patterns share an archetype when the
*method of verification* is the same, not merely the domain.

### Family A — Correctness & State (TST-020 … TST-025)

| ID | Archetype | File | Covers |
|---|---|---|---|
| TST-020 | Idempotency & Replay Safety | `idempotency-replay.md` | BSP-002 Idempotent Payment Key, EIP-024 Idempotent Receiver, PRIN-006 Idempotency-by-default, INT-014 Webhook Delivery Reliability, RES-003 Retry with Backoff |
| TST-021 | Ledger & Monetary Invariant | `ledger-monetary-invariant.md` | BSP-001 Double-Entry Ledger, BSP-015 Position Keeping Engine, BSP-016 Settlement Engine, BSP-005 Reversal and Chargeback, REF-010 Ledger Posting Engine |
| TST-022 | Deterministic Calculation Engine | `deterministic-calculation-engine.md` | BSP-018 Accrual Engine, BSP-007 Interest Calculation, BSP-008 Fee Engine, BSP-009 Tax Calculation, BSP-006 Pricing Engine, BSP-020 Relationship Pricing, BSP-014 FX Rate Engine, BSP-017 Product Factory |
| TST-023 | Concurrent Limit & Counter Contention | `concurrent-limit-contention.md` | BSP-011 Credit Limit Engine, BSP-012 Transaction Limit Engine, BSP-013 Collateral Management Engine |
| TST-024 | Saga & Compensation Correctness | `saga-compensation.md` | INT-001 Saga Orchestration, INT-016 Distributed Saga Choreography, EIP-017 Process Manager, EIP-016 Routing Slip |
| TST-025 | Decision Table & Screening Accuracy | `decision-screening-accuracy.md` | BSP-010 Rule / Decisioning Engine, BSP-003 Sanction Screening Pipeline, BSP-019 Collections Engine, SEC-009 Fraud Signal Collection, SEC-010 ABAC policy decisions |

`TST-025` is separated from `TST-022` deliberately: a calculation engine is verified against
an exact expected value, whereas a screening or decisioning engine is verified against a
confusion matrix — precision, recall, and false-positive rate against a labelled corpus. The
oracles are different, so the test design is different.

### Family B — Messaging & Integration (TST-026 … TST-030)

| ID | Archetype | File | Covers |
|---|---|---|---|
| TST-026 | Message Transformation & Routing Correctness | `message-transformation-routing.md` | EIP-004 Message Router, EIP-005 Content-Based Router, EIP-006 Message Translator, EIP-007 Content Enricher, EIP-008 Content Filter, EIP-010 Normalizer, EIP-014 Composed Message Processor, EIP-012 Splitter, EIP-019 Smart Proxy, INT-009 Content-Based Router, INT-005 Anti-Corruption Layer, INT-012 Error Code Mapping Standard |
| TST-027 | Ordering, Sequencing & Resequencing | `ordering-resequencing.md` | EIP-013 Resequencer, INT-017 Message Sequencer, EIP-003 Publish-Subscribe Channel (ordering guarantees) |
| TST-028 | Fan-out / Fan-in Correlation | `fanout-fanin-correlation.md` | EIP-015 Scatter-Gather, EIP-011 Aggregator, EIP-009 Claim Check, EIP-018 Message Store |
| TST-029 | Delivery Guarantee, Retry & DLQ | `delivery-guarantee-dlq.md` | EIP-023 Guaranteed Delivery, EIP-022 Durable Subscriber, EIP-025 Dead Letter Channel, EIP-021 Channel Purger, EIP-001 Message Channel, EIP-002 Point-to-Point Channel, EIP-020 Test Message, INT-014 Webhook Delivery Reliability |
| TST-030 | Contract & Schema Compatibility | `contract-schema-compatibility.md` | INT-015 API Contract Testing, INT-010 AsyncAPI Specification Standard, INT-011 CloudEvents Envelope Standard, INT-013 Schema Registry Governance, INT-003 API Gateway Routing contracts |

### Family C — Load & Capacity (TST-031 … TST-034)

| ID | Archetype | File | Covers |
|---|---|---|---|
| TST-031 | Rate Limit, Throttle & Breakpoint | `rate-limit-breakpoint.md` | RES-008 Throttling / Rate Limiting, RES-009 Load Shedding, RES-011 Queue-Based Load Levelling |
| TST-032 | Batch Window & Cutoff Throughput | `batch-window-cutoff.md` | BSP-004 End-of-Day Batch Window, BSP-019 Collections Engine, REF-008 Regulatory Reporting, DATA-004 Data Vault 2.0 loads |
| TST-033 | Multi-Tenant Isolation & Noisy Neighbour | `multitenant-noisy-neighbour.md` | PLT-008 Multi-Tenancy Isolation, RES-001 Bulkhead Isolation, RES-005 Cell-Based Architecture, PLT-006 FinOps cost-per-transaction |
| TST-034 | Blended Journey Workload | `blended-journey-workload.md` | All 20 `REF-*` reference architectures. Owns the mixed and journey-level soak profiles. |

### Family D — Resilience (TST-035 … TST-036)

| ID | Archetype | File | Covers |
|---|---|---|---|
| TST-035 | Fault Injection & Graceful Degradation | `fault-injection-degradation.md` | RES-002 Circuit Breaker, RES-007 Fallback Strategies, RES-004 Graceful Degradation, RES-006 Timeout Budget, RES-012 Health Check Aggregation, RES-010 Leader Election, RES-001 Bulkhead Isolation, RES-003 Retry with Backoff (retry-amplification), BP-005 Chaos Engineering |
| TST-036 | Zero-Downtime Deploy, Traffic Shift & Rotation | `zero-downtime-deploy-rotation.md` | PLT-003 GitOps Deployment Pipeline, PLT-001 Service Mesh Traffic Management, PLT-005 Kubernetes Operator Pattern, INT-006 Strangler Fig, SEC-007 Secrets Rotation, SEC-003 Vault Secret Management, FE-004 Web Feature Flags, MOB-006 Mobile Force-Upgrade |

### Family E — Data (TST-037 … TST-039)

| ID | Archetype | File | Covers |
|---|---|---|---|
| TST-037 | Read-Model Convergence & CDC Lag | `read-model-convergence-lag.md` | DATA-001 CQRS Pattern, DATA-008 Change Data Capture, DATA-007 Kappa Architecture, DATA-006 Lambda Architecture, DATA-012 Data Virtualization, INT-002 Transactional Outbox + CDC, INT-004 Event Sourcing |
| TST-038 | Temporal & Historisation Correctness | `temporal-historisation.md` | DATA-005 Slowly Changing Dimensions, DATA-003 Temporal Tables, DATA-004 Data Vault 2.0, DATA-010 Time-Series Modelling |
| TST-039 | Data Quality & Reconciliation | `data-quality-reconciliation.md` | DATA-011 Data Quality Rules, DATA-013 Reference Data Master, DATA-009 Data Lineage, DATA-002 Data Mesh Ownership |

### Family F — Security (TST-040 … TST-041)

| ID | Archetype | File | Covers |
|---|---|---|---|
| TST-040 | AuthN/AuthZ Matrix & Token Lifecycle | `authn-authz-token-lifecycle.md` | SEC-010 Attribute-Based Access Control, SEC-006 JWT Best Practices, SEC-002 OAuth2 Authorization, SEC-005 BFF + Token-Binding, SEC-011 Session Revocation, SEC-001 mTLS Service Mesh, MOB-003 Mobile Biometric Auth |
| TST-041 | Data Protection, Masking & Tokenisation | `data-protection-masking-tokenisation.md` | SEC-008 Data Masking, SEC-013 PII Tokenization (Format-Preserving), SEC-004 Tokenization + HSM Key Management, SEC-012 Tamper-Evident Audit Logging, MOB-002 Mobile Secure Storage, FE-003 Web CSP Hardening, MOB-005 Mobile Deep Link Attestation, MOB-004 Mobile Push Notification (Secure) |

### Family G — Observability & Client (TST-042 … TST-043)

| ID | Archetype | File | Covers |
|---|---|---|---|
| TST-042 | Telemetry & Observability Verification | `telemetry-verification.md` | OBS-001 … OBS-010 (trace continuity, cardinality under load, log pipeline throughput, alert-fire drills, sampling fidelity, burn-rate correctness) |
| TST-043 | Client Experience, Offline Sync & Perf Budget | `client-experience-offline-perf.md` | FE-005 Web Error Boundary, FE-006 Web i18n / RTL, FE-001 Web Performance Budgets, FE-002 Web Resilience / Offline-First, MOB-001 Mobile Offline Queue, MOB-006 Mobile Force-Upgrade |

Meta rows — the 5 `NFR-*`, 13 `PRIN-*`, 5 `TPL-*`, 8 `COMP-*`, and the non-testable
process/platform rows (BP-009 Runbook Authoring, BP-010 Incident Postmortem, BP-011 Blameless
Culture, PLT-002 CNCF Stack Selection, PLT-004 Internal Developer Platform, PLT-007 Platform
Service Catalog) — take `disciplines: {…: governs}` rather than an archetype assignment, so
"not applicable" is never confused with "not yet covered."

## 6. Coverage Matrix Mechanics

`knowledge-base/testing/coverage/_testing-coverage.yml` holds one row per existing catalog
row. Schema mirrors `_catalog-inventory.yml` (flat `version` / `last_updated` / `rows`):

```yaml
version: 1
last_updated: '2026-08-12'
rows:
  - catalog_id: RES-002
    title: Circuit Breaker Pattern
    path: knowledge-base/patterns/resilience/circuit-breaker.md
    tiers: [T0, T1, T2]
    archetypes: [TST-035, TST-031]
    disciplines:
      functional: required
      performance: required
      resilience: required
      contract: n/a
      security: n/a
      data_quality: n/a
    perf_profiles: [baseline, load, spike, failover-under-load]
    primary_tool: jmeter
    owner: sre-lead
    notes: ''
```

Field domains — `disciplines.*` ∈ `{required, recommended, n/a, governs}`;
`perf_profiles[]` ∈ the eight profile names defined in TST-002; `primary_tool` ∈
`{jmeter, gatling-karate, k6, locust}`; `archetypes[]` must be non-empty unless every
discipline is `governs`.

### `scripts/validate-testing-coverage.py`

Modelled on the existing `scripts/audit-catalog-consistency.py`. Exits non-zero when:

1. A row in `_catalog-inventory.yml` has no matching `catalog_id` in `_testing-coverage.yml`.
2. A coverage row references a `catalog_id` absent from the inventory.
3. A referenced `TST-*` archetype ID does not exist as a file under `archetypes/`.
4. Any `disciplines`, `perf_profiles`, or `primary_tool` value falls outside its domain.
5. `path` does not exist on disk.
6. `archetypes` is empty while any discipline is `required` or `recommended`.
7. `tiers` disagrees with the inventory row's `tiers`.

### `scripts/render-testing-coverage.py`

Modelled on `scripts/render-catalog-table.py`. Regenerates the table body of
`coverage-matrix.md` from the YAML between `<!-- BEGIN GENERATED -->` and
`<!-- END GENERATED -->` markers, so hand-written narrative above the table survives.

Both scripts are pure-stdlib plus `PyYAML`, already in `scripts/requirements.txt`. They are
consistent with the 16 Python and shell scripts already in `scripts/` and introduce no build
system — the D2 constraint concerns test-harness tooling, not repository automation.

## 7. Test Data — Mandatory Constraints

`TST-004 Test Data Management` is normative and binding on every other document in the folder:

- **Synthetic or anonymised data only.** Production data — including masked production
  extracts — is prohibited in any test environment covered by this corpus.
- **No PII or PHI in committed fixtures.** No real names, dates of birth, national ID
  numbers, member IDs, or account holder details appear in any snippet, example, or fixture
  in this repository.
- Card PANs use designated test BIN ranges only; account numbers, CIF identifiers, and
  customer references are synthetic and clearly marked as such.
- Referential integrity across synthetic customer / account / ledger / transaction sets is a
  generation requirement, so ledger-invariant and reconciliation tests remain valid.
- Generation is seeded and deterministic, so a failing performance run is reproducible.
- Data volume scales with tier: the perf environment dataset must match production
  cardinality within the ratio declared in TST-005, because index selectivity and cache hit
  rate — not row count — drive latency.
- Teardown and reset procedure is defined per archetype; no test leaves residue that changes
  a subsequent run's baseline.

This section reflects a hard organisational data-handling requirement, not a preference.

## 8. Performance Test Standard (TST-002)

Eight normative profiles, parameterised by service tier. Thresholds are **derived from** the
NFR spine by reference: `NFR-001` (tier, RTO, RPO, availability), `NFR-002` (P50/P95/P99 per
tier), `NFR-004` (sustained and peak throughput), `NFR-003` (capacity headroom), and `NFR-005`
(error budget, which bounds the acceptable error burst during `failover-under-load`). No
archetype restates a latency or throughput number — each links to the tier row, so a change to
the spine propagates instead of drifting.

| Profile | Purpose | Load shape | Pass criteria | Required for |
|---|---|---|---|---|
| `baseline` | per-build sanity | 10% of sustained, 10 min | zero errors; P95 within tier budget | T0–T3 |
| `load` | steady-state proof | 100% of sustained, 60 min | P50/P95/P99 all within the NFR-002 tier row; error rate ≤ 0.1% | T0–T2 |
| `stress` | locate the knee | step +10% every 5 min until failure | knee ≥ declared `peak_rps`; degradation is graceful, not cliff-edge | T0, T1 |
| `spike` | burst absorption | sustained → peak in 30 s, hold 5 min, release | recovery to baseline P95 ≤ 60 s; zero message loss; no DLQ growth | T0, T1 |
| `soak` | leak and drift | 70% of sustained; 12 h (T0: 24 h) | RSS growth ≤ 5%; P95 drift ≤ 10% first hour vs last hour; connection-pool and thread counts flat; DLQ depth flat; no unbounded cache growth | T0, T1 |
| `mixed` | realistic contention | named journey blend from TST-003, 4 h | every journey's own P95 within its own tier budget — not just the aggregate | T0, T1 |
| `scalability` | linearity and autoscaling | 25 / 50 / 75 / 100 / 125% step-ramp, 15 min per step | throughput linear within ±15%; HPA settles < 3 min; no thrash | T0–T2 |
| `failover-under-load` | HA proof under traffic | 100% sustained with an injected fault from the service's declared `failure_modes` | RTO and RPO within the NFR-001 tier row; error burst consumes ≤ the agreed share of the error budget | T0, T1 |

`mixed` is called out because an aggregate P95 can pass while a low-volume, high-value
journey inside the blend fails its own budget. The pass criterion is per-journey.

### The `test_acceptance_criteria` block

`TST-001` defines a YAML block that mirrors `TPL-001`'s `nfr_acceptance_criteria` one-to-one:

```yaml
test_acceptance_criteria:
  service_name: payment-auth-service
  tier: T0                                   # must equal nfr_acceptance_criteria.tier
  catalog_refs: [BSP-002, RES-002, INT-001]
  archetypes: [TST-020, TST-031, TST-035]
  slo_source: [NFR-001, NFR-002, NFR-004]    # thresholds derived, never restated

  functional:
    invariants_covered: 12
    negative_paths_covered: 8
    oracle: golden-dataset                   # golden-dataset | invariant-assertion |
                                             # confusion-matrix | contract-schema
  performance:
    profiles_executed: [baseline, load, stress, spike, soak, mixed, failover-under-load]
    sustained_rps: 5000                      # must equal nfr_acceptance_criteria value
    peak_rps: 15000                          # must equal nfr_acceptance_criteria value
    workload_model: closed                   # closed | open — see TST-003
    blend_ref: journey-blend-payments-peak
  resilience:
    fault_scenarios: [FM1, FM2]              # must reference declared failure_modes IDs
  contract: {consumer_contracts_verified: 4, schema_compat_mode: BACKWARD}
  security: {authz_matrix_cells_covered: 36, token_lifecycle_cases: 7}
  data_quality: {dq_rules_asserted: 18, reconciliation_tolerance: '0.00'}

  evidence:
    report_path: <perf-report-artifact>
    executed_on: '2026-08-12'
    environment: perf-prod-like
    signed_off_by: '@qe-lead'
```

Three cross-block invariants make gaps machine-detectable:

1. `tier` must equal the `nfr_acceptance_criteria.tier` for the same service.
2. `resilience.fault_scenarios` must reference `FM*` IDs actually declared in that service's
   `failure_modes`. A declared failure mode with no test is a detectable gap.
3. `performance.sustained_rps` / `peak_rps` must equal the declared NFR values, so a service
   cannot quietly load-test below what it promised.

These invariants are documented in TST-001. Enforcing them in CI over `dab/` content is a
DAB-process change and is explicitly out of scope (§2).

## 9. Workload Modelling (TST-003)

- Volumetrics to concurrency via Little's Law: `concurrency = arrival_rate × residence_time`.
  Documents how to derive test concurrency from business volumetrics rather than guessing
  thread counts.
- **Open versus closed workload models.** JMeter's default thread group is closed — a slow
  response reduces offered load, which masks the very saturation a stress test is looking
  for. Gatling and k6 default to open arrival-rate models. Breakpoint and spike results are
  not comparable across the two, so `workload_model` is a required field in
  `test_acceptance_criteria`. JMeter's Concurrency Thread Group / Arrivals Thread Group
  plugins provide an open model where one is required.
- Vietnam-specific peak factors: Tet, end-of-month payroll, payday clustering, NAPAS 247
  intraday profile. These set the `peak_rps` multiplier over sustained.
- Think time, pacing, and arrival distribution — constant, Poisson, and burst — with guidance
  on which to use per profile.
- Named journey blends, referenced by `blend_ref` from `test_acceptance_criteria` and owned
  by `TST-034`. Each blend names its constituent journeys, their percentage mix, and the
  per-journey tier that supplies each one's budget.

## 10. Tool Selection (TST-010) and the Four Guides

`TST-010` carries a capability matrix and a Mermaid decision tree over: protocol coverage
(HTTP, JMS, Kafka, gRPC, JDBC, SOAP, ISO 8583, ISO 20022), workload model, scripting
language, CI ergonomics, resource cost per virtual user, distributed execution, reporting,
correlation and assertion capability, and learning curve.

| Tool | Position | Strongest fit |
|---|---|---|
| **JMeter** | Primary. Canonical recipe in every archetype. | Broadest protocol coverage — JDBC, JMS, Kafka, SOAP, ISO 8583 via samplers — plus distributed master/worker execution and the HTML dashboard. Default choice for protocol-heavy banking flows. |
| **Gatling + Karate** | Secondary, highest leverage. | `karate-gatling` reuses the same Karate `.feature` files as both functional API tests and performance scenarios — one artifact, two disciplines. Open model, low resource cost per VU, strong for high concurrency. |
| **k6** | CI gate. | Thresholds-as-code make it the natural pipeline gate for the `baseline` profile. `xk6` extensions cover Kafka, SQL, and browser. Grafana-native output. |
| **Locust** | Specialist. | Chosen when a scenario needs bespoke stateful logic or reuse of existing Python domain libraries that would be awkward in JMX or Scala. |

Each guide (`TST-011` … `TST-014`) covers installation and version pinning, project layout,
2–3 complete worked examples, parameterisation and correlation, assertions and thresholds,
distributed execution, result output and baselining conventions, CI invocation, and the
common failure modes specific to that tool.

## 11. Archetype Document Anatomy (TPL-005)

Fixed section order. Discipline overlay sections appear only where the archetype applies to
that discipline; a section is omitted rather than filled with "N/A".

1. **Header** — Status, Last Reviewed, Owner, Catalog ID + Radii, Tier Applicability
2. **Applies To** — table of covered catalog rows with relative links
3. **Failure Taxonomy** — what this archetype exists to catch, as concrete defect classes
4. **Functional Test Design** — invariants, equivalence classes, boundary and negative paths,
   and the *oracle*: where expected results come from
5. **Performance Test Design** — applicable profiles from TST-002, workload inputs, and the
   NFR-002/NFR-001 tier row supplying each threshold
6. **Canonical Harness — JMeter** — fenced JMX fragment, `jmeter -n -t … -l … -e -o …` CLI,
   assertion and listener configuration
7. **Tool Fit** — JMeter / Gatling+Karate / k6 / Locust rated `BEST` / `good` / `fair` with
   a one-line reason each
8. **Overlays** — resilience, contract, security, data-quality, as applicable
9. **Test Data Requirements** — synthetic only; cross-link to TST-004
10. **Evidence & Observability** — metrics to capture, trace assertions, artifacts to attach
11. **Exit Criteria** — the archetype's `test_acceptance_criteria` fragment
12. **Compliance Mapping** — Ring 0/1/2 table
13. **Related Patterns** — links back into the pattern catalog
14. **Mermaid diagram** — test topology or load-profile shape (mandatory)

### Compliance anchors available per ring

- **Ring 0** — ISTQB test-level and test-type definitions; OWASP ASVS and WSTG for the
  security archetypes; Google SRE Workbook Chapter 5 (load and stress) for the perf family;
  NIST SP 800-53 CA-2 / CA-8 (assessment and penetration testing); `EIP §11 Test Message`.
- **Ring 1** — BCBS 230 Principle 9 (operational resilience testing, including severe-but-
  plausible scenario testing); BCBS 239 Principle 3 (accuracy — reconciliation and ledger
  invariants); PCI-DSS 4.0 §6.4 (secure development testing), §11.3–11.4 (vulnerability and
  penetration testing), §3 (data protection for the masking archetype); ISO 20022 message
  conformance for the messaging archetypes; SWIFT CSP control 2.x.
- **Ring 2** — SBV Circular 09/2020 §IV.3 (system testing and BCP drill obligations);
  Decree 13/2023 (personal-data protection, bounding what test data may contain). Both marked
  `⚠️ (working summary — pending Legal review)` per existing convention.

## 12. Delivery Waves

| Wave | Contents | New files | Edits |
|---|---|---|---|
| **A** | `README.md`, TST-001 … TST-010, TPL-005, empty-schema `_testing-coverage.yml`, both scripts, and all four registration edits | 15 | 4 |
| **B** | Tooling guides TST-011 … TST-014 | 4 | 2 |
| **C** | Families A + B archetypes (TST-020 … TST-030) | 11 | 2 |
| **D** | Families C + D archetypes (TST-031 … TST-036) | 6 | 2 |
| **E** | Families E + G archetypes (TST-037 … TST-039, TST-042 … TST-043) | 5 | 2 |
| **F** | Family F archetypes (TST-040 … TST-041) — requires `@infosec-architect` review | 2 | 2 |
| **G** | `_testing-coverage.yml` populated to all 191 rows; `coverage-matrix.md` generated; catalog reconciliation re-run | 1 | 3 |

Sequencing rationale:

- **Wave A is the gate.** Once TST-001 and TST-002 are approved as spine, every later
  archetype is mechanical fill against a fixed template and a fixed profile catalogue.
  Nothing in waves C–F should be authored before then, or profiles will diverge.
- **Wave B before the archetypes**, so archetype authors have settled JMeter conventions to
  reference rather than reinventing JMX idioms 24 times.
- **Wave F last** because the security archetypes need InfoSec ownership and review, which is
  a scheduling dependency outside QE's control.
- **Wave G last** because coverage rows reference archetype IDs, which are only final once
  every archetype file exists — otherwise `validate-testing-coverage.py` fails on check 3.

Per-wave definition of done: `markdownlint` clean; `scripts/mermaid-lint-doc.sh` clean on
every new file; `scripts/validate-internal-links.py` reports no broken links;
`scripts/audit-catalog-consistency.py` clean; from Wave G,
`scripts/validate-testing-coverage.py` exits zero.

## 13. Risks

| Risk | Mitigation |
|---|---|
| Thresholds drift from the NFR spine as archetypes multiply | No archetype states a latency or throughput number. Every threshold links to its NFR-002/NFR-001 tier row. Reviewers reject hard-coded numbers. |
| 24 archetypes drift structurally | TPL-005 fixes the section order; a wave's definition of done includes a structural diff against the template. |
| Coverage YAML rots as the catalog grows | `validate-testing-coverage.py` fails when an inventory row has no coverage row, so adding a pattern without testing coverage breaks the build. |
| Archetype boundaries prove wrong during authoring | Coverage rows accept multiple archetype IDs, so a pattern can be re-pointed without restructuring the folder. Boundary changes are cheap; renumbering is not — IDs are never reused. |
| `.markdownlint.json` is referenced by the blocking CI job but absent from the repository, so the gate is likely already failing | Out of scope here. Flagged for the platform owner; authors should run `markdownlint` with default rules locally in the interim. |
| Security archetypes blocked on InfoSec availability | Wave F is last and independent; waves A–E and G complete without it. |

## 14. Open Items

None. All five design sections and the `@qe-lead` owner handle were approved on 2026-08-12.
Two items are deliberately deferred rather than open:

- Making test evidence a DAB submission gate — needs EA Board and DAB chair decision (§2).
- Creating `.markdownlint.json` — pre-existing repository issue, not part of this scope (§13).
