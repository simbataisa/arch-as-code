// TST-040 authz-matrix and clock-skew assertion (Task 19).
//
// Runs once, sole sampler in the TearDown Thread Group, after every "Authz
// Matrix Sweep" iteration has finished writing its own cell's RAW evidence
// (status code, X-Authz-Decision header, response body) into props -- same
// "assert after load has fully drained" placement TST-021/TST-031/TST-035's
// own TearDown Thread Groups use.
//
// classify() below is TST-040's three-outcome oracle, and the single most
// load-bearing piece of logic in this module: a 401/403 that carries no
// X-Authz-Decision header is classified "error", never "deny" -- see
// AuthzDecisionFilter's own Javadoc in reference-sut (Task 9) for why a bare
// denial-shaped status code must never be mistaken for a correct, deliberate
// authorization denial. The authz-missing-marker defect strips that header
// from every response, which is exactly what
// Tst040ModuleTest#classifiesBareForbiddenAsErrorNotDeny proves this module
// catches: every deny cell in the matrix then classifies as "error" instead
// of "deny", so I1 fails and the whole run reports FAILED. A two-outcome
// oracle (status < 400 ? allow : deny) would collapse "error" into "deny",
// scoring a crashed/misconfigured policy layer as a clean denial -- exactly
// the fail-open-vs-fail-closed confusion the archetype's three-outcome
// oracle (authn-authz-token-lifecycle.md §3) exists to prevent.
//
// I2's clock-skew sweep is a genuine MEASUREMENT, not an assertion against a
// hardcoded number: it presents progressively staler tokens (minted via the
// reference SUT's own POST /_test/token/expired -- a Task 19 follow-up
// addition to reference-sut, since JwtService#mintExpiredAccessToken is a
// plain Java method with no HTTP door of its own; see that controller's
// Javadoc) until the SUT first rejects one, and records the largest offset
// still accepted -- the same sweep-to-first-rejection shape
// TokenLifecycleTest's in-process equivalent uses, run here over real HTTP
// from a separate process instead.
//
// Bound variables the JSR223 Sampler receives from the JMeter engine:
//   props - JMeter's single cross-thread shared store; holds every cell's
//           raw evidence, written by "Authz Matrix Sweep"'s own
//           "record-cell-result" JSR223PostProcessor in plan.jmx, plus the
//           tst040_token_<role> values "Mint Tokens..." (setUp Thread
//           Group) wrote before load.
//   log    - JMeter's SLF4J logger element.

import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion
import groovy.json.JsonSlurper

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.time.Duration

// TST-040's three-outcome oracle (archetype §3, authn-authz-token-lifecycle.md):
//   allow -- 2xx AND the body carries the expected resource payload
//   deny  -- 401 or 403 AND the response carries an explicit
//            X-Authz-Decision: deny marker (AuthzDecisionFilter, Task 9)
//   error -- anything else: a bare 401/403 with no marker, any 5xx, an
//            unparseable/wrong 2xx body, or a connect/read failure
//            (status -1, see record-cell-result in plan.jmx)
// A two-outcome form (status < 400 ? allow : deny) would collapse "error"
// into "deny" -- precisely the defect authz-missing-marker exists to prove
// this module catches.
def classify(int status, String decisionHeader, String body, String endpoint) {
    if (status >= 200 && status < 300) {
        String expectedResource = endpoint.substring(endpoint.lastIndexOf('/') + 1)
        boolean bodyOk = body != null && body.contains('"resource":"' + expectedResource + '"')
        return bodyOk ? "allow" : "error"
    } else if (status == 401 || status == 403) {
        return "deny".equals(decisionHeader) ? "deny" : "error"
    } else {
        return "error"
    }
}

// ---- I1: every matrix cell's outcome matches its declared expected_verdict ----
//
// Compared against expected_verdict FROM THE CSV, never against a second
// live code path (e.g. a direct-to-service call) -- if both were wrong in
// the same direction, a path-to-path comparison would report green on a
// system that is broken outright. This reference SUT has no separate
// gateway hop to bypass (unlike the full archetype's I2), so that
// distinction does not apply here; expected_verdict is the only oracle this
// module ever reads.

String cellKeysProp = props.getProperty("tst040_cell_keys", "")
List<String> cellKeys = cellKeysProp.isEmpty() ? [] : cellKeysProp.split(";").toList()

List<String> mismatches = []
StringBuilder cellReport = new StringBuilder()
for (String cellKey : cellKeys) {
    String endpoint = props.getProperty("tst040_cell_" + cellKey + "_endpoint", "")
    String expected = props.getProperty("tst040_cell_" + cellKey + "_expected", "")
    int status = Integer.parseInt(props.getProperty("tst040_cell_" + cellKey + "_status", "-1"))
    String decision = props.getProperty("tst040_cell_" + cellKey + "_decision", "")
    String body = props.getProperty("tst040_cell_" + cellKey + "_body", "")

    String outcome = classify(status, decision, body, endpoint)
    // outcome == "error" always fails the cell, even if expected_verdict
    // happens to be "deny" -- the archetype's own companion assertion
    // ("assert outcome != error" alongside every deny check) exists so an
    // error is reported as its own distinct failure category, never
    // silently absorbed into a passing deny count.
    boolean cellOk = (outcome == expected) && (outcome != "error")
    if (!cellOk) {
        mismatches.add("${cellKey} (expected=${expected}, got=${outcome}, status=${status}, decision='${decision}')")
    }
    cellReport.append(cellKey).append(": expected=").append(expected).append(" got=").append(outcome)
        .append(cellOk ? "" : " MISMATCH").append("\n")
}

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1",
    "Every authorisation-matrix cell's outcome matches its expected_verdict under the " +
        "three-outcome oracle (allow/deny/error); a 401/403 with no X-Authz-Decision marker " +
        "is classified error, not deny",
    { cellKeys.size() == 12 && mismatches.isEmpty() } as java.util.function.BooleanSupplier)

// ---- I2: measured maximum accepted exp offset stays within the declared clock-skew tolerance ----

// app.authz.clock-skew-seconds (reference-sut/src/main/resources/application.properties):
// the *declared* tolerance JwtService's JJWT parser is actually configured with -- no HTTP
// surface exposes this value back to an external caller (the same limitation
// assert-ratelimit.groovy documents for its own hardcoded configuredLimitRps), so this must be
// kept in sync by hand if that property ever changes; see README.md.
long declaredClockSkewToleranceSeconds = 5L
// Comfortably above the declared tolerance so the sweep always finds a real
// rejection rather than exhausting the range inconclusively -- same
// SWEEP_MAX_SECONDS margin TokenLifecycleTest's in-process sweep uses.
long sweepMaxSeconds = 30L

String sutBaseUrl = System.getenv().getOrDefault("SUT_BASE_URL", "http://localhost:8080")
HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
JsonSlurper jsonSlurper = new JsonSlurper()

long maxAcceptedOffset = -1L
boolean sweepTerminated = false
for (long offset = 0L; offset <= sweepMaxSeconds; offset++) {
    HttpRequest mintRequest = HttpRequest.newBuilder(URI.create(sutBaseUrl + "/_test/token/expired"))
        .timeout(Duration.ofSeconds(5))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString('{"role":"reader","secondsPastExpiry":' + offset + '}'))
        .build()
    HttpResponse<String> mintResponse = http.send(mintRequest, HttpResponse.BodyHandlers.ofString())
    if (mintResponse.statusCode() != 200) {
        throw new IllegalStateException(
            "POST /_test/token/expired offset=" + offset + " -> " + mintResponse.statusCode()
                + " " + mintResponse.body())
    }
    String token = jsonSlurper.parseText(mintResponse.body()).accessToken as String

    HttpRequest probeRequest = HttpRequest.newBuilder(URI.create(sutBaseUrl + "/protected/read"))
        .timeout(Duration.ofSeconds(5))
        .header("Authorization", "Bearer " + token)
        .GET()
        .build()
    HttpResponse<String> probeResponse = http.send(probeRequest, HttpResponse.BodyHandlers.ofString())
    if (probeResponse.statusCode() == 200) {
        maxAcceptedOffset = offset
    } else {
        sweepTerminated = true
        break
    }
}
if (!sweepTerminated) {
    // An inconclusive sweep is a failed measurement, not a passing one --
    // InvariantAssertion.check's condition below already scores this FAILED
    // (sweepTerminated is false), this just makes the reason loud in the log.
    log.error("TST-040 clock-skew sweep exhausted " + sweepMaxSeconds
        + "s without ever observing a rejection -- widen sweepMaxSeconds, the validator's "
        + "real leeway was never found")
}

RunFragment.Entry i2 = InvariantAssertion.check(
    "I2",
    "Measured maximum accepted exp offset (${maxAcceptedOffset}s) stays within the declared " +
        "clock-skew tolerance (${declaredClockSkewToleranceSeconds}s)",
    { sweepTerminated && maxAcceptedOffset <= declaredClockSkewToleranceSeconds } as java.util.function.BooleanSupplier)

RunFragment fragment = RunFragment.builder()
    .archetype(System.getenv("QE_ARCHETYPE"))
    .module("jmeter")
    .serviceName("reference-sut")
    .tier("T0")
    .oracle("invariant-assertion")
    .environment(System.getenv().getOrDefault("QE_ENVIRONMENT", "local-compose"))
    .invariant(i1.id(), i1.description(), i1.result())
    .invariant(i2.id(), i2.description(), i2.result())
    .build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "I1 authz-matrix-cells-match-expected-verdict: ${i1.result().wire()} " +
        "(cellsEvaluated=${cellKeys.size()}, mismatches=${mismatches.size()})\n" +
    cellReport.toString() +
    (mismatches.isEmpty() ? "" : mismatches.collect { "  MISMATCH: " + it }.join("\n") + "\n") +
    "I2 clock-skew-offset-within-declared-tolerance: ${i2.result().wire()} " +
        "(maxAcceptedOffset=${maxAcceptedOffset}s, declaredTolerance=${declaredClockSkewToleranceSeconds}s, " +
        "sweepTerminated=${sweepTerminated})\n"
).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
