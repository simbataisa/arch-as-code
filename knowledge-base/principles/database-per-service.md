# Database-Per-Service

Status: Approved | Last Reviewed: 2026-01-25 | Owner: @ea-board
Catalog ID: PRIN-004 | Radii
Tier Applicability: T0, T1, T2, T3

## Problem Statement

Shared databases create tight coupling between microservices:
- Schema changes require coordinating multiple teams
- Cannot scale databases independently
- Difficult to deploy services independently (DDL migrations block)
- Vendor lock-in (all services tied to same database technology)
- Performance issues: one service's queries slow down others
- Compliance: cannot isolate data ownership
- Cascading failures: database down = all services down

## Solution

Each microservice owns its data store. Services exchange data via APIs and events, not shared tables.

### Key Principles

1. **Data Ownership**: Each service owns its schema and database
2. **Polyglot Persistence**: Use the best database for each service's needs
3. **No Shared Tables**: Avoid `select * from shared_table`
4. **Asynchronous Sync**: Use events (Kafka, CDC) to sync data across services
5. **Read Replicas**: For analytics/reporting, replicate data via CDC

## Implementation Guidelines

1. **Identify Service Boundaries**
   - Use Domain-Driven Design (DDD) to define bounded contexts
   - Each bounded context = one service = one database
   - Example: Order Service owns `orders`, `order_items` tables
   - Example: Inventory Service owns `products`, `stock_levels` tables

2. **Database Technology Selection**
   - Choose based on access patterns, not convenience
   - Relational (PostgreSQL): Structured data, complex queries
   - Document (MongoDB): Semi-structured, flexible schema
   - Key-value (Redis): High-speed caching, sessions
   - Search (Elasticsearch): Full-text search, analytics
   - Time-series (InfluxDB): Metrics, monitoring

3. **Data Synchronization**
   - **Via Events**: Service A publishes event → Service B subscribes
   - **Via CDC**: Capture changes from Service A's database → Service B updates
   - **Via Read Replicas**: Replicate tables to central data warehouse for reporting
   - Use Debezium for CDC: tracks binlog/WAL, streams to Kafka

4. **Querying Across Services**
   - **Never join across services**: Don't select from another service's tables
   - **Use APIs**: Service B needs Order data → calls Order Service API
   - **Read replicas for analytics**: Central warehouse has copies of data from all services
   - **Materialized views**: Service B keeps cached view of Order data it needs

5. **Transactions and Consistency**
   - Local ACID transactions: within single service only
   - Distributed transactions: use SAGA pattern, not 2-phase commit
   - Eventual consistency: accept that updates propagate asynchronously
   - Reconciliation jobs: periodic checks to fix data drift

6. **Migration Strategy** (Strangler Fig Pattern)
   - Phase 1: Create new service database (empty)
   - Phase 2: Run dual writes (old DB + new DB)
   - Phase 3: Verify data consistency
   - Phase 4: Switch reads to new DB
   - Phase 5: Remove dual writes, retire old DB

## Exceptions to Database-Per-Service

1. **Read Replicas for Reporting**
   ```
   Order Service DB → CDC → Data Warehouse → Analytics Team
   ```
   Okay: reading replicated data for analytics/BI

2. **Shared Reference Data**
   - Example: Currency, Country, Tax Rate tables
   - Small, infrequently changed data
   - Replicate to each service as read-only reference data
   - Update via CDC trigger

3. **Testing/Development**
   - May use shared database for local development
   - Production MUST use database-per-service

## Anti-Patterns to Avoid

❌ **Shared Database**: Multiple services share same schema
```sql
-- Order Service and Inventory Service both access this table
SELECT * FROM shared.products WHERE product_id = ?;
UPDATE shared.products SET stock = stock - 1;
```

❌ **Direct Table Access**: Service B queries Service A's database
```sql
-- Inventory Service directly queries Order Service DB (BAD)
SELECT customer_id, SUM(quantity) FROM orders.orders GROUP BY customer_id;
```

✅ **Correct**: Service B calls API or reads replicated data
```java
// Inventory Service calls Order Service API
OrderServiceClient.getOrdersByCustomer(customerId);

// Or reads from local replica (via CDC)
SELECT customer_id, SUM(quantity) FROM inventory.customer_orders_cache;
```

## Techcombank Checklist

- [ ] Each service has dedicated database
- [ ] Services never query other service's tables directly
- [ ] Data sync uses events (Kafka, CDC) or read replicas
- [ ] SAGA pattern for distributed transactions
- [ ] Reference data replicated read-only to all services
- [ ] Analytics/BI use central data warehouse (CDC replicated)

## References

- [Building Microservices](https://samnewman.net/books/building_microservices/) (Newman, O'Reilly)
- [Domain-Driven Design](https://www.domainlanguage.com/ddd/) (Evans)
- [Debezium CDC](https://debezium.io/)
- [Strangler Fig Pattern](https://martinfowler.com/bliki/StranglerFigApplication.html)

---

**Key Takeaway**: Each service owns its database. Sync data via events and CDC. Use APIs for cross-service queries. Never share tables.
