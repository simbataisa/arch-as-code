package com.techcombank.qe.harness.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads a TST-002 performance profile from {@code qe-harness/profiles/}.
 *
 * <p>Sibling of {@link ThresholdResolver} and deliberately the same shape: a
 * no-arg constructor that locates the real profiles directory by walking up
 * from the working directory, an explicit-path constructor for fixtures, and a
 * resolve method that <b>throws on an unknown name and never defaults</b>. A
 * silently-defaulted workload shape would make a run's own parameters
 * unfalsifiable.
 *
 * <p>This is the first code in the repository to read a profile file at all --
 * before Wave 17, {@code mixed.yml} and {@code soak.yml} were parsed by
 * nothing. Only the fields TST-034 actually asserts against are surfaced;
 * profile shape parameters this harness does not consume stay unread rather
 * than being exposed speculatively.
 *
 * <p>Note: {@code blend_ref}, {@code blend}, and {@code hold_seconds} live
 * under the profile's {@code load_shape} map (not at the document root) --
 * this reads them from there accordingly.
 */
public final class ProfileResolver {

    /** One journey in a declared blend. */
    public record Journey(String name, long share, String tier, String endpoint) {}

    /** The subset of a profile this harness consumes. */
    public record Profile(
        String name,
        String workloadModel,
        String blendRef,
        Map<String, Long> blend,
        Map<String, Journey> journeys,
        long holdSeconds,
        long smokeHoldSeconds
    ) {}

    private final Path profilesDir;

    public ProfileResolver() {
        this(locateDefaultProfilesDir());
    }

    public ProfileResolver(Path profilesDir) {
        this.profilesDir = profilesDir;
    }

    @SuppressWarnings("unchecked")
    public Profile load(String name) {
        Path path = profilesDir.resolve(name + ".yml");
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("unknown profile: " + name + " (looked in " + profilesDir + ")");
        }

        Map<String, Object> raw;
        try {
            raw = new Yaml().load(Files.readString(path));
        } catch (IOException e) {
            throw new IllegalStateException("cannot read profile " + path, e);
        }
        if (raw == null) {
            throw new IllegalStateException("profile " + path + " is empty");
        }

        Map<String, Object> loadShape = (Map<String, Object>) raw.get("load_shape");
        if (loadShape == null) {
            throw new IllegalStateException("profile " + name + " declares no load_shape");
        }

        String blendRef = (String) loadShape.get("blend_ref");
        if (blendRef == null || blendRef.isBlank()) {
            throw new IllegalStateException(
                "profile " + name + " declares no blend_ref; a blended run needs a declared mix");
        }

        Map<String, Object> blendRaw = (Map<String, Object>) loadShape.get("blend");
        if (blendRaw == null || blendRaw.isEmpty()) {
            throw new IllegalStateException("profile " + name + " declares blend_ref but no blend");
        }

        Map<String, Long> shares = new LinkedHashMap<>();
        Map<String, Journey> journeys = new LinkedHashMap<>();
        long total = 0;
        for (Map.Entry<String, Object> entry : blendRaw.entrySet()) {
            Map<String, Object> j = (Map<String, Object>) entry.getValue();
            long share = ((Number) j.get("share")).longValue();
            shares.put(entry.getKey(), share);
            journeys.put(entry.getKey(), new Journey(
                entry.getKey(), share, (String) j.get("tier"), (String) j.get("endpoint")));
            total += share;
        }
        if (total != 100L) {
            throw new IllegalStateException(
                "profile " + name + " blend shares sum to " + total + ", not 100");
        }

        long holdSeconds = ((Number) loadShape.get("hold_seconds")).longValue();
        long smokeHold = holdSeconds;
        Map<String, Object> overrides = (Map<String, Object>) raw.get("smoke_mode_overrides");
        if (overrides != null && overrides.get("hold_seconds") != null) {
            smokeHold = ((Number) overrides.get("hold_seconds")).longValue();
        }

        return new Profile(name, (String) raw.get("workload_model"), blendRef,
            Map.copyOf(shares), Map.copyOf(journeys), holdSeconds, smokeHold);
    }

    private static Path locateDefaultProfilesDir() {
        Path cursor = Path.of("").toAbsolutePath();
        while (cursor != null) {
            Path candidate = cursor.resolve("qe-harness/profiles");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            Path direct = cursor.resolve("profiles");
            if (Files.isDirectory(direct) && Files.isRegularFile(direct.resolve("mixed.yml"))) {
                return direct;
            }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("cannot locate qe-harness/profiles from " + Path.of("").toAbsolutePath());
    }
}
