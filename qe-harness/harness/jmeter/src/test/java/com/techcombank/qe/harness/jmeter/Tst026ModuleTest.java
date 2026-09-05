package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-026 transformation and routing module. Requires
 * {@code make up PROFILES="core messaging"} -- the broker is not in the core
 * profile, so a core-only stack cannot serve this module.
 */
class Tst026ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void passesAgainstTheCleanSut() throws Exception {
        ModuleResult r = runner.run("TST-026", Map.of());
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void emitsTheContractSchemaOracle() throws Exception {
        ModuleResult r = runner.run("TST-026", Map.of());
        assertEquals("contract-schema", r.fragment().oracle(),
            "this is the only module using this oracle; the fragment must say so");
    }

    @Test
    void reportsQuarantineFailureAgainstTheFallthroughDefect() throws Exception {
        ModuleResult r = runner.run("TST-026", Map.of("SUT_DEFECT", "route-default-fallthrough"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I2") && i.result() == RunFragment.Result.FAILED));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I5") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: transformation fidelity is untouched");
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I6") && i.result() == RunFragment.Result.PASSED));
    }
}
