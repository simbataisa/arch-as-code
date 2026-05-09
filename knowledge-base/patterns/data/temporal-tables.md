# Temporal Tables (Versioned Tables) Pattern

Status: Approved | Last Reviewed: 2026-02-28 | Owner: @ea-board

## Problem Statement

Audit trails are critical but manually maintained in application code:
- Difficult to query historical state at any point in time
- Easy to miss audit logging in edge cases
- Complex application code mixing business logic with auditing
- Cannot easily reconstruct state on a specific date
- Compliance requires immutable audit trail

## Solution

Use database-level temporal tables (system-versioned tables). Database automatically maintains history; queries can access any point in time.

```
Application creates Order:
  INSERT INTO orders (customer_id, amount, status)
  VALUES (123, 50000, 'CREATED')

Database automatically:
  1. Inserts into active table
  2. Records valid_from timestamp
  3. Maintains history in temporal table

Query current state:
  SELECT * FROM orders WHERE customer_id = 123

Query historical state (as of 2026-01-15):
  SELECT * FROM orders FOR SYSTEM_TIME AS OF '2026-01-15'
  WHERE customer_id = 123
```

## Implementation Guidelines

1. **Create Temporal Table** (PostgreSQL 14+, SQL Server, MySQL 8.0)
   ```sql
   -- PostgreSQL example
   CREATE TABLE orders (
     id BIGSERIAL PRIMARY KEY,
     customer_id INT NOT NULL,
     amount DECIMAL(19,2) NOT NULL,
     status VARCHAR(50) NOT NULL,
     valid_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
     valid_to TIMESTAMP NOT NULL DEFAULT 'infinity'
   );

   -- Create history table
   CREATE TABLE orders_history (
     id BIGINT,
     customer_id INT,
     amount DECIMAL(19,2),
     status VARCHAR(50),
     valid_from TIMESTAMP,
     valid_to TIMESTAMP
   );

   -- Create trigger to maintain history
   CREATE OR REPLACE FUNCTION orders_audit()
   RETURNS TRIGGER AS $$
   BEGIN
     IF TG_OP = 'UPDATE' THEN
       -- Move old row to history with end timestamp
       UPDATE orders SET valid_to = CURRENT_TIMESTAMP
       WHERE id = NEW.id AND valid_to = 'infinity';

       INSERT INTO orders_history (id, customer_id, amount, status, valid_from, valid_to)
       VALUES (OLD.id, OLD.customer_id, OLD.amount, OLD.status, OLD.valid_from, CURRENT_TIMESTAMP);

       RETURN NEW;
     ELSIF TG_OP = 'DELETE' THEN
       UPDATE orders SET valid_to = CURRENT_TIMESTAMP
       WHERE id = NEW.id;
       RETURN OLD;
     END IF;
     RETURN NULL;
   END;
   $$ LANGUAGE plpgsql;

   CREATE TRIGGER orders_audit_trigger
   AFTER UPDATE ON orders FOR EACH ROW
   EXECUTE FUNCTION orders_audit();
   ```

2. **SQL Server System-Versioned Temporal Tables** (Recommended)
   ```sql
   -- SQL Server: built-in temporal support
   CREATE TABLE Orders (
     OrderId INT PRIMARY KEY,
     CustomerId INT NOT NULL,
     Amount DECIMAL(19,2) NOT NULL,
     Status VARCHAR(50) NOT NULL,
     SysStartTime DATETIME2 GENERATED ALWAYS AS ROW START HIDDEN,
     SysEndTime DATETIME2 GENERATED ALWAYS AS ROW END HIDDEN,
     PERIOD FOR SYSTEM_TIME (SysStartTime, SysEndTime)
   )
   WITH (SYSTEM_VERSIONING = ON (HISTORY_TABLE = dbo.Orders_History));

   -- Automatic: Orders_History table created automatically
   -- Database automatically manages version tracking

   -- Query current state
   SELECT * FROM Orders
   WHERE CustomerId = 123;

   -- Query as of specific time
   SELECT * FROM Orders
   FOR SYSTEM_TIME AS OF '2026-01-15 10:30:00'
   WHERE CustomerId = 123;

   -- Query all versions
   SELECT * FROM Orders
   FOR SYSTEM_TIME ALL
   WHERE CustomerId = 123
   ORDER BY SysStartTime;

   -- Query version range
   SELECT * FROM Orders
   FOR SYSTEM_TIME BETWEEN '2026-01-01' AND '2026-03-08'
   WHERE CustomerId = 123;
   ```

3. **MySQL 8.0+ Temporal Tables**
   ```sql
   CREATE TABLE orders (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     customer_id INT NOT NULL,
     amount DECIMAL(19,2) NOT NULL,
     status VARCHAR(50) NOT NULL,
     valid_from TIMESTAMP(6) GENERATED ALWAYS AS ROW START,
     valid_to TIMESTAMP(6) GENERATED ALWAYS AS ROW END,
     PERIOD FOR SYSTEM_TIME (valid_from, valid_to)
   )
   WITH SYSTEM VERSIONING;

   -- Query historical data
   SELECT * FROM orders
   FOR SYSTEM_TIME AS OF '2026-01-15'
   WHERE customer_id = 123;
   ```

4. **Java Application Usage**
   ```java
   @Entity
   @Table(name = "orders")
   public class Order {
     @Id
     private Long id;

     @Column(name = "customer_id")
     private Integer customerId;

     @Column(name = "amount")
     private BigDecimal amount;

     @Column(name = "status")
     private String status;

     @Column(name = "valid_from", insertable = false, updatable = false)
     @Temporal(TemporalType.TIMESTAMP)
     private Date validFrom;

     @Column(name = "valid_to", insertable = false, updatable = false)
     @Temporal(TemporalType.TIMESTAMP)
     private Date validTo;
   }

   @Repository
   public interface OrderRepository extends JpaRepository<Order, Long> {
     List<Order> findByCustomerId(Integer customerId);

     // Native query for temporal queries
     @Query(value = """
       SELECT * FROM orders
       FOR SYSTEM_TIME AS OF :asOf
       WHERE customer_id = :customerId
       """, nativeQuery = true)
     List<Order> findByCustomerIdAsOf(
       @Param("customerId") Integer customerId,
       @Param("asOf") LocalDateTime asOf
     );

     // Query all versions
     @Query(value = """
       SELECT * FROM orders
       FOR SYSTEM_TIME ALL
       WHERE customer_id = :customerId
       ORDER BY valid_from
       """, nativeQuery = true)
     List<Order> findAllVersions(@Param("customerId") Integer customerId);
   }

   @Service
   public class OrderAuditService {

     @Autowired
     private OrderRepository orderRepository;

     // Get current state
     public Order getCurrentOrder(Long orderId) {
       return orderRepository.findById(orderId).orElseThrow();
     }

     // Get state at specific time
     public Order getOrderAsOf(Long customerId, LocalDateTime asOf) {
       return orderRepository.findByCustomerIdAsOf(customerId.intValue(), asOf)
         .stream()
         .findFirst()
         .orElseThrow(() -> new OrderNotFoundException("No order found"));
     }

     // Get all versions (audit trail)
     public List<Order> getOrderAuditTrail(Integer customerId) {
       return orderRepository.findAllVersions(customerId);
     }

     // Reconstruct state at any point in time
     public OrderState reconstructState(Integer customerId, LocalDateTime targetDate) {
       List<Order> versions = getOrderAuditTrail(customerId);
       Order stateAtTime = versions.stream()
         .filter(v -> v.getValidFrom().toInstant()
           .isBefore(targetDate.toInstant()))
         .max(Comparator.comparing(Order::getValidFrom))
         .orElseThrow();

       return new OrderState(stateAtTime);
     }
   }
   ```

5. **Audit Reports and Analysis**
   ```sql
   -- Show all changes to an order
   SELECT
     valid_from,
     valid_to,
     customer_id,
     amount,
     status,
     DATEDIFF(minute, valid_from, valid_to) AS duration_minutes
   FROM Orders
   FOR SYSTEM_TIME ALL
   WHERE OrderId = 12345
   ORDER BY valid_from DESC;

   -- Find when status changed from PENDING to PROCESSING
   SELECT
     DISTINCT a.OrderId, a.Status, b.Status,
     a.valid_from AS changed_at
   FROM Orders FOR SYSTEM_TIME ALL a
   JOIN Orders FOR SYSTEM_TIME ALL b
     ON a.OrderId = b.OrderId
   WHERE a.Status = 'PENDING'
     AND b.Status = 'PROCESSING'
     AND a.valid_from < b.valid_from
     AND a.valid_to = b.valid_from;

   -- Show most volatile orders (changed most times)
   SELECT
     OrderId,
     COUNT(*) AS change_count,
     MIN(valid_from) AS created_at,
     MAX(valid_to) AS last_modified
   FROM Orders FOR SYSTEM_TIME ALL
   GROUP BY OrderId
   ORDER BY change_count DESC
   LIMIT 10;
   ```

## Temporal Table Benefits

| Benefit | Description |
|---------|---|
| **Automatic Auditing** | No application code needed |
| **Point-in-Time Queries** | Recreate state at any date/time |
| **Compliance** | Immutable audit trail |
| **Debugging** | Understand what changed and when |
| **Rollback Support** | Restore to previous state |
| **Database-Enforced** | Cannot bypass versioning |

## Performance Considerations

- **Current queries**: Same performance as normal tables
- **Historical queries**: Slower (more data to scan)
- **Storage**: 2x space (active + history tables)
- **Cleanup policy**: Archive old history periodically

```sql
-- Archive old history (older than 2 years)
DELETE FROM Orders_History
WHERE SysEndTime < DATEADD(year, -2, GETDATE());
```

## When to Use

- Compliance requirements (financial, healthcare)
- Audit trails required
- Need to query "what was the state on 2026-01-15?"
- Debugging state changes
- Legal/regulatory investigations

## When NOT to Use

- Simple, non-audited applications
- Performance-critical with huge volumes
- Time-series data (use specialized time-series DB)

## Comparison: Temporal Tables vs Event Sourcing

| Aspect | Temporal Tables | Event Sourcing |
|--------|---|---|
| **Storage** | 2x space | Large (events accumulate) |
| **Query simplicity** | Simple SQL | Need event replaying |
| **Compliance** | Automatic | Requires manual audit |
| **Debugging** | Point-in-time queries | Replay events |
| **Complexity** | Low (DB-managed) | High (app-managed) |

## References

- [SQL Server Temporal Tables](https://docs.microsoft.com/en-us/sql/relational-databases/tables/temporal-tables)
- [PostgreSQL Temporal Queries](https://www.postgresql.org/docs/current/functions-datetime.html)
- [MySQL Temporal Data](https://dev.mysql.com/doc/refman/8.0/en/create-table.html)

---

**Key Takeaway**: Use system-versioned temporal tables for automatic audit trails. Query any point in time with simple SQL. Minimal application code.
