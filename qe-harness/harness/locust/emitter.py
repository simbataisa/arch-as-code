"""Python evidence emitter (Task 21, Wave 16).

Mirrors `qe-harness/harness/common/src/main/java/.../evidence/{RunFragment,
EvidenceEmitter}.java` field-for-field, so a fragment written by this module
and one written by the JVM emitter validate against the exact same
`qe-harness/traceability/evidence.schema.json` (Task 2) -- the two languages
must never drift on what "a valid run fragment" means:

  - `result` (top level) is derived, never accepted verbatim from the
    caller: `failed` if any invariant/threshold failed, else `passed` if
    anything was actually evaluated, else `not-evaluated`. This is
    `RunFragment.result()`'s exact rule -- a run that checked nothing must
    never silently report `passed`.
  - a `not-evaluated` threshold MUST carry a `reason`. The JVM emitter
    enforces this itself, in `RunFragment.Builder#threshold`, as an
    `IllegalArgumentException` guard; here it is enforced by
    `evidence.schema.json` itself (see that file's `thresholds[].allOf/
    if/then`) and surfaced as the schema-validation failure it already is,
    so the two emitters can never disagree about the rule -- only one of
    them (the schema) defines it.
  - optional empty/absent fields (`invariants`, `thresholds`, `sut_defect`)
    are omitted from the JSON entirely, never serialised as `null`/`[]`,
    matching the JVM emitter's `JsonInclude.Include.NON_NULL` +
    empty-list-to-null behaviour.

`emit_fragment` validates the fragment against the schema BEFORE writing
anything to disk. This emitter must never produce a fragment the
traceability gate (`scripts/validate-harness-coverage.py`) or a later
coverage merge would go on to reject -- a schema violation is a
programming error in the caller (locustfile.py), not a legitimate "failed"
oracle result, so it raises `ValueError`, not silently writing a
best-effort file.
"""

from __future__ import annotations

import json
from datetime import date, datetime, timezone
from pathlib import Path
from typing import Any

import jsonschema

# harness/locust/emitter.py -> parents[0]=locust, [1]=harness, [2]=qe-harness
_SCHEMA_PATH = Path(__file__).resolve().parents[2] / "traceability" / "evidence.schema.json"


def _overall_result(invariants: list[dict[str, Any]], thresholds: list[dict[str, Any]]) -> str:
    """FAILED if any invariant/threshold failed; else NOT_EVALUATED if nothing was
    evaluated; else PASSED. Mirrors RunFragment.result() (Java) exactly."""
    entries = [*invariants, *thresholds]
    if any(entry["result"] == "failed" for entry in entries):
        return "failed"
    if any(entry["result"] == "passed" for entry in entries):
        return "passed"
    return "not-evaluated"


def _report_path(output_path: Path) -> str:
    """Repo-relative path to the fragment, mirroring
    EvidenceEmitter.computeRepoRelativePath (Java): walk up looking for an
    ancestor directory literally named "qe-harness" and report relative to
    ITS parent (the repo root). Falls back to the bare filename when no such
    ancestor exists (e.g. a pytest `tmp_path` fixture, well outside the repo)."""
    absolute = output_path.resolve()
    for ancestor in absolute.parents:
        if ancestor.name == "qe-harness":
            return str(absolute.relative_to(ancestor.parent))
    return absolute.name


def emit_fragment(fragment: dict[str, Any], output_dir: Path) -> Path:
    """Write one evidence fragment under `output_dir`, after validating it
    against `evidence.schema.json`. Raises `ValueError` (never a raw
    `jsonschema` exception, and never a partially-written file) on any
    schema violation.

    `fragment` accepts the same shape `RunFragment.Builder` accepts in the
    JVM emitter: `archetype`, `module`, `service_name`, `tier`, `oracle`
    (all required, passed straight through); `environment` (required,
    nested under `evidence` in the output, matching the schema); optional
    `invariants`/`thresholds` lists (each item exactly `{id, description,
    result}` / `{name, threshold_ref, result, reason?}`); and an optional
    `sut_defect` (nested under `evidence`, omitted when absent/None).
    """
    invariants = list(fragment.get("invariants") or [])
    thresholds = list(fragment.get("thresholds") or [])

    archetype = fragment["archetype"]
    # <ISO-instant>-<archetype>.json, matching EvidenceEmitter.emit's own
    # filename shape (Instant.now().toString(), ":"/"." -> "-").
    timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H-%M-%S-%f") + "Z"
    output_path = Path(output_dir) / f"{timestamp}-{archetype}.json"

    evidence: dict[str, Any] = {
        "executed_on": fragment.get("executed_on") or date.today().isoformat(),
        "environment": fragment["environment"],
        "report_path": _report_path(output_path),
    }
    sut_defect = fragment.get("sut_defect")
    if sut_defect is not None:
        evidence["sut_defect"] = sut_defect

    document: dict[str, Any] = {
        "archetype": archetype,
        "module": fragment["module"],
        "service_name": fragment["service_name"],
        "tier": fragment["tier"],
        "oracle": fragment["oracle"],
        "result": fragment.get("result") or _overall_result(invariants, thresholds),
        "evidence": evidence,
    }
    if invariants:
        document["invariants"] = invariants
    if thresholds:
        document["thresholds"] = thresholds

    schema = json.loads(_SCHEMA_PATH.read_text())
    try:
        jsonschema.validate(document, schema)
    except jsonschema.exceptions.ValidationError as exc:
        raise ValueError(
            f"emit_fragment: fragment for {archetype} fails evidence.schema.json: {exc.message}"
        ) from exc

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(document, indent=2) + "\n")
    return output_path
