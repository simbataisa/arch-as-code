# Lending Domain

| Property | Value |
|----------|-------|
| **Domain** | Lending |
| **Team** | Lending Technology |
| **Domain Lead** | @lending-lead |
| **Last Updated** | 2026-03-08 |
| **Slack Channel** | #lending-domain |

---

## Overview

The Lending domain manages all aspects of loan products and services at Techcombank, including loan origination, credit decisioning, loan servicing, collections, and limit management. This domain supports retail consumer loans, SME business loans, and corporate credit facilities.

### Key Responsibilities

- **Loan Origination** — End-to-end loan application and approval process
- **Credit Decisioning** — Automated and manual credit approval decisions
- **Loan Servicing** — Loan management, payment processing, account maintenance
- **Collections** — Arrear management and collections workflows
- **Limit Management** — Credit line management and renewal

## Architecture

This domain integrates with:

- **Core Banking Domain** — Account management, GL posting, customer master
- **Payments Domain** — Loan disbursement, repayment collection
- **Risk Management Domain** — Credit risk assessment, credit limits
- **Digital Channels Domain** — Loan application interface, document submission
- **Data Platform Domain** — Portfolio analytics and reporting

### Related Folders

- [`./domain-model.md`](./domain-model.md) — Business capability map
- [`./shared/`](./shared/) — Shared lending policies and procedures

## Current Projects

| Project | Status | Lead | Updated |
|---------|--------|------|---------|
| Auto Loan Origination | Draft | @product-manager | 2026-02-20 |

See [`dab/2026/`](./dab/2026/) for active projects.

## Technology Stack

**Current Technologies:**
- **Loan Origination System**: Fiserv CoreLend
- **Credit Decisioning**: Custom rule engine + credit scoring
- **Database**: Oracle (loan ledger), PostgreSQL (APIs)
- **Workflow**: Business process management (BPMN-based)
- **Reporting**: Tableau, custom dashboards

## Key Contacts

- **Domain Lead**: @lending-lead — Lending strategy and product direction
- **Architecture**: @architect-lead — Technical decisions
- **Lead Engineer**: @lending-engineer — Team management
- **Product Manager**: @lending-pm — Feature prioritization
- **Credit Manager**: @credit-manager — Credit policy oversight

## Confluence Links

- [Lending Policies and Procedures](https://confluence.techcombank.io/lending-policies)
- [Credit Scoring Model](https://confluence.techcombank.io/credit-scoring)
- [Loan Product Catalog](https://confluence.techcombank.io/loan-products)

## Related Domains

- [Core Banking Domain](../core-banking/README.md)
- [Payments Domain](../payments/README.md)
- [Risk Management Domain](../risk-management/README.md)

---

Last Updated: March 8, 2026 | Team: Lending Technology
