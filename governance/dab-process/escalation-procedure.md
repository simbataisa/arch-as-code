# DAB Escalation Procedure

When reviewers disagree on DAB approval or deadlocks occur, follow this three-level escalation process to reach a timely decision.

---

## When to Escalate

Escalation is triggered by any of these situations:

1. **Reviewer Deadlock** — Two or more Required reviewers cannot reach consensus after discussion
2. **Blocking Objection** — A Required reviewer has a "Do Not Approve" position with unresolved concerns
3. **Timeline Breach** — Decision delayed beyond SLA target (Full DAB 10 days, Light DAB 5 days) without resolution path
4. **Architectural Conflict** — Fundamental disagreement on technology choice, pattern, or approach
5. **Risk Disagreement** — Reviewers cannot agree on risk level or mitigation adequacy

---

## Escalation Path

```
Reviewer Disagreement or Deadlock
         ↓
    Level 1: Domain Lead Mediation (2 business days)
         ↓
    If unresolved → Level 2: EA Director Arbitration (2 business days)
         ↓
    If unresolved → Level 3: CTO Decision (1 business day)
```

---

## Level 1: Domain Lead Mediation (2 Business Days)

### When to Escalate to Level 1
- Two Required reviewers disagree but both have legitimate concerns
- Discussion in MR comments has stalled (3+ days without progress)
- Submitter requests mediation to move forward

### Process
1. **Submitter or any reviewer** creates comment on MR with tag `@escalation-l1`
2. **Domain Lead** (determined by project domain) acknowledges within 24 hours
3. **Domain Lead facilitates** 30-minute sync meeting with:
   - Submitter (1-2 people)
   - Each disagreeing reviewer (1 representative each)
   - Any other stakeholders if needed

4. **During meeting:**
   - Each side presents position (5 min each)
   - Domain Lead asks clarifying questions
   - Group explores middle-ground compromises
   - Domain Lead listens for technical merit vs. personal preference

5. **Outcome (within 48 hours of meeting):**
   - **Consensus reached** → Domain Lead summarizes agreement in MR comment, proceeds to merge or conditional approval
   - **Compromise found** → Adjust design/requirements, submitter updates MR
   - **Unresolved** → Escalate to Level 2 with Domain Lead recommendation documented

### Typical Mediation Scenarios

**Scenario A: Technology Choice Disagreement**
- Reviewer 1: "Use Kafka for event streaming"
- Reviewer 2: "Use RabbitMQ, simpler for our needs"
- Domain Lead: "Proposes hybrid: use Kafka for high-volume payment events, RabbitMQ for lower-volume notifications"
- Outcome: Compromise accepted, design updated

**Scenario B: Scope Disagreement**
- Reviewer 1: "This requires Full DAB (10 days)"
- Reviewer 2: "Light DAB sufficient (5 days)"
- Domain Lead: "Assess: does it touch external integration or cross-domain? If yes, Full DAB. If no, Light DAB."
- Outcome: Scope re-assessed with clear criteria, process restarted at correct level

**Scenario C: Risk Tolerance**
- Security Architect: "TLS 1.1 not sufficient, require 1.3"
- Infrastructure Architect: "TLS 1.2 meets compliance, 1.3 adds complexity"
- Domain Lead: "Compliance requirement is clear, implement TLS 1.2 minimum; upgrade to 1.3 as separate task"
- Outcome: TLS 1.2 approved, TLS 1.3 planned for future

---

## Level 2: EA Director Arbitration (2 Business Days)

### When to Escalate to Level 2
- Level 1 mediation did not reach consensus
- Fundamental disagreement persists after compromise attempts
- Domain Lead documents unresolved points in escalation summary

### Process
1. **Domain Lead creates Level 2 escalation** in MR with tag `@escalation-l2`
2. **Escalation summary must include:**
   - Position of each disagreeing reviewer (bullet points)
   - Domain Lead's assessment and recommendation
   - Key facts or technical data
   - Why compromise was not possible

3. **EA Director** (Chief Enterprise Architect) acknowledges within 24 hours

4. **EA Director reviews:**
   - All documentation in DAB submission
   - MR discussion thread and escalation summary
   - May request additional information via async Q&A (does not require meeting)

5. **EA Director decision (within 48 hours):**
   - **Approves submission** — Submitter proceeds to merge per decision, applies tag
   - **Approves with conditions** — Lists specific changes before merge allowed
   - **Rejects submission** — Provides detailed rationale; submitter may resubmit if revised
   - **Remands to Level 1** — If more info needed from domain-level discussion

6. **MR status update:** EA Director comments final decision in MR; Submitter implements if conditional or closes if rejected.

### Typical Arbitration Scenarios

**Scenario A: Overriding Specialist Objection**
- Infrastructure Architect: "Kubernetes deployment adds operational complexity we're not ready for"
- EA Director: "Kubernetes is standard in our roadmap; IR Architect trains team in parallel effort. Approved with training plan."
- Outcome: Approved with condition; Infra Architect begins training sprint

**Scenario B: Technology Veto**
- Security Architect: "NoSQL database violates our data consistency requirements for payments"
- Submitter: "MongoDB improves performance"
- EA Director: "For payment domain, ACID transactions are non-negotiable. Use relational DB. Rejected."
- Outcome: Rejected; Submitter redesigns with SQL database

**Scenario C: Risk vs. Speed Tradeoff**
- Reviewer 1: "Canary rollout takes 3 weeks, proposal is too slow"
- Reviewer 2: "Big-bang rollout is too risky with untested microservice"
- EA Director: "Balanced decision: 2-phase canary (Week 1: 10%, Week 2: 100%), meets safety and timeline"
- Outcome: Approved with modified rollout plan

---

## Level 3: CTO Decision (1 Business Day)

### When to Escalate to Level 3
- EA Director decision challenged by submitter or reviewer (rare)
- Architectural decision has company-wide strategic implications
- Persistent disagreement threatens roadmap timeline

### Process
1. **EA Director (or submitter with EA approval)** creates Level 3 escalation with tag `@escalation-l3`
2. **Escalation package must include:**
   - Level 2 decision and rationale
   - Specific reason Level 2 decision is being challenged
   - Proposed alternative or concern
   - Impact if escalation not resolved (business, technical, timeline)

3. **CTO** (Chief Technology Officer) reviews package

4. **CTO decision (within 24 hours):**
   - **Upholds Level 2 decision** — Finalized; no further escalation
   - **Overrides Level 2 decision** — Provides new direction; finalized
   - **Sends back to Level 2** — With specific guidance for reconsideration

5. **Decision is final.** No further escalation.

### Typical Level 3 Scenarios (Very Rare)

**Scenario A: Strategic Technology Choice**
- DAB rejected because proposal uses new programming language not approved
- Submitter argues new language needed for team velocity
- CTO: "We're standardizing on 3 languages; yours isn't approved. Rejected. Use approved stack."
- Outcome: Finalized; submitter redesigns

**Scenario B: Precedent-Setting Decision**
- DAB approved by EA Director for distributed transaction pattern
- Later DAB cites this as precedent for unapproved pattern
- CTO: "Pattern approved only for payment domain. Overrides subsequent decision in other domain."
- Outcome: Finalized; original DAB stands; newer DAB rejected

---

## Escalation Timeline Summary

| Level | Trigger | Participants | Timeline | Decision Maker |
|-------|---------|--------------|----------|---|
| **Level 1** | Reviewer disagreement | Domain Lead, reviewers, submitter | 2 business days | Domain Lead |
| **Level 2** | Level 1 unresolved | EA Director, Domain Lead | 2 business days | EA Director |
| **Level 3** | Level 2 challenged | CTO, EA Director | 1 business day | CTO |

**Total escalation time:** Up to 5 business days (Level 1 + 2 + 3).

---

## Guidelines for Escalation Discussions

### For Submitters
- Escalate only if genuinely stuck (not for expedited review)
- Provide complete context so escalators don't need to re-read full DAB
- Be open to compromise; escalation is for deadlocks, not disagreements

### For Escalators (Domain Lead, EA Director, CTO)
- Ask questions to understand underlying concerns, not just positions
- Look for interests behind positions (e.g., "slow rollout" may hide "unproven team")
- Favor technical data over opinion (load tests > intuition)
- Document decision rationale for future precedent

### For Reviewers in Escalation
- Be constructive; avoid personal objections
- Focus on risk and technical merit, not ego
- Accept escalation decision as final; no re-litigation

---

## SLA Impact of Escalation

**Base SLA:**
- Full DAB: 10 business days end-to-end
- Light DAB: 5 business days end-to-end

**If escalated to Level 1:** Add 2 business days
**If escalated to Level 2:** Add 2 more business days (4 total)
**If escalated to Level 3:** Add 1 more business day (5 total)

**Example timeline:**
- Day 1-3: Initial review
- Day 3-5: Disagreement emerges
- Day 5-7: Level 1 mediation (add 2 days)
- Day 7-9: Level 2 arbitration (add 2 days)
- Day 9-10: CTO decision (add 1 day)
- Total: 10 business days (Full DAB, no buffer)

**Note:** If escalation causes breach, root cause is not escalation process but initial review delays. Teams should aim to identify disagreements by Day 3.

---

## Appeals Process

Decisions from EA Director and CTO are **final and not appealable** within the same DAB submission.

**After decision, options:**
1. **Accept decision** and proceed with approved design (or resubmit if rejected)
2. **Resubmit later** with significant new information or changed requirements
3. **Request ADR review** if decision conflicts with established architecture standards (separate process, not escalation)

---

## Conflict Prevention Best Practices

To minimize escalations:

1. **Early alignment** — Solution Architect and Enterprise Architect align before MR created
2. **Stakeholder pre-review** — Security Architect reviews Section 5 draft before final submission
3. **Clear requirements** — Business context (Section 1) is explicit about constraints, not ambiguous
4. **Risk documentation** — Risks identified early, not discovered during review
5. **Compromise-ready design** — Design allows flexibility (e.g., "Kafka preferred, RabbitMQ acceptable for Phase 2")

---

## Escalation Metrics

The DAB governance team tracks:
- % of submissions requiring escalation (target: <10%)
- Average time to resolution at each level
- Frequency of escalation type (disagreement, deadlock, timeline breach)
- Reviewer patterns (who escalates most frequently?)

**If escalation rate exceeds 15%**, conduct retrospective to improve process.

---

## Contact Information

**Level 1 Escalation (Domain Leads by Domain):**
- Payments: [Domain Lead Name]
- Cards: [Domain Lead Name]
- Deposits: [Domain Lead Name]
- Lending: [Domain Lead Name]
- Operations: [Domain Lead Name]
- Technology: [Domain Lead Name]

**Level 2 Escalation (EA Director):**
- [EA Director Name] — Email: [email] | Slack: @ea-director

**Level 3 Escalation (CTO):**
- [CTO Name] — Email: [email] | Slack: @cto

---

## Related Documents
- [DAB Full Process](./dab-full-process.md)
- [DAB Light Process](./dab-light-process.md)
- [Approval Matrix](./approval-matrix.md)
- [SLA Targets](./sla-targets.md)
