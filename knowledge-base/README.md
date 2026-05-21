# Techcombank Architecture Knowledge Base

This repository contains reusable architecture principles, patterns, and best practices that serve as the foundation for all Design Architecture Board (DAB) submissions and technical decisions.

## Structure

- **[Principles](./principles/README.md)** — Core architectural beliefs and decision frameworks
- **[Patterns](./patterns/)** — Proven solutions to recurring architecture problems
  - [Integration Patterns](./patterns/integration/) — Cross-service communication, legacy bridges, sagas, outbox
  - [Security Patterns](./patterns/security/) — Authentication, authorization, secrets, tokenisation, BFF
  - [Data Patterns](./patterns/data/) — Data modeling, CQRS, data mesh, lineage, CDC
  - [Resilience Patterns](./patterns/resilience/) — Fault tolerance, cell-based architecture, circuit breakers, throttling
  - [EIP Patterns](./patterns/eip/) — Banking-relevant Hohpe/Woolf Enterprise Integration Patterns subset (25 of 65)
  - [Frontend Patterns](./patterns/frontend/) — Web (React+TS) performance budgets, offline-first, CSP, error boundary
  - [Mobile Patterns](./patterns/mobile/) — Native iOS/Android offline queue, secure storage, biometric auth, deep-link attestation
  - [Banking Solutions Patterns](./patterns/banking-solutions/) — Atomic banking patterns: double-entry ledger, idempotent payment key, sanctions screening
- **[Reference Architectures](./reference-architectures/)** — End-to-end designs (multi-region active-active, real-time payments NAPAS, KYC/AML, card auth 3DS2)
- **[NFR](./nfr/)** — Non-functional requirement catalogues (service tiering RTO/RPO, latency budgets, error budgets)
- **[Compliance](./compliance/)** — Master regulatory mapping matrix + per-regulation deep dives (SBV / Decree 13 / Decree 53 / PCI-DSS / Basel / SWIFT / ISO 20022)
- **[Templates](./templates/)** — Reusable doc skeletons (NFR Acceptance Criteria DAB template, pattern doc, stub doc)
- **[Best Practices](./best-practices/)** — Operational and process guidelines (CI/CD, DR, observability, chaos engineering, SRE golden signals, error budgets)

> **Master catalog**: every artefact above is indexed in [`governance/standards/enterprise-architecture-catalog.md`](../governance/standards/enterprise-architecture-catalog.md). Cite by Catalog ID (e.g., `RES-005`, `EIP-024`) in your DAB submissions.

## How to Reference

In your DAB submissions, reference knowledge-base content using relative links:

```markdown
# Service Architecture

This service implements the [API-First Design](./principles/api-first-design.md) principle and follows the [SAGA Orchestration Pattern](./patterns/integration/saga-orchestration.md) for distributed transactions.
```

## Contributing New Patterns/Principles

1. **Validate** the pattern/principle through implementation in 2+ projects
2. **Document** using the standard template (see any file in this knowledge base)
3. **Review** with the Enterprise Architecture Board
4. **Announce** in architecture guild meetings
5. **Version** using the status badge (Status, Last Reviewed, Owner)

### Template Structure

All patterns and principles follow this structure:

```markdown
# [Name]

Status: [Draft | Proposed | Approved | Deprecated] | Last Reviewed: YYYY-MM-DD | Owner: @team

## Problem Statement
What challenge does this address?

## Solution
How do we solve it? Include diagrams where helpful.

## Implementation Guidelines
Concrete steps to apply this pattern/principle.

## When to Use / When NOT to Use
Scope and boundaries.

## References
Links to implementations, external resources, tools.
```

## Governance

- **Status Legend**:
  - **Draft**: Under development, not yet validated
  - **Proposed**: Ready for EA board review
  - **Approved**: Endorsed by EA board, use in production designs
  - **Deprecated**: Replaced by newer pattern/principle

- **Review Cadence**: Annual review for all approved patterns; quarterly for draft/proposed
- **Changes**: Require EA board approval and update to the status badge

## Quick Links — Spine docs (Wave 0)

- [NFR-001 Service Tiering + RTO/RPO Matrix](./nfr/service-tiering-rto-rpo.md)
- [NFR-002 Latency Budget Model](./nfr/latency-budget-model.md)
- [PRIN-006 Idempotency-by-default](./principles/idempotency-by-default.md)
- [TPL-001 NFR Acceptance Criteria DAB Template](./templates/nfr-acceptance-criteria-dab.md)
- [COMP-001 Compliance Mapping Matrix](./compliance/compliance-mapping-matrix.md)
- [REF-001 Multi-Region Active-Active](./reference-architectures/multi-region-active-active.md)

## Quick Links — Banking Reference Architectures (Wave 0)

- [REF-002 Real-Time Payments NAPAS](./reference-architectures/real-time-payments-napas.md)
- [REF-003 KYC / AML Onboarding](./reference-architectures/kyc-aml-onboarding.md)
- [REF-004 Card Authorization 3DS2](./reference-architectures/card-authorization-3ds2.md)

## Quick Links — Existing Principles

- [PRIN-001 API-First Design](./principles/api-first-design.md)
- [PRIN-002 Event-Driven Architecture](./principles/event-driven-architecture.md)
- [PRIN-003 Zero-Trust Security](./principles/zero-trust-security.md)
- [PRIN-004 Database-Per-Service](./principles/database-per-service.md)
- [PRIN-005 Cloud-Native-First](./principles/cloud-native-first.md)
- [PRIN-007 Data Residency](./principles/data-residency.md)

---

**Last Updated**: 2026-05-09 | **Maintained By**: Enterprise Architecture Board
