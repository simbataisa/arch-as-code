package com.techcombank.qe.harness.oracle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techcombank.qe.harness.evidence.RunFragment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Golden-dataset oracle: compares a system's actual output against a pre-recorded
 * "golden" fixture -- the expected result for a fixed, deterministic input (e.g. an
 * as-of balance query run against a synthetic seed with a known correct answer).
 *
 * <p>No Wave 16 archetype uses this as its primary oracle (recorded as a deliberate
 * omission in the Wave 16 design doc, section 5.1) -- it lands with a later wave's
 * archetype (TST-022 or TST-038). Implemented here now so the shared oracle library
 * is complete and every harness module can depend on a stable {@code common} API from
 * day one, rather than adding this type mid-wave once a consumer shows up.
 */
public final class GoldenDataset {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GoldenDataset() {
    }

    /**
     * Compares {@code actual} against a golden {@code expected} value using
     * {@link Objects#equals}. Suitable once both sides have already been deserialized
     * into comparable Java objects (records, maps, lists, ...).
     *
     * @param id          stable identifier for this comparison within its archetype run
     * @param description human-readable statement of what golden fixture is being checked
     */
    public static RunFragment.Entry check(String id, String description, Object expected, Object actual) {
        boolean matches = Objects.equals(expected, actual);
        return new RunFragment.Entry(id, description,
            matches ? RunFragment.Result.PASSED : RunFragment.Result.FAILED);
    }

    /**
     * Loads a golden fixture from a JSON file on disk and deserializes it as {@code type}.
     * Kept separate from {@link #check} so a module can load once and compare many times
     * (e.g. one golden fixture checked against several concurrent query results).
     */
    public static <T> T loadGolden(Path fixturePath, Class<T> type) {
        try {
            return MAPPER.readValue(Files.readString(fixturePath), type);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load golden fixture from " + fixturePath, e);
        }
    }
}
