# Timeout Budget

Status: Draft | Last Reviewed: 2026-05-09 | Owner: @sre-lead
Catalog ID: RES-006 | Radii
Tier Applicability: T0, T1, T2

## Problem Statement

- Without explicit per-call timeouts, a slow downstream silently exhausts upstream thread pools and connection budgets; the platform appears healthy until a downstream is briefly degraded, at which point the whole stack hangs and the incident is indistinguishable from a full outage.
- Timeouts that are independent of each other violate the deadline-propagation rule: a caller with a 3-second SLA cannot have a downstream with a 5-second timeout — the callee will never complete within the caller's budget, creating phantom "successful" calls that always arrive late.
- Banking payment call chains are long: client → API gateway → payment service → NAPAS → bank network → beneficiary bank. Each hop consumes time from a fixed user-facing SLA budget; without explicit accounting, any one hop can inadvertently consume the entire budget.
- Hung threads without timeouts waste compute on work that will be discarded: a payment response that arrives after the caller has already timed out and returned an error to the customer is not just useless, it actively harms the system by holding threads, connections, and memory for the duration of the wait.

## Solution

Assign every network call an explicit timeout that is strictly less than the caller's remaining deadline. Model the full call chain as a waterfall of decreasing timeout budgets, summing to less than the user-facing SLA.

```mermaid
sequenceDiagram
    participant C  as Client<br/>(Mobile / Browser)
    participant GW as API Gateway<br/>timeout: 3 000 ms
    participant PS as Payment Service<br/>timeout: 2 500 ms
    participant NS as NAPAS Adapter<br/>timeout: 2 000 ms
    participant NP as NAPAS Network<br/>timeout: 1 500 ms

    Note over C,NP: User-facing SLA = 3 000 ms (T0)

    C->>GW: POST /payments/initiate
    activate GW
    GW->>PS: POST /internal/payments (deadline: 2 500 ms)
    activate PS
    PS->>NS: initiateNapasTransfer (deadline: 2 000 ms)
    activate NS
    NS->>NP: ISO 8583 0200 (deadline: 1 500 ms)
    activate NP

    alt Happy path (NP responds in 800 ms)
        NP-->>NS: ISO 8583 0210 (response code 00)
        deactivate NP
        NS-->>PS: PaymentResult.SUCCESS
        deactivate NS
        PS-->>GW: HTTP 200 PaymentResponse
        deactivate PS
        GW-->>C: HTTP 200 (total: ~900 ms)
        deactivate GW
    else NAPAS slow (NP takes > 1 500 ms)
        NP--xNS: [timeout fires at 1 500 ms]
        deactivate NP
        NS-->>PS: TimeLimitExceededException
        deactivate NS
        PS-->>GW: HTTP 504 (payment pending — idempotent retry safe)
        deactivate PS
        GW-->>C: HTTP 504 (total: ~1 600 ms — still within 3 000 ms SLA)
        deactivate GW
        Note over C,NP: Fast failure preserves remaining 1 400 ms budget
    end
```

The budget must be cascaded: each service declares its own `callTimeout` smaller than the upstream-assigned deadline, with a headroom margin (typically 200–500ms) to account for network transit, serialisation, and filter-chain overhead.

## Implementation Guidelines

### 1. Resilience4j TimeLimiter with @TimeLimiter

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

```yaml
# application.yml — timeout budgets declared per downstream
resilience4j:
  timelimiter:
    configs:
      default:
        timeout-duration: 2s
        cancel-running-future: true # critical: cancel the thread, don't just return
    instances:
      napas-adapter:
        timeout-duration: 2000ms # payment service → NAPAS budget
        cancel-running-future: true
      t24-ofs-bridge:
        timeout-duration: 5000ms # T24 OFS is slower — separate budget
        cancel-running-future: true
      swift-gateway:
        timeout-duration: 8000ms # SWIFT international — wider budget
        cancel-running-future: true
      account-service:
        timeout-duration: 500ms # internal service — tight budget
        cancel-running-future: true

  circuitbreaker:
    instances:
      napas-adapter:
        slowCallDurationThreshold: 1800ms # flag calls > 1.8s as slow (before 2s timeout)
        slowCallRateThreshold: 20 # open CB if 20% of calls are slow
```

```java
@Service
@Slf4j
public class NapasAdapterService {

    private final NapasHttpClient napasClient;
    private final MeterRegistry meterRegistry;

    // TimeLimiter + CircuitBreaker compose: timeout per attempt, CB over window
    @TimeLimiter(name = "napas-adapter", fallbackMethod = "napasTimeoutFallback")
    @CircuitBreaker(name = "napas-adapter", fallbackMethod = "napasCircuitFallback")
    public CompletableFuture<PaymentResult> submitToNapas(PaymentRequest request) {
        // Must return CompletableFuture for @TimeLimiter to work
        return CompletableFuture.supplyAsync(() -> {
            log.info("Submitting to NAPAS payment_ref={} channel={}",
                     request.getPaymentRef(), request.getChannel());
            return napasClient.submit(request);
        });
    }

    // Fallback when timeout fires
    public CompletableFuture<PaymentResult> napasTimeoutFallback(
            PaymentRequest request, TimeoutException e) {
        log.warn("NAPAS timeout payment_ref={} — returning PENDING for retry",
                 request.getPaymentRef());
        meterRegistry.counter("tcb.napas.timeout.total").increment();
        // Idempotency (PRIN-006) ensures retry is safe
        return CompletableFuture.completedFuture(
            PaymentResult.pending(request.getPaymentRef(), "NAPAS timeout — retry safe"));
    }

    // Fallback when circuit breaker is OPEN
    public CompletableFuture<PaymentResult> napasCircuitFallback(
            PaymentRequest request, CallNotPermittedException e) {
        log.error("NAPAS circuit breaker OPEN payment_ref={}", request.getPaymentRef());
        return CompletableFuture.completedFuture(
            PaymentResult.failed(request.getPaymentRef(), "Payment network temporarily unavailable"));
    }
}
```

### 2. WebClient with Reactive Timeouts

For reactive (non-blocking) services, timeouts must be applied at both the connection and response level. A single `responseTimeout` is insufficient — configure both.

```java
@Configuration
public class NapasWebClientConfig {

    @Bean("napasWebClient")
    public WebClient napasWebClient() {
        // Connection pool with explicit timeouts
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1_000)     // TCP connect: 1s
            .responseTimeout(Duration.ofMillis(1_500))                // Full response: 1.5s
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(1_500, TimeUnit.MILLISECONDS))
                .addHandlerLast(new WriteTimeoutHandler(500, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
            .baseUrl(napasProperties.getBaseUrl())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader("X-Correlation-Id",
                MDC.get("traceId") != null ? MDC.get("traceId") : UUID.randomUUID().toString())
            .build();
    }
}

@Service
public class NapasReactiveAdapter {

    private final WebClient napasWebClient;

    public Mono<PaymentResult> submitPayment(PaymentRequest request) {
        return napasWebClient.post()
            .uri("/transfers")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(NapasResponse.class)
            // Reactive timeout as safety net — belt-and-suspenders with httpClient.responseTimeout
            .timeout(Duration.ofMillis(1_500),
                Mono.error(new NapasTimeoutException("NAPAS response timeout")))
            .map(this::toDomainResult)
            .doOnError(TimeoutException.class, e ->
                log.warn("NAPAS reactive timeout payment_ref={}", request.getPaymentRef()));
    }
}
```

### 3. Feign Client Timeout Configuration

```java
@FeignClient(name = "account-service",
             url = "${services.account.url}",
             configuration = AccountFeignConfig.class)
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts/{accountId}/balance")
    AccountBalance getBalance(@PathVariable String accountId);
}

@Configuration
public class AccountFeignConfig {

    @Bean
    public Request.Options accountServiceRequestOptions() {
        return new Request.Options(
            500,  TimeUnit.MILLISECONDS,   // connect timeout: 500ms
            500,  TimeUnit.MILLISECONDS,   // read timeout: 500ms — internal service, tight
            true                           // follow redirects
        );
    }

    @Bean
    public Retryer accountServiceRetryer() {
        // No retry at this level — retry handled by Resilience4j @Retry annotation
        return Retryer.NEVER_RETRY;
    }
}
```

### 4. Deadline Propagation via gRPC Context

For gRPC inter-service calls, deadline propagation is built into the protocol. Always use `withDeadline`, never relying on the default (infinite).

```java
@Service
public class PaymentGrpcClient {

    private final PaymentServiceGrpc.PaymentServiceBlockingStub blockingStub;

    public PaymentProto.PaymentResponse initiate(PaymentProto.PaymentRequest request,
                                                  long remainingBudgetMs) {
        // Propagate the remaining deadline from the calling context
        // Reserve 200ms for network overhead and serialisation
        long grpcDeadlineMs = Math.max(remainingBudgetMs - 200, 100);

        return blockingStub
            .withDeadlineAfter(grpcDeadlineMs, TimeUnit.MILLISECONDS)
            .withInterceptors(TracingClientInterceptor.create()) // propagate traceparent
            .initiatePayment(request);
    }
}
```

### 5. Timeout Budget Registry — Centralised Configuration

Declare all timeout budgets in a single YAML source of truth to make the waterfall visible and auditable.

```yaml
# config/timeout-budgets.yml — the call-chain waterfall for payment initiation
timeout_budgets:
  payment_initiation_chain:
    user_sla_ms: 3000
    hops:
      - name: api_gateway_to_payment_service
        budget_ms: 2500
        headroom_ms: 500 # network + gateway overhead
      - name: payment_service_to_napas_adapter
        budget_ms: 2000
        headroom_ms: 500 # payment service processing overhead
      - name: napas_adapter_to_napas_network
        budget_ms: 1500
        headroom_ms: 500 # NAPAS adapter overhead
    notes: >
      Each hop budget = previous budget minus headroom.
      NAPAS network must respond within 1 500 ms for the entire chain
      to stay within the 3 000 ms user SLA.

  account_enquiry_chain:
    user_sla_ms: 500
    hops:
      - name: gateway_to_account_service
        budget_ms: 400
        headroom_ms: 100
      - name: account_service_to_t24_ofs
        budget_ms: 300
        headroom_ms: 100

  t24_ofs_write_chain:
    user_sla_ms: 10000 # T24 writes — wider SLA for complex OFS operations
    hops:
      - name: payment_service_to_t24_ofs_bridge
        budget_ms: 8000
        headroom_ms: 2000
```

### 6. Monitoring Timeout Rates

```java
@Component
@Slf4j
public class TimeoutMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    @PostConstruct
    public void bindTimeLimiterMetrics() {
        // Bind Resilience4j TimeLimiter events to Micrometer
        timeLimiterRegistry.getAllTimeLimiters().forEach(tl ->
            TaggedTimeLimiterMetrics.ofTimeLimiterRegistry(timeLimiterRegistry)
                .bindTo(meterRegistry)
        );
    }

    // Alert rule: timeout rate > 1% of traffic on any T0 downstream for 5 minutes
    // Prometheus alert: rate(resilience4j_timelimiter_calls_seconds_count{kind="timeout"}[5m]) /
    //   rate(resilience4j_timelimiter_calls_seconds_count[5m]) > 0.01
}
```

## Compliance Mapping

| Ring   | Regulation                 | Provision                                     | How this pattern satisfies                                                                                                                                             |
| ------ | -------------------------- | --------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Ring 0 | NIST SP 800-53             | SC-5 Denial of Service Protection             | Explicit timeouts prevent resource exhaustion (thread/connection starvation) that constitutes a self-inflicted DoS                                                     |
| Ring 0 | NIST SP 800-53             | SC-6 Resource Availability                    | Timeout budgets ensure bounded resource hold times, maintaining availability during partial downstream degradation                                                     |
| Ring 0 | AWS WAF                    | Well-Architected Reliability — Limit retries  | Timeouts are the prerequisite for bounded retry loops; without them, retries with backoff cannot be safely configured                                                  |
| Ring 0 | ISO 27001                  | A.17.2 Redundancies                           | Timeout-driven fast failure is a key enabler of service redundancy — a timed-out call can be rerouted to a secondary                                                   |
| Ring 1 | BCBS 230                   | Principle 6 (Incident Management)             | Bounded timeouts limit the duration of a downstream degradation event, capping the impact window of an incident                                                        |
| Ring 1 | BCBS 230                   | Principle 7 — Information and Technology Risk | Cascading failures caused by missing timeouts represent an operational risk; this pattern directly mitigates that risk                                                 |
| Ring 2 | SBV Circular 09/2020 §IV.2 | Operational continuity requirements           | Timeout budgets prevent thread-pool exhaustion cascades that would violate the operational continuity obligations of §IV.2 ⚠️ (working summary — pending Legal review) |
| Ring 2 | SBV Circular 09/2020 §IV.3 | Incident response timeliness                  | Fast failure via timeouts reduces MTTR by surfacing degradation early; timeout events are logged as incident precursors ⚠️ (working summary — pending Legal review)    |

## NFR Acceptance Criteria

```yaml
service_name: timeout-budget-pattern
tier: T0
rto_minutes: 0 # timeouts are passive controls; their absence causes the outage
rpo_seconds: 0
latency:
  user_sla_ms: 3000 # T0 payment initiation end-to-end
  gateway_to_payment_ms: 2500
  payment_to_napas_ms: 2000
  napas_network_ms: 1500
  timelimiter_overhead_us: 50 # TimeLimiter evaluation overhead: < 0.1ms
failure_modes:
  - mode: Timeout fires before response received (NAPAS slow)
    impact: Caller receives TimeoutException / HTTP 504; payment status PENDING
    mitigation: Idempotency key (PRIN-006) makes retry safe; caller retries with same key
  - mode: Timeout too aggressive — fires on healthy downstream under load
    impact: False-positive failures; error rate spike; circuit breaker may open spuriously
    mitigation: Set timeout = P99 of downstream latency * 1.5 (not P50); review quarterly
  - mode: cancel-running-future=false (misconfiguration)
    impact: Timeout returns to caller but the downstream call continues, exhausting resources
    mitigation: CI test verifies cancel-running-future=true for all TimeLimiter instances
  - mode: Timeout cascades — upstream times out before downstream completes
    impact: Double payment risk if downstream processes then caller retries
    mitigation: All mutation calls use idempotency keys (PRIN-006); downstream deduplicates
blast_radius:
  scope: Limited to the specific call hop where the timeout fires; does not affect parallel calls
  isolation: Timeout per instance; one downstream timing out does not affect other downstreams
catalog_references:
  - RES-002 # Circuit Breaker (timeout and CB compose)
  - RES-003 # Retry with Backoff (timeout sets per-attempt cap)
  - PRIN-006 # Idempotency-by-default (makes timed-out mutations safe to retry)
  - INT-005 # Anti-Corruption Layer (T24 OFS timeout budget defined here)
  - NFR-002 # Latency Budget Model (timeout budgets derive from tier P95 targets)
  - PRIN-009 # Observability-First (timeout events are observability signals)
```

## Cost/FinOps

- Resilience4j TimeLimiter is MIT-licensed with zero licensing cost; the evaluation overhead per call is below 0.1ms and effectively invisible in any P95 budget.
- Hung threads without timeouts have a direct compute cost: a thread blocked for 30 seconds consumes approximately 512 KB of stack memory for that duration. At 100 concurrent hung threads, this is 50 MB of wasted heap plus the CPU cost of thread context-switching. For a production-scale incident (1,000 hung threads over 10 minutes), the compute waste is measurable and the incident recovery cost (pod restart, manual intervention) far exceeds the engineering cost of adding timeouts.
- Correct timeout configuration reduces T24 and NAPAS connection pool sizes needed: with a 2-second timeout, a connection pool of 50 handles 25 RPS against a 2-second-latency downstream. Without timeouts, under degraded conditions 25 RPS would saturate hundreds of threads. Right-sizing connection pools based on `timeout × RPS` reduces infrastructure costs.
- Timeout events generate telemetry (TimeLimiter metrics, log entries) at approximately 1 KB per event. At 1% timeout rate on 500 RPS, this is 5 events/second — negligible log volume. Telemetry is the early-warning signal that avoids a full-scale incident, making the storage cost well-justified.
- `cancel-running-future: true` is not free — it sends a thread interrupt and incurs a context switch. However, the alternative (a completed background thread that is never collected) leaks resources. Always pay the cancel cost; it is orders of magnitude cheaper than a thread leak.

## Threat Model

- **Denial of Service — slow-downstream-induced thread exhaustion**: A downstream service deliberately slows its responses to exhaust upstream thread pools (slow-read attack). Mitigation: timeouts cap the maximum time any thread is held; Thread pool saturation alerts fire before total exhaustion; combined with Circuit Breaker (RES-002), the slow downstream is isolated after the threshold is crossed.
- **Denial of Service — timeout-too-short attack**: An attacker sends requests that are individually legitimate but collectively cause the service to time out on its own downstream, generating a cascade of retries that amplifies load. Mitigation: Circuit Breaker opens when error rate (including timeouts) exceeds threshold, cutting off the amplification loop; retry with exponential backoff (RES-003) prevents retry storms.
- **Tampering — timeout bypass via direct downstream call**: A misconfigured or malicious service bypasses the TimeLimiter by calling the downstream client directly. Mitigation: downstream clients (WebClient, FeignClient) have connection and response timeouts set at the HTTP client level as a defense-in-depth measure; even without TimeLimiter, the HTTP client enforces a hard cap.
- **Repudiation — double payment due to timeout + retry**: A payment is processed by NAPAS but the timeout fires before the response is received. The caller retries with the same request but without an idempotency key, creating a duplicate payment. Mitigation: all payment mutations require an idempotency key (PRIN-006); NAPAS deduplicates on the key; the ACL (INT-005) passes the `paymentRef` as the deduplication key on every OFS call.
- **Information Disclosure — timeout error messages expose topology**: A `TimeoutException` message that includes the downstream URL or internal service name exposes system topology to clients. Mitigation: timeout fallback methods return generic user-facing messages (`"Payment network temporarily unavailable"`); the detailed timeout context is logged internally only.
- **Elevation of Privilege — long timeout window for session hijacking**: A timeout of 30 seconds on an authentication call gives an attacker a longer window to replay tokens. Mitigation: authentication call timeouts are set to 1 second (tight — the IDP is internal and HA); token validation uses locally-cached JWKS (zero network call in the hot path).

## Operational Runbook

1. **Timeout rate spike alert** (`tcb.timelimiter.timeout_rate > 0.01`): Identify the affected `TimeLimiter` instance name from the metric label. Check the downstream service's latency histogram in Grafana. If downstream P99 latency has increased beyond the configured timeout, the timeout is firing correctly — investigate the downstream. If downstream latency is normal, the timeout may be misconfigured — review the `timeout-duration` setting against current P99.

2. **Adjust timeout budget**: Open the `timeout-budgets.yml` and the `application.yml` for the affected service. Recalculate the waterfall to ensure all hops still sum to less than the user SLA. Submit a change record. Update both the Resilience4j config and the budget registry YAML atomically (same PR). Deploy to staging and verify with a load test that the new timeout does not trigger spuriously under normal load.

3. **Payment stuck in PENDING after timeout**: The payment timed out before NAPAS confirmed. Query NAPAS using the `paymentRef` as the retrieval reference number (ISO 8583 field 37). If NAPAS has a `00` response code, the payment succeeded — update the payment record status to SUCCESS. If NAPAS has no record, the payment did not complete — mark as FAILED and the customer can retry.

4. **CircuitBreaker opened by timeout cascade**: A timeout storm on the NAPAS adapter triggers the circuit breaker. Follow the RES-002 Circuit Breaker runbook for the NAPAS adapter instance. Do not reduce the timeout budget during an incident — this risks data integrity. Instead, wait for the circuit breaker to probe (HALF_OPEN) and auto-recover when NAPAS stabilises.

5. **cancel-running-future alert**: If metrics show `timelimiter_calls{kind="timeout"}` increasing but thread pool is not recovering, check whether `cancel-running-future` is set to `true` for all instances. A `false` setting means threads are not interrupted on timeout. Deploy the corrected configuration with `cancel-running-future: true` and roll the pods.

6. **T24 OFS timeout — EOD window**: T24 OFS calls during the end-of-day processing window (22:30–00:30 ICT) may legitimately take longer than normal. The `t24-ofs-bridge` timeout is set to 8 seconds (wider than NAPAS) to accommodate this. If T24 still times out during EOD, check whether T24 has started EOD processing early. Do not increase the timeout beyond 8 seconds without a full latency budget recalculation.

7. **Quarterly timeout budget review**: Pull the P99 latency histograms for all downstream calls from the last 30 days. Compare against the configured timeout budgets. Timeouts should fire at most 0.1% of the time under normal load; if they fire more frequently, the budget is too tight. Update the budget registry and service configs. Document changes in `governance/decisions/TIMEOUT-REVIEW-{year}-Q{quarter}.md`.

## Test Strategy

### Unit Tests

Test the fallback methods directly: verify that `napasTimeoutFallback` returns a `PENDING` `PaymentResult` and increments the `tcb.napas.timeout.total` counter. Verify that `napasCircuitFallback` returns a `FAILED` result. Test the Feign client options bean to assert the configured timeout values match the budget registry.

```java
@ExtendWith(MockitoExtension.class)
class NapasAdapterTimeoutTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Test
    void timeoutFallback_returnsPendingStatus() {
        NapasAdapterService service = new NapasAdapterService(mockClient, registry);
        CompletableFuture<PaymentResult> result = service.napasTimeoutFallback(
            validRequest(), new TimeoutException("NAPAS timeout"));

        assertThat(result.join().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(registry.counter("tcb.napas.timeout.total").count()).isEqualTo(1.0);
    }
}
```

### Integration Tests

Use WireMock with a fixed delay response (`withFixedDelay(2100)`) to simulate a slow NAPAS. Assert that the `@TimeLimiter` fires within 2000ms and the fallback method is invoked. Use a Testcontainer with Resilience4j configured to verify that a sustained timeout rate above threshold opens the circuit breaker.

### Compliance Tests

Verify the timeout budget registry YAML is consistent with the Resilience4j application configuration: a CI test parses both files and asserts that for each hop in the budget registry, the corresponding Resilience4j TimeLimiter instance exists with a `timeout-duration` that matches. Any drift between the documentation and the implementation fails the build.

### Chaos Tests

Using [BP-005 Chaos Engineering](../../best-practices/chaos-engineering.md), inject 100ms, 500ms, 1500ms, and 2500ms latency into the NAPAS WireMock during a load test. Verify:

- At 100ms: no timeouts, normal operation.
- At 1500ms: timeouts begin firing; circuit breaker stays CLOSED (rate below threshold).
- At 2500ms: timeout rate exceeds CB threshold; CB opens; fallback serves all requests.
- After latency removed: CB enters HALF_OPEN; probes succeed; CB closes; normal operation resumes.
- At no point does upstream service P95 latency exceed the user SLA.

## References

- [RES-002 Circuit Breaker](circuit-breaker.md)
- [RES-003 Retry with Backoff](retry-with-backoff.md)
- [PRIN-006 Idempotency-by-default](../../principles/idempotency-by-default.md)
- [PRIN-009 Observability-First](../../principles/observability-first.md)
- [INT-005 Anti-Corruption Layer](../integration/anti-corruption-layer.md)
- [NFR-002 Latency Budget Model](../../nfr/latency-budget-model.md)
- [BP-005 Chaos Engineering](../../best-practices/chaos-engineering.md)
- [BP-007 Golden Signals (SRE)](../../best-practices/golden-signals-sre.md)
- [Resilience4j TimeLimiter Documentation](https://resilience4j.readme.io/docs/timeout)
- [gRPC Deadline Propagation](https://grpc.io/docs/guides/deadlines/)

---

**Key Takeaway**: Every network call in Techcombank's payment stack must declare an explicit timeout smaller than its caller's remaining deadline — the timeout waterfall is the primary defence against slow-downstream-induced cascading failure, and it is auditable as a first-class architecture artifact.
