// TST-031 rate-limit breakpoint assertion (Task 17).
//
// Runs once, as the sole sampler-producing element in the TearDown Thread
// Group, once every Rate Limit Load thread has finished -- see plan.jmx and
// its README.md for why the check has to happen there, not mid-run: a step
// still in progress would report an incomplete (understated) admitted rate.
//
// Bound variables this JSR223 Sampler receives from the JMeter engine:
//   props       - JMeter's single cross-thread shared store; holds the
//                 per-step tallies "tally-accept-reject" (plan.jmx) wrote
//                 during the load, and the ramp shape "Record Run Start and
//                 Ramp Shape" (plan.jmx's setUp Thread Group) wrote before it.
//   SampleResult - this sampler's own result; its success/failure and
//                 response data become what JMeter's own .jtl reports for
//                 this element, independent of the EvidenceEmitter fragment
//                 below (which is the harness's real source of truth).
//
// Smoke mode never skips correctness: I1-I3 are evaluated identically
// whether HarnessConfig.smokeMode() is true or false (see the InvariantAssertion
// calls below -- none of them read smoke at all). Only the *threshold* section
// at the bottom branches on it, per profiles/stress.yml's smoke_mode_overrides
// ("thresholds: not-evaluated" -- correctness invariants still assert; perf
// does not). This is this module's entire reason to exist: prove that
// degrading what is measured never degrades whether correctness is checked.

import com.techcombank.qe.harness.config.HarnessConfig
import com.techcombank.qe.harness.config.ThresholdResolver
import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion

import java.nio.file.Path

// Must match plan.jmx's load_profile row count (below-limit/at-limit/above-limit).
int numSteps = 3
long stepHoldSeconds = Long.parseLong(props.getProperty("tst031_step_hold_seconds"))
long warmupSeconds = Long.parseLong(props.getProperty("tst031_warmup_seconds"))
// The window "tally-accept-reject" (plan.jmx) actually tallied "accepted"
// over -- see that script's own comment for why the warm-up window (where a
// full bucket's carried-over burst is spent, near-instantly, the moment a
// step's offered rate exceeds its refill rate) is excluded from I1's rate
// measurement rather than papered over with a wider tolerance.
double measuredWindowSeconds = (double) (stepHoldSeconds - warmupSeconds)

// The reference SUT's configured TokenBucket rate (app.ratelimit.permits-per-second,
// reference-sut/src/main/resources/application.properties). A fixed capability
// constant of THIS synthetic SUT, not an NFR spine value -- same rationale as
// TST-021's hardcoded ACC-000001/ACC-000002 seed accounts: no HTTP surface
// exposes this back to a caller, so it is documented here and in README.md
// and must be kept in sync with that property by hand if it ever changes.
double configuredLimitRps = 10.0d

// Measurement-noise tolerance only (HTTP round-trip jitter, thread-scheduling
// imprecision in the Throughput Shaping Timer's own pacing) -- the burst
// itself is already excluded via the warm-up window above, so this does not
// need to (and must not) be wide enough to also absorb a real leak. The
// ratelimit-leaky defect's violation is a sustained ~2x the configured rate,
// far outside this margin either way.
double measurementToleranceRps = 0.10d * configuredLimitRps

long total5xx = Long.parseLong(props.getProperty("tst031_total_5xx", "0"))
long totalRejectedMissingRetryAfter = 0L
double maxAdmittedRate = 0.0d

for (int i = 0; i < numSteps; i++) {
    long accepted = Long.parseLong(props.getProperty("tst031_step_" + i + "_accepted", "0"))
    long rejectedMissing = Long.parseLong(props.getProperty("tst031_step_" + i + "_rejected_missing_retry_after", "0"))
    totalRejectedMissingRetryAfter += rejectedMissing
    double admittedRate = accepted / measuredWindowSeconds
    maxAdmittedRate = Math.max(maxAdmittedRate, admittedRate)
}

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1", "Admitted rate never exceeds the configured rate limit",
    { maxAdmittedRate <= configuredLimitRps + measurementToleranceRps } as java.util.function.BooleanSupplier)
RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "Every rejection carries a Retry-After header",
    { totalRejectedMissingRetryAfter == 0L } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "No 5xx response at any load stage",
    { total5xx == 0L } as java.util.function.BooleanSupplier)

boolean smoke = HarnessConfig.smokeMode()

RunFragment.Builder builder = RunFragment.builder()
    .archetype(System.getenv("QE_ARCHETYPE"))
    .module("jmeter")
    .serviceName("reference-sut")
    .tier("T0")
    .oracle("invariant-assertion")
    .environment(System.getenv().getOrDefault("QE_ENVIRONMENT", "local-compose"))
    .invariant(i1.id(), i1.description(), i1.result())
    .invariant(i2.id(), i2.description(), i2.result())
    .invariant(i3.id(), i3.description(), i3.result())

// peak_rps / NFR-004: the same threshold name profiles/stress.yml declares
// for this profile. Resolving it here (rather than skipping the citation
// entirely) keeps the traceability gate's citation check meaningful even in
// smoke mode -- see qe-harness/README.md's "What the Threshold Gate Does Not
// Prove": the gate proves the citation resolves, not that 1500 tps is the
// right number for this synthetic SUT's 10 rps bucket (it plainly is not;
// a human owns that accuracy judgement, this module owns citing it honestly).
ThresholdResolver.Threshold peakRps = new ThresholdResolver().resolve("peak_rps")
if (smoke) {
    builder.threshold("peak_rps", peakRps.thresholdRef(), RunFragment.Result.NOT_EVALUATED, "smoke-mode")
} else {
    boolean withinNfrCeiling = maxAdmittedRate <= peakRps.value()
    builder.threshold("peak_rps", peakRps.thresholdRef(),
        withinNfrCeiling ? RunFragment.Result.PASSED : RunFragment.Result.FAILED, null)
}

RunFragment fragment = builder.build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "I1 admitted-rate-within-limit: ${i1.result().wire()} (maxAdmittedRate=${maxAdmittedRate}, " +
        "configuredLimitRps=${configuredLimitRps}, toleranceRps=${measurementToleranceRps})\n" +
    "I2 rejections-carry-retry-after: ${i2.result().wire()} (missing=${totalRejectedMissingRetryAfter})\n" +
    "I3 no-5xx: ${i3.result().wire()} (total5xx=${total5xx})\n" +
    "peak_rps threshold: ${smoke ? "not-evaluated (smoke-mode)" : "evaluated"}\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
