package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-031 rate-limit breakpoint module (Task 17). Drives real HTTP traffic
 * against the reference SUT's token-bucket rate limiter via
 * {@code run-module.sh TST-031} -- requires {@code make up PROFILES=core} to
 * already be running (see qe-harness/README.md).
 *
 * <p>Every test here runs in smoke mode: the second test,
 * {@link #smokeModeStillAssertsCorrectnessInvariants()}, is the one this
 * module exists to prove -- smoke mode must degrade *what* is measured
 * (every performance threshold), never *whether* correctness is checked
 * (I1-I3, evaluated identically either way).
 *
 * <p>{@link #smokeModeStillAssertsCorrectnessInvariants()} asserts
 * {@code allMatch}, not the brief's originally-given {@code anyMatch}: with
 * three invariants (I1-I3), {@code anyMatch(PASSED)} is satisfied by I2/I3
 * alone even if I1 genuinely failed, so a real I1 regression could pass this
 * exact test while still corrupting the emitted fragment -- silently, since
 * nothing else in this class re-checks I1 on a clean run. {@code allMatch}
 * closes that gap without weakening the test's own stated purpose (proving
 * smoke mode does not skip correctness); it strengthens it.
 */
class Tst031ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void smokeModeRecordsThresholdsNotEvaluatedWithAReason() throws Exception {
        ModuleResult r = runner.run("TST-031", Map.of("HARNESS_SMOKE_MODE", "true"));
        assertFalse(r.fragment().thresholds().isEmpty(), "must still declare its thresholds");
        r.fragment().thresholds().forEach(t -> {
            assertEquals(RunFragment.Result.NOT_EVALUATED, t.result());
            assertEquals("smoke-mode", t.reason());
        });
    }

    @Test
    void smokeModeStillAssertsCorrectnessInvariants() throws Exception {
        ModuleResult r = runner.run("TST-031", Map.of("HARNESS_SMOKE_MODE", "true"));
        assertFalse(r.fragment().invariants().isEmpty(), "must still declare its invariants");
        assertTrue(r.fragment().invariants().stream()
            .allMatch(i -> i.result() == RunFragment.Result.PASSED),
            "smoke mode must not skip correctness -- every invariant must genuinely pass "
                + "on a clean SUT, not merely at least one of them");
    }

    @Test
    void reportsFailureAgainstTheLeakyDefect() throws Exception {
        ModuleResult r = runner.run("TST-031",
            Map.of("SUT_DEFECT", "ratelimit-leaky", "HARNESS_SMOKE_MODE", "true"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
    }
}
