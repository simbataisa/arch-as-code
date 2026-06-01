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
    r"\|\s*(?P<title>[^|]*?)\s*"
    r"\|\s*(?P<category>[^|]*?)\s*"
    r"\|\s*(?P<status>[^|]*?)\s*"
    r"\|\s*(?P<spine_or_radii>[^|]*?)\s*"
    r"\|\s*@?(?P<owner>[^|]*?)\s*"
    r"\|\s*`(?P<path>[^`]+)`\s*"
    r"\|\s*(?P<tiers>[^|]*?)\s*"
    r"\|\s*(?P<compliance>[^|]*?)\s*"
    r"\|\s*(?P<last_reviewed>[^|]*?)\s*"
    r"\|\s*(?P<target_wave>[^|]*?)\s*"
    r"\|\s*(?P<notes>[^|]*?)\s*\|",
    re.MULTILINE,
)
TABLE_ID_RE = re.compile(r"^\|\s*[A-Z]+-\d{3}\s*\|", re.MULTILINE)


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
    markdown = CATALOG_PATH.read_text()
    expected_count = len(TABLE_ID_RE.findall(markdown))
    rows: list[dict[str, str]] = []
    seen: set[str] = set()
    for match in TABLE_ROW_RE.finditer(markdown):
        row = {key: clean(value) for key, value in match.groupdict().items()}
        if row["id"] in seen:
            raise SystemExit(f"duplicate catalog table ID: {row['id']}")
        seen.add(row["id"])
        rows.append(row)
    if not rows:
        raise SystemExit("no catalog table rows found")
    if len(rows) != expected_count:
        raise SystemExit(
            f"parsed {len(rows)} catalog table rows but found {expected_count} ID rows"
        )
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
