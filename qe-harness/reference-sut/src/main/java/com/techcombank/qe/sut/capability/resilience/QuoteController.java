package com.techcombank.qe.sut.capability.resilience;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * TST-035 circuit breaker capability's HTTP surface. Task 18's JMeter module
 * drives this single endpoint.
 *
 * <p>Deliberately thin: all breaker/fallback logic lives in
 * {@link DownstreamClient#fetch}, so this controller never sees a
 * downstream-failure exception to handle -- {@code fetch} always returns a
 * {@link DegradedResponse}, live or cached, at a plain {@code 200}. The one
 * exception is the {@code breaker-disabled} defect flag: that path's
 * fallback rethrows on purpose (see {@code DownstreamClient.fallback}), and
 * this controller does not catch it -- Spring's default exception handling
 * turns the resulting {@code ResponseStatusException} into the {@code 500}
 * {@code defectFlagLetsDownstreamFailureSurfaceAsFiveHundred} asserts on.
 */
@RestController
public class QuoteController {

    private final DownstreamClient downstreamClient;

    public QuoteController(DownstreamClient downstreamClient) {
        this.downstreamClient = downstreamClient;
    }

    /** GET /quotes/{id} -> 200 with live data, or 200
     *  {"degraded": true, "source": "cache"} when the breaker is open.
     *  Never a 5xx on downstream failure, unless the breaker-disabled
     *  defect flag is active. */
    @GetMapping("/quotes/{id}")
    public DegradedResponse get(@PathVariable String id) {
        return downstreamClient.fetch(id);
    }
}
