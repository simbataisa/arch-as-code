# Design: Observability, Platform & Standards Expansion
**Date:** 2026-05-10
**Status:** Draft — pending EA-Board review
**Author:** Architecture Catalog Team
**Relates to:** Banking Enterprise Architecture Catalog (see `docs/superpowers/specs/2026-05-08-banking-enterprise-architecture-catalog-design.md`)

---

## 1. Problem Statement

The existing catalog (Wave 0 + Wave 1a/b/c — 58 Draft/Approved docs) covers resilience, security, EIP messaging, and core banking patterns. The following cross-cutting concerns are either absent or too shallow to guide implementation:

| Gap | Current state | Impact |
|---|---|---|
| OTEL instrumentation | Not covered | Engineers instrument inconsistently; traces don't correlate across services |
| Trace propagation across protocols | Not covered | Traces break at Kafka, gRPC, WebSocket boundaries |
| Structured logging standard | BP-004 mentions logs superficially | No PII masking standard, no OpenSearch index policy |
| SLO alerting | BP-007 covers golden signals only | No burn-rate alerting, no Dynatrace SLO definitions |
| Async middleware observability | Not covered | No visibility into Kafka lag, SQS age, DLQ depth |
| Full service mesh (traffic) | SEC-001 covers mTLS only | No canary, no fault injection, no mesh-level observability |
| CNCF stack selection | PRIN-005 mentions cloud-native generically | No governed project selection; teams adopt sandbox tools ad-hoc |
| AsyncAPI specification | Not covered | Event-driven API contracts published as informal Confluence pages |
| CloudEvents envelope | Not covered | Every team invents its own event envelope schema |
| Error code mapping | Not covered | T24 OFS errors surface as raw strings to mobile clients |
| OpenAPI 3.2 | PRIN-001 mentions OpenAPI generically | Webhooks and JSON Schema 2020-12 alignment not addressed |

---

## 2. Approach Selected

**Approach B — Two new domains + integration extensions.**

Two new first-class domains created under `knowledge-base/patterns/`:
- `patterns/observability/` — OBS-001 through OBS-005
- `patterns/platform/` — PLT-001, PLT-002

Three new entries in the existing `patterns/integration/` domain:
- INT-010 AsyncAPI Specification
- INT-011 CloudEvents Envelope Standard
- INT-012 Error Code Mapping & Propagation

One in-place enhancement:
- PRIN-001 api-first-design.md — add `### OpenAPI 3.2 Additions` subsection

**Rejected alternatives:**
- *Approach A (enhance existing docs):* BP-004 and SEC-001 would exceed 600 lines and mix concerns.
- *Approach C (standards/ directory):* Adds a third new directory and a distinct template type; complexity not justified at current catalog scale.

---

## 3. Observability Domain (patterns/observability/)

### 3.1 OBS-001 — OpenTelemetry Instrumentation

**Catalog ID:** OBS-001 | **Tier:** T0, T1, T2, T3 | **Owner:** @sre-lead

**Scope:** Java Spring Boot 3.x auto-instrumentation via the OTEL Java agent plus manual span creation for business-critical banking events.

**Key content:**
- OTEL Java agent attachment: `-javaagent:opentelemetry-javaagent.jar` with Spring Boot configuration
- Manual instrumentation: `@WithSpan` for service methods; `Span.current().setAttribute()` for banking-specific attributes (`payment.reference`, `customer.tier`, `napas.channel`, `t24.ofs.function`)
- OTEL Collector as vendor-neutral routing layer — single Collector config fans out to three backends:
  - **Grafana Tempo:** gRPC OTLP exporter (`otlp/grpc`)
  - **Dynatrace:** HTTP OTLP exporter with API token (`otlp/http` to `{tenant}.live.dynatrace.com/api/v2/otlp`)
  - **OpenSearch:** via Data Prepper OTLP receiver → OpenSearch Trace Analytics index
- Resource detectors: Kubernetes pod metadata (namespace, pod name, node), service version from `MANIFEST.MF`
- Sampling strategy:
  - T0 payment flows: always-sample (100%)
  - T1: 50% head-based
  - T2/T3: 10% head-based
  - Error traces: tail-based (always sampled regardless of tier, via Grafana Tempo / Dynatrace)

**Compliance:** NIST SP 800-92 (log/trace management), BCBS 239 §6 Accuracy (complete audit trail), SBV Circular 09/2020 §IV.3 ⚠️ (working summary — pending Legal review).

---

### 3.2 OBS-002 — Distributed Trace Propagation

**Catalog ID:** OBS-002 | **Tier:** T0, T1, T2 | **Owner:** @sre-lead

**Scope:** W3C TraceContext (`traceparent` / `tracestate`) propagation across every protocol and middleware in use at Techcombank.

**Key content:**

| Protocol | Propagation mechanism | Implementation |
|---|---|---|
| HTTP / HTTPS | `traceparent` request header | Spring Boot auto (Micrometer Tracing + OTEL bridge) |
| gRPC | metadata key `traceparent` | `grpc-opentelemetry` interceptor; `GrpcTelemetry` bean |
| WebSocket (STOMP) | Custom header on HTTP upgrade handshake; propagated via `StompHeaderAccessor` | Manual `beforeHandshake` interceptor |
| QUIC / HTTP/3 | HEADERS frame (identical to HTTP/3 headers) | Netty QUIC transport with OTEL HTTP instrumentation |
| Kafka | `traceparent` message header | `opentelemetry-kafka-clients` → `KafkaTelemetry.wrap(producer/consumer)` |
| SQS | `MessageAttribute` named `traceparent` (String type) | AWS SDK v2 + OTEL SQS instrumentation |
| SNS → SQS | SNS passes `MessageAttribute` through to SQS subscriber | SNS `MessageAttribute` passthrough — no special handling |
| ActiveMQ / JMS | JMS `StringProperty` named `traceparent` | OpenTelemetry JMS instrumentation |
| Simple Queue Service | Same as SQS | AWS SDK v2 |

**Standards decisions:**
- W3C TraceContext only; B3 propagation disabled to reduce header surface
- Span relationship semantics: `CHILD_OF` for synchronous calls; `FOLLOWS_FROM` for async consumer spans (Kafka, SQS) — consumer is not a synchronous child of the producer
- Baggage propagation: `customer.tier` and `payment.reference` carried as W3C Baggage for downstream filtering

**Compliance:** BCBS 239 §6 Accuracy, BCBS 230 Principle 6 ⚠️ (working summary — pending PDF fetch), SBV Circular 09/2020 §IV.3 ⚠️ (working summary — pending Legal review).

---

### 3.3 OBS-003 — Structured Logging Standard

**Catalog ID:** OBS-003 | **Tier:** T0, T1, T2, T3 | **Owner:** @sre-lead

**Scope:** Mandatory JSON log schema, PII masking at emission, and OpenSearch index lifecycle policy.

**Key content:**

Mandatory fields every log line must carry:

```json
{
  "timestamp": "2026-05-10T10:30:15.123Z",
  "level": "INFO",
  "service.name": "payment-gateway",
  "service.version": "2.3.1",
  "environment": "production",
  "tier": "T0",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "message": "Payment authorisation approved",
  "payment.reference": "TCB-2026-001234",
  "napas.channel": "IBFT"
}
```

PII masking:
- Logback `MaskingConverter` applied at appender level — field-name allowlist approach
- Fields always masked before emission: `customerId` → `***-4567`, `accountNumber` → `***1234`, `cccdNumber` → `MASKED`, `biometricHash` → `REDACTED`
- Masking happens before log emission — never rely on downstream scrubbing

OpenSearch index strategy:
- Pattern: `techcombank-logs-{service}-{yyyy.MM.dd}`
- ILM policy per tier: T0 hot 7d → warm 30d → cold 90d → delete at 5 years (SBV §IV.3 retention requirement); T2/T3 hot 3d → warm 14d → delete at 90d
- Dynatrace log ingest: same JSON forwarded via Fluent Bit sidecar with `dynatrace` output plugin

**Compliance:** BCBS 239 §6, PCI-DSS §10.3 (log integrity), Decree 13/2023 Art. 9 ⚠️ (working summary — pending Legal review) — PII masking before cross-system log transit.

---

### 3.4 OBS-004 — SLO Alerting

**Catalog ID:** OBS-004 | **Tier:** T0, T1, T2 | **Owner:** @sre-lead

**Scope:** SLO definition → SLI measurement → error-budget burn-rate alerting across Grafana, Dynatrace, and OpenSearch.

**Key content:**

Per-tier error budget targets:

| Tier | SLO | Error budget (30d) | Burn-rate page threshold |
|---|---|---|---|
| T0 | 99.95% | 21.9 min | 2% budget in 1h (fast burn) |
| T1 | 99.9% | 43.8 min | 5% budget in 1h |
| T2 | 99.5% | 3.6 hours | 10% budget in 6h |

Multi-window burn-rate alerting (Prometheus recording rules):
- Fast page: 2% budget consumed in last 1h → PagerDuty P1
- Slow page: 5% budget consumed in last 6h → PagerDuty P2
- Warning: 10% consumed in 3d → Slack alert only

Dynatrace SLO definition via DQL — mirrors Prometheus thresholds. Grafana alerting rules reference the Prometheus recording rules; no duplicate alert definitions.

Relation to BP-007 (golden signals): BP-007 defines *what* to measure (latency, traffic, errors, saturation); OBS-004 defines *how to alert* on those measurements using error budgets.

**Compliance:** BCBS 230 Principle 2 ⚠️ (working summary — pending PDF fetch), SBV Circular 09/2020 §IV.2 ⚠️ (working summary — pending Legal review).

---

### 3.5 OBS-005 — Async Middleware Observability

**Catalog ID:** OBS-005 | **Tier:** T0, T1 | **Owner:** @sre-lead

**Scope:** Observability *through* message brokers — trace propagation semantics, consumer lag alerting, and DLQ depth as a first-class signal.

**Key content:**
- Producer → header injection → consumer span extraction with `FOLLOWS_FROM` link (not `CHILD_OF`)
- Kafka-specific: consumer lag via Prometheus JMX exporter (`kafka_consumer_group_lag`); alert at lag > 1000 messages sustained 5 min for T0 topics
- SQS: `ApproximateAgeOfOldestMessage` CloudWatch metric → alert at age > 30s for T0 queues
- SNS: no native lag metric; alert via subscriber SQS queue age
- ActiveMQ: queue depth via JMX → Prometheus; alert at depth > 500 for T0
- DLQ depth as a business signal: DLQ depth > 0 for T0 topics triggers immediate PagerDuty alert (cross-links to EIP-025 Dead Letter Channel)
- OpenSearch: Kafka audit events indexed for compliance replay audit

**Compliance:** BCBS 239 §6, SBV Circular 09/2020 §IV.2 ⚠️ (working summary — pending Legal review).

---

## 4. Platform Domain (patterns/platform/)

### 4.1 PLT-001 — Service Mesh Traffic Management

**Catalog ID:** PLT-001 | **Tier:** T0, T1 | **Owner:** @platform-lead

**Scope:** Istio traffic management — the complement to SEC-001 (mTLS/auth). SEC-001 owns *security*; PLT-001 owns *traffic shaping, canary, fault injection, mesh observability*.

**Key content:**

1. **Canary release** — `VirtualService` weighted routing (95% stable / 5% canary); header-based routing (`X-Beta-User: true` → canary); automated promotion via Argo Rollouts
2. **Traffic policies** — `DestinationRule` for connection pool limits, outlier detection (circuit breaker at mesh layer); relationship to Resilience4j (application layer): mesh handles infrastructure failures, Resilience4j handles business-logic failures
3. **Fault injection** — `VirtualService` `fault` block for chaos drills (delay 500ms, abort 503 5%); integration with BP-005 chaos engineering runbook
4. **Mesh-level observability** — Istio telemetry API v2 → OTEL Collector → Grafana/Dynatrace; automatic span generation for inter-pod calls without code changes; Kiali service graph for dependency visualisation
5. **mTLS policy migration** — `PERMISSIVE` → `STRICT` rollout path for brownfield services; cross-link to SEC-001

Banking use cases: zero-downtime canary of NAPAS payment gateway adapter; fault injection in T24 OFS connector for quarterly DR drills.

**Compliance:** BCBS 230 Principle 4 ⚠️ (working summary — pending PDF fetch), SBV Circular 09/2020 §IV.2 ⚠️ (working summary — pending Legal review).

---

### 4.2 PLT-002 — CNCF Stack Selection

**Catalog ID:** PLT-002 | **Tier:** All | **Owner:** @ea-board

**Scope:** Governed selection of CNCF projects for Techcombank. Decision record with rationale. Sandbox projects prohibited in T0/T1 without EA-Board ADR.

**Governed selections:**

| Layer | Selected | Rejected | Reason |
|---|---|---|---|
| GitOps / CD | Argo CD (graduated) | Flux | Less UI maturity for Techcombank ops team |
| CI Pipeline | GitLab CI | Tekton | Too low-level for banking delivery teams |
| Tracing | Grafana Tempo + Dynatrace | Jaeger | Tempo supersedes Jaeger; Dynatrace for APM |
| Logging | OpenSearch + Loki | ELK | Elastic license risk post-2021 |
| Metrics | Prometheus + Grafana | InfluxDB | Cost and OSS longevity |
| Policy enforcement | OPA / Gatekeeper (graduated) | Kyverno | More expressive for complex banking rules |
| Certificate management | cert-manager (graduated) | Manual | Automation required for 90-day rotation |
| Runtime security | Falco (graduated) | Sysdig | Commercial cost |
| Service mesh | Istio (graduated) | Linkerd | Richer traffic management; SEC-001 / PLT-001 |

Adoption gate: CNCF **graduated** status required for T0/T1. **Incubating** allowed T2/T3 with domain-owner approval. **Sandbox** requires EA-Board ADR regardless of tier.

**Compliance:** BCBS 230 Principle 7 ⚠️ (working summary — pending PDF fetch), SBV Circular 09/2020 §III ⚠️ (working summary — pending Legal review).

---

## 5. Integration Extensions (patterns/integration/)

### 5.1 INT-010 — AsyncAPI Specification

**Catalog ID:** INT-010 | **Tier:** T0, T1, T2 | **Owner:** @tech-lead-backend

**Scope:** AsyncAPI 3.0 as the contract standard for all event-driven channels, parallel to OpenAPI for REST.

**Key content:**
- AsyncAPI 3.0 document anatomy: `info`, `servers` (Kafka broker binding), `channels` (topic name + Kafka binding: partitions, retention), `operations` (publish/subscribe), `messages` (Avro `$ref` to schema registry URL)
- Schema Registry integration: `$ref: 'https://schema-registry.techcombank.com/subjects/payment-transaction-created/versions/latest'`
- Code generation: `asyncapi-generator` with `@asyncapi/java-spring-template` → Spring `@KafkaListener` stubs, producer interfaces
- Contract testing: Microcks for async API mocking (replays recorded messages against spec); consumer-driven contract tests via Pact async extension
- Versioning: AsyncAPI document versioned in Git alongside the service; breaking changes require new `channels` entry (not mutation of existing)
- Relationship to CloudEvents (INT-011): AsyncAPI defines the channel/operation contract; CloudEvents defines the message envelope; they compose

Banking example: full AsyncAPI 3.0 spec for `com.techcombank.payments.transaction.created` with Kafka server binding, Avro schema reference, and consumer operation.

**Compliance:** BCBS 239 §6 Accuracy, SWIFT CSP 2024 §5, SBV Circular 09/2020 §IV.2 ⚠️ (working summary — pending Legal review).

---

### 5.2 INT-011 — CloudEvents Envelope Standard

**Catalog ID:** INT-011 | **Tier:** T0, T1, T2 | **Owner:** @tech-lead-backend

**Scope:** CloudEvents 1.0 as the standard message envelope for all Techcombank domain events.

**Key content:**

Mandatory CloudEvents attributes for every event:
```json
{
  "specversion": "1.0",
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "source": "/techcombank/payments/gateway",
  "type": "com.techcombank.payments.transaction.created",
  "time": "2026-05-10T10:30:00Z",
  "datacontenttype": "application/json",
  "techcombank-tier": "T0",
  "techcombank-traceparent": "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01"
}
```

Extension attributes (Techcombank-specific):
- `techcombank-tier` — T0/T1/T2/T3; drives consumer SLA
- `techcombank-traceparent` — links to OBS-002 W3C trace (separate from Kafka `traceparent` header to support HTTP binding)
- `techcombank-correlationid` — business-level correlation (maps to payment reference)

Bindings:
- **Kafka structured-content mode:** entire CloudEvent as JSON in message value; `content-type: application/cloudevents+json` header
- **HTTP binding:** for webhook delivery to open banking partners (PSD2 / REF-011); `Content-Type: application/cloudevents+json`
- **SQS binding:** CloudEvent JSON in message body; `MessageAttribute` `content-type`

Relationship to AsyncAPI: AsyncAPI `message` schema `$ref` points to CloudEvents schema; the two standards compose — AsyncAPI describes the channel, CloudEvents describes the envelope.

**Compliance:** BCBS 239 §6, SBV Circular 09/2020 §IV.2 ⚠️ (working summary — pending Legal review).

---

### 5.3 INT-012 — Error Code Mapping & Propagation

**Catalog ID:** INT-012 | **Tier:** T0, T1, T2 | **Owner:** @tech-lead-backend

**Scope:** Three-tier error translation from T24 OFS / NAPAS raw codes → domain errors → client-facing RFC 9457 Problem Details.

**Error taxonomy — `ERR-{DOMAIN}-{CODE}` format:**

| Domain prefix | Scope |
|---|---|
| `ERR-PAY-*` | Payment processing |
| `ERR-ACC-*` | Account management |
| `ERR-KYC-*` | KYC / identity |
| `ERR-FRD-*` | Fraud / risk decisions |
| `ERR-SYS-*` | Infrastructure / platform |

**Three-tier translation table (sample):**

| T24 OFS / NAPAS raw | Domain error | HTTP status | Error code | gRPC status |
|---|---|---|---|---|
| `OVERDUE.FUNDS` | `InsufficientFundsError` | 422 | `ERR-PAY-001` | `FAILED_PRECONDITION` |
| `INVALID.ACCOUNT` | `AccountNotFoundError` | 404 | `ERR-ACC-001` | `NOT_FOUND` |
| NAPAS timeout | `PaymentGatewayTimeout` | 503 | `ERR-PAY-002` | `UNAVAILABLE` |
| Fraud DECLINE | `TransactionDeclined` | 422 | `ERR-FRD-001` | `PERMISSION_DENIED` |
| OFS parse failure | `UpstreamProtocolError` | 502 | `ERR-SYS-001` | `INTERNAL` |

**RFC 9457 Problem Details** wire format for all HTTP error responses:
```json
{
  "type": "https://errors.techcombank.com/ERR-PAY-001",
  "title": "Insufficient Funds",
  "status": 422,
  "detail": "Account balance insufficient for this transaction.",
  "instance": "/payments/TCB-2026-001234",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736"
}
```

**`ErrorTranslator` interface:** each ACL (anti-corruption layer, e.g., T24 OFS bridge) must implement `ErrorTranslator<RawError, DomainError>` — ensures every integration point has an explicit, tested translation.

**Protocol coverage:**
- HTTP → RFC 9457 Problem Details
- gRPC → `Status` code + `ErrorInfo` `google.rpc.ErrorInfo` detail
- WebSocket → STOMP ERROR frame with `error-code` header
- Kafka / async → `CloudEvents` `dataerror` extension attribute; dead-letter with error envelope

**Mobile client contract:** error codes are stable across API versions; the Techcombank Developer Portal publishes the full error catalogue; mobile SDKs map `ERR-*` codes to localised user messages.

**Compliance:** BCBS 239 §6 Accuracy, PCI-DSS §6.5 (input validation / error handling), SBV Circular 09/2020 §IV.3 ⚠️ (working summary — pending Legal review).

---

## 6. PRIN-001 Enhancement — OpenAPI 3.2

**File:** `knowledge-base/principles/api-first-design.md`
**Change:** Add `### OpenAPI 3.2 Additions` subsection to the existing `## Implementation Guidelines` section.

Three meaningful 3.2 changes for Techcombank:

1. **Webhooks** (`webhooks` object) — used for open banking PSD2 callbacks (REF-011); replaces the informal `x-webhooks` extension previously used. Example: `paymentStatusCallback` webhook in the payment gateway OpenAPI spec.

2. **JSON Schema 2020-12 alignment** — removes prior `nullable`, `exclusiveMinimum` inconsistencies; relevant for strict schema validation in compliance flows (KYC document schema, payment instruction validation). Teams must migrate from `nullable: true` to `type: [string, null]` syntax.

3. **`pathItem` `$ref`** — reuse path definitions across specs; enables sharing the `/payments/{paymentId}` path between the internal payment service spec and the partner-facing open banking spec without duplication.

No new Catalog ID; no new file. In-place addition of ~40 lines to PRIN-001.

---

## 7. Catalog ID Registry (new entries)

| Catalog ID | File | Domain | Status after Wave |
|---|---|---|---|
| OBS-001 | patterns/observability/otel-instrumentation.md | Observability | Draft |
| OBS-002 | patterns/observability/distributed-trace-propagation.md | Observability | Draft |
| OBS-003 | patterns/observability/structured-logging-standard.md | Observability | Draft |
| OBS-004 | patterns/observability/slo-alerting.md | Observability | Draft |
| OBS-005 | patterns/observability/async-middleware-observability.md | Observability | Draft |
| PLT-001 | patterns/platform/service-mesh-traffic.md | Platform | Draft |
| PLT-002 | patterns/platform/cncf-stack-selection.md | Platform | Draft |
| INT-010 | patterns/integration/asyncapi-specification.md | Integration | Draft |
| INT-011 | patterns/integration/cloudevents-envelope.md | Integration | Draft |
| INT-012 | patterns/integration/error-code-mapping.md | Integration | Draft |
| PRIN-001 | principles/api-first-design.md | Principles | Approved (enhanced) |

**Total new entries:** 10 new Catalog IDs + 1 in-place enhancement.

---

## 8. Implementation Wave Plan

These 10 new docs map to a single new wave (**Wave 2a**) using the same parallel-agent approach as Waves 1a/b/c:

| Group | Files | Agent |
|---|---|---|
| Group 1 | OBS-001, OBS-002, OBS-003 | backend-engineer (observability focus) |
| Group 2 | OBS-004, OBS-005 | backend-engineer |
| Group 3 | PLT-001, PLT-002 | backend-engineer (platform focus) |
| Group 4 | INT-010, INT-011, INT-012 + PRIN-001 enhancement | backend-engineer |

Each document: full ops-runbook depth (300–450 lines), Mermaid diagrams, Java 21/Spring Boot 3.x + OTEL SDK code, 3-ring compliance mapping, NFR-AC YAML, STRIDE threat model, runbook, test strategy.

OTEL backends in all code samples: Grafana Tempo, Dynatrace, OpenSearch (via Data Prepper).

---

## 9. Compliance Coverage (new entries)

All new patterns carry 3-ring compliance mapping. Notable additions:
- **OBS-003** adds Decree 13/2023 Art. 9 (PII in logs must be masked before cross-system transit)
- **INT-012** adds PCI-DSS §6.5 (error handling must not leak internal state)
- **PLT-002** adds BCBS 230 Principle 7 (ICT and Cyber Security — governed tool selection)

All Ring 2 (Vietnam) cells carry `⚠️ (working summary — pending Legal review)`.
All BCBS 230 cells carry `⚠️ (working summary — pending PDF fetch)`.

---

## 10. Out of Scope (deferred)

The following were considered and explicitly deferred:

| Topic | Reason deferred |
|---|---|
| OpenTelemetry for mobile (iOS Swift, Android Kotlin) | Covered in MOB-* wave when mobile patterns are authored |
| Dynatrace-specific DQL query library | Operational tooling, not architecture pattern |
| OpenSearch dashboards / index templates | DevSecOps artefact; not a catalog pattern |
| API gateway OTEL integration (Envoy / Kong) | Covered indirectly by PLT-001 mesh telemetry |
| QUIC / HTTP/3 full implementation guide | Protocol support still maturing; noted in OBS-002 table, deep-dive deferred |

---

**Key Takeaway:** Wave 2a adds 10 new patterns across two new catalog domains (Observability, Platform) and three integration extensions, closing the gaps in OTEL instrumentation, cross-protocol trace propagation, structured logging, SLO alerting, async middleware visibility, full service mesh traffic management, CNCF stack governance, AsyncAPI/CloudEvents contracts, and error code standardisation.
