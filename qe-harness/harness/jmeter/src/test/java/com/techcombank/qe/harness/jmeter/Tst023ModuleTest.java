package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-023 concurrent limit module. Drives real HTTP traffic against the
 * reference SUT via {@code run-module.sh TST-023} -- requires
 * {@code make up PROFILES=core} to already be running (see qe-harness/README.md).
 */
class Tst023ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void passesAgainstTheCleanSut() throws Exception {
        ModuleResult r = runner.run("TST-023", Map.of());
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void reportsCapacityFailureAgainstTheOvercommitDefect() throws Exception {
        ModuleResult r = runner.run("TST-023", Map.of("SUT_DEFECT", "reservation-overcommit"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I1") && i.result() == RunFragment.Result.FAILED));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I2") && i.result() == RunFragment.Result.FAILED));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I3") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: I3 is untouched by the skipped capacity check");
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I4") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: I4 is untouched by the skipped capacity check");
    }
}
