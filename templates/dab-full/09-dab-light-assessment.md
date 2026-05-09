# 09 — DAB Light Assessment

<!--
INSTRUCTIONS:
1. Assess system complexity to justify Full vs. Light DAB
2. Verify alignment with architecture principles
3. Obtain stakeholder sign-off on architecture decisions
4. Remove these instruction comments when complete
-->

## Complexity Assessment

<!--
Determine if this project truly requires a Full DAB or if Light DAB is sufficient.
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

**Determination:** [Based on above, this project uses: Full / Light DAB]

**Justification:** [Explain why this complexity level is appropriate]

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
| **Maintainability** | Design for future teams to understand | [Document] | Yes / No | [Details] |

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
| Message Queue | [Kafka / RabbitMQ / SQS] | [Trade-offs] | [Approver] |
| Deployment | [Kubernetes / Serverless / EC2] | [Trade-offs] | [Approver] |

### Rationale for Key Decisions

**Decision 1: [Decision Title]**

- Selected: [Option]
- Reason: [Why this choice makes sense]
- Alternative considered: [Option] — rejected because [reason]
- Business impact: [How does this support business goals?]
- Technical impact: [Maintainability, scalability, etc.]

[Repeat for 3-5 major decisions]

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
| [Limitation] | Yes / No | [Workaround or future plan] |

---

## Vendor/Technology Choices Justification

### Technology Stack Review

| Technology | Chosen? | Justification | Risk | Roadmap Support |
|-----------|---------|--------------|------|-----------------|
| [Tech] | Yes / No | [Why selected] | [Any risks?] | [Vendor support timeline] |
| [Tech] | Yes / No | [Why selected] | [Any risks?] | [Vendor support timeline] |

### Make vs. Build vs. Buy Analysis

| Capability | Decision | Owner | Notes |
|-----------|----------|-------|-------|
| [Capability] | Buy / Build / Make | [Team] | [Rationale] |
| [Capability] | Buy / Build / Make | [Team] | [Rationale] |

---

## Performance & Scalability Validation

### Load Projections

| Metric | Year 1 | Year 2 | Year 3 | Design Accommodates? |
|--------|--------|---------|---------|-----------------|
| Peak TPS | [TPS] | [TPS] | [TPS] | Yes / No |
| Daily active users | [Count] | [Count] | [Count] | Yes / No |
| Data volume (GB) | [GB] | [GB] | [GB] | Yes / No |
| Concurrent connections | [Count] | [Count] | [Count] | Yes / No |

**Scaling Strategy:** [How the system will scale as volume grows]

**Validation Method:** [Load testing, capacity planning, etc.]

---

## Cost & Resource Analysis

### Estimated Costs

| Component | Year 1 | Year 2 | Year 3 |
|-----------|--------|---------|---------|
| Infrastructure (cloud) | $[X] | $[X] | $[X] |
| Development team | $[X] | $[X] | $[X] |
| Operations & support | $[X] | $[X] | $[X] |
| **Total Cost of Ownership** | **$[X]** | **$[X]** | **$[X]** |

**ROI Assumptions:**
- [Assumption 1]
- [Assumption 2]

### Team & Resource Requirements

| Role | FTE Required | Duration | Notes |
|------|--------------|----------|-------|
| Solution Architect | [#] | [Timeline] | [Details] |
| Senior Developer | [#] | [Timeline] | [Details] |
| QA Engineer | [#] | [Timeline] | [Details] |
| DevOps Engineer | [#] | [Timeline] | [Details] |

---

## Timeline & Deliverables

### Project Timeline

| Phase | Duration | Start | End | Dependencies |
|-------|----------|-------|-----|--------------|
| Design & Planning | [weeks] | [Date] | [Date] | [Prior phase] |
| Development (Sprint 1-2) | [weeks] | [Date] | [Date] | Design complete |
| Development (Sprint 3-N) | [weeks] | [Date] | [Date] | Previous sprint |
| Integration Testing | [weeks] | [Date] | [Date] | Dev complete |
| UAT & Hardening | [weeks] | [Date] | [Date] | Integration complete |
| Production Deployment | [weeks] | [Date] | [Date] | UAT approved |

### Key Milestones

- [ ] Architecture approval (this DAB)
- [ ] Technical spike / PoC completion
- [ ] Development sprint 1 complete
- [ ] Internal integration testing complete
- [ ] UAT sign-off
- [ ] Production deployment

---

## Stakeholder Sign-off

<!--
All stakeholders must review and approve the architecture before implementation.
Document their approval and any conditions/concerns.
-->

### Sign-off Table

| Stakeholder Role | Name | Approval | Date | Conditions/Notes |
|-----------------|------|----------|------|-----------------|
| **Business Owner** | [Name] | Approved / Pending / Rejected | [Date] | [Any conditions] |
| **Domain Lead** | [Name] | Approved / Pending / Rejected | [Date] | [Any conditions] |
| **Security Officer** | [Name] | Approved / Pending / Rejected | [Date] | [Any conditions] |
| **Compliance Officer** | [Name] | Approved / Pending / Rejected | [Date] | [Any conditions] |
| **Infrastructure Lead** | [Name] | Approved / Pending / Rejected | [Date] | [Any conditions] |
| **Quality Lead** | [Name] | Approved / Pending / Rejected | [Date] | [Any conditions] |
| **Chief Architect** | [Name] | Approved / Pending / Rejected | [Date] | [Any conditions] |

**Overall Status:**
- [ ] All stakeholders have approved
- [ ] Conditional approvals (conditions listed above must be addressed)
- [ ] Approvals pending (list who needs to approve)

### Contingent Approvals

If any stakeholder approved conditionally, list conditions and resolution:

| Stakeholder | Condition | Resolution Plan | Owner | Target Date |
|------------|-----------|-----------------|-------|------------|
| [Name] | [Condition] | [How will this be addressed?] | [Owner] | [When?] |
| [Name] | [Condition] | [How will this be addressed?] | [Owner] | [When?] |

---

## Dependencies & Assumptions

### External Dependencies

| Dependency | Owner | Status | Impact if Missing |
|-----------|-------|--------|-------------------|
| [Dependency] | [Team] | Available / In progress / Blocked | Critical / High / Medium |
| [Dependency] | [Team] | Available / In progress / Blocked | Critical / High / Medium |

### Critical Assumptions

| Assumption | Verification Method | Risk if Wrong |
|-----------|-------------------|--------------|
| [Assumption] | [How will we verify?] | [What happens if false?] |
| [Assumption] | [How will we verify?] | [What happens if false?] |

---

## Approval Authority Chain

This DAB submission requires approval from:

1. **Architect Review** (Technical approval)
   - Lead: [Name]
   - Timeline: [Days for review]

2. **Domain Review** (Business & domain alignment)
   - Lead: [Name]
   - Timeline: [Days for review]

3. **Security Review** (Security & compliance)
   - Lead: [Name]
   - Timeline: [Days for review]

4. **Executive Approval** (Final sign-off)
   - Authority: [Title]
   - Timeline: [Days for review]

**Expected Approval Date:** [Date, assuming no major blockers]

---

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | [Date] | [Author] | Initial submission |
| 1.1 | [Date] | [Author] | [Summary of changes] |

---

## Appendices

### A. Supporting Documents

- [Link to business case]
- [Link to RFP / requirements]
- [Link to competitive analysis]
- [Link to technical spike results]

### B. Glossary

- **[Term]:** [Definition]
- **[Term]:** [Definition]

### C. References

- [Architecture principles](https://techcombank.com/architecture/principles)
- [Data classification](https://techcombank.com/data-classification)
- [Security standards](https://techcombank.com/security)
