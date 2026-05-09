# PII Tokenization (Format-Preserving)

Status: Proposed | Target Wave: 1 | Owner: @ciso-delegate
Catalog ID: SEC-013
Tier Applicability: T0, T1

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

Some downstream systems (legacy T24, partners' fixed-format files, regulatory reports) require fields to keep their original shape (e.g., a 16-digit number, a national-ID-shaped string). Plain tokenisation breaks these. Format-preserving encryption (FPE, NIST SP 800-38G FF1/FF3-1) yields a ciphertext that has the same length and character class as the plaintext while remaining reversible only with HSM-held keys.

## Sketch of Solution

- FPE service backed by HSM key per data class (PAN, CCCD/national-ID, phone, email)
- Wrap [SEC-004 Tokenization + HSM](tokenization-hsm.md) — FPE is a variant
- Token vault stores the mapping; legacy systems only see tokens
- Detokenisation gated by ABAC (SEC-010): legitimate operational lookups only
- Distinct from masking (SEC-008) — FPE is reversible by authorised callers

## Compliance Hooks

- Ring 0: NIST SP 800-38G (FF1, FF3-1 mode specifications)
- Ring 1: PCI-DSS 4.0 §3 (PAN protection); GDPR Art. 32 (encryption)
- Ring 2: Decree 13/2023 personal-data protection (UNOFFICIAL TRANSLATION pending Legal)

## NFR Hooks

- HA: HSM cluster active-active; FPE service horizontally scalable
- HP: FPE adds ~3–5ms P95 vs plain encryption; budget per [NFR-002](../../nfr/latency-budget-model.md)
- HR: bounds breach blast radius — leaked storage yields tokens not plaintext

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid diagram of FPE flow (encrypt at boundary; decrypt in vault)
- [ ] Java sample using Voltage / AWS / Thales SDK
- [ ] T24 OFS bridge integration notes (FPE before write to T24)
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps (HSM cost; vs alternative: random tokens + indexed lookup)
- [ ] Threat Model (key compromise → bulk reversal; FPE-specific cryptanalysis)
- [ ] Operational Runbook (key rotation; vault recovery)
- [ ] Test Strategy

## References

- NIST SP 800-38G (Recommendation for Block Cipher Modes of Operation: Format-Preserving Encryption)
- Catalog: SEC-004 Tokenization + HSM; SEC-008 Data Masking; SEC-003 Vault
