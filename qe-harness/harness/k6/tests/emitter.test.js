'use strict';

/** Task 22's Step 1 tests, given verbatim in the task brief. */

const fs = require('fs');
const path = require('path');
// evidence.schema.json is a draft 2020-12 schema; see emitter.js's own
// comment on why this needs ajv's dedicated 2020-12 build, not the plain
// `require('ajv')` default (draft-07 only).
const Ajv2020 = require('ajv/dist/2020');

const { emitFragment } = require('../emitter');

// tests/emitter.test.js -> '..' = k6, '..' = harness, '..' = qe-harness
const SCHEMA_PATH = path.join(__dirname, '..', '..', '..', 'traceability', 'evidence.schema.json');
const schema = JSON.parse(fs.readFileSync(SCHEMA_PATH, 'utf8'));
const ajv = new Ajv2020({ strict: false });

test('js emitter output validates against the shared schema', () => {
  const out = emitFragment({
    archetype: 'TST-043', module: 'k6', service_name: 'reference-sut',
    tier: 'T0', oracle: 'invariant-assertion',
    invariants: [{ id: 'I1', description: 'ETag present', result: 'passed' }],
    environment: 'ci-smoke',
  });
  const validate = ajv.compile(schema);
  expect(validate(JSON.parse(out))).toBe(true);
});

test('js emitter rejects not-evaluated threshold without reason', () => {
  expect(() => emitFragment({
    archetype: 'TST-043', module: 'k6', service_name: 'reference-sut',
    tier: 'T0', oracle: 'invariant-assertion', environment: 'ci-smoke',
    thresholds: [{ name: 'payload_kb', threshold_ref: 'NFR-004#payload-budget',
                   result: 'not-evaluated' }],
  })).toThrow();
});
