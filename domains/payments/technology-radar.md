# Payments Domain Technology Radar

## Overview

The Technology Radar tracks technology adoption decisions for the Payments domain. This document guides technology selection, migration planning, and team training priorities.

Last Updated: **March 8, 2026**
Domain: **Payments Technology**

---

## Technology Quadrants

### 1. ADOPT — Use in Production

These technologies are proven, mature, and production-ready. All new projects should use these technologies by default.

| Technology | Category | Rationale | Status | Adopted |
|-----------|----------|-----------|--------|---------|
| **Temporal** | Workflow Orchestration | Distributed payment sagas require reliable workflow orchestration. Temporal provides strong consistency guarantees and clear failure handling. Chosen over Camunda for cloud-native architecture. | In Production | 2025-Q3 |
| **Kafka** | Message Streaming | Event-driven architecture requires high-throughput streaming. Kafka handles real-time payment events and audit trails reliably. | In Production | 2024-Q4 |
| **Spring Boot 3.x** | Application Framework | Standard framework across Techcombank. Spring Boot 3.x provides GraalVM support, improved performance, and virtual thread support. | In Production | 2025-Q1 |
| **PostgreSQL 15+** | Relational Database | ACID compliance required for financial transactions. PostgreSQL chosen for strong consistency guarantees and native JSON support for payment metadata. | In Production | 2024-Q2 |
| **Docker/Kubernetes** | Containerization & Orchestration | Cloud-native deployment required. Docker for image standardization; Kubernetes for container orchestration and auto-scaling. | In Production | 2024-Q3 |
| **HashiCorp Vault** | Secrets Management | PCI-DSS compliance requires secure credential management. Vault provides encryption, rotation, and audit trails for API keys and passwords. | In Production | 2025-Q2 |
| **OpenTelemetry** | Observability | Distributed tracing required for saga debugging. OpenTelemetry provides vendor-neutral instrumentation and integration with DataDog. | In Production | 2025-Q2 |

---

### 2. TRIAL — Evaluate in Non-Critical Path

These technologies show promise and should be evaluated on non-critical projects or specific use cases. Success in trial may lead to adoption.

| Technology | Category | Rationale | Status | Started |
|-----------|----------|-----------|--------|---------|
| **gRPC** | Internal Communication | Lower latency than REST for internal service-to-service communication (e.g., fraud scoring). Proto3 contract-first approach improves API clarity. Consider for fraud screening integration (< 100ms SLA). | Evaluating | 2026-Q1 |
| **NATS Streaming** | Lightweight Messaging | Alternative to Kafka for lower-throughput use cases. Lower operational overhead and memory footprint. Evaluate for internal event publishing. | Planning | 2026-Q2 |
| **Redis Cluster** | In-Memory Cache | Cache payment metadata and transaction state. High throughput required (> 100K ops/sec). Evaluate Redis vs Memcached. | Evaluating | 2026-Q1 |

---

### 3. ASSESS — Research and Spike

These technologies are interesting but require deeper research. Do not use in production yet. Assign a team to spike and evaluate.

| Technology | Category | Rationale | Next Step |
|-----------|----------|-----------|-----------|
| **Dapr** | Distributed Application Runtime | Microservices runtime providing state management, pub/sub, and actor model. Could simplify payment saga orchestration. Requires proof-of-concept on non-critical flow. | POC Q2 2026 |
| **Flink** | Real-time Stream Processing | Low-latency stream processing for fraud detection and payment monitoring. Evaluate vs Kafka Streams. | Research Q2 2026 |
| **Service Mesh (Istio)** | Service Mesh | Advanced traffic management, circuit breaking, retry policies. Currently not justified; evaluate if complexity grows significantly. | Research Q3 2026 |

---

### 4. HOLD — Do Not Use

These technologies should not be adopted. Either replaced by better alternatives or unsuitable for payment domain.

| Technology | Category | Reason | Alternative |
|-----------|----------|--------|-------------|
| **RabbitMQ** | Message Streaming | Replaced by Kafka for higher throughput and durability guarantees. RabbitMQ lacks partition-based scalability required for payment streams. | Kafka |
| **Apache Airflow** | Workflow Orchestration | Batch-only workflow engine unsuitable for real-time payment orchestration. Temporal provides better distributed transaction semantics. | Temporal |
| **Cassandra** | NoSQL Database | AP model (availability over consistency) unsuitable for financial transactions. PostgreSQL provides stronger ACID guarantees. | PostgreSQL |
| **Elasticsearch** | Search Engine | Payment data not suitable for full-text search. Use PostgreSQL with JSON queries for structured data; DataDog for log aggregation. | PostgreSQL + DataDog |

---

## Technology Recommendations by Capability

### Payment Initiation

| Capability | Recommended | Details |
|-----------|-------------|---------|
| Request Intake | Spring Boot 3.x REST API | Standard REST with OpenAPI documentation |
| Validation Rules Engine | Spring Boot + Drools | Business rules for validation and limits |
| Authentication | OAuth2 (Spring Security) | Standards-based authentication |

### Payment Processing

| Capability | Recommended | Details |
|-----------|-------------|---------|
| Saga Orchestration | Temporal | Distributed transaction management |
| Debit/Credit Posting | PostgreSQL ACID transactions | Financial data consistency |
| Event Publishing | Kafka | Payment events for downstream systems |

### Payment Routing

| Capability | Recommended | Details |
|-----------|-------------|---------|
| NAPAS Integration | Spring Boot + ISO 20022 library | Mature XML messaging |
| SWIFT Integration | Spring Boot + SWIFT library | SWIFT FIN message handling |
| VietQR Integration | Spring Boot REST client | QR network API integration |

### Reconciliation

| Capability | Recommended | Details |
|-----------|-------------|---------|
| Data Matching | PostgreSQL window functions | Efficient SQL-based matching |
| Break Handling | Spring Boot + workflow | Manual exception workflow |
| Audit Trail | Kafka (append-only log) | Immutable transaction history |

### Fraud Screening

| Capability | Recommended | Details |
|-----------|-------------|---------|
| Rule Engine | Drools + Spring Boot | Rule-based detection |
| Scoring | gRPC service (Trial) | Low-latency ML model serving |
| Alerting | Kafka + DataDog | Real-time alert generation |

---

## Migration Roadmap

### Current State (2026-Q1)

- Spring Boot 2.7.x (legacy)
- RabbitMQ for messaging (being phased out)
- Camunda for some workflows (being replaced)
- Manual reconciliation (partially automated)

### Target State (2026-Q4)

- Spring Boot 3.2.x across all services
- Kafka as primary messaging layer
- Temporal for all payment sagas
- Automated reconciliation at 99%+

### Phases

| Phase | Timeline | Work |
|-------|----------|------|
| Phase 1: gRPC Trial | Q1-Q2 2026 | Implement fraud screening gRPC service; compare performance vs REST |
| Phase 2: Kafka Migration | Q2-Q3 2026 | Migrate RabbitMQ topics to Kafka; update consumers |
| Phase 3: Temporal Scale | Q3-Q4 2026 | Deploy Temporal cluster; migrate Camunda workflows; scale to all payment sagas |
| Phase 4: Optimization | Q4 2026-Q1 2027 | Performance tuning; cost optimization; advanced features (query endpoints, visibility) |

---

## Cost Analysis

### Annual Technology Costs (2026 estimate)

| Technology | Cost | Usage | Notes |
|-----------|------|-------|-------|
| Kafka Cluster | $150K | 5 clusters, 3 zones | Self-managed on Kubernetes |
| PostgreSQL (RDS) | $120K | Multi-AZ, backups, replicas | AWS managed service |
| Temporal Cloud | $80K | 50K workflow runs/day | Fully managed service |
| Kubernetes | $200K | EKS, compute, storage | AWS managed + compute nodes |
| Monitoring (DataDog) | $180K | 500+ services, logs, traces | Per-container pricing |
| **Total** | **$730K** | — | Annual estimate |

---

## Training and Adoption

### Immediate Priorities (Q1 2026)

1. **Temporal Workshop** — Train team on workflow modeling, failure handling, compensation
2. **gRPC Bootcamp** — Evaluate gRPC for fraud service integration
3. **Spring Boot 3 Migration** — Upgrade existing services to Spring Boot 3.2.x

### Resource Requirements

- 2 engineers on Temporal deep-dive (4 weeks)
- 1 engineer on gRPC evaluation (2 weeks)
- 1 engineer on Spring Boot migration (4 weeks)

---

## Technology Governance

### Decision Making

All new technology additions must:

1. **Spike (1 week)** — Proof-of-concept on a small use case
2. **Design Review** — Present to architecture team; compare with alternatives
3. **Pilot (4 weeks)** — Run in non-critical path with monitoring
4. **Decision** — Move to Trial or Hold based on results

### Review Cadence

- **Quarterly** — Update radar with new technologies and progress on trials
- **Annually** — Major architecture review; reassess all technologies

### Owner

- **Technology Lead**: @architect-lead
- **Review Committee**: Platform team leads

---

## References

- [Temporal Documentation](https://docs.temporal.io)
- [Kafka Best Practices](https://kafka.apache.org/documentation.html)
- [Spring Boot 3 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Release-Notes)
- [gRPC Protocol Buffers](https://grpc.io/docs/what-is-grpc/)
- [Technology Radar Concept](https://www.thoughtworks.com/radar)

---

Last Updated: March 8, 2026 | Maintained by: @architect-lead
