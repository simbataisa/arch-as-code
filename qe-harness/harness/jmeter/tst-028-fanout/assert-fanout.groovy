// TST-028 fan-out / fan-in correlation assertion (Wave 17).
//
// I4 is the invariant most easily faked: a sequential fan-out still produces a
// correct aggregate. So fan-in elapsed time is compared against the SUM of the
// branch latencies -- if fan-in took as long as the sum, the branches ran in
// series and this is a fan-out in name only.
//
// I3 reads its own dedicated "dedup" correlation's partCount (a correlation
// that only ever received two replies to the SAME branch, and never
// completes naturally), NOT "complete"'s own partCount. It used to: but
// "complete" is shared with I1's own completeness check, and under the
// aggregate-emitted-incomplete defect every correlation's own parts are
// truncated to size 1 on the first reply regardless of which correlation --
// so an I3 check tied to complete.partCount==complete.branchCount would fail
// under that defect for a reason that has nothing to do with dedup, breaking
// the specific defect proof this module exists to make (I2/I3 untouched).
// Giving I3 its own correlation, independent of completeness entirely, is
// what keeps it a genuine dedup check under every SUT state.

import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion

import groovy.json.JsonSlurper

import java.nio.file.Path

def slurper = new JsonSlurper()
def complete = slurper.parseText(vars.get("aggregate_complete"))
def timedOut = slurper.parseText(vars.get("aggregate_timedout"))

long correlationsIssued = Long.parseLong(props.getProperty("tst028_correlations_issued"))
long correlationsDistinct = Long.parseLong(props.getProperty("tst028_correlations_distinct"))
long duplicateBranchReplies = Long.parseLong(props.getProperty("tst028_duplicate_branch_replies"))
long fanInElapsedMs = Long.parseLong(props.getProperty("tst028_fanin_elapsed_ms"))
long branchSumMs = Long.parseLong(props.getProperty("tst028_branch_sum_ms"))
long branchMaxMs = Long.parseLong(props.getProperty("tst028_branch_max_ms"))
boolean claimCheckResolved = Boolean.parseBoolean(props.getProperty("tst028_claim_check_resolved"))
long dedupPartCount = Long.parseLong(props.getProperty("tst028_dedup_part_count"))

String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

// I1 has two legal shapes and one illegal one: complete-and-unmarked is fine,
// timed-out-and-marked is fine, incomplete-and-unmarked is the violation.
boolean completeIsWhole = complete.partCount == complete.branchCount && !complete.partial
boolean timedOutIsMarked = timedOut.partCount < timedOut.branchCount && timedOut.partial

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1", "An aggregate is emitted only when complete, or timed out and marked partial",
    { completeIsWhole && timedOutIsMarked } as java.util.function.BooleanSupplier)
RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "Correlation IDs are unique within the window",
    { correlationsIssued > 0L && correlationsDistinct == correlationsIssued } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "The aggregate is the union of branch responses, with no duplicates",
    { duplicateBranchReplies > 0L && dedupPartCount == 1L } as java.util.function.BooleanSupplier)
RunFragment.Entry i4 = InvariantAssertion.check(
    "I4", "Fan-in latency approximates max(branch) and is below sum(branch)",
    { branchSumMs > branchMaxMs && fanInElapsedMs < branchSumMs } as java.util.function.BooleanSupplier)
RunFragment.Entry i5 = InvariantAssertion.check(
    "I5", "A claim-check reference resolves through its retention boundary",
    { claimCheckResolved } as java.util.function.BooleanSupplier)

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
    "I1 complete-or-marked-partial: ${i1.result().wire()} (complete=${complete.partCount}/${complete.branchCount} partial=${complete.partial}; timedOut=${timedOut.partCount}/${timedOut.branchCount} partial=${timedOut.partial})\n" +
    "I2 correlation-unique: ${i2.result().wire()} (${correlationsDistinct}/${correlationsIssued})\n" +
    "I3 union-no-duplicates: ${i3.result().wire()} (duplicateReplies=${duplicateBranchReplies} dedupPartCount=${dedupPartCount})\n" +
    "I4 fanin-below-sum: ${i4.result().wire()} (elapsed=${fanInElapsedMs}ms max=${branchMaxMs}ms sum=${branchSumMs}ms)\n" +
    "I5 claim-check-resolves: ${i5.result().wire()}\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
