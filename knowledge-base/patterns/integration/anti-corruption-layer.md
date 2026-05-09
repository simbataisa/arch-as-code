# Anti-Corruption Layer

Status: Proposed | Target Wave: 2 | Owner: @tech-lead-backend
Catalog ID: INT-005
Tier Applicability: T0, T1, T2

> **STUB** — full content authored in Wave 2.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

Modern Techcombank Java services need to integrate with T24 / mainframe core banking — but adopting T24's data model, terminology, or behavioural assumptions into greenfield services pollutes the new architecture. An Anti-Corruption Layer (DDD term) is the bilingual translator between the legacy bounded context and the new one: it isolates the new domain model from legacy concepts, shielding it from accidental coupling and from churn when T24 is eventually replaced.

## Sketch of Solution

- Place ACL as an explicit service or module sitting between consumers and T24 OFS bridge
- ACL owns: mapping (T24 fields ↔ domain model), translation (currency, date formats, codes), error normalisation
- Strict contract on the modern side; T24-specific quirks contained inside ACL
- Versioned: when T24 is upgraded or replaced (Strangler Fig — INT-006), only ACL changes, not consumers
- Pair with idempotency (PRIN-006) to keep T24 bridge calls safely retryable

## Compliance Hooks

- Ring 0: DDD (Eric Evans) Tactical Patterns
- Ring 1: BCBS 239 §6 (data accuracy across system boundaries)
- Ring 2: SBV Circular 09/2020 §IV (data integrity) ⚠️ (working summary — pending Legal review)

## NFR Hooks

- HA: ACL itself horizontally scalable; cache T24 lookups where safe
- HP: adds 5–20ms P95 (translation overhead); often offset by reducing T24 round-trips
- HR: legacy outage → ACL returns degraded responses with explicit "stale" flag

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid diagram showing ACL between greenfield service and T24
- [ ] Java sample with mapper / DTO / domain-model separation
- [ ] T24 OFS bridge integration notes
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps (extra hop cost; long-term migration savings)
- [ ] Threat Model (data-integrity risk during translation)
- [ ] Operational Runbook
- [ ] Test Strategy (golden-master tests for T24 round-trips)

## References

- Eric Evans — Domain-Driven Design (Anti-Corruption Layer)
- Microsoft Cloud Pattern: Anti-Corruption Layer
- Catalog: INT-006 Strangler Fig; PRIN-006 Idempotency; INT-002 Outbox+CDC
