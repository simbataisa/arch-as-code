# Risk Management Domain

| Property | Value |
|----------|-------|
| **Domain** | Risk Management |
| **Team** | Risk Technology |
| **Domain Lead** | @risk-lead |
| **Last Updated** | 2026-03-08 |
| **Slack Channel** | #risk-management-domain |

---

## Overview

The Risk Management domain is responsible for identifying, assessing, and mitigating risks across Techcombank's operations. This includes fraud detection, credit risk assessment, AML/KYC compliance, and regulatory reporting to the State Bank of Vietnam.

### Key Responsibilities

- **Fraud Detection** — Real-time detection of fraudulent transactions
- **Credit Risk** — Assessment of customer credit worthiness and loan risk
- **AML/KYC** — Anti-money laundering and Know Your Customer verification
- **Regulatory Reporting** — Compliance with SBV and government regulations
- **Transaction Monitoring** — Continuous monitoring for suspicious patterns

## Architecture

This domain integrates with:

- **Payments Domain** — Fraud screening for transactions
- **Lending Domain** — Credit risk scoring for loan decisions
- **Core Banking Domain** — Customer master, transaction data
- **Data Platform Domain** — Risk analytics and monitoring

### Related Folders

- [`./domain-model.md`](./domain-model.md) — Business capability map
- [`./shared/`](./shared/) — Shared risk policies and procedures

## Current Projects

| Project | Status | Lead | Updated |
|---------|--------|------|---------|
| ML-based Fraud Detection | In Review | @architect-lead | 2026-01-25 |

See [`dab/2026/`](./dab/2026/) for active projects.

## Technology Stack

**Current Technologies:**
- **Rules Engine**: Drools (rule-based fraud detection)
- **ML Platform**: TensorFlow, Python
- **Database**: PostgreSQL (risk rules, transaction logs)
- **Monitoring**: DataDog, Elasticsearch

## Key Contacts

- **Domain Lead**: @risk-lead — Risk strategy and policy
- **Architecture**: @architect-lead — Technical decisions
- **Lead Engineer**: @risk-engineer — Team management
- **Compliance Officer**: @compliance-officer — Regulatory oversight

## Confluence Links

- [Risk Management Policies](https://confluence.techcombank.io/risk-policies)
- [Fraud Detection Rules](https://confluence.techcombank.io/fraud-rules)
- [SBV Compliance Guide](https://confluence.techcombank.io/sbv-compliance)

## Related Domains

- [Payments Domain](../payments/README.md)
- [Lending Domain](../lending/README.md)
- [Core Banking Domain](../core-banking/README.md)

---

Last Updated: March 8, 2026 | Team: Risk Technology
