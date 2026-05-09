# Least-Privilege

Status: Proposed | Target Wave: 1 | Owner: @ciso-delegate
Catalog ID: PRIN-011
Tier Applicability: T0, T1, T2, T3

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

Over-granted permissions are the most common attack-surface enabler in banking platforms. A service or human with broader-than-needed access magnifies the blast radius of any compromise. Least-privilege requires that every principal — human, service, batch job, build agent — has the minimum permissions to perform its function and nothing more.

## Sketch of Solution

- Default-deny IAM; permissions granted explicitly per role
- Service-to-service auth with scoped OAuth2 tokens (SEC-002) or workload identity (SPIFFE / Pod Identity)
- Just-in-time elevation for human admins (e.g., AWS Identity Center session tags + approval workflow)
- Quarterly access review with auto-revocation of unused entitlements
- ABAC over RBAC where context (region, tenant, time-of-day) matters — see SEC-010

## Compliance Hooks

- Ring 0: NIST SP 800-53 AC-6 (Least Privilege)
- Ring 1: PCI-DSS 4.0 §7 (Restrict access by need-to-know); SOC 2 CC6.1
- Ring 2: SBV Circular 09/2020 §II / §III ⚠️ (working summary — pending Legal review)

## NFR Hooks

- HA: not directly applicable
- HP: not directly applicable
- HR: improves recoverability — compromised account has bounded effect

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid diagram of identity types (human / service / batch) and their scopes
- [ ] AWS IAM / Spring Security examples
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria (where applicable)
- [ ] Cost/FinOps (overhead of access review process)
- [ ] Threat Model (insider threat, lateral movement)
- [ ] Operational Runbook stub
- [ ] Test Strategy

## References

- NIST SP 800-53 AC-6
- AWS IAM Best Practices
- Catalog: PRIN-003 Zero-Trust; SEC-002 OAuth2; SEC-010 ABAC; SEC-011 Session Revocation
