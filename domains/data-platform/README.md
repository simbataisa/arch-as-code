# Data Platform Domain

| Property | Value |
|----------|-------|
| **Domain** | Data Platform |
| **Team** | Data Engineering |
| **Domain Lead** | @data-lead |
| **Last Updated** | 2026-03-08 |
| **Slack Channel** | #data-platform-domain |

---

## Overview

The Data Platform domain provides centralized data infrastructure for analytics, reporting, and machine learning across Techcombank. This includes data ingestion, data warehouse, lakehouse, real-time streaming analytics, BI/reporting, and data governance.

### Key Responsibilities

- **Data Ingestion** — Real-time and batch data collection from all systems
- **Data Lake** — Raw data storage with schema-on-read approach
- **Data Warehouse** — Dimensional modeling for business analytics
- **Real-Time Analytics** — Stream processing and real-time dashboards
- **BI/Reporting** — Self-service analytics and reporting
- **Data Governance** — Data quality, lineage, and compliance

## Architecture

This domain integrates with:

- **Payments Domain** — Real-time payment analytics and audit logs
- **Core Banking Domain** — Account and GL data replication
- **Risk Management Domain** — Fraud analytics and monitoring
- **Lending Domain** — Portfolio analytics and risk reporting
- **Digital Channels Domain** — User analytics and engagement metrics

### Related Folders

- [`./domain-model.md`](./domain-model.md) — Business capability map
- [`./shared/`](./shared/) — Shared data standards and procedures

## Current Projects

| Project | Status | Lead | Updated |
|---------|--------|------|---------|
| Lakehouse Migration | Draft | @data-architect | 2026-02-10 |
| Real-Time Analytics | Draft | @data-architect | 2026-02-05 |

See [`dab/2026/`](./dab/2026/) for active projects.

## Technology Stack

**Current Technologies:**
- **Data Warehouse**: Snowflake (cloud-based)
- **Data Lake**: S3 + Delta Lake (Apache Spark)
- **Stream Processing**: Kafka + Spark Streaming
- **BI/Reporting**: Tableau, Looker
- **Data Quality**: Great Expectations
- **Orchestration**: Apache Airflow

## Key Contacts

- **Domain Lead**: @data-lead — Data strategy and roadmap
- **Data Architecture**: @data-architect — Technical decisions
- **Lead Engineer**: @data-engineer — Team management
- **Analytics Manager**: @analytics-manager — Stakeholder engagement

## Confluence Links

- [Data Platform Architecture](https://confluence.techcombank.io/data-platform)
- [Data Catalog](https://confluence.techcombank.io/data-catalog)
- [Analytics Dictionary](https://confluence.techcombank.io/analytics-dict)

## Related Domains

- [Payments Domain](../payments/README.md)
- [Core Banking Domain](../core-banking/README.md)
- [Risk Management Domain](../risk-management/README.md)

---

Last Updated: March 8, 2026 | Team: Data Engineering
