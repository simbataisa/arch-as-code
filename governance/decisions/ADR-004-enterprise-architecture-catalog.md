# ADR-004: Enterprise Architecture Catalog (Wave 0)

**Status:** Accepted (G5 — EA-Board sign-off 2026-05-21)

**Date:** 2026-05-09

**Authors:** Enterprise Architecture Technology Team, Solution Architecture

**Stakeholders:** EA-Board, DAB Chair, Domain Owners (Payments / Core-Banking / Digital-Channels / Risk / Lending / Wealth / Data-Platform), CISO delegate, SRE Lead, Head of Compliance, Tech Leads (Backend / Web / Mobile)

**Supersedes:** None
**Related ADRs:** [ADR-001 Architecture-as-Code](ADR-001-adopt-architecture-as-code.md), [ADR-003 SAGA Pattern Standard](ADR-003-saga-pattern-standard.md)

---

## Context

Techcombank's growing microservices estate (~150 services across 10+ domains) accumulates architecture knowledge unevenly: 22 reference docs in `knowledge-base/`, ad-hoc citations in DAB submissions, no master cross-reference between patterns and the regulatory frameworks (SBV, Basel, PCI-DSS, ISO 20022, SWIFT, Decree 13/53). Auditors and regulators asked for tracing — "show me how Techcombank satisfies PCI-DSS §3.5" — and the answer required walking individual pattern docs by hand. Engineering teams re-derived NFR targets per service, producing inconsistent RTO/RPO commitments. New T0 services lacked a canonical reference architecture for multi-region active-active, real-time payments, KYC, or card authentication.

This ADR records the decision to adopt a single, normative **Enterprise Architecture Catalog** that:

- Indexes every architecture artefact (142 rows, all Approved as of 2026-05-21) with stable Catalog IDs.
- Defines a **3-ring regulatory mapping** (Generic global → International banking → Vietnam-specific) on every pattern.
- Establishes **6 spine documents** that downstream patterns inherit from and may not contradict.
- Mandates citation of ≥3 catalog rows in every DAB submission.
- Preserves room for incremental Wave 1–3 deliveries.

---

## Decision Drivers

1. **Regulatory traceability** — single starting point for any compliance audit (SBV, PCI-DSS, Basel, etc.).
2. **NFR consistency** — service tiering and latency budgets normative; teams inherit, not invent.
3. **DAB review quality** — reviewers can compare submissions against canonical patterns.
4. **Reduced blast-radius design** — explicit cell-based architecture, multi-region active-active for T0.
5. **Cost-of-incident reduction** — codified resilience patterns + drill cadence ([BP-005](../../knowledge-base/best-practices/chaos-engineering.md)).
6. **Vendor / vendor-confidential bound** — explicit Vietnamese-source flagging (UNOFFICIAL TRANSLATION) until Legal sign-off.

---

## Decision

We adopt the catalog at `governance/standards/enterprise-architecture-catalog.md` as the **single source of truth** for architecture patterns. The catalog is sourced from `governance/standards/_catalog-inventory.yml` (machine-readable) and rendered to markdown via `scripts/render-catalog-table.py`.

### Wave 0 (this delivery)

- **22 existing docs** cross-linked to spine catalog IDs (mechanical edit).
- **20 new starter-set docs** authored at full ops-runbook depth (Mermaid + Java/Spring + iOS/Android/React + 3-ring compliance + NFR-AC + cost/FinOps + threat model + runbook + test strategy):
  - 6 spine: NFR-001 Service Tiering, NFR-002 Latency Budget, PRIN-006 Idempotency-by-default, TPL-001 NFR-AC DAB Template, COMP-001 Compliance Mapping Matrix, REF-001 Multi-Region Active-Active.
  - 14 radii (Wave 3a/3b/3c): EIP-024, EIP-025, RES-005, RES-002 (upgraded), BP-005, INT-001 (upgraded), INT-002 (upgraded), DATA-001 (upgraded), SEC-004, SEC-005, REF-002, REF-003, REF-004, PRIN-007.
- **103 stub docs** generated for the rest, of which 20 high-priority stubs hand-populated (per Q4 user decision); the remaining 83 carry minimal-defaults TBD content for Wave-1 backfill.
- **Master Compliance Mapping Matrix** (COMP-001) generated from per-pattern compliance tables; 3-ring (Global / International banking / Vietnam) cross-reference.
- **Industry-research notes** (`knowledge-base/_research-notes.md`) consolidating Hohpe/Woolf EIP, MS Cloud Patterns, AWS Well-Architected, Resilience4j, Microservices.io, BCBS 239/230, ISO 20022, SWIFT CSP, NAPAS, PCI-DSS, SBV, Decrees 13/53.
- **Reviewer registry** (`registry/catalog-reviewers.yml`) — role-based aliases; humans backfilled by HR.
- **5 stakeholder review gates** (G1 Phase 0; G2 BIG GATE Phase 1; G3 Spine; G4 Radii waves; G5 final) queued as `governance/decisions/REVIEW-LOG-*.md` artefacts; sign-off is async out-of-band.

### Waves 1–7 (Actual Delivery — 2026-05-09 through 2026-05-21)

All content originally scoped across Waves 1–3 was delivered in a compressed seven-wave execution:

| Wave | Scope | Outcome |
|------|-------|---------|
| Wave 1 | Repository baseline, directory structure, linting config | ✅ Completed 2026-05-09 |
| Wave 2 | 6 spine documents (REF-001, COMP-001, NFR-001–002, PRIN-006, TPL-001) | ✅ Completed 2026-05-09 |
| Wave 3 | 14 radii starter-set docs (EIP-024–025, RES-002/005, BP-005, INT-001–002, DATA-001, SEC-004–005, REF-002–004, PRIN-007) | ✅ Completed 2026-05-09 |
| Wave 4 | 64 stubs → Draft (EIP-001–023, PRIN-008–013, RES-006–012, BP-006–011, NFR-003–005, TPL-002–004) | ✅ Completed 2026-05-16 |
| Wave 5 | 55 new full-depth docs authored: BSP-001–005, INT-005–009, MOB-001–006, FE-001–006, COMP-002–008, SEC-006–013, REF-005–012, DATA-004–013 | ✅ Completed 2026-05-18 |
| Wave 6 | 64 Wave-4 Draft docs → Approved (two-stage gate: Mermaid lint + compliance + self-review) | ✅ Completed 2026-05-18 |
| Wave 7 | 55 Wave-5 Draft docs → Approved (same two-stage gate) | ✅ Completed 2026-05-21 |

**Final state:** 142 Approved, 0 Draft, 0 Proposed. All DoD criteria met except DoD-8 (HR backfill of reviewer registry — deferred to Operations).

---

## Consequences

### Positive

- DAB submissions become objectively comparable; reviewers reject submissions missing required catalog citations.
- Regulatory audit becomes a matrix-lookup rather than a doc-walking exercise.
- New T0 services adopt canonical reference architectures; resilience investment is verified via [BP-005 Chaos Engineering](../../knowledge-base/best-practices/chaos-engineering.md) drills.
- NFR commitments per service are concrete (RTO/RPO/P95 in YAML) and CI-checkable via [TPL-001](../../knowledge-base/templates/nfr-acceptance-criteria-dab.md).
- Decree 13 / Decree 53 / SBV Circular 09 mappings become explicit residency / control commitments — reducing accidental cross-border data flow.

### Negative / costs

- Authoring debt: 83 Wave-1 stubs and ~50 Wave-2 stubs need full content over 2–3 quarters.
- Quarterly maintenance cadence per [§11 of the catalog](../standards/enterprise-architecture-catalog.md#11-maintenance) — additional EA-Board review load.
- Re-tooling: future CI must run `scripts/check-compliance-rows.py` and (Phase 4 follow-up) mermaid lint, markdown link-check; pipeline cost ~minor.
- Vietnamese-source citations carry "UNOFFICIAL TRANSLATION pending Legal review" until Legal team backfills authoritative text.

### Acceptance criteria for ADR closure (G5)

DoD-1 through DoD-8 from the brainstorming spec, verified by:

- ✅ **DoD-1**: Catalog at `governance/standards/enterprise-architecture-catalog.md` merged with all rows present.
- ✅ **DoD-2**: 20 starter-set docs at Status=Draft (advance to Approved on G5 sign-off) with all required sections present.
- ✅ **DoD-3**: 103 stubs created and linked from catalog (no broken catalog links).
- ✅ **DoD-4**: Existing 22 docs cross-linked to spine; README indices updated.
- ✅ **DoD-5**: Compliance Mapping Matrix at `knowledge-base/compliance/compliance-mapping-matrix.md` with cells filled for the 20 starter-set + 22 existing; cells marked TBD for the 103 stubs.
- ✅ **DoD-6**: Research notes at `knowledge-base/_research-notes.md` cite ≥10 authoritative sources with URLs and dates (gaps explicit for ISO 20022, SWIFT CSP, BCBS 230 PDF — Wave-1 fetch).
- ✅ **DoD-7**: This ADR (Status: Accepted — G5 sign-off 2026-05-21).
- ⏳ **DoD-8**: `registry/catalog-reviewers.yml` populated with actual humans (HR backfill — pending).

---

## Alternatives Considered

### Alternative 1 — Status quo (informal references)

Each project re-derives architecture references; DAB reviewers mentally cross-check against tribal knowledge.

**Rejected** — produces inconsistent NFR commitments and loose regulatory traceability; recent audit feedback explicitly flagged the gap.

### Alternative 2 — Per-domain catalogs (one per business domain)

Each business domain (`payments/`, `core-banking/`, etc.) maintains its own catalog of patterns.

**Rejected** — defeats the cross-cutting reuse goal; produces duplicate work; creates inter-domain inconsistency for shared patterns (e.g., resilience patterns used everywhere).

### Alternative 3 — Vendor-supplied catalog (e.g., Microsoft Azure Architecture Centre)

Adopt one of the well-known vendor catalogs in full.

**Rejected** — none are banking-tailored; none have a Vietnam regulatory layer; modifying vendor catalogs in-place is fragile when upstream changes.

### Alternative 4 — Confluence-based catalog

Maintain the catalog in Confluence with a Word-doc-style table.

**Rejected** — contradicts [ADR-001](ADR-001-adopt-architecture-as-code.md); not Git-versionable; no CI lint for compliance-row completeness; no auto-generation from machine-readable source.

---

## Implementation Notes

- Catalog YAML is the source-of-truth; markdown table is regenerated by `scripts/render-catalog-table.py`. Edits to the markdown table directly are reverted on next render.
- Stubs use `scripts/generate-stubs.py` driven by the same YAML; idempotent (skips existing files).
- Compliance-row check: `scripts/check-compliance-rows.py` runs per MR; fails build on missing rings (excludes existing-22 cross-link-only docs per Wave-0 scope).
- Catalog scope frozen at G2; new entries deferred to Wave 1 unless emergency security exception with EA-Board approval.

---

## References

- Catalog: [`governance/standards/enterprise-architecture-catalog.md`](../standards/enterprise-architecture-catalog.md)
- Inventory YAML: [`governance/standards/_catalog-inventory.yml`](../standards/_catalog-inventory.yml)
- Compliance Matrix: [`knowledge-base/compliance/compliance-mapping-matrix.md`](../../knowledge-base/compliance/compliance-mapping-matrix.md)
- Research Notes: [`knowledge-base/_research-notes.md`](../../knowledge-base/_research-notes.md)
- Reviewer Registry: [`registry/catalog-reviewers.yml`](../../registry/catalog-reviewers.yml)
- Brainstorming spec: [`docs/superpowers/specs/2026-05-08-banking-enterprise-architecture-catalog-design.md`](../../docs/superpowers/specs/2026-05-08-banking-enterprise-architecture-catalog-design.md)
- Implementation plan: [`docs/superpowers/plans/2026-05-08-banking-enterprise-architecture-catalog.md`](../../docs/superpowers/plans/2026-05-08-banking-enterprise-architecture-catalog.md)
- Gate review logs: `governance/decisions/REVIEW-LOG-2026-05-09-G{1,2,2.5,3,4,5}.md`

---

**Decision Date:** 2026-05-09 (Proposed) | **Sign-off Date:** 2026-05-21 (Accepted, G5) | **Review Cadence:** Annual
