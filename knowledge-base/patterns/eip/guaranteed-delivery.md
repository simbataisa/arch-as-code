# Guaranteed Delivery

Status: Proposed | Target Wave: 1 | Owner: @tech-lead-backend
Catalog ID: EIP-023
Tier Applicability: T0, T1

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

Banking flows cannot lose messages. A payment event missed during a broker hiccup is a real, unrecoverable loss — manifesting as a ledger inconsistency or a duplicate-prevented-but-actually-lost transaction. Guaranteed Delivery requires the messaging infrastructure to persist messages durably before acknowledging the producer, and to keep them until the consumer confirms processing.

## Sketch of Solution

- Producer ACK only after broker has persisted to disk (Kafka `acks=all` + replication factor ≥ 3 across AZs)
- Consumer commits offset only after side-effects are durably persisted (offset-after-commit pattern, or transactional outbox INT-002)
- Pair with [EIP-024 Idempotent Receiver](idempotent-receiver.md) so retried delivery is safe
- Pair with [EIP-025 Dead Letter Channel](dead-letter-channel.md) for un-processable messages
- Cross-region replication (Kafka MirrorMaker 2) for [REF-001 Multi-Region](../../reference-architectures/multi-region-active-active.md) durability

## Compliance Hooks

- Ring 0: EIP §3 Messaging Channels — Guaranteed Delivery
- Ring 1: BCBS 239 §6 (no message loss); ISO 20022 (settlement message integrity)
- Ring 2: SBV Circular 09/2020 §IV.2 (UNOFFICIAL TRANSLATION pending Legal)

## NFR Hooks

- HA: cross-region replication; broker survives single-AZ loss
- HP: `acks=all` adds 1–3ms ack latency vs `acks=1` — accepted within T0 budget
- HR: explicit message persistence and replay-on-failure semantics

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid diagram of producer/broker/consumer ack flow
- [ ] Spring Kafka configuration (acks=all, replication=3, enable.idempotence=true)
- [ ] Outbox-pattern integration (link to INT-002)
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps (replication storage; cross-region egress)
- [ ] Threat Model (message tampering — pair with mTLS SEC-001)
- [ ] Operational Runbook (broker failure; replication lag)
- [ ] Test Strategy (kill broker, verify no message loss)

## References

- Hohpe & Woolf — EIP Chapter 3 (Guaranteed Delivery)
- Kafka durability guarantees documentation
- Catalog: EIP-024 Idempotent Receiver; EIP-025 DLC; INT-002 Outbox+CDC; REF-001 Multi-Region
