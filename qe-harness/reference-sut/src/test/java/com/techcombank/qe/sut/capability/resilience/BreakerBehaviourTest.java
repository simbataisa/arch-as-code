package com.techcombank.qe.sut.capability.resilience;

import com.sun.net.httpserver.HttpServer;
import com.techcombank.qe.sut.DefectFlags;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * TST-035 circuit breaker and degraded response tests.
 *
 * <p>{@code @SpringBootTest(classes = TestApp.class, webEnvironment =
 * RANDOM_PORT)} rather than the production {@code ReferenceSutApplication}:
 * this capability needs a real embedded server (so {@link DownstreamClient}'s
 * Resilience4j {@code @CircuitBreaker} is woven via a genuine Spring AOP
 * proxy, and so {@code rest} makes real HTTP round trips) but, per the
 * brief, no Postgres/Testcontainers/Docker. {@link TestApp} is a nested
 * {@code @SpringBootApplication} whose default component scan is scoped to
 * its own package -- {@code com.techcombank.qe.sut.capability.resilience},
 * i.e. exactly {@link DownstreamClient} and {@link QuoteController} -- so
 * the ledger/contract/authz/ratelimit capabilities' beans (several of which
 * need a live {@code DataSource}) never enter this context at all. The
 * three excluded autoconfigurations are belt-and-braces: {@code DataSource}/
 * {@code Flyway} would otherwise still try to activate purely from the
 * {@code org.postgresql:postgresql} driver and {@code spring-boot-starter-
 * jdbc} being on the module's classpath (autoconfiguration is
 * classpath-triggered, not scan-scoped), and excluding Spring Security's
 * autoconfiguration avoids its zero-config deny-all default landing on
 * {@code /quotes/**} -- the real {@code SecurityConfig} (Task 9) already
 * permits this path in production; this capability has no auth requirement
 * of its own to exercise here.
 *
 * <p>{@link #downstream} is a hand-rolled, in-process stub built on the
 * JDK's own {@code com.sun.net.httpserver.HttpServer} -- no WireMock/
 * Testcontainers dependency needed for what these tests require.
 * {@code blackhole()} stops the server outright, so a connection attempt
 * fails immediately with "connection refused" (fast and deterministic --
 * no read-timeout wait needed to detect the fault). {@code restore()}
 * rebinds a fresh server to the exact same port. The port itself is fixed
 * before the Spring context starts and wired in via
 * {@code @DynamicPropertySource}, the standard way to hand a test's own
 * ahead-of-context-startup state into {@code application.yml}'s
 * {@code app.downstream.base-url}.
 *
 * <p>Flakiness note: {@code breakerOpensWithinDeclaredThreshold} and
 * {@code breakerClosesAfterFaultRemoved} are the two tests exposed to real
 * wall-clock/state-transition timing. Neither hardcodes a duplicate
 * threshold number -- {@link #breakerMinimumCalls} is read directly off the
 * live {@link CircuitBreaker}'s own config each test (same
 * "measure-the-declared-configuration" rule as TST-040's clock-skew test),
 * and {@code application.yml}'s {@code waitDurationInOpenState} (2s) is
 * deliberately short so {@code awaitClosed}'s 20-second budget has wide
 * margin rather than being a near-miss.
 */
@SpringBootTest(classes = BreakerBehaviourTest.TestApp.class,
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BreakerBehaviourTest {

    private static final DownstreamStub downstream = new DownstreamStub();
    private static final int BREAKER_SLACK = 2;

    @DynamicPropertySource
    static void downstreamProps(DynamicPropertyRegistry registry) {
        registry.add("app.downstream.base-url", downstream::baseUrl);
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private int breakerMinimumCalls;

    @BeforeEach
    void resetState() {
        DefectFlags.clear();
        downstream.restore();
        CircuitBreaker breaker = breaker();
        breaker.reset(); // clears metrics and transitions back to CLOSED
        breakerMinimumCalls = breaker.getCircuitBreakerConfig().getMinimumNumberOfCalls();
    }

    @AfterAll
    static void stopStub() {
        downstream.blackhole();
    }

    @Test
    void downstreamFailureYieldsDeclaredDegradedResponseNotAnError() {
        downstream.blackhole();
        ResponseEntity<Map> r = rest.getForEntity("/quotes/Q1", Map.class);
        assertEquals(200, r.getStatusCode().value(), "downstream failure must not surface as 5xx");
        assertEquals(true, r.getBody().get("degraded"));
    }

    @Test
    void breakerOpensWithinDeclaredThreshold() {
        downstream.blackhole();
        int callsUntilOpen = callUntilBreakerOpen();
        assertTrue(callsUntilOpen <= breakerMinimumCalls + BREAKER_SLACK,
            "breaker took " + callsUntilOpen + " calls to open");
    }

    @Test
    void breakerClosesAfterFaultRemoved() {
        downstream.blackhole();
        callUntilBreakerOpen();
        downstream.restore();
        awaitClosed(Duration.ofSeconds(20));
        assertEquals(false, rest.getForEntity("/quotes/Q1", Map.class).getBody().get("degraded"));
    }

    @Test
    void defectFlagLetsDownstreamFailureSurfaceAsFiveHundred() {
        withDefect("breaker-disabled", () -> {
            downstream.blackhole();
            assertEquals(500, rest.getForEntity("/quotes/Q1", Map.class).getStatusCode().value());
        });
    }

    // ---- helpers ----

    private CircuitBreaker breaker() {
        return circuitBreakerRegistry.circuitBreaker("downstream");
    }

    /** Calls GET /quotes/Q1 until the breaker reports OPEN, returning the
     *  number of calls that took. Bounded well above the declared threshold
     *  so a regression fails fast with a clear message instead of hanging. */
    private int callUntilBreakerOpen() {
        int maxAttempts = (breakerMinimumCalls + BREAKER_SLACK) * 3;
        for (int i = 1; i <= maxAttempts; i++) {
            rest.getForEntity("/quotes/Q1", Map.class);
            if (breaker().getState() == CircuitBreaker.State.OPEN) {
                return i;
            }
        }
        fail("breaker never opened after " + maxAttempts + " calls");
        return -1;
    }

    /** Polls with real HTTP calls (each one both exercises and observes the
     *  breaker) until it reports CLOSED, or fails at {@code timeout}. */
    private void awaitClosed(Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            rest.getForEntity("/quotes/Q1", Map.class);
            if (breaker().getState() == CircuitBreaker.State.CLOSED) {
                return;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fail("interrupted while awaiting breaker close");
            }
        }
        fail("breaker did not close within " + timeout);
    }

    private void withDefect(String flag, Runnable action) {
        DefectFlags.activate(flag);
        try {
            action.run();
        } finally {
            DefectFlags.clear();
        }
    }

    /** Minimal, self-contained app context for this capability only -- see
     *  the class Javadoc above for why. */
    @SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
    })
    static class TestApp {
    }

    /** Hand-rolled downstream test double: a JDK {@code HttpServer} that
     *  always answers a canned quote until {@link #blackhole()}, which
     *  stops it outright so subsequent connection attempts fail immediately
     *  ("connection refused") rather than hanging until a read timeout. */
    private static final class DownstreamStub {
        private final int port;
        private volatile HttpServer server;

        DownstreamStub() {
            this.port = findFreePort();
            startServer();
        }

        String baseUrl() {
            return "http://localhost:" + port;
        }

        void blackhole() {
            HttpServer s = server;
            if (s != null) {
                s.stop(0);
                server = null;
            }
        }

        void restore() {
            if (server == null) {
                startServer();
            }
        }

        private void startServer() {
            try {
                HttpServer s = HttpServer.create(new InetSocketAddress(port), 0);
                s.createContext("/", exchange -> {
                    byte[] body = "{\"id\":\"stub\",\"price\":100.0}"
                        .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(body);
                    }
                });
                s.setExecutor(null);
                s.start();
                server = s;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        private static int findFreePort() {
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
