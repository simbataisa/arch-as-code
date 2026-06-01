# Wave 14 Catalog Source-of-Truth Reconciliation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `_catalog-inventory.yml`, the rendered enterprise architecture catalog table, and catalog document headers agree on the current 191-row Approved catalog, then add an automated audit gate to prevent drift in later waves.

**Architecture:** Treat the current rendered catalog table as the recovery source for display fields because `_catalog-inventory.yml` has duplicate and stale rows. Add read-only audit tooling first, reconcile the YAML inventory, render the catalog from YAML, sync document header statuses, and fix the current broken internal links. The wave is a quality gate and metadata repair wave; it does not author new pattern content.

**Tech Stack:** Markdown, Python 3, PyYAML, Bash, Git, GitNexus MCP

---

## Current State

- `governance/standards/enterprise-architecture-catalog.md` currently has 191 unique catalog rows and all 191 rendered rows are `Approved`.
- `governance/standards/_catalog-inventory.yml` currently has 194 rows: 74 `Approved`, 71 `Draft`, 49 `Proposed`.
- `_catalog-inventory.yml` has 3 duplicate IDs: `INT-010`, `INT-011`, `INT-012`.
- 169 catalog document headers still say `Status: Draft` even though the catalog table marks their rows as `Approved`.
- `python3 scripts/validate-internal-links.py` currently fails on five Basel compliance links that use `bcbs-239.md` or `bcbs-230.md` instead of the actual file names.

## File Structure

- Create: `scripts/audit-catalog-consistency.py`
  - Read-only audit gate for duplicate inventory IDs, inventory/table mismatches, missing files, missing catalog IDs, and optional document header status drift.
- Create: `scripts/reconcile-catalog-inventory.py`
  - One-purpose reconciliation utility that rebuilds unique inventory rows from the current rendered table while preserving the richest existing YAML metadata.
- Create: `scripts/sync-catalog-doc-status.py`
  - Mechanical status-header sync utility that updates the first `Status:` line in catalog docs from the inventory row status.
- Modify: `governance/standards/_catalog-inventory.yml`
  - Remove duplicate rows and update all row statuses to match the current rendered catalog table.
- Modify: `governance/standards/enterprise-architecture-catalog.md`
  - Re-render §4 from `_catalog-inventory.yml`.
- Modify: 169 files under `knowledge-base/`
  - Change the first status header from `Draft` to `Approved`; keep all other content unchanged.
- Modify: five link-fix files:
  - `knowledge-base/patterns/observability/error-budget-burn-rate.md`
  - `knowledge-base/patterns/observability/synthetic-monitoring-canary.md`
  - `knowledge-base/patterns/observability/tracing-sampling-strategy.md`
  - `knowledge-base/patterns/platform/finops-cost-allocation.md`
  - `knowledge-base/patterns/platform/gitops-deployment-pipeline.md`
- Modify: `.bmad/handoff-log.md`
  - Record Wave 14 completion, changed files, and verification results.

---

## Task 0: Pre-Flight Snapshot

**Files:**
- Read: `governance/standards/_catalog-inventory.yml`
- Read: `governance/standards/enterprise-architecture-catalog.md`
- Read: `knowledge-base/**/*.md`
- Commit: `docs/superpowers/plans/2026-05-30-wave-14-catalog-reconciliation.md`

- [ ] **Step 1: Confirm worktree state**

Run:

```bash
git status --short --branch
```

Expected: no unexpected tracked changes in catalog, script, or knowledge-base files. Untracked local agent instruction files such as `AGENTS.md` and `CLAUDE.md` may be present; do not add or modify them for this wave.

- [ ] **Step 2: Capture the current catalog drift**

Run:

```bash
python3 - <<'EOF'
import re
import yaml
from collections import Counter

inventory = yaml.safe_load(open("governance/standards/_catalog-inventory.yml"))
rows = inventory["rows"]
ids = [row["id"] for row in rows]
markdown = open("governance/standards/enterprise-architecture-catalog.md").read()
markdown_ids = re.findall(r"^\|\s*([A-Z]+-\d{3})\s*\|", markdown, re.MULTILINE)
markdown_statuses = re.findall(
    r"^\|\s*[A-Z]+-\d{3}\s*\|[^|]*\|[^|]*\|\s*([^|]+?)\s*\|",
    markdown,
    re.MULTILINE,
)

print("inventory_total", len(rows))
print("inventory_statuses", dict(sorted(Counter(row["status"] for row in rows).items())))
print("inventory_duplicate_ids", sorted(k for k, v in Counter(ids).items() if v > 1))
print("markdown_total", len(markdown_ids))
print("markdown_duplicate_ids", sorted(k for k, v in Counter(markdown_ids).items() if v > 1))
print("markdown_statuses", dict(sorted(Counter(s.strip() for s in markdown_statuses).items())))
EOF
```

Expected:

```text
inventory_total 194
inventory_statuses {'Approved': 74, 'Draft': 71, 'Proposed': 49}
inventory_duplicate_ids ['INT-010', 'INT-011', 'INT-012']
markdown_total 191
markdown_duplicate_ids []
markdown_statuses {'Approved': 191}
```

- [ ] **Step 3: Commit the plan**

Before committing, run GitNexus change detection:

```text
gitnexus_detect_changes(scope: "all", repo: "arch-as-code")
```

Expected: changed symbol scope is empty or limited to markdown documentation. No execution flows should be affected.

Run:

```bash
git add docs/superpowers/plans/2026-05-30-wave-14-catalog-reconciliation.md
git commit -m "docs(plans): add Wave 14 catalog reconciliation plan"
```

Expected: one docs-only commit.

---

## Task 1: Add Catalog Consistency Audit Script

**Files:**
- Create: `scripts/audit-catalog-consistency.py`

- [ ] **Step 1: Create the audit script**

Write `scripts/audit-catalog-consistency.py` with this complete content:

```python
#!/usr/bin/env python3
"""Audit catalog inventory, rendered catalog table, and document headers.

Usage:
    python3 scripts/audit-catalog-consistency.py
    python3 scripts/audit-catalog-consistency.py --check-doc-status
"""

from __future__ import annotations

import argparse
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any

import yaml

ROOT = Path(__file__).resolve().parent.parent
INVENTORY_PATH = ROOT / "governance/standards/_catalog-inventory.yml"
CATALOG_PATH = ROOT / "governance/standards/enterprise-architecture-catalog.md"

TABLE_ROW_RE = re.compile(
    r"^\|\s*(?P<id>[A-Z]+-\d{3})\s*"
    r"\|\s*(?P<title>[^|]+?)\s*"
    r"\|\s*(?P<category>[^|]+?)\s*"
    r"\|\s*(?P<status>[^|]+?)\s*"
    r"\|\s*(?P<spine>[^|]+?)\s*"
    r"\|\s*@?(?P<owner>[^|]+?)\s*"
    r"\|\s*`(?P<path>[^`]+)`\s*\|",
    re.MULTILINE,
)
STATUS_RE = re.compile(r"^Status:\s*(?P<status>[A-Za-z]+)\b", re.MULTILINE)
CATALOG_ID_RE = re.compile(r"(?:^Catalog ID:|Catalog ID:)\s*(?P<id>[A-Z]+-\d{3})\b", re.MULTILINE)


def clean(value: str) -> str:
    return value.strip().replace("\\|", "|")


def load_inventory() -> list[dict[str, Any]]:
    data = yaml.safe_load(INVENTORY_PATH.read_text())
    return data["rows"]


def load_catalog_table() -> dict[str, dict[str, str]]:
    table: dict[str, dict[str, str]] = {}
    duplicates: list[str] = []
    for match in TABLE_ROW_RE.finditer(CATALOG_PATH.read_text()):
        row = {key: clean(value) for key, value in match.groupdict().items()}
        if row["id"] in table:
            duplicates.append(row["id"])
        table[row["id"]] = row
    if duplicates:
        table["__duplicates__"] = {"ids": ", ".join(sorted(duplicates))}
    return table


def parse_doc_header(path: Path) -> tuple[str | None, str | None]:
    text = path.read_text(errors="ignore")
    status_match = STATUS_RE.search(text)
    id_match = CATALOG_ID_RE.search(text)
    status = status_match.group("status") if status_match else None
    catalog_id = id_match.group("id") if id_match else None
    return status, catalog_id


def audit(check_doc_status: bool) -> int:
    issues: list[str] = []
    rows = load_inventory()
    table = load_catalog_table()
    table_duplicates = table.pop("__duplicates__", None)

    row_ids = [row["id"] for row in rows]
    duplicate_ids = sorted(key for key, count in Counter(row_ids).items() if count > 1)
    if duplicate_ids:
        issues.append(f"duplicate inventory IDs: {', '.join(duplicate_ids)}")
    if table_duplicates:
        issues.append(f"duplicate markdown table IDs: {table_duplicates['ids']}")

    inventory_by_id = {row["id"]: row for row in rows}
    inventory_ids = set(inventory_by_id)
    table_ids = set(table)

    for missing in sorted(table_ids - inventory_ids):
        issues.append(f"catalog table row missing from inventory: {missing}")
    for extra in sorted(inventory_ids - table_ids):
        issues.append(f"inventory row missing from catalog table: {extra}")

    missing_paths = 0
    mismatches = 0
    doc_status_checked = 0

    for catalog_id in sorted(table_ids & inventory_ids):
        inv = inventory_by_id[catalog_id]
        rendered = table[catalog_id]
        for field in ("category", "status", "path"):
            left = str(inv.get(field, "")).strip()
            right = rendered[field].strip()
            if left != right:
                mismatches += 1
                issues.append(f"{catalog_id} {field} mismatch: inventory={left!r} table={right!r}")

        doc_path = ROOT / inv["path"]
        if not doc_path.exists():
            missing_paths += 1
            issues.append(f"{catalog_id} file missing: {inv['path']}")
            continue

        doc_status, doc_catalog_id = parse_doc_header(doc_path)
        if doc_catalog_id != catalog_id:
            mismatches += 1
            issues.append(f"{catalog_id} document header catalog ID mismatch: found={doc_catalog_id!r}")
        if check_doc_status:
            doc_status_checked += 1
            if doc_status != inv["status"]:
                mismatches += 1
                issues.append(f"{catalog_id} document status mismatch: doc={doc_status!r} inventory={inv['status']!r}")

    if issues:
        for issue in issues:
            print(f"FAIL {issue}")
        print()
        print("FAIL catalog consistency")
        print(
            f"inventory_rows={len(rows)} markdown_rows={len(table)} "
            f"duplicate_ids={len(duplicate_ids)} missing_paths={missing_paths} "
            f"mismatches={mismatches} doc_status_checked={doc_status_checked}"
        )
        return 1

    print("PASS catalog consistency")
    print(
        f"inventory_rows={len(rows)} markdown_rows={len(table)} "
        f"duplicate_ids=0 missing_paths=0 mismatches=0 "
        f"doc_status_checked={doc_status_checked}"
    )
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--check-doc-status",
        action="store_true",
        help="Also require each document Status header to match the inventory row status.",
    )
    args = parser.parse_args()
    return audit(check_doc_status=args.check_doc_status)


if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Step 2: Run the new audit and verify it fails on the known drift**

Run:

```bash
python3 scripts/audit-catalog-consistency.py
```

Expected: exit code `1`, with failures including:

```text
FAIL duplicate inventory IDs: INT-010, INT-011, INT-012
FAIL catalog consistency
```

- [ ] **Step 3: Commit the audit script**

Before committing, run GitNexus change detection:

```text
gitnexus_detect_changes(scope: "all", repo: "arch-as-code")
```

Expected: changed symbols limited to new script functions in `scripts/audit-catalog-consistency.py`; no existing execution flow is affected.

Run:

```bash
git add scripts/audit-catalog-consistency.py
git commit -m "test(catalog): add catalog consistency audit"
```

---

## Task 2: Reconcile Inventory YAML from the Rendered Catalog Table

**Files:**
- Create: `scripts/reconcile-catalog-inventory.py`
- Modify: `governance/standards/_catalog-inventory.yml`
- Modify: `governance/standards/enterprise-architecture-catalog.md`
- Modify: `knowledge-base/templates/stub-doc-template.md`

- [ ] **Step 1: Create the inventory reconciliation script**

Write `scripts/reconcile-catalog-inventory.py` with this complete content:

```python
#!/usr/bin/env python3
"""Rebuild unique inventory rows from the rendered catalog table.

This is a controlled recovery utility for Wave 14. It uses the rendered table
for display fields and status, while preserving richer YAML metadata such as
ring-specific compliance references from the existing inventory when present.
"""

from __future__ import annotations

import argparse
import datetime as dt
import re
from collections import defaultdict
from pathlib import Path
from typing import Any

import yaml

ROOT = Path(__file__).resolve().parent.parent
INVENTORY_PATH = ROOT / "governance/standards/_catalog-inventory.yml"
CATALOG_PATH = ROOT / "governance/standards/enterprise-architecture-catalog.md"

TABLE_ROW_RE = re.compile(
    r"^\|\s*(?P<id>[A-Z]+-\d{3})\s*"
    r"\|\s*(?P<title>[^|]+?)\s*"
    r"\|\s*(?P<category>[^|]+?)\s*"
    r"\|\s*(?P<status>[^|]+?)\s*"
    r"\|\s*(?P<spine_or_radii>[^|]+?)\s*"
    r"\|\s*@?(?P<owner>[^|]+?)\s*"
    r"\|\s*`(?P<path>[^`]+)`\s*"
    r"\|\s*(?P<tiers>[^|]+?)\s*"
    r"\|\s*(?P<compliance>[^|]+?)\s*"
    r"\|\s*(?P<last_reviewed>[^|]+?)\s*"
    r"\|\s*(?P<target_wave>[^|]+?)\s*"
    r"\|\s*(?P<notes>[^|]+?)\s*\|",
    re.MULTILINE,
)


def clean(value: str) -> str:
    return value.strip().replace("\\|", "|")


def parse_tiers(value: str) -> list[str]:
    value = clean(value)
    if value in {"", "—", "-"}:
        return []
    return [part.strip() for part in value.split(",") if part.strip()]


def parse_wave(value: str) -> int | str:
    value = clean(value)
    return int(value) if value.isdigit() else value


def compliance_score(row: dict[str, Any]) -> int:
    refs = row.get("compliance_refs") or {}
    return sum(len(refs.get(ring) or []) for ring in ("ring0", "ring1", "ring2"))


def row_score(row: dict[str, Any]) -> int:
    score = compliance_score(row) * 10
    for field in ("last_reviewed", "notes", "target_wave", "tiers"):
        if row.get(field):
            score += 1
    return score


def parse_markdown_rows() -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    seen: set[str] = set()
    for match in TABLE_ROW_RE.finditer(CATALOG_PATH.read_text()):
        row = {key: clean(value) for key, value in match.groupdict().items()}
        if row["id"] in seen:
            raise SystemExit(f"duplicate catalog table ID: {row['id']}")
        seen.add(row["id"])
        rows.append(row)
    if not rows:
        raise SystemExit("no catalog table rows found")
    return rows


def fallback_compliance_refs(value: str) -> dict[str, list[str]]:
    value = clean(value)
    if value in {"", "—", "-"}:
        return {"ring0": [], "ring1": [], "ring2": []}
    return {
        "ring0": [part.strip() for part in value.split(";") if part.strip()],
        "ring1": [],
        "ring2": [],
    }


def build_rows(existing_rows: list[dict[str, Any]], markdown_rows: list[dict[str, str]]) -> list[dict[str, Any]]:
    by_id: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in existing_rows:
        by_id[row["id"]].append(row)

    reconciled: list[dict[str, Any]] = []
    for rendered in markdown_rows:
        catalog_id = rendered["id"]
        candidates = by_id.get(catalog_id)
        if not candidates:
            raise SystemExit(f"catalog table row has no inventory source: {catalog_id}")

        base = dict(max(candidates, key=row_score))
        refs = base.get("compliance_refs")
        if not refs or compliance_score(base) == 0:
            refs = fallback_compliance_refs(rendered["compliance"])

        base.update(
            {
                "id": catalog_id,
                "title": rendered["title"],
                "category": rendered["category"],
                "status": rendered["status"],
                "owner": rendered["owner"].lstrip("@"),
                "path": rendered["path"],
                "tiers": parse_tiers(rendered["tiers"]),
                "spine_or_radii": rendered["spine_or_radii"],
                "compliance_refs": refs,
                "last_reviewed": rendered["last_reviewed"],
                "notes": rendered["notes"],
                "target_wave": parse_wave(rendered["target_wave"]),
            }
        )
        reconciled.append(base)
    return reconciled


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--date", default=dt.date.today().isoformat())
    args = parser.parse_args()

    inventory = yaml.safe_load(INVENTORY_PATH.read_text())
    existing_rows = inventory["rows"]
    markdown_rows = parse_markdown_rows()
    new_rows = build_rows(existing_rows, markdown_rows)

    duplicate_removed = len(existing_rows) - len(new_rows)
    inventory["last_updated"] = args.date
    inventory["rows"] = new_rows

    print("Reconciled catalog inventory")
    print(f"source_markdown_rows={len(markdown_rows)}")
    print(f"written_yaml_rows={len(new_rows)}")
    print(f"removed_duplicate_rows={duplicate_removed}")
    print(f"last_updated={args.date}")

    if not args.dry_run:
        INVENTORY_PATH.write_text(yaml.safe_dump(inventory, sort_keys=False, allow_unicode=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
```

- [ ] **Step 2: Dry-run the reconciliation**

Run:

```bash
python3 scripts/reconcile-catalog-inventory.py --dry-run --date 2026-05-30
```

Expected:

```text
Reconciled catalog inventory
source_markdown_rows=191
written_yaml_rows=191
removed_duplicate_rows=3
last_updated=2026-05-30
```

- [ ] **Step 3: Run the reconciliation**

Run:

```bash
python3 scripts/reconcile-catalog-inventory.py --date 2026-05-30
```

Expected: same output as the dry run, and `_catalog-inventory.yml` now has 191 rows with no duplicate IDs.

- [ ] **Step 4: Re-render the catalog table from YAML**

Run:

```bash
python3 scripts/render-catalog-table.py \
  --yaml governance/standards/_catalog-inventory.yml \
  --markdown governance/standards/enterprise-architecture-catalog.md \
  --section 4
```

Expected:

```text
Rendered 191 rows into §4 (Approved=191)
```

- [ ] **Step 5: Repair the TPL-003 document catalog ID header**

The stronger audit validates document `Catalog ID` headers even when status-header enforcement is disabled. `knowledge-base/templates/stub-doc-template.md` currently uses the generic stub value `Catalog ID: [XXX-NNN]`; change only that header to the concrete catalog row ID:

```bash
python3 - <<'EOF'
from pathlib import Path

path = Path("knowledge-base/templates/stub-doc-template.md")
text = path.read_text()
old = "Catalog ID: [XXX-NNN]"
new = "Catalog ID: TPL-003"
if old not in text:
    raise SystemExit(f"expected header not found in {path}")
path.write_text(text.replace(old, new, 1))
EOF
```

Expected: `knowledge-base/templates/stub-doc-template.md` now contains `Catalog ID: TPL-003`.

- [ ] **Step 6: Run the audit without document status enforcement**

Run:

```bash
python3 scripts/audit-catalog-consistency.py
```

Expected:

```text
PASS catalog consistency
inventory_rows=191 markdown_rows=191 duplicate_ids=0 missing_paths=0 mismatches=0 doc_status_checked=0
```

- [ ] **Step 7: Commit inventory reconciliation**

Before committing, run GitNexus change detection:

```text
gitnexus_detect_changes(scope: "all", repo: "arch-as-code")
```

Expected: changed symbols limited to new script functions plus catalog markdown/YAML files and the TPL-003 template header. No application execution flows should be affected.

Run:

```bash
git add scripts/reconcile-catalog-inventory.py governance/standards/_catalog-inventory.yml governance/standards/enterprise-architecture-catalog.md knowledge-base/templates/stub-doc-template.md
git commit -m "chore(catalog): reconcile inventory source of truth"
```

---

## Task 3: Sync Document Header Statuses

**Files:**
- Create: `scripts/sync-catalog-doc-status.py`
- Modify: 169 `knowledge-base/**/*.md` catalog documents whose first status line currently says `Draft`

- [ ] **Step 1: Create the document status sync script**

Write `scripts/sync-catalog-doc-status.py` with this complete content:

```python
#!/usr/bin/env python3
"""Sync the first Status header in each catalog document from inventory rows."""

from __future__ import annotations

import argparse
import re
from pathlib import Path
from typing import Any

import yaml

ROOT = Path(__file__).resolve().parent.parent
INVENTORY_PATH = ROOT / "governance/standards/_catalog-inventory.yml"
STATUS_LINE_RE = re.compile(r"^Status:\s*(?P<status>[A-Za-z]+)(?P<suffix>[^\n]*)", re.MULTILINE)
CATALOG_ID_RE = re.compile(r"(?:^Catalog ID:|Catalog ID:)\s*(?P<id>[A-Z]+-\d{3})\b", re.MULTILINE)


def load_rows() -> list[dict[str, Any]]:
    return yaml.safe_load(INVENTORY_PATH.read_text())["rows"]


def sync_file(path: Path, catalog_id: str, desired_status: str, dry_run: bool) -> bool:
    text = path.read_text()
    status_match = STATUS_LINE_RE.search(text)
    if not status_match:
        raise RuntimeError(f"{catalog_id} has no Status header in {path}")

    current_status = status_match.group("status")
    if current_status == desired_status:
        return False

    suffix = status_match.group("suffix")
    replacement = f"Status: {desired_status}{suffix}"
    updated = text[: status_match.start()] + replacement + text[status_match.end() :]

    if not CATALOG_ID_RE.search(updated):
        line_end = updated.find("\n", status_match.start())
        insertion_point = line_end + 1 if line_end >= 0 else len(updated)
        updated = updated[:insertion_point] + f"Catalog ID: {catalog_id}\n" + updated[insertion_point:]

    if not dry_run:
        path.write_text(updated)
    return True


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    updated = 0
    missing_files: list[str] = []
    for row in load_rows():
        path = ROOT / row["path"]
        if not path.exists():
            missing_files.append(f"{row['id']} {row['path']}")
            continue
        if sync_file(path, row["id"], row["status"], args.dry_run):
            updated += 1

    label = "DRY RUN catalog doc status sync" if args.dry_run else "Updated catalog doc statuses"
    print(label)
    print(f"{'would_update' if args.dry_run else 'updated'}={updated}")
    print(f"missing_files={len(missing_files)}")
    for item in missing_files:
        print(f"MISSING {item}")
    return 1 if missing_files else 0


if __name__ == "__main__":
    raise SystemExit(main())
```

- [ ] **Step 2: Dry-run document status sync**

Run:

```bash
python3 scripts/sync-catalog-doc-status.py --dry-run
```

Expected:

```text
DRY RUN catalog doc status sync
would_update=169
missing_files=0
```

- [ ] **Step 3: Apply document status sync**

Run:

```bash
python3 scripts/sync-catalog-doc-status.py
```

Expected:

```text
Updated catalog doc statuses
updated=169
missing_files=0
```

- [ ] **Step 4: Run strict audit**

Run:

```bash
python3 scripts/audit-catalog-consistency.py --check-doc-status
```

Expected:

```text
PASS catalog consistency
inventory_rows=191 markdown_rows=191 duplicate_ids=0 missing_paths=0 mismatches=0 doc_status_checked=191
```

- [ ] **Step 5: Commit document status sync**

Before committing, run GitNexus change detection:

```text
gitnexus_detect_changes(scope: "all", repo: "arch-as-code")
```

Expected: changed files are the new sync script and first-line metadata changes in catalog documents. No application execution flows should be affected.

Run:

```bash
git add scripts/sync-catalog-doc-status.py knowledge-base
git commit -m "chore(catalog): sync document status headers"
```

---

## Task 4: Fix Current Internal Link Gate Failures

**Files:**
- Modify: `knowledge-base/patterns/observability/error-budget-burn-rate.md`
- Modify: `knowledge-base/patterns/observability/synthetic-monitoring-canary.md`
- Modify: `knowledge-base/patterns/observability/tracing-sampling-strategy.md`
- Modify: `knowledge-base/patterns/platform/finops-cost-allocation.md`
- Modify: `knowledge-base/patterns/platform/gitops-deployment-pipeline.md`

- [ ] **Step 1: Verify the five known broken links**

Run:

```bash
python3 scripts/validate-internal-links.py
```

Expected: exit code `1`, with these five failures:

```text
BROKEN  knowledge-base/patterns/observability/error-budget-burn-rate.md:275  →  ../../compliance/bcbs-239.md
BROKEN  knowledge-base/patterns/observability/synthetic-monitoring-canary.md:280  →  ../../compliance/bcbs-230.md
BROKEN  knowledge-base/patterns/observability/tracing-sampling-strategy.md:297  →  ../../compliance/bcbs-239.md
BROKEN  knowledge-base/patterns/platform/finops-cost-allocation.md:287  →  ../../compliance/bcbs-230.md
BROKEN  knowledge-base/patterns/platform/gitops-deployment-pipeline.md:311  →  ../../compliance/bcbs-230.md
```

- [ ] **Step 2: Replace incorrect Basel link targets**

Edit the five files as follows:

```text
knowledge-base/patterns/observability/error-budget-burn-rate.md
  ../../compliance/bcbs-239.md
  ../../compliance/basel-bcbs-239.md

knowledge-base/patterns/observability/synthetic-monitoring-canary.md
  ../../compliance/bcbs-230.md
  ../../compliance/basel-bcbs-230.md

knowledge-base/patterns/observability/tracing-sampling-strategy.md
  ../../compliance/bcbs-239.md
  ../../compliance/basel-bcbs-239.md

knowledge-base/patterns/platform/finops-cost-allocation.md
  ../../compliance/bcbs-230.md
  ../../compliance/basel-bcbs-230.md

knowledge-base/patterns/platform/gitops-deployment-pipeline.md
  ../../compliance/bcbs-230.md
  ../../compliance/basel-bcbs-230.md
```

- [ ] **Step 3: Verify internal links now pass**

Run:

```bash
python3 scripts/validate-internal-links.py
```

Expected:

```text
Scanned 217 files. Broken links: 0
```

- [ ] **Step 4: Commit link fixes**

Before committing, run GitNexus change detection:

```text
gitnexus_detect_changes(scope: "all", repo: "arch-as-code")
```

Expected: only five markdown documents changed; no code symbols or application execution flows affected.

Run:

```bash
git add knowledge-base/patterns/observability/error-budget-burn-rate.md \
  knowledge-base/patterns/observability/synthetic-monitoring-canary.md \
  knowledge-base/patterns/observability/tracing-sampling-strategy.md \
  knowledge-base/patterns/platform/finops-cost-allocation.md \
  knowledge-base/patterns/platform/gitops-deployment-pipeline.md
git commit -m "fix(catalog): repair Basel compliance links"
```

---

## Task 5: Final Gate and BMAD Handoff

**Files:**
- Modify: `.bmad/handoff-log.md`

- [ ] **Step 1: Run full catalog consistency audit**

Run:

```bash
python3 scripts/audit-catalog-consistency.py --check-doc-status
```

Expected:

```text
PASS catalog consistency
inventory_rows=191 markdown_rows=191 duplicate_ids=0 missing_paths=0 mismatches=0 doc_status_checked=191
```

- [ ] **Step 2: Run compliance mapping gate**

Run:

```bash
python3 scripts/check-compliance-rows.py
```

Expected:

```text
Done: checked=173, failures=0, skipped_existing_cross_link=18
```

- [ ] **Step 3: Run internal link gate**

Run:

```bash
python3 scripts/validate-internal-links.py
```

Expected:

```text
Scanned 217 files. Broken links: 0
```

- [ ] **Step 4: Verify rendered catalog counts**

Run:

```bash
python3 - <<'EOF'
import re
markdown = open("governance/standards/enterprise-architecture-catalog.md").read()
ids = re.findall(r"^\|\s*([A-Z]+-\d{3})\s*\|", markdown, re.MULTILINE)
approved = len(re.findall(r"^\|\s*[A-Z]+-\d{3}\s*\|[^|]*\|[^|]*\|\s*Approved\s*\|", markdown, re.MULTILINE))
print(f"rows={len(ids)} approved={approved} unique={len(set(ids))}")
EOF
```

Expected:

```text
rows=191 approved=191 unique=191
```

- [ ] **Step 5: Append BMAD handoff log entry**

Append this row to `.bmad/handoff-log.md`:

```markdown
| 2026-05-30 | tech-lead | Wave 14 catalog reconciliation — added catalog consistency audit tooling, reconciled `_catalog-inventory.yml` to 191 unique Approved rows, rendered the catalog table from YAML, synced 169 document status headers, fixed 5 Basel compliance links; catalog audit, compliance gate, and internal-link gate passed |
```

- [ ] **Step 6: Commit BMAD handoff**

Before committing, run GitNexus change detection:

```text
gitnexus_detect_changes(scope: "all", repo: "arch-as-code")
```

Expected: `.bmad/handoff-log.md` changed plus already committed Wave 14 changes; no unexpected execution flows affected.

Run:

```bash
git add .bmad/handoff-log.md
git commit -m "docs(bmad): record Wave 14 catalog reconciliation"
```

- [ ] **Step 7: Final git summary**

Run:

```bash
git status --short --branch
git log --oneline -5
```

Expected: only pre-existing unrelated untracked local files remain, and the last commits are the Wave 14 plan, audit, reconciliation, status sync, link fix, and handoff commits.

---

## Self-Review Notes

- **Spec coverage:** The plan covers the discovered Wave 14 work: inventory duplicates, YAML/table status drift, document-header status drift, broken internal links, and final gates.
- **Unresolved markers:** The plan contains concrete commands, exact file paths, full script content, and explicit expected outputs.
- **Type consistency:** Script names, function names, CLI flags, and expected audit output are consistent across tasks.
