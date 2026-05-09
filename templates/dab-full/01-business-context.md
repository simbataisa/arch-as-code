# 01 — Business Context

<!--
INSTRUCTIONS:
1. Provide a clear overview of your system, its purpose, and business value
2. Document the problems your system solves with measurable impact
3. Define success criteria for architectural decisions
4. Establish scope boundaries and key requirements
5. Remove these instruction comments when complete
-->

## Introduction & Overview

<!--
Describe your system, what it does, and why it matters to Techcombank.
Include: system name, high-level purpose, key business drivers, expected users/stakeholders.
2-3 paragraphs.
Example: "Payment Orchestration Platform is a central hub for routing payment requests across
multiple processing channels (domestic transfers, card networks, international corridors) with
dynamic routing logic based on transaction attributes and real-time liquidity constraints."
-->

**System Name:** [Your System Name]

**Purpose:** [What does this system do and why is it important?]

**Business Value:** [How does this support Techcombank's strategic objectives?]

**Key Stakeholders:**
- Business Owner: [Name/Title]
- Product Lead: [Name/Title]
- Technical Lead: [Name/Title]

---

## Problem Statements

<!--
Document the current challenges that this architecture is solving.
Use a table format: Problem # | Problem Description | Business Impact
Include 2-5 key problems with quantifiable impact where possible.
-->

| # | Problem | Impact | Current State |
|---|---------|--------|---------------|
| P1 | [Problem description] | [Measurable impact] | [How is it handled today?] |
| P2 | [Problem description] | [Measurable impact] | [How is it handled today?] |
| P3 | [Problem description] | [Measurable impact] | [How is it handled today?] |

---

## Objectives & Success Criteria

<!--
Define what success looks like for this project.
Table format: Objective # | Objective | Success Criteria | Measurement Method
Include business, technical, and operational objectives.
-->

| # | Objective | Success Criteria | Measurement |
|---|-----------|------------------|-------------|
| O1 | [Objective statement] | [Specific, measurable criteria] | [How will we measure?] |
| O2 | [Objective statement] | [Specific, measurable criteria] | [How will we measure?] |
| O3 | [Objective statement] | [Specific, measurable criteria] | [How will we measure?] |

---

## Scope

### In Scope

<!--
What systems, domains, processes, or features are included in this architecture?
Use bullet points.
-->

- [Capability/Component 1]
- [Capability/Component 2]
- [Capability/Component 3]

### Out of Scope

<!--
What is explicitly NOT included, to set clear boundaries?
-->

- [Excluded capability 1]
- [Excluded capability 2]

### Assumptions

<!--
What are we assuming to be true? (e.g., "All partner APIs return responses within 2s",
"Batch processing can run during 2-4 AM maintenance window")
-->

- [Assumption 1]
- [Assumption 2]

### Constraints

<!--
What are the hard limits? (e.g., "Must integrate with legacy mainframe by Q3 2026",
"Kubernetes cluster in Jakarta region only", "PCI-DSS compliance required")
-->

- [Constraint 1]
- [Constraint 2]

---

## Key Requirements

### Functional Requirements

<!--
What must the system DO? (behaviors, workflows, business rules)
Table format: ID | Requirement | Description | Priority
-->

| ID | Requirement | Description | Priority |
|----|-------------|-------------|----------|
| FR-01 | [Requirement name] | [Detailed description of behavior] | High/Medium/Low |
| FR-02 | [Requirement name] | [Detailed description of behavior] | High/Medium/Low |
| FR-03 | [Requirement name] | [Detailed description of behavior] | High/Medium/Low |

### Non-Functional Requirements

<!--
What are the -ilities? (performance, scalability, availability, security, maintainability, etc.)
Table format: ID | Requirement | Target Metric | Justification
-->

| ID | Requirement | Target | Justification |
|----|-------------|--------|---------------|
| NFR-01 | Availability | [99.9% / 99.95% SLA] | [Business need] |
| NFR-02 | Latency | [P50/P99 latency targets] | [User experience requirement] |
| NFR-03 | Throughput | [TPS / requests/min targets] | [Peak load forecast] |
| NFR-04 | Data Consistency | [Strong / Eventual] | [Regulatory / business requirement] |
| NFR-05 | Security | [Authentication/encryption standards] | [Compliance requirement] |
| NFR-06 | Maintainability | [Code coverage, deployment frequency] | [Operational requirement] |

---

## Governance & Timeline

- **Planned Start Date:** [YYYY-MM-DD]
- **Planned Delivery Date:** [YYYY-MM-DD]
- **Budget Approval:** [Approved / Pending] — [Cost estimate if approved]
- **Architecture Review Status:** This DAB submission
- **Related Projects:** [Link to epic, roadmap, or dependent projects]

---

## References

<!--
Link to supporting documents: business case, product requirements, competitor analysis, RFP, etc.
-->

- [Document Title](link)
- [Document Title](link)
