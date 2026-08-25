package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-021 ledger invariant module (Task 16). Drives real HTTP traffic
 * against the reference SUT via {@code run-module.sh TST-021} -- requires
 * {@code make up PROFILES=core} to already be running (see qe-harness/README.md).
 */
class Tst021ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void passesAgainstTheCleanSut() throws Exception {
        ModuleResult r = runner.run("TST-021", Map.of());
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void reportsInvariantFailureAgainstTheUnbalancedDefect() throws Exception {
        ModuleResult r = runner.run("TST-021", Map.of("SUT_DEFECT", "ledger-unbalanced"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I1") && i.result() == RunFragment.Result.FAILED));
    }
}
