// TST-034 blended journey workload assertion (Wave 17).
//
// The first assertion script to read a TST-002 performance profile. The blend
// comes from profiles/mixed.yml via ProfileResolver, so the declared mix and
// the asserted mix cannot drift; per-journey tallies arrive through JMeter's
// cross-thread props map, the same mechanism assert-ratelimit.groovy uses.
//
// I1 resolves ONE THRESHOLD PER TIER and asserts each journey against its own,
// never against a single blended figure -- that distinction is the invariant.

import com.techcombank.qe.harness.config.HarnessConfig
import com.techcombank.qe.harness.config.ProfileResolver
import com.techcombank.qe.harness.config.ThresholdResolver
import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion

import java.nio.file.Path

boolean smoke = HarnessConfig.smokeMode()
ProfileResolver.Profile profile = new ProfileResolver().load("mixed")
ThresholdResolver thresholds = new ThresholdResolver()

// Share tolerance is a profile shape parameter, owned by TST-002, so it stays
// a literal here rather than being projected into _nfr-thresholds.yml -- see
// that file's own header comment.
final double SHARE_TOLERANCE = 0.20

long totalSamples = Long.parseLong(props.getProperty("tst034_total_samples"))
int subWindows = Integer.parseInt(props.getProperty("tst034_sub_windows"))

boolean everyJourneyWithinBudget = true
boolean everyShareWithinTolerance = true
boolean noJourneyStarved = true
boolean errorsAttributed = true
StringBuilder detail = new StringBuilder()

profile.journeys().each { name, journey ->
    long count = Long.parseLong(props.getProperty("tst034_${name}_count", "0"))
    long p95 = Long.parseLong(props.getProperty("tst034_${name}_p95", "0"))
    long errors = Long.parseLong(props.getProperty("tst034_${name}_errors", "-1"))
    long minPerWindow = Long.parseLong(props.getProperty("tst034_${name}_min_window_count", "0"))

    ThresholdResolver.Threshold tierBudget =
        thresholds.resolve("p95_latency_" + journey.tier().toLowerCase() + "_ms")

    double actualShare = totalSamples == 0 ? 0d : (double) count / totalSamples
    double declaredShare = journey.share() / 100.0d
    boolean shareOk = Math.abs(actualShare - declaredShare) <= declaredShare * SHARE_TOLERANCE
    // Smoke mode's 20s hold cannot honestly measure real p95 latency (see I1
    // below, reported not-evaluated rather than folded into this flag), so
    // this figure is left real either way and just goes unused under smoke.
    boolean budgetOk = p95 <= tierBudget.value()

    if (!budgetOk) everyJourneyWithinBudget = false
    if (!shareOk) everyShareWithinTolerance = false
    if (count == 0 || minPerWindow == 0) noJourneyStarved = false
    if (errors < 0) errorsAttributed = false

    detail.append("  ${name} (${journey.tier()}): count=${count} share=${String.format('%.3f', actualShare)} " +
                  "declared=${String.format('%.3f', declaredShare)} p95=${p95}ms " +
                  "budget=${(long) tierBudget.value()}ms errors=${errors} minWindow=${minPerWindow}\n")
}

String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

// A 20s smoke hold cannot honestly measure real p95 latency against a budget
// tuned for the declared 14,400s full hold, so I1 is reported not-evaluated
// under smoke rather than short-circuited to a hardcoded pass -- the same
// honesty the threshold rows below already apply, and the pattern
// assert-dlq.groovy/assert-readmodel.groovy use for their own run-gated
// invariants.
RunFragment.Entry i1 = smoke
    ? new RunFragment.Entry("I1", "Every journey meets its own tier budget, never a blended figure",
        RunFragment.Result.NOT_EVALUATED)
    : InvariantAssertion.check(
        "I1", "Every journey meets its own tier budget, never a blended figure",
        { everyJourneyWithinBudget } as java.util.function.BooleanSupplier)
RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "Each journey's actual share is within tolerance of its declared share",
    { everyShareWithinTolerance } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "No journey is starved in any sub-window",
    { noJourneyStarved } as java.util.function.BooleanSupplier)
RunFragment.Entry i4 = InvariantAssertion.check(
    "I4", "Errors are attributed per journey, not pooled",
    { errorsAttributed } as java.util.function.BooleanSupplier)
RunFragment.Entry i5 = InvariantAssertion.check(
    "I5", "Steady state is reached before measurement begins",
    { subWindows >= 2 } as java.util.function.BooleanSupplier)

RunFragment.Builder builder = RunFragment.builder()
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

// One threshold row per tier the blend touches. In smoke mode the hold is 20s
// against a declared 14,400s, so a latency budget cannot be honestly evaluated
// -- each row is emitted not-evaluated WITH a reason, which RunFragment
// enforces (a blank reason throws).
["t0", "t1", "t2"].each { tier ->
    ThresholdResolver.Threshold t = thresholds.resolve("p95_latency_${tier}_ms")
    if (smoke) {
        builder.threshold("p95_latency_${tier}_ms", t.thresholdRef(),
            RunFragment.Result.NOT_EVALUATED, "smoke-mode: 20s hold against a declared 14400s")
    } else {
        long worst = Long.parseLong(props.getProperty("tst034_worst_p95_${tier}", "0"))
        builder.threshold("p95_latency_${tier}_ms", t.thresholdRef(),
            worst <= t.value() ? RunFragment.Result.PASSED : RunFragment.Result.FAILED, null)
    }
}

RunFragment fragment = builder.build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "blend=${profile.blendRef()} smoke=${smoke} totalSamples=${totalSamples}\n" +
    detail.toString() +
    "I1 per-journey-tier-budget: ${i1.result().wire()}${smoke ? ' (smoke-mode: 20s hold against a declared 14400s)' : ''}\n" +
    "I2 share-within-tolerance: ${i2.result().wire()}\n" +
    "I3 no-journey-starved: ${i3.result().wire()}\n" +
    "I4 errors-attributed: ${i4.result().wire()}\n" +
    "I5 steady-state-reached: ${i5.result().wire()}\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
