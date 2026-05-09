#!/usr/bin/env python3
"""Generate stub markdown files from _catalog-inventory.yml.

Usage:
    python3 scripts/generate-stubs.py \
        --yaml governance/standards/_catalog-inventory.yml \
        --root /Users/dennis.dao/Documents/Arch-As-Code [--force]

Skips rows that are not status=Proposed. Refuses to overwrite existing files
unless --force. Renders one markdown file per row using the stub template
from Spec §4b.

Stub content fields (problem, sketch, ha_hook, hp_hook, hr_hook, references)
are read from the optional `stub_content` block in each YAML row. When absent,
the script writes generic TBD placeholders that signal "high-priority backfill
needed in Wave 1" — matching the Q4 user decision (~20 fully-populated;
remaining ~80 have minimal defaults).
"""
import argparse
import sys
from pathlib import Path
from textwrap import dedent

import yaml

STUB_TEMPLATE = dedent('''\
    # {title}

    Status: Proposed | Target Wave: {target_wave} | Owner: @{owner}
    Catalog ID: {id}
    Tier Applicability: {tiers}

    > **STUB** — full content authored in Wave {target_wave}.
    > Catalog: ../../governance/standards/enterprise-architecture-catalog.md

    ## Problem Statement

    {problem}

    ## Sketch of Solution

    {sketch_bullets}

    ## Compliance Hooks

    - Ring 0: {ring0}
    - Ring 1: {ring1}
    - Ring 2: {ring2}

    ## NFR Hooks

    - HA: {ha_hook}
    - HP: {hp_hook}
    - HR: {hr_hook}

    ## Authoring Checklist (DoD for moving Status → Approved)

    - [ ] Mermaid solution diagram
    - [ ] Java/Spring code sample
    - [ ] Legacy / frontend / mobile notes (if applicable)
    - [ ] Compliance Mapping table populated (3 rings)
    - [ ] NFR Acceptance Criteria block (HA/HP/HR with concrete numbers)
    - [ ] Cost/FinOps notes
    - [ ] Threat Model summary
    - [ ] Operational Runbook stub
    - [ ] Test Strategy stub
    - [ ] EA-Board review
    - [ ] Domain-owner review

    ## References

    {references}
    ''')


def render_row(row: dict) -> str:
    s = (row.get("stub_content") or {})
    cr = (row.get("compliance_refs") or {})
    sketch = s.get("sketch") or ["TBD — to be authored in Wave " + str(row.get("target_wave", "1"))]
    refs = s.get("references") or [
        "Catalog reference: `governance/standards/enterprise-architecture-catalog.md`",
        "Research notes: `knowledge-base/_research-notes.md`",
    ]
    sketch_bullets = "\n".join(f"- {b}" for b in sketch)
    references = "\n".join(f"- {r}" for r in refs)

    ring0 = ", ".join(cr.get("ring0") or []) or "TBD"
    ring1 = ", ".join(cr.get("ring1") or []) or "TBD"
    ring2 = ", ".join(cr.get("ring2") or []) or "TBD"

    return STUB_TEMPLATE.format(
        title=row["title"],
        target_wave=row.get("target_wave", "1"),
        owner=row.get("owner", "tbd"),
        id=row["id"],
        tiers=", ".join(row.get("tiers") or []) or "TBD",
        problem=s.get("problem") or "TBD — populate during Wave authoring per Procedure A.2 of the implementation plan.",
        sketch_bullets=sketch_bullets,
        ring0=ring0,
        ring1=ring1,
        ring2=ring2,
        ha_hook=s.get("ha_hook") or "TBD",
        hp_hook=s.get("hp_hook") or "TBD",
        hr_hook=s.get("hr_hook") or "TBD",
        references=references,
    )


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--yaml", required=True)
    p.add_argument("--root", required=True)
    p.add_argument("--force", action="store_true")
    args = p.parse_args()

    data = yaml.safe_load(Path(args.yaml).read_text())
    root = Path(args.root)

    created = skipped = 0
    for row in data["rows"]:
        if row.get("status") != "Proposed":
            continue
        path = root / row["path"]
        if path.exists() and not args.force:
            skipped += 1
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(render_row(row))
        created += 1

    print(f"Done: created={created}, skipped={skipped}")


if __name__ == "__main__":
    main()
