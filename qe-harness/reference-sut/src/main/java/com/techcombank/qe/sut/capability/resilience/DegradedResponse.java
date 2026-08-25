package com.techcombank.qe.sut.capability.resilience;

/**
 * TST-035 circuit breaker capability's single response shape for
 * {@code GET /quotes/{id}} -- both the live-data path ({@link #live}) and
 * the breaker-open/fallback path ({@link #cached}) return this same record,
 * so {@link DownstreamClient#fetch} and its Resilience4j fallback method
 * satisfy the framework's same-return-type fallback-matching requirement
 * without a separate DTO per path.
 *
 * <p>{@code degraded} is the field Task 18's JMeter module (and
 * {@code BreakerBehaviourTest}) actually assert on: {@code false} for live
 * data, {@code true} whenever the fallback served a cached/synthetic quote
 * instead of a real downstream round trip. {@code price} is {@code null} on
 * the degraded path -- there is no live number to report.
 */
public record DegradedResponse(String id, Double price, boolean degraded, String source) {

    public static DegradedResponse live(String id, double price) {
        return new DegradedResponse(id, price, false, "live");
    }

    public static DegradedResponse cached(String id) {
        return new DegradedResponse(id, null, true, "cache");
    }
}
