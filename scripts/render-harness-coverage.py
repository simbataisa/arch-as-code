#!/usr/bin/env python3
"""Render the QE harness coverage table into harness-coverage.md.

Offline renderer: it reads only files, never the live SUT or the
CapabilityRegistry's /_capabilities endpoint. Two inputs feed the table:

  1. The archetype catalog (id, family, title) — parsed from
     knowledge-base/testing/README.md's "## Index — Archetypes" section,
     which groups all 24 archetypes into seven families. This is fixed
     corpus: always read from this repository checkout, never from
     --root, matching validate-harness-coverage.py's fixed-corpus split.
  2. qe-harness/traceability/modules.yml — which archetypes the harness
     actually implements, with what tool, module path, coverage level,
     and defect flag. This is harness state: resolved against --root.

harness-coverage.md is a fully generated file (no hand-written narrative
to preserve), so --check compares the whole file verbatim against a fresh
render and exits 1 if it differs (stale), matching
render-testing-coverage.py's --check contract.

Usage:
    python3 scripts/render-harness-coverage.py
    python3 scripts/render-harness-coverage.py --check
    python3 scripts/render-harness-coverage.py --root /path/to/tree
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path
from typing import Any

import yaml

ROOT = Path(__file__).resolve().parent.parent

# Fixed corpus — always resolved against this repository, never --root.
TESTING_README = ROOT / "knowledge-base/testing/README.md"

# Harness locations — resolved against --root (defaults to this repository).
MODULES_REL = Path("qe-harness/traceability/modules.yml")
OUTPUT_REL = Path("qe-harness/traceability/harness-coverage.md")

ARCHETYPE_INDEX_HEADING = "## Index — Archetypes"
FAMILY_HEADING_RE = re.compile(
    r"^### Family (?P<letter>[A-Z]) — (?P<name>.+?)(?: \(landed\))?\s*$"
)
ARCHETYPE_ROW_RE = re.compile(r"^\|\s*(?P<id>TST-\d{3})\s*\|\s*\[(?P<title>[^\]]+)\]")


def load_catalog() -> list[dict[str, str]]:
    """Parse the archetype catalog (id, family, title) out of the testing
    README's grouped archetype index. Rows outside that section (the
    Strategy/Tooling indices, which also use TST-0NN ids) are ignored by
    scoping the scan to between "## Index — Archetypes" and the next
    level-2 heading.
    """
    catalog: list[dict[str, str]] = []
    if not TESTING_README.exists():
        return catalog

    in_index = False
    family_label = ""
    for line in TESTING_README.read_text().splitlines():
        if line.startswith("## "):
            in_index = line.strip() == ARCHETYPE_INDEX_HEADING
            continue
        if not in_index:
            continue

        heading = FAMILY_HEADING_RE.match(line)
        if heading:
            family_label = "%s — %s" % (heading.group("letter"), heading.group("name"))
            continue

        row = ARCHETYPE_ROW_RE.match(line)
        if row:
            catalog.append(
                {
                    "archetype": row.group("id"),
                    "family": family_label,
                    "title": row.group("title"),
                }
            )

    catalog.sort(key=lambda entry: entry["archetype"])
    return catalog


def load_modules(path: Path) -> dict[str, dict[str, Any]]:
    """Load modules.yml, keyed by archetype. Absence means no modules
    implemented yet — a legitimate state, not malformed input."""
    if not path.exists():
        return {}
    data = yaml.safe_load(path.read_text()) or {}
    modules = data.get("modules") or []
    by_archetype: dict[str, dict[str, Any]] = {}
    for module in modules:
        if isinstance(module, dict) and module.get("archetype"):
            by_archetype[module["archetype"]] = module
    return by_archetype


def coverage_cell(module: dict[str, Any]) -> str:
    """The Coverage cell. Partial rows carry their partial_reason inline,
    right in the cell, so nobody reading the table mistakes a partial
    module for full coverage."""
    coverage = str(module.get("coverage") or "").strip() or "?"
    if coverage == "partial":
        reason = " ".join(str(module.get("partial_reason") or "").split())
        if reason:
            return "partial — %s" % reason
    return coverage


def escape(cell: str) -> str:
    return cell.replace("|", "\\|")


def render(catalog: list[dict[str, str]], modules: dict[str, dict[str, Any]]) -> str:
    header = ["Archetype", "Family", "Tool", "Module", "Coverage", "Defect Flag"]
    lines = ["| " + " | ".join(header) + " |", "|" + "---|" * len(header)]

    implemented = 0
    partial = 0
    for entry in catalog:
        module = modules.get(entry["archetype"])
        if module is not None:
            implemented += 1
            if module.get("coverage") == "partial":
                partial += 1

        if module is not None:
            cells = [
                entry["archetype"],
                entry["family"],
                str(module.get("tool", "—")),
                str(module.get("path", "—")),
                coverage_cell(module),
                str(module.get("defect_flag", "—")),
            ]
        else:
            cells = [entry["archetype"], entry["family"], "—", "—", "declared", "—"]

        lines.append("| " + " | ".join(escape(cell) for cell in cells) + " |")

    declared = len(catalog) - implemented
    lines.append("")
    lines.append(
        "%d of %d archetypes implemented · %d declared · %d partial"
        % (implemented, len(catalog), declared, partial)
    )
    lines.append("")
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="exit non-zero if the committed table is stale",
    )
    parser.add_argument(
        "--root",
        type=Path,
        default=ROOT,
        help="qe-harness tree to render against (default: this repository)",
    )
    args = parser.parse_args()

    harness_root = args.root.resolve()
    catalog = load_catalog()
    modules = load_modules(harness_root / MODULES_REL)
    content = render(catalog, modules)

    output_path = harness_root / OUTPUT_REL

    if args.check:
        if not output_path.exists() or output_path.read_text() != content:
            print(
                "FAIL: %s is stale — run render-harness-coverage.py"
                % output_path
            )
            return 1
        print("OK: %s matches modules.yml" % output_path)
        return 0

    if output_path.exists() and output_path.read_text() == content:
        print("OK: %s already current" % output_path)
        return 0

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(content)
    print("Updated %s" % output_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
