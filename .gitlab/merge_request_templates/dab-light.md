## DAB Light Submission

### Summary
<!-- Brief description of the proposed change and business impact (1-2 paragraphs) -->

**Project Name:**
**Domain:**
**SA Lead:**
**Submission Date:**
**Target Implementation Date:**

---

### Project Information

**Primary Domain:**
<!-- Select: Payments | Core-Banking | Digital-Channels | Risk-Management | Data-Platform | Lending | Wealth-Management -->

**Systems Involved:**
<!-- List primary systems affected -->

**Change Scope:**
<!-- Select: Configuration | Enhancement | Non-Core Functionality | Contained Migration | Other -->

**Estimated Complexity Level:** Light DAB (4 required documents)

---

### DAB Light Submission Checklist

**Required Documents:**
- [ ] **01-business-context.md** – Business drivers, value proposition, key stakeholders
- [ ] **03-high-level-architecture.md** – Solution approach, technology choices, key components
- [ ] **08-security-design.md** – Security considerations, threat assessment, compliance alignment
- [ ] **09-dab-light-assessment.md** – Concise risk register, key assumptions, critical decisions

**Validation:**
- [ ] Architecture diagrams included and render correctly
- [ ] All blockers/dependencies clearly identified
- [ ] No major security or compliance concerns
- [ ] OpenAPI specs provided (if introducing new APIs)
- [ ] `.gitlab/reviewers.yml` configured for routing

**Process:**
- [ ] DAB slot requested and approved (issue link: _______)
- [ ] SA and domain architect sign-off obtained
- [ ] No blocking architecture exceptions outstanding

---

### Review Guidance

| Role | Primary Focus | Expected Turnaround |
|------|---------------|-------------------|
| **Domain Architect** | Architecture fitness, patterns alignment | 2-3 days |
| **Security Lead** | Security and compliance assessment | 2-3 days |
| **DAB Chair** | Governance approval | 1-2 days |

---

### Related Issues

**DAB Request Issue:** (link to dab-request issue)

**Blocked By / Blocks:**
- (Reference any dependencies or blocking items)

---

## What is DAB Light?

DAB Light is a streamlined approval process for lower-risk architectural changes:
- **Configuration changes** to approved platforms
- **Enhancements** to existing systems within defined domains
- **Non-core functionality** additions
- **Contained migrations** with clear rollback paths
- **Component replacements** within approved technology families

**Not suitable for DAB Light:**
- New major systems or significant integrations (use Full DAB)
- Cross-domain impacts affecting multiple business lines
- High-risk security or compliance implications
- Significant technology platform changes

---

## Next Steps

1. Ensure all 4 required documents are complete and in `dab/{project-name}/` directory
2. Self-review against checklist above
3. Submit MR for architectural review
4. Respond to feedback in follow-up commits
5. Approval by domain architect and security lead
6. DAB Chair approval completes the process

**Typical Timeline:** 5 business days

---

*For detailed guidance on the DAB Light process, see [DAB Process Documentation](../governance/dab-process.md)*
