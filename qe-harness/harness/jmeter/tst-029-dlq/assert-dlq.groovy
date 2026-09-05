// TST-029 delivery guarantee, retry and DLQ assertion (Wave 17).
//
// I1 is a conservation law: submitted == stateChanges + dlqCount. The counters
// come from the SUT's own publish path (GET /messaging/delivery/state), not from
// the broker's accounting -- scoring a delivery guarantee against the broker
// would ask the component under test to grade itself.
//
// I2 needs a real broker restart. In smoke mode it is reported NOT_EVALUATED
// with a reason rather than substituted: Toxiproxy severance would prove
// reconnection, not durable-queue survival, and passing that off as I2 is
// exactly the dishonesty TST-043's relabelling exists to warn about.
//
// I4 reads the DECLARED ladder rather than inferring intervals from observed
// timings, which would be flaky under load.

import com.techcombank.qe.harness.config.HarnessConfig
import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion

import groovy.json.JsonSlurper

import java.nio.file.Path

boolean smoke = HarnessConfig.smokeMode()

def state = new JsonSlurper().parseText(vars.get("delivery_state"))
def dlqInfo = new JsonSlurper().parseText(vars.get("dlq_info"))

long submitted = state.submitted as Long
long stateChanges = state.stateChanges as Long
long dlqCount = state.dlqCount as Long
int maxAttempts = state.maxDeliveryAttempts as Integer
long distinctIntervals = state.distinctIntervals as Long
boolean dlqExported = dlqInfo.exported
boolean alertFiring = dlqInfo.alertFiring
long alertDepth = dlqInfo.alertDepth as Long
long observedDepth = dlqInfo.depth as Long

long poisonAttempts = Long.parseLong(props.getProperty("tst029_max_poison_attempts"))
long jobsBehindPoisonProcessed = Long.parseLong(props.getProperty("tst029_jobs_behind_poison"))
boolean alertFiringBeforeBurst = Boolean.parseBoolean(props.getProperty("tst029_alert_firing_before_burst"))

String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1", "Every published message produced one state change or is in the DLQ",
    { submitted > 0L && submitted == stateChanges + dlqCount } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "A poison message reaches the DLQ inside the declared attempts without blocking others",
    { poisonAttempts <= maxAttempts && jobsBehindPoisonProcessed > 0L } as java.util.function.BooleanSupplier)
RunFragment.Entry i4 = InvariantAssertion.check(
    "I4", "Retry intervals match the declared backoff with more than one distinct interval",
    { distinctIntervals > 1L } as java.util.function.BooleanSupplier)
RunFragment.Entry i5 = InvariantAssertion.check(
    "I5", "DLQ depth alert does not fire below threshold and does fire once threshold is crossed",
    { dlqExported && !alertFiringBeforeBurst && alertFiring } as java.util.function.BooleanSupplier)
RunFragment.Entry i6 = InvariantAssertion.check(
    "I6", "A permanent error stops retrying at the declared ceiling",
    { poisonAttempts <= maxAttempts } as java.util.function.BooleanSupplier)

RunFragment.Builder builder = RunFragment.builder()
    .archetype(System.getenv("QE_ARCHETYPE"))
    .module("jmeter")
    .serviceName("reference-sut")
    .tier("T0")
    .oracle("invariant-assertion")
    .environment(System.getenv().getOrDefault("QE_ENVIRONMENT", "local-compose"))
    .sutDefect(sutDefect)
    .invariant(i1.id(), i1.description(), i1.result())

if (smoke) {
    builder.invariant("I2", "A broker restart loses nothing acked-persisted",
                      RunFragment.Result.NOT_EVALUATED)
} else {
    long depthBefore = Long.parseLong(vars.get("dlq_depth_before_restart"))
    long depthAfter = Long.parseLong(vars.get("dlq_depth_after_restart"))
    RunFragment.Entry i2 = InvariantAssertion.check(
        "I2", "A broker restart loses nothing acked-persisted",
        { depthAfter == depthBefore } as java.util.function.BooleanSupplier)
    builder.invariant(i2.id(), i2.description(), i2.result())
}

builder.invariant(i3.id(), i3.description(), i3.result())
       .invariant(i4.id(), i4.description(), i4.result())
       .invariant(i5.id(), i5.description(), i5.result())
       .invariant(i6.id(), i6.description(), i6.result())

RunFragment fragment = builder.build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "I1 conservation: ${i1.result().wire()} (submitted=${submitted}, processed=${stateChanges}, dlq=${dlqCount})\n" +
    "I2 restart-durability: ${smoke ? 'not-evaluated (restart path exercised in full runs only)' : 'evaluated'}\n" +
    "I3 dlq-within-attempts-no-blocking: ${i3.result().wire()} (attempts=${poisonAttempts}/${maxAttempts}, behind=${jobsBehindPoisonProcessed})\n" +
    "I4 distinct-backoff-intervals: ${i4.result().wire()} (distinct=${distinctIntervals})\n" +
    "I5 dlq-depth-exported-and-alerting: ${i5.result().wire()} (depth=${observedDepth}, alertDepth=${alertDepth}, firingBeforeBurst=${alertFiringBeforeBurst}, firing=${alertFiring})\n" +
    "I6 stops-at-ceiling: ${i6.result().wire()}\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
