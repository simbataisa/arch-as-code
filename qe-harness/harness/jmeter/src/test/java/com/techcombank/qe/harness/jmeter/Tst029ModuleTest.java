package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-029 delivery guarantee module. Driven in smoke mode from the test suite:
 * a full run restarts the broker container, which a unit test must not do to a
 * developer's running stack without being asked.
 */
class Tst029ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void passesAgainstTheCleanSut() throws Exception {
        ModuleResult r = runner.run("TST-029", Map.of("HARNESS_SMOKE_MODE", "true"));
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void smokeModeReportsTheRestartInvariantNotEvaluated() throws Exception {
        ModuleResult r = runner.run("TST-029", Map.of("HARNESS_SMOKE_MODE", "true"));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I2")
                && i.result() == RunFragment.Result.NOT_EVALUATED),
            "the restart path must never report passed without actually restarting");
    }

    @Test
    void reportsDeliveryGuaranteeFailureAgainstTheBypassDefect() throws Exception {
        ModuleResult r = runner.run("TST-029",
            Map.of("HARNESS_SMOKE_MODE", "true", "SUT_DEFECT", "dlq-bypass-drop"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I1") && i.result() == RunFragment.Result.FAILED));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I4") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: the retry ladder is untouched");
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I5") && i.result() == RunFragment.Result.PASSED));
    }
}
