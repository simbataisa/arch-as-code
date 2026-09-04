package com.techcombank.qe.sut.capability.messaging;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-027 ordering and resequencing.
 *
 * <p>Scope is declared per_key. RabbitMQ has no partitions, so the archetype's
 * per_partition and global scopes are out of scope here -- which is why the
 * module ships coverage: partial rather than claiming I5 outright.
 */
class ResequencerServiceTest extends AbstractMessagingIntegrationTest {

    @Test
    void emitsInSequenceOrderRegardlessOfArrivalOrder() {
        resequencer.reset();
        // Deliberately shuffled: emission order must follow the sequence
        // numbers, not the order the messages showed up in.
        for (long seq : List.of(3L, 1L, 4L, 2L)) {
            resequencer.accept("key-a", seq, "payload-" + seq);
        }
        assertTrue(awaitEmissionCount("key-a", 4));
        assertEquals(List.of(1L, 2L, 3L, 4L), resequencer.emittedSequences("key-a"),
            "I1: emitted order must equal sorted order");
    }

    @Test
    void eachSequenceIsEmittedExactlyOnce() {
        resequencer.reset();
        resequencer.accept("key-a", 1L, "payload-1");
        resequencer.accept("key-a", 1L, "payload-1-again");
        assertTrue(awaitEmissionCount("key-a", 1));
        assertEquals(1, resequencer.emittedSequences("key-a").size(),
            "I3: a duplicate sequence must not be emitted twice");
    }

    @Test
    void aGapEitherResolvesOrEscalates() {
        resequencer.reset();
        resequencer.accept("key-a", 2L, "payload-2");
        // 1 is missing. Within the declared gap timeout nothing may be emitted;
        // past it, an escalation must appear. Bounded wait, never indefinite.
        assertTrue(resequencer.awaitGapOutcome("key-a", gapTimeoutMs() * 2),
            "I2: a gap must resolve or escalate inside the declared timeout");
    }

    @Test
    void bufferOverflowIsSignalledAndNothingIsDroppedSilently() {
        resequencer.reset();
        long bound = resequencer.bufferBound();
        for (long seq = 2; seq <= bound + 2; seq++) {
            resequencer.accept("key-b", seq, "payload-" + seq);
        }
        assertTrue(resequencer.overflowSignalled("key-b"),
            "I4: an overflow event must be emitted at the bound");
        assertEquals(0L, resequencer.silentlyDropped("key-b"),
            "I4: silently_dropped must stay zero");
    }

    @Test
    void emitOnArrivalDefectBreaksOnlyTheOrderingInvariant() {
        resequencer.reset();
        withDefect("resequencer-emits-on-arrival", () -> {
            for (long seq : List.of(3L, 1L, 2L)) {
                resequencer.accept("key-a", seq, "payload-" + seq);
            }
        });
        assertEquals(List.of(3L, 1L, 2L), resequencer.emittedSequences("key-a"),
            "the defect must emit in arrival order");
        assertEquals(3, resequencer.emittedSequences("key-a").size(),
            "the defect must be specific: exactly-once (I3) still holds");
    }
}
