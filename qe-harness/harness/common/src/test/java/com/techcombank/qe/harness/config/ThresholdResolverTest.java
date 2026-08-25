package com.techcombank.qe.harness.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the real {@code qe-harness/profiles/_nfr-thresholds.yml} on disk (Task 4) --
 * not a copy or fixture -- so a change to that file is reflected here without any
 * test-side duplication of its contents.
 */
class ThresholdResolverTest {

    private ThresholdResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ThresholdResolver();
    }

    @Test
    void resolvesByNameAndCarriesItsCitation() {
        var t = resolver.resolve("p99_latency_ms");
        assertTrue(t.thresholdRef().matches("^NFR-\\d{3}#[a-z0-9-]+$"));
    }

    @Test
    void unknownNameThrowsRatherThanDefaulting() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("made_up_metric"));
    }

    // --- Extra end-to-end wiring checks beyond the two given cases ---
    // These confirm the resolver is genuinely parsing the real file on disk (real
    // values, real units, real citations) rather than a hardcoded stand-in.

    @Test
    void resolvesP99LatencyWithItsRealValueAndUnit() {
        var t = resolver.resolve("p99_latency_ms");
        assertEquals(500.0, t.value());
        assertEquals("ms", t.unit());
        assertEquals("NFR-002#end-to-end-budgets-per-tier-customer-facing", t.thresholdRef());
    }

    @Test
    void resolvesErrorRatePctWithItsRealValueAndCitation() {
        var t = resolver.resolve("error_rate_pct");
        assertEquals(0.01, t.value());
        assertEquals("pct", t.unit());
        assertTrue(t.thresholdRef().matches("^NFR-\\d{3}#[a-z0-9-]+$"));
        assertEquals("NFR-004#napas-payment-processing--throughput-targets", t.thresholdRef());
    }
}
