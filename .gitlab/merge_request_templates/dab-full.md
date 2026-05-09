## DAB Full Submission

### Summary
<!-- Provide a brief executive summary of the proposed architecture and its business impact -->

**Project Name:**
**Domain:**
**SA Lead:**
**Submission Date:**
**Target Implementation Date:**

<!-- Describe the key business drivers, value proposition, and expected outcomes -->

---

### Domain & Project Information

**Primary Domain:**
<!-- Select: Payments | Core-Banking | Digital-Channels | Risk-Management | Data-Platform | Lending | Wealth-Management -->

**Related Domains:**
<!-- List any secondary/tertiary domain impacts -->

**Project Type:**
<!-- Select: New System | Modernization | Migration | Integration | Technology Refresh | Other -->

**Systems Impacted:**
<!-- List all systems that will be created, modified, or integrated -->

**Estimated Complexity Level:** Full DAB (all 9 required documents)

---

### Design Approval Board (DAB) Submission Checklist

**Document Completeness:**
- [ ] **01-business-context.md** – Business drivers, value proposition, stakeholder alignment
- [ ] **02-current-state-architecture.md** – Existing landscape, system dependencies, pain points
- [ ] **03-high-level-architecture.md** – Target architecture, major components, technology choices
- [ ] **04-detailed-design.md** – Component specifications, interfaces, data flows, deployment topology
- [ ] **05-migration-strategy.md** – Phasing, sequencing, rollback procedures, cutover plan
- [ ] **06-operational-readiness.md** – Monitoring, alerting, runbooks, SLAs, capacity planning
- [ ] **07-financial-analysis.md** – Cost-benefit analysis, TCO, ROI, budget allocation
- [ ] **08-security-design.md** – Threat model, security controls, compliance mapping, incident response
- [ ] **09-dab-light-assessment.md** – Risk register, critical assumptions, open items, decision summary
- [ ] **10-risk-register.md** – Detailed risk assessment with mitigation strategies

**Artifacts & Validation:**
- [ ] Architecture diagrams render correctly in PlantUML/Mermaid
- [ ] All diagrams include legend and context
- [ ] OpenAPI specifications provided (if applicable for new APIs)
- [ ] API Gateway routing rules documented (if applicable)
- [ ] Data dictionary/entity-relationship diagrams included (if data platform changes)
- [ ] Capacity planning workbook referenced or attached
- [ ] Security assessment (STRIDE/similar) completed
- [ ] Compliance mapping complete (BCBS, AML/CFT, Data Protection)

**Process Compliance:**
- [ ] `.gitlab/reviewers.yml` configured for domain-specific review routing
- [ ] DAB slot formally requested and approved (DAB Request issue link: _______)
- [ ] SA and domain architect(s) sign-off obtained
- [ ] No blocking architecture exceptions outstanding
- [ ] Technical spike/PoC results documented (if applicable)

**Review Guidance by Role:**

| Role | Primary Focus | Files to Review |
|------|---------------|-----------------|
| **DAB Chair** | Governance alignment, enterprise impact | 01, 03, 05, 09 |
| **Domain Architect** | Domain-specific implications, pattern adherence | 02, 03, 04, 07 |
| **Security Board** | Threat model, controls, compliance | 08, 10 |
| **EA Directors** | Risk assessment, business case, assumptions | 07, 09, 10 |
| **Platform Ops** | Operational impact, runbook readiness | 04, 06, 10 |
| **Finance/PMO** | Cost justification, timeline feasibility | 05, 07 |

---

### Related Issues & Decisions

**DAB Request Issue:** (link to dab-request issue)

**Related Architecture Decisions:**
- ADR-XXX: (if applicable, link to any related architecture decision records)

**Dependencies on Other Initiatives:**
- (Link projects or epics this depends on or blocks)

**Architecture Exceptions Requested:**
- (Link any open architecture exception requests)

---

### Additional Notes

<!-- Any additional context, considerations, or concerns for reviewers -->

---

## Review Workflow

1. **Self Review:** Ensure all checklist items are complete before submitting
2. **Domain Architect Review:** Architecture fitness for domain
3. **Security Board Review:** Security and compliance posture
4. **EA Director Review:** Risk assessment and business case validation
5. **DAB Chair Review:** Final governance and enterprise alignment check
6. **DAB Approval:** Formal approval in DAB meeting

**Typical Review Timeline:** 5-7 business days (depends on review complexity and feedback iterations)

---

## MR Best Practices

- **Document Organization:** Keep all 9 documents in a single `dab/{project-name}/` directory
- **Commit Messages:** Reference the DAB Request issue in commit messages (e.g., `Closes #1234`)
- **Iterations:** Address reviewer feedback in follow-up commits (do not force-push)
- **Comments:** Respond to all review comments; mark as resolved only after action taken
- **Approval:** MR cannot be merged until all code owners approve

---

*For questions or guidance on the DAB process, see [DAB Process Documentation](../governance/dab-process.md)*
