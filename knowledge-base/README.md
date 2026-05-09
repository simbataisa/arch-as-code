# Techcombank Architecture Knowledge Base

This repository contains reusable architecture principles, patterns, and best practices that serve as the foundation for all Design Architecture Board (DAB) submissions and technical decisions.

## Structure

- **[Principles](./principles/README.md)** — Core architectural beliefs and decision frameworks
- **[Patterns](./patterns/)** — Proven solutions to recurring architecture problems
  - [Integration Patterns](./patterns/integration/) — Cross-service communication and data synchronization
  - [Security Patterns](./patterns/security/) — Authentication, authorization, and secrets management
  - [Data Patterns](./patterns/data/) — Data modeling, CQRS, data mesh approaches
  - [Resilience Patterns](./patterns/resilience/) — Fault tolerance, recovery, and failure handling
- **[Best Practices](./best-practices/)** — Operational and process guidelines

## How to Reference

In your DAB submissions, reference knowledge-base content using relative links:

```markdown
# Service Architecture

This service implements the [API-First Design](../../knowledge-base/principles/api-first-design.md) principle and follows the [SAGA Orchestration Pattern](../../knowledge-base/patterns/integration/saga-orchestration.md) for distributed transactions.
```

## Contributing New Patterns/Principles

1. **Validate** the pattern/principle through implementation in 2+ projects
2. **Document** using the standard template (see any file in this knowledge base)
3. **Review** with the Enterprise Architecture Board
4. **Announce** in architecture guild meetings
5. **Version** using the status badge (Status, Last Reviewed, Owner)

### Template Structure

All patterns and principles follow this structure:

```markdown
# [Name]

Status: [Draft | Proposed | Approved | Deprecated] | Last Reviewed: YYYY-MM-DD | Owner: @team

## Problem Statement
What challenge does this address?

## Solution
How do we solve it? Include diagrams where helpful.

## Implementation Guidelines
Concrete steps to apply this pattern/principle.

## When to Use / When NOT to Use
Scope and boundaries.

## References
Links to implementations, external resources, tools.
```

## Governance

- **Status Legend**:
  - **Draft**: Under development, not yet validated
  - **Proposed**: Ready for EA board review
  - **Approved**: Endorsed by EA board, use in production designs
  - **Deprecated**: Replaced by newer pattern/principle

- **Review Cadence**: Annual review for all approved patterns; quarterly for draft/proposed
- **Changes**: Require EA board approval and update to the status badge

## Quick Links

- [API-First Design](./principles/api-first-design.md)
- [Event-Driven Architecture](./principles/event-driven-architecture.md)
- [Zero-Trust Security](./principles/zero-trust-security.md)
- [Database-Per-Service](./principles/database-per-service.md)
- [Cloud-Native-First](./principles/cloud-native-first.md)

---

**Last Updated**: 2026-03-08 | **Maintained By**: Enterprise Architecture Board
