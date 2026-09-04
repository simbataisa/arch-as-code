package com.techcombank.qe.sut.capability.messaging;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TST-028 fan-out / fan-in correlation. */
class AggregatorServiceTest extends AbstractMessagingIntegrationTest {

    @Test
    void emitsAnAggregateOnlyWhenEveryBranchHasReplied() {
        aggregator.reset();
        String corr = aggregator.fanOut("corr-0001");
        aggregator.branchReply(corr, "a", "one");
        aggregator.branchReply(corr, "b", "two");
        assertFalse(aggregator.aggregateFor(corr).isPresent(),
            "I1: an incomplete set must not emit an aggregate");
        aggregator.branchReply(corr, "c", "three");
        assertTrue(aggregator.aggregateFor(corr).isPresent());
        assertFalse(aggregator.aggregateFor(corr).orElseThrow().partial(),
            "a complete aggregate must not carry the partial marker");
    }

    @Test
    void aTimedOutSetEmitsAPartialMarkerRatherThanSilence() {
        aggregator.reset();
        String corr = aggregator.fanOut("corr-0002");
        aggregator.branchReply(corr, "a", "one");
        assertTrue(aggregator.awaitAggregate(corr, aggregateTimeoutMs() * 2),
            "I1: past the timeout an aggregate must appear, marked partial");
        assertTrue(aggregator.aggregateFor(corr).orElseThrow().partial());
    }

    @Test
    void correlationIdsAreUniqueWithinTheWindow() {
        aggregator.reset();
        Set<String> ids = Set.of(
            aggregator.fanOut(null), aggregator.fanOut(null), aggregator.fanOut(null));
        assertEquals(3, ids.size(), "I2: correlation ids must be unique in the window");
    }

    @Test
    void theAggregateIsTheUnionOfBranchRepliesWithNoDuplicates() {
        aggregator.reset();
        String corr = aggregator.fanOut("corr-0003");
        aggregator.branchReply(corr, "a", "one");
        aggregator.branchReply(corr, "a", "one-again");
        aggregator.branchReply(corr, "b", "two");
        aggregator.branchReply(corr, "c", "three");
        assertEquals(3, aggregator.aggregateFor(corr).orElseThrow().parts().size(),
            "I3: a repeated branch must not add a duplicate part");
    }

    @Test
    void incompleteEmitDefectBreaksOnlyTheCompletenessInvariant() {
        aggregator.reset();
        String corr = aggregator.fanOut("corr-0004");
        withDefect("aggregate-emitted-incomplete", () -> aggregator.branchReply(corr, "a", "one"));
        assertTrue(aggregator.aggregateFor(corr).isPresent(),
            "the defect must emit on the first reply");
        assertFalse(aggregator.aggregateFor(corr).orElseThrow().partial(),
            "and must do so without the partial marker -- that is the violation");
    }

    @Test
    void aLateBranchReplyAfterTimeoutDoesNotOverwriteThePartialAggregate() {
        aggregator.reset();
        String corr = aggregator.fanOut("corr-0005");
        aggregator.branchReply(corr, "a", "one");
        assertTrue(aggregator.awaitAggregate(corr, aggregateTimeoutMs() * 2),
            "the timeout arm must emit a partial aggregate before the slow branches arrive");
        assertTrue(aggregator.aggregateFor(corr).orElseThrow().partial(),
            "sanity check: the aggregate emitted so far must be the timed-out, partial one");

        // Branches b and c are slow consumers that finally reply AFTER the
        // timeout-triggered emit already happened. This must not re-emit and
        // flip the recorded outcome from "timed out, partial" to "complete,
        // non-partial" -- that would be a silent double-emit.
        aggregator.branchReply(corr, "b", "two");
        aggregator.branchReply(corr, "c", "three");

        assertTrue(aggregator.aggregateFor(corr).orElseThrow().partial(),
            "a late reply arriving after a timeout emission must not overwrite the partial marker");
    }
}
