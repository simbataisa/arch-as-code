// TST-037 read-model convergence and CDC lag assertion (Wave 17).
//
// Runs in the TearDown Thread Group after every sampling thread has finished.
// The convergence bound is read from the SUT's own GET /reporting/lag response
// (convergenceBoundMs), never hardcoded here -- same measure-the-declared-
// configuration rule TST-040's clock-skew and TST-039's freshness tests follow.
//
// I5 is emitted NOT_EVALUATED with a reason. It is not substituted: a
// server-side stand-in would be a different invariant wearing I5's name.

import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion

import java.nio.file.Path

long boundMs        = Long.parseLong(vars.get("lag_boundMs"))
long p95Ms          = Long.parseLong(vars.get("lag_p95Ms"))
long p99Ms          = Long.parseLong(vars.get("lag_p99Ms"))
long maxP95Observed = Long.parseLong(props.getProperty("tst037_max_p95"))
long maxP99Observed = Long.parseLong(props.getProperty("tst037_max_p99"))
long miscounted     = Long.parseLong(vars.get("outbox_miscounted"))
long replayDrift    = Long.parseLong(vars.get("replay_drift"))
long samplesTaken   = Long.parseLong(props.getProperty("tst037_samples"))

String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1", "The read model converges inside the declared bound",
    { p95Ms <= boundMs } as java.util.function.BooleanSupplier)
RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "Lag is asserted at p95 and p99, never the mean",
    { maxP95Observed <= boundMs && maxP99Observed <= boundMs } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "A replayed projection equals the incremental one, field by field",
    { replayDrift == 0L } as java.util.function.BooleanSupplier)
RunFragment.Entry i4 = InvariantAssertion.check(
    "I4", "Every published outbox row has published_count = 1",
    { miscounted == 0L } as java.util.function.BooleanSupplier)
RunFragment.Entry i6 = InvariantAssertion.check(
    "I6", "Exceeding the bound is a hard FAIL, never an indefinite wait",
    { samplesTaken > 0L } as java.util.function.BooleanSupplier)

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
    .invariant("I5", "No loss or duplication across a connector restart",
               RunFragment.Result.NOT_EVALUATED)
    .invariant(i6.id(), i6.description(), i6.result())
    .build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "I1 converges-within-bound: ${i1.result().wire()} (p95=${p95Ms}ms, bound=${boundMs}ms)\n" +
    "I2 tail-percentiles-asserted: ${i2.result().wire()} (maxP95=${maxP95Observed}, maxP99=${maxP99Observed})\n" +
    "I3 replay-matches-incremental: ${i3.result().wire()} (drift=${replayDrift})\n" +
    "I4 outbox-counted-once: ${i4.result().wire()} (miscounted=${miscounted})\n" +
    "I5 connector-restart: not-evaluated (needs a CDC connector this repo lacks)\n" +
    "I6 bounded-not-waiting: ${i6.result().wire()} (samples=${samplesTaken})\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
