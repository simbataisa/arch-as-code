import importlib.util
import pathlib
import subprocess
import sys
import textwrap

import pytest

SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "render-harness-coverage.py"


def run(args):
    return subprocess.run(
        [sys.executable, str(SCRIPT), *args], capture_output=True, text=True
    )


def load_module():
    """Load render-harness-coverage.py as an importable module (its
    filename is hyphenated, matching this scripts/ dir's house style), so
    load_catalog() can be unit-tested directly with TESTING_README
    monkeypatched to a broken fixture -- something subprocess-level tests
    can't do, since the real README is fixed corpus and never moves under
    --root.
    """
    spec = importlib.util.spec_from_file_location("render_harness_coverage", SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


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


def test_load_catalog_fails_loudly_when_readme_missing(tmp_path, monkeypatch, capsys):
    module = load_module()
    monkeypatch.setattr(module, "TESTING_README", tmp_path / "does-not-exist.md")
    with pytest.raises(SystemExit) as exc_info:
        module.load_catalog()
    assert exc_info.value.code == 2
    assert "ERROR" in capsys.readouterr().err


def test_load_catalog_fails_loudly_when_index_heading_reformatted(
    tmp_path, monkeypatch, capsys
):
    # Reproduces the reviewer's live repro: swap the index heading's em
    # dash for a plain hyphen. Before the fix this silently collapsed the
    # catalog to 0 rows with no error -- it must now fail loudly instead.
    module = load_module()
    broken = tmp_path / "README.md"
    broken.write_text(
        textwrap.dedent(
            """\
            ## Index - Archetypes

            ### Family A — Correctness & State (landed)

            | Catalog ID | Archetype | Covers |
            |---|---|---|
            | TST-020 | [Idempotency & Replay Safety](./archetypes/idempotency-replay.md) | BSP-002 |
            """
        )
    )
    monkeypatch.setattr(module, "TESTING_README", broken)
    with pytest.raises(SystemExit) as exc_info:
        module.load_catalog()
    assert exc_info.value.code == 2
    assert "ERROR" in capsys.readouterr().err


def test_load_catalog_fails_loudly_on_row_before_family_heading(
    tmp_path, monkeypatch, capsys
):
    module = load_module()
    broken = tmp_path / "README.md"
    broken.write_text(
        textwrap.dedent(
            """\
            ## Index — Archetypes

            | Catalog ID | Archetype | Covers |
            |---|---|---|
            | TST-020 | [Idempotency & Replay Safety](./archetypes/idempotency-replay.md) | BSP-002 |

            ### Family A — Correctness & State (landed)
            """
        )
    )
    monkeypatch.setattr(module, "TESTING_README", broken)
    with pytest.raises(SystemExit) as exc_info:
        module.load_catalog()
    assert exc_info.value.code == 2
    assert "ERROR" in capsys.readouterr().err
