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
| BSP-003 | Sanction Screening Pipeline | T0 | TST-025 | R | R | — | — | — | R | baseline, load, stress, soak | locust |
| BSP-004 | End-of-Day Batch Window | T0, T1 | TST-032 | R | R | R | — | — | R | baseline, load, scalability, soak, mixed | jmeter |
| BSP-005 | Reversal and Chargeback | T0 | TST-021 | R | R | R | — | — | R | baseline, load, stress, soak, failover-under-load | jmeter |
| BSP-006 | Pricing Engine | T0, T1 | TST-022 | R | R | — | — | — | R | baseline, load | locust |
| BSP-007 | Interest Calculation Engine | T0, T1 | TST-022 | R | R | — | — | — | R | baseline, load | locust |
| BSP-008 | Fee Engine | T0, T1, T2 | TST-022 | R | R | — | — | — | R | baseline, load | locust |
| BSP-009 | Tax Calculation Engine | T0, T1, T2 | TST-022 | R | R | — | — | — | R | baseline, load | locust |
| BSP-010 | Rule / Decisioning Engine | T0, T1 | TST-025 | R | R | — | — | — | R | baseline, load, stress, soak | locust |
| BSP-011 | Credit Limit Engine | T0, T1 | TST-023 | R | R | R | — | — | — | baseline, load, stress, spike | jmeter |
| BSP-012 | Transaction Limit Engine | T0, T1, T2 | TST-023 | R | R | R | — | — | — | baseline, load, stress, spike | jmeter |
| BSP-013 | Collateral Management Engine | T0, T1 | TST-023 | R | R | R | — | — | — | baseline, load, stress, spike | jmeter |
| BSP-014 | FX Rate Engine | T0, T1 | TST-022 | R | R | — | — | — | R | baseline, load | locust |
| BSP-015 | Position Keeping Engine | T0, T1 | TST-021 | R | R | R | — | — | R | baseline, load, stress, soak, failover-under-load | jmeter |
| BSP-016 | Settlement Engine | T0 | TST-021 | R | R | R | — | — | R | baseline, load, stress, soak, failover-under-load | jmeter |
| BSP-017 | Product Factory | T0, T1, T2, T3 | TST-022 | R | R | — | — | — | R | baseline, load | locust |
| BSP-018 | Accrual Engine | T0, T1 | TST-022 | R | R | — | — | — | R | baseline, load | locust |
| BSP-019 | Collections Engine | T1, T2 | TST-025, TST-032 | R | R | R | — | — | R | baseline, load, stress, soak, scalability, mixed | jmeter |
| BSP-020 | Relationship Pricing Engine | T0, T1 | TST-022 | R | R | — | — | — | R | baseline, load | locust |
| DATA-004 | Data Vault 2.0 | T2, T3 | TST-032 | R | R | R | — | — | R | baseline, load, scalability, soak, mixed | jmeter |
| EIP-001 | Message Channel | T0, T1, T2 | TST-029 | R | R | R | — | — | — | baseline, load, spike, soak, failover-under-load | jmeter |
| EIP-002 | Point-to-Point Channel | T0, T1 | TST-029 | R | R | R | — | — | — | baseline, load, spike, soak, failover-under-load | jmeter |
| EIP-003 | Publish-Subscribe Channel | T0, T1, T2 | TST-027 | R | R | R | — | — | — | baseline, load, stress, soak | jmeter |
| EIP-004 | Message Router | T0, T1 | TST-026 | R | R | — | R | — | — | baseline, load, soak | jmeter |
| EIP-005 | Content-Based Router | T0, T1 | TST-026 | R | R | — | R | — | — | baseline, load, soak | jmeter |
| EIP-006 | Message Translator | T0, T1 | TST-026 | R | R | — | R | — | — | baseline, load, soak | jmeter |
| EIP-007 | Content Enricher | T0, T1 | TST-026 | R | R | — | R | — | — | baseline, load, soak | jmeter |
| EIP-008 | Content Filter | T0, T1 | TST-026 | R | R | — | R | — | — | baseline, load, soak | jmeter |
| EIP-009 | Claim Check | T0, T1 | TST-028 | R | R | R | — | — | — | baseline, load, spike, failover-under-load | jmeter |
| EIP-010 | Normalizer | T0, T1 | TST-026 | R | R | — | R | — | — | baseline, load, soak | jmeter |
| EIP-011 | Aggregator | T0, T1 | TST-028 | R | R | R | — | — | — | baseline, load, spike, failover-under-load | jmeter |
| EIP-012 | Splitter | T0, T1 | TST-026 | R | R | — | R | — | — | baseline, load, soak | jmeter |
| EIP-013 | Resequencer | T0, T1 | TST-027 | R | R | R | — | — | — | baseline, load, stress, soak | jmeter |
| EIP-014 | Composed Message Processor | T0, T1 | TST-026 | R | R | — | R | — | — | baseline, load, soak | jmeter |
| EIP-015 | Scatter-Gather | T0, T1 | TST-028 | R | R | R | — | — | — | baseline, load, spike, failover-under-load | jmeter |
| EIP-016 | Routing Slip | T0, T1 | TST-024 | R | R | R | R | — | — | baseline, load, soak, failover-under-load | locust |
| EIP-017 | Process Manager | T0, T1 | TST-024 | R | R | R | R | — | — | baseline, load, soak, failover-under-load | locust |
| EIP-018 | Message Store | T0, T1 | TST-028 | R | R | R | — | — | — | baseline, load, spike, failover-under-load | jmeter |
| EIP-019 | Smart Proxy | T0, T1 | TST-026 | R | R | — | R | — | — | baseline, load, soak | jmeter |
| EIP-020 | Test Message | T0, T1, T2 | TST-029 | R | R | R | — | — | — | baseline, load, spike, soak, failover-under-load | jmeter |
| EIP-021 | Channel Purger | T1, T2 | TST-029 | R | R | R | — | — | — | baseline, load, spike, soak, failover-under-load | jmeter |
| EIP-022 | Durable Subscriber | T0, T1 | TST-029 | R | R | R | — | — | — | baseline, load, spike, soak, failover-under-load | jmeter |
| EIP-023 | Guaranteed Delivery | T0, T1 | TST-029 | R | R | R | — | — | — | baseline, load, spike, soak, failover-under-load | jmeter |
| EIP-024 | Idempotent Receiver | T0, T1 | TST-020 | R | R | R | — | — | R | baseline, load, stress, spike, soak | jmeter |
| EIP-025 | Dead Letter Channel | T0, T1 | TST-029 | R | R | R | — | — | — | baseline, load, spike, soak, failover-under-load | jmeter |
| INT-001 | Saga Orchestration | T0, T1 | TST-024 | R | R | R | R | — | — | baseline, load, soak, failover-under-load | locust |
| INT-003 | API Gateway Routing | T0, T1, T2, T3 | TST-030 | R | R | — | R | — | — | baseline | gatling-karate |
| INT-005 | Anti-Corruption Layer | T0, T1, T2 | TST-026 | R | R | — | R | — | — | baseline, load, soak | jmeter |
| INT-007 | Sidecar / Ambassador | T0, T1 | TST-026 | R | R | — | R | — | — | baseline, load, soak | jmeter |
| INT-008 | Backend-for-Frontend Routing | T0, T1 | TST-026 | R | R | — | R | — | — | baseline, load, soak | jmeter |
| INT-009 | Content-Based Router | T0, T1 | TST-026 | R | R | — | R | — | — | baseline, load, soak | jmeter |
| INT-010 | AsyncAPI Specification Standard | T0, T1, T2 | TST-030 | R | R | — | R | — | — | baseline | gatling-karate |
| INT-011 | CloudEvents Envelope Standard | T0, T1, T2 | TST-030 | R | R | — | R | — | — | baseline | gatling-karate |
| INT-012 | Error Code Mapping Standard | T0, T1, T2 | TST-026 | R | R | — | R | — | — | baseline, load, soak | jmeter |
| INT-013 | Schema Registry Governance | T0, T1, T2 | TST-030 | R | R | — | R | — | — | baseline | gatling-karate |
| INT-014 | Webhook Delivery Reliability | T0, T1, T2 | TST-020, TST-029 | R | R | R | — | — | R | baseline, load, stress, spike, soak | jmeter |
| INT-015 | API Contract Testing | T1, T2 | TST-030 | R | R | — | R | — | — | baseline | gatling-karate |
| INT-016 | Distributed Saga Choreography | T0, T1 | TST-024 | R | R | R | R | — | — | baseline, load, soak, failover-under-load | locust |
| INT-017 | Message Sequencer | T0, T1 | TST-027 | R | R | R | — | — | — | baseline, load, stress, soak | jmeter |
| NFR-002 | Latency Budget Model | — | — | G | G | G | G | G | G | — | jmeter |
| PLT-006 | FinOps Cost Allocation | T0, T1, T2, T3 | TST-033 | R | R | R | — | R | — | baseline, load, stress, mixed, failover-under-load | jmeter |
| PLT-008 | Multi-Tenancy Isolation | T0, T1, T2 | TST-033 | R | R | R | — | R | — | baseline, load, stress, mixed, failover-under-load | jmeter |
| REF-001 | Multi-Region Active-Active | T0, T1 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-002 | Real-Time Payments — NAPAS / Instant | T0 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-003 | KYC / AML Onboarding | T1 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-004 | Card Authorization (3DS2) | T0 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-005 | SWIFT MT/MX Wire Transfer | T0 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-006 | Loan Origination | T1 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-007 | Fraud Screening Platform | T0 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-008 | Regulatory Reporting | T1 | TST-032, TST-034 | R | R | R | — | — | R | baseline, load, scalability, soak, mixed, failover-under-load | jmeter |
| REF-009 | Account Opening (Omnichannel) | T1 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-010 | Ledger Posting Engine | T0 | TST-021, TST-034 | R | R | R | — | — | R | baseline, load, stress, soak, mixed, failover-under-load | jmeter |
| REF-011 | Open Banking (PSD2) | T1 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-012 | Dispute Management | T1 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-013 | Retail Deposits Platform | T0, T1 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-014 | Consumer Lending Platform | T0, T1 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-015 | Credit Card Issuing Platform | T0 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-016 | Corporate Lending and Syndications | T0, T1 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-017 | Trade Finance Platform | T0, T1 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-018 | Treasury and FX Platform | T0 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-019 | Wealth Management Platform | T0, T1 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| REF-020 | Cash Management and Liquidity | T0, T1 | TST-034 | R | R | R | — | — | — | mixed, soak, load, failover-under-load | jmeter |
| RES-001 | Bulkhead Isolation | T0, T1, T2 | TST-033 | R | R | R | — | R | — | baseline, load, stress, mixed, failover-under-load | jmeter |
| RES-002 | Circuit Breaker | T0, T1, T2 | TST-035, TST-031 | R | R | R | — | — | — | baseline, load, spike, failover-under-load | jmeter |
| RES-003 | Retry with Backoff | T0, T1, T2 | TST-020 | R | R | R | — | — | R | baseline, load, stress, spike, soak | jmeter |
| RES-005 | Cell-Based Architecture | T0, T1 | TST-033 | R | R | R | — | R | — | baseline, load, stress, mixed, failover-under-load | jmeter |
| RES-008 | Throttling / Rate Limiting | T0, T1, T2 | TST-031 | R | R | R | — | — | — | baseline, load, stress, spike, scalability | jmeter |
| RES-009 | Load Shedding | T0, T1 | TST-031 | R | R | R | — | — | — | baseline, load, stress, spike, scalability | jmeter |
| RES-011 | Queue-Based Load Levelling | T0, T1, T2 | TST-031 | R | R | R | — | — | — | baseline, load, stress, spike, scalability | jmeter |
| SEC-009 | Fraud Signal Collection | T0, T1 | TST-025 | R | R | — | — | — | R | baseline, load, stress, soak | locust |
| SEC-010 | Attribute-Based Access Control | T0, T1, T2 | TST-025 | R | R | — | — | R | R | baseline, load, stress, soak | locust |

Legend: `R` required · `r` recommended · `—` not applicable · `G` governs. 92 rows.

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
