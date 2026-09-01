#!/usr/bin/env node
'use strict';

/**
 * usage: node write-fragment.js <RAW_REPORT_JSON_PATH> <RUNS_DIR>
 *
 * The plain-Node half of TST-043's split emission (see `emitter.js`'s own
 * comment for why it is split at all): `script.js`'s `handleSummary` writes
 * a RAW report -- check results plus the measured payload size -- because
 * k6's own sandboxed JS engine cannot load `ajv`. This script reads that raw
 * report, builds the real evidence fragment via `emitFragment` (which DOES
 * validate against `evidence.schema.json`, in this real Node process), and
 * writes it to `<RUNS_DIR>/<timestamp>-<archetype>.json` -- the exact same
 * `<ISO-instant>-<archetype>.json` naming convention every other module's
 * `EvidenceEmitter`/`emit_fragment` already uses.
 *
 * Computes the timestamped filename itself, once, and passes it to
 * `emitFragment` as an explicit `report_path` -- rather than letting
 * `emitFragment` synthesise its own (it would, if omitted, but on a
 * different clock tick) -- so the fragment's own `evidence.report_path`
 * field always names the exact file this script goes on to write, never a
 * close-but-different one.
 *
 * Invoked only by `bin/run-k6.sh` (never directly by a test), mirroring
 * every other `run-<tool>.sh`'s own "never invoked directly" convention.
 */

const fs = require('fs');
const path = require('path');

const { emitFragment, timestampForFilename } = require('./emitter');

function fail(message) {
  process.stderr.write(`write-fragment.js: ${message}\n`);
  process.exit(1);
}

const [, , rawReportPath, runsDir] = process.argv;
if (!rawReportPath || !runsDir) {
  fail('usage: node write-fragment.js <RAW_REPORT_JSON_PATH> <RUNS_DIR>');
}

let raw;
try {
  raw = JSON.parse(fs.readFileSync(rawReportPath, 'utf8'));
} catch (err) {
  fail(`cannot read/parse raw report at ${rawReportPath}: ${err.message}`);
}

const archetype = raw.archetype;
if (!archetype) {
  fail(`raw report at ${rawReportPath} has no archetype`);
}

// Only the schema's own threshold shape survives into the fragment
// (`name`/`threshold_ref`/`result`/`reason?`) -- script.js's raw report
// additionally carries `measured_bytes`/`budget_bytes` for human debugging
// (see its own console.log/summaryText), which evidence.schema.json's
// `additionalProperties: false` would otherwise reject outright.
const thresholds = (raw.thresholds || []).map((t) => {
  const shaped = { name: t.name, threshold_ref: t.threshold_ref, result: t.result };
  if (t.reason) {
    shaped.reason = t.reason;
  }
  if (t.result === 'not-evaluated' && !shaped.reason) {
    // Defensive only -- script.js never emits not-evaluated today (I4
    // always runs), but a threshold missing its required reason must fail
    // loudly here rather than silently reach emitFragment's own guard with
    // a less useful error.
    fail(`threshold '${t.name}' is not-evaluated but raw report carries no reason`);
  }
  return shaped;
});

const timestamp = timestampForFilename(new Date());
const fileName = `${timestamp}-${archetype}.json`;
const reportPath = `qe-harness/traceability/runs/${fileName}`;

let fragmentText;
try {
  fragmentText = emitFragment({
    archetype,
    module: raw.module,
    service_name: raw.service_name,
    tier: raw.tier,
    oracle: raw.oracle,
    environment: raw.environment,
    invariants: raw.invariants || [],
    thresholds,
    report_path: reportPath,
  });
} catch (err) {
  fail(err.message);
}

fs.mkdirSync(runsDir, { recursive: true });
const outputPath = path.join(runsDir, fileName);
fs.writeFileSync(outputPath, fragmentText);

process.stdout.write(`${outputPath}\n`);
