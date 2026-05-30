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

    field_pairs = (
        ("title", "title"),
        ("category", "category"),
        ("status", "status"),
        ("spine_or_radii", "spine"),
        ("owner", "owner"),
        ("path", "path"),
    )

    for catalog_id in sorted(table_ids & inventory_ids):
        inv = inventory_by_id[catalog_id]
        rendered = table[catalog_id]
        for inventory_field, table_field in field_pairs:
            left = str(inv.get(inventory_field, "")).strip().lstrip("@")
            right = rendered[table_field].strip().lstrip("@")
            if left != right:
                mismatches += 1
                issues.append(
                    f"{catalog_id} {inventory_field} mismatch: "
                    f"inventory={left!r} table={right!r}"
                )

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
