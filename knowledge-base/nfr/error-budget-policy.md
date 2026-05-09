# Error Budget Policy

Status: Proposed | Target Wave: 1 | Owner: @sre-lead
Catalog ID: NFR-005
Tier Applicability: T0, T1, T2

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

A 99.99% T0 SLO grants ~52 minutes of "allowable downtime" per year — the *error budget*. Spending this without governance leads to perpetual outages; under-spending leads to over-engineering. An Error Budget Policy makes the trade-off explicit: when the budget is healthy, teams can ship faster; when budget is exhausted, feature work pauses in favour of reliability work.

## Sketch of Solution

- Per-tier error-budget formula: `(1 - target_availability) × period`
- Budget tracked as a leading indicator (rolling-30-day burn rate)
- Alerts at burn rates 1× / 6× / 14× (Google SRE multi-window multi-burn-rate alerting)
- Policy bands:
  - **Green** (>50% budget remaining): full feature velocity
  - **Amber** (10–50%): feature freeze on the affected service; reliability bug-bash
  - **Red** (<10% or exhausted): all non-reliability merges blocked until recovery
- Decoupled per service tier; T0 services have stricter response than T2

## Compliance Hooks

- Ring 0: Google SRE Book Chapter 4 (SLO / Error Budgets)
- Ring 1: BCBS 230 (operational resilience — explicit reliability commitments, ⚠️ working summary); BCBS 239 §3 (timeliness)
- Ring 2: SBV Circular 09/2020 §IV.2 ⚠️ (working summary — pending Legal review)

## NFR Hooks

- HA: forces reliability investment proportional to uptime SLO
- HP: error budget includes latency-SLO breaches, not just availability
- HR: provides governance lever to prioritise reliability over features

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid diagram of burn-rate states (green/amber/red)
- [ ] Prometheus alert rules for multi-burn-rate
- [ ] Sample policy text for Tier T0 (full freeze) and T1 (partial)
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps (alerting infra; cultural cost of feature freeze)
- [ ] Threat Model (gaming the metric; SLI definition matters)
- [ ] Operational Runbook (red-state procedure)
- [ ] Test Strategy

## References

- Google SRE Book Chapter 4 (Service Level Objectives)
- Google SRE Workbook Chapter 5 (Alerting on SLOs)
- Catalog: NFR-001 Service Tiering; NFR-002 Latency Budget; BP-007 Golden Signals; BP-008 Error Budgets (best-practice complement)
