# Message Channel

Status: Proposed | Target Wave: 1 | Owner: @tech-lead-backend
Catalog ID: EIP-001
Tier Applicability: T0, T1, T2

> **STUB** — full content authored in Wave 1.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

Application code that talks directly to other applications via shared databases or sockets becomes brittle: changes propagate, scaling is hard, failures cascade. Hohpe & Woolf's Message Channel is the foundational pattern: a logical conduit through which messages flow, decoupling sender from receiver in time and identity.

## Sketch of Solution

- Adopt a single messaging backbone — Kafka for high-throughput event streams; RabbitMQ / Solace for command-style queues
- Channels named after the *what* not the *who* (`payment-events`, `kyc-events`) — see also `governance/standards/naming-conventions.md`
- Topology: producer → channel (durable) → consumer; no direct producer↔consumer coupling
- Schema registry (Confluent / Apicurio) governs the contract on each channel
- Retention, partitioning, replication factor declared per channel based on tier (NFR-001)

## Compliance Hooks

- Ring 0: EIP §3 Messaging Channels (Hohpe & Woolf)
- Ring 1: BCBS 239 §6 (no message loss); ISO 20022 message envelope concepts
- Ring 2: SBV Circular 09/2020 §IV.2 ⚠️ (working summary — pending Legal review)

## NFR Hooks

- HA: durable channels survive consumer/producer outages
- HP: low-latency producers (Kafka < 5ms P95 ack on local cluster)
- HR: message persistence enables replay and recovery

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid diagram of channel topology
- [ ] Spring Kafka producer + listener sample
- [ ] Channel-naming convention reference
- [ ] Schema registry usage example
- [ ] Compliance Mapping (3 rings)
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps (Kafka cluster sizing; partition planning)
- [ ] Threat Model (channel access control, mTLS)
- [ ] Operational Runbook
- [ ] Test Strategy

## References

- Hohpe & Woolf — EIP Chapter 3
- Apache Kafka documentation
- Catalog: PRIN-002 Event-Driven Architecture; EIP-002 Point-to-Point; EIP-003 Pub-Sub; EIP-024 Idempotent Receiver
