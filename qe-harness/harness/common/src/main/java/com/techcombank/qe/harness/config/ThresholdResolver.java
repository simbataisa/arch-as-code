package com.techcombank.qe.harness.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves named NFR thresholds from {@code qe-harness/profiles/_nfr-thresholds.yml}
 * (Task 4) -- the one place a numeric SLO target is allowed to live, each entry
 * carrying the {@code NFR-*} citation it was copied from.
 *
 * <p>Wave 15 forbade hardcoded thresholds; this class is the enforcement point for that
 * rule on the read side. {@link #resolve(String)} <b>throws</b> on an unknown name rather
 * than defaulting to some fallback value -- a silently-defaulted threshold is an invented
 * threshold, and this whole harness exists to forbid exactly that. Callers must not catch
 * {@link IllegalArgumentException} here and substitute a guess.
 */
public final class ThresholdResolver {

    /**
     * @param value       the numeric threshold, in {@code unit}
     * @param unit        e.g. {@code "ms"}, {@code "tps"}, {@code "pct"}, {@code "min"}
     * @param thresholdRef the {@code NFR-*} citation this value was copied from, e.g.
     *                    {@code "NFR-002#end-to-end-budgets-per-tier-customer-facing"}
     */
    public record Threshold(double value, String unit, String thresholdRef) {
    }

    private final Map<String, Threshold> thresholds;

    /** Locates and loads the real {@code qe-harness/profiles/_nfr-thresholds.yml}. */
    public ThresholdResolver() {
        this(locateDefaultYamlPath());
    }

    /** Loads thresholds from an explicit YAML path (for tests that need a fixture). */
    public ThresholdResolver(Path yamlPath) {
        this.thresholds = load(yamlPath);
    }

    /**
     * Resolves the threshold named {@code name}.
     *
     * @throws IllegalArgumentException if {@code name} is not a threshold defined in
     *                                   {@code _nfr-thresholds.yml} -- never defaulted.
     */
    public Threshold resolve(String name) {
        Threshold threshold = thresholds.get(name);
        if (threshold == null) {
            throw new IllegalArgumentException(
                "Unknown threshold name '" + name + "'. Refusing to default -- add it to "
                    + "qe-harness/profiles/_nfr-thresholds.yml with a real NFR-* citation "
                    + "instead of inventing a value here.");
        }
        return threshold;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Threshold> load(Path yamlPath) {
        try (InputStream in = Files.newInputStream(yamlPath)) {
            Map<String, Object> root = new Yaml().load(in);
            Object rawThresholds = (root == null) ? null : root.get("thresholds");
            if (!(rawThresholds instanceof List<?> rows)) {
                throw new IllegalStateException(
                    "Expected a 'thresholds' list in " + yamlPath + " but found none");
            }

            Map<String, Threshold> result = new LinkedHashMap<>();
            for (Object rowObj : rows) {
                Map<String, Object> row = (Map<String, Object>) rowObj;
                String name = (String) row.get("name");
                String thresholdRef = (String) row.get("threshold_ref");
                Number value = (Number) row.get("value");
                String unit = (String) row.get("unit");
                if (name == null || thresholdRef == null || value == null || unit == null) {
                    throw new IllegalStateException(
                        "Threshold row missing a required field (name/threshold_ref/value/unit) "
                            + "in " + yamlPath + ": " + row);
                }
                result.put(name, new Threshold(value.doubleValue(), unit, thresholdRef));
            }
            return Map.copyOf(result);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load NFR thresholds from " + yamlPath, e);
        }
    }

    /**
     * Walks up from the current working directory looking for
     * {@code qe-harness/profiles/_nfr-thresholds.yml} (Maven's working directory for a
     * reactor module is that module's own directory, e.g. {@code qe-harness/harness/common},
     * so this has to search upward rather than assume the repo root).
     */
    private static Path locateDefaultYamlPath() {
        Path start = Path.of("").toAbsolutePath();
        Path current = start;
        while (current != null) {
            Path viaQeHarness = current.resolve("qe-harness/profiles/_nfr-thresholds.yml");
            if (Files.isRegularFile(viaQeHarness)) {
                return viaQeHarness;
            }
            Path viaProfiles = current.resolve("profiles/_nfr-thresholds.yml");
            if (Files.isRegularFile(viaProfiles)) {
                return viaProfiles;
            }
            current = current.getParent();
        }
        throw new IllegalStateException(
            "Could not locate qe-harness/profiles/_nfr-thresholds.yml walking up from " + start);
    }
}
