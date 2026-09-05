// TST-020 idempotency and replay assertion (Wave 17).
//
// Sequenced last in the wave deliberately: I7 (dedup survives broker
// redelivery) needs the broker Phase 2 introduced, so running this module
// earlier would have meant shipping coverage: partial with I7 unreached.
//
// I2 compares the replay BYTE FOR BYTE. The SUT stores its response verbatim
// rather than re-serialising, so a body that differs by key order or whitespace
// is a real violation, not a formatting artefact.
//
// I7's own redelivery is a repeated identical POST /messaging/work call (this
// repo's established convention for replay-style invariants, per I1/I2/I5's
// own same-key burst above), not a real AMQP-level nack/requeue. See
// README.md's "Why I7 is NOT a POST /messaging/work redelivery through
// ledger_entry" for why the task brief's original ledger_entry-count mechanic
// was unimplementable, and plan.jmx's "Verify Idempotency" comments for what
// this module drives instead. redelivery_deduped is written by plan.jmx as
// (stateChanges == 1) read from a single GET /messaging/delivery/state call
// after both /messaging/work submissions have settled -- this assertion
// script does not need to change to consume that corrected mechanic.

import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion

import groovy.json.JsonSlurper

import java.nio.file.Path

long entriesAfterBurst = Long.parseLong(vars.get("entries_after_burst"))
long distinctBodies = Long.parseLong(props.getProperty("tst020_distinct_bodies"))
long createdResponses = Long.parseLong(props.getProperty("tst020_created_responses"))
long replayResponses = Long.parseLong(props.getProperty("tst020_replay_responses"))
long burstSize = Long.parseLong(props.getProperty("tst020_burst_size"))

long entriesAfterDistinctKeys = Long.parseLong(vars.get("entries_after_distinct_keys"))
long distinctKeysSent = Long.parseLong(vars.get("distinct_keys_sent"))
boolean conflictObserved = Boolean.parseBoolean(vars.get("conflict_observed"))
boolean redeliveryDeduped = Boolean.parseBoolean(vars.get("redelivery_deduped"))

def ttlInfo = new JsonSlurper().parseText(vars.get("ttl_info"))
long keyTtlSeconds = ttlInfo.keyTtlSeconds as Long
long clientRetryWindowSeconds = ttlInfo.clientMaxRetryWindowSeconds as Long

String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1", "N same-key requests produce exactly one state change",
    { burstSize > 1L && entriesAfterBurst == 2L } as java.util.function.BooleanSupplier)
RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "A replay returns a byte-identical status and body",
    { distinctBodies == 1L && replayResponses > 0L } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "N distinct keys produce N state changes",
    { entriesAfterDistinctKeys == distinctKeysSent * 2L } as java.util.function.BooleanSupplier)
RunFragment.Entry i4 = InvariantAssertion.check(
    "I4", "The same key with a different payload is a conflict",
    { conflictObserved } as java.util.function.BooleanSupplier)
RunFragment.Entry i5 = InvariantAssertion.check(
    "I5", "Under true concurrency one wins and the rest are served the stored response",
    { createdResponses == 1L && replayResponses == burstSize - 1L } as java.util.function.BooleanSupplier)
RunFragment.Entry i6 = InvariantAssertion.check(
    "I6", "Key TTL is at least the declared client max retry window",
    { keyTtlSeconds >= clientRetryWindowSeconds } as java.util.function.BooleanSupplier)
RunFragment.Entry i7 = InvariantAssertion.check(
    "I7", "Deduplication survives broker redelivery",
    { redeliveryDeduped } as java.util.function.BooleanSupplier)

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
    .invariant(i6.id(), i6.description(), i6.result())
    .invariant(i7.id(), i7.description(), i7.result())
    .build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "I1 one-state-change: ${i1.result().wire()} (entries=${entriesAfterBurst}, burst=${burstSize})\n" +
    "I2 byte-identical-replay: ${i2.result().wire()} (distinctBodies=${distinctBodies}, replays=${replayResponses})\n" +
    "I3 distinct-keys-distinct-changes: ${i3.result().wire()} (entries=${entriesAfterDistinctKeys}, keys=${distinctKeysSent})\n" +
    "I4 payload-conflict: ${i4.result().wire()}\n" +
    "I5 one-winner: ${i5.result().wire()} (created=${createdResponses}, replayed=${replayResponses})\n" +
    "I6 ttl-covers-retry-window: ${i6.result().wire()} (ttl=${keyTtlSeconds}s, window=${clientRetryWindowSeconds}s)\n" +
    "I7 dedup-survives-redelivery: ${i7.result().wire()}\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
