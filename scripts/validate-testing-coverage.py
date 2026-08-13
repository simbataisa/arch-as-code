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
