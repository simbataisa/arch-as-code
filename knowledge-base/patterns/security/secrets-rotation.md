# Secrets Rotation

Status: Proposed | Target Wave: 1 | Owner: @ciso-delegate
Catalog ID: SEC-007
Tier Applicability: T0, T1, T2

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

Static, long-lived secrets (DB passwords, API keys, signing keys) accumulate operational entropy: copied into wikis, leaked into logs, embedded in git history, re-used across environments. Banking compliance requires demonstrable rotation; engineering reality demands it not break services. Automated rotation reduces compromise blast radius and is a hard PCI-DSS requirement on cryptographic keys.

## Sketch of Solution

- HashiCorp Vault (or AWS Secrets Manager) as single source of truth — see [SEC-003](vault-secret-management.md)
- Dynamic credentials where supported (e.g., Vault DB engine generates short-lived DB passwords)
- Static-secret rotation via push-based "rotation manager" + dual-secret window (old + new both valid for transition)
- Application-level: ephemeral cached credentials with TTL; refresh on TTL expiry
- HSM-backed keys (SEC-004) rotated per PCI-DSS §3.6 schedule

## Compliance Hooks

- Ring 0: NIST SP 800-57 Part 1 (Key Management Lifecycle)
- Ring 1: PCI-DSS 4.0 §3.6 (key lifecycle); §3.7 (key management procedures)
- Ring 2: SBV Circular 09/2020 §III cryptographic controls (UNOFFICIAL TRANSLATION pending Legal)

## NFR Hooks

- HA: dual-secret window prevents rotation-driven outages
- HP: cached secrets at app start; rotation only on TTL expiry — no per-request hit
- HR: bounds blast radius of any leaked secret to its TTL

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid diagram of rotation lifecycle (issue → grace → revoke)
- [ ] Spring sample integrating with Vault dynamic credentials
- [ ] HSM key-rotation procedure (paired with SEC-004)
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps (Vault Enterprise vs OSS; rotation tooling)
- [ ] Threat Model (rotation-induced outage; rollback on bad rotation)
- [ ] Operational Runbook (rotation playbook)
- [ ] Test Strategy

## References

- NIST SP 800-57 Part 1 — Key Management Lifecycle
- HashiCorp Vault dynamic credentials
- Catalog: SEC-003 Vault; SEC-004 Tokenization + HSM
