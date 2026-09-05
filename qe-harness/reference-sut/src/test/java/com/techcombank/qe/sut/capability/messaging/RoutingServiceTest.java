package com.techcombank.qe.sut.capability.messaging;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TST-026 message transformation and routing. */
class RoutingServiceTest extends AbstractMessagingIntegrationTest {

    @Test
    void routesOnlyOnDeclaredConditionsAndQuarantinesTheRest() {
        routing.publish("pay.domestic.credit", samplePayload("VND", "1500.00"));
        routing.publish("pay.unknown.kind", samplePayload("VND", "1500.00"));
        assertTrue(awaitQueueDepth(MessagingTopology.Q_DOMESTIC, 1));
        assertTrue(awaitQueueDepth(MessagingTopology.Q_UNROUTABLE, 1),
            "I2: an unmatched key must reach quarantine, never a default route");
    }

    @Test
    void anUnmappedEnumIsRejectedNeverDefaulted() {
        assertThrows(RoutingService.UnmappedEnum.class,
            () -> routing.publish("pay.domestic.credit", samplePayload("VND", "1500.00")
                .replace("\"CREDIT\"", "\"TELEPORT\"")),
            "I3: an unknown enum member must be rejected, not silently defaulted");
    }

    @Test
    void amountScaleAndCurrencySurviveRoundTrip() {
        String routed = routing.transform(samplePayload("VND", "1500.00"));
        assertEquals(0, new BigDecimal("1500.00").compareTo(routing.amountOf(routed)),
            "I5: compareTo, not equals -- scale must survive but need not be identical");
        assertEquals("VND", routing.currencyOf(routed));
    }

    @Test
    void vietnameseDiacriticsSurviveByteIdentically() {
        String name = "Nguyễn Thị Hoà";
        String routed = routing.transform(samplePayload("VND", "1500.00").replace("PARTY", name));
        assertTrue(routed.contains(name), "I6: diacritics must survive byte-identically");
    }

    @Test
    void defaultFallthroughDefectBreaksOnlyTheQuarantineInvariant() {
        withDefect("route-default-fallthrough", () ->
            routing.publish("pay.unknown.kind", samplePayload("VND", "1500.00")));
        assertTrue(awaitQueueDepth(MessagingTopology.Q_DOMESTIC, 1),
            "the defect must route an unmatched key to a real queue");
        assertEquals(0L, queueDepth(MessagingTopology.Q_UNROUTABLE));
    }

    private String samplePayload(String currency, String amount) {
        return """
            {"messageId":"msg-0001","kind":"CREDIT","currency":"%s","amount":"%s","party":"PARTY"}
            """.formatted(currency, amount).strip();
    }
}
