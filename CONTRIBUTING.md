# Contributing to Techcombank Architecture

Thank you for contributing to our Architecture-As-Code repository! This guide covers the Design Approval Board (DAB) process, code standards, and our collaborative review practices.

## Table of Contents

- [DAB Submission Process](#dab-submission-process)
- [Branching Strategy](#branching-strategy)
- [Merge Request Conventions](#merge-request-conventions)
- [Writing Guidelines](#writing-guidelines)
- [Review Process](#review-process)
- [Adding/Updating Architecture Patterns](#addingupdating-architecture-patterns)
- [Code of Conduct](#code-of-conduct)

---

## DAB Submission Process

### Overview

A Design Approval Board (DAB) submission documents and governs major architectural changes. DABs are the formal mechanism for architecture governance at Techcombank.

### Full DAB vs Light DAB

**Full DAB** (9 documents) - Required for:
- New platform/system initiatives
- Major architectural decisions affecting multiple domains
- Systems handling sensitive data or regulatory requirements
- Projects estimated >3 months effort

**Light DAB** (subset of documents) - Acceptable for:
- Incremental improvements to existing systems
- Single-domain changes with low cross-domain impact
- Quick wins and operational improvements

### Step-by-Step Submission

#### 1. Check for Existing Work

```bash
# Search for similar DABs in your domain
ls -la domains/{your-domain}/dab/
```

#### 2. Create Your DAB Folder

```bash
# Full DAB structure (2026 example)
mkdir -p domains/{domain}/dab/2026/{project-slug}/

# Create documents (01-09 for full, subset for light)
touch 01-business-context.md
touch 02-key-design-concerns.md
touch 03-high-level-architecture.md
touch 04-data-design.md
touch 05-detailed-design.md
touch 06-integration-design.md
touch 07-infrastructure-design.md
touch 08-security-design.md
touch 09-dab-light-assessment.md
```

#### 3. Complete the DAB Documents

Each document should:
- Start with a clear heading (e.g., `# I. BUSINESS CONTEXT`)
- Include a breadcrumb link back to an index (optional but recommended)
- Use Markdown with semantic headers (`#`, `##`, `###`)
- Include diagrams (Mermaid recommended, PlantUML acceptable)
- Have clear section references throughout

**Document Requirements:**

| # | Document | Purpose | Full DAB | Light DAB |
|---|----------|---------|----------|-----------|
| **01** | Business Context | Problem statement, opportunities, business drivers | ✅ | ✅ |
| **02** | Key Design Concerns | Constraints, risks, non-functional requirements | ✅ | ✅ |
| **03** | High-Level Architecture | System boundary, major components, key flows | ✅ | ✅ |
| **04** | Data Design | Data models, storage, integration patterns | ✅ | Optional |
| **05** | Detailed Design | Component deep-dives, technology choices, rationales | ✅ | Optional |
| **06** | Integration Design | External APIs, webhook patterns, integration sequence | ✅ | Optional |
| **07** | Infrastructure Design | Deployment, scalability, resilience patterns | ✅ | Optional |
| **08** | Security Design | Authentication, authorization, data protection | ✅ | Optional |
| **09** | DAB Light Assessment | Justification for lightweight review | ✅ | ✅ |

#### 4. Add Diagrams and Supporting Files

```bash
# Create an architecture diagram using Mermaid
cat > 03-high-level-architecture.md << 'EOF'
## 3.1 System Architecture

\`\`\`mermaid
graph TB
    API[API Gateway] --> SA[Service A]
    API --> SB[Service B]
    SA --> DB1[(Database A)]
    SB --> DB2[(Database B)]
\`\`\`
EOF

# Optional: Include OpenAPI spec for REST APIs
touch openapi.yaml
touch openapi-reference.md  # Auto-generated from openapi.yaml
```

#### 5. Create reviewers.yml

Copy and customize the reviewer assignment file:

```bash
cp templates/reviewers-template.yml reviewers.yml
# Edit with your domain's specialist architects
```

See [reviewers.yml Format](#reviewersynml-format) below.

#### 6. Validate Your Submission

```bash
# Run automated validation
./scripts/validate-dab-structure.sh domains/{domain}/dab/2026/{project-slug}/

# Check for broken links
./scripts/check-links.sh domains/{domain}/dab/2026/{project-slug}/
```

#### 7. Create a Merge Request

```bash
git checkout -b dab/{domain}/{project-slug}
git add domains/{domain}/dab/2026/{project-slug}/
git commit -m "DAB: {Project Name} in {Domain}"
git push origin dab/{domain}/{project-slug}
```

Then open a MR using the **DAB Full** or **DAB Light** template (`.gitlab/merge_request_templates/`).

#### 8. Iterate on Feedback

- Address reviewer comments in follow-up commits
- CI/CD runs validation on every push
- Use `/assign` in comments to request specific reviewers
- DAB approval requires sign-off from EA Board, Security Board, and domain specialists

---

## Branching Strategy

### Branch Naming Convention

All DAB submissions use the `dab/` prefix:

```
dab/{domain}/{project-slug}
└─ {domain}        = domains/{domain}/ folder (e.g., payments, lending)
└─ {project-slug}  = kebab-case project identifier (e.g., payment-saga-platform)
```

### Examples

```bash
dab/payments/payment-saga-platform
dab/lending/credit-scoring-v2
dab/core-banking/account-provisioning
dab/data-platform/events-streaming-hub
```

### Non-DAB Branches

For other repository changes (pattern updates, docs, tooling):

```bash
feature/pattern-{name}
docs/update-{section}
chore/update-mkdocs-config
fix/broken-link-in-{domain}
```

---

## Merge Request Conventions

### MR Title Format

**For DABs:**
```
[DAB] {Project Name} — {Domain} | {Status}
```

Examples:
```
[DAB] Payment SAGA Platform — Payments | Full Review
[DAB] Credit Scoring v2 — Lending | Light Review
```

**For non-DAB changes:**
```
{Type}: {Description}
```

Examples:
```
docs: Update principles in knowledge-base
feat: Add saga-pattern template
fix: Correct broken links in core-banking
```

### MR Description Template

Use the appropriate template from `.gitlab/merge_request_templates/`:

- `dab-full.md` — Full DAB (9 documents)
- `dab-light.md` — Light DAB (subset)
- `architecture-change.md` — Other architectural changes

### Reviewer Assignment

The CI/CD pipeline automatically assigns reviewers based on `reviewers.yml`:

```yaml
# reviewers.yml in your DAB folder
project:
  name: "Payment SAGA Platform"
  domain: "Payments"
  submitted_by: "@your-username"
  submission_date: "2026-03-08"

reviewers:
  solution_architecture:
    approvers:
      - "@sa-john"
      - "@sa-jane"
    required: 1
  data_architecture:
    approvers:
      - "@data-arch-1"
    required: 1
```

**Fixed reviewers** (always required, enforced via `CODEOWNERS`):
- EA Board (1 approval)
- Security Board (1 approval)
- EA Directors (assessment sign-off)

---

## Writing Guidelines

### Markdown Standards

1. **Headings**: Use semantic hierarchy (H1 → H2 → H3)
   ```markdown
   # I. BUSINESS CONTEXT          ← Document title (H1)
   ## 1.1 Introduction            ← Major section (H2)
   ### 1.1.1 Problem Details      ← Subsection (H3)
   ```

2. **Emphasis**: Use *italics* for emphasis, **bold** for definitions
   ```markdown
   The **Event Sourcing** pattern is an *architectural style*...
   ```

3. **Code Blocks**: Always specify language
   ```markdown
   \`\`\`java
   public class PaymentService {
       // Code here
   }
   \`\`\`
   ```

4. **Links**: Use relative paths within the repo
   ```markdown
   [See integration design](06-integration-design.md)
   [Payment pattern](../../knowledge-base/patterns/payment-saga.md)
   ```

5. **Tables**: Use standard Markdown tables with alignment
   ```markdown
   | Component | Responsibility | Technology |
   |-----------|----------------|------------|
   | Service A | Order processing | Spring Boot |
   | Service B | Payment handling | Quarkus |
   ```

### Diagrams

**Preferred: Mermaid** (renders directly in GitLab)

```markdown
\`\`\`mermaid
graph LR
    Client -->|HTTP| API[API Gateway]
    API -->|gRPC| ServiceA[Service A]
    API -->|gRPC| ServiceB[Service B]
    ServiceA --> DB1[(PostgreSQL)]
    ServiceB --> Cache[(Redis)]
\`\`\`
```

**Alternative: PlantUML** (requires plugin)

```
@startuml
Client -> API: HTTP
API -> ServiceA: gRPC
API -> ServiceB: gRPC
@enduml
```

### File Naming Conventions

```
{domain}/
├── dab/
│   └── {year}/
│       └── {project-slug}/
│           ├── 01-business-context.md
│           ├── 02-key-design-concerns.md
│           ├── reviewers.yml
│           ├── openapi.yaml
│           └── decisions/
│               ├── adr-001-event-sourcing.md
│               └── adr-002-saga-pattern.md
│
└── knowledge-base/
    └── patterns/
        ├── event-sourcing.md
        ├── saga-pattern.md
        └── cqrs.md
```

**Naming rules:**
- Use kebab-case for files and folders
- Prefix DAB documents with zero-padded numbers (01, 02, ..., 09)
- Decision records use `adr-NNN-description.md` format
- Use descriptive names that reflect content

### Best Practices

- **Be concise**: Aim for 1-2 pages per section in 01-03 documents
- **Use diagrams**: Every architectural document should have at least one diagram
- **Link consistently**: Reference other documents and patterns throughout
- **Include rationale**: Explain *why* decisions were made, not just *what*
- **Define terms**: Use a glossary for domain-specific terminology
- **Reference standards**: Link to applicable governance standards
- **Consider alternatives**: Mention rejected options and why they didn't fit

---

## Review Process

### Timeline & SLAs

| Stage | Participants | SLA | Notes |
|-------|--------------|-----|-------|
| **Initial Review** | Domain specialists | 3 business days | First pass on technical content |
| **Specialist Rounds** | Data/Integration/Infra leads | 5 business days | Parallel reviews based on responsibility |
| **Security Review** | Security Board | 5 business days | Mandatory for all DABs |
| **EA Board Review** | EA Board + Directors | 7 business days | Final approval authority |

### Review Workflow

```
1. Author submits DAB via MR
   ↓
2. CI/CD validation runs (link checks, Mermaid syntax, YAML)
   ↓
3. Domain specialists review (parallel)
   ↓
4. Security Board review (mandatory)
   ↓
5. EA Board review & approval
   ↓
6. Author creates issue to track implementation
   ↓
7. MR is merged; DAB is official
```

### How Reviewers Provide Feedback

Reviewers should:

1. **Approve sections**, not just skim
   - Write: "Approved: 02-key-design-concerns.md — rationale is clear"
   - Not: "LGTM"

2. **Request changes clearly**
   - Use GitLab's suggestion feature for specific edits
   - Reference section numbers and lines
   - Explain *why* the change is needed

3. **Ask clarifying questions**
   - Use conversation threads, not comments on every change
   - Group related questions

4. **Maintain professionalism**
   - See [Code of Conduct](#code-of-conduct) below

### How Authors Respond to Feedback

1. **Address every comment** — respond or commit a fix
2. **Explain decisions** if disagreeing with feedback
3. **Update the MR description** to reflect changes made
4. **Tag reviewers** when re-requesting review (use `/reassign`)
5. **Mark conversations as resolved** only after reviewer approves the fix

### Approval Rules

A DAB is **approved** when:

- ✅ All domain specialists have approved their sections
- ✅ Security Board has approved
- ✅ EA Board has approved
- ✅ CI/CD checks pass (linting, validation, links)
- ✅ No unresolved conversations

### Escalation Path

If a reviewer disagrees with a decision:

1. **Author and reviewer discuss** in MR thread
2. **Domain lead or EA Board** mediates if consensus isn't reached
3. **EA Directors** make final decision

---

## Adding/Updating Architecture Patterns

### When to Add a Pattern

A new pattern should be added to `knowledge-base/patterns/` when:
- Multiple DABs reference the same approach
- The approach solves a recurring problem
- It has been validated in production

### Creating a Pattern

```bash
# Create a new pattern
cat > knowledge-base/patterns/my-pattern.md << 'EOF'
# My Pattern Name

## Intent

Brief description of what problem this solves.

## Context

When should this pattern be used? What constraints apply?

## Solution

How does this pattern work? Include a diagram.

\`\`\`mermaid
graph TB
    ...
\`\`\`

## Example

Link to a DAB that successfully applied this pattern:
- [Payment SAGA Platform](../../domains/payments/dab/2026/payment-saga-platform/)

## Trade-offs

What are the pros and cons?

- **Pros**: ...
- **Cons**: ...

## See Also

- [Another pattern](other-pattern.md)
- [Related principle](../principles/README.md)
EOF
```

### Updating Existing Patterns

1. Create a branch: `feature/pattern-{pattern-name}`
2. Update the pattern file
3. Update any DABs that reference it (cross-link)
4. Create MR for review by pattern maintainers
5. Patterns require approval from EA Board

---

## Code of Conduct

### Principles for Collaboration

1. **Assume good intent** — treat all feedback as coming from a place of improving the architecture
2. **Be specific** — reference exact sections, lines, or examples
3. **Ask questions first** — try to understand context before critiquing
4. **Focus on ideas, not people** — criticize proposals, not individuals
5. **Respect expertise** — domain specialists know their area best
6. **Escalate respectfully** — if stuck, involve a mediator early

### Unacceptable Behavior

- Dismissing feedback without consideration
- Personal attacks or derogatory language
- Refusing to engage in good-faith discussion
- Blocking merges without clear technical reasoning
- Using authority to bypass the review process

### Conflict Resolution

1. **Private message** — discuss concerns 1-on-1 before public escalation
2. **Mediation** — involve domain lead or EA Board member
3. **Documentation** — record decisions and rationale in MR thread for future reference

### Anti-Patterns to Avoid

| Anti-Pattern | Instead |
|--------------|---------|
| "That'll never work" | "I'm concerned about scalability. Have you considered...?" |
| "I don't like this" | "I see a risk here: ... Could we mitigate it by...?" |
| "Do it my way" | "We could achieve the same goal with..., which would..." |
| "This is standard" | "Industry best practice here is..., which fits because..." |

---

## Questions?

- **Repository structure**: See [README.md](/README.md)
- **DAB process details**: See [governance/dab-process/](governance/dab-process/)
- **Architecture patterns**: See [knowledge-base/patterns/](knowledge-base/patterns/)
- **Standards & guidelines**: See [governance/standards/](governance/standards/)
- **Technical help**: Ask in #architecture Slack channel

Thank you for maintaining Techcombank's architecture excellence!
