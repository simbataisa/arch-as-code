// TST-023 concurrent limit and counter assertion (Wave 17).
//
// Runs as the sole sampler-producing element in the TearDown Thread Group,
// after every Reservation Burst thread has finished -- see plan.jmx and
// README.md for why the tallies cannot be read mid-run.
//
// Bound variables from the JMeter engine:
//   vars         - JMeterVariables for this thread; the teardown group's own
//                  HTTP samplers wrote i3_returned/i4_rejected/i5_timezone.
//   props        - cross-thread JMeter properties; the Reservation Burst
//                  group's PostProcessor tallied admitted/rejected counts and
//                  the running utilisation maximum here (the same mechanism
//                  tst-031-ratelimit's assert-ratelimit.groovy relies on).
//   SampleResult - this sampler's own result.
//   log          - JMeter's SLF4J logger for this element.
//
// No ThresholdResolver call and no threshold entry: I1/I2's bound is the
// account's own declared_limit, a fixture value read from the SUT's data. It
// is not a service SLO, so citing an NFR row for it would fabricate
// provenance -- see the design spec section 7.1.

import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion

import java.nio.file.Path

long declaredLimit = Long.parseLong(props.getProperty("tst023_declared_limit"))
long attempts      = Long.parseLong(props.getProperty("tst023_attempts"))
long admitted      = Long.parseLong(props.getProperty("tst023_admitted"))
long maxUtilisation = Long.parseLong(props.getProperty("tst023_max_utilisation"))

long i3Returned  = Long.parseLong(vars.get("i3_returned"))
boolean i4Rejected = Boolean.parseBoolean(vars.get("i4_rejected"))
String declaredTz  = vars.get("i5_timezone")

long expectedAdmitted = Math.min(attempts, declaredLimit)

String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1", "Admitted count equals min(N, L) under a genuine concurrency burst",
    { admitted == expectedAdmitted } as java.util.function.BooleanSupplier)
RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "Sampled utilisation never exceeds the declared limit at any instant",
    { maxUtilisation <= declaredLimit } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "Releasing a reservation returns exactly its own amount",
    { i3Returned == 0L } as java.util.function.BooleanSupplier)
RunFragment.Entry i4 = InvariantAssertion.check(
    "I4", "A second release of the same reservation is rejected",
    { i4Rejected } as java.util.function.BooleanSupplier)
RunFragment.Entry i5 = InvariantAssertion.check(
    "I5", "The window boundary uses the account's declared timezone",
    { declaredTz != null && !declaredTz.trim().isEmpty() } as java.util.function.BooleanSupplier)

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
    .invariant(i4.id(), i4.description(), i4.result())
    .invariant(i5.id(), i5.description(), i5.result())
    .build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "I1 admitted-equals-min: ${i1.result().wire()} (admitted=${admitted}, expected=${expectedAdmitted})\n" +
    "I2 utilisation-within-limit: ${i2.result().wire()} (max=${maxUtilisation}, limit=${declaredLimit})\n" +
    "I3 release-returns-own-amount: ${i3.result().wire()} (residual=${i3Returned})\n" +
    "I4 double-release-rejected: ${i4.result().wire()}\n" +
    "I5 declared-timezone-present: ${i5.result().wire()} (tz=${declaredTz})\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
