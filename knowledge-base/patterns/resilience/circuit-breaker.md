# Circuit Breaker Pattern

Status: Approved | Last Reviewed: 2026-03-02 | Owner: @ea-board

## Problem Statement

Cascading failures occur when a service calls a downstream service that's slow or down:
- Thread pool exhaustion: requests wait for timeout, threads starve
- Resource depletion: connections, memory pile up
- Cascading outage: failures propagate upstream
- Slow requests degrade user experience

```
Service A
  └─ calls Service B (slow)
      └─ Service B calls Service C (down)
          └─ Requests timeout, threads blocked
              └─ Thread pool exhausted
                  └─ Service A becomes unresponsive too
```

## Solution

Implement a circuit breaker. Monitor call failures; fail fast when service is struggling.

```
CLOSED (Normal):
  Requests → Service B ✓
  Failure count: 0

OPEN (Service down):
  X Requests blocked immediately (fail fast)
  Failure count: 10+ or error rate >50%

HALF_OPEN (Testing):
  1-2 requests → Service B
  If success: CLOSED
  If fail: OPEN
```

## State Diagram

```
        ┌────────────────────────┐
        │ CLOSED (Normal)        │
        │ ✓ Requests pass through│
        │ ✓ Fast response        │
        └──────────┬─────────────┘
                   │
          Request fails X times or
          Error rate >50% for N seconds
                   │
                   ↓
        ┌────────────────────────┐
        │ OPEN (Failing)         │
        │ ✗ Requests fail fast   │
        │ ✗ No calls to service  │
        └──────────┬─────────────┘
                   │
          Timeout (reset timeout)
          usually 30-60 seconds
                   │
                   ↓
        ┌────────────────────────┐
        │ HALF_OPEN (Testing)    │
        │ ▲ Allow 1-2 requests   │
        │ ▲ Test if service OK   │
        └──────────┬──────────┬───┘
                   │          │
                Success      Fail
                   │          │
                   ↓          ↓
              CLOSED        OPEN
```

## Implementation Guidelines

1. **Resilience4j Configuration** (Java)
   ```java
   @Configuration
   public class CircuitBreakerConfig {

     @Bean
     public CircuitBreaker orderServiceCircuitBreaker() {
       CircuitBreakerConfig config = CircuitBreakerConfig.custom()
         .failureRateThreshold(50)           // Open if error rate > 50%
         .slowCallRateThreshold(50)          // Count slow calls as failures
         .slowCallDurationThreshold(         // 2 second timeout = slow
           Duration.ofSeconds(2))
         .waitDurationInOpenState(           // Stay open for 30 seconds
           Duration.ofSeconds(30))
         .permittedNumberOfCallsInHalfOpenState(3)  // Allow 3 calls in half-open
         .slidingWindowType(SlidingWindowType.COUNT_BASED)
         .slidingWindowSize(10)              // Evaluate last 10 calls
         .build();

       return CircuitBreaker.of("order-service", config);
     }

     @Bean
     public CircuitBreakerRegistry circuitBreakerRegistry() {
       return CircuitBreakerRegistry.of(
         CircuitBreakerConfig.ofDefaults()
       );
     }
   }
   ```

2. **Annotated Service Call**
   ```java
   @Service
   public class OrderService {

     @Autowired
     private PaymentServiceClient paymentClient;

     @CircuitBreaker(name = "payment-service", fallbackMethod = "paymentFallback")
     @Retry(name = "payment-service")
     @Timeout(name = "payment-service")
     public PaymentResult processPayment(PaymentRequest request) {
       log.info("Calling payment service");
       return paymentClient.processPayment(request);
     }

     // Fallback method: called when circuit is open
     public PaymentResult paymentFallback(
         PaymentRequest request,
         CallNotPermittedException e) {
       log.error("Payment service circuit open, using fallback");
       // Option 1: Reject order
       throw new PaymentUnavailableException("Payment service unavailable");

       // Option 2: Use cached/stale data
       // return cache.getLastPaymentResult();

       // Option 3: Queue for async retry
       // asyncRetryQueue.add(request);
       // return PaymentResult.PENDING;
     }

     // Alternative: Fallback on any exception
     public PaymentResult paymentFallback(
         PaymentRequest request,
         Exception e) {
       if (e instanceof CallNotPermittedException) {
         log.error("Circuit breaker open");
         return PaymentResult.CIRCUIT_OPEN;
       }
       return PaymentResult.ERROR;
     }
   }
   ```

3. **Manual CircuitBreaker Usage**
   ```java
   @Service
   public class InventoryService {

     @Autowired
     private CircuitBreaker inventoryCircuitBreaker;

     public InventoryCheckResult checkInventory(String productId) {
       return inventoryCircuitBreaker.executeSupplier(() -> {
         // This is called when circuit is CLOSED
         log.info("Checking inventory for: {}", productId);
         return inventoryClient.check(productId);
       });
     }
   }
   ```

4. **Composite Resilience Pattern** (Circuit Breaker + Retry + Timeout)
   ```java
   @Service
   public class ResilientOrderService {

     @Autowired
     private OrderServiceClient orderClient;

     // Combined: timeout (2s) → retry (3 attempts) → circuit breaker
     @CircuitBreaker(name = "order-service", fallbackMethod = "orderFallback")
     @Retry(
       name = "order-service",
       maxAttempts = 3,
       delay = 1000,
       multiplier = 2.0  // exponential backoff
     )
     @Timeout(name = "order-service", duration = 2000)  // 2 second timeout
     public Order createOrder(CreateOrderRequest request) {
       return orderClient.create(request);
     }

     public Order orderFallback(CreateOrderRequest request, Exception e) {
       log.warn("Order service failed: {}", e.getMessage());
       // Return degraded response
       return new Order()
         .status(OrderStatus.PENDING_RETRY)
         .reason("Service temporarily unavailable");
     }
   }
   ```

5. **Monitoring Circuit Breaker State**
   ```java
   @Component
   public class CircuitBreakerMetrics {

     @Autowired
     private CircuitBreakerRegistry registry;

     @Scheduled(fixedRate = 10000)  // Every 10 seconds
     public void logCircuitBreakerStatus() {
       registry.getAllCircuitBreakers().forEach(cb -> {
         CircuitBreaker.State state = cb.getState();
         CircuitBreakerMetrics metrics = cb.getMetrics();

         log.info("CircuitBreaker: {} | State: {} | " +
           "Calls: {} | Failures: {} | Slow: {} | Error Rate: {}%",
           cb.getName(),
           state,
           metrics.getNumberOfBufferedCalls(),
           metrics.getNumberOfFailedCalls(),
           metrics.getNumberOfSlowCalls(),
           Math.round(metrics.getFailureRate() * 100)
         );
       });
     }

     @GetMapping("/health/circuit-breakers")
     public ResponseEntity<List<CircuitBreakerStatus>> getCircuitBreakerStatus() {
       List<CircuitBreakerStatus> statuses = registry.getAllCircuitBreakers()
         .stream()
         .map(cb -> new CircuitBreakerStatus(
           cb.getName(),
           cb.getState().toString(),
           cb.getMetrics().getFailureRate(),
           cb.getMetrics().getSlowCallRate()
         ))
         .collect(Collectors.toList());

       return ResponseEntity.ok(statuses);
     }
   }
   ```

6. **Configuration Best Practices**
   ```yaml
   # application.yml
   resilience4j:
     circuitbreaker:
       configs:
         default:
           failureRateThreshold: 50
           slowCallRateThreshold: 100
           slowCallDurationThreshold: 2000ms
           waitDurationInOpenState: 30s
           permittedNumberOfCallsInHalfOpenState: 3
           automaticTransitionFromOpenToHalfOpenEnabled: true
           slidingWindowType: COUNT_BASED
           slidingWindowSize: 100

       instances:
         payment-service:
           baseConfig: default
           failureRateThreshold: 60  # More lenient
           waitDurationInOpenState: 60s

         inventory-service:
           baseConfig: default
           failureRateThreshold: 40  # Stricter
           waitDurationInOpenState: 15s

     retry:
       configs:
         default:
           maxAttempts: 3
           waitDuration: 1000
           retryExceptions:
             - java.io.IOException
             - java.net.ConnectException
           ignoreExceptions:
             - java.lang.IllegalArgumentException

     timeout:
       configs:
         default:
           timeoutDuration: 2s
           cancelRunningFuture: true

   management:
     endpoints:
       web:
         exposure:
           include: health,metrics
     health:
       circuitbreakers:
         enabled: true
   ```

## Fallback Strategies

| Strategy | Use Case |
|----------|----------|
| **Fail Fast** | User gets error immediately |
| **Cached Data** | Show stale data (dashboard, catalog) |
| **Queue for Retry** | Async retry later (background jobs) |
| **Default Value** | Return sensible default |
| **Graceful Degradation** | Limited functionality |

## When to Use

- Microservice calls across services
- External API calls (payment gateway, SMS provider)
- Database queries that might be slow
- Cache misses (call database)
- Any call that might fail or be slow

## When NOT to Use

- Local method calls (no network involved)
- Critical paths that require real-time data
- Calls where failure must be immediate

## Metrics to Monitor

```
Circuit Breaker Metrics:
  - State: CLOSED, OPEN, HALF_OPEN
  - Failure rate: % calls that failed
  - Slow call rate: % calls exceeding duration threshold
  - Buffered calls: calls in current window
  - Last call outcome: success/failure

Alerts:
  - Circuit opened (notify ops)
  - State transitions (log for debugging)
  - High error rate (>50%)
  - Frequent circuit open/close (flapping)
```

## References

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Martin Fowler on Circuit Breaker](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Release It! (Book)](https://pragprog.com/titles/mnee2/release-it-second-edition/)

---

**Key Takeaway**: Monitor failures; fail fast when service is struggling. Circuit breaker prevents cascading failures by stopping requests before they timeout.
