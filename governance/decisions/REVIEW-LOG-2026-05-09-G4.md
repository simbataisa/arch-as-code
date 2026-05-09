# Review Log — Gate G4 (Phase 3: Radii Waves 3a/3b/3c)

Status: Queued for async review (3 sub-waves batched)
Gate: G4
Phase: Phase 3 (Radii — 14 docs across 3 sub-waves)
Date queued: 2026-05-09
SLA: 2–3 business days per sub-wave (3a, 3b parallel; 3c heavier)

## Review scope

### Wave 3a — Resilience + EIP (5 docs)
- `knowledge-base/patterns/eip/idempotent-receiver.md` — EIP-024
- `knowledge-base/patterns/eip/dead-letter-channel.md` — EIP-025
- `knowledge-base/patterns/resilience/cell-based-architecture.md` — RES-005
- `knowledge-base/patterns/resilience/circuit-breaker.md` — RES-002 (upgraded)
- `knowledge-base/best-practices/chaos-engineering.md` — BP-005

### Wave 3b — Integration + Security (5 docs)
- `knowledge-base/patterns/integration/saga-orchestration.md` — INT-001 (upgraded)
- `knowledge-base/patterns/integration/cdc-outbox-pattern.md` — INT-002 (upgraded)
- `knowledge-base/patterns/data/cqrs-pattern.md` — DATA-001 (upgraded)
- `knowledge-base/patterns/security/tokenization-hsm.md` — SEC-004
- `knowledge-base/patterns/security/bff-token-binding.md` — SEC-005

### Wave 3c — Reference Architectures + Data Residency (4 docs)
- `knowledge-base/reference-architectures/real-time-payments-napas.md` — REF-002
- `knowledge-base/reference-architectures/kyc-aml-onboarding.md` — REF-003
- `knowledge-base/reference-architectures/card-authorization-3ds2.md` — REF-004
- `knowledge-base/principles/data-residency.md` — PRIN-007

## Reviewers per wave

| Wave | Required reviewers |
|---|---|
| 3a | @sre-lead, @ea-board, @tech-lead-backend |
| 3b | @ciso-delegate, @ea-board, @tech-lead-backend, @tech-lead-web, @tech-lead-mobile |
| 3c | @payments-domain-owner, @risk-management-domain-owner, @ciso-delegate, @head-of-compliance, @ea-board, @sre-lead |

## Reject criteria (verbatim from Spec §5)

- Missing required sections (Mermaid, Java/Spring sample, NFR-AC, 3-ring Compliance, Cost/FinOps, Threat Model, Runbook stub, Test Strategy)
- Compliance refs wrong section
- Code samples diverge from Techcombank conventions (Spring 3.x; Resilience4j 2.x; React 18 + TS 5; Swift 5.x; Kotlin)
- Cross-references to spine docs broken or contradictory

## Outstanding items

1. Vietnamese-source citations (SBV §IV / §III; Decree 13 / 53; BCBS 230 §27) all flagged "(UNOFFICIAL)" — replace with Legal-team translations before G5.
2. T24 / NAPAS / SWIFT / PCI-DSS code references constrained to publicly documented behaviour per Spec §8.
3. Wave 3c reference architectures cite Wave 3a/3b patterns; cross-reference link integrity will be verified in Phase 4 (markdown-link-check).
4. The 4 upgraded existing files (RES-002, INT-001, INT-002, DATA-001) preserve original content + add catalog-required sections; reviewers should focus on the new sections.

## Action items if approved

1. Proceed to Phase 4 (cross-link existing 22 docs; lint; ADR-004; G5 final).
2. Engage Legal team to backfill UNOFFICIAL citations before G5.

## Reviewer sign-off

### Wave 3a
- [ ] @sre-lead
- [ ] @ea-board
- [ ] @tech-lead-backend

### Wave 3b
- [ ] @ciso-delegate
- [ ] @ea-board
- [ ] @tech-lead-backend
- [ ] @tech-lead-web
- [ ] @tech-lead-mobile

### Wave 3c
- [ ] @payments-domain-owner
- [ ] @risk-management-domain-owner
- [ ] @ciso-delegate
- [ ] @head-of-compliance
- [ ] @ea-board
- [ ] @sre-lead

## Reviewer feedback

(empty)
