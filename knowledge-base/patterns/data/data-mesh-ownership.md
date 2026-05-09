# Data Mesh Pattern

Status: Approved | Last Reviewed: 2026-03-01 | Owner: @ea-board
Catalog ID: DATA-002 | Radii
Tier Applicability: T1, T2

## Problem Statement

Centralized data teams become bottlenecks:
- Single data warehouse team manages all data
- Business domains wait for data team to build pipelines
- Data quality issues affect all downstream consumers
- Lack of domain understanding in data pipelines
- Scaling data infrastructure becomes cost-prohibitive
- No incentive for source systems to maintain data quality

## Solution

Distribute data ownership to domain teams. Each team owns its data as a product. Domains build data pipelines and expose data through APIs and data products.

```
Before (Centralized):
  ┌─────────────┐
  │Domain Teams │
  └──────┬──────┘
         │
         ↓ ETL requests
  ┌──────────────────┐
  │Data Team (Bottleneck)
  │ - Data Warehouse│
  │ - BI Tools      │
  └──────┬───────────┘
         │
         ↓ Dashboards
  ┌─────────────┐
  │Analytics    │
  │Teams        │
  └─────────────┘

After (Data Mesh):
  ┌──────────────────────┐
  │ Domain A (Payments)  │
  │ ├─ Payment Events   │
  │ ├─ Data API         │
  │ └─ Quality Metrics  │
  └──────────┬───────────┘
             │
  ┌──────────────────────┐
  │ Domain B (Orders)    │
  │ ├─ Order Events     │
  │ ├─ Data API         │
  │ └─ Quality Metrics  │
  └──────────┬───────────┘
             │
  ┌──────────────────────┐
  │ Data Mesh Platform   │
  │ ├─ Data Catalog    │
  │ ├─ Discovery       │
  │ ├─ Governance      │
  │ └─ Quality Checks  │
  └──────────┬───────────┘
             │
  ┌────────────────────┐
  │Analytics (Self-serve)
  └────────────────────┘
```

## Four Principles of Data Mesh

1. **Domain Ownership**
   - Each domain team owns its data products
   - Domain expert → better data quality
   - Incentivizes clean data

2. **Data as a Product**
   - Treat data like a service: API contracts, SLAs
   - Data consumers are customers
   - Manage dependencies, versioning, backward compatibility

3. **Self-Serve Data Infrastructure**
   - Platform team provides tools, not manual ETL
   - Domains build pipelines autonomously
   - Tools: Apache Kafka, dbt, Great Expectations

4. **Federated Computational Governance**
   - Decentralized decision-making
   - Global standards (data lineage, quality, privacy)
   - Domain autonomy within boundaries

## Implementation Guidelines

1. **Define Domain Data Products**
   - Domain: Payments Service
   - Data product: `payments-events`
   - Contains: Transaction events, payment status changes
   - Owners: Payment team
   - SLA: 99.9% availability, <100ms latency

   ```yaml
   # data-product-manifest.yaml
   name: payments-events
   owner:
     team: payments-platform
     slack: #payments-team
   description: >
     Real-time payment transaction events
     including authorization, clearing, settlement

   contract:
     version: 1.0
     schema: schema/payments-v1.json
     examples: examples/sample-events.json

   delivery:
     kafka_topic: com.techcombank.payments.transactions
     format: json
     partitioning: by payment_id

   quality:
     freshness_sla: 100ms
     availability_sla: 99.9%
     completeness: >95%
     accuracy: certified

   lineage:
     sources:
       - system: payment-processor
         table: transactions
     consumers:
       - analytics-team
       - fraud-detection
   ```

2. **Data Product API**
   ```java
   @RestController
   @RequestMapping("/data-products/payments")
   public class PaymentsDataProductController {

     @Autowired
     private PaymentEventRepository eventRepository;

     // Kafka topic for streaming
     @KafkaListener(topics = "payments-events")
     public void onPaymentEvent(PaymentEvent event) {
       // Process and store for analytics
     }

     // REST API for batch queries
     @GetMapping("/transactions")
     public ResponseEntity<Page<PaymentTransaction>> getTransactions(
         @RequestParam String customerId,
         @RequestParam(required = false) LocalDateTime from,
         @RequestParam(required = false) LocalDateTime to,
         Pageable pageable) {

       Page<PaymentTransaction> transactions = eventRepository
         .findByCustomerIdAndDateRange(customerId, from, to, pageable);

       return ResponseEntity.ok()
         .header("X-Data-Product-Version", "1.0")
         .header("X-Quality-Certified", "true")
         .body(transactions);
     }

     // Data lineage endpoint
     @GetMapping("/lineage")
     public ResponseEntity<DataLineage> getLineage() {
       return ResponseEntity.ok(new DataLineage()
         .source("payment-processor")
         .consumer("analytics-team")
         .consumer("fraud-detection")
         .lastUpdated(Instant.now())
       );
     }

     // Quality metrics endpoint
     @GetMapping("/quality")
     public ResponseEntity<QualityMetrics> getQualityMetrics() {
       return ResponseEntity.ok(new QualityMetrics()
         .freshness("2 seconds")
         .completeness(99.5)
         .accuracy(99.8)
         .availabilitySla(99.9)
       );
     }
   }
   ```

3. **Self-Serve Data Platform** (Powered by dbt + Kafka)
   ```yaml
   # dbt project for Payments domain
   # models/staging/stg_payments.sql
   {{config(materialized='table')}}

   select
     payment_id,
     customer_id,
     amount,
     currency,
     status,
     created_at,
     updated_at
   from raw.payments.transactions
   where created_at >= '{{ run_started_at - interval 1 day }}'

   ---
   # models/marts/payment_metrics.sql
   {{config(materialized='table', tags=['daily'])}}

   select
     date_trunc('day', created_at) as payment_date,
     customer_id,
     count(*) as transaction_count,
     sum(amount) as total_amount,
     avg(amount) as avg_amount
   from {{ ref('stg_payments') }}
   group by 1, 2
   ```

4. **Data Catalog & Discovery**
   ```java
   @Service
   public class DataCatalogService {

     // Register data product
     public void registerDataProduct(DataProductMetadata metadata) {
       // Catalog entry includes:
       // - Name, owner, description
       // - Schema, format, location
       // - Quality metrics
       // - Dependencies, lineage
       // - Access controls
       dataCatalogRepository.save(metadata);
     }

     // Discovery API
     public List<DataProductMetadata> discoverByTag(String tag) {
       return dataCatalogRepository.findByTagsContaining(tag);
     }

     // Check dependencies
     public DataProductLineage getLineage(String dataProductId) {
       return lineageRepository.getLineage(dataProductId);
     }
   }
   ```

5. **Quality & Governance**
   ```yaml
   # Great Expectations for data quality
   # tests/payments-events.yml
   datasets:
     - name: payments_events
       tests:
         - expect_table_row_count_to_be_between:
             min_value: 100000
             max_value: 10000000
         - expect_column_values_to_not_be_null:
             column: payment_id
         - expect_column_values_to_be_in_set:
             column: status
             value_set: ["AUTHORIZED", "CLEARED", "SETTLED", "FAILED"]
         - expect_column_values_to_match_regex:
             column: payment_id
             regex: '^PAY-[0-9]{10}$'
   ```

6. **Access Control & Governance**
   ```java
   @Service
   public class DataAccessControlService {

     // Define who can access what data
     public void grantAccess(
         String dataProductId,
         String consumerId,
         Set<String> permissions) {

       AccessControlEntry ace = new AccessControlEntry()
         .dataProduct(dataProductId)
         .consumer(consumerId)
         .permissions(permissions)  // read, write, admin
         .expiresAt(LocalDateTime.now().plusMonths(6))
         .auditLog(true);

       accessControlRepository.save(ace);
     }

     // Audit data access
     @Service
     public void auditDataAccess(String dataProductId, String consumerId) {
       auditLogRepository.log(
         "DATA_ACCESS",
         dataProductId,
         consumerId,
         Instant.now()
       );
     }
   }
   ```

## Implementation Roadmap

**Phase 1: Foundation** (Months 1-2)
- Identify domain boundaries
- Establish data product standards
- Deploy self-serve platform (dbt, Kafka)

**Phase 2: Migration** (Months 3-6)
- First 3 domains build data products
- Establish quality metrics
- Build data catalog

**Phase 3: Scaling** (Months 7-12)
- Scale to all domains
- Implement governance framework
- Migrate analytics to self-serve

**Phase 4: Optimization** (Months 13+)
- Performance tuning
- Cost optimization
- Advanced analytics

## Tools & Technologies

| Layer | Tools |
|-------|-------|
| **Data Ingestion** | Kafka, CDC (Debezium) |
| **Storage** | Data Lake (S3, ADLS), Data Warehouse (Snowflake, BigQuery) |
| **Transformation** | dbt, Apache Spark |
| **Catalog** | DataHub, Collibra |
| **Quality** | Great Expectations, dbt tests |
| **Governance** | Apache Atlas, open-metadata |

## When to Use

- Multiple teams producing/consuming data
- Data quality issues affecting analytics
- Centralized team is bottleneck
- Organization > 50 engineers
- Diverse data domains (payments, orders, inventory)

## When NOT to Use

- Single small team
- Simple data warehouse
- All data in one system
- Low data complexity

## Benefits vs Challenges

| Benefits | Challenges |
|----------|---|
| Decentralized ownership | Governance complexity |
| Faster time-to-value | Requires cultural shift |
| Better data quality | More tools to maintain |
| Scalable | Steep learning curve |

## References

- [Data Mesh Learning](https://datamesh-learning.com/)
- [Data Mesh Thinking](https://martinfowler.com/articles/data-monolith-to-mesh.html)
- [Zhamak Dehghani's Articles](https://martinfowler.com/articles/data-mesh-principles.html)
- [dbt Documentation](https://docs.getdbt.com/)
- [Great Expectations](https://greatexpectations.io/)

---

**Key Takeaway**: Distribute data ownership to domain teams. Each domain owns its data as a product with APIs, quality metrics, and SLAs. Platform provides self-serve tools.
