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
