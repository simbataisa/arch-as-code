# 09 — DAB Light Assessment

<!--
INSTRUCTIONS:
1. Assess system complexity to justify Light DAB choice
2. Verify alignment with architecture principles
3. Obtain stakeholder sign-off on architecture decisions
4. Remove these instruction comments when complete
-->

## Complexity Assessment

<!--
Determine if Light DAB is sufficient or if Full DAB is required.
Use the checklist below. If 2+ items checked, Full DAB is required.
-->

### Full DAB Requirements Checklist

| Criterion | Required? | Explanation |
|-----------|-----------|-------------|
| **New Platform Launch** | Yes / No | [Is this a brand-new system (not an enhancement)?] |
| **Cross-Domain Impact** | Yes / No | [Does this architecture affect multiple domains?] |
| **External Integration** | Yes / No | [New integration with external partners or complex APIs?] |
| **Regulated Data** | Yes / No | [Handling PCI, GDPR, or other restricted data?] |
| **Complexity Score** | Yes / No | [Story points > 50 or architecture > 5 services?] |
| **Infrastructure Complexity** | Yes / No | [Multi-region, auto-scaling, or custom infrastructure?] |
| **Performance Critical** | Yes / No | [Sub-second latency or >10k TPS requirements?] |

**Assessment Result:**
- **Full DAB Required** if 2+ criteria checked as "Yes"
- **Light DAB Sufficient** if ≤1 criterion checked as "Yes"

**Determination:** [Based on above, this project uses: Light DAB]

**Justification:** [Explain why Light DAB is appropriate for this project]

---

## Architecture Principles Alignment

<!--
Verify the design adheres to Techcombank's Architecture Principles.
See: https://techcombank.com/architecture/principles
-->

### Principle Compliance Matrix

| Principle | Description | Addressed In | Compliant? | Notes |
|-----------|-------------|--------------|-----------|-------|
| **Cloud-First** | Prefer cloud platforms over on-premises | [Document] | Yes / No | [Details] |
| **Microservices** | Decompose into independently deployable services | [Document] | Yes / No | [Details] |
| **API-Driven** | Design systems as API-first | [Document] | Yes / No | [Details] |
| **Data Minimization** | Collect only necessary data | [Document] | Yes / No | [Details] |
| **Security-by-Design** | Integrate security from the start | [Document] | Yes / No | [Details] |
| **Observability-First** | Logs, metrics, traces from day one | [Document] | Yes / No | [Details] |
| **Infrastructure-as-Code** | Automate infrastructure provisioning | [Document] | Yes / No | [Details] |

**Overall Principle Alignment:** [Fully aligned / Minor gaps (acceptable) / Significant concerns (mitigate)]

---

## Architectural Decision Summary

### Key Decisions Made

| Decision | Choice | Trade-off | Approved By |
|----------|--------|-----------|------------|
| [Decision area] | [Selected option] | [What's given up] | [Approver] |
| API Gateway | [Kong / AWS API Gateway / Custom] | [Trade-offs] | [Approver] |
| Database | [PostgreSQL / MongoDB / Managed service] | [Trade-offs] | [Approver] |
| Cache | [Redis / Memcached / None] | [Trade-offs] | [Approver] |
| Deployment | [Kubernetes / Serverless / EC2] | [Trade-offs] | [Approver] |

### Rationale for Key Decisions

**Decision 1: [Decision Title]**

- Selected: [Option]
- Reason: [Why this choice makes sense]
- Alternative considered: [Option] — rejected because [reason]
- Business impact: [How does this support business goals?]

---

## Risk Assessment

### Architectural Risks

| Risk | Probability | Impact | Mitigation |
|------|-----------|--------|-----------|
| [Risk description] | High/Med/Low | High/Med/Low | [How to prevent/manage] |
| [Risk description] | High/Med/Low | High/Med/Low | [How to prevent/manage] |

### Known Limitations

| Limitation | Acceptable? | Workaround / Plan |
|-----------|-----------|-----------------|
| [Limitation] | Yes / No | [Workaround or future plan] |

---

## Vendor/Technology Choices Justification

### Technology Stack Review

| Technology | Chosen? | Justification | Risk |
|-----------|---------|--------------|------|
| [Tech] | Yes / No | [Why selected] | [Any risks?] |
| [Tech] | Yes / No | [Why selected] | [Any risks?] |

---

## Performance & Scalability Validation

### Load Projections

| Metric | Year 1 | Year 2 | Year 3 | Design Accommodates? |
|--------|---------|---------|---------|-----------------|
| Peak TPS | [TPS] | [TPS] | [TPS] | Yes / No |
| Daily active users | [Count] | [Count] | [Count] | Yes / No |
| Data volume (GB) | [GB] | [GB] | [GB] | Yes / No |

**Scaling Strategy:** [How the system will scale as volume grows]

---

## Cost & Resource Analysis

### Estimated Costs

| Component | Year 1 | Year 2 |
|-----------|--------|---------|
| Infrastructure (cloud) | $[X] | $[X] |
| Development team | $[X] | $[X] |
| **Total Cost of Ownership** | **$[X]** | **$[X]** |

### Team & Resource Requirements

| Role | FTE Required | Duration |
|------|--------------|----------|
| Solution Architect | [#] | [Timeline] |
| Senior Developer | [#] | [Timeline] |
| DevOps Engineer | [#] | [Timeline] |

---

## Timeline & Deliverables

### Project Timeline

| Phase | Duration | Start | End |
|-------|----------|-------|-----|
| Design | [weeks] | [Date] | [Date] |
| Development | [weeks] | [Date] | [Date] |
| Testing | [weeks] | [Date] | [Date] |
| Deployment | [weeks] | [Date] | [Date] |

### Key Milestones

- [ ] Architecture approval (this DAB)
- [ ] Development complete
- [ ] Testing complete
- [ ] Production deployment

---

## Stakeholder Sign-off

<!--
All stakeholders must review and approve the architecture before implementation.
-->

### Sign-off Table

| Stakeholder Role | Name | Approval | Date | Notes |
|-----------------|------|----------|------|-------|
| **Business Owner** | [Name] | Approved / Pending | [Date] | [Notes] |
| **Domain Lead** | [Name] | Approved / Pending | [Date] | [Notes] |
| **Security Officer** | [Name] | Approved / Pending | [Date] | [Notes] |
| **Technical Lead** | [Name] | Approved / Pending | [Date] | [Notes] |

**Overall Status:**
- [ ] All stakeholders have approved
- [ ] Conditional approvals (see conditions below)
- [ ] Approvals pending (list who needs to approve)

### Contingent Approvals

If any stakeholder approved conditionally:

| Stakeholder | Condition | Resolution Plan | Owner | Target Date |
|------------|-----------|-----------------|-------|------------|
| [Name] | [Condition] | [How will this be addressed?] | [Owner] | [When?] |

---

## Dependencies & Assumptions

### External Dependencies

| Dependency | Owner | Status | Impact if Missing |
|-----------|-------|--------|-------------------|
| [Dependency] | [Team] | Available | Critical / High |
| [Dependency] | [Team] | Available | Critical / High |

### Critical Assumptions

| Assumption | Verification Method | Risk if Wrong |
|-----------|-------------------|--------------|
| [Assumption] | [How will we verify?] | [Impact] |
| [Assumption] | [How will we verify?] | [Impact] |

---

## Approval Authority

This DAB submission requires approval from:

1. **Technical Review**
   - Lead: [Name]
   - Timeline: [Days]

2. **Domain Review**
   - Lead: [Name]
   - Timeline: [Days]

3. **Security Review**
   - Lead: [Name]
   - Timeline: [Days]

---

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | [Date] | [Author] | Initial submission |

---

## References

- [Architecture principles](https://techcombank.com/architecture/principles)
- [Data classification](https://techcombank.com/data-classification)
- [Security standards](https://techcombank.com/security)
