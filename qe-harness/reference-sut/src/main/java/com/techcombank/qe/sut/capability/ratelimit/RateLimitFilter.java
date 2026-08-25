package com.techcombank.qe.sut.capability.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

/**
 * TST-031 rate limiter enforcement point.
 *
 * <p>Guards the {@code /rate-limited/**} surface only -- every other request
 * path passes straight through {@link #doFilterInternal}, so this filter
 * never throttles the capability endpoints Tasks 6-13 already ship. Backed
 * by a single process-wide {@link TokenBucket} sized from
 * {@code app.ratelimit.permits-per-second} (capacity equals the per-second
 * rate, refilling over a one-second window -- see {@link TokenBucket}).
 *
 * <p>Under the configured rate: passes the request through to
 * {@link RateLimitedController}. Above it: responds {@code 429 Too Many
 * Requests} with a {@code Retry-After} header set to the whole seconds until
 * the bucket's next token, and never lets an overload condition surface as a
 * {@code 5xx} -- {@code neverReturnsServerErrorUnderOverload} in
 * {@code TokenBucketTest} pins this.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String GUARDED_PATH_PREFIX = "/rate-limited";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private final TokenBucket bucket;

    public RateLimitFilter(@Value("${app.ratelimit.permits-per-second:10}") long permitsPerSecond) {
        this.bucket = new TokenBucket(permitsPerSecond, Duration.ofSeconds(1), Clock.systemUTC());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(GUARDED_PATH_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        if (bucket.tryAcquire()) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(RETRY_AFTER_HEADER, String.valueOf(bucket.secondsUntilNextToken()));
        }
    }

    /** Resets the shared bucket to full capacity. Test-support only --
     *  {@link RateLimitResetController} is the sole caller; see that class's
     *  own javadoc for why the harness needs this. */
    public void resetForTest() {
        bucket.reset();
    }
}
