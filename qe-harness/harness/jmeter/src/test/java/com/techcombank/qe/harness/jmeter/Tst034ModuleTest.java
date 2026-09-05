package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-034 blended journey module. Always driven in smoke mode from the test
 * suite -- the full profile holds for 14,400 seconds, which no test may wait on.
 */
class Tst034ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void passesAgainstTheCleanSut() throws Exception {
        ModuleResult r = runner.run("TST-034", Map.of("HARNESS_SMOKE_MODE", "true"));
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void smokeModeReportsTierThresholdsNotEvaluatedWithAReason() throws Exception {
        ModuleResult r = runner.run("TST-034", Map.of("HARNESS_SMOKE_MODE", "true"));
        assertTrue(r.fragment().thresholds().stream()
            .allMatch(t -> t.result() == RunFragment.Result.NOT_EVALUATED
                && t.reason() != null && !t.reason().isBlank()),
            "a not-evaluated threshold without a reason is rejected by RunFragment itself");
        assertTrue(r.fragment().thresholds().stream()
            .anyMatch(t -> t.thresholdRef().equals(
                "NFR-002#end-to-end-budgets-per-tier-customer-facing")));
    }

    @Test
    void reportsStarvationAgainstTheStarvedJourneyDefect() throws Exception {
        ModuleResult r = runner.run("TST-034",
            Map.of("HARNESS_SMOKE_MODE", "true", "SUT_DEFECT", "journey-starved"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I3") && i.result() == RunFragment.Result.FAILED));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I4") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: per-journey error attribution still works");
    }
}
