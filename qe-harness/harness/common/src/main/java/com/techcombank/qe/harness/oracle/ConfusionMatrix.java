package com.techcombank.qe.harness.oracle;

import java.util.HashSet;
import java.util.Set;

/**
 * Confusion-matrix oracle: compares a seeded/expected set (the deliberately-planted
 * ground truth, e.g. rows a reconciliation run should have flagged) against an
 * actual/reported set (what the system under test actually flagged), and scores the
 * divergence.
 *
 * <p>Used by TST-039 (Data Quality &amp; Reconciliation) and any other archetype whose
 * pass criterion is precision/recall against a labelled corpus rather than a single
 * boolean invariant.
 */
public final class ConfusionMatrix {

    private ConfusionMatrix() {
    }

    /**
     * @param tp        true positives: reported and actually seeded
     * @param fp        false positives: reported but not seeded
     * @param fn        false negatives: seeded but not reported
     * @param precision {@code tp / (tp + fp)}; {@link Double#NaN} when {@code tp + fp == 0}
     *                  -- a run that reported nothing has no precision to speak of, and
     *                  must never be scored as a perfect {@code 1.0}.
     * @param recall    {@code tp / (tp + fn)}; {@link Double#NaN} when {@code tp + fn == 0}
     *                  -- true only when nothing was seeded, i.e. there was nothing to find.
     */
    public record Score(int tp, int fp, int fn, double precision, double recall) {
    }

    /**
     * Scores {@code actual} against {@code expected}. Never defaults an undefined
     * precision/recall to a "clean" value -- see {@link Score}.
     */
    public static <T> Score score(Set<T> expected, Set<T> actual) {
        Set<T> truePositives = new HashSet<>(expected);
        truePositives.retainAll(actual);

        Set<T> falsePositives = new HashSet<>(actual);
        falsePositives.removeAll(expected);

        Set<T> falseNegatives = new HashSet<>(expected);
        falseNegatives.removeAll(actual);

        int tp = truePositives.size();
        int fp = falsePositives.size();
        int fn = falseNegatives.size();

        double precision = (tp + fp == 0) ? Double.NaN : (double) tp / (tp + fp);
        double recall = (tp + fn == 0) ? Double.NaN : (double) tp / (tp + fn);

        return new Score(tp, fp, fn, precision, recall);
    }
}
