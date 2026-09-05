package com.techcombank.qe.sut.capability.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Harness-side ground truth for the messaging invariants. */
class MessageLogTest extends AbstractMessagingIntegrationTest {

    @Test
    void recordsEveryPublicationWithAShortCorrelationId() {
        log.clear();
        String id = log.recordPublished("pay.domestic.credit", "corr-a1b2-c3d4");
        assertEquals(1, log.published().size());
        // Gate check 5 rejects any run of 13-19 digits anywhere under
        // qe-harness/, and epoch-millis is exactly 13. Correlation ids are
        // therefore hyphenated short forms, and this test pins that shape.
        assertFalse(id.matches(".*(?<!\\d)\\d{13,19}(?!\\d).*"),
            "a correlation id must not contain a 13-19 digit run: " + id);
    }

    @Test
    void emissionOrderIsRecordedSeparatelyFromPublishOrder() {
        log.clear();
        log.recordPublished("sequence", "corr-0003");
        log.recordPublished("sequence", "corr-0001");
        log.recordEmitted("corr-0001", 1L);
        log.recordEmitted("corr-0003", 3L);
        assertEquals(2, log.emissions().size());
        assertTrue(log.emissions().get(0).sequence() < log.emissions().get(1).sequence(),
            "TST-027 I1 compares emission order against sorted order, so both must be kept");
    }

    @Test
    void dlqAlertFiresOnlyPastTheDeclaredDepth() {
        assertFalse(observability.dlqAlertFiring(dlqAlertDepth() - 1));
        assertTrue(observability.dlqAlertFiring(dlqAlertDepth() + 1));
    }
}
