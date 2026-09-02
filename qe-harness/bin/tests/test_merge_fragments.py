"""Tests for bin/merge-fragments.py -- Task 23 (Wave 16 QE Harness).

merge-fragments.py's filename has a hyphen, so it cannot be `import`ed by
name; it is loaded here via importlib.util, the same mechanism the module
itself would need if some future caller wanted to reuse it as a library.

`write_fragment`/`write_fragments` and `merge` below are test-only helpers
(not part of the brief's three given test bodies, which are reproduced
verbatim): they write minimal, schema-shaped evidence fragments into a
tmp_path standing in for `traceability/runs/`, and call the real merge()
function against them plus THIS REPO'S actual `traceability/modules.yml` --
coverage per archetype is declared there, not something a fixture can
invent, so it must come from the real file for `test_merge_marks_partial_
coverage_not_passed` (TST-043 is declared `coverage: partial`) to mean
anything.
"""
from __future__ import annotations

import importlib.util
import json
import pathlib
import sys

BIN_DIR = pathlib.Path(__file__).resolve().parents[1]

_spec = importlib.util.spec_from_file_location("merge_fragments", BIN_DIR / "merge-fragments.py")
merge_fragments = importlib.util.module_from_spec(_spec)
sys.modules["merge_fragments"] = merge_fragments
_spec.loader.exec_module(merge_fragments)


def write_fragment(tmp_path, archetype, *, service_name="reference-sut", result="passed",
                    threshold_result=None, reason=None):
    """Writes one minimal, evidence.schema.json-shaped fragment for
    `archetype` into tmp_path, named the same way EvidenceEmitter names real
    ones (ISO-instant prefix) so mtime-based latest-per-archetype selection
    has a real file to stat."""
    fragment = {
        "archetype": archetype,
        "module": "jmeter",
        "service_name": service_name,
        "tier": "T1",
        "oracle": "invariant-assertion",
        "result": result,
        "evidence": {
            "executed_on": "2026-09-01",
            "environment": "qe-harness-test",
            "report_path": f"reports/{archetype}.json",
        },
    }
    if threshold_result is not None:
        threshold = {
            "name": f"{archetype.lower()}-threshold",
            "threshold_ref": "NFR-001#p99-latency",
            "result": threshold_result,
        }
        if reason is not None:
            threshold["reason"] = reason
        fragment["thresholds"] = [threshold]

    path = tmp_path / f"2026-09-01T00-00-00-000000Z-{archetype}.json"
    path.write_text(json.dumps(fragment))
    return path


def write_fragments(tmp_path, archetypes, **kwargs):
    return [write_fragment(tmp_path, archetype, **kwargs) for archetype in archetypes]


def merge(tmp_path):
    return merge_fragments.merge(runs_dir=tmp_path)


def test_merge_produces_one_block_listing_every_archetype(tmp_path):
    write_fragments(tmp_path, ["TST-021", "TST-030", "TST-031",
                               "TST-035", "TST-039", "TST-040", "TST-043"])
    block = merge(tmp_path)["test_acceptance_criteria"]
    assert block["archetypes"] == ["TST-021", "TST-030", "TST-031",
                                   "TST-035", "TST-039", "TST-040", "TST-043"]
    assert block["service_name"] == "reference-sut"

def test_merge_marks_partial_coverage_not_passed(tmp_path):
    write_fragments(tmp_path, ["TST-043"])
    block = merge(tmp_path)["test_acceptance_criteria"]
    assert block["coverage"]["TST-043"] == "partial"

def test_merge_preserves_not_evaluated_thresholds(tmp_path):
    write_fragment(tmp_path, "TST-031", threshold_result="not-evaluated",
                   reason="smoke-mode")
    block = merge(tmp_path)["test_acceptance_criteria"]
    assert block["performance"]["thresholds_not_evaluated"] == 1
