package com.techcombank.qe.harness.oracle;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfusionMatrixTest {

    @Test
    void scoresFalsePositivesAndNegativesSeparately() {
        var score = ConfusionMatrix.score(Set.of("A", "B", "C"), Set.of("B", "C", "D"));
        assertEquals(2, score.tp());   // B, C
        assertEquals(1, score.fp());   // D reported but not seeded
        assertEquals(1, score.fn());   // A seeded but not reported
    }

    @Test
    void perfectCleanRunScoresZeroEverywhere() {
        var score = ConfusionMatrix.score(Set.of(), Set.of());
        assertEquals(0, score.tp() + score.fp() + score.fn());
        assertTrue(Double.isNaN(score.precision()), "precision is undefined with no predictions");
    }
}
