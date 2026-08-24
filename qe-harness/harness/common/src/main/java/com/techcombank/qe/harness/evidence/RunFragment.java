package com.techcombank.qe.harness.evidence;

import java.time.LocalDate;
import java.util.*;

public record RunFragment(
    String archetype, String module, String serviceName, String tier, String oracle,
    List<Entry> invariants, List<Threshold> thresholds,
    String environment, String sutDefect, LocalDate executedOn
) {
    public enum Result {
        PASSED("passed"), FAILED("failed"),
        NOT_EVALUATED("not-evaluated"), NOT_IMPLEMENTED("not-implemented");
        private final String wire;
        Result(String w) { this.wire = w; }
        public String wire() { return wire; }
    }

    public record Entry(String id, String description, Result result) {}
    public record Threshold(String name, String thresholdRef, Result result, String reason) {}

    /** FAILED if any invariant or threshold failed; else NOT_EVALUATED if nothing was
     *  evaluated at all; else PASSED. Never silently PASSED on an empty run. */
    public Result result() {
        boolean anyFailed = invariants.stream().anyMatch(i -> i.result() == Result.FAILED)
            || thresholds.stream().anyMatch(t -> t.result() == Result.FAILED);
        if (anyFailed) return Result.FAILED;
        boolean anyEvaluated = invariants.stream().anyMatch(i -> i.result() == Result.PASSED)
            || thresholds.stream().anyMatch(t -> t.result() == Result.PASSED);
        return anyEvaluated ? Result.PASSED : Result.NOT_EVALUATED;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String archetype, module, serviceName, tier, oracle, environment, sutDefect;
        private final List<Entry> invariants = new ArrayList<>();
        private final List<Threshold> thresholds = new ArrayList<>();

        public Builder archetype(String v) { this.archetype = v; return this; }
        public Builder module(String v) { this.module = v; return this; }
        public Builder serviceName(String v) { this.serviceName = v; return this; }
        public Builder tier(String v) { this.tier = v; return this; }
        public Builder oracle(String v) { this.oracle = v; return this; }
        public Builder environment(String v) { this.environment = v; return this; }
        public Builder sutDefect(String v) { this.sutDefect = v; return this; }

        public Builder invariant(String id, String desc, Result r) {
            invariants.add(new Entry(id, desc, r)); return this;
        }

        public Builder threshold(String name, String ref, Result r, String reason) {
            if (r == Result.NOT_EVALUATED && (reason == null || reason.isBlank())) {
                throw new IllegalArgumentException(
                    "threshold '" + name + "' is not-evaluated and must carry a reason");
            }
            thresholds.add(new Threshold(name, ref, r, reason)); return this;
        }

        public RunFragment build() {
            Objects.requireNonNull(archetype, "archetype");
            Objects.requireNonNull(oracle, "oracle");
            return new RunFragment(archetype, module, serviceName, tier, oracle,
                List.copyOf(invariants), List.copyOf(thresholds),
                environment, sutDefect, LocalDate.now());
        }
    }
}
