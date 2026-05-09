# Architecture Principles

Core beliefs that guide all architectural decisions at Techcombank. These principles define our approach to system design, security, scalability, and operational excellence.

## Index

| Catalog ID | Principle | Purpose | Status |
|---|---|---|---|
| PRIN-001 | [API-First Design](./api-first-design.md) | Design APIs before implementation to enable parallel development | Approved |
| PRIN-002 | [Event-Driven Architecture](./event-driven-architecture.md) | Use asynchronous events for loose coupling and scalability | Approved |
| PRIN-003 | [Zero-Trust Security](./zero-trust-security.md) | Never trust, always verify every request and connection | Approved |
| PRIN-004 | [Database-Per-Service](./database-per-service.md) | Each microservice owns its data store, avoiding shared databases | Approved |
| PRIN-005 | [Cloud-Native-First](./cloud-native-first.md) | Design systems for cloud from day one with containers and automation | Approved |
| PRIN-006 | [Idempotency-by-default](./idempotency-by-default.md) | Every write API and message consumer is idempotent — Wave 0 spine | Draft (Wave 0) |
| PRIN-007 | [Data Residency](./data-residency.md) | PII and transactional data stay in Vietnam by default; tokens may cross | Draft (Wave 0) |
| PRIN-008 | [Defense-in-Depth](./defense-in-depth.md) | Multiple independent control layers — Wave 1 stub | Proposed |
| PRIN-009 | [Observability-First](./observability-first.md) | OpenTelemetry from day one — Wave 1 stub | Proposed |
| PRIN-010 | [Fail-Safe Defaults](./fail-safe-defaults.md) | Wave 1 stub | Proposed |
| PRIN-011 | [Least-Privilege](./least-privilege.md) | Wave 1 stub | Proposed |
| PRIN-012 | [Async-by-default](./async-by-default.md) | Wave 1 stub | Proposed |
| PRIN-013 | [Modular Monolith Preference](./modular-monolith-preference.md) | Wave 1 stub | Proposed |

## Principle vs. Pattern vs. Best Practice

- **Principle**: A high-level belief or approach (e.g., "API-First")
- **Pattern**: A proven solution to a specific problem (e.g., "SAGA Orchestration")
- **Best Practice**: Operational guidelines for consistent execution (e.g., "Observability Standards")

## Using These Principles

1. Review the relevant principle(s) for your domain
2. Identify which patterns support each principle
3. Reference both in your DAB submission
4. Document deviations (with EA board approval)

---

**Last Updated**: 2026-05-09 | **Maintained By**: Enterprise Architecture Board
