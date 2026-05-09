# DAB SLA Targets

Service Level Agreements for Design Approval Board review cycles. Defines expected turnaround times per role and process type.

---

## Full DAB Process SLA (10 Business Days End-to-End)

### Phase-by-Phase Breakdown

| Phase | Duration | Participants | Success Criteria |
|-------|----------|--------------|---|
| **Phase 1: Initiation** | Day 0-1 (1 day) | Submitter, Reviewer Assignments | MR created, reviewers assigned, labels applied |
| **Phase 2: Quality Gate** | Day 1-3 (2 days) | CI/CD automation | All 9 documents validated, naming conventions met, syntax correct |
| **Phase 3: Peer Review** | Day 3-5 (2 days) | Solution Architect, Domain Lead | Business context approved, architecture fit assessed, initial feedback provided |
| **Phase 4: Specialist Review** | Day 5-8 (3 days) | Security, Infrastructure, Data Architects | Deep-dive reviews complete, conditions/objections documented |
| **Phase 5: EA Approval** | Day 8-10 (2 days) | Enterprise Architect, final decision | Decision made, merge executed, commit tagged |

### Per-Reviewer SLA within Full DAB

#### Solution Architect (Full DAB Only)
| Milestone | SLA | Expectation |
|-----------|-----|---|
| Initial response | Within 2 business days | Acknowledge receipt, ask clarifying questions |
| Detailed review (Section 1-2, 4) | Within 5 business days | Complete assessment of business context and integration points |
| Final approval/rejection | Within 5 business days | Approve for specialist review or reject with rationale |

#### Enterprise Architect (Full & Light DAB)
| Milestone | SLA | Expectation |
|-----------|-----|---|
| Initial response | Within 2 business days | Confirm review started, coordinate with other reviewers |
| Detailed review | Within 5 business days | Assess full design, synthesize specialist feedback |
| **Final decision (critical path)** | **Within 10 business days total** | **Approve, conditional, or reject; merge or close MR** |

#### Security Architect (Full DAB Required)
| Milestone | SLA | Expectation |
|-----------|-----|---|
| Initial response | Within 2 business days | Acknowledge, identify any obvious security gaps |
| Detailed review (Section 5) | Within 5 business days | Complete security assessment, approve or conditions |
| Feedback implementation | Within 5 business days | Respond to submitter if changes needed |

#### Infrastructure Architect (Full DAB Required)
| Milestone | SLA | Expectation |
|-----------|-----|---|
| Initial response | Within 2 business days | Acknowledge, initial scalability/deployment assessment |
| Detailed review (Section 6-7) | Within 5 business days | Operational and performance review complete |
| Approval or conditions | Within 5 business days | Support for production readiness |

#### IT Ops / SRE (Full DAB Required)
| Milestone | SLA | Expectation |
|-----------|-----|---|
| Initial response | Within 2 business days | Acknowledge, review operational requirements |
| Detailed review (Section 6) | Within 5 business days | Runbook validation, incident procedure assessment |
| Approval or conditions | Within 5 business days | Confirm operational procedures are sound |

#### Data Architect (Full DAB Optional)
| Milestone | SLA | Expectation |
|-----------|-----|---|
| Initial response (if consulted) | Within 2 business days | Acknowledge, ask clarifying questions about data model |
| Detailed review | Within 5 business days | Data governance assessment if needed |

---

## Light DAB Process SLA (5 Business Days End-to-End)

### Phase-by-Phase Breakdown

| Phase | Duration | Participants | Success Criteria |
|-------|----------|--------------|---|
| **Phase 1: Initiation** | Day 0-1 (same-day) | Submitter, EA assigned | MR created, assigned, labels applied |
| **Phase 2: Quality Gate** | Day 1-2 (1 day) | CI/CD automation | All 4 documents present, formatting valid |
| **Phase 3: EA Fast-Track** | Day 2-5 (3 days) | Enterprise Architect, optional Security | Decision made, approved/rejected, merge executed |

### Per-Reviewer SLA within Light DAB

#### Enterprise Architect (Primary, Light DAB)
| Milestone | SLA | Expectation |
|-----------|-----|---|
| Initial review | Within 2 business days | Quick scan of 4 sections (15-20 minute target) |
| **Final decision** | **Within 5 business days total** | **Approve, conditional, or reject; merge or close** |

#### Security Architect (Optional, Light DAB)
| Milestone | SLA | Expectation |
|-----------|-----|---|
| Initial response (if consulted) | Within 1 business day | Quick assessment of security implications |
| Final input | Within 3 business days | Thumbs-up or concerns to feed into EA decision |

---

## SLA Breach Escalation

### What Constitutes a Breach

- **Full DAB:** No response from Required reviewer by Day 5; no decision by Day 10
- **Light DAB:** No response from EA by Day 3; no decision by Day 5

### Escalation on Breach

If a reviewer breaches SLA:

1. **Day 2 of breach:** Submitter or team lead sends reminder message (Slack/email)
2. **Day 3 of breach:** Manager of breaching reviewer is notified
3. **Day 4 of breach:** Escalation to EAT Leadership — consider:
   - Reassigning review to backup reviewer
   - Time-boxing remaining review (must complete within 24 hours)
   - Provisional approval pending retroactive review (rare, for business-critical submissions)

### Escalation Timeline

| Days Overdue | Action |
|---|---|
| 1-2 days | Friendly reminder to reviewer |
| 3-4 days | Escalate to reviewer's manager + EAT lead |
| 5+ days | Reassign review to backup reviewer; original reviewer provides notes |

### Preventing SLA Breaches

Best practices to keep DAB on track:

**For Submitters:**
- Plan submissions 2 weeks in advance (not rushed)
- Respond to feedback within 24 hours
- Don't ignore feedback; escalate if unclear

**For Reviewers:**
- Block calendar time for scheduled DAB reviews
- Acknowledge receipt within 1 business day (even if can't start review)
- Use escalation if you need more context; don't just go silent
- If on vacation, delegate to backup before departure

**For EAT Leadership:**
- Redistribute workload if reviewer consistently breaches
- Ensure adequate staffing in specialist roles (Security, Infra, etc.)
- Track per-reviewer SLA metrics quarterly

---

## Response Time vs. Decision Quality

**SLA targets are conservative** — designed to provide adequate review depth while keeping process efficient.

| Role | Initial Response | Detailed Review | Notes |
|------|---|---|---|
| Solution Architect | 2 days | 5 days | May ask questions; don't expect instant approval |
| Enterprise Architect | 2 days | 10 days (Full) / 5 days (Light) | Synthesis step takes time after specialist input |
| Security Architect | 2 days | 5 days | May request external security assessment if novel approach |
| Infrastructure Architect | 2 days | 5 days | May require load test coordination |
| IT Ops / SRE | 2 days | 5 days | May need to coordinate runbook validation |

**Quality > Speed:** If reviewer needs more time to do thorough review, escalate rather than rush approval.

---

## Fast-Track Exceptions

Rare business-critical submissions may request **expedited DAB** (3-5 day target for Full DAB). Requires:
- CTO or EVP business sponsorship
- Clear business justification (regulatory deadline, security incident, competitive threat)
- Willingness to operate with increased risk (accept conditional approval rather than blocking review)
- Mandatory post-go-live mini-review (within 2 weeks) to confirm assumptions

**Process:**
1. Submitter requests expedited review in MR description
2. EA Director evaluates business case
3. If approved, core reviewers (EA, Security, Infra) form "fast-track" team
4. Synchronous kickoff meeting to align on scope
5. Async review with daily progress check-ins
6. Decision target: Day 3-5 depending on complexity

**Expedited SLA:**
- Day 1: Initiation + Quality gate
- Day 2-3: Core reviewer input (parallel, not sequential)
- Day 3-5: EA decision

---

## Monitoring & Reporting

### Metrics Tracked by EAT Governance

**Submission Health:**
- Average Full DAB cycle time (current: 7 business days, target: ≤10)
- Average Light DAB cycle time (current: 3 business days, target: ≤5)
- % of submissions meeting SLA (target: ≥95%)

**Reviewer Performance:**
- Per-reviewer average response time
- % of responses meeting SLA targets
- % of reviews requiring follow-up questions (indicates thoroughness vs. speed)

**Process Health:**
- % of submissions requiring escalation (target: <10%)
- Average escalation resolution time
- % of escalated submissions ultimately approved (should be >80%)

### Monthly Reporting

EAT Governance publishes monthly metrics:
- Total DABs submitted (Full vs. Light breakdown)
- SLA compliance rate
- Escalation rate and resolution
- Cycle time trends (improving, stable, degrading?)
- Bottleneck analysis (which phase consistently delays?)

---

## SLA Waivers

Waivers are **not granted** for process compliance (e.g., "we skipped peer review, can EA approve faster?").

Waivers **may be granted** for reviewer unavailability:
- Reviewer on medical leave → Delegate to backup
- Reviewer on vacation → Cover arranged in advance
- Reviewer departed company → Reassign to permanent replacement

**Waiver process:** Manager of reviewer requests exception, documents reason. EAT Director approves and reassigns.

---

## Related Documents
- [DAB Full Process](./dab-full-process.md)
- [DAB Light Process](./dab-light-process.md)
- [Escalation Procedure](./escalation-procedure.md)
- [Approval Matrix](./approval-matrix.md)
