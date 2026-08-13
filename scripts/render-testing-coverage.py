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
