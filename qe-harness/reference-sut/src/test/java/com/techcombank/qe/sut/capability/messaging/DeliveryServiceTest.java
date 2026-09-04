package com.techcombank.qe.sut.capability.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TST-029 delivery guarantee, retry and DLQ. */
class DeliveryServiceTest extends AbstractMessagingIntegrationTest {

    @Test
    void everyMessageEitherChangesStateOrLandsInTheDlq() {
        delivery.reset();
        for (int i = 0; i < 4; i++) {
            delivery.submit("job-000" + i, false);
        }
        delivery.submit("poison-0001", true);
        assertTrue(delivery.awaitSettled(5, 15_000L));
        assertEquals(delivery.submitted(), delivery.stateChanges() + delivery.dlqCount(),
            "I1: nothing may be neither processed nor dead-lettered");
    }

    @Test
    void aPoisonMessageReachesTheDlqInsideTheDeclaredAttemptCeiling() {
        delivery.reset();
        delivery.submit("poison-0002", true);
        assertTrue(delivery.awaitDlq(1, 15_000L));
        assertTrue(delivery.attemptsFor("poison-0002") <= maxDeliveryAttempts(),
            "I3/I6: retries must stop at the declared ceiling, read from configuration");
    }

    @Test
    void aPoisonMessageDoesNotBlockItsNeighbours() {
        delivery.reset();
        delivery.submit("poison-0003", true);
        delivery.submit("job-9001", false);
        assertTrue(delivery.awaitStateChanges(1, 15_000L),
            "I3: a good message behind a poison one must still be processed");
    }

    @Test
    void theRetryLadderHasMoreThanOneDistinctInterval() {
        // I4 asserts distinct_intervals > 1 against the SUT's own declared
        // backoff, so the declared value is checked at the source rather than
        // inferred from observed timings, which would be flaky.
        assertTrue(retryIntervalsMs().stream().distinct().count() > 1);
    }

    @Test
    void dlqDepthIsExportedAndAlertsPastTheDeclaredDepth() {
        delivery.reset();
        for (int i = 0; i < dlqAlertDepth() + 1; i++) {
            delivery.submit("poison-90" + i, true);
        }
        assertTrue(delivery.awaitDlq(dlqAlertDepth() + 1, 30_000L));
        assertTrue(observability.dlqAlertFiring(delivery.dlqCount()),
            "I5: the alert must fire once depth passes the declared threshold");
    }

    @Test
    void dlqBypassDefectBreaksOnlyTheDeliveryGuarantee() {
        delivery.reset();
        withDefect("dlq-bypass-drop", () -> delivery.submit("poison-0004", true));
        assertTrue(delivery.awaitSettled(1, 15_000L));
        assertTrue(delivery.submitted() > delivery.stateChanges() + delivery.dlqCount(),
            "the defect must drop a message with neither a state change nor a DLQ entry");
    }
}
