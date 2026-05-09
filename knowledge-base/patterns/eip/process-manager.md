# Process Manager

Status: Proposed | Target Wave: 1 | Owner: @tech-lead-backend
Catalog ID: EIP-017
Tier Applicability: T0, T1

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

Multi-step business processes (e.g., loan origination — credit check → risk score → underwriting → KYC → fund release) need explicit orchestration: sequencing, parallelism, error handling, compensation. Without a Process Manager pattern, this logic spreads across services and is impossible to debug, audit, or evolve. The Process Manager pattern is the orchestrator-side dual of [EIP-016 Routing Slip](routing-slip.md) and the conceptual sibling of [INT-001 Saga Orchestration](../integration/saga-orchestration.md).

## Sketch of Solution

- A central, durable workflow engine (Camunda, Temporal, AWS Step Functions, or Spring State Machine)
- Process definition declarative: states, transitions, guards, compensations
- State persisted at every step (durability survives crashes); replay-safe (idempotent steps)
- Process instance correlated to business key (`loan_application_id`)
- Observability — every state transition logged with `process_id`, duration, outcome
- Distinct from Saga (INT-001): Saga is a banking-specific compensable transaction; Process Manager is a general workflow

## Compliance Hooks

- Ring 0: EIP §7 Message Routing — Process Manager
- Ring 1: BCBS 239 §6 (auditable risk-data flow); ISO 20022 process-flow representations
- Ring 2: SBV Circular 09/2020 §IV.2 ⚠️ (working summary — pending Legal review)

## NFR Hooks

- HA: workflow engine itself is HA (e.g., Temporal cluster, multi-region)
- HP: per-step latency bounded by service-level budgets; total process time may be hours
- HR: durable state — restart-safe; compensations explicit

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid state diagram of an example process
- [ ] Temporal / Camunda Java sample
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps (workflow-engine licensing/hosting)
- [ ] Threat Model (process tampering; state-store integrity)
- [ ] Operational Runbook (stuck-process detection; manual intervention SOP)
- [ ] Test Strategy

## References

- Hohpe & Woolf — EIP Chapter 7 (Process Manager)
- Temporal documentation
- Camunda BPMN documentation
- Catalog: INT-001 Saga; EIP-016 Routing Slip; EIP-024 Idempotent Receiver
