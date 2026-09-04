package com.techcombank.qe.sut.capability.messaging;

import com.techcombank.qe.sut.DefectFlags;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TST-028 fan-out / fan-in correlation capability.
 *
 * <p><b>The partial marker is the point.</b> I1 permits an aggregate to be
 * emitted incomplete only when it is timed out AND carries a partial marker.
 * Emitting an incomplete set without that marker, or silently emitting nothing
 * at all past the timeout, are both violations -- so the timeout path produces
 * a marked aggregate rather than silence.
 *
 * <p><b>Parts are keyed by branch, not appended.</b> I3 requires the aggregate
 * to be the union of branch responses with no duplicates, so a branch replying
 * twice overwrites its own slot instead of adding a second part.
 *
 * <p><b>Correlation ids are hyphenated short forms</b> -- gate check 5 fails
 * the build on any run of 13-19 digits under {@code qe-harness/}, and an
 * epoch-millis suffix would be exactly 13.
 *
 * <p><b>Defect injection:</b> {@code aggregate-emitted-incomplete} emits on the
 * first branch reply with no partial marker. I1 fails; correlation uniqueness
 * (I2) and union semantics (I3) are untouched.
 */
@Service
public class AggregatorService {

    private static final Set<String> BRANCHES = Set.of("a", "b", "c");

    /** An emitted aggregate. {@code partial} is true only for a timed-out set. */
    public record Aggregate(String correlationId, Map<String, String> parts, boolean partial,
                            String emittedAt) {}

    private final RabbitTemplate rabbit;
    private final MessagingTopology topology;
    private final MessageLog log;
    private final long aggregateTimeoutMs;

    private final Map<String, Map<String, String>> pending = new ConcurrentHashMap<>();
    private final Map<String, Instant> startedAt = new ConcurrentHashMap<>();
    private final Map<String, Aggregate> emitted = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();

    public AggregatorService(RabbitTemplate rabbit, MessagingTopology topology, MessageLog log,
                             @Value("${app.messaging.aggregate-timeout-ms}") long aggregateTimeoutMs) {
        this.rabbit = rabbit;
        this.topology = topology;
        this.log = log;
        this.aggregateTimeoutMs = aggregateTimeoutMs;
    }

    public long aggregateTimeoutMs() {
        return aggregateTimeoutMs;
    }

    public synchronized void reset() {
        pending.clear();
        startedAt.clear();
        emitted.clear();
        counter.set(0);
        log.clear();
    }

    /** Sprays the fanout exchange and opens a correlation window. */
    public String fanOut(String correlationId) {
        topology.declareTopology();
        String corr = correlationId != null ? correlationId : nextCorrelationId();
        pending.put(corr, new LinkedHashMap<>());
        startedAt.put(corr, Instant.now());
        log.recordPublished("fanout", corr);
        rabbit.convertAndSend(MessagingTopology.FANOUT_EXCHANGE, "", corr);
        return corr;
    }

    public synchronized void branchReply(String correlationId, String branch, String payload) {
        Map<String, String> parts = pending.get(correlationId);
        // Once an aggregate has been emitted for this correlation -- whether
        // by completeness or by the timeout arm -- a later reply must be a
        // no-op. Without this guard, a slow branch arriving after
        // awaitAggregate's timeout-triggered partial emit would see
        // containsAll(BRANCHES) turn true and re-emit with partial=false,
        // silently overwriting the already-published, correctly-marked
        // aggregate (a double-emit that flips the recorded outcome after the
        // fact). This is a DIFFERENT race than the same-instant reply-vs-
        // timeout case the method's own synchronized lock already handles --
        // this is reply-AFTER-timeout, which nothing else defends against.
        if (parts == null || emitted.containsKey(correlationId)) {
            return;
        }
        // Keyed, not appended: a branch replying twice overwrites its own slot,
        // so the union stays duplicate-free (I3).
        parts.put(branch, payload);

        if (DefectFlags.isActive("aggregate-emitted-incomplete")) {
            emit(correlationId, parts, false);
            return;
        }
        if (parts.keySet().containsAll(BRANCHES)) {
            emit(correlationId, parts, false);
        }
    }

    /** I1's timeout arm: past the window an aggregate is emitted WITH the
     *  partial marker. Bounded, never an indefinite wait. */
    public boolean awaitAggregate(String correlationId, long budgetMs) {
        Instant deadline = Instant.now().plus(Duration.ofMillis(budgetMs));
        while (Instant.now().isBefore(deadline)) {
            if (emitted.containsKey(correlationId)) {
                return true;
            }
            Instant since = startedAt.get(correlationId);
            if (since != null
                    && Duration.between(since, Instant.now()).toMillis() > aggregateTimeoutMs) {
                synchronized (this) {
                    Map<String, String> parts = pending.get(correlationId);
                    if (parts != null && !emitted.containsKey(correlationId)) {
                        emit(correlationId, parts, true);
                    }
                }
                return true;
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void emit(String correlationId, Map<String, String> parts, boolean partial) {
        Aggregate aggregate = new Aggregate(correlationId, Map.copyOf(parts), partial,
            Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS).toString());
        emitted.put(correlationId, aggregate);
        rabbit.convertAndSend(MessagingTopology.IN_EXCHANGE, "aggregate", correlationId);
    }

    public Optional<Aggregate> aggregateFor(String correlationId) {
        return Optional.ofNullable(emitted.get(correlationId));
    }

    public int branchCount() {
        return BRANCHES.size();
    }

    /** Hyphenated short form -- never a 13-digit epoch suffix. */
    private String nextCorrelationId() {
        return "corr-%04d".formatted(counter.incrementAndGet());
    }
}
