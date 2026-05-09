# ADR-001: Adopt Architecture-as-Code (AaC) Approach

**Status:** Accepted

**Date:** 2026-01-15

**Authors:** Enterprise Architecture Technology Team

**Stakeholders:** Engineering leads, DevOps, Architecture, Security

---

## Context

Techcombank has grown its microservices architecture to 150+ services across 10+ business domains. Architecture documentation and diagrams are currently managed in three disconnected systems:

1. **Confluence** — narrative documentation, often outdated, version control limited
2. **Drawio (cloud)** — binary diagram files, not diffable, $50K annual license cost
3. **PowerPoint/Visio** — design presentations, no version history, shared via email

**Current Challenges:**

- **Fragmentation:** No single source of truth; teams maintain separate documentation
- **Staleness:** Diagrams fall out of sync with actual implementations; last-update timestamps often obsolete
- **Version Control:** Binary formats (Drawio, Visio) don't support Git diffing or conflict resolution
- **Cost:** Drawio licensing ($50K/year) and storage overhead
- **Collaboration:** No native Git workflows; feedback via comments or exports
- **AI Readiness:** Drawio binaries not parsable by AI tools for auto-generation or validation
- **DevOps Integration:** Architecture docs isolated from infrastructure-as-code (Terraform, Kubernetes manifests)

**Scale Challenge:** With 150+ services, maintaining consistent architecture documentation is currently manual and error-prone. Teams lack incentive to keep diagrams updated if process is cumbersome.

---

## Decision Drivers

1. **Single Source of Truth** — Architecture and code live in same repository, change together
2. **Version Control** — Full Git history, branching, pull request reviews, rollback capability
3. **Cost Reduction** — Eliminate Drawio licensing; free tools (Mermaid, PlantUML)
4. **Developer Experience** — Architects and engineers use familiar Git workflows (no learning curve)
5. **AI/ML Integration** — Text-based diagrams enable AI-assisted generation, validation, and documentation
6. **Scalability** — Framework must scale with growth of architecture (200+, 500+ services)
7. **Audit & Compliance** — Immutable history of all decisions and changes with full blame/accountability

---

## Considered Options

### Option 1: Status Quo (Confluence + Drawio)
**Pros:**
- No migration cost
- Teams familiar with tools

**Cons:**
- Continues fragmentation and staleness
- Annual $50K licensing cost persists
- No version control for diagrams
- Binary files not AI-parsable
- Poor collaboration workflows

**Verdict:** Rejected. Does not address core problems.

---

### Option 2: Migrate to Cloud-Based Wiki (Notion, Slite)
**Pros:**
- Modern interface
- Better search and organization
- Collaborative editing

**Cons:**
- Still separate from code repository
- Proprietary lock-in
- No version control
- Diagram support still weak (embedded third-party)
- Ongoing SaaS subscription cost

**Verdict:** Rejected. Solves UX but not version control or sync problem.

---

### Option 3: Architecture-as-Code (GitLab + Markdown + Text Diagrams)
**Pros:**
- Single repository with code and architecture
- Version-controlled diagrams (Git diff, blame)
- Free tools: Mermaid, PlantUML (open source)
- Native Git workflows: branches, MRs, code review
- AI-ready: Text format parsable by LLMs
- Cost: $0 for diagramming tools
- Scalable: Same infrastructure as code repos
- Audit trail: Every change has timestamp, author, rationale
- Offline-capable: Git works disconnected

**Cons:**
- Learning curve: Teams must adopt Markdown + Mermaid/PlantUML syntax
- Drag-and-drop diagram editing not available
- Initial migration effort: ~2-3 weeks to migrate existing diagrams
- Rendering dependencies: Mermaid/PlantUML require web support or CI/CD integration

**Verdict:** Selected. Best alignment with drivers.

---

## Decision

**Adopt Architecture-as-Code (AaC) approach for all architecture documentation and diagrams:**

1. **Primary Repository:** GitLab (same as code, infrastructure, configs)
2. **Format:** Markdown (`.md`) for narrative, text-based diagrams
3. **Diagram Tools:**
   - **Mermaid** (preferred) for flowcharts, sequences, state diagrams, dependencies
   - **PlantUML** for C4 architecture diagrams, class diagrams, deployment diagrams
4. **Versioning:** Semantic versioning for architecture specs; Git tags for approved designs
5. **Governance:** Design Approval Board (DAB) process enforces design reviews pre-merge
6. **CI/CD Integration:** Automatic diagram rendering, validation, and documentation generation

---

## Implementation Approach

### Phase 1: Setup (Weeks 1-2)
- Create `/governance` directory structure in architecture repo
- Establish naming conventions (files, branches, tags)
- Set up Mermaid + PlantUML CI/CD pipeline (renders diagrams on MR)
- Create sample templates and examples

### Phase 2: Migration (Weeks 3-4)
- Migrate existing Drawio diagrams to Mermaid/PlantUML (batch + manual review)
- Migrate Confluence strategic documents to Markdown
- Archive old Confluence (read-only, link to new docs)
- Train teams on Markdown + diagram syntax

### Phase 3: Governance (Week 4+)
- Design Approval Board process goes live
- All new architecture requires MR + peer review
- Monthly reviews of documentation quality
- Quarterly training refreshers

---

## Consequences

### Positive

1. **Version Control:** Full Git history eliminates "who changed what and why?" mystery
2. **Cost Savings:** ~$50K/year from eliminated Drawio licensing
3. **Developer Velocity:** Architects and engineers in same workflow (Git), no tool switching
4. **AI Integration:** Text-based diagrams enable future AI-assisted documentation and validation
5. **Audit Trail:** Every design change has timestamp, author, commit message, review comments
6. **Scalability:** Same approach works for 150 services today, 500+ services tomorrow
7. **Consistency:** Naming conventions and templates enforce consistency
8. **Discoverability:** Git search, CODEOWNERS, etc. make it easy to find relevant docs

### Negative

1. **Learning Curve:** Teams must learn Markdown, Mermaid, PlantUML syntax
   - Mitigation: Training, templates, examples, pair programming initially
2. **Loss of Drag-and-Drop:** Cannot draw freely as in Drawio
   - Mitigation: Acceptable trade-off; structure > visual perfection
3. **Initial Migration Effort:** Converting 50+ existing diagrams
   - Mitigation: Phased approach; tooling to assist conversion
4. **Rendering Complexity:** Mermaid/PlantUML rendering may fail or look wrong sometimes
   - Mitigation: CI/CD validation catches issues early; fallback to text description
5. **Accessibility:** Team members preferring GUI may resist
   - Mitigation: Show cost savings, time savings, and version control benefits

### Risks & Mitigations

| Risk | Probability | Impact | Mitigation |
|------|---|---|---|
| **Adoption resistance** | High | Medium | Strong leadership support, early wins, demos |
| **Syntax errors in diagrams** | Medium | Low | CI/CD linting + validation before merge |
| **Tool limitations** | Medium | Low | Mermaid + PlantUML cover 90% of needs; hybrid approach if needed |
| **Performance (large diagrams)** | Low | Low | Splitting large diagrams into smaller, modular pieces |
| **Render inconsistency** | Medium | Low | Testing in CI/CD; fallback to SVG export if issues |

---

## Validation & Success Criteria

**Rollout Success (Target: 6 weeks):**
- [ ] 50+ existing diagrams migrated to Mermaid/PlantUML
- [ ] All architecture teams trained on Markdown + diagram syntax
- [ ] DAB process launched and 10+ submissions reviewed
- [ ] Drawio license cancelled (savings realized)
- [ ] Documentation staleness reduced (last-update timestamps recent)

**Long-term Success (6 months):**
- [ ] 100% of new architecture documented in AaC format
- [ ] Diagram rendering latency < 5 seconds (CI/CD)
- [ ] Zero critical issues from missing/outdated architecture docs
- [ ] Team velocity improved (architects spend less time on manual docs)
- [ ] AI-assisted architecture generation piloted successfully

**Metrics:**
- Architecture doc staleness: Currently 40% > 6 months old → Target: 10%
- Time to publish architecture: Currently 2 weeks → Target: 2 days
- Cost: Currently $50K/year (Drawio) → Target: $0
- DAB review quality: Target 95% catching architectural issues pre-implementation

---

## Related Decisions

- **ADR-002:** Prefer MermaidJS/PlantUML over Drawio (detailed tool rationale)
- **[DAB Governance Process](../dab-process/README.md):** Design Approval Board controls architecture changes
- **[Naming Conventions](../standards/naming-conventions.md):** File naming, branch naming, tags

---

## Appendix: Example Architecture-as-Code File Structure

```
architecture-repo/
├── governance/
│   ├── README.md (overview)
│   ├── dab-process/
│   │   ├── dab-full-process.md
│   │   ├── approval-matrix.md
│   │   └── ...
│   ├── standards/
│   │   ├── naming-conventions.md
│   │   ├── diagram-standards.md
│   │   └── ...
│   └── decisions/
│       ├── ADR-001-adopt-architecture-as-code.md (this file)
│       ├── ADR-002-mermaid-over-drawio.md
│       └── ...
└── domains/
    ├── payments/
    │   ├── README.md (domain overview)
    │   ├── diagrams/
    │   │   ├── system-context.mmd
    │   │   ├── payment-flow.mmd
    │   │   └── component-architecture.puml
    │   └── dab-submissions/
    │       ├── payment-acceleration-engine/
    │       │   ├── 01-business-context.md
    │       │   ├── 02-high-level-architecture.md
    │       │   └── ...
    ├── cards/
    │   ├── ...
    └── deposits/
        ├── ...
```

---

## References

- Mermaid Project: https://mermaid.js.org/
- PlantUML: https://plantuml.com/
- Architecture-as-Code: https://www.thoughtworks.com/insights/blog/architecture-code
- C4 Model: https://c4model.com/

---

## Sign-Off

**Enterprise Architecture Director:** _______________ **Date:** ___________

**CTO Approval:** _______________ **Date:** ___________
