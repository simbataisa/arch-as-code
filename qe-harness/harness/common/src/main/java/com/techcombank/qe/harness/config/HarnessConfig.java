package com.techcombank.qe.harness.config;

import java.util.Set;

/**
 * Process-wide harness configuration read from the environment.
 *
 * <p>Kept deliberately tiny: one flag today ({@link #smokeMode()}), read straight from
 * {@code System.getenv()} rather than a config file, so CI can flip it per-job without
 * touching a profile.
 */
public final class HarnessConfig {

    private static final Set<String> TRUTHY = Set.of("true", "1", "yes", "on");

    private HarnessConfig() {
    }

    /**
     * True when {@code HARNESS_SMOKE_MODE} is set to a truthy value ({@code true}, {@code 1},
     * {@code yes}, {@code on}; case-insensitive). Used by the perf modules (Tasks 17-18) to
     * decide whether an NFR threshold should be enforced or recorded as
     * {@code not-evaluated} -- a smoke run against a trivial load profile has no meaningful
     * p99 to compare against a production threshold.
     */
    public static boolean smokeMode() {
        String raw = System.getenv("HARNESS_SMOKE_MODE");
        return raw != null && TRUTHY.contains(raw.trim().toLowerCase());
    }
}
