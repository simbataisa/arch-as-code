#!/usr/bin/env python3
"""Phase 4 P4.4 — Verify every Approved/Draft-status row's markdown file
contains a Compliance Mapping section with all 3 rings.

Usage:
    python3 scripts/check-compliance-rows.py [repo_root]

Exit code 0 = all good; non-zero = some files missing rings.
"""
import re
import sys
from pathlib import Path

import yaml

REQUIRED_RINGS = ("Ring 0", "Ring 1", "Ring 2")
GATED_STATUSES = {"Approved", "Draft"}

# Match an H2 heading whose first words are "Compliance Mapping".
HEADING_RE = re.compile(r"^## Compliance Mapping[^\n]*\n", re.MULTILINE)
NEXT_H2_RE = re.compile(r"^## (?!Compliance Mapping)", re.MULTILINE)


def check_doc(path: Path) -> list[str]:
    if not path.exists():
        return [f"FILE MISSING: {path}"]
    text = path.read_text()
    m = HEADING_RE.search(text)
    if not m:
        return ["missing '## Compliance Mapping' heading"]
    after = text[m.end():]
    next_h2 = NEXT_H2_RE.search(after)
    section = after[: next_h2.start()] if next_h2 else after
    return [f"missing '{r}'" for r in REQUIRED_RINGS if r not in section]


def main() -> None:
    repo = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("/Users/dennis.dao/Documents/Arch-As-Code")
    inv_path = repo / "governance/standards/_catalog-inventory.yml"
    inv = yaml.safe_load(inv_path.read_text())

    fails = 0
    checked = 0
    skipped_existing = 0
    for row in inv["rows"]:
        if row["status"] not in GATED_STATUSES:
            continue
        # Existing-22 docs cross-linked only in Wave 0 — full Compliance Mapping
        # backfill is Wave 1+. Their YAML notes contain "Existing — cross-link only".
        notes = (row.get("notes") or "").lower()
        if "existing" in notes and "cross-link" in notes:
            skipped_existing += 1
            continue
        checked += 1
        path = repo / row["path"]
        issues = check_doc(path)
        if issues:
            fails += 1
            print(f"FAIL {row['id']} ({row['path']}): {'; '.join(issues)}")
    print(f"\nDone: checked={checked}, failures={fails}, skipped_existing_cross_link={skipped_existing}")
    sys.exit(0 if fails == 0 else 1)


if __name__ == "__main__":
    main()
