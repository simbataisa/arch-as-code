# Throttling / Rate Limiting

Status: Proposed | Target Wave: 1 | Owner: @sre-lead
Catalog ID: RES-008
Tier Applicability: T0, T1, T2

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

Without explicit throttling, a misbehaving client (or a Tet-eve traffic spike) can saturate any service. Banking platforms must protect both themselves and their downstreams: a sudden 10× spike on an internet-banking flow can cascade into NAPAS via fraud-screening if there's no per-tenant or per-tier throttle at the gateway.

## Sketch of Solution

- Two-tier throttling: edge (per-IP, per-API-key) at the API Gateway, and intra-cluster (per-tenant, per-customer) at the service
- Token-bucket or leaky-bucket algorithms; Resilience4j RateLimiter for in-process; Spring Cloud Gateway RequestRateLimiter for edge
- Distinguish 429 (rate-limited, retry-after) from 503 (overloaded, back off harder)
- Per-tier defaults: T0 generous (own customer flow), T1/T2 stricter
- Combined with load shedding (RES-009) when even rate-limiting can't keep up

## Compliance Hooks

- Ring 0: Microsoft Cloud "Throttling" + "Rate Limiting" patterns
- Ring 1: PSD2 §SCA backoff requirements; FAPI rate-limit guidance (where applicable)
- Ring 2: SBV Circular 09/2020 §IV.2 (UNOFFICIAL TRANSLATION pending Legal)

## NFR Hooks

- HA: protects service from saturation; preserves capacity for legitimate traffic
- HP: rate limiter adds < 1ms P95 (in-memory token bucket)
- HR: bounds blast radius from misbehaving callers; combines with cell-based (RES-005) to isolate

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid diagram of edge + service throttling layers
- [ ] Java sample with Resilience4j @RateLimiter
- [ ] Spring Cloud Gateway YAML config
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps (Redis-backed counters at high RPS)
- [ ] Threat Model (defends against credential stuffing, scraping, DDoS L7)
- [ ] Operational Runbook stub (alerts on 429 spike rate)
- [ ] Test Strategy

## References

- Resilience4j RateLimiter
- Microsoft Cloud Patterns: Throttling, Rate Limiting
- Catalog: RES-002 Circuit Breaker; RES-009 Load Shedding; INT-003 API Gateway
