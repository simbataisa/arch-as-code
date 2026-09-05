package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-020 idempotency module. Requires make up PROFILES="core messaging" --
 * I7's redelivery path needs the broker Phase 2 introduced.
 */
class Tst020ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void passesAgainstTheCleanSut() throws Exception {
        ModuleResult r = runner.run("TST-020", Map.of());
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void assertsAllSevenInvariantsIncludingTheBrokerRedeliveryOne() throws Exception {
        ModuleResult r = runner.run("TST-020", Map.of());
        assertEquals(7, r.fragment().invariants().size());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I7") && i.result() == RunFragment.Result.PASSED),
            "I7 is why this module is sequenced after Phase 2; it must be genuinely evaluated");
    }

    @Test
    void reportsDeduplicationFailureAgainstTheIgnoredKeyDefect() throws Exception {
        // IdempotencyService.execute() skips the key lookup (and therefore the
        // reservation row it writes) ENTIRELY under this defect -- not just the
        // dedup check. I1 (one state change per key), I2 (byte-identical
        // replay) and I5 (one winner) all rest on that same reservation row, so
        // they fail alongside I1; so does I4, because PayloadConflict detection
        // (replayOrConflict) is reached only via that same bypassed reservation
        // path -- there is no separate conflict check left to still pass. What
        // makes the defect specific is that invariants resting on genuinely
        // DIFFERENT mechanisms are untouched: I3 (distinct keys never collide,
        // reservation or not), I6 (TTL is read from static configuration, not
        // the reservation table), and I7 (DeliveryService's own seenJobIds
        // dedup gate, a wholly separate capability from IdempotencyService).
        ModuleResult r = runner.run("TST-020", Map.of("SUT_DEFECT", "idempotency-key-ignored"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I1") && i.result() == RunFragment.Result.FAILED));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I3") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: I3 (distinct keys) does not touch the reservation path");
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I6") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: I6 reads static configuration, not the reservation table");
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I7") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: I7 exercises DeliveryService, a wholly separate capability");
    }
}
