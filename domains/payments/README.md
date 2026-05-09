# Payments Domain

| Property | Value |
|----------|-------|
| **Domain** | Payments |
| **Team** | Payment Technology |
| **Domain Lead** | @payments-lead |
| **Last Updated** | 2026-03-08 |
| **Slack Channel** | #payments-domain |

## Overview

The Payments domain is responsible for all payment processing across Techcombank, including domestic transfers, international remittances, card payments, and integration with major Vietnamese payment networks. This domain handles the critical path for transaction initiation, routing, processing, and reconciliation.

### Key Responsibilities

- **Payment Initiation**: Accept payment requests from internal and external channels
- **Payment Processing**: Execute payment transactions with real-time processing
- **Payment Routing**: Route payments through appropriate networks (NAPAS, SWIFT, VietQR)
- **Reconciliation**: Reconcile transactions with external networks and internal ledgers
- **Fraud Detection**: Screen payments for suspicious activities in real-time
- **Notification**: Deliver payment confirmations and status updates to customers

## Architecture

This domain integrates with:

- **Core Banking Domain** — Account master, GL posting, customer master
- **Risk Management Domain** — Fraud screening, transaction limits, AML checks
- **Data Platform Domain** — Payment analytics, reporting, audit logs
- **External Payments Services Providers** — Stripe, PayPal (for card processing)
- **National Payment Networks** — NAPAS, SWIFT, VietQR

### Related Folders

- [`./domain-model.md`](./domain-model.md) — Business capability map
- [`./context-map.md`](./context-map.md) — System context diagram
- [`./technology-radar.md`](./technology-radar.md) — Technology decisions
- [`./shared/`](./shared/) — Shared payment glossary, error codes, diagram templates
- [`./dab/`](./dab/) — Domain Architecture Board projects

## Current Projects

| Project | Status | Lead | Updated |
|---------|--------|------|---------|
| Payment SAGA Platform | In Review | @architect-lead | 2026-01-15 |
| Domestic Transfer v3 | Approved | @architect-lead | 2025-06-15 |

See [`dab/2026/`](./dab/2026/) for active projects and [`dab/2025/`](./dab/2025/) for archived work.

## Technology Stack

**Recommended Technologies:**
- **Workflow Orchestration**: Temporal (Adopted)
- **Message Streaming**: Kafka (Adopted)
- **Framework**: Spring Boot 3.x (Adopted)
- **Internal Communication**: gRPC (Trial)
- **Process Automation**: Dapr (Assess)

See [`technology-radar.md`](./technology-radar.md) for full technology decisions.

## Domain Model

The domain is organized around these core business capabilities:

```
Payment Initiation
├── Channel Intake
├── Request Validation
└── Customer Authentication

Payment Routing
├── NAPAS Routing
├── SWIFT Routing
└── VietQR Routing

Payment Processing
├── Debit Processing
├── Credit Processing
└── Fee Calculation

Reconciliation
├── Network Reconciliation
├── GL Reconciliation
└── Settlement

Fraud Screening
├── Real-time Detection
├── Risk Scoring
└── Alert Management

Notification
├── Customer Notification
├── Internal Notification
└── Regulatory Reporting
```

See [`domain-model.md`](./domain-model.md) for detailed capability definitions.

## Key Contacts

- **Domain Lead**: @payments-lead — Domain strategy and roadmap
- **Architecture**: @architect-lead — Technical decisions and ADRs
- **Lead Engineer**: @payments-engineer — Team management and execution
- **Product Manager**: @payments-pm — Feature prioritization and requirements

## Confluence Links

- [Payment Processing Policies](https://confluence.techcombank.io/payments)
- [NAPAS Integration Guide](https://confluence.techcombank.io/napas-integration)
- [SBV Circular 64 Compliance](https://confluence.techcombank.io/sbv-circular-64)
- [Payment Testing Matrix](https://confluence.techcombank.io/payment-testing)

## Related Domains

- [Core Banking Domain](../core-banking/README.md)
- [Risk Management Domain](../risk-management/README.md)
- [Data Platform Domain](../data-platform/README.md)

---

Last Updated: March 8, 2026 | Team: Payment Technology
