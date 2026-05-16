# [PLACEHOLDER: Document Title]

Status: Draft | Last Reviewed: 2026-05-09 | Owner: [@owner]
Catalog ID: [XXX-NNN]
Tier Applicability: [T0 | T1 | T2 | T3 | N/A]

> **STUB** — full content to be authored per the wave plan.
> Catalog reference: `governance/standards/enterprise-architecture-catalog.md`

## Problem Statement

TBD — describe the concrete pain points this document addresses.

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

## When to Use

- TBD — list the scenarios where this pattern/principle is the right choice.

## When Not to Use

- TBD — list the anti-patterns or scenarios where this document does not apply.

## Key Takeaway

TBD — one sentence that captures the essential insight of this document.

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
