# Active DAB Submissions

**Auto-generated — refreshed by CI/CD on schedule**

Last refresh: 2026-03-08 14:30 UTC | Refresh interval: Every 6 hours

---

## Currently Open Merge Requests

These are all DAB submissions currently under review, awaiting approvals.

### Summary

| Total Open | Days Open (Avg) | Oldest | Newest | Pending Approvals (Total) |
|-----------|-----------------|--------|--------|--------------------------|
| 3 | 12 days | 37 days | 7 days | 7 pending |

---

## Open Submissions Sorted by Days Open

### 1. Web Portal Redesign (37 days open)

**Domain:** Digital Channels
**Author:** [architect-name]
**MR Link:** [!1256](https://gitlab.techcombank.com/architecture/arch-as-code/-/merge_requests/1256)
**Submitted:** 2026-02-20

**Status:** 🔄 In Review

**Pending Approvals:**
- [ ] Primary Reviewer: @digital_lead (Pending 30 days)
- [ ] Technical Reviewer 1: @digital-channels-architect-1 (Pending 30 days)

**Days Open:** 37 days ⚠️ **ESCALATION RECOMMENDED**

**Recent Activity:**
- 2026-03-06: Developer addressed feedback, pushed new commits
- 2026-03-01: Architecture review comment: "Need clarification on caching strategy"
- 2026-02-28: @digital-channels-architect-2 approved

**Notes:** This submission has been open longer than typical (SLA: 7 days). Recommend escalation to domain lead if approvals not received within 2 days.

---

### 2. Real-time Analytics Pipeline (12 days open)

**Domain:** Data Platform
**Author:** [architect-name]
**MR Link:** [!1260](https://gitlab.techcombank.com/architecture/arch-as-code/-/merge_requests/1260)
**Submitted:** 2026-02-25

**Status:** 🔄 In Review

**Pending Approvals:**
- [ ] Primary Reviewer: @data_lead (Pending 12 days)
- [x] Technical Reviewer 1: @data-architect-1 (Approved 2026-03-05)
- [ ] Technical Reviewer 2: @data-architect-2 (Pending 3 days)

**Days Open:** 12 days (within SLA)

**Recent Activity:**
- 2026-03-05: @data-architect-1 approved with comments: "Good design, check storage costs"
- 2026-03-03: @data-architect-2 requested changes to backup strategy
- 2026-02-28: Initial submission review started

**Next Steps:** Awaiting approval from @data_lead and response from @data-architect-2

---

### 3. Payment Analytics Dashboard (7 days open)

**Domain:** Payments
**Author:** [architect-name]
**MR Link:** [!1267](https://gitlab.techcombank.com/architecture/arch-as-code/-/merge_requests/1267)
**Submitted:** 2026-03-01

**Status:** 🔄 In Review

**Pending Approvals:**
- [ ] Primary Reviewer: @payments_lead (Pending 7 days)
- [ ] Technical Reviewer 1: @payments-architect-1 (Pending 7 days)
- [ ] Technical Reviewer 2: @payments-architect-2 (Pending 7 days)

**Days Open:** 7 days (at SLA threshold)

**Recent Activity:**
- 2026-03-01: Submitted DAB (Full DAB)
- Auto-comments: CI/CD validation passed, registry updated

**Next Steps:** Awaiting initial review from payment domain architects

---

## Review SLA Status

### By Submission Age

| Submission | Days Open | SLA (Days) | Status | Action |
|-----------|-----------|-----------|--------|--------|
| Web Portal Redesign | 37 | 7 | 🔴 **OVERDUE** | Escalate to domain lead |
| Real-time Analytics Pipeline | 12 | 7 | 🟠 **AT RISK** | Follow up on pending reviews |
| Payment Analytics Dashboard | 7 | 7 | 🟡 **AT THRESHOLD** | Monitor, follow up tomorrow |

### SLA Thresholds

- 🟢 **Green (0-5 days):** On track
- 🟡 **Yellow (6-7 days):** Approaching SLA
- 🟠 **Orange (8-10 days):** At risk, follow-up recommended
- 🔴 **Red (>10 days):** Overdue, escalation recommended

---

## Pending Approvers

### Primary Reviewers Outstanding

| Approver | Domain | Reviews Outstanding | Days Awaiting | Status |
|----------|--------|-------------------|----------------|--------|
| @digital_lead | Digital Channels | 1 | 30 days | 🔴 CRITICAL |
| @data_lead | Data Platform | 1 | 12 days | 🟠 NEEDS ATTENTION |
| @payments_lead | Payments | 1 | 7 days | 🟡 MONITOR |

### Recommendations

**Immediate Action:**
- Contact @digital_lead regarding Web Portal Redesign (37 days overdue)
- Escalate through domain leadership if no response within 24 hours

**This Week:**
- Follow up with @data_lead on Real-time Analytics Pipeline
- Monitor @payments_lead for Payment Analytics Dashboard

---

## Submission Details

### Web Portal Redesign

```yaml
Type: Full DAB
Domain: Digital Channels
Project: web-portal-redesign-2026
Author: jane.smith@techcombank.com
Created: 2026-02-20 10:30 UTC
Last Updated: 2026-03-06 15:45 UTC

Description: |
  Complete redesign of customer web portal with new UI framework,
  improved accessibility, and enhanced performance.

  Key Changes:
  - Migration from legacy jQuery to React 18
  - New responsive design framework
  - Database optimization for portal queries
  - Integration with new auth service

Reviewers Required:
  Primary: @digital_lead (PENDING - 30 days)
  Technical: @digital-channels-architect-1 (PENDING - 30 days)
  Technical: @digital-channels-architect-2 (APPROVED - 2026-03-01)

Commits: 34
Comments: 12 (2 unresolved)
Changes Requested: 2
```

### Real-time Analytics Pipeline

```yaml
Type: Full DAB
Domain: Data Platform
Project: realtime-analytics-pipeline-2026
Author: john.doe@techcombank.com
Created: 2026-02-25 09:00 UTC
Last Updated: 2026-03-05 14:20 UTC

Description: |
  Real-time analytics pipeline using Kafka + Spark Streaming + Delta Lake.
  Enables sub-second latency reporting for business intelligence.

  Key Components:
  - Kafka topics for event streaming
  - Spark Streaming for transformations
  - Delta Lake for ACID compliance
  - Databricks notebooks for analytics

Reviewers Required:
  Primary: @data_lead (PENDING - 12 days)
  Technical: @data-architect-1 (APPROVED - 2026-03-05)
  Technical: @data-architect-2 (PENDING - 3 days, requested changes)

Commits: 18
Comments: 8 (1 unresolved - backup strategy)
Changes Requested: 1
```

### Payment Analytics Dashboard

```yaml
Type: Full DAB
Domain: Payments
Project: payment-analytics-dashboard-2026
Author: alice.johnson@techcombank.com
Created: 2026-03-01 08:15 UTC
Last Updated: 2026-03-01 08:15 UTC

Description: |
  Executive dashboard for real-time payment analytics and monitoring.
  Provides visibility into transaction volumes, routing performance,
  and system health.

  Components:
  - GraphQL API for data querying
  - React-based dashboard frontend
  - PostgreSQL analytical database
  - Elasticsearch for search/filters

Reviewers Required:
  Primary: @payments_lead (PENDING - 7 days)
  Technical: @payments-architect-1 (PENDING - 7 days)
  Technical: @payments-architect-2 (PENDING - 7 days)

Commits: 12
Comments: 0
Changes Requested: 0
```

---

## Recent Actions

### Last 7 Days

| Date | Action | Submission | Author |
|------|--------|-----------|--------|
| 2026-03-06 | New commits pushed | Web Portal Redesign | @jane.smith |
| 2026-03-05 | Approved with feedback | Real-time Analytics Pipeline | @data-architect-1 |
| 2026-03-03 | Changes requested | Real-time Analytics Pipeline | @data-architect-2 |
| 2026-03-01 | New submission | Payment Analytics Dashboard | @alice.johnson |

---

## How to Monitor Your Submission

### Check Status
1. Go to your MR: [View all open MRs](https://gitlab.techcombank.com/architecture/arch-as-code/-/merge_requests?state=opened&draft=no)
2. Click on your project name
3. See "Approval Status" section
4. Check who has approved/is pending

### Get Approval
1. Review pending approvers in the table above
2. Send them a reminder if >5 days
3. Address any "Changes Requested"
4. Push new commits to update the MR
5. Leave comment to re-notify reviewers

### Escalate If Stuck
1. Contact your domain lead (see domain-owners.yml)
2. Explain blocker or timeline pressure
3. Domain lead can expedite or provide guidance
4. For CISO escalations, contact security-architect

---

## FAQ

**Q: How long should review take?**
A: Light DAB: 3 days. Full DAB: 7 days. If overdue, escalate to domain lead.

**Q: My submission is stuck. What do I do?**
A: Contact the primary reviewer directly. Check domain-owners.yml for email.

**Q: How often is this list updated?**
A: Every 6 hours. Last update: 2026-03-08 14:30 UTC

**Q: Can I see old/closed submissions?**
A: Yes, see `dab-index.md` for approved/archived submissions.

**Q: How do I request a prioritized review?**
A: Contact your domain lead. They can work with reviewers to expedite if justified.

---

## For Architecture Reviewers

### Review Workload by Approver

| Approver | Domain | Pending | In Progress | This Week |
|----------|--------|---------|------------|-----------|
| @digital_lead | Digital Channels | 1 | 0 | Review Web Portal |
| @data_lead | Data Platform | 1 | 0 | Review Analytics Pipeline |
| @payments_lead | Payments | 1 | 0 | Review Dashboard |

### How to Approve

1. Open the MR link
2. Review all documents thoroughly
3. Check alignment with architecture principles
4. Leave comments or approve
5. Click "Approve" when ready
6. MR can be merged after all required approvals

### Escalation Contacts

- **Architecture Issues:** @chief_architect
- **Security Issues:** @security_architect
- **Compliance Issues:** @compliance_officer

---

For more information, see `registry/README.md` and `templates/README.md`.
