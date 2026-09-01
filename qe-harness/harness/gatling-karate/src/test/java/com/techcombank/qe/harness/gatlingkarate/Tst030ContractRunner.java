package com.techcombank.qe.harness.gatlingkarate;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import com.intuit.karate.core.ScenarioResult;
import com.techcombank.qe.harness.evidence.EvidenceEmitter;
import com.techcombank.qe.harness.evidence.RunFragment;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-030 contract compatibility module (Task 20): a plain Karate JUnit 5
 * runner. Unlike the jmeter module's {@code ModuleRunner} (a subprocess
 * fixture forking a whole separate JMeter JVM), Karate is a normal JVM
 * library -- {@link Runner#path(String...)} drives
 * {@code transfer-contract.feature} directly, in this same test process,
 * against the reference SUT's real HTTP surface.
 *
 * <p>The same {@code .feature} file this class runs is also what
 * {@code Tst030Simulation.scala}'s {@code karateFeature(...)} call drives
 * under Gatling, classpath-identical, not a copy -- see
 * {@link #sameFeatureDrivesTheGatlingSimulation()}, which is the test that
 * would catch the two runners silently drifting apart.
 */
class Tst030ContractRunner {

    static final String FEATURE = "classpath:tst-030-contract/transfer-contract.feature";
    static final String SIMULATION_SCALA =
        "src/test/scala/com/techcombank/qe/harness/gatlingkarate/Tst030Simulation.scala";

    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
    private static final String SUT_BASE_URL =
        System.getenv().getOrDefault("SUT_BASE_URL", "http://localhost:8080");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();

    /**
     * Seeds the two fixture accounts (ACC-000001/ACC-000002) this feature's
     * requests transfer between, and truncates any prior run's ledger rows
     * first. Same {@code TRUNCATE ... RESTART IDENTITY CASCADE} + insert
     * pattern the TST-021 module's own {@code plan.jmx} setUp Thread Group
     * uses -- neither {@code /v1/transfers} nor {@code /v2/transfers}
     * exposes an account-creation endpoint of its own, and
     * {@code GET}-less {@link com.techcombank.qe.sut.capability.ledger.TransferService}
     * (reference-sut) requires both accounts to already exist before a
     * transfer between them can be posted at all.
     */
    @BeforeAll
    static void seedFixtureAccounts() throws Exception {
        String jdbcUrl = System.getenv().getOrDefault("LEDGER_JDBC_URL", "jdbc:postgresql://localhost:5432/sut");
        String user = System.getenv().getOrDefault("LEDGER_JDBC_USER", "sut");
        String password = System.getenv().getOrDefault("LEDGER_JDBC_PASSWORD", "sut");
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password);
             Statement st = conn.createStatement()) {
            st.executeUpdate("TRUNCATE TABLE ledger_entry, account RESTART IDENTITY CASCADE");
            st.executeUpdate("INSERT INTO account (account_ref, party_name) VALUES ('ACC-000001', 'Test Fixture Debtor Co')");
            st.executeUpdate("INSERT INTO account (account_ref, party_name) VALUES ('ACC-000002', 'Test Fixture Creditor Co')");
        }
    }

    /**
     * Belt-and-braces: an injected defect must never leak into a later,
     * unrelated test run against this same SUT container even if a test in
     * this class throws before its own {@code finally} block runs.
     */
    @AfterAll
    static void clearAnyLeakedDefect() {
        clearDefect();
    }

    @Test
    void featureFailsAgainstTheSchemaDriftDefect() throws Exception {
        // The brief's literal given test sets a JVM systemProperty("sutDefect", ...)
        // on THIS process -- the reference SUT is a separate, already-running
        // Docker container (`docker compose --profile core up`), so a system
        // property here has zero effect on it. The real mechanism (same one
        // ModuleRunner.java uses for the jmeter modules) is activating the
        // defect over HTTP on the SUT's own test-control endpoint before the
        // run, and always clearing it afterward -- a leaked defect would
        // corrupt every subsequent module run against this SUT.
        activateDefect("schema-drift");
        try {
            Results r = Runner.path(FEATURE).parallel(1);
            assertTrue(r.getFailCount() > 0, "schema-drift must break the contract assertions");
            emitFragment(r, "schema-drift");
        } finally {
            clearDefect();
        }
    }

    @Test
    void sameFeatureDrivesTheGatlingSimulation() throws IOException {
        // The Gatling simulation MUST reference the same .feature path, not a copy.
        String sim = Files.readString(Path.of(SIMULATION_SCALA));
        assertTrue(sim.contains("tst-030-contract/transfer-contract.feature"),
            "Gatling must drive the shared feature, not a duplicated scenario");
    }

    /**
     * Not one of the brief's two given proof tests, but needed to actually
     * satisfy this module's own evidence contract ("Produces: a fragment
     * with oracle: contract-schema") for the ordinary, defect-free case --
     * matching the sibling jmeter modules' own pattern of pairing a
     * clean-SUT pass with a defect-injection failure (see
     * {@code Tst021ModuleTest#passesAgainstTheCleanSut}).
     */
    @Test
    void passesAgainstTheCleanSut() throws Exception {
        Results r = Runner.path(FEATURE).parallel(1);
        assertTrue(r.getFailCount() == 0, "the clean SUT must satisfy both contract scenarios");
        emitFragment(r, null);
    }

    private static void emitFragment(Results results, String sutDefect) throws Exception {
        RunFragment.Builder builder = RunFragment.builder()
            .archetype("TST-030")
            .module("gatling-karate")
            .serviceName("reference-sut")
            .tier("T0")
            .oracle("contract-schema")
            .environment(System.getenv().getOrDefault("QE_ENVIRONMENT", "local-compose"))
            .sutDefect(sutDefect);

        try (var scenarios = results.getScenarioResults()) {
            scenarios.forEach(sr -> builder.invariant(
                scenarioId(sr), sr.getScenario().getName(),
                sr.isFailed() ? RunFragment.Result.FAILED : RunFragment.Result.PASSED));
        }

        RunFragment fragment = builder.build();
        Path outputDir = Path.of(System.getenv().getOrDefault("EVIDENCE_OUTPUT_DIR", "../../traceability/runs"));
        new EvidenceEmitter(outputDir).emit(fragment);
    }

    private static String scenarioId(ScenarioResult sr) {
        return "SCN-" + (sr.getScenario().getName().startsWith("v2") ? "v2" : "v1");
    }

    private static void activateDefect(String flag) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(SUT_BASE_URL + "/_test/defect/" + flag))
            .timeout(HTTP_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 204) {
            throw new IllegalStateException(
                "POST /_test/defect/" + flag + " -> " + response.statusCode() + " " + response.body());
        }
    }

    private static void clearDefect() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(SUT_BASE_URL + "/_test/defect"))
                .timeout(HTTP_TIMEOUT)
                .DELETE()
                .build();
            HTTP.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            // Best-effort: a run that already failed must not fail a second
            // time, more confusingly, because cleanup itself couldn't reach
            // the SUT.
            Thread.currentThread().interrupt();
        }
    }
}
