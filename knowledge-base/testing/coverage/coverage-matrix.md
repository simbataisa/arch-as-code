# Testing Coverage Matrix

Status: Approved | Last Reviewed: 2026-08-12 | Owner: @qe-lead
Catalog ID: TST-015 | Radii
Tier Applicability: N/A (generated coverage report)

## Purpose

Maps every Approved catalog row to the test archetypes that cover it, the disciplines that are
obligatory for it, and the performance profiles its tier requires. Coverage is enforced
mechanically: `scripts/validate-testing-coverage.py` fails when an inventory row has no coverage
row, so a new pattern cannot be added without deciding how it is tested.

## How to Read This Table

- **Disciplines** use the four obligation levels from
  [TST-001](../strategy/test-strategy-standard.md): `required`, `recommended`, `n/a`, `governs`.
- **`governs`** marks a meta-document that constrains testing rather than being tested.
- **Profiles** are the eight defined in [TST-002](../strategy/performance-test-standard.md).
- **Primary tool** is the default per [TST-010](../tooling/tool-selection-matrix.md); an
  archetype's Tool Fit table may justify another.

## Source of Truth

Do not hand-edit the table below. Edit `_testing-coverage.yml` and regenerate:

```bash
python3 scripts/render-testing-coverage.py
```

<!-- BEGIN GENERATED -->

| Catalog ID | Title | Tiers | Archetypes | Func | Perf | Resil | Contr | Sec | DQ | Profiles | Tool |
|---|---|---|---|---|---|---|---|---|---|---|---|
| BSP-001 | Double-Entry Ledger | T0 | TST-021 | R | R | R | — | — | R | baseline, load, stress, soak, failover-under-load | jmeter |
| BSP-002 | Idempotent Payment Key | T0 | TST-020 | R | R | R | — | — | R | baseline, load, stress, spike, soak | jmeter |
| BSP-005 | Reversal and Chargeback | T0 | TST-021 | R | R | R | — | — | R | baseline, load, stress, soak, failover-under-load | jmeter |
| BSP-015 | Position Keeping Engine | T0, T1 | TST-021 | R | R | R | — | — | R | baseline, load, stress, soak, failover-under-load | jmeter |
| BSP-016 | Settlement Engine | T0 | TST-021 | R | R | R | — | — | R | baseline, load, stress, soak, failover-under-load | jmeter |
| EIP-024 | Idempotent Receiver | T0, T1 | TST-020 | R | R | R | — | — | R | baseline, load, stress, spike, soak | jmeter |
| INT-014 | Webhook Delivery Reliability | T0, T1, T2 | TST-020 | R | R | R | — | — | R | baseline, load, stress, spike, soak | jmeter |
| NFR-002 | Latency Budget Model | — | — | G | G | G | G | G | G | — | jmeter |
| REF-010 | Ledger Posting Engine | T0 | TST-021 | R | R | R | — | — | R | baseline, load, stress, soak, failover-under-load | jmeter |
| RES-002 | Circuit Breaker | T0, T1, T2 | TST-035, TST-031 | R | R | R | — | — | — | baseline, load, spike, failover-under-load | jmeter |
| RES-003 | Retry with Backoff | T0, T1, T2 | TST-020 | R | R | R | — | — | R | baseline, load, stress, spike, soak | jmeter |

Legend: `R` required · `r` recommended · `—` not applicable · `G` governs. 11 rows.

<!-- END GENERATED -->

## Compliance Mapping

> **Authoring note**: Every Approved catalog row needs this heading, unnumbered
> (`## Compliance Mapping`, no leading digit) — `scripts/check-compliance-rows.py` enforces it
> repo-wide with no exemption for generated or meta-documents; the four existing `TPL-*`
> templates and all five `NFR-*` spine docs carry one even though they are themselves
> meta-documents. This table's own compliance disposition is about the EVIDENCE the table
> represents, not a control it implements — inherit `compliance_refs: {ring0: [], ring1: [],
> ring2: []}` in the inventory (matching the `TPL-*` convention), since the table indexes
> other documents' compliance postures rather than declaring its own.

| Layer | Reference | Section/Control | How this satisfies |
|---|---|---|---|
| Ring 0 | ISTQB requirements-traceability matrix practice | Coverage-to-requirement traceability | This table is the traceability matrix from catalog row to test archetype |
| Ring 1 | Basel BCBS 230 Principle 9 | Operational resilience — evidence that testing was performed | A generated, regenerable coverage table is durable evidence a pattern's test obligations were assigned and tracked, citable in a DAB submission |
| Ring 2 | SBV Circular 09/2020 §IV.3 ⚠️ (working summary — pending Legal review) | System testing evidence | Satisfies the expectation that test coverage across the system is documented and auditable |

## Related

- [TST-001](../strategy/test-strategy-standard.md) — disciplines and obligation levels
- [TST-002](../strategy/performance-test-standard.md) — performance profiles
- [TST-010](../tooling/tool-selection-matrix.md) — tool selection
- [`enterprise-architecture-catalog.md`](../../../governance/standards/enterprise-architecture-catalog.md) — the catalog this table covers
