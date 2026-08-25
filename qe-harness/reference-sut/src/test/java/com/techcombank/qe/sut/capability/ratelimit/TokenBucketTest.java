package com.techcombank.qe.sut.capability.ratelimit;

import com.techcombank.qe.sut.DefectFlags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TST-031 token-bucket rate limiter tests.
 *
 * <p>{@code @WebMvcTest(RateLimitedController.class)} boots only the web
 * layer -- no DataSource/Flyway autoconfiguration, unlike the ledger
 * capability's {@code AbstractLedgerIntegrationTest} -- because the rate
 * limiter is pure in-memory state. Spring Boot's web-slice type filter still
 * picks up {@link RateLimitFilter} automatically: it is a {@code Filter}
 * bean, one of the types {@code @WebMvcTest} auto-includes alongside
 * controllers.
 *
 * <p>{@link #clock} is a {@link Clock#fixed} instant that never advances, so
 * a directly-constructed {@link TokenBucket} in this test never refills
 * mid-loop -- the assertions below are deterministic regardless of how fast
 * the test JVM executes the loop, rather than racing a real clock.
 */
@WebMvcTest(RateLimitedController.class)
class TokenBucketTest {

    private final Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void clearDefectFlag() {
        DefectFlags.clear();
    }

    @Test
    void admitsNoMoreThanConfiguredRate() {
        TokenBucket bucket = new TokenBucket(10, Duration.ofSeconds(1), clock);
        int admitted = 0;
        for (int i = 0; i < 100; i++) if (bucket.tryAcquire()) admitted++;
        assertEquals(10, admitted, "bucket must not admit above its configured rate");
    }

    @Test
    void rejectionCarriesRetryAfter() throws Exception {
        exhaustBucket();
        mvc.perform(get("/rate-limited/ping"))
           .andExpect(status().isTooManyRequests())
           .andExpect(header().exists("Retry-After"));
    }

    @Test
    void neverReturnsServerErrorUnderOverload() throws Exception {
        for (int i = 0; i < 500; i++) {
            int sc = mvc.perform(get("/rate-limited/ping")).andReturn().getResponse().getStatus();
            assertTrue(sc == 200 || sc == 429, "unexpected status under overload: " + sc);
        }
    }

    @Test
    void defectFlagAdmitsAboveConfiguredRate() {
        withDefect("ratelimit-leaky", () -> {
            TokenBucket bucket = new TokenBucket(10, Duration.ofSeconds(1), clock);
            int admitted = 0;
            for (int i = 0; i < 100; i++) if (bucket.tryAcquire()) admitted++;
            assertTrue(admitted > 10, "leaky defect must admit above the rate");
        });
    }

    /** Drains the shared, app-wide {@code RateLimitFilter} bucket (configured
     *  capacity 10, {@code app.ratelimit.permits-per-second}) with real HTTP
     *  calls. 20 rapid in-process MockMvc calls comfortably exceeds capacity
     *  even allowing for a token or two of real-clock refill between calls. */
    private void exhaustBucket() throws Exception {
        for (int i = 0; i < 20; i++) {
            mvc.perform(get("/rate-limited/ping"));
        }
    }

    /** Activates {@code flag} for the duration of {@code action}, always
     *  clearing it afterwards even if {@code action} throws -- same pattern
     *  as the ledger capability's {@code AbstractLedgerIntegrationTest}. */
    private void withDefect(String flag, Runnable action) {
        DefectFlags.activate(flag);
        try {
            action.run();
        } finally {
            DefectFlags.clear();
        }
    }
}
