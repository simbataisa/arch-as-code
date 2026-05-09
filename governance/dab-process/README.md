# Design Approval Board (DAB) Process

## What is the Design Approval Board?

The Design Approval Board is Techcombank's formal governance mechanism for reviewing, evaluating, and approving significant architecture decisions and technology initiatives. The DAB ensures that all major changes align with business strategy, security requirements, operational capabilities, and technology roadmap.

**The DAB exists to:**
- Prevent inconsistent architecture across domains
- Ensure security and compliance at design time
- Identify integration risks and dependencies early
- Build consensus among architecture stakeholders
- Create an audit trail of decisions

---

## When is a DAB Required?

### Mandatory Full DAB

A **Full DAB process** is required for:

1. **New Platforms or Products** — New customer-facing or internal platform
2. **Major Refactoring** — Significant rearchitecture of existing systems
3. **Cross-Domain Integration** — Connecting systems across 2+ business domains
4. **New External Integration** — Integration with external 3rd-party systems (banks, vendors, regulators)
5. **Data Architecture Changes** — New data sources, significant model changes, or new classification tiers
6. **Technology Stack Change** — Adopting new infrastructure, languages, frameworks, or databases
7. **Security or Compliance Initiative** — New security controls or regulatory requirements

**Full DAB SLA:** 10 business days end-to-end

### Light DAB Fast-Track

A **Light DAB process** is suitable for:

1. **Minor Enhancements** — Incremental feature additions with no architecture impact
2. **Configuration Changes** — Parameter tuning, feature flags, routing rules
3. **Version Upgrades** — Framework, library, or middleware version updates with no architecture change
4. **Performance Optimization** — Algorithmic improvements with no design changes
5. **Bug Fixes** — Defect corrections that don't change API contracts

**Light DAB SLA:** 5 business days end-to-end

### No DAB Required

The following do NOT require a DAB:

- Code refactoring with no API change
- Internal variable renaming
- Documentation updates
- Test coverage improvements
- Pipeline/CI-CD optimizations

**Uncertainty?** Err toward Full DAB if unclear. Contact the Enterprise Architecture Technology team.

---

## Who Participates in DAB?

### Required Reviewers (vary by process type)

| Role | Full DAB | Light DAB | Responsibility |
|------|----------|-----------|-----------------|
| **Solution Architect** | Required | Required | Business context, domain fit, user requirements |
| **Enterprise Architect** | Required | Required | End-to-end design, roadmap alignment, consistency |
| **Security Architect** | Required | Optional | Security baseline, compliance, data handling |
| **Infrastructure Architect** | Required | Optional | Deployment, scaling, performance, operations |
| **Data Architect** | Optional | Optional | Data models, classification, retention, privacy |
| **IT Operations / SRE** | Required | Optional | Operational runbooks, monitoring, incident procedures |

### Submitter Responsibilities

The team submitting the architecture (via MR) must:
- Provide complete, clear documentation
- Be available to answer reviewer questions
- Respond to feedback within 2 business days
- Incorporate changes or document disagreement

### Approver Responsibilities

Each reviewer must:
- Read submission within 2 business days
- Complete detailed review within 5 business days
- Provide constructive feedback with rationale
- Use escalation procedure if issues arise

---

## DAB Lifecycle Overview

Both Full and Light DAB processes follow a phased progression:

```
┌─────────────┐     ┌──────────────────┐     ┌────────────┐     ┌──────────────┐     ┌─────────────┐
│ Initiation  │────▶│ Automated Quality │────▶│ Peer Review│────▶│ Specialist   │────▶│ EA Approval │
│             │     │ Gate              │     │            │     │ Review       │     │ & Merge     │
└─────────────┘     └──────────────────┘     └────────────┘     └──────────────┘     └─────────────┘
```

### Phase 1: Initiation (Day 1-2)
- Submitter creates GitLab MR with required documents
- Assign to appropriate reviewers based on DAB type
- Automated checks verify structure and formatting

### Phase 2: Automated Quality Gate (Day 1-3)
- CI/CD pipeline validates document completeness
- Checks naming conventions and diagram standards
- Validates security baseline checklist

### Phase 3: Peer Review (Day 3-5)
- Solution Architect and Domain Lead review and comment
- Assess business context and integration fit
- Flag dependencies and risks

### Phase 4: Specialist Review (Day 5-8)
- Security, Infrastructure, Data architects deep-dive
- Provide detailed technical feedback
- Identify mitigations or requirements

### Phase 5: EA Approval & Merge (Day 8-10)
- Enterprise Architect synthesizes feedback
- Makes final approval decision
- Merges MR and closes DAB

---

## Document Requirements

### Full DAB Requires All 9 Sections
1. Business Context & Requirements
2. High-Level Architecture (HLA)
3. Detailed Design (component diagrams, data flows)
4. Integration Points & Dependencies
5. Security & Compliance Assessment
6. Operational Requirements & Runbooks
7. Performance & Scalability Analysis
8. Migration & Rollout Strategy
9. Risk Assessment & Mitigation

### Light DAB Requires 4 Sections
1. Business Context & Requirements (brief)
2. High-Level Architecture (diagram only)
3. Security & Compliance Assessment
4. Risk Assessment

---

## Getting Started

**For teams submitting to DAB:**
- Read the full process guide applicable to your initiative: [Full DAB](./dab-full-process.md) or [Light DAB](./dab-light-process.md)
- Consult the [Approval Matrix](./approval-matrix.md) to identify required reviewers
- Follow [Naming Conventions](../standards/naming-conventions.md)
- Use [SLA Targets](./sla-targets.md) to plan your timeline

**For reviewers:**
- Understand your role in the [Approval Matrix](./approval-matrix.md)
- Know the [SLA Targets](./sla-targets.md) for your review phase
- Review the [Escalation Procedure](./escalation-procedure.md) if consensus fails

---

## Questions?

Contact the Enterprise Architecture Technology team or submit a discussion on the GitLab repo.
