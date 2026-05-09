# Timeout Budget

Status: Proposed | Target Wave: 1 | Owner: @sre-lead
Catalog ID: RES-006
Tier Applicability: T0, T1, T2

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

Without explicit per-call timeouts, slow downstreams silently exhaust upstream thread pools and connection budgets. The result is latent cascading failure: things "work" until a downstream is briefly slow, at which point the whole stack hangs. Timeouts must be smaller than the upstream caller's deadline (the classic deadline-propagation rule), and the sum of timeouts on a request path must fit within the calling service's latency budget ([NFR-002](../../nfr/latency-budget-model.md)).

## Sketch of Solution

- Every external (out-of-process) call has an explicit timeout, declared in code
- Timeouts respect deadline-propagation: callee's timeout < caller's remaining budget
- Spring + Resilience4j `@TimeLimiter` for synchronous; `Mono.timeout(...)` for reactive
- gRPC deadlines / OkHttp call timeouts on inter-service calls; not just connect/read separately
- Combined with circuit breaker (RES-002) and retry (RES-003) — timeout sets the per-attempt cap

## Compliance Hooks

- Ring 0: Resilience4j `TimeLimiter` module; AWS Well-Architected Reliability
- Ring 1: BCBS 230 Principle 6 (Incident Management) ⚠️ (working summary — preventing cascading failure)
- Ring 2: SBV Circular 09/2020 §IV.2 ⚠️ (working summary — pending Legal review)

## NFR Hooks

- HA: prevents thread-pool exhaustion (a primary cause of cascading outages)
- HP: caps latency contribution per dependency; deadline-propagation forces honesty
- HR: failures become fast and bounded

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid sequence diagram showing deadline propagation
- [ ] Java sample with @TimeLimiter + WebClient timeout
- [ ] YAML config (Resilience4j timeout per instance)
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps (negligible)
- [ ] Threat Model (timeout-too-aggressive risks; defends against slow-DoS)
- [ ] Operational Runbook stub (alerts on timeout-rate spikes)
- [ ] Test Strategy (chaos: inject latency on downstream; verify timeout fires)

## References

- Resilience4j TimeLimiter docs
- gRPC deadline propagation
- Catalog: RES-002 Circuit Breaker; RES-003 Retry; NFR-002 Latency Budget
