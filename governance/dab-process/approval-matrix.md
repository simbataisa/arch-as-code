# DAB Approval Matrix

This matrix defines which reviewer roles must approve each document type in Full and Light DAB submissions. Use this to understand approval requirements for your specific change.

**Legend:**
- **Required (R)** — Must review and approve before merge
- **Optional (O)** — Should review if expertise relevant; approval not blocking
- **N/A** — Not applicable to this process or document type

---

## Full DAB Approval Matrix

Complete design submission requiring all 9 documents and multi-reviewer consensus.

| Document Section | Solution Architect | Enterprise Architect | Security Architect | Infrastructure Architect | Data Architect | IT Ops / SRE |
|---|---|---|---|---|---|---|
| **01-Business Context & Requirements** | R | R | O | O | O | O |
| **02-High-Level Architecture** | R | R | O | O | O | O |
| **03-Detailed Design** | O | R | O | R | O | O |
| **04-Integration Points & Dependencies** | R | R | O | O | O | R |
| **05-Security & Compliance Assessment** | O | O | R | O | O | O |
| **06-Operational Requirements & Runbooks** | O | O | O | R | O | R |
| **07-Performance & Scalability Analysis** | O | O | O | R | O | O |
| **08-Migration & Rollout Strategy** | O | R | O | R | O | R |
| **09-Risk Assessment & Mitigation** | O | R | O | O | O | O |

### Full DAB Approval Workflow

1. **All Required (R) reviewers must approve** — Conditions must be satisfied before merge
2. **Optional (O) reviewers** — If they comment, submitter responds; approval not required but feedback should be addressed
3. **Enterprise Architect makes final decision** — Synthesizes all feedback and approves merge

### Full DAB Minimum Approval Set
**Always required, regardless of domain:**
- Solution Architect
- Enterprise Architect
- Security Architect (if any data handling, auth, or external integration)
- Infrastructure Architect (if deployment, scaling, or operations involved)
- IT Ops / SRE (if operational changes)

**May vary by initiative:**
- Data Architect (if data model, new sources, or classification changes)

---

## Light DAB Approval Matrix

Fast-track submission requiring 4 focused documents and single-reviewer approval.

| Document Section | Enterprise Architect | Security Architect |
|---|---|---|
| **01-Business Context & Requirements** | R | N/A |
| **02-High-Level Architecture** | R | N/A |
| **03-Security & Compliance Assessment** | R | O |
| **04-Risk Assessment & Mitigation** | R | N/A |

### Light DAB Approval Workflow

1. **Enterprise Architect reviews all 4 sections** — Primary decision maker
2. **Security Architect optional** — Consulted only if security aspects unclear or novel
3. **Approval decision within 3 business days** — Single reviewer approval (no consensus required)

### Light DAB Minimum Approval Set
- **Always:** Enterprise Architect
- **Conditionally:** Security Architect (if security-sensitive, e.g., authentication, encryption, PII handling)

---

## Role Definitions & Responsibilities

### Solution Architect (Full DAB Only)
**Responsibility:** Validate business context, domain fit, and integration approach.

**Approval criteria:**
- Problem statement is clear and compelling
- Requirements align with domain strategy
- Scope is appropriate (not too broad, not too narrow)
- Integration points identified with dependent teams consulted
- Success criteria are measurable

**Typical review time:** 2-3 business days

---

### Enterprise Architect (Full & Light DAB)
**Responsibility:** Ensure end-to-end design consistency, roadmap alignment, and overall technical merit.

**Approval criteria (Full DAB):**
- Design aligns with architectural standards and patterns
- Technology choices justified
- Scalability and performance plans are sound
- Operational runbooks are complete
- Risk assessment is thorough
- Rollout strategy minimizes business impact

**Approval criteria (Light DAB):**
- Change truly qualifies as Light DAB (not actually Full scope)
- Risk assessment is reasonable
- Rollback approach is clear

**Typical review time:** 2-3 business days (Full) | 1-2 business days (Light)

---

### Security Architect (Full DAB Required; Light DAB Optional)
**Responsibility:** Validate security baseline, data classification, compliance, and risk mitigation.

**Approval criteria:**
- Authentication mechanism (OAuth2/OIDC) implemented correctly
- Authorization model (RBAC minimum) enforced
- Encryption standards met (TLS 1.2+ transit, AES-256 at rest)
- Data classification properly applied
- Logging and audit trail complete (PII masked)
- Compliance frameworks (PCI-DSS, BCL, GDPR if relevant) addressed
- Vulnerability assessment completed
- No known security gaps or acceptable risk documented

**Typical review time:** 3-4 business days

---

### Infrastructure Architect (Full DAB Required; Light DAB Optional)
**Responsibility:** Validate deployment model, scalability, operational requirements, and performance.

**Approval criteria:**
- Deployment architecture (K8s, serverless, VMs) appropriate for scale
- Auto-scaling configuration reasonable for projected load
- SLA targets are achievable with proposed infrastructure
- Monitoring and alerting strategy covers critical paths
- Disaster recovery and backup procedures adequate
- Capacity planning aligns with growth projections
- Cost projections are realistic

**Typical review time:** 2-3 business days

---

### Data Architect (Full DAB Optional; Light DAB Optional)
**Responsibility:** Validate data models, classification, governance, and privacy compliance.

**Approval criteria (when involved):**
- Data model is normalized and efficient
- Data classification tier is appropriate
- Data retention policies are documented
- Privacy/GDPR compliance addressed (if PII)
- Integration with data lake/warehouse is sound
- Historical data handling is clear

**Typical review time:** 2-3 business days (consulted only if relevant)

---

### IT Operations / SRE (Full DAB Required; Light DAB Optional)
**Responsibility:** Validate operational runbooks, incident procedures, and production readiness.

**Approval criteria:**
- Runbooks are step-by-step, testable, and complete
- Incident response procedures are documented
- Backup/disaster recovery procedures validated
- Monitoring alert thresholds are realistic
- On-call handoff procedures are clear
- Change deployment procedure is safe
- Rollback procedure is proven and tested

**Typical review time:** 2-3 business days

---

## Approval Scenarios

### Scenario 1: New Payment Integration (Cross-Domain, External)
**DAB Type:** Full DAB

**Required Reviewers:**
- ✅ Solution Architect (payment domain context)
- ✅ Enterprise Architect (cross-domain impact)
- ✅ Security Architect (external integration, encryption, API keys)
- ✅ Infrastructure Architect (new service, scaling)
- ✅ IT Ops / SRE (new operational procedures)
- ⚠️ Data Architect (if sharing customer data with external partner)

**Approval order:**
1. Solution Architect approves business fit
2. Security Architect approves security design
3. Infrastructure Architect approves deployment
4. IT Ops approves operational procedures
5. Enterprise Architect synthesizes and approves merge

---

### Scenario 2: Redis Cache Addition (Performance Optimization)
**DAB Type:** Light DAB

**Required Reviewers:**
- ✅ Enterprise Architect (primary decision)
- ⚠️ Security Architect (only if cache stores sensitive data)

**Approval:**
1. Security Architect quickly confirms cache data is not sensitive
2. Enterprise Architect approves within 1-2 days
3. Merge to main

---

### Scenario 3: Spring Boot Version Upgrade (No Architecture Change)
**DAB Type:** Light DAB

**Required Reviewers:**
- ✅ Enterprise Architect (sole approver)

**Approval:**
1. Enterprise Architect reviews and approves within 1 day
2. Merge to main

---

### Scenario 4: New Microservice for Analytics (Single Domain, No External)
**DAB Type:** Full DAB

**Required Reviewers:**
- ✅ Solution Architect (analytics domain)
- ✅ Enterprise Architect (new service, consistency)
- ⚠️ Security Architect (if handling customer data)
- ✅ Infrastructure Architect (Kubernetes deployment, auto-scaling)
- ✅ IT Ops / SRE (runbooks, monitoring)

**Optional:**
- ⚠️ Data Architect (if new data source or model)

---

### Scenario 5: Database Index Optimization (Performance, Single Service)
**DAB Type:** Light DAB

**Required Reviewers:**
- ✅ Enterprise Architect (sole approver)

**Approval:**
1. Enterprise Architect reviews, approves same day
2. Merge and deploy

---

## Reviewer SLA Expectations

### Initial Response (within 2 business days)
- Acknowledgment that review has begun
- Any clarifying questions to submitter
- Estimated timeline for detailed review

### Detailed Review (within 5 business days)
- Complete review of all assigned sections
- Specific feedback with rationale
- Approval, conditional approval, or rejection

### Final Decision (Full DAB within 10 days; Light DAB within 5 days)
- Enterprise Architect makes final merge decision
- Commit tagged with approval
- Close DAB tracking issue

---

## Conflict Resolution

If reviewers disagree on approval:

1. **Level 1:** Domain Lead (Solution Architect + EA) mediate (2 days)
2. **Level 2:** EA Director arbitrates (2 days)
3. **Level 3:** CTO decides if still unresolved (1 day)

See [Escalation Procedure](./escalation-procedure.md) for detailed process.

---

## Customization Notes

This matrix applies to most Techcombank architecture submissions. Customizations may apply:

- **Regulatory initiatives** (PCI-DSS, BCL compliance) → Add Compliance Officer as required reviewer
- **High-stakes consumer facing** (retail banking features) → Add Product Manager as optional reviewer
- **Internal tools only** → May skip some SRE requirements if no production SLA
- **Emergency patches** → May use expedited review process (contact EAT lead)

---

## How to Use This Matrix

**For Submitters:**
1. Determine your initiative type (Full DAB or Light DAB)
2. Find applicable row in matrix
3. Identify all "Required (R)" reviewers — these must approve
4. Identify "Optional (O)" reviewers — engage if their expertise is relevant
5. Assign MR to all Required reviewers
6. Plan timeline based on SLA targets

**For Reviewers:**
1. Check matrix to confirm you're assigned
2. Review applicable sections only (not entire submission)
3. Provide feedback within SLA (2 days initial, 5 days detailed)
4. Approve or request changes
5. Escalate if disagreement with other reviewers

**For EA Director (Final Decision Maker):**
1. Verify all Required reviewers have signed off
2. Synthesize feedback from Optional reviewers (if any)
3. Make final approval/conditional/rejection decision
4. Merge when appropriate
5. Tag commit and close DAB
