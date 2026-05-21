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
    in_fence = False
    for lineno, line in enumerate(lines, 1):
        if line.startswith("```"):
            in_fence = not in_fence
        if in_fence:
            continue
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
