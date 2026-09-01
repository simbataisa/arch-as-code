import pathlib
import subprocess
import sys
import textwrap

SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "render-harness-coverage.py"


def run(args):
    return subprocess.run(
        [sys.executable, str(SCRIPT), *args], capture_output=True, text=True
    )


def setup_tree(tmp_path):
    """Build a minimal --root tree with its own modules.yml.

    Only qe-harness/traceability/ moves under --root — the archetype
    catalog (knowledge-base/testing/README.md) is fixed corpus and is
    always read from the real repository, matching
    validate-harness-coverage.py's fixed-corpus convention. This fixture
    only needs a modules.yml with one full and one partial entry.
    """
    traceability = tmp_path / "qe-harness/traceability"
    traceability.mkdir(parents=True)
    (traceability / "modules.yml").write_text(
        textwrap.dedent(
            """\
            version: 1
            modules:
              - archetype: TST-021
                tool: jmeter
                path: qe-harness/harness/jmeter/tst-021-ledger
                coverage: full
                defect_flag: ledger-unbalanced
              - archetype: TST-043
                tool: k6
                path: qe-harness/harness/k6/tst-043-clientexp
                coverage: partial
                partial_reason: >-
                  Requires offline-sync capability that needs a live client app; this
                  repository does not contain one.
                defect_flag: cache-headers-absent
            """
        )
    )


def test_check_mode_fails_when_file_is_stale(tmp_path):
    setup_tree(tmp_path)
    (tmp_path / "qe-harness/traceability/harness-coverage.md").write_text("stale\n")
    r = run(["--check", "--root", str(tmp_path)])
    assert r.returncode == 1
    assert "stale" in r.stdout.lower()


def test_render_marks_partial_rows_visibly(tmp_path):
    setup_tree(tmp_path)
    run(["--root", str(tmp_path)])
    text = (tmp_path / "qe-harness/traceability/harness-coverage.md").read_text()
    assert "partial" in text
    assert "offline-sync" in text, "partial rows must carry their reason inline"
