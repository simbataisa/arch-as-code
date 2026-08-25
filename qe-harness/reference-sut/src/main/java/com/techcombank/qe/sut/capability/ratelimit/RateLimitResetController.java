package com.techcombank.qe.sut.capability.ratelimit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Meta/test-control endpoint for resetting TST-031's rate limiter to a known
 * state (Task 17 follow-up fix).
 *
 * <p>{@code _test}-prefixed, matching {@code DefectController}'s own
 * convention -- this exists only because this SUT's purpose is to be
 * deliberately test-controllable, not because a real production service
 * would expose it. Unlike {@code DefectFlags} (a simple on/off toggle a test
 * can re-set at will), {@link TokenBucket}'s own state ({@code tokens},
 * {@code lastRefillNanos}) accrues continuously from whatever a *previous*
 * run -- or a previous idle period -- happened to leave it at, and the
 * harness modules (Tasks 16-23) run as separate tool processes against an
 * already-running {@code docker compose up} container, so they can only
 * reach that in-process state over HTTP, never directly.
 *
 * <p>This is exactly TST-021's own "reset the ledger before every run"
 * problem (see that module's plan.jmx, which {@code TRUNCATE}s
 * {@code ledger_entry}/{@code account} in its setUp Thread Group), but the
 * token bucket has no persistence layer for a harness module to reset
 * through directly -- an HTTP endpoint calling back into the running
 * process is the only way in. Confirmed empirically: running TST-031's
 * three-step ramp back-to-back against a container that had already been
 * exercised by a prior run reported a false {@code I1} failure on an
 * otherwise-clean run, because the bucket's state (and therefore how much
 * burst was available to spend during the warm-up window) depended on
 * whatever the previous run left behind.
 */
@RestController
@RequestMapping("/_test/reset")
public class RateLimitResetController {

    private final RateLimitFilter rateLimitFilter;

    public RateLimitResetController(RateLimitFilter rateLimitFilter) {
        this.rateLimitFilter = rateLimitFilter;
    }

    /** POST /_test/reset/ratelimit -> 204, resets the shared TokenBucket to full capacity. */
    @PostMapping("/ratelimit")
    public ResponseEntity<Void> resetRateLimit() {
        rateLimitFilter.resetForTest();
        return ResponseEntity.noContent().build();
    }
}
