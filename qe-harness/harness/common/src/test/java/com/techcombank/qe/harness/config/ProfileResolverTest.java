package com.techcombank.qe.harness.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * First reader of a TST-002 performance profile in this repository. Mirrors
 * ThresholdResolver's contract deliberately: locate the real profiles
 * directory by walking up, and throw on an unknown name rather than
 * defaulting -- a silently-defaulted workload shape is a fabricated test.
 */
class ProfileResolverTest {

    @Test
    void resolvesTheDeclaredBlendFromMixedProfile() {
        ProfileResolver.Profile mixed = new ProfileResolver().load("mixed");
        assertEquals("open", mixed.workloadModel());
        assertTrue(mixed.blend().size() >= 2, "a blend needs at least two journeys");
        long total = mixed.blend().values().stream().mapToLong(Long::longValue).sum();
        assertEquals(100L, total, "declared journey shares must sum to 100");
    }

    @Test
    void smokeModeOverridesTheHoldDuration() {
        ProfileResolver.Profile mixed = new ProfileResolver().load("mixed");
        assertTrue(mixed.smokeHoldSeconds() < mixed.holdSeconds(),
            "smoke mode must be shorter than the full hold");
    }

    @Test
    void anUnknownProfileThrowsRatherThanDefaulting() {
        assertThrows(IllegalArgumentException.class, () -> new ProfileResolver().load("nonesuch"));
    }
}
