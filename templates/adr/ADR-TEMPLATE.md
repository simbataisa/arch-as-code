# ADR-[NUMBER]: [TITLE]

<!--
Architecture Decision Records (ADRs) document significant architectural decisions.
They help teams understand the reasoning behind design choices and provide historical context.

Instructions:
1. Replace [NUMBER] with the next sequential ADR number (e.g., ADR-001, ADR-002)
2. Replace [TITLE] with a concise, descriptive title (noun phrases)
3. Fill in all required sections
4. Remove these instruction comments when complete

ADR Numbering: ADRs are numbered sequentially starting at 001.
Example: ADR-001, ADR-002, ADR-003, etc.

ADR Naming: File should be named ADR-[NUMBER]-[kebab-case-title].md
Example: ADR-001-use-postgresql-for-primary-database.md
-->

**Status:** Proposed | Accepted | Deprecated | Superseded

**Date:** [YYYY-MM-DD]

**Author(s):** [Name(s)] — [Email(s)]

**Stakeholders:** [Who was consulted or affected by this decision?]

**Decision ID:** [Internal reference if applicable]

---

## Context

<!--
Describe the issue or problem that motivated this decision.
What conditions led to making this architectural choice?
Include background, constraints, and relevant history.

Example: "Our application was experiencing performance degradation with
increasing user load. MySQL queries were becoming bottlenecks. We needed
a database solution that could handle >10k TPS with sub-100ms latency."
-->

[Provide context: What is the problem we're trying to solve?]

**Background:**
- [Relevant context point 1]
- [Relevant context point 2]
- [Relevant context point 3]

**Constraints:**
- [Constraint 1]
- [Constraint 2]

---

## Decision Drivers

<!--
What factors influenced this decision? List the drivers that motivated
the choice, in priority order.

Example:
- Performance: System must handle >10k TPS
- Cost: Need cost-effective solution within $50k budget
- Maintainability: Team has PostgreSQL expertise
- Availability: Must support 99.95% SLA
-->

| Driver | Priority | Impact |
|--------|----------|--------|
| [Factor] | High / Medium / Low | [Why it matters] |
| [Factor] | High / Medium / Low | [Why it matters] |
| [Factor] | High / Medium / Low | [Why it matters] |
| [Factor] | High / Medium / Low | [Why it matters] |

---

## Considered Options

<!--
List all significant alternatives that were considered.
For each option, describe:
- What the option is
- Pros (advantages)
- Cons (disadvantages)
- Cost/effort estimate

Use a table format for easy comparison.
-->

### Option A: [Option Name]

**Description:** [What is this option? How would it work?]

**Pros:**
- [Pro 1]
- [Pro 2]
- [Pro 3]

**Cons:**
- [Con 1]
- [Con 2]
- [Con 3]

**Effort/Cost:** [Estimation]

---

### Option B: [Option Name]

**Description:** [What is this option? How would it work?]

**Pros:**
- [Pro 1]
- [Pro 2]

**Cons:**
- [Con 1]
- [Con 2]
- [Con 3]

**Effort/Cost:** [Estimation]

---

### Option C: [Option Name]

**Description:** [What is this option? How would it work?]

**Pros:**
- [Pro 1]

**Cons:**
- [Con 1]
- [Con 2]
- [Con 3]

**Effort/Cost:** [Estimation]

---

## Decision Outcome

<!--
State clearly which option was chosen and why.
This is the "decision" part of the ADR.
Be specific about what was decided.
-->

**Chosen:** Option B — [Option Name]

**Rationale:**

Option B was selected because:

1. [Reason 1: How does it address the primary drivers?]
2. [Reason 2: How does it balance competing concerns?]
3. [Reason 3: How does it align with team capabilities or standards?]

This option provides the best balance between [key factor 1] and [key factor 2], while [addressing concern 3].

**Implementation Plan:**

1. [Step 1]
2. [Step 2]
3. [Step 3]

**Timeline:** [When will this be implemented?]

**Owner:** [Who is responsible for implementation?]

---

## Positive Consequences

<!--
What benefits or improvements will result from this decision?
What does this decision enable?
-->

- [Benefit 1] — [Explanation]
- [Benefit 2] — [Explanation]
- [Benefit 3] — [Explanation]

**Measurable Outcomes:**
- [How will we measure success? Metric 1]
- [Metric 2]
- [Metric 3]

---

## Negative Consequences

<!--
What trade-offs or drawbacks does this decision introduce?
What did we give up?
What new challenges might it create?
-->

- [Trade-off 1] — [Explanation and mitigation]
- [Trade-off 2] — [Explanation and mitigation]
- [Trade-off 3] — [Explanation and mitigation]

**Mitigation Strategies:**
- [How we'll mitigate trade-off 1]
- [How we'll mitigate trade-off 2]
- [How we'll mitigate trade-off 3]

---

## Compliance & Regulatory Alignment

<!--
Does this decision align with regulatory requirements, security standards,
or compliance frameworks (PCI-DSS, GDPR, ISO 27001, etc.)?
-->

| Standard | Requirement | Alignment | Notes |
|----------|-------------|-----------|-------|
| [Standard] | [Requirement] | Compliant / Non-compliant / N/A | [Details] |
| [Standard] | [Requirement] | Compliant / Non-compliant / N/A | [Details] |

---

## Related Decisions

<!--
Link to other ADRs that are related to or dependent on this decision.
Show how this decision fits into the broader architectural landscape.
-->

- **ADR-XXX:** [Related Decision Title] — [Relationship]
  - [How it relates]

- **ADR-YYY:** [Related Decision Title] — [Relationship]
  - [How it relates]

### Superseded Decisions

- [If this ADR replaces a previous decision, reference it]

### Decisions Dependent on This

- [If other decisions depend on this one, list them]

---

## Implementation Checklist

<!--
What needs to happen to make this decision operational?
-->

- [ ] [Deliverable 1] — [Owner] — [Due date]
- [ ] [Deliverable 2] — [Owner] — [Due date]
- [ ] [Deliverable 3] — [Owner] — [Due date]
- [ ] Documentation updated
- [ ] Team trained
- [ ] Monitoring configured
- [ ] Production rollout complete

---

## Review & Approval

| Role | Name | Date | Approval |
|------|------|------|----------|
| Proposer | [Name] | [Date] | ✓ Proposed |
| Technical Lead | [Name] | [Date] | Pending |
| Domain Architect | [Name] | [Date] | Pending |
| Security Review | [Name] | [Date] | Pending |

---

## Lessons Learned (Post-Implementation)

<!--
After implementation, document what was learned.
Update this section 3-6 months after going live.
-->

**Status:** Not yet implemented

**Actual Outcomes:**
- [Outcome 1 vs. expectation]
- [Outcome 2 vs. expectation]

**Unexpected Issues:**
- [Issue 1 and resolution]
- [Issue 2 and resolution]

**Future Improvements:**
- [Suggested improvement 1]
- [Suggested improvement 2]

---

## References

<!--
Link to supporting documents, research, external resources, etc.
-->

### Internal References
- [Document Title](link)
- [Design Document](link)
- [Techcombank Architecture Principles](https://techcombank.com/architecture/principles)

### External References
- [Technology Documentation](link)
- [Industry Standard](link)
- [Research Paper](link)

### Related Discussion
- [Slack channel discussion](link)
- [Architecture discussion notes](link)
- [Team meeting notes](link)

---

## Appendices

### A. Detailed Comparison Table

| Criteria | Option A | Option B | Option C |
|----------|----------|----------|----------|
| Performance | [Rating] | [Rating] | [Rating] |
| Cost | [Rating] | [Rating] | [Rating] |
| Maintainability | [Rating] | [Rating] | [Rating] |
| Team expertise | [Rating] | [Rating] | [Rating] |
| Vendor support | [Rating] | [Rating] | [Rating] |

### B. Cost Breakdown (Option B)

| Component | Cost | Timeline |
|-----------|------|----------|
| [Component] | $[X] | [When] |
| [Component] | $[X] | [When] |
| **Total** | **$[X]** | |

### C. Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-----------|--------|-----------|
| [Risk] | High/Med/Low | High/Med/Low | [Mitigation] |
| [Risk] | High/Med/Low | High/Med/Low | [Mitigation] |

---

## Change Log

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | [Date] | [Author] | Initial ADR |
| 1.1 | [Date] | [Author] | [Summary of changes] |
| [Status Change] | [Date] | [Author] | Status: Accepted |
