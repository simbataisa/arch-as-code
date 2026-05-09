# Data Masking

Status: Proposed | Target Wave: 1 | Owner: @ciso-delegate
Catalog ID: SEC-008
Tier Applicability: T0, T1, T2

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

Display, log, and lower-environment usage of customer data must not expose PII or PCI data unnecessarily. Data masking renders sensitive fields into a non-reversible form for these contexts (e.g., showing `**** **** **** 1234` for a card PAN, or `dat***@gmail.com` for email). Distinct from tokenisation (SEC-004 / SEC-013) which preserves reversibility for authorised callers.

## Sketch of Solution

- Field-level masking rules in `data-classification.md` map each field type to its mask pattern
- API-layer masking via Jackson serialiser (Spring Boot) — fields annotated `@Masked` rendered with their mask
- Database-side masking via PostgreSQL views or RLS for analytics access; raw columns gated by IAM role
- Lower environments (dev, test): masked dataset only; never copy production raw
- Logs scrubbed at structured-log emission — `LogMasker` filter applied before serialisation

## Compliance Hooks

- Ring 0: OWASP ASVS V8 (Data Protection)
- Ring 1: PCI-DSS 4.0 §3.4 (PAN display masking)
- Ring 2: Decree 13/2023 personal-data minimisation (UNOFFICIAL TRANSLATION pending Legal); SBV Circular 09 (UNOFFICIAL)

## NFR Hooks

- HA: not directly applicable
- HP: ~1 µs per field at serialisation; negligible
- HR: limits exposure during incidents (logs already masked)

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid diagram of mask-application points (DB → service → API → UI; logs)
- [ ] Spring Boot Jackson @Masked sample
- [ ] React UI utility for display masking
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps (negligible runtime; some test-data engineering)
- [ ] Threat Model (insider read; analytics access)
- [ ] Operational Runbook
- [ ] Test Strategy (assert masks present in API responses, logs)

## References

- PCI-DSS 4.0 §3.4
- OWASP ASVS V8
- Catalog: SEC-004 Tokenization + HSM; SEC-013 PII Tokenization (FPE); `governance/standards/data-classification.md`
