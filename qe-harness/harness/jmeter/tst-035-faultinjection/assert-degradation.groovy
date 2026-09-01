// TST-035 fault-injection & degraded-response assertion (Task 18).
//
// Runs once, as the SECOND sampler in the TearDown Thread Group --
// "restore-downstream" (toxic-control.groovy, parameters="remove") always
// runs first and unconditionally; see plan.jmx's PostThreadGroup-level
// comment for why that ordering is the module's single most important
// correctness property. By the time this script runs, the Toxiproxy fault
// is already gone, which is exactly the precondition I3 below needs to
// observe genuine recovery rather than a fault that never went away.
//
// I3 is checked here, not by a separate upstream sampler, via a direct poll
// of GET /quotes/Q1 -- the same "reach the SUT directly from the assertion
// script" pattern assert-trial-balance.groovy uses for its own Postgres
// query, because no dedicated HTTP sampler needs to exist in the plan just
// to serve one polling loop that belongs conceptually to this invariant's
// own evaluation.
//
// Bound variables the JSR223 Sampler receives from the JMeter engine:
//   props - JMeter's single cross-thread shared store; holds the "Fault
//           Load" Thread Group's own tallies (tst035_total_calls/
//           _total_5xx/_shape_violations), written by its own
//           "tally-response" JSR223PostProcessor in plan.jmx.
//   log   - JMeter's SLF4J logger for this element.

import com.techcombank.qe.harness.config.HarnessConfig
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

long totalCalls = Long.parseLong(props.getProperty("tst035_total_calls", "0"))
long total5xx = Long.parseLong(props.getProperty("tst035_total_5xx", "0"))
long shapeViolations = Long.parseLong(props.getProperty("tst035_shape_violations", "0"))

// ---- I3: poll for recovery now that the fault is gone ----
//
// Smoke mode shortens the poll BUDGET, never what is checked -- I1-I3 are
// evaluated identically either way, the same rule TST-031's README states
// for its own smoke/full split. The reference SUT's breaker config
// (waitDurationInOpenState: 2s, permittedNumberOfCallsInHalfOpenState: 2,
// automaticTransitionFromOpenToHalfOpenEnabled: true -- application.yml's
// TST-035 comment block) self-heals within a couple of seconds regardless
// of run mode, so even the smoke budget below carries wide margin over
// that -- same rationale as BreakerBehaviourTest's own 20-second
// awaitClosed() budget against the identical 2-second wait duration.
boolean smoke = HarnessConfig.smokeMode()
Duration recoveryBudget = smoke ? Duration.ofSeconds(8) : Duration.ofSeconds(15)
long pollIntervalMs = 300L

String sutBaseUrl = System.getenv().getOrDefault("SUT_BASE_URL", "http://localhost:8080")
HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
HttpRequest quoteRequest = HttpRequest.newBuilder(URI.create(sutBaseUrl + "/quotes/Q1"))
    .timeout(Duration.ofSeconds(5))
    .GET()
    .build()

boolean recovered = false
long deadlineNanos = System.nanoTime() + recoveryBudget.toNanos()
while (System.nanoTime() < deadlineNanos) {
    totalCalls++
    HttpResponse<String> response = http.send(quoteRequest, HttpResponse.BodyHandlers.ofString())
    int status = response.statusCode()
    if (status >= 500) {
        total5xx++
    } else if (status == 200) {
        def json = new JsonSlurper().parseText(response.body())
        boolean degraded = json.degraded
        String source = json.source
        // Same DegradedResponse.live()/cached() shape check the Fault Load
        // Thread Group's own tally-response applies to every in-fault
        // response -- reapplied here so a shape regression that only shows
        // up on the live/recovered path (source != "live" with
        // degraded=false) is caught too, not just the degraded/cache path.
        boolean shapeOk = degraded ? (source == "cache") : (source == "live")
        if (!shapeOk) {
            shapeViolations++
        }
        if (!degraded) {
            recovered = true
            break
        }
    }
    Thread.sleep(pollIntervalMs)
}

// run-defects.sh exports QE_SUT_DEFECT (the active defect flag) as a plain
// process environment variable right before invoking run-module.sh -- same
// mechanism every other env var this module reads already relies on.
// Blank/absent means omitted -- an ordinary clean run must never carry this
// field (I4).
String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1", "Downstream failure never yields a 5xx response",
    { total5xx == 0L } as java.util.function.BooleanSupplier)

RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "Degraded response matches the declared shape (degraded -> source=cache, live -> source=live)",
    { shapeViolations == 0L } as java.util.function.BooleanSupplier)

RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "Breaker recovers (degraded=false) within " + recoveryBudget.toSeconds() + "s after the fault is removed",
    { recovered } as java.util.function.BooleanSupplier)

RunFragment fragment = RunFragment.builder()
    .archetype(System.getenv("QE_ARCHETYPE"))
    .module("jmeter")
    .serviceName("reference-sut")
    .tier("T0")
    .oracle("invariant-assertion")
    .environment(System.getenv().getOrDefault("QE_ENVIRONMENT", "local-compose"))
    .sutDefect(sutDefect)
    .invariant(i1.id(), i1.description(), i1.result())
    .invariant(i2.id(), i2.description(), i2.result())
    .invariant(i3.id(), i3.description(), i3.result())
    .build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "I1 no-5xx-on-downstream-failure: ${i1.result().wire()} (total5xx=${total5xx}, totalCalls=${totalCalls})\n" +
    "I2 degraded-response-shape: ${i2.result().wire()} (shapeViolations=${shapeViolations})\n" +
    "I3 breaker-recovers-after-fault-removed: ${i3.result().wire()} (recovered=${recovered}, budget=${recoveryBudget})\n"
).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
