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
| BP-001 | CI/CD Pipeline Design | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| BP-002 | Disaster Recovery Playbook | T0, T1 | TST-035 | R | R | R | — | — | — | baseline, load, spike, failover-under-load | jmeter |
| BP-003 | Microservice Decomposition | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| BP-004 | Observability Standards | T0, T1, T2, T3 | TST-042 | R | R | R | — | — | — | load, spike, soak | jmeter |
| BP-005 | Chaos Engineering | T0, T1 | TST-035 | R | R | R | — | — | — | baseline, load, spike, failover-under-load | jmeter |
| BP-006 | Capacity Planning | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| BP-007 | Golden Signals (SRE) | T0, T1, T2 | TST-042 | R | R | R | — | — | — | load, spike, soak | jmeter |
| BP-008 | Error Budgets | T0, T1 | TST-042 | R | R | R | — | — | — | load, spike, soak | jmeter |
| BP-009 | Runbook Authoring | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| BP-010 | Incident Postmortem | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| BP-011 | Blameless Culture | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
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
| COMP-001 | Compliance Mapping Matrix (master) | — | — | G | G | G | G | G | G | — | jmeter |
| COMP-002 | SBV Circular 09/2020/TT-NHNN — Deep Dive | — | — | G | G | G | G | G | G | — | jmeter |
| COMP-003 | Decree 13/2023 (Personal Data) — Deep Dive | — | — | G | G | G | G | G | G | — | jmeter |
| COMP-004 | PCI-DSS 4.0 — Deep Dive | — | — | G | G | G | G | G | G | — | jmeter |
| COMP-005 | Basel BCBS 239 — Deep Dive | — | — | G | G | G | G | G | G | — | jmeter |
| COMP-006 | Basel BCBS 230 (Operational Resilience) — Deep Dive | — | — | G | G | G | G | G | G | — | jmeter |
| COMP-007 | ISO 20022 Messaging — Deep Dive | — | — | G | G | G | G | G | G | — | jmeter |
| COMP-008 | SWIFT CSP v2024 — Deep Dive | — | — | G | G | G | G | G | G | — | jmeter |
| DATA-001 | CQRS Pattern | T1, T2 | TST-037 | R | R | R | — | — | R | baseline, load, spike, soak, failover-under-load | jmeter |
| DATA-002 | Data Mesh Ownership | T1, T2 | TST-039 | R | R | — | — | — | R | baseline, load | locust |
| DATA-003 | Temporal Tables | T1, T2 | TST-038 | R | R | — | — | — | R | baseline, load, scalability | jmeter |
| DATA-004 | Data Vault 2.0 | T2, T3 | TST-032, TST-038 | R | R | R | — | — | R | baseline, load, scalability, soak, mixed | jmeter |
| DATA-005 | Slowly Changing Dimensions | T2, T3 | TST-038 | R | R | — | — | — | R | baseline, load, scalability | jmeter |
| DATA-006 | Lambda Architecture | T2, T3 | TST-037 | R | R | R | — | — | R | baseline, load, spike, soak, failover-under-load | jmeter |
| DATA-007 | Kappa Architecture | T2, T3 | TST-037 | R | R | R | — | — | R | baseline, load, spike, soak, failover-under-load | jmeter |
| DATA-008 | Change Data Capture (general) | T0, T1 | TST-037 | R | R | R | — | — | R | baseline, load, spike, soak, failover-under-load | jmeter |
| DATA-009 | Data Lineage | T1, T2 | TST-039 | R | R | — | — | — | R | baseline, load | locust |
| DATA-010 | Time-Series Modelling | T2, T3 | TST-038 | R | R | — | — | — | R | baseline, load, scalability | jmeter |
| DATA-011 | Data Quality Rules | T1, T2 | TST-039 | R | R | — | — | — | R | baseline, load | locust |
| DATA-012 | Data Virtualization | T2, T3 | TST-037 | R | R | R | — | — | R | baseline, load, spike, soak, failover-under-load | jmeter |
| DATA-013 | Reference Data Master | T0, T1 | TST-039 | R | R | — | — | — | R | baseline, load | locust |
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
| FE-001 | Web Performance Budgets | T0, T1, T2 | TST-043 | R | R | — | — | R | — | baseline, load | k6 |
| FE-002 | Web Resilience / Offline-First | T1, T2 | TST-043 | R | R | — | — | R | — | baseline, load | k6 |
| FE-003 | Web CSP Hardening | T0, T1, T2 | TST-041 | R | R | — | — | R | R | baseline, load, stress, soak | jmeter |
| FE-004 | Web Feature Flags | T1, T2, T3 | TST-036 | R | R | R | — | R | — | baseline, load, failover-under-load | jmeter |
| FE-005 | Web Error Boundary | T0, T1, T2 | TST-043 | R | R | — | — | R | — | baseline, load | k6 |
| FE-006 | Web i18n / RTL | T0, T1, T2, T3 | TST-043 | R | R | — | — | R | — | baseline, load | k6 |
| INT-001 | Saga Orchestration | T0, T1 | TST-024 | R | R | R | R | — | — | baseline, load, soak, failover-under-load | locust |
| INT-002 | Transactional Outbox + CDC | T0, T1 | TST-037 | R | R | R | — | — | R | baseline, load, spike, soak, failover-under-load | jmeter |
| INT-003 | API Gateway Routing | T0, T1, T2, T3 | TST-030 | R | R | — | R | — | — | baseline | gatling-karate |
| INT-004 | Event Sourcing | T0, T1 | TST-037 | R | R | R | — | — | R | baseline, load, spike, soak, failover-under-load | jmeter |
| INT-005 | Anti-Corruption Layer | T0, T1, T2 | TST-026 | R | R | — | R | — | — | baseline, load, soak | jmeter |
| INT-006 | Strangler Fig | T1, T2 | TST-036 | R | R | R | — | R | — | baseline, load, failover-under-load | jmeter |
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
| MOB-001 | Mobile Offline Queue | T1, T2 | TST-043 | R | R | — | — | R | — | baseline, load | k6 |
| MOB-002 | Mobile Secure Storage | T0, T1 | TST-041 | R | R | — | — | R | R | baseline, load, stress, soak | jmeter |
| MOB-003 | Mobile Biometric Auth | T0, T1 | TST-040 | R | R | — | — | R | — | baseline, load, soak | jmeter |
| MOB-004 | Mobile Push Notification (Secure) | T1, T2 | TST-041 | R | R | — | — | R | R | baseline, load, stress, soak | jmeter |
| MOB-005 | Mobile Deep Link Attestation | T0, T1 | TST-041 | R | R | — | — | R | R | baseline, load, stress, soak | jmeter |
| MOB-006 | Mobile Force-Upgrade | T0, T1, T2 | TST-036, TST-043 | R | R | R | — | R | — | baseline, load | k6 |
| NFR-001 | Service Tiering + RTO/RPO Matrix | — | — | G | G | G | G | G | G | — | jmeter |
| NFR-002 | Latency Budget Model | — | — | G | G | G | G | G | G | — | jmeter |
| NFR-003 | Capacity Planning Model | — | — | G | G | G | G | G | G | — | jmeter |
| NFR-004 | Throughput Model | — | — | G | G | G | G | G | G | — | jmeter |
| NFR-005 | Error Budget Policy | — | — | G | G | G | G | G | G | — | jmeter |
| OBS-001 | OpenTelemetry Instrumentation | T0, T1, T2 | TST-042 | R | R | R | — | — | — | load, spike, soak | jmeter |
| OBS-002 | Distributed Trace Propagation | T0, T1, T2 | TST-042 | R | R | R | — | — | — | load, spike, soak | jmeter |
| OBS-003 | Structured Logging Standard | T0, T1, T2 | TST-042 | R | R | R | — | — | — | load, spike, soak | jmeter |
| OBS-004 | SLO Alerting | T0, T1, T2 | TST-042 | R | R | R | — | — | — | load, spike, soak | jmeter |
| OBS-005 | Async Middleware Observability | T0, T1, T2 | TST-042 | R | R | R | — | — | — | load, spike, soak | jmeter |
| OBS-006 | Error Budget Burn Rate Alerting | T0, T1, T2 | TST-042 | R | R | R | — | — | — | load, spike, soak | jmeter |
| OBS-007 | Distributed Tracing Sampling Strategy | T0, T1, T2 | TST-042 | R | R | R | — | — | — | load, spike, soak | jmeter |
| OBS-008 | Log Aggregation Pipeline | T0, T1, T2 | TST-042 | R | R | R | — | — | — | load, spike, soak | jmeter |
| OBS-009 | Synthetic Monitoring and Canary Probes | T0, T1, T2 | TST-042 | R | R | R | — | — | — | load, spike, soak | jmeter |
| OBS-010 | Metrics Cardinality Management | T0, T1, T2 | TST-042 | R | R | R | — | — | — | load, spike, soak | jmeter |
| PLT-001 | Service Mesh Traffic Management | T0, T1 | TST-036 | R | R | R | — | R | — | baseline, load, failover-under-load | jmeter |
| PLT-002 | CNCF Stack Selection | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| PLT-003 | GitOps Deployment Pipeline | T0, T1, T2 | TST-036 | R | R | R | — | R | — | baseline, load, failover-under-load | jmeter |
| PLT-004 | Internal Developer Platform | T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| PLT-005 | Kubernetes Operator Pattern | T0, T1, T2 | TST-036 | R | R | R | — | R | — | baseline, load, failover-under-load | jmeter |
| PLT-006 | FinOps Cost Allocation | T0, T1, T2, T3 | TST-033 | R | R | R | — | R | — | baseline, load, stress, mixed, failover-under-load | jmeter |
| PLT-007 | Platform Service Catalog | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| PLT-008 | Multi-Tenancy Isolation | T0, T1, T2 | TST-033 | R | R | R | — | R | — | baseline, load, stress, mixed, failover-under-load | jmeter |
| PRIN-001 | API-First Design | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| PRIN-002 | Event-Driven Architecture | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| PRIN-003 | Zero-Trust Security | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| PRIN-004 | Database-Per-Service | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| PRIN-005 | Cloud-Native-First | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| PRIN-006 | Idempotency-by-default | T0, T1, T2 | — | G | G | G | G | G | G | — | jmeter |
| PRIN-007 | Data Residency | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| PRIN-008 | Defense-in-Depth | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| PRIN-009 | Observability-First | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| PRIN-010 | Fail-Safe Defaults | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| PRIN-011 | Least-Privilege | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| PRIN-012 | Async-by-default | T0, T1, T2 | — | G | G | G | G | G | G | — | jmeter |
| PRIN-013 | Modular Monolith Preference | T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
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
| RES-001 | Bulkhead Isolation | T0, T1, T2 | TST-033, TST-035 | R | R | R | — | R | — | baseline, load, stress, mixed, failover-under-load | jmeter |
| RES-002 | Circuit Breaker | T0, T1, T2 | TST-035, TST-031 | R | R | R | — | — | — | baseline, load, spike, failover-under-load | jmeter |
| RES-003 | Retry with Backoff | T0, T1, T2 | TST-020, TST-035 | R | R | R | — | — | R | baseline, load, stress, spike, soak | jmeter |
| RES-004 | Graceful Degradation | T0, T1, T2 | TST-035 | R | R | R | — | — | — | baseline, load, spike, failover-under-load | jmeter |
| RES-005 | Cell-Based Architecture | T0, T1 | TST-033 | R | R | R | — | R | — | baseline, load, stress, mixed, failover-under-load | jmeter |
| RES-006 | Timeout Budget | T0, T1, T2 | TST-035 | R | R | R | — | — | — | baseline, load, spike, failover-under-load | jmeter |
| RES-007 | Fallback Strategies | T0, T1 | TST-035 | R | R | R | — | — | — | baseline, load, spike, failover-under-load | jmeter |
| RES-008 | Throttling / Rate Limiting | T0, T1, T2 | TST-031 | R | R | R | — | — | — | baseline, load, stress, spike, scalability | jmeter |
| RES-009 | Load Shedding | T0, T1 | TST-031 | R | R | R | — | — | — | baseline, load, stress, spike, scalability | jmeter |
| RES-010 | Leader Election | T0, T1, T2 | TST-035 | R | R | R | — | — | — | baseline, load, spike, failover-under-load | jmeter |
| RES-011 | Queue-Based Load Levelling | T0, T1, T2 | TST-031 | R | R | R | — | — | — | baseline, load, stress, spike, scalability | jmeter |
| RES-012 | Health Check Aggregation | T0, T1, T2, T3 | TST-035 | R | R | R | — | — | — | baseline, load, spike, failover-under-load | jmeter |
| SEC-001 | mTLS Service Mesh | T0, T1, T2 | TST-040 | R | R | — | — | R | — | baseline, load, soak | jmeter |
| SEC-002 | OAuth2 Authorization | T0, T1, T2 | TST-040 | R | R | — | — | R | — | baseline, load, soak | jmeter |
| SEC-003 | Vault Secret Management | T0, T1, T2 | TST-036 | R | R | R | — | R | — | baseline, load, failover-under-load | jmeter |
| SEC-004 | Tokenization + HSM Key Management | T0, T1 | TST-041 | R | R | — | — | R | R | baseline, load, stress, soak | jmeter |
| SEC-005 | BFF + Token-Binding (web + iOS + Android) | T0, T1 | TST-040 | R | R | — | — | R | — | baseline, load, soak | jmeter |
| SEC-006 | JWT Best Practices | T0, T1, T2 | TST-040 | R | R | — | — | R | — | baseline, load, soak | jmeter |
| SEC-007 | Secrets Rotation | T0, T1, T2 | TST-036 | R | R | R | — | R | — | baseline, load, failover-under-load | jmeter |
| SEC-008 | Data Masking | T0, T1, T2 | TST-041 | R | R | — | — | R | R | baseline, load, stress, soak | jmeter |
| SEC-009 | Fraud Signal Collection | T0, T1 | TST-025 | R | R | — | — | — | R | baseline, load, stress, soak | locust |
| SEC-010 | Attribute-Based Access Control | T0, T1, T2 | TST-025, TST-040 | R | R | — | — | R | R | baseline, load, stress, soak | jmeter |
| SEC-011 | Session Revocation | T0, T1, T2 | TST-040 | R | R | — | — | R | — | baseline, load, soak | jmeter |
| SEC-012 | Tamper-Evident Audit Logging | T0, T1 | TST-041 | R | R | — | — | R | R | baseline, load, stress, soak | jmeter |
| SEC-013 | PII Tokenization (Format-Preserving) | T0, T1 | TST-041 | R | R | — | — | R | R | baseline, load, stress, soak | jmeter |
| TPL-001 | NFR Acceptance Criteria — DAB Submission Template | — | — | G | G | G | G | G | G | — | jmeter |
| TPL-002 | Pattern Doc Template | — | — | G | G | G | G | G | G | — | jmeter |
| TPL-003 | Stub Doc Template | — | — | G | G | G | G | G | G | — | jmeter |
| TPL-004 | Reference Architecture Doc Template | — | — | G | G | G | G | G | G | — | jmeter |
| TPL-005 | Test Archetype Doc Template | — | — | G | G | G | G | G | G | — | jmeter |
| TST-001 | Test Strategy Standard | — | — | G | G | G | G | G | G | — | jmeter |
| TST-002 | Performance Test Standard | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| TST-003 | Workload Modelling | T0, T1, T2 | — | G | G | G | G | G | G | — | jmeter |
| TST-004 | Test Data Management | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| TST-005 | Test Environments and Quality Gates | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| TST-006 | Resilience Test Standard | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-007 | Contract and Integration Test Standard | T0, T1, T2 | — | G | G | G | G | G | G | — | jmeter |
| TST-008 | Security Test Standard | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-009 | Data Quality Test Standard | T0, T1, T2 | — | G | G | G | G | G | G | — | jmeter |
| TST-010 | Test Tool Selection Matrix | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| TST-011 | JMeter Guide | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| TST-012 | Gatling + Karate Guide | T0, T1, T2 | — | G | G | G | G | G | G | — | jmeter |
| TST-013 | k6 Guide | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| TST-014 | Locust Guide | T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| TST-015 | Testing Coverage Matrix | — | — | G | G | G | G | G | G | — | jmeter |
| TST-016 | QE Harness Reference Implementation | T0, T1, T2, T3 | — | G | G | G | G | G | G | — | jmeter |
| TST-020 | Idempotency Replay Safety | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-021 | Ledger and Monetary Invariant | T0 | — | G | G | G | G | G | G | — | jmeter |
| TST-022 | Deterministic Calculation Engine | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-023 | Concurrent Limit & Counter Contention | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-024 | Saga and Compensation Correctness | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-025 | Decision Table and Screening Accuracy | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-026 | Message Transformation and Routing Correctness | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-027 | Ordering, Sequencing and Resequencing | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-028 | Fan-out / Fan-in Correlation | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-029 | Delivery Guarantee, Retry, and Dead Letter Queue Testing | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-030 | Contract and Schema Compatibility | T0, T1, T2 | — | G | G | G | G | G | G | — | jmeter |
| TST-031 | Rate Limit, Throttle & Breakpoint Testing | T0, T1, T2 | — | G | G | G | G | G | G | — | jmeter |
| TST-032 | Batch Window and Cutoff Throughput | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-033 | Multi-Tenant Isolation and Noisy-Neighbour Testing | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-034 | Blended Journey Workload Testing | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-035 | Fault Injection and Graceful Degradation Testing | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-036 | Zero-Downtime Deploy, Traffic Shift and Rotation Testing | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-037 | Read-Model Convergence and CDC Lag | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-038 | Temporal and Historisation Correctness | T1, T2 | — | G | G | G | G | G | G | — | jmeter |
| TST-039 | Data Quality and Reconciliation | T0, T1, T2 | — | G | G | G | G | G | G | — | jmeter |
| TST-040 | AuthN/AuthZ Matrix & Token Lifecycle | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-041 | Data Protection, Masking & Tokenisation | T0, T1 | — | G | G | G | G | G | G | — | jmeter |
| TST-042 | Telemetry and Observability Verification | T0, T1, T2 | — | G | G | G | G | G | G | — | jmeter |
| TST-043 | Client Experience, Offline Sync and Performance Budget Testing | T1, T2 | — | G | G | G | G | G | G | — | jmeter |

Legend: `R` required · `r` recommended · `—` not applicable · `G` governs. 232 rows.

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
