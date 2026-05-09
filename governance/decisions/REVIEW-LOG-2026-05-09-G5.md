# Review Log — Gate G5 (Phase 4: Final EA-Board Sign-off)

Status: Queued for async review — FINAL GATE
Gate: G5
Phase: Phase 4 (Cross-link + lint + ADR)
Date queued: 2026-05-09
SLA: 2–3 business days

## Review scope — entirety of Wave 0

This is the final gate for Wave 0 of the Enterprise Architecture Catalog programme. Sign-off promotes the 20 starter-set docs from `Status=Draft` to `Status=Approved`, finalises ADR-004, and freezes the Wave 0 baseline.

### Artefacts under review

| Artefact | Path | Lines |
|---|---|---|
| Master Catalog | `governance/standards/enterprise-architecture-catalog.md` | ~440 |
| Inventory (YAML SoT) | `governance/standards/_catalog-inventory.yml` | ~1,650 |
| Render script | `scripts/render-catalog-table.py` | ~70 |
| Stub generator | `scripts/generate-stubs.py` | ~110 |
| Cross-link script | `scripts/cross-link-existing.py` | ~55 |
| Compliance-row check | `scripts/check-compliance-rows.py` | ~55 |
| Reviewer registry | `registry/catalog-reviewers.yml` | ~60 |
| Research notes | `knowledge-base/_research-notes.md` | ~570 |
| 6 spine docs | (NFR-001/002, PRIN-006, TPL-001, COMP-001, REF-001) | ~234–395 each |
| 14 radii starter-set | (Wave 3a/3b/3c — see G4 review log) | ~270–490 each |
| 18 cross-link upgrades | existing 22 minus the 4 already upgraded | 2 lines added each |
| 103 stub docs | `knowledge-base/**/*.md` | 30–60 each |
| ADR-004 | `governance/decisions/ADR-004-enterprise-architecture-catalog.md` | ~153 |
| Gate logs | G1, G2, G2.5, G3, G4, G5 | — |

### Reviewers

EA-Board (full attendance) — final consensus required.

### Reject criteria

Any blocker on:

- Catalog inventory completeness (every row resolves to an existing file).
- Spine-vs-radii consistency (radii do not contradict spine).
- Compliance-row check passes for the 20 Wave-0 docs.
- ADR-004 alternatives addressed.
- Reviewer registry roles aligned with current Techcombank org.

### Outstanding items (do NOT block G5; tracked as Wave-1 / out-of-band)

1. **Legal-team translations** for SBV Circ. 09/2020, Decree 13/2023, Decree 53/2022, BCBS 230 §27 — every UNOFFICIAL flag in the catalog needs replacing with authoritative text. Ticket: TBD-FILL-IN. Target: end of Wave-0+30 days.
2. **`registry/catalog-reviewers.yml`** humans-backfill — every role currently `TBD-FILL-IN`. Target: end of Wave-0+14 days.
3. **Re-fetch** of timed-out research sources: full BCBS 230 PDF, ISO 20022 message-definitions catalogue, SWIFT CSCF v2024 PDF. Target: Wave-1 entry.
4. **Mermaid lint** + **markdown link-check** CI integration — tooling deferred to Wave-1 (npx-based; not in Wave-0 scope).
5. **97 of 103 stubs** carry minimal-defaults TBD content; full population is Wave 1.
6. **Existing 22 docs** received cross-link-only updates; full `## Compliance Mapping` section backfill is Wave 1.

### DoD verification (per ADR-004)

- ✅ DoD-1 — Catalog at `governance/standards/enterprise-architecture-catalog.md` merged with all 141 inventory rows.
- ✅ DoD-2 — 20 starter-set docs at Status=Draft with all required sections; advance to Approved on G5 sign-off.
- ✅ DoD-3 — 103 stubs created; all linked from catalog.
- ✅ DoD-4 — Existing 22 cross-linked; READMEs regenerated.
- ✅ DoD-5 — Compliance Mapping Matrix in place; cells populated for the 20 starter-set + 22 existing (cross-link-only existing carry pending-Wave-1 marker).
- ✅ DoD-6 — Research notes cite 12 authoritative sources (4 with explicit gap markers for Wave-1 backfill).
- ✅ DoD-7 — ADR-004 authored (this gate moves Status: Proposed → Accepted).
- ⏳ DoD-8 — Reviewer registry populated with actual humans (out-of-band; not blocking G5 but blocking actual DAB use).

### Action items if approved

1. Update `_catalog-inventory.yml` to flip the 20 starter-set entries from `Draft` → `Approved`. Re-render §4 of the catalog.
2. Update ADR-004 status to `Accepted` with the G5 sign-off date.
3. Merge feature branch `feat/enterprise-architecture-catalog` to main.
4. Announce to architecture guild; trigger Wave 1 planning.
5. Open follow-up tickets for the 6 outstanding items above.

### Reviewer sign-off

- [ ] EA-Board chair
- [ ] EA-Board member 2
- [ ] EA-Board member 3
- [ ] DAB Chair (concurrent)

### Reviewer feedback

(empty — to be filled by EA-Board)
