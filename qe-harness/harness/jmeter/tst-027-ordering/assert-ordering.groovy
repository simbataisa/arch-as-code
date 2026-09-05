// TST-027 ordering and resequencing assertion (Wave 17).
//
// Reads GET /messaging/sequence/state once in the TearDown Thread Group: it
// carries the emitted order, the overflow flag, silently_dropped, the
// escalation flag and the SUT's own declared scope. Asking the SUT for its
// declared scope rather than assuming one keeps this module honest about what
// it is actually checking.
//
// I5 is asserted for per_key ONLY. RabbitMQ has no partitions, so per_partition
// and global have no meaning here -- see this module's partial_reason.

import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion

import groovy.json.JsonSlurper

import java.nio.file.Path

def state = new JsonSlurper().parseText(vars.get("sequence_state"))

List<Long> emitted = state.emitted.collect { it as Long }
List<Long> sorted = new ArrayList<>(emitted).sort()
boolean overflowSignalled = state.overflowSignalled
long silentlyDropped = state.silentlyDropped as Long
boolean escalated = state.escalated
String declaredScope = state.declaredScope

long duplicatesSent = Long.parseLong(props.getProperty("tst027_duplicates_sent"))
long distinctSent = Long.parseLong(props.getProperty("tst027_distinct_sent"))
boolean gapOutcomeObserved = Boolean.parseBoolean(props.getProperty("tst027_gap_outcome"))

String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1", "Emitted order equals sorted order against a shuffled publish order",
    { emitted == sorted && !emitted.isEmpty() } as java.util.function.BooleanSupplier)
RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "A gap resolves inside the declared timeout, or an escalation is emitted",
    { gapOutcomeObserved || escalated } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "Each sequence is emitted exactly once",
    { duplicatesSent > 0L && emitted.size() == emitted.unique().size() &&
      emitted.size() == distinctSent } as java.util.function.BooleanSupplier)
RunFragment.Entry i4 = InvariantAssertion.check(
    "I4", "An overflow event fires at the bound and silently_dropped is zero",
    { overflowSignalled && silentlyDropped == 0L } as java.util.function.BooleanSupplier)
RunFragment.Entry i5 = InvariantAssertion.check(
    "I5", "Declared scope is per_key with zero violations in that scope",
    { declaredScope == "per_key" && emitted == sorted } as java.util.function.BooleanSupplier)

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
    "I1 emitted-equals-sorted: ${i1.result().wire()} (emitted=${emitted})\n" +
    "I2 gap-resolves-or-escalates: ${i2.result().wire()} (escalated=${escalated})\n" +
    "I3 exactly-once: ${i3.result().wire()} (emitted=${emitted.size()}, distinctSent=${distinctSent}, duplicatesSent=${duplicatesSent})\n" +
    "I4 overflow-signalled: ${i4.result().wire()} (overflow=${overflowSignalled}, dropped=${silentlyDropped})\n" +
    "I5 per-key-scope: ${i5.result().wire()} (scope=${declaredScope})\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
