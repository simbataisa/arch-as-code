# Reference Architecture Document Template

Status: Draft | Last Reviewed: 2026-05-09 | Owner: @ea-board
Catalog ID: TPL-004
Tier Applicability: N/A (meta-document — template for reference architecture authors)

---

## How to Use This Template

1. Copy this file to `knowledge-base/reference-architectures/<domain>/<your-ref-arch-name>.md`.
2. Assign the next available Catalog ID in the REF-NNN series from `governance/standards/enterprise-architecture-catalog.md`.
3. Replace every `[PLACEHOLDER]` with real content and delete authoring notes (lines beginning with `>`) once the section is complete.
4. Reference architectures are larger documents (250–400 lines) describing end-to-end banking flows — not single patterns. They reference multiple implementing patterns from the `knowledge-base/patterns/` catalog.
5. Set Status to `Draft` while authoring. EA Board promotes to `Approved` after cross-domain review.
6. This template file itself must never describe a real architecture. It is a meta-document only.

---

# [PLACEHOLDER: Reference Architecture Title — e.g., "Real-Time Payment Processing Reference Architecture"]

Status: Draft | Last Reviewed: [YYYY-MM-DD] | Owner: [@domain-owner]
Catalog ID: [REF-NNN]
Tier Applicability: [T0 | T1 | T2 | T3 — list applicable tiers]

> **Authoring note**: Reference architectures are authoritative end-to-end descriptions of a banking capability. They document what Techcombank has decided to build, not what it might consider. All major architectural decisions in this document should have an ADR reference.

## Business Context

> **Authoring note**: 3–5 sentences describing the banking flow this reference architecture covers. Name the business capability, the customer journey it enables, the regulatory context, and the volume/criticality. Avoid technical jargon here — this section is readable by a business stakeholder.

[PLACEHOLDER: e.g., "The Real-Time Payment Processing capability enables Techcombank retail and SME customers to transfer funds via NAPAS 247 in under 5 seconds, 24 hours a day, 365 days a year. The flow spans customer-facing mobile and internet banking channels, Techcombank's internal ledger and compliance systems, and the NAPAS interbank switch. Payment volumes peak at Tết (Vietnamese Lunar New Year) at approximately 3× normal daily throughput, placing this capability firmly in the T0 service tier with a 99.99% availability requirement and a regulatory obligation under SBV Circular 09/2020."]

**Business capability**: [PLACEHOLDER: e.g., Real-Time Domestic Fund Transfer via NAPAS 247]
**Regulatory category**: [PLACEHOLDER: e.g., Payment processing — SBV Circular 09/2020, BCBS 239]
**Primary users**: [PLACEHOLDER: e.g., Retail customers, SME customers, internal treasury]
**Transaction volume**: [PLACEHOLDER: e.g., ~500 TPS normal; ~1,500 TPS Tết peak]
**Service tier**: [PLACEHOLDER: e.g., T0 — payment-critical path]

## Architecture Overview

> **Authoring note**: The architecture overview diagram is mandatory. It must show ALL major components (channels, services, data stores, external systems, messaging infrastructure) and the primary flows between them. Use `graph LR` for left-to-right flows (typical for payment pipelines), `graph TD` for top-down (typical for layered architectures). Replace the placeholder comment below with the real diagram. The diagram should be self-contained — a reader who has not read the rest of the document should understand the high-level topology from the diagram alone.

```mermaid
%% [PLACEHOLDER: Replace this comment with your architecture overview diagram]
%% Example skeleton for a payment flow — delete and replace:
%%
%% graph LR
%%     subgraph Channels
%%         MB[Mobile Banking App]
%%         IB[Internet Banking]
%%     end
%%     subgraph API Layer
%%         GW[API Gateway / BFF]
%%     end
%%     subgraph Core Services
%%         PAY[Payment Service]
%%         LED[Ledger Service]
%%         FRD[Fraud Screening Service]
%%     end
%%     subgraph Messaging
%%         KF[Kafka — payment-events topic]
%%     end
%%     subgraph External
%%         NAPAS[NAPAS 247 Switch]
%%     end
%%     subgraph Data
%%         DB[(Aurora PostgreSQL)]
%%     end
%%
%%     MB --> GW
%%     IB --> GW
%%     GW --> PAY
%%     PAY --> FRD
%%     PAY --> LED
%%     PAY --> KF
%%     PAY --> NAPAS
%%     LED --> DB
```

## Component Descriptions

> **Authoring note**: List every component shown in the overview diagram. Minimum 6 rows for a reference architecture. Include all major services, data stores, messaging infrastructure, and external systems. For technology, name the specific product and version in use at Techcombank (e.g., "Aurora PostgreSQL 15.4", not "relational database").

| Component | Technology | Purpose | Tier |
|---|---|---|---|
| [PLACEHOLDER: Component name — e.g., API Gateway / BFF] | [PLACEHOLDER: e.g., Spring Cloud Gateway 4.x on Kubernetes] | [PLACEHOLDER: e.g., Authenticates customer sessions, routes payment requests to Payment Service, enforces rate limits] | [PLACEHOLDER: T0] |
| [PLACEHOLDER: Component 2] | [PLACEHOLDER: Technology] | [PLACEHOLDER: Purpose] | [PLACEHOLDER: Tier] |
| [PLACEHOLDER: Component 3] | [PLACEHOLDER: Technology] | [PLACEHOLDER: Purpose] | [PLACEHOLDER: Tier] |
| [PLACEHOLDER: Component 4] | [PLACEHOLDER: Technology] | [PLACEHOLDER: Purpose] | [PLACEHOLDER: Tier] |
| [PLACEHOLDER: Component 5] | [PLACEHOLDER: Technology] | [PLACEHOLDER: Purpose] | [PLACEHOLDER: Tier] |
| [PLACEHOLDER: Component 6] | [PLACEHOLDER: Technology] | [PLACEHOLDER: Purpose] | [PLACEHOLDER: Tier] |
| [PLACEHOLDER: Add rows as needed] | | | |

## Key Integration Points

> **Authoring note**: Describe each external or cross-domain integration boundary. For each: name the counterparty, the protocol, the SLA, and the failure mode handling. Typical integrations for a Techcombank flow: NAPAS switch, SBV reporting endpoint, core banking (T24/Temenos), fraud screening, notification service.

**[PLACEHOLDER: Integration 1 — e.g., NAPAS 247 Switch]**
- Protocol: [PLACEHOLDER: e.g., ISO 8583 over TCP/IP, dedicated leased line]
- SLA: [PLACEHOLDER: e.g., P95 < 100 ms response from NAPAS; 99.95% availability per NAPAS SLA]
- Failure handling: [PLACEHOLDER: e.g., Circuit breaker (RES-002) opens after 5 consecutive timeouts; customer receives "payment queued" UX; retry with idempotency key when circuit closes]
- Owner: [PLACEHOLDER: e.g., NAPAS Channel team]

**[PLACEHOLDER: Integration 2]**
- Protocol: [PLACEHOLDER]
- SLA: [PLACEHOLDER]
- Failure handling: [PLACEHOLDER]
- Owner: [PLACEHOLDER]

**[PLACEHOLDER: Add integrations as needed]**

## Data Flow

> **Authoring note**: A sequence diagram showing the happy-path request flow with realistic message names. Show at minimum: the channel initiating the request, the primary service chain, any async event emissions, and the response path. Include timing annotations (e.g., "< 50ms") at key hops to link back to the latency budget (NFR-002). Add a separate sequence for the primary failure path.

### Happy Path

```mermaid
%% [PLACEHOLDER: Replace with your happy-path sequence diagram]
%% sequenceDiagram
%%     participant Customer
%%     participant MobileApp
%%     participant APIGateway
%%     participant PaymentService
%%     participant FraudService
%%     participant NAPAS
%%     participant LedgerService
%%
%%     Customer->>MobileApp: initiateTransfer(amount, toAccount)
%%     MobileApp->>APIGateway: POST /v1/payments [JWT]  (<5ms)
%%     APIGateway->>PaymentService: route + validate  (<10ms)
%%     PaymentService->>FraudService: screenPayment(payload)  (<50ms)
%%     FraudService-->>PaymentService: APPROVED
%%     PaymentService->>NAPAS: submitTransfer(ISO8583)  (<100ms)
%%     NAPAS-->>PaymentService: ACK + transactionRef
%%     PaymentService->>LedgerService: debitAccount(event) [async Kafka]
%%     PaymentService-->>APIGateway: PaymentAccepted(transactionRef)
%%     APIGateway-->>MobileApp: 200 OK (transactionRef)
%%     MobileApp-->>Customer: "Transfer successful" (<200ms total p99)
```

### Primary Failure Path

```mermaid
%% [PLACEHOLDER: Replace with your failure-path sequence diagram]
%% Show: timeout, circuit breaker trip, customer-facing error response, compensating action
```

## NFR Targets

> **Authoring note**: Populate concrete numbers. Do not leave "TBD". All latency targets must be consistent with NFR-002. All availability targets must be consistent with NFR-001. Reference the NFR doc IDs. Add rows for any additional metrics relevant to this specific flow (e.g., NAPAS settlement success rate, fraud false-positive rate).

| Metric | Target | Tier | Measurement | Reference |
|---|---|---|---|---|
| End-to-end p99 latency | [PLACEHOLDER: e.g., < 200 ms] | [T0] | Gatling `responseTime.percentile3`; Grafana `payment-latency-overview` | [NFR-002] |
| Service availability | [PLACEHOLDER: e.g., 99.99%] | [T0] | Prometheus `job:sli_availability:ratio_rate5m` rolling 30d | [NFR-001] |
| Throughput (goodput) | [PLACEHOLDER: e.g., 1,500 TPS at Tết peak] | [T0] | Gatling `successfulRequests.count / duration` | [NFR-004] |
| CPU utilisation | [PLACEHOLDER: e.g., < 70% at peak] | [T0] | Kubernetes `container_cpu_usage_seconds_total` | [NFR-003] |
| Kafka consumer lag | [PLACEHOLDER: e.g., < 5,000 messages] | [T0] | `kafka_consumer_group_lag` | [NFR-003] |
| RTO | [PLACEHOLDER: e.g., < 5 min] | [T0] | DR drill — time from failure detection to traffic restoration | [NFR-001] |
| RPO | [PLACEHOLDER: e.g., < 30 sec] | [T0] | DR drill — last committed transaction before failover | [NFR-001] |
| [PLACEHOLDER: Additional metric] | [PLACEHOLDER] | [PLACEHOLDER] | [PLACEHOLDER] | [PLACEHOLDER] |

## Compliance Mapping

> **Authoring note**: 3-ring table. Reference architectures often have more compliance obligations than individual patterns. Include at least 2 rows per ring. Ring 2 cells MUST end with `⚠️ (working summary — pending Legal review)`; BCBS 230 cells MUST end with `⚠️ (working summary — pending PDF fetch)`.

| Ring | Regulation | Provision | How this architecture satisfies |
|---|---|---|---|
| Ring 0 | [PLACEHOLDER: e.g., ISO 27001] | [PLACEHOLDER: e.g., A.12.1.3 Capacity Management] | [PLACEHOLDER: explanation] |
| Ring 0 | [PLACEHOLDER: e.g., NIST SP 800-53] | [PLACEHOLDER: e.g., SC-5 DoS Protection] | [PLACEHOLDER: explanation] |
| Ring 1 | [PLACEHOLDER: e.g., BCBS 239] | [PLACEHOLDER: e.g., §3 Timeliness] | [PLACEHOLDER: explanation] |
| Ring 1 | [PLACEHOLDER: e.g., BCBS 230] | [PLACEHOLDER: e.g., Principle 2 ⚠️ (working summary — pending PDF fetch)] | [PLACEHOLDER: explanation] |
| Ring 2 | [PLACEHOLDER: e.g., SBV Circular 09/2020] | [PLACEHOLDER: e.g., §IV.2 ⚠️ (working summary — pending Legal review)] | [PLACEHOLDER: explanation] |
| Ring 2 | [PLACEHOLDER: Add second Ring 2 row if applicable] | [PLACEHOLDER ⚠️ (working summary — pending Legal review)] | [PLACEHOLDER] |

## DR / Resilience Strategy

> **Authoring note**: Describe the disaster recovery strategy for this specific flow. Reference NFR-001 RTO/RPO targets. State the Recovery Strategy (Warm Standby, Active-Active, Pilot Light, etc.) and the failover procedure in numbered steps. Include the recovery time estimate for each step.

**Recovery strategy**: [PLACEHOLDER: e.g., Active-Active across two AWS availability zones; Warm Standby to the DR region (ap-southeast-1) for full regional failure]

**RTO**: [PLACEHOLDER: e.g., < 5 minutes for AZ failure; < 30 minutes for full region failure]
**RPO**: [PLACEHOLDER: e.g., < 30 seconds — Aurora Global Database synchronous replication lag]

**Failover procedure** (AZ failure):

1. [PLACEHOLDER: Step 1 — e.g., "Route 53 health check detects payment-service unhealthy in AZ-A (< 30 seconds)"]
2. [PLACEHOLDER: Step 2 — e.g., "Route 53 failover routing redirects traffic to AZ-B replica (< 60 seconds)"]
3. [PLACEHOLDER: Step 3 — e.g., "Aurora reader endpoint promotes AZ-B replica to writer (< 2 minutes)"]
4. [PLACEHOLDER: Step 4 — validate and update runbook]

**Chaos engineering**: [PLACEHOLDER: Describe which chaos experiments are run quarterly to validate this DR strategy — pod kill, AZ blackout simulation, NAPAS stub timeout injection.]

## Deployment Topology

> **Authoring note**: Describe the physical/logical deployment: cloud provider, regions, availability zones, Kubernetes cluster topology, network segmentation. A table or a brief prose description is acceptable. For T0 flows, describe the multi-AZ and multi-region topology explicitly.

| Environment | Region | AZs | Cluster | Notes |
|---|---|---|---|---|
| Production | [PLACEHOLDER: e.g., ap-southeast-1 (Singapore)] | [PLACEHOLDER: e.g., 3 AZs] | [PLACEHOLDER: e.g., eks-prod-tcb] | [PLACEHOLDER: e.g., T0 services on dedicated node group; Spot instances for T2] |
| DR | [PLACEHOLDER: e.g., ap-east-1 (Hong Kong)] | [PLACEHOLDER: e.g., 2 AZs] | [PLACEHOLDER: e.g., eks-dr-tcb] | [PLACEHOLDER: e.g., Warm standby — 50% capacity; scales to 100% on failover] |
| Staging | [PLACEHOLDER: e.g., ap-southeast-1] | [PLACEHOLDER: e.g., 2 AZs] | [PLACEHOLDER: e.g., eks-staging-tcb] | [PLACEHOLDER: e.g., Full topology mirror; Spot instances throughout] |

**Network segmentation**: [PLACEHOLDER: e.g., All inter-service traffic within the VPC private subnet; NAPAS connection via dedicated leased line terminating at the Transit Gateway; no inter-service traffic traverses the public internet.]

## Cost / FinOps

> **Authoring note**: Break down the major cost components. Include compute, storage, data transfer, and external service costs. Provide monthly estimates where possible. Name the primary optimisation levers.

| Component | Sizing | Estimated monthly cost (USD) | Optimisation lever |
|---|---|---|---|
| [PLACEHOLDER: e.g., EKS compute — T0 services] | [PLACEHOLDER: e.g., 6× m5.xlarge Reserved] | [PLACEHOLDER: e.g., ~USD 420] | [PLACEHOLDER: e.g., Reserved Instances for baseline 3 pods; On-Demand for Tết surge pods] |
| [PLACEHOLDER: e.g., Aurora PostgreSQL] | [PLACEHOLDER: e.g., r6g.2xlarge, Multi-AZ] | [PLACEHOLDER: e.g., ~USD 540] | [PLACEHOLDER: e.g., Aurora Serverless v2 for staging; read replicas for reporting queries] |
| [PLACEHOLDER: e.g., MSK Kafka] | [PLACEHOLDER: e.g., 3× kafka.m5.xlarge, 3 AZs] | [PLACEHOLDER: e.g., ~USD 310] | [PLACEHOLDER: e.g., LZ4 compression reduces storage 4:1; delete topics on decommission] |
| [PLACEHOLDER: Add rows] | | | |
| **Total** | | [PLACEHOLDER: e.g., ~USD 1,270/month] | |

**Cost of service degradation**: [PLACEHOLDER: e.g., "At VND 50B/day transaction volume, a 5-minute T0 outage costs approximately VND 174M (~USD 7,000) in lost transaction value; the resilience infrastructure above has a payback period of < 1 month if it prevents a single major incident per year."]

## Related Patterns

> **Authoring note**: List the implementing patterns from the catalog that collectively realise this reference architecture. Include at least 4–6 links. These are the patterns that a development team would implement when building the components described in this reference architecture.

- [PLACEHOLDER: e.g., "[RES-002 Circuit Breaker](../patterns/resilience/circuit-breaker.md) — protects NAPAS and fraud-service integration points"]
- [PLACEHOLDER: e.g., "[INT-001 Saga Orchestration](../patterns/integration/saga-orchestration.md) — coordinates multi-service payment state machine"]
- [PLACEHOLDER: e.g., "[NFR-001 Service Tiering + RTO/RPO Matrix](../nfr/service-tiering-rto-rpo.md) — defines T0 availability and DR targets for this flow"]
- [PLACEHOLDER: e.g., "[NFR-002 Latency Budget Model](../nfr/latency-budget-model.md) — p99 < 200ms budget decomposition across the payment call chain"]
- [PLACEHOLDER: e.g., "[NFR-003 Capacity Planning Model](../nfr/capacity-planning-model.md) — pod and connection pool sizing for Tết peak load"]
- [PLACEHOLDER: Add 1–2 more]

## References

> **Authoring note**: 5–10 references. Include: primary standards (ISO 20022, NAPAS specifications, SBV circulars), architectural references (books, cloud provider guides), and internal ADRs.

- [PLACEHOLDER: e.g., "NAPAS 247 Technical Integration Specification v3.2 (internal — contact NAPAS Channel team)"]
- [PLACEHOLDER: e.g., "SBV Circular 09/2020 — Regulations on electronic payment operations (official SBV publication)"]
- [PLACEHOLDER: e.g., "ISO 20022 Real-Time Payment Scheme — Message definitions (iso20022.org)"]
- [PLACEHOLDER: e.g., "AWS Reference Architecture — Real-Time Payment Processing on AWS (aws.amazon.com/financial-services)"]
- [PLACEHOLDER: e.g., "ADR-NNN — [Decision title] (governance/decisions/ADR-NNN.md)"]
- [PLACEHOLDER: e.g., "`knowledge-base/_research-notes.md` §NAPAS and §Payment for Techcombank-specific data"]

---

**Key Takeaway**: [PLACEHOLDER: One sentence that captures the essential design choice and the primary benefit. e.g., "The NAPAS real-time payment flow achieves T0 99.99% availability by combining Active-Active multi-AZ deployment, circuit-breaker protection on the NAPAS interface, saga orchestration for multi-service consistency, and pre-provisioned Tết peak capacity — treating every component on the payment critical path as a T0 reliability obligation."]
