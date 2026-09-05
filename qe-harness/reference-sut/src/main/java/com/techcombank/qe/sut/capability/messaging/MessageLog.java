package com.techcombank.qe.sut.capability.messaging;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory record of what this SUT published and emitted.
 *
 * <p><b>Why not read the broker's counters:</b> TST-029's I1 is "every
 * published message either produced one state change or is in the DLQ". Scoring
 * that against the broker's own accounting would be asking the component under
 * test to grade itself. This log is written by the publish path, so the harness
 * compares two independent records.
 *
 * <p><b>Identifier shape is load-bearing.</b> Gate check 5 fails the build on
 * any run of 13-19 consecutive digits anywhere under {@code qe-harness/}, and an
 * epoch-millis timestamp is exactly 13. Correlation ids are therefore
 * hyphenated short forms and timestamps are ISO-8601, truncated to
 * milliseconds. {@code MessageLogTest} pins this.
 */
@Component
public class MessageLog {

    public record Published(String routingKey, String correlationId, String publishedAt) {}

    public record Emitted(String correlationId, long sequence, String emittedAt) {}

    private final List<Published> published = new CopyOnWriteArrayList<>();
    private final List<Emitted> emissions = new CopyOnWriteArrayList<>();
    private final AtomicLong counter = new AtomicLong();

    /** Records a publication and returns the correlation id actually used. */
    public String recordPublished(String routingKey, String correlationId) {
        String id = correlationId != null ? correlationId : nextCorrelationId();
        published.add(new Published(routingKey, id, now()));
        return id;
    }

    public void recordEmitted(String correlationId, long sequence) {
        emissions.add(new Emitted(correlationId, sequence, now()));
    }

    public List<Published> published() {
        return List.copyOf(published);
    }

    public List<Emitted> emissions() {
        return List.copyOf(emissions);
    }

    public void clear() {
        published.clear();
        emissions.clear();
        counter.set(0);
    }

    /** Hyphenated short form -- never a bare counter wide enough to look like a
     *  PAN to gate check 5. */
    private String nextCorrelationId() {
        long n = counter.incrementAndGet();
        return "corr-%04d-%04d".formatted(n / 10000, n % 10000);
    }

    /** ISO-8601, not epoch millis: 13 digits would fail gate check 5. */
    private static String now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS).toString();
    }
}
