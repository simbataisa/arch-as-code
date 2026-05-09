# Retry with Exponential Backoff Pattern

Status: Approved | Last Reviewed: 2026-03-04 | Owner: @ea-board
Catalog ID: RES-003 | Radii
Tier Applicability: T0, T1, T2

## Problem Statement

Transient failures (network hiccup, temporary service unavailability) often resolve quickly:
- Without retry: User sees error, but service might recover in 100ms
- Naive retry: All clients retry simultaneously, overwhelming recovering service
- No backoff: Retry storm hammers the service back into failure
- Missing idempotency: Retry might apply operation twice

## Solution

Retry with exponential backoff and jitter. Initial failure waits 100ms, next 200ms, then 400ms, etc. Jitter randomizes to prevent thundering herd.

```
Request to Service A:
  Attempt 1: Fail (0ms elapsed) → Wait 100ms
  Attempt 2: Fail (100ms elapsed) → Wait 200ms
  Attempt 3: Fail (300ms elapsed) → Wait 400ms
  Attempt 4: Success (700ms elapsed) ✓

Without Backoff (Thundering Herd):
  10,000 clients all retry after 1 second
  Service receives 10,000 × 1 = 10,000 req/s spike → Collapses again

With Exponential Backoff + Jitter:
  Clients retry at: 100ms, 200ms, 350ms, 810ms, 1500ms, ...
  Service receives: 100-200 req/s → Can handle → Recovers
```

## Implementation Guidelines

1. **Resilience4j Retry Configuration**
   ```java
   @Configuration
   public class RetryConfig {

     @Bean
     public RetryRegistry retryRegistry() {
       RetryConfig config = RetryConfig.custom()
         .maxAttempts(3)                          // Try 3 times total
         .waitDuration(Duration.ofSeconds(1))     // Initial wait: 1 second
         .intervalFunction(                        // Exponential backoff: 2x
           IntervalFunction.ofExponentialBackoff(
             1000,      // Initial interval: 1 second
             2          // Multiplier: 2x each retry
           )
         )
         .ignoreExceptions(                       // Don't retry on these
           IllegalArgumentException.class,
           NullPointerException.class
         )
         .retryExceptions(                        // Retry on these
           IOException.class,
           ConnectException.class,
           TimeoutException.class
         )
         .build();

       return RetryRegistry.of(config);
     }

     // Payment service: strict retry (fail fast)
     @Bean
     public Retry paymentRetry() {
       return Retry.of("payment-service",
         RetryConfig.custom()
           .maxAttempts(2)               // Only 2 attempts
           .waitDuration(Duration.ofMillis(500))
           .build()
       );
     }

     // Inventory service: lenient retry (give it time)
     @Bean
     public Retry inventoryRetry() {
       return Retry.of("inventory-service",
         RetryConfig.custom()
           .maxAttempts(5)               // Up to 5 attempts
           .waitDuration(Duration.ofSeconds(1))
           .build()
       );
     }
   }
   ```

2. **Exponential Backoff with Jitter** (Recommended)
   ```java
   @Configuration
   public class AdvancedRetryConfig {

     @Bean
     public Retry exponentialBackoffWithJitter() {
       // Exponential backoff with jitter (AWS recommended)
       IntervalFunction intervalFunction =
         IntervalFunction.ofExponentialRandomBackoff(
           1000,    // Initial interval: 1 second
           2.0,     // Multiplier: 2x
           0.5      // Jitter: ±50%
         );

       return Retry.of("api-call",
         RetryConfig.custom()
           .maxAttempts(5)
           .intervalFunction(intervalFunction)
           .failAfterMaxAttempts(false)   // Throw exception after max attempts
           .build()
       );
     }
   }
   ```

3. **Annotated Service Method**
   ```java
   @Service
   public class OrderService {

     @Retry(
       name = "order-service",
       fallbackMethod = "createOrderFallback"
     )
     public Order createOrder(CreateOrderRequest request) {
       // Automatically retried with exponential backoff
       return orderClient.create(request);
     }

     public Order createOrderFallback(
         CreateOrderRequest request,
         Exception e) {
       log.error("Order creation failed after retries: {}", e.getMessage());
       // Return degraded response or queue for async processing
       return new Order().status(OrderStatus.PENDING_RETRY);
     }
   }
   ```

4. **Manual Retry with Backoff**
   ```java
   @Service
   public class RobustPaymentService {

     private static final int MAX_RETRIES = 3;
     private static final long INITIAL_BACKOFF_MS = 100;

     public PaymentResult processPayment(PaymentRequest request) {
       long backoff = INITIAL_BACKOFF_MS;

       for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
         try {
           log.info("Payment attempt {}/{}", attempt, MAX_RETRIES);
           return paymentGateway.charge(request);

         } catch (TransientException e) {
           if (attempt == MAX_RETRIES) {
             log.error("Payment failed after {} attempts", MAX_RETRIES);
             throw new PaymentFailedException("Max retries exceeded", e);
           }

           long jitter = new Random().nextLong(backoff);  // Random jitter
           long actualBackoff = backoff + jitter;
           log.warn("Payment failed (attempt {}), retrying after {}ms",
             attempt, actualBackoff);

           try {
             Thread.sleep(actualBackoff);
           } catch (InterruptedException ie) {
             Thread.currentThread().interrupt();
             throw new PaymentFailedException("Interrupted during retry", ie);
           }

           backoff *= 2;  // Double for next attempt
         }
       }

       return null;
     }
   }
   ```

5. **Idempotency for Safe Retries** (Critical!)
   ```java
   @Service
   public class IdempotentOrderService {

     @Autowired
     private OrderRepository orderRepository;

     // Idempotency key: UUID generated by client, same for all retries
     public Order createOrder(CreateOrderRequest request, String idempotencyKey) {
       // Check if already processed
       Optional<Order> existing = orderRepository.findByIdempotencyKey(idempotencyKey);
       if (existing.isPresent()) {
         log.info("Order already created with idempotency key: {}", idempotencyKey);
         return existing.get();
       }

       // Create new order
       Order order = Order.builder()
         .customerId(request.getCustomerId())
         .amount(request.getAmount())
         .idempotencyKey(idempotencyKey)  // Store the key
         .build();

       order = orderRepository.save(order);
       log.info("Created order with idempotency key: {}", idempotencyKey);
       return order;
     }
   }

   // Client usage
   @RestController
   public class OrderController {

     @PostMapping("/orders")
     public ResponseEntity<Order> createOrder(
         @RequestBody CreateOrderRequest request,
         @RequestHeader("Idempotency-Key") String idempotencyKey) {

       Order order = orderService.createOrder(request, idempotencyKey);
       return ResponseEntity
         .status(HttpStatus.CREATED)
         .header("Idempotency-Key", idempotencyKey)
         .body(order);
     }
   }
   ```

6. **Retry Configuration**
   ```yaml
   # application.yml
   resilience4j:
     retry:
       configs:
         default:
           maxAttempts: 3
           waitDuration: 1000ms
           retryExceptions:
             - java.io.IOException
             - java.net.ConnectException
             - java.util.concurrent.TimeoutException
             - org.springframework.web.client.HttpServerErrorException

       instances:
         # Conservative: payment gateway
         payment-gateway:
           baseConfig: default
           maxAttempts: 2
           waitDuration: 500ms

         # Lenient: internal service
         internal-api:
           baseConfig: default
           maxAttempts: 5
           waitDuration: 1000ms

         # Database queries
         database-query:
           baseConfig: default
           maxAttempts: 3
           waitDuration: 100ms

     timeLimiter:
       configs:
         default:
           timeoutDuration: 2s
           cancelRunningFuture: true
   ```

## Retry Decision Matrix

| Error | Transient | Idempotent | Retry? |
|---|---|---|---|
| **Network timeout** | Yes | Yes | ✓ Yes |
| **Connection refused** | Yes | Yes | ✓ Yes |
| **503 Service Unavailable** | Yes | Yes | ✓ Yes |
| **Payment declined** | No | Yes | ✗ No |
| **Duplicate order** | No | Yes | ✗ No |
| **Invalid request (400)** | No | Yes | ✗ No |
| **Authentication error (401)** | No | Yes | ✗ No |

## Backoff Calculation

```
Exponential Backoff Formula:
  Delay = min(baseDelay × 2^attempt, maxDelay)

Example (baseDelay=100ms, maxDelay=30s):
  Attempt 1: 100ms
  Attempt 2: 200ms
  Attempt 3: 400ms
  Attempt 4: 800ms
  Attempt 5: 1600ms
  ...
  Attempt 10+: 30000ms (capped)

With Jitter (±50%):
  Attempt 1: 50-150ms
  Attempt 2: 100-300ms
  Attempt 3: 200-600ms
  Attempt 4: 400-1200ms
```

## Best Practices

1. **Idempotency**: Every request must be safe to retry
   - Use idempotency keys (UUID)
   - Store request in database with outcome
   - Return same result on retry

2. **Exponential Backoff**: Prevent thundering herd
   - Initial: 100ms-1s
   - Multiplier: 2x per attempt
   - Max attempts: 3-5
   - Cap max delay: 30s

3. **Jitter**: Add randomization
   - Random: ±50% of backoff
   - Prevents synchronized retries

4. **Circuit Breaker**: Stop retrying if service down
   - Combine retry + circuit breaker
   - Circuit breaker prevents retry storm

5. **Distinguish Transient vs Permanent**
   ```java
   private boolean isTransient(Exception e) {
     return e instanceof IOException ||
            e instanceof TimeoutException ||
            e instanceof HttpServerErrorException;  // 5xx
   }

   private boolean isPermanent(Exception e) {
     return e instanceof IllegalArgumentException ||
            e instanceof HttpClientErrorException;   // 4xx
   }
   ```

## When to Use

- Network calls (APIs, databases)
- External services (payment gateways, email)
- Any transient failure (timeout, network error)
- Idempotent operations (GET, PUT, DELETE)

## When NOT to Use

- Non-idempotent operations without request deduplication
- Permanent failures (bad request, auth error)
- Real-time latency-critical paths
- Operations that might have side effects

## References

- [Resilience4j Retry](https://resilience4j.readme.io/docs/retry)
- [AWS Best Practices](https://docs.aws.amazon.com/general/latest/gr/retries.html)
- [Google SRE Book - Handling Overload](https://sre.google/books/)
- [Exponential Backoff And Jitter](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/)

---

**Key Takeaway**: Retry transient failures with exponential backoff + jitter. Ensure operations are idempotent. Combine with circuit breaker to prevent retry storms.
