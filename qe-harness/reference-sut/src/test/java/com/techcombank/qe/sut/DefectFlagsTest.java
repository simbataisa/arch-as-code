package com.techcombank.qe.sut;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefectFlagsTest {

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
    void knownFlagsContainsAllSevenArchetypeDefects() {
        assertEquals(7, DefectFlags.KNOWN_FLAGS.size());
        assertTrue(DefectFlags.KNOWN_FLAGS.containsAll(java.util.Set.of(
            "ledger-unbalanced", "schema-drift", "ratelimit-leaky",
            "breaker-disabled", "recon-false-clean", "authz-missing-marker",
            "cache-headers-absent")));
    }
}
