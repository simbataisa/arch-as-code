# Bulkhead Isolation Pattern

Status: Approved | Last Reviewed: 2026-03-03 | Owner: @ea-board

## Problem Statement

Shared resources create failure dependencies:
- One slow customer exhausts thread pool
- All other requests get blocked
- One service consuming all connections starves others
- One API endpoint's failure blocks unrelated endpoints

```
Without Bulkhead:
  Shared Thread Pool (10 threads)
  ├─ 7 threads: Slow API call (Payment Service down)
  ├─ 2 threads: Fast API call (Inventory Service) — BLOCKED
  └─ 1 thread: Database query — BLOCKED

  Result: All services degraded due to one failure
```

## Solution

Isolate resources per consumer/domain. If one service consumes all resources, others are unaffected.

```
With Bulkhead:
  Payment Service Pool (5 threads)
  ├─ 4 threads: Slow call (service down)
  └─ 1 thread: Queued request

  Inventory Service Pool (5 threads)
  ├─ 2 threads: Requests (responsive)
  ├─ 2 threads: Requests (responsive)
  └─ 1 thread: Ready

  Result: Inventory continues working; Payment Service degrades gracefully
```

## Implementation Guidelines

1. **Thread Pool Bulkhead** (Resilience4j)
   ```java
   @Configuration
   public class BulkheadConfig {

     @Bean
     public BulkheadRegistry bulkheadRegistry() {
       BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
         .maxConcurrentCalls(10)           // Max 10 concurrent calls
         .maxWaitDuration(Duration.ofSeconds(1))  // Max 1s wait in queue
         .build();

       return BulkheadRegistry.of(bulkheadConfig);
     }

     // Dedicated thread pool for payment service (smaller)
     @Bean
     public ThreadPoolTaskExecutor paymentServiceExecutor() {
       ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
       executor.setThreadNamePrefix("payment-service-");
       executor.setCorePoolSize(3);        // Min 3 threads
       executor.setMaxPoolSize(5);         // Max 5 threads
       executor.setQueueCapacity(10);      // Queue up to 10 requests
       executor.initialize();
       return executor;
     }

     // Dedicated thread pool for inventory service (larger)
     @Bean
     public ThreadPoolTaskExecutor inventoryServiceExecutor() {
       ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
       executor.setThreadNamePrefix("inventory-service-");
       executor.setCorePoolSize(5);
       executor.setMaxPoolSize(10);
       executor.setQueueCapacity(20);
       executor.initialize();
       return executor;
     }

     // Database query thread pool (small, limited connections)
     @Bean
     public ThreadPoolTaskExecutor databaseExecutor() {
       ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
       executor.setThreadNamePrefix("database-");
       executor.setCorePoolSize(2);
       executor.setMaxPoolSize(5);
       executor.setQueueCapacity(5);
       executor.setRejectedExecutionHandler(
         new ThreadPoolTaskExecutor.CallerRunsPolicy()
       );
       executor.initialize();
       return executor;
     }
   }
   ```

2. **Using Bulkhead with Annotations**
   ```java
   @Service
   public class OrderService {

     @Bulkhead(
       name = "payment-service",
       type = Bulkhead.Type.THREADPOOL,
       fallbackMethod = "paymentFallback"
     )
     public PaymentResult processPayment(PaymentRequest request) {
       log.info("Processing payment");
       return paymentClient.process(request);
     }

     public PaymentResult paymentFallback(
         PaymentRequest request,
         BulkheadFullException e) {
       log.warn("Payment bulkhead exhausted, returning fallback");
       return PaymentResult.QUEUED_FOR_RETRY;
     }
   }
   ```

3. **Database Connection Pool Bulkhead**
   ```java
   @Configuration
   public class DataSourceConfig {

     @Bean
     public DataSource paymentDataSource() {
       HikariConfig config = new HikariConfig();
       config.setJdbcUrl("jdbc:postgresql://postgres/payments");
       config.setUsername("payment_user");
       config.setPassword("${DB_PASSWORD}");
       config.setMaximumPoolSize(10);      // Max 10 connections
       config.setMinimumIdle(2);           // Keep 2 idle
       config.setConnectionTimeout(5000);  // 5 second timeout
       config.setIdleTimeout(600000);      // 10 minute idle timeout
       return new HikariDataSource(config);
     }

     @Bean
     public DataSource reportingDataSource() {
       HikariConfig config = new HikariConfig();
       config.setJdbcUrl("jdbc:postgresql://postgres/analytics");
       config.setUsername("analytics_user");
       config.setPassword("${DB_PASSWORD}");
       config.setMaximumPoolSize(20);      // Large pool for analytics
       config.setReadOnly(true);           // Read-only (less resource usage)
       return new HikariDataSource(config);
     }
   }
   ```

4. **Semaphore Bulkhead** (Lighter than thread pool)
   ```java
   @Service
   public class InventoryService {

     private final Semaphore semaphore = new Semaphore(5);  // Max 5 concurrent

     public InventoryResult checkInventory(String productId) {
       try {
         semaphore.acquire();
         return inventoryClient.check(productId);
       } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         throw new BulkheadException("Inventory service overloaded");
       } finally {
         semaphore.release();
       }
     }
   }
   ```

5. **Kafka Consumer Bulkhead** (Thread pool per consumer group)
   ```java
   @Configuration
   public class KafkaConsumerConfig {

     @Bean(name = "orderConsumerExecutor")
     public TaskScheduler orderConsumerExecutor() {
       ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
       executor.setPoolSize(5);             // 5 threads for order consumers
       executor.setThreadNamePrefix("kafka-order-");
       executor.initialize();
       return executor;
     }

     @Bean(name = "paymentConsumerExecutor")
     public TaskScheduler paymentConsumerExecutor() {
       ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
       executor.setPoolSize(3);             // 3 threads for payment
       executor.setThreadNamePrefix("kafka-payment-");
       executor.initialize();
       return executor;
     }

     @KafkaListener(topics = "orders", groupId = "order-service")
     public void onOrderEvent(OrderEvent event) {
       // Processed by orderConsumerExecutor (5 threads max)
       processOrder(event);
     }

     @KafkaListener(topics = "payments", groupId = "payment-service")
     public void onPaymentEvent(PaymentEvent event) {
       // Processed by paymentConsumerExecutor (3 threads max)
       processPayment(event);
     }
   }
   ```

6. **Queue-Based Bulkhead** (Async with bounded queue)
   ```java
   @Service
   public class AsyncBulkheadService {

     private final BlockingQueue<Task> taskQueue =
       new LinkedBlockingQueue<>(100);  // Queue size: 100

     private final ExecutorService executor =
       Executors.newFixedThreadPool(5);  // 5 workers

     @PostConstruct
     public void init() {
       // Worker thread processing queue
       executor.submit(() -> {
         while (true) {
           try {
             Task task = taskQueue.take();
             task.execute();
           } catch (InterruptedException e) {
             Thread.currentThread().interrupt();
             break;
           }
         }
       });
     }

     public void submitTask(Task task) throws BulkheadException {
       // Non-blocking: add to queue or fail fast
       if (!taskQueue.offer(task)) {
         throw new BulkheadException("Task queue full, rejecting request");
       }
     }

     public int getQueueSize() {
       return taskQueue.size();
     }
   }
   ```

7. **Monitoring Bulkheads**
   ```java
   @Component
   public class BulkheadMetrics {

     @Autowired
     private BulkheadRegistry bulkheadRegistry;

     @Scheduled(fixedRate = 10000)
     public void logBulkheadMetrics() {
       bulkheadRegistry.getAllBulkheads().forEach(bh -> {
         Bulkhead.Metrics metrics = bh.getMetrics();
         log.info("Bulkhead: {} | " +
           "Available: {} | Pending: {} | Max: {}",
           bh.getName(),
           metrics.getAvailableConcurrentCalls(),
           metrics.getWaitingQueuePopulation(),
           metrics.getMaxConcurrentCalls()
         );
       });
     }

     @GetMapping("/metrics/bulkheads")
     public ResponseEntity<List<BulkheadMetricDto>> getBulkheadMetrics() {
       return ResponseEntity.ok(
         bulkheadRegistry.getAllBulkheads()
           .stream()
           .map(bh -> new BulkheadMetricDto(
             bh.getName(),
             bh.getMetrics().getAvailableConcurrentCalls(),
             bh.getMetrics().getWaitingQueuePopulation(),
             bh.getMetrics().getMaxConcurrentCalls()
           ))
           .collect(Collectors.toList())
       );
     }
   }
   ```

## Bulkhead Configuration Best Practices

| Component | Thread Pool Size | Queue Size | Timeout |
|-----------|---|---|---|
| **API (Web)** | CPUs × 2 | 20-50 | 5-10s |
| **Database Query** | CPUs | 10-20 | 10-30s |
| **External API** | CPUs × 0.5 | 5-10 | 3-5s |
| **Message Processing** | CPUs × 1 | 100-500 | 60s |
| **Reporting/Analytics** | CPUs × 4 | 1000+ | 60s+ |

Formula:
```
Thread Pool Size = (Number of Tasks × Task Execution Time) / CPU Cores
Example: (1000 requests/sec × 0.1s per request) / 8 cores = 12.5 ≈ 13 threads
```

## When to Use

- Different services with different resource requirements
- Some endpoints slow, others fast
- Shared database (bulkhead per client)
- Preventing one client from starving others

## When NOT to Use

- Single, homogeneous service
- Low concurrency
- Resources already isolated (separate servers)

## Rejection Policies

| Policy | Behavior |
|---|---|
| **Abort** | Throw exception, reject request |
| **Caller Runs** | Caller thread handles task (sync) |
| **Discard** | Silently drop request |
| **Discard Oldest** | Drop oldest queued request |

```java
executor.setRejectedExecutionHandler(
  new ThreadPoolTaskExecutor.AbortPolicy()  // Or CallerRunsPolicy, etc.
);
```

## References

- [Resilience4j Bulkhead](https://resilience4j.readme.io/docs/bulkhead)
- [Release It! (Book) - Bulkheads](https://pragprog.com/titles/mnee2/release-it-second-edition/)
- [Thread Pool Sizing Guide](https://www.oracle.com/technical-resources/articles/java/fork-join.html)

---

**Key Takeaway**: Isolate resources per consumer/service. If one service exhausts its pool, others continue working. Prevents resource starvation and cascading failures.
