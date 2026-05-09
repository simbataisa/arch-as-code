## Architecture Knowledge Base Change

### Change Summary
<!-- Describe what is being changed and why -->

**Type of Change:**
<!-- Select: New Pattern | New Standard | Update to Existing Pattern | Update to Existing Standard | New Governance Policy | New Decision Record | Other -->

**Area(s) Affected:**
<!-- Select all that apply: Patterns | Standards | Principles | Governance | Domain Architecture | Technology Radar | Other -->

---

### Detailed Description

**What is Changing:**
<!-- Clearly describe the specific content changes, new patterns, or standards being introduced -->

**Why This Change:**
<!-- Business drivers, market conditions, technical improvements, or lessons learned -->

**Impact Assessment:**
<!-- Who will be affected by this change? Which projects, teams, or domains? -->

**Effective Date:**
<!-- When should this new pattern/standard be applied to new initiatives? -->

---

### Scope & Applicability

**Applies To:**
- [ ] All new projects and initiatives
- [ ] Specific domain(s): _______________
- [ ] Specific technology family/platform: _______________
- [ ] Existing systems (retroactive): Explain applicability
- [ ] Future modernization efforts only

**Backward Compatibility:**
- [ ] Backward compatible (no changes required to existing systems)
- [ ] Backward compatible with waiver/exception process
- [ ] Not backward compatible (requires migration timeline)

**If Not Backward Compatible, Provide:**
- [ ] Migration path or timeline for affected systems
- [ ] Transitional guidance for ongoing support
- [ ] Definition of "end of life" for deprecated approach

---

### Technical Content

### Pattern/Standard Definition

<!-- Include the detailed technical definition. For patterns, include:
- Intent (what problem does it solve?)
- Structure/Architecture (how is it implemented?)
- Participants/Responsibilities
- Consequences (benefits and trade-offs)
- Related patterns
- Known uses
- Example implementation or reference project

For standards, include:
- Requirement statement (what must be done)
- Why (rationale)
- Scope (what applies, what doesn't)
- Verification/Compliance method
- References and related standards -->

---

### Decision Rationale

**Alternatives Considered:**
1. (Alternative approach and why not selected)
2. (Alternative approach and why not selected)
3. (Alternative approach and why not selected)

**Key Trade-offs:**
| Aspect | Benefit | Trade-off |
|--------|---------|-----------|
| Example: Complexity | Improved performance | Increased operational overhead |
| | | |

**Alignment with Strategy:**
<!-- How does this support enterprise architecture strategy and principles? -->

**Industry Best Practice:**
<!-- References to industry standards, analyst reports, or proven precedents -->

---

### Governance & Review

**Stakeholder Review:**
- [ ] EA Board review and approval
- [ ] Domain architect(s) consultation
- [ ] Security Board review (if security-related)
- [ ] Compliance/Risk review (if compliance-related)
- [ ] Platform Operations consultation (if ops-related)

**Consultation Feedback:**
<!-- Summarize feedback received and how it's addressed in the proposal -->

**Related Architecture Decisions:**
- ADR-XXX: (link to any related decision records)

**Related Policies/Standards:**
- (Link to related governance documents or standards)

---

### Implementation Guidance

**How to Use This Pattern/Standard:**
<!-- Practical guidance, step-by-step instructions, or worked examples -->

**Common Pitfalls:**
- (Pitfall 1 and how to avoid it)
- (Pitfall 2 and how to avoid it)

**When to Consider Exceptions:**
<!-- Describe scenarios where exceptions to this standard might be justified and reference the exception request process -->

**Supporting Tools/Templates:**
- (Links to tools, templates, or reference implementations)

**Training/Onboarding:**
- (How will teams learn about this? Workshops, documentation links, examples?)

---

### Reference Materials

**Documentation Links:**
- Related architecture patterns: (links)
- Related standards: (links)
- Governance policies: (links)

**External References:**
- Industry standards or best practices cited
- Academic or technical publications
- Vendor documentation or frameworks

**Reference Implementations:**
- Project/system examples demonstrating this pattern/standard: (links to DAB submissions or project repos)

---

### Effective Date & Rollout

**Effective Date:** (For new projects/initiatives starting on or after this date)

**Rollout Communication:**
- [ ] Architecture community notification sent
- [ ] Knowledge base updated
- [ ] Team workshops scheduled
- [ ] Office hours/Q&A sessions planned

**Support & Questions:**
- POC for questions: (@team-lead or @email)
- Slack channel: (#architecture-patterns or similar)

---

### Review Checklist

Before submitting for approval:

**Content Quality:**
- [ ] Writing is clear, concise, and free of jargon (or jargon is well-defined)
- [ ] Diagrams are included and properly rendered (PlantUML/Mermaid)
- [ ] Examples are concrete and illustrative
- [ ] Related patterns/standards are properly cross-referenced

**Governance:**
- [ ] Naming follows repository conventions
- [ ] File is placed in correct directory structure
- [ ] CODEOWNERS assignment is appropriate
- [ ] No conflicts with existing policies

**Technical Soundness:**
- [ ] Solution is proven (reference implementations exist or PoC completed)
- [ ] Trade-offs are honestly presented
- [ ] Alignment with enterprise principles is clear
- [ ] Backward compatibility implications are addressed

---

## Approval Workflow

1. **Submit MR:** Ensure all sections complete
2. **Architecture Board Review:** Technical validation
3. **EA Directors Review:** Enterprise alignment
4. **Domain Architect Review:** Domain-specific implications (if applicable)
5. **Merge & Publication:** Approved changes published to knowledge base

**Expected Timeline:** 7-10 business days

---

## After Merge

- Knowledge base is automatically updated
- Architecture community notification is sent
- Change becomes effective on specified date
- Implementation guidance is monitored for feedback

---

*For questions on architectural governance, see [Architecture Standards & Governance](../governance/architecture-standards.md)*
