import subprocess, sys, textwrap, pathlib

SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "validate-harness-coverage.py"

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
