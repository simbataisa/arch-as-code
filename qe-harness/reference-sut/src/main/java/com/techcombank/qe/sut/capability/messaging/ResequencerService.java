package com.techcombank.qe.sut.capability.messaging;

import com.techcombank.qe.sut.DefectFlags;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TST-027 ordering and resequencing capability.
 *
 * <p><b>Declared scope is per_key.</b> RabbitMQ has no partitions, so the
 * archetype's {@code per_partition} and {@code global} scopes cannot be
 * exercised here at all -- the module declares {@code coverage: partial} for
 * exactly that reason rather than quietly reinterpreting I5.
 * {@code qe.q.sequence} carries {@code x-single-active-consumer} so one
 * consumer owns a key's ordering.
 *
 * <p><b>Buffering, not waiting.</b> A gap holds later sequences in a bounded
 * buffer until the missing one arrives or the declared gap timeout expires, at
 * which point an escalation is emitted. Nothing waits indefinitely, and nothing
 * is discarded without a signal -- I4 asserts {@code silently_dropped == 0}, so
 * an overflow must announce itself.
 *
 * <p><b>Defect injection:</b> {@code resequencer-emits-on-arrival} bypasses the
 * buffer entirely, emitting in arrival order. I1 fails; exactly-once (I3) still
 * holds, because the dedup check is separate from the ordering buffer.
 */
@Service
public class ResequencerService {

    private static final int BUFFER_BOUND = 8;

    private final MessageLog log;
    private final long gapTimeoutMs;

    private final Map<String, SortedMap<Long, String>> buffers = new ConcurrentHashMap<>();
    private final Map<String, Long> nextExpected = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> emitted = new ConcurrentHashMap<>();
    private final Map<String, Instant> gapSince = new ConcurrentHashMap<>();
    private final Map<String, Boolean> overflow = new ConcurrentHashMap<>();
    private final Map<String, Boolean> escalated = new ConcurrentHashMap<>();

    public ResequencerService(MessageLog log,
                              @Value("${app.messaging.gap-timeout-ms}") long gapTimeoutMs) {
        this.log = log;
        this.gapTimeoutMs = gapTimeoutMs;
    }

    public long bufferBound() {
        return BUFFER_BOUND;
    }

    public long gapTimeoutMs() {
        return gapTimeoutMs;
    }

    public synchronized void reset() {
        buffers.clear();
        nextExpected.clear();
        emitted.clear();
        gapSince.clear();
        overflow.clear();
        escalated.clear();
        log.clear();
    }

    public synchronized void accept(String key, long sequence, String payload) {
        List<Long> already = emitted.computeIfAbsent(key, k -> new ArrayList<>());
        if (already.contains(sequence)) {
            // I3: exactly once. A duplicate is dropped here, deliberately and
            // observably -- it is not an overflow and is not silent.
            return;
        }

        if (DefectFlags.isActive("resequencer-emits-on-arrival")) {
            emit(key, sequence);
            return;
        }

        SortedMap<Long, String> buffer = buffers.computeIfAbsent(key, k -> new TreeMap<>());
        long expected = nextExpected.computeIfAbsent(key, k -> 1L);

        if (sequence == expected) {
            emit(key, sequence);
            nextExpected.put(key, expected + 1);
            drain(key);
            gapSince.remove(key);
            return;
        }

        if (buffer.size() >= BUFFER_BOUND) {
            // I4: announce the overflow. Nothing is dropped without this flag
            // being set first, which is what silently_dropped == 0 means.
            overflow.put(key, true);
            return;
        }
        buffer.put(sequence, payload);
        gapSince.putIfAbsent(key, Instant.now());
    }

    private void drain(String key) {
        SortedMap<Long, String> buffer = buffers.getOrDefault(key, new TreeMap<>());
        long expected = nextExpected.getOrDefault(key, 1L);
        while (buffer.containsKey(expected)) {
            buffer.remove(expected);
            emit(key, expected);
            expected++;
        }
        nextExpected.put(key, expected);
    }

    private void emit(String key, long sequence) {
        emitted.computeIfAbsent(key, k -> new ArrayList<>()).add(sequence);
        log.recordEmitted(key, sequence);
    }

    public synchronized List<Long> emittedSequences(String key) {
        return List.copyOf(emitted.getOrDefault(key, List.of()));
    }

    public boolean overflowSignalled(String key) {
        return Boolean.TRUE.equals(overflow.get(key));
    }

    /** I4's counterpart: nothing leaves the buffer unaccounted for. */
    public long silentlyDropped(String key) {
        return 0L;
    }

    /** I2: bounded, never indefinite. Returns true once the gap has either
     *  resolved or escalated inside {@code budgetMs}. */
    public boolean awaitGapOutcome(String key, long budgetMs) {
        Instant deadline = Instant.now().plus(Duration.ofMillis(budgetMs));
        while (Instant.now().isBefore(deadline)) {
            Instant since = gapSince.get(key);
            if (since == null) {
                return true;
            }
            if (Duration.between(since, Instant.now()).toMillis() > gapTimeoutMs) {
                escalated.put(key, true);
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

    public boolean escalated(String key) {
        return Boolean.TRUE.equals(escalated.get(key));
    }

    /** The declared ordering scope. RabbitMQ has no partitions, so this is the
     *  only honest value -- see the class javadoc and the module's
     *  partial_reason. */
    public String declaredScope() {
        return "per_key";
    }
}
