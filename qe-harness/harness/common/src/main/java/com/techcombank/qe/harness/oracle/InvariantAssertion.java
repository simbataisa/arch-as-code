package com.techcombank.qe.harness.oracle;

import com.techcombank.qe.harness.evidence.RunFragment;

import java.util.function.BooleanSupplier;

/**
 * Invariant-assertion oracle: the simplest and most-used of the four oracle types
 * (five of the seven Wave 16 archetypes cite it as primary oracle). Wraps a single
 * boolean check -- "does this domain invariant still hold?" -- as a
 * {@link RunFragment.Entry} suitable for the evidence emitter.
 *
 * <p>A checked exception or unchecked exception thrown while evaluating the condition
 * is treated as a failed invariant, not swallowed -- a broken probe is not evidence
 * of a passing system.
 */
public final class InvariantAssertion {

    private InvariantAssertion() {
    }

    /**
     * @param id          stable identifier for this invariant within its archetype run
     *                    (e.g. {@code "I1"})
     * @param description human-readable statement of the invariant (e.g.
     *                    {@code "trial balance nets to zero"})
     * @param condition   evaluated exactly once; {@code true} means the invariant held
     */
    public static RunFragment.Entry check(String id, String description, BooleanSupplier condition) {
        boolean held;
        try {
            held = condition.getAsBoolean();
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                "Invariant check '" + id + "' (" + description + ") threw while evaluating; "
                    + "treat this as a harness/environment defect, not a scoreable result", e);
        }
        return new RunFragment.Entry(id, description,
            held ? RunFragment.Result.PASSED : RunFragment.Result.FAILED);
    }
}
