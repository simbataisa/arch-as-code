# [PLACEHOLDER: Document Title]

Status: Draft | Last Reviewed: 2026-05-09 | Owner: [@owner]
Catalog ID: [XXX-NNN]
Tier Applicability: [T0 | T1 | T2 | T3 | N/A]

> **STUB** — full content to be authored per the wave plan.
> Catalog reference: `governance/standards/enterprise-architecture-catalog.md`

## Problem Statement

TBD — describe the concrete pain points this document addresses.

## Context

_[2–3 sentences describing the banking or engineering context that motivates this pattern. Include the key constraint or driver — regulatory, operational, or architectural — that makes this pattern necessary at Techcombank. Reference the relevant service tier (T0–T3) and any SBV or BCBS obligations that apply.]_

## Sketch of Solution

- TBD — outline the approach at one level of abstraction above implementation.

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | [e.g. ISO 27001 A.x.x] | [control description — replace with actual] |
| Ring 1 | [e.g. BCBS 239 Principle N] | [control description — replace with actual] |
| Ring 2 | [e.g. SBV Circular 09/2020 Article N] | [control description] ⚠️ (working summary — pending Legal review) |

## NFR Hooks

- Availability (HA): TBD
- Performance (HP): TBD
- Resilience (HR): TBD
- Document authoring completion p50 < 5 working days from stub creation
- Stub-to-Draft promotion latency p95 < 30 calendar days

## When to Use

- TBD — list the scenarios where this pattern/principle is the right choice.

## When Not to Use

- TBD — list the anti-patterns or scenarios where this document does not apply.

## Key Takeaway

TBD — one sentence that captures the essential insight of this document.

## Variants & Trade-offs

_[Bullet points listing 2–3 variant approaches and their trade-offs. Compare operational cost, complexity, and suitability for T0 vs T1 workloads.]_

- **Variant A** — TBD: describe the primary implementation variant and when to prefer it.
- **Variant B** — TBD: describe an alternative approach and its trade-offs (e.g., higher operational cost but lower latency).

## Cost / FinOps Notes

_[Bullet points estimating infrastructure cost impact — compute, storage, egress. Reference the T0–T3 tier from NFR-001.]_

- TBD — estimate compute cost for primary deployment topology.
- TBD — estimate storage and egress costs; reference T-tier capacity envelope from NFR-001.

## Threat Model Summary

| Threat | STRIDE | Mitigation |
|--------|--------|------------|
| _[Stub promoted to Approved without complete content]_ | _(Tampering)_ | _[Automated section-count check (≥15 sections) blocks promotion in CI]_ |
| _[Sensitive design details exposed in stub before Legal review]_ | _(Information Disclosure)_ | _[Ring 2 rows marked ⚠️ pending Legal review; draft docs not published externally]_ |

## Operational Runbook (stub)

**Alert: StubPromotionBlocked** — fires when a stub doc remains in Draft status beyond 90 days without an assigned author update.

1. Assign a document owner from the EA team.
2. Schedule a 2-hour authoring session targeting the 15-section template.
3. Run `bash scripts/mermaid-lint-doc.sh <file>` and `python3 scripts/check-compliance-rows.py` before promoting.

## Test Strategy (stub)

_[Unit test: validate that the stub file passes the automated section-count check (≥15 sections). Integration test: confirm cross-links resolve after the full document is written.]_

- **Unit**: verify the stub file passes `grep -c "^## " <file>` returning ≥15.
- **Integration**: confirm that all relative links in `## Related Patterns` resolve after the full document is authored.

## Related Patterns

- [TPL-002 Pattern Doc Template](pattern-doc-template.md)
- [TPL-004 Reference Architecture Doc Template](ref-arch-doc-template.md)
- [NFR-001 Service Tiering + RTO/RPO Matrix](../nfr/service-tiering-rto-rpo.md)

## References

- Catalog: `governance/standards/enterprise-architecture-catalog.md`
- Research notes: `knowledge-base/_research-notes.md`

---

## Authoring Checklist (Definition of Done — Status: Draft → Approved)

> This checklist lives here and only here. Copy it into your PR description when submitting for EA-Board review. Do not duplicate it in the authored document.

- [ ] Problem Statement: 5–7 concrete bullets specific to Techcombank context
- [ ] Solution diagram: Mermaid diagram present and renders without error
- [ ] Implementation Guidelines: Java/Spring code sample + at least one of frontend / mobile / infra YAML
- [ ] Compliance Mapping table: 3-ring table fully populated; Ring 2 cells end with `⚠️ (working summary — pending Legal review)`; BCBS 230 cells end with `⚠️ (working summary — pending PDF fetch)`
- [ ] NFR Acceptance Criteria: YAML block with IDs; concrete measurable numbers (no "TBD")
- [ ] Cost / FinOps notes: quantified where possible
- [ ] Threat Model summary: STRIDE applied; top 3 threats addressed + top 3 residual
- [ ] Operational Runbook stub: alert names + first-response steps
- [ ] Test Strategy stub: unit / integration / performance / chaos bullets
- [ ] Related Patterns: 4–6 links with relative paths
- [ ] Line count: NFR docs 250–350 lines; Pattern docs 200–350 lines; Ref-Arch docs 250–400 lines; Template docs 150–250 lines
- [ ] No `[PLACEHOLDER]` text remaining in the authored document
- [ ] No Authoring Checklist section in the authored document (checklist lives in this stub only)
- [ ] Domain-owner review: sign-off recorded in PR comments
- [ ] EA-Board review: sign-off recorded in PR comments
