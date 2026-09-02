import json, subprocess, sys, textwrap, pathlib

SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "validate-harness-coverage.py"
REAL_EVIDENCE_SCHEMA = SCRIPT.parents[1] / "qe-harness/traceability/evidence.schema.json"

def run(tmp_root):
    return subprocess.run([sys.executable, str(SCRIPT), "--root", str(tmp_root)],
                          capture_output=True, text=True)

def test_flags_tool_mismatch(tmp_path, monkeypatch):
    # A module claiming locust for TST-021, whose declared best fit is jmeter.
    mods = tmp_path / "qe-harness/traceability"
    mods.mkdir(parents=True)
    (mods / "modules.yml").write_text(textwrap.dedent("""
        version: 1
        modules:
          - archetype: TST-021
            tool: locust
            path: qe-harness/harness/locust/tst_021
            coverage: full
            defect_flag: ledger-unbalanced
    """))
    result = run(tmp_path)
    assert result.returncode == 1
    assert "tool mismatch" in result.stdout

def test_flags_pan_shaped_string(tmp_path):
    bad = tmp_path / "qe-harness/harness/jmeter/seed.csv"
    bad.parent.mkdir(parents=True)
    bad.write_text("account,pan\nACC-000001,4111111111111111\n")
    result = run(tmp_path)
    assert result.returncode == 1
    assert "PAN-shaped" in result.stdout

def test_check6_resolves_real_citation_and_flags_bad_one(tmp_path):
    # NFR-002 (knowledge-base/nfr/latency-budget-model.md) really does have a
    # heading "End-to-end budgets per tier (customer-facing)", which slugifies
    # to end-to-end-budgets-per-tier-customer-facing. That citation must
    # resolve cleanly. A citation naming a heading that doesn't exist must not.
    profiles = tmp_path / "qe-harness/profiles"
    profiles.mkdir(parents=True)
    (profiles / "_nfr-thresholds.yml").write_text(textwrap.dedent("""
        version: 1
        thresholds:
          - name: real p99 latency budget citation
            threshold_ref: "NFR-002#end-to-end-budgets-per-tier-customer-facing"
            result: pass
          - name: bogus citation
            threshold_ref: "NFR-002#not-a-real-heading"
            result: pass
    """))
    result = run(tmp_path)
    assert result.returncode == 1
    assert "FAIL: 1 harness-coverage issue(s)" in result.stdout
    assert "threshold_ref 'NFR-002#not-a-real-heading' anchor does not resolve" in result.stdout

def _seed_evidence_schema(tmp_root):
    # check7 resolves evidence.schema.json against --root, not the fixed
    # corpus (it lives under qe-harness/traceability/, same tree as
    # modules.yml/profiles/) -- copy the real schema in so the fixture
    # validates against the exact same rules the real gate does, with no
    # separate copy of the schema to keep in sync by hand.
    dest = tmp_root / "qe-harness/traceability/evidence.schema.json"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(REAL_EVIDENCE_SCHEMA.read_text())
    return dest.parent / "runs"

def test_check7_allows_a_schema_valid_fragment(tmp_path):
    runs_dir = _seed_evidence_schema(tmp_path)
    runs_dir.mkdir(parents=True)
    (runs_dir / "2026-01-01T00-00-00-000000Z-TST-021.json").write_text(json.dumps({
        "archetype": "TST-021",
        "module": "jmeter",
        "service_name": "reference-sut",
        "tier": "T0",
        "oracle": "invariant-assertion",
        "result": "passed",
        "invariants": [
            {"id": "I1", "description": "trial balance nets to zero", "result": "passed"}
        ],
        "evidence": {
            "executed_on": "2026-01-01",
            "environment": "local-compose",
            "report_path": "qe-harness/traceability/runs/2026-01-01T00-00-00-000000Z-TST-021.json",
        },
    }))
    result = run(tmp_path)
    assert result.returncode == 0
    assert "check7" not in result.stdout

def test_check7_flags_a_fragment_that_fails_the_schema(tmp_path):
    # additionalProperties: false at the top level -- an unknown field is a
    # real schema violation, the same class of drift design spec §5.4 names
    # this gate as the mitigation for.
    runs_dir = _seed_evidence_schema(tmp_path)
    runs_dir.mkdir(parents=True)
    bad_fragment = runs_dir / "2026-01-01T00-00-00-000000Z-TST-021.json"
    bad_fragment.write_text(json.dumps({
        "archetype": "TST-021",
        "module": "jmeter",
        "service_name": "reference-sut",
        "tier": "T0",
        "oracle": "invariant-assertion",
        "result": "passed",
        "unexpected_field": "this should never be here",
        "evidence": {
            "executed_on": "2026-01-01",
            "environment": "local-compose",
            "report_path": "qe-harness/traceability/runs/2026-01-01T00-00-00-000000Z-TST-021.json",
        },
    }))
    result = run(tmp_path)
    assert result.returncode == 1
    rel = bad_fragment.relative_to(tmp_path).as_posix()
    assert ("check7 %s fails evidence.schema.json" % rel) in result.stdout

def test_check7_flags_invariant_id_that_does_not_match_the_pattern(tmp_path):
    # Real bug this check caught live in this repo: Tst030ContractRunner's
    # scenarioId() used to emit "SCN-v1"/"SCN-v2", which never matched
    # evidence.schema.json's invariants[].id pattern ("^I[0-9]+$").
    runs_dir = _seed_evidence_schema(tmp_path)
    runs_dir.mkdir(parents=True)
    bad_fragment = runs_dir / "2026-01-01T00-00-00-000000Z-TST-030.json"
    bad_fragment.write_text(json.dumps({
        "archetype": "TST-030",
        "module": "gatling-karate",
        "service_name": "reference-sut",
        "tier": "T0",
        "oracle": "contract-schema",
        "result": "passed",
        "invariants": [
            {"id": "SCN-v1", "description": "v1 transfer contract", "result": "passed"}
        ],
        "evidence": {
            "executed_on": "2026-01-01",
            "environment": "local-compose",
            "report_path": "qe-harness/traceability/runs/2026-01-01T00-00-00-000000Z-TST-030.json",
        },
    }))
    result = run(tmp_path)
    assert result.returncode == 1
    rel = bad_fragment.relative_to(tmp_path).as_posix()
    assert ("check7 %s fails evidence.schema.json" % rel) in result.stdout

def test_flags_partial_without_reason(tmp_path):
    mods = tmp_path / "qe-harness/traceability"
    mods.mkdir(parents=True)
    (mods / "modules.yml").write_text(textwrap.dedent("""
        version: 1
        modules:
          - archetype: TST-043
            tool: k6
            path: qe-harness/harness/k6/tst-043
            coverage: partial
            defect_flag: cache-headers-absent
    """))
    result = run(tmp_path)
    assert result.returncode == 1
    assert "partial coverage without partial_reason" in result.stdout
