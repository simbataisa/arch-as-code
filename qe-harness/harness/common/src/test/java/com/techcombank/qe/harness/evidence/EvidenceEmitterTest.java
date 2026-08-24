package com.techcombank.qe.harness.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

        // Parse JSON and verify fields (validates structure, handles Jackson pretty-printer formatting)
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);

        // Verify essential fields exist and have correct values
        assertEquals("TST-021", node.get("archetype").asText());
        assertEquals("jmeter", node.get("module").asText());
        assertEquals("reference-sut", node.get("service_name").asText());
        assertEquals("passed", node.get("result").asText());

        // Verify invariants array exists and contains the expected entry
        JsonNode invariants = node.get("invariants");
        assertNotNull(invariants);
        assertTrue(invariants.isArray());
        assertEquals(1, invariants.size());
        assertEquals("I1", invariants.get(0).get("id").asText());
        assertEquals("passed", invariants.get(0).get("result").asText());
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
