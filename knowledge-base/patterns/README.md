# Design Patterns

Proven, reusable solutions to recurring architecture problems, organised into ten
categories. Each pattern follows the standard template (Status, Last Reviewed, Owner,
Problem Statement, Solution) — see
[the knowledge base's contributing guide](../README.md#contributing-new-patternsprinciples)
for how to propose a new one.

## Integration Patterns

Cross-service communication, legacy bridges, sagas, outbox.

- [Anti-Corruption Layer](./integration/anti-corruption-layer.md)
- [API Contract Testing](./integration/api-contract-testing.md)
- [API Gateway Pattern](./integration/api-gateway-routing.md)
- [AsyncAPI Specification](./integration/asyncapi-specification.md)
- [Backend-for-Frontend Routing](./integration/backend-for-frontend-routing.md)
- [Change Data Capture (CDC) with Outbox Pattern](./integration/cdc-outbox-pattern.md)
- [CloudEvents Envelope Standard](./integration/cloudevents-envelope.md)
- [Content-Based Router](./integration/content-based-router.md)
- [Distributed Saga Choreography](./integration/distributed-saga-choreography.md)
- [Error Code Mapping and Propagation](./integration/error-code-mapping.md)
- [Event Sourcing Pattern](./integration/event-sourcing.md)
- [Message Sequencer](./integration/message-sequencer.md)
- [SAGA Orchestration Pattern](./integration/saga-orchestration.md)
- [Schema Registry Governance](./integration/schema-registry-governance.md)
- [Sidecar / Ambassador](./integration/sidecar-ambassador.md)
- [Strangler Fig](./integration/strangler-fig.md)
- [Webhook Delivery Reliability](./integration/webhook-delivery-reliability.md)

## Security Patterns

Authentication, authorization, secrets, tokenisation, BFF.

- [Attribute-Based Access Control](./security/attribute-based-access-control.md)
- [Tamper-Evident Audit Logging](./security/audit-logging-tamper-evident.md)
- [BFF + Token-Binding (Web, iOS, Android)](./security/bff-token-binding.md)
- [Data Masking](./security/data-masking.md)
- [Fraud Signal Collection](./security/fraud-signal-collection.md)
- [JWT Best Practices](./security/jwt-best-practices.md)
- [mTLS via Service Mesh (Istio) Pattern](./security/mtls-service-mesh.md)
- [OAuth2 & OIDC Authorization Pattern](./security/oauth2-authorization.md)
- [PII Tokenization (Format-Preserving)](./security/pii-tokenization-format-preserving.md)
- [Secrets Rotation](./security/secrets-rotation.md)
- [Session Revocation](./security/session-revocation.md)
- [Tokenization + HSM Key Management](./security/tokenization-hsm.md)
- [HashiCorp Vault for Secrets Management Pattern](./security/vault-secret-management.md)

## Data Patterns

Data modeling, CQRS, data mesh, lineage, CDC.

- [Change Data Capture (General)](./data/change-data-capture.md)
- [CQRS (Command Query Responsibility Segregation) Pattern](./data/cqrs-pattern.md)
- [Data Lineage](./data/data-lineage.md)
- [Data Mesh Pattern](./data/data-mesh-ownership.md)
- [Data Quality Rules](./data/data-quality-rules.md)
- [Data Vault 2.0](./data/data-vault-2.md)
- [Data Virtualization](./data/data-virtualization.md)
- [Kappa Architecture](./data/kappa-architecture.md)
- [Lambda Architecture](./data/lambda-architecture.md)
- [Reference Data Master](./data/reference-data-master.md)
- [Slowly Changing Dimensions (SCD Type 2)](./data/slowly-changing-dimensions.md)
- [Temporal Tables (Versioned Tables) Pattern](./data/temporal-tables.md)
- [Time-Series Modelling](./data/time-series-modelling.md)

## Resilience Patterns

Fault tolerance, cell-based architecture, circuit breakers, throttling.

- [Bulkhead Isolation Pattern](./resilience/bulkhead-isolation.md)
- [Cell-Based Architecture](./resilience/cell-based-architecture.md)
- [Circuit Breaker Pattern](./resilience/circuit-breaker.md)
- [Fallback Strategies](./resilience/fallback-strategies.md)
- [Graceful Degradation](./resilience/graceful-degradation.md)
- [Health Check Aggregation](./resilience/health-check-aggregation.md)
- [Leader Election](./resilience/leader-election.md)
- [Load Shedding](./resilience/load-shedding.md)
- [Queue-Based Load Levelling](./resilience/queue-based-load-levelling.md)
- [Retry with Exponential Backoff Pattern](./resilience/retry-with-backoff.md)
- [Throttling / Rate Limiting](./resilience/throttling-rate-limiting.md)
- [Timeout Budget](./resilience/timeout-budget.md)

## EIP Patterns

Banking-relevant Hohpe/Woolf Enterprise Integration Patterns subset (25 of 65).

- [Aggregator](./eip/aggregator.md)
- [Channel Purger](./eip/channel-purger.md)
- [Claim Check](./eip/claim-check.md)
- [Composed Message Processor](./eip/composed-message-processor.md)
- [Content-Based Router](./eip/content-based-router.md)
- [Content Enricher](./eip/content-enricher.md)
- [Content Filter](./eip/content-filter.md)
- [Dead Letter Channel](./eip/dead-letter-channel.md)
- [Durable Subscriber](./eip/durable-subscriber.md)
- [Guaranteed Delivery](./eip/guaranteed-delivery.md)
- [Idempotent Receiver](./eip/idempotent-receiver.md)
- [Message Channel](./eip/message-channel.md)
- [Message Router](./eip/message-router.md)
- [Message Store](./eip/message-store.md)
- [Message Translator](./eip/message-translator.md)
- [Normalizer](./eip/normalizer.md)
- [Point-to-Point Channel](./eip/point-to-point-channel.md)
- [Process Manager](./eip/process-manager.md)
- [Publish-Subscribe Channel](./eip/publish-subscribe-channel.md)
- [Resequencer](./eip/resequencer.md)
- [Routing Slip](./eip/routing-slip.md)
- [Scatter-Gather](./eip/scatter-gather.md)
- [Smart Proxy](./eip/smart-proxy.md)
- [Splitter](./eip/splitter.md)
- [Test Message](./eip/test-message.md)

## Frontend Patterns

Web (React+TS) performance budgets, offline-first, CSP, error boundary.

- [Web CSP Hardening](./frontend/web-csp-hardening.md)
- [Web Error Boundary](./frontend/web-error-boundary.md)
- [Web Feature Flags](./frontend/web-feature-flags.md)
- [Web i18n / RTL](./frontend/web-i18n-rtl.md)
- [Web Performance Budgets](./frontend/web-performance-budgets.md)
- [Web Resilience / Offline-First](./frontend/web-resilience-offline-first.md)

## Mobile Patterns

Native iOS/Android offline queue, secure storage, biometric auth, deep-link attestation.

- [Mobile Biometric Authentication](./mobile/mobile-biometric-auth.md)
- [Mobile Deep Link Attestation](./mobile/mobile-deep-link-attestation.md)
- [Mobile Force-Upgrade](./mobile/mobile-force-upgrade.md)
- [Mobile Offline Queue](./mobile/mobile-offline-queue.md)
- [Mobile Push Notification (Secure)](./mobile/mobile-push-notification-secure.md)
- [Mobile Secure Storage](./mobile/mobile-secure-storage.md)

## Banking Solutions Patterns

Atomic banking building blocks: ledgers, engines, and screening pipelines.

- [Accrual Engine](./banking-solutions/accrual-engine.md)
- [Collateral Management Engine](./banking-solutions/collateral-management-engine.md)
- [Collections Engine](./banking-solutions/collections-engine.md)
- [Credit Limit Engine](./banking-solutions/credit-limit-engine.md)
- [Double-Entry Ledger](./banking-solutions/double-entry-ledger.md)
- [End-of-Day Batch Window](./banking-solutions/end-of-day-batch-window.md)
- [Fee Engine](./banking-solutions/fee-engine.md)
- [FX Rate Engine](./banking-solutions/fx-rate-engine.md)
- [Idempotent Payment Key](./banking-solutions/idempotent-payment-key.md)
- [Interest Calculation Engine](./banking-solutions/interest-calculation-engine.md)
- [Position Keeping Engine](./banking-solutions/position-keeping-engine.md)
- [Pricing Engine](./banking-solutions/pricing-engine.md)
- [Product Factory](./banking-solutions/product-factory.md)
- [Relationship Pricing Engine](./banking-solutions/relationship-pricing-engine.md)
- [Reversal and Chargeback](./banking-solutions/reversal-and-chargeback.md)
- [Rule / Decisioning Engine](./banking-solutions/rule-decisioning-engine.md)
- [Sanction Screening Pipeline](./banking-solutions/sanction-screening-pipeline.md)
- [Settlement Engine](./banking-solutions/settlement-engine.md)
- [Tax Calculation Engine](./banking-solutions/tax-calculation-engine.md)
- [Transaction Limit Engine](./banking-solutions/transaction-limit-engine.md)

## Observability Patterns

Tracing, structured logging, metrics, SLOs, and synthetic monitoring.

- [Async Middleware Observability](./observability/async-middleware-observability.md)
- [Distributed Trace Propagation](./observability/distributed-trace-propagation.md)
- [Error Budget Burn Rate Alerting](./observability/error-budget-burn-rate.md)
- [Log Aggregation Pipeline](./observability/log-aggregation-pipeline.md)
- [Metrics Cardinality Management](./observability/metrics-cardinality-management.md)
- [OpenTelemetry Instrumentation](./observability/otel-instrumentation.md)
- [SLO Alerting](./observability/slo-alerting.md)
- [Structured Logging Standard](./observability/structured-logging-standard.md)
- [Synthetic Monitoring and Canary Probes](./observability/synthetic-monitoring-canary.md)
- [Distributed Tracing Sampling Strategy](./observability/tracing-sampling-strategy.md)

## Platform Patterns

Platform engineering: Kubernetes, GitOps, multi-tenancy, FinOps.

- [CNCF Stack Selection](./platform/cncf-stack-selection.md)
- [FinOps Cost Allocation](./platform/finops-cost-allocation.md)
- [GitOps Deployment Pipeline](./platform/gitops-deployment-pipeline.md)
- [Internal Developer Platform](./platform/internal-developer-platform.md)
- [Kubernetes Operator Pattern](./platform/kubernetes-operator-pattern.md)
- [Multi-Tenancy Isolation](./platform/multi-tenancy-isolation.md)
- [Platform Service Catalog](./platform/platform-service-catalog.md)
- [Service Mesh Traffic Management](./platform/service-mesh-traffic.md)

---

> **Master catalog**: every pattern above is indexed by Catalog ID in
> [`governance/standards/enterprise-architecture-catalog.md`](../../governance/standards/enterprise-architecture-catalog.md).
> Cite by Catalog ID (e.g., `RES-005`, `EIP-024`) in DAB submissions.
