# Testing Knowledge Base Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `knowledge-base/testing/` — a catalog-governed testing corpus for the IT Quality Engineering team that covers all 191 existing catalog rows across six test disciplines, with 24 reusable test archetypes and a machine-checkable coverage matrix.

**Architecture:** Documentation-only, following the repository's architecture-as-code conventions. A 9-document strategy layer (of which `TST-001` becomes the 7th normative spine doc) fixes the vocabulary, the eight performance profiles, and the `test_acceptance_criteria` contract. 24 archetype documents group the 191 catalog rows by *shared verification method* rather than by domain, so test strategy is written once per method instead of once per pattern. A YAML coverage file plus two Python validators prove coverage mechanically rather than asserting it in prose.

**Tech Stack:** Markdown (CommonMark + Material for MkDocs extensions), Mermaid diagrams, YAML (PyYAML ≥6.0 <7.0), Python 3.11 stdlib for the two new scripts. Documented test tooling: Apache JMeter (primary), Gatling + Karate, k6, Locust — as fenced snippets only, never executed.

**Source spec:** [`docs/superpowers/specs/2026-08-12-testing-knowledge-base-design.md`](../specs/2026-08-12-testing-knowledge-base-design.md)

---

## Global Constraints

Every task's requirements implicitly include this section.

- **Documentation-only.** No Maven/Gradle project, no `package.json`, no dependency
  manifests for test tools, no CI test execution, no new `.gitlab-ci.yml` stages or jobs.
  Test-tool code appears exclusively inside fenced Markdown blocks.
- **No modification of existing pattern documents.** Cross-links flow one way: testing docs
  link out to patterns; pattern files are never edited. The only existing files this plan
  touches are the four registration files listed in the File Structure section.
- **Synthetic data only, no PII/PHI.** No real names, dates of birth, national ID numbers,
  member IDs, account holder details, or production extracts appear in any document, snippet,
  or fixture. Card PANs use designated test BIN ranges. Account numbers, CIF identifiers, and
  customer references are synthetic and marked as such. This is a hard organisational
  data-handling requirement.
- **Thresholds are derived, never restated.** No document outside `TST-002` states a latency,
  throughput, RTO, RPO, or availability number. Each links to the owning spine row:
  `NFR-001` (tier / RTO / RPO / availability), `NFR-002` (P50/P95/P99), `NFR-003` (capacity
  headroom), `NFR-004` (sustained and peak throughput), `NFR-005` (error budget). A reviewer
  rejects any hard-coded number.
- **Document header, five lines exactly**, on every file except category `README.md`:

  ```text
  Status: Approved | Last Reviewed: 2026-08-12 | Owner: @qe-lead
  Catalog ID: TST-0NN | Radii
  Tier Applicability: T0, T1, T2
  ```

  `TST-001` alone uses `Catalog ID: TST-001 | **Spine**` and
  `Tier Applicability: N/A (defines test obligations)`.
- **Every document contains at least one Mermaid diagram**, per
  `governance/standards/diagram-standards.md`.
- **Every document completes the Ring 0/1/2 Compliance Mapping table** with the exact column
  header `| Layer | Reference | Section/Control | How this satisfies |`. Ring 2 entries carry
  the existing marker `⚠️ (working summary — pending Legal review)`.
- **Catalog IDs are never reused or renumbered.** `TST-001`…`TST-015` for strategy, tooling,
  and coverage; `TST-020`…`TST-043` for the 24 archetypes; `TST-016`…`TST-019` are left
  deliberately unallocated as headroom for future strategy documents.
- **Owner handles:** `@qe-lead` owns the category. `@sre-lead` co-owns `TST-002`, `TST-003`,
  and all of `tooling/`. `@infosec-architect` co-owns `TST-008`, `TST-040`, `TST-041`.
- **Commit message format:** conventional commits, scope `testing`, e.g.
  `docs(testing): add TST-002 performance test standard`. Every commit ends with the
  `Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>` trailer.
- **Line length:** wrap prose at 100 characters. Tables and fenced blocks may exceed it.
- **British/International English** spelling to match the existing corpus ("modelling",
  "prioritise", "tokenisation", "behaviour").

---

## Current State

- 191 Approved catalog rows across 16 categories, 6 spine docs, 185 radii docs.
- 130 pattern documents, 20 reference architectures, 5 NFR spine docs, 13 principles,
  11 best-practice docs, 8 compliance deep-dives, 4 templates.
- No testing guidance exists anywhere in the knowledge base.
- Testing-adjacent rows to cross-link, never duplicate: `INT-015` API Contract Testing,
  `EIP-020` Test Message, `OBS-009` Synthetic Monitoring and Canary Probes,
  `BP-005` Chaos Engineering.
- `scripts/` holds 16 Python and shell utilities. There is **no `tests/` directory and no
  pytest harness** — the repository's established test cycle for a new script is: run it
  against known-bad input and confirm a non-zero exit, then against real input and confirm a
  zero exit. Tasks 9 and 10 follow that idiom.
- **Known pre-existing defect, out of scope:** `.gitlab-ci.yml` job `validate:markdown-lint`
  invokes `markdownlint --config .markdownlint.json` but `.markdownlint.json` does not exist
  in the repository, so that blocking gate is already failing on `main`. Do not create the
  file as part of this plan — it changes lint behaviour repository-wide and belongs to the
  platform owner. Local verification steps in this plan therefore run `markdownlint` with
  default rules.

---

## Authoring Convention For This Plan

This is a documentation repository. For the two Python scripts (Tasks 9 and 10) the plan gives
complete, literal source. For the 41 Markdown documents the plan gives, per task, the complete
**content specification**: the exact filename, header block, section list, and the *distinct
technical substance* of every section — the real catalog rows covered, the actual invariants to
assert, the specific fault scenarios, the named performance profiles, the JMeter elements to
use, the tool-fit ratings with their reasons, and the real compliance references.

That content specification is the deliverable definition. An implementer writes the prose that
carries it; they never invent the substance. A task is not complete if any listed invariant,
fault, profile, or compliance row is missing from the document.

`TPL-005` (Task 1) supplies the section skeleton every archetype fills, and is given in full
literal form. Tasks 17 onward therefore specify content, not structure — the structure is
already fixed by the template, and repeating it 24 times would let it drift.

### Both scripts in this plan were verified before it was written

`scripts/validate-testing-coverage.py` (Task 10) and `scripts/render-testing-coverage.py` (Task 11)
were extracted from this plan and executed against the real 191-row
`_catalog-inventory.yml` in a throwaway root. Confirmed:

- Both compile under Python 3 with `PyYAML 6.0.3`.
- The validator reports exactly `188 × check1 + 3 × check3` against the three seed rows in Task 10
  Step 1 — which is what Task 10 Step 3 tells you to expect.
- All seven checks fire under mutation, including both `check2` variants (unknown ID and duplicate
  row), all four `check4` variants (bad tool, bad profile, bad discipline value, missing discipline
  key), and both `check6` variants (empty archetypes outside `governs`, and archetypes named on an
  all-`governs` row).
- The renderer detects staleness, renders, is idempotent on a second run, preserves narrative both
  above and below the markers, sorts by catalog ID, and exits `2` with a clear message when the
  markers are absent.

So a failure in Task 10 or 11 means the file was transcribed incorrectly, not that the logic is
wrong. Note that `PyYAML` is already in `scripts/requirements.txt`, but the system `python3` on a
developer machine may not have it — use the repository's documented `.venv` flow.

---

## File Structure

**Created — 44 files**

| Path | Responsibility |
|---|---|
| `knowledge-base/testing/README.md` | Category index table + how the corpus is used. No Catalog ID. |
| `knowledge-base/testing/strategy/test-strategy-standard.md` | `TST-001` **Spine**. Discipline definitions, tier→obligation matrix, the `test_acceptance_criteria` contract, cross-block invariants. |
| `knowledge-base/testing/strategy/performance-test-standard.md` | `TST-002`. The 8 normative performance profiles and their pass criteria. |
| `knowledge-base/testing/strategy/workload-modelling.md` | `TST-003`. Volumetrics→concurrency, open vs closed models, peak factors, named journey blends. |
| `knowledge-base/testing/strategy/test-data-management.md` | `TST-004`. Synthetic-only data strategy, PII/PHI prohibition, referential integrity, seeding, teardown. |
| `knowledge-base/testing/strategy/environments-quality-gates.md` | `TST-005`. Environment tiers, perf-env sizing and extrapolation, gate placement, flakiness policy, evidence retention. |
| `knowledge-base/testing/strategy/resilience-test-standard.md` | `TST-006`. Fault-injection taxonomy and the `failure_modes`→test obligation. |
| `knowledge-base/testing/strategy/contract-integration-test-standard.md` | `TST-007`. Consumer-driven contracts, schema compatibility modes, integration scope. |
| `knowledge-base/testing/strategy/security-test-standard.md` | `TST-008`. AuthZ matrix method, token lifecycle cases, DAST scope and boundaries. |
| `knowledge-base/testing/strategy/data-quality-test-standard.md` | `TST-009`. DQ dimensions, reconciliation tolerance, lag assertions. |
| `knowledge-base/testing/tooling/tool-selection-matrix.md` | `TST-010`. Capability matrix + Mermaid decision tree over the four tools. |
| `knowledge-base/testing/tooling/jmeter.md` | `TST-011`. Primary tool. Deepest guide. |
| `knowledge-base/testing/tooling/gatling-karate.md` | `TST-012`. Karate feature reuse via `karate-gatling`. |
| `knowledge-base/testing/tooling/k6.md` | `TST-013`. Thresholds-as-code, CI gate role. |
| `knowledge-base/testing/tooling/locust.md` | `TST-014`. Bespoke stateful Python scenarios. |
| `knowledge-base/testing/coverage/coverage-matrix.md` | `TST-015`. Generated coverage table between generation markers. |
| `knowledge-base/testing/coverage/_testing-coverage.yml` | Machine-readable coverage source of truth, one row per catalog row. |
| `knowledge-base/testing/archetypes/` ×24 | `TST-020`…`TST-043`. One archetype per shared verification method. |
| `knowledge-base/templates/test-archetype-template.md` | `TPL-005`. Section skeleton for all 24 archetypes. |
| `scripts/validate-testing-coverage.py` | Coverage gate. 7 checks, non-zero exit on any failure. |
| `scripts/render-testing-coverage.py` | Regenerates the `coverage-matrix.md` table body from YAML. |

**Modified — 4 files**

| Path | Change |
|---|---|
| `governance/standards/_catalog-inventory.yml` | Append 40 rows (39 `TST-*`, 1 `TPL-005`), in ID order. |
| `governance/standards/enterprise-architecture-catalog.md` | Append 40 table rows; update the §1 coverage sentence; add `testing` to the §5 category table and update `templates` and `**Total**`; add a `testing` subsection to §3; add `TST-001` to the §2.2 spine list. |
| `mkdocs.yml` | Add a `Testing (QE)` section under `Architecture Knowledge Base`. |
| `.gitlab/CODEOWNERS` | Ownership rules for `knowledge-base/testing/`. |

### Relative link depths

Getting these wrong is the single most common cause of `validate:links` failures.

| From | To a pattern | To an NFR doc | To governance |
|---|---|---|---|
| `testing/README.md` | `../patterns/<domain>/<file>.md` | `../nfr/<file>.md` | `../../governance/standards/<file>.md` |
| `testing/strategy/*.md` | `../../patterns/<domain>/<file>.md` | `../../nfr/<file>.md` | `../../../governance/standards/<file>.md` |
| `testing/archetypes/*.md` | `../../patterns/<domain>/<file>.md` | `../../nfr/<file>.md` | `../../../governance/standards/<file>.md` |
| `testing/tooling/*.md` | `../../patterns/<domain>/<file>.md` | `../../nfr/<file>.md` | `../../../governance/standards/<file>.md` |
| `testing/coverage/*.md` | `../../patterns/<domain>/<file>.md` | `../../nfr/<file>.md` | `../../../governance/standards/<file>.md` |
| `templates/test-archetype-template.md` | `../patterns/<domain>/<file>.md` | `../nfr/<file>.md` | `../../governance/standards/<file>.md` |

---

## Wave A — Foundation

Wave A is the gate. Nothing in Waves C–F may be authored before `TPL-005`, `TST-001`, and
`TST-002` are merged, or the 24 archetypes will diverge structurally and their performance
profiles will drift.

## Task 0: Pre-Flight Baseline

**Files:**
- Create: none
- Modify: none

**Interfaces:**
- Consumes: nothing
- Produces: the recorded baseline counts (191 rows / 16 categories / 6 spine) that Task 11
  and Task 47 assert against.

- [ ] **Step 1: Confirm the working directory is the isolated worktree**

```bash
pwd
git branch --show-current
git status --short
```

Expected: path under `.claude/worktrees/`, a non-`main` branch, and a clean tree. Stop if the
branch is `main`.

- [ ] **Step 2: Record the catalog baseline**

```bash
grep -c '^| [A-Z]\+-[0-9]\+ |' governance/standards/enterprise-architecture-catalog.md
grep -n 'Coverage: 191' governance/standards/enterprise-architecture-catalog.md
grep -c '^- id: ' governance/standards/_catalog-inventory.yml
```

Expected: `191`, one match on the coverage sentence, `191`. If any number differs, the catalog
has moved since the spec was written — stop and reconcile before continuing.

- [ ] **Step 3: Confirm the four validation gates run and record their current state**

```bash
python3 scripts/audit-catalog-consistency.py; echo "audit exit=$?"
python3 scripts/validate-internal-links.py; echo "links exit=$?"
python3 scripts/check-compliance-rows.py; echo "compliance exit=$?"
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/resilience/circuit-breaker.md; echo "mermaid exit=$?"
```

Expected: all four exit `0`. This is the baseline every later task must preserve. If a gate is
already red on the branch point, record the exact failure — later tasks must not be blamed for
it, and must not fix it silently.

- [ ] **Step 4: Confirm `markdownlint` is available**

```bash
markdownlint --version || npm install --global markdownlint-cli
```

Note: run `markdownlint` with default rules. Do **not** create `.markdownlint.json` — see
Current State.

- [ ] **Step 5: Commit the plan**

```bash
git add docs/superpowers/plans/2026-08-12-testing-knowledge-base.md
git commit -m "docs(plans): add testing knowledge base implementation plan

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 1: TPL-005 Test Archetype Template

The template fixes the section order for all 24 archetypes and must land first.

**Files:**
- Create: `knowledge-base/templates/test-archetype-template.md`
- Modify: none (registration happens in Task 11)

**Interfaces:**
- Consumes: nothing
- Produces: the canonical 14-section archetype skeleton. Every task from 17 onward fills
  exactly these sections in exactly this order. Section names are load-bearing — Task 11's
  structural check greps for them.

- [ ] **Step 1: Create the template**

Write `knowledge-base/templates/test-archetype-template.md` with this literal content:

````markdown
# Test Archetype Document Template

Status: Approved | Last Reviewed: 2026-08-12 | Owner: @qe-lead
Catalog ID: TPL-005 | Radii
Tier Applicability: N/A (meta-document — template for archetype authors)

---

## How to Use This Template

1. Copy this file to `knowledge-base/testing/archetypes/<archetype-name>.md`.
2. Take the next unallocated `TST-0NN` ID from the range `TST-020`–`TST-043` as recorded in
   `governance/standards/enterprise-architecture-catalog.md`. Never reuse an ID.
3. Replace every `[PLACEHOLDER]` with real content. Delete every authoring note (lines
   beginning with `>`) once its section is complete.
4. Keep the 14 section headings below, in this order, with this exact spelling. Omit an
   overlay subsection in §8 entirely if the archetype does not apply to that discipline —
   never leave it filled with "N/A".
5. State no latency, throughput, RTO, RPO, or availability number. Link to the owning
   spine row instead: [NFR-001](../../nfr/service-tiering-rto-rpo.md),
   [NFR-002](../../nfr/latency-budget-model.md),
   [NFR-003](../../nfr/capacity-planning-model.md),
   [NFR-004](../../nfr/throughput-model.md),
   [NFR-005](../../nfr/error-budget-policy.md).
6. Use synthetic data in every example. No PII or PHI. See
   [TST-004](../../testing/strategy/test-data-management.md).
7. Set Status to `Draft` while authoring. The EA Board moves it to `Approved` after review.
8. This template file itself must never be modified to describe a real archetype.

---

# [PLACEHOLDER: Archetype Name — e.g., "Idempotency and Replay Safety"]

Status: Draft | Last Reviewed: [YYYY-MM-DD] | Owner: @qe-lead
Catalog ID: [TST-0NN] | Radii
Tier Applicability: [T0 | T1 | T2 | T3 — the tiers for which this archetype is mandatory]

## 1. Applies To

> **Authoring note**: One row per catalog row this archetype covers. The path column is a
> working relative link — `../../patterns/<domain>/<file>.md`. Two rows belong in the same
> archetype only when the *method of verification* is the same, not merely the domain.

| Catalog ID | Title | Document |
|---|---|---|
| [PLACEHOLDER: e.g. BSP-002] | [Idempotent Payment Key] | [`../../patterns/banking-solutions/idempotent-payment-key.md`] |

## 2. Failure Taxonomy

> **Authoring note**: 5–8 concrete defect classes this archetype exists to catch. Each is a
> specific, observable failure — not a quality attribute. "Duplicate posting when the client
> retries after a gateway timeout" is a defect class; "correctness issues" is not.

- [PLACEHOLDER: Defect class 1]

## 3. Functional Test Design

> **Authoring note**: Name the *oracle* first — where expected results come from. One of:
> `golden-dataset`, `invariant-assertion`, `confusion-matrix`, `contract-schema`. Then list
> the invariants as assertable statements, the equivalence classes, and the boundary and
> negative paths. Every invariant must be mechanically checkable.

**Oracle:** [PLACEHOLDER]

### Invariants

| # | Invariant | Assertion |
|---|---|---|
| I1 | [PLACEHOLDER] | [PLACEHOLDER] |

### Equivalence classes and boundaries

- [PLACEHOLDER]

### Negative paths

- [PLACEHOLDER]

## 4. Performance Test Design

> **Authoring note**: Name only the profiles from [TST-002](../../testing/strategy/performance-test-standard.md)
> that apply, and say why each applies to *this* archetype. State the workload model —
> `open` or `closed` per [TST-003](../../testing/strategy/workload-modelling.md) — and the
> spine row supplying each threshold. Do not restate the threshold value.

| Profile | Applies | Why | Threshold source |
|---|---|---|---|
| [PLACEHOLDER] | yes/no | [PLACEHOLDER] | [NFR-00N tier row] |

**Workload model:** [open | closed] — [reason]

## 5. Canonical Harness — JMeter

> **Authoring note**: A fenced JMX fragment naming the real elements used, plus the CLI
> invocation, plus assertion and listener configuration. Parameterise via `${__P(name,default)}`
> so the same plan runs at every profile. Never embed real data.

```xml
<!-- [PLACEHOLDER: JMX fragment] -->
```

```bash
jmeter -n -t [plan].jmx \
  -Jusers=... -Jrampup=... -Jduration=... \
  -l results.jtl -e -o report/
```

## 6. Tool Fit

> **Authoring note**: Rate all four. `BEST` for exactly one. Give a one-line reason each.

| Tool | Fit | When to prefer |
|---|---|---|
| JMeter | [BEST/good/fair] | [PLACEHOLDER] |
| Gatling + Karate | [BEST/good/fair] | [PLACEHOLDER] |
| k6 | [BEST/good/fair] | [PLACEHOLDER] |
| Locust | [BEST/good/fair] | [PLACEHOLDER] |

## 7. Overlays

> **Authoring note**: Include only the subsections that apply. Delete the others entirely.

### Resilience overlay

### Contract overlay

### Security overlay

### Data-quality overlay

## 8. Test Data Requirements

> **Authoring note**: Synthetic only. State the entities needed, the cardinality driver, the
> referential-integrity requirement, and the teardown. Cross-link
> [TST-004](../../testing/strategy/test-data-management.md).

## 9. Evidence and Observability

> **Authoring note**: Metrics to capture, trace assertions, and the artifacts to attach to a
> DAB submission.

## 10. Exit Criteria

> **Authoring note**: The archetype's `test_acceptance_criteria` fragment, as defined in
> [TST-001](../../testing/strategy/test-strategy-standard.md). Only the fields this archetype
> constrains.

```yaml
test_acceptance_criteria:
  archetypes: [TST-0NN]
  # [PLACEHOLDER]
```

## 11. Compliance Mapping

| Layer | Reference | Section/Control | How this satisfies |
|---|---|---|---|
| Ring 0 | [PLACEHOLDER] | [PLACEHOLDER] | [PLACEHOLDER] |
| Ring 1 | [PLACEHOLDER] | [PLACEHOLDER] | [PLACEHOLDER] |
| Ring 2 | [PLACEHOLDER] ⚠️ (working summary — pending Legal review) | [PLACEHOLDER] | [PLACEHOLDER] |

## 12. Related Patterns

- [PLACEHOLDER: link back into the pattern catalog]

## 13. Related Archetypes

- [PLACEHOLDER: sibling archetypes that commonly run alongside this one]

## 14. Diagram

> **Authoring note**: Mandatory. Use `sequenceDiagram` for a replay or fault sequence,
> `graph LR` for a test topology, `xychart-beta` or `graph LR` for a load-profile shape.

```mermaid
%% [PLACEHOLDER: Replace with the real diagram]
```
````

- [ ] **Step 2: Verify the template lints and its Mermaid block parses**

```bash
markdownlint knowledge-base/templates/test-archetype-template.md
bash scripts/mermaid-lint-doc.sh knowledge-base/templates/test-archetype-template.md
echo "exit=$?"
```

Expected: both exit `0`. The Mermaid linter must tolerate a comment-only block, exactly as it
does for `knowledge-base/templates/pattern-doc-template.md` — verify that file passes too if
this one fails, to tell a template problem apart from a linter limitation.

- [ ] **Step 3: Verify the 14 section headings are present and correctly ordered**

```bash
grep -n '^## ' knowledge-base/templates/test-archetype-template.md
```

Expected, in order: `How to Use This Template`, `1. Applies To`, `2. Failure Taxonomy`,
`3. Functional Test Design`, `4. Performance Test Design`, `5. Canonical Harness — JMeter`,
`6. Tool Fit`, `7. Overlays`, `8. Test Data Requirements`, `9. Evidence and Observability`,
`10. Exit Criteria`, `11. Compliance Mapping`, `12. Related Patterns`,
`13. Related Archetypes`, `14. Diagram`.

- [ ] **Step 4: Commit**

```bash
git add knowledge-base/templates/test-archetype-template.md
git commit -m "docs(testing): add TPL-005 test archetype template

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: Category README and TST-001 Test Strategy Standard (Spine)

`TST-001` is the 7th spine doc. It defines the vocabulary every other document inherits and
may not contradict.

**Files:**
- Create: `knowledge-base/testing/README.md`
- Create: `knowledge-base/testing/strategy/test-strategy-standard.md`

**Interfaces:**
- Consumes: `TPL-005` (Task 1) for the archetype section vocabulary.
- Produces: the six discipline keys (`functional`, `performance`, `resilience`, `contract`,
  `security`, `data_quality`); the four oracle names (`golden-dataset`, `invariant-assertion`,
  `confusion-matrix`, `contract-schema`); the four obligation levels (`required`,
  `recommended`, `n/a`, `governs`); and the `test_acceptance_criteria` block schema. Tasks 3–46
  all consume these exact strings — `scripts/validate-testing-coverage.py` (Task 10) validates
  against them, so a spelling change here breaks the gate.

- [ ] **Step 1: Create the category README**

Write `knowledge-base/testing/README.md`. No Catalog ID — category READMEs carry none, matching
`knowledge-base/principles/README.md`. Required content:

- H1 `# Testing Knowledge Base (IT Quality Engineering)`.
- Two paragraphs: what the corpus is (test strategy for the 191 catalog rows, organised by
  verification method) and what it is not (not a test harness; no runnable projects; snippets
  are copied into the QE team's own repository).
- `## How to Use This Corpus` — a 4-step flow: find your pattern's catalog ID in
  [`coverage/coverage-matrix.md`](./coverage/coverage-matrix.md) → open the archetypes it maps
  to → read [`TST-002`](./strategy/performance-test-standard.md) for the profiles your tier
  requires → fill the `test_acceptance_criteria` block from
  [`TST-001`](./strategy/test-strategy-standard.md).
- `## Index — Strategy` table with columns Catalog ID, Document, Purpose — 9 rows,
  `TST-001`…`TST-009`.
- `## Index — Tooling` table — 5 rows, `TST-010`…`TST-014`.
- `## Index — Archetypes` table grouped by the 7 families — 24 rows, `TST-020`…`TST-043`, each
  linking to `./archetypes/<file>.md`.
- `## Coverage` — one paragraph plus a link to `coverage/coverage-matrix.md`, stating that
  coverage is enforced by `scripts/validate-testing-coverage.py` and that a new catalog row
  without a coverage row fails the gate.
- `## Related` — links to `../nfr/service-tiering-rto-rpo.md`,
  `../nfr/latency-budget-model.md`, `../templates/nfr-acceptance-criteria-dab.md`,
  `../templates/test-archetype-template.md`, `../patterns/integration/api-contract-testing.md`,
  `../patterns/eip/test-message.md`,
  `../patterns/observability/synthetic-monitoring-canary.md`,
  `../best-practices/chaos-engineering.md`.
- One Mermaid `graph LR` showing: Catalog Row → Coverage Matrix → Archetype(s) → Profiles
  (TST-002) → `test_acceptance_criteria` → DAB evidence.

The archetype index rows point at files that do not exist until Wave F completes.
`validate-internal-links.py` will report those as warnings meanwhile; that job is
`allow_failure: true` so it does not block. Task 47 asserts the link gate is clean, so write
every archetype filename correctly now rather than backfilling.

- [ ] **Step 2: Create TST-001 with these sections and this substance**

Write `knowledge-base/testing/strategy/test-strategy-standard.md`.

Header:

```text
# Test Strategy Standard

Status: Approved | Last Reviewed: 2026-08-12 | Owner: @qe-lead
Catalog ID: TST-001 | **Spine**
Tier Applicability: N/A (defines test obligations)
```

**`## Problem Statement`** — 6 bullets from the spec's §1: no comparable test approach across
squads; declared NFR numbers with no evidence standard; soak/spike/mixed undefined; declared
`failure_modes` with no test obligation; tool fragmentation with silently mixed open and closed
workload models; no way to answer "which patterns are untested?".

**`## Context`** — 4 trigger scenarios: authoring a DAB's test section; onboarding a squad to
QE standards; planning a release regression cycle; investigating an escaped defect to find
which archetype should have caught it.

**`## The Six Disciplines`** — this table. The `Key` column values are normative and
machine-validated:

| Discipline | Key | Verifies | Owning standard |
|---|---|---|---|
| Functional | `functional` | Behaviour matches specification | TST-001 |
| Performance | `performance` | Behaviour holds at declared load | TST-002 |
| Resilience | `resilience` | Behaviour degrades safely under fault | TST-006 |
| Contract | `contract` | Producer and consumer stay compatible | TST-007 |
| Security | `security` | Controls cannot be bypassed | TST-008 |
| Data quality | `data_quality` | Data is accurate, complete, and timely | TST-009 |

**`## Obligation Levels`** — `required` (a tier gate; release blocks without it),
`recommended` (expected; a documented waiver is permitted), `n/a` (the discipline does not
apply to this pattern's shape), `governs` (the row is a meta-document that constrains testing
rather than being tested — the 5 `NFR-*`, 13 `PRIN-*`, 5 `TPL-*`, 8 `COMP-*` rows, plus
`BP-009`, `BP-010`, `BP-011`, `PLT-002`, `PLT-004`, `PLT-007`). Include the sentence: `n/a` and
"not yet covered" are different states, and the coverage gate distinguishes them.

**`## Tier Obligation Matrix`** — which disciplines are `required` per tier. T0: all six
required. T1: functional, performance, resilience, and contract required; security and data
quality required where the pattern handles credentials or regulated data. T2: functional and
contract required; performance recommended. T3: functional required; the rest recommended.
Reference [NFR-001](../../nfr/service-tiering-rto-rpo.md) for tier definitions; do not restate
RTO, RPO, or availability values.

**`## The Four Oracles`** — `golden-dataset` (an exact expected value from a signed-off
dataset; use for calculation engines), `invariant-assertion` (a property that must hold over
any input; use for ledgers, ordering, idempotency), `confusion-matrix` (precision, recall, and
false-positive rate against a labelled corpus; use for screening and decisioning),
`contract-schema` (conformance to a published schema or contract; use for messaging and APIs).
State that each archetype declares exactly one primary oracle.

**``## The `test_acceptance_criteria` Contract``** — the full YAML block reproduced verbatim
from the spec's §8, including the `evidence` sub-block, followed by a field-by-field table:
field, type, required, source.

**`## Cross-Block Invariants`** — three checkable rules:

1. `test_acceptance_criteria.tier` equals `nfr_acceptance_criteria.tier` for the same service.
2. Every ID in `resilience.fault_scenarios` appears in that service's
   `nfr_acceptance_criteria.failure_modes[].id`. A declared failure mode with no test is a gap.
3. `performance.sustained_rps` and `performance.peak_rps` equal the declared
   `nfr_acceptance_criteria.throughput_target` values. A service may not load-test below what
   it promised.

State explicitly: these are documented obligations, **not** CI-enforced. Enforcing them over
`dab/` content is a DAB-process change and is out of scope — see the spec's §2.

**`## Relationship to TPL-001`** — one paragraph plus a Mermaid `graph LR`:
`nfr_acceptance_criteria` (TPL-001) supplies tier, RTO/RPO, latency, throughput, and failure
modes → `test_acceptance_criteria` (TST-001) references them and adds executed profiles,
covered invariants, fault scenarios, and evidence. Link
[TPL-001](../../templates/nfr-acceptance-criteria-dab.md).

**`## Shift-Left Placement`** — unit and contract tests in the merge pipeline; the `baseline`
profile as a pipeline gate; `load`, `stress`, `spike`, and `scalability` on a scheduled perf
pipeline; `soak` and `mixed` pre-release; `failover-under-load` in the release readiness drill.
Cross-link [TST-005](./environments-quality-gates.md) and
[BP-001](../../best-practices/ci-cd-pipeline-design.md).

**`## Compliance Mapping`** — Ring 0: ISTQB Foundation test levels and test types (canonical
taxonomy); NIST SP 800-53 CA-2 (control assessment). Ring 1: BCBS 230 Principle 9 (operational
resilience testing, including severe-but-plausible scenarios); BCBS 239 Principle 3 (accuracy).
Ring 2: SBV Circular 09/2020 §IV.3 (system testing and BCP drill obligations) with the
`⚠️ (working summary — pending Legal review)` marker.

**`## Related`** — TST-002…TST-010, TPL-005, NFR-001…NFR-005.

- [ ] **Step 3: Verify both documents lint and their Mermaid blocks parse**

```bash
markdownlint knowledge-base/testing/README.md knowledge-base/testing/strategy/test-strategy-standard.md
bash scripts/mermaid-lint-doc.sh knowledge-base/testing/README.md
bash scripts/mermaid-lint-doc.sh knowledge-base/testing/strategy/test-strategy-standard.md
echo "exit=$?"
```

Expected: all exit `0`.

- [ ] **Step 4: Verify no service threshold leaked in**

```bash
grep -nE '[0-9]+ *(ms|rps|RPS)\b' knowledge-base/testing/strategy/test-strategy-standard.md
```

Expected: matches only inside the reproduced `test_acceptance_criteria` example block, which is
labelled as illustrative. Any other numeric service budget is a defect — replace it with a link
to the owning NFR row.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/testing/README.md knowledge-base/testing/strategy/test-strategy-standard.md
git commit -m "docs(testing): add testing README and TST-001 test strategy standard

TST-001 becomes the 7th spine doc: six disciplines, four oracles, four
obligation levels, the test_acceptance_criteria contract, and the three
cross-block invariants tying it to TPL-001.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: TST-002 Performance Test Standard

The eight profiles. Every archetype's §4 references this document and nothing else for load
shapes and pass criteria.

**Files:**
- Create: `knowledge-base/testing/strategy/performance-test-standard.md`

**Interfaces:**
- Consumes: `TST-001` discipline and obligation vocabulary.
- Produces: the eight profile keys — `baseline`, `load`, `stress`, `spike`, `soak`, `mixed`,
  `scalability`, `failover-under-load`. These exact strings are the `perf_profiles[]` domain
  validated by `scripts/validate-testing-coverage.py` (Task 10), and are referenced by every
  archetype §4.

- [ ] **Step 1: Create TST-002**

Header: `Catalog ID: TST-002 | Radii`, `Owner: @qe-lead`,
`Tier Applicability: T0, T1, T2, T3`.

**`## Problem Statement`** — 5 bullets: profile names used inconsistently across squads; soak
duration and spike shape undefined so results are not comparable; aggregate pass criteria hide
per-journey failures; thresholds copied by hand from NFR docs then drift; no defined evidence
artifact so DAB reviewers cannot audit a claim.

**`## The Eight Profiles`** — this exact table:

| Profile | Purpose | Load shape | Pass criteria | Required for |
|---|---|---|---|---|
| `baseline` | per-build sanity | 10% of sustained, 10 min | zero errors; P95 within tier budget | T0–T3 |
| `load` | steady-state proof | 100% of sustained, 60 min | P50/P95/P99 all within the NFR-002 tier row; error rate ≤ 0.1% | T0–T2 |
| `stress` | locate the knee | step +10% every 5 min until failure | knee ≥ declared `peak_rps`; degradation graceful, not cliff-edge | T0, T1 |
| `spike` | burst absorption | sustained → peak in 30 s, hold 5 min, release | recovery to baseline P95 ≤ 60 s; zero message loss; no DLQ growth | T0, T1 |
| `soak` | leak and drift | 70% of sustained; 12 h (T0: 24 h) | RSS growth ≤ 5%; P95 drift ≤ 10% first hour vs last hour; connection-pool and thread counts flat; DLQ depth flat; no unbounded cache growth | T0, T1 |
| `mixed` | realistic contention | named journey blend from TST-003, 4 h | every journey's own P95 within its own tier budget | T0, T1 |
| `scalability` | linearity and autoscaling | 25/50/75/100/125% step-ramp, 15 min per step | throughput linear within ±15%; HPA settles < 3 min; no thrash | T0–T2 |
| `failover-under-load` | HA proof under traffic | 100% sustained with an injected fault from the declared `failure_modes` | RTO and RPO within the NFR-001 tier row; error burst ≤ the agreed share of the error budget | T0, T1 |

Add this clarification immediately below the table: the numbers here are *profile parameters* —
durations, ramp shapes, drift tolerances — and belong in this document. They are not service
SLOs. Service SLOs (`P95`, `sustained_rps`, `RTO`) stay in the NFR spine and are linked, never
copied.

**`## Per-Profile Detail`** — one subsection per profile with: purpose in two sentences; the
load shape as a Mermaid `graph LR` or an ASCII ramp sketch; required inputs; pass criteria
restated as assertable checks; the common false pass; and the evidence artifact produced.

The "common false pass" content is required, because these are what make perf results
untrustworthy:

- `baseline`: passes because 10% load never leaves the JIT-warm fast path.
- `load`: passes because the dataset is too small — index selectivity and cache hit rate, not
  row count, drive latency. Cross-link [TST-004](./test-data-management.md).
- `stress`: passes because a closed workload model throttled offered load as latency rose, so
  the knee was never reached. Cross-link [TST-003](./workload-modelling.md).
- `spike`: passes because a queue absorbed the burst and the run ended before the queue
  drained — assert drain-to-baseline, not just accepted requests.
- `soak`: passes because the run was too short to expose a slow leak, or because the process
  was restarted by a deploy mid-run.
- `mixed`: passes on aggregate P95 while a low-volume, high-value journey inside the blend
  breaches its own budget. The pass criterion is per-journey.
- `scalability`: passes because the load generator, not the system under test, saturated first.
- `failover-under-load`: passes because the fault was injected at a quiet moment, or because
  the client retried transparently and the error burst was never measured.

**`## Threshold Derivation`** — a table mapping each pass criterion to its owning spine row:
latency → [NFR-002](../../nfr/latency-budget-model.md); sustained and peak throughput →
[NFR-004](../../nfr/throughput-model.md); capacity headroom →
[NFR-003](../../nfr/capacity-planning-model.md); RTO/RPO/availability →
[NFR-001](../../nfr/service-tiering-rto-rpo.md); acceptable error burst →
[NFR-005](../../nfr/error-budget-policy.md). State the rule: a document that contains a service
latency or throughput number instead of a link to its spine row is rejected at review.

**`## Profile Selection by Tier`** — a matrix of the 8 profiles × T0/T1/T2/T3 marking
`required` / `recommended` / `n/a`, consistent with the "Required for" column above.

**`## Result Baselining and Regression`** — how a run becomes the baseline, the regression
threshold that fails a comparison, how many runs establish confidence, and the evidence
retention period. Cross-link [TST-005](./environments-quality-gates.md).

**`## Compliance Mapping`** — Ring 0: Google SRE Workbook Ch. 5 (load and stress testing);
ISTQB performance-testing test types. Ring 1: BCBS 230 Principle 9 (severe-but-plausible
scenario testing), mapped specifically to `stress`, `spike`, and `failover-under-load`;
PCI-DSS 4.0 §6.4. Ring 2: SBV Circular 09/2020 §IV.3 with the `⚠️` marker.

**`## Related`** — TST-001, TST-003, TST-005, TST-006, TST-010, NFR-001…NFR-005, and TST-034
(owner of the named journey blends used by `mixed`).

- [ ] **Step 2: Verify the eight profile keys are spelled exactly as the gate expects**

```bash
for p in baseline load stress spike soak mixed scalability failover-under-load; do
  printf '%-22s %s\n' "$p" "$(grep -c "\`$p\`" knowledge-base/testing/strategy/performance-test-standard.md)"
done
```

Expected: every profile returns a count of at least 3 — profile table, detail subsection, tier
matrix. A count of `0` is a typo that will break Task 9's validation.

- [ ] **Step 3: Verify lint and Mermaid**

```bash
markdownlint knowledge-base/testing/strategy/performance-test-standard.md
bash scripts/mermaid-lint-doc.sh knowledge-base/testing/strategy/performance-test-standard.md
echo "exit=$?"
```

Expected: both exit `0`.

- [ ] **Step 4: Commit**

```bash
git add knowledge-base/testing/strategy/performance-test-standard.md
git commit -m "docs(testing): add TST-002 performance test standard

Eight normative performance profiles with load shapes, pass criteria,
per-profile false-pass modes, and threshold derivation from the NFR spine.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: TST-003 Workload Modelling

**Files:**
- Create: `knowledge-base/testing/strategy/workload-modelling.md`

**Interfaces:**
- Consumes: the eight profile keys from `TST-002`.
- Produces: the `workload_model` domain (`open`, `closed`); the named-blend identifier
  convention `journey-blend-<domain>-<condition>`; and the blend registry table that `TST-034`
  (Task 32) extends and every archetype's `blend_ref` points into.

- [ ] **Step 1: Create TST-003**

Header: `Catalog ID: TST-003 | Radii`, `Owner: @qe-lead`, `Tier Applicability: T0, T1, T2`.

**`## Problem Statement`** — 5 bullets: thread counts guessed rather than derived from
volumetrics; open and closed models mixed silently so breakpoint results are not comparable;
peak factors chosen ad hoc so `peak_rps` is unjustified; think time omitted, which turns a
realistic profile into an unrealistic hammer; journey blends undefined so `mixed` runs differ
between squads.

**`## Deriving Concurrency From Volumetrics`** — Little's Law as
`concurrency = arrival_rate × residence_time`, worked through end to end with a synthetic
example: a stated business volumetric, the residence time taken from the
[NFR-002](../../nfr/latency-budget-model.md) tier row, the resulting concurrency, and the
resulting thread or arrival-rate setting. State that concurrency is *derived*, never chosen.

**`## Open Versus Closed Workload Models`** — the most important section in this document.
Required content:

- **Closed model**: a fixed population of virtual users, each waiting for a response before
  issuing the next request. Offered load *falls* as latency rises. JMeter's standard Thread
  Group is closed.
- **Open model**: requests arrive at a specified rate regardless of whether prior requests have
  completed. Offered load is independent of latency. Gatling and k6 are open by default.
- **Why it matters**: under a closed model a saturating system self-throttles the test, so
  `stress` never finds the knee and `spike` under-represents the burst. A closed-model
  breakpoint number is not comparable to an open-model one.
- **Rule**: `stress`, `spike`, and `scalability` require `open`. `load`, `soak`, and `mixed` may
  use either, but the choice is declared in `test_acceptance_criteria.performance.workload_model`
  and held constant across comparable runs.
- **JMeter guidance**: use the Concurrency Thread Group or Arrivals Thread Group from the Custom
  Thread Groups plugin set to obtain an open model. Cross-link
  [TST-011](../tooling/jmeter.md).
- A Mermaid `graph LR` contrasting the two feedback paths — closed has a return edge from
  response to next request, open does not.

**`## Peak Factors`** — a table of the multipliers over sustained load, each with its driver and
the profile it feeds: Tet (annual, multi-day sustained elevation); end-of-month payroll and
settlement; payday clustering; NAPAS 247 intraday shape; promotional or campaign bursts.
Express these as *relative* multipliers and state that the absolute sustained and peak numbers
come from [NFR-004](../../nfr/throughput-model.md). Do not put an absolute RPS figure in this
document.

**`## Think Time, Pacing, and Arrival Distribution`** — define each; give the guidance per
profile: constant arrival for `load` and `soak`; Poisson for `mixed` to produce realistic
queueing; deterministic burst for `spike`; monotonic step for `stress` and `scalability`. State
that zero think time is valid only for machine-to-machine and batch flows, and misleading for
customer journeys.

**`## Named Journey Blends`** — the registry. Naming convention
`journey-blend-<domain>-<condition>`, for example `journey-blend-payments-peak`. Table columns:
Blend ID, Constituent journeys, Percentage mix, Reference architecture, Tier supplying each
journey's budget. Seed it with at least these three, built from real reference-architecture
rows:

| Blend ID | Constituents | Reference architecture |
|---|---|---|
| `journey-blend-payments-peak` | NAPAS instant transfer, balance enquiry, statement fetch, standing-order execution | [REF-002](../../reference-architectures/real-time-payments-napas.md) |
| `journey-blend-cards-authorisation` | card authorisation, 3DS2 challenge, reversal, dispute initiation | [REF-004](../../reference-architectures/card-authorization-3ds2.md) |
| `journey-blend-onboarding-campaign` | KYC submission, document upload, account opening, first funding | [REF-003](../../reference-architectures/kyc-aml-onboarding.md), [REF-009](../../reference-architectures/account-opening-omnichannel.md) |

State that percentages must sum to 100 and that each constituent journey carries its own tier
budget — a blend does not average tiers. Note that `TST-034` owns blend execution and may add
blends to this registry.

**`## Load Generator Sizing`** — how to confirm the generator is not the bottleneck: generator
CPU and network headroom, per-VU cost differences across the four tools, when distributed
generation is required, and the rule that a `scalability` result is void if generator
utilisation exceeded the documented ceiling.

**`## Compliance Mapping`** — Ring 0: Little's Law / queueing theory; Google SRE Workbook Ch. 5.
Ring 1: BCBS 230 Principle 9 (plausible scenario definition — peak factors are the scenario
inputs). Ring 2: SBV Circular 09/2020 §IV.3 with the `⚠️` marker.

**`## Related`** — TST-002, TST-005, TST-011, TST-034, NFR-003, NFR-004.

- [ ] **Step 2: Verify**

```bash
markdownlint knowledge-base/testing/strategy/workload-modelling.md
bash scripts/mermaid-lint-doc.sh knowledge-base/testing/strategy/workload-modelling.md
grep -c 'journey-blend-' knowledge-base/testing/strategy/workload-modelling.md
```

Expected: lint and Mermaid exit `0`; the blend grep returns at least `4` (convention statement
plus three registry rows).

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/testing/strategy/workload-modelling.md
git commit -m "docs(testing): add TST-003 workload modelling

Derives concurrency from volumetrics, fixes the open vs closed workload
model rule, and seeds the named journey blend registry.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: TST-004 Test Data Management

Binding on every other document in the corpus. The PII/PHI prohibition here is a hard
organisational requirement, not a preference.

**Files:**
- Create: `knowledge-base/testing/strategy/test-data-management.md`

**Interfaces:**
- Consumes: nothing.
- Produces: the data-classification rule cited by every archetype's §8, and the
  `synthetic-only` constraint that reviewers enforce.

- [ ] **Step 1: Create TST-004**

Header: `Catalog ID: TST-004 | Radii`, `Owner: @qe-lead`, `Tier Applicability: T0, T1, T2, T3`.

**`## Problem Statement`** — 5 bullets: production extracts in lower environments create
regulatory exposure; masked extracts still carry re-identification risk; undersized datasets
make load tests pass falsely; unseeded generation makes failures unreproducible; residue from a
prior run silently shifts the next run's baseline.

**`## Prohibitions`** — state these as absolute rules, each on its own line:

- Production data must not be used in any test environment covered by this corpus. This
  includes masked or partially masked production extracts.
- No PII or PHI in any committed fixture, snippet, or example: no real names, dates of birth,
  national ID numbers, member IDs, or account-holder details.
- Card PANs use designated test BIN ranges only. Never a real PAN, and never a PAN that
  passes Luhn against a live BIN.
- Account numbers, CIF identifiers, and customer references are synthetic and visibly marked
  as such.
- Cross-link [SEC-008 Data Masking](../../patterns/security/data-masking.md),
  [SEC-013 PII Tokenization](../../patterns/security/pii-tokenization-format-preserving.md),
  and [`governance/standards/data-classification.md`](../../../governance/standards/data-classification.md).

**`## Synthetic Generation Strategy`** — generation approaches and when each applies:
rule-based generation from the domain model; distribution-matched generation where cardinality
and skew matter; graph-consistent generation where referential integrity spans entities. State
the required properties: deterministic given a seed, reproducible across environments,
volume-scalable without regeneration from scratch.

**`## Referential Integrity`** — the entity graph that must stay consistent for ledger and
reconciliation archetypes to be valid: customer → account → ledger entry → transaction →
settlement instruction. State that a broken reference makes `TST-021` and `TST-039` results
meaningless rather than merely wrong. Include a Mermaid `graph LR` of the entity graph.

**`## Volume and Cardinality`** — the rule that matters: index selectivity and cache hit rate
drive latency, not row count. A perf dataset must match production *cardinality and skew*
within the ratio declared in [TST-005](./environments-quality-gates.md), not merely production
row count. Give the failure mode: a uniformly distributed synthetic dataset produces an
unrealistically high cache hit rate and a load test that passes but does not predict production.

**`## Seeding and Reproducibility`** — seed recorded in the run evidence; the same seed
reproduces the same dataset; a defect report cites the seed.

**`## Teardown and Reset`** — per-archetype teardown obligation; the rule that no run leaves
residue that changes a later run's baseline; how to verify reset (row counts, queue depths, DLQ
depth, cache state).

**`## Data for Each Discipline`** — a short table: which discipline needs what shape of data.
Functional needs boundary and negative cases. Performance needs volume, cardinality, and skew.
Resilience needs in-flight state at fault-injection time. Contract needs schema-valid and
deliberately schema-invalid payloads. Security needs an identity and entitlement matrix. Data
quality needs deliberately dirty records with known defect counts.

**`## Compliance Mapping`** — Ring 0: NIST SP 800-53 SA-15 / OWASP test-data guidance;
CIS control on data handling in non-production. Ring 1: PCI-DSS 4.0 §6.5.5 (live PANs
prohibited in test and development), §3 (stored account data protection); GDPR Art. 5(1)(c)
data minimisation. Ring 2: Decree 13/2023 on personal-data protection, bounding what test data
may contain, with the `⚠️ (working summary — pending Legal review)` marker.

**`## Related`** — TST-001, TST-002, TST-005, TST-039, TST-041, SEC-008, SEC-013, COMP-003,
COMP-004.

- [ ] **Step 2: Verify the prohibitions are present and unambiguous**

```bash
markdownlint knowledge-base/testing/strategy/test-data-management.md
bash scripts/mermaid-lint-doc.sh knowledge-base/testing/strategy/test-data-management.md
grep -niE 'must not|prohibit|never' knowledge-base/testing/strategy/test-data-management.md | head -20
```

Expected: lint and Mermaid exit `0`; the grep shows the five prohibition statements as
imperatives. Hedged wording ("should avoid", "where possible") is a defect here — rewrite as an
absolute rule.

- [ ] **Step 3: Verify no realistic-looking personal data crept into examples**

```bash
grep -nE '\b(4[0-9]{12}|5[1-5][0-9]{14})\b' knowledge-base/testing/strategy/test-data-management.md
grep -niE '\b(19|20)[0-9]{2}-[0-9]{2}-[0-9]{2}\b.*(dob|birth)' knowledge-base/testing/strategy/test-data-management.md
```

Expected: no matches. Any hit is a live-format PAN or a date of birth and must be removed.

- [ ] **Step 4: Commit**

```bash
git add knowledge-base/testing/strategy/test-data-management.md
git commit -m "docs(testing): add TST-004 test data management

Synthetic-only data strategy with absolute PII/PHI and live-PAN
prohibitions, referential integrity requirements, cardinality-over-row-count
sizing rule, seeding, and teardown.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: TST-005 Test Environments and Quality Gates

**Files:**
- Create: `knowledge-base/testing/strategy/environments-quality-gates.md`

**Interfaces:**
- Consumes: profile keys from `TST-002`; discipline keys from `TST-001`.
- Produces: the environment names (`dev`, `sit`, `uat`, `perf`, `prod-like`) used in
  `test_acceptance_criteria.evidence.environment`; the extrapolation ratio rule cited by
  `TST-004`; and the flakiness policy cited by every archetype.

- [ ] **Step 1: Create TST-005**

Header: `Catalog ID: TST-005 | Radii`, `Owner: @qe-lead`, `Tier Applicability: T0, T1, T2, T3`.

**`## Problem Statement`** — 5 bullets: perf results from an undersized environment extrapolated
without a stated ratio; shared environments producing contaminated results; gates placed so late
that defects are expensive; flaky tests retried until green, hiding real defects; evidence not
retained long enough to satisfy an audit.

**`## Environment Tiers`** — a table: name, purpose, data source, who owns it, which profiles
may run there. `dev` (unit and component, synthetic minimal), `sit` (integration and contract),
`uat` (business acceptance), `perf` (all performance profiles, isolated), `prod-like` (release
readiness drill including `failover-under-load`).

**`## Performance Environment Sizing and Extrapolation`** — the rule: a perf environment smaller
than production may be used only with a declared sizing ratio, and only latency-per-request and
per-instance throughput may be extrapolated. Explicitly non-extrapolable: anything gated by a
shared singleton (a single database primary, one HSM, one NAPAS link), cache hit rate, and
anything where cardinality differs. Cross-link [TST-004](./test-data-management.md) and
[NFR-003](../../nfr/capacity-planning-model.md).

**`## Isolation Requirements`** — no other workload on the perf environment during a run; state
the observable checks that prove isolation (neighbour CPU, shared-database session count,
network saturation) and the rule that a run without an isolation check is not evidence.

**`## Gate Placement`** — a table mapping each discipline and profile to its pipeline stage:
merge-request pipeline (unit, contract, `baseline`); scheduled nightly (`load`, `scalability`);
scheduled weekly (`stress`, `spike`); pre-release (`soak`, `mixed`); release readiness drill
(`failover-under-load`). Include a Mermaid `graph LR` of the pipeline with the gates attached.
Cross-link [BP-001](../../best-practices/ci-cd-pipeline-design.md).

**`## Entry and Exit Criteria`** — what must be true before a profile may run (build identity
pinned, dataset seeded and verified, isolation confirmed, baseline available) and what must be
true to call it passed (all pass criteria met, evidence captured, no unexplained anomaly).

**`## Flakiness Policy`** — the definition (a test that changes verdict without a change in the
system under test), the prohibition on blind retry-until-green, the quarantine mechanism, the
time limit on quarantine, and the rule that a quarantined test blocking a `required` discipline
blocks the release.

**`## Evidence and Retention`** — what constitutes evidence per profile (raw results file,
generated report, resource metrics, trace samples, the seed, the build identity, the isolation
check), where it is stored, and the retention period aligned to audit needs. Cross-link
[SEC-012 Tamper-Evident Audit Logging](../../patterns/security/audit-logging-tamper-evident.md)
for the integrity requirement on retained evidence.

**`## Compliance Mapping`** — Ring 0: NIST SP 800-53 CM-4 (security impact analysis) and CA-2.
Ring 1: PCI-DSS 4.0 §6.5.3 (separation of test and production environments), §6.5.5;
BCBS 230 Principle 9 (drill evidence). Ring 2: SBV Circular 09/2020 §IV.3 with the `⚠️` marker.

**`## Related`** — TST-001, TST-002, TST-003, TST-004, BP-001, NFR-003.

- [ ] **Step 2: Verify**

```bash
markdownlint knowledge-base/testing/strategy/environments-quality-gates.md
bash scripts/mermaid-lint-doc.sh knowledge-base/testing/strategy/environments-quality-gates.md
echo "exit=$?"
```

Expected: both exit `0`.

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/testing/strategy/environments-quality-gates.md
git commit -m "docs(testing): add TST-005 environments and quality gates

Environment tiers, perf sizing and extrapolation limits, isolation proof,
gate placement, flakiness policy, and evidence retention.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: TST-006 Resilience Test Standard

**Files:**
- Create: `knowledge-base/testing/strategy/resilience-test-standard.md`

**Interfaces:**
- Consumes: `failover-under-load` profile from `TST-002`; the `resilience.fault_scenarios`
  field from `TST-001`.
- Produces: the fault-class taxonomy (`dependency-latency`, `dependency-error`,
  `dependency-blackhole`, `resource-exhaustion`, `instance-loss`, `zone-loss`, `region-loss`,
  `clock-skew`, `partial-partition`, `slow-disk`) used by the resilience overlay in
  `TST-035` and every archetype with a resilience overlay.

- [ ] **Step 1: Create TST-006**

Header: `Catalog ID: TST-006 | Radii`, `Owner: @qe-lead`, `Tier Applicability: T0, T1`.

**`## Problem Statement`** — 5 bullets: declared `failure_modes` never exercised; faults injected
only at idle so recovery behaviour under load is unknown; blast radius asserted but not
measured; retry amplification untested so a recovery attempt worsens the outage; chaos
experiments run without a steady-state hypothesis so results are anecdotal.

**`## Relationship to BP-005`** — one paragraph. [BP-005 Chaos
Engineering](../../best-practices/chaos-engineering.md) owns the practice, culture, and
experiment lifecycle. `TST-006` owns the *test obligation*: which faults must be exercised for
a given tier, and how the result is asserted. Do not restate BP-005's content.

**`## Fault Class Taxonomy`** — a table with the ten fault classes named in Interfaces above,
each with: what it simulates, how to inject it, the expected system response, and the pattern
rows that must survive it. This taxonomy is the vocabulary for every resilience overlay.

**`## The failure_modes Obligation`** — the rule: every `FM*` entry in a service's
`nfr_acceptance_criteria.failure_modes` requires a corresponding entry in
`test_acceptance_criteria.resilience.fault_scenarios`. Show a worked synthetic example of a
declared failure mode, its `time_to_detect_seconds` and `time_to_recover_seconds`, and the
assertion that proves them. State that an unmatched failure mode is a detectable gap, and
reference [TPL-001](../../templates/nfr-acceptance-criteria-dab.md).

**`## Steady-State Hypothesis`** — how to state one before injecting: the metric, its normal
band, the duration of observation, and the abort condition. State that a fault injection without
a pre-declared steady state cannot be interpreted.

**`## Fault Injection Under Load`** — why a fault at idle proves little: connection pools are
empty, circuit breakers have no traffic to sample, and queues have no backlog to drain. The rule:
resilience assertions for T0 and T1 are made during the `failover-under-load` profile.

**`## Blast Radius Measurement`** — how to measure rather than assert: the set of affected
journeys, the fraction of requests impacted, the duration, and the recovery shape. Cross-link
[RES-005 Cell-Based Architecture](../../patterns/resilience/cell-based-architecture.md) and
[PLT-008 Multi-Tenancy Isolation](../../patterns/platform/multi-tenancy-isolation.md).

**`## Retry Amplification`** — the specific test: inject a dependency fault while the caller has
retries enabled, and measure offered load on the dependency during recovery. The failure mode is
a thundering herd on recovery. Cross-link
[RES-003 Retry with Backoff](../../patterns/resilience/retry-with-backoff.md).

Add a Mermaid `sequenceDiagram` showing: steady state → inject → detect → respond → recover →
verify steady state, with the assertion points marked.

**`## Compliance Mapping`** — Ring 0: NIST SP 800-53 CP-4 (contingency plan testing);
Principles of Chaos Engineering. Ring 1: BCBS 230 Principle 9 (severe-but-plausible scenario
testing and drill evidence); BCBS 230 §27 (blast radius containment). Ring 2: SBV Circular
09/2020 §IV.3 (BCP drill obligations) with the `⚠️` marker.

**`## Related`** — TST-001, TST-002, TST-035, TST-036, BP-005, RES-001…RES-012, NFR-001, NFR-005.

- [ ] **Step 2: Verify the ten fault classes are present**

```bash
for f in dependency-latency dependency-error dependency-blackhole resource-exhaustion \
         instance-loss zone-loss region-loss clock-skew partial-partition slow-disk; do
  printf '%-24s %s\n' "$f" "$(grep -c "$f" knowledge-base/testing/strategy/resilience-test-standard.md)"
done
markdownlint knowledge-base/testing/strategy/resilience-test-standard.md
bash scripts/mermaid-lint-doc.sh knowledge-base/testing/strategy/resilience-test-standard.md
```

Expected: every fault class returns at least `1`; lint and Mermaid exit `0`.

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/testing/strategy/resilience-test-standard.md
git commit -m "docs(testing): add TST-006 resilience test standard

Ten-class fault taxonomy, the failure_modes test obligation, steady-state
hypothesis requirement, fault-under-load rule, blast radius measurement,
and retry amplification testing.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: TST-007 Contract, TST-008 Security, TST-009 Data Quality Standards

Three discipline standards. They are grouped into one task because each is short, they share a
single review context, and none is independently useful — an archetype overlay needs all three
vocabularies available.

**Files:**
- Create: `knowledge-base/testing/strategy/contract-integration-test-standard.md`
- Create: `knowledge-base/testing/strategy/security-test-standard.md`
- Create: `knowledge-base/testing/strategy/data-quality-test-standard.md`

**Interfaces:**
- Consumes: discipline keys from `TST-001`.
- Produces: `schema_compat_mode` domain (`BACKWARD`, `FORWARD`, `FULL`, `NONE`) for TST-007;
  the authorisation-matrix method and token lifecycle case list for TST-008; the six DQ
  dimensions for TST-009. Consumed by the overlay sections of TST-030, TST-040, TST-041,
  TST-037, TST-038, TST-039.

- [ ] **Step 1: Create TST-007 Contract and Integration Test Standard**

Header: `Catalog ID: TST-007 | Radii`, `Owner: @qe-lead`, `Tier Applicability: T0, T1, T2`.

Required substance:

- **`## Problem Statement`** — 5 bullets: a producer ships a compatible-looking change that
  breaks a consumer; integration suites test the mock, not the contract; schema compatibility
  mode undeclared so the registry's guarantee is unknown; async contracts untested because only
  REST has tooling; error codes drift so consumers switch on prose.
- **`## Relationship to INT-015`** — [INT-015 API Contract
  Testing](../../patterns/integration/api-contract-testing.md) owns the pattern. `TST-007` owns
  the test obligation and the coverage definition. Do not restate INT-015.
- **`## Consumer-Driven Contract Method`** — who writes the contract, where it is published,
  how the producer verifies, what "verified" means, and what happens when verification fails.
- **`## Schema Compatibility Modes`** — a table of `BACKWARD`, `FORWARD`, `FULL`, `NONE`: what
  each permits, which change classes it rejects, and the test that proves it. Cross-link
  [INT-013 Schema Registry
  Governance](../../patterns/integration/schema-registry-governance.md).
- **`## Async Contract Testing`** — AsyncAPI and CloudEvents envelope conformance, and the
  specific difficulty: there is no request/response pair to assert on, so assertions are made on
  the published message plus the consumer's observable effect. Cross-link
  [INT-010](../../patterns/integration/asyncapi-specification.md) and
  [INT-011](../../patterns/integration/cloudevents-envelope.md).
- **`## Error Contract Testing`** — every documented error code is reachable by a test, and the
  mapping is stable. Cross-link
  [INT-012 Error Code Mapping](../../patterns/integration/error-code-mapping.md).
- **`## Integration Scope Boundary`** — what belongs in a contract test versus an integration
  test versus an end-to-end journey test, with the rule that an assertion belongs at the lowest
  level that can make it.
- Mermaid `sequenceDiagram` of the consumer-driven verification loop.
- **`## Compliance Mapping`** — Ring 0: Pact consumer-driven contract specification; OpenAPI and
  AsyncAPI specifications. Ring 1: ISO 20022 message conformance; SWIFT CSP control 2.x. Ring 2:
  SBV Circular 09/2020 §IV.3 with the `⚠️` marker.

- [ ] **Step 2: Create TST-008 Security Test Standard**

Header: `Catalog ID: TST-008 | Radii`, `Owner: @qe-lead`, `Tier Applicability: T0, T1`.
Note `@infosec-architect` co-ownership in the Related section.

Required substance:

- **`## Problem Statement`** — 5 bullets: authorisation tested only on the happy path so
  privilege escalation is undetected; token expiry and revocation asserted by reading config
  rather than by testing behaviour; masking verified in the UI but not in logs, traces, or error
  payloads; DAST run against an environment with controls disabled; secrets rotation tested at
  idle so in-flight requests during rotation are never exercised.
- **`## Scope and Boundary`** — state clearly what this standard covers (verification that
  declared controls behave as declared) and what it does not (penetration testing engagements,
  red-team exercises, and vulnerability research, which are owned by InfoSec under their own
  process). This boundary matters: it keeps QE's obligation testable and avoids implying QE
  performs offensive security work.
- **`## Authorisation Matrix Method`** — the core technique: enumerate the cross-product of
  identity × role or attribute × resource × operation, then assert the expected allow or deny for
  every cell. Give the cell-count formula and the rule that an untested cell is an unverified
  control. State that deny cases matter more than allow cases. Cross-link
  [SEC-010 ABAC](../../patterns/security/attribute-based-access-control.md).
- **`## Token Lifecycle Cases`** — the required case list: valid token accepted; expired token
  rejected; token with wrong audience rejected; token with wrong issuer rejected; tampered
  signature rejected; revoked token rejected before its natural expiry; refresh rotation
  invalidates the prior refresh token; token bound to one client rejected when replayed by
  another. Cross-link [SEC-006](../../patterns/security/jwt-best-practices.md),
  [SEC-011](../../patterns/security/session-revocation.md),
  [SEC-005](../../patterns/security/bff-token-binding.md).
- **`## Egress Assertion for Sensitive Data`** — the rule that masking must be asserted on every
  egress path, not just the primary response: logs, traces, metrics labels, error payloads,
  webhook bodies, exports, and support tooling. State that this is where masking most often
  fails.
- **`## DAST Placement and Preconditions`** — where automated scanning runs, the precondition
  that all controls are enabled (a scan against a control-disabled environment is worthless),
  and the requirement that findings are triaged rather than counted.
- **`## Rotation Under Load`** — secrets and certificate rotation must be exercised with traffic
  in flight; the assertion is zero failed requests attributable to the rotation. Cross-link
  [SEC-007](../../patterns/security/secrets-rotation.md) and
  [TST-036](../archetypes/zero-downtime-deploy-rotation.md).
- Mermaid `graph TD` of the authorisation matrix cross-product.
- **`## Compliance Mapping`** — Ring 0: OWASP ASVS and OWASP WSTG; NIST SP 800-53 CA-8, AC-3.
  Ring 1: PCI-DSS 4.0 §6.4, §11.3, §11.4; SWIFT CSP control 2.x. Ring 2: Decree 13/2023 and SBV
  Circular 09/2020 §IV.3 with the `⚠️` marker.

- [ ] **Step 3: Create TST-009 Data Quality Test Standard**

Header: `Catalog ID: TST-009 | Radii`, `Owner: @qe-lead`, `Tier Applicability: T0, T1, T2`.

Required substance:

- **`## Problem Statement`** — 5 bullets: DQ rules defined but never asserted in a test; read
  models compared for equality without accounting for eventual consistency, producing flaky
  tests; reconciliation run with an undeclared tolerance so a real break is absorbed; lag
  measured as an average so tail lag is invisible; lineage documented but not verified.
- **`## The Six Dimensions`** — a table: completeness, accuracy, consistency, timeliness,
  uniqueness, validity. For each: the assertion form, the metric, and a synthetic example.
- **`## Reconciliation Testing`** — the method: independent recomputation from source, not
  re-reading the same aggregate. Declared tolerance, with the rule that a monetary
  reconciliation tolerance is exactly zero and any non-zero tolerance requires named approval.
  Cross-link [TST-021](../archetypes/ledger-monetary-invariant.md) and
  [DATA-011 Data Quality Rules](../../patterns/data/data-quality-rules.md).
- **`## Convergence and Lag Assertions`** — how to assert eventual consistency without flakiness:
  a bounded convergence window with a declared upper bound, asserted at the tail percentile
  rather than the mean, and a hard failure if the bound is exceeded. State that polling until
  success without a bound is not a test.
- **`## Lineage Verification`** — assert that a value's stated provenance is real by perturbing
  the source and observing the derived value change. Cross-link
  [DATA-009 Data Lineage](../../patterns/data/data-lineage.md).
- **`## Dirty-Data Corpus`** — a deliberately defective synthetic corpus with known defect counts
  per dimension, so a DQ rule's recall is measurable. Cross-link
  [TST-004](./test-data-management.md).
- Mermaid `graph LR` of source → transform → read model, with the assertion points marked.
- **`## Compliance Mapping`** — Ring 0: DAMA-DMBOK data-quality dimensions. Ring 1: BCBS 239
  Principles 3 (accuracy and integrity), 4 (completeness), and 5 (timeliness). Ring 2: SBV
  Circular 09/2020 reporting-accuracy expectations with the `⚠️` marker.

- [ ] **Step 4: Verify all three documents**

```bash
markdownlint knowledge-base/testing/strategy/contract-integration-test-standard.md \
             knowledge-base/testing/strategy/security-test-standard.md \
             knowledge-base/testing/strategy/data-quality-test-standard.md
for f in contract-integration-test-standard security-test-standard data-quality-test-standard; do
  bash scripts/mermaid-lint-doc.sh "knowledge-base/testing/strategy/$f.md"
  printf '%s mermaid exit=%s\n' "$f" "$?"
done
python3 scripts/validate-internal-links.py
echo "links exit=$?"
```

Expected: lint exits `0`; each Mermaid check exits `0`. The link check may warn about
`../archetypes/*.md` targets that do not exist until Waves C–F — that is expected and
non-blocking here, but the filenames must match the File Structure table exactly so they resolve
once those files land.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/testing/strategy/contract-integration-test-standard.md \
        knowledge-base/testing/strategy/security-test-standard.md \
        knowledge-base/testing/strategy/data-quality-test-standard.md
git commit -m "docs(testing): add TST-007, TST-008, TST-009 discipline standards

Contract compatibility modes and consumer-driven verification; the
authorisation-matrix method, token lifecycle cases, and egress masking
assertions; the six data-quality dimensions with bounded convergence
assertions.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 9: TST-010 Tool Selection Matrix

**Files:**
- Create: `knowledge-base/testing/tooling/tool-selection-matrix.md`

**Interfaces:**
- Consumes: profile keys from `TST-002`; the `workload_model` domain from `TST-003`.
- Produces: the `primary_tool` domain — `jmeter`, `gatling-karate`, `k6`, `locust` — validated by
  `scripts/validate-testing-coverage.py` (Task 10) and referenced by every archetype's §6 Tool
  Fit table.

- [ ] **Step 1: Create TST-010**

Header: `Catalog ID: TST-010 | Radii`, `Owner: @qe-lead`, `Tier Applicability: T0, T1, T2, T3`.

**`## Problem Statement`** — 5 bullets: tool chosen by squad familiarity rather than fit; results
compared across tools with different workload models; protocol gaps discovered mid-engagement;
four tools maintained with no stated division of labour; CI gate built on a tool that cannot
express thresholds as code.

**`## Position of Each Tool`** — this exact table:

| Tool | Position | Strongest fit |
|---|---|---|
| JMeter | **Primary.** Canonical recipe in every archetype. | Broadest protocol coverage — JDBC, JMS, Kafka, SOAP, ISO 8583 via samplers — plus distributed master/worker execution and the HTML dashboard. Default for protocol-heavy banking flows. |
| Gatling + Karate | Secondary, highest leverage. | `karate-gatling` reuses the same Karate `.feature` files as both functional API tests and performance scenarios — one artifact, two disciplines. Open model, low resource cost per virtual user. |
| k6 | CI gate. | Thresholds-as-code make it the natural pipeline gate for the `baseline` profile. `xk6` extensions cover Kafka, SQL, and browser. |
| Locust | Specialist. | Bespoke stateful scenario logic, or reuse of existing Python domain libraries that would be awkward in JMX or Scala. |

**`## Capability Matrix`** — rows are the four tools; columns: HTTP/REST, SOAP, gRPC, JMS, Kafka,
JDBC, ISO 8583, ISO 20022, default workload model, scripting language, thresholds-as-code,
distributed execution, per-VU resource cost, built-in reporting, correlation support, learning
curve. Fill every cell — `native`, `plugin`, `extension`, or `no`. A blank cell is a defect.

**`## Decision Tree`** — a Mermaid `flowchart TD` that resolves to exactly one tool. Required
decision points, in this order: Is this the CI merge-gate `baseline` profile? → k6. Does the
scenario need a protocol with no native support outside JMeter (JMS, ISO 8583, JDBC-heavy)? →
JMeter. Does a Karate functional suite already exist for this API? → Gatling + Karate. Does the
scenario need bespoke stateful logic or an existing Python domain library? → Locust. Otherwise →
JMeter.

**`## Cross-Tool Comparability Rules`** — three rules: results from different workload models are
not comparable; a baseline established with one tool may only be compared against runs from the
same tool and version; the tool and version are recorded in the run evidence. Cross-link
[TST-003](../strategy/workload-modelling.md) and [TST-005](../strategy/environments-quality-gates.md).

**`## Licensing and Support Posture`** — one short paragraph: all four are open-source; note where
a plugin's licence differs from the core tool, and that plugin additions follow the technology
radar process. Cross-link [`knowledge-base/technology-radar.md`](../../technology-radar.md).

**`## Compliance Mapping`** — Ring 0: ISTQB test-tool selection guidance. Ring 1: BCBS 230
Principle 9 (repeatable, evidenced scenario testing depends on tool determinism). Ring 2: SBV
Circular 09/2020 §IV.3 with the `⚠️` marker.

**`## Related`** — TST-002, TST-003, TST-011…TST-014.

- [ ] **Step 2: Verify the four tool keys and the capability matrix completeness**

```bash
for t in jmeter gatling-karate k6 locust; do
  printf '%-16s %s\n' "$t" "$(grep -ci "$t" knowledge-base/testing/tooling/tool-selection-matrix.md)"
done
markdownlint knowledge-base/testing/tooling/tool-selection-matrix.md
bash scripts/mermaid-lint-doc.sh knowledge-base/testing/tooling/tool-selection-matrix.md
grep -n '| *|' knowledge-base/testing/tooling/tool-selection-matrix.md
```

Expected: each tool key appears at least twice; lint and Mermaid exit `0`; the final grep
returns **no matches** — an empty table cell is a defect.

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/testing/tooling/tool-selection-matrix.md
git commit -m "docs(testing): add TST-010 tool selection matrix

Positions JMeter as primary with Gatling+Karate, k6, and Locust in defined
secondary roles; adds a capability matrix, a single-answer decision tree,
and cross-tool comparability rules.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 10: Coverage Schema and the Validation Gate

This is the task that makes coverage provable. The script is written first and must fail against
a deliberately broken fixture before it is trusted.

**Files:**
- Create: `knowledge-base/testing/coverage/_testing-coverage.yml`
- Create: `scripts/validate-testing-coverage.py`

**Interfaces:**
- Consumes: the discipline keys, obligation levels, profile keys, and tool keys produced by
  Tasks 2, 3, and 9.
- Produces: `load_rows(path, label) -> list[dict]`, `archetype_ids() -> dict[str, str]`, and
  `validate() -> int` (returns the issue count; `0` means clean). Task 11's render script imports
  nothing from this file — the two are independent by design so a render failure cannot mask a
  validation failure.

- [ ] **Step 1: Create the coverage file with the schema and three seed rows**

Write `knowledge-base/testing/coverage/_testing-coverage.yml`:

```yaml
# Testing coverage — source of truth.
#
# One row per Approved row in governance/standards/_catalog-inventory.yml.
# Validated by scripts/validate-testing-coverage.py.
# Rendered into coverage-matrix.md by scripts/render-testing-coverage.py.
#
# Field domains:
#   disciplines.*   required | recommended | n/a | governs
#   perf_profiles[] baseline | load | stress | spike | soak | mixed |
#                   scalability | failover-under-load
#   primary_tool    jmeter | gatling-karate | k6 | locust
#   archetypes[]    TST-020 .. TST-043; may be empty only when every
#                   discipline is 'governs'
version: 1
last_updated: '2026-08-12'
rows:
  - catalog_id: RES-002
    title: Circuit Breaker
    path: knowledge-base/patterns/resilience/circuit-breaker.md
    tiers: [T0, T1, T2]
    archetypes: [TST-035, TST-031]
    disciplines:
      functional: required
      performance: required
      resilience: required
      contract: n/a
      security: n/a
      data_quality: n/a
    perf_profiles: [baseline, load, spike, failover-under-load]
    primary_tool: jmeter
    owner: sre-lead
    notes: ''
  - catalog_id: BSP-002
    title: Idempotent Payment Key
    path: knowledge-base/patterns/banking-solutions/idempotent-payment-key.md
    tiers: [T0]
    archetypes: [TST-020]
    disciplines:
      functional: required
      performance: required
      resilience: required
      contract: recommended
      security: n/a
      data_quality: required
    perf_profiles: [baseline, load, stress, spike, soak]
    primary_tool: jmeter
    owner: payments-domain-owner
    notes: ''
  - catalog_id: NFR-002
    title: Latency Budget Model
    path: knowledge-base/nfr/latency-budget-model.md
    tiers: []
    archetypes: []
    disciplines:
      functional: governs
      performance: governs
      resilience: governs
      contract: governs
      security: governs
      data_quality: governs
    perf_profiles: []
    primary_tool: jmeter
    owner: sre-lead
    notes: Spine doc — supplies latency thresholds; not itself under test
```

Verify the three `title`, `path`, `tiers`, and `owner` values against
`governance/standards/_catalog-inventory.yml` before moving on — they must match that file
exactly, because checks 5 and 7 compare them.

- [ ] **Step 2: Create the validation script**

Write `scripts/validate-testing-coverage.py` with this literal content:

```python
#!/usr/bin/env python3
"""Validate the testing coverage matrix against the catalog inventory.

Seven checks:
  1. Every inventory row has a coverage row.
  2. Every coverage row names a catalog_id that exists in the inventory.
  3. Every referenced archetype ID exists as an archetype document.
  4. Every disciplines / perf_profiles / primary_tool value is in its domain.
  5. Every coverage row's path exists on disk.
  6. archetypes[] is non-empty unless every discipline is 'governs'.
  7. Every coverage row's tiers match the inventory row's tiers.

Usage:
    python3 scripts/validate-testing-coverage.py
    python3 scripts/validate-testing-coverage.py --quiet
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path
from typing import Any

import yaml

ROOT = Path(__file__).resolve().parent.parent
INVENTORY_PATH = ROOT / "governance/standards/_catalog-inventory.yml"
COVERAGE_PATH = ROOT / "knowledge-base/testing/coverage/_testing-coverage.yml"
ARCHETYPE_DIR = ROOT / "knowledge-base/testing/archetypes"

CATALOG_ID_RE = re.compile(r"^Catalog ID:\s*(?P<id>TST-\d{3})\b", re.MULTILINE)

DISCIPLINES = (
    "functional",
    "performance",
    "resilience",
    "contract",
    "security",
    "data_quality",
)
OBLIGATIONS = {"required", "recommended", "n/a", "governs"}
PROFILES = {
    "baseline",
    "load",
    "stress",
    "spike",
    "soak",
    "mixed",
    "scalability",
    "failover-under-load",
}
TOOLS = {"jmeter", "gatling-karate", "k6", "locust"}


def load_rows(path: Path, label: str) -> list[dict[str, Any]]:
    if not path.exists():
        sys.stderr.write("ERROR: %s not found at %s\n" % (label, path))
        raise SystemExit(2)
    data = yaml.safe_load(path.read_text()) or {}
    rows = data.get("rows")
    if not isinstance(rows, list):
        sys.stderr.write("ERROR: %s has no 'rows' list\n" % label)
        raise SystemExit(2)
    return rows


def archetype_ids() -> dict[str, str]:
    """Map TST-0NN -> repo-relative path, read from each archetype's header."""
    found: dict[str, str] = {}
    if not ARCHETYPE_DIR.exists():
        return found
    for path in sorted(ARCHETYPE_DIR.glob("*.md")):
        match = CATALOG_ID_RE.search(path.read_text(errors="ignore"))
        if match:
            found[match.group("id")] = path.relative_to(ROOT).as_posix()
    return found


def validate() -> list[str]:
    issues: list[str] = []

    inventory = load_rows(INVENTORY_PATH, "catalog inventory")
    coverage = load_rows(COVERAGE_PATH, "testing coverage")

    inv_by_id = {row["id"]: row for row in inventory}
    cov_by_id: dict[str, dict[str, Any]] = {}
    for row in coverage:
        cid = row.get("catalog_id")
        if not cid:
            issues.append("coverage row missing catalog_id: %r" % row)
            continue
        if cid in cov_by_id:
            issues.append("check2 duplicate coverage row for %s" % cid)
        cov_by_id[cid] = row

    known_archetypes = archetype_ids()

    # Check 1 — every inventory row has a coverage row.
    for cid in sorted(inv_by_id):
        if cid not in cov_by_id:
            issues.append("check1 %s has no coverage row" % cid)

    # Check 2 — every coverage row names a real catalog_id.
    for cid in sorted(cov_by_id):
        if cid not in inv_by_id:
            issues.append("check2 %s is not in the catalog inventory" % cid)

    for cid in sorted(cov_by_id):
        row = cov_by_id[cid]
        inv = inv_by_id.get(cid)

        archetypes = row.get("archetypes") or []
        disciplines = row.get("disciplines") or {}
        profiles = row.get("perf_profiles") or []

        # Check 3 — referenced archetypes exist.
        for aid in archetypes:
            if aid not in known_archetypes:
                issues.append(
                    "check3 %s references archetype %s which has no document" % (cid, aid)
                )

        # Check 4 — enum domains.
        for key in DISCIPLINES:
            if key not in disciplines:
                issues.append("check4 %s missing discipline key '%s'" % (cid, key))
            elif disciplines[key] not in OBLIGATIONS:
                issues.append(
                    "check4 %s discipline '%s' has invalid value '%s'"
                    % (cid, key, disciplines[key])
                )
        for extra in sorted(set(disciplines) - set(DISCIPLINES)):
            issues.append("check4 %s has unknown discipline key '%s'" % (cid, extra))
        for profile in profiles:
            if profile not in PROFILES:
                issues.append("check4 %s has invalid perf_profile '%s'" % (cid, profile))
        if row.get("primary_tool") not in TOOLS:
            issues.append(
                "check4 %s has invalid primary_tool '%s'" % (cid, row.get("primary_tool"))
            )

        # Check 5 — path exists.
        rel = row.get("path")
        if not rel:
            issues.append("check5 %s has no path" % cid)
        elif not (ROOT / rel).exists():
            issues.append("check5 %s path does not exist: %s" % (cid, rel))

        # Check 6 — archetypes required unless everything governs.
        values = [disciplines.get(key) for key in DISCIPLINES]
        all_governs = bool(values) and all(value == "governs" for value in values)
        if not archetypes and not all_governs:
            issues.append(
                "check6 %s has no archetypes but is not fully 'governs'" % cid
            )
        if archetypes and all_governs:
            issues.append(
                "check6 %s is fully 'governs' but still names archetypes" % cid
            )

        # Check 7 — tiers agree with the inventory.
        if inv is not None:
            inv_tiers = set(inv.get("tiers") or [])
            cov_tiers = set(row.get("tiers") or [])
            if inv_tiers != cov_tiers:
                issues.append(
                    "check7 %s tiers %s disagree with inventory %s"
                    % (cid, sorted(cov_tiers), sorted(inv_tiers))
                )

    return issues


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--quiet",
        action="store_true",
        help="print only the summary line",
    )
    args = parser.parse_args()

    issues = validate()

    if issues:
        if not args.quiet:
            for issue in issues:
                print("  X %s" % issue)
        print("FAIL: %d testing-coverage issue(s)" % len(issues))
        return 1

    print("OK: testing coverage is consistent with the catalog inventory")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
```

- [ ] **Step 3: Run the script and verify it FAILS on the expected 188 missing rows**

```bash
python3 scripts/validate-testing-coverage.py | tail -5
echo "exit=$?"
```

Expected: exit `1`. The output must report `check1` failures for the 188 inventory rows that have
no coverage row yet (191 − 3 seeded), and **no** `check3`…`check7` failures for the three seeded
rows. If a seeded row reports `check3`, that is correct at this point: no archetype documents
exist yet, so `TST-035`, `TST-031`, and `TST-020` cannot resolve. Confirm that is the only
`check3` cause before continuing.

- [ ] **Step 4: Prove each check fires, using a scratch copy**

Do this against a temporary copy so the real file is never left broken:

```bash
cp knowledge-base/testing/coverage/_testing-coverage.yml /tmp/cov-backup.yml
python3 - <<'PY'
import re, pathlib
p = pathlib.Path("knowledge-base/testing/coverage/_testing-coverage.yml")
t = p.read_text()
p.write_text(t.replace("primary_tool: jmeter", "primary_tool: wrk", 1))
PY
python3 scripts/validate-testing-coverage.py --quiet
echo "should be exit=1 with a check4 invalid primary_tool"
cp /tmp/cov-backup.yml knowledge-base/testing/coverage/_testing-coverage.yml
```

Repeat the same pattern for: an invalid `perf_profiles` value (`check4`), a `path` pointing at a
nonexistent file (`check5`), removing `archetypes` from the `RES-002` row (`check6`), and changing
the `NFR-002` row's `tiers` to `[T0]` (`check7`). After each, restore from the backup. Confirm
every check produces a distinct, readable message. A check that cannot be made to fire is a
broken check.

- [ ] **Step 5: Restore the file and confirm only check1 and check3 remain**

```bash
cp /tmp/cov-backup.yml knowledge-base/testing/coverage/_testing-coverage.yml
python3 scripts/validate-testing-coverage.py --quiet
git diff --stat knowledge-base/testing/coverage/_testing-coverage.yml
```

Expected: exit `1` with only `check1` and `check3` messages, and an empty `git diff` — the file is
byte-identical to Step 1.

- [ ] **Step 6: Commit**

```bash
git add knowledge-base/testing/coverage/_testing-coverage.yml scripts/validate-testing-coverage.py
git commit -m "feat(scripts): add testing coverage schema and validation gate

Seven checks tie the testing coverage file to the catalog inventory:
missing rows, unknown IDs, dangling archetype references, enum domains,
missing paths, empty archetypes outside 'governs', and tier disagreement.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 11: Coverage Matrix Renderer

**Files:**
- Create: `scripts/render-testing-coverage.py`
- Create: `knowledge-base/testing/coverage/coverage-matrix.md`

**Interfaces:**
- Consumes: `_testing-coverage.yml` written in Task 10.
- Produces: `render() -> str` and a `--check` mode that exits non-zero when the committed table
  differs from what the YAML would generate. Task 47 runs `--check` as a release gate.

- [ ] **Step 1: Create the matrix document with generation markers**

Write `knowledge-base/testing/coverage/coverage-matrix.md`:

````markdown
# Testing Coverage Matrix

Status: Approved | Last Reviewed: 2026-08-12 | Owner: @qe-lead
Catalog ID: TST-015 | Radii
Tier Applicability: N/A (generated coverage report)

## Purpose

Maps every Approved catalog row to the test archetypes that cover it, the disciplines that are
obligatory for it, and the performance profiles its tier requires. Coverage is enforced
mechanically: `scripts/validate-testing-coverage.py` fails when an inventory row has no coverage
row, so a new pattern cannot be added without deciding how it is tested.

## How to Read This Table

- **Disciplines** use the four obligation levels from
  [TST-001](../strategy/test-strategy-standard.md): `required`, `recommended`, `n/a`, `governs`.
- **`governs`** marks a meta-document that constrains testing rather than being tested.
- **Profiles** are the eight defined in [TST-002](../strategy/performance-test-standard.md).
- **Primary tool** is the default per [TST-010](../tooling/tool-selection-matrix.md); an
  archetype's Tool Fit table may justify another.

## Source of Truth

Do not hand-edit the table below. Edit `_testing-coverage.yml` and regenerate:

```bash
python3 scripts/render-testing-coverage.py
```

<!-- BEGIN GENERATED -->
<!-- END GENERATED -->

## Related

- [TST-001](../strategy/test-strategy-standard.md) — disciplines and obligation levels
- [TST-002](../strategy/performance-test-standard.md) — performance profiles
- [TST-010](../tooling/tool-selection-matrix.md) — tool selection
- [`enterprise-architecture-catalog.md`](../../../governance/standards/enterprise-architecture-catalog.md) — the catalog this table covers
````

- [ ] **Step 2: Create the renderer**

Write `scripts/render-testing-coverage.py` with this literal content:

```python
#!/usr/bin/env python3
"""Render the testing coverage table into coverage-matrix.md.

Rewrites only the block between the BEGIN/END GENERATED markers, so
hand-written narrative in the document survives regeneration.

Usage:
    python3 scripts/render-testing-coverage.py
    python3 scripts/render-testing-coverage.py --check
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path
from typing import Any

import yaml

ROOT = Path(__file__).resolve().parent.parent
COVERAGE_PATH = ROOT / "knowledge-base/testing/coverage/_testing-coverage.yml"
MATRIX_PATH = ROOT / "knowledge-base/testing/coverage/coverage-matrix.md"

BEGIN = "<!-- BEGIN GENERATED -->"
END = "<!-- END GENERATED -->"

DISCIPLINES = (
    ("functional", "Func"),
    ("performance", "Perf"),
    ("resilience", "Resil"),
    ("contract", "Contr"),
    ("security", "Sec"),
    ("data_quality", "DQ"),
)
SHORT = {"required": "R", "recommended": "r", "n/a": "—", "governs": "G"}


def load_rows() -> list[dict[str, Any]]:
    data = yaml.safe_load(COVERAGE_PATH.read_text()) or {}
    rows = data.get("rows") or []
    return sorted(rows, key=lambda row: row.get("catalog_id", ""))


def render() -> str:
    rows = load_rows()
    header = ["Catalog ID", "Title", "Tiers", "Archetypes"]
    header += [label for _, label in DISCIPLINES]
    header += ["Profiles", "Tool"]

    lines = [BEGIN, ""]
    lines.append("| " + " | ".join(header) + " |")
    lines.append("|" + "---|" * len(header))

    for row in rows:
        disciplines = row.get("disciplines") or {}
        cells = [
            row.get("catalog_id", ""),
            str(row.get("title", "")).replace("|", "\\|"),
            ", ".join(row.get("tiers") or []) or "—",
            ", ".join(row.get("archetypes") or []) or "—",
        ]
        cells += [SHORT.get(disciplines.get(key), "?") for key, _ in DISCIPLINES]
        cells.append(", ".join(row.get("perf_profiles") or []) or "—")
        cells.append(str(row.get("primary_tool", "")))
        lines.append("| " + " | ".join(cells) + " |")

    lines.append("")
    lines.append(
        "Legend: `R` required · `r` recommended · `—` not applicable · `G` governs. "
        "%d rows." % len(rows)
    )
    lines.append("")
    lines.append(END)
    return "\n".join(lines)


def splice(document: str, block: str) -> str:
    start = document.find(BEGIN)
    end = document.find(END)
    if start == -1 or end == -1:
        sys.stderr.write(
            "ERROR: generation markers not found in %s\n" % MATRIX_PATH
        )
        raise SystemExit(2)
    return document[:start] + block + document[end + len(END):]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="exit non-zero if the committed table is stale",
    )
    args = parser.parse_args()

    document = MATRIX_PATH.read_text()
    updated = splice(document, render())

    if args.check:
        if updated != document:
            print("FAIL: coverage-matrix.md is stale — run render-testing-coverage.py")
            return 1
        print("OK: coverage-matrix.md matches _testing-coverage.yml")
        return 0

    if updated == document:
        print("OK: coverage-matrix.md already current")
        return 0

    MATRIX_PATH.write_text(updated)
    print("Updated %s" % MATRIX_PATH.relative_to(ROOT))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
```

- [ ] **Step 3: Verify --check fails on the empty table, then render**

```bash
python3 scripts/render-testing-coverage.py --check
echo "expected exit=1 (stale: table is empty, YAML has 3 rows)"
python3 scripts/render-testing-coverage.py
python3 scripts/render-testing-coverage.py --check
echo "expected exit=0 now"
```

Expected: first `--check` exits `1`, the render reports an update, the second `--check` exits `0`.

- [ ] **Step 4: Verify the rendered table and that the narrative survived**

```bash
sed -n '/BEGIN GENERATED/,/END GENERATED/p' knowledge-base/testing/coverage/coverage-matrix.md
grep -c '^## ' knowledge-base/testing/coverage/coverage-matrix.md
markdownlint knowledge-base/testing/coverage/coverage-matrix.md
```

Expected: three data rows in catalog-ID order — `BSP-002`, `NFR-002`, `RES-002` — with the
`NFR-002` row showing `G` in all six discipline columns and `—` for both archetypes and profiles;
the `grep -c '^## '` count is exactly `4` (`Purpose`, `How to Read This Table`, `Source of Truth`,
`Related`), proving the narrative above and below the markers survived; and lint exits `0`.

The legend line closing the generated block reads:
`Legend: \`R\` required · \`r\` recommended · \`—\` not applicable · \`G\` governs. 3 rows.`

- [ ] **Step 5: Verify markers are idempotent**

```bash
python3 scripts/render-testing-coverage.py
git diff --stat knowledge-base/testing/coverage/coverage-matrix.md
```

Expected: "already current" and an empty diff. A non-empty diff on a second run means the
renderer is not idempotent — fix before committing.

- [ ] **Step 6: Commit**

```bash
git add scripts/render-testing-coverage.py knowledge-base/testing/coverage/coverage-matrix.md
git commit -m "feat(scripts): add coverage matrix renderer and TST-015 report

Renders _testing-coverage.yml into coverage-matrix.md between generation
markers so narrative survives; --check mode gates staleness.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 12: Wave A Catalog Registration

Registers the 12 Wave A rows: `TST-001`…`TST-010`, `TST-015`, and `TPL-005`. Later waves each
register their own rows and update the counts again, so the catalog is internally consistent
after every wave rather than only at the end.

**Files:**
- Modify: `governance/standards/_catalog-inventory.yml`
- Modify: `governance/standards/enterprise-architecture-catalog.md`
- Modify: `mkdocs.yml`
- Modify: `.gitlab/CODEOWNERS`

**Interfaces:**
- Consumes: every document created in Tasks 1–11.
- Produces: the `testing` category, `TST-001` as the 7th spine doc, and the running row count
  that Tasks 17, 29, 36, 42, and 45 each increment.

**Running totals** — after each wave. Every wave's registration task asserts its own row:

| After wave | Rows added | Total rows | `testing` rows | `templates` rows | Spine |
|---|---|---|---|---|---|
| baseline | — | 191 | 0 | 4 | 6 |
| A (this task) | 12 | 203 | 11 | 5 | 7 |
| B | 4 | 207 | 15 | 5 | 7 |
| C | 11 | 218 | 26 | 5 | 7 |
| D | 6 | 224 | 32 | 5 | 7 |
| E | 5 | 229 | 37 | 5 | 7 |
| F | 2 | 231 | 39 | 5 | 7 |

- [ ] **Step 1: Append the inventory rows**

`governance/standards/_catalog-inventory.yml` is sorted alphabetically by `id`. Insert `TPL-005`
immediately after `TPL-004`, then append the eleven `TST-*` rows at the end of the file — `TPL`
sorts before `TST`, so the file stays ordered.

Use exactly this row shape, matching the existing rows:

```yaml
- id: TST-001
  title: Test Strategy Standard
  category: testing
  status: Approved
  owner: qe-lead
  path: knowledge-base/testing/strategy/test-strategy-standard.md
  tiers: []
  spine_or_radii: spine
  compliance_refs:
    ring0:
      - ISTQB Foundation — test levels and test types
      - NIST SP 800-53 CA-2
    ring1:
      - Basel BCBS 230 Principle 9
      - Basel BCBS 239 Principle 3
    ring2:
      - SBV Circular 09/2020 §IV.3
  last_reviewed: '2026-08-12'
  notes: Wave 15A — testing corpus foundation
  target_wave: 4
```

And `TPL-005`:

```yaml
- id: TPL-005
  title: Test Archetype Doc Template
  category: templates
  status: Approved
  owner: qe-lead
  path: knowledge-base/templates/test-archetype-template.md
  tiers: []
  spine_or_radii: radii
  compliance_refs:
    ring0: []
    ring1: []
    ring2: []
  last_reviewed: '2026-08-12'
  notes: Wave 15A — testing corpus foundation
  target_wave: 4
```

Row values for the remaining ten:

| id | title | path (under `knowledge-base/testing/`) | owner | tiers | spine_or_radii |
|---|---|---|---|---|---|
| TST-002 | Performance Test Standard | `strategy/performance-test-standard.md` | qe-lead | T0, T1, T2, T3 | radii |
| TST-003 | Workload Modelling | `strategy/workload-modelling.md` | qe-lead | T0, T1, T2 | radii |
| TST-004 | Test Data Management | `strategy/test-data-management.md` | qe-lead | T0, T1, T2, T3 | radii |
| TST-005 | Test Environments and Quality Gates | `strategy/environments-quality-gates.md` | qe-lead | T0, T1, T2, T3 | radii |
| TST-006 | Resilience Test Standard | `strategy/resilience-test-standard.md` | qe-lead | T0, T1 | radii |
| TST-007 | Contract and Integration Test Standard | `strategy/contract-integration-test-standard.md` | qe-lead | T0, T1, T2 | radii |
| TST-008 | Security Test Standard | `strategy/security-test-standard.md` | qe-lead | T0, T1 | radii |
| TST-009 | Data Quality Test Standard | `strategy/data-quality-test-standard.md` | qe-lead | T0, T1, T2 | radii |
| TST-010 | Test Tool Selection Matrix | `tooling/tool-selection-matrix.md` | qe-lead | T0, T1, T2, T3 | radii |
| TST-015 | Testing Coverage Matrix | `coverage/coverage-matrix.md` | qe-lead | (empty) | radii |

`compliance_refs` per row: `ring0`/`ring1`/`ring2` must match the Compliance Mapping table
actually written in that document. `TST-015` carries three empty lists — it is a generated report,
not a control. Populate the rest from each document's §Compliance Mapping.

- [ ] **Step 2: Append the catalog table rows**

In `governance/standards/enterprise-architecture-catalog.md`, the main table starts at the header
`| ID | Title | Category | Status | Spine | Owner | Path | Tiers | Compliance | Last Reviewed | Wave | Notes |`.
Insert `TPL-005` after the `TPL-004` row and append the eleven `TST-*` rows after it, matching
this exact column order and formatting:

```text
| TPL-005 | Test Archetype Doc Template | templates | Approved | radii | @qe-lead | `knowledge-base/templates/test-archetype-template.md` | — | — | 2026-08-12 | 4 | Wave 15A — testing corpus foundation |
| TST-001 | Test Strategy Standard | testing | Approved | spine | @qe-lead | `knowledge-base/testing/strategy/test-strategy-standard.md` | — | ISTQB test levels; BCBS 230 P9; BCBS 239 P3; SBV Circ. 09/2020 §IV.3 | 2026-08-12 | 4 | Wave 15A — testing corpus foundation |
```

Use `—` for empty Tiers and Compliance cells, as the existing `TPL-*` rows do.

- [ ] **Step 3: Update the coverage sentence**

Line 5 of `enterprise-architecture-catalog.md` currently reads:

```text
Coverage: 191 Approved catalog rows across 16 categories — 6 spine docs and 185 radii docs after Wave 14 source-of-truth reconciliation.
```

Replace with:

```text
Coverage: 203 Approved catalog rows across 17 categories — 7 spine docs and 196 radii docs after Wave 15A testing corpus foundation.
```

- [ ] **Step 4: Update the §5 category summary table**

Add a `testing` row in alphabetical position (after `templates`), change `templates` from 4 to 5,
and update the `**Total**` row:

```text
| templates | 5 | 0 | 0 | 5 | 100% | Required DAB, pattern, stub, reference, and test archetype templates |
| testing | 11 | 0 | 0 | 11 | 100% | Test strategy, performance profiles, tooling, and coverage for the QE team |
| **Total** | **203** | **0** | **0** | **203** | **100%** | |
```

- [ ] **Step 5: Add TST-001 to the §2.2 spine list and add a §3 taxonomy subsection**

In §2.2, the numbered spine list currently has 6 entries. Append:

```text
7. [TST-001 Test Strategy Standard](../../knowledge-base/testing/strategy/test-strategy-standard.md)
```

In §3, add a subsection following the existing category subsections:

```text
### 3.N Testing (`knowledge-base/testing/`)

Test strategy for the QE team, organised by verification method rather than by domain. A
strategy layer fixes the six disciplines, the eight performance profiles, and the
`test_acceptance_criteria` contract; 24 archetypes group catalog rows by shared verification
method; a generated coverage matrix proves every catalog row is addressed.

**Inclusion**: guidance on how to verify a catalog row. Excludes the runnable harness, which
lives in the QE team's own repository.
```

Number the subsection to follow the existing sequence — read the current §3 headings and use the
next integer rather than guessing.

- [ ] **Step 6: Update mkdocs nav**

In `mkdocs.yml`, inside the `Architecture Knowledge Base` section, add after the
`Design Patterns` entry:

```yaml
    - Testing (QE):
      - Overview: knowledge-base/testing/README.md
      - Test Strategy Standard: knowledge-base/testing/strategy/test-strategy-standard.md
      - Performance Test Standard: knowledge-base/testing/strategy/performance-test-standard.md
      - Workload Modelling: knowledge-base/testing/strategy/workload-modelling.md
      - Test Data Management: knowledge-base/testing/strategy/test-data-management.md
      - Environments and Quality Gates: knowledge-base/testing/strategy/environments-quality-gates.md
      - Tool Selection: knowledge-base/testing/tooling/tool-selection-matrix.md
      - Coverage Matrix: knowledge-base/testing/coverage/coverage-matrix.md
      - Archetypes: knowledge-base/testing/archetypes/
```

Match the surrounding indentation exactly — the existing entries under
`Architecture Knowledge Base` use 4 spaces for the section key and 6 for its children.

- [ ] **Step 7: Add CODEOWNERS rules**

Append to `.gitlab/CODEOWNERS`, following the file's existing section-comment style:

```text
# --- Testing knowledge base (IT Quality Engineering) ---
knowledge-base/testing/                                    @qe-lead
knowledge-base/templates/test-archetype-template.md        @qe-lead
knowledge-base/testing/strategy/performance-test-standard.md  @qe-lead @sre-lead
knowledge-base/testing/strategy/workload-modelling.md         @qe-lead @sre-lead
knowledge-base/testing/tooling/                               @qe-lead @sre-lead
knowledge-base/testing/strategy/security-test-standard.md     @qe-lead @infosec-architect
scripts/validate-testing-coverage.py                       @qe-lead
scripts/render-testing-coverage.py                         @qe-lead
```

Read the file first and match its existing alignment convention rather than the spacing above.

- [ ] **Step 8: Run every gate**

```bash
python3 scripts/audit-catalog-consistency.py --check-doc-status
echo "audit exit=$?"
python3 scripts/validate-internal-links.py
echo "links exit=$?"
python3 scripts/check-compliance-rows.py
echo "compliance exit=$?"
python3 scripts/validate-testing-coverage.py --quiet
echo "coverage exit=$? (expected 1 — 188 rows still unpopulated)"
python3 -c "import yaml,sys; yaml.safe_load(open('governance/standards/_catalog-inventory.yml')); yaml.safe_load(open('mkdocs.yml')); print('yaml ok')"
markdownlint knowledge-base/testing/ knowledge-base/templates/test-archetype-template.md
```

Expected: `audit`, `links`, `compliance`, and the YAML parse all exit `0`. The coverage gate
exits `1` — that is correct until Wave G and must not be "fixed" by weakening the check.

`mkdocs.yml` is parsed with `yaml.safe_load` here as a syntax check only. Do not run
`mkdocs build`; it needs the full plugin set and is the CI job's responsibility.

- [ ] **Step 9: Verify the counts you just wrote**

```bash
grep -c '^| [A-Z]\+-[0-9]\+ |' governance/standards/enterprise-architecture-catalog.md
grep -c '^- id: ' governance/standards/_catalog-inventory.yml
grep -n 'Coverage: 203' governance/standards/enterprise-architecture-catalog.md
grep -c '^- id: TST-' governance/standards/_catalog-inventory.yml
```

Expected: `203`, `203`, one match, `11`.

- [ ] **Step 10: Commit**

```bash
git add governance/standards/_catalog-inventory.yml \
        governance/standards/enterprise-architecture-catalog.md \
        mkdocs.yml .gitlab/CODEOWNERS
git commit -m "chore(catalog): register Wave A testing rows

Adds the testing category (17th) with TST-001..TST-010 and TST-015, plus
TPL-005. TST-001 becomes the 7th spine doc. Catalog 191 -> 203 rows.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Wave B — Tooling Guides

Wave B lands before the archetypes so archetype authors reference settled JMeter conventions
instead of reinventing JMX idioms 24 times.

Every guide shares this section skeleton:

1. Header, then `## Problem Statement` (4–5 bullets specific to this tool)
2. `## When to Use This Tool` — cross-link [TST-010](./tool-selection-matrix.md); do not restate
   the decision tree
3. `## Version and Installation` — pinned version, install method, plugin or extension set
4. `## Project Layout` — directory structure for a QE harness repository
5. `## Worked Example 1 — Synchronous API under load`
6. `## Worked Example 2 — Asynchronous / messaging scenario`
7. `## Worked Example 3` — tool-specific, named per guide below
8. `## Parameterisation and Correlation` — how a single script serves all eight profiles
9. `## Assertions and Thresholds`
10. `## Distributed Execution`
11. `## Result Output and Baselining` — cross-link [TST-005](../strategy/environments-quality-gates.md)
12. `## CI Invocation` — the command only; no pipeline YAML, per the Global Constraints
13. `## Common Failure Modes` — 5+ tool-specific traps
14. `## Compliance Mapping`
15. `## Related`
16. At least one Mermaid diagram

All examples use synthetic data and `${...}` / environment-variable parameterisation. No
hostname, credential, or dataset in any example may resemble a real system.

## Task 13: TST-011 JMeter Guide

The primary tool. This is the deepest guide and the one every archetype's §5 depends on.

**Files:**
- Create: `knowledge-base/testing/tooling/jmeter.md`

**Interfaces:**
- Consumes: profile keys (`TST-002`), workload models (`TST-003`), tool position (`TST-010`).
- Produces: the JMX property-naming convention `-Jusers`, `-Jrampup`, `-Jduration`, `-Jtargetrps`,
  `-Jprofile`, and the standard `jmeter -n -t … -l results.jtl -e -o report/` invocation. Every
  archetype §5 uses these exact property names so one plan can serve all eight profiles.

- [ ] **Step 1: Create TST-011**

Header: `Catalog ID: TST-011 | Radii`, `Owner: @qe-lead`, `Tier Applicability: T0, T1, T2, T3`.

Required substance beyond the shared skeleton:

- **Version**: pin a 5.6.x line; state that the version is recorded in run evidence because
  results are not comparable across major versions.
- **Plugin set**: Custom Thread Groups (Concurrency Thread Group and Arrivals Thread Group — the
  route to an **open** workload model), Throughput Shaping Timer, PerfMon listener, and the Kafka
  and JDBC samplers. State explicitly which plugin provides the open model, because the default
  Thread Group is closed and that is the single most consequential JMeter default.
- **Worked Example 3**: JDBC and JMS sampler configuration — the capability that justifies
  JMeter's primary position.
- **Parameterisation**: the `${__P(name,default)}` idiom, a properties file per profile, and a
  table mapping each of the eight profiles to its property values. Show that `stress` and `spike`
  select the Concurrency Thread Group while `load` and `soak` may use the standard Thread Group.
- **Correlation**: Regular Expression Extractor, JSON Extractor, and CSV Data Set Config for
  synthetic test data; the rule that a CSV must be marked as synthetic in its header comment.
- **Assertions**: Response Assertion, JSON Assertion, Duration Assertion, and the caution that a
  Duration Assertion asserts per-request latency and is not a substitute for a percentile check
  computed over the run.
- **Distributed execution**: master/worker topology, RMI ports, the requirement that workers are
  identically sized, and the rule that worker count is recorded in evidence.
- **Result output**: `.jtl` format choice, the HTML dashboard, and why the GUI must never be used
  for a measured run.
- **Common failure modes**: GUI mode used for measurement; default closed Thread Group used for a
  breakpoint test; listeners left enabled and skewing results; DNS cached for the whole run so
  load-balancer rotation is not exercised; heap too small so the generator garbage-collects
  mid-run; assertion added inside the timed transaction, inflating measured latency.

- [ ] **Step 2: Verify**

```bash
markdownlint knowledge-base/testing/tooling/jmeter.md
bash scripts/mermaid-lint-doc.sh knowledge-base/testing/tooling/jmeter.md
grep -c '__P(' knowledge-base/testing/tooling/jmeter.md
grep -c 'Concurrency Thread Group' knowledge-base/testing/tooling/jmeter.md
```

Expected: lint and Mermaid exit `0`; `__P(` appears at least 3 times; the Concurrency Thread Group
is named at least twice (plugin list and the open-model discussion).

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/testing/tooling/jmeter.md
git commit -m "docs(testing): add TST-011 JMeter guide

Primary tool guide: pinned version, plugin set including the Concurrency
Thread Group for open-model runs, three worked examples, the -J property
convention shared by every archetype, and six common failure modes.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 14: TST-012 Gatling + Karate Guide

**Files:**
- Create: `knowledge-base/testing/tooling/gatling-karate.md`

**Interfaces:**
- Consumes: the shared guide skeleton; `TST-007` contract vocabulary.
- Produces: the `karate-gatling` feature-reuse convention that `TST-030` (contract archetype)
  references.

- [ ] **Step 1: Create TST-012**

Header: `Catalog ID: TST-012 | Radii`, `Owner: @qe-lead`, `Tier Applicability: T0, T1, T2`.

Required substance beyond the shared skeleton:

- **The core value proposition, stated first**: a Karate `.feature` file written for functional
  API testing is reused unchanged as a Gatling performance scenario via `karate-gatling`. One
  artifact serves both the functional and performance disciplines. Show the same feature file used
  both ways.
- **Version**: pin the Gatling and Karate lines together, since `karate-gatling` couples them.
- **Open model by default**: `injectOpen`, `constantUsersPerSec`, `rampUsersPerSec`,
  `stressPeakUsers`. Contrast with JMeter's closed default and cross-link
  [TST-003](../strategy/workload-modelling.md).
- **Worked Example 3**: reusing an existing Karate contract suite as a `mixed`-profile scenario,
  with the per-journey assertion requirement from `TST-002`.
- **Assertions**: Gatling `assertions(global..., details(...))`, and the important detail that
  per-request-group assertions are what satisfy the per-journey pass criterion for `mixed`.
- **Resource efficiency**: why the actor model yields low cost per virtual user, making this the
  right choice for high-concurrency scenarios where JMeter would need distributed workers.
- **Common failure modes**: Scala compilation errors surfacing only at run time; `injectClosed`
  used by mistake, silently reverting to a closed model; Karate `karate.callSingle` misuse causing
  per-user setup cost that inflates measured latency; the HTML report's global percentile mistaken
  for a per-journey percentile; feature files sharing mutable state across virtual users.

- [ ] **Step 2: Verify**

```bash
markdownlint knowledge-base/testing/tooling/gatling-karate.md
bash scripts/mermaid-lint-doc.sh knowledge-base/testing/tooling/gatling-karate.md
grep -cE 'injectOpen|karate-gatling' knowledge-base/testing/tooling/gatling-karate.md
```

Expected: lint and Mermaid exit `0`; the grep returns at least `3`.

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/testing/tooling/gatling-karate.md
git commit -m "docs(testing): add TST-012 Gatling and Karate guide

Documents karate-gatling feature reuse so one artifact serves both the
functional and performance disciplines, plus open-model injection profiles
and per-request-group assertions for the mixed profile.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 15: TST-013 k6 Guide

**Files:**
- Create: `knowledge-base/testing/tooling/k6.md`

**Interfaces:**
- Consumes: the shared guide skeleton; the `baseline` profile from `TST-002`; gate placement from
  `TST-005`.
- Produces: the thresholds-as-code convention used by the `baseline` CI gate.

- [ ] **Step 1: Create TST-013**

Header: `Catalog ID: TST-013 | Radii`, `Owner: @qe-lead`, `Tier Applicability: T0, T1, T2, T3`.

Required substance beyond the shared skeleton:

- **Role, stated first**: k6 is the CI merge-gate tool because `thresholds` are declarative and
  the process exit code reflects them, so a pipeline needs no result-parsing step.
- **Version**: pin a version; note that `xk6` extensions require a custom binary build, and that
  the built binary's extension set must be recorded in evidence.
- **`options` and `scenarios`**: `constant-arrival-rate` and `ramping-arrival-rate` (open),
  `constant-vus` and `ramping-vus` (closed). Map each of the eight profiles to a scenario type.
- **Thresholds**: `http_req_duration: ['p(95)<...']`, `http_req_failed`, and custom `Trend` and
  `Rate` metrics. Show a per-journey threshold using tags, which is how the `mixed` per-journey
  criterion is satisfied.
- **Worked Example 3**: the `baseline` profile wired as a merge-gate — script, thresholds, exit
  code semantics, and the CLI invocation. No pipeline YAML, per the Global Constraints.
- **xk6 extensions**: Kafka, SQL, and browser; when each is warranted and the maintenance cost of
  a custom binary.
- **Common failure modes**: thresholds defined but `--no-thresholds` left in the invocation;
  default summary percentiles differing from the required ones; a `ramping-vus` scenario used for
  a breakpoint test, reintroducing a closed model; `discardResponseBodies` masking a
  response-validation failure; per-iteration `sleep()` mistaken for think time in an arrival-rate
  scenario, where it does not throttle arrivals.

- [ ] **Step 2: Verify**

```bash
markdownlint knowledge-base/testing/tooling/k6.md
bash scripts/mermaid-lint-doc.sh knowledge-base/testing/tooling/k6.md
grep -cE 'thresholds|constant-arrival-rate' knowledge-base/testing/tooling/k6.md
```

Expected: lint and Mermaid exit `0`; the grep returns at least `4`.

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/testing/tooling/k6.md
git commit -m "docs(testing): add TST-013 k6 guide

Positions k6 as the CI merge-gate tool via thresholds-as-code, maps the
eight profiles to scenario executors, and shows tag-based per-journey
thresholds for the mixed profile.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 16: TST-014 Locust Guide

**Files:**
- Create: `knowledge-base/testing/tooling/locust.md`

**Interfaces:**
- Consumes: the shared guide skeleton.
- Produces: the stateful-scenario convention referenced by `TST-024` (saga) and `TST-023`
  (limit contention), where bespoke sequencing logic is the deciding factor.

- [ ] **Step 1: Create TST-014**

Header: `Catalog ID: TST-014 | Radii`, `Owner: @qe-lead`, `Tier Applicability: T1, T2, T3`.

Required substance beyond the shared skeleton:

- **Role, stated first**: chosen when the scenario needs bespoke stateful logic or reuse of
  existing Python domain libraries — for example driving a saga through a specific interleaving,
  or reusing a Python decimal-arithmetic library to compute an expected value inline.
- **Version**: pin a version; note the `FastHttpUser` alternative to `HttpUser` and its
  significantly lower per-user cost.
- **`User`, `TaskSet`, and `SequentialTaskSet`**: how to express an ordered multi-step journey.
  This is Locust's actual advantage and should be the centrepiece.
- **Load shape control**: `LoadTestShape` for programmatic step-ramps and spikes; the caution that
  Locust's model is user-based and therefore closed unless the shape explicitly compensates, which
  makes it a poor fit for `stress`. State plainly that Locust is `fair` rather than `good` for
  breakpoint work.
- **Worked Example 3**: a `SequentialTaskSet` driving a multi-step saga with per-step assertions
  and a compensating-path branch.
- **Distributed execution**: master and worker processes, and the requirement that a custom
  `LoadTestShape` is evaluated on the master only.
- **Common failure modes**: `HttpUser` used at high concurrency where `FastHttpUser` was needed;
  shared mutable module-level state across users producing false contention; `wait_time` confused
  with pacing; a custom shape not accounting for worker count so actual load differs from
  intended; exceptions inside a task counted as failures without distinguishing an assertion
  failure from a script bug.

- [ ] **Step 2: Verify**

```bash
markdownlint knowledge-base/testing/tooling/locust.md
bash scripts/mermaid-lint-doc.sh knowledge-base/testing/tooling/locust.md
grep -cE 'SequentialTaskSet|FastHttpUser|LoadTestShape' knowledge-base/testing/tooling/locust.md
```

Expected: lint and Mermaid exit `0`; the grep returns at least `4`.

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/testing/tooling/locust.md
git commit -m "docs(testing): add TST-014 Locust guide

Positions Locust for bespoke stateful scenarios via SequentialTaskSet,
documents LoadTestShape and FastHttpUser, and states plainly why it is a
poor fit for breakpoint testing.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 17: Wave B Catalog Registration

**Files:**
- Modify: `governance/standards/_catalog-inventory.yml`
- Modify: `governance/standards/enterprise-architecture-catalog.md`
- Modify: `mkdocs.yml`

**Interfaces:**
- Consumes: the four guides from Tasks 13–16.
- Produces: catalog total 207, `testing` category count 15.

- [ ] **Step 1: Append the four inventory rows**

`TST-011` JMeter Guide (`tooling/jmeter.md`, tiers T0–T3), `TST-012` Gatling and Karate Guide
(`tooling/gatling-karate.md`, tiers T0–T2), `TST-013` k6 Guide (`tooling/k6.md`, tiers T0–T3),
`TST-014` Locust Guide (`tooling/locust.md`, tiers T1–T3). All `category: testing`,
`status: Approved`, `owner: qe-lead`, `spine_or_radii: radii`, `last_reviewed: '2026-08-12'`,
`notes: Wave 15B — tooling guides`, `target_wave: 4`. Populate `compliance_refs` from each
document's Compliance Mapping table.

- [ ] **Step 2: Append the four catalog table rows and update the counts**

Coverage sentence → `Coverage: 207 Approved catalog rows across 17 categories — 7 spine docs and 200 radii docs after Wave 15B tooling guides.`
Category table: `testing` 11 → 15; `**Total**` 203 → 207.

- [ ] **Step 3: Add the three new tooling entries to the mkdocs nav**

Under `Testing (QE)`, after `Tool Selection`, add `JMeter`, `Gatling + Karate`, `k6`, `Locust`
entries pointing at their documents.

- [ ] **Step 4: Run the gates**

```bash
python3 scripts/audit-catalog-consistency.py --check-doc-status; echo "audit exit=$?"
python3 scripts/validate-internal-links.py; echo "links exit=$?"
python3 scripts/check-compliance-rows.py; echo "compliance exit=$?"
grep -c '^| [A-Z]\+-[0-9]\+ |' governance/standards/enterprise-architecture-catalog.md
grep -c '^- id: ' governance/standards/_catalog-inventory.yml
```

Expected: the three gates exit `0`; both counts return `207`.

- [ ] **Step 5: Commit**

```bash
git add governance/standards/_catalog-inventory.yml \
        governance/standards/enterprise-architecture-catalog.md mkdocs.yml
git commit -m "chore(catalog): register Wave B testing tooling rows

TST-011..TST-014. Catalog 203 -> 207 rows.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Standard Archetype Steps

Tasks 18–44 each produce one archetype document. The mechanical steps are identical, so they are
defined once here. Each archetype task supplies only what differs: the covered catalog rows, the
failure taxonomy, the invariants, the profiles, the harness focus, the tool-fit ratings, and the
compliance anchors.

For an archetype task with `ID` = its Catalog ID and `FILE` = its path under
`knowledge-base/testing/archetypes/`:

- [ ] **Standard Step 1: Copy the template and fill it**

```bash
cp knowledge-base/templates/test-archetype-template.md "$FILE"
```

Then fill all 14 sections from the task's content specification. Delete every authoring note and
every `[PLACEHOLDER]`. Set the header to `Catalog ID: $ID | Radii`, `Owner: @qe-lead`,
`Status: Approved`, `Last Reviewed: 2026-08-12`, and the Tier Applicability the task states.

- [ ] **Standard Step 2: Verify no template residue survived**

```bash
grep -nE 'PLACEHOLDER|^> \*\*Authoring note|TST-0NN|YYYY-MM-DD' "$FILE"
```

Expected: no matches. Any hit means the template was not fully filled.

- [ ] **Standard Step 3: Verify the section skeleton matches TPL-005**

```bash
diff <(grep -oE '^#{2,3} [0-9]+\. [A-Za-z].*' knowledge-base/templates/test-archetype-template.md) \
     <(grep -oE '^#{2,3} [0-9]+\. [A-Za-z].*' "$FILE")
```

Expected: differences only where the task's specification says an overlay subsection is omitted.
Any other structural difference is drift — fix it rather than accepting it.

- [ ] **Standard Step 4: Verify no service threshold was hard-coded**

```bash
grep -nE '[0-9]+ *(ms|rps|RPS)\b' "$FILE"
```

Expected: no matches outside a `test_acceptance_criteria` example block. A latency or throughput
number must be a link to its `NFR-*` row.

- [ ] **Standard Step 5: Verify lint, Mermaid, and links**

```bash
markdownlint "$FILE"
bash scripts/mermaid-lint-doc.sh "$FILE"
python3 scripts/validate-internal-links.py 2>&1 | grep "$(basename "$FILE")" || echo "no link issues for this file"
```

Expected: lint and Mermaid exit `0`; no link issues naming this file.

- [ ] **Standard Step 6: Add the coverage rows for every catalog row this archetype covers**

Append a row to `knowledge-base/testing/coverage/_testing-coverage.yml` for each catalog ID in the
task's Applies To list, unless a row already exists — in which case append this archetype's ID to
that row's existing `archetypes` list. Take `title`, `path`, `tiers`, and `owner` verbatim from
`governance/standards/_catalog-inventory.yml`; check 7 compares `tiers` and check 5 compares
`path`.

```bash
python3 scripts/validate-testing-coverage.py --quiet
```

Expected: exit `1`, with the remaining failures being `check1` only (rows not yet populated). Any
`check2`…`check7` failure concerns a row you just wrote — fix it now.

- [ ] **Standard Step 7: Commit**

```bash
git add "$FILE" knowledge-base/testing/coverage/_testing-coverage.yml
git commit -m "docs(testing): add $ID <archetype name>

<one-line summary of the verification method>

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Wave C — Correctness, State, Messaging and Integration Archetypes

Eleven archetypes: Family A (TST-020…TST-025) and Family B (TST-026…TST-030).

## Task 18: TST-020 Idempotency and Replay Safety

**Files:** Create `knowledge-base/testing/archetypes/idempotency-replay.md`

**Interfaces:**
- Consumes: `TPL-005` skeleton; `TST-001` oracle names; `TST-002` profile keys; `TST-011` `-J`
  property convention.
- Produces: the replay-assertion method reused by TST-029 (redelivery) and TST-024 (compensation
  idempotency). Those tasks cross-link here rather than restating it.

**Tier Applicability:** T0, T1

**Applies To:** `BSP-002` Idempotent Payment Key
(`../../patterns/banking-solutions/idempotent-payment-key.md`); `EIP-024` Idempotent Receiver
(`../../patterns/eip/idempotent-receiver.md`); `INT-014` Webhook Delivery Reliability
(`../../patterns/integration/webhook-delivery-reliability.md`); `RES-003` Retry with Backoff
(`../../patterns/resilience/retry-with-backoff.md`).

`PRIN-006` Idempotency-by-default belongs in §12 Related Patterns, **not** §1 Applies To. It is a
principle and therefore carries `governs` in the coverage file — a principle constrains design
rather than being itself under test. Putting it in §1 would make its coverage row fail check 6.

**Oracle:** `invariant-assertion`

**Failure taxonomy:** duplicate posting when the client retries after a gateway timeout;
idempotency key collision across customers; key retention expiring before the client's retry
window closes; a non-deterministic response body on replay, so the client reads a success as a
failure; two concurrent in-flight duplicates both proceeding because there is no lock; idempotency
enforced at the API layer but not at the message consumer; replay after a schema change producing
a different result.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | N identical requests bearing the same key produce exactly one state change |
| I2 | A replay returns the same status and response body as the original |
| I3 | Different keys with identical payloads produce N distinct state changes |
| I4 | The same key with a *different* payload is rejected explicitly, never silently ignored |
| I5 | Under true concurrency exactly one duplicate wins; the other returns the stored response |
| I6 | Key retention window is at least the client's maximum retry window |
| I7 | Consumer-side deduplication survives broker redelivery |

**Profiles:** `baseline`, `load`, `stress`, `spike`, `soak`. `spike` is essential — a retry storm
is the realistic trigger. `soak` asserts the deduplication store does not grow without bound and
that TTL eviction actually runs. Workload model `open` for `stress` and `spike`.

**Harness focus (JMeter):** CSV Data Set Config supplying synthetic idempotency keys; a controller
issuing each key twice; a JSON Extractor capturing the first response and a Response Assertion
comparing the replay against it; a **Synchronizing Timer** to release threads simultaneously for
I5 — this is the only clean way to test true concurrency and is why JMeter is `BEST` here.

**Tool fit:** JMeter `BEST` (Synchronizing Timer gives true simultaneity); Gatling + Karate `good`;
k6 `good`; Locust `fair`.

**Overlays:** Resilience — inject `dependency-blackhole` mid-request to force a genuine client
retry, then assert I1. Data-quality — assert the ledger entry count is unchanged after replay.
Omit the contract and security overlays.

**Compliance:** Ring 0 — EIP §10.1 Idempotent Receiver. Ring 1 — BCBS 239 Principle 3 (duplicate
suppression preserves accuracy); ISO 20022 retry and duplicate semantics. Ring 2 — SBV Circular
09/2020 §IV.2, `⚠️`.

**Steps:** Apply the Standard Archetype Steps with `FILE=knowledge-base/testing/archetypes/idempotency-replay.md`
and `ID=TST-020`. `primary_tool: jmeter` for all five coverage rows.

---

## Task 19: TST-021 Ledger and Monetary Invariant

**Files:** Create `knowledge-base/testing/archetypes/ledger-monetary-invariant.md`

**Interfaces:**
- Consumes: `TST-020` replay-assertion method; `TST-009` reconciliation tolerance rule.
- Produces: the independent-recomputation method (recompute from source rather than re-reading the
  aggregate) reused by TST-039.

**Tier Applicability:** T0

**Applies To:** `BSP-001` Double-Entry Ledger
(`../../patterns/banking-solutions/double-entry-ledger.md`); `BSP-015` Position Keeping Engine
(`../../patterns/banking-solutions/position-keeping-engine.md`); `BSP-016` Settlement Engine
(`../../patterns/banking-solutions/settlement-engine.md`); `BSP-005` Reversal and Chargeback
(`../../patterns/banking-solutions/reversal-and-chargeback.md`); `REF-010` Ledger Posting Engine
(`../../reference-architectures/ledger-posting-engine.md`).

**Oracle:** `invariant-assertion`

**Failure taxonomy:** debits and credits failing to sum to zero after a partial failure; a rounding
remainder silently dropped; a reversal that is not the exact symmetric negation of its original;
double reversal permitted; position drift under concurrent posting; two currencies mixed within
one journal; a back-dated entry mutating a closed accounting period.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | Within every journal, sum(debits) equals sum(credits) |
| I2 | Across the ledger, the sum of all entries per currency is zero |
| I3 | A reversal is the exact negation of its original — same accounts, same currency, same magnitude |
| I4 | A reversal cannot itself be reversed more than once |
| I5 | Every account balance equals the independent sum of its entries |
| I6 | No entry is accepted into a closed accounting period |
| I7 | A rounding remainder is posted to a designated rounding account, never discarded |
| I8 | Position after N concurrent postings equals the sum of the N amounts |

**Profiles:** `baseline`, `load`, `soak`, `mixed`, `failover-under-load`. `failover-under-load` is
the decisive one: a failover mid-posting must neither create nor destroy money. Assert I1 and I2
*after* the failover, not only before.

**Harness focus (JMeter):** a JDBC PostProcessor recomputing `sum(debits) - sum(credits)` after the
run; a JSR223 assertion performing the independent recomputation with `BigDecimal` — state
explicitly that `double` must never appear in the money path, in the harness or the system.

**Tool fit:** JMeter `BEST` (the JDBC sampler enables independent recomputation from source);
Locust `good` (Python `decimal` for expected values); Gatling + Karate `fair`; k6 `fair` (needs
`xk6-sql`).

**Overlays:** Resilience — `instance-loss` on the posting service and `zone-loss` mid-transaction.
Data-quality — reconciliation with a tolerance of exactly zero, per TST-009. Omit contract and
security overlays.

**Compliance:** Ring 0 — double-entry bookkeeping as the canonical invariant. Ring 1 — BCBS 239
Principle 3 (accuracy and integrity) and Principle 4 (completeness); closed-period integrity under
IFRS. Ring 2 — SBV Circular 09/2020 accounting-integrity expectations, `⚠️`.

**Steps:** Apply the Standard Archetype Steps with `FILE=…/ledger-monetary-invariant.md` and
`ID=TST-021`. `primary_tool: jmeter` for all five coverage rows.

---

## Task 20: TST-022 Deterministic Calculation Engine

**Files:** Create `knowledge-base/testing/archetypes/deterministic-calculation-engine.md`

**Interfaces:**
- Consumes: `TST-001` `golden-dataset` oracle; `TST-004` seeding rule.
- Produces: the golden-dataset comparison method with explicit scale and rounding mode, reused by
  TST-038 (temporal recomputation).

**Tier Applicability:** T0, T1

**Applies To:** `BSP-018` Accrual Engine (`../../patterns/banking-solutions/accrual-engine.md`);
`BSP-007` Interest Calculation Engine
(`../../patterns/banking-solutions/interest-calculation-engine.md`); `BSP-008` Fee Engine
(`../../patterns/banking-solutions/fee-engine.md`); `BSP-009` Tax Calculation Engine
(`../../patterns/banking-solutions/tax-calculation-engine.md`); `BSP-006` Pricing Engine
(`../../patterns/banking-solutions/pricing-engine.md`); `BSP-020` Relationship Pricing Engine
(`../../patterns/banking-solutions/relationship-pricing-engine.md`); `BSP-014` FX Rate Engine
(`../../patterns/banking-solutions/fx-rate-engine.md`); `BSP-017` Product Factory
(`../../patterns/banking-solutions/product-factory.md`).

**Oracle:** `golden-dataset`

**Failure taxonomy:** rounding mode differing between engine and expectation; day-count convention
wrong at month and year boundaries; leap-year and 29 February mishandling; floating point used
instead of decimal in the money path; a tiered threshold off by one at its boundary; an
effective-dated rate change applied at the wrong instant; a timezone applied to a value date;
recalculation that is not idempotent, so re-running changes the result.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | The same input and effective date produce a bit-identical output |
| I2 | Output matches the golden dataset to the declared scale |
| I3 | Rounding uses the declared mode, verified at an exact `.5` boundary |
| I4 | A value exactly on a tier boundary resolves to the documented tier |
| I5 | Recalculation is idempotent |
| I6 | No floating-point arithmetic appears in the money path |

**Boundaries to cover explicitly:** 28-, 29-, 30-, and 31-day months; leap year; year end; each
tier edge and one unit either side; zero amount; maximum permitted amount; negative amount where
permitted.

**Profiles:** `baseline`, `load`. For engines that run inside the end-of-day window, batch
throughput is asserted by TST-032 rather than here — cross-link it rather than duplicating.

**Harness focus:** a synthetic golden dataset as a CSV with an expected-result column; a JSR223
assertion comparing with `BigDecimal` at an explicit scale. In Locust, the same comparison uses
Python `decimal` inline, which is why Locust wins here.

**Tool fit:** Locust `BEST` (Python `decimal` reproduces the expected value inline, which is
precisely this archetype's oracle); JMeter `good`; Gatling + Karate `fair`; k6 `fair` (no decimal
type — a real disqualifier for money arithmetic).

**Note — this is the first archetype whose primary tool is not JMeter.** Record
`primary_tool: locust` for all eight coverage rows, and state the reason in the document so a
reviewer does not read it as an inconsistency.

**Overlays:** Data-quality — golden-dataset provenance and sign-off. Omit resilience, contract, and
security overlays.

**Compliance:** Ring 0 — decimal arithmetic over IEEE 754 for monetary values. Ring 1 — BCBS 239
Principle 3; IFRS 9 effective-interest determinism. Ring 2 — SBV interest and fee disclosure
accuracy expectations, `⚠️`.

**Steps:** Apply the Standard Archetype Steps with `FILE=…/deterministic-calculation-engine.md` and
`ID=TST-022`.

---

## Task 21: TST-023 Concurrent Limit and Counter Contention

**Files:** Create `knowledge-base/testing/archetypes/concurrent-limit-contention.md`

**Interfaces:**
- Consumes: `TST-003` open-model rule; `TST-020` Synchronizing Timer technique.
- Produces: the true-simultaneity load pattern reused by TST-031.

**Tier Applicability:** T0, T1

**Applies To:** `BSP-011` Credit Limit Engine
(`../../patterns/banking-solutions/credit-limit-engine.md`); `BSP-012` Transaction Limit Engine
(`../../patterns/banking-solutions/transaction-limit-engine.md`); `BSP-013` Collateral Management
Engine (`../../patterns/banking-solutions/collateral-management-engine.md`).

**Oracle:** `invariant-assertion`

**Failure taxonomy:** oversubscription under concurrency so the limit is exceeded; a lost update on
read-modify-write; a limit released twice on rollback; a reservation leak leaving capacity
permanently unusable; a counter window resetting on the wrong timezone boundary; a pessimistic lock
producing a latency cliff at contention; deadlock between two limit checks taken in different
orders.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | Given N concurrent requests against a limit of L, exactly `min(N, L)` succeed |
| I2 | Observed utilisation never exceeds the limit at any instant |
| I3 | A rolled-back reservation returns exactly its own amount |
| I4 | A double release is rejected |
| I5 | The counter window boundary uses the declared timezone |
| I6 | No reservation outlives its declared TTL |

**Profiles:** `baseline`, `load`, `stress`, `spike`. `stress` is essential — contention pathologies
only appear near saturation. **Workload model `open` is mandatory**: a closed model self-throttles
as latency rises and will hide oversubscription entirely. Say so in the document.

**Harness focus (JMeter):** a Synchronizing Timer releasing N threads simultaneously against a
single synthetic limit; a JDBC PostProcessor asserting final utilisation ≤ limit; a Counter element
cycling synthetic account identifiers; the Concurrency Thread Group for the open model.

**Tool fit:** JMeter `BEST` (Synchronizing Timer is the cleanest true-simultaneity primitive of the
four); Gatling + Karate `good`; k6 `good`; Locust `fair`.

**Overlays:** Resilience — `instance-loss` while reservations are in flight; assert I3 and I6 after
recovery. Omit contract, security, and data-quality overlays.

**Compliance:** Ring 0 — ACID isolation levels and optimistic versus pessimistic concurrency
control. Ring 1 — BCBS 239 Principle 3. Ring 2 — SBV Circular 09/2020 transaction-limit
obligations, `⚠️`.

**Steps:** Apply the Standard Archetype Steps with `FILE=…/concurrent-limit-contention.md` and
`ID=TST-023`. `primary_tool: jmeter` for all three coverage rows.

---

## Task 22: TST-024 Saga and Compensation Correctness

**Files:** Create `knowledge-base/testing/archetypes/saga-compensation.md`

**Interfaces:**
- Consumes: `TST-020` idempotency assertions (compensations must be idempotent); `TST-006` fault
  classes.
- Produces: the bounded-wait terminal-state assertion reused by TST-037.

**Tier Applicability:** T0, T1

**Applies To:** `INT-001` Saga Orchestration
(`../../patterns/integration/saga-orchestration.md`); `INT-016` Distributed Saga Choreography
(`../../patterns/integration/distributed-saga-choreography.md`); `EIP-017` Process Manager
(`../../patterns/eip/process-manager.md`); `EIP-016` Routing Slip
(`../../patterns/eip/routing-slip.md`).

**Oracle:** `invariant-assertion`

**Failure taxonomy:** compensation not executed for a step that partially succeeded; a compensation
that itself fails with no escalation path; a non-idempotent compensation applied twice; a saga
stuck with no timeout; out-of-order compensation corrupting state; an orchestrator crash between
step commit and event publication; a compensation running for a step that never committed.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | For every committed step, either the saga completes or every committed step is compensated |
| I2 | Compensations execute in reverse order of their forward steps |
| I3 | Each compensation is idempotent |
| I4 | Every saga reaches a terminal state within its declared timeout |
| I5 | No compensation runs for a step that did not commit |
| I6 | Orchestrator restart resumes from persisted state, never from the beginning |

**Profiles:** `baseline`, `load`, `soak`, `failover-under-load`. `failover-under-load` is the
decisive profile: kill the orchestrator mid-saga under traffic and assert I1 and I6.

**Harness focus:** a Transaction Controller per saga; bounded-wait polling for the terminal state —
state that unbounded polling until success is not a test, per TST-009; fault injection between
steps 2 and 3.

**Tool fit:** Locust `BEST` (`SequentialTaskSet` expresses an ordered multi-step journey and a
compensating branch directly, which JMX and Scala both make awkward); JMeter `good`; Gatling +
Karate `good`; k6 `fair`.

Record `primary_tool: locust` for all four coverage rows.

**Overlays:** Resilience — `instance-loss` on the orchestrator mid-saga; `dependency-blackhole` on
step 2 of 3; `partial-partition` between orchestrator and participant. Contract — the saga's event
contracts, cross-linking TST-030. Omit security and data-quality overlays.

**Compliance:** Ring 0 — Saga pattern (Garcia-Molina and Salem); Microsoft Cloud Design Patterns —
Saga. Ring 1 — BCBS 239 Principle 3; ISO 20022 multi-leg flows (`pacs.008` → `pacs.002` →
`pacs.004` reversal). Ring 2 — SBV Circular 09/2020 §IV.2, `⚠️`.

**Steps:** Apply the Standard Archetype Steps with `FILE=…/saga-compensation.md` and `ID=TST-024`.

---

## Task 23: TST-025 Decision Table and Screening Accuracy

**Files:** Create `knowledge-base/testing/archetypes/decision-screening-accuracy.md`

**Interfaces:**
- Consumes: `TST-001` `confusion-matrix` oracle; `TST-008` authorisation-matrix method (the corpus
  for `SEC-010`).
- Produces: the labelled-corpus and confusion-matrix method, and the list-cardinality latency curve
  technique.

**Tier Applicability:** T0, T1

**Applies To:** `BSP-010` Rule / Decisioning Engine
(`../../patterns/banking-solutions/rule-decisioning-engine.md`); `BSP-003` Sanction Screening
Pipeline (`../../patterns/banking-solutions/sanction-screening-pipeline.md`); `BSP-019` Collections
Engine (`../../patterns/banking-solutions/collections-engine.md`); `SEC-009` Fraud Signal
Collection (`../../patterns/security/fraud-signal-collection.md`); `SEC-010` Attribute-Based Access
Control (`../../patterns/security/attribute-based-access-control.md`).

**Oracle:** `confusion-matrix`

**Failure taxonomy:** overlapping rules producing a non-deterministic decision; no default rule, so
an input falls through silently; a fuzzy-match threshold tuned so false negatives pass unnoticed; a
list or policy update not taking effect until restart; a decision with no explanation, so a
regulator query cannot be answered; screening latency growing with list cardinality; an unreachable
rule that can never fire.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | Every input matches exactly one decision path — no overlap, no gap |
| I2 | Precision, recall, and false-positive rate against the labelled corpus meet the declared thresholds |
| I3 | Every decision emits an explanation naming the rule that fired |
| I4 | A list or policy update takes effect within its declared propagation window, without restart |
| I5 | No rule is unreachable |
| I6 | The same input yields the same decision until the ruleset version changes |

State explicitly that the precision and recall *targets* are business-owned, declared per engine,
and cited — this document defines the method, not the numbers.

**Profiles:** `baseline`, `load`, `stress`, `soak`. The specific concern for `stress` is latency
versus **list cardinality**, not just request rate: run the same profile against increasing list
sizes and plot the curve. A screening engine that meets its budget against a small list and misses
it in production is the failure this catches.

**Harness focus:** a labelled synthetic corpus as a CSV with an expected-decision column; an
assertion accumulating true/false positives and negatives across the run and evaluating the
confusion matrix at the end; one run per list size for the cardinality curve.

**Tool fit:** Locust `BEST` (accumulating and evaluating a confusion matrix across iterations is
natural in Python and awkward elsewhere); JMeter `good`; Gatling + Karate `fair`; k6 `fair`.

Record `primary_tool: locust` for all five coverage rows.

**Overlays:** Security — for `SEC-010`, the authorisation matrix from TST-008 *is* the labelled
corpus; cross-link it. Data-quality — labelled-corpus provenance and its refresh cadence. Omit
resilience and contract overlays.

**Compliance:** Ring 0 — classifier evaluation via confusion matrix; OWASP ASVS access-control
verification for the `SEC-010` case. Ring 1 — FATF screening expectations; BCBS 239 Principle 3.
Ring 2 — SBV anti-money-laundering screening obligations, `⚠️`.

**Steps:** Apply the Standard Archetype Steps with `FILE=…/decision-screening-accuracy.md` and
`ID=TST-025`.

---

## Task 24: TST-026 Message Transformation and Routing Correctness

**Files:** Create `knowledge-base/testing/archetypes/message-transformation-routing.md`

**Interfaces:**
- Consumes: `TST-011` JMS and Kafka sampler configuration; `TST-007` contract vocabulary.
- Produces: the round-trip precision and encoding assertions reused by TST-030.

**Tier Applicability:** T0, T1

**Applies To:** twelve rows — `EIP-004` Message Router (`../../patterns/eip/message-router.md`);
`EIP-005` Content-Based Router (`../../patterns/eip/content-based-router.md`); `EIP-006` Message
Translator (`../../patterns/eip/message-translator.md`); `EIP-007` Content Enricher
(`../../patterns/eip/content-enricher.md`); `EIP-008` Content Filter
(`../../patterns/eip/content-filter.md`); `EIP-010` Normalizer (`../../patterns/eip/normalizer.md`);
`EIP-014` Composed Message Processor
(`../../patterns/eip/composed-message-processor.md`); `EIP-012` Splitter
(`../../patterns/eip/splitter.md`); `EIP-019` Smart Proxy (`../../patterns/eip/smart-proxy.md`);
`INT-009` Content-Based Router (`../../patterns/integration/content-based-router.md`); `INT-005`
Anti-Corruption Layer (`../../patterns/integration/anti-corruption-layer.md`); `INT-012` Error Code
Mapping (`../../patterns/integration/error-code-mapping.md`); `INT-007` Sidecar / Ambassador
(`../../patterns/integration/sidecar-ambassador.md`); `INT-008` Backend-for-Frontend Routing
(`../../patterns/integration/backend-for-frontend-routing.md`).

That is **fourteen** rows. `INT-007` and `INT-008` are proxy and routing-topology patterns whose
verification method is routing correctness, which is why they sit here rather than in a separate
edge-routing archetype.

**Oracle:** `contract-schema`

**Failure taxonomy:** silent field truncation on translation; an unmapped enum value defaulting
rather than erroring; a router falling through to a default channel so a message class is lost; an
enricher failing open and emitting an incomplete message; a filter removing a field a downstream
consumer requires; a splitter losing the final element; character-encoding corruption of Vietnamese
diacritics; decimal precision lost translating a monetary amount.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | Every source-contract field maps to a defined target field or an explicit, documented discard |
| I2 | In a passing run, no message reaches a default or fallback route |
| I3 | Unmapped enum values are rejected, never defaulted |
| I4 | Splitter output count equals the declared element count |
| I5 | Round-trip translation preserves amount precision and currency exactly |
| I6 | Non-ASCII text, including Vietnamese diacritics, survives byte-identically |
| I7 | An enricher failure yields an error, never a partial message |

**Profiles:** `baseline`, `load`, `soak`. `soak` catches unbounded growth in a transformation or
XSLT cache.

**Harness focus (JMeter):** JMS and Kafka samplers publishing synthetic canonical messages;
JSON Assertion and XPath2 Assertion for schema conformance; a JSR223 assertion comparing
round-tripped decimals with `BigDecimal`; a UTF-8 fixture containing Vietnamese diacritics.

**Tool fit:** JMeter `BEST` (native JMS and Kafka samplers); Gatling + Karate `good` (Karate's
payload matching is unusually strong for transformation assertions); k6 `fair`; Locust `fair`.

**Overlays:** Contract — cross-link TST-030 for schema compatibility; this archetype asserts
*conformance*, TST-030 asserts *compatibility across versions*. Omit resilience, security, and
data-quality overlays.

**Compliance:** Ring 0 — EIP §4 (routing) and §8 (transformation). Ring 1 — ISO 20022 element
conformance; BCBS 239 Principle 3. Ring 2 — SBV Circular 09/2020, `⚠️`; note Vietnamese-language
field handling as a practical Ring 2 requirement.

**Steps:** Apply the Standard Archetype Steps with `FILE=…/message-transformation-routing.md` and
`ID=TST-026`. `primary_tool: jmeter` for all twelve coverage rows.

---

## Task 25: TST-027 Ordering, Sequencing and Resequencing

**Files:** Create `knowledge-base/testing/archetypes/ordering-resequencing.md`

**Interfaces:**
- Consumes: `TST-011` Kafka sampler; `TST-006` fault classes.
- Produces: the shuffled-injection and monotonicity-assertion method reused by TST-037.

**Tier Applicability:** T0, T1

**Applies To:** `EIP-013` Resequencer (`../../patterns/eip/resequencer.md`); `INT-017` Message
Sequencer (`../../patterns/integration/message-sequencer.md`); `EIP-003` Publish-Subscribe Channel
(`../../patterns/eip/publish-subscribe-channel.md`) — for its ordering guarantees.

**Oracle:** `invariant-assertion`

**Failure taxonomy:** out-of-order delivery accepted so an earlier state overwrites a later one; a
resequencer buffer overflowing and dropping silently; missing gap detection so a permanently absent
sequence number blocks forever; per-partition ordering mistaken for global ordering; a consumer
group rebalance reordering in-flight messages; a duplicate sequence number accepted.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | Messages are emitted in sequence order regardless of arrival order |
| I2 | A gap is detected and either resolves within a bounded window or escalates |
| I3 | The resequencer never emits the same message twice |
| I4 | The buffer bound is enforced with a defined overflow behaviour — never silent loss |
| I5 | The ordering guarantee's scope is documented and asserted: per-key, per-partition, or global |

**Profiles:** `baseline`, `load`, `stress`, `soak`. `stress` deliberately fills the reorder buffer,
which is the point of the profile here.

**Harness focus (JMeter):** publish a deliberately shuffled synthetic sequence through the Kafka
sampler; a consumer-side assertion verifying monotonic emission; an injected permanent gap to prove
escalation rather than an indefinite block.

**Tool fit:** JMeter `BEST` (Kafka sampler plus ordered assertion); Gatling + Karate `fair`;
k6 `fair` (needs `xk6-kafka`); Locust `good` (sequence bookkeeping is easy in Python).

**Overlays:** Resilience — a broker `instance-loss` and a consumer-group rebalance during the run;
assert I1 and I3 across the rebalance. Omit contract, security, and data-quality overlays.

**Compliance:** Ring 0 — EIP §7 Resequencer. Ring 1 — ISO 20022 sequencing requirements; BCBS 239
Principle 3. Ring 2 — SBV Circular 09/2020, `⚠️`.

**Steps:** Apply the Standard Archetype Steps with `FILE=…/ordering-resequencing.md` and
`ID=TST-027`. `primary_tool: jmeter` for all three coverage rows.

---

## Task 26: TST-028 Fan-out / Fan-in Correlation

**Files:** Create `knowledge-base/testing/archetypes/fanout-fanin-correlation.md`

**Interfaces:**
- Consumes: `TST-006` `dependency-blackhole` and `dependency-latency` fault classes.
- Produces: the partial-aggregate and bounded-wait assertions reused by TST-035.

**Tier Applicability:** T0, T1

**Applies To:** `EIP-015` Scatter-Gather (`../../patterns/eip/scatter-gather.md`); `EIP-011`
Aggregator (`../../patterns/eip/aggregator.md`); `EIP-009` Claim Check
(`../../patterns/eip/claim-check.md`); `EIP-018` Message Store
(`../../patterns/eip/message-store.md`).

**Oracle:** `invariant-assertion`

**Failure taxonomy:** an aggregator waiting indefinitely for a response that will never arrive; a
partial aggregate emitted as though complete; a correlation ID collision merging two unrelated
conversations; the slowest branch dictating total latency with no partial-result strategy; a
claim-check payload expiring before retrieval; an aggregate emitted twice on retry.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | An aggregate is emitted only when its completeness condition is met, or on timeout with an explicit partial marker |
| I2 | Correlation IDs are unique within the correlation window |
| I3 | Aggregate contents equal the union of received branch responses, with no duplication |
| I4 | Fan-in latency approximates the slowest branch, not the sum of branches |
| I5 | A claim-check reference resolves for at least its declared retention period |

**Profiles:** `baseline`, `load`, `spike`, `failover-under-load`. Run with one branch made
deliberately slow and another blackholed — a fan-in that only ever sees healthy branches is
untested.

**Harness focus (JMeter):** a Parallel Controller for fan-out; `${__UUID()}` for the correlation
ID; a bounded-wait assertion on the aggregate; one branch routed to a blackhole endpoint.

**Tool fit:** JMeter `BEST` (Parallel Controller plus Synchronizing Timer); Gatling + Karate `good`;
k6 `good`; Locust `fair`.

**Overlays:** Resilience — `dependency-latency` on one branch, `dependency-blackhole` on another,
simultaneously. Omit contract, security, and data-quality overlays.

**Compliance:** Ring 0 — EIP §7 Aggregator and Scatter-Gather. Ring 1 — BCBS 239 Principle 4
(completeness — a partial aggregate presented as complete is a completeness breach). Ring 2 — SBV
Circular 09/2020, `⚠️`.

**Steps:** Apply the Standard Archetype Steps with `FILE=…/fanout-fanin-correlation.md` and
`ID=TST-028`. `primary_tool: jmeter` for all four coverage rows.

---

## Task 27: TST-029 Delivery Guarantee, Retry and DLQ

**Files:** Create `knowledge-base/testing/archetypes/delivery-guarantee-dlq.md`

**Interfaces:**
- Consumes: `TST-020` idempotency invariants (redelivery must not duplicate effects); `TST-006`
  `instance-loss`.
- Produces: the DLQ-depth and backoff-interval assertions reused by TST-035 (retry amplification).

**Tier Applicability:** T0, T1

**Applies To:** eight rows — `EIP-023` Guaranteed Delivery
(`../../patterns/eip/guaranteed-delivery.md`); `EIP-022` Durable Subscriber
(`../../patterns/eip/durable-subscriber.md`); `EIP-025` Dead Letter Channel
(`../../patterns/eip/dead-letter-channel.md`); `EIP-021` Channel Purger
(`../../patterns/eip/channel-purger.md`); `EIP-001` Message Channel
(`../../patterns/eip/message-channel.md`); `EIP-002` Point-to-Point Channel
(`../../patterns/eip/point-to-point-channel.md`); `EIP-020` Test Message
(`../../patterns/eip/test-message.md`); `INT-014` Webhook Delivery Reliability
(`../../patterns/integration/webhook-delivery-reliability.md`).

**Oracle:** `invariant-assertion`

**Failure taxonomy:** a message lost on broker restart because it was never persisted; a consumer
acknowledging before processing, so a crash loses the message; a poison message blocking its
partition indefinitely; a DLQ filling with no alert; retry without backoff amplifying an outage; a
webhook retried forever against a permanently dead endpoint; redelivery after acknowledgement
producing a duplicate side effect.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | Every published message is either processed exactly once in effect or lands in the DLQ — never silently lost |
| I2 | A broker restart loses nothing that was acknowledged as persisted |
| I3 | A poison message reaches the DLQ within its declared attempt count and does not block its partition |
| I4 | Retry intervals follow the declared backoff curve, with jitter |
| I5 | DLQ depth is observable and alertable |
| I6 | An endpoint returning a permanent error stops being retried per the declared policy |

**Profiles:** `baseline`, `load`, `spike`, `soak`, `failover-under-load`. `soak` asserts DLQ depth
stays flat over the full run; `failover-under-load` kills the broker mid-run and asserts I2.

**Harness focus (JMeter):** Kafka or JMS sampler with a synthetic poison message injected at a known
offset; a DLQ-depth assertion via JDBC or the broker admin API; broker `instance-loss` during the
run; `EIP-020` Test Message as the liveness probe mechanism, cross-linked rather than restated.

**Tool fit:** JMeter `BEST` (broker-native samplers plus admin-API assertions); Gatling +
Karate `fair`; k6 `fair`; Locust `good`.

**Overlays:** Resilience — `instance-loss` on the broker, `resource-exhaustion` on the consumer, and
the retry-amplification measurement from TST-006. Omit contract, security, and data-quality
overlays.

**Compliance:** Ring 0 — EIP §4 Guaranteed Delivery and §10 Dead Letter Channel. Ring 1 — BCBS 239
Principle 4 (completeness); ISO 20022 non-repudiation of delivery. Ring 2 — SBV Circular 09/2020
§IV.2, `⚠️`.

**Steps:** Apply the Standard Archetype Steps with `FILE=…/delivery-guarantee-dlq.md` and
`ID=TST-029`. `primary_tool: jmeter` for all eight coverage rows.

---

## Task 28: TST-030 Contract and Schema Compatibility

**Files:** Create `knowledge-base/testing/archetypes/contract-schema-compatibility.md`

**Interfaces:**
- Consumes: `TST-007` compatibility modes; `TST-012` `karate-gatling` feature reuse.
- Produces: the compatibility-mode verification method referenced by every messaging archetype's
  contract overlay.

**Tier Applicability:** T0, T1, T2

**Applies To:** `INT-015` API Contract Testing
(`../../patterns/integration/api-contract-testing.md`); `INT-010` AsyncAPI Specification
(`../../patterns/integration/asyncapi-specification.md`); `INT-011` CloudEvents Envelope
(`../../patterns/integration/cloudevents-envelope.md`); `INT-013` Schema Registry Governance
(`../../patterns/integration/schema-registry-governance.md`); `INT-003` API Gateway Routing
(`../../patterns/integration/api-gateway-routing.md`).

**Oracle:** `contract-schema`

**Failure taxonomy:** a producer adding a required field and breaking consumers under `BACKWARD`;
compatibility mode set to `NONE` and nobody noticing; consumer tests passing against a stale mock
rather than the real producer; CloudEvents required attributes missing so routing fails; an error
contract changed without a version bump; a gateway route change silently altering the effective
contract.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | Every registered schema version satisfies the declared compatibility mode against its predecessor |
| I2 | Every consumer contract verifies against the current producer, not a mock |
| I3 | The declared compatibility mode equals the registry's actual configured mode |
| I4 | Every CloudEvents required attribute is present and correctly typed |
| I5 | Every documented error code is reachable and its mapping is stable |
| I6 | A deliberately incompatible candidate schema is rejected by the registry |

**Profiles:** `baseline` only. State plainly why: this is a functional and contract archetype, and
schema-registry lookup latency on a hot path is asserted inside the owning service's `load` run
rather than duplicated here. Record `perf_profiles: [baseline]` so the coverage matrix is honest
rather than padded.

**Harness focus:** Karate feature files as the contract suite, reused unchanged as a Gatling
scenario via `karate-gatling` per TST-012 — the single artifact serving both disciplines is the
reason this archetype's primary tool differs.

**Tool fit:** Gatling + Karate `BEST` (Karate is a contract-testing tool first; its feature files
double as the performance scenario); k6 `good` (schema assertion in JavaScript); JMeter `fair`;
Locust `fair`.

Record `primary_tool: gatling-karate` for all five coverage rows.

**Overlays:** Contract — this is the body of the document; cross-link TST-007 for the mode
definitions rather than restating them. Omit resilience, security, and data-quality overlays.

**Compliance:** Ring 0 — Pact consumer-driven contract specification; OpenAPI, AsyncAPI, and
CloudEvents specifications. Ring 1 — ISO 20022 message conformance; SWIFT CSP control 2.x.
Ring 2 — SBV Circular 09/2020, `⚠️`.

**Steps:** Apply the Standard Archetype Steps with `FILE=…/contract-schema-compatibility.md` and
`ID=TST-030`.

---

## Task 29: Wave C Catalog Registration

**Files:**
- Modify: `governance/standards/_catalog-inventory.yml`
- Modify: `governance/standards/enterprise-architecture-catalog.md`
- Modify: `mkdocs.yml`
- Modify: `knowledge-base/testing/README.md` (archetype index — mark the eleven as landed)

**Interfaces:**
- Consumes: the eleven archetypes from Tasks 18–28.
- Produces: catalog total 218, `testing` category count 26.

- [ ] **Step 1: Append eleven inventory rows and eleven catalog table rows**

`TST-020`…`TST-030`, all `category: testing`, `status: Approved`, `owner: qe-lead`,
`spine_or_radii: radii`, `last_reviewed: '2026-08-12'`, `notes: Wave 15C — correctness and
messaging archetypes`, `target_wave: 4`. Take each row's `tiers` from the Tier Applicability the
archetype task specified, and `compliance_refs` from each document's Compliance Mapping table.

- [ ] **Step 2: Update the counts**

Coverage sentence → `Coverage: 218 Approved catalog rows across 17 categories — 7 spine docs and 211 radii docs after Wave 15C correctness and messaging archetypes.`
Category table: `testing` 15 → 26; `**Total**` 207 → 218.

- [ ] **Step 3: Add an Archetypes subsection to the mkdocs nav**

Replace the placeholder `Archetypes: knowledge-base/testing/archetypes/` entry with an explicit
nested list of the eleven documents, so the published site has real navigation rather than a
directory listing.

- [ ] **Step 4: Run every gate**

```bash
python3 scripts/audit-catalog-consistency.py --check-doc-status; echo "audit exit=$?"
python3 scripts/validate-internal-links.py; echo "links exit=$?"
python3 scripts/check-compliance-rows.py; echo "compliance exit=$?"
python3 scripts/validate-testing-coverage.py --quiet
grep -c '^| [A-Z]\+-[0-9]\+ |' governance/standards/enterprise-architecture-catalog.md
```

Expected: the first three gates exit `0`; the catalog row count returns `218`. The coverage gate
still exits `1` on `check1` for unpopulated rows — but it must now report **zero** `check3`
failures, because all eleven Wave C archetype documents exist. A surviving `check3` means a
coverage row references an archetype ID that no document declares.

- [ ] **Step 5: Commit**

```bash
git add governance/standards/_catalog-inventory.yml \
        governance/standards/enterprise-architecture-catalog.md \
        mkdocs.yml knowledge-base/testing/README.md
git commit -m "chore(catalog): register Wave C testing archetype rows

TST-020..TST-030 — correctness, state, messaging, and integration
archetypes. Catalog 207 -> 218 rows.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Wave D — Load, Capacity and Resilience Archetypes

Six archetypes: Family C (TST-031…TST-034) and Family D (TST-035…TST-036).

## Task 30: TST-031 Rate Limit, Throttle and Breakpoint

This archetype owns the breakpoint method. Other archetypes that need a knee reference it.

**Files:** Create `knowledge-base/testing/archetypes/rate-limit-breakpoint.md`

**Interfaces:**
- Consumes: `TST-003` open-model rule; `TST-011` Throughput Shaping Timer and Concurrency Thread
  Group.
- Produces: the step-ramp breakpoint method and the knee-identification criterion, referenced by
  TST-033, TST-034, TST-035, and TST-041.

**Tier Applicability:** T0, T1, T2

**Applies To:** `RES-008` Throttling / Rate Limiting
(`../../patterns/resilience/throttling-rate-limiting.md`); `RES-009` Load Shedding
(`../../patterns/resilience/load-shedding.md`); `RES-011` Queue-Based Load Levelling
(`../../patterns/resilience/queue-based-load-levelling.md`).

**Oracle:** `invariant-assertion`

**Failure taxonomy:** a limit enforced per instance rather than globally, so the effective limit is
N × the configured value; `429` returned without a retry hint, so clients retry immediately and
amplify; shedding dropping high-value requests indiscriminately; a queue absorbing a burst while
requests silently breach their end-to-end budget inside it; the limiter itself becoming the
bottleneck; a burst allowance mis-tuned so legitimate bursts are rejected; no distinction between
per-client and global limits.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | Offered load above the limit is rejected, not queued indefinitely |
| I2 | Accepted rate stays within the configured limit measured **globally**, not per instance |
| I3 | Every rejection carries a machine-readable retry hint |
| I4 | Shedding preserves the classes the declared policy prioritises |
| I5 | A queued request either meets its end-to-end budget or is rejected before enqueue |
| I6 | The limiter's own overhead is bounded and measured |

**Profiles:** `baseline`, `load`, `stress`, `spike`, `scalability`. **Workload model `open` is
mandatory** — a closed model makes a breakpoint test meaningless. Define the knee explicitly: the
offered rate beyond which goodput stops increasing while latency continues to rise.

**Harness focus (JMeter):** Concurrency Thread Group plus Throughput Shaping Timer for a precise
step-ramp; an assertion comparing observed `429` rate against the configured limit; distributed
workers where a single generator cannot exceed the limit — and the rule from TST-003 that a result
is void if the generator saturated first.

**Tool fit:** JMeter `BEST` (Throughput Shaping Timer gives the most precise step-ramp of the four);
k6 `good` (`ramping-arrival-rate`); Gatling + Karate `good`; Locust `fair` (user-based model makes it
a poor breakpoint tool — say so plainly).

**Overlays:** Resilience — `resource-exhaustion` on the limiter's backing store; assert I2 still
holds when the store is degraded. Omit contract, security, and data-quality overlays.

**Compliance:** Ring 0 — AWS Well-Architected Reliability pillar on throttling and load shedding.
Ring 1 — BCBS 230 Principle 9 (capacity under severe-but-plausible load). Ring 2 — SBV Circular
09/2020, `⚠️`.

**Steps:** Standard Archetype Steps, `FILE=…/rate-limit-breakpoint.md`, `ID=TST-031`.
`primary_tool: jmeter` for all three coverage rows.

---

## Task 31: TST-032 Batch Window and Cutoff Throughput

**Files:** Create `knowledge-base/testing/archetypes/batch-window-cutoff.md`

**Interfaces:**
- Consumes: `TST-004` seeded volume generation; `TST-020` restart idempotency.
- Produces: the window-completion and restartability assertions referenced by TST-022 and TST-039
  for their batch modes.

**Tier Applicability:** T0, T1

**Applies To:** `BSP-004` End-of-Day Batch Window
(`../../patterns/banking-solutions/end-of-day-batch-window.md`); `BSP-019` Collections Engine
(`../../patterns/banking-solutions/collections-engine.md`); `REF-008` Regulatory Reporting
(`../../reference-architectures/regulatory-reporting.md`); `DATA-004` Data Vault 2.0
(`../../patterns/data/data-vault-2.md`).

**Oracle:** `invariant-assertion`

**Failure taxonomy:** a batch exceeding its window and colliding with the next business day; no
restartability, so any failure forces a full re-run that no longer fits the window; cutoff applied
on the wrong timezone; a partial batch committed with no idempotent restart; throughput degrading
nonlinearly as volume grows; a late-arriving transaction landing in the wrong business date; batch
and online traffic contending for the same database.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | The batch completes within its declared window at declared volume |
| I2 | Restart after a mid-batch failure yields the same final state as an uninterrupted run |
| I3 | The cutoff boundary uses the declared timezone and business calendar |
| I4 | Every transaction is processed in exactly one business date — never two, never none |
| I5 | Throughput degradation stays within the declared bound across the declared volume range |
| I6 | Online latency during the batch window stays within its own budget |

**Profiles:** `baseline`, `load`, `scalability`, `soak`, `mixed`. `mixed` is the important one here —
batch and online running concurrently is the real contention case, and I6 can only be asserted that
way. Note that the window duration comes from the business calendar, not from `NFR-002`.

**Harness focus (JMeter):** seeded synthetic volume at 1×, 2×, and 4× declared volume for the
scalability curve; a kill-and-restart mid-batch with a JDBC assertion comparing final state against
an uninterrupted control run; concurrent online load to assert I6.

**Tool fit:** JMeter `BEST` (JDBC plus volume generation); Locust `good`; Gatling + Karate `fair`;
k6 `fair`.

**Overlays:** Resilience — `instance-loss` mid-batch (asserts I2) and `slow-disk` to test window
sensitivity. Data-quality — completeness of the batch output against input count. Omit contract and
security overlays.

**Compliance:** Ring 0 — batch checkpointing and restartability. Ring 1 — BCBS 239 Principle 5
(timeliness); BCBS 230 Principle 9. Ring 2 — SBV regulatory reporting submission deadlines, `⚠️`.

**Steps:** Standard Archetype Steps, `FILE=…/batch-window-cutoff.md`, `ID=TST-032`.
`primary_tool: jmeter` for all four coverage rows.

---

## Task 32: TST-033 Multi-Tenant Isolation and Noisy Neighbour

**Files:** Create `knowledge-base/testing/archetypes/multitenant-noisy-neighbour.md`

**Interfaces:**
- Consumes: `TST-031` breakpoint method (used to drive tenant A to saturation); `TST-006` blast
  radius measurement.
- Produces: the two-tenant differential measurement method.

**Tier Applicability:** T0, T1

**Applies To:** `PLT-008` Multi-Tenancy Isolation
(`../../patterns/platform/multi-tenancy-isolation.md`); `RES-001` Bulkhead Isolation
(`../../patterns/resilience/bulkhead-isolation.md`); `RES-005` Cell-Based Architecture
(`../../patterns/resilience/cell-based-architecture.md`); `PLT-006` FinOps Cost Allocation
(`../../patterns/platform/finops-cost-allocation.md`).

**Oracle:** `invariant-assertion`

**Failure taxonomy:** one tenant's burst degrading another's latency; a shared connection pool
exhausted by a single tenant; cross-tenant data leakage appearing only under concurrency; a cell
failure affecting more than its declared blast radius; a per-tenant quota not actually enforced;
cost attribution wrong, so a noisy tenant is commercially invisible.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | Tenant A at maximum quota does not push tenant B's P95 beyond tenant B's budget |
| I2 | No response contains another tenant's data, under any concurrency |
| I3 | A bulkhead's saturation is contained to its own pool |
| I4 | A cell failure's blast radius equals its declared tenant set — no more |
| I5 | Per-tenant quota is enforced |
| I6 | Cost per transaction is attributable per tenant |

**Profiles:** `baseline`, `load`, `stress`, `mixed`, `failover-under-load`. The core method is
differential: drive tenant A to saturation with `stress` while measuring tenant B at steady
`load`, and assert on tenant B's numbers.

**Harness focus (JMeter):** two Thread Groups with distinct synthetic tenant credentials and
**separate** assertions and reports per tenant — a combined aggregate would average away exactly
the effect being tested; plus a deliberate cross-tenant read asserted to fail.

**Tool fit:** JMeter `BEST` (independent Thread Groups with separate per-tenant assertions);
Gatling + Karate `good`; k6 `good` (tag-scoped thresholds); Locust `fair`.

**Overlays:** Resilience — `zone-loss` against one cell; assert I4. Security — the cross-tenant
access attempt, cross-linking TST-040. Omit contract and data-quality overlays.

**Compliance:** Ring 0 — AWS Well-Architected cell-based isolation. Ring 1 — BCBS 230 §27 (blast
radius containment); PCI-DSS 4.0 network and tenant segmentation. Ring 2 — SBV Circular 09/2020,
`⚠️`.

**Steps:** Standard Archetype Steps, `FILE=…/multitenant-noisy-neighbour.md`, `ID=TST-033`.
`primary_tool: jmeter` for all four coverage rows.

---

## Task 33: TST-034 Blended Journey Workload

Owns the `mixed` profile and the named blend registry's execution. Covers all 20 reference
architectures.

**Files:** Create `knowledge-base/testing/archetypes/blended-journey-workload.md`

**Interfaces:**
- Consumes: `TST-003` named journey blends; `TST-002` `mixed` per-journey pass criterion.
- Produces: the per-journey attribution method that makes a blend result interpretable.

**Tier Applicability:** T0, T1

**Applies To:** all twenty `REF-*` rows, each linked as `../../reference-architectures/<file>.md`.
List all twenty explicitly in §1 — this is the archetype that discharges reference-architecture
coverage, so an omission is a coverage gap. The files are: `multi-region-active-active`,
`real-time-payments-napas`, `kyc-aml-onboarding`, `card-authorization-3ds2`,
`swift-mt-mx-wire-transfer`, `loan-origination`, `fraud-screening-platform`,
`regulatory-reporting`, `account-opening-omnichannel`, `ledger-posting-engine`,
`open-banking-psd2`, `dispute-management`, `retail-deposits-platform`,
`consumer-lending-platform`, `credit-card-issuing-platform`,
`corporate-lending-syndications`, `trade-finance-platform`, `treasury-fx-platform`,
`wealth-management-platform`, `cash-management-liquidity`.

**Oracle:** `invariant-assertion`

**Failure taxonomy:** an aggregate P95 passing while a journey inside the blend breaches its own
budget; blend percentages drifting from real traffic so the test measures a fiction; a low-volume,
high-value journey starved below its share; shared-resource contention that appears only in a blend
and never in single-journey runs; a blend run too short to reach cache steady state; one journey's
failure cascading and being attributed to another.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | Every constituent journey meets its **own** tier budget |
| I2 | Blend percentages match the declared mix within tolerance |
| I3 | No journey is starved below its declared share |
| I4 | Errors are attributed per journey, never only in aggregate |
| I5 | Cache and connection-pool steady state is reached before measurement begins |

**Profiles:** `mixed` (primary), `soak`, `load`, `failover-under-load`.

**Harness focus (JMeter):** one Thread Group per journey with a Throughput Controller expressing its
percentage; per-journey Transaction Controllers and separate aggregate reports; an explicit warm-up
period excluded from measurement, per I5.

**Tool fit:** JMeter `BEST` (Throughput Controller expresses a percentage blend directly);
Gatling + Karate `good` (per-request-group assertions); k6 `good` (tag-scoped thresholds);
Locust `fair`.

**Overlays:** Resilience — inject a fault affecting one journey and assert I4, that the error is
attributed correctly and does not contaminate other journeys' numbers. Omit contract, security, and
data-quality overlays.

**Compliance:** Ring 0 — Google SRE Workbook Ch. 5. Ring 1 — BCBS 230 Principle 9 (a blended peak
is the plausible scenario). Ring 2 — SBV Circular 09/2020, `⚠️`.

**Steps:** Standard Archetype Steps, `FILE=…/blended-journey-workload.md`, `ID=TST-034`.
`primary_tool: jmeter` for all twenty coverage rows, `perf_profiles: [mixed, soak, load, failover-under-load]`.

---

## Task 34: TST-035 Fault Injection and Graceful Degradation

**Files:** Create `knowledge-base/testing/archetypes/fault-injection-degradation.md`

**Interfaces:**
- Consumes: `TST-006` fault taxonomy and steady-state hypothesis; `TST-029` DLQ and backoff
  assertions.
- Produces: the state-transition timing assertions for breaker and fallback behaviour.

**Tier Applicability:** T0, T1

**Applies To:** nine rows — `RES-002` Circuit Breaker
(`../../patterns/resilience/circuit-breaker.md`); `RES-007` Fallback Strategies
(`../../patterns/resilience/fallback-strategies.md`); `RES-004` Graceful Degradation
(`../../patterns/resilience/graceful-degradation.md`); `RES-006` Timeout Budget
(`../../patterns/resilience/timeout-budget.md`); `RES-012` Health Check Aggregation
(`../../patterns/resilience/health-check-aggregation.md`); `RES-010` Leader Election
(`../../patterns/resilience/leader-election.md`); `RES-001` Bulkhead Isolation
(`../../patterns/resilience/bulkhead-isolation.md`); `RES-003` Retry with Backoff
(`../../patterns/resilience/retry-with-backoff.md`); `BP-005` Chaos Engineering
(`../../best-practices/chaos-engineering.md`); `BP-002` Disaster Recovery Playbook
(`../../best-practices/disaster-recovery-playbook.md`).

That is **ten** rows. `BP-002` sits here because a DR playbook is verified by executing it under
the `failover-under-load` profile — it is exercised, not merely referenced.

**Oracle:** `invariant-assertion`

**Failure taxonomy:** a breaker whose threshold is unreachable so it never opens; a breaker that
opens while its fallback also fails; a fallback returning stale data with no indication of
degradation; a callee timeout longer than its caller's, so the caller always gives up first; a
health check reporting healthy while dependencies are down; leader election flapping under
partition and producing two leaders; a retry storm on recovery; degradation silently dropping a
regulated function.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | The breaker opens within its declared threshold under the injected fault |
| I2 | While open, calls fail fast without reaching the dependency |
| I3 | Half-open probes are bounded, and a probe failure reopens the breaker |
| I4 | The fallback path is exercised and its result is marked as degraded |
| I5 | The callee's timeout is strictly less than its caller's |
| I6 | The health check reflects real dependency state |
| I7 | Exactly one leader exists during and after a partition |
| I8 | Recovery produces no thundering herd on the dependency |
| I9 | No regulated function is silently dropped by degradation |

**Profiles:** `failover-under-load` (primary), `baseline`, `load`, `spike`.

**Harness focus (JMeter):** the fault injected mid-run at a recorded timestamp; assertions on
state-transition timing relative to that timestamp; a separate measurement of offered load on the
dependency during recovery, which is the retry-amplification assertion from TST-006.

**Tool fit:** JMeter `BEST`; Gatling + Karate `good`; k6 `good`; Locust `fair`.

**Overlays:** Resilience — this is the body of the document; use all ten fault classes from TST-006
and state which invariant each one exercises. Omit contract, security, and data-quality overlays.

**Compliance:** Ring 0 — NIST SP 800-53 CP-4 (contingency plan testing); Principles of Chaos
Engineering. Ring 1 — BCBS 230 Principle 9 and §27. Ring 2 — SBV Circular 09/2020 §IV.3 BCP drill
obligations, `⚠️`.

**Steps:** Standard Archetype Steps, `FILE=…/fault-injection-degradation.md`, `ID=TST-035`.
`primary_tool: jmeter` for all nine coverage rows.

---

## Task 35: TST-036 Zero-Downtime Deploy, Traffic Shift and Rotation

**Files:** Create `knowledge-base/testing/archetypes/zero-downtime-deploy-rotation.md`

**Interfaces:**
- Consumes: `TST-006` fault classes; `TST-008` rotation-under-load rule.
- Produces: the continuous-load-across-a-change-event method and its error-attribution technique.

**Tier Applicability:** T0, T1

**Applies To:** eight rows — `PLT-003` GitOps Deployment Pipeline
(`../../patterns/platform/gitops-deployment-pipeline.md`); `PLT-001` Service Mesh Traffic Management
(`../../patterns/platform/service-mesh-traffic.md`); `PLT-005` Kubernetes Operator Pattern
(`../../patterns/platform/kubernetes-operator-pattern.md`); `INT-006` Strangler Fig
(`../../patterns/integration/strangler-fig.md`); `SEC-007` Secrets Rotation
(`../../patterns/security/secrets-rotation.md`); `SEC-003` Vault Secret Management
(`../../patterns/security/vault-secret-management.md`); `FE-004` Web Feature Flags
(`../../patterns/frontend/web-feature-flags.md`); `MOB-006` Mobile Force-Upgrade
(`../../patterns/mobile/mobile-force-upgrade.md`).

**Oracle:** `invariant-assertion`

**Failure taxonomy:** in-flight requests dropped at pod termination because there is no graceful
shutdown or `preStop` delay; a readiness probe passing before warm-up so traffic hits a cold
instance; canary metrics evaluated over too short a window; a rollback that leaves a schema change
applied; a secret rotated while pooled connections still hold the old credential; a feature-flag
flip producing inconsistent state within a single request; a forced upgrade blocking a user
mid-transaction; a strangler route shifted where old and new behaviour diverge.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | Zero failed requests attributable to a deploy, rotation, or traffic shift |
| I2 | In-flight requests either complete or are cleanly retriable |
| I3 | Readiness gates traffic until the instance is warm |
| I4 | Rollback restores prior behaviour completely, including schema compatibility |
| I5 | Rotation completes with no failed request attributable to it |
| I6 | Old and new strangler routes produce equivalent results for the same input |
| I7 | A flag flip is atomic from a single request's perspective |

**Profiles:** `failover-under-load` (primary — the change event *is* the injected event), `load`,
`baseline`.

**Harness focus (JMeter):** continuous load spanning the entire deploy or rotation window; error
attribution by timestamp correlated against the change event; a shadow-comparison assertion for I6,
sending the same synthetic input to both strangler routes and comparing.

**Tool fit:** JMeter `BEST`; k6 `good` (natural fit where the deploy is pipeline-driven);
Gatling + Karate `good`; Locust `fair`.

**Overlays:** Resilience — `instance-loss` during rollout, `partial-partition` during traffic shift.
Security — rotation under load, cross-linking TST-008 and `SEC-007`. Omit contract and data-quality
overlays.

**Compliance:** Ring 0 — NIST SP 800-53 CM-3 (configuration change control); twelve-factor disposability.
Ring 1 — PCI-DSS 4.0 §6.5.2 (change control); BCBS 230 Principle 9. Ring 2 — SBV Circular 09/2020
change-management expectations, `⚠️`.

**Steps:** Standard Archetype Steps, `FILE=…/zero-downtime-deploy-rotation.md`, `ID=TST-036`.
`primary_tool: jmeter` for all eight coverage rows.

---

## Task 36: Wave D Catalog Registration

**Files:** Modify `_catalog-inventory.yml`, `enterprise-architecture-catalog.md`, `mkdocs.yml`,
`knowledge-base/testing/README.md`.

**Interfaces:**
- Consumes: the six archetypes from Tasks 30–35.
- Produces: catalog total 224, `testing` category count 32.

- [ ] **Step 1: Append six inventory rows and six catalog table rows**

`TST-031`…`TST-036`, `notes: Wave 15D — load, capacity, and resilience archetypes`,
`target_wave: 4`, everything else as in Task 29 Step 1.

- [ ] **Step 2: Update the counts**

Coverage sentence → `Coverage: 224 Approved catalog rows across 17 categories — 7 spine docs and 217 radii docs after Wave 15D load and resilience archetypes.`
Category table: `testing` 26 → 32; `**Total**` 218 → 224.

- [ ] **Step 3: Add the six nav entries and update the README archetype index**

- [ ] **Step 4: Run every gate**

```bash
python3 scripts/audit-catalog-consistency.py --check-doc-status; echo "audit exit=$?"
python3 scripts/validate-internal-links.py; echo "links exit=$?"
python3 scripts/check-compliance-rows.py; echo "compliance exit=$?"
python3 scripts/validate-testing-coverage.py --quiet
grep -c '^| [A-Z]\+-[0-9]\+ |' governance/standards/enterprise-architecture-catalog.md
```

Expected: the first three gates exit `0`; row count `224`; coverage gate exits `1` with `check1`
only. All twenty `REF-*` rows must now have coverage rows via TST-034 — confirm none of them still
appears in the `check1` list.

- [ ] **Step 5: Commit**

```bash
git add governance/standards/_catalog-inventory.yml \
        governance/standards/enterprise-architecture-catalog.md \
        mkdocs.yml knowledge-base/testing/README.md
git commit -m "chore(catalog): register Wave D testing archetype rows

TST-031..TST-036 — load, capacity, and resilience archetypes. Catalog
218 -> 224 rows.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Wave E — Data, Observability and Client Archetypes

Five archetypes: Family E (TST-037…TST-039) and Family G (TST-042…TST-043).

## Task 37: TST-037 Read-Model Convergence and CDC Lag

**Files:** Create `knowledge-base/testing/archetypes/read-model-convergence-lag.md`

**Interfaces:**
- Consumes: `TST-009` bounded-convergence rule; `TST-024` bounded-wait terminal-state assertion.
- Produces: the tail-percentile lag assertion reused by TST-038 and TST-039.

**Tier Applicability:** T0, T1

**Applies To:** seven rows — `DATA-001` CQRS Pattern (`../../patterns/data/cqrs-pattern.md`);
`DATA-008` Change Data Capture (`../../patterns/data/change-data-capture.md`); `DATA-007` Kappa
Architecture (`../../patterns/data/kappa-architecture.md`); `DATA-006` Lambda Architecture
(`../../patterns/data/lambda-architecture.md`); `DATA-012` Data Virtualization
(`../../patterns/data/data-virtualization.md`); `INT-002` Transactional Outbox + CDC
(`../../patterns/integration/cdc-outbox-pattern.md`); `INT-004` Event Sourcing
(`../../patterns/integration/event-sourcing.md`).

**Oracle:** `invariant-assertion`

**Failure taxonomy:** a read model that never converges because an event was dropped; convergence
asserted by unbounded polling, producing a test that always eventually passes and therefore tests
nothing; lag measured as a mean, hiding the tail; an outbox row committed but never published; a
full replay producing a different projection than the incremental path; a schema change breaking the
projector silently; a CDC connector restarting from the wrong offset and causing duplication or
gaps.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | The read model converges to match the write model within the declared bound |
| I2 | Lag is asserted at the tail percentile, never the mean |
| I3 | A full replay produces a projection identical to the incrementally built one |
| I4 | Every outbox row is eventually published exactly once |
| I5 | No event is lost or duplicated across a connector restart |
| I6 | Exceeding the convergence bound is a hard failure, never an indefinite wait |

**Profiles:** `baseline`, `load`, `spike`, `soak`, `failover-under-load`. `soak` asserts lag does not
creep upward over the run — a slowly growing lag is the characteristic CDC failure and is invisible
in a 60-minute run.

**Harness focus (JMeter):** write through the API, then poll the read model with a **bounded** wait;
JDBC assertions on both write and read sides; connector `instance-loss` mid-run for I5; a replay run
compared against the incremental projection for I3.

**Tool fit:** JMeter `BEST` (JDBC sampler on both sides of the projection); Locust `good`;
Gatling + Karate `fair`; k6 `fair`.

**Overlays:** Resilience — `instance-loss` on the connector and on the projector.
Data-quality — completeness and timeliness per TST-009. Omit contract and security overlays.

**Compliance:** Ring 0 — CQRS and event-sourcing canonical definitions. Ring 1 — BCBS 239
Principle 3 (accuracy) and Principle 5 (timeliness). Ring 2 — SBV Circular 09/2020, `⚠️`.

**Steps:** Standard Archetype Steps, `FILE=…/read-model-convergence-lag.md`, `ID=TST-037`.
`primary_tool: jmeter` for all seven coverage rows.

---

## Task 38: TST-038 Temporal and Historisation Correctness

**Files:** Create `knowledge-base/testing/archetypes/temporal-historisation.md`

**Interfaces:**
- Consumes: `TST-022` golden-dataset comparison method.
- Produces: the as-of query assertion set.

**Tier Applicability:** T1, T2

**Applies To:** `DATA-005` Slowly Changing Dimensions
(`../../patterns/data/slowly-changing-dimensions.md`); `DATA-003` Temporal Tables
(`../../patterns/data/temporal-tables.md`); `DATA-004` Data Vault 2.0
(`../../patterns/data/data-vault-2.md`); `DATA-010` Time-Series Modelling
(`../../patterns/data/time-series-modelling.md`).

**Oracle:** `golden-dataset`

**Failure taxonomy:** an as-of query returning the current row rather than the historically valid
one; overlapping validity intervals in an SCD-2 dimension; a gap between validity intervals losing a
period entirely; late-arriving data not back-dated correctly; a DST boundary producing a duplicate or
missing hour; a retroactive correction overwriting history instead of versioning it; downsampling a
time series and losing a spike.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | An as-of query returns exactly the row valid at that instant |
| I2 | Validity intervals per key are contiguous and non-overlapping |
| I3 | Exactly one current row exists per key |
| I4 | A late-arriving record lands in its correct effective period |
| I5 | DST transitions produce neither duplicate nor missing periods |
| I6 | A retroactive correction creates a new version and preserves its predecessor |
| I7 | Downsampling preserves the declared extrema |

**Profiles:** `baseline`, `load`, `scalability`. The scalability axis here is **history depth**, not
request rate — assert that an as-of query's latency does not degrade beyond its bound as history
grows. Say so explicitly, since it is an unusual scaling axis.

**Harness focus (JMeter):** a golden dataset of as-of queries with expected results as a synthetic
CSV; JDBC assertions; explicit fixtures at DST transition boundaries and at 29 February.

**Tool fit:** JMeter `BEST` (parameterised as-of queries via JDBC); Locust `good`;
Gatling + Karate `fair`; k6 `fair`.

**Overlays:** Data-quality — interval contiguity as a DQ rule per TST-009. Omit resilience, contract,
and security overlays.

**Compliance:** Ring 0 — Kimball SCD types; Data Vault 2.0 satellite historisation. Ring 1 — BCBS 239
Principle 3; audit-trail reconstruction requirements. Ring 2 — SBV record-retention and
reconstruction expectations, `⚠️`.

**Steps:** Standard Archetype Steps, `FILE=…/temporal-historisation.md`, `ID=TST-038`.
`primary_tool: jmeter` for all four coverage rows.

---

## Task 39: TST-039 Data Quality and Reconciliation

**Files:** Create `knowledge-base/testing/archetypes/data-quality-reconciliation.md`

**Interfaces:**
- Consumes: `TST-009` six dimensions and zero-tolerance rule; `TST-021` independent recomputation;
  `TST-025` confusion-matrix method.
- Produces: the dirty-corpus recall measurement.

**Tier Applicability:** T0, T1, T2

**Applies To:** `DATA-011` Data Quality Rules (`../../patterns/data/data-quality-rules.md`);
`DATA-013` Reference Data Master (`../../patterns/data/reference-data-master.md`); `DATA-009` Data
Lineage (`../../patterns/data/data-lineage.md`); `DATA-002` Data Mesh Ownership
(`../../patterns/data/data-mesh-ownership.md`).

**Oracle:** `confusion-matrix` as primary — measuring each DQ rule's recall against a corpus with
known defects. State explicitly that the reconciliation invariants (I2, I3) use
`invariant-assertion` as a declared secondary method, so a reviewer does not read it as a
template violation.

**Failure taxonomy:** a DQ rule defined but never executed; reconciliation with an undeclared
non-zero tolerance absorbing a real break; a reference-data update not propagating so two systems
disagree; lineage documented but never verified; an unassigned data-product owner so a failure has
no owner; dirty records counted but not classified by dimension.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | Every declared DQ rule executes and reports a result |
| I2 | Monetary reconciliation tolerance is exactly zero; any non-zero tolerance is named and approved |
| I3 | Reconciliation recomputes independently from source — never re-reads the same aggregate |
| I4 | A reference-data change propagates to all consumers within its declared window |
| I5 | Perturbing a source changes the derived value, proving the stated lineage |
| I6 | Each DQ rule's recall against the dirty corpus meets its declared threshold |

**Profiles:** `baseline`, `load`. Batch-scale DQ execution is asserted via TST-032 rather than
duplicated here — cross-link it.

**Harness focus:** a deliberately dirty synthetic corpus with known defect counts per dimension; an
independent reconciliation recomputation; a source-perturbation step for I5.

**Tool fit:** Locust `BEST` (independent recomputation and per-dimension defect accounting are
natural in Python); JMeter `good`; Gatling + Karate `fair`; k6 `fair`.

Record `primary_tool: locust` for all four coverage rows.

**Overlays:** Data-quality — the body of the document. Omit resilience, contract, and security
overlays.

**Compliance:** Ring 0 — DAMA-DMBOK data-quality dimensions. Ring 1 — BCBS 239 Principle 3
(accuracy), Principle 4 (completeness), Principle 5 (timeliness). Ring 2 — SBV reporting-accuracy
obligations, `⚠️`.

**Steps:** Standard Archetype Steps, `FILE=…/data-quality-reconciliation.md`, `ID=TST-039`.

---

## Task 40: TST-042 Telemetry and Observability Verification

**Files:** Create `knowledge-base/testing/archetypes/telemetry-verification.md`

**Interfaces:**
- Consumes: `TST-002` `load`, `spike`, and `soak` profiles.
- Produces: the post-run backend-assertion phase, which every other archetype's §10 Evidence
  section references for capturing trace and metric evidence.

**Tier Applicability:** T0, T1, T2

**Applies To:** all ten `OBS-*` rows — `OBS-001` OpenTelemetry Instrumentation
(`../../patterns/observability/otel-instrumentation.md`); `OBS-002` Distributed Trace Propagation
(`../../patterns/observability/distributed-trace-propagation.md`); `OBS-003` Structured Logging
Standard (`../../patterns/observability/structured-logging-standard.md`); `OBS-004` SLO Alerting
(`../../patterns/observability/slo-alerting.md`); `OBS-005` Async Middleware Observability
(`../../patterns/observability/async-middleware-observability.md`); `OBS-006` Error Budget Burn Rate
(`../../patterns/observability/error-budget-burn-rate.md`); `OBS-007` Tracing Sampling Strategy
(`../../patterns/observability/tracing-sampling-strategy.md`); `OBS-008` Log Aggregation Pipeline
(`../../patterns/observability/log-aggregation-pipeline.md`); `OBS-009` Synthetic Monitoring and
Canary Probes (`../../patterns/observability/synthetic-monitoring-canary.md`); `OBS-010` Metrics
Cardinality Management (`../../patterns/observability/metrics-cardinality-management.md`).

Plus three best-practice rows whose content this archetype actually exercises: `BP-004`
Observability Standards (`../../best-practices/observability-standards.md`); `BP-007` Golden Signals
(SRE) (`../../best-practices/golden-signals-sre.md`); `BP-008` Error Budgets
(`../../best-practices/error-budgets.md`). That is **thirteen** rows. Without these three, those
best-practice rows would have no coverage and would fail check 1 at Wave G.

**Oracle:** `invariant-assertion`

**Failure taxonomy:** a trace broken at an async hop so the journey cannot be reconstructed; metric
cardinality exploding under load and overwhelming the backend; a log pipeline dropping silently under
burst; sampling so aggressive that failing requests are never captured; an alert defined but never
actually firing; a burn-rate alert firing on an arithmetic error; inconsistent structured-log fields
so queries miss records; trace context lost across a queue boundary.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | A trace spans the full journey, including asynchronous hops |
| I2 | Metric cardinality stays within its declared bound at peak load |
| I3 | The log pipeline drops nothing at peak, or drops observably and countably |
| I4 | An error is always captured in traces regardless of the base sampling rate |
| I5 | Every declared alert fires in a drill |
| I6 | Burn-rate computation matches an independent calculation |
| I7 | Required structured-log fields are present on every record |

**Profiles:** `load`, `spike`, `soak`. Observability failures appear specifically at peak and over
duration — a `baseline` run will not surface any of them. State that reasoning explicitly.

**Harness focus (JMeter):** run any load profile, then a post-run assertion phase querying the
tracing and metrics backends over their APIs; include one deliberately failing synthetic request and
assert it is present in traces, which is how I4 is proven.

**Tool fit:** JMeter `BEST` (a post-run backend assertion phase is straightforward to express);
k6 `good`; Locust `good`; Gatling + Karate `fair`.

**Overlays:** Resilience — assert observability survives the fault injection of TST-035, since
telemetry that dies during an incident is worthless. Omit contract, security, and data-quality
overlays.

**Compliance:** Ring 0 — OpenTelemetry specification; Google SRE golden signals. Ring 1 — BCBS 230
Principle 9 (monitoring as an operational-resilience control). Ring 2 — SBV Circular 09/2020 §IV.3
monitoring obligations, `⚠️`.

**Steps:** Standard Archetype Steps, `FILE=…/telemetry-verification.md`, `ID=TST-042`.
`primary_tool: jmeter` for all ten coverage rows.

---

## Task 41: TST-043 Client Experience, Offline Sync and Performance Budget

**Files:** Create `knowledge-base/testing/archetypes/client-experience-offline-perf.md`

**Interfaces:**
- Consumes: `TST-027` ordering assertions (offline queue replay order); `TST-020` idempotency
  (replay must not duplicate).
- Produces: the client-side budget assertion method, which differs in kind from the protocol-level
  profiles.

**Tier Applicability:** T1, T2

**Applies To:** `FE-005` Web Error Boundary (`../../patterns/frontend/web-error-boundary.md`);
`FE-006` Web i18n / RTL (`../../patterns/frontend/web-i18n-rtl.md`); `FE-001` Web Performance
Budgets (`../../patterns/frontend/web-performance-budgets.md`); `FE-002` Web Resilience /
Offline-First (`../../patterns/frontend/web-resilience-offline-first.md`); `MOB-001` Mobile Offline
Queue (`../../patterns/mobile/mobile-offline-queue.md`); `MOB-006` Mobile Force-Upgrade
(`../../patterns/mobile/mobile-force-upgrade.md`).

**Oracle:** `invariant-assertion`

**Failure taxonomy:** an offline queue replaying in the wrong order after reconnect; an offline queue
duplicating on reconnect; an error boundary swallowing an error with no telemetry; i18n or RTL layout
breaking at the longest supported translation; a Core Web Vitals budget met on a fast development
device but missed on the target device class; force-upgrade blocking a user mid-transaction with
unsaved state; an offline queue growing without bound.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | An offline queue replays in original order, exactly once, on reconnect |
| I2 | The queue is bounded with a defined overflow behaviour |
| I3 | An error boundary contains the failure and emits telemetry |
| I4 | Layout holds at the longest supported translation and in RTL |
| I5 | The declared Core Web Vitals budget is met on the declared device and network class |
| I6 | Force-upgrade preserves or safely discards in-progress state, and never loses committed state |

**Profiles:** `baseline`, `load`. State explicitly that I5 is **not** a protocol load test: a client
performance budget is measured with browser-based tooling on a throttled device and network profile,
which is a different kind of measurement from the server-side profiles in TST-002. This is why this
archetype's performance obligations read differently from every other archetype, and the document
should say so rather than leave a reader to infer it. Record `perf_profiles: [baseline, load]`.

**Harness focus:** the k6 browser module for client-side vitals against a throttled device and
network profile; protocol-level tooling for the offline-queue replay assertions on the server side.
Do not name a tool the repository does not already document.

**Tool fit:** k6 `BEST` (the only one of the four with a real browser module, which I5 requires);
JMeter `fair` (protocol-level only — it cannot measure Core Web Vitals, and the document must say
that plainly); Gatling + Karate `fair`; Locust `fair`.

Record `primary_tool: k6` for all six coverage rows.

**Overlays:** Security — client-side storage of queued items at rest, cross-linking `MOB-002` and
TST-041. Omit contract and data-quality overlays.

**Compliance:** Ring 0 — Core Web Vitals thresholds; WCAG 2.2 AA for i18n and RTL. Ring 1 — GDPR
Art. 32 for queued personal data at rest on a device. Ring 2 — SBV Circular 09/2020 mobile-banking
security expectations, `⚠️`.

**Steps:** Standard Archetype Steps, `FILE=…/client-experience-offline-perf.md`, `ID=TST-043`.

---

## Task 42: Wave E Catalog Registration

**Files:** Modify `_catalog-inventory.yml`, `enterprise-architecture-catalog.md`, `mkdocs.yml`,
`knowledge-base/testing/README.md`.

**Interfaces:**
- Consumes: the five archetypes from Tasks 37–41.
- Produces: catalog total 229, `testing` category count 37.

- [ ] **Step 1: Append five inventory rows and five catalog table rows**

`TST-037`, `TST-038`, `TST-039`, `TST-042`, `TST-043`,
`notes: Wave 15E — data, observability, and client archetypes`, `target_wave: 4`.

Note the deliberate ID gap: `TST-040` and `TST-041` are reserved for Wave F and are registered
there, so the catalog table will temporarily show `TST-039` followed by `TST-042`. That is
intentional, not an error — record it in the notes column so a reviewer does not "fix" it.

- [ ] **Step 2: Update the counts**

Coverage sentence → `Coverage: 229 Approved catalog rows across 17 categories — 7 spine docs and 222 radii docs after Wave 15E data, observability, and client archetypes.`
Category table: `testing` 32 → 37; `**Total**` 224 → 229.

- [ ] **Step 3: Add the five nav entries and update the README archetype index**

- [ ] **Step 4: Run every gate**

```bash
python3 scripts/audit-catalog-consistency.py --check-doc-status; echo "audit exit=$?"
python3 scripts/validate-internal-links.py; echo "links exit=$?"
python3 scripts/check-compliance-rows.py; echo "compliance exit=$?"
python3 scripts/validate-testing-coverage.py --quiet
grep -c '^| [A-Z]\+-[0-9]\+ |' governance/standards/enterprise-architecture-catalog.md
```

Expected: the first three gates exit `0`; row count `229`; coverage gate exits `1` with `check1`
only. All ten `OBS-*` and all thirteen `DATA-*` rows must now have coverage rows.

- [ ] **Step 5: Commit**

```bash
git add governance/standards/_catalog-inventory.yml \
        governance/standards/enterprise-architecture-catalog.md \
        mkdocs.yml knowledge-base/testing/README.md
git commit -m "chore(catalog): register Wave E testing archetype rows

TST-037..TST-039, TST-042, TST-043 — data, observability, and client
archetypes. TST-040 and TST-041 remain reserved for Wave F. Catalog
224 -> 229 rows.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Wave F — Security Archetypes

Two archetypes, sequenced last because both require `@infosec-architect` review, which is a
scheduling dependency outside QE's control. Waves A–E and G complete without them.

## Task 43: TST-040 AuthN/AuthZ Matrix and Token Lifecycle

**Files:** Create `knowledge-base/testing/archetypes/authn-authz-token-lifecycle.md`

**Interfaces:**
- Consumes: `TST-008` authorisation-matrix method and token lifecycle case list; `TST-025`
  cardinality-curve technique (applied to matrix size).
- Produces: the bypass-path assertion (calling the service directly, around the gateway) referenced
  by TST-033's security overlay.

**Tier Applicability:** T0, T1

**Applies To:** `SEC-010` Attribute-Based Access Control
(`../../patterns/security/attribute-based-access-control.md`); `SEC-006` JWT Best Practices
(`../../patterns/security/jwt-best-practices.md`); `SEC-002` OAuth2 Authorization
(`../../patterns/security/oauth2-authorization.md`); `SEC-005` BFF + Token-Binding
(`../../patterns/security/bff-token-binding.md`); `SEC-011` Session Revocation
(`../../patterns/security/session-revocation.md`); `SEC-001` mTLS Service Mesh
(`../../patterns/security/mtls-service-mesh.md`); `MOB-003` Mobile Biometric Auth
(`../../patterns/mobile/mobile-biometric-auth.md`).

`SEC-010` is shared with TST-025 — its coverage row names both archetypes.

**Oracle:** `invariant-assertion`

**Failure taxonomy:** authorisation enforced at the gateway but not at the service, so a direct call
bypasses it; an expired token accepted because clock-skew tolerance is too wide; revocation not
honoured until natural expiry; refresh-token reuse permitted; a client-bound token replayed
successfully by a different client; mTLS validating the certificate chain but not the identity; a
biometric fallback to a weaker factor with no policy; an entitlement change not taking effect until
re-login.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | Every authorisation-matrix cell returns its expected allow or deny |
| I2 | A deny cannot be bypassed by calling the service directly, around the gateway |
| I3 | Expired, wrong-audience, wrong-issuer, and tampered-signature tokens are all rejected |
| I4 | A revoked token is rejected before its natural expiry, within the declared propagation window |
| I5 | A used refresh token cannot be reused |
| I6 | A client-bound token replayed by another client is rejected |
| I7 | mTLS asserts peer *identity*, not merely a valid chain |
| I8 | An entitlement change takes effect within its declared window |

I2 deserves emphasis in the document: gateway-only enforcement is the most common real
authorisation defect, and it is invisible to any test that only ever calls through the gateway.

**Profiles:** `baseline`, `load`, `soak`. `soak` targets token-cache and revocation-list growth. The
performance concern specific to this archetype is authorisation-decision latency as **matrix size**
grows — reuse TST-025's cardinality-curve method rather than restating it.

**Harness focus (JMeter):** a parameterised matrix sweep driven by CSV Data Set Config over
identity × role/attribute × resource × operation, with the expected verdict as a column; JMeter's
keystore configuration for the mTLS cases; a second Thread Group issuing direct-to-service calls for
I2.

**Tool fit:** JMeter `BEST` (CSV-driven matrix sweep plus native keystore and mTLS support);
k6 `good`; Gatling + Karate `good`; Locust `fair`.

**Overlays:** Security — the body of the document; cross-link TST-008 for the method and the case
list rather than restating them. Omit resilience, contract, and data-quality overlays.

**Compliance:** Ring 0 — OWASP ASVS V1 (architecture) and V4 (access control); OAuth 2.0
(RFC 6749/6750); RFC 8705 (mTLS client authentication and certificate-bound tokens); NIST SP 800-53
AC-3. Ring 1 — PCI-DSS 4.0 §7 (least privilege) and §8 (authentication); SWIFT CSP control 2.x.
Ring 2 — SBV Circular 09/2020 authentication requirements and Decree 13/2023, `⚠️`.

**Review:** `@infosec-architect` must review before this document moves to `Approved`. Author it with
`Status: Draft`, obtain review, then flip to `Approved` in the same task before registration.

**Steps:** Standard Archetype Steps, `FILE=…/authn-authz-token-lifecycle.md`, `ID=TST-040`.
`primary_tool: jmeter`. For `SEC-010`, append `TST-040` to its existing coverage row's `archetypes`
list rather than creating a second row — a duplicate row fails check 2.

---

## Task 44: TST-041 Data Protection, Masking and Tokenisation

**Files:** Create `knowledge-base/testing/archetypes/data-protection-masking-tokenisation.md`

**Interfaces:**
- Consumes: `TST-008` egress-assertion rule; `TST-004` synthetic-data prohibitions; `TST-031`
  breakpoint method (applied to the HSM ceiling).
- Produces: the multi-egress-path masking assertion, referenced by TST-043's security overlay.

**Tier Applicability:** T0, T1

**Applies To:** `SEC-008` Data Masking (`../../patterns/security/data-masking.md`); `SEC-013` PII
Tokenization, Format-Preserving
(`../../patterns/security/pii-tokenization-format-preserving.md`); `SEC-004` Tokenization + HSM Key
Management (`../../patterns/security/tokenization-hsm.md`); `SEC-012` Tamper-Evident Audit Logging
(`../../patterns/security/audit-logging-tamper-evident.md`); `MOB-002` Mobile Secure Storage
(`../../patterns/mobile/mobile-secure-storage.md`); `FE-003` Web CSP Hardening
(`../../patterns/frontend/web-csp-hardening.md`); `MOB-005` Mobile Deep Link Attestation
(`../../patterns/mobile/mobile-deep-link-attestation.md`); `MOB-004` Mobile Push Notification,
Secure (`../../patterns/mobile/mobile-push-notification-secure.md`).

**Oracle:** `invariant-assertion`

**Failure taxonomy:** data masked in the UI but present in logs, traces, or error payloads; a
format-preserving token colliding within the keyspace; detokenisation permitted for an unauthorised
caller; an HSM throughput ceiling discovered only in production; a mutable audit log so tampering is
undetectable; secure storage readable on a rooted or jailbroken device; CSP left in report-only mode
in production; a deep link accepted without attestation; a push payload carrying sensitive content.

**Invariants:**

| # | Invariant |
|---|---|
| I1 | Sensitive data is absent from **every** egress path — response, logs, traces, metric labels, error payloads, webhooks, exports, and support tooling |
| I2 | Format-preserving tokens are collision-free across the declared keyspace |
| I3 | Detokenisation requires the declared entitlement and is itself audited |
| I4 | HSM operation throughput meets its declared rate, and its ceiling is known and documented |
| I5 | Audit log entries are append-only and tamper-evident |
| I6 | Client secure storage is inaccessible without the declared authentication |
| I7 | CSP is enforcing, not report-only |
| I8 | An unattested deep link is rejected |
| I9 | Push payloads carry no sensitive content |

I1 is the invariant that matters most and the one most often missed: masking is usually verified on
the primary response only, and leaks through logs and error payloads. Enumerate every egress path
explicitly in the document.

**Profiles:** `baseline`, `load`, `stress`, `soak`. `stress` exists here for a specific reason: to
locate the **HSM throughput ceiling**, a hard capacity constraint that appears only under load and
cannot be extrapolated from a smaller environment — cross-link TST-005's non-extrapolable list.

**Harness focus (JMeter):** assertions that query the log and trace backends for the synthetic
sensitive value after the run — the absence assertion, which is the only way to prove I1; a
high-volume tokenisation run for I2 collision detection across a large synthetic keyspace; a
crypto-throughput step-ramp for I4.

**Tool fit:** JMeter `BEST` (post-run egress assertions plus HSM-backed crypto throughput);
k6 `good`; Locust `good`; Gatling + Karate `fair`.

**Overlays:** Security — the body of the document. Data-quality — token collision detection over the
synthetic keyspace. Omit resilience and contract overlays.

**Compliance:** Ring 0 — OWASP ASVS V6 (stored cryptography) and V8 (data protection); NIST SP
800-53 SC-28 (protection of information at rest). Ring 1 — PCI-DSS 4.0 §3 (protect stored account
data) and §10 (audit trails); GDPR Art. 32. Ring 2 — Decree 13/2023 personal-data protection, `⚠️`.

**Review:** `@infosec-architect` must review before this document moves to `Approved`, as in Task 43.

**Steps:** Standard Archetype Steps, `FILE=…/data-protection-masking-tokenisation.md`, `ID=TST-041`.
`primary_tool: jmeter` for all eight coverage rows.

---

## Task 45: Wave F Catalog Registration

**Files:** Modify `_catalog-inventory.yml`, `enterprise-architecture-catalog.md`, `mkdocs.yml`,
`knowledge-base/testing/README.md`.

**Interfaces:**
- Consumes: the two archetypes from Tasks 43–44.
- Produces: catalog total 231, `testing` category count 39 — the final counts.

- [ ] **Step 1: Append two inventory rows and two catalog table rows**

`TST-040` and `TST-041`, `notes: Wave 15F — security archetypes (InfoSec reviewed)`,
`target_wave: 4`, `owner: qe-lead`. Both documents must read `Status: Approved` by this point, with
`@infosec-architect` review complete.

- [ ] **Step 2: Update the counts to their final values**

Coverage sentence → `Coverage: 231 Approved catalog rows across 17 categories — 7 spine docs and 224 radii docs after Wave 15F security archetypes.`
Category table: `testing` 37 → 39; `**Total**` 229 → 231.

- [ ] **Step 3: Add the two nav entries and complete the README archetype index**

All 24 archetype rows in the README index should now point at existing files.

- [ ] **Step 4: Run every gate**

```bash
python3 scripts/audit-catalog-consistency.py --check-doc-status; echo "audit exit=$?"
python3 scripts/validate-internal-links.py; echo "links exit=$?"
python3 scripts/check-compliance-rows.py; echo "compliance exit=$?"
grep -c '^| [A-Z]\+-[0-9]\+ |' governance/standards/enterprise-architecture-catalog.md
grep -c '^- id: TST-' governance/standards/_catalog-inventory.yml
ls knowledge-base/testing/archetypes/*.md | wc -l
```

Expected: the three gates exit `0`; catalog row count `231`; `TST-` inventory rows `39`; archetype
file count `24`.

- [ ] **Step 5: Commit**

```bash
git add governance/standards/_catalog-inventory.yml \
        governance/standards/enterprise-architecture-catalog.md \
        mkdocs.yml knowledge-base/testing/README.md
git commit -m "chore(catalog): register Wave F security archetype rows

TST-040 and TST-041, both InfoSec-reviewed. Catalog 229 -> 231 rows,
completing the testing category at 39 rows across 17 categories.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Wave G — Coverage Completion

## Task 46: Populate the Coverage File to Every Catalog Row

The coverage file must have one row per **inventory** row. By this point the inventory holds 231
rows, not the 191 baseline — the 39 `TST-*` rows and `TPL-005` are themselves catalog rows and
therefore need coverage rows. They take `governs`.

**Files:**
- Modify: `knowledge-base/testing/coverage/_testing-coverage.yml`
- Modify: `knowledge-base/testing/coverage/coverage-matrix.md` (regenerated, not hand-edited)

**Interfaces:**
- Consumes: every archetype's Applies To list, and the `governs` roster below.
- Produces: a green `validate-testing-coverage.py`, which is the gate Task 47 asserts.

**The `governs` roster — 79 rows, none of which name an archetype:**

| Group | Rows | Count |
|---|---|---|
| NFR spine | `NFR-001`…`NFR-005` | 5 |
| Principles | `PRIN-001`…`PRIN-013` | 13 |
| Templates | `TPL-001`…`TPL-005` | 5 |
| Compliance deep-dives | `COMP-001`…`COMP-008` | 8 |
| Process best practices | `BP-001`, `BP-003`, `BP-006`, `BP-009`, `BP-010`, `BP-011` | 6 |
| Platform meta | `PLT-002`, `PLT-004`, `PLT-007` | 3 |
| Testing corpus itself | `TST-001`…`TST-015`, `TST-020`…`TST-043` | 39 |
| | **Total** | **79** |

Every other row — 231 − 79 = **152 rows** — names at least one archetype. `BP-002`, `BP-004`,
`BP-005`, `BP-007`, and `BP-008` are in the tested set, not the `governs` set, because TST-035 and
TST-042 exercise them.

- [ ] **Step 1: Enumerate what is still missing**

```bash
python3 scripts/validate-testing-coverage.py 2>&1 | grep check1 | sed 's/.*check1 //' | sort
python3 scripts/validate-testing-coverage.py 2>&1 | grep -c check1
```

Expected: a sorted list of every catalog ID still lacking a coverage row. Work from this list — do
not work from memory, and do not stop until the count reaches zero.

- [ ] **Step 2: Add the 79 `governs` rows**

Each takes this shape, with all six disciplines `governs`, empty `archetypes`, empty
`perf_profiles`, and `tiers` copied verbatim from the inventory row:

```yaml
  - catalog_id: PRIN-006
    title: Idempotency-by-default
    path: knowledge-base/principles/idempotency-by-default.md
    tiers: []
    archetypes: []
    disciplines:
      functional: governs
      performance: governs
      resilience: governs
      contract: governs
      security: governs
      data_quality: governs
    perf_profiles: []
    primary_tool: jmeter
    owner: ea-board
    notes: Principle — constrains design; verified via TST-020
```

`primary_tool` is a required field with no meaningful value for a `governs` row; set it to `jmeter`
uniformly and say so in the file's header comment, so a reader does not mistake it for a real
assignment. The `notes` field is the right place to name the archetype that verifies the principle in
practice, as shown.

- [ ] **Step 3: Add the remaining tested rows**

Every row named in an archetype's §1 Applies To, taking `archetypes`, `disciplines`, and
`perf_profiles` from that archetype task's specification. Where a row appears in more than one
archetype, it gets **one** row naming both — a second row fails check 2. The shared rows are:
`INT-014` (TST-020, TST-029); `BSP-019` (TST-025, TST-032); `RES-001` (TST-033, TST-035); `RES-003`
(TST-020, TST-035); `DATA-004` (TST-032, TST-038); `SEC-010` (TST-025, TST-040); `MOB-006` (TST-036,
TST-043); `REF-010` (TST-021, TST-034); `REF-008` (TST-032, TST-034).

Set each row's `disciplines` from the tier obligation matrix in `TST-001` intersected with what the
covering archetypes actually assert. A discipline no archetype covers for that row is `n/a`, not
`required` — an aspirational `required` with no archetype behind it is exactly the false coverage
this file exists to prevent.

- [ ] **Step 4: Drive the gate to green**

```bash
python3 scripts/validate-testing-coverage.py
echo "exit=$?"
```

Expected: exit `0` and `OK: testing coverage is consistent with the catalog inventory`. Iterate
until it is green. Do not weaken a check to achieve this.

- [ ] **Step 5: Regenerate the matrix**

```bash
python3 scripts/render-testing-coverage.py
python3 scripts/render-testing-coverage.py --check
markdownlint knowledge-base/testing/coverage/coverage-matrix.md
grep -c '^| [A-Z]' knowledge-base/testing/coverage/coverage-matrix.md
```

Expected: render succeeds, `--check` then exits `0`, lint exits `0`, and the grep returns `232` —
231 data rows plus the `| Catalog ID |` header row, which the pattern also matches. The legend line
must read `231 rows.`

- [ ] **Step 6: Sanity-check the distribution**

```bash
python3 - <<'PY'
import yaml, collections
rows = yaml.safe_load(open('knowledge-base/testing/coverage/_testing-coverage.yml'))['rows']
print('total rows:', len(rows))
governs = [r for r in rows if all(v == 'governs' for v in r['disciplines'].values())]
print('governs rows:', len(governs), '(expected 79)')
print('tested rows:', len(rows) - len(governs), '(expected 152)')
used = collections.Counter(a for r in rows for a in (r['archetypes'] or []))
print('archetypes referenced:', len(used), '(expected 24)')
for aid in sorted(used):
    print(' ', aid, used[aid])
unref = {'TST-%03d' % n for n in range(20, 44)} - set(used)
print('archetypes with ZERO rows:', sorted(unref) or 'none')
PY
```

Expected: 231 total, 79 `governs`, 152 tested, 24 archetypes referenced, and no archetype with zero
rows. An archetype covering nothing means its rows were never added — find them.

- [ ] **Step 7: Commit**

```bash
git add knowledge-base/testing/coverage/_testing-coverage.yml \
        knowledge-base/testing/coverage/coverage-matrix.md
git commit -m "feat(testing): complete coverage for all 231 catalog rows

152 tested rows mapped to the 24 archetypes; 79 governs rows covering the
NFR spine, principles, templates, compliance deep-dives, process
best-practices, platform meta-docs, and the testing corpus itself.
validate-testing-coverage.py is now green.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 47: Final Gate and Handoff

**Files:**
- Modify: `knowledge-base/testing/README.md` (final index pass)
- Modify: `.bmad/` handoff log, if the repository's BMAD convention requires an entry

**Interfaces:**
- Consumes: everything.
- Produces: a fully green gate set and the branch ready for merge request.

- [ ] **Step 1: Run every gate in the repository**

```bash
python3 scripts/audit-catalog-consistency.py --check-doc-status; echo "audit exit=$?"
python3 scripts/validate-testing-coverage.py; echo "coverage exit=$?"
python3 scripts/render-testing-coverage.py --check; echo "render exit=$?"
python3 scripts/validate-internal-links.py; echo "links exit=$?"
python3 scripts/check-compliance-rows.py; echo "compliance exit=$?"
bash scripts/validate-dab-structure.sh; echo "dab exit=$?"
```

Expected: every one exits `0`. This is the first point at which the coverage gate is green, and
it must stay green from here.

- [ ] **Step 2: Lint and Mermaid-check every new document**

```bash
markdownlint knowledge-base/testing/ knowledge-base/templates/test-archetype-template.md
echo "markdownlint exit=$?"
for f in $(find knowledge-base/testing -name '*.md'); do
  bash scripts/mermaid-lint-doc.sh "$f" >/dev/null || echo "MERMAID FAIL: $f"
done
echo "mermaid sweep complete"
```

Expected: lint exits `0` and the sweep prints no `MERMAID FAIL` lines.

- [ ] **Step 3: Verify the final counts one last time**

```bash
grep -n 'Coverage: 231' governance/standards/enterprise-architecture-catalog.md
grep -c '^| [A-Z]\+-[0-9]\+ |' governance/standards/enterprise-architecture-catalog.md
grep -c '^- id: ' governance/standards/_catalog-inventory.yml
find knowledge-base/testing -name '*.md' | wc -l
ls knowledge-base/testing/archetypes/*.md | wc -l
git diff --stat main --  | tail -1
```

Expected: the coverage sentence matches; both catalog counts are `231`; `knowledge-base/testing`
holds `40` Markdown files (1 README + 9 strategy + 5 tooling + 1 coverage matrix + 24 archetypes;
`_testing-coverage.yml` is not Markdown and is not counted); the archetype count is `24`.

- [ ] **Step 4: Verify the global constraints held**

```bash
grep -rniE '\b(4[0-9]{12}|5[1-5][0-9]{14})\b' knowledge-base/testing/ && echo "PAN-LIKE FOUND" || echo "no PAN-like strings"
grep -rn 'package.json\|pom.xml\|build.gradle\|requirements.txt' knowledge-base/testing/ && echo "BUILD FILE REFERENCE" || echo "no build manifests"
git diff --name-only main -- knowledge-base/patterns/ knowledge-base/nfr/ knowledge-base/reference-architectures/
```

Expected: no PAN-like strings; no build-manifest files created under `knowledge-base/testing/`
(references inside a fenced snippet are fine — inspect any hit rather than assuming a violation);
and the last command prints **nothing**, proving no existing pattern, NFR, or reference-architecture
document was modified.

- [ ] **Step 5: Confirm no threshold values were hard-coded across the corpus**

```bash
grep -rnE '[0-9]+ *(ms|rps|RPS) ' knowledge-base/testing/archetypes/ | grep -v 'test_acceptance_criteria' | head -20
```

Expected: no output, or only matches inside an illustrative YAML block. Any archetype asserting a
service latency or throughput number instead of linking its `NFR-*` row must be corrected.

- [ ] **Step 6: Record the BMAD handoff if the repository requires one**

Check whether `.bmad/` carries a handoff log that prior waves appended to:

```bash
ls .bmad/
grep -rl 'Wave 14' .bmad/ docs/ 2>/dev/null | head
```

If a handoff or wave log exists, append an entry for Wave 15A–15G in the same format the prior
waves used. If none exists, skip this step — do not invent a new logging convention.

- [ ] **Step 7: Push and open a merge request**

```bash
git push -u origin "$(git branch --show-current)"
```

Open a merge request titled `Wave 15: testing knowledge base for IT Quality Engineering`, using the
repository's existing merge-request template from `.gitlab/merge_request_templates/`. In the
description, state: 44 new files, 4 modified; catalog 191 → 231 rows across 17 categories; `TST-001`
added as the 7th spine doc; documentation-only with no CI changes; and that
`validate-testing-coverage.py` is a new gate that will need adding to `.gitlab-ci.yml` by the
platform owner in a follow-up — this plan deliberately does not modify CI.

- [ ] **Step 8: Final commit if Step 6 produced changes**

```bash
git add -A
git commit -m "docs(testing): record Wave 15 testing corpus handoff

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
git push
```

---

## Follow-Ups Deliberately Out Of Scope

Record these in the merge request description rather than doing them here:

1. **Wire `validate-testing-coverage.py` and `render-testing-coverage.py --check` into
   `.gitlab-ci.yml`.** Adding CI jobs is a platform-owner change; the Global Constraints forbid it
   in this plan. Until then the gates are run manually.
2. **Create `.markdownlint.json`.** The blocking `validate:markdown-lint` job references a file that
   does not exist, so that gate is already failing on `main`. Creating it changes lint behaviour
   repository-wide and belongs to the platform owner.
3. **Make test evidence a DAB submission gate.** `TST-001` documents the three cross-block
   invariants tying `test_acceptance_criteria` to `nfr_acceptance_criteria`, but enforcing them over
   `dab/` content changes the DAB process and needs EA Board and DAB chair approval.
4. **`TST-016`…`TST-019` remain unallocated** as headroom for future strategy documents. Do not
   backfill them with archetypes — archetype IDs run `TST-020`…`TST-043`.
