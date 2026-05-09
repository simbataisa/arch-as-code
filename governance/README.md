# Techcombank Architecture Governance Framework

## Overview

This directory contains the official governance framework for Techcombank's architecture decision-making, design approval processes, and technology standards. The framework ensures consistent, secure, and scalable architecture across all business domains and technology initiatives.

**Owned by:** Enterprise Architecture Technology (EAT) Team

**Last Updated:** 2026-01-15

---

## Framework Structure

### Design Approval Board (DAB) Process
The DAB is Techcombank's primary mechanism for reviewing and approving significant architecture changes, new platforms, and cross-domain integrations.

- **[DAB Process Overview](./dab-process/README.md)** — What is a DAB, when it's required, participants, and lifecycle
- **[Full DAB Process](./dab-process/dab-full-process.md)** — Comprehensive track for major initiatives (10-day SLA)
- **[Light DAB Process](./dab-process/dab-light-process.md)** — Fast-track for minor changes (5-day SLA)
- **[Approval Matrix](./dab-process/approval-matrix.md)** — Role-based approval requirements by document type
- **[Escalation Procedure](./dab-process/escalation-procedure.md)** — Conflict resolution and deadlock handling
- **[SLA Targets](./dab-process/sla-targets.md)** — Service level agreements per review phase

### Standards & Conventions
Technical standards that apply across all DAB submissions and architecture documentation.

- **[Naming Conventions](./standards/naming-conventions.md)** — Branch, file, folder, tag, and MR naming rules
- **[Diagram Standards](./standards/diagram-standards.md)** — Mermaid and PlantUML conventions, color schemes
- **[API Standards](./standards/api-standards.md)** — OpenAPI 3.0+ conventions and naming patterns
- **[Security Baseline](./standards/security-baseline.md)** — Mandatory controls for all submissions
- **[Data Classification](./standards/data-classification.md)** — Data tier definitions and handling rules

### Architecture Decision Records (ADRs)
Significant decisions made by the Architecture team that establish precedent and direction.

- **[ADR-001: Adopt Architecture-As-Code](./decisions/ADR-001-adopt-architecture-as-code.md)** — GitLab + Markdown approach
- **[ADR-002: MermaidJS/PlantUML over Drawio](./decisions/ADR-002-mermaid-over-drawio.md)** — Text-based diagram strategy
- **[ADR-003: SAGA Pattern Standard](./decisions/ADR-003-saga-pattern-standard.md)** — Distributed transaction handling

---

## Key Principles

1. **Consistency** — All architecture follows established standards and naming conventions
2. **Traceability** — Every decision is versioned and linked to business context
3. **Security-First** — Mandatory baseline controls apply to all initiatives
4. **Efficiency** — Clear SLAs and fast-track processes minimize approval delays
5. **Scalability** — Framework supports Techcombank's growth across domains

---

## Getting Started

### For Teams Submitting to DAB

1. Determine if your initiative requires **Full DAB** (major changes) or **Light DAB** (minor changes)
2. Review the applicable process document
3. Prepare required documents according to the approval matrix
4. Follow naming conventions for branches, files, and MR titles
5. Submit MR and wait for phase progression

**Target SLA:** Full DAB = 10 business days | Light DAB = 5 business days

### For Reviewers

1. Consult the **Approval Matrix** to understand your role
2. Review submissions within 2 business days (initial response)
3. Complete detailed review within 5 business days
4. Use escalation procedure if consensus cannot be reached

---

## Governance Contacts

| Role | Ownership |
|------|-----------|
| **Enterprise Architecture Technology (EAT) Lead** | Overall governance framework |
| **Solution Architect** | Business context and integration points |
| **Enterprise Architect** | End-to-end design and roadmap alignment |
| **Security Architect** | Security baseline and compliance |
| **Infrastructure Architect** | Deployment, scaling, and operations |
| **Data Architect** | Data modeling, classification, governance |
| **IT Operations / SRE** | Operational runbooks and SLAs |

---

## Governance Updates

Changes to governance framework require:
- Consensus from EAT leadership
- Documentation of rationale in ADR format
- Communication to all affected teams
- 2-week notice period for major changes

---

## Related Resources

- Techcombank Architecture Repository (GitLab)
- Confluence: Enterprise Architecture (legacy, being migrated to this framework)
- IT Security Policy: Data Classification and Protection
- Incident Management Procedures
