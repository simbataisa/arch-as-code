# DAB Submission Templates

This directory contains templates for Design Approval Board (DAB) submissions to Techcombank's Architecture-As-Code repository.

## Getting Started with DAB Submission

A Design Approval Board submission documents the architectural decisions, design rationale, and approval status of new systems or significant changes to existing architecture.

### Step 1: Determine Submission Type

Decide whether your project requires a **Full DAB** or **Light DAB** submission by reviewing the [DAB Governance Criteria](https://techcombank.com/architecture/dab-governance).

**Use Full DAB if:**
- New system/platform launch
- Cross-domain or enterprise-wide impact
- Significant integration with external systems
- Story point estimate >50
- Data classification involves regulated data

**Use Light DAB if:**
- Internal project within single domain
- Standard technology patterns
- Story point estimate ≤50
- Limited compliance implications

### Step 2: Copy Template to Your Domain

Copy the appropriate template folder to your domain path:

```bash
# For Full DAB
cp -r templates/dab-full/ domains/{your-domain}/dab/2026/{your-project}/

# For Light DAB
cp -r templates/dab-light/ domains/{your-domain}/dab/2026/{your-project}/
```

### Step 3: Fill in All Sections

Complete each section of the template with your project-specific information:

1. **01-business-context.md** — Problem statement, objectives, scope, requirements
2. **02-key-design-concerns.md** — Architectural trade-offs and design decisions (Full DAB only)
3. **03-high-level-architecture.md** — System diagrams and technology choices
4. **04-data-design.md** — Data models and schemas (Full DAB only)
5. **05-detailed-design.md** — Workflows and error handling (Full DAB only)
6. **06-integration-design.md** — External system integration (Full DAB only)
7. **07-infrastructure-design.md** — Deployment and scalability (Full DAB only)
8. **08-security-design.md** — Security, compliance, and threat models
9. **09-dab-light-assessment.md** — Complexity assessment and stakeholder sign-off

Remove instruction comments as you complete each section.

### Step 4: Configure Reviewers

Edit `reviewers.yml` to specify the architects and stakeholders required for approval:

```yaml
# Example
primary_reviewer: payments-lead
technical_reviewers:
  - @payments-architect-1
  - @payments-architect-2
stakeholder_reviewers:
  - @payments-lead
approval_required:
  primary: true
  technical: all
  stakeholder: at_least_one
```

### Step 5: Create Branch and Merge Request

```bash
# Create feature branch
git checkout -b dab/{domain}/{project}-2026

# Commit your DAB submission
git add domains/{your-domain}/dab/2026/{your-project}/
git commit -m "DAB submission: {Project Name}

- Full/Light DAB for {Project Name}
- Submitted by {Team}
- Targeting Q{Quarter} 2026 delivery"

# Push and create MR
git push -u origin dab/{domain}/{project}-2026
```

The MR title should follow: **[DAB-2026] {Project Name} — {Domain} Domain**

## DAB Template Files

### Full DAB (9 documents)
- Business Context
- Key Design Concerns
- High-Level Architecture
- Data Design
- Detailed Design (workflows, error handling)
- Integration Design
- Infrastructure Design
- Security Design
- DAB Light Assessment (justification for Full DAB)

### Light DAB (4 documents)
- Business Context
- High-Level Architecture
- Security Design
- DAB Light Assessment

Both include `reviewers.yml` for approval configuration.

## Resources

- [Techcombank Architecture Governance](https://techcombank.com/architecture)
- [Data Classification Standard](https://techcombank.com/data-classification)
- [C4 Model Documentation](https://c4model.com/)
- [ADR Template](adr/ADR-TEMPLATE.md)
- [Domain Architecture Registry](../registry/domain-owners.yml)

## Submission Process

1. Complete all template sections
2. Add supporting diagrams (Mermaid/visio exports)
3. Create MR to `main` branch
4. Request review from domain architects (via `reviewers.yml`)
5. Address feedback
6. Obtain all required approvals
7. Merge to `main` — registry auto-updates
8. Archive submission folder reference in changelog

## Questions?

Contact your domain architect or the Architecture Governance team.
