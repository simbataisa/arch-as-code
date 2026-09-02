// TST-043 -- Client Experience Budget (k6). Task 22, Wave 16.
//
// Drives the reference SUT's `GET /catalogue` (Task 13's `CatalogueController`/
// `CachePolicyFilter`) once and checks four invariants/thresholds against the
// real, running response -- never a hardcoded literal standing in for one.
// `vus: 1, iterations: 1` is deliberate, the same "single thread, one fixed
// check" reasoning TST-040's `plan.jmx` documents for its own authz sweep:
// these are correctness/budget checks against one deterministic response
// (`CatalogueController`'s own Javadoc: "The response body is intentionally
// static across requests"), not a throughput measurement.
//
// See README.md for the full write-up, including why the evidence fragment
// itself is NOT built here: this script runs inside k6's own sandboxed JS
// engine (goja), which cannot load `ajv` to validate against
// `evidence.schema.json` in-process the way JMeter's Groovy or Locust's
// Python module can (confirmed empirically -- see `../emitter.js`'s own
// comment). `handleSummary` below only writes a RAW report (check results +
// the measured payload size) to `K6_RAW_REPORT_PATH`; `bin/run-k6.sh` then
// hands that raw report to `../write-fragment.js` (a plain Node script,
// which does have real npm/ajv) to build and write the actual fragment.

import http from 'k6/http';
import { check } from 'k6';
import { Gauge } from 'k6/metrics';

export const options = { vus: 1, iterations: 1 };

const payloadBytesMetric = new Gauge('payload_bytes');

const I1_NAME = 'I1 Cache-Control and ETag present';
const I2_NAME = 'I2 matching If-None-Match yields 304 with empty body';
const I3_NAME = 'I3 gzip applied when Accept-Encoding: gzip is sent';
const I4_NAME = 'I4 payload size within budget';

export default function () {
  const base = __ENV.SUT_BASE_URL || 'http://localhost:8080';

  // I1 -- a plain GET carries both Cache-Control and ETag.
  const plain = http.get(`${base}/catalogue`);
  check(plain, {
    [I1_NAME]: (r) => Boolean(r.headers['Cache-Control']) && Boolean(r.headers['Etag']),
  });

  // I2 -- presenting the ETag just observed via If-None-Match yields 304
  // with no body at all (CachePolicyFilter never calls copyBodyToResponse
  // on that path -- see its own Javadoc). Real measured ETag, not a
  // literal: a stale/guessed ETag would legitimately get a fresh 200 back,
  // which would make this check meaningless.
  const etag = plain.headers['Etag'];
  const conditional = http.get(`${base}/catalogue`, {
    headers: etag ? { 'If-None-Match': etag } : {},
  });
  const conditionalBodyEmpty = !conditional.body || conditional.body.length === 0;
  check(conditional, {
    [I2_NAME]: (r) => r.status === 304 && conditionalBodyEmpty,
  });

  // I3 -- advertising Accept-Encoding: gzip gets a genuinely gzip-encoded
  // response back (CachePolicyFilter compresses the body itself and sets
  // Content-Encoding -- see its own Javadoc on why this SUT does not rely
  // solely on Tomcat's connector-level compression).
  const gzipped = http.get(`${base}/catalogue`, {
    headers: { 'Accept-Encoding': 'gzip' },
  });
  check(gzipped, {
    [I3_NAME]: (r) => r.headers['Content-Encoding'] === 'gzip',
  });

  // I4 -- the UNCOMPRESSED response body (`plain`, the same request I1
  // checked) stays within the payload budget `bin/run-k6.sh` resolved from
  // `profiles/_nfr-thresholds.yml` (`payload_budget_bytes`) and passed in
  // via PAYLOAD_BUDGET_BYTES/PAYLOAD_BUDGET_REF -- this script never
  // hardcodes that number itself. See that entry's own comment in
  // `_nfr-thresholds.yml` for exactly what NFR-003 does and does not state,
  // and how the budget was derived from it.
  const measuredBytes = plain.body ? plain.body.length : 0;
  payloadBytesMetric.add(measuredBytes);
  const budgetBytes = Number(__ENV.PAYLOAD_BUDGET_BYTES);
  check(plain, {
    [I4_NAME]: () => Number.isFinite(budgetBytes) && measuredBytes <= budgetBytes,
  });

  console.log(
    `TST-043: measured payload ${measuredBytes} bytes, budget ${budgetBytes} bytes ` +
    `(${__ENV.PAYLOAD_BUDGET_REF || 'no ref resolved'})`
  );
}

/** Looks up one named check's pass/fail outcome in k6's own summary data
 *  (`data.root_group.checks[]`, each `{name, passes, fails}`), mapping it to
 *  this harness's three-value result vocabulary. A check absent from the
 *  summary entirely (should not happen -- every check above always runs
 *  exactly once) maps to `not-evaluated` rather than silently `passed`. */
function checkResult(data, name) {
  const entry = (data.root_group && data.root_group.checks || []).find((c) => c.name === name);
  if (!entry) {
    return 'not-evaluated';
  }
  if (entry.fails > 0) {
    return 'failed';
  }
  return entry.passes > 0 ? 'passed' : 'not-evaluated';
}

export function handleSummary(data) {
  const measuredBytes = (data.metrics.payload_bytes && data.metrics.payload_bytes.values.value) || 0;
  const budgetBytes = Number(__ENV.PAYLOAD_BUDGET_BYTES);
  const budgetRef = __ENV.PAYLOAD_BUDGET_REF;

  const rawReport = {
    archetype: __ENV.QE_ARCHETYPE || 'TST-043',
    module: 'k6',
    service_name: 'reference-sut',
    tier: 'T0',
    oracle: 'invariant-assertion',
    environment: __ENV.QE_ENVIRONMENT || 'local-compose',
    invariants: [
      { id: 'I1', description: 'Cache-Control and ETag present', result: checkResult(data, I1_NAME) },
      { id: 'I2', description: 'matching If-None-Match yields 304 with an empty body', result: checkResult(data, I2_NAME) },
      { id: 'I3', description: 'gzip applied when Accept-Encoding: gzip is sent', result: checkResult(data, I3_NAME) },
    ],
    thresholds: [
      {
        name: 'payload_budget_bytes',
        threshold_ref: budgetRef,
        result: checkResult(data, I4_NAME),
        measured_bytes: measuredBytes,
        budget_bytes: budgetBytes,
      },
    ],
  };

  // run-defects.sh exports QE_SUT_DEFECT (the active defect flag) as a
  // plain process environment variable right before invoking
  // run-module.sh -- k6 forwards the process environment into __ENV by
  // default, the same mechanism SUT_BASE_URL/QE_ARCHETYPE/QE_ENVIRONMENT
  // above already rely on. write-fragment.js (real Node, real ajv) is
  // where this actually gets validated and threaded into the fragment's
  // evidence.sut_defect -- see that file's own comment. Only set when
  // non-empty, so an ordinary clean run never carries this key at all (I4).
  if (__ENV.QE_SUT_DEFECT) {
    rawReport.sut_defect = __ENV.QE_SUT_DEFECT;
  }

  const summaryText =
    `TST-043 client-experience budget\n` +
    `  I1 (cache headers):    ${checkResult(data, I1_NAME)}\n` +
    `  I2 (conditional 304):  ${checkResult(data, I2_NAME)}\n` +
    `  I3 (gzip):             ${checkResult(data, I3_NAME)}\n` +
    `  I4 (payload budget):   ${checkResult(data, I4_NAME)} ` +
    `(measured ${measuredBytes}B, budget ${budgetBytes}B, ${budgetRef})\n`;

  const rawReportPath = __ENV.K6_RAW_REPORT_PATH || 'k6-raw-report.json';

  const outputs = {
    stdout: summaryText,
  };
  outputs[rawReportPath] = JSON.stringify(rawReport, null, 2);
  return outputs;
}
