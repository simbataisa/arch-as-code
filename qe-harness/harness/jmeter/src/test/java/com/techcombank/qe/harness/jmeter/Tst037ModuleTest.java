package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TST-037 read-model convergence module. Requires make up PROFILES=core. */
class Tst037ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void passesAgainstTheCleanSut() throws Exception {
        ModuleResult r = runner.run("TST-037", Map.of());
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void reportsTheUnimplementedInvariantAsNotEvaluated() throws Exception {
        ModuleResult r = runner.run("TST-037", Map.of());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I5")
                && i.result() == RunFragment.Result.NOT_EVALUATED),
            "I5 needs a CDC connector this repo lacks and must never report passed");
    }

    @Test
    void reportsOutboxFailureAgainstTheStaleCountDefect() throws Exception {
        ModuleResult r = runner.run("TST-037", Map.of("SUT_DEFECT", "outbox-published-count-stale"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I4") && i.result() == RunFragment.Result.FAILED));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I1") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: convergence is untouched");
    }
}
