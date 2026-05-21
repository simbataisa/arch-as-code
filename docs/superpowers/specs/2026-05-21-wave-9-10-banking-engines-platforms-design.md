# Wave 9 + Wave 10 Design — Banking Engines & Product Line Platforms

**Date:** 2026-05-21
**Status:** Approved for implementation

---

## Goal

Extend the enterprise architecture catalog with two sequential waves:

- **Wave 9** — Author 15 banking engine pattern docs (BSP-006–020): the core processing engines consumed by every product line
- **Wave 10** — Author 8 product line reference architecture docs (REF-013–020): end-to-end platform solutions that stitch the Wave 9 engines together

These two waves run sequentially. Wave 10 begins only after Wave 9 completes (all 15 engine docs are Approved).

---

## Current Catalog State (as of 2026-05-21)

| Status | Count |
|--------|-------|
| Approved | 142 |
| Draft | 0 |
| Proposed | 0 |
| **Total** | **142** |

**Promotion path:**
- Wave 9 authors BSP-006–020 as full-depth docs; per-group gate promotes each group Proposed → Draft
- Wave 10 authors REF-013–020 as full-depth docs; single gate promotes all 8 Proposed → Draft
- A follow-on Wave 11 quality gate (Mermaid lint + compliance + self-review) promotes all 23 Draft → Approved

After Waves 9, 10, and 11 complete the target state is: **Approved: 165, Draft: 0, Proposed: 0**.

---

## Wave 9 — Banking Engines (BSP-006–020)

### Scope

15 new `radii`-level docs in the `banking-solutions` category, extending BSP-005.

File location: `knowledge-base/patterns/banking-solutions/<slug>.md`

| ID | Title | Slug | Owner | Tier |
|----|-------|------|-------|------|
| BSP-006 | Pricing Engine | `pricing-engine.md` | `@payments-domain-owner` | T0, T1 |
| BSP-007 | Interest Calculation Engine | `interest-calculation-engine.md` | `@core-banking-domain-owner` | T0, T1 |
| BSP-008 | Fee Engine | `fee-engine.md` | `@core-banking-domain-owner` | T0, T1, T2 |
| BSP-009 | Tax Calculation Engine | `tax-calculation-engine.md` | `@head-of-compliance` | T0, T1, T2 |
| BSP-010 | Rule / Decisioning Engine | `rule-decisioning-engine.md` | `@tech-lead-backend` | T0, T1 |
| BSP-011 | Credit Limit Engine | `credit-limit-engine.md` | `@risk-management-domain-owner` | T0, T1 |
| BSP-012 | Transaction Limit Engine | `transaction-limit-engine.md` | `@risk-management-domain-owner` | T0, T1, T2 |
| BSP-013 | Collateral Management Engine | `collateral-management-engine.md` | `@risk-management-domain-owner` | T0, T1 |
| BSP-014 | FX Rate Engine | `fx-rate-engine.md` | `@payments-domain-owner` | T0, T1 |
| BSP-015 | Position Keeping Engine | `position-keeping-engine.md` | `@wealth-domain-owner` | T0, T1 |
| BSP-016 | Settlement Engine | `settlement-engine.md` | `@payments-domain-owner` | T0 |
| BSP-017 | Product Factory | `product-factory.md` | `@ea-board` | T0, T1, T2, T3 |
| BSP-018 | Accrual Engine | `accrual-engine.md` | `@core-banking-domain-owner` | T0, T1 |
| BSP-019 | Collections Engine | `collections-engine.md` | `@lending-domain-owner` | T1, T2 |
| BSP-020 | Relationship Pricing Engine | `relationship-pricing-engine.md` | `@core-banking-domain-owner` | T0, T1 |

### Sub-Wave Grouping

Executed in four groups; each group commits after passing the per-group gate.

| Sub-wave | IDs | Theme |
|----------|-----|-------|
| 9A | BSP-006–009 | Core transaction engines — pricing, interest, fee, tax |
| 9B | BSP-010–013 | Risk & control engines — rules, credit limits, transaction limits, collateral |
| 9C | BSP-014–016 | Capital markets engines — FX rate, position keeping, settlement |
| 9D | BSP-017–020 | Lifecycle engines — product factory, accrual, collections, relationship pricing |

### Document Format (15 Sections)

Every doc follows this exact section order at full ops-runbook depth (target: 380–450 lines):

```
# <Engine Name>

Status: Draft | Last Reviewed: 2026-05-21 | Owner: @<owner>
Catalog ID: BSP-XXX | Radii
Tier Applicability: T0[, T1[, T2[, T3]]]

## Problem Statement
## Context
## Solution
  ```mermaid ...```
## Implementation Guidelines
  (numbered list with Java 21 / Spring Boot 3.x code blocks)
## When to Use
## When Not to Use
## Variants
## NFR Acceptance Criteria
## Compliance Mapping
  | Layer | Reference | Section/Control | How this satisfies |
## Cost / FinOps Notes
## Threat Model Summary
## Operational Runbook (stub)
## Test Strategy (stub)
## Related Patterns
## References
---
**Key Takeaway**: ...
```

### Section Detail — Engine-Specific Expectations

**Problem Statement** — Describes the specific banking pain without the engine: inconsistency, manual re-derivation, compliance gap, or outage risk.

**Context** — Lists which product lines consume this engine, deployment topology (sidecar, shared service, embedded library), and when the engine applies vs. when a simpler approach suffices.

**Solution** — Two-layer Mermaid diagram:
1. Request/response flow (caller → engine → response with calculated value)
2. Event-driven variant (where applicable — e.g., Accrual Engine triggered by EOD event)

**Implementation Guidelines** — Numbered steps with complete Java 21 / Spring Boot 3.x code snippets covering:
- Engine API definition (REST or gRPC contract)
- Core calculation logic (formula or rule evaluation)
- Caching strategy (Redis 7 for rate/fee lookups)
- Integration with T24 core banking or downstream systems
- Configuration management (Spring Cloud Config / Vault)

**Variants** — 2–3 named variants with explicit trade-offs. Examples:
- Rule Engine: table-driven (DB-backed) vs. Drools 9.x embedded vs. OPA/Rego
- Settlement Engine: RTGS gross vs. DNS net vs. bilateral netting
- Pricing Engine: static schedule vs. parametric model vs. ML-assisted

**NFR Acceptance Criteria** — At least one measurable threshold per relevant NFR:
- Latency: p99 ≤ Xms under Y TPS
- Availability: ≥99.9% or ≥99.99% (T0 engines)
- Throughput: handles Z calculations/second

**Compliance Mapping** — 3-ring table:
- Ring 0 (Global): NIST, IFRS 9, Basel, PCI-DSS, FATF (as applicable)
- Ring 1 (International banking): BCBS 239, BCBS 230, ISO 20022, SWIFT CSP (as applicable)
- Ring 2 (Vietnam): SBV Circular 09/2020, Decree 13/2023, Decree 53 — row ends with `⚠️ (working summary — pending Legal review)`

**Threat Model** — ≥2 named threats with STRIDE category in explicit parens. Valid categories: `(Spoofing)`, `(Tampering)`, `(Repudiation)`, `(Information Disclosure)`, `(Denial of Service)`, `(Elevation of Privilege)`.

**Operational Runbook** — ≥1 named alert in `Alert: SomeName` colon format (not backtick-wrapped metrics) with p50/p99 thresholds and ≥1 named remediation step.

**Related Patterns** — ≥2 cross-links to existing catalog entries (EIP, RES, SEC, DATA, or other BSP docs).

### Tech Stack

| Component | Technology |
|-----------|-----------|
| Runtime | Java 21, Spring Boot 3.x |
| Rules | Drools 9.x, OPA/Rego |
| Events | Spring Kafka, Apache Camel |
| Persistence | PostgreSQL 16, Redis 7 |
| Batch | Spring Batch 5.x |
| Secrets | HashiCorp Vault |
| Observability | OTEL Java Agent 2.x, Micrometer |

### Per-Group Gate

After all docs in a sub-wave are authored, before promoting to Draft:

1. `bash scripts/mermaid-lint-doc.sh <file>` for every file in the group — 0 failures
2. `python3 scripts/check-compliance-rows.py` — 0 failures
3. `python3 scripts/validate-internal-links.py` — 0 broken links
4. Self-review checklist passed for every doc (all 7 items checked)
5. Update catalog rows Proposed → Draft for all docs in the group
6. Commit: `feat(catalog): Wave 9X gate — promote BSP-XXX–XXX Proposed→Draft`

---

## Wave 10 — Product Line Reference Architectures (REF-013–020)

### Scope

8 new `radii`-level docs in the `reference-architectures` category, extending REF-012.

File location: `knowledge-base/reference-architectures/<slug>.md`

| ID | Title | Slug | Owner | Key BSP engines referenced |
|----|-------|------|-------|---------------------------|
| REF-013 | Retail Deposits Platform | `retail-deposits-platform.md` | `@core-banking-domain-owner` | BSP-007, BSP-008, BSP-017, BSP-018 |
| REF-014 | Consumer Lending Platform | `consumer-lending-platform.md` | `@lending-domain-owner` | BSP-007, BSP-009, BSP-010, BSP-011, BSP-019 |
| REF-015 | Credit Card Issuing Platform | `credit-card-issuing-platform.md` | `@payments-domain-owner` | BSP-006, BSP-008, BSP-012, BSP-020 |
| REF-016 | Corporate Lending & Syndications | `corporate-lending-syndications.md` | `@lending-domain-owner` | BSP-007, BSP-010, BSP-011, BSP-013, BSP-018 |
| REF-017 | Trade Finance Platform | `trade-finance-platform.md` | `@core-banking-domain-owner` | BSP-010, BSP-013, BSP-014, BSP-016 |
| REF-018 | Treasury & FX Platform | `treasury-fx-platform.md` | `@wealth-domain-owner` | BSP-006, BSP-014, BSP-015, BSP-016 |
| REF-019 | Wealth Management Platform | `wealth-management-platform.md` | `@wealth-domain-owner` | BSP-006, BSP-015, BSP-017, BSP-020 |
| REF-020 | Cash Management & Liquidity | `cash-management-liquidity.md` | `@core-banking-domain-owner` | BSP-008, BSP-011, BSP-016, BSP-018 |

### Document Format

Same 15-section template as Wave 9, with platform-level emphasis:

**Solution diagram** — System-of-systems Mermaid diagram showing:
- Which BSP engines are invoked (with Catalog IDs)
- Which EIP/INT patterns handle messaging
- Which external systems are integrated (T24 core, NAPAS, SWIFT, card schemes)
- Which SEC controls are applied at each boundary

**Implementation Guidelines** — Java 21 / Spring Boot microservice wiring at the platform level; implementation detail for each engine lives in its BSP doc (cross-link, do not duplicate).

**Related Patterns** — Always cross-links to ≥3 BSP engines + ≥2 other catalog entries (EIP, INT, SEC, COMP).

**Compliance Mapping** — Product-line-specific regulatory obligations (e.g., REF-014 Consumer Lending references IFRS 9 loan classification; REF-017 Trade Finance references FATF Rec. 16, UCP 600).

### Wave 10 Gate

Single gate after all 8 docs are authored:

1. `bash scripts/mermaid-lint-doc.sh <file>` for all 8 files — 0 failures
2. `python3 scripts/check-compliance-rows.py` — 0 failures
3. `python3 scripts/validate-internal-links.py` — 0 broken links
4. Self-review checklist passed for every doc
5. Update catalog rows Draft → Approved for all 8
6. Commit: `feat(catalog): Wave 10 gate — promote REF-013–020 Draft→Approved`

---

## Sequencing

```
Wave 9A (BSP-006–009) → Wave 9B (BSP-010–013) → Wave 9C (BSP-014–016) → Wave 9D (BSP-017–020)
  → Wave 10 (REF-013–020)
```

Each sub-wave commits before the next begins. No parallel authoring within a wave.

---

## Success Criteria

| Metric | Target |
|--------|--------|
| New BSP engine docs authored (Wave 9) | 15 — all Draft after per-group gates |
| New REF platform docs authored (Wave 10) | 8 — all Draft after gate |
| All 23 new docs promoted to Approved (Wave 11) | 165 Approved total |
| Mermaid lint failures | 0 |
| Compliance check failures | 0 |
| Broken internal links | 0 |
| Open self-review checklist items | 0 |
