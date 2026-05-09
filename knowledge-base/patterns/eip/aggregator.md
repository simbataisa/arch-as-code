# Aggregator

Status: Proposed | Target Wave: 1 | Owner: @tech-lead-backend
Catalog ID: EIP-011
Tier Applicability: T0, T1

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

A single business event often arrives as multiple correlated messages (e.g., one card-authorisation results in a separate fraud-screening event, balance event, posting event). Downstream consumers need the *combined* outcome, not each message in isolation. The Aggregator collects related messages (correlated by a key) and emits a single composite once a completion condition is met (count, timeout, or business condition).

## Sketch of Solution

- Spring Integration `Aggregator` or Kafka Streams `windowedBy + reduce`
- Correlation key = business key (`payment_id`, `case_id`)
- Completion strategy: count-based (e.g., 3-of-3 events), time-based (window of 5s), or condition-based
- Persistent aggregate state (Kafka Streams state store, or an external store) survives restarts
- Pair with [EIP-013 Resequencer](resequencer.md) when message order matters
- Emit "incomplete" event on timeout (signals partial completion for compensating actions)

## Compliance Hooks

- Ring 0: EIP §7 Message Routing — Aggregator
- Ring 1: BCBS 239 §6 (no message loss; combined-event accuracy)
- Ring 2: SBV Circular 09/2020 §IV.2 ⚠️ (working summary — pending Legal review)

## NFR Hooks

- HA: aggregate state replicated across regions (Kafka Streams or external store)
- HP: aggregation overhead bounded by window size (typically < 5s for T0)
- HR: timeouts produce explicit "incomplete" events — no silent drops

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid sequence + window diagram
- [ ] Spring Integration sample
- [ ] Kafka Streams sample (windowed reduce + state store)
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps (state-store size; rocks-DB if Kafka Streams)
- [ ] Threat Model (incomplete-event handling; timeout abuse)
- [ ] Operational Runbook (window stuck; incomplete-rate alerts)
- [ ] Test Strategy

## References

- Hohpe & Woolf — EIP Chapter 7 (Aggregator)
- Kafka Streams Windowing
- Catalog: EIP-013 Resequencer; EIP-017 Process Manager; INT-001 Saga
