# Defense-in-Depth

Status: Proposed | Target Wave: 1 | Owner: @ciso-delegate
Catalog ID: PRIN-008
Tier Applicability: T0, T1, T2, T3

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

A single perimeter control (a WAF, a firewall, an auth proxy) is a single point of compromise. Banking platforms must assume any one layer can be breached and still maintain the security posture. Defense-in-Depth requires multiple independent control layers — network, identity, application, data — each fail-secure on its own and assuming the others are not intact. This is the operational foundation under [PRIN-003 Zero-Trust](zero-trust-security.md).

## Sketch of Solution

- Stack independent control layers: network segmentation → mTLS service mesh ([SEC-001](../patterns/security/mtls-service-mesh.md)) → OAuth2/OIDC ([SEC-002](../patterns/security/oauth2-authorization.md)) → ABAC policies (SEC-010) → field-level tokenisation ([SEC-004](../patterns/security/tokenization-hsm.md), SEC-013) → tamper-evident audit (SEC-012)
- Each layer fails-secure (deny by default) when degraded
- No layer assumes another is intact — service-to-service still authenticates even behind a mesh
- Trust boundaries explicit; every cross-boundary call enforces full auth

## Compliance Hooks

- Ring 0: NIST SP 800-53 (multi-layer defence); OWASP ASVS V1
- Ring 1: PCI-DSS 4.0 multi-layer requirements (§1, §6, §7, §8); BCBS 239 §6
- Ring 2: SBV Circular 09/2020 §II security organisation ⚠️ (working summary — pending Legal review)

## NFR Hooks

- HA: each layer independently HA; one-layer failure must not topple the rest
- HP: ~5ms additional P95 per layer; budget composes per [NFR-002](../nfr/latency-budget-model.md)
- HR: improves recoverability — no single-layer compromise yields full control

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid diagram of layer stack with trust boundaries
- [ ] Java/Spring code sample for each layer's enforcement
- [ ] Compliance Mapping table (3 rings)
- [ ] NFR Acceptance Criteria with concrete numbers
- [ ] Cost/FinOps notes
- [ ] Threat Model (STRIDE per layer)
- [ ] Operational Runbook stub
- [ ] Test Strategy (chaos: disable each layer in turn)
- [ ] EA-Board + CISO review

## References

- NIST SP 800-160 (Systems Security Engineering)
- OWASP Application Security Verification Standard
- Catalog: PRIN-003 Zero-Trust; SEC-001 mTLS; SEC-002 OAuth2; SEC-010 ABAC
