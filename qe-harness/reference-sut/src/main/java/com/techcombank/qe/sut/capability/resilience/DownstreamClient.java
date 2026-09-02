package com.techcombank.qe.sut.capability.resilience;

import com.techcombank.qe.sut.DefectFlags;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * TST-035 circuit breaker capability: the one place that actually talks to
 * the (real, network-reachable in compose; blackholed/restored-in-tests)
 * downstream quote service behind {@code app.downstream.base-url}.
 *
 * <p>{@link #fetch} is wrapped by Resilience4j's {@code @CircuitBreaker}
 * (breaker instance {@code downstream}, thresholds declared in
 * {@code application.yml} -- see that file's TST-035 comment block).
 * Any exception out of the real HTTP call -- refused connection, timeout,
 * or {@link io.github.resilience4j.circuitbreaker.CallNotPermittedException}
 * once the breaker is OPEN -- routes to {@link #fallback}, never back to
 * {@link com.techcombank.qe.sut.capability.resilience.QuoteController} as a
 * raw exception. That is what keeps {@code GET /quotes/{id}} at a declared
 * {@code 200} instead of a {@code 5xx} on downstream failure.
 */
@Component
public class DownstreamClient {

    private final RestClient restClient;

    public DownstreamClient(
            @Value("${app.downstream.base-url}") String baseUrl,
            @Value("${app.downstream.connect-timeout-ms:1000}") int connectTimeoutMs,
            @Value("${app.downstream.read-timeout-ms:1000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        // No custom status handler: RestClient's default already throws
        // HttpClientErrorException/HttpServerErrorException on any non-2xx
        // response, which is exactly "a downstream failure" for this
        // capability's purposes -- the circuit breaker records it and routes
        // to fallback the same as a connection refusal or timeout.
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build();
    }

    @CircuitBreaker(name = "downstream", fallbackMethod = "fallback")
    public DegradedResponse fetch(String id) {
        QuoteDto dto = restClient.get()
            .uri("/downstream/quotes/{id}", id)
            .retrieve()
            .body(QuoteDto.class);
        double price = dto == null ? 0.0 : dto.price();
        return DegradedResponse.live(id, price);
    }

    /**
     * Resilience4j invokes this reflectively -- same parameter list as
     * {@link #fetch} plus a trailing {@link Throwable} -- whenever the
     * annotated call fails (a real exception) or is refused outright
     * ({@code CallNotPermittedException} while the breaker is OPEN).
     *
     * <p>The {@code breaker-disabled} defect flag is this capability's
     * proof that the capability can fail for the right reason: with it
     * active, the fallback stops masking the failure and instead rethrows,
     * so the exception propagates out of the circuit breaker's AOP proxy
     * and {@link QuoteController} surfaces it as a genuine {@code 500}.
     */
    @SuppressWarnings("unused")
    private DegradedResponse fallback(String id, Throwable ex) {
        if (DefectFlags.isActive("breaker-disabled")) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "downstream unavailable for quote " + id, ex);
        }
        return DegradedResponse.cached(id);
    }

    private record QuoteDto(String id, double price) {}
}
