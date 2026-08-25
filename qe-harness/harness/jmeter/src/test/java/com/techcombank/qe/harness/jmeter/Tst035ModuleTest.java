package com.techcombank.qe.harness.jmeter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-035 fault-injection module (Task 18). Drives real HTTP traffic against
 * the reference SUT's circuit-breaker capability (Task 11) while a Toxiproxy
 * toxic breaks its downstream dependency -- requires {@code make up
 * PROFILES="core resilience"} to already be running (see
 * qe-harness/README.md): the {@code resilience} profile is what brings up
 * Toxiproxy and {@code downstream-stub} in addition to {@code core}'s
 * postgres + reference-sut, unlike TST-021/TST-031 which need only
 * {@code core}.
 *
 * <p>These three tests are given verbatim by the task brief. The third,
 * {@link #restoresTheProxyEvenWhenAssertionsFail()}, is this module's most
 * important correctness property (the fault-injection analogue of TST-016's
 * defect-flag cleanup-on-failure guarantee): it forces a run that genuinely
 * fails an invariant (I1, via {@code breaker-disabled} making a downstream
 * failure surface as a real {@code 500} while the fault is still active) and
 * then asserts the Toxiproxy {@code downstream} proxy carries no toxic
 * afterward -- proving {@code plan.jmx}'s TearDownThreadGroup removed it
 * unconditionally, not only on a clean pass. A failure to restore here would
 * corrupt every later run against this same long-lived container, exactly
 * the failure mode a "classic fault-injection bug" produces.
 *
 * <p>{@link #toxiproxy} is a small, local test helper (not part of the
 * shared {@code ModuleRunner}/{@code ModuleResult} fixture, which has no
 * reason to know about Toxiproxy at all) that reads the {@code downstream}
 * proxy's own state straight from its control API -- the same {@code
 * GET /proxies/{name}} call {@code toxic-control.groovy} itself uses to
 * decide whether a toxic is already present.
 */
class Tst035ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();
    private final ToxiproxyProbe toxiproxy = new ToxiproxyProbe();

    @Test
    void assertsDegradedResponseRatherThanFiveHundred() throws Exception {
        ModuleResult r = runner.run("TST-035", Map.of("HARNESS_SMOKE_MODE", "true"));
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void reportsFailureAgainstTheBreakerDisabledDefect() throws Exception {
        ModuleResult r = runner.run("TST-035",
            Map.of("SUT_DEFECT", "breaker-disabled", "HARNESS_SMOKE_MODE", "true"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
    }

    @Test
    void restoresTheProxyEvenWhenAssertionsFail() throws Exception {
        runner.run("TST-035", Map.of("SUT_DEFECT", "breaker-disabled",
                                     "HARNESS_SMOKE_MODE", "true"));
        assertTrue(toxiproxy.isClean(), "module must not leave the proxy in a faulted state");
    }

    /**
     * Reads the {@code downstream} Toxiproxy proxy's own {@code toxics}
     * list over its control API ({@code TOXIPROXY_BASE_URL}, default
     * {@code http://localhost:8474} -- same default {@code
     * toxic-control.groovy} uses). "Clean" means exactly what {@code
     * plan.jmx}'s TearDown Thread Group is responsible for guaranteeing:
     * no leftover toxic on the proxy this module faulted.
     */
    private static final class ToxiproxyProbe {

        private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        private final ObjectMapper mapper = new ObjectMapper();
        private final String baseUrl = System.getenv().getOrDefault(
            "TOXIPROXY_BASE_URL", "http://localhost:8474");

        boolean isClean() throws IOException, InterruptedException {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/proxies/downstream"))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                    "GET " + baseUrl + "/proxies/downstream -> " + response.statusCode()
                        + " " + response.body());
            }
            JsonNode toxics = mapper.readTree(response.body()).path("toxics");
            return toxics.isArray() && toxics.isEmpty();
        }
    }
}
