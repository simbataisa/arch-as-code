# Event-Driven Architecture

Status: Approved | Last Reviewed: 2026-01-20 | Owner: @ea-board

## Problem Statement

Synchronous, request-response communication creates tight coupling:
- Service A calls Service B directly; if B is slow or down, A fails
- Scaling is difficult (each request ties up resources)
- Temporal dependencies create transaction complexity
- Difficult to broadcast state changes to many consumers
- Cascading failures propagate across the system

## Solution

Decouple services using asynchronous events. Services publish events when state changes; interested subscribers react independently.

### Key Principles

1. **Loose Coupling**: Services don't know about each other; only about events
2. **Scalability**: Subscribers can process events at their own pace
3. **Resilience**: Failures in one subscriber don't block others
4. **Event Sourcing**: Events are the source of truth for state changes
5. **Eventual Consistency**: Data synchronization happens asynchronously

## Implementation Guidelines

1. **Define Event Schema**
   - Unique event type: `com.techcombank.order.OrderCreated`
   - Required fields: `eventId`, `eventType`, `timestamp`, `aggregateId`, `version`
   - Domain data: fields specific to the event
   - Example:
     ```json
     {
       "eventId": "evt_abc123",
       "eventType": "com.techcombank.order.OrderCreated",
       "timestamp": "2026-03-08T10:30:00Z",
       "aggregateId": "ord_xyz789",
       "version": 1,
       "customerId": "cust_456",
       "amount": 50000.00,
       "currency": "VND"
     }
     ```

2. **Choose Event Transport**
   - **Kafka**: High-throughput, durable, ordered per partition, retention policies
   - **RabbitMQ**: Lower latency, flexible routing, dead-letter queues
   - **AWS SNS/SQS**: Managed, scalable, built-in AWS integration
   - **Google Pub/Sub**: Exactly-once semantics, schema registry

3. **Publisher Implementation**
   - Publish events after state change (use Outbox pattern to ensure durability)
   - Include correlation IDs for tracing: `correlationId`, `causationId`
   - Serialize consistently (JSON recommended)

4. **Subscriber Implementation**
   - Implement idempotent event handlers (same event processed twice = same result)
   - Use unique consumer group/subscription names
   - Handle out-of-order events gracefully (check versions)
   - Implement dead-letter queues for failed events

5. **Event Versioning**
   - Start with `version: 1` in event metadata
   - Backward compatible: add optional fields
   - Use separate topics/versions for breaking changes: `OrderCreated.v1` → `OrderCreated.v2`

## When to Use

- **Cross-service communication**: Order → Inventory → Shipping → Notification
- **State broadcasting**: Account balance changes → multiple UIs
- **Audit trails**: Financial transactions, compliance events
- **Real-time analytics**: User activity streams
- **Workflows with delays**: Approval workflows, expiration handling
- **IoT/sensor data**: Time-series events from devices

## When NOT to Use

- **Synchronous queries**: "What's my account balance?" → don't use events
- **Strong consistency requirements**: ACID transactions across services (use Saga pattern instead)
- **Request-response**: RPC-style calls (use REST API or gRPC)
- **Low-latency requirements** (under 100ms) where event processing introduces unacceptable delay

## Event-Driven Patterns

- **Event Sourcing**: Store events as immutable log, derive state from events
- **CQRS**: Separate read model (events) from write model (commands)
- **Outbox Pattern**: Write to local table + event log atomically
- **SAGA Pattern**: Coordinate distributed transactions via events

## References

- [Event Streaming Patterns](https://www.confluent.io/blog/event-streaming-patterns-and-topologies/)
- [Apache Kafka](https://kafka.apache.org/)
- [RabbitMQ](https://www.rabbitmq.com/)
- [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html)

---

**Key Takeaway**: Use events to decouple services, enable scalability, and establish eventual consistency. Combine with Outbox and Saga patterns for reliable distributed systems.
