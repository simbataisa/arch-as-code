"""Task 21's Step 1 tests for emitter.py, given verbatim in the task brief."""

import json
from pathlib import Path

import jsonschema
import pytest

from emitter import emit_fragment

# tests/test_emitter.py -> parents[0]=tests, [1]=locust, [2]=harness, [3]=qe-harness
SCHEMA_PATH = Path(__file__).resolve().parents[3] / "traceability" / "evidence.schema.json"


def test_python_emitter_output_validates_against_the_shared_schema(tmp_path):
    schema = json.loads(SCHEMA_PATH.read_text())
    out = emit_fragment({
        "archetype": "TST-039", "module": "locust", "service_name": "reference-sut",
        "tier": "T0", "oracle": "confusion-matrix",
        "invariants": [{"id": "I1", "description": "no false negatives", "result": "passed"}],
        "environment": "ci-smoke",
    }, tmp_path)
    jsonschema.validate(json.loads(out.read_text()), schema)


def test_emitter_rejects_not_evaluated_threshold_without_reason(tmp_path):
    with pytest.raises(ValueError):
        emit_fragment({
            "archetype": "TST-039", "module": "locust", "service_name": "reference-sut",
            "tier": "T0", "oracle": "confusion-matrix", "environment": "ci-smoke",
            "thresholds": [
                {"name": "freshness_s", "threshold_ref": "NFR-002#freshness",
                 "result": "not-evaluated"},
            ],
        }, tmp_path)
