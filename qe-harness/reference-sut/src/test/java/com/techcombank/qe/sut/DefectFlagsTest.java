package com.techcombank.qe.sut;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DefectFlagsTest {

    /** The registry's expected contents. Each Wave 17 task that introduces a
     *  new defect flag appends its name here in the same commit that adds it
     *  to DefectFlags.KNOWN_FLAGS -- so this set is the drift guard, mirroring
     *  CapabilityRegistryTest's IMPLEMENTED_AT_WAVE_17. */
    private static final Set<String> KNOWN_FLAGS_AT_WAVE_17 = Set.of(
        "ledger-unbalanced", "schema-drift", "ratelimit-leaky",
        "breaker-disabled", "recon-false-clean", "authz-missing-marker",
        "cache-headers-absent", "reservation-overcommit",
        "outbox-published-count-stale", "journey-starved",
        "route-default-fallthrough", "resequencer-emits-on-arrival",
        "aggregate-emitted-incomplete"
    );

    @AfterEach
    void clearState() {
        DefectFlags.clear();
    }

    @Test
    void activateAndIsActiveRoundTrip() {
        DefectFlags.activate("ledger-unbalanced");
        assertTrue(DefectFlags.isActive("ledger-unbalanced"));
        assertFalse(DefectFlags.isActive("schema-drift"));
    }

    @Test
    void clearDeactivatesFlag() {
        DefectFlags.activate("ratelimit-leaky");
        DefectFlags.clear();
        assertFalse(DefectFlags.isActive("ratelimit-leaky"));
    }

    @Test
    void nothingIsActiveInitially() {
        assertFalse(DefectFlags.isActive("breaker-disabled"));
    }

    @Test
    void activateRejectsUnknownFlag() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> DefectFlags.activate("not-a-real-flag"));
        assertTrue(ex.getMessage().contains("not-a-real-flag"));
    }

    @Test
    void switchesBetweenFlagsWithAndWithoutInterveningClear() {
        DefectFlags.activate("ledger-unbalanced");
        assertTrue(DefectFlags.isActive("ledger-unbalanced"));

        DefectFlags.clear();
        DefectFlags.activate("schema-drift");
        assertTrue(DefectFlags.isActive("schema-drift"));
        assertFalse(DefectFlags.isActive("ledger-unbalanced"));

        // Direct switch, no intervening clear() — the sequence the harness
        // (Tasks 16-23) depends on when moving from one defect-pair run to
        // the next without a container restart.
        DefectFlags.activate("ratelimit-leaky");
        assertTrue(DefectFlags.isActive("ratelimit-leaky"));
        assertFalse(DefectFlags.isActive("schema-drift"));
    }

    @Test
    void knownFlagsMatchesTheDeclaredSetExactly() {
        assertEquals(KNOWN_FLAGS_AT_WAVE_17, DefectFlags.KNOWN_FLAGS);
    }
}
