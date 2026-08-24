package com.techcombank.qe.harness.evidence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class EvidenceEmitterTest {

    @Test
    void emitsFragmentValidatingAgainstSchema(@TempDir Path dir) throws Exception {
        RunFragment f = RunFragment.builder()
            .archetype("TST-021").module("jmeter").serviceName("reference-sut")
            .tier("T0").oracle("invariant-assertion")
            .invariant("I1", "trial balance nets to zero", RunFragment.Result.PASSED)
            .environment("ci-smoke")
            .build();

        Path out = new EvidenceEmitter(dir).emit(f);

        assertTrue(Files.exists(out));
        String json = Files.readString(out);
        assertTrue(json.contains("\"archetype\": \"TST-021\""));
        assertTrue(json.contains("\"result\": \"passed\""));
    }

    @Test
    void rejectsNotEvaluatedThresholdWithoutReason() {
        assertThrows(IllegalArgumentException.class, () ->
            RunFragment.builder()
                .archetype("TST-031").module("jmeter").serviceName("reference-sut")
                .tier("T0").oracle("invariant-assertion").environment("ci-smoke")
                .threshold("p99_latency_ms", "NFR-003#p99-latency",
                           RunFragment.Result.NOT_EVALUATED, null)
                .build());
    }

    @Test
    void overallResultIsFailedIfAnyInvariantFailed() {
        RunFragment f = RunFragment.builder()
            .archetype("TST-021").module("jmeter").serviceName("reference-sut")
            .tier("T0").oracle("invariant-assertion").environment("ci-smoke")
            .invariant("I1", "a", RunFragment.Result.PASSED)
            .invariant("I2", "b", RunFragment.Result.FAILED)
            .build();
        assertEquals(RunFragment.Result.FAILED, f.result());
    }
}
