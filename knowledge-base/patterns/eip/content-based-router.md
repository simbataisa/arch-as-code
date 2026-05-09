# Content-Based Router

Status: Proposed | Target Wave: 1 | Owner: @tech-lead-backend
Catalog ID: EIP-005
Tier Applicability: T0, T1

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

Different message types or different message contents need different processing. Coupling all consumers to all messages forces every consumer to filter — wasting CPU and risking accidental processing of the wrong type. A Content-Based Router inspects the message and routes it to a specific channel based on payload content. In banking: payment messages routed by `currency`, `country`, `amount-tier`, or `messageType` (ISO 20022 pacs.008 vs pacs.007).

## Sketch of Solution

- Spring Integration `@Router` annotation, or Kafka Streams branch, or in-broker routing (RabbitMQ topic exchange)
- Routing rules versioned and observable (route hits logged for traceability)
- Default route for unknown content (avoid silent drops)
- Pair with [EIP-008 Content Filter](content-filter.md) when only some content is relevant
- Use sparingly — too much routing logic in one place becomes a spaghetti broker

## Compliance Hooks

- Ring 0: EIP §4 Message Routing
- Ring 1: ISO 20022 message-type-driven routing; BCBS 239 (consistent routing prevents data drops)
- Ring 2: SBV Circular 09/2020 §IV.2 (UNOFFICIAL TRANSLATION pending Legal)

## NFR Hooks

- HA: stateless routing — easily replicated across regions
- HP: typically < 1ms per route decision (in-process); broker-side may be slightly higher
- HR: explicit "unknown" route prevents silent loss

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid diagram of router with multiple downstream branches
- [ ] Spring Integration sample
- [ ] Kafka Streams branching sample
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps (CPU cost of complex predicates)
- [ ] Threat Model (route-table tampering; default-route abuse)
- [ ] Operational Runbook (route-mismatch alerts)
- [ ] Test Strategy

## References

- Hohpe & Woolf — EIP Chapter 7 (Content-Based Router)
- Spring Integration Routers documentation
- Catalog: EIP-001 Message Channel; EIP-008 Content Filter; INT-001 Saga Orchestration
