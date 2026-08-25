package com.techcombank.qe.harness.jmeter.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techcombank.qe.harness.evidence.RunFragment;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Test-only fixture shared by every JMeter-driven module's own test
 * (Tst021ModuleTest here; the TST-031/TST-035/TST-040 tests Tasks 17-19
 * add reuse this exact class unmodified).
 *
 * <h2>What {@code run(archetype, env)} actually does</h2>
 * <ol>
 *   <li>If {@code env} carries a {@code "SUT_DEFECT"} entry, activates it on
 *       the already-running reference SUT container via {@code POST
 *       /_test/defect/{flag}} over plain HTTP -- <b>not</b> a process
 *       environment variable. The SUT is a separate container the harness
 *       modules drive over HTTP; setting an env var on this JVM (or on the
 *       {@code run-module.sh} subprocess below) has no effect on it. See
 *       {@code DefectController}/{@code DefectFlags} in the reference-sut
 *       module for the mechanism this calls.</li>
 *   <li>Shells out to {@code ./bin/run-module.sh <archetype>} with cwd set
 *       to the {@code qe-harness/} directory, forwarding every OTHER entry
 *       of {@code env} (i.e. everything except {@code SUT_DEFECT}) as a
 *       literal process environment variable on that subprocess -- kept for
 *       forward compatibility with a future module that genuinely does need
 *       one, though none of Tasks 16-19 currently do.</li>
 *   <li>Locates the newest file under {@code traceability/runs/} matching
 *       {@code *-<archetype>.json} (EvidenceEmitter's naming scheme: an
 *       ISO-instant prefix, so lexical and chronological order agree) and
 *       parses it into a {@link RunFragment}.</li>
 *   <li><b>Always</b> calls {@code DELETE /_test/defect} in a
 *       {@code finally} block, regardless of whether the run above
 *       succeeded, threw, or timed out -- an injected defect must never
 *       leak into the next test.</li>
 * </ol>
 */
public final class ModuleRunner {

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration PROCESS_TIMEOUT = Duration.ofMinutes(5);

    private final Path qeHarnessRoot;
    private final Path runsDir;
    private final String sutBaseUrl;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public ModuleRunner() {
        this(findQeHarnessRoot(), System.getenv().getOrDefault("SUT_BASE_URL", "http://localhost:8080"));
    }

    ModuleRunner(Path qeHarnessRoot, String sutBaseUrl) {
        this.qeHarnessRoot = qeHarnessRoot;
        this.runsDir = qeHarnessRoot.resolve("traceability").resolve("runs");
        this.sutBaseUrl = sutBaseUrl;
        this.http = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
        this.mapper = new ObjectMapper();
    }

    /**
     * Runs one archetype's module against the currently-running SUT.
     *
     * @param archetype e.g. {@code "TST-021"}
     * @param env       {@code "SUT_DEFECT"} (if present) is applied via the
     *                  SUT's HTTP defect-control endpoint before the run and
     *                  always cleared afterward; every other entry is passed
     *                  as a literal environment variable to the
     *                  {@code run-module.sh} subprocess.
     */
    public ModuleResult run(String archetype, Map<String, String> env) throws Exception {
        String defect = env.get("SUT_DEFECT");
        if (defect != null) {
            activateDefect(defect);
        }
        try {
            Instant startedAt = Instant.now();
            int exitCode = runModuleScript(archetype, env);
            Path fragmentFile = findNewestFragment(archetype, startedAt)
                .orElseThrow(() -> new IllegalStateException(
                    "run-module.sh " + archetype + " (exit " + exitCode + ") produced no "
                        + "traceability/runs/*-" + archetype + ".json fragment"));
            RunFragment fragment = parseFragment(fragmentFile);
            return new ModuleResult(exitCode, fragment);
        } finally {
            clearDefect();
        }
    }

    private void activateDefect(String flag) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(sutBaseUrl + "/_test/defect/" + flag))
            .timeout(HTTP_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 204) {
            throw new IllegalStateException(
                "POST /_test/defect/" + flag + " -> " + response.statusCode() + " " + response.body());
        }
    }

    private void clearDefect() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(sutBaseUrl + "/_test/defect"))
                .timeout(HTTP_TIMEOUT)
                .DELETE()
                .build();
            http.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            // Best-effort: a run that already failed must not fail a second time,
            // more confusingly, because cleanup itself couldn't reach the SUT.
            Thread.currentThread().interrupt();
        }
    }

    private int runModuleScript(String archetype, Map<String, String> env) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("./bin/run-module.sh", archetype)
            .directory(qeHarnessRoot.toFile())
            .redirectErrorStream(true);
        env.forEach((key, value) -> {
            if (!"SUT_DEFECT".equals(key)) {
                pb.environment().put(key, value);
            }
        });
        Process process = pb.start();
        // Drain stdout/stderr concurrently so a chatty subprocess (JMeter's own
        // console output) can never fill the pipe buffer and deadlock waitFor().
        Thread drain = new Thread(() -> {
            try (var in = process.getInputStream()) {
                in.transferTo(System.out);
            } catch (IOException ignored) {
                // process ended; nothing left to drain
            }
        }, "run-module.sh-output-drain");
        drain.setDaemon(true);
        drain.start();

        boolean finished = process.waitFor(PROCESS_TIMEOUT.toSeconds(), java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("run-module.sh " + archetype + " did not finish within " + PROCESS_TIMEOUT);
        }
        drain.join(Duration.ofSeconds(5).toMillis());
        return process.exitValue();
    }

    private Optional<Path> findNewestFragment(String archetype, Instant notBefore) throws IOException {
        if (!Files.isDirectory(runsDir)) {
            return Optional.empty();
        }
        String suffix = "-" + archetype + ".json";
        try (Stream<Path> files = Files.list(runsDir)) {
            return files
                .filter(p -> p.getFileName().toString().endsWith(suffix))
                .max(Comparator.comparing(ModuleRunner::lastModifiedSafely));
        }
    }

    private static Instant lastModifiedSafely(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }

    /**
     * Parses the JSON file {@code EvidenceEmitter} wrote back into a
     * {@link RunFragment}. Deliberately hand-rolled rather than a direct
     * Jackson {@code readValue(file, RunFragment.class)}: the wire format
     * ({@code evidence.schema.json}) nests {@code executed_on}/
     * {@code environment}/{@code sut_defect} inside an {@code evidence}
     * object and carries a computed top-level {@code result} field, neither
     * of which matches {@link RunFragment}'s own flat record shape -- see
     * {@code EvidenceEmitter.EvidenceWrapper}, the class that produced this
     * exact asymmetry on the way out.
     */
    private RunFragment parseFragment(Path file) throws IOException {
        JsonNode root = mapper.readTree(file.toFile());
        RunFragment.Builder builder = RunFragment.builder()
            .archetype(root.path("archetype").asText())
            .oracle(root.path("oracle").asText());
        if (root.hasNonNull("module")) builder.module(root.get("module").asText());
        if (root.hasNonNull("service_name")) builder.serviceName(root.get("service_name").asText());
        if (root.hasNonNull("tier")) builder.tier(root.get("tier").asText());

        JsonNode evidence = root.path("evidence");
        if (evidence.hasNonNull("environment")) builder.environment(evidence.get("environment").asText());
        if (evidence.hasNonNull("sut_defect")) builder.sutDefect(evidence.get("sut_defect").asText());

        for (JsonNode inv : root.path("invariants")) {
            builder.invariant(inv.path("id").asText(), inv.path("description").asText(),
                resultFromWire(inv.path("result").asText()));
        }
        for (JsonNode th : root.path("thresholds")) {
            String reason = th.hasNonNull("reason") ? th.get("reason").asText() : null;
            builder.threshold(th.path("name").asText(), th.path("threshold_ref").asText(),
                resultFromWire(th.path("result").asText()), reason);
        }
        return builder.build();
    }

    private static RunFragment.Result resultFromWire(String wire) {
        for (RunFragment.Result r : RunFragment.Result.values()) {
            if (r.wire().equals(wire)) {
                return r;
            }
        }
        throw new IllegalArgumentException("unknown evidence result value: " + wire);
    }

    /**
     * Walks up from the test JVM's working directory (the {@code jmeter}
     * module's basedir under Surefire's default fork configuration) until it
     * finds the {@code qe-harness/} directory -- identified by having both
     * {@code bin/run-module.sh} and {@code traceability/modules.yml} -- so
     * this fixture works the same whether Maven is invoked from the repo
     * root, from {@code qe-harness/harness}, or from an IDE's own working
     * directory.
     */
    private static Path findQeHarnessRoot() {
        Path candidate = Path.of("").toAbsolutePath();
        while (candidate != null) {
            if (Files.isRegularFile(candidate.resolve("bin/run-module.sh"))
                && Files.isRegularFile(candidate.resolve("traceability/modules.yml"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        throw new IllegalStateException(
            "could not locate qe-harness/ (looked for bin/run-module.sh + traceability/modules.yml "
                + "walking up from " + Path.of("").toAbsolutePath());
    }
}
