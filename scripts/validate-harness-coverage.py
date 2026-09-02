#!/usr/bin/env python3
"""Validate the QE harness against the Wave 15 testing corpus.

Seven checks:
  1. Every modules.yml archetype exists as an archetype document.
  2. Every module's tool equals that archetype's declared best fit in TST-010.
  3. Every module's path exists on disk.
  4. coverage: partial requires a non-empty partial_reason.
  5. No PAN-shaped string (13-19 consecutive digits) anywhere under qe-harness/.
  6. Every threshold_ref in profiles/_nfr-thresholds.yml cites an existing
     NFR-* row and a heading anchor that resolves in that document.
  7. Every fragment under traceability/runs/*.json validates against
     evidence.schema.json. Design spec §5.4 names this gate as the stated
     mitigation for cross-language drift between the three emitters
     ("caught by a gate that validates all three outputs against the one
     schema -- not by code review, which will not catch it reliably") --
     this is that gate. Reports which fragment(s) fail and why.

The testing corpus (knowledge-base/, governance/) is always read from this
repository checkout, no matter what --root is given — the corpus does not
move. --root only relocates the qe-harness/ tree under test: modules.yml,
the harness/ sources scanned for PAN-shaped strings, and profiles/. That
split is what lets the test suite point the gate at an isolated fixture
while still checking real archetypes and real tool-selection data.

Check 2's best fit: TST-010 (tool-selection-matrix.md) states tool
positioning in prose and a decision tree, not a per-archetype table, so it
does not express a machine-readable best fit. This gate instead reads
knowledge-base/testing/coverage/_testing-coverage.yml's primary_tool field
from every row that lists the archetype in its `archetypes` array (i.e. the
catalog rows the archetype covers). When those rows agree, that is the
declared best fit. When there are none, or they disagree, the module is
reported `cannot verify` rather than guessed at or silently skipped.

Usage:
    python3 scripts/validate-harness-coverage.py
    python3 scripts/validate-harness-coverage.py --quiet
    python3 scripts/validate-harness-coverage.py --root /path/to/tree
"""

from __future__ import annotations

import argparse
import importlib.util
import json
import re
import sys
from pathlib import Path
from typing import Any

import jsonschema
import yaml

ROOT = Path(__file__).resolve().parent.parent

# Fixed corpus locations — always resolved against this repository, never --root.
ARCHETYPE_DIR = ROOT / "knowledge-base/testing/archetypes"
NFR_DIR = ROOT / "knowledge-base/nfr"
COVERAGE_PATH = ROOT / "knowledge-base/testing/coverage/_testing-coverage.yml"
TOOL_MATRIX_PATH = ROOT / "knowledge-base/testing/tooling/tool-selection-matrix.md"

# Harness locations — resolved against --root (defaults to this repository).
MODULES_REL = Path("qe-harness/traceability/modules.yml")
HARNESS_REL = Path("qe-harness")
THRESHOLDS_REL = Path("qe-harness/profiles/_nfr-thresholds.yml")
PAN_SKIP_REL = Path("qe-harness/traceability/runs")
PAN_SKIP_DIR_NAMES = {"target", "node_modules", ".venv"}
RUNS_REL = Path("qe-harness/traceability/runs")
EVIDENCE_SCHEMA_REL = Path("qe-harness/traceability/evidence.schema.json")

CATALOG_ID_RE = re.compile(r"^Catalog ID:\s*(?P<id>TST-\d{3})\b", re.MULTILINE)
NFR_ID_RE = re.compile(r"^Catalog ID:\s*(?P<id>NFR-\d{3})\b", re.MULTILINE)
PAN_RE = re.compile(r"(?<!\d)\d{13,19}(?!\d)")
THRESHOLD_REF_RE = re.compile(r"^(?P<nfr>NFR-\d{3})#(?P<anchor>[a-z0-9-]+)$")
COVERAGE_VALUES = {"full", "partial"}


def _load_heading_slug():
    """Import heading_slug() from validate-internal-links.py.

    That module's filename is hyphenated (house style for this scripts/
    directory), so it cannot be reached with a normal `import` statement;
    load it by file path instead, as the brief directs — reuse the anchor
    logic rather than reimplementing it.
    """
    path = ROOT / "scripts" / "validate-internal-links.py"
    spec = importlib.util.spec_from_file_location("validate_internal_links", path)
    if spec is None or spec.loader is None:
        sys.stderr.write("ERROR: cannot load anchor logic from %s\n" % path)
        raise SystemExit(2)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module.heading_slug


heading_slug = _load_heading_slug()


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


def nfr_ids() -> dict[str, Path]:
    """Map NFR-0NN -> absolute path of the document that declares it."""
    found: dict[str, Path] = {}
    if not NFR_DIR.exists():
        return found
    for path in sorted(NFR_DIR.glob("*.md")):
        match = NFR_ID_RE.search(path.read_text(errors="ignore"))
        if match:
            found[match.group("id")] = path
    return found


def best_fit_tools() -> dict[str, set[str]]:
    """Map archetype id -> primary_tool values of the catalog rows it covers.

    A row "covers" the archetype when the archetype id appears in that row's
    `archetypes` list. The archetype's own self-describing catalog row (the
    row whose own catalog_id equals the archetype id) is deliberately
    excluded: those rows carry a placeholder primary_tool and are not a
    reliable best-fit signal.
    """
    mapping: dict[str, set[str]] = {}
    if not COVERAGE_PATH.exists():
        return mapping
    data = yaml.safe_load(COVERAGE_PATH.read_text()) or {}
    for row in data.get("rows") or []:
        tool = row.get("primary_tool")
        if not tool:
            continue
        catalog_id = row.get("catalog_id")
        for archetype_id in row.get("archetypes") or []:
            if archetype_id == catalog_id:
                # Explicit guard, not just reliance on today's corpus convention
                # (self-rows currently declare archetypes: []): a row can never
                # be evidence of its own best fit — its primary_tool is a
                # governs-row placeholder, not a real signal. If a future
                # corpus edit ever put an archetype's own ID in its own
                # archetypes list, this must still be excluded rather than
                # silently entering the best-fit set.
                continue
            mapping.setdefault(archetype_id, set()).add(tool)
    return mapping


def load_modules(path: Path) -> list[dict[str, Any]]:
    """Load modules.yml's module list. Absence means zero modules to check —
    Tasks 16-22 append entries incrementally, so an empty/missing file is a
    legitimate state, not malformed input."""
    if not path.exists():
        return []
    try:
        data = yaml.safe_load(path.read_text()) or {}
    except yaml.YAMLError as exc:
        sys.stderr.write("ERROR: %s is not valid YAML: %s\n" % (path, exc))
        raise SystemExit(2)
    if not isinstance(data, dict):
        sys.stderr.write("ERROR: %s does not contain a mapping\n" % path)
        raise SystemExit(2)
    modules = data.get("modules") or []
    if not isinstance(modules, list):
        sys.stderr.write("ERROR: %s 'modules' is not a list\n" % path)
        raise SystemExit(2)
    return modules


def load_thresholds(path: Path) -> list[dict[str, Any]]:
    """Load profiles/_nfr-thresholds.yml's threshold entries. Absence means
    zero entries to check — this file is a later task's deliverable."""
    if not path.exists():
        return []
    try:
        data = yaml.safe_load(path.read_text()) or {}
    except yaml.YAMLError as exc:
        sys.stderr.write("ERROR: %s is not valid YAML: %s\n" % (path, exc))
        raise SystemExit(2)
    if isinstance(data, list):
        entries = data
    elif isinstance(data, dict):
        entries = data.get("thresholds") or []
    else:
        sys.stderr.write("ERROR: %s has an unrecognised top-level shape\n" % path)
        raise SystemExit(2)
    if not isinstance(entries, list):
        sys.stderr.write("ERROR: %s 'thresholds' is not a list\n" % path)
        raise SystemExit(2)
    return entries


def check_pan_shaped(harness_root: Path) -> list[str]:
    """Check 5 — no PAN-shaped (13-19 consecutive digit) string anywhere
    under qe-harness/, skipping generated run evidence and dependency dirs.
    Never print the matched digits — only file, line, and span length."""
    issues: list[str] = []
    base = (harness_root / HARNESS_REL).resolve()
    if not base.exists():
        return issues
    skip_runs = (harness_root / PAN_SKIP_REL).resolve()
    for path in sorted(base.rglob("*")):
        if not path.is_file():
            continue
        resolved = path.resolve()
        if resolved == skip_runs or skip_runs in resolved.parents:
            continue
        if PAN_SKIP_DIR_NAMES & {parent.name for parent in path.parents}:
            continue
        try:
            lines = path.read_text(encoding="utf-8").splitlines()
        except (UnicodeDecodeError, OSError):
            continue
        rel = path.relative_to(harness_root).as_posix()
        for lineno, line in enumerate(lines, 1):
            for match in PAN_RE.finditer(line):
                issues.append(
                    "check5 PAN-shaped string in %s:%d (span length %d)"
                    % (rel, lineno, len(match.group(0)))
                )
    return issues


def document_heading_slugs(path: Path) -> set[str]:
    slugs: set[str] = set()
    for line in path.read_text(errors="ignore").splitlines():
        if line.startswith("#"):
            text = line.lstrip("#").strip()
            if text:
                slugs.add(heading_slug(text))
    return slugs


def check_evidence_fragments(harness_root: Path) -> list[str]:
    """Check 7 -- every fragment under traceability/runs/*.json validates
    against evidence.schema.json.

    This is the gate design spec §5.4 names as the stated mitigation for
    cross-language drift between the JVM/Python/JS emitters ("caught by a
    gate that validates all three outputs against the one schema -- not by
    code review, which will not catch it reliably"). Absence of either the
    schema or the runs directory means nothing to check yet (both are
    later-task deliverables), not malformed input -- same convention
    load_modules/load_thresholds already follow.
    """
    issues: list[str] = []
    schema_path = harness_root / EVIDENCE_SCHEMA_REL
    runs_dir = harness_root / RUNS_REL
    if not schema_path.exists() or not runs_dir.exists():
        return issues

    try:
        schema = json.loads(schema_path.read_text())
    except (OSError, json.JSONDecodeError) as exc:
        issues.append(
            "check7 %s is not valid JSON: %s" % (schema_path.relative_to(harness_root).as_posix(), exc)
        )
        return issues

    for path in sorted(runs_dir.glob("*.json")):
        rel = path.relative_to(harness_root).as_posix()
        try:
            fragment = json.loads(path.read_text())
        except (OSError, json.JSONDecodeError) as exc:
            issues.append("check7 %s is not valid JSON: %s" % (rel, exc))
            continue
        try:
            jsonschema.validate(fragment, schema)
        except jsonschema.exceptions.ValidationError as exc:
            issues.append("check7 %s fails evidence.schema.json: %s" % (rel, exc.message))
    return issues


def validate(harness_root: Path) -> list[str]:
    issues: list[str] = []

    known_archetypes = archetype_ids()
    known_nfrs = nfr_ids()
    best_fit = best_fit_tools()

    modules = load_modules(harness_root / MODULES_REL)

    seen_archetypes: set[str] = set()
    for module in modules:
        if not isinstance(module, dict):
            issues.append("modules.yml entry is not a mapping: %r" % module)
            continue

        archetype = module.get("archetype")
        if not archetype:
            issues.append("modules.yml entry missing 'archetype': %r" % module)
            continue
        if archetype in seen_archetypes:
            issues.append("modules.yml has a duplicate entry for %s" % archetype)
        seen_archetypes.add(archetype)

        # Check 1 — archetype exists as an archetype document.
        if archetype not in known_archetypes:
            issues.append("check1 %s has no archetype document" % archetype)

        # Check 2 — tool equals the archetype's declared best fit.
        tool = module.get("tool")
        candidates = best_fit.get(archetype)
        if not candidates:
            issues.append(
                "check2 ⚠️ cannot verify tool for %s "
                "(no catalog row in _testing-coverage.yml covers it)" % archetype
            )
        elif len(candidates) > 1:
            issues.append(
                "check2 ⚠️ cannot verify tool for %s "
                "(covering rows disagree on primary_tool: %s)"
                % (archetype, ", ".join(sorted(candidates)))
            )
        else:
            best = next(iter(candidates))
            if tool != best:
                issues.append(
                    "check2 %s tool mismatch: modules.yml declares '%s', "
                    "best fit per _testing-coverage.yml is '%s'"
                    % (archetype, tool, best)
                )

        # Check 3 — path exists on disk.
        rel_path = module.get("path")
        if not rel_path:
            issues.append("check3 %s has no path" % archetype)
        elif not (harness_root / rel_path).exists():
            issues.append("check3 %s path does not exist: %s" % (archetype, rel_path))

        # Check 4 — partial coverage requires a non-empty partial_reason.
        coverage = module.get("coverage")
        if coverage not in COVERAGE_VALUES:
            issues.append(
                "check4 %s has invalid coverage '%s' (expected one of %s)"
                % (archetype, coverage, sorted(COVERAGE_VALUES))
            )
        elif coverage == "partial" and not str(module.get("partial_reason") or "").strip():
            issues.append("check4 %s has partial coverage without partial_reason" % archetype)

    # Check 5 — no PAN-shaped strings under qe-harness/.
    issues.extend(check_pan_shaped(harness_root))

    # Check 6 — threshold_ref citations resolve to a real row and anchor.
    thresholds = load_thresholds(harness_root / THRESHOLDS_REL)
    for entry in thresholds:
        if not isinstance(entry, dict):
            issues.append("check6 threshold entry is not a mapping: %r" % entry)
            continue
        ref = entry.get("threshold_ref")
        if not ref:
            issues.append("check6 threshold entry missing threshold_ref: %r" % entry)
            continue
        match = THRESHOLD_REF_RE.match(ref)
        if not match:
            issues.append(
                "check6 threshold_ref '%s' is not shaped like NFR-NNN#anchor" % ref
            )
            continue
        nfr_id, anchor = match.group("nfr"), match.group("anchor")
        doc = known_nfrs.get(nfr_id)
        if doc is None:
            issues.append(
                "check6 threshold_ref '%s' cites %s, which has no NFR document" % (ref, nfr_id)
            )
            continue
        if anchor not in document_heading_slugs(doc):
            issues.append(
                "check6 threshold_ref '%s' anchor does not resolve in %s"
                % (ref, doc.relative_to(ROOT).as_posix())
            )

    # Check 7 -- every emitted evidence fragment validates against the schema.
    issues.extend(check_evidence_fragments(harness_root))

    return issues


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--quiet",
        action="store_true",
        help="print only the summary line",
    )
    parser.add_argument(
        "--root",
        type=Path,
        default=ROOT,
        help="qe-harness tree to validate (default: this repository)",
    )
    args = parser.parse_args()

    harness_root = args.root.resolve()
    issues = validate(harness_root)

    if issues:
        if not args.quiet:
            for issue in issues:
                print("  X %s" % issue)
        print("FAIL: %d harness-coverage issue(s)" % len(issues))
        return 1

    print("OK: harness coverage is consistent with the testing corpus")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
