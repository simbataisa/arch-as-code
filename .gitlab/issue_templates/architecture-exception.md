## Architecture Exception Request

### Overview

**Request ID:**
<!-- Will be auto-assigned -->

**Requestor:**
<!-- Name and Techcombank email -->

**Project/Initiative:**
<!-- Name of the project requesting the exception -->

**Submission Date:**
<!-- Today's date -->

**Requested Decision Date:**
<!-- By when do you need a decision? -->

---

### Standard Being Excepted

**Which Architecture Standard(s) are you requesting an exception to?**
<!-- Link to the specific standard in the knowledge base. Include standard name, version, and relevant requirements. -->

**Specific Requirement(s) Being Excepted:**
<!-- Quote or reference the exact requirement(s) that cannot be met -->

**Affected Architecture Pattern(s) (if applicable):**
<!-- If this relates to a specific pattern, link it here -->

---

### Business Justification

**Why is This Exception Necessary?**
<!-- Detailed explanation of business drivers and why the standard cannot be met -->

**Business Impact of NOT Granting Exception:**
<!-- What would happen if you had to comply? Cost, timeline, capability impact? -->

**Strategic Alignment:**
<!-- How does this exception support business objectives despite deviating from standards? -->

**Can You Work Within the Standard?**
- [ ] No – Impossible to meet standard given constraints (explain constraints)
- [ ] Technically feasible but requires significant additional cost/timeline
- [ ] Technically feasible but reduces desired business value/capability

**Cost/Timeline/Quality Impact of Staying Compliant:**
<!-- Quantify if possible: additional cost, timeline delay, reduced functionality, performance impact, etc. -->

---

### Technical Rationale

**Current Standard Requirement:**
<!-- Restate what the standard requires -->

**Proposed Alternative Approach:**
<!-- Detailed description of how you will deviate from the standard -->

**Why This Alternative?**
<!-- Technical justification for the alternative approach -->

**How Does Alternative Address the Original Intent of the Standard?**
<!-- Standards exist to solve problems. Does your alternative solve the same problems? -->

**Gap Analysis:**
| Standard Objective | How Standard Achieves It | How Your Alternative Achieves It | Residual Risk |
|---|---|---|---|
| (Objective 1) | | | |
| (Objective 2) | | | |
| (Objective 3) | | | |

---

### Alternatives Analyzed

**Option 1: Full Compliance**
- **Approach:** (Meet standard completely)
- **Cost:** (Estimate)
- **Timeline Impact:** (Estimate)
- **Technical Feasibility:** (Practical? Any blockers?)
- **Why Not Selected:** (Explain trade-off decision)

**Option 2: (Your Proposed Alternative)**
- **Approach:** (Describe deviation from standard)
- **Cost:** (Estimate)
- **Timeline Impact:** (Estimate)
- **Technical Feasibility:** (Practical? Proven elsewhere?)
- **Why Selected:** (Explain reasoning)

**Option 3: (Alternative approach)**
- **Approach:** (Other way to deviate or achieve objectives)
- **Cost:** (Estimate)
- **Timeline Impact:** (Estimate)
- **Why Not Selected:** (Explain trade-off decision)

---

### Risk Assessment

**What Risks Does This Exception Introduce?**

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|-----------|
| (e.g., Operational complexity) | High/Med/Low | High/Med/Low | (Specific control) |
| (e.g., Security gap) | | | |
| (e.g., Technology debt) | | | |
| (e.g., Maintainability concern) | | | |

**Unmitigated Risk Acceptance:**
<!-- If risks cannot be fully mitigated, are you accepting residual risk? -->
- [ ] Yes, accepting residual risk for: (specify)
- [ ] No, all risks must be mitigated

**Compensating Controls:**
<!-- What additional controls, monitoring, or governance will offset the risk? -->

1. (Control 1 and how it reduces risk)
2. (Control 2 and how it reduces risk)
3. (Control 3 and how it reduces risk)

**Monitoring & Compliance Verification:**
<!-- How will you verify this exception remains in good standing? -->
- Monitoring approach: (e.g., quarterly architecture review, metric tracking)
- Threshold for escalation: (When would you revisit this exception?)
- Responsible party: (Who ensures ongoing compliance?)

---

### Scope & Duration

**Is This Exception Time-Bound or Permanent?**
- [ ] **Time-Bound:** Expires on _____________ (date or event-based: e.g., "upon system replacement")
- [ ] **Permanent:** Exception applies indefinitely

**If Time-Bound, Provide:**
- Expiration date or triggering condition for re-evaluation
- Plan for coming into compliance at expiration: (How will you address the standard by then?)
- Quarterly review required: (Yes / No)

**If Permanent, Justify:**
<!-- Why is ongoing deviation acceptable? Should the standard itself be updated instead? -->

**Scope of Systems Affected:**
<!-- Which systems/teams/domains does this exception apply to? -->
- Applies to: (Specific system, component, or team)
- Does NOT apply to: (Other areas that must still comply)

**Precedent Check:**
<!-- Have similar exceptions been granted before? -->
- [ ] Similar exception(s) already exist: (link to prior exception)
- [ ] This is first exception for this standard
- [ ] Related exception under different standard: (link)

---

### Compliance & Governance

**Regulatory/Compliance Implications:**
<!-- Does this exception affect your compliance posture? -->
- [ ] No regulatory impact
- [ ] Possible regulatory implication (describe): _____
- [ ] Compliance/Legal review completed: (Link to review or approval)

**Audit Trail:**
- [ ] This exception has been documented for audit purposes
- [ ] Exception is traceable in architecture documentation
- [ ] Compensating controls are logged and monitored

**Cross-Domain Impact:**
<!-- Does this exception affect other domains or systems? -->
- [ ] Single-domain exception (confined to _____)
- [ ] Cross-domain implications: (describe impact on other areas)
- [ ] Stakeholder consultation completed: (who was consulted?)

---

### Supporting Evidence

**Attach or Link to:**
- [ ] Business case or project charter
- [ ] Technical design documentation
- [ ] PoC or feasibility study results
- [ ] Risk assessment or threat model
- [ ] Cost-benefit analysis
- [ ] Similar exceptions or precedents
- [ ] Compliance/Legal review (if applicable)

---

### Decision Authority & Approval

**This exception requires approval from:**
- [ ] **Domain Architect** (for domain-specific deviations)
- [ ] **Security Board** (for security-related exceptions)
- [ ] **Compliance/Risk** (for compliance implications)
- [ ] **EA Directors** (for significant enterprise-wide exceptions)

**Escalation Path (if needed):**
<!-- Who is final decision authority if there is disagreement? -->

---

### Review Checklist

Before submitting:

- [ ] Specific standard being excepted is clearly identified
- [ ] Business justification is compelling and quantified where possible
- [ ] Risk assessment is thorough and realistic
- [ ] Compensating controls are specific and measurable
- [ ] Duration (time-bound or permanent) is clearly defined
- [ ] Monitoring approach ensures ongoing compliance
- [ ] All supporting documentation is attached or linked
- [ ] Stakeholders have been consulted
- [ ] Compliance/regulatory implications addressed (if applicable)

---

### Timeline

**Exception Submission:** (Today)
**Target Decision Date:** (Specify when you need a decision)
**Typical Review Timeline:** 5-10 business days (depending on complexity)

**Approval Workflow:**
1. **Triage:** EA Board screens for completeness and relevance (1-2 days)
2. **Review:** Assigned reviewers assess exception (3-5 days)
3. **Decision:** Exception approved, approved with conditions, or declined (1-2 days)
4. **Communication:** Requestor notified with decision and any conditions (same day as approval)

---

### After Decision

**If Approved:**
- Exception is logged in exception registry
- Exception is referenced in relevant architecture documentation
- Quarterly reviews scheduled (if time-bound)
- Compensating controls are monitored per defined approach

**If Approved with Conditions:**
- Conditions must be met before exception takes effect
- Timeline for meeting conditions specified
- Re-review scheduled after conditions met

**If Declined:**
- Feedback provided on why exception cannot be granted
- Alternative paths forward discussed
- Option to appeal to EA Directors (if desired)

---

### Additional Context

**Comments or Concerns:**
<!-- Any additional information for reviewers -->

---

*For questions on the exception process, see [Architecture Exception Process](../governance/exception-process.md)*

/cc @techcombank/ea-board @techcombank/security-board
