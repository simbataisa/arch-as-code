package com.techcombank.qe.sut.capability.reporting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TST-037 read-model convergence and CDC lag. */
class ReportingServiceTest extends AbstractReportingIntegrationTest {

    @Test
    void refreshDrivesLagToNearZero() {
        service.refresh();
        ReportingService.Lag lag = service.lag();
        assertTrue(lag.p95Ms() < convergenceBoundMs(),
            "I1: the read model must converge inside the declared bound");
        assertTrue(lag.p99Ms() < convergenceBoundMs(), "I2: p99 is asserted, not just p95");
    }

    @Test
    void lagExposesBothTailPercentilesNeverTheMean() {
        service.refresh();
        ReportingService.Lag lag = service.lag();
        assertTrue(lag.p95Ms() >= 0 && lag.p99Ms() >= 0);
        assertTrue(lag.p99Ms() >= lag.p95Ms(), "p99 can never be below p95");
    }

    @Test
    void everyPublishedOutboxRowIsCountedExactlyOnce() {
        service.enqueue("acct-balance-changed", "ACC-000001");
        service.publishPending();
        assertEquals(0L, service.outboxMiscountedRows(),
            "I4: every published row must have published_count = 1");
    }

    @Test
    void staleCountDefectBreaksOnlyTheOutboxInvariant() {
        service.enqueue("acct-balance-changed", "ACC-000001");
        withDefect("outbox-published-count-stale", service::publishPending);
        assertTrue(service.outboxMiscountedRows() > 0,
            "the defect must leave a published row with published_count = 0");
        service.refresh();
        assertTrue(service.lag().p95Ms() < convergenceBoundMs(),
            "the defect must be specific: convergence is untouched");
    }
}
