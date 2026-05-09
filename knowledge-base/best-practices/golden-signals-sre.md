# Golden Signals (SRE)

Status: Proposed | Target Wave: 1 | Owner: @sre-lead
Catalog ID: BP-007
Tier Applicability: T0, T1, T2

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

A service can be observed in many dimensions; many teams over-instrument and miss what matters, or under-instrument and miss outages. The Google SRE "Four Golden Signals" — **Latency**, **Traffic**, **Errors**, **Saturation** — are the minimum viable observability set. Every Techcombank service must emit these four, with consistent labels, so dashboards and alerts can be templated per tier.

## Sketch of Solution

- Latency: P50 / P95 / P99 per endpoint and per dependency call
- Traffic: requests per second, partitioned by endpoint
- Errors: error rate, partitioned by endpoint and error class (4xx vs 5xx)
- Saturation: CPU, memory, connection-pool, DB-connection, queue-depth, GC pause time
- Standard label set across all metrics: `service`, `tier`, `region`, `cell`, `endpoint`, `version`
- Dashboards templated per tier in Grafana folders; alerts derived from [NFR-002 Latency Budget](../nfr/latency-budget-model.md) and [NFR-005 Error Budget](../nfr/error-budget-policy.md)

## Compliance Hooks

- Ring 0: Google SRE Book Chapter 6 (Monitoring)
- Ring 1: BCBS 239 §3 (timeliness — golden signals are the "is it timely" metric)
- Ring 2: SBV Circular 09/2020 §IV.3 incident logging ⚠️ (working summary — pending Legal review)

## NFR Hooks

- HA: signals enable rapid detection of HA degradation
- HP: latency signal directly enforces NFR-002 budgets
- HR: saturation is the leading indicator of impending overload

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid diagram of signal sources → collector → backend
- [ ] Spring Boot Micrometer config for the four signals
- [ ] Grafana dashboard template (JSON) per tier
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps (cardinality control; storage cost)
- [ ] Threat Model (PII in metric labels)
- [ ] Operational Runbook
- [ ] Test Strategy

## References

- Google SRE Book Chapter 6 (Monitoring Distributed Systems) — Four Golden Signals
- Catalog: PRIN-009 Observability-First; BP-004 Observability Standards; NFR-002 Latency Budget; NFR-005 Error Budget Policy
