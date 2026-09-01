#!/usr/bin/env python3
"""merge-fragments.py -- merges evidence fragments under traceability/runs/
into a `test_acceptance_criteria` block, keyed by `service_name`.

Scope note: this is a SCOPED SUBSET of the full contract TST-001 (the Test
Strategy Standard, knowledge-base/testing/strategy/test-strategy-standard.md)
defines, not the whole thing -- Task 23's own brief narrows the requirement
to exactly three things: `archetypes`, per-archetype `coverage`, and an
explicit threshold-evaluation count. The real TST-001 contract additionally
calls for `tier`, `catalog_refs`, `slo_source`, `functional.
negative_paths_covered`, `performance.profiles_executed`/`sustained_rps`/
`peak_rps`/`workload_model`, `resilience.fault_scenarios`, `contract.*`,
`security.*`, `data_quality.*`, and `evidence.signed_off_by`, none of which
this script populates (`tier` is the one exception -- see build_block()).
Filling in the rest is out of this task's scope; do not read this docstring
as a claim that the output below is a complete `test_acceptance_criteria`
block.

Selection rule: for each archetype, only the MOST RECENTLY WRITTEN fragment
counts. `traceability/runs/` accumulates one file per invocation across the
harness's whole history (every `make run ARCH=...` a developer or CI ever
ran, including ad-hoc TDD runs against a defect-injected SUT) -- an
archetype's CURRENT evidence is whichever fragment for it was written last,
not the union of everything ever recorded for it. This mirrors the
convention Tasks 16-19's `ModuleRunner` test fixture already uses for its
own assertions: "the newest file under traceability/runs/ matching
*-<archetype>.json". Ties (equal mtime -- coarse filesystem mtime
resolution is a real risk on some Docker-Desktop bind-mount configurations,
and Task 27's CI will run `make run-all` repeatedly) resolve to whichever
fragment was iterated LAST, i.e. the one with the lexicographically later
filename -- see latest_per_archetype()'s `>=` comparison.

The highest-severity risk this script guards against (per the Task 23
brief): a merge that silently drops a `not-evaluated` threshold would make a
smoke-mode run -- one that never reached steady state, e.g. TST-031's
clock-skew sweep on a short profile -- look like a full, passing performance
run. `performance.thresholds_not_evaluated` (and `not_evaluated_detail`,
which names exactly which threshold on which archetype) is computed
explicitly and can never come out as a silent zero.

Depends on PyYAML, the same as `bin/run-module.sh`'s own inline `import
yaml` -- no separate `requirements.txt` for `qe-harness/bin/`; both rely on
the system `python3`'s already-installed PyYAML (see qe-harness/README.md's
Pinned Versions table note on `_nfr-thresholds.yml` parsing).
"""
from __future__ import annotations

import json
import pathlib
import sys
from datetime import datetime, timezone

import yaml

QE_HARNESS_ROOT = pathlib.Path(__file__).resolve().parents[1]  # qe-harness/
DEFAULT_RUNS_DIR = QE_HARNESS_ROOT / "traceability" / "runs"
DEFAULT_MODULES_YML = QE_HARNESS_ROOT / "traceability" / "modules.yml"
DEFAULT_OUTPUT = QE_HARNESS_ROOT / "traceability" / "test_acceptance_criteria.yml"

THRESHOLD_NOT_EVALUATED = "not-evaluated"
RESULT_PASSED = "passed"


def load_modules(modules_yml_path: pathlib.Path = DEFAULT_MODULES_YML) -> dict:
    """archetype -> its traceability/modules.yml entry (coverage, defect_flag, tool, ...)."""
    doc = yaml.safe_load(pathlib.Path(modules_yml_path).read_text())
    return {m["archetype"]: m for m in doc["modules"]}


def load_fragments(runs_dir: pathlib.Path) -> list[dict]:
    """Every *.json fragment directly under runs_dir, each annotated with the
    file's own mtime so latest_per_archetype doesn't have to re-stat."""
    fragments = []
    runs_dir = pathlib.Path(runs_dir)
    for path in sorted(runs_dir.glob("*.json")):
        try:
            data = json.loads(path.read_text())
        except (OSError, json.JSONDecodeError) as exc:
            print(f"merge-fragments.py: skipping unreadable fragment {path}: {exc}", file=sys.stderr)
            continue
        data["_mtime"] = path.stat().st_mtime
        data["_path"] = str(path)
        fragments.append(data)
    return fragments


def latest_per_archetype(fragments: list[dict]) -> dict[str, dict]:
    """Collapse fragments to (at most) one per archetype: whichever has the
    latest mtime wins, so a stale fragment from an earlier defect-injection
    or smoke-mode dev run never outranks the current run's evidence.

    `fragments` is iterated in the order load_fragments() produced it --
    `sorted(runs_dir.glob(...))`, i.e. chronological by filename. On an
    EXACT mtime tie (coarse filesystem mtime resolution can genuinely
    produce this -- confirmed by direct reproduction, not just theory), `>=`
    rather than strict `>` makes the later-iterated (truly more recent,
    per its own filename) fragment win instead of the earlier one. A
    strict `>` here would let a stale fragment silently outrank the real
    latest one whenever two runs happen to land on the same mtime."""
    latest: dict[str, dict] = {}
    for frag in fragments:
        arch = frag["archetype"]
        if arch not in latest or frag["_mtime"] >= latest[arch]["_mtime"]:
            latest[arch] = frag
    return latest


def group_by_service(fragments_by_archetype: dict[str, dict]) -> dict[str, list[dict]]:
    by_service: dict[str, list[dict]] = {}
    for frag in fragments_by_archetype.values():
        by_service.setdefault(frag["service_name"], []).append(frag)
    return by_service


def build_block(service_name: str, frags: list[dict], modules: dict) -> dict:
    frags = sorted(frags, key=lambda f: f["archetype"])
    archetypes = [f["archetype"] for f in frags]

    # TST-001 also declares `tier` on the block itself (must equal the
    # service's nfr_acceptance_criteria.tier). Every fragment already
    # carries its own `tier` (evidence.schema.json requires it) but
    # build_block() never surfaced it -- a real, if partial, step toward
    # actual TST-001 fidelity. If a service's fragments ever disagree on
    # tier, record the disagreement explicitly rather than picking one
    # arbitrarily (not expected in this harness -- one service, one tier --
    # but this script must never paper over the archetypes actually
    # disagreeing if that assumption ever breaks).
    tiers = sorted({f["tier"] for f in frags})
    tier = tiers[0] if len(tiers) == 1 else tiers

    # coverage per archetype comes from modules.yml, NEVER from the
    # fragment's own `result` -- a module can report result: passed while
    # its archetype's declared coverage is still "partial" (TST-043): the
    # run succeeded within its scoped boundary, but that boundary itself
    # never grew to "full". Conflating the two is exactly what
    # test_merge_marks_partial_coverage_not_passed guards against.
    coverage = {arch: modules[arch]["coverage"] if arch in modules else "unknown" for arch in archetypes}

    thresholds_evaluated = 0
    thresholds_passed = 0
    thresholds_failed = 0
    thresholds_not_evaluated = 0
    not_evaluated_detail = []
    for frag in frags:
        for threshold in frag.get("thresholds", []):
            result = threshold["result"]
            if result == THRESHOLD_NOT_EVALUATED:
                thresholds_not_evaluated += 1
                not_evaluated_detail.append(
                    {
                        "archetype": frag["archetype"],
                        "name": threshold["name"],
                        "reason": threshold.get("reason", ""),
                    }
                )
            else:
                thresholds_evaluated += 1
                if result == RESULT_PASSED:
                    thresholds_passed += 1
                elif result == "failed":
                    thresholds_failed += 1
                # "not-implemented" counts as evaluated-but-neither, on purpose:
                # it is not a dropped threshold, it is a declared non-goal.

    invariants_covered = sum(len(f.get("invariants", [])) for f in frags)
    invariants_failed = sum(1 for f in frags for inv in f.get("invariants", []) if inv["result"] == "failed")

    failed_archetypes = [f["archetype"] for f in frags if f["result"] != RESULT_PASSED]

    evidence = [
        {
            "archetype": f["archetype"],
            "result": f["result"],
            "executed_on": f["evidence"]["executed_on"],
            "environment": f["evidence"]["environment"],
            "report_path": f["evidence"]["report_path"],
        }
        for f in frags
    ]

    return {
        "service_name": service_name,
        "tier": tier,
        "archetypes": archetypes,
        "coverage": coverage,
        "functional": {
            "invariants_covered": invariants_covered,
            "invariants_failed": invariants_failed,
            "oracles": sorted({f["oracle"] for f in frags}),
        },
        "performance": {
            "thresholds_evaluated": thresholds_evaluated,
            "thresholds_passed": thresholds_passed,
            "thresholds_failed": thresholds_failed,
            # Explicit, never-dropped count -- see module docstring.
            "thresholds_not_evaluated": thresholds_not_evaluated,
            "not_evaluated_detail": not_evaluated_detail,
        },
        "failed_archetypes": failed_archetypes,
        "overall_result": RESULT_PASSED if not failed_archetypes else "failed",
        "evidence": evidence,
    }


def merge(runs_dir: pathlib.Path = DEFAULT_RUNS_DIR, modules_yml_path: pathlib.Path = DEFAULT_MODULES_YML) -> dict:
    """Returns {"test_acceptance_criteria": <block>} for the (expected, single)
    service the fragments belong to. If fragments ever span more than one
    service_name, returns {"test_acceptance_criteria": {service: block, ...}}
    instead -- not exercised by this harness today (it has exactly one
    service, reference-sut), but grouping by service_name is the brief's own
    stated contract, so it degrades explicitly rather than picking one
    service arbitrarily.
    """
    modules = load_modules(modules_yml_path)
    fragments = load_fragments(runs_dir)
    if not fragments:
        raise SystemExit(f"merge-fragments.py: no evidence fragments found under {runs_dir}")

    by_service = group_by_service(latest_per_archetype(fragments))
    blocks = {svc: build_block(svc, frags, modules) for svc, frags in by_service.items()}

    if len(blocks) == 1:
        (block,) = blocks.values()
        return {"test_acceptance_criteria": block}
    return {"test_acceptance_criteria": blocks}


def main() -> int:
    merged = merge()
    DEFAULT_OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    DEFAULT_OUTPUT.write_text(
        f"# Generated by qe-harness/bin/merge-fragments.py at "
        f"{datetime.now(timezone.utc).isoformat(timespec='seconds')} -- do not hand-edit.\n"
        + yaml.safe_dump(merged, sort_keys=False, default_flow_style=False)
    )
    print(f"merge-fragments.py: wrote {DEFAULT_OUTPUT}")

    top = merged["test_acceptance_criteria"]
    blocks = [top] if "service_name" in top else list(top.values())
    failed = [b for b in blocks if b["overall_result"] != RESULT_PASSED]
    for b in failed:
        print(
            f"merge-fragments.py: {b['service_name']} has failed archetypes: {b['failed_archetypes']}",
            file=sys.stderr,
        )
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
