# Pattern Document Template

Status: Draft | Last Reviewed: 2026-05-09 | Owner: @ea-board
Catalog ID: TPL-002
Tier Applicability: N/A (meta-document — template for pattern authors)

---

## How to Use This Template

1. Copy this file to the correct catalog location: `knowledge-base/patterns/<domain>/<your-pattern-name>.md`.
2. Assign the next available Catalog ID from `governance/standards/enterprise-architecture-catalog.md`.
3. Replace every `[PLACEHOLDER]` with real content. Replace every authoring note (lines beginning with `>`) with the actual content or delete the note once the section is complete.
4. Set Status to `Draft` while authoring. The EA Board moves it to `Approved` after review.
5. Do NOT leave `[PLACEHOLDER]` text or authoring notes in an Approved document.
6. This template file itself must never be modified to describe a real pattern — it is a meta-document only.

---

# [PLACEHOLDER: Pattern Name — e.g., "Saga Orchestration Pattern"]

Status: Draft | Last Reviewed: [YYYY-MM-DD] | Owner: [@domain-owner]
Catalog ID: [PAT-NNN] | [Spine | Radii | Leaf — choose one]
Tier Applicability: [T0 | T1 | T2 | T3 | N/A]

> **Authoring note**: The Catalog ID comes from `enterprise-architecture-catalog.md`. Spine = foundational, few; Radii = supporting; Leaf = implementation detail.

## Problem Statement

> **Authoring note**: 5–7 bullets. Each bullet names a concrete pain point that this pattern solves. Be specific to Techcombank's context (NAPAS, SBV, digital banking). Avoid generic statements.

- [PLACEHOLDER: Describe the first concrete problem — e.g., "Distributed transactions across the Ledger Service and NAPAS Channel Service create tight temporal coupling; a NAPAS timeout causes the entire ledger write to roll back, violating T0 availability targets."]
- [PLACEHOLDER: Second problem]
- [PLACEHOLDER: Third problem]
- [PLACEHOLDER: Fourth problem]
- [PLACEHOLDER: Fifth problem — regulatory or compliance dimension if applicable]

## Context

> **Authoring note**: When should an engineer reach for this pattern? List 3–5 concrete trigger scenarios.

Reach for this pattern when:

- [PLACEHOLDER: Scenario 1 — e.g., "You need to coordinate a business transaction across two or more microservices and a database rollback cannot cross service boundaries."]
- [PLACEHOLDER: Scenario 2]
- [PLACEHOLDER: Scenario 3]

## Solution

> **Authoring note**: 2–4 paragraphs of prose explaining the pattern's core idea. Follow with a mandatory Mermaid diagram. The diagram must show the actual components and message flows — not a generic box-and-arrow sketch.

[PLACEHOLDER: Explain the solution approach. Name the key components, the flow of control, and why this design solves the problems listed above. Tie back to the Techcombank context — NAPAS, Kafka, Spring Boot, Kubernetes, Aurora.]

[PLACEHOLDER: Second prose paragraph — describe the failure modes the pattern handles and how.]

### Solution Diagram

> **Authoring note**: Every pattern document requires a Mermaid diagram. Use `sequenceDiagram` for request/response flows; `graph LR` or `flowchart TD` for structural or process flows; `stateDiagram-v2` for state machines. Replace the comment below with the real diagram.

```mermaid
%% [PLACEHOLDER: Replace this comment with your Mermaid diagram]
%% Example skeleton — delete and replace:
%%
%% sequenceDiagram
%%     participant Client
%%     participant OrchestratorService
%%     participant ServiceA
%%     participant ServiceB
%%     Client->>OrchestratorService: initiateTransaction(request)
%%     OrchestratorService->>ServiceA: stepA(payload)
%%     ServiceA-->>OrchestratorService: stepA_OK
%%     OrchestratorService->>ServiceB: stepB(payload)
%%     ServiceB-->>OrchestratorService: stepB_OK
%%     OrchestratorService-->>Client: transactionComplete(result)
```

## Implementation Guidelines

> **Authoring note**: 4–6 numbered sections. Each section has a short prose explanation followed by a code sample. Provide implementations for the primary language (Java/Spring Boot) and at least one of: frontend (TypeScript/React), mobile (Swift or Kotlin), or infrastructure (YAML/Terraform). Remove sections that don't apply and add sections for any stack that IS used.

### 1. [PLACEHOLDER: First implementation concern — e.g., "Orchestrator Service — Spring Boot"]

> **Authoring note**: Explain what this code does and why it is structured this way.

[PLACEHOLDER: Brief prose about this implementation section.]

```java
// [PLACEHOLDER: Java/Spring Boot implementation]
// Package: com.techcombank.[domain].[subdomain]
//
// Replace this comment with real, compiling code.
// Include:
//   - Correct package declaration
//   - Required imports (abbreviated as // ... if obvious)
//   - Class/method signature with JavaDoc
//   - Core business logic (not a stub — real logic)
//   - Structured logging with correlation ID: log.info("event={} correlationId={}", event, correlationId)
//   - No secrets in code — use @Value("${property.name}") or AWS Secrets Manager reference
```

### 2. [PLACEHOLDER: Second implementation concern — e.g., "Compensation Transaction Handler"]

[PLACEHOLDER: Brief prose.]

```java
// [PLACEHOLDER: Java code for compensation / rollback logic]
```

### 3. [PLACEHOLDER: Third implementation concern — e.g., "Kafka event publishing with idempotency key"]

[PLACEHOLDER: Brief prose.]

```java
// [PLACEHOLDER: Kafka producer or consumer implementation]
```

### 4. [PLACEHOLDER: Fourth implementation concern — e.g., "Infrastructure — Kubernetes deployment YAML or Terraform"]

[PLACEHOLDER: Brief prose.]

```yaml
# [PLACEHOLDER: YAML configuration — HPA, ConfigMap, Kafka topic definition, etc.]
```

### 5. [PLACEHOLDER: Fifth implementation concern — optional; e.g., "Frontend / Mobile considerations"]

> **Authoring note**: Delete this section if the pattern is purely backend. Include it if the pattern has a user-visible timeout, retry, or progressive disclosure behaviour.

[PLACEHOLDER: TypeScript or Swift/Kotlin snippet if applicable.]

### 6. [PLACEHOLDER: Sixth implementation concern — optional; e.g., "Observability — Prometheus metrics and alerts"]

[PLACEHOLDER: Brief prose on what to instrument and alert on.]

```yaml
# [PLACEHOLDER: Prometheus alerting rule or Grafana dashboard panel spec]
```

## When to Use

> **Authoring note**: 3–5 positive conditions where this pattern is the right choice.

- [PLACEHOLDER: e.g., "You need atomicity across two or more services but cannot use a distributed two-phase commit (2PC) due to availability requirements."]
- [PLACEHOLDER: Second positive condition]
- [PLACEHOLDER: Third positive condition]

## When NOT to Use

> **Authoring note**: 3–5 counter-indications. Be direct — tell the author when to reach for a different pattern instead.

- [PLACEHOLDER: e.g., "The transaction involves only a single service and a single database — use a local ACID transaction instead; introducing a saga adds complexity with no benefit."]
- [PLACEHOLDER: Second counter-indication]
- [PLACEHOLDER: Third counter-indication — include a reference to the alternative pattern]

## Variants and Trade-offs

> **Authoring note**: 3–5 rows. Each row names a variant, when to prefer it, and the trade-off accepted. If there is only one way to implement the pattern, describe the key design decisions instead.

| Variant | Use when | Trade-off |
|---|---|---|
| [PLACEHOLDER: Variant name — e.g., "Orchestration (this pattern)"] | [PLACEHOLDER: e.g., "Business logic is complex; centralised visibility is important"] | [PLACEHOLDER: e.g., "Orchestrator is a single point of failure; must be T0-resilient"] |
| [PLACEHOLDER: Variant 2 — e.g., "Choreography (event-driven)"] | [PLACEHOLDER: Simpler flows; teams own their own compensations] | [PLACEHOLDER: Harder to trace; distributed debugging complexity] |
| [PLACEHOLDER: Variant 3] | [PLACEHOLDER] | [PLACEHOLDER] |

## NFR Acceptance Criteria

> **Authoring note**: YAML block. IDs follow the pattern `[SHORT-PREFIX]-NN` (e.g., `SAG-01`). Include at minimum: one availability (HA) criterion, one performance (HP) criterion, and one resilience (HR) criterion. Populate concrete numbers — no "TBD".

```yaml
nfr_acceptance_criteria:
  id: [PLACEHOLDER: PAT-NNN]
  pattern: [PLACEHOLDER: Pattern Name]

  availability:
    - id: [PLACEHOLDER: ABC-01]
      statement: >
        [PLACEHOLDER: e.g., "The orchestrator service MUST maintain 99.99% availability
        (T0 SLO). Saga state MUST be persisted to Aurora before any step is executed,
        ensuring orchestrator restarts do not lose in-flight sagas."]
      measurement: >
        [PLACEHOLDER: How to verify — e.g., "Load test with orchestrator pod restart mid-saga;
        assert all in-flight sagas resume correctly within 5 seconds of pod restart."]

  performance:
    - id: [PLACEHOLDER: ABC-02]
      statement: >
        [PLACEHOLDER: e.g., "End-to-end saga completion (all steps committed) MUST complete
        within p99 < 500 ms for a two-step saga under 500 TPS sustained load."]
      measurement: >
        [PLACEHOLDER: Gatling load test at 500 TPS; assert responseTime.percentile3 <= 500.]

  resilience:
    - id: [PLACEHOLDER: ABC-03]
      statement: >
        [PLACEHOLDER: e.g., "On step failure, the compensation sequence MUST complete within
        10 seconds and leave all participating services in a consistent rolled-back state.
        Compensation failures MUST be sent to the saga-dlq topic for manual review."]
      measurement: >
        [PLACEHOLDER: Inject step failure via WireMock; assert compensation events appear in
        Kafka within 10 seconds; assert DLQ receives the event on compensation failure."]
```

## Compliance Mapping

> **Authoring note**: 3-ring table. Ring 0 = generic industry standards (ISO 27001, NIST). Ring 1 = international banking (BCBS 230, BCBS 239, ISO 20022). Ring 2 = Vietnam-specific (SBV Circular 09/2020). Include at least one row per ring. Use the exact compliance note suffix required: Ring 2 cells MUST end with `⚠️ (working summary — pending Legal review)`; BCBS 230 cells MUST end with `⚠️ (working summary — pending PDF fetch)`.

| Ring | Regulation | Provision | How this pattern satisfies |
|---|---|---|---|
| Ring 0 | [PLACEHOLDER: e.g., ISO 27001] | [PLACEHOLDER: e.g., A.12.1.2 Change Management] | [PLACEHOLDER: How the pattern satisfies this control] |
| Ring 1 | [PLACEHOLDER: e.g., BCBS 239] | [PLACEHOLDER: e.g., §3 Timeliness] | [PLACEHOLDER: Explanation] |
| Ring 1 | [PLACEHOLDER: e.g., BCBS 230] | [PLACEHOLDER: e.g., Principle 2 — Infrastructure ⚠️ (working summary — pending PDF fetch)] | [PLACEHOLDER: Explanation] |
| Ring 2 | [PLACEHOLDER: e.g., SBV Circular 09/2020] | [PLACEHOLDER: e.g., §IV.2 Operational continuity ⚠️ (working summary — pending Legal review)] | [PLACEHOLDER: Explanation] |

## Cost / FinOps Notes

> **Authoring note**: Quantify where possible — extra compute, Kafka storage, Aurora connections. Reference the levers that reduce cost without breaking the pattern. Include a "cost of NOT using this pattern" if the alternative is an incident.

[PLACEHOLDER: e.g., "The orchestrator service adds one additional Spring Boot pod per environment (staging + production). At m5.large Reserved pricing (~USD 35/month), the annual cost is USD 840 per environment, well offset by avoiding manual reconciliation incidents that historically cost 4 engineer-days each (~USD 4,000 at loaded cost)."]

[PLACEHOLDER: Second FinOps note — Kafka topic cost, Aurora additional write IOPS, etc.]

## Threat Model Summary

> **Authoring note**: Apply STRIDE. Name the top 3 threats the pattern addresses and the top 3 residual threats with their mitigations. Be specific — name the attack vector, not just the category.

STRIDE applied to [PLACEHOLDER: pattern name]:

- **Top 3 threats addressed**:
  1. [PLACEHOLDER: e.g., "Tampering — a partial saga leaves service A committed but service B rolled back, causing data inconsistency. The orchestrator's compensation sequence reverts service A within 10 seconds."]
  2. [PLACEHOLDER: Second threat addressed]
  3. [PLACEHOLDER: Third threat addressed]
- **Top 3 residual threats**:
  1. [PLACEHOLDER: e.g., "Repudiation — a compensation fails silently and the saga state is lost. Mitigation: saga state is persisted to Aurora with a `COMPENSATION_FAILED` terminal state; the DLQ captures the event; on-call SRE reviews DLQ daily."]
  2. [PLACEHOLDER: Second residual threat + mitigation]
  3. [PLACEHOLDER: Third residual threat + mitigation]

## Operational Runbook (stub)

> **Authoring note**: List the alerts this pattern produces and the first 3 steps for each. Link to the full runbook in the internal wiki once it is written.

- **Alert `[PLACEHOLDER: AlertName]`**: [PLACEHOLDER: condition]. Steps: (1) [PLACEHOLDER] (2) [PLACEHOLDER] (3) [PLACEHOLDER].
- **Alert `[PLACEHOLDER: AlertName2]`**: [PLACEHOLDER: condition]. Steps: (1) [PLACEHOLDER] (2) [PLACEHOLDER] (3) [PLACEHOLDER].
- **Dashboards**: Grafana — `[PLACEHOLDER: dashboard-name]`.
- **Full runbook**: [PLACEHOLDER: link to wiki.techcombank.local/runbook/...]

## Test Strategy (stub)

> **Authoring note**: One bullet per test level. Describe what is tested and what the pass criterion is. Do not leave blank.

- **Unit**: [PLACEHOLDER: e.g., "Orchestrator state machine — given `STEP_A_OK` event, assert state transitions to `AWAITING_STEP_B`."]
- **Integration**: [PLACEHOLDER: e.g., "Full saga happy path with real Kafka and WireMocked downstream; assert saga reaches `COMMITTED` state within 500 ms."]
- **Performance**: [PLACEHOLDER: e.g., "Gatling 500 TPS for 30 min; assert p99 < 500 ms and zero saga stuck in `IN_PROGRESS` after test completes."]
- **Chaos**: [PLACEHOLDER: e.g., "Kill service B mid-saga; assert compensation fires within 10 s and saga reaches `COMPENSATED` state."]

## Related Patterns

> **Authoring note**: List 4–6 related catalog entries with relative links. Include: the spine NFR docs that govern this pattern's SLO, any prerequisite patterns, and common alternatives.

- [PLACEHOLDER: e.g., "[NFR-001 Service Tiering + RTO/RPO Matrix](../../nfr/service-tiering-rto-rpo.md) — tier determines the availability SLO the saga must meet"]
- [PLACEHOLDER: e.g., "[RES-002 Circuit Breaker](../resilience/circuit-breaker.md) — protects downstream calls within each saga step"]
- [PLACEHOLDER: e.g., "[PAT-NNN Alternative Pattern Name](../domain/alternative.md) — use instead when only a single service is involved"]
- [PLACEHOLDER: Add 1–3 more]

## References

> **Authoring note**: 4–8 references. Prefer primary sources (RFC, book, official docs). Include internal references (research notes, ADRs) where applicable.

- [PLACEHOLDER: e.g., "Richardson, C. (2018) — Microservices Patterns, Chapter 4: Managing Transactions with Sagas"]
- [PLACEHOLDER: e.g., "Kleppmann, M. (2017) — Designing Data-Intensive Applications, Chapter 9: Consistency and Consensus"]
- [PLACEHOLDER: e.g., "Spring Modulith — Saga support (docs.spring.io/spring-modulith)"]
- [PLACEHOLDER: e.g., "`knowledge-base/_research-notes.md` §Sagas for Techcombank-specific benchmark data"]
- [PLACEHOLDER: Add more as needed]

---

**Key Takeaway**: [PLACEHOLDER: One sentence that captures the pattern's essential insight and primary benefit. E.g., "Use saga orchestration when a business transaction spans multiple microservices and you need explicit compensation logic, a central audit trail, and a clean failure mode — accepting the trade-off that the orchestrator becomes a reliability-critical component requiring T0 treatment."]
