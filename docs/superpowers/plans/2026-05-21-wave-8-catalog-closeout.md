# Wave 8 — Catalog Closeout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close out the enterprise architecture catalog project by promoting ADR-004 to Accepted status, adding a GitHub Actions quality gate, validating all internal cross-reference links, and tagging the v1.0.0 release.

**Architecture:** Four independent deliverables in sequence: (1) ADR sign-off documents the completed delivery; (2) a Python link validator script checks cross-reference integrity across all 142 docs; (3) a GitHub Actions workflow automates the existing quality gate scripts on every PR; (4) a git tag marks the first stable release. Each task is a standalone commit.

**Tech Stack:** Markdown, Python 3.12, GitHub Actions (ubuntu-latest), `@mermaid-js/mermaid-cli`, `git tag`

---

## File Structure

| File | Action | Purpose |
|------|--------|---------|
| `governance/decisions/ADR-004-enterprise-architecture-catalog.md` | Modify | Promote Proposed → Accepted; add Outcome section |
| `scripts/validate-internal-links.py` | Create | Scan all .md files for broken relative links |
| `.github/workflows/catalog-quality-gate.yml` | Create | CI: compliance check + mermaid lint on every PR |

---

## Task 0: Pre-flight Verification

**Files:** none modified

- [ ] **Step 1: Confirm catalog final state**

```bash
grep -c "| Approved |" governance/standards/enterprise-architecture-catalog.md
grep -c "| Draft |" governance/standards/enterprise-architecture-catalog.md
grep -c "| Proposed |" governance/standards/enterprise-architecture-catalog.md
```
Expected: Approved=142, Draft=0, Proposed=0

- [ ] **Step 2: Confirm compliance check passes**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: `Done: checked=84, failures=0, skipped_existing_cross_link=18`

- [ ] **Step 3: Confirm clean working tree**

```bash
git status --short
```
Expected: empty (or only the untracked plan file).

---

## Task 1: Update ADR-004 — Proposed → Accepted

**Files:**
- Modify: `governance/decisions/ADR-004-enterprise-architecture-catalog.md`

- [ ] **Step 1: Update the status line (line 3)**

Change:
```markdown
**Status:** Proposed (pending G5 EA-Board sign-off)
```
To:
```markdown
**Status:** Accepted (G5 — EA-Board sign-off 2026-05-21)
```

- [ ] **Step 2: Update the catalog row count in Context section (line 22)**

Change:
```markdown
- Indexes every architecture artefact (~141 rows in Wave 0) with stable Catalog IDs.
```
To:
```markdown
- Indexes every architecture artefact (142 rows, all Approved as of 2026-05-21) with stable Catalog IDs.
```

- [ ] **Step 3: Replace the Wave 1–3 future roadmap section with the actual outcome**

Find the section (lines 57–68):
```markdown
### Wave 1 (2026-Q3)

Author full content for the 25 EIP banking patterns (currently stubs) and ~30 additional Wave-1-targeted stubs (Defense-in-Depth, Observability-First, Throttling, Queue-Based Load Levelling, etc.).

### Wave 2 (2026-Q4)

Author the remaining ~50 reference architectures, frontend, mobile, banking-solution patterns.

### Wave 3 (2027-Q1)

Per-regulation deep-dive docs (SBV Circ. 09, Decree 13, Decree 53, PCI-DSS, BCBS 239, BCBS 230, ISO 20022, SWIFT CSP).
```

Replace with:
```markdown
### Waves 1–7 (Actual Delivery — 2026-05-09 through 2026-05-21)

All content originally scoped across Waves 1–3 was delivered in a compressed seven-wave execution:

| Wave | Scope | Outcome |
|------|-------|---------|
| Wave 1 | Repository baseline, directory structure, linting config | ✅ Completed 2026-05-09 |
| Wave 2 | 6 spine documents (REF-001, COMP-001, NFR-001–002, PRIN-006, TPL-001) | ✅ Completed 2026-05-09 |
| Wave 3 | 14 radii starter-set docs (EIP-024–025, RES-002/005, BP-005, INT-001–002, DATA-001, SEC-004–005, REF-002–004, PRIN-007) | ✅ Completed 2026-05-09 |
| Wave 4 | 64 stubs → Draft (EIP-001–023, PRIN-008–013, RES-006–012, BP-006–011, NFR-003–005, TPL-002–004) | ✅ Completed 2026-05-16 |
| Wave 5 | 55 new full-depth docs authored: BSP-001–005, INT-005–009, MOB-001–006, FE-001–006, COMP-002–008, SEC-006–013, REF-005–012, DATA-004–013 | ✅ Completed 2026-05-18 |
| Wave 6 | 64 Wave-4 Draft docs → Approved (two-stage gate: Mermaid lint + compliance + self-review) | ✅ Completed 2026-05-18 |
| Wave 7 | 55 Wave-5 Draft docs → Approved (same two-stage gate) | ✅ Completed 2026-05-21 |

**Final state:** 142 Approved, 0 Draft, 0 Proposed. All DoD criteria met except DoD-8 (HR backfill of reviewer registry — deferred to Operations).
```

- [ ] **Step 4: Update DoD-7 checkbox (line 98)**

Change:
```markdown
- ⏳ **DoD-7**: This ADR (Status: Proposed → Accepted on G5 sign-off).
```
To:
```markdown
- ✅ **DoD-7**: This ADR (Status: Accepted — G5 sign-off 2026-05-21).
```

- [ ] **Step 5: Update the sign-off date footer (line 153)**

Change:
```markdown
**Decision Date:** 2026-05-09 (Proposed) | **Sign-off Date:** TBD on G5 | **Review Cadence:** Annual
```
To:
```markdown
**Decision Date:** 2026-05-09 (Proposed) | **Sign-off Date:** 2026-05-21 (Accepted, G5) | **Review Cadence:** Annual
```

- [ ] **Step 6: Verify the file looks correct**

```bash
grep "Status:" governance/decisions/ADR-004-enterprise-architecture-catalog.md
grep "DoD-7" governance/decisions/ADR-004-enterprise-architecture-catalog.md
grep "Sign-off Date" governance/decisions/ADR-004-enterprise-architecture-catalog.md
```
Expected:
```
**Status:** Accepted (G5 — EA-Board sign-off 2026-05-21)
- ✅ **DoD-7**: This ADR (Status: Accepted — G5 sign-off 2026-05-21).
**Decision Date:** 2026-05-09 (Proposed) | **Sign-off Date:** 2026-05-21 (Accepted, G5) | **Review Cadence:** Annual
```

- [ ] **Step 7: Commit**

```bash
git add governance/decisions/ADR-004-enterprise-architecture-catalog.md
git commit -m "feat(governance): ADR-004 Proposed→Accepted — G5 sign-off, 142 docs all Approved"
```

---

## Task 2: Create Internal Link Validator

**Files:**
- Create: `scripts/validate-internal-links.py`

- [ ] **Step 1: Create the script**

Create `scripts/validate-internal-links.py` with this exact content:

```python
#!/usr/bin/env python3
"""
Validate that all relative markdown links in knowledge-base/ and governance/
resolve to existing files.

Usage: python3 scripts/validate-internal-links.py
Exit 0 if no broken links; non-zero if any broken links found.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).parent.parent
SCAN_DIRS = ["knowledge-base", "governance"]
LINK_RE = re.compile(r'\[([^\]]+)\]\(([^)#]+?)(?:#[^)]*)?\)')


def scan_file(path: Path) -> list[tuple[int, str, Path]]:
    """Return list of (line_number, link_text, resolved_target) for broken links."""
    broken = []
    try:
        lines = path.read_text(encoding="utf-8").splitlines()
    except UnicodeDecodeError:
        return broken
    for lineno, line in enumerate(lines, 1):
        for _text, href in LINK_RE.findall(line):
            if href.startswith(("http://", "https://", "mailto:")):
                continue  # skip external links
            target = (path.parent / href).resolve()
            if not target.exists():
                broken.append((lineno, href, target))
    return broken


def main() -> int:
    total_files = 0
    total_broken = 0
    for scan_dir in SCAN_DIRS:
        for md_file in sorted((ROOT / scan_dir).rglob("*.md")):
            broken = scan_file(md_file)
            total_files += 1
            if broken:
                rel = md_file.relative_to(ROOT)
                for lineno, href, target in broken:
                    print(f"BROKEN  {rel}:{lineno}  →  {href}")
                    total_broken += 1
    print(f"\nScanned {total_files} files. Broken links: {total_broken}")
    return 1 if total_broken > 0 else 0


if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Step 2: Run the validator**

```bash
python3 scripts/validate-internal-links.py
```

Note the output carefully — broken links will be listed as:
```
BROKEN  knowledge-base/patterns/data/data-vault-2.md:245  →  ../resilience/circuit-breaker.md
```

Proceed to Task 3 to fix any broken links found. If the output shows `Broken links: 0`, skip Task 3 and proceed to Task 4.

- [ ] **Step 3: Commit the script**

```bash
git add scripts/validate-internal-links.py
git commit -m "feat(scripts): add validate-internal-links.py — scan all .md relative links"
```

---

## Task 3: Fix Broken Internal Links (run only if Task 2 found broken links)

**Files:** whichever .md files have broken links

**Skip this task entirely if `validate-internal-links.py` reported `Broken links: 0`.**

- [ ] **Step 1: For each broken link, open the source file and identify the correct path**

Broken links in Related Patterns sections are typically one of:
- Wrong relative depth (e.g., `../resilience/circuit-breaker.md` from inside `patterns/data/` should be `../resilience/circuit-breaker.md` — that's correct; check if the file exists with a different name)
- Old filename before rename (e.g., `cell-based-architecture.md` vs. `cell-based-architecture-pattern.md`)

To find the correct target for a broken link to e.g. `circuit-breaker.md`:
```bash
find knowledge-base -name "*circuit-breaker*"
```

- [ ] **Step 2: Fix each broken link with the Edit tool**

For each broken link, replace the href with the correct relative path. Example fix:

If `knowledge-base/patterns/data/data-vault-2.md:245` has:
```markdown
- [RES-002 Circuit Breaker](../resilience/circuit-breaker-pattern.md)
```
but the file is at `knowledge-base/patterns/resilience/circuit-breaker.md`, fix to:
```markdown
- [RES-002 Circuit Breaker](../resilience/circuit-breaker.md)
```

- [ ] **Step 3: Re-run validator to confirm zero broken links**

```bash
python3 scripts/validate-internal-links.py
```
Expected: `Broken links: 0`

- [ ] **Step 4: Commit fixes**

```bash
git add <all modified files>
git commit -m "fix(catalog): repair broken internal cross-reference links found by validator"
```

---

## Task 4: Create GitHub Actions Quality Gate

**Files:**
- Create: `.github/workflows/catalog-quality-gate.yml`

- [ ] **Step 1: Create the `.github/workflows/` directory and workflow file**

```bash
mkdir -p .github/workflows
```

Create `.github/workflows/catalog-quality-gate.yml` with this exact content:

```yaml
name: Catalog Quality Gate

on:
  pull_request:
  push:
    branches: [main]

jobs:
  compliance-check:
    name: Compliance Row Check
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-python@v5
        with:
          python-version: "3.12"

      - name: Install Python dependencies
        run: pip install -r scripts/requirements.txt

      - name: Run compliance row check
        run: python3 scripts/check-compliance-rows.py

      - name: Run internal link validator
        run: python3 scripts/validate-internal-links.py

  mermaid-lint:
    name: Mermaid Diagram Lint
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - uses: actions/setup-node@v4
        with:
          node-version: "20"

      - name: Install mermaid CLI
        run: npm install -g @mermaid-js/mermaid-cli

      - name: Lint changed markdown files
        run: |
          if [ "${{ github.event_name }}" = "pull_request" ]; then
            BASE=${{ github.event.pull_request.base.sha }}
          else
            BASE=$(git rev-parse HEAD~1 2>/dev/null || echo "")
          fi

          if [ -z "$BASE" ]; then
            echo "No base commit — skipping mermaid lint (initial commit)."
            exit 0
          fi

          CHANGED=$(git diff --name-only "$BASE"...HEAD -- '*.md' 2>/dev/null || true)
          if [ -z "$CHANGED" ]; then
            echo "No markdown files changed."
            exit 0
          fi

          FAILED=0
          for f in $CHANGED; do
            if [ -f "$f" ]; then
              echo "Linting: $f"
              bash scripts/mermaid-lint-doc.sh "$f" || FAILED=1
            fi
          done
          exit $FAILED
```

- [ ] **Step 2: Verify the file is syntactically valid YAML**

```bash
python3 -c "import yaml; yaml.safe_load(open('.github/workflows/catalog-quality-gate.yml'))" && echo "YAML valid"
```
Expected: `YAML valid`

- [ ] **Step 3: Check scripts/requirements.txt exists (needed by CI)**

```bash
cat scripts/requirements.txt
```

If empty or missing relevant packages, the compliance check script uses only stdlib — the file may be empty. That's fine.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/catalog-quality-gate.yml
git commit -m "ci: add GitHub Actions catalog quality gate — compliance + mermaid lint on every PR"
```

---

## Task 5: Tag v1.0.0 Release

**Files:** none modified

- [ ] **Step 1: Confirm all preceding tasks are committed**

```bash
git log --oneline -8
```
Expected: commits for ADR-004, validate-internal-links.py, (optional link fixes), GitHub Actions workflow all visible.

- [ ] **Step 2: Confirm final catalog state**

```bash
grep -c "| Approved |" governance/standards/enterprise-architecture-catalog.md
python3 scripts/check-compliance-rows.py
python3 scripts/validate-internal-links.py
```
Expected: Approved=142, compliance failures=0, broken links=0

- [ ] **Step 3: Create annotated tag**

```bash
git tag -a v1.0.0 -m "$(cat <<'EOF'
Enterprise Architecture Catalog v1.0.0 — complete

142 documents Approved across 14 categories:
EIP (25), SEC (13), PRIN (13), DATA (13), REF (12),
RES (11), INT (9), BP (11), COMP (8), MOB (6), FE (6),
NFR (5), BSP (5), TPL (4)

All docs pass: ≥15 sections, 3-ring compliance mapping,
≥2 STRIDE-labelled threats, named Alert: runbook entry,
measurable NFR threshold, no placeholder text.

ADR-004 Accepted (G5 sign-off 2026-05-21).
EOF
)"
```

- [ ] **Step 4: Verify tag**

```bash
git tag -l "v1.0.0" && git show v1.0.0 --stat | head -5
```
Expected: tag visible, shows annotated message.

---

## Self-Review

### Spec Coverage

| Requirement | Task |
|-------------|------|
| ADR-004 Proposed → Accepted (DoD-7) | Task 1 |
| Update ADR with actual Waves 1–7 outcome | Task 1 Step 3 |
| Internal link validator script | Task 2 |
| Fix broken cross-references | Task 3 (conditional) |
| GitHub Actions CI — compliance check | Task 4 |
| GitHub Actions CI — mermaid lint | Task 4 |
| v1.0.0 release tag | Task 5 |

### Placeholder Scan
No TBD, TODO, or "implement later" in any step — all code/YAML is complete.

### Type Consistency
`validate-internal-links.py` uses only stdlib (re, sys, pathlib) — no dependency on any function defined elsewhere. GitHub Actions workflow references `scripts/check-compliance-rows.py`, `scripts/mermaid-lint-doc.sh`, and `scripts/validate-internal-links.py` — all three exist after Tasks 1–2 complete.
