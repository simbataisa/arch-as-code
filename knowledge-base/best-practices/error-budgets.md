# Error Budgets

Status: Proposed | Target Wave: 1 | Owner: @sre-lead
Catalog ID: BP-008
Tier Applicability: T0, T1

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

The complement of [NFR-005 Error Budget Policy](../nfr/error-budget-policy.md) — the *practice* side. NFR-005 declares the policy (formula, burn-rate alerts, response bands); BP-008 documents the operational practices: how teams define SLIs, how they track budget consumption, how they conduct the budget review, and what an "exhausted budget" sprint looks like in day-to-day work.

## Sketch of Solution

- SLI definition workshop on every new service: define what "good" looks like (success ratio, latency-bound success ratio)
- Monthly SLO/SLI review per T0 service; quarterly per T1
- Burn-rate dashboards visible to the team — nudge before alert
- Budget exhaustion → reliability sprint — playbook (top 3 risks, first 5 actions)
- Post-incident: reconcile actual outage minutes against budget; update SLI if the metric is wrong

## Compliance Hooks

- Ring 0: Google SRE Book and SRE Workbook
- Ring 1: BCBS 230 (operational resilience accountability, ⚠️ working summary)
- Ring 2: SBV Circular 09/2020 §IV.2 ⚠️ (working summary — pending Legal review)

## NFR Hooks

- HA: forces explicit reliability commitments and budget-driven trade-offs
- HP: latency-based SLOs included in budget calculation
- HR: ensures reliability work is funded by policy rather than heroics

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid burn-rate visualisation
- [ ] SLI/SLO definition template
- [ ] Reliability-sprint playbook
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps (cultural cost of feature freezes; SRE engagement)
- [ ] Threat Model (gaming the metric — choose SLIs the customer experiences)
- [ ] Operational Runbook
- [ ] Test Strategy (audit: SLI matches a real customer-impact metric)

## References

- Google SRE Workbook Chapters 4 (SLO Engineering) and 5 (Alerting)
- Catalog: NFR-005 Error Budget Policy; PRIN-009 Observability-First; BP-007 Golden Signals
