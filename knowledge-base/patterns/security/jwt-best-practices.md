# JWT Best Practices

Status: Proposed | Target Wave: 1 | Owner: @ciso-delegate
Catalog ID: SEC-006
Tier Applicability: T0, T1, T2

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

JWTs are easy to use wrong: long-lived access tokens, weak signing algorithms, missing audience checks, leaked refresh tokens, no revocation. In a banking context, a leaked or forged JWT is direct access to customer money. This pattern codifies the safe defaults that every Techcombank service applies — paired with [SEC-005 BFF + Token-Binding](bff-token-binding.md).

## Sketch of Solution

- Short-lived access tokens (5 min); long-lived refresh tokens stored only server-side or in BFF cookies
- Reject `alg=none`; pin to RS256 / ES256
- Mandatory `iss`, `aud`, `exp`, `nbf`, `jti` validation; reject if missing
- Key rotation via JWKS endpoint with grace window
- Token revocation via JTI denylist (Redis) on logout / suspicious activity
- No PII in claims beyond user-id-hash and roles

## Compliance Hooks

- Ring 0: RFC 8725 (JWT BCP); OWASP ASVS V3 / V4
- Ring 1: PCI-DSS 4.0 §8 Authentication; FAPI 2.0 token guidance
- Ring 2: SBV Circular 09/2020 §III multi-factor ⚠️ (working summary — pending Legal review)

## NFR Hooks

- HA: stateless verification (no session DB lookup) survives any backend outage
- HP: token verification < 1ms P95 (cached JWKS)
- HR: short TTL bounds compromise window; revocation list bounds further

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid diagram of issue / verify / refresh / revoke flow
- [ ] Spring Security Resource Server config
- [ ] iOS / Android refresh-token storage best practices (Keychain / Keystore)
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps (JWKS hosting, denylist Redis)
- [ ] Threat Model (alg-confusion, replay, token leakage)
- [ ] Operational Runbook stub (key rotation procedure)
- [ ] Test Strategy

## References

- RFC 8725 (JSON Web Token Best Current Practices)
- OWASP ASVS V3 (Session Management) and V4 (Access Control)
- Catalog: SEC-002 OAuth2; SEC-005 BFF + Token-Binding; SEC-011 Session Revocation
