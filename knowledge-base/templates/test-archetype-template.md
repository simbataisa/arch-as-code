# Test Archetype Document Template

Status: Approved | Last Reviewed: 2026-08-12 | Owner: @qe-lead
Catalog ID: TPL-005 | Radii
Tier Applicability: N/A (meta-document — template for archetype authors)

---

## How to Use This Template

1. Copy this file to `knowledge-base/testing/archetypes/<archetype-name>.md`.
2. Take the next unallocated `TST-0NN` ID from the range `TST-020`–`TST-043` as recorded in
   `governance/standards/enterprise-architecture-catalog.md`. Never reuse an ID.
3. Replace every `[PLACEHOLDER]` with real content. Delete every authoring note (lines
   beginning with `>`) once its section is complete.
4. Keep the 14 section headings below, in this order, with this exact spelling — except §11, which is deliberately unnumbered (`## Compliance Mapping`, no leading `11.`): the repository's `scripts/check-compliance-rows.py` gate matches only a literal `^## Compliance Mapping` heading with no numeric prefix, and every Approved/Draft catalog row must pass it — including this template itself and every archetype that copies it. Omit an overlay subsection in §7 entirely if the archetype does not apply to that discipline — never leave it filled with "N/A".
5. State no latency, throughput, RTO, RPO, or availability number. Link to the owning
   spine row instead: [NFR-001](../nfr/service-tiering-rto-rpo.md),
   [NFR-002](../nfr/latency-budget-model.md),
   [NFR-003](../nfr/capacity-planning-model.md),
   [NFR-004](../nfr/throughput-model.md),
   [NFR-005](../nfr/error-budget-policy.md).
6. Use synthetic data in every example. No PII or PHI. See
   [TST-004](../testing/strategy/test-data-management.md).
7. Set Status to `Draft` while authoring. The EA Board moves it to `Approved` after review.
8. This template file itself must never be modified to describe a real archetype.

---

# [PLACEHOLDER: Archetype Name — e.g., "Idempotency and Replay Safety"]

Status: Draft | Last Reviewed: [YYYY-MM-DD] | Owner: @qe-lead
Catalog ID: [TST-0NN] | Radii
Tier Applicability: [T0 | T1 | T2 | T3 — the tiers for which this archetype is mandatory]

## 1. Applies To

> **Authoring note**: One row per catalog row this archetype covers. The path column is a
> working relative link — `../../patterns/<domain>/<file>.md`. Two rows belong in the same
> archetype only when the *method of verification* is the same, not merely the domain.

| Catalog ID | Title | Document |
|---|---|---|
| [PLACEHOLDER: e.g. BSP-002] | [Idempotent Payment Key] | [`../../patterns/banking-solutions/idempotent-payment-key.md`] |

## 2. Failure Taxonomy

> **Authoring note**: 5–8 concrete defect classes this archetype exists to catch. Each is a
> specific, observable failure — not a quality attribute. "Duplicate posting when the client
> retries after a gateway timeout" is a defect class; "correctness issues" is not.

- [PLACEHOLDER: Defect class 1]

## 3. Functional Test Design

> **Authoring note**: Name the *oracle* first — where expected results come from. One of:
> `golden-dataset`, `invariant-assertion`, `confusion-matrix`, `contract-schema`. Then list
> the invariants as assertable statements, the equivalence classes, and the boundary and
> negative paths. Every invariant must be mechanically checkable.

**Oracle:** [PLACEHOLDER]

### Invariants

| # | Invariant | Assertion |
|---|---|---|
| I1 | [PLACEHOLDER] | [PLACEHOLDER] |

### Equivalence classes and boundaries

- [PLACEHOLDER]

### Negative paths

- [PLACEHOLDER]

## 4. Performance Test Design

> **Authoring note**: Name only the profiles from [TST-002](../testing/strategy/performance-test-standard.md)
> that apply, and say why each applies to *this* archetype. State the workload model —
> `open` or `closed` per [TST-003](../testing/strategy/workload-modelling.md) — and the
> spine row supplying each threshold. Do not restate the threshold value.

| Profile | Applies | Why | Threshold source |
|---|---|---|---|
| [PLACEHOLDER] | yes/no | [PLACEHOLDER] | [NFR-00N tier row] |

**Workload model:** [open | closed] — [reason]

## 5. Canonical Harness — JMeter

> **Authoring note**: A fenced JMX fragment naming the real elements used, plus the CLI
> invocation, plus assertion and listener configuration. Parameterise via `${__P(name,default)}`
> so the same plan runs at every profile. Never embed real data.

```xml
<!-- [PLACEHOLDER: JMX fragment] -->
```

```bash
jmeter -n -t [plan].jmx \
  -Jusers=... -Jrampup=... -Jduration=... \
  -l results.jtl -e -o report/
```

## 6. Tool Fit

> **Authoring note**: Rate all four. `BEST` for exactly one. Give a one-line reason each.

| Tool | Fit | When to prefer |
|---|---|---|
| JMeter | [BEST/good/fair] | [PLACEHOLDER] |
| Gatling + Karate | [BEST/good/fair] | [PLACEHOLDER] |
| k6 | [BEST/good/fair] | [PLACEHOLDER] |
| Locust | [BEST/good/fair] | [PLACEHOLDER] |

## 7. Overlays

> **Authoring note**: Include only the subsections that apply. Delete the others entirely.

### Resilience overlay

### Contract overlay

### Security overlay

### Data-quality overlay

## 8. Test Data Requirements

> **Authoring note**: Synthetic only. State the entities needed, the cardinality driver, the
> referential-integrity requirement, and the teardown. Cross-link
> [TST-004](../testing/strategy/test-data-management.md).

## 9. Evidence and Observability

> **Authoring note**: Metrics to capture, trace assertions, and the artifacts to attach to a
> DAB submission.

## 10. Exit Criteria

> **Authoring note**: The archetype's `test_acceptance_criteria` fragment, as defined in
> [TST-001](../testing/strategy/test-strategy-standard.md). Only the fields this archetype
> constrains.

```yaml
test_acceptance_criteria:
  archetypes: [TST-0NN]
  # [PLACEHOLDER]
```

## Compliance Mapping

> **Authoring note**: This heading is deliberately unnumbered — see "How to Use This
> Template" step 4. `scripts/check-compliance-rows.py` matches only a literal
> `^## Compliance Mapping` heading; a numbered variant (`## 11. Compliance Mapping`) fails
> the gate. Do not add a number back.

| Layer | Reference | Section/Control | How this satisfies |
|---|---|---|---|
| Ring 0 | [PLACEHOLDER] | [PLACEHOLDER] | [PLACEHOLDER] |
| Ring 1 | [PLACEHOLDER] | [PLACEHOLDER] | [PLACEHOLDER] |
| Ring 2 | [PLACEHOLDER] ⚠️ (working summary — pending Legal review) | [PLACEHOLDER] | [PLACEHOLDER] |

## 12. Related Patterns

- [PLACEHOLDER: link back into the pattern catalog]

## 13. Related Archetypes

- [PLACEHOLDER: sibling archetypes that commonly run alongside this one]

## 14. Diagram

> **Authoring note**: Mandatory. Use `sequenceDiagram` for a replay or fault sequence,
> `graph LR` for a test topology, `xychart-beta` or `graph LR` for a load-profile shape.

```mermaid
%% [PLACEHOLDER: Replace with the real diagram]
```
