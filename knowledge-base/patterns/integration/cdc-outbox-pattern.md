# Change Data Capture (CDC) with Outbox Pattern

Status: Approved | Last Reviewed: 2026-02-12 | Owner: @ea-board

## Problem Statement

Microservices need to publish events when data changes, but naive approaches fail:
- **Dual-write problem**: Write to DB, then publish event → if second fails, inconsistency
- **Race conditions**: Consumer reads event before transaction commits
- **Event loss**: Event broker goes down between DB write and publish
- **Exactly-once semantics**: Ensure event published exactly once, no duplicates or loss

## Solution

Write changes to an outbox table in the same transaction as business data. Separate process (CDC) captures changes and publishes to event broker.

```
Service A:
  BEGIN TRANSACTION
    1. INSERT INTO orders (id, customer_id, amount) VALUES (...)
    2. INSERT INTO outbox (event_type, payload) VALUES ('OrderCreated', {...})
  COMMIT

CDC Process (Debezium):
  Watches outbox table changes
    ↓
  Publishes to Kafka Topic
    ↓
  Service B subscribes to topic
```

## Implementation Guidelines

1. **Create Outbox Table**
   ```sql
   CREATE TABLE outbox (
     id BIGINT PRIMARY KEY AUTO_INCREMENT,
     aggregate_id VARCHAR(255) NOT NULL,
     aggregate_type VARCHAR(100) NOT NULL,
     event_type VARCHAR(100) NOT NULL,
     payload JSON NOT NULL,
     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     processed_at TIMESTAMP,
     INDEX idx_processed (processed_at),
     INDEX idx_aggregate (aggregate_type, aggregate_id)
   );
   ```

2. **Atomic Write**
   ```java
   @Service
   @Transactional
   public class OrderService {

     @Autowired
     private OrderRepository orderRepository;
     @Autowired
     private OutboxRepository outboxRepository;
     @Autowired
     private ObjectMapper objectMapper;

     public Order createOrder(CreateOrderRequest request) {
       // Business logic
       Order order = Order.builder()
         .customerId(request.getCustomerId())
         .amount(request.getAmount())
         .status(OrderStatus.PENDING)
         .createdAt(Instant.now())
         .build();

       // Step 1: Save order (in same transaction)
       order = orderRepository.save(order);

       // Step 2: Write event to outbox (in same transaction)
       OrderCreatedEvent event = OrderCreatedEvent.builder()
         .orderId(order.getId())
         .customerId(order.getCustomerId())
         .amount(order.getAmount())
         .timestamp(Instant.now())
         .build();

       Outbox outboxRecord = Outbox.builder()
         .aggregateId(order.getId().toString())
         .aggregateType("Order")
         .eventType("OrderCreated")
         .payload(objectMapper.writeValueAsString(event))
         .createdAt(Instant.now())
         .build();

       outboxRepository.save(outboxRecord);

       // Transaction commits atomically with both inserts
       return order;
     }
   }
   ```

3. **Debezium CDC Configuration**
   ```json
   {
     "name": "order-outbox-connector",
     "config": {
       "connector.class": "io.debezium.connector.mysql.MySqlConnector",
       "database.hostname": "mysql-prod",
       "database.port": 3306,
       "database.user": "debezium",
       "database.password": "secret",
       "database.server.id": 1,
       "database.server.name": "order-service",
       "table.include.list": "order_service.outbox",
       "plugin.name": "pgoutput",
       "publication.name": "dbz_publication",

       "transforms": "unwrap,route",
       "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
       "transforms.route.type": "org.apache.kafka.connect.transforms.RegexRouter",
       "transforms.route.regex": "([^.]+)\\.([^.]+)\\.([^.]+)",
       "transforms.route.replacement": "$3",

       "key.converter": "org.apache.kafka.connect.json.JsonConverter",
       "value.converter": "org.apache.kafka.connect.json.JsonConverter",
       "key.converter.schemas.enable": false,
       "value.converter.schemas.enable": false,

       "database.snapshot.mode": "initial",
       "snapshot.mode": "initial",
       "snapshot.isolation.mode": "repeatable_read"
     }
   }
   ```

4. **Outbox Poller** (Alternative to CDC if Debezium not available)
   ```java
   @Component
   public class OutboxPollerScheduler {

     @Autowired
     private OutboxRepository outboxRepository;
     @Autowired
     private KafkaTemplate<String, String> kafkaTemplate;

     @Scheduled(fixedDelay = 100) // Poll every 100ms
     public void pollAndPublish() {
       List<Outbox> unprocessedEvents = outboxRepository.findByProcessedAtIsNull();

       for (Outbox outbox : unprocessedEvents) {
         try {
           // Publish to Kafka
           kafkaTemplate.send(
             outbox.getAggregateType().toLowerCase(),
             outbox.getAggregateId(),
             outbox.getPayload()
           ).get(5, TimeUnit.SECONDS);

           // Mark as processed
           outbox.setProcessedAt(Instant.now());
           outboxRepository.save(outbox);
         } catch (Exception e) {
           log.error("Failed to publish event: {}", outbox.getId(), e);
           // Retry on next poll
         }
       }
     }
   }
   ```

5. **Idempotent Consumer**
   - Consumers must handle duplicate events (same event published twice)
   - Use `eventId` as idempotency key
   - Track processed events: `processed_events` table
   ```java
   @Service
   public class OrderEventListener {

     @Autowired
     private ProcessedEventRepository processedEventRepository;

     @KafkaListener(topics = "Order")
     public void onOrderCreated(String message) {
       OrderCreatedEvent event = deserialize(message);

       // Check if already processed
       if (processedEventRepository.existsById(event.getEventId())) {
         log.debug("Event already processed: {}", event.getEventId());
         return;
       }

       // Process event
       // ...

       // Mark as processed
       processedEventRepository.save(new ProcessedEvent(event.getEventId()));
     }
   }
   ```

## Architecture Diagram

```
┌─────────────────────────────────┐
│   Service A (Order Service)      │
├─────────────────────────────────┤
│  Application Code                │
│    ↓                             │
│  BEGIN TRANSACTION               │
│    INSERT orders (...)           │
│    INSERT outbox (...)           │
│  COMMIT                          │
└──────────────┬────────────────────┘
               │
               ↓ (Database WAL/Binlog)
        ┌──────────────────┐
        │  Debezium CDC    │
        │  (Kafka Connect) │
        └────────┬─────────┘
                 │
                 ↓ (Event Stream)
        ┌──────────────────┐
        │  Kafka Topic     │
        │  "Order"         │
        └────────┬─────────┘
                 │
                 ↓
┌─────────────────────────────────┐
│   Service B (Inventory Service)  │
├─────────────────────────────────┤
│  Kafka Consumer (Idempotent)    │
│    Processes OrderCreated event │
└─────────────────────────────────┘
```

## Comparison: Outbox vs Direct Publish

| Aspect | Outbox Pattern | Direct Publish |
|--------|----------------|---|
| Consistency | Atomic (one transaction) | Race condition risk |
| Event Loss | No (in database) | Possible |
| Duplicates | Possible (tolerate & deduplicate) | No |
| Latency | Low (100ms-1s polling or CDC real-time) | Immediate |
| Complexity | Medium | Low |
| **Recommendation** | Use for critical events | Use for non-critical |

## When to Use

- Financial transactions (payments, transfers, ledger entries)
- Order processing (state changes must be published)
- Audit trails (regulatory compliance)
- Real-time data sync across services
- Exactly-once semantics required

## When NOT to Use

- Low-volume events
- Acceptable event loss (e.g., metrics, non-critical notifications)
- Extremely low latency (outbox adds 100ms+)

## Tools

| Tool | Use Case |
|------|----------|
| **Debezium** | CDC from DB to Kafka, fully managed |
| **Kafka Connect** | Extensible connectors for many sources |
| **Outbox Poller** | Simple polling without external CDC tool |

## References

- [Debezium Documentation](https://debezium.io/)
- [Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
- [Event Sourcing & Outbox](https://www.eventstore.com/)

---

**Key Takeaway**: Use Outbox Pattern to ensure events are reliably published with business data. Debezium automatically captures and streams changes. Consumers must be idempotent.
