# Architecture Principles

Core beliefs that guide all architectural decisions at Techcombank. These principles define our approach to system design, security, scalability, and operational excellence.

## Index

| Principle | Purpose | Status |
|-----------|---------|--------|
| [API-First Design](./api-first-design.md) | Design APIs before implementation to enable parallel development | Approved |
| [Event-Driven Architecture](./event-driven-architecture.md) | Use asynchronous events for loose coupling and scalability | Approved |
| [Zero-Trust Security](./zero-trust-security.md) | Never trust, always verify every request and connection | Approved |
| [Database-Per-Service](./database-per-service.md) | Each microservice owns its data store, avoiding shared databases | Approved |
| [Cloud-Native-First](./cloud-native-first.md) | Design systems for cloud from day one with containers and automation | Approved |

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

**Last Updated**: 2026-03-08 | **Maintained By**: Enterprise Architecture Board
