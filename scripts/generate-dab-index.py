#!/usr/bin/env python3
# ============================================================
# DAB Index Generator
# ============================================================
# Scans all DAB folders and generates:
#  - registry/dab-index.md (master index by year/domain)
#  - registry/active-submissions.md (open MRs)
#  - registry/approved-dabs.md (approved with status)
#
# Usage:
#   python3 scripts/generate-dab-index.py
#   python3 scripts/generate-dab-index.py --dry-run
#   python3 scripts/generate-dab-index.py --index-file registry/custom-index.md
#
# Dependencies: Python 3.7+, stdlib only (os, re, datetime, pathlib)
# ============================================================

import os
import re
import sys
import argparse
from datetime import datetime
from pathlib import Path
from collections import defaultdict

# Configuration
DEFAULT_INDEX_FILE = "registry/dab-index.md"
DEFAULT_ACTIVE_FILE = "registry/active-submissions.md"
DEFAULT_APPROVED_FILE = "registry/approved-dabs.md"

# Status emoji indicators
STATUS_EMOJI = {
    "approved": "🟢",
    "in-review": "🟡",
    "in_review": "🟡",
    "submitted": "🔴",
    "archived": "⚪",
}

class DABMetadata:
    """Represents metadata for a single DAB project."""

    def __init__(self, name, domain, year, project_slug, path):
        self.name = name
        self.domain = domain
        self.year = year
        self.project_slug = project_slug
        self.path = path
        self.status = "submitted"  # Default
        self.submitted_date = None
        self.approved_date = None
        self.mr_link = None
        self.implementation_pct = 0

    def to_dict(self):
        """Convert to dictionary for sorting/display."""
        return {
            "name": self.name,
            "domain": self.domain,
            "year": self.year,
            "project_slug": self.project_slug,
            "path": self.path,
            "status": self.status,
            "submitted_date": self.submitted_date,
            "approved_date": self.approved_date,
            "mr_link": self.mr_link,
            "implementation_pct": self.implementation_pct,
        }

    def __repr__(self):
        return f"<DAB {self.domain}/{self.name} ({self.status})>"


def find_all_dabs(root_path="domains"):
    """
    Scan domains/ tree and find all DAB folders.
    Returns list of DABMetadata objects.
    """
    dabs = []
    root = Path(root_path)

    if not root.exists():
        return dabs

    # Pattern: domains/{domain}/dab/{year}/{project-slug}
    for domain_dir in root.iterdir():
        if not domain_dir.is_dir() or domain_dir.name.startswith("."):
            continue

        domain_name = domain_dir.name
        dab_dir = domain_dir / "dab"

        if not dab_dir.exists():
            continue

        # Iterate by year
        for year_dir in dab_dir.iterdir():
            if not year_dir.is_dir() or not year_dir.name.isdigit():
                continue

            year = year_dir.name

            # Iterate by project
            for project_dir in year_dir.iterdir():
                if not project_dir.is_dir() or project_dir.name.startswith("."):
                    continue

                project_slug = project_dir.name
                readme_path = project_dir / "README.md"

                # Create metadata object
                # Extract project name from README or use slug
                project_name = project_slug.replace("-", " ").title()

                dab = DABMetadata(
                    name=project_name,
                    domain=domain_name.replace("-", " ").title(),
                    year=year,
                    project_slug=project_slug,
                    path=str(project_dir.relative_to(root.parent)),
                )

                # Extract metadata from README if it exists
                if readme_path.exists():
                    extract_metadata(dab, readme_path)

                dabs.append(dab)

    return dabs


def extract_metadata(dab, readme_path):
    """
    Extract status, dates, and MR info from README.md.
    Looks for patterns like:
      - **Status**: Approved
      - **Submitted**: 2026-03-08
      - **Approved**: 2026-03-15
      - **MR**: !123
      - **Implementation**: 60%
    """
    try:
        with open(readme_path, "r", encoding="utf-8") as f:
            content = f.read()

        # Extract project name from main heading
        name_match = re.search(r"^#\s+(.+?)$", content, re.MULTILINE)
        if name_match:
            dab.name = name_match.group(1).strip()

        # Extract status
        status_match = re.search(
            r"-\s*\*\*Status\*\*\s*:\s*([^\n]+)", content, re.IGNORECASE
        )
        if status_match:
            status = status_match.group(1).strip().lower()
            dab.status = status

        # Extract submitted date
        submitted_match = re.search(
            r"-\s*\*\*Submitted\*\*\s*:\s*(\d{4}-\d{2}-\d{2})", content
        )
        if submitted_match:
            dab.submitted_date = submitted_match.group(1)

        # Extract approved date
        approved_match = re.search(
            r"-\s*\*\*Approved\*\*\s*:\s*(\d{4}-\d{2}-\d{2})", content
        )
        if approved_match:
            dab.approved_date = approved_match.group(1)

        # Extract MR link
        mr_match = re.search(r"-\s*\*\*MR\*\*\s*:\s*(!?\d+)", content)
        if mr_match:
            dab.mr_link = mr_match.group(1)

        # Extract implementation percentage
        impl_match = re.search(
            r"-\s*\*\*Implementation\*\*\s*:\s*(\d+)%", content
        )
        if impl_match:
            dab.implementation_pct = int(impl_match.group(1))

    except Exception as e:
        print(f"  ⚠️  Warning: Could not extract metadata from {readme_path}: {e}", file=sys.stderr)


def generate_dab_index(dabs, output_file):
    """
    Generate master index organized by year → domain.
    """
    # Group by year, then domain
    by_year = defaultdict(lambda: defaultdict(list))
    for dab in dabs:
        by_year[dab.year][dab.domain].append(dab)

    # Sort
    for year in by_year:
        for domain in by_year[year]:
            by_year[year][domain].sort(key=lambda d: d.submitted_date or "")

    # Generate markdown
    lines = [
        "# DAB Registry",
        "",
        "> Master index of all Design Approval Board submissions",
        "",
        f"Last updated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S UTC')}",
        "",
    ]

    # Iterate by year (newest first)
    for year in sorted(by_year.keys(), reverse=True):
        lines.append(f"## {year}")
        lines.append("")

        domains = by_year[year]
        for domain in sorted(domains.keys()):
            lines.append(f"### {domain}")
            lines.append("")

            # Create table
            lines.append(
                "| Project | Status | Submitted | Approved | MR | Implementation |"
            )
            lines.append(
                "|---------|--------|-----------|----------|----|--------------"
            )

            for dab in sorted(domains[domain], key=lambda d: d.submitted_date or ""):
                status_emoji = STATUS_EMOJI.get(dab.status, "❓")
                status_text = dab.status.replace("_", " ").title()
                submitted = dab.submitted_date or "—"
                approved = dab.approved_date or "—"
                mr_text = (
                    f"[!{dab.mr_link}](../../merge_requests/{dab.mr_link})"
                    if dab.mr_link
                    else "—"
                )
                impl_text = f"{dab.implementation_pct}%" if dab.implementation_pct > 0 else "—"

                lines.append(
                    f"| {status_emoji} **{dab.name}** | {status_text} | {submitted} | {approved} | {mr_text} | {impl_text} |"
                )

            lines.append("")

    # Add legend
    lines.extend([
        "---",
        "",
        "## Status Indicators",
        "",
        "- 🟢 **Approved** — DAB has been approved by EA Board; implementation underway or complete",
        "- 🟡 **In Review** — DAB is in active review (MR is open)",
        "- 🔴 **Submitted** — DAB recently submitted, awaiting initial review",
        "- ⚪ **Archived** — Superseded or no longer active",
        "",
    ])

    # Write file
    with open(output_file, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    return len(dabs)


def generate_active_submissions(dabs, output_file):
    """
    Generate list of DABs currently in review (open MRs).
    """
    active = [dab for dab in dabs if dab.status in ("submitted", "in-review", "in_review")]
    active.sort(key=lambda d: d.submitted_date or "", reverse=True)

    lines = [
        "# Active DAB Submissions",
        "",
        "> DABs currently awaiting approval",
        "",
        f"Last updated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S UTC')}",
        "",
    ]

    if not active:
        lines.append("No active submissions at this time.")
        lines.append("")
    else:
        lines.append("| Domain | Project | Submitted By | Date | MR | Review Stage |")
        lines.append("|--------|---------|--------------|------|----|----|")

        for dab in active:
            submitted = dab.submitted_date or "—"
            mr_link = (
                f"[!{dab.mr_link}](../../merge_requests/{dab.mr_link})"
                if dab.mr_link
                else "—"
            )
            # Infer review stage based on status
            stage = "Awaiting Specialist Review" if dab.status == "submitted" else "Under Review"

            lines.append(
                f"| {dab.domain} | **{dab.name}** | — | {submitted} | {mr_link} | {stage} |"
            )

        lines.append("")

    lines.extend([
        "---",
        "",
        "For submission guidelines, see [CONTRIBUTING.md](../CONTRIBUTING.md)",
        "",
    ])

    with open(output_file, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    return len(active)


def generate_approved_dabs(dabs, output_file):
    """
    Generate list of approved DABs with implementation tracking.
    """
    approved = [dab for dab in dabs if dab.status == "approved"]
    approved.sort(key=lambda d: d.approved_date or "", reverse=True)

    lines = [
        "# Approved DABs",
        "",
        "> Design Approval Board submissions that have received architecture approval",
        "",
        f"Last updated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S UTC')}",
        "",
    ]

    if not approved:
        lines.append("No approved DABs yet.")
        lines.append("")
    else:
        lines.append("| Domain | Project | Approved | Implementation | MR |")
        lines.append("|--------|---------|----------|-----------------|-----|")

        for dab in approved:
            approved_date = dab.approved_date or "—"
            impl_bar = (
                "█" * (dab.implementation_pct // 10)
                + "░" * (10 - dab.implementation_pct // 10)
            )
            impl_text = f"{impl_bar} {dab.implementation_pct}%" if dab.implementation_pct > 0 else "—"
            mr_link = (
                f"[!{dab.mr_link}](../../merge_requests/{dab.mr_link})"
                if dab.mr_link
                else "—"
            )

            lines.append(
                f"| {dab.domain} | **{dab.name}** | {approved_date} | {impl_text} | {mr_link} |"
            )

        lines.append("")

    lines.extend([
        "---",
        "",
        "## Tracking Implementation",
        "",
        "The **Implementation** column shows progress on approved DABs. "
        "Update the README.md in each DAB folder with:",
        "",
        "```yaml",
        "- **Status**: Approved",
        "- **Implementation**: 75%  # Update as work progresses",
        "```",
        "",
    ])

    with open(output_file, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    return len(approved)


def main():
    parser = argparse.ArgumentParser(
        description="Generate DAB registry files from domains/ tree"
    )
    parser.add_argument(
        "--index-file",
        default=DEFAULT_INDEX_FILE,
        help=f"Output file for DAB index (default: {DEFAULT_INDEX_FILE})",
    )
    parser.add_argument(
        "--active-file",
        default=DEFAULT_ACTIVE_FILE,
        help=f"Output file for active submissions (default: {DEFAULT_ACTIVE_FILE})",
    )
    parser.add_argument(
        "--approved-file",
        default=DEFAULT_APPROVED_FILE,
        help=f"Output file for approved DABs (default: {DEFAULT_APPROVED_FILE})",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Preview output without writing files",
    )
    parser.add_argument(
        "-v", "--verbose",
        action="store_true",
        help="Show detailed scanning progress",
    )

    args = parser.parse_args()

    # Create registry directory if it doesn't exist
    registry_dir = Path(args.index_file).parent
    registry_dir.mkdir(parents=True, exist_ok=True)

    # Scan for DABs
    if args.verbose:
        print("🔍 Scanning domains/ for DABs...")

    dabs = find_all_dabs()

    if args.verbose:
        print(f"   Found {len(dabs)} DAB(s)")
        for dab in dabs:
            print(f"   - {dab}")

    if not dabs:
        print("⚠️  No DABs found in domains/ tree")
        sys.exit(0)

    # Generate registries
    if args.dry_run:
        print(f"\n📋 Would generate:")
        print(f"   - {args.index_file} ({len(dabs)} DABs)")
        print(f"   - {args.active_file} (open MRs)")
        print(f"   - {args.approved_file} (approved DABs)")
        print(f"\n✅ Dry-run complete")
    else:
        print(f"\n🏗️  Generating registry files...")

        # Generate index
        count = generate_dab_index(dabs, args.index_file)
        print(f"   ✅ {args.index_file} ({count} DABs)")

        # Generate active submissions
        active_count = generate_active_submissions(dabs, args.active_file)
        print(f"   ✅ {args.active_file} ({active_count} active)")

        # Generate approved DABs
        approved_count = generate_approved_dabs(dabs, args.approved_file)
        print(f"   ✅ {args.approved_file} ({approved_count} approved)")

        print(f"\n✅ Registry generation complete")


if __name__ == "__main__":
    main()
