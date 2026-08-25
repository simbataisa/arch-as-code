package com.techcombank.qe.sut.capability.ratelimit;

import com.techcombank.qe.sut.DefectFlags;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * TST-031 token-bucket rate limiter core.
 *
 * <p>Refills lazily from an injected {@link Clock} rather than a background
 * thread: every {@link #tryAcquire()} (and {@link #secondsUntilNextToken()})
 * call first computes how many whole tokens should have accrued since the
 * last refill, based on elapsed wall time read from the clock, before
 * checking capacity. Injecting the clock (rather than reading
 * {@code Instant.now()} directly) is what makes {@code TokenBucketTest}'s
 * rate-limit assertions deterministic: a test can pass a {@link Clock#fixed}
 * instance so no time ever elapses between calls, instead of racing a real
 * clock against however fast the test loop executes.
 *
 * <p><b>Thread safety:</b> {@link #tryAcquire()} and
 * {@link #secondsUntilNextToken()} are both {@code synchronized} on the
 * bucket instance -- refill-then-check-then-decrement is a single atomic
 * step, so concurrent callers (e.g. {@code RateLimitFilter} under concurrent
 * HTTP requests) can never both observe and consume the same last token.
 *
 * <p><b>Defect injection:</b> when
 * {@code DefectFlags.isActive("ratelimit-leaky")} is true, {@link #tryAcquire()}
 * skips the capacity check entirely and always admits -- this is what proves
 * the capability can fail for the right reason (an uncapped admit rate) once
 * a defect-pair harness run (Tasks 16-23) exercises it. See
 * {@link com.techcombank.qe.sut.DefectFlags}.
 */
public final class TokenBucket {

    private final long capacity;
    private final long refillNanosPerToken;
    private final Clock clock;

    private double tokens;
    private long lastRefillNanos;

    public TokenBucket(long capacity, Duration window, Clock clock) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.capacity = capacity;
        this.refillNanosPerToken = window.toNanos() / capacity;
        this.clock = clock;
        this.tokens = capacity;
        this.lastRefillNanos = nowNanos();
    }

    /** Attempts to admit one request. Synchronized so concurrent callers
     *  never both observe and consume the same last token. */
    public synchronized boolean tryAcquire() {
        if (DefectFlags.isActive("ratelimit-leaky")) {
            return true;
        }
        refill();
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }

    /** Whole seconds until enough refill accumulates for one more token,
     *  rounded up -- used by {@code RateLimitFilter} to set the
     *  {@code Retry-After} header on a rejected request. Returns 0 if a
     *  token is already available. */
    public synchronized long secondsUntilNextToken() {
        refill();
        if (tokens >= 1) {
            return 0;
        }
        double deficitTokens = 1 - tokens;
        long deficitNanos = (long) Math.ceil(deficitTokens * refillNanosPerToken);
        long deficitSeconds = (deficitNanos + 999_999_999L) / 1_000_000_000L;
        return Math.max(deficitSeconds, 1);
    }

    private void refill() {
        long now = nowNanos();
        long elapsedNanos = now - lastRefillNanos;
        if (elapsedNanos <= 0) {
            return;
        }
        double accrued = (double) elapsedNanos / refillNanosPerToken;
        if (accrued > 0) {
            tokens = Math.min(capacity, tokens + accrued);
            lastRefillNanos = now;
        }
    }

    private long nowNanos() {
        Instant instant = clock.instant();
        return instant.getEpochSecond() * 1_000_000_000L + instant.getNano();
    }
}
