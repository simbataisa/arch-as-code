# Observability-First

Status: Proposed | Target Wave: 1 | Owner: @sre-lead
Catalog ID: PRIN-009
Tier Applicability: T0, T1, T2, T3

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

A service that cannot be observed cannot be operated, debugged, or scaled. Banking platforms generate audit obligations, regulatory reporting needs (BCBS 239 timeliness), and customer-impact incidents — all requiring continuous, structured observability from day one. Adding observability after the fact produces patchy coverage and high MTTR.

## Sketch of Solution

- Adopt OpenTelemetry as the single observability standard for traces, metrics, logs
- Every service emits the four golden signals (latency, traffic, errors, saturation) — see [BP-007](../best-practices/golden-signals-sre.md)
- Distributed tracing on every cross-service call — `traceparent` propagated end-to-end including T24 OFS bridge
- Structured logs only (JSON) with mandatory fields: `trace_id`, `span_id`, `tenant`, `user_id_hash`, `tier`
- Metrics dashboards templated per tier (Grafana folder per service tier)

## Compliance Hooks

- Ring 0: OpenTelemetry standards; Google SRE Book Chapter 6
- Ring 1: BCBS 239 §3 (timeliness); §11 (consistency of reporting)
- Ring 2: SBV Circular 09/2020 §IV.3 incident logging (UNOFFICIAL TRANSLATION pending Legal)

## NFR Hooks

- HA: observability survives regional failover (telemetry to a separate region)
- HP: instrumentation overhead ≤ 1% CPU; tail-sampling for traces at high RPS
- HR: incident detection ≤ 60s for T0; MTTR reduction is the primary KPI

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid diagram of pipeline (app → collector → backend)
- [ ] Spring Boot OpenTelemetry config snippet
- [ ] Mobile (iOS Swift os.signpost / Android Trace) snippets
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps notes (telemetry is a real budget item — sample at high tiers)
- [ ] Threat Model (PII in logs)
- [ ] Operational Runbook stub
- [ ] Test Strategy

## References

- Google SRE Book Chapter 6 (Monitoring Distributed Systems)
- OpenTelemetry specification
- Catalog: BP-004 Observability Standards; BP-007 Golden Signals
