package com.techcombank.qe.sut.capability.ratelimit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TST-031 token-bucket rate limiter capability's HTTP surface. Task 17's
 * JMeter module drives this single endpoint; all rate limiting happens
 * upstream in {@link RateLimitFilter}, so this controller body is trivial.
 */
@RestController
public class RateLimitedController {

    /** GET /rate-limited/ping -> 200 "pong" under the configured rate,
     *  or 429 (set by {@link RateLimitFilter}) above it. */
    @GetMapping("/rate-limited/ping")
    public String ping() {
        return "pong";
    }
}
