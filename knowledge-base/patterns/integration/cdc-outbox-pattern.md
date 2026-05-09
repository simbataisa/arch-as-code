# Change Data Capture (CDC) with Outbox Pattern

Status: Approved | Last Reviewed: 2026-05-09 | Owner: @tech-lead-backend
Catalog ID: INT-002 | Radii (upgraded to ops-runbook depth in Wave 3b)
Tier Applicability: T0, T1

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

## NFR Acceptance Criteria

- **HA**: outbox table is on the service's own Aurora cluster (REF-001 multi-region for T0). Debezium connector itself is HA via Kafka Connect cluster with 3+ workers; failover is automatic.
- **HP**: outbox-poller adds 5–20ms P95 between commit and publish (acceptable for async event publishing; not in the request hot path). Per-event publish overhead < 5ms in Kafka.
- **HR**: dual-write inconsistency eliminated; CDC connector restart resumes from last-committed offset; pair with [EIP-024 Idempotent Receiver](../eip/idempotent-receiver.md) downstream for full at-least-once → effectively exactly-once.

## Compliance Mapping

| Layer | Reference | Section/Control | How this satisfies |
|---|---|---|---|
| Ring 0 | Microservices.io — Transactional Outbox | Canonical pattern | Implementation reference |
| Ring 0 | Debezium documentation | CDC implementation | Tooling reference |
| Ring 1 | Basel BCBS 239 §6 (Accuracy) | "Aggregation must avoid double-counting; no message loss" | Outbox guarantees the event reflects committed data; CDC guarantees no event loss |
| Ring 1 | ISO 20022 settlement-message integrity | Settlement events must mirror the underlying ledger commit | Outbox eliminates the dual-write race that would otherwise produce out-of-order or lost settlement messages |
| Ring 2 | SBV Circular 09/2020 §IV.2 (UNOFFICIAL TRANSLATION pending Legal) | Operational continuity | Outbox + CDC ensures downstream services receive every committed event |

## Cost / FinOps Notes

| Item | Cost driver | Order of magnitude |
|---|---|---|
| Outbox table storage | Event volume × retention (typically 7d after CDC capture) | ~10 GB at 10M events/day; trivial |
| Debezium / Kafka Connect cluster | Worker count × throughput | ~$300–800/month for typical T0 |
| Outbox-purge job | DELETE on expired rows | Negligible |
| Cross-region CDC replication | Egress + dual broker cost | Same as REF-001 baseline |

**Cost of NOT using Outbox**: dual-write race produces missing or duplicated events; downstream reconciliation costs / regulatory exposure / engineering time-to-debug far exceed Debezium's cost.

## Threat Model Summary

STRIDE: addresses **Tampering** (consistency) and **Repudiation** (no message loss).

- **Top 3 threats addressed**:
  1. *Dual-write inconsistency* — eliminated by single-transaction outbox insert.
  2. *Event loss on broker outage* — events stay in outbox table until CDC catches up.
  3. *Out-of-order events* — CDC preserves commit order from the database log.
- **Top 3 residual threats**:
  1. *Outbox bloat* if purger fails — alerts on table size growth; nightly purge job monitored.
  2. *Schema-evolution incompatibility* — outbox events should use versioned schemas (Confluent Schema Registry); breaking changes require dual-publish window.
  3. *Sensitive data in outbox table* — events may carry PII; same data-protection rules as the source table apply (Decree 13/2023, [PRIN-007](../../principles/data-residency.md), [SEC-008 Data Masking](../security/data-masking.md)).

## Operational Runbook (stub)

- **Alerts**:
  - `Outbox_LagSeconds_T0`: time between commit and CDC publish > 5 s sustained over 5 min. Severity: High (suggests Debezium issue).
  - `Outbox_TableSize`: outbox table > 2× expected steady-state size. Severity: Warning (purger may have stopped).
  - `Debezium_ConnectorState`: connector not in RUNNING state. Severity: Critical.
- **Dashboards**: Grafana — `outbox-cdc-overview` (table size, publish lag, connector health, throughput).
- **Recovery**:
  - Connector failure: Kafka Connect auto-restarts; if not, manual restart per runbook.
  - Stuck publish: identify the offending row; investigate why Debezium can't process it (often a binlog format issue); skip or replay per runbook.

## Test Strategy (stub)

- **Unit**: outbox-write transaction test (verify atomicity of business-row + outbox-row insert).
- **Integration**: Debezium Testcontainer + Kafka; verify event appears in topic after commit.
- **Chaos**: kill Debezium connector mid-publish; verify recovery from same offset; no duplicates downstream (paired with EIP-024).
- **Performance**: high-throughput write load; verify publish lag stays within tier budget.

## Related Patterns

- [PRIN-006 Idempotency-by-default](../../principles/idempotency-by-default.md) — outbox publish is idempotent by design
- [EIP-023 Guaranteed Delivery](../eip/guaranteed-delivery.md) — outbox is one of the canonical implementations
- [EIP-024 Idempotent Receiver](../eip/idempotent-receiver.md) — required downstream for at-most-once effective semantics
- [EIP-025 Dead Letter Channel](../eip/dead-letter-channel.md) — required for handling permanent downstream failures
- [INT-001 Saga Orchestration](saga-orchestration.md) — saga step events published via outbox
- [INT-004 Event Sourcing](event-sourcing.md) — outbox is a lightweight alternative to full event sourcing
- [REF-001 Multi-Region Active-Active](../../reference-architectures/multi-region-active-active.md) — outbox + MirrorMaker = cross-region event continuity

## References

- [Debezium Documentation](https://debezium.io/)
- [Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
- [Event Sourcing & Outbox](https://www.eventstore.com/)

---

**Key Takeaway**: Write business row + outbox row in one DB transaction. CDC (Debezium) publishes outbox rows to Kafka. Downstream consumers are idempotent (EIP-024). This eliminates the dual-write race that would otherwise produce lost or duplicated events.
