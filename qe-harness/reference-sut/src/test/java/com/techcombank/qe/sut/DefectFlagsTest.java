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
    void knownFlagsContainsAllSevenArchetypeDefects() {
        assertEquals(7, DefectFlags.KNOWN_FLAGS.size());
        assertTrue(DefectFlags.KNOWN_FLAGS.containsAll(java.util.Set.of(
            "ledger-unbalanced", "schema-drift", "ratelimit-leaky",
            "breaker-disabled", "recon-false-clean", "authz-missing-marker",
            "cache-headers-absent")));
    }
}
