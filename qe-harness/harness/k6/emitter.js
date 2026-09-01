'use strict';

/**
 * JavaScript evidence emitter (Task 22, Wave 16).
 *
 * Mirrors `qe-harness/harness/common/src/main/java/.../evidence/{RunFragment,
 * EvidenceEmitter}.java` and `qe-harness/harness/locust/emitter.py`
 * field-for-field, so a fragment built by this module and one built by
 * either of the other two emitters validate against the exact same
 * `qe-harness/traceability/evidence.schema.json` (Task 2) -- all three
 * languages must never drift on what "a valid run fragment" means:
 *
 *   - `result` (top level) is derived, never accepted verbatim from the
 *     caller: `failed` if any invariant/threshold failed, else `passed` if
 *     anything was actually evaluated, else `not-evaluated`. This is
 *     `RunFragment.result()`'s exact rule -- a run that checked nothing must
 *     never silently report `passed`.
 *   - a `not-evaluated` threshold MUST carry a `reason`. The JVM emitter
 *     enforces this itself, in `RunFragment.Builder#threshold`, as an
 *     `IllegalArgumentException` guard; the Python emitter enforces it via
 *     `evidence.schema.json` itself (its `thresholds[].allOf/if/then`) and
 *     surfaces the resulting schema-validation failure as a `ValueError`.
 *     This emitter follows the Python emitter's approach exactly -- the
 *     schema is the one place the rule is defined, so all three emitters
 *     can never disagree about it -- and surfaces the failure as a plain
 *     `Error` thrown from `emitFragment`.
 *   - optional empty/absent fields (`invariants`, `thresholds`, `sut_defect`)
 *     are omitted from the JSON entirely, never serialised as `null`/`[]`,
 *     matching the JVM emitter's `JsonInclude.Include.NON_NULL` +
 *     empty-list-to-null behaviour.
 *
 * Interface shape (per Task 22's brief): `emitFragment(obj) -> string`.
 * Unlike the JVM/Python emitters (which own writing the fragment file to
 * disk, in-process, from inside the very tool that ran the oracle), this
 * function never touches disk -- it only builds and schema-validates the
 * fragment, returning the JSON text. See `../k6/tst-043-clientexp/README.md`
 * ("Why emission is a separate Node step") for why: `script.js` runs inside
 * k6's own sandboxed JS engine (goja), which cannot load `ajv` -- confirmed
 * empirically: `ajv`'s own internal module graph (relative, extension-less
 * requires resolved by Node's module algorithm) does not resolve under k6's
 * much simpler loader, failing with `moduleSpecifier "./core" couldn't be
 * found`. `emitFragment` is instead called from a plain Node process
 * (`write-fragment.js`, invoked by `bin/run-k6.sh` after k6 finishes), which
 * does have real npm/ajv available, and which validates the fragment via
 * this function before writing it anywhere -- exactly what the other two
 * emitters do in-process, just relocated one process over.
 */

const fs = require('fs');
const path = require('path');
// evidence.schema.json declares "$schema":
// "https://json-schema.org/draft/2020-12/schema" -- ajv's default export
// (require('ajv')) only understands draft-07; draft 2020-12 needs this
// dedicated build (confirmed empirically: the plain import throws "no
// schema with key or ref .../2020-12/schema" at compile time). Same rule
// the Python emitter gets for free from `jsonschema` (which supports
// 2020-12 out of the box, no separate import needed).
const Ajv2020 = require('ajv/dist/2020');

// harness/k6/emitter.js -> '..' = harness, '..' = qe-harness
const SCHEMA_PATH = path.join(__dirname, '..', '..', 'traceability', 'evidence.schema.json');
const RUNS_DIR_REL = 'qe-harness/traceability/runs';

let cachedSchema = null;

function loadSchema() {
  if (!cachedSchema) {
    cachedSchema = JSON.parse(fs.readFileSync(SCHEMA_PATH, 'utf8'));
  }
  return cachedSchema;
}

/** FAILED if any invariant/threshold failed; else NOT_EVALUATED if nothing
 *  was evaluated; else PASSED. Mirrors RunFragment.result() (Java) /
 *  _overall_result (Python) exactly. */
function overallResult(entries) {
  if (entries.some((entry) => entry.result === 'failed')) {
    return 'failed';
  }
  if (entries.some((entry) => entry.result === 'passed')) {
    return 'passed';
  }
  return 'not-evaluated';
}

/** `<ISO-instant, hyphenated>Z`, matching EvidenceEmitter.emit's /
 *  emit_fragment's own filename shape (millisecond precision here vs.
 *  microsecond in Java/Python -- immaterial, both are unique-enough,
 *  lexically-sortable timestamps for a single evidence fragment). */
function timestampForFilename(date) {
  return date.toISOString().replace(/[:.]/g, '-');
}

/**
 * Builds one evidence fragment from `fragment` (the same shape the JVM
 * `RunFragment.Builder`/Python `emit_fragment` accept: `archetype`,
 * `module`, `service_name`, `tier`, `oracle` (required, passed straight
 * through); `environment` (required, nested under `evidence` in the
 * output); optional `invariants`/`thresholds` lists (each item exactly
 * `{id, description, result}` / `{name, threshold_ref, result, reason?}`);
 * an optional `sut_defect` (nested under `evidence`, omitted when absent);
 * and an optional `report_path`, defaulted to the canonical
 * `qe-harness/traceability/runs/<timestamp>-<archetype>.json` shape every
 * other module's fragments use, since this function itself never chooses
 * where a caller ultimately writes the string it returns), validates it
 * against `evidence.schema.json`, and returns the validated JSON text.
 *
 * Throws a plain `Error` (never a raw `ajv` exception) on any schema
 * violation -- a schema violation is a programming error in the caller
 * (`write-fragment.js`), not a legitimate "failed" oracle result.
 */
function emitFragment(fragment) {
  const invariants = fragment.invariants ? [...fragment.invariants] : [];
  const thresholds = fragment.thresholds ? [...fragment.thresholds] : [];

  const archetype = fragment.archetype;
  const executedOn = fragment.executed_on || new Date().toISOString().slice(0, 10);
  const reportPath = fragment.report_path
    || `${RUNS_DIR_REL}/${timestampForFilename(new Date())}-${archetype}.json`;

  const evidence = {
    executed_on: executedOn,
    environment: fragment.environment,
    report_path: reportPath,
  };
  if (fragment.sut_defect != null) {
    evidence.sut_defect = fragment.sut_defect;
  }

  const document = {
    archetype,
    module: fragment.module,
    service_name: fragment.service_name,
    tier: fragment.tier,
    oracle: fragment.oracle,
    result: fragment.result || overallResult([...invariants, ...thresholds]),
    evidence,
  };
  if (invariants.length) {
    document.invariants = invariants;
  }
  if (thresholds.length) {
    document.thresholds = thresholds;
  }

  // strict: false -- same reason emit_fragment's Python counterpart never
  // passes a jsonschema FormatChecker: evidence.schema.json's
  // `evidence.executed_on` declares `"format": "date"`, and ajv's default
  // strict mode treats an unrecognised format keyword as a hard compile
  // error rather than a no-op (confirmed empirically). Neither the JVM
  // emitter (which always serialises a real `LocalDate`) nor the Python
  // emitter (`jsonschema.validate` with no format_checker, which never
  // validates `format` at all) enforces the date format any more strictly
  // than this does -- all three emitters agree that shape-correctness, not
  // format-string-correctness, is what the schema check is for. `logger:
  // false` only silences ajv's own "unknown format ... ignored" console
  // warning that `strict: false` still prints on every compile -- purely
  // cosmetic, does not change what is validated.
  const ajv = new Ajv2020({ strict: false, allErrors: true, logger: false });
  const validate = ajv.compile(loadSchema());
  if (!validate(document)) {
    const detail = ajv.errorsText(validate.errors, { separator: '; ' });
    throw new Error(
      `emitFragment: fragment for ${archetype} fails evidence.schema.json: ${detail}`
    );
  }

  return `${JSON.stringify(document, null, 2)}\n`;
}

module.exports = { emitFragment, overallResult, timestampForFilename };
