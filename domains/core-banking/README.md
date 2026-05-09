# Core Banking Domain

| Property | Value |
|----------|-------|
| **Domain** | Core Banking |
| **Team** | Core Banking Technology |
| **Domain Lead** | @core-banking-lead |
| **Last Updated** | 2026-03-08 |
| **Slack Channel** | #core-banking-domain |

---

## Overview

The Core Banking domain is the foundational system providing account management, customer master data, and general ledger functionality for Techcombank. This domain integrates with Temenos T24/Transact and manages critical banking data used by all other domains.

### Key Responsibilities

- **Account Management**: Creation, maintenance, and lifecycle management of customer accounts
- **Customer Master**: Centralized customer identity and profile data
- **General Ledger**: Financial accounting and GL reconciliation
- **Product Catalog**: Definition and management of banking products
- **Interest Calculation**: Accrual and posting of interest income
- **Statement Generation**: Monthly and ad-hoc account statements

## Architecture

This domain integrates with:

- **Payments Domain** — Account debit/credit, fee posting
- **Lending Domain** — Account-level limits, borrowing limits
- **Digital Channels Domain** — Account display, transaction inquiry
- **Data Platform Domain** — GL and account data replication
- **Temenos T24/Transact** — Core banking ledger system

### Related Folders

- [`./domain-model.md`](./domain-model.md) — Business capability map
- [`./context-map.md`](./context-map.md) — System context diagram
- [`./shared/`](./shared/) — Shared banking glossary

## Current Projects

| Project | Status | Lead | Updated |
|---------|--------|------|---------|
| T24 Modernization | In Review | @architect-lead | 2026-01-20 |
| Account Opening v2 | Draft | @product-manager | 2026-02-01 |

See [`dab/2026/`](./dab/2026/) for active projects.

## Technology Stack

**Current Technologies:**
- **Core System**: Temenos T24/Transact R23
- **APIs**: REST (Spring Boot), SOAP (legacy)
- **Database**: Oracle (T24 repository), PostgreSQL (APIs)
- **Messaging**: Kafka (event streaming)

## Key Contacts

- **Domain Lead**: @core-banking-lead — Domain strategy
- **Architecture**: @architect-lead — Technical decisions
- **Lead Engineer**: @core-banking-engineer — Team management
- **Product Manager**: @core-banking-pm — Roadmap and priorities

## Confluence Links

- [Core Banking Policies](https://confluence.techcombank.io/core-banking)
- [T24 Configuration Guide](https://confluence.techcombank.io/t24-config)
- [Account Opening Procedures](https://confluence.techcombank.io/account-opening)

## Related Domains

- [Payments Domain](../payments/README.md)
- [Lending Domain](../lending/README.md)
- [Digital Channels Domain](../digital-channels/README.md)

---

Last Updated: March 8, 2026 | Team: Core Banking Technology
