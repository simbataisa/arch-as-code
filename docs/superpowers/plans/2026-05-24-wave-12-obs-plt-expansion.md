# Wave 12 — Observability + Platform Expansion Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Register existing OBS-001–005 and PLT-001–002 in the catalog markdown (they exist at Draft in the YAML but are absent from the table), author 11 new full-depth pattern docs (OBS-006–010 + PLT-003–008), run quality gates, and promote all 18 OBS/PLT docs to Approved. Target: **183 Approved** total.

**Architecture:** Two sub-waves — Wave 12A (observability, 10 docs OBS-001–010) and Wave 12B (platform engineering, 8 docs PLT-001–008). Task 0 registers the 11 new IDs in the catalog and adds missing markdown rows for existing 7 docs. Tasks 1–5 author OBS-006–010. Task 6 gates + promotes OBS-001–010. Tasks 7–12 author PLT-003–008. Task 13 gates + promotes PLT-001–008. Task 14 is final verification and v1.2.0 tag.

**Tech Stack:** Kubernetes 1.30, ArgoCD 2.x, Backstage 1.x, Prometheus + VictoriaMetrics, OpenTelemetry Collector, Jaeger/Grafana Tempo, Vector log agent, Kubecost, Spring Boot 3.x Micrometer, HashiCorp Vault, OPA/Gatekeeper, Crossplane, Helm 3.x

---

## Document Template

Every new doc follows the 15-section radii format in this order:

```
# <Pattern Name>

Status: Draft | Last Reviewed: 2026-05-24 | Owner: @<owner>
Catalog ID: <ID> | Radii
Tier Applicability: T0[, T1[, T2]]

## Problem Statement     (~150–200 words — pain without this pattern)
## Context               (~80 words — where it sits, what calls it)
## Solution              (~80 words narrative + Mermaid flowchart)
## Implementation Guidelines  (3–4 numbered subsections with YAML/bash/Java code)
## When to Use
## When Not to Use
## Variants              (table: Variant | When to prefer | Trade-off)
## NFR Acceptance Criteria (YAML block; catalog_id + named IDs e.g. OBS-006-HP-01)
## Compliance Mapping    (Ring 0 / Ring 1 / Ring 2 table)
## Cost / FinOps Notes   (5 bullet points)
## Threat Model          (≥2 STRIDE threats with category in parentheses)
## Operational Runbook   (≥1 Alert: SomeName with p50/p99 resolution times)
## Test Strategy         (Unit / Integration / Compliance / Chaos subsections)
## Related Patterns      (≥3 OBS or PLT cross-links + ≥2 other catalog entries)
## References
---
**Key Takeaway**: <one sentence>
```

**Mermaid rules:** subgraph names with spaces → `subgraph MyId["Label With Spaces"]`; multi-line labels → `NODE["Line1<br/>Line2"]`; never use `\n` in labels.

**Ring 2 rule:** Ring 2 row must reference SBV Circular 09/2020 or Decree 13/2023 and end with `⚠️ (working summary — pending Legal review)`.

**STRIDE rule:** ≥2 threats, each with explicit category in parentheses: `(Tampering)`, `(Spoofing)`, `(Repudiation)`, `(Information Disclosure)`, `(Denial of Service)`, `(Elevation of Privilege)`.

**Alert rule:** `Alert: SomeName — fires when [condition]. p50 resolution: X min; p99: Y min.`

---

## Task 0: Catalog Setup — register OBS-006–010 + PLT-003–008 in YAML + markdown

**Files:**
- Modify: `governance/standards/_catalog-inventory.yml`
- Modify: `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Append 11 new entries to `_catalog-inventory.yml`**

After the existing OBS-005 block, append OBS-006–010. After the existing PLT-002 block, append PLT-003–008. Use the same structure as existing entries:

```yaml
# --- OBS-006 ---
- id: OBS-006
  title: Error Budget Burn Rate Alerting
  category: observability
  status: Proposed
  owner: sre-lead
  path: knowledge-base/patterns/observability/error-budget-burn-rate.md
  tiers: [T0, T1, T2]
  spine_or_radii: radii
  compliance_refs:
    ring0: [Google SRE Workbook — Multi-Window Alerting]
    ring1: [BCBS 239 §5 Timeliness, BCBS 230 Principle 9]
    ring2: [SBV Circular 09/2020 §IV.3]
  last_reviewed: '2026-05-24'
  notes: Wave 12A
  target_wave: 12

- id: OBS-007
  title: Distributed Tracing Sampling Strategy
  category: observability
  status: Proposed
  owner: sre-lead
  path: knowledge-base/patterns/observability/tracing-sampling-strategy.md
  tiers: [T0, T1, T2]
  spine_or_radii: radii
  compliance_refs:
    ring0: [OpenTelemetry Specification — Sampling]
    ring1: [BCBS 239 §4 Granularity]
    ring2: [SBV Circular 09/2020 §IV.2]
  last_reviewed: '2026-05-24'
  notes: Wave 12A
  target_wave: 12

- id: OBS-008
  title: Log Aggregation Pipeline
  category: observability
  status: Proposed
  owner: sre-lead
  path: knowledge-base/patterns/observability/log-aggregation-pipeline.md
  tiers: [T0, T1, T2]
  spine_or_radii: radii
  compliance_refs:
    ring0: [OpenTelemetry Logs Specification]
    ring1: [BCBS 239 §6 Adaptability, ISO/IEC 27001 A.12.4]
    ring2: [SBV Circular 09/2020 §IV.2, Decree 13/2023 Art. 9]
  last_reviewed: '2026-05-24'
  notes: Wave 12A
  target_wave: 12

- id: OBS-009
  title: Synthetic Monitoring and Canary Probes
  category: observability
  status: Proposed
  owner: sre-lead
  path: knowledge-base/patterns/observability/synthetic-monitoring-canary.md
  tiers: [T0, T1, T2]
  spine_or_radii: radii
  compliance_refs:
    ring0: [Prometheus Blackbox Exporter, OTEL Synthetic Monitoring]
    ring1: [BCBS 230 Principle 9 — operational resilience testing]
    ring2: [SBV Circular 09/2020 §IV.3]
  last_reviewed: '2026-05-24'
  notes: Wave 12A
  target_wave: 12

- id: OBS-010
  title: Metrics Cardinality Management
  category: observability
  status: Proposed
  owner: sre-lead
  path: knowledge-base/patterns/observability/metrics-cardinality-management.md
  tiers: [T0, T1, T2]
  spine_or_radii: radii
  compliance_refs:
    ring0: [Prometheus Best Practices — Label Cardinality]
    ring1: [BCBS 239 §4 Data Granularity]
    ring2: [SBV Circular 09/2020 §IV.2]
  last_reviewed: '2026-05-24'
  notes: Wave 12A
  target_wave: 12

# --- PLT-003 ---
- id: PLT-003
  title: GitOps Deployment Pipeline
  category: platform
  status: Proposed
  owner: ea-board
  path: knowledge-base/patterns/platform/gitops-deployment-pipeline.md
  tiers: [T0, T1, T2]
  spine_or_radii: radii
  compliance_refs:
    ring0: [OpenGitOps Principles v1.0]
    ring1: [BCBS 230 Principle 7 — change management]
    ring2: [SBV Circular 09/2020 §III.2]
  last_reviewed: '2026-05-24'
  notes: Wave 12B
  target_wave: 12

- id: PLT-004
  title: Internal Developer Platform
  category: platform
  status: Proposed
  owner: platform-lead
  path: knowledge-base/patterns/platform/internal-developer-platform.md
  tiers: [T1, T2, T3]
  spine_or_radii: radii
  compliance_refs:
    ring0: [CNCF Platforms White Paper 2023]
    ring1: [BCBS 239 §6 Adaptability]
    ring2: [SBV Circular 09/2020 §III]
  last_reviewed: '2026-05-24'
  notes: Wave 12B
  target_wave: 12

- id: PLT-005
  title: Kubernetes Operator Pattern
  category: platform
  status: Proposed
  owner: ea-board
  path: knowledge-base/patterns/platform/kubernetes-operator-pattern.md
  tiers: [T0, T1, T2]
  spine_or_radii: radii
  compliance_refs:
    ring0: [Kubernetes Operator Pattern — CNCF]
    ring1: [BCBS 239 §4 Data Accuracy]
    ring2: [SBV Circular 09/2020 §IV.2]
  last_reviewed: '2026-05-24'
  notes: Wave 12B
  target_wave: 12

- id: PLT-006
  title: FinOps Cost Allocation
  category: platform
  status: Proposed
  owner: ea-board
  path: knowledge-base/patterns/platform/finops-cost-allocation.md
  tiers: [T0, T1, T2, T3]
  spine_or_radii: radii
  compliance_refs:
    ring0: [FinOps Foundation — Cost Allocation Framework]
    ring1: [BCBS 239 §4 Data Management]
    ring2: [SBV Circular 09/2020 §III]
  last_reviewed: '2026-05-24'
  notes: Wave 12B
  target_wave: 12

- id: PLT-007
  title: Platform Service Catalog
  category: platform
  status: Proposed
  owner: platform-lead
  path: knowledge-base/patterns/platform/platform-service-catalog.md
  tiers: [T0, T1, T2, T3]
  spine_or_radii: radii
  compliance_refs:
    ring0: [Backstage.io — Service Catalog Spec]
    ring1: [BCBS 239 §6 Adaptability]
    ring2: [SBV Circular 09/2020 §III.2]
  last_reviewed: '2026-05-24'
  notes: Wave 12B
  target_wave: 12

- id: PLT-008
  title: Multi-Tenancy Isolation
  category: platform
  status: Proposed
  owner: ea-board
  path: knowledge-base/patterns/platform/multi-tenancy-isolation.md
  tiers: [T0, T1, T2]
  spine_or_radii: radii
  compliance_refs:
    ring0: [NIST SP 800-204 — Microservices Security]
    ring1: [BCBS 239 §4, ISO/IEC 27001 A.9]
    ring2: [SBV Circular 09/2020 §IV.2, Decree 13/2023 Art. 9]
  last_reviewed: '2026-05-24'
  notes: Wave 12B
  target_wave: 12
```

- [ ] **Step 2: Add 18 rows to `enterprise-architecture-catalog.md`**

Find the correct location (after the last OBS/PLT section or after the existing entries section). Add 7 rows for existing docs (as `| Draft |`) and 11 rows for new docs (as `| Proposed |`). Use this format:

```markdown
| OBS-001 | OpenTelemetry Instrumentation | observability | Draft | radii | @sre-lead | `knowledge-base/patterns/observability/otel-instrumentation.md` | T0, T1, T2 | OTEL Spec; BCBS 239 §5; SBV Circ. 09 §IV.2 | 2026-05-10 | 0 | Wave 2a |
| OBS-002 | Distributed Trace Propagation | observability | Draft | radii | @sre-lead | `knowledge-base/patterns/observability/distributed-trace-propagation.md` | T0, T1, T2 | W3C TraceContext; BCBS 239 §5; SBV Circ. 09 §IV.2 | 2026-05-10 | 0 | Wave 2a |
| OBS-003 | Structured Logging Standard | observability | Draft | radii | @sre-lead | `knowledge-base/patterns/observability/structured-logging-standard.md` | T0, T1, T2 | OTEL Logs; ISO/IEC 27001 A.12.4; SBV Circ. 09 §IV.2 | 2026-05-10 | 0 | Wave 2a |
| OBS-004 | SLO Alerting | observability | Draft | radii | @sre-lead | `knowledge-base/patterns/observability/slo-alerting.md` | T0, T1, T2 | Google SRE SLO; BCBS 230 Principle 9; SBV Circ. 09 §IV.3 | 2026-05-10 | 0 | Wave 2a |
| OBS-005 | Async Middleware Observability | observability | Draft | radii | @sre-lead | `knowledge-base/patterns/observability/async-middleware-observability.md` | T0, T1, T2 | OTEL Messaging Spec; BCBS 239 §5; SBV Circ. 09 §IV.2 | 2026-05-10 | 0 | Wave 2a |
| OBS-006 | Error Budget Burn Rate Alerting | observability | Proposed | radii | @sre-lead | `knowledge-base/patterns/observability/error-budget-burn-rate.md` | T0, T1, T2 | Google SRE Workbook; BCBS 239 §5; SBV Circ. 09 §IV.3 | 2026-05-24 | 0 | Wave 12A |
| OBS-007 | Distributed Tracing Sampling Strategy | observability | Proposed | radii | @sre-lead | `knowledge-base/patterns/observability/tracing-sampling-strategy.md` | T0, T1, T2 | OTEL Sampling Spec; BCBS 239 §4; SBV Circ. 09 §IV.2 | 2026-05-24 | 0 | Wave 12A |
| OBS-008 | Log Aggregation Pipeline | observability | Proposed | radii | @sre-lead | `knowledge-base/patterns/observability/log-aggregation-pipeline.md` | T0, T1, T2 | OTEL Logs; ISO/IEC 27001 A.12.4; SBV Circ. 09 §IV.2 | 2026-05-24 | 0 | Wave 12A |
| OBS-009 | Synthetic Monitoring and Canary Probes | observability | Proposed | radii | @sre-lead | `knowledge-base/patterns/observability/synthetic-monitoring-canary.md` | T0, T1, T2 | Prometheus Blackbox; BCBS 230 Principle 9; SBV Circ. 09 §IV.3 | 2026-05-24 | 0 | Wave 12A |
| OBS-010 | Metrics Cardinality Management | observability | Proposed | radii | @sre-lead | `knowledge-base/patterns/observability/metrics-cardinality-management.md` | T0, T1, T2 | Prometheus Best Practices; BCBS 239 §4; SBV Circ. 09 §IV.2 | 2026-05-24 | 0 | Wave 12A |
| PLT-001 | Service Mesh Traffic Management | platform | Draft | radii | @platform-lead | `knowledge-base/patterns/platform/service-mesh-traffic.md` | T0, T1 | CNCF Istio; BCBS 230 Principle 7; SBV Circ. 09 §III | 2026-05-10 | 0 | Wave 2a |
| PLT-002 | CNCF Stack Selection | platform | Draft | radii | @ea-board | `knowledge-base/patterns/platform/cncf-stack-selection.md` | T0, T1, T2, T3 | CNCF Landscape; BCBS 230 Principle 7; SBV Circ. 09 §III | 2026-05-10 | 0 | Wave 2a |
| PLT-003 | GitOps Deployment Pipeline | platform | Proposed | radii | @ea-board | `knowledge-base/patterns/platform/gitops-deployment-pipeline.md` | T0, T1, T2 | OpenGitOps v1.0; BCBS 230 Principle 7; SBV Circ. 09 §III.2 | 2026-05-24 | 0 | Wave 12B |
| PLT-004 | Internal Developer Platform | platform | Proposed | radii | @platform-lead | `knowledge-base/patterns/platform/internal-developer-platform.md` | T1, T2, T3 | CNCF Platforms WP; BCBS 239 §6; SBV Circ. 09 §III | 2026-05-24 | 0 | Wave 12B |
| PLT-005 | Kubernetes Operator Pattern | platform | Proposed | radii | @ea-board | `knowledge-base/patterns/platform/kubernetes-operator-pattern.md` | T0, T1, T2 | Kubernetes Operator Pattern; BCBS 239 §4; SBV Circ. 09 §IV.2 | 2026-05-24 | 0 | Wave 12B |
| PLT-006 | FinOps Cost Allocation | platform | Proposed | radii | @ea-board | `knowledge-base/patterns/platform/finops-cost-allocation.md` | T0, T1, T2, T3 | FinOps Foundation; BCBS 239 §4; SBV Circ. 09 §III | 2026-05-24 | 0 | Wave 12B |
| PLT-007 | Platform Service Catalog | platform | Proposed | radii | @platform-lead | `knowledge-base/patterns/platform/platform-service-catalog.md` | T0, T1, T2, T3 | Backstage.io Spec; BCBS 239 §6; SBV Circ. 09 §III.2 | 2026-05-24 | 0 | Wave 12B |
| PLT-008 | Multi-Tenancy Isolation | platform | Proposed | radii | @ea-board | `knowledge-base/patterns/platform/multi-tenancy-isolation.md` | T0, T1, T2 | NIST SP 800-204; ISO/IEC 27001 A.9; SBV Circ. 09 §IV.2 | 2026-05-24 | 0 | Wave 12B |
```

- [ ] **Step 3: Verify counts and commit**

```bash
grep -c "| Approved |" governance/standards/enterprise-architecture-catalog.md
grep -c "| Draft |" governance/standards/enterprise-architecture-catalog.md
grep -c "| Proposed |" governance/standards/enterprise-architecture-catalog.md
```

Expected: Approved=165, Draft=7, Proposed=11.

```bash
git add governance/standards/_catalog-inventory.yml governance/standards/enterprise-architecture-catalog.md
git commit -m "feat(catalog): Wave 12 setup — register OBS-001–010 + PLT-001–008 in inventory + catalog"
```

---

## Task 1: OBS-006 — Error Budget Burn Rate Alerting

**File:** Create `knowledge-base/patterns/observability/error-budget-burn-rate.md`

**Spec for implementer:**

Owner: `@sre-lead` | Tiers: T0, T1, T2

**Problem:** Teams running payment APIs at SLO 99.95% have no early-warning system for budget exhaustion. A 1-hour outage burns 10% of the monthly error budget, but existing alerts only fire on symptom metrics (latency spikes, error rate). By the time an alert fires, the budget is already depleted. Slow degradation (0.1% elevated error rate sustained for days) never alerts at all. Four consequences: SLO breaches discovered after the fact during monthly review; regulatory SLA commitments to SBV unprovable; incident responders context-switch on noise rather than budget-threatening events; no data to justify freeze windows for high-SLO services.

**Solution:** Multiwindow burn-rate alerting using two complementary windows (1h fast-burn + 6h slow-burn) expressed as Prometheus recording rules and Alertmanager routes. Fast-burn catches acute outages (>14× burn rate); slow-burn catches sustained degradation (>6× burn rate). Both windows must fire simultaneously to page on-call (AND logic eliminates flaps).

**Mermaid:** flowchart LR with nodes: SLI Metrics → Recording Rules (1h/6h ratio) → Burn Rate Calculator → Alertmanager → [PagerDuty P1 (fast+slow both firing) | Slack warning (one window only)] and SLO Dashboard (Grafana)

**Implementation sections:**
1. Prometheus recording rules (ratio_rate1h, ratio_rate6h, error_budget_burn_rate_1h, error_budget_burn_rate_6h) — YAML
2. Alertmanager alert definition (FastBurnAndSlowBurn AND logic, severity: critical vs warning) — YAML  
3. Grafana SLO shield panel query and error budget remaining gauge — PromQL
4. Spring Micrometer SLI instrumentation (MeterRegistry, `http.server.requests` success rate) — Java

**NFR:** p99 alert delivery < 30s from budget breach; recording rule evaluation ≤ 15s; false-positive rate < 1/week per service

**Related:** OBS-004 (SLO Alerting), OBS-001 (OTel Instrumentation), OBS-003 (Structured Logging), RES-002 (Circuit Breaker — budget breach triggers circuit open), COMP-005 (BCBS 239 — data timeliness)

- [ ] **Step 1: Create the file** with the complete 15-section document per spec above
- [ ] **Step 2: Lint** — `bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/observability/error-budget-burn-rate.md` — expected: OK (1 block)
- [ ] **Step 3: Commit** — `git commit -m "feat(catalog): OBS-006 Error Budget Burn Rate Alerting — Wave 12"`

---

## Task 2: OBS-007 — Distributed Tracing Sampling Strategy

**File:** Create `knowledge-base/patterns/observability/tracing-sampling-strategy.md`

**Spec for implementer:**

Owner: `@sre-lead` | Tiers: T0, T1, T2

**Problem:** Without a deliberate sampling strategy, banks either trace 100% of requests (unsustainable storage cost: a payment gateway at 10K TPS generates 864M spans/day) or 1% uniformly (missing the rare-but-critical T0 payment failure traces needed for regulatory audit). Tail-based sampling is complex to configure; most teams default to head-based 1% and then cannot reproduce latency anomalies. Regulatory requirement: every failed T0 payment must have a complete trace available for 90 days.

**Solution:** Tiered sampling strategy: head-based 100% for T0 payment and fee-engine spans (using OTEL sampler `ParentBased(AlwaysOn)` with service attribute gate); tail-based 10% for T1 with error-forced sampling (Grafana Tempo tail sampler policy: always keep error spans); head-based 1% for T2 internal services. OTEL Collector pipeline routes by `service.tier` attribute.

**Mermaid:** flowchart TD showing Application → OTEL SDK → OTEL Collector (with sampler decision diamond: T0? → 100%; T1 error? → 100%; T1 normal? → 10%; T2? → 1%) → Tempo storage → Grafana query

**Implementation sections:**
1. OTEL Collector sampling pipeline config (tail_sampling processor with policies) — YAML
2. Service tier propagation via resource attribute (`service.tier: T0`) — Java OTEL SDK setup
3. Tempo retention config by tier (T0: 90d, T1: 30d, T2: 7d) — YAML
4. Sampling rate verification query in TraceQL — query + expected output

**NFR:** Tail sampler decision latency < 5s; T0 trace capture rate = 100%; storage cost per million T0 spans < $0.50

**Related:** OBS-001 (OTel Instrumentation), OBS-002 (Trace Propagation), OBS-008 (Log Aggregation — correlate traceId), COMP-005 (BCBS 239 §4 data granularity), SEC-012 (Tamper-Evident Audit Logging — payment audit chain)

- [ ] **Step 1: Create the file** with complete 15-section document per spec above
- [ ] **Step 2: Lint** — `bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/observability/tracing-sampling-strategy.md`
- [ ] **Step 3: Commit** — `git commit -m "feat(catalog): OBS-007 Distributed Tracing Sampling Strategy — Wave 12"`

---

## Task 3: OBS-008 — Log Aggregation Pipeline

**File:** Create `knowledge-base/patterns/observability/log-aggregation-pipeline.md`

**Spec for implementer:**

Owner: `@sre-lead` | Tiers: T0, T1, T2

**Problem:** A 200-microservice banking platform generates logs in three formats (JSON, plaintext, multiline Java stack traces) across Kubernetes pods, T24 Temenos VMs, and NAPAS gateway appliances. Without a unified pipeline: log correlation across system boundaries is impossible (no shared traceId field); SBV audit requests for "all logs related to account X between date A and date B" take 3 days; log retention is ad-hoc (some pods keep 7 days, legacy VMs keep 1 day, regulatory minimum is 7 years for T0 transaction logs); ERROR logs don't trigger PagerDuty.

**Solution:** Vector agent on every node → OpenSearch pipeline. Vector normalises all log formats to a shared JSON schema (traceId, spanId, serviceId, accountId, severity, timestamp ISO-8601). Router rule sends ERROR severity to Alertmanager webhook (PagerDuty integration). Three retention tiers: hot index (7d, NVMe), warm (30d, SSD), cold S3-compatible archive (7 years, for regulatory).

**Mermaid:** flowchart LR: [App Pods | T24 VM | NAPAS GW] → Vector Agent → OpenSearch Ingest Pipeline → [Hot Index 7d | Warm Index 30d | Cold Archive 7y S3] and ERROR path → Alertmanager → PagerDuty

**Implementation sections:**
1. Vector source + transform config (JSON parsing, field normalisation, traceId extraction from MDC) — YAML
2. OpenSearch ILM policy (hot→warm→cold lifecycle transitions) — JSON
3. Spring Boot Logback MDC integration (traceId/spanId injection) — XML + Java
4. Log correlation query example (find all logs for traceId across services) — OpenSearch DSL

**NFR:** Log ingestion lag < 5s p99; query for 1M log events < 10s; ERROR → PagerDuty delivery < 60s; zero log loss during Vector restart (uses disk buffer)

**Related:** OBS-001 (OTel Instrumentation), OBS-002 (Trace Propagation — traceId correlation), OBS-007 (Sampling — log/trace join), SEC-012 (Tamper-Evident Audit Logging), COMP-002 (SBV Circular 09/2020 — log retention)

- [ ] **Step 1: Create the file** with complete 15-section document per spec above
- [ ] **Step 2: Lint** — `bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/observability/log-aggregation-pipeline.md`
- [ ] **Step 3: Commit** — `git commit -m "feat(catalog): OBS-008 Log Aggregation Pipeline — Wave 12"`

---

## Task 4: OBS-009 — Synthetic Monitoring and Canary Probes

**File:** Create `knowledge-base/patterns/observability/synthetic-monitoring-canary.md`

**Spec for implementer:**

Owner: `@sre-lead` | Tiers: T0, T1, T2

**Problem:** Passive monitoring (waiting for real user errors) detects payment API failures only after customers are impacted. A 3-minute window between failure onset and first real-user error is unacceptable for T0 payment flows where SBV requires near-real-time SLA evidence. Zero-traffic periods (2–5 AM) produce no signals at all, leaving the bank blind to failures that occur just before the morning payment rush. SBV and external auditors demand SLA evidence — passive monitoring cannot produce it.

**Solution:** Prometheus Blackbox Exporter runs HTTP probes against critical endpoints every 30s (T0 payment health, fee-engine readiness, NAPAS connectivity check). Synthetic transaction replayer executes a scripted payment flow (login → balance inquiry → fund transfer → confirmation) every 5 minutes using dedicated synthetic customer accounts. Results feed the same SLO alerting pipeline as real traffic. Probe results are retained for 90 days as SLA evidence for SBV audit.

**Mermaid:** flowchart TD: [Blackbox Exporter HTTP probes | Synthetic Transaction Replayer] → Prometheus scrape → [SLO Recording Rules | Probe Up/Down alert] → Alertmanager → [PagerDuty | SLA Evidence Store (90d)]

**Implementation sections:**
1. Blackbox Exporter probe config (payment API, NAPAS ping, fee-engine health) — YAML
2. Prometheus scrape config for Blackbox targets — YAML
3. Synthetic transaction replayer (k6 script or Java RestAssured scheduled test) — JavaScript or Java
4. SLA evidence query (probe_success timeseries for 90-day availability report) — PromQL

**NFR:** Probe interval ≤ 30s for T0; synthetic transaction execution < 10s end-to-end; probe result retention 90 days; false-positive probe failure rate < 1/week

**Related:** OBS-004 (SLO Alerting — probes feed SLI), OBS-006 (Error Budget Burn Rate), RES-002 (Circuit Breaker — probe failure triggers circuit check), REF-013 (Retail Deposits — payment flow probed), COMP-006 (BCBS 230 — operational resilience testing)

- [ ] **Step 1: Create the file** with complete 15-section document per spec above
- [ ] **Step 2: Lint** — `bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/observability/synthetic-monitoring-canary.md`
- [ ] **Step 3: Commit** — `git commit -m "feat(catalog): OBS-009 Synthetic Monitoring and Canary Probes — Wave 12"`

---

## Task 5: OBS-010 — Metrics Cardinality Management

**File:** Create `knowledge-base/patterns/observability/metrics-cardinality-management.md`

**Spec for implementer:**

Owner: `@sre-lead` | Tiers: T0, T1, T2

**Problem:** Banking microservices instrument `accountId`, `customerId`, or `transactionId` as Prometheus labels, creating millions of unique time series. A single service with 5M accounts × 3 metric names × 5 label dimensions = 75M active series — crashing Prometheus and VictoriaMetrics. Memory OOM kills on the metrics server take down the entire observability stack, leaving the bank blind during incidents. New engineers do not know which labels are safe because there is no taxonomy.

**Solution:** Label cardinality policy: only low-cardinality labels allowed on raw metrics (serviceId, tier, region, productCode, httpMethod, httpStatus). High-cardinality dimensions (accountId, customerId) are pre-aggregated via Prometheus recording rules before storage. VictoriaMetrics cardinality explorer used weekly to detect new high-cardinality labels introduced by deployments. OPA/Gatekeeper admission webhook rejects Kubernetes deployments that introduce banned label patterns in metric definitions.

**Mermaid:** flowchart LR: Application emits metrics → [Low-cardinality labels: pass through | High-cardinality labels: recording rule pre-aggregate] → VictoriaMetrics store → Cardinality Explorer weekly scan → [OK: continue | Violation: OPA webhook alert + build gate failure]

**Implementation sections:**
1. Approved label taxonomy table (safe vs banned labels) and Micrometer tag filter config — Java
2. Prometheus recording rule for high-cardinality pre-aggregation (e.g. fee count by productCode, not by accountId) — YAML
3. VictoriaMetrics cardinality explorer API query to detect top-N series by label — bash/curl
4. OPA Gatekeeper constraint for banned metric label patterns in pod annotations — Rego

**NFR:** Active time series count < 2M per cluster; Prometheus memory < 16 GB; cardinality scan runs in < 5 min weekly; banned-label PR gate failure rate < 2 false-positives/week

**Related:** OBS-001 (OTel Instrumentation — where labels originate), OBS-004 (SLO Alerting — depends on clean metrics), OBS-006 (Error Budget Burn Rate — recording rules), PLT-008 (Multi-Tenancy Isolation — per-namespace metric isolation), PLT-003 (GitOps — OPA admission in deployment pipeline)

- [ ] **Step 1: Create the file** with complete 15-section document per spec above
- [ ] **Step 2: Lint** — `bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/observability/metrics-cardinality-management.md`
- [ ] **Step 3: Commit** — `git commit -m "feat(catalog): OBS-010 Metrics Cardinality Management — Wave 12"`

---

## Task 6: Wave 12A Gate — promote OBS-001–010 Draft/Proposed → Approved

**Files modified:**
- `governance/standards/_catalog-inventory.yml`
- `governance/standards/enterprise-architecture-catalog.md`
- Any OBS docs needing fixes

- [ ] **Step 1: Mermaid lint for all 10 OBS docs**

```bash
for f in \
  knowledge-base/patterns/observability/otel-instrumentation.md \
  knowledge-base/patterns/observability/distributed-trace-propagation.md \
  knowledge-base/patterns/observability/structured-logging-standard.md \
  knowledge-base/patterns/observability/slo-alerting.md \
  knowledge-base/patterns/observability/async-middleware-observability.md \
  knowledge-base/patterns/observability/error-budget-burn-rate.md \
  knowledge-base/patterns/observability/tracing-sampling-strategy.md \
  knowledge-base/patterns/observability/log-aggregation-pipeline.md \
  knowledge-base/patterns/observability/synthetic-monitoring-canary.md \
  knowledge-base/patterns/observability/metrics-cardinality-management.md; do
  bash scripts/mermaid-lint-doc.sh "$f"
done
```

- [ ] **Step 2: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```

Expected: 0 failures.

- [ ] **Step 3: Self-review diagnostic**

```bash
for f in knowledge-base/patterns/observability/*.md; do
  sections=$(grep -c "^## " "$f")
  nfr=$(grep -iE "p[0-9]+.*ms|<[0-9]+.*ms|[0-9]+%" "$f" | grep -c "")
  ring2=$(grep -c "Ring 2\|SBV\|Decree 13" "$f")
  stride=$(grep -iE "\(Tampering\)|\(Spoofing\)|\(Repudiation\)|\(Information Disclosure\)|\(Denial of Service\)|\(Elevation of Privilege\)" "$f" | grep -c "")
  alert=$(grep -c "Alert:" "$f")
  echo "$(basename $f .md): sections=$sections nfr=$nfr ring2=$ring2 stride=$stride alert=$alert"
done
```

Pass criteria: sections≥15, nfr≥1, ring2≥1, stride≥2, alert≥1. Fix any failing docs.

- [ ] **Step 4: Update OBS-001–010 status in `_catalog-inventory.yml`** — change `status: Draft` and `status: Proposed` to `status: Approved` for all 10 OBS entries.

- [ ] **Step 5: Update OBS rows in catalog markdown** — change `| Draft |` and `| Proposed |` to `| Approved |` for all 10 OBS rows.

- [ ] **Step 6: Verify counts**

```bash
grep -c "| Approved |" governance/standards/enterprise-architecture-catalog.md
grep -c "| Draft |" governance/standards/enterprise-architecture-catalog.md
grep -c "| Proposed |" governance/standards/enterprise-architecture-catalog.md
```

Expected: Approved=175, Draft=2 (PLT-001–002), Proposed=6 (PLT-003–008).

- [ ] **Step 7: Commit**

```bash
git add knowledge-base/patterns/observability/ governance/standards/_catalog-inventory.yml governance/standards/enterprise-architecture-catalog.md
git commit -m "feat(catalog): Wave 12A gate — promote OBS-001–010 to Approved"
```

---

## Task 7: PLT-003 — GitOps Deployment Pipeline

**File:** Create `knowledge-base/patterns/platform/gitops-deployment-pipeline.md`

**Spec for implementer:**

Owner: `@ea-board` | Tiers: T0, T1, T2

**Problem:** Manual Kubernetes deployments via `kubectl apply` leave no audit trail, making it impossible to answer "who deployed what to production at what time" — a BCBS 230 change management requirement. Environment drift between dev, UAT, and prod is common: a hotfix applied directly to prod via `kubectl` is not reflected in the git repo, breaking the next automated deployment. Rollback requires hunting for the previous Docker image tag and re-applying manually. SBV inspectors requesting the change log for a prod deployment window cannot be satisfied from any system.

**Solution:** ArgoCD ApplicationSet pattern with one Application per environment (dev, uat, prod). All changes flow through pull requests to the git repo (Helm chart values files per env). ArgoCD syncs automatically for dev/uat, requires manual sync approval for prod. Rollback is `git revert` + ArgoCD sync. Every sync event is recorded in the ArgoCD audit log (who approved, which commit SHA, timestamp).

**Mermaid:** flowchart LR: Developer PR → GitHub Actions (lint + test) → Merge to main → ArgoCD ApplicationSet → [Dev: auto-sync | UAT: auto-sync | Prod: manual approval] → Kubernetes cluster. Also show: git revert → ArgoCD auto-detect → rollback path.

**Implementation sections:**
1. ArgoCD ApplicationSet definition (matrix generator for dev/uat/prod) — YAML
2. Helm chart values file structure per environment (values-dev.yaml, values-prod.yaml) — YAML
3. GitHub Actions workflow for image tag promotion (PR from dev values → prod values) — YAML
4. Vault agent injector integration in deployment template (secrets never in git) — YAML

**NFR:** Prod deployment end-to-end (merge→live) < 10 min; ArgoCD sync health check < 30s; rollback (git revert + sync) < 5 min; audit log retention 7 years

**Related:** PLT-001 (Service Mesh Traffic Management — ArgoCD deploys mesh config), PLT-005 (Kubernetes Operator — operators deployed via GitOps), PLT-008 (Multi-Tenancy — ApplicationSet scoped per namespace), SEC-007 (Secrets Rotation — Vault injector), COMP-006 (BCBS 230 — change management audit trail)

- [ ] **Step 1: Create the file** with complete 15-section document per spec above
- [ ] **Step 2: Lint** — `bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/platform/gitops-deployment-pipeline.md`
- [ ] **Step 3: Commit** — `git commit -m "feat(catalog): PLT-003 GitOps Deployment Pipeline — Wave 12"`

---

## Task 8: PLT-004 — Internal Developer Platform

**File:** Create `knowledge-base/patterns/platform/internal-developer-platform.md`

**Spec for implementer:**

Owner: `@platform-lead` | Tiers: T1, T2, T3

**Problem:** A 200-engineer banking engineering org has no central service registry: 200+ microservices with no ownership, no API contracts, no dependency map. Onboarding a new engineer to build a new service takes 2 weeks (finding the right template, configuring CI/CD, setting up Vault access, registering in API gateway). Teams re-invent scaffolding for every new service. SBV IT audit requires a list of all production services with their owners and SLOs — producing this manually takes a week.

**Solution:** Backstage-based Internal Developer Platform with three pillars: (1) Software Catalog (all services, APIs, teams registered via catalog-info.yaml in each repo); (2) Golden Path Templates (scaffolding for Spring Boot microservice, Kafka consumer, BFF — generates repo + CI/CD + Vault policy + ArgoCD Application in < 5 min); (3) TechDocs (auto-published from markdown in each repo, searchable in the portal).

**Mermaid:** flowchart TD: Developer → Backstage Portal → [Software Catalog (discover services, find owners, SLO scorecards) | Scaffolder (create new service from golden path template) | TechDocs (browse API docs, ADRs)] → [ArgoCD (deploy) | Vault (secrets) | GitHub (code) | Prometheus (SLOs)]

**Implementation sections:**
1. catalog-info.yaml entity definition for a banking microservice (Component, API, System entities) — YAML
2. Backstage scaffolder template for Spring Boot microservice (generates repo, Dockerfile, Helm chart, ArgoCD Application) — YAML
3. Backstage TechDocs integration (mkdocs.yml, GitHub Actions publish) — YAML
4. SLO scorecard plugin config (links Prometheus SLO metrics to catalog entity) — TypeScript/YAML

**NFR:** Catalog entity search response < 500ms; new service scaffolding end-to-end < 5 min; TechDocs build < 3 min; catalog completeness ≥ 95% (automated via missing catalog-info.yaml detection)

**Related:** PLT-003 (GitOps — Backstage triggers ArgoCD), PLT-007 (Platform Service Catalog — Backstage IS the service catalog), PLT-001 (Service Mesh — services registered in Backstage), OBS-004 (SLO Alerting — SLO scores displayed in Backstage), SEC-007 (Secrets Rotation — Vault policy generated by scaffolder)

- [ ] **Step 1: Create the file** with complete 15-section document per spec above
- [ ] **Step 2: Lint** — `bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/platform/internal-developer-platform.md`
- [ ] **Step 3: Commit** — `git commit -m "feat(catalog): PLT-004 Internal Developer Platform — Wave 12"`

---

## Task 9: PLT-005 — Kubernetes Operator Pattern

**File:** Create `knowledge-base/patterns/platform/kubernetes-operator-pattern.md`

**Spec for implementer:**

Owner: `@ea-board` | Tiers: T0, T1, T2

**Problem:** Banking platform teams manage domain-specific resources (fee schedules, sweep configurations, product rate tables) through a mix of database admin scripts, Helm chart values, and manual API calls. When a new branch opens, operators must manually run 12 setup steps across 5 systems in the correct order — taking 4 hours with frequent human error. There is no Kubernetes-native way to express "a FeeSchedule that requires BSP-006 and BSP-008 to be running before it activates."

**Solution:** Kubernetes Operator using controller-runtime. Banking CRDs (`FeeSchedule`, `SweepConfiguration`, `ProductRateTable`) managed by a reconciliation loop. The operator watches for CRD changes, validates pre-conditions (dependent services healthy), applies configuration via BSP engine APIs, and reports status conditions (`Ready`, `Failed`, `Degraded`). Finalizers ensure cleanup on deletion.

**Mermaid:** flowchart TD: GitOps (ArgoCD) applies FeeSchedule CRD → Kubernetes API Server → [Operator Controller watches] → Reconcile loop: [check BSP-006 ready? | check BSP-008 ready? | call Fee Engine API to activate schedule] → Status: Ready. Also show: CRD delete → Finalizer → cleanup → Remove CRD.

**Implementation sections:**
1. CRD definition for `FeeSchedule` (spec, status, finalizer annotations) — YAML
2. controller-runtime Reconciler implementation (Reconcile method, condition update, error requeue) — Go
3. RBAC for operator service account (limited to banking namespace) — YAML
4. Helm chart for operator deployment (with CRD install hook) — YAML

**NFR:** Reconciliation loop latency < 5s for ready state; operator pod restart recovers full reconciliation within 30s; CRD validation rejects invalid fee schedules in < 1s admission time

**Related:** PLT-003 (GitOps — operators deployed via ArgoCD), PLT-008 (Multi-Tenancy — operator RBAC scoped per namespace), BSP-006 (Pricing Engine — FeeSchedule operator configures), BSP-008 (Fee Engine — SweepConfiguration operator), OBS-001 (OTel Instrumentation — operator emits reconciliation metrics)

- [ ] **Step 1: Create the file** with complete 15-section document per spec above
- [ ] **Step 2: Lint** — `bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/platform/kubernetes-operator-pattern.md`
- [ ] **Step 3: Commit** — `git commit -m "feat(catalog): PLT-005 Kubernetes Operator Pattern — Wave 12"`

---

## Task 10: PLT-006 — FinOps Cost Allocation

**File:** Create `knowledge-base/patterns/platform/finops-cost-allocation.md`

**Spec for implementer:**

Owner: `@ea-board` | Tiers: T0, T1, T2, T3

**Problem:** A 200-microservice banking platform running on Kubernetes has no cost visibility below the cluster level. Finance knows the total cloud bill but cannot allocate it to product teams (retail lending, payments, treasury). Engineering over-provisions T2 workloads with T0-grade compute because there are no chargeback consequences. When the board asks "what does the payments platform cost per transaction?", the answer takes two weeks of manual estimation. Over-running 120% of budget has no automated alert.

**Solution:** Kubecost deployed per cluster for real-time namespace-level cost allocation. Every namespace is labelled with `costCenter`, `product`, and `tier`. Monthly chargeback report generated by Kubecost API → delivered to finance via scheduled export to S3. Budget alert fires at 80% and 100% of monthly namespace allocation (Prometheus alert on Kubecost cost metrics). T2 namespaces use spot instances (Karpenter node pool with `capacity-type: spot`).

**Mermaid:** flowchart LR: Kubernetes Namespaces (labelled with costCenter/product/tier) → Kubecost → [Real-time cost dashboard | Monthly chargeback S3 export | Budget alert at 80%/100%]. Also show: Karpenter → Spot nodes for T2 → 60% cost reduction.

**Implementation sections:**
1. Namespace label taxonomy and enforcement via OPA Gatekeeper (required labels policy) — Rego + YAML
2. Kubecost Helm deployment + cost allocation config (namespace cost grouping) — YAML
3. Budget alert Prometheus rule (kubecost_namespace_monthly_cost > threshold) — YAML
4. Karpenter NodePool for T2 spot instances (on-demand fallback for T0) — YAML

**NFR:** Cost allocation report latency < 1h; budget alert delivery < 5 min from threshold breach; spot instance interruption handling < 30s (Karpenter node replacement); monthly report accuracy ± 2%

**Related:** PLT-003 (GitOps — Karpenter config deployed via ArgoCD), PLT-008 (Multi-Tenancy — namespace = cost boundary), PLT-004 (IDP — cost scorecard per service in Backstage), OBS-004 (SLO Alerting — budget alert uses same Alertmanager), BSP-017 (Product Factory — new product line triggers new namespace + budget)

- [ ] **Step 1: Create the file** with complete 15-section document per spec above
- [ ] **Step 2: Lint** — `bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/platform/finops-cost-allocation.md`
- [ ] **Step 3: Commit** — `git commit -m "feat(catalog): PLT-006 FinOps Cost Allocation — Wave 12"`

---

## Task 11: PLT-007 — Platform Service Catalog

**File:** Create `knowledge-base/patterns/platform/platform-service-catalog.md`

**Spec for implementer:**

Owner: `@platform-lead` | Tiers: T0, T1, T2, T3

**Problem:** 200+ banking microservices with no central registry. Engineers spend 30 min per integration searching Confluence for API docs that may be 2 years out of date. A production incident requires 20 minutes to identify the on-call owner of a failing upstream service. SBV IT audit requires a list of all production APIs with their security controls — producing this manually takes 3 days. Dependency mapping for blast-radius analysis before a deployment is done informally via Slack.

**Solution:** Backstage Software Catalog as the authoritative service registry. Every service has a `catalog-info.yaml` in its repo declaring: owner (team), system (domain), dependencies (consumesAPIs, providesAPIs), SLO link (Prometheus query), and security classification (T0/T1/T2). CI/CD validates catalog-info.yaml on every PR. API specs (OpenAPI 3.1, AsyncAPI 2.x) are registered in the catalog and rendered in the portal. Dependency graph enables blast-radius analysis before deployments.

**Mermaid:** flowchart TD: Each service repo has catalog-info.yaml → GitHub Actions validates schema → Backstage ingests → [API Docs browser (OpenAPI/AsyncAPI rendered) | Dependency Graph (who calls whom) | Ownership registry (team → services) | SLO Scorecards (Prometheus-backed) | Security classification labels]

**Implementation sections:**
1. catalog-info.yaml full example for a T0 banking microservice (all required fields) — YAML
2. OpenAPI 3.1 spec registration in Backstage (API entity + spec URL) — YAML
3. CI validation GitHub Action (validate catalog-info.yaml schema on PR) — YAML
4. Backstage custom plugin: SLO scorecard fetching Prometheus availability metric — TypeScript snippet

**NFR:** Catalog search response < 200ms; dependency graph render for 200 services < 3s; catalog completeness ≥ 95% (automated gap detection); API spec freshness check (warn if OpenAPI not updated in > 30 days)

**Related:** PLT-004 (IDP — Backstage scaffolder populates catalog), PLT-003 (GitOps — ArgoCD applications registered as catalog entities), PLT-008 (Multi-Tenancy — namespace ownership in catalog), OBS-004 (SLO Alerting — SLO scores displayed in catalog), SEC-010 (ABAC — service security classification from catalog)

- [ ] **Step 1: Create the file** with complete 15-section document per spec above
- [ ] **Step 2: Lint** — `bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/platform/platform-service-catalog.md`
- [ ] **Step 3: Commit** — `git commit -m "feat(catalog): PLT-007 Platform Service Catalog — Wave 12"`

---

## Task 12: PLT-008 — Multi-Tenancy Isolation

**File:** Create `knowledge-base/patterns/platform/multi-tenancy-isolation.md`

**Spec for implementer:**

Owner: `@ea-board` | Tiers: T0, T1, T2

**Problem:** A shared Kubernetes cluster runs both T0 payment services and T2 internal tooling in the same namespace. A misconfigured T2 batch job consumes all cluster CPU, causing T0 payment API latency to spike and SLO breach. A developer in the reporting team can `kubectl exec` into a T0 payment pod (no RBAC namespace scoping). Secrets for different product lines (retail lending credentials, treasury API keys) share the same Vault mount. SBV inspectors ask for network-level proof that the sanctions screening service cannot be accessed by internal analytics services.

**Solution:** Namespace-per-tenant isolation with four enforcement layers: (1) NetworkPolicy deny-all default + explicit allow rules per service; (2) RBAC namespace-scoped roles (developers can only exec in their own namespace); (3) ResourceQuota + LimitRange per namespace tier (T0: Guaranteed QoS, T2: Burstable); (4) Vault namespace-per-tenant (separate PKI, separate KV mounts). OPA/Gatekeeper enforces naming conventions and required labels on all resources.

**Mermaid:** flowchart TD: Kubernetes Cluster → [T0 Namespace: payment-prod (Guaranteed QoS, deny-all NetworkPolicy, Vault ns: payment) | T1 Namespace: lending-prod (Burstable, NetworkPolicy: allow lending/* only) | T2 Namespace: analytics (Burstable, spot nodes, Vault ns: analytics)]. Show NetworkPolicy blocking analytics→payment traffic.

**Implementation sections:**
1. NetworkPolicy deny-all default + allow rules for T0 payment namespace — YAML
2. ResourceQuota + LimitRange for T0 (Guaranteed QoS) vs T2 (Burstable) namespaces — YAML
3. RBAC: namespace-scoped developer role (no exec/logs in other namespaces) — YAML
4. OPA Gatekeeper constraint for required namespace labels (`tier`, `costCenter`, `owner`) — Rego

**NFR:** NetworkPolicy enforcement latency < 1ms; RBAC deny response < 50ms; namespace provisioning (full isolation stack) < 5 min via GitOps; ResourceQuota eviction protection: T0 pods never evicted for T2 workload pressure

**Related:** PLT-003 (GitOps — namespace manifests in git), PLT-006 (FinOps — namespace = cost boundary), PLT-005 (Operator — operator RBAC scoped per namespace), SEC-010 (ABAC — namespace-level access control), SEC-003 (Zero Trust — network policy as zero-trust enforcement)

- [ ] **Step 1: Create the file** with complete 15-section document per spec above
- [ ] **Step 2: Lint** — `bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/platform/multi-tenancy-isolation.md`
- [ ] **Step 3: Commit** — `git commit -m "feat(catalog): PLT-008 Multi-Tenancy Isolation — Wave 12"`

---

## Task 13: Wave 12B Gate — promote PLT-001–008 Draft/Proposed → Approved

**Files modified:**
- `governance/standards/_catalog-inventory.yml`
- `governance/standards/enterprise-architecture-catalog.md`
- Any PLT docs needing fixes

- [ ] **Step 1: Mermaid lint for all 8 PLT docs**

```bash
for f in \
  knowledge-base/patterns/platform/service-mesh-traffic.md \
  knowledge-base/patterns/platform/cncf-stack-selection.md \
  knowledge-base/patterns/platform/gitops-deployment-pipeline.md \
  knowledge-base/patterns/platform/internal-developer-platform.md \
  knowledge-base/patterns/platform/kubernetes-operator-pattern.md \
  knowledge-base/patterns/platform/finops-cost-allocation.md \
  knowledge-base/patterns/platform/platform-service-catalog.md \
  knowledge-base/patterns/platform/multi-tenancy-isolation.md; do
  bash scripts/mermaid-lint-doc.sh "$f"
done
```

- [ ] **Step 2: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```

- [ ] **Step 3: Internal link validator**

```bash
python3 scripts/validate-internal-links.py
```

Expected: 0 broken links. PLT relative links from `knowledge-base/patterns/platform/` use `../observability/xxx.md`, `../resilience/xxx.md`, `../security/xxx.md`, `../banking-solutions/xxx.md`.

- [ ] **Step 4: Self-review diagnostic**

```bash
for f in knowledge-base/patterns/platform/*.md; do
  sections=$(grep -c "^## " "$f")
  nfr=$(grep -iE "p[0-9]+.*ms|<[0-9]+.*ms|[0-9]+%" "$f" | grep -c "")
  ring2=$(grep -c "Ring 2\|SBV\|Decree 13" "$f")
  stride=$(grep -iE "\(Tampering\)|\(Spoofing\)|\(Repudiation\)|\(Information Disclosure\)|\(Denial of Service\)|\(Elevation of Privilege\)" "$f" | grep -c "")
  alert=$(grep -c "Alert:" "$f")
  echo "$(basename $f .md): sections=$sections nfr=$nfr ring2=$ring2 stride=$stride alert=$alert"
done
```

Fix any failing docs.

- [ ] **Step 5: Update PLT-001–008 in `_catalog-inventory.yml`** — `status: Draft/Proposed` → `status: Approved` for all 8 PLT entries.

- [ ] **Step 6: Update PLT rows in catalog markdown** — `| Draft |` and `| Proposed |` → `| Approved |` for all 8 PLT rows.

- [ ] **Step 7: Verify counts**

```bash
grep -c "| Approved |" governance/standards/enterprise-architecture-catalog.md
grep -c "| Draft |" governance/standards/enterprise-architecture-catalog.md
grep -c "| Proposed |" governance/standards/enterprise-architecture-catalog.md
```

Expected: **Approved=183, Draft=0, Proposed=0**.

- [ ] **Step 8: Commit**

```bash
git add knowledge-base/patterns/platform/ governance/standards/_catalog-inventory.yml governance/standards/enterprise-architecture-catalog.md
git commit -m "feat(catalog): Wave 12B gate — promote PLT-001–008 to Approved"
```

---

## Task 14: Final Verification + Tag v1.2.0

- [ ] **Step 1: Final catalog count verification**

```bash
grep -c "| Approved |" governance/standards/enterprise-architecture-catalog.md
grep -c "| Draft |" governance/standards/enterprise-architecture-catalog.md
grep -c "| Proposed |" governance/standards/enterprise-architecture-catalog.md
```

Expected: **Approved=183, Draft=0, Proposed=0**.

- [ ] **Step 2: Run all quality scripts**

```bash
python3 scripts/check-compliance-rows.py
python3 scripts/validate-internal-links.py
```

Expected: 0 failures, 0 broken links.

- [ ] **Step 3: Tag v1.2.0**

```bash
git tag -a v1.2.0 -m "v1.2.0 — Wave 12 complete: 183 Approved (10 OBS + 8 PLT observability and platform patterns)"
```

- [ ] **Step 4: Final git log**

```bash
git log --oneline -8
```

Expected top entries:
```
feat(catalog): Wave 12B gate — promote PLT-001–008 to Approved
feat(catalog): PLT-008 Multi-Tenancy Isolation — Wave 12
feat(catalog): PLT-007 Platform Service Catalog — Wave 12
feat(catalog): PLT-006 FinOps Cost Allocation — Wave 12
feat(catalog): PLT-005 Kubernetes Operator Pattern — Wave 12
feat(catalog): PLT-004 Internal Developer Platform — Wave 12
feat(catalog): PLT-003 GitOps Deployment Pipeline — Wave 12
feat(catalog): Wave 12A gate — promote OBS-001–010 to Approved
```

---

## Self-Review

**Spec coverage:**
- Task 0: 11 new IDs registered + 7 existing OBS/PLT added to catalog markdown ✅
- Tasks 1–5: OBS-006–010 authored (5 docs) ✅
- Task 6: Wave 12A gate — OBS-001–010 Approved ✅
- Tasks 7–12: PLT-003–008 authored (6 docs) ✅
- Task 13: Wave 12B gate — PLT-001–008 Approved ✅
- Task 14: 183 Approved, v1.2.0 ✅

**No placeholders** — all tasks have exact file paths, commands, and expected outputs ✅

**Type consistency** — all file paths use kebab-case filenames, all IDs use correct prefix (OBS-/PLT-) ✅
