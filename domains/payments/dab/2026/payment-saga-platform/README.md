# Payment SAGA Platform

| Property | Value |
|----------|-------|
| **Project Name** | Payment SAGA Platform |
| **Status** | In Review |
| **Submitted** | 2026-01-15 |
| **SA Lead** | @architect-lead |
| **Project Manager** | @payments-pm |
| **Team Lead** | @payments-engineer |
| **Domain** | Payments |
| **Repository** | https://github.com/techcombank/payment-saga-platform |

---

## Executive Summary

The Payment SAGA Platform is a strategic initiative to unify payment transaction orchestration across Techcombank using the SAGA distributed transaction pattern. This will replace current ad-hoc payment processing with a standardized, resilient, and auditable payment orchestration system.

### Key Objectives

1. **Eliminate Payment Failures** — Implement saga compensation logic to handle partial failures
2. **Improve Auditability** — Create immutable transaction history for regulatory compliance
3. **Reduce Manual Intervention** — Automate payment exception handling
4. **Enable New Payment Types** — Support complex multi-step payment scenarios
5. **Improve Developer Experience** — Standardize payment development patterns

### Expected Impact

- **Reduction in Manual Breaks**: 95% → 5% (90% improvement)
- **Payment Processing Latency**: 3.5s → 2.0s (43% reduction)
- **Development Time for New Flows**: 4 weeks → 1 week
- **System Availability**: 99.90% → 99.95%

---

## Problem Statement

### Current State

The current payment system processes payments through loosely coordinated services:

1. Core Banking debit occurs
2. Payment Network routing happens separately
3. Core Banking credit happens in a batch
4. Reconciliation is manual and error-prone

**Issues**:
- Partial failures leave accounts in inconsistent state
- Manual reconciliation identifies breaks days later
- Complex payment types (multi-leg, conditional) are not supported
- Audit trail is fragmented across multiple systems
- Recovery from failures requires manual intervention

### Example: Current Failure Scenario

```
1. Debit account → SUCCESS
2. Route to NAPAS → TIMEOUT
3. Retry routing → FAIL (network down)
4. System hangs; human must investigate
   - Was payment sent to NAPAS? (unknown)
   - Should we debit the account again? (manual check)
   - How to recover? (manual process)
```

---

## Proposed Solution

### SAGA Orchestration Pattern

Use Temporal workflow engine to orchestrate distributed payment transactions:

1. **Temporal Orchestrator** coordinates all steps
2. **Activities** represent individual service calls (debit, route, credit)
3. **Compensation** logic handles failures and reversals
4. **Idempotency** prevents duplicate processing
5. **Event Log** creates immutable audit trail

### Example: Proposed Flow with SAGA

```
Payment Saga (orchestrated by Temporal)
├─ Activity 1: Debit Account [RUNNING]
│  └─ Success → Continue
├─ Activity 2: Route to Payment Network [RUNNING]
│  ├─ Timeout → Retry 3x
│  └─ Fail → Trigger Compensation
├─ Compensation 1: Reverse Debit [RUNNING]
│  └─ Success → Mark transaction as failed
└─ Complete

Outcome: Payment marked as "FAILED_AND_REVERSED"
Account State: Consistent (no funds deducted)
Audit Trail: Complete history of all steps and reversals
```

---

## Scope

### In Scope

- Domestic transfers (NAPAS)
- International transfers (SWIFT)
- QR code payments (VietQR)
- Card payments (Stripe/PayPal)
- Fee calculation and GL posting
- Reconciliation integration

### Out of Scope

- Customer notification system (Digital Channels domain)
- Fraud detection (Risk Management domain)
- Account master data (Core Banking domain)

---

## Architecture

### High-Level Design

```
┌─────────────────────────────────────────────────────────────┐
│                   Payment Initiation                        │
│                   (Mobile/Web/API)                          │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────┐
│                  Payment API (Spring Boot 3)                │
│             - Validate payment request                      │
│             - Start Temporal workflow                       │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ↓
┌─────────────────────────────────────────────────────────────┐
│              Temporal Workflow Orchestrator                  │
│                                                             │
│  PaymentWorkflow {                                          │
│    1. debitAccount() → Core Banking                         │
│    2. validateFraud() → Risk Management                     │
│    3. routePayment() → Payment Network                      │
│    4. creditAccount() → Core Banking                        │
│    5. calculateFee() → Payments Service                     │
│    6. notifyCustomer() → Digital Channels                   │
│  }                                                          │
└──────────────────────┬──────────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┬──────────────┐
        ↓              ↓              ↓              ↓
┌──────────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐
│   Core       │ │  Risk    │ │ Payment  │ │   Digital    │
│  Banking     │ │Management│ │ Networks │ │  Channels    │
│  (T24)       │ │          │ │ (NAPAS)  │ │              │
└──────────────┘ └──────────┘ └──────────┘ └──────────────┘
```

### Technology Stack

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| **Workflow Orchestration** | Temporal | Distributed transaction semantics, strong consistency |
| **API Framework** | Spring Boot 3.2 | Standard at Techcombank, native GraalVM support |
| **Event Streaming** | Kafka | Payment events, audit trail, downstream analytics |
| **Database** | PostgreSQL 15 | ACID compliance, JSON support for payment metadata |
| **Deployment** | Kubernetes (EKS) | Cloud-native, auto-scaling, high availability |
| **Monitoring** | DataDog + OpenTelemetry | Distributed tracing for saga debugging |

---

## Implementation Timeline

### Phase 1: Foundation (Q1 2026)
- [ ] Set up Temporal cluster (AWS managed)
- [ ] Design PaymentWorkflow interface
- [ ] Implement core workflow activities (debit, credit, route)
- [ ] Build Temporal dashboard and monitoring
- **Deliverable**: Basic payment workflow POC
- **Duration**: 6 weeks

### Phase 2: Integration (Q2 2026)
- [ ] Integrate with Core Banking service
- [ ] Integrate with Risk Management service
- [ ] Integrate with Payment Networks
- [ ] Implement compensation logic
- [ ] Implement idempotency key tracking
- **Deliverable**: End-to-end workflow for domestic transfers
- **Duration**: 8 weeks

### Phase 3: Migration (Q3 2026)
- [ ] Migrate domestic transfers to SAGA
- [ ] Migrate international transfers to SAGA
- [ ] Migrate QR payments to SAGA
- [ ] Parallel run (old and new systems) for 2 weeks
- [ ] Cutover to SAGA-only
- **Deliverable**: Production rollout for all payment types
- **Duration**: 10 weeks

### Phase 4: Optimization (Q4 2026)
- [ ] Performance tuning and optimization
- [ ] Cost analysis and reduction
- [ ] Automation of manual exception handling
- [ ] Advanced workflow patterns (conditional routing, etc.)
- **Deliverable**: Optimized production system
- **Duration**: Ongoing

---

## Risk Assessment

### High Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| Temporal cluster performance at scale (100K+/min) | Medium | High | Load testing in Q1; redundant clusters across AZs |
| Saga compensation logic bugs | Medium | High | Comprehensive unit + integration tests; chaos engineering |
| Integration delays with Core Banking | Medium | High | Early API design; shared integration testing |

### Medium Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|-----------|
| Developer learning curve (new pattern) | Medium | Medium | Training workshops; detailed documentation |
| Network latency affecting saga duration | Low | Medium | Caching; request batching; optimized retry logic |

### Mitigation Strategy

- **Testing**: Unit tests (90%+ coverage), integration tests, chaos engineering
- **Rollback Plan**: Maintain old system in parallel for 2 weeks; quick rollback if issues
- **Communication**: Weekly architecture reviews; monthly stakeholder updates
- **Monitoring**: Real-time dashboards tracking workflow success rates and latency

---

## Success Criteria

| Metric | Current | Target | Timeline |
|--------|---------|--------|----------|
| Payment Processing Latency (p99) | 3.5s | 2.0s | Q3 2026 |
| System Availability | 99.90% | 99.95% | Q3 2026 |
| Manual Break Rate | 0.05% (volume) | < 0.01% | Q3 2026 |
| Saga Compensation Success Rate | N/A | > 99% | Q3 2026 |
| Developer Onboarding Time | 3 weeks | 1 week | Q2 2026 |

---

## Budget and Resources

### Team Composition

- **1 Principal Engineer** — Architecture and design
- **2 Senior Engineers** — Temporal implementation and integration
- **2 Mid-level Engineers** — Service integration and testing
- **1 QA Engineer** — Test strategy and automation
- **1 DevOps Engineer** — Cluster deployment and monitoring

Total: **7 FTE** over 12 months

### Budget Estimate

| Item | Cost | Notes |
|------|------|-------|
| Team Salaries | $900K | 7 FTE @ $130K average |
| Temporal Cloud | $80K | Fully managed service |
| AWS Infrastructure | $150K | EKS, storage, networking |
| Tools & Licenses | $50K | Monitoring, testing tools |
| **Total** | **$1.18M** | 12-month project |

---

## Deliverables

### Code & Configuration

- [ ] `payment-saga-platform/` — Core SAGA library
- [ ] Temporal workflow definitions (TypeScript)
- [ ] Activity implementations (Spring Boot)
- [ ] Integration tests and chaos tests
- [ ] Terraform/Helm charts for deployment

### Documentation

- [ ] Architecture design document
- [ ] Workflow design document
- [ ] Integration guide (how to add new payment types)
- [ ] Operational runbook (troubleshooting guide)
- [ ] ADRs (Architectural Decision Records):
  - [ ] ADR-001: SAGA vs 2PC
  - [ ] ADR-002: Temporal vs Camunda

### Training & Knowledge Transfer

- [ ] Temporal workshop (internal)
- [ ] Architecture review sessions
- [ ] Troubleshooting guide and runbooks
- [ ] Knowledge base articles

---

## Related ADRs

- [`decisions/ADR-001-saga-vs-2pc.md`](./decisions/ADR-001-saga-vs-2pc.md) — Why we chose SAGA over 2PC
- [`decisions/ADR-002-temporal-over-camunda.md`](./decisions/ADR-002-temporal-over-camunda.md) — Why we chose Temporal over Camunda

---

## Governance and Sign-Off

### Approval Status

- [ ] Architecture Review Board — **Pending**
- [ ] Business Stakeholder (VP Payments) — **Pending**
- [ ] InfoSec (security review) — **Pending**
- [ ] Compliance (regulatory review) — **Pending**

### Review Schedule

- **Architecture Review**: March 15, 2026 (tentative)
- **Security Review**: March 22, 2026 (tentative)
- **Final Approval**: March 29, 2026 (target)

### Next Steps

1. Address review comments from architecture team
2. Finalize security and compliance requirements
3. Obtain sign-offs from stakeholders
4. Kick off Phase 1 (Foundation) in early April 2026

---

## Merge Request

A draft MR will be available at: **[MR Link - TBD]**

Comments and suggestions welcome on:
- Architecture decisions
- Technology choices
- Risk mitigation strategies
- Timeline feasibility
- Budget assumptions

---

## See Also

- [Payments Domain README](../../README.md)
- [Technology Radar - SAGA & Temporal](../../technology-radar.md)
- [Payment Flow Template](../../shared/diagrams/payment-flow-template.md)
- [Temporal Documentation](https://docs.temporal.io)

---

Last Updated: March 8, 2026 | Project Status: In Review
Next Review: March 15, 2026
