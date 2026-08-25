package com.techcombank.qe.sut.capability.recon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-039 reconciliation with seeded defects (Task 12, given verbatim in the
 * task brief).
 *
 * <p>{@link #reportFindsEverySeededDefect} and {@link
 * #reportFindsNothingOnCleanData} are what let Task 21's Locust module build
 * a real confusion matrix: {@link DefectSeeder#seed} returns the genuinely
 * known truth, and {@link ReconService#report} must independently rediscover
 * it by comparing {@code account_balance_report}/{@code
 * report_refresh_timestamp} against {@code ledger_entry} -- never by
 * echoing back what the seeder claims it seeded. See {@link ReconService}
 * and {@link DefectSeeder} for how that independence is maintained.
 */
class ReconServiceTest extends AbstractReconIntegrationTest {

    @Test
    void reportFindsEverySeededDefect() {
        SeededDefects seeded = defectSeeder.seed(42L);   // known ids per dimension
        ReconReport report = service.report();
        assertEquals(seeded.completeness(), report.completeness().defects());
        assertEquals(seeded.accuracy(),     report.accuracy().defects());
        assertEquals(seeded.timeliness(),   report.timeliness().defects());
    }

    @Test
    void reportFindsNothingOnCleanData() {
        ReconReport report = service.report();
        assertTrue(report.completeness().defects().isEmpty());
        assertTrue(report.accuracy().defects().isEmpty());
        assertTrue(report.timeliness().defects().isEmpty());
    }

    @Test
    void defectFlagReportsCleanDespiteSeededDefects() {
        withDefect("recon-false-clean", () -> {
            defectSeeder.seed(42L);
            assertTrue(service.report().accuracy().defects().isEmpty(),
                "false-clean defect must hide real defects");
        });
    }
}
