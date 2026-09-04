package com.techcombank.qe.sut;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * At-runtime defect toggle for the reference SUT.
 *
 * <p>Design note: this intentionally does NOT read {@code SUT_DEFECT} once at
 * process startup and reject an unrecognised value there. The harness modules
 * built in Tasks 16-23 drive HTTP requests against this SUT from a separate
 * process (a JMeter/Karate/Locust/k6 tool process, or a shell script) against
 * an already-running {@code docker compose up} container (Task 14) — none of
 * them can set an environment variable on a running container. An
 * env-var-at-startup design would make defect injection impossible without
 * restarting the container for every defect-pair test run.
 *
 * <p>Instead, the active flag is a runtime-mutable reference toggled via the
 * {@code POST /_test/defect/{flag}} and {@code DELETE /_test/defect} HTTP
 * endpoints (see {@link DefectController}). {@link #activate(String)} still
 * throws on an unrecognised flag — a typo'd flag name must fail loudly, not
 * silently produce a clean SUT — just enforced at activation time rather than
 * at process startup.
 */
public final class DefectFlags {

    /** The complete, closed set of defect flags this SUT understands. Wave 16
     *  added the first seven; each Wave 17 task that introduces a new defect
     *  flag adds it here and to DefectFlagsTest's KNOWN_FLAGS_AT_WAVE_17 in the
     *  same commit -- see that test for the guard that keeps this set from
     *  drifting from what the flag actually ships. */
    public static final Set<String> KNOWN_FLAGS = Set.of(
        "ledger-unbalanced", "schema-drift", "ratelimit-leaky",
        "breaker-disabled", "recon-false-clean", "authz-missing-marker",
        "cache-headers-absent",
        "reservation-overcommit",
        "outbox-published-count-stale",
        "journey-starved",
        "route-default-fallthrough"
    );

    private static final AtomicReference<String> ACTIVE = new AtomicReference<>();

    private DefectFlags() {}

    /** Activates a defect flag. Throws IllegalArgumentException on an unknown
     *  flag — a typo'd flag name must fail loudly, not silently produce a
     *  clean SUT. This preserves the original "loud failure on typo" property,
     *  now enforced at activation time rather than at process startup. */
    public static void activate(String flag) {
        if (!KNOWN_FLAGS.contains(flag)) {
            throw new IllegalArgumentException("unknown defect flag: " + flag);
        }
        ACTIVE.set(flag);
    }

    public static void clear() {
        ACTIVE.set(null);
    }

    public static boolean isActive(String flag) {
        return flag.equals(ACTIVE.get());
    }
}
