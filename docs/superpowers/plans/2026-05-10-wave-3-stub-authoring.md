# Wave 3 Stub Authoring (55 docs) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Author all 55 Proposed-status stub documents across 8 catalog domains, advancing each from Proposed → Draft with full content per the enterprise architecture catalog Definition of Done.

**Architecture:** Each pattern document follows the canonical 15-section template. All Mermaid diagrams validated with `scripts/mermaid-lint-doc.sh`. 3-ring compliance rows validated with `scripts/check-compliance-rows.py`. Catalog status updated from Proposed → Draft in `governance/standards/enterprise-architecture-catalog.md` at each sub-wave gate.

**Tech Stack:** Java 21 / Spring Boot 3.x / Resilience4j / Spring Kafka / Apache Camel / PostgreSQL 16 / TimescaleDB / Trino / Kubernetes 1.29 / React 18 + TypeScript 5 / Swift 5.10 / Android Kotlin 2.x / Debezium / OPA/Rego / HashiCorp Vault

**Ring 2 rule:** Any Vietnamese regulation (SBV Circular 09/2020, Decree 13/2023) cited in Ring 2 must end: `⚠️ (working summary — pending Legal review)`

**Sub-wave map:**
| Wave | Docs | Domain |
|------|------|--------|
| 3A | 10 | DATA-004 – DATA-013 |
| 3B | 13 | INT-005 – INT-009, SEC-006 – SEC-013 |
| 3C | 12 | FE-001 – FE-006, MOB-001 – MOB-006 |
| 3D | 13 | BSP-001 – BSP-005, REF-005 – REF-012 |
| 3E | 7 | COMP-002 – COMP-008 |
| **Total** | **55** | |

---

## Task 0: Pre-Flight Verification

**Files:** none modified

- [ ] **Step 1: Confirm tooling**

```bash
which mmdc && mmdc --version
python3 --version
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/data/cqrs-pattern.md
python3 scripts/check-compliance-rows.py
```
Expected: mmdc present; lint exits 0; compliance script prints PASS or lists only Proposed files.

- [ ] **Step 2: Count Proposed stubs**

```bash
grep -c "| Proposed |" governance/standards/enterprise-architecture-catalog.md
```
Expected: `55`

- [ ] **Step 3: Confirm working tree clean**

```bash
git status --short
```
Expected: empty output.

---

## Wave 3A — Data Patterns

---

### Task 1: DATA-004 Data Vault 2.0

**Files:**
- Modify: `knowledge-base/patterns/data/data-vault-2.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/data/data-vault-2.md
```
Expected: `Status: Proposed | Target Wave: 2 | Owner: @data-platform-domain-owner`

- [ ] **Step 2: Write full document**

Replace the entire file with:

```markdown
# Data Vault 2.0

Status: Draft | Catalog ID: DATA-004 | Owner: @data-platform-domain-owner
Tier Applicability: T2, T3

## Problem Statement

- Regulatory audit trails require full history of master-data changes; star-schema
  dimensions overwrite history on UPDATE, destroying point-in-time replay.
- Tightly coupled fact-dimension schemas break downstream consumers every time a
  source system adds or renames a field.
- BCBS 239 §3 demands traceable lineage from raw source to aggregated report;
  traditional DW models cannot prove which source row produced which KPI value.
- Multiple upstream systems use different natural keys for the same business
  entity (e.g., customer); reconciliation requires a canonical hub.

## Context

Applicable when building an enterprise data warehouse that must satisfy audit
requirements, survive schema evolution without breakage, and integrate data from
heterogeneous source systems. Less suitable for OLTP or latency-sensitive reads.

## Solution

```mermaid
graph TB
    subgraph Raw Vault
        HUB_CUST[HUB_CUSTOMER\nbiz_key, load_date, rec_src]
        LNK_ACCT[LNK_CUST_ACCOUNT\nhash_key, load_date]
        SAT_CUST[SAT_CUSTOMER_DETAIL\nhash_key, load_date,\nload_end_date, name, email]
        HUB_ACCT[HUB_ACCOUNT\nbiz_key, load_date, rec_src]
        SAT_ACCT[SAT_ACCOUNT_BALANCE\nhash_key, load_date,\nload_end_date, balance]
    end
    HUB_CUST --> LNK_ACCT --> HUB_ACCT
    HUB_CUST --> SAT_CUST
    HUB_ACCT --> SAT_ACCT
    subgraph Business Vault
        BV[Bridge / PIT tables\nPoint-in-time snapshots]
    end
    Raw Vault --> BV
```

Hubs store the business key + metadata only. Links store foreign-key relationships
between hubs. Satellites store all descriptive attributes with load and end dates
for historisation. All keys are SHA-256 hashes of the business key.

## Implementation Guidelines

```java
// Spring Data JPA entity for a Data Vault Hub
@Entity
@Table(name = "HUB_CUSTOMER")
public class HubCustomer {
    @Id
    @Column(name = "HASH_KEY", length = 64)
    private String hashKey; // SHA-256(biz_key + rec_src)

    @Column(name = "BIZ_KEY", nullable = false)
    private String bizKey;

    @Column(name = "LOAD_DATE", nullable = false)
    private Instant loadDate;

    @Column(name = "REC_SRC", length = 50, nullable = false)
    private String recSrc;

    // computed on insert, never updated
    @PrePersist
    void computeKey() {
        this.hashKey = DigestUtils.sha256Hex(bizKey + "|" + recSrc);
    }
}
```

Load satellites via append-only inserts; set `load_end_date` on the prior row
before inserting the new row inside a single transaction.

## When to Use

- Enterprise DW requiring full audit lineage for BCBS 239 / GDPR / regulatory reporting.
- Integration hub receiving data from 5+ heterogeneous source systems.
- Schema evolution expected regularly without breaking downstream consumers.

## When NOT to Use

- Operational OLTP systems (Hub-Link-Satellite overhead is too high).
- Small DW with stable schema and no audit requirement (star schema is simpler).
- Sub-second query SLAs (use a Business Vault / Information Mart on top).

## Variants

- **Raw Vault + Business Vault**: Add derived satellite and bridge tables for query
  performance without losing raw history.
- **Ensemble Modelling**: Mix Data Vault hubs with relational tables for domains
  where full historisation is not required.

## NFR Acceptance Criteria

| Metric | T2 | T3 |
|--------|----|----|
| Satellite insert p99 latency | ≤ 50 ms | ≤ 100 ms |
| Point-in-time query p99 (PIT table) | ≤ 500 ms | ≤ 2 s |
| Hub load throughput | ≥ 5,000 rows/s | ≥ 1,000 rows/s |
| Availability | ≥ 99.5 % | ≥ 99.0 % |
| RTO | ≤ 24 h | ≤ 48 h |
| RPO | ≤ 4 h | ≤ 8 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.12.4.3 | Audit log integrity via append-only satellite pattern |
| Ring 1 | BCBS 239 §3 | Lineage from source BIZ_KEY to aggregated KPI via Hub-Link-Satellite chain |
| Ring 2 | SBV Circ. 09/2020 §IV.2 | Data retention ≥ 5 years satisfied by load_date / load_end_date historisation ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Storage grows at ~3× vs star schema due to historisation; use PostgreSQL table
partitioning by `load_date` and archive cold partitions to S3 Glacier after 2 years.
Trino external tables allow querying archived data without restore cost.

## Threat Model

- **SQL injection via BIZ_KEY**: Parameterised queries only; hash key is computed
  server-side, never passed from client.
- **Satellite tampering**: Grant INSERT-only on satellite tables to ETL role;
  no UPDATE/DELETE permitted.
- **Hash collision**: SHA-256 collision probability negligible; document assumption
  in ADR if compliance auditor challenges.

## Operational Runbook Stub

1. **Duplicate hub key alert** → query `SELECT biz_key, COUNT(*) FROM HUB_CUSTOMER GROUP BY biz_key HAVING COUNT(*) > 1`; deduplicate by `load_date` ascending.
2. **Satellite load lag > 15 min** → check CDC pipeline (Debezium connector status via Kafka Connect REST API).
3. **PIT table stale** → re-run `CALL refresh_pit_customer()` stored procedure.
4. **Disk growth alert** → run partition archival job `scripts/archive_cold_partitions.sh`.

## Test Strategy Stub

- Unit: assert `HubCustomer.computeKey()` produces deterministic SHA-256 for same bizKey + recSrc.
- Integration: load 10,000 rows via ETL; verify satellite `load_end_date` chain has no gaps.
- Regression: run BCBS 239 lineage query; assert every KPI row traces to a hub record.

## Related Patterns

- [DATA-009 Data Lineage](data-lineage.md) — attach OpenLineage facets at each DV load stage.
- [DATA-003 Temporal Tables](temporal-tables.md) — alternative for single-source historisation.
- [INT-002 Transactional Outbox + CDC](../integration/cdc-outbox-pattern.md) — feeds the DV load pipeline.

## References

- Linstedt & Olschimke, *Building a Scalable Data Warehouse with Data Vault 2.0* (2016).
- BCBS 239 Principles for effective risk data aggregation (Jan 2013).

## Key Takeaway

Use Hub-Link-Satellite to decouple business keys from descriptive attributes;
every fact is append-only, giving full audit lineage without custom change-tracking.
```

- [ ] **Step 3: Lint Mermaid**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/data/data-vault-2.md
```
Expected: exits 0 with no FAIL lines.

- [ ] **Step 4: Compliance check** *(run after setting Status: Draft in the file)*

```bash
python3 scripts/check-compliance-rows.py
```
Expected: `PASS data-vault-2.md` in output (or no mention if script only reports failures).

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/data/data-vault-2.md
git commit -m "feat(catalog): DATA-004 Data Vault 2.0 — Wave 3A"
```

---

### Task 2: DATA-005 Slowly Changing Dimensions

**Files:**
- Modify: `knowledge-base/patterns/data/slowly-changing-dimensions.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/data/slowly-changing-dimensions.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Slowly Changing Dimensions (SCD Type 2)

Status: Draft | Catalog ID: DATA-005 | Owner: @data-platform-domain-owner
Tier Applicability: T2, T3

## Problem Statement

- Product, customer, and account master data changes over time; overwriting the
  current row loses historical state needed for point-in-time reporting.
- Regulatory reports (e.g., BCBS 239 data quality) require the exact attribute
  values that existed at a specific past date.
- Naive SCD Type 1 (overwrite) causes report reproducibility failures — the same
  report run on different dates produces different numbers.

## Context

Applicable to any dimension table in a relational DW where attribute history is
required. SCD Type 2 is the standard for banking DW dimensions (customer segment,
product category, account status).

## Solution

```mermaid
sequenceDiagram
    participant ETL
    participant DIM as DIM_CUSTOMER
    ETL->>DIM: SELECT WHERE biz_key=X AND is_current=TRUE
    DIM-->>ETL: existing row (row_id=42)
    ETL->>DIM: UPDATE SET is_current=FALSE, effective_to=NOW() WHERE row_id=42
    ETL->>DIM: INSERT new row (is_current=TRUE, effective_from=NOW(), effective_to=9999-12-31)
    DIM-->>ETL: new row_id=43
```

Each row carries `effective_from`, `effective_to` (sentinel `9999-12-31` for
current), and `is_current` boolean for fast filtering. Surrogate key is
auto-generated; business key is stored for JOIN.

## Implementation Guidelines

```sql
-- PostgreSQL DDL for SCD Type 2 customer dimension
CREATE TABLE dim_customer (
    surrogate_key   BIGSERIAL PRIMARY KEY,
    biz_key         VARCHAR(50)  NOT NULL,
    full_name       VARCHAR(200) NOT NULL,
    segment         VARCHAR(50)  NOT NULL,
    effective_from  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    effective_to    TIMESTAMPTZ  NOT NULL DEFAULT '9999-12-31',
    is_current      BOOLEAN      NOT NULL DEFAULT TRUE,
    load_date       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    rec_src         VARCHAR(50)  NOT NULL
);
CREATE INDEX idx_dim_cust_biz_current ON dim_customer(biz_key, is_current);
CREATE INDEX idx_dim_cust_pit ON dim_customer(biz_key, effective_from, effective_to);
```

Spring Batch ItemWriter: expire current row then insert new row in one transaction.

## When to Use

- DW dimensions where auditors need point-in-time attribute values.
- Customer segmentation history for campaign attribution analysis.
- Product pricing history for margin calculation.

## When NOT to Use

- High-churn attributes changing >100× per day (use SCD Type 6 mini-dimensions).
- OLTP tables — use temporal tables (DATA-003) instead.

## Variants

- **SCD Type 1**: Overwrite — use only for correction of data errors, not normal change.
- **SCD Type 6 (Hybrid)**: Combines Type 1 (current value column) + Type 2 (history rows) for
  fast current-state queries without filtering by `is_current`.

## NFR Acceptance Criteria

| Metric | T2 | T3 |
|--------|----|----|
| SCD expire+insert transaction p99 | ≤ 20 ms | ≤ 50 ms |
| Point-in-time lookup p99 | ≤ 100 ms | ≤ 300 ms |
| Throughput (rows/s per batch job) | ≥ 10,000 | ≥ 5,000 |
| Availability | ≥ 99.5 % | ≥ 99.0 % |
| RTO | ≤ 24 h | ≤ 48 h |
| RPO | ≤ 4 h | ≤ 8 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 8601 | `effective_from`/`effective_to` stored as ISO 8601 TIMESTAMPTZ |
| Ring 1 | BCBS 239 §4 | Point-in-time reproducibility: report as-of any past date via `effective_from ≤ T < effective_to` predicate |
| Ring 2 | SBV Circ. 09/2020 §IV.2 | 5-year retention satisfied by retaining all SCD Type 2 rows without physical delete ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Table grows at ~1.5–3× vs SCD Type 1 depending on churn rate. Partition by
`effective_from` year and archive rows with `effective_to < NOW() - 7 years` to
cold storage. Index on `(biz_key, is_current)` keeps current-state queries fast.

## Threat Model

- **Mass expire attack**: ETL service role granted UPDATE only on `is_current`,
  `effective_to` columns; cannot DELETE.
- **Future-date injection**: validate `effective_from` ≤ NOW() + 1 min at ETL layer.

## Operational Runbook Stub

1. **Duplicate current rows** → `SELECT biz_key, COUNT(*) FROM dim_customer WHERE is_current=TRUE GROUP BY biz_key HAVING COUNT(*)>1` — fix by setting the older row's `is_current=FALSE`.
2. **PIT query returns zero rows** → check sentinel date; ensure `effective_to` default is `9999-12-31 00:00:00+00`.
3. **Batch job lag** → check Spring Batch `BATCH_JOB_EXECUTION` table for FAILED status.

## Test Strategy Stub

- Unit: assert expire+insert produces exactly one `is_current=TRUE` row per biz_key.
- Integration: run batch for customer segment change; query `AS OF TIMESTAMP` returns old segment before change time, new segment after.
- Load: 1M dimension rows with 10% churn; verify p99 insert ≤ 20 ms.

## Related Patterns

- [DATA-004 Data Vault 2.0](data-vault-2.md) — alternative historisation strategy.
- [DATA-003 Temporal Tables](temporal-tables.md) — PostgreSQL native temporal support.
- [DATA-009 Data Lineage](data-lineage.md) — track which source triggered the SCD expire.

## References

- Kimball & Ross, *The Data Warehouse Toolkit* 3rd ed., Chapter 5.
- PostgreSQL documentation: Date/Time functions.

## Key Takeaway

Set `effective_to = 9999-12-31` as the open-ended sentinel; expire by closing it
and inserting a new row — never UPDATE the attribute columns of an existing row.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/data/slowly-changing-dimensions.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `slowly-changing-dimensions.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/data/slowly-changing-dimensions.md
git commit -m "feat(catalog): DATA-005 Slowly Changing Dimensions — Wave 3A"
```

---

### Task 3: DATA-006 Lambda Architecture

**Files:**
- Modify: `knowledge-base/patterns/data/lambda-architecture.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/data/lambda-architecture.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Lambda Architecture

Status: Draft | Catalog ID: DATA-006 | Owner: @data-platform-domain-owner
Tier Applicability: T2, T3

## Problem Statement

- Batch views (daily/hourly Spark jobs) are stale; business needs near-real-time
  dashboards for fraud and liquidity monitoring.
- Pure streaming systems cannot cheaply reprocess years of history when a bug
  is discovered in the aggregation logic.
- Serving layer must answer queries spanning both historical (corrected) and
  real-time (unconfirmed) data in a single result set.

## Context

Applicable when a data platform needs both batch-accurate historical views and
low-latency streaming views, and can accept the operational cost of maintaining
two processing paths. For simpler pipelines, prefer Kappa Architecture (DATA-007).

## Solution

```mermaid
graph TB
    SRC[Source Systems\nKafka topics] --> SPEED[Speed Layer\nFlink / Kafka Streams\np99 ≤ 500 ms]
    SRC --> BATCH[Batch Layer\nApache Spark on S3\ndaily / hourly]
    SPEED --> SERVING[Serving Layer\nTrino federation]
    BATCH --> SERVING
    SERVING --> APP[Applications\nDashboards / APIs]
```

The speed layer produces approximate, low-latency views. The batch layer
re-computes exact views periodically. The serving layer merges both, with
batch results taking precedence for historical windows.

## Implementation Guidelines

```java
// Flink speed-layer job: rolling 5-minute transaction sum per account
StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
DataStream<Transaction> txns = env
    .fromSource(kafkaSource, WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(5)),
                "kafka-txn-source");
txns.keyBy(Transaction::getAccountId)
    .window(TumblingEventTimeWindows.of(Time.minutes(5)))
    .aggregate(new TransactionSumAggregator())
    .addSink(redisSink); // speed layer result
```

Spark batch job: scheduled via Airflow, reads S3 Parquet, writes to Trino-queryable
Hive metastore table. Trino UNION ALL query merges speed (Redis) + batch (Hive).

## When to Use

- Dashboards requiring both real-time (< 1 min) and historically-corrected data.
- Regulatory reports that need re-runnable batch accuracy but near-real-time monitoring.

## When NOT to Use

- Pipelines where re-processing via stream replay is sufficient — use Kappa (DATA-007).
- Teams too small to operate two separate processing layers.

## Variants

- **Micro-batch Lambda**: Replace Flink with Spark Structured Streaming for unified codebase.
- **Lakehouse Lambda**: Use Apache Iceberg for both batch and serving; speed layer writes
  to Iceberg with short compaction cycles.

## NFR Acceptance Criteria

| Metric | T2 | T3 |
|--------|----|----|
| Speed layer end-to-end latency p99 | ≤ 500 ms | ≤ 2 s |
| Batch recomputation lag (after source) | ≤ 1 h | ≤ 4 h |
| Serving query p99 | ≤ 2 s | ≤ 10 s |
| Availability | ≥ 99.5 % | ≥ 99.0 % |
| RTO | ≤ 24 h | ≤ 48 h |
| RPO | ≤ 1 h | ≤ 4 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.12.1.3 | Capacity management: Flink parallelism and Spark executor sizing documented in runbook |
| Ring 1 | BCBS 239 §3 | Batch layer provides corrected, authoritative lineage; speed layer marked as preliminary in metadata |
| Ring 2 | SBV Circ. 09/2020 §IV.2 | 5-year retention: batch results stored on S3 with lifecycle policy ≥ 5 years ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Speed layer: 4–8 Flink task managers (4 vCPU / 8 GB each) ≈ $400–800/month on AWS.
Batch layer: EMR spot cluster, auto-terminates after job ≈ $200–400/month.
Serving: Trino on EKS, scale to zero on weekends.

## Threat Model

- **Poison pill in Kafka**: DLQ configured; Flink checkpoint restored on restart.
- **Batch S3 bucket misconfiguration**: Enforce S3 Block Public Access + bucket policy;
  encrypt with KMS CMK.

## Operational Runbook Stub

1. **Speed layer lag > 30 s** → check Flink job manager UI; scale task managers if backpressure.
2. **Batch job failure** → check Airflow DAG run; re-trigger with `airflow dags trigger lambda_batch`.
3. **Serving query timeout** → check Trino coordinator; reduce Flink result TTL to force batch fallback.

## Test Strategy Stub

- Unit: Flink windowed aggregation produces same result as Spark batch for identical input.
- Integration: inject 1,000 events; assert speed layer result matches batch result within 0.1%.
- Chaos: kill Flink job mid-window; verify checkpoint restores with no duplicate counts.

## Related Patterns

- [DATA-007 Kappa Architecture](kappa-architecture.md) — simpler single-layer alternative.
- [INT-005 Anti-Corruption Layer](../integration/anti-corruption-layer.md) — isolate source schema from pipeline.
- [OBS-005 Async Middleware Observability](../observability/async-middleware-observability.md) — monitor Kafka lag.

## References

- Marz & Warren, *Big Data* (Manning, 2015) — original Lambda pattern.
- Apache Flink documentation: Windowing.

## Key Takeaway

Lambda's speed + batch duality gives real-time freshness with batch correctness;
pay the operational cost only if Kappa replay cannot meet your latency SLA.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/data/lambda-architecture.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `lambda-architecture.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/data/lambda-architecture.md
git commit -m "feat(catalog): DATA-006 Lambda Architecture — Wave 3A"
```

---

### Task 4: DATA-007 Kappa Architecture

**Files:**
- Modify: `knowledge-base/patterns/data/kappa-architecture.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/data/kappa-architecture.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Kappa Architecture

Status: Draft | Catalog ID: DATA-007 | Owner: @data-platform-domain-owner
Tier Applicability: T2, T3

## Problem Statement

- Lambda Architecture requires maintaining two codebases (batch + stream) that
  must produce identical results — divergence bugs are common and hard to detect.
- Batch layer introduces multi-hour recomputation cycles; business wants corrections
  reflected in dashboards within minutes, not hours.
- Storage and compute costs double when running both Spark batch and Flink stream
  clusters for the same pipeline.

## Context

Applicable when Kafka log retention is sufficient to replay all history needed for
re-computation (typically 30–90 days). For longer historical reprocessing, Lambda
or a Lakehouse pattern is required.

## Solution

```mermaid
graph TB
    SRC[Source Systems] --> K[Kafka\nRetention: 90 days]
    K --> PROC[Stream Processor\nFlink / Kafka Streams\nstateful, replayable]
    PROC --> STORE[Output Store\nPostgreSQL / Redis / S3]
    STORE --> APP[Applications]
    subgraph Reprocessing
        K -->|replay from offset 0| PROC2[New version\nof Processor]
        PROC2 --> STORE2[Shadow table\n/ new topic]
    end
```

A single stream processor handles all computation. When logic changes, deploy a
new version reading from offset 0 into a shadow output; swap traffic when validated.

## Implementation Guidelines

```java
// Kafka Streams topology: windowed transaction sum with state store
StreamsBuilder builder = new StreamsBuilder();
builder.stream("transactions", Consumed.with(Serdes.String(), txnSerde))
    .groupByKey()
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
    .aggregate(
        () -> 0L,
        (key, txn, agg) -> agg + txn.getAmount(),
        Materialized.<String, Long, WindowStore<Bytes, byte[]>>as("txn-sum-store")
            .withValueSerde(Serdes.Long()))
    .toStream()
    .to("txn-sum-output", Produced.with(WindowedSerdes.timeWindowedSerdeFrom(String.class), Serdes.Long()));
```

Re-processing: deploy new topology with `application.id=txn-sum-v2`, reads from
earliest offset, writes to `txn-sum-v2-output`; after validation, switch consumers.

## When to Use

- Single streaming codebase is sufficient; historical replay fits within Kafka retention.
- Team is small and cannot afford dual pipeline maintenance overhead.
- Latency requirement is seconds to minutes (not milliseconds).

## When NOT to Use

- Historical reprocessing requires data older than Kafka retention (use Lambda).
- Queries span multi-year history requiring full table scans (use Lakehouse / DV).

## Variants

- **Flink Kappa**: Flink with RocksDB state backend for large state; more expressive windowing.
- **Lakehouse Kappa**: Iceberg tables as output; unified batch/streaming with table format.

## NFR Acceptance Criteria

| Metric | T2 | T3 |
|--------|----|----|
| End-to-end stream latency p99 | ≤ 1 s | ≤ 5 s |
| Reprocessing throughput | ≥ 100,000 events/s | ≥ 20,000 events/s |
| Consumer lag steady-state | ≤ 5,000 messages | ≤ 50,000 messages |
| Availability | ≥ 99.5 % | ≥ 99.0 % |
| RTO | ≤ 4 h | ≤ 24 h |
| RPO | ≤ 1 h | ≤ 4 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.12.3.1 | Kafka topic data backed up via MirrorMaker 2 to secondary region |
| Ring 1 | BCBS 239 §3 | Reprocessing from Kafka offset 0 provides deterministic lineage replay |
| Ring 2 | SBV Circ. 09/2020 §IV.2 | Kafka retention ≥ 90 days + S3 archival for 5-year requirement ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Flink cluster: 4 task managers (4 vCPU / 16 GB) ≈ $600/month; eliminate Spark EMR
cluster saving ~$400/month vs Lambda. Kafka storage: 90-day retention for 10 TB/day
source ≈ $900/month on MSK; compress with LZ4.

## Threat Model

- **Replay amplification**: consumer group `application.id` must be unique per
  reprocessing run to avoid re-delivering to downstream consumers prematurely.
- **State store corruption**: enable Kafka Streams standby replicas (num.standby.replicas=1).

## Operational Runbook Stub

1. **Consumer lag growing** → scale Kafka Streams instances horizontally (EKS HPA on custom lag metric).
2. **State store rebuild slow on restart** → increase standby replicas; pre-warm with latest changelog.
3. **Reprocessing deployment** → use blue/green: new `application.id` + shadow topic; validate then cut over.

## Test Strategy Stub

- Unit: Kafka Streams TopologyTestDriver verifies aggregate output for 100 synthetic events.
- Integration: replay 1M historical events; assert output matches Lambda batch result within 0.01%.
- Chaos: kill 1 of 4 stream processors; verify lag recovers within 60 s via rebalance.

## Related Patterns

- [DATA-006 Lambda Architecture](lambda-architecture.md) — more complex dual-layer alternative.
- [INT-002 Transactional Outbox + CDC](../integration/cdc-outbox-pattern.md) — source event feed.
- [OBS-005 Async Middleware Observability](../observability/async-middleware-observability.md) — Kafka lag monitoring.

## References

- Kreps, Jay. "Questioning the Lambda Architecture" (O'Reilly, 2014).
- Apache Kafka Streams documentation: State Stores.

## Key Takeaway

Kappa simplifies to one streaming codebase; replay from Kafka offset 0 to reprocess —
only choose Lambda when history exceeds Kafka retention.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/data/kappa-architecture.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `kappa-architecture.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/data/kappa-architecture.md
git commit -m "feat(catalog): DATA-007 Kappa Architecture — Wave 3A"
```

---

### Task 5: DATA-008 Change Data Capture (General)

**Files:**
- Modify: `knowledge-base/patterns/data/change-data-capture.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/data/change-data-capture.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Change Data Capture (General)

Status: Draft | Catalog ID: DATA-008 | Owner: @tech-lead-backend
Tier Applicability: T0, T1

## Problem Statement

- Polling-based data sync (SELECT WHERE updated_at > last_run) misses deletes,
  puts heavy read load on source DB, and fails to detect mid-second changes.
- Outbox-only pattern (INT-002) handles new application writes but cannot capture
  changes from legacy batch processes that bypass the application layer.
- BCBS 239 §3 requires demonstrable, traceable data flow from source system to
  downstream consumers; ad-hoc scripts break this lineage.
- Schema changes in source DB (column add/rename) need to propagate without
  manual intervention and without downtime to downstream consumers.

## Context

Applicable when you need a reliable, low-latency change feed from a relational
database without modifying the source application. Complements INT-002 (Outbox
+ CDC) for application-controlled writes. This document covers the general CDC
pattern; see INT-002 for the outbox-specific variant.

## Solution

```mermaid
sequenceDiagram
    participant PG as PostgreSQL\n(WAL)
    participant DEB as Debezium\nConnector
    participant K as Kafka Topic\n(cdc.schema.table)
    participant CONS as Downstream\nConsumer
    PG->>DEB: WAL replication slot\n(logical decoding)
    DEB->>K: INSERT / UPDATE / DELETE\nevents as JSON
    K->>CONS: at-least-once delivery
    CONS->>CONS: idempotent upsert\nvia composite PK
```

Debezium reads PostgreSQL's Write-Ahead Log via a logical replication slot,
transforms row-level changes into CloudEvents-compatible JSON, and publishes to
Kafka. Consumers apply idempotent upserts using the primary key.

## Implementation Guidelines

```json
// Kafka Connect Debezium PostgreSQL connector config
{
  "name": "pg-accounts-cdc",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "pg-primary.internal",
    "database.port": "5432",
    "database.user": "debezium",
    "database.password": "${file:/secrets/debezium.properties:password}",
    "database.dbname": "corebanking",
    "database.server.name": "corebanking",
    "table.include.list": "public.accounts,public.transactions",
    "plugin.name": "pgoutput",
    "slot.name": "debezium_accounts",
    "publication.name": "debezium_pub",
    "heartbeat.interval.ms": "5000",
    "transforms": "unwrap",
    "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
    "transforms.unwrap.drop.tombstones": "false"
  }
}
```

Grant: `CREATE ROLE debezium REPLICATION LOGIN; GRANT SELECT ON ALL TABLES IN SCHEMA public TO debezium;`

## When to Use

- Capturing changes from legacy systems that bypass application-layer events.
- Data lake / DW ingestion requiring full INSERT + UPDATE + DELETE feed.
- Cross-service cache invalidation driven by authoritative DB changes.

## When NOT to Use

- Source DB cannot support logical replication (some managed DB tiers restrict it).
- Source system owns its own event bus — use that instead of CDC overhead.

## Variants

- **Debezium Outbox Routing**: combined with INT-002; Debezium reads the outbox
  table only, avoiding wide schema capture.
- **AWS DMS CDC**: for RDS/Aurora; trades flexibility for managed operation.

## NFR Acceptance Criteria

| Metric | T0 | T1 |
|--------|----|----|
| CDC event latency (WAL → Kafka) p99 | ≤ 200 ms | ≤ 500 ms |
| Throughput | ≥ 5,000 events/s | ≥ 1,000 events/s |
| Replication slot lag | ≤ 10 MB | ≤ 50 MB |
| Availability | ≥ 99.99 % | ≥ 99.9 % |
| RTO | ≤ 4 h | ≤ 8 h |
| RPO | ≤ 15 min | ≤ 1 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.12.4.1 | CDC events logged to Kafka with consumer-group audit trail |
| Ring 1 | BCBS 239 §3 | Debezium source connector metadata (server name, LSN, txID) provides immutable lineage |
| Ring 2 | SBV Circ. 09/2020 §IV.2 | Kafka topic retention ≥ 5 years via S3 archival tier ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Debezium runs as a Kafka Connect worker (2 vCPU / 4 GB per connector) ≈ $80/month
on EKS. Replication slot holds WAL until consumer ACKs — monitor slot lag to avoid
disk exhaustion ($0.10/GB overage on RDS); set `max_slot_wal_keep_size = 2GB`.

## Threat Model

- **Replication slot leak**: if connector dies and slot is not cleaned up, WAL
  accumulates indefinitely. Alert on `pg_replication_slots.wal_status = 'lost'`.
- **PII in CDC events**: mask sensitive columns in Debezium SMT (Single Message
  Transform) before publishing to Kafka.

## Operational Runbook Stub

1. **Slot lag > 500 MB** → check connector health via Kafka Connect REST; restart if FAILED.
2. **Connector PAUSED** → `curl -X PUT .../connectors/pg-accounts-cdc/resume`.
3. **Schema change breaks consumer** → use Confluent Schema Registry with FORWARD compatibility; consumers ignore unknown fields.
4. **Snapshot needed** → set `snapshot.mode=initial_only` and restart connector.

## Test Strategy Stub

- Integration: INSERT a row; assert CDC event arrives on Kafka within 200 ms with correct before/after fields.
- Contract: Schema Registry compatibility check passes for each new connector deployment.
- Chaos: kill Debezium pod; restart; assert no events lost (LSN-based replay).

## Related Patterns

- [INT-002 Transactional Outbox + CDC](../integration/cdc-outbox-pattern.md) — preferred for application-controlled writes.
- [DATA-009 Data Lineage](data-lineage.md) — attach OpenLineage facets to CDC events.
- [OBS-002 Distributed Trace Propagation](../observability/distributed-trace-propagation.md) — propagate trace context through CDC events.

## References

- Debezium documentation: PostgreSQL connector.
- Kleppmann, *Designing Data-Intensive Applications* (O'Reilly), Chapter 11.

## Key Takeaway

Debezium + PostgreSQL logical replication slot gives sub-200 ms, schema-aware
change feed without polling — monitor replication slot lag to prevent WAL
accumulation from filling the primary disk.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/data/change-data-capture.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `change-data-capture.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/data/change-data-capture.md
git commit -m "feat(catalog): DATA-008 Change Data Capture — Wave 3A"
```

---

### Task 6: DATA-009 Data Lineage

**Files:**
- Modify: `knowledge-base/patterns/data/data-lineage.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/data/data-lineage.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Data Lineage

Status: Draft | Catalog ID: DATA-009 | Owner: @data-platform-domain-owner
Tier Applicability: T1, T2

## Problem Statement

- BCBS 239 §3 requires demonstrable, end-to-end lineage from raw source data to
  aggregated risk KPIs; manual documentation is stale within weeks.
- When a bug corrupts a downstream report, engineers spend days tracing which
  upstream transformation introduced the error.
- Data consumers cannot assess the trustworthiness of a dataset without knowing
  its origin, transformation history, and freshness.
- Regulatory auditors request lineage maps; current answer is PowerPoint diagrams
  that do not match production pipelines.

## Context

Applicable to any data pipeline that feeds regulatory reporting, risk aggregation,
or financial reconciliation. OpenLineage is the open standard; Marquez is the
open-source lineage server.

## Solution

```mermaid
graph LR
    SRC[Source DB\nPostgreSQL] -->|Debezium CDC| K[Kafka Topic\ncdc.accounts]
    K -->|Flink Job\nOpenLineage emitter| DV[Data Vault\nSatellite]
    DV -->|Spark Transform\nOpenLineage emitter| DM[Data Mart\nTrino table]
    DM -->|Report Job\nOpenLineage emitter| REP[Regulatory Report\nBCBS 239 KPI]
    K & DV & DM & REP -->|lineage events| MAR[Marquez\nLineage Server]
```

Each processing step emits `RunEvent` (START/COMPLETE/FAIL) and `DatasetEvent`
facets to Marquez via the OpenLineage HTTP API. Marquez stores the directed acyclic
graph of dataset → job → dataset relationships.

## Implementation Guidelines

```java
// OpenLineage facet emitter in a Spring Batch ItemProcessor
@Component
public class AccountLineageEmitter {
    private final OpenLineage ol = new OpenLineage(URI.create("https://marquez.internal/api/v1"));

    public void emitStart(UUID runId, String inputTable, String outputTable) {
        OpenLineage.RunEvent event = ol.newRunEventBuilder()
            .eventType(OpenLineage.RunEvent.EventType.START)
            .run(ol.newRunBuilder().runId(runId).build())
            .job(ol.newJobBuilder().namespace("corebanking").name("account-dv-load").build())
            .inputs(List.of(ol.newInputDatasetBuilder()
                .namespace("postgresql://pg-primary:5432").name(inputTable).build()))
            .outputs(List.of(ol.newOutputDatasetBuilder()
                .namespace("postgresql://dw:5432").name(outputTable).build()))
            .build();
        new OpenLineageClient(ol).emit(event);
    }
}
```

## When to Use

- Pipelines feeding regulatory / audit reports.
- Any dataset consumed by more than 3 downstream processes.
- Post-incident root-cause investigations requiring impact analysis.

## When NOT to Use

- Simple ETL with 2 hops and a single team — lineage overhead outweighs benefit.
- Real-time OLTP — lineage server adds latency; use async emission instead.

## Variants

- **Column-level lineage**: OpenLineage `ColumnLineageDatasetFacet` traces individual
  column transformations; required for BCBS 239 field-level traceability.
- **dbt lineage**: dbt natively emits OpenLineage events via `dbt-ol` adapter.

## NFR Acceptance Criteria

| Metric | T1 | T2 |
|--------|----|----|
| Lineage event emission latency p99 | ≤ 100 ms (async) | ≤ 200 ms (async) |
| Marquez API query p99 | ≤ 500 ms | ≤ 1 s |
| Lineage graph completeness | 100 % of pipeline hops | ≥ 95 % |
| Availability | ≥ 99.9 % | ≥ 99.5 % |
| RTO | ≤ 8 h | ≤ 24 h |
| RPO | ≤ 1 h | ≤ 4 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.12.4.3 | Lineage server audit logs retained ≥ 1 year |
| Ring 1 | BCBS 239 §3 | OpenLineage DAG provides machine-readable lineage from raw source to aggregated KPI |
| Ring 2 | SBV Circ. 09/2020 §IV.2 | Marquez lineage records retained ≥ 5 years via PostgreSQL backup ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Marquez server: 2 vCPU / 4 GB + PostgreSQL 16 (db.t3.medium) ≈ $150/month.
OpenLineage emission is async fire-and-forget; adds < 5 ms to job start/end.

## Threat Model

- **Lineage server unavailable**: use async non-blocking HTTP client; job must not
  fail if Marquez is down — log warning, continue.
- **Sensitive table names in lineage graph**: Marquez access restricted to data
  engineers and compliance team via RBAC.

## Operational Runbook Stub

1. **Missing lineage for a job** → check job's `OpenLineageClient` configuration; verify `OPENLINEAGE_URL` env var.
2. **Marquez storage full** → run `VACUUM ANALYZE` on Marquez PostgreSQL; archive old runs via `marquez-pg-archive.sh`.
3. **Lineage graph shows unknown inputs** → add `InputDatasetFacet` to the emitter for that job.

## Test Strategy Stub

- Integration: run a Spark job with OpenLineage emitter; assert Marquez API returns the expected lineage graph.
- Contract: Marquez REST API schema version pinned in integration test; fail on breaking changes.
- BCBS 239 audit: automated script queries Marquez for all paths from `cdc.accounts` to BCBS KPI dataset; assert no gaps.

## Related Patterns

- [DATA-004 Data Vault 2.0](data-vault-2.md) — DV Hub-Link-Satellite provides structural lineage foundation.
- [DATA-008 CDC](change-data-capture.md) — source events carry Debezium LSN as provenance.
- [OBS-002 Distributed Trace Propagation](../observability/distributed-trace-propagation.md) — correlate lineage run IDs with trace IDs.

## References

- OpenLineage specification: https://openlineage.io/spec
- BCBS 239, Principle 3: Accuracy and Integrity.

## Key Takeaway

Emit OpenLineage `RunEvent` at START and COMPLETE in every pipeline job; Marquez
auto-builds the lineage DAG — BCBS 239 auditors get a live, machine-readable map.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/data/data-lineage.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `data-lineage.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/data/data-lineage.md
git commit -m "feat(catalog): DATA-009 Data Lineage — Wave 3A"
```

---

### Task 7: DATA-010 Time-Series Modelling

**Files:**
- Modify: `knowledge-base/patterns/data/time-series-modelling.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/data/time-series-modelling.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Time-Series Modelling

Status: Draft | Catalog ID: DATA-010 | Owner: @data-platform-domain-owner
Tier Applicability: T2, T3

## Problem Statement

- Vanilla PostgreSQL tables with 100M+ metric rows degrade to multi-second query
  latency for range scans; index bloat makes writes slow.
- SRE golden-signal dashboards require sub-second queries over 30-day windows at
  per-second granularity — billions of rows per metric.
- Cardinality explosion from high-dimensional labels (service × instance × endpoint)
  makes naive RDBMS storage impractical.
- Regulatory lookback windows (BCBS 239 risk metrics) require retention of raw
  time-series for 3–5 years with point-in-time queryability.

## Context

Applicable to metrics, IoT sensor data, financial tick data, and audit event
streams. TimescaleDB (PostgreSQL extension) is preferred when the team already
operates PostgreSQL and wants SQL query capability. InfluxDB or Prometheus TSDB
are alternatives for pure metrics.

## Solution

```mermaid
graph TB
    PROD[Producers\nmicrometer / OTel] --> K[Kafka\nmetrics topic]
    K --> INGEST[TimescaleDB Ingestor\nSpring Batch / Flink]
    INGEST --> TS[TimescaleDB\nHypertable\nautomatically partitioned\nby time chunk]
    TS --> CA[Continuous Aggregates\nhourly / daily rollups]
    CA --> DASH[Grafana Dashboard\nSQL queries]
    TS --> ARCH[S3 Archival\nchunks > 90 days\nvia tiered storage]
```

TimescaleDB `create_hypertable` partitions by time automatically into chunks
(default 7-day). Continuous aggregates pre-compute rollups incrementally.

## Implementation Guidelines

```sql
-- Create hypertable for service metrics
CREATE TABLE service_metrics (
    time        TIMESTAMPTZ NOT NULL,
    service     TEXT        NOT NULL,
    instance    TEXT        NOT NULL,
    metric_name TEXT        NOT NULL,
    value       DOUBLE PRECISION NOT NULL
);
SELECT create_hypertable('service_metrics', 'time',
    chunk_time_interval => INTERVAL '1 day',
    create_default_indexes => FALSE);
CREATE INDEX ON service_metrics (service, metric_name, time DESC);

-- Continuous aggregate: hourly p99 latency
CREATE MATERIALIZED VIEW service_metrics_hourly
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS bucket,
    service, metric_name,
    percentile_cont(0.99) WITHIN GROUP (ORDER BY value) AS p99
FROM service_metrics
GROUP BY bucket, service, metric_name;

SELECT add_continuous_aggregate_policy('service_metrics_hourly',
    start_offset => INTERVAL '3 hours', end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');
```

## When to Use

- Metrics, monitoring, or financial tick data at > 10,000 inserts/second.
- Team needs SQL queries and PostgreSQL operational familiarity.
- Multi-year retention with tiered cold storage.

## When NOT to Use

- Pure metrics with Prometheus-compatible exporters — use VictoriaMetrics or Thanos.
- Sparse event data with complex filtering — use Elasticsearch.

## Variants

- **TimescaleDB + Grafana**: standard SRE metrics stack.
- **TimescaleDB + Trino**: federate time-series with relational tables for regulatory reports.
- **InfluxDB**: higher write throughput but loses SQL compatibility.

## NFR Acceptance Criteria

| Metric | T2 | T3 |
|--------|----|----|
| Ingest throughput | ≥ 50,000 rows/s | ≥ 10,000 rows/s |
| Range query p99 (30-day window) | ≤ 2 s | ≤ 10 s |
| Continuous aggregate refresh lag | ≤ 1 h | ≤ 4 h |
| Availability | ≥ 99.5 % | ≥ 99.0 % |
| RTO | ≤ 24 h | ≤ 48 h |
| RPO | ≤ 1 h | ≤ 4 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.12.4.2 | TimescaleDB chunk compression + encryption at rest via PostgreSQL TDE |
| Ring 1 | BCBS 239 §6 | 5-year raw retention via tiered storage policy; continuous aggregates provide risk metric history |
| Ring 2 | SBV Circ. 09/2020 §IV.2 | Metric data classified as operational; retained per SBV data classification rules ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

TimescaleDB on RDS (db.r6g.2xlarge) ≈ $600/month. Enable native compression after
7 days (90%+ size reduction for time-series). Tiered S3 storage after 90 days at
$0.023/GB/month vs $0.10/GB/month for SSD.

## Threat Model

- **Unbounded cardinality**: reject metrics with > 100 unique label combinations
  per service at ingest; alert on cardinality growth.
- **Replication slot lag**: TimescaleDB streaming replication; monitor replica lag ≤ 30 s.

## Operational Runbook Stub

1. **Chunk compression stalled** → `SELECT * FROM chunk_compression_stats('service_metrics');` — re-run `compress_chunk()` manually.
2. **Continuous aggregate stale > 2 h** → check `timescaledb_information.continuous_aggregate_stats`; trigger `CALL refresh_continuous_aggregate(...)`.
3. **Disk usage spike** → run `SELECT * FROM chunks_detailed_size('service_metrics') ORDER BY total_bytes DESC LIMIT 10;` to identify hot chunks.

## Test Strategy Stub

- Load test: insert 50,000 rows/s for 10 min; assert p99 insert latency ≤ 5 ms.
- Query benchmark: range scan over 30 days; assert ≤ 2 s with continuous aggregate enabled.
- Compression: assert chunk compression ratio ≥ 85% after 7-day compression policy runs.

## Related Patterns

- [OBS-001 Golden Signals SRE](../../best-practices/golden-signals-sre.md) — consumes time-series metrics.
- [DATA-009 Data Lineage](data-lineage.md) — track metric pipeline transformations.
- [DATA-006 Lambda Architecture](lambda-architecture.md) — time-series as serving layer output.

## References

- TimescaleDB documentation: Hypertables and Continuous Aggregates.
- BCBS 239, Principle 6: Adaptability.

## Key Takeaway

`create_hypertable` + `add_continuous_aggregate_policy` turns PostgreSQL into a
high-throughput time-series store — enable native compression after 7 days to cut
storage cost by 90%.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/data/time-series-modelling.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `time-series-modelling.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/data/time-series-modelling.md
git commit -m "feat(catalog): DATA-010 Time-Series Modelling — Wave 3A"
```

---

### Task 8: DATA-011 Data Quality Rules

**Files:**
- Modify: `knowledge-base/patterns/data/data-quality-rules.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/data/data-quality-rules.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Data Quality Rules

Status: Draft | Catalog ID: DATA-011 | Owner: @data-platform-domain-owner
Tier Applicability: T1, T2

## Problem Statement

- Dirty data (null amounts, duplicate transaction IDs, invalid ISO 4217 currency
  codes) propagates silently through pipelines and surfaces as regulatory report
  errors discovered by auditors, not engineers.
- BCBS 239 §4 requires that risk data aggregation systems have a defined data
  quality framework with measurable scores published to senior management.
- Ad-hoc validation scripts differ per team; no consistent DQ score metric for
  dashboards or SLA alerting.
- Data quality failures at ingestion are cheaper to fix than at reporting;
  current architecture catches errors at the report stage only.

## Context

Applicable to any pipeline ingesting data for regulatory reporting, risk
aggregation, or financial reconciliation. Spring Validation handles per-record
checks; Great Expectations handles dataset-level statistical expectations.

## Solution

```mermaid
flowchart TB
    SRC[Source Record] --> VAL{Schema +\nBusiness Rules\nValidation}
    VAL -->|PASS| ENRICH[Enrichment\nPipeline]
    VAL -->|FAIL| Q[DQ Quarantine\nTable]
    ENRICH --> OUT[Downstream\nConsumers]
    Q --> ALERT[PagerDuty Alert\nif DQ score < 95%]
    Q --> DQ_DASH[DQ Dashboard\nGrafana]
```

Each record passes schema validation (non-null, type, range) then business
rule validation (referential integrity, business-key uniqueness). Failed records
go to a quarantine table; a DQ score (passing / total) is published as a Micrometer
gauge and visualised in Grafana.

## Implementation Guidelines

```java
// Spring Validation DTO with DQ rules
public record TransactionRequest(
    @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,  // ISO 4217
    @NotNull @Positive BigDecimal amount,
    @NotNull @Size(min = 1, max = 50) String endToEndId
) {}

// DQ score publisher
@Component
public class DqScorePublisher {
    private final MeterRegistry registry;
    private final AtomicLong passed = new AtomicLong();
    private final AtomicLong total  = new AtomicLong();

    public DqScorePublisher(MeterRegistry registry) {
        this.registry = registry;
        Gauge.builder("dq.score", this, p -> p.score())
             .tag("pipeline", "transactions")
             .register(registry);
    }

    public void record(boolean pass) {
        total.incrementAndGet();
        if (pass) passed.incrementAndGet();
    }

    private double score() {
        long t = total.get();
        return t == 0 ? 1.0 : (double) passed.get() / t;
    }
}
```

## When to Use

- Any pipeline feeding regulatory or financial reporting.
- Onboarding new data source where quality is unknown.
- Post-incident remediation requiring quarantine + reprocessing.

## When NOT to Use

- Simple passthrough APIs where source system guarantees validity.
- Ultra-low-latency paths (< 10 ms) — move validation async.

## Variants

- **Great Expectations**: statistical expectations (mean, std-dev, value-set) in
  addition to per-record rules; run as pipeline step in Airflow/Spark.
- **dbt tests**: SQL-based DQ assertions on warehouse tables.

## NFR Acceptance Criteria

| Metric | T1 | T2 |
|--------|----|----|
| Validation throughput p99 overhead | ≤ 5 ms/record | ≤ 10 ms/record |
| DQ score alert threshold | < 99 % triggers P2 | < 95 % triggers P3 |
| Quarantine table lag | ≤ 1 min | ≤ 5 min |
| Availability | ≥ 99.9 % | ≥ 99.5 % |
| RTO | ≤ 8 h | ≤ 24 h |
| RPO | ≤ 1 h | ≤ 4 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.12.2.1 | Validation controls protect against malformed data injection |
| Ring 1 | BCBS 239 §4 | DQ score metric published to Grafana; management dashboard updated daily |
| Ring 2 | SBV Circ. 09/2020 §IV.1 | Data accuracy requirements satisfied via per-field validation rules ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Validation adds < 5% CPU overhead on ingest workers. Quarantine table: PostgreSQL
partition by day; purge after 30 days (keep summary stats in TimescaleDB). Great
Expectations checkpoint runs hourly on Spark at $5–10/run.

## Threat Model

- **Validation bypass**: all records must pass through the validator; no direct
  DB insert path. Enforced via application-layer gateway, not DB constraints alone.
- **DQ score gaming**: quarantine table is append-only; DQ score computed from
  immutable counts.

## Operational Runbook Stub

1. **DQ score drops below threshold** → query quarantine table: `SELECT error_reason, COUNT(*) FROM dq_quarantine WHERE ts > NOW()-INTERVAL '1h' GROUP BY 1`.
2. **Quarantine table growing** → re-process quarantined records after upstream fix: `INSERT INTO transactions SELECT * FROM dq_quarantine WHERE resolved=TRUE`.
3. **Validation rule change** → bump schema version in Confluent Schema Registry; deploy new validator; quarantine old-schema records for triage.

## Test Strategy Stub

- Unit: assert each validation rule rejects the specific invalid input it targets.
- Integration: inject 1,000 records with 5% synthetic errors; assert DQ score = 0.95 ± 0.01.
- Regression: DQ score on production snapshot ≥ 99% (automated nightly check in CI).

## Related Patterns

- [DATA-009 Data Lineage](data-lineage.md) — emit DQ failure events as lineage FAIL.
- [INT-011 CloudEvents Envelope](../integration/cloudevents-envelope.md) — wrap DQ events in CloudEvents.
- [OBS-001 Golden Signals](../../best-practices/golden-signals-sre.md) — DQ score as a service-level indicator.

## References

- BCBS 239, Principle 4: Data Quality Framework.
- Great Expectations documentation: Expectations and Checkpoints.

## Key Takeaway

Publish a single `dq.score` Micrometer gauge per pipeline and alert below 99%;
quarantine-first lets you reprocess bad records after the upstream fix without
data loss.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/data/data-quality-rules.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `data-quality-rules.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/data/data-quality-rules.md
git commit -m "feat(catalog): DATA-011 Data Quality Rules — Wave 3A"
```

---

### Task 9: DATA-012 Data Virtualization

**Files:**
- Modify: `knowledge-base/patterns/data/data-virtualization.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/data/data-virtualization.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Data Virtualization

Status: Draft | Catalog ID: DATA-012 | Owner: @data-platform-domain-owner
Tier Applicability: T2, T3

## Problem Statement

- Cross-system queries (e.g., join PostgreSQL accounts with S3 Parquet transaction
  history) require expensive ETL copies; data is stale by the time the copy lands.
- Data copies proliferate across 5+ team-owned schemas; ownership is unclear and
  PII governance impossible to enforce consistently.
- Analysts need ad-hoc queries spanning 3 different source systems; provisioning
  a dedicated ETL pipeline for each request takes weeks.
- Storage duplication costs exceed $50k/year for data that is already available
  in source systems with adequate SLAs.

## Context

Applicable when read latency of seconds-to-minutes is acceptable (not sub-100 ms
OLTP). Trino (formerly PrestoSQL) is the federation engine; it queries source
systems directly using connectors without copying data.

## Solution

```mermaid
graph TB
    TRINO[Trino Coordinator\nQuery Planner]
    TRINO --> PG[PostgreSQL Connector\naccounts, customers]
    TRINO --> S3[Hive/Iceberg Connector\nS3 Parquet transactions]
    TRINO --> REDIS[Redis Connector\nfeature store]
    TRINO --> KAFKA[Kafka Connector\nlive events]
    ANALYST[Analysts / Reports\nSQL] --> TRINO
```

Trino splits the query into sub-queries per connector, executes in parallel, and
joins results in memory. No data movement for analytical queries.

## Implementation Guidelines

```sql
-- Trino cross-source query: accounts (PostgreSQL) × transactions (S3 Iceberg)
SELECT
    a.account_id,
    a.customer_name,
    SUM(t.amount)  AS total_volume_30d,
    COUNT(t.txn_id) AS txn_count_30d
FROM postgresql.corebanking.accounts  a
JOIN iceberg.datalake.transactions    t
    ON a.account_id = t.account_id
WHERE t.txn_date >= CURRENT_DATE - INTERVAL '30' DAY
GROUP BY a.account_id, a.customer_name
ORDER BY total_volume_30d DESC
LIMIT 100;
```

Trino catalog config (`etc/catalog/postgresql.properties`):
```properties
connector.name=postgresql
connection-url=jdbc:postgresql://pg-primary.internal:5432/corebanking
connection-user=${ENV:PG_USER}
connection-password=${ENV:PG_PASSWORD}
```

## When to Use

- Ad-hoc cross-system analytics where sub-minute latency is acceptable.
- Eliminating ETL copies for read-only analytical workloads.
- Regulatory reports joining 3+ source systems.

## When NOT to Use

- OLTP with sub-100 ms latency requirements — query the source directly.
- Workloads requiring DML (INSERT/UPDATE) on source systems.

## Variants

- **Iceberg REST Catalog**: unified table format across cloud storage; Trino reads
  Iceberg directly without Hive Metastore.
- **dbt + Trino**: dbt models on top of Trino federation for governed transformations.

## NFR Acceptance Criteria

| Metric | T2 | T3 |
|--------|----|----|
| Cross-source query p99 (< 10 GB scan) | ≤ 30 s | ≤ 120 s |
| Trino coordinator availability | ≥ 99.5 % | ≥ 99.0 % |
| Concurrent queries | ≥ 50 | ≥ 10 |
| RTO | ≤ 24 h | ≤ 48 h |
| RPO | N/A (stateless) | N/A |
| Worker node scale-out | ≤ 5 min | ≤ 15 min |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.9.4.1 | Trino access control via OPA plugin; column-level masking for PII |
| Ring 1 | BCBS 239 §3 | Trino query history logged to S3; lineage derived from query plan |
| Ring 2 | Decree 13/2023 Art. 18 | PII columns masked via Trino column masking rules for non-DPO roles ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Trino on EKS: 1 coordinator (4 vCPU / 16 GB) + 4–8 workers auto-scaled by
query queue depth. Workers scale to zero during off-hours saving ~60% vs always-on.
Eliminate 5 TB of ETL data copies = $500/month S3 storage saving.

## Threat Model

- **SQL injection via analyst queries**: Trino runs in read-only mode per catalog;
  no DML connectors in production.
- **Data exfiltration via cross-join**: enforce row-count limits (max_rows_per_query)
  and audit all queries to S3.

## Operational Runbook Stub

1. **Coordinator OOM** → increase JVM heap (`-Xmx24G`); check for Cartesian join in query plan.
2. **Worker eviction during long query** → enable fault-tolerant execution mode with S3 spill.
3. **Source DB connection exhausted** → configure `max-connections-per-node` per connector catalog.

## Test Strategy Stub

- Integration: run cross-source query; assert result matches ETL copy within 0.01%.
- Security: assert masked PII columns return `***` for non-DPO role.
- Performance: 10 concurrent analysts; assert no query degrades below 2× median.

## Related Patterns

- [DATA-010 Time-Series Modelling](time-series-modelling.md) — Trino queries TimescaleDB for long-range analytics.
- [DATA-009 Data Lineage](data-lineage.md) — emit Trino query plan as OpenLineage facet.
- [SEC-008 Data Masking](../security/data-masking.md) — Trino column masking for PII.

## References

- Trino documentation: Connectors and Security.
- Armbrust et al., "Lakehouse: A New Generation of Open Platforms" (CIDR 2021).

## Key Takeaway

Trino federates queries across PostgreSQL, S3, Kafka, and Redis without data
movement — apply OPA column masking to enforce PII governance at the query layer.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/data/data-virtualization.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `data-virtualization.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/data/data-virtualization.md
git commit -m "feat(catalog): DATA-012 Data Virtualization — Wave 3A"
```

---

### Task 10: DATA-013 Reference Data Master

**Files:**
- Modify: `knowledge-base/patterns/data/reference-data-master.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/data/reference-data-master.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Reference Data Master

Status: Draft | Catalog ID: DATA-013 | Owner: @data-platform-domain-owner
Tier Applicability: T0, T1

## Problem Statement

- Country codes, currency codes, branch codes, and product codes are duplicated
  in 12+ microservices; they diverge silently over time causing reconciliation
  failures.
- When the SBV adds a new currency code, engineers must update 12 services
  individually, leading to partial rollout windows where services disagree.
- BCBS 239 §6 requires consistent reference data across all risk aggregation
  systems; inconsistency is an audit finding.
- Services hardcode reference data in application config or in-memory maps;
  no audit trail for when a code was added or deprecated.

## Context

Applicable to any data element that is shared across multiple services and
changes infrequently but must be consistent (currency codes, country codes,
product categories, branch identifiers, GL account codes).

## Solution

```mermaid
graph TB
    RDS[Reference Data Service\nCRUD API + versioning] --> K[Kafka Topic\nreference-data-changed]
    K --> SVC_A[Service A\nlocal cache]
    K --> SVC_B[Service B\nlocal cache]
    K --> SVC_C[Service C\nlocal cache]
    ADMIN[Admin UI] --> RDS
    RDS --> PG[(PostgreSQL\nversioned reference table)]
```

A single Reference Data Service owns the canonical copy. Changes are published
as CloudEvents to a Kafka topic. Consumers maintain a local cache (Redis or
in-process) refreshed on `reference-data-changed` events. Services fall back to
the last known cache on Reference Data Service unavailability.

## Implementation Guidelines

```java
// Reference data service: versioned currency code entity
@Entity
@Table(name = "ref_currency")
public class RefCurrency {
    @Id private String code;          // ISO 4217, e.g. "VND"
    private String name;
    private int decimalPlaces;
    private boolean active;
    private int version;              // incremented on each change
    private Instant effectiveFrom;
    private Instant effectiveTo;      // null = current
}

// Event publisher on save
@Service
public class RefDataPublisher {
    private final KafkaTemplate<String, CloudEvent> kafka;

    public void publish(RefCurrency updated) {
        CloudEvent event = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withType("com.bank.refdata.currency.updated")
            .withSource(URI.create("/reference-data-service"))
            .withData("application/json", serialize(updated))
            .build();
        kafka.send("reference-data-changed", updated.getCode(), event);
    }
}
```

Consumer: `@KafkaListener` updates local `ConcurrentHashMap<String, RefCurrency>`.

## When to Use

- Any reference data set shared by ≥ 3 services.
- Reference data changes ≥ 1× per quarter requiring audit trail.
- Regulatory requirement for consistent cross-system reference data.

## When NOT to Use

- Reference data is truly static (compile-time constants) and changes require a
  code release anyway.
- Single-service reference data with no cross-service sharing.

## Variants

- **DB-backed cache with TTL**: services read directly from reference DB with
  a 5-minute cache; simpler but creates DB coupling.
- **GitOps reference data**: reference data as YAML in Git, deployed via CI/CD;
  suitable for very slow-changing data (country list).

## NFR Acceptance Criteria

| Metric | T0 | T1 |
|--------|----|----|
| Reference data read latency p99 (local cache) | ≤ 1 ms | ≤ 5 ms |
| Propagation lag after update (Kafka event) | ≤ 5 s | ≤ 30 s |
| Reference Data Service availability | ≥ 99.99 % | ≥ 99.9 % |
| RTO | ≤ 4 h | ≤ 8 h |
| RPO | ≤ 15 min | ≤ 1 h |
| Cache fallback on RDS outage | ≥ 1 h stale serve | ≥ 4 h stale serve |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.12.4.3 | Audit log of all reference data changes stored in PostgreSQL with immutable insert-only table |
| Ring 1 | BCBS 239 §6 | Single authoritative source + Kafka propagation ensures consistent reference data across all risk aggregation services |
| Ring 2 | SBV Circ. 09/2020 §IV.1 | Currency and branch codes conform to SBV published code lists; change log retained ≥ 5 years ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Reference Data Service: 2 replicas × 1 vCPU / 512 MB ≈ $30/month. Kafka topic:
low volume (< 100 events/day), negligible cost. Consumer caches eliminate repeated
DB calls — estimated 50,000 DB queries/day reduced to 0 per downstream service.

## Threat Model

- **Poisoned reference data**: admin UI requires 4-eyes approval workflow for any
  change; changes signed with admin JWT.
- **Cache poisoning via Kafka**: consumer validates CloudEvent signature before
  applying update.

## Operational Runbook Stub

1. **Reference data out of sync between services** → check Kafka consumer group lag: `kafka-consumer-groups.sh --describe --group ref-data-consumers`.
2. **Stale cache after service restart** → `GET /api/v1/reference-data/sync` on Reference Data Service triggers full cache refresh.
3. **Invalid code rejected by service** → verify code exists in Reference Data Service: `GET /api/v1/reference-data/currencies/{code}`.

## Test Strategy Stub

- Unit: assert local cache returns updated value within 5 s of Kafka event.
- Contract: consumer Pact contract against Reference Data Service REST API.
- Chaos: kill Reference Data Service; assert services continue serving from stale cache for ≥ 1 h.

## Related Patterns

- [INT-011 CloudEvents Envelope](../integration/cloudevents-envelope.md) — CloudEvents wrapping for reference data events.
- [DATA-011 Data Quality Rules](data-quality-rules.md) — validate incoming data against reference master.
- [INT-005 Anti-Corruption Layer](../integration/anti-corruption-layer.md) — translate external codes to internal reference codes.

## References

- BCBS 239, Principle 6: Adaptability.
- ISO 4217 Currency Codes (ISO.org).

## Key Takeaway

One service owns each reference dataset; publish changes as CloudEvents to Kafka
so all consumers stay consistent within 5 seconds — local caches survive Reference
Data Service outages for at least an hour.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/data/reference-data-master.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `reference-data-master.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/data/reference-data-master.md
git commit -m "feat(catalog): DATA-013 Reference Data Master — Wave 3A"
```

---

## Wave 3A Gate — Data Patterns (10 docs)

- [ ] **Step 1: Run batch lint**

```bash
for f in knowledge-base/patterns/data/data-vault-2.md \
          knowledge-base/patterns/data/slowly-changing-dimensions.md \
          knowledge-base/patterns/data/lambda-architecture.md \
          knowledge-base/patterns/data/kappa-architecture.md \
          knowledge-base/patterns/data/change-data-capture.md \
          knowledge-base/patterns/data/data-lineage.md \
          knowledge-base/patterns/data/time-series-modelling.md \
          knowledge-base/patterns/data/data-quality-rules.md \
          knowledge-base/patterns/data/data-virtualization.md \
          knowledge-base/patterns/data/reference-data-master.md; do
  bash scripts/mermaid-lint-doc.sh "$f" || echo "FAIL: $f"
done
```
Expected: 10 files, 0 FAIL lines.

- [ ] **Step 2: Run batch compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for any of the 10 DATA files.

- [ ] **Step 3: Update catalog status — DATA-004 through DATA-013**

In `governance/standards/enterprise-architecture-catalog.md`, change each of
the following rows from `Proposed` to `Draft`:
DATA-004, DATA-005, DATA-006, DATA-007, DATA-008, DATA-009, DATA-010,
DATA-011, DATA-012, DATA-013.

```bash
# Verify changes
grep -E "DATA-0(0[4-9]|1[0-3])" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |"
```
Expected: 10 lines, all showing `| Draft |`.

- [ ] **Step 4: Commit gate**

```bash
git add governance/standards/enterprise-architecture-catalog.md
git commit -m "chore(catalog): Wave 3A gate — DATA-004–013 promoted to Draft"
```

---

## Wave 3B — Integration + Security Patterns

---

### Task 11: INT-005 Anti-Corruption Layer

**Files:**
- Modify: `knowledge-base/patterns/integration/anti-corruption-layer.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/integration/anti-corruption-layer.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Anti-Corruption Layer

Status: Draft | Catalog ID: INT-005 | Owner: @tech-lead-backend
Tier Applicability: T0, T1, T2

## Problem Statement

- T24 core banking model (flat account structures, proprietary field names) leaks
  into new microservices via shared DTOs, coupling them to T24's internal model
  and making migration to a replacement core difficult.
- Upstream system domain concepts (e.g., T24's `COMPANY.CODE` field) have no
  direct equivalent in the new bounded context; naive mapping creates semantic
  inconsistencies.
- Teams evolve new services but must freeze their domain model to avoid breaking
  the shared T24 translation layer, slowing delivery.
- A bug in T24's data representation (e.g., amount encoding as string) propagates
  into 8 downstream services simultaneously.

## Context

Critical for any T24 modernisation or legacy integration where the new bounded
context must be protected from the legacy model. DDD Tactical Pattern — part of
the Context Map. Applied at the boundary between T24 (legacy context) and any
new microservice context.

## Solution

```mermaid
graph LR
    T24[T24 Core Banking\nLegacy Context] -->|OFS request/response\nor TAFJ API| ACL[Anti-Corruption\nLayer Adapter]
    ACL -->|Domain Event\nor REST DTO| SVC[New Microservice\nDomain Model]
    SVC -->|Command| ACL
    ACL -->|T24 OFS command| T24
```

The ACL adapter translates between T24's OFS (Open Financial Services) format
and the new service's domain model. Translation is bi-directional: inbound
(T24 → domain) and outbound (domain command → T24 OFS command).

## Implementation Guidelines

```java
// ACL adapter: translates T24 account response to domain Account
@Component
public class T24AccountAdapter {

    public Account fromT24(T24AccountResponse t24) {
        return Account.builder()
            .id(AccountId.of(t24.getAccountNumber().trim()))
            // T24 encodes amounts as strings with implicit decimal; e.g., "100050" = 1000.50 VND
            .balance(parseT24Amount(t24.getWorkingBalance(), t24.getCurrency()))
            .currency(Currency.of(t24.getCurrency()))
            .status(mapT24Status(t24.getAccountStatus()))
            .build();
    }

    private Money parseT24Amount(String raw, String ccy) {
        int decimals = CurrencyDecimalRegistry.decimalsFor(ccy);
        BigDecimal value = new BigDecimal(raw).movePointLeft(decimals);
        return Money.of(value, Currency.of(ccy));
    }

    private AccountStatus mapT24Status(String t24Status) {
        return switch (t24Status) {
            case "ACTIVE"    -> AccountStatus.ACTIVE;
            case "SUSPENDED" -> AccountStatus.FROZEN;
            case "CLOSED"    -> AccountStatus.CLOSED;
            default          -> AccountStatus.UNKNOWN;
        };
    }
}
```

## When to Use

- Integrating with legacy systems (T24, SAP) whose model must not leak into new services.
- Any context map boundary where upstream model quality is poor or uncontrolled.
- Strangler Fig migration (INT-006) — the ACL wraps the legacy calls during coexistence.

## When NOT to Use

- Internal service-to-service calls within the same bounded context.
- When the upstream model is already well-designed and stable (add a Shared Kernel instead).

## Variants

- **ACL as sidecar**: deploy the adapter as a sidecar container; service calls
  localhost; sidecar translates and forwards.
- **ACL as gateway**: single shared adapter service; adds deployment dependency
  but centralises translation logic.

## NFR Acceptance Criteria

| Metric | T0 | T1 | T2 |
|--------|----|----|-----|
| ACL translation latency p99 overhead | ≤ 5 ms | ≤ 10 ms | ≤ 20 ms |
| Availability | ≥ 99.99 % | ≥ 99.9 % | ≥ 99.5 % |
| RTO | ≤ 4 h | ≤ 8 h | ≤ 24 h |
| RPO | ≤ 15 min | ≤ 1 h | ≤ 4 h |
| Translation error rate | < 0.01 % | < 0.1 % | < 0.5 % |
| Unit test coverage of translation logic | ≥ 95 % | ≥ 90 % | ≥ 80 % |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.14.2.5 | ACL validates all T24 inputs against schema before passing to domain; prevents malformed data injection |
| Ring 1 | BCBS 239 §3 | Data lineage documented at ACL boundary: T24 field → domain field mapping recorded in OpenLineage facets |
| Ring 2 | SBV Circ. 09/2020 §IV | Integration with T24 (SBV-regulated core) mediated via ACL; no direct T24 DB access from new services ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

ACL is stateless; deploy as 2 replicas × 0.5 vCPU / 512 MB ≈ $15/month.
Translation logic is in-process (no network hop) when embedded in the service.

## Threat Model

- **T24 response injection**: validate all T24 fields against expected types and
  value sets before passing to domain model.
- **Mapping gap (UNKNOWN status)**: alert on any `AccountStatus.UNKNOWN` occurrence;
  fail fast rather than silently forward corrupt state.

## Operational Runbook Stub

1. **Translation error spike** → check ACL logs for `T24AccountAdapter` WARN; compare against T24 release notes for field format changes.
2. **T24 timeout** → ACL applies Resilience4j CircuitBreaker on T24 calls; fallback returns `ServiceUnavailableException` to caller.
3. **New T24 status code** → add case to `mapT24Status()` switch; unit test before deploy.

## Test Strategy Stub

- Unit: for each T24 status code, assert correct domain `AccountStatus` mapping.
- Unit: assert `parseT24Amount("100050", "VND")` = Money(1000.50, VND).
- Contract: Pact consumer contract test against T24 stub for all response fields used by ACL.
- Integration: call T24 sandbox; assert domain `Account` round-trips without information loss.

## Related Patterns

- [INT-006 Strangler Fig](strangler-fig.md) — ACL is the coexistence bridge during migration.
- [INT-002 Transactional Outbox + CDC](cdc-outbox-pattern.md) — captures T24 events via CDC at the ACL boundary.
- [DATA-013 Reference Data Master](../data/reference-data-master.md) — ACL resolves T24 branch/currency codes via reference master.

## References

- Evans, *Domain-Driven Design* (Addison-Wesley, 2003), Chapter 14: Maintaining Model Integrity.
- Fowler, "Anti-Corruption Layer" (martinfowler.com/eaaCatalog).

## Key Takeaway

The ACL is a translation firewall: every T24 field mapping has a unit test, every
unknown value raises an alert — the new bounded context must never know T24 exists.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/integration/anti-corruption-layer.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `anti-corruption-layer.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/integration/anti-corruption-layer.md
git commit -m "feat(catalog): INT-005 Anti-Corruption Layer — Wave 3B"
```

---

### Task 12: INT-006 Strangler Fig

**Files:**
- Modify: `knowledge-base/patterns/integration/strangler-fig.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/integration/strangler-fig.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Strangler Fig

Status: Draft | Catalog ID: INT-006 | Owner: @tech-lead-backend
Tier Applicability: T1, T2

## Problem Statement

- Big-bang replacement of T24 core banking is too risky; all-or-nothing cutovers
  have a 70%+ failure rate for core banking systems at this scale.
- New feature development is blocked on T24's release calendar; teams cannot
  ship independently while T24 owns the authoritative data.
- Running parallel systems (T24 + new) without a clear routing mechanism leads
  to data inconsistency between the two systems.
- Business cannot accept downtime for a single migration cutover event.

## Context

The Strangler Fig pattern (named after the strangler fig tree that grows around
a host tree and eventually replaces it) incrementally routes traffic from the
legacy system to new services, function by function, until the legacy can be
decommissioned. Applied in combination with INT-005 Anti-Corruption Layer.

## Solution

```mermaid
graph TB
    CLIENT[Client / Channel] --> GW[API Gateway\nSpring Cloud Gateway]
    GW -->|route: /accounts/* v1| T24[T24 Core Banking\nLegacy]
    GW -->|route: /accounts/* v2| NEW[New Account Service\nMicroservice]
    GW -->|route: /payments/*| PAYSVC[Payment Service\nMicroservice]
    T24 -->|CDC via Debezium| NEW
    subgraph Migration State
        T24 -.->|decommission when\ntraffic = 0| DECOMM[Decommissioned]
    end
```

Spring Cloud Gateway routes requests by path version prefix or feature flag.
During migration, the new service shadows T24 writes via CDC (INT-002) to stay
in sync. Traffic is shifted incrementally: 1% → 10% → 50% → 100%.

## Implementation Guidelines

```yaml
# Spring Cloud Gateway route configuration (application.yml)
spring:
  cloud:
    gateway:
      routes:
        # New Account Service — v2 routes
        - id: account-service-v2
          uri: http://account-service:8080
          predicates:
            - Path=/api/v2/accounts/**
          filters:
            - RewritePath=/api/v2/accounts/(?<segment>.*), /accounts/${segment}

        # T24 legacy — v1 routes (to be removed after migration)
        - id: t24-legacy
          uri: http://t24-gateway:9000
          predicates:
            - Path=/api/v1/accounts/**
          filters:
            - AddRequestHeader=X-Migration-Phase, strangler-active
```

Feature-flag-based canary: use GrowthBook flag `new-account-service-enabled`
to route a percentage of users to the new service.

## When to Use

- Incremental replacement of a monolith or legacy core system.
- When downtime for cutover is not acceptable.
- When the team needs to ship new features on the new stack before full migration.

## When NOT to Use

- Simple two-service splits where a single cutover is low-risk.
- When the legacy system cannot tolerate parallel operation (e.g., exclusive locks).

## Variants

- **Branch by abstraction**: replace calls to the old service with an interface
  that delegates to old or new implementation; swap at runtime via feature flag.
- **Data synchronisation Strangler**: new service writes to its own DB; CDC keeps
  T24 in sync as the system of record moves over time.

## NFR Acceptance Criteria

| Metric | T1 | T2 |
|--------|----|----|
| Gateway routing overhead p99 | ≤ 5 ms | ≤ 10 ms |
| Traffic shift granularity | 1% increments | 5% increments |
| Rollback time on regression | ≤ 5 min (route revert) | ≤ 15 min |
| Availability | ≥ 99.9 % | ≥ 99.5 % |
| RTO | ≤ 8 h | ≤ 24 h |
| RPO | ≤ 1 h | ≤ 4 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.14.2.9 | Gateway routing rules version-controlled in Git; changes require PR approval |
| Ring 1 | BCBS 239 §3 | During migration, both T24 and new service tagged with `X-Migration-Phase` header for lineage tracking |
| Ring 2 | SBV Circ. 09/2020 §IV | Core banking migration plan submitted to SBV IT department per regulatory notification requirements ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Spring Cloud Gateway: 2 replicas × 1 vCPU / 2 GB ≈ $60/month. Parallel operation
cost: new service + T24 running simultaneously until decommission; target ≤ 6
months of parallel operation per function to limit dual-run cost.

## Threat Model

- **Routing misconfiguration**: all route changes tested in staging with traffic
  mirroring before production.
- **Data drift between T24 and new service**: monitor CDC lag; halt traffic shift
  if lag > 30 s.

## Operational Runbook Stub

1. **New service returns 5xx spike** → revert route in Gateway: `kubectl apply -f gateway-routes-v1-only.yaml`.
2. **CDC lag growing during migration** → pause traffic shift; scale Debezium workers; resume after lag < 5 s.
3. **T24 and new service diverge on balance** → run reconciliation job `scripts/reconcile-accounts.sh`; escalate if delta > 0.

## Test Strategy Stub

- Shadow test: send 100% of production traffic to both T24 and new service; assert responses match within 0.01%.
- Canary: 1% traffic to new service for 24 h; assert error rate < 0.1% before increasing.
- Rollback drill: force 5xx from new service; assert gateway reverts within 5 min via alerting runbook.

## Related Patterns

- [INT-005 Anti-Corruption Layer](anti-corruption-layer.md) — wraps T24 calls during coexistence.
- [INT-002 Transactional Outbox + CDC](cdc-outbox-pattern.md) — keeps new service in sync with T24 during migration.
- [INT-003 API Gateway Routing](api-gateway-routing.md) — base routing infrastructure.

## References

- Fowler, "Strangler Fig Application" (martinfowler.com, 2004).
- Richardson, *Microservices Patterns* (Manning, 2018), Chapter 13.

## Key Takeaway

Route by version prefix at the gateway; shift 1% increments; run shadow traffic
comparison before each increment — rollback is a 5-minute route revert.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/integration/strangler-fig.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `strangler-fig.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/integration/strangler-fig.md
git commit -m "feat(catalog): INT-006 Strangler Fig — Wave 3B"
```

---

### Task 13: INT-007 Sidecar / Ambassador

**Files:**
- Modify: `knowledge-base/patterns/integration/sidecar-ambassador.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/integration/sidecar-ambassador.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Sidecar / Ambassador

Status: Draft | Catalog ID: INT-007 | Owner: @sre-lead
Tier Applicability: T0, T1

## Problem Statement

- mTLS certificate rotation, retry logic, circuit breaking, and distributed tracing
  are duplicated in every microservice; a bug in the retry implementation affects
  12 services simultaneously.
- Non-Java services (Python ML models, Node.js BFFs) cannot use the Spring
  Resilience4j library; they have inconsistent resilience behaviour.
- Legacy services cannot be modified to add observability without significant
  refactoring risk.
- Policy enforcement (e.g., all inter-service calls must use mTLS) requires
  auditing every service; no single enforcement point.

## Context

The Sidecar pattern deploys a helper container alongside the main application
container in the same Kubernetes Pod. The sidecar (Envoy proxy) handles all
network cross-cutting concerns; the main service communicates via `localhost`.
Istio service mesh automates sidecar injection.

## Solution

```mermaid
graph LR
    SVC[Application\nContainer\n:8080 localhost] <-->|localhost| SC[Envoy Sidecar\n:15001 outbound\n:15006 inbound]
    SC <-->|mTLS| REMOTE[Remote Service\n+ Its Sidecar]
    SC --> JAEGER[Jaeger\nTracing]
    SC --> PROM[Prometheus\nMetrics]
    ISTIO[Istiod\nControl Plane] -->|xDS config| SC
```

Istio injects the Envoy sidecar automatically via `istio-injection=enabled`
namespace label. Services communicate over plain HTTP to `localhost`; Envoy
upgrades to mTLS at the network boundary.

## Implementation Guidelines

```yaml
# Kubernetes namespace label to enable Istio sidecar injection
apiVersion: v1
kind: Namespace
metadata:
  name: payments
  labels:
    istio-injection: enabled
---
# DestinationRule: require mTLS for all services in namespace
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: payments-mtls
  namespace: payments
spec:
  host: "*.payments.svc.cluster.local"
  trafficPolicy:
    tls:
      mode: ISTIO_MUTUAL
---
# VirtualService: circuit breaker via Envoy outlier detection
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: account-service
  namespace: payments
spec:
  hosts: [account-service]
  http:
    - retries:
        attempts: 3
        retryOn: 5xx,gateway-error,connect-failure
        perTryTimeout: 2s
      route:
        - destination:
            host: account-service
```

## When to Use

- Kubernetes-based deployments where all services must conform to mTLS policy.
- Polyglot environments where not all services can use a shared SDK.
- Adding observability or security to services that cannot be modified.

## When NOT to Use

- Non-Kubernetes deployments (sidecar requires pod co-location).
- Ultra-low-latency paths where the localhost proxy hop adds unacceptable overhead.

## Variants

- **Ambassador**: sidecar handles outbound proxy only; simpler than full service
  mesh for API gateway patterns.
- **Dapr sidecar**: application-level sidecar for pub/sub, state, secrets — higher
  abstraction than Envoy.

## NFR Acceptance Criteria

| Metric | T0 | T1 |
|--------|----|----|
| Sidecar overhead p99 per hop | ≤ 1 ms | ≤ 2 ms |
| mTLS handshake overhead p99 | ≤ 5 ms (session reuse) | ≤ 10 ms |
| Control plane convergence (config push) | ≤ 30 s | ≤ 60 s |
| Availability | ≥ 99.99 % | ≥ 99.9 % |
| RTO | ≤ 4 h | ≤ 8 h |
| RPO | ≤ 15 min | ≤ 1 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | NIST SP 800-204A | Envoy sidecar enforces mutual TLS for all inter-service communication |
| Ring 1 | PCI-DSS 4.0 §4.2 | All cardholder data in transit encrypted via ISTIO_MUTUAL mTLS; Envoy handles certificate rotation |
| Ring 2 | SBV Circ. 09/2020 §III.2 | Inter-service encryption policy enforced at infrastructure layer; audit log of mTLS policy changes in Istiod ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Istio control plane (istiod): 2 vCPU / 4 GB ≈ $80/month. Envoy sidecar per pod:
0.1 vCPU / 128 MB overhead × 50 pods ≈ $120/month additional. Eliminates per-service
retry/mTLS library licensing and maintenance.

## Threat Model

- **Sidecar bypass**: enforce `PeerAuthentication` policy `STRICT` mode to reject
  non-mTLS traffic at the Envoy level.
- **Control plane compromise**: Istiod compromise can push malicious routing rules;
  restrict Istiod RBAC and sign xDS config.

## Operational Runbook Stub

1. **Service cannot reach remote** → `istioctl proxy-status` to check sidecar sync; `istioctl analyze` for policy conflicts.
2. **mTLS certificate expired** → Istio cert-manager auto-rotates every 24h; manual trigger: `kubectl rollout restart deploy/istiod -n istio-system`.
3. **High sidecar CPU** → check Envoy stats: `kubectl exec -it <pod> -c istio-proxy -- pilot-agent request GET /stats | grep upstream_rq`.

## Test Strategy Stub

- Integration: deploy two services in `istio-injection=enabled` namespace; assert plain HTTP is rejected with 503.
- Performance: measure p99 latency with and without sidecar on localhost loopback; assert overhead ≤ 1 ms.
- Chaos: revoke service certificate; assert 401 returned within 1 s.

## Related Patterns

- [SEC-001 mTLS Service Mesh](../security/mtls-service-mesh.md) — security policy built on the sidecar.
- [OBS-002 Distributed Trace Propagation](../observability/distributed-trace-propagation.md) — Envoy auto-propagates `traceparent` header.
- [INT-003 API Gateway Routing](api-gateway-routing.md) — Istio Ingress Gateway as entry point.

## References

- Istio documentation: Traffic Management and Security.
- Burns et al., *Kubernetes Patterns* (O'Reilly, 2019), Chapter 15: Sidecar.

## Key Takeaway

Label the namespace `istio-injection=enabled` and set `PeerAuthentication: STRICT`;
every pod gets Envoy mTLS, retries, and tracing without changing a line of
application code.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/integration/sidecar-ambassador.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `sidecar-ambassador.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/integration/sidecar-ambassador.md
git commit -m "feat(catalog): INT-007 Sidecar/Ambassador — Wave 3B"
```

---

### Task 14: INT-008 Backend-for-Frontend Routing

**Files:**
- Modify: `knowledge-base/patterns/integration/backend-for-frontend-routing.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/integration/backend-for-frontend-routing.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Backend-for-Frontend (BFF) Routing

Status: Draft | Catalog ID: INT-008 | Owner: @tech-lead-backend
Tier Applicability: T0, T1

## Problem Statement

- A single generic API serving mobile, web, and partner channels forces mobile
  clients to fetch 10× more data than needed (overfetch), increasing mobile
  data costs and battery usage for Vietnamese users on 4G.
- Web dashboard requires aggregated views (account + recent transactions + balance)
  in a single call; generic API requires 3 separate requests per page load,
  adding 400 ms round-trip penalty.
- Partner APIs need a stable versioned contract that evolves independently of
  internal service changes; coupling partners to internal API breaks them on
  any microservice refactor.
- Security policies differ per channel: mobile requires PKCE, web requires
  HttpOnly cookies, partners require mTLS client certificates.

## Context

The BFF pattern creates one backend aggregator per frontend channel. Each BFF
owns its own schema, security policy, and aggregation logic. This document
covers the routing and aggregation aspects; see SEC-005 for token-binding
security details.

## Solution

```mermaid
graph TB
    MOB[Mobile App\niOS / Android] --> MBFF[Mobile BFF\nSpring WebFlux\n:8081]
    WEB[Web SPA\nReact] --> WBFF[Web BFF\nSpring WebFlux\n:8082]
    PART[Partner\nTPP / FAPI] --> PBFF[Partner BFF\nSpring WebFlux\n:8083]
    MBFF & WBFF & PBFF --> ACCT[Account\nService]
    MBFF & WBFF & PBFF --> TXN[Transaction\nService]
    MBFF --> PUSH[Push Notification\nService]
    WBFF --> DASH[Dashboard\nAggregator]
```

Each BFF is a Spring WebFlux reactive aggregator. It calls downstream services
in parallel using `Mono.zip` / `Flux.merge`, assembles the channel-specific
response shape, and applies channel-specific security (token validation, rate limits).

## Implementation Guidelines

```java
// Mobile BFF: aggregate account summary in one call
@RestController
@RequestMapping("/mobile/v1/accounts")
public class MobileAccountController {

    @GetMapping("/{accountId}/summary")
    public Mono<MobileAccountSummary> getSummary(
            @PathVariable String accountId,
            @AuthenticationPrincipal Jwt jwt) {
        return Mono.zip(
            accountClient.getAccount(accountId),
            transactionClient.getRecent(accountId, 5),
            notificationClient.getUnreadCount(jwt.getSubject())
        ).map(tuple -> MobileAccountSummary.of(
            tuple.getT1(),   // Account
            tuple.getT2(),   // List<Transaction>
            tuple.getT3()    // int unreadCount
        ));
    }
}

// MobileAccountSummary — mobile-optimised shape (omits fields unused by mobile)
public record MobileAccountSummary(
    String accountId,
    String maskedAccountNumber,
    BigDecimal availableBalance,
    String currency,
    List<TransactionSummary> recentTransactions,
    int unreadNotifications
) {}
```

## When to Use

- Multiple channels (mobile, web, partner) with materially different response shapes.
- Channel-specific security policies that differ enough to warrant separate services.
- GraphQL is not desired (BFF is simpler for team skill set).

## When NOT to Use

- Single-channel API — add a BFF layer only when ≥ 2 channels diverge.
- When downstream services already provide channel-specific shapes.

## Variants

- **GraphQL BFF**: single BFF exposing GraphQL schema; channels query only the
  fields they need — eliminates per-channel BFF proliferation at the cost of
  GraphQL complexity.

## NFR Acceptance Criteria

| Metric | T0 | T1 |
|--------|----|----|
| BFF aggregation latency p99 (parallel calls) | ≤ 200 ms | ≤ 500 ms |
| BFF availability | ≥ 99.99 % | ≥ 99.9 % |
| Downstream timeout cascade prevention | Resilience4j timeout ≤ 1 s per call | ≤ 2 s |
| RTO | ≤ 4 h | ≤ 8 h |
| RPO | ≤ 15 min | ≤ 1 h |
| Concurrent requests per BFF instance | ≥ 1,000 (WebFlux non-blocking) | ≥ 500 |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.14.1.2 | BFF validates JWT claims per channel; no cross-channel token reuse |
| Ring 1 | PCI-DSS 4.0 §6.2 | Mobile BFF masks PAN to last-4 before returning to mobile client |
| Ring 2 | Decree 13/2023 Art. 13 | BFF logs access to personal data (account details, transaction history) for DSR audit ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

3 BFF services × 2 replicas × 0.5 vCPU / 512 MB ≈ $45/month total.
WebFlux non-blocking I/O handles 1,000+ concurrent requests per instance,
eliminating thread-per-request cost of blocking REST.

## Threat Model

- **JWT claim elevation**: validate `aud` claim matches BFF identifier; reject
  tokens issued for other services.
- **Downstream aggregation amplification**: cap parallel downstream calls at 10;
  circuit-break if any downstream returns 503.

## Operational Runbook Stub

1. **BFF latency spike** → check downstream service latencies in Jaeger; identify slowest call in `Mono.zip`.
2. **Mobile BFF 5xx spike** → check Resilience4j circuit breaker state: `/actuator/circuitbreakers`.
3. **Partner BFF mTLS auth failure** → verify client certificate CN matches partner registry entry.

## Test Strategy Stub

- Unit: mock all downstream services; assert `MobileAccountSummary` shape matches mobile OpenAPI contract.
- Integration: spin up downstream service stubs; assert parallel aggregation completes in ≤ 200 ms.
- Contract: Pact consumer contract for each BFF → downstream service pair.
- Security: assert mobile BFF rejects tokens with `aud=web-bff`.

## Related Patterns

- [SEC-005 BFF Token Binding](../security/bff-token-binding.md) — security complement to this pattern.
- [INT-003 API Gateway Routing](api-gateway-routing.md) — gateway routes to the correct BFF.
- [INT-007 Sidecar/Ambassador](sidecar-ambassador.md) — Envoy sidecar handles mTLS for partner BFF.

## References

- Richardson, Sam. "Pattern: Backends For Frontends" (microservices.io).
- Spring WebFlux documentation: Reactive Web.

## Key Takeaway

One BFF per channel with `Mono.zip` parallel aggregation; mobile gets a slim
response shape in one call — validate `aud` claim per BFF to prevent cross-channel
token reuse.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/integration/backend-for-frontend-routing.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `backend-for-frontend-routing.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/integration/backend-for-frontend-routing.md
git commit -m "feat(catalog): INT-008 Backend-for-Frontend Routing — Wave 3B"
```

---

### Task 15: INT-009 Content-Based Router

**Files:**
- Modify: `knowledge-base/patterns/integration/content-based-router.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/integration/content-based-router.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Content-Based Router

Status: Draft | Catalog ID: INT-009 | Owner: @tech-lead-backend
Tier Applicability: T0, T1

## Problem Statement

- Payment messages require different processing paths based on currency, amount
  threshold, and destination bank: domestic VND < 500M VND goes via NAPAS instant;
  FX or high-value goes via SWIFT; internal transfers bypass both.
- Routing logic is hardcoded in every consuming service, leading to divergence
  when routing rules change (e.g., NAPAS raises the 500M VND threshold).
- Adding a new payment rail (e.g., regional QR code scheme) requires changes in
  5 services simultaneously.
- No audit trail of which routing predicate was applied to each payment.

## Context

The Content-Based Router (CBR) is Enterprise Integration Pattern §4. Apache Camel
implements CBR natively with `choice().when().otherwise()`. The router is a
stateless, purely functional component that inspects message content and routes
to the correct channel.

## Solution

```mermaid
graph TB
    IN[Inbound Payment\nKafka topic] --> CBR{Content-Based\nRouter}
    CBR -->|currency=VND\nAND amount<500M| NAPAS[NAPAS\nInstant Channel]
    CBR -->|currency!=VND\nOR amount>=500M| SWIFT[SWIFT\nChannel]
    CBR -->|destinationBic\n=INTERNAL| INTERNAL[Internal\nLedger Channel]
    CBR -->|no match| DLQ[Dead Letter\nChannel]
```

The router evaluates predicates in priority order; first match wins. All routing
decisions are logged with the `routedTo` field for audit.

## Implementation Guidelines

```java
// Apache Camel Content-Based Router for payment routing
@Component
public class PaymentRouter extends RouteBuilder {

    @Override
    public void configure() {
        from("kafka:payments-inbound?brokers={{kafka.brokers}}")
            .routeId("payment-cbr")
            .log(LoggingLevel.INFO, "Routing payment ${header.endToEndId} amount=${body.amount} ccy=${body.currency}")
            .choice()
                .when(exchange -> isInternalTransfer(exchange))
                    .to("kafka:payments-internal")
                .when(exchange -> isNapasEligible(exchange))
                    .to("kafka:payments-napas")
                .when(exchange -> isSwiftEligible(exchange))
                    .to("kafka:payments-swift")
                .otherwise()
                    .log(LoggingLevel.ERROR, "No route matched for payment ${header.endToEndId}")
                    .to("kafka:payments-dlq")
            .end();
    }

    private boolean isInternalTransfer(Exchange ex) {
        Payment p = ex.getMessage().getBody(Payment.class);
        return p.getDestinationBic().startsWith("TCBVVNVX");
    }

    private boolean isNapasEligible(Exchange ex) {
        Payment p = ex.getMessage().getBody(Payment.class);
        return "VND".equals(p.getCurrency())
            && p.getAmount().compareTo(new BigDecimal("500000000")) < 0
            && isNapasDestination(p.getDestinationBic());
    }

    private boolean isSwiftEligible(Exchange ex) {
        Payment p = ex.getMessage().getBody(Payment.class);
        return !"VND".equals(p.getCurrency())
            || p.getAmount().compareTo(new BigDecimal("500000000")) >= 0;
    }
}
```

Routing rule thresholds externalised via `application.yml`; no code change needed
for threshold adjustments.

## When to Use

- Payment routing based on currency, amount, destination, or product type.
- Any message-driven system with ≥ 3 routing destinations.
- Centralising routing logic that currently lives scattered across consumers.

## When NOT to Use

- Simple 2-way if/else routing — use direct conditional in the consumer.
- Request/response REST APIs where routing is better done at the API gateway layer.

## Variants

- **Dynamic Router**: routing targets determined at runtime from a routing table
  stored in Redis; changes take effect without redeploy.
- **Recipient List**: route to multiple destinations simultaneously (broadcast
  to all matching channels).

## NFR Acceptance Criteria

| Metric | T0 | T1 |
|--------|----|----|
| Routing decision latency p99 | ≤ 5 ms | ≤ 10 ms |
| Routing throughput | ≥ 10,000 msg/s | ≥ 2,000 msg/s |
| DLQ rate (no-match) | < 0.001 % | < 0.01 % |
| Availability | ≥ 99.99 % | ≥ 99.9 % |
| RTO | ≤ 4 h | ≤ 8 h |
| RPO | ≤ 15 min | ≤ 1 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.12.4.1 | All routing decisions logged with endToEndId, amount, currency, and `routedTo` channel |
| Ring 1 | ISO 20022 pacs.008 | Router evaluates BIC-based predicates conforming to ISO 20022 agent identification |
| Ring 2 | SBV Circ. 09/2020 §IV.2 | NAPAS-eligible payments routed via SBV-approved domestic clearing channel; routing log retained ≥ 5 years ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Camel router: stateless, 2 replicas × 0.5 vCPU / 512 MB ≈ $30/month.
Routing rule changes via config map update — no rebuild cost. DLQ monitoring
via PagerDuty costs $0 (uses existing alerting infrastructure).

## Threat Model

- **Routing predicate bypass**: message structure validated against JSON schema
  before CBR evaluation; malformed messages rejected at Kafka consumer.
- **DLQ silent drop**: DLQ topic monitored; any DLQ message triggers P2 alert.

## Operational Runbook Stub

1. **DLQ messages accumulating** → inspect DLQ: `kafka-console-consumer.sh --topic payments-dlq --from-beginning --max-messages 10`; identify missing routing rule.
2. **Wrong channel routed** → check Camel route predicate logic for the affected payment type; adjust threshold in `application.yml` without redeploy.
3. **Router throughput degrading** → scale Camel pods; check Kafka partition count matches router instances.

## Test Strategy Stub

- Unit: for each routing predicate, assert correct channel for boundary values (e.g., exactly 500,000,000 VND).
- Unit: assert DLQ is reached for a payment with no matching predicate.
- Integration: publish 1,000 synthetic payments with mixed types; assert 0 DLQ, correct channel distribution.
- Regression: routing table snapshot tested on every CI build.

## Related Patterns

- [EIP Dead Letter Channel](../eip/dead-letter-channel.md) — DLQ handling for unroutable messages.
- [INT-002 Transactional Outbox + CDC](cdc-outbox-pattern.md) — source of payment events.
- [INT-012 Error Code Mapping](../observability/error-code-mapping.md) — map routing failures to canonical error codes.

## References

- Hohpe & Woolf, *Enterprise Integration Patterns* (Addison-Wesley, 2003), Chapter 8.
- Apache Camel documentation: Content-Based Router DSL.

## Key Takeaway

Externalise routing predicates to config; route to DLQ on no-match and alert
immediately — never silently drop a payment.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/integration/content-based-router.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `content-based-router.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/integration/content-based-router.md
git commit -m "feat(catalog): INT-009 Content-Based Router — Wave 3B"
```

---

### Task 16: SEC-006 JWT Best Practices

**Files:**
- Modify: `knowledge-base/patterns/security/jwt-best-practices.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/security/jwt-best-practices.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# JWT Best Practices

Status: Draft | Catalog ID: SEC-006 | Owner: @ciso-delegate
Tier Applicability: T0, T1, T2

## Problem Statement

- Services using `HS256` shared-secret JWTs — if the secret leaks from any one
  service, all tokens issued by the entire platform are forged-able.
- `exp` claim not validated in 3 legacy services; tokens remain valid indefinitely.
- JWKs endpoint not rotated on schedule; a compromised private key stays active
  for months.
- `aud` claim absent from tokens; tokens issued for Service A accepted by Service B.

## Context

RFC 7519 (JWT), RFC 7517 (JWK), and RFC 8725 (JWT Best Current Practices) define
the standards. Spring Security 6.x implements JWT validation via `JwtDecoder`.

## Solution

```mermaid
sequenceDiagram
    participant CLIENT
    participant AUTH as Auth Service\n(issues RS256 JWT)
    participant SVC as Downstream Service\n(validates via JWKs)
    CLIENT->>AUTH: authenticate (credentials)
    AUTH-->>CLIENT: JWT (RS256, exp=15min, aud=account-service)
    CLIENT->>SVC: Bearer JWT
    SVC->>AUTH: GET /.well-known/jwks.json (cached 5 min)
    AUTH-->>SVC: JWK Set (public keys)
    SVC->>SVC: verify sig, exp, aud, iss
    SVC-->>CLIENT: 200 OK
```

## Implementation Guidelines

```java
// Spring Security JWT configuration with RS256 and audience validation
@Configuration
@EnableWebSecurity
public class JwtSecurityConfig {

    @Bean
    public JwtDecoder jwtDecoder(@Value("${auth.jwks-uri}") String jwksUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
            .withJwkSetUri(jwksUri)
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build();

        // Validate exp, iss, and aud
        OAuth2TokenValidator<Jwt> validators = new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer("https://auth.bank.internal"),
            new JwtClaimValidator<List<String>>("aud",
                aud -> aud != null && aud.contains("account-service"))
        );
        decoder.setJwtValidator(validators);
        return decoder;
    }
}
```

JWK rotation: Auth service rotates keys every 30 days. Old key retained for
`grace_period=15min` to allow in-flight tokens to complete.

## When to Use

- All inter-service and client-to-service authentication.
- Any token issued with a validity window > 0 seconds.

## When NOT to Use

- Internal service-to-service calls within Istio mTLS mesh — use mTLS identity
  instead of JWT for machine-to-machine.

## Variants

- **DPoP (Demonstrating Proof-of-Possession)**: binds JWT to client's private key,
  preventing bearer token replay. Required for open banking (FAPI 2.0).
- **Introspection**: stateful token validation via auth server on each request;
  enables instant revocation at cost of latency.

## NFR Acceptance Criteria

| Metric | T0 | T1 | T2 |
|--------|----|----|-----|
| JWK cache TTL | 5 min | 5 min | 10 min |
| JWT validation overhead p99 | ≤ 2 ms | ≤ 5 ms | ≤ 10 ms |
| Key rotation interval | ≤ 30 days | ≤ 90 days | ≤ 180 days |
| Token max expiry | 15 min | 30 min | 60 min |
| Availability | ≥ 99.99 % | ≥ 99.9 % | ≥ 99.5 % |
| RTO | ≤ 4 h | ≤ 8 h | ≤ 24 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | RFC 8725 §3 | `alg=RS256`, `exp` enforced, `aud` validated per RFC 8725 best practices |
| Ring 1 | PCI-DSS 4.0 §8.6 | Service-to-service authentication via asymmetric JWT; no shared secrets for cardholder data systems |
| Ring 2 | SBV Circ. 09/2020 §III.2 | Access tokens for banking APIs expire in ≤ 30 minutes per SBV API security guidance ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

JWKs endpoint: 2 replicas of auth service already running; no additional cost.
JWK cache at 5 min TTL eliminates 99.9% of JWKs endpoint calls — 1,000 RPS service
generates ~2 JWKs fetches/min (cache refresh), not 1,000.

## Threat Model

- **Algorithm confusion (alg=none)**: reject any JWT with `alg=none` or symmetric
  algorithms in `JwtDecoder` configuration.
- **Token replay**: short `exp` (15 min) limits replay window; pair with
  SEC-011 Session Revocation for immediate invalidation.

## Operational Runbook Stub

1. **JWKs endpoint unreachable** → check auth service health; JWK cache serves stale keys for 5 min grace.
2. **401 spike after key rotation** → verify old key retained in JWKS for `grace_period`; add previous key to JWK set.
3. **`aud` validation failures** → confirm token issuer config includes correct `aud` value for the service.

## Test Strategy Stub

- Unit: assert `JwtDecoder` rejects tokens with `alg=none`, expired `exp`, wrong `aud`.
- Integration: issue RS256 token from auth service; validate in downstream service.
- Rotation: rotate JWK; assert in-flight tokens (< 15 min old) still accepted during grace period.

## Related Patterns

- [SEC-011 Session Revocation](session-revocation.md) — revoke tokens before `exp`.
- [SEC-005 BFF Token Binding](bff-token-binding.md) — bind JWT to client channel.
- [INT-008 BFF Routing](../integration/backend-for-frontend-routing.md) — BFF validates `aud` per channel.

## References

- RFC 8725: JSON Web Token Best Current Practices.
- Spring Security documentation: OAuth2 Resource Server.

## Key Takeaway

Use `RS256`, validate `exp` + `aud` + `iss`, rotate JWK every 30 days, keep `exp`
≤ 15 min — never use `HS256` in a multi-service deployment.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/security/jwt-best-practices.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `jwt-best-practices.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/security/jwt-best-practices.md
git commit -m "feat(catalog): SEC-006 JWT Best Practices — Wave 3B"
```

---

### Task 17: SEC-007 Secrets Rotation

**Files:**
- Modify: `knowledge-base/patterns/security/secrets-rotation.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/security/secrets-rotation.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Secrets Rotation

Status: Draft | Catalog ID: SEC-007 | Owner: @ciso-delegate
Tier Applicability: T0, T1, T2

## Problem Statement

- Long-lived database passwords (unchanged for 18+ months) provide large blast
  radius if leaked — attacker has persistent DB access until manually rotated.
- API keys for payment rails (NAPAS, SWIFT) are baked into Kubernetes ConfigMaps,
  visible to any developer with `kubectl get configmap` access.
- Manual rotation processes fail: a DB password rotation causes 2-hour outage
  when services are not restarted in the correct order.
- PCI-DSS §3.6 requires cryptographic key rotation within defined periods; no
  automated enforcement exists.

## Context

HashiCorp Vault dynamic secrets generate short-lived DB credentials on demand.
Spring Cloud Vault auto-renews leases; credential TTL = 1 hour. Kubernetes secrets
are the anti-pattern — Vault replaces them for all sensitive values.

## Solution

```mermaid
sequenceDiagram
    participant SVC as Microservice\n(Spring Boot)
    participant VAULT as HashiCorp Vault\nDynamic Secrets
    participant PG as PostgreSQL
    SVC->>VAULT: GET /v1/database/creds/account-service-role
    VAULT->>PG: CREATE ROLE svc_xyz WITH PASSWORD ... VALID UNTIL NOW()+1h
    VAULT-->>SVC: username=svc_xyz, password=..., lease_id=..., ttl=3600s
    SVC->>PG: connect with dynamic credentials
    Note over SVC,VAULT: At TTL/2 (30 min): Spring Cloud Vault renews lease
    SVC->>VAULT: PUT /v1/sys/leases/renew/{lease_id}
    VAULT-->>SVC: renewed ttl=3600s
```

## Implementation Guidelines

```yaml
# Spring Cloud Vault configuration (application.yml)
spring:
  cloud:
    vault:
      uri: https://vault.internal:8200
      authentication: KUBERNETES
      kubernetes:
        role: account-service
        kubernetes-path: auth/kubernetes
      database:
        enabled: true
        role: account-service-db-role
        backend: database
        username-property: spring.datasource.username
        password-property: spring.datasource.password
      config:
        lifecycle:
          enabled: true
          min-renewal: 10s
          expiry-threshold: 1m
```

Vault DB engine config (run once by platform team):
```bash
vault write database/config/corebanking \
    plugin_name=postgresql-database-plugin \
    connection_url="postgresql://{{username}}:{{password}}@pg-primary:5432/corebanking" \
    allowed_roles="account-service-db-role" \
    username="vault_admin" password="$VAULT_PG_PASSWORD"

vault write database/roles/account-service-db-role \
    db_name=corebanking \
    creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; GRANT account_service_privs TO \"{{name}}\";" \
    default_ttl=1h max_ttl=4h
```

## When to Use

- All database credentials, API keys, and TLS private keys for T0/T1 services.
- Any secret with blast radius > 1 service.
- Compliance-mandated rotation periods (PCI-DSS, NIST).

## When NOT to Use

- Development/local environment — use `.env` files with developer-specific keys.
- Read-only public API keys with no sensitive capability.

## Variants

- **AWS Secrets Manager**: managed alternative to Vault; auto-rotates RDS credentials
  via Lambda; integrates with EKS via CSI driver.
- **Kubernetes CSI Secrets Store**: mounts Vault secrets as files; Spring reads from
  file system — no Vault SDK dependency.

## NFR Acceptance Criteria

| Metric | T0 | T1 | T2 |
|--------|----|----|-----|
| Credential TTL | 1 h | 4 h | 24 h |
| Rotation without restart | Yes (lease renewal) | Yes | Yes |
| Vault availability | ≥ 99.99 % | ≥ 99.9 % | ≥ 99.5 % |
| Rotation time (manual break-glass) | ≤ 5 min | ≤ 15 min | ≤ 30 min |
| RTO | ≤ 4 h | ≤ 8 h | ≤ 24 h |
| RPO | ≤ 15 min | ≤ 1 h | ≤ 4 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | NIST SP 800-57 Pt1 | Cryptographic key lifetime ≤ 1 year; DB credentials TTL ≤ 1 h for T0 |
| Ring 1 | PCI-DSS 4.0 §3.6.1 | Cryptographic key rotation documented and enforced via Vault TTL policies |
| Ring 2 | SBV Circ. 09/2020 §III.2 | Access credentials for core banking systems rotated per SBV IT security requirements ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Vault Enterprise: 3-node cluster ≈ $2,000/month (licensing + EKS). Dynamic secrets
eliminate credential-rotation incidents (estimated 2 incidents/year × $20k/incident = $40k savings).

## Threat Model

- **Vault unavailable**: Spring Cloud Vault caches last credential; service continues
  for up to TTL/2; Vault HA cluster with 3 nodes prevents single-point failure.
- **Lease hijack**: Vault lease IDs are short-lived UUIDs; Vault access policy
  restricts `renew` to the service's Vault role.

## Operational Runbook Stub

1. **Vault sealed** → unseal with 3 of 5 Shamir key shares (stored in separate break-glass procedures).
2. **Service cannot connect to DB** → `vault read database/creds/account-service-db-role` to test credential generation; check Vault DB engine connectivity.
3. **Lease not renewing** → check Spring Cloud Vault actuator: `/actuator/vault`; restart pod to force fresh credential fetch.

## Test Strategy Stub

- Integration: fetch DB credential from Vault; connect to PostgreSQL; assert success.
- Rotation: let TTL expire without renewal; assert service transparently fetches new credential.
- Revoke: `vault lease revoke -prefix database/creds/account-service-db-role`; assert service reconnects within 30 s.

## Related Patterns

- [SEC-006 JWT Best Practices](jwt-best-practices.md) — JWK private keys also managed via Vault.
- [SEC-001 mTLS Service Mesh](mtls-service-mesh.md) — TLS certificates managed via Vault PKI.
- [INT-007 Sidecar/Ambassador](../integration/sidecar-ambassador.md) — Vault Agent Injector sidecar handles Vault auth.

## References

- HashiCorp Vault documentation: Database Secrets Engine.
- NIST SP 800-57 Part 1 Rev5: Key Management Recommendations.

## Key Takeaway

Dynamic DB credentials with 1-hour TTL via Vault eliminate persistent passwords;
Spring Cloud Vault lease renewal is transparent — services never need restart for rotation.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/security/secrets-rotation.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `secrets-rotation.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/security/secrets-rotation.md
git commit -m "feat(catalog): SEC-007 Secrets Rotation — Wave 3B"
```

---

### Task 18: SEC-008 Data Masking

**Files:**
- Modify: `knowledge-base/patterns/security/data-masking.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/security/data-masking.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Data Masking

Status: Draft | Catalog ID: SEC-008 | Owner: @ciso-delegate
Tier Applicability: T0, T1, T2

## Problem Statement

- PII (National ID, phone number, email) and PAN (Primary Account Number) appear
  in application logs, making log aggregation systems PII sinks requiring the
  same security controls as production databases.
- Non-production databases contain full production PAN data copied via pg_dump,
  exposing cardholder data to developers without PCI-DSS clearance.
- API responses return full NRIC (e.g., `012345678901`) to mobile clients that
  display only the last 4 digits — unnecessary data exposure.
- Decree 13/2023 Article 18 requires minimisation of personal data shared between
  systems; current API contracts return full personal data to all consumers.

## Context

Two distinct masking contexts: (1) **Dynamic masking** — mask at query or API
response time without modifying stored data; (2) **Static masking** — replace
data in non-prod copies at rest. This document covers dynamic masking via Logback
and API response masking. See SEC-013 for format-preserving tokenization of PANs.

## Solution

```mermaid
flowchart LR
    APP[Application Code] -->|log.info| LC[Logback\nMasking Layout]
    LC -->|masked log| ES[Elasticsearch\nLog Store]
    APP -->|API Response| MASK[Response Masking\nJackson Filter]
    MASK -->|masked JSON| CLIENT[Mobile / Web Client]
```

Logback replaces PII patterns in log strings before writing. Jackson `@JsonFilter`
applies per-role field-level masking in API responses.

## Implementation Guidelines

```java
// Logback masking converter (logback.xml pattern layout)
public class PiiMaskingConverter extends ClassicConverter {
    private static final Pattern PAN_RE  = Pattern.compile("\\b(\\d{4})[- ]?(\\d{4})[- ]?(\\d{4})[- ]?(\\d{4})\\b");
    private static final Pattern NRIC_RE = Pattern.compile("\\b(\\d{3})\\d{6}(\\d{3})\\b");
    private static final Pattern EMAIL_RE = Pattern.compile("(\\w{1,3})\\w+(@\\S+)");

    @Override
    public String convert(ILoggingEvent event) {
        String msg = event.getFormattedMessage();
        msg = PAN_RE.matcher(msg).replaceAll("$1-****-****-$4");
        msg = NRIC_RE.matcher(msg).replaceAll("$1******$2");
        msg = EMAIL_RE.matcher(msg).replaceAll("$1***$2");
        return msg;
    }
}
```

```xml
<!-- logback-spring.xml: apply masking converter -->
<conversionRule conversionWord="mask" converterClass="com.bank.logging.PiiMaskingConverter"/>
<appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
  <encoder>
    <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %mask%n</pattern>
  </encoder>
</appender>
```

```java
// Jackson response masking: mask NRIC for non-admin roles
@JsonFilter("piiFilter")
public record CustomerResponse(String customerId,
    @JsonProperty("nric") String nric,   // masked to last-4 for non-admin
    String name) {}

// In controller: apply filter based on JWT role
FilterProvider filters = new SimpleFilterProvider().addFilter("piiFilter",
    isAdmin(jwt) ? SimpleBeanPropertyFilter.serializeAll()
                 : SimpleBeanPropertyFilter.filterOutAllExcept("customerId", "name"));
```

## When to Use

- All log appenders in T0/T1 services handling PAN or NRIC.
- API responses returning personal data to channel clients.
- Non-prod database copies (pair with static masking tool, e.g., Faker-based).

## When NOT to Use

- Internal audit systems where full PII is required by compliance team with
  appropriate access controls — use role-based access, not masking.

## Variants

- **Database-level dynamic masking**: PostgreSQL column-level security policies
  mask columns based on role at SELECT time — no application changes needed.
- **Masking proxy**: deploy a transparent proxy (e.g., Dataguise) between app and DB.

## NFR Acceptance Criteria

| Metric | T0 | T1 | T2 |
|--------|----|----|-----|
| Logback masking overhead p99 | ≤ 0.5 ms/event | ≤ 1 ms/event | ≤ 2 ms/event |
| API response masking overhead p99 | ≤ 2 ms | ≤ 5 ms | ≤ 10 ms |
| False-positive masking rate | < 0.01 % | < 0.1 % | < 0.5 % |
| Availability | ≥ 99.99 % | ≥ 99.9 % | ≥ 99.5 % |
| RTO | ≤ 4 h | ≤ 8 h | ≤ 24 h |
| RPO | ≤ 15 min | ≤ 1 h | ≤ 4 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.8.2.3 | PII masked in logs before transmission to centralised log store |
| Ring 1 | PCI-DSS 4.0 §3.4 | PAN masked to first-6/last-4 in all non-storage contexts; Logback converter enforced via code review |
| Ring 2 | Decree 13/2023 Art. 18 | Personal data minimised at API response layer; NRIC masked to last-4 for non-DPO consumers ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Logback masking: zero infrastructure cost — regex runs in-process. Non-prod DB
static masking: 1-hour Spark job on 4 nodes, run once per refresh cycle ≈ $5/run.

## Threat Model

- **Regex bypass**: PAN stored without spaces — add `\d{16}` unspaced variant
  to PAN regex.
- **Masking not applied in new service**: enforce Logback masking via shared
  `logging-commons` library in Maven BOM; fail CI if library not present.

## Operational Runbook Stub

1. **PII found in logs** → run log scanner `scripts/scan-pii-logs.py --date 2026-05-10`; identify service; add masking pattern; deploy.
2. **Non-prod DB refresh** → run `scripts/static-mask-db.sh staging` before copying prod snapshot.
3. **False positive masking** → add exclusion regex in `PiiMaskingConverter`; unit test before deploy.

## Test Strategy Stub

- Unit: assert `PiiMaskingConverter` masks `4111-1111-1111-1234` → `4111-****-****-1234`.
- Unit: assert converter does NOT mask a 16-digit order ID that is not a Luhn-valid PAN.
- Security: scan 1,000 production log lines with PII scanner; assert 0 unmasked PANs or NRICs.

## Related Patterns

- [SEC-013 PII Tokenization](pii-tokenization-format-preserving.md) — stronger protection for stored PAN.
- [DATA-012 Data Virtualization](../data/data-virtualization.md) — Trino column masking for analytical queries.
- [DATA-011 Data Quality Rules](../data/data-quality-rules.md) — DQ rules reject PAN in non-payment fields.

## References

- PCI-DSS v4.0 Requirement 3.4: PAN must be unreadable anywhere it is stored.
- Decree 13/2023/NĐ-CP, Article 18: Personal data minimisation.

## Key Takeaway

Apply `PiiMaskingConverter` globally via `logback-spring.xml`; use
`@JsonFilter("piiFilter")` in Jackson for API-layer masking — never log a PAN
or NRIC in unmasked form, ever.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/security/data-masking.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `data-masking.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/security/data-masking.md
git commit -m "feat(catalog): SEC-008 Data Masking — Wave 3B"
```

---

### Task 19: SEC-009 Fraud Signal Collection

**Files:**
- Modify: `knowledge-base/patterns/security/fraud-signal-collection.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/security/fraud-signal-collection.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Fraud Signal Collection

Status: Draft | Catalog ID: SEC-009 | Owner: @risk-management-domain-owner
Tier Applicability: T0, T1

## Problem Statement

- Fraud signals (login anomalies, device fingerprint changes, unusual transaction
  patterns) are generated in 6 separate services but never aggregated into a
  unified signal bus for the fraud ML model to consume.
- Rule-based fraud detection operates on isolated signals; ML model requires
  cross-channel signal correlation (e.g., login from new device followed by
  high-value payment within 60 seconds).
- Signal collection is synchronous and in the critical payment path, adding
  50–100 ms latency to every transaction.
- No centralised fraud signal schema — each service invents its own format,
  making the ML feature store unreliable.

## Context

Fraud signal collection is the data ingestion layer for the fraud ML platform
(see REF-007). Signals must be emitted asynchronously (fire-and-forget) to
avoid adding latency to the payment critical path. CloudEvents schema enforces
a canonical signal format.

## Solution

```mermaid
graph TB
    LOGIN[Login Service] -->|async CloudEvent| K[Kafka\nfraud-signals topic]
    PAYMENT[Payment Service] -->|async CloudEvent| K
    DEVICE[Device Fingerprint\nService] -->|async CloudEvent| K
    K --> FEATURE[Feature Store\nRedis — rolling windows]
    K --> STREAM[Flink Fraud\nCorrelation Job]
    STREAM --> SCORE[Risk Score\nKafka topic]
    SCORE --> PAYMENT
```

Each service publishes a CloudEvent to the `fraud-signals` Kafka topic
asynchronously (non-blocking). The Flink job correlates signals within a
60-second tumbling window and updates the Redis feature store with risk scores.

## Implementation Guidelines

```java
// Fraud signal publisher — fire-and-forget, never blocks payment thread
@Component
public class FraudSignalPublisher {
    private final KafkaTemplate<String, CloudEvent> kafka;

    public void publishLoginSignal(String userId, DeviceInfo device, boolean isNewDevice) {
        CloudEvent signal = CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withType("com.bank.fraud.login.new-device")
            .withSource(URI.create("/login-service"))
            .withSubject(userId)
            .withTime(OffsetDateTime.now())
            .withExtension("riskweight", isNewDevice ? "HIGH" : "LOW")
            .withData("application/json", serialize(new LoginSignalPayload(
                userId, device.getFingerprint(), device.getIpAddress(), isNewDevice
            )))
            .build();

        // Non-blocking send — fraud signal must not block the login response
        kafka.send("fraud-signals", userId, signal)
            .exceptionally(ex -> {
                log.warn("Fraud signal publish failed for user {}: {}", userId, ex.getMessage());
                return null;  // intentional: fraud signal failure is non-fatal
            });
    }
}
```

Flink job: `TumblingEventTimeWindows.of(Time.seconds(60))` correlates signals
per userId; outputs risk score to `fraud-scores` Kafka topic.

## When to Use

- Any channel generating fraud-relevant signals (login, payment, device change).
- ML-based fraud detection requiring cross-channel feature correlation.
- Asynchronous, non-blocking fraud risk scoring (score applied to next transaction).

## When NOT to Use

- Real-time synchronous fraud decision required in < 50 ms — use pre-computed
  risk score from Redis feature store, not live signal collection.

## Variants

- **Synchronous lightweight rule check**: query Redis feature store for pre-computed
  risk score at transaction time; signal collection is async upstream.
- **Event streaming with Kafka Streams**: replace Flink with Kafka Streams for
  simpler deployment if window state fits in RocksDB.

## NFR Acceptance Criteria

| Metric | T0 | T1 |
|--------|----|----|
| Signal publish latency (async, non-blocking) | ≤ 1 ms | ≤ 2 ms |
| Signal-to-feature-store propagation p99 | ≤ 5 s | ≤ 30 s |
| Feature store read latency p99 (Redis) | ≤ 2 ms | ≤ 5 ms |
| Availability | ≥ 99.99 % | ≥ 99.9 % |
| RTO | ≤ 4 h | ≤ 8 h |
| RPO | ≤ 15 min | ≤ 1 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.12.4.1 | Fraud signal Kafka topic access limited to fraud detection services via ACL |
| Ring 1 | PSD2 Art. 2(1) | Fraud signal collection supports transaction monitoring obligation under PSD2 SCA |
| Ring 2 | SBV Circ. 09/2020 §V | Fraud monitoring system feeds AML/CFT transaction screening per SBV requirements ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Kafka topic: 100 MB/day signal volume — negligible cost. Flink job: 2 task
managers × 2 vCPU / 4 GB ≈ $160/month. Redis feature store: `cache.t3.medium`
≈ $50/month. Total: ~$210/month.

## Threat Model

- **Signal spoofing**: signals published via internal Kafka with mTLS + SASL;
  external channels cannot publish to `fraud-signals` topic.
- **Feature store poisoning**: Redis feature store is write-restricted to Flink
  job role; read-only for fraud scoring service.

## Operational Runbook Stub

1. **Flink job stopped** → check Flink job manager; restart via `flink run -d fraud-correlation.jar`; signals buffered in Kafka (7-day retention).
2. **Feature store stale > 60 s** → check Flink task manager lag; scale workers; verify Redis connection.
3. **High false-positive rate** → review Flink window predicates; adjust risk weight thresholds in config.

## Test Strategy Stub

- Unit: assert `FraudSignalPublisher.publishLoginSignal` returns without blocking and logs warning on Kafka failure.
- Integration: publish 100 login signals; assert Redis feature store updated within 5 s.
- End-to-end: simulate new-device login + high-value payment within 60 s; assert risk score > 0.7 in `fraud-scores` topic.

## Related Patterns

- [REF-007 Fraud Screening Platform](../../reference-architectures/fraud-screening-platform.md) — full platform consuming these signals.
- [INT-011 CloudEvents Envelope](../integration/cloudevents-envelope.md) — CloudEvents schema for signals.
- [OBS-005 Async Middleware Observability](../observability/async-middleware-observability.md) — monitor Kafka signal lag.

## References

- FATF Recommendation 16: Wire transfer transparency (transaction monitoring).
- PSD2 Regulatory Technical Standards: Strong Customer Authentication.

## Key Takeaway

Publish fraud signals as CloudEvents async (fire-and-forget) — never let signal
collection block the payment thread; consume via Flink with 60-second tumbling
windows into a Redis feature store.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/security/fraud-signal-collection.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `fraud-signal-collection.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/security/fraud-signal-collection.md
git commit -m "feat(catalog): SEC-009 Fraud Signal Collection — Wave 3B"
```

---

### Task 20: SEC-010 Attribute-Based Access Control

**Files:**
- Modify: `knowledge-base/patterns/security/attribute-based-access-control.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/security/attribute-based-access-control.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Attribute-Based Access Control (ABAC)

Status: Draft | Catalog ID: SEC-010 | Owner: @ciso-delegate
Tier Applicability: T0, T1, T2

## Problem Statement

- Role-Based Access Control (RBAC) is too coarse for banking: the role `CUSTOMER`
  can see all accounts, but a customer must only see their own accounts.
- Relationship-based rules (a relationship manager can view accounts they manage)
  cannot be expressed in RBAC roles without an explosion of role combinations.
- Policy changes (e.g., block access to accounts in jurisdictions under sanctions)
  require code deployments; no runtime policy update mechanism exists.
- Audit cannot demonstrate that a specific access decision was made by a specific
  policy rule — RBAC grants are binary.

## Context

OPA (Open Policy Agent) evaluates Rego policies at request time. Spring Security
integrates OPA via a REST call to the OPA `/v1/data` endpoint. Policy files are
stored in Git and deployed via CI/CD — changes take effect without service restart.

## Solution

```mermaid
sequenceDiagram
    participant SVC as Account Service
    participant OPA as OPA Sidecar\n:8181
    participant POLICY as Rego Policy\n(Git-managed)
    SVC->>OPA: POST /v1/data/bank/accounts/allow\n{input: {subject, resource, action}}
    OPA->>POLICY: evaluate allow rule
    POLICY-->>OPA: {allow: true/false, reason: "..."}
    OPA-->>SVC: {result: true}
    SVC-->>CLIENT: 200 OK or 403 Forbidden
```

## Implementation Guidelines

```rego
# policy/bank/accounts/allow.rego
package bank.accounts

import future.keywords.if

default allow = false

# Customer can only view their own accounts
allow if {
    input.action == "READ"
    input.subject.role == "CUSTOMER"
    input.resource.owner_id == input.subject.user_id
}

# Relationship manager can view managed accounts
allow if {
    input.action == "READ"
    input.subject.role == "RELATIONSHIP_MANAGER"
    input.resource.rm_id == input.subject.user_id
}

# Compliance officer can view any account (read-only)
allow if {
    input.action == "READ"
    input.subject.role == "COMPLIANCE_OFFICER"
}

# No one can access sanctioned accounts (runtime embargo check)
deny if {
    data.sanctions.blocked_accounts[input.resource.account_id]
}
```

```java
// Spring Security OPA integration
@Component
public class OpaAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final WebClient opaClient;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> auth, RequestAuthorizationContext ctx) {
        Map<String, Object> input = Map.of(
            "subject", Map.of(
                "user_id", ((Jwt) auth.get().getPrincipal()).getSubject(),
                "role", extractRole(auth.get())
            ),
            "resource", Map.of("account_id", extractAccountId(ctx)),
            "action", ctx.getRequest().getMethod()
        );
        Boolean allowed = opaClient.post()
            .uri("/v1/data/bank/accounts/allow")
            .bodyValue(Map.of("input", input))
            .retrieve().bodyToMono(Map.class)
            .map(r -> (Boolean) r.get("result"))
            .block(Duration.ofMillis(50));
        return new AuthorizationDecision(Boolean.TRUE.equals(allowed));
    }
}
```

## When to Use

- Multi-tenant banking where users must be isolated by ownership.
- Fine-grained policies that depend on resource attributes (account status, jurisdiction).
- Runtime policy updates without service redeployment.

## When NOT to Use

- Simple two-role systems (admin / user) — RBAC is sufficient and cheaper.
- Ultra-low-latency paths where 5 ms OPA call is unacceptable — pre-compute
  permissions into the JWT claims at token issuance time.

## Variants

- **Cedar**: AWS Cedar policy language for banking-grade ABAC with formal verification.
- **Casbin**: lightweight Go/Java ABAC library; no separate server required.

## NFR Acceptance Criteria

| Metric | T0 | T1 | T2 |
|--------|----|----|-----|
| OPA policy evaluation p99 | ≤ 5 ms (sidecar) | ≤ 10 ms | ≤ 20 ms |
| Policy deployment lag | ≤ 30 s (ConfigMap update) | ≤ 60 s | ≤ 5 min |
| Availability | ≥ 99.99 % | ≥ 99.9 % | ≥ 99.5 % |
| RTO | ≤ 4 h | ≤ 8 h | ≤ 24 h |
| RPO | ≤ 15 min | ≤ 1 h | ≤ 4 h |
| Policy decision audit log retention | ≥ 1 year | ≥ 6 months | ≥ 3 months |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | NIST SP 800-162 | ABAC implementation follows NIST attribute-based access control guidelines |
| Ring 1 | PCI-DSS 4.0 §7.3 | Access to cardholder data controlled by OPA policy; policy changes version-controlled in Git |
| Ring 2 | Decree 13/2023 Art. 13 | Personal data access decisions logged by OPA with subject, resource, action, and allow/deny result ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

OPA sidecar: 0.1 vCPU / 128 MB per pod — negligible. Policy management via
Git + CI/CD, no additional tooling cost. Decision logging to Elasticsearch:
~1 KB/decision × 10M decisions/day = 10 GB/day → ~$15/day on ELK.

## Threat Model

- **OPA sidecar compromise**: OPA runs in read-only filesystem; policies loaded
  from read-only ConfigMap; cannot be modified at runtime.
- **Policy bypass**: all service endpoints must use `OpaAuthorizationManager`;
  enforce via Spring Security `authorizeHttpRequests` — no open endpoints.

## Operational Runbook Stub

1. **403 on valid resource access** → check OPA decision log: `kubectl logs -l app=opa | grep DENY | grep {user_id}`.
2. **Policy change not taking effect** → verify ConfigMap updated: `kubectl get configmap opa-policies -o yaml`; check OPA bundle reload log.
3. **OPA sidecar 5xx** → restart OPA sidecar: `kubectl rollout restart deploy/account-service`; evaluate whether to fall back to deny-all or allow-all.

## Test Strategy Stub

- Unit (Rego): OPA `rego.New().Query()` in JUnit — assert `CUSTOMER` accessing own account returns `allow=true`.
- Unit (Rego): assert `CUSTOMER` accessing another customer's account returns `allow=false`.
- Integration: end-to-end REST call with JWT; assert HTTP 403 for cross-customer access.
- Policy update: push new Rego policy; assert takes effect within 30 s without service restart.

## Related Patterns

- [SEC-006 JWT Best Practices](jwt-best-practices.md) — JWT claims provide subject attributes to OPA input.
- [SEC-012 Tamper-Evident Audit Logging](audit-logging-tamper-evident.md) — OPA decision logs stored tamper-evidently.
- [DATA-012 Data Virtualization](../data/data-virtualization.md) — Trino OPA plugin applies same policies to analytical queries.

## References

- NIST SP 800-162: Guide to ABAC Definition and Considerations.
- OPA documentation: Rego Policy Language.

## Key Takeaway

Rego policies in Git + OPA sidecar: policy changes take effect in 30 s without
deployment — every deny decision is logged with the rule that triggered it.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/security/attribute-based-access-control.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `attribute-based-access-control.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/security/attribute-based-access-control.md
git commit -m "feat(catalog): SEC-010 Attribute-Based Access Control — Wave 3B"
```

---

### Task 21: SEC-011 Session Revocation

**Files:**
- Modify: `knowledge-base/patterns/security/session-revocation.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/security/session-revocation.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Session Revocation

Status: Draft | Catalog ID: SEC-011 | Owner: @ciso-delegate
Tier Applicability: T0, T1, T2

## Problem Statement

- Logout does not invalidate JWT tokens — a compromised bearer token remains
  valid until `exp` (up to 30 minutes), giving attackers a window to act.
- Forced logout for compromised accounts (fraud detected) requires waiting for
  token expiry rather than immediate revocation.
- PCI-DSS §8.2.8 requires session termination after 15 minutes of inactivity;
  stateless JWTs cannot be passively invalidated.
- Customers report being still "logged in" on stolen devices because the app
  holds a valid refresh token that cannot be centrally revoked.

## Context

A token revocation list (Redis-backed) is the standard approach for stateless
JWT environments. Each token is given a unique `jti` (JWT ID) claim; on logout
or forced-revocation, the `jti` is added to a Redis set with TTL equal to the
token's remaining lifetime.

## Solution

```mermaid
sequenceDiagram
    participant USER
    participant SVC as Any Service\n(JWT validation filter)
    participant REDIS as Revocation Store\nRedis SET jti-revoked
    participant AUTH as Auth Service
    USER->>AUTH: POST /logout (Bearer JWT)
    AUTH->>REDIS: SADD jti-revoked {jti} EX {remaining_ttl}
    AUTH-->>USER: 200 OK (session terminated)
    Note over USER,REDIS: Next request with same token
    USER->>SVC: GET /accounts (Bearer JWT)
    SVC->>REDIS: SISMEMBER jti-revoked {jti}
    REDIS-->>SVC: 1 (revoked)
    SVC-->>USER: 401 Unauthorized
```

## Implementation Guidelines

```java
// Token revocation store
@Component
public class TokenRevocationStore {
    private final RedisTemplate<String, String> redis;
    private static final String KEY = "jti-revoked";

    public void revoke(String jti, Duration remainingTtl) {
        redis.opsForSet().add(KEY, jti);
        redis.expire(KEY, remainingTtl.plus(Duration.ofMinutes(1)));
        // Note: use SADD + individual key per jti for fine-grained TTL
        redis.opsForValue().set("jti:" + jti, "1", remainingTtl.plus(Duration.ofMinutes(1)));
    }

    public boolean isRevoked(String jti) {
        return Boolean.TRUE.equals(redis.hasKey("jti:" + jti));
    }
}

// Spring Security JWT validation filter extension
@Component
public class RevocationCheckFilter extends OncePerRequestFilter {
    private final TokenRevocationStore revocationStore;
    private final JwtDecoder jwtDecoder;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String token = extractToken(req);
        if (token != null) {
            Jwt jwt = jwtDecoder.decode(token);
            if (revocationStore.isRevoked(jwt.getId())) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
```

## When to Use

- All services where tokens can be compromised (all T0/T1 banking services).
- Forced logout for fraud incidents or device loss.
- PCI-DSS §8.2.8 inactivity timeout enforcement.

## When NOT to Use

- Pure service-to-service M2M tokens with very short TTL (≤ 5 min) and
  no user session semantics — token expiry is sufficient.

## Variants

- **Distributed revocation via Kafka**: auth service publishes revocation events;
  all services maintain local revocation sets — eliminates Redis dependency in
  the hot path at cost of eventual consistency.
- **Token introspection**: call auth server on every request; immediate revocation
  but adds 10–50 ms latency per call.

## NFR Acceptance Criteria

| Metric | T0 | T1 | T2 |
|--------|----|----|-----|
| Revocation propagation latency | ≤ 100 ms (Redis) | ≤ 500 ms | ≤ 5 s |
| Revocation check overhead p99 | ≤ 2 ms (Redis GET) | ≤ 5 ms | ≤ 10 ms |
| Redis availability | ≥ 99.99 % | ≥ 99.9 % | ≥ 99.5 % |
| RTO | ≤ 4 h | ≤ 8 h | ≤ 24 h |
| RPO | ≤ 15 min | ≤ 1 h | ≤ 4 h |
| Max concurrent revoked tokens | ≥ 10M | ≥ 1M | ≥ 100K |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.9.4.2 | Session termination enforced via Redis revocation check on every authenticated request |
| Ring 1 | PCI-DSS 4.0 §8.2.8 | Inactivity timeout: client sends logout after 15 min; server-side revocation prevents reuse |
| Ring 2 | SBV Circ. 09/2020 §III.2 | Banking session revocation within 100 ms on logout or fraud detection as per SBV IT security standards ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Redis Cluster (3 nodes, `cache.r6g.large`): ≈ $300/month. Revocation store TTL
auto-expires entries, keeping memory bounded. At 100,000 active sessions with
15-min TTL: ~500 MB Redis memory.

## Threat Model

- **Redis unavailable**: circuit-break to DENY on revocation check failure (fail
  closed, not open) — reduces availability but prevents revoked token from being
  accepted.
- **Redis poisoning**: grant only `SET`/`GET`/`DEL` to revocation service role;
  no `FLUSHDB` or admin commands.

## Operational Runbook Stub

1. **Redis down** → fail-closed: all requests with tokens return 401 until Redis recovers; escalate to P1.
2. **False revocation (user cannot log in)** → `redis-cli DEL jti:{jti}` to remove incorrect entry after investigation.
3. **Fraud-triggered mass revocation** → `redis-cli KEYS "jti:*" | xargs redis-cli DEL` for all tokens of affected user (requires user-to-jti index in Redis).

## Test Strategy Stub

- Unit: assert `isRevoked()` returns false for unknown jti, true after `revoke()`.
- Integration: issue JWT, call logout, assert next API call returns 401.
- Availability: simulate Redis down; assert all requests return 401 (fail-closed), not 200.
- Performance: 10,000 concurrent requests with revocation check; assert p99 ≤ 2 ms.

## Related Patterns

- [SEC-006 JWT Best Practices](jwt-best-practices.md) — JWTs must include `jti` claim for revocation.
- [SEC-010 ABAC](attribute-based-access-control.md) — revocation check runs before policy evaluation.
- [MOB-002 Mobile Secure Storage](../mobile/mobile-secure-storage.md) — refresh tokens stored in device Keychain; revocation invalidates refresh tokens too.

## References

- RFC 7009: OAuth 2.0 Token Revocation.
- PCI-DSS v4.0, Requirement 8.2.8.

## Key Takeaway

Store `jti` in Redis with TTL equal to remaining token lifetime on logout; check
`GET jti:{jti}` in the filter before every authenticated call — fail closed if
Redis is unreachable.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/security/session-revocation.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `session-revocation.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/security/session-revocation.md
git commit -m "feat(catalog): SEC-011 Session Revocation — Wave 3B"
```

---

### Task 22: SEC-012 Tamper-Evident Audit Logging

**Files:**
- Modify: `knowledge-base/patterns/security/audit-logging-tamper-evident.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/security/audit-logging-tamper-evident.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Tamper-Evident Audit Logging

Status: Draft | Catalog ID: SEC-012 | Owner: @ciso-delegate
Tier Applicability: T0, T1

## Problem Statement

- Audit logs stored in PostgreSQL can be modified by a DBA with `UPDATE` or
  `DELETE` privileges; PCI-DSS §10.3 requires log integrity controls.
- Regulatory investigations require proof that audit logs were not altered after
  the event; a flat file or database row provides no such proof.
- Log rotation overwrites critical audit events; retention policy not enforced
  consistently across services.
- OPA access decisions (SEC-010) and payment routing decisions (INT-009) are
  logged but integrity is unverified.

## Context

HMAC-chained audit logs create a linked list of log entries where each entry
includes the HMAC of the previous entry. Tampering with any entry invalidates
the chain. Periodic Merkle root publication to S3 creates an external checkpoint
that cannot be altered even by internal admins.

## Solution

```mermaid
sequenceDiagram
    participant SVC as Service
    participant AL as AuditLogger
    participant DB as audit_log table\n(append-only)
    participant S3 as S3 Merkle\nCheckpoint
    SVC->>AL: log(AuditEvent)
    AL->>DB: SELECT hmac FROM last row
    AL->>AL: compute HMAC-SHA256(event || prev_hmac)
    AL->>DB: INSERT (event, hmac, seq_no)
    Note over AL,S3: Every 15 minutes
    AL->>DB: SELECT all hmac for window
    AL->>AL: compute Merkle root
    AL->>S3: PUT merkle-root-{timestamp}.json (immutable)
```

## Implementation Guidelines

```java
@Service
public class AuditLogger {
    private static final String HMAC_ALGO = "HmacSHA256";
    private final AuditLogRepository repo;
    private final SecretKeySpec hmacKey; // loaded from Vault at startup

    @Transactional
    public void log(AuditEvent event) {
        String prevHmac = repo.findLastHmac().orElse("GENESIS");
        String payload = event.toCanonicalJson() + prevHmac;
        String hmac = computeHmac(payload);

        repo.save(AuditLogEntry.builder()
            .eventType(event.getType())
            .subjectId(event.getSubjectId())
            .resourceId(event.getResourceId())
            .action(event.getAction())
            .outcome(event.getOutcome())
            .timestamp(Instant.now())
            .prevHmac(prevHmac)
            .hmac(hmac)
            .build());
    }

    private String computeHmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(hmacKey);
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new AuditIntegrityException("HMAC computation failed", e);
        }
    }
}
```

PostgreSQL table: `CREATE TABLE audit_log (...) WITH (AUTOVACUUM_ENABLED = false);`
Grant `INSERT` only to audit logger role; no `UPDATE`/`DELETE`.

## When to Use

- All T0/T1 financial and security events (payment authorisation, login, access decisions).
- Any event required for regulatory investigation (PCI-DSS §10, BCBS 239).

## When NOT to Use

- High-frequency debug logs (> 10,000 events/s) — HMAC chaining is sequential
  and becomes a bottleneck; use append-only Kafka topic for high-frequency events.

## Variants

- **Blockchain anchor**: publish Merkle root to public blockchain (Ethereum)
  for third-party verifiability — higher cost, use only for highest-assurance
  requirements.
- **AWS CloudTrail**: for infrastructure events; provides tamper-evident logs as
  managed service without custom HMAC implementation.

## NFR Acceptance Criteria

| Metric | T0 | T1 |
|--------|----|----|
| Audit log write latency p99 | ≤ 10 ms | ≤ 20 ms |
| Merkle checkpoint interval | 15 min | 1 h |
| Chain integrity verification time (1M rows) | ≤ 5 min | ≤ 30 min |
| Availability | ≥ 99.99 % | ≥ 99.9 % |
| RTO | ≤ 4 h | ≤ 8 h |
| Log retention | ≥ 7 years | ≥ 5 years |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.12.4.2 | HMAC chain provides cryptographic evidence that no audit record was modified or deleted |
| Ring 1 | PCI-DSS 4.0 §10.3.3 | Audit log copies protected from modifications; HMAC chain + S3 immutable object lock enforces §10.3.3 |
| Ring 2 | SBV Circ. 09/2020 §IV.3 | Audit records for core banking operations retained ≥ 5 years with integrity verification ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

PostgreSQL audit table: 1 KB/row × 10M events/day = 10 GB/day → partition by month
and archive to S3 Glacier ($0.004/GB/month) after 90 days. Merkle checkpoints to S3
Standard: 1 KB/checkpoint × 96 checkpoints/day = negligible cost.

## Threat Model

- **HMAC key compromise**: Vault-managed HMAC key; rotation requires chain replay
  with new key — document in ADR as accepted risk.
- **Audit log table truncation**: PostgreSQL row-level security (`FOR ALL USING (FALSE)
  WITH CHECK (TRUE)`) on the audit table prevents DELETE/TRUNCATE for app role.

## Operational Runbook Stub

1. **Chain integrity failure** → run `scripts/verify-audit-chain.sh --from {seq_no}` to identify first tampered row.
2. **Audit log write failures** → check `audit_log` table lock contention; scale writer instances.
3. **Merkle checkpoint missing** → `scripts/recompute-merkle.sh --window {timestamp}` to backfill.

## Test Strategy Stub

- Unit: assert `AuditLogger.log()` produces different HMAC for same event if `prevHmac` changes.
- Tamper test: manually UPDATE one audit row; run chain verifier; assert chain breaks at that row.
- Retention: assert rows older than 90 days are in S3 Glacier tier via `scripts/check-archive-policy.sh`.

## Related Patterns

- [SEC-010 ABAC](attribute-based-access-control.md) — OPA decisions logged via AuditLogger.
- [SEC-012 pairs with COMP-004 PCI-DSS 4.0](../../compliance/pci-dss-4-0.md) — §10.3 control.
- [DATA-009 Data Lineage](../data/data-lineage.md) — lineage events also written to audit log.

## References

- PCI-DSS v4.0, Requirements 10.2–10.7.
- NIST SP 800-92: Guide to Computer Security Log Management.

## Key Takeaway

HMAC-chain every audit entry using the previous row's HMAC; publish Merkle roots
to S3 every 15 minutes — tampering with any row breaks the chain and invalidates
all subsequent Merkle checkpoints.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/security/audit-logging-tamper-evident.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `audit-logging-tamper-evident.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/security/audit-logging-tamper-evident.md
git commit -m "feat(catalog): SEC-012 Tamper-Evident Audit Logging — Wave 3B"
```

---

### Task 23: SEC-013 PII Tokenization (Format-Preserving)

**Files:**
- Modify: `knowledge-base/patterns/security/pii-tokenization-format-preserving.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/security/pii-tokenization-format-preserving.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# PII Tokenization (Format-Preserving Encryption)

Status: Draft | Catalog ID: SEC-013 | Owner: @ciso-delegate
Tier Applicability: T0, T1

## Problem Statement

- PAN (Primary Account Number) stored in transaction tables in plaintext; a DB
  dump exposes all card numbers — a direct PCI-DSS §3.4 violation.
- Replacing PAN with random tokens requires all downstream systems (analytics,
  reconciliation, dispute management) to use a lookup table, adding latency
  and a new SPOF.
- National ID (NRIC/CCCD) stored in customer records must be protected under
  Decree 13/2023 but downstream systems need to validate format (12 digits).
- HSM-based tokenization is required for PAN but NRIC requires software FPE.

## Context

Format-Preserving Encryption (FPE) produces a cipher-text that has the same
format (length, character set) as the plain-text. PAN `4111-1111-1111-1111`
becomes a token like `7893-2054-1762-3891` — still a valid 16-digit Luhn number.
NIST SP 800-38G defines the FF3-1 algorithm. Bouncy Castle provides the Java
implementation.

## Solution

```mermaid
graph TB
    SVC[Payment Service] -->|PAN plaintext| FPE[FPE Tokenizer\nFF3-1 + Vault key]
    FPE -->|PAN token| DB[(Transaction DB\nstores token only)]
    FPE -->|PAN token| MQ[Kafka / SQS]
    subgraph De-tokenization
        AUTH[Authorised Service\nHSM gateway] -->|token| FPE2[FPE De-tokenizer]
        FPE2 -->|PAN plaintext| CLEARING[Clearing / SWIFT]
    end
```

Tokenization: encrypt PAN with FF3-1 using a 256-bit key from Vault Transit
engine. The key never leaves Vault; the tokenization service calls Vault Transit
for each encrypt/decrypt operation.

## Implementation Guidelines

```java
// FPE tokenizer using Bouncy Castle FF3-1
@Service
public class FpeTokenizer {
    private static final String ALPHABET = "0123456789";
    private final byte[] fpeKey;    // 256-bit key from Vault Transit
    private final byte[] fpeTweak;  // 8-byte per-domain tweak

    public String tokenizePan(String pan) {
        // Strip spaces/dashes; preserve last-4 for display
        String digitsOnly = pan.replaceAll("[^0-9]", "");
        FF3_1Engine engine = new FF3_1Engine(new AESEngine());
        FPEParameters params = new FPEParameters(
            new KeyParameter(fpeKey), ALPHABET.length(), fpeTweak);
        engine.init(true, params);

        char[] input  = digitsOnly.toCharArray();
        char[] output = new char[input.length];
        engine.processBlock(input, 0, input.length, output, 0);
        return new String(output);
    }

    public String detokenizePan(String token) {
        char[] tokenChars = token.toCharArray();
        char[] plainChars = new char[tokenChars.length];
        FF3_1Engine engine = new FF3_1Engine(new AESEngine());
        FPEParameters params = new FPEParameters(
            new KeyParameter(fpeKey), ALPHABET.length(), fpeTweak);
        engine.init(false, params); // false = decrypt
        engine.processBlock(tokenChars, 0, tokenChars.length, plainChars, 0);
        return new String(plainChars);
    }
}
```

Key management: FPE key stored in Vault Transit engine; fetched at service startup
via `vault read transit/export/encryption-key/fpe-pan-key` — never persisted
in service memory beyond request scope.

## When to Use

- PAN storage in any system that must be PCI-DSS compliant.
- NRIC/CCCD storage where downstream systems need format validation.
- Any PII that must be searchable or matchable without revealing plaintext.

## When NOT to Use

- Short, low-cardinality secrets (e.g., 4-digit PIN) — FPE on short alphabets
  is weaker; use AES-GCM + lookup table instead.
- When downstream systems do not need the tokenized format to appear valid — use
  random token (UUID) instead of FPE.

## Variants

- **Vault Transit Tokenization**: Vault natively tokenizes arbitrary data; simpler
  but produces non-format-preserving tokens.
- **HSM-backed FPE**: Thales Luna or AWS CloudHSM with PKCS#11 interface; required
  for PCI-DSS SAQ D Level 1 merchants.

## NFR Acceptance Criteria

| Metric | T0 | T1 |
|--------|----|----|
| Tokenization latency p99 (in-process) | ≤ 2 ms | ≤ 5 ms |
| De-tokenization latency p99 | ≤ 5 ms | ≤ 10 ms |
| Key rotation without data migration | Yes (re-encrypt in background) | Yes |
| Availability | ≥ 99.99 % | ≥ 99.9 % |
| RTO | ≤ 4 h | ≤ 8 h |
| RPO | ≤ 15 min | ≤ 1 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | NIST SP 800-38G | FF3-1 algorithm; 256-bit key; 8-byte domain tweak as per NIST specification |
| Ring 1 | PCI-DSS 4.0 §3.5 | PAN rendered unreadable via FF3-1 FPE; token passes Luhn check, satisfying downstream validation without exposing PAN |
| Ring 2 | Decree 13/2023 Art. 18 | NRIC tokenized before storage; de-tokenization restricted to authorised processing roles with audit log ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Bouncy Castle FPE is in-process; no additional server cost. Vault Transit API call:
~2 ms per operation — at 1,000 TPS, adds 2 ms per transaction. Vault Enterprise
licensing already counted in SEC-007.

## Threat Model

- **FPE key extraction**: key managed by Vault Transit; never exported to service
  memory; Vault audit log records every key usage.
- **Token collision**: FF3-1 is a bijection (one-to-one mapping) — no collisions
  for same key + tweak + plaintext.

## Operational Runbook Stub

1. **FPE key rotation** → generate new key version in Vault Transit; background job re-encrypts existing tokens with new key version.
2. **Tokenization service unavailable** → check Vault Transit health; payment service should fail-fast (not proceed without tokenization).
3. **De-tokenization fails for old token** → check Vault Transit key versions; ensure old key version not deleted before all tokens migrated.

## Test Strategy Stub

- Unit: assert `tokenizePan("4111111111111111")` produces a 16-digit Luhn-valid token.
- Unit: assert `detokenizePan(tokenizePan(pan))` == original PAN (round-trip).
- Unit: assert same PAN with different tweak produces different token.
- Integration: tokenize PAN; store in DB; de-tokenize for clearing; assert clearing receives original PAN.

## Related Patterns

- [SEC-008 Data Masking](data-masking.md) — masking for display (last-4); tokenization for storage.
- [SEC-007 Secrets Rotation](secrets-rotation.md) — Vault Transit key managed as a secret.
- [BSP-002 Idempotent Payment Key](../banking-solutions/idempotent-payment-key.md) — idempotency key based on tokenized PAN.

## References

- NIST SP 800-38G Rev1: Recommendation for Block Cipher Modes: Methods for Format-Preserving Encryption.
- Bouncy Castle documentation: FF3-1 implementation.

## Key Takeaway

FF3-1 FPE produces a token that looks like a valid PAN — downstream systems need
no changes; de-tokenize only at the HSM gateway boundary, never in application code.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/security/pii-tokenization-format-preserving.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `pii-tokenization-format-preserving.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/security/pii-tokenization-format-preserving.md
git commit -m "feat(catalog): SEC-013 PII Tokenization Format-Preserving — Wave 3B"
```

---

## Wave 3B Gate — Integration + Security (13 docs)

- [ ] **Step 1: Batch lint**

```bash
for f in \
  knowledge-base/patterns/integration/anti-corruption-layer.md \
  knowledge-base/patterns/integration/strangler-fig.md \
  knowledge-base/patterns/integration/sidecar-ambassador.md \
  knowledge-base/patterns/integration/backend-for-frontend-routing.md \
  knowledge-base/patterns/integration/content-based-router.md \
  knowledge-base/patterns/security/jwt-best-practices.md \
  knowledge-base/patterns/security/secrets-rotation.md \
  knowledge-base/patterns/security/data-masking.md \
  knowledge-base/patterns/security/fraud-signal-collection.md \
  knowledge-base/patterns/security/attribute-based-access-control.md \
  knowledge-base/patterns/security/session-revocation.md \
  knowledge-base/patterns/security/audit-logging-tamper-evident.md \
  knowledge-base/patterns/security/pii-tokenization-format-preserving.md; do
  bash scripts/mermaid-lint-doc.sh "$f" || echo "FAIL: $f"
done
```
Expected: 13 files, 0 FAIL lines.

- [ ] **Step 2: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for any of the 13 Wave 3B files.

- [ ] **Step 3: Update catalog — INT-005 to INT-009, SEC-006 to SEC-013 → Draft**

In `governance/standards/enterprise-architecture-catalog.md`:
- Change `INT-005`, `INT-006`, `INT-007`, `INT-008`, `INT-009` from `Proposed` → `Draft`
- Change `SEC-006`, `SEC-007`, `SEC-008`, `SEC-009`, `SEC-010`, `SEC-011`, `SEC-012`, `SEC-013` from `Proposed` → `Draft`

```bash
grep -E "(INT-00[5-9]|SEC-0(0[6-9]|1[0-3]))" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |"
```
Expected: 13 lines.

- [ ] **Step 4: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md
git commit -m "chore(catalog): Wave 3B gate — INT-005–009 + SEC-006–013 promoted to Draft"
```

---

## Wave 3C — Frontend + Mobile Patterns

---

### Task 24: FE-001 Web Performance Budgets

**Files:**
- Modify: `knowledge-base/patterns/frontend/web-performance-budgets.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/frontend/web-performance-budgets.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Web Performance Budgets

Status: Draft | Catalog ID: FE-001 | Owner: @tech-lead-web
Tier Applicability: T0, T1, T2

## Problem Statement

- Mobile banking app LCP (Largest Contentful Paint) measures 4.2 s on a Moto G5
  with Vietnamese 4G connection (10 Mbps / 80 ms RTT); Core Web Vitals threshold
  is 2.5 s — failing Google's "Good" threshold affects SEO and app store ranking.
- JavaScript bundle grew from 350 KB to 1.2 MB over 18 months with no gate;
  first parse on mid-range Android takes 3.5 s.
- INP (Interaction to Next Paint) exceeds 200 ms on the transaction history page
  due to blocking rendering from unvirtualised 1,000-row lists.
- No CI gate exists for bundle size; engineers add dependencies without awareness
  of total impact.

## Context

Performance budgets define numeric thresholds (LCP, INP, CLS, bundle size) that
must not be exceeded. Lighthouse CI enforces these budgets in CI/CD. Webpack
Bundle Analyzer and `@next/bundle-analyzer` provide visibility. This document
covers the budget definition, enforcement, and optimisation patterns.

## Solution

```mermaid
graph LR
    PR[Pull Request] --> CI[CI Pipeline]
    CI --> LC[Lighthouse CI\nperformance audit]
    CI --> BA[Webpack Bundle\nAnalyzer + budget]
    LC -->|LCP > 2.5s| FAIL[Build FAIL]
    BA -->|JS > 400KB| FAIL
    LC -->|LCP ≤ 2.5s| PASS[Build PASS]
    BA -->|JS ≤ 400KB| PASS
```

## Implementation Guidelines

```javascript
// lighthouserc.js — Lighthouse CI budget enforcement
module.exports = {
  ci: {
    collect: {
      url: ['http://localhost:3000/', 'http://localhost:3000/accounts'],
      numberOfRuns: 3,
    },
    assert: {
      assertions: {
        'categories:performance': ['error', { minScore: 0.9 }],
        'first-contentful-paint': ['error', { maxNumericValue: 2000 }],
        'largest-contentful-paint': ['error', { maxNumericValue: 2500 }],
        'interactive': ['error', { maxNumericValue: 3500 }],
        'cumulative-layout-shift': ['error', { maxNumericValue: 0.1 }],
        'total-blocking-time': ['error', { maxNumericValue: 200 }],
        'resource-summary:script:size': ['error', { maxNumericValue: 400000 }], // 400 KB
        'resource-summary:total:size': ['error', { maxNumericValue: 1000000 }],
      },
    },
    upload: { target: 'temporary-public-storage' },
  },
};
```

```typescript
// React: virtualise long lists to reduce INP
import { FixedSizeList } from 'react-window';

const TransactionList: React.FC<{ transactions: Transaction[] }> = ({ transactions }) => (
  <FixedSizeList height={600} width="100%" itemCount={transactions.length} itemSize={72}>
    {({ index, style }) => (
      <div style={style}>
        <TransactionRow transaction={transactions[index]} />
      </div>
    )}
  </FixedSizeList>
);
```

Code-split by route: `const AccountsPage = React.lazy(() => import('./AccountsPage'))`.
Preload critical chunks: `<link rel="preload" as="script" href="/static/js/main.chunk.js">`.

## When to Use

- All customer-facing web surfaces (T0/T1/T2 services with web UI).
- Any PR introducing a new npm dependency or page.

## When NOT to Use

- Internal admin tools with < 100 users — performance budget overhead not justified.

## Variants

- **Next.js built-in budgets**: `next.config.js` `experimental.bundleSizeLimit` for framework-native enforcement.
- **Web Vitals library**: `web-vitals` npm package sends real-user CWV metrics to Grafana.

## NFR Acceptance Criteria

| Metric | T0 | T1 | T2 |
|--------|----|----|-----|
| LCP p75 (mobile 4G) | ≤ 2.5 s | ≤ 3.0 s | ≤ 4.0 s |
| INP p75 | ≤ 200 ms | ≤ 300 ms | ≤ 500 ms |
| CLS p75 | ≤ 0.1 | ≤ 0.15 | ≤ 0.25 |
| JS bundle (initial) | ≤ 300 KB gzip | ≤ 400 KB | ≤ 600 KB |
| Total page weight | ≤ 800 KB | ≤ 1 MB | ≤ 2 MB |
| Lighthouse Performance score | ≥ 90 | ≥ 80 | ≥ 70 |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.14.2.5 | Performance testing in CI prevents degraded user experience from unreviewed code |
| Ring 1 | Core Web Vitals (Google) | LCP, INP, CLS budgets aligned with Google "Good" thresholds for search ranking |
| Ring 2 | SBV Circ. 09/2020 §IV.2 | Banking app must maintain acceptable response times for digital banking services ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Lighthouse CI: open source, runs on existing CI workers. `react-window` adds
3 KB gzip — eliminates 500 ms render cost for 1,000-row lists.

## Threat Model

- **Budget bypass via configuration drift**: Lighthouse CI config checked into Git;
  changes require PR review.

## Operational Runbook Stub

1. **Lighthouse CI fails in PR** → run `npx lhci autorun` locally; check which metric exceeded budget; optimise before merging.
2. **Real-user LCP degrading** → check Grafana Web Vitals dashboard; identify specific page; run profiler.
3. **Bundle size spike** → run `npm run analyze`; identify large dependency; evaluate lazy-load or replacement.

## Test Strategy Stub

- CI gate: Lighthouse CI runs on every PR; build fails on budget exceedance.
- Real-user monitoring: `web-vitals` sends CWV to Grafana; alert if p75 LCP > 3 s.
- Weekly synthetic: Lighthouse against staging on Vietnamese 4G network profile.

## Related Patterns

- [FE-002 Web Resilience / Offline-First](web-resilience-offline-first.md) — Service Worker caching reduces repeat load LCP.
- [FE-005 Web Error Boundary](web-error-boundary.md) — error boundaries prevent CLS from crash fallback reflow.
- [OBS-002 Distributed Trace Propagation](../observability/distributed-trace-propagation.md) — trace HTTP calls that contribute to LCP.

## References

- web.dev: Core Web Vitals and Performance Budgets.
- Webpack documentation: Bundle Analysis.

## Key Takeaway

Gate every PR with `lighthouserc.js` budgets; virtualise lists with `react-window`
to fix INP; code-split by route to keep initial JS ≤ 300 KB gzip.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/frontend/web-performance-budgets.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `web-performance-budgets.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/frontend/web-performance-budgets.md
git commit -m "feat(catalog): FE-001 Web Performance Budgets — Wave 3C"
```

---

### Task 25: FE-002 Web Resilience / Offline-First

**Files:**
- Modify: `knowledge-base/patterns/frontend/web-resilience-offline-first.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/frontend/web-resilience-offline-first.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Web Resilience / Offline-First

Status: Draft | Catalog ID: FE-002 | Owner: @tech-lead-web
Tier Applicability: T1, T2

## Problem Statement

- Vietnamese 4G coverage is unreliable in rural branches; banking app becomes
  completely unusable during 5–30 second connectivity drops, causing user
  abandonment and incomplete transactions.
- Account balance and recent transactions require a network call on every page
  load; users cannot view their last-known balance without connectivity.
- Form data (loan application, money transfer) is lost on connectivity loss,
  frustrating users who re-enter data.
- Service Worker is not configured; every page load fetches all static assets
  from CDN, wasting bandwidth on slow connections.

## Context

Offline-First means designing for no connectivity as the baseline; online is
an enhancement. Service Worker caches static assets and API responses;
IndexedDB queues pending mutations for sync-when-online.

## Solution

```mermaid
graph TB
    USER[User Action] --> SW{Service Worker\nCache?}
    SW -->|cache hit| CACHE[Serve from Cache\nStale-While-Revalidate]
    SW -->|cache miss| NET[Fetch from Network]
    NET -->|success| UPDATE[Update Cache]
    NET -->|offline| QUEUE[IndexedDB\nMutation Queue]
    QUEUE -->|online event| SYNC[Background Sync\nreplay mutations]
```

## Implementation Guidelines

```typescript
// service-worker.ts — Workbox-based Service Worker
import { precacheAndRoute, createHandlerBoundToURL } from 'workbox-precaching';
import { registerRoute, NavigationRoute } from 'workbox-routing';
import { StaleWhileRevalidate, NetworkFirst } from 'workbox-strategies';
import { BackgroundSyncPlugin } from 'workbox-background-sync';

// Precache all static assets (injected by Workbox webpack plugin)
precacheAndRoute(self.__WB_MANIFEST);

// API: network-first with 10s timeout, fall back to cache
registerRoute(
  ({ url }) => url.pathname.startsWith('/api/v1/accounts'),
  new NetworkFirst({
    cacheName: 'accounts-api',
    networkTimeoutSeconds: 10,
    plugins: [{ cacheWillUpdate: async ({ response }) =>
      response.status === 200 ? response : null }],
  })
);

// Mutation queue: retry failed POST/PUT when back online
const bgSyncPlugin = new BackgroundSyncPlugin('mutation-queue', {
  maxRetentionTime: 24 * 60, // 24 hours in minutes
});
registerRoute(
  ({ request }) => request.method === 'POST' && request.url.includes('/api/v1/transfers'),
  new NetworkFirst({ plugins: [bgSyncPlugin] }),
  'POST'
);
```

```typescript
// React hook: offline indicator
function useOnlineStatus(): boolean {
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  useEffect(() => {
    const handleOnline  = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    window.addEventListener('online',  handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      window.removeEventListener('online',  handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);
  return isOnline;
}
```

## When to Use

- Banking apps accessed on mobile devices in areas with unreliable connectivity.
- Read-heavy pages (account balance, recent transactions) where stale data is
  acceptable for ≤ 5 minutes.

## When NOT to Use

- Real-time trading or payment confirmation UIs where stale data is dangerous.
- Pages where security policy prohibits client-side caching of sensitive data.

## Variants

- **Cache-Only**: serve entirely from cache — only for fully static marketing pages.
- **Network-Only**: no caching — only for security-critical pages (OTP entry).

## NFR Acceptance Criteria

| Metric | T1 | T2 |
|--------|----|----|
| Repeat load LCP (cached assets) | ≤ 1 s | ≤ 2 s |
| Offline balance view availability | ≥ 5 min stale | ≥ 30 min stale |
| Mutation queue replay on reconnect | ≤ 30 s | ≤ 2 min |
| Service Worker install time | ≤ 500 ms | ≤ 1 s |
| Cache storage limit | ≤ 50 MB | ≤ 100 MB |
| Availability | ≥ 99.9 % | ≥ 99.5 % |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.8.2.3 | Service Worker cache encrypted via browser-native encryption; sensitive data (OTP, PIN) excluded from cache |
| Ring 1 | Core Web Vitals | Repeat LCP ≤ 1 s via asset precaching satisfies performance SLA |
| Ring 2 | Decree 13/2023 Art. 18 | Cached account data minimised to last-4 of account number; full PAN never cached client-side ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Workbox: open source. Service Worker reduces CDN egress by ~60% on repeat visits
(assets served from cache) — at 1M monthly users × 100 KB CDN assets = 60 GB
CDN savings ≈ $5/month.

## Threat Model

- **Sensitive data in cache**: configure `Cache-Control: no-store` for OTP,
  PIN, and PAN responses; Workbox respects these headers.
- **Cache poisoning**: Service Worker scope restricted to app origin;
  cross-origin requests bypass cache.

## Operational Runbook Stub

1. **Old Service Worker blocking update** → prompt user to refresh: `registration.waiting.postMessage({ type: 'SKIP_WAITING' })`.
2. **Mutation queue not replaying** → check `navigator.serviceWorker.ready` and Background Sync API support.
3. **Cache storage quota exceeded** → trim old cache entries: `caches.delete('accounts-api-v1')` on Service Worker activate.

## Test Strategy Stub

- Integration: simulate offline via `chrome.networkConditions`; assert cached balance renders.
- Integration: submit transfer offline; restore connectivity; assert transfer replayed and confirmed.
- Performance: repeat load LCP; assert ≤ 1 s with precached assets.

## Related Patterns

- [FE-001 Web Performance Budgets](web-performance-budgets.md) — offline-first reduces repeat LCP within budget.
- [MOB-001 Mobile Offline Queue](../mobile/mobile-offline-queue.md) — same pattern on native mobile.
- [SEC-008 Data Masking](../security/data-masking.md) — mask data before caching client-side.

## References

- Workbox documentation: Strategies and Background Sync.
- Google Offline Cookbook (web.dev).

## Key Takeaway

Use `StaleWhileRevalidate` for account data and `BackgroundSyncPlugin` for
mutations — never cache PANs, PINs, or OTPs client-side.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/frontend/web-resilience-offline-first.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `web-resilience-offline-first.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/frontend/web-resilience-offline-first.md
git commit -m "feat(catalog): FE-002 Web Resilience Offline-First — Wave 3C"
```

---

### Task 26: FE-003 Web CSP Hardening

**Files:**
- Modify: `knowledge-base/patterns/frontend/web-csp-hardening.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/frontend/web-csp-hardening.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Web CSP Hardening

Status: Draft | Catalog ID: FE-003 | Owner: @tech-lead-web
Tier Applicability: T0, T1, T2

## Problem Statement

- Banking portal uses `Content-Security-Policy: default-src *` — effectively
  no CSP; XSS injection can exfiltrate session tokens to attacker-controlled domains.
- Inline `<script>` tags used for analytics snippets; any XSS in page content
  can execute arbitrary script in the same CSP context.
- `X-Frame-Options` not set; banking portal can be embedded in attacker iframe
  for clickjacking attacks on high-value actions (approve transfer).
- OWASP ASVS V14.4 (HTTP Security Headers) partially satisfied; 4 of 7 required
  headers are missing.

## Context

Content Security Policy (CSP) is an HTTP response header that whitelists
permitted script, style, and resource sources. Nonce-based CSP allows inline
scripts only if they carry the server-generated nonce — the safest approach
without `unsafe-inline`.

## Solution

```mermaid
sequenceDiagram
    participant SERVER as Express Server
    participant BROWSER
    SERVER->>SERVER: generate crypto nonce per request
    SERVER->>BROWSER: HTML + CSP header\n(nonce-{abc123})
    BROWSER->>BROWSER: execute only scripts\nwith nonce={abc123}
    BROWSER->>BROWSER: block any injected\nscript without nonce
```

## Implementation Guidelines

```typescript
// Express middleware: CSP with per-request nonce
import crypto from 'crypto';
import helmet from 'helmet';

app.use((req, res, next) => {
  res.locals.cspNonce = crypto.randomBytes(16).toString('base64');
  next();
});

app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: [
        "'self'",
        (req, res) => `'nonce-${(res as any).locals.cspNonce}'`,
      ],
      styleSrc: ["'self'", 'https://fonts.googleapis.com'],
      fontSrc:  ["'self'", 'https://fonts.gstatic.com'],
      imgSrc:   ["'self'", 'data:', 'https://cdn.bank.internal'],
      connectSrc: ["'self'", 'https://api.bank.internal'],
      frameSrc:  ["'none'"],
      objectSrc: ["'none'"],
      baseUri:   ["'self'"],
      formAction: ["'self'"],
      upgradeInsecureRequests: [],
      reportUri: '/csp-report',
    },
  },
  referrerPolicy: { policy: 'strict-origin-when-cross-origin' },
  xFrameOptions: { action: 'deny' },
  hsts: { maxAge: 31536000, includeSubDomains: true, preload: true },
  xContentTypeOptions: true,
  xXssProtection: false, // deprecated; CSP is the replacement
}));
```

```typescript
// React: render inline script with nonce (SSR)
// _document.tsx (Next.js)
import { Html, Head, Main, NextScript } from 'next/document';
export default function Document() {
  // nonce injected server-side via response locals
  return (
    <Html><Head nonce={process.env.CSP_NONCE} />
    <body><Main /><NextScript nonce={process.env.CSP_NONCE} /></body></Html>
  );
}
```

## When to Use

- All customer-facing web applications (banking portal, admin dashboard).
- Any page handling authentication, payments, or personal data.

## When NOT to Use

- Static file servers serving only public content with no authentication.

## Variants

- **CSP Report-Only**: deploy header as `Content-Security-Policy-Report-Only`
  to collect violations without blocking; tune before enforcing.
- **Trusted Types**: additional Chrome protection against DOM XSS; use alongside
  CSP for higher assurance.

## NFR Acceptance Criteria

| Metric | T0 | T1 | T2 |
|--------|----|----|-----|
| CSP header present | Required | Required | Required |
| `unsafe-inline` in script-src | Forbidden | Forbidden | Forbidden |
| HSTS max-age | ≥ 31536000 | ≥ 15768000 | ≥ 2592000 |
| Nonce entropy | ≥ 128 bits | ≥ 128 bits | ≥ 96 bits |
| CSP violation report latency | ≤ 5 s | ≤ 30 s | ≤ 5 min |
| OWASP ASVS V14.4 controls satisfied | 7/7 | 6/7 | 5/7 |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.14.2.5 | CSP header reviewed in security code review checklist; automated header audit in CI |
| Ring 1 | OWASP ASVS V14.4 | All 7 HTTP security headers required by ASVS enforced via Helmet middleware |
| Ring 2 | SBV Circ. 09/2020 §III | Banking web application security controls include XSS and clickjacking prevention ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Helmet: open source. Nonce generation: < 0.1 ms per request. CSP violation
reporting: 1 KB/violation × 1,000 violations/day = 1 MB/day → Elasticsearch
at negligible cost.

## Threat Model

- **Nonce reuse**: nonce generated with `crypto.randomBytes(16)` — 128-bit entropy,
  no reuse risk.
- **CSP bypass via JSONP endpoint**: remove JSONP endpoints; if required, add
  explicit allowlist in `script-src`.

## Operational Runbook Stub

1. **CSP violation spike** → check `/csp-report` endpoint logs; identify injected source; block at WAF.
2. **Third-party script blocked** → add source to CSP `connectSrc` or `scriptSrc` after security review.
3. **HSTS breaks HTTP access** → ensure HTTP → HTTPS redirect in ALB/nginx before deploying HSTS.

## Test Strategy Stub

- Automated: `npx observatory-cli https://portal.bank.internal` — assert Mozilla Observatory score ≥ A.
- Unit: assert Express response includes all 7 required headers.
- Security scan: OWASP ZAP passive scan on staging; assert no CSP violations in green headers.

## Related Patterns

- [FE-004 Web Feature Flags](web-feature-flags.md) — feature flag scripts must carry CSP nonce.
- [SEC-006 JWT Best Practices](../security/jwt-best-practices.md) — XSS protection prevents JWT theft from localStorage.
- [OBS-002 Distributed Trace Propagation](../observability/distributed-trace-propagation.md) — trace ID injected into page via nonce-gated script.

## References

- OWASP ASVS v4.0, Chapter 14: Configuration.
- MDN: Content-Security-Policy documentation.

## Key Takeaway

Generate a 128-bit nonce per request; add `'nonce-{abc}'` to `script-src`; set
`X-Frame-Options: DENY` and `HSTS: max-age=31536000` — never use `unsafe-inline`.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/frontend/web-csp-hardening.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `web-csp-hardening.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/frontend/web-csp-hardening.md
git commit -m "feat(catalog): FE-003 Web CSP Hardening — Wave 3C"
```

---

### Task 27: FE-004 Web Feature Flags

**Files:**
- Modify: `knowledge-base/patterns/frontend/web-feature-flags.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/frontend/web-feature-flags.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Web Feature Flags

Status: Draft | Catalog ID: FE-004 | Owner: @tech-lead-web
Tier Applicability: T1, T2, T3

## Problem Statement

- New features (e.g., instant loan pre-approval UX) are deployed to all users
  simultaneously; a bug requires a full redeployment to roll back, taking 20 minutes.
- A/B testing requires separate deployments for each variant; no runtime control.
- High-risk UI changes (payment flow redesign) cannot be rolled out to a small
  percentage of users before full launch.
- Feature flag state is hardcoded in environment variables; changing a flag
  requires a service restart.

## Context

Feature flags decouple feature release from code deployment. A flag service
(GrowthBook — open source, self-hosted) evaluates rules at runtime; the React
app queries flag state on load and re-evaluates on flag change events.

## Solution

```mermaid
graph LR
    APP[React App] -->|GET /api/features\n(userId, attributes)| GB[GrowthBook\nFeature Service]
    GB --> RULES[Flag Rules\n% rollout, targeting]
    RULES -->>|{instantLoan: true}| APP
    APP --> GATE{Feature Gate}
    GATE -->|enabled| NEW[New Loan\nPre-Approval UI]
    GATE -->|disabled| OLD[Existing\nLoan UI]
```

## Implementation Guidelines

```typescript
// GrowthBook React integration
import { GrowthBook, GrowthBookProvider, useFeature } from '@growthbook/growthbook-react';

// App initialization
const gb = new GrowthBook({
  apiHost: 'https://growthbook.internal',
  clientKey: process.env.REACT_APP_GB_KEY,
  enableDevMode: process.env.NODE_ENV !== 'production',
  trackingCallback: (experiment, result) => {
    analytics.track('Feature Flag Evaluated', {
      featureId: experiment.key,
      variationId: result.variationId,
    });
  },
});

// Set user attributes for targeting
await gb.loadFeatures({ autoRefresh: true, timeout: 2000 });
gb.setAttributes({ id: user.id, tier: user.tier, region: user.region });

// React hook usage
function LoanApplicationPage() {
  const instantLoan = useFeature('instant-loan-preapproval');
  return instantLoan.on ? <InstantLoanFlow /> : <ClassicLoanFlow />;
}
```

```typescript
// Feature flag provider wrapping the app
function App() {
  return (
    <GrowthBookProvider growthbook={gb}>
      <Router>...</Router>
    </GrowthBookProvider>
  );
}
```

## When to Use

- Any feature with non-trivial rollback risk (payment flows, auth changes).
- A/B testing or gradual rollout to % of users.
- Kill switch for third-party integrations.

## When NOT to Use

- Simple config values that never need runtime change — use env vars.
- Security controls — never gate security enforcement behind a feature flag.

## Variants

- **LaunchDarkly**: managed alternative with SDKs for all platforms; higher cost.
- **OpenFeature**: vendor-neutral SDK standard; swap providers without code change.

## NFR Acceptance Criteria

| Metric | T1 | T2 | T3 |
|--------|----|----|-----|
| Flag evaluation latency (in-process, cached) | ≤ 1 ms | ≤ 1 ms | ≤ 2 ms |
| Flag refresh latency (polling) | ≤ 30 s | ≤ 60 s | ≤ 5 min |
| GrowthBook service availability | ≥ 99.9 % | ≥ 99.5 % | ≥ 99.0 % |
| Fallback on service unavailable | Use last cached value | Use defaults | Use defaults |
| RTO | ≤ 8 h | ≤ 24 h | ≤ 48 h |
| RPO | ≤ 1 h | ≤ 4 h | ≤ 8 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.14.2.9 | Flag changes version-controlled in GrowthBook audit log; rollback in ≤ 5 s |
| Ring 1 | — | No specific regulatory requirement; supports controlled rollout reducing risk of regulatory-impacting bugs |
| Ring 2 | SBV Circ. 09/2020 §IV.1 | New banking features requiring SBV approval gated behind flags until regulatory sign-off received ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

GrowthBook self-hosted on EKS: 2 replicas × 0.5 vCPU / 512 MB ≈ $30/month.
Eliminates rollback deployments (2–3 per month × 20 min each = $200 in developer
time saved/month).

## Threat Model

- **Flag service down**: fall back to default flag state (conservative defaults
  mean new risky features are OFF by default).
- **Flag state manipulation**: GrowthBook API access restricted to admin role;
  flag changes audited.

## Operational Runbook Stub

1. **Kill switch a feature** → GrowthBook UI → feature → Override → 0% → Save (takes effect in ≤ 30 s).
2. **Flag service unreachable** → React app falls back to `gb.getFeatureValue('feature-key', defaultValue)`.
3. **A/B test showing regressions** → stop experiment in GrowthBook UI; set flag to control variant.

## Test Strategy Stub

- Unit: assert component renders `<InstantLoanFlow>` when flag ON; `<ClassicLoanFlow>` when OFF.
- Integration: mock GrowthBook SDK; assert flag evaluation uses correct targeting attributes.
- E2E: Playwright test with flag forced ON; assert new UI renders and completes flow.

## Related Patterns

- [FE-003 Web CSP Hardening](web-csp-hardening.md) — GrowthBook inline script requires CSP nonce.
- [FE-005 Web Error Boundary](web-error-boundary.md) — new feature UI wrapped in Error Boundary.
- [INT-006 Strangler Fig](../integration/strangler-fig.md) — feature flags gate new service traffic during migration.

## References

- GrowthBook documentation: React SDK.
- Fowler, "Feature Toggle" (martinfowler.com).

## Key Takeaway

Default all new features to OFF; set the kill switch before deployment — flag
state rolls back in 30 s without redeployment.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/frontend/web-feature-flags.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `web-feature-flags.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/frontend/web-feature-flags.md
git commit -m "feat(catalog): FE-004 Web Feature Flags — Wave 3C"
```

---

### Task 28: FE-005 Web Error Boundary

**Files:**
- Modify: `knowledge-base/patterns/frontend/web-error-boundary.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/frontend/web-error-boundary.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Web Error Boundary

Status: Draft | Catalog ID: FE-005 | Owner: @tech-lead-web
Tier Applicability: T0, T1, T2

## Problem Statement

- A null reference error in the account balance component crashes the entire
  React tree; the user sees a blank white page with no recovery path.
- JavaScript errors from third-party widgets (analytics, chat) propagate up
  and crash the banking portal.
- Error recovery requires a full page reload, losing all form state.
- Front-end errors are not reported to Sentry with sufficient context (component
  stack, user session, failed API call) to diagnose root cause.

## Context

React Error Boundaries are class components that implement `componentDidCatch`
and `getDerivedStateFromError`. They catch rendering errors in their subtree and
render a fallback UI. Sentry captures the error with component stack and user context.

## Solution

```mermaid
graph TB
    PARENT[Parent Component] --> EB[Error Boundary]
    EB --> CHILD[Child Component]
    CHILD -->|throw Error| EB
    EB -->|componentDidCatch| SENTRY[Sentry\nerror.captureException]
    EB -->|getDerivedStateFromError| FALLBACK[Fallback UI\nRetry button]
```

## Implementation Guidelines

```typescript
// ErrorBoundary.tsx — reusable error boundary
import * as Sentry from '@sentry/react';

interface Props {
  fallback?: React.ReactNode;
  onReset?: () => void;
  children: React.ReactNode;
}

interface State { hasError: boolean; error?: Error }

export class ErrorBoundary extends React.Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    Sentry.captureException(error, {
      contexts: { react: { componentStack: info.componentStack } },
      tags: { component: 'ErrorBoundary' },
    });
  }

  handleReset = () => {
    this.setState({ hasError: false, error: undefined });
    this.props.onReset?.();
  };

  render() {
    if (this.state.hasError) {
      return this.props.fallback ?? (
        <div role="alert" className="error-boundary">
          <p>Something went wrong. Please try again.</p>
          <button onClick={this.handleReset}>Retry</button>
        </div>
      );
    }
    return this.props.children;
  }
}
```

```typescript
// Usage: wrap high-risk components
function AccountPage() {
  return (
    <ErrorBoundary fallback={<AccountError />} onReset={() => refetchAccount()}>
      <AccountBalance accountId={accountId} />
    </ErrorBoundary>
  );
}
```

## When to Use

- All high-risk components (balance display, transaction list, payment forms).
- Any component consuming third-party widgets or dynamic data.
- Top-level page wrapper for all customer-facing routes.

## When NOT to Use

- Simple utility components with no async operations or complex rendering.

## Variants

- **Sentry Error Boundary**: `@sentry/react` provides `Sentry.ErrorBoundary`
  with built-in capture; wraps the pattern above.
- **Async error boundary** (React 19+): catches async errors in `use()` calls.

## NFR Acceptance Criteria

| Metric | T0 | T1 | T2 |
|--------|----|----|-----|
| Error capture latency (Sentry) | ≤ 2 s | ≤ 5 s | ≤ 30 s |
| Fallback render time | ≤ 100 ms | ≤ 200 ms | ≤ 500 ms |
| Error boundary coverage (% of routes) | 100 % | 100 % | ≥ 80 % |
| Availability of Sentry SDK | ≥ 99.9 % | ≥ 99.5 % | ≥ 99.0 % |
| RTO | ≤ 4 h | ≤ 8 h | ≤ 24 h |
| User recovery without full reload | Yes (Retry button) | Yes | Best effort |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.16.1.2 | All front-end errors reported to Sentry with component stack; error response team notified within 15 min of P1 |
| Ring 1 | — | No direct regulatory requirement; error boundaries prevent loss of customer financial data in form state |
| Ring 2 | SBV Circ. 09/2020 §IV.2 | Banking portal must degrade gracefully; blank-page failures constitute service unavailability per SBV IT standards ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Sentry Team plan: $26/month for up to 50,000 errors/month. ErrorBoundary has
zero runtime overhead when no error occurs.

## Threat Model

- **Error message leaking internals**: fallback UI shows generic message only;
  full error detail sent to Sentry, not exposed to user.
- **Sentry capturing PII in error context**: configure Sentry `beforeSend` hook
  to strip account IDs and tokens from error payloads.

## Operational Runbook Stub

1. **Error boundary triggered spike** → check Sentry for componentStack; identify failing component; deploy fix.
2. **Sentry rate limit exceeded** → filter noise (console warnings, network errors) with Sentry `ignoreErrors` config.
3. **Recovery button not working** → check `onReset` callback; ensure it clears component-level state that caused the error.

## Test Strategy Stub

- Unit: render a child that throws; assert fallback UI appears and `Sentry.captureException` called.
- Unit: click Retry; assert `getDerivedStateFromError` resets to `{ hasError: false }`.
- E2E: Playwright — mock API error causing component throw; assert fallback renders; click Retry; assert recovery.

## Related Patterns

- [FE-004 Web Feature Flags](web-feature-flags.md) — new feature components wrapped in ErrorBoundary.
- [FE-001 Web Performance Budgets](web-performance-budgets.md) — fallback UI must not cause CLS.
- [OBS-002 Distributed Trace Propagation](../observability/distributed-trace-propagation.md) — Sentry error linked to trace ID for backend correlation.

## References

- React documentation: Error Boundaries.
- Sentry documentation: React Error Boundary.

## Key Takeaway

Wrap every route and third-party widget in an `ErrorBoundary` with a Retry button;
send full component stack to Sentry with PII stripped — a blank page is never
acceptable in a banking portal.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/frontend/web-error-boundary.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `web-error-boundary.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/frontend/web-error-boundary.md
git commit -m "feat(catalog): FE-005 Web Error Boundary — Wave 3C"
```

---

### Task 29: FE-006 Web i18n / RTL

**Files:**
- Modify: `knowledge-base/patterns/frontend/web-i18n-rtl.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/frontend/web-i18n-rtl.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Web i18n / RTL

Status: Draft | Catalog ID: FE-006 | Owner: @tech-lead-web
Tier Applicability: T0, T1, T2, T3

## Problem Statement

- Vietnamese date format (DD/MM/YYYY) hardcoded as US format (MM/DD/YYYY) in
  the transaction history page — January 02 displayed as February 01.
- Currency formatting shows "$1,000.00" for VND amounts that should display
  "1.000 ₫" (no decimals, period as thousand separator, ₫ suffix).
- Arabic right-to-left (RTL) support required for potential expansion to
  Middle Eastern markets; current CSS uses `margin-left`/`float:left` which
  do not flip in RTL context.
- English-only error messages shown to Vietnamese-speaking users; no translation
  pipeline exists.

## Context

`react-i18next` provides the translation layer. `Intl.DateTimeFormat` and
`Intl.NumberFormat` handle locale-aware formatting. CSS logical properties
(`margin-inline-start` instead of `margin-left`) provide RTL support without
media queries.

## Solution

```mermaid
graph LR
    UI[React Component] --> T[t() hook\nreact-i18next]
    T --> NS[Namespace files\nen.json / vi.json / ar.json]
    UI --> FMT[Intl formatter\nlocale-aware]
    FMT --> LOCALE[Browser locale\nor user preference]
    HTML[html dir=rtl] --> CSS[CSS logical props\nmargin-inline-start]
```

## Implementation Guidelines

```typescript
// i18n.ts — react-i18next configuration
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    fallbackLng: 'vi',
    supportedLngs: ['vi', 'en', 'ar'],
    ns: ['common', 'accounts', 'payments'],
    defaultNS: 'common',
    backend: { loadPath: '/locales/{{lng}}/{{ns}}.json' },
    interpolation: { escapeValue: false },
  });

export default i18n;
```

```typescript
// Locale-aware formatting hook
export function useFormatter(locale: string) {
  const dateFormatter = new Intl.DateTimeFormat(locale, {
    day: '2-digit', month: '2-digit', year: 'numeric',
  });
  const currencyFormatter = (amount: number, currency: string) =>
    new Intl.NumberFormat(locale, { style: 'currency', currency }).format(amount);

  return { formatDate: (d: Date) => dateFormatter.format(d), formatCurrency: currencyFormatter };
}

// Usage:
// formatDate(new Date('2026-01-02')) → 'vi' = '02/01/2026', 'en' = '01/02/2026'
// formatCurrency(1000, 'VND') → 'vi' = '1.000 ₫', 'en' = '₫1,000'
```

```css
/* CSS logical properties — RTL-safe layout */
.transaction-list__item {
  margin-inline-start: 1rem;   /* instead of margin-left */
  padding-inline-end: 0.5rem;  /* instead of padding-right */
  text-align: start;           /* instead of text-align: left */
}
```

```typescript
// Set dir attribute on <html> based on locale
const RTL_LOCALES = new Set(['ar', 'he', 'fa', 'ur']);
document.documentElement.dir = RTL_LOCALES.has(i18n.language) ? 'rtl' : 'ltr';
document.documentElement.lang = i18n.language;
```

## When to Use

- All customer-facing UI serving users whose locale may differ from the developer's.
- Any UI with date, currency, or number formatting.
- Applications planning expansion to RTL markets.

## When NOT to Use

- Internal dev tools with English-only audience.

## Variants

- **ICU Message Format**: `i18next-icu` plugin; supports pluralisation rules
  (Vietnamese has no plural; Arabic has 6 plural forms).
- **Phrase.io / Lokalise**: translation management platforms for translator workflow.

## NFR Acceptance Criteria

| Metric | All Tiers |
|--------|-----------|
| Translation key coverage | 100 % of user-visible strings |
| Missing translation fallback | Falls back to `vi` (primary locale) |
| Date format accuracy | Matches `Intl.DateTimeFormat` for locale |
| Currency format accuracy | Matches `Intl.NumberFormat` for locale |
| RTL layout correctness (ar) | No `margin-left`/`float:left` in production CSS |
| i18n bundle load time | ≤ 50 ms (lazy-loaded per namespace) |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.12.1.1 | Documented operational procedures available in Vietnamese for all banking operations |
| Ring 1 | — | No direct regulatory requirement; localisation is a product requirement |
| Ring 2 | Decree 13/2023 Art. 7 | Privacy notices and consent forms presented in Vietnamese (user's primary language) ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

react-i18next: open source. Translation files: JSON, hosted on CDN at negligible
cost. Initial translator cost: ~$500 to translate 500 keys; ongoing via translation
management platform.

## Threat Model

- **XSS via translation interpolation**: `i18next` escapes all interpolated values
  by default (`escapeValue: true` unless overridden with `escapeValue: false` for
  trusted HTML only).
- **Locale injection**: locale detection from `Accept-Language` header; server
  validates against `supportedLngs` allowlist.

## Operational Runbook Stub

1. **Missing translation key** → i18next logs warning in dev mode; displays key path in production if fallback locale also missing.
2. **RTL layout broken** → check for `margin-left`/`float:left` with `grep -r 'margin-left\|float: left' src/`; replace with logical properties.
3. **Currency format wrong** → verify `locale` passed to `Intl.NumberFormat`; check browser `Intl` support for currency.

## Test Strategy Stub

- Unit: `formatDate(new Date('2026-01-02'), 'vi')` returns `'02/01/2026'`.
- Unit: `formatCurrency(1000, 'VND', 'vi')` returns `'1.000 ₫'`.
- Visual regression: Percy/Chromatic snapshot of Arabic RTL layout; alert on pixel diff.
- i18n coverage: CI script asserts every translation key in `vi.json` exists in `en.json`.

## Related Patterns

- [FE-001 Web Performance Budgets](web-performance-budgets.md) — locale JSON lazy-loaded to avoid impacting bundle size budget.
- [FE-005 Web Error Boundary](web-error-boundary.md) — error messages translated via i18n.
- [MOB-006 Mobile Force-Upgrade](../mobile/mobile-force-upgrade.md) — upgrade messages localised.

## References

- MDN: Intl API documentation.
- react-i18next documentation.

## Key Takeaway

Use `Intl.DateTimeFormat` and `Intl.NumberFormat` for all date/currency display;
replace every `margin-left` with `margin-inline-start` — RTL support requires
zero media queries when CSS logical properties are used consistently.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/frontend/web-i18n-rtl.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `web-i18n-rtl.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/frontend/web-i18n-rtl.md
git commit -m "feat(catalog): FE-006 Web i18n RTL — Wave 3C"
```

---

### Task 30: MOB-001 Mobile Offline Queue

**Files:**
- Modify: `knowledge-base/patterns/mobile/mobile-offline-queue.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/mobile/mobile-offline-queue.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Mobile Offline Queue

Status: Draft | Catalog ID: MOB-001 | Owner: @tech-lead-mobile
Tier Applicability: T1, T2

## Problem Statement

- Users initiate a payment transfer in a low-connectivity area; the API call
  fails silently; the user taps "Submit" again, creating a duplicate transaction.
- Fund transfer failures during connectivity loss cause app crashes; no offline
  state recovery path exists.
- Transaction history does not show pending (not-yet-synced) operations, confusing
  users who see the amount not deducted from balance.
- Idempotency key not generated client-side; retries on the server create
  duplicate payments.

## Context

An offline queue stores intended mutations (payments, transfers) in device-local
SQLite (iOS: CoreData; Android: Room) with an idempotency key generated at the
time of user intent. A background sync job replays the queue when connectivity
is restored. See also BSP-002 Idempotent Payment Key.

## Solution

```mermaid
sequenceDiagram
    participant USER
    participant APP
    participant QUEUE as SQLite Queue
    participant SYNC as Background Sync
    participant API
    USER->>APP: Submit transfer
    APP->>APP: Generate idempotency key (UUID v4)
    APP->>QUEUE: Enqueue(transfer + key, status=PENDING)
    APP-->>USER: "Transfer queued — syncing when online"
    Note over SYNC: When network restored
    SYNC->>QUEUE: Fetch PENDING items
    SYNC->>API: POST /transfers (idempotency-key: {key})
    API-->>SYNC: 201 Created (or 200 if duplicate)
    SYNC->>QUEUE: Update status=COMPLETE
    SYNC->>APP: Notify: transfer confirmed
```

## Implementation Guidelines

```swift
// iOS: OfflineQueue using CoreData
@MainActor
class OfflineQueueManager: ObservableObject {
    private let container = NSPersistentContainer(name: "OfflineQueue")

    func enqueue(_ transfer: TransferRequest) {
        let entry = QueueEntry(context: container.viewContext)
        entry.id = UUID()
        entry.idempotencyKey = UUID().uuidString  // generated at intent time
        entry.payload = try! JSONEncoder().encode(transfer)
        entry.status = "PENDING"
        entry.createdAt = Date()
        entry.retryCount = 0
        try! container.viewContext.save()
        scheduleBackgroundSync()
    }

    func syncPending() async {
        let pending = fetchPending()
        for entry in pending {
            do {
                let transfer = try JSONDecoder().decode(TransferRequest.self, from: entry.payload!)
                _ = try await TransferAPI.submit(transfer, idempotencyKey: entry.idempotencyKey!)
                entry.status = "COMPLETE"
            } catch let error as APIError where error.isIdempotentDuplicate {
                entry.status = "COMPLETE"  // server already processed
            } catch {
                entry.retryCount += 1
                if entry.retryCount > 5 { entry.status = "FAILED" }
            }
            try! container.viewContext.save()
        }
    }
}
```

```kotlin
// Android: WorkManager-based background sync
class SyncTransfersWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val pending = transferDao.getPending()
        for (entry in pending) {
            try {
                val response = api.submitTransfer(entry.payload, entry.idempotencyKey)
                transferDao.markComplete(entry.id)
            } catch (e: HttpException) {
                if (e.code() == 409) transferDao.markComplete(entry.id) // idempotent duplicate
                else if (entry.retryCount >= 5) transferDao.markFailed(entry.id)
                else transferDao.incrementRetry(entry.id)
            }
        }
        return Result.success()
    }
}

// Schedule sync on connectivity restored
val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
WorkManager.getInstance(context).enqueueUniqueWork(
    "transfer-sync", ExistingWorkPolicy.KEEP,
    OneTimeWorkRequestBuilder<SyncTransfersWorker>().setConstraints(constraints).build()
)
```

## When to Use

- Payment and transfer actions in mobile banking apps.
- Any user-initiated mutation that must survive connectivity loss.

## When NOT to Use

- Read-only operations (balance check) — use cached response from last successful fetch.
- Operations requiring immediate server confirmation (OTP verification).

## Variants

- **Cloud-native offline**: Firestore offline persistence for apps already using Firestore.
- **Optimistic UI**: show pending state immediately in UI; reconcile on sync.

## NFR Acceptance Criteria

| Metric | T1 | T2 |
|--------|----|----|
| Queue enqueue latency | ≤ 50 ms | ≤ 100 ms |
| Sync on connectivity restore | ≤ 30 s | ≤ 2 min |
| Idempotency guarantee | Exactly-once on server | Exactly-once |
| Max queue depth | ≥ 100 pending items | ≥ 20 items |
| Availability | ≥ 99.9 % | ≥ 99.5 % |
| RTO | ≤ 8 h | ≤ 24 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.8.2.3 | Queue entries encrypted at rest using device Keychain / Android Keystore key |
| Ring 1 | ISO 20022 EndToEndId | Idempotency key used as ISO 20022 `EndToEndId` for payment messages |
| Ring 2 | SBV Circ. 09/2020 §IV.2 | Payment queued offline replayed within 30 s of connectivity restore; no data loss ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

SQLite / CoreData / Room: on-device, zero cost. WorkManager / BackgroundTasks:
OS-managed, zero cost. No additional infrastructure needed.

## Threat Model

- **Queue tampering**: SQLite queue encrypted using device Keychain-derived key
  (see MOB-002 Mobile Secure Storage).
- **Replay attack with stolen idempotency key**: idempotency key is a UUID with
  128-bit entropy; server idempotency window = 24 hours; key is device-local.

## Operational Runbook Stub

1. **Transfer stuck in PENDING** → check sync log in device; force manual sync via app settings "Sync Now".
2. **Server returns 500 on sync** → queue marks retry; exponential backoff up to 5 retries; moves to FAILED after.
3. **FAILED queue items** → user shown error notification; deep link to manual resubmit flow.

## Test Strategy Stub

- Unit (iOS): enqueue transfer offline; assert CoreData contains 1 PENDING entry with idempotency key.
- Unit (Android): WorkManager test; assert `SyncTransfersWorker` marks entry COMPLETE on 201.
- Integration: submit transfer offline; restore connectivity; assert server receives exactly 1 request.
- Idempotency: retry 3 times; assert server processes exactly once.

## Related Patterns

- [BSP-002 Idempotent Payment Key](../banking-solutions/idempotent-payment-key.md) — server-side idempotency matching.
- [MOB-002 Mobile Secure Storage](mobile-secure-storage.md) — queue encrypted at rest.
- [FE-002 Web Resilience Offline-First](../frontend/web-resilience-offline-first.md) — web equivalent.

## References

- Apple BackgroundTasks framework documentation.
- Android WorkManager documentation.

## Key Takeaway

Generate the idempotency key (UUID) at the moment the user taps Submit — not at
retry time — and persist it with the queue entry; the server must accept the same
key as a no-op on retry.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/mobile/mobile-offline-queue.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `mobile-offline-queue.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/mobile/mobile-offline-queue.md
git commit -m "feat(catalog): MOB-001 Mobile Offline Queue — Wave 3C"
```

---

### Task 31: MOB-002 Mobile Secure Storage

**Files:**
- Modify: `knowledge-base/patterns/mobile/mobile-secure-storage.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/mobile/mobile-secure-storage.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Mobile Secure Storage

Status: Draft | Catalog ID: MOB-002 | Owner: @tech-lead-mobile
Tier Applicability: T0, T1

## Problem Statement

- Access tokens stored in `UserDefaults` (iOS) or `SharedPreferences` (Android)
  are readable by any app on a rooted device and by backup/restore processes.
- PCI-DSS §3 prohibits storing sensitive cardholder data without encryption;
  transaction history containing masked PANs is stored unencrypted in SQLite.
- Biometric-gated private keys must be hardware-backed (Secure Enclave / StrongBox)
  to meet NIST SP 800-63B AAL2 requirements; current implementation stores keys
  in software-only keystore.
- SBV Circ. 09/2020 §III.2 requires banking app data at rest to be encrypted
  with keys meeting national cryptographic standards.

## Context

iOS: Keychain Services with `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` +
`kSecAttrTokenIDSecureEnclave` for hardware-backed keys. Android: `EncryptedSharedPreferences`
and `EncryptedFile` backed by Android Keystore with `STRONGBOX_REQUIRED`.

## Solution

```mermaid
graph TB
    APP[Mobile App] --> KM{Key Manager}
    KM -->|iOS| KC[Keychain Services\nSecure Enclave]
    KM -->|Android| AKS[Android Keystore\nStrongBox HSM]
    KC & AKS --> ENC[Encrypted Storage]
    ENC --> DATA[Token / PII / Queue]
```

## Implementation Guidelines

```swift
// iOS: Keychain wrapper for access token storage
import Security

class KeychainManager {
    static let shared = KeychainManager()

    func store(key: String, value: String) throws {
        let data = value.data(using: .utf8)!
        let query: [String: Any] = [
            kSecClass as String:            kSecClassGenericPassword,
            kSecAttrAccount as String:      key,
            kSecValueData as String:        data,
            kSecAttrAccessible as String:   kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
        ]
        SecItemDelete(query as CFDictionary) // remove existing
        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess else {
            throw KeychainError.storeFailed(status)
        }
    }

    func retrieve(key: String) throws -> String {
        let query: [String: Any] = [
            kSecClass as String:        kSecClassGenericPassword,
            kSecAttrAccount as String:  key,
            kSecReturnData as String:   true,
            kSecMatchLimit as String:   kSecMatchLimitOne,
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else {
            throw KeychainError.retrieveFailed(status)
        }
        return String(data: data, encoding: .utf8)!
    }
}
```

```kotlin
// Android: EncryptedSharedPreferences for token storage
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .setRequestStrongBoxBacked(true)    // require hardware-backed key
    .build()

val securePrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)

// Store access token
securePrefs.edit().putString("access_token", token).apply()

// Retrieve access token
val token = securePrefs.getString("access_token", null)
```

## When to Use

- All credential storage (access tokens, refresh tokens, biometric keys) on mobile.
- Any PII or financial data persisted on device (offline queue, transaction cache).

## When NOT to Use

- Truly public, non-sensitive data — use regular SQLite/UserDefaults for performance.

## Variants

- **Hardware Security Module (embedded)**: for highest assurance, use Secure Enclave (iOS) or StrongBox (Android) for key generation and storage.
- **App-level encryption**: if Keychain/Keystore not available (old OS), AES-GCM with key derived from user PIN via Argon2.

## NFR Acceptance Criteria

| Metric | T0 | T1 |
|--------|----|----|
| Key storage | Hardware Secure Enclave / StrongBox | Hardware-preferred, software fallback |
| Read latency p99 (Keychain/Keystore) | ≤ 50 ms | ≤ 100 ms |
| Data accessible after device wipe | Never (ThisDeviceOnly) | Never |
| Backup exclusion | kSecAttrSynchronizable=NO | Keystore not exported |
| Availability | ≥ 99.99 % | ≥ 99.9 % |
| RTO | ≤ 4 h | ≤ 8 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | NIST SP 800-63B | Hardware-backed key storage satisfies AAL2 authenticator requirements for biometric authentication |
| Ring 1 | PCI-DSS 4.0 §3.5 | Access tokens and PANs encrypted at rest using hardware-backed AES-256-GCM |
| Ring 2 | SBV Circ. 09/2020 §III.2 | Banking app credentials stored in hardware security module per SBV mobile banking security requirements ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Secure Enclave / Android Keystore: on-device hardware, zero cost. EncryptedSharedPreferences:
open source Jetpack Security library.

## Threat Model

- **Root/jailbreak**: `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` rejects access
  from processes outside the app sandbox; `STRONGBOX_REQUIRED` requires physical
  hardware key.
- **Backup extraction**: `kSecAttrSynchronizable=NO` prevents iCloud backup;
  Android `allowBackup=false` in Manifest.
- **Memory scraping**: zero-out token byte arrays after use in memory.

## Operational Runbook Stub

1. **Token not found after OS upgrade** → iOS: Keychain data preserved across upgrade; Android: Keystore keys may need re-enrolment if factory reset protection triggered.
2. **StrongBox not available** → device does not have StrongBox; fall back to TEE-backed Keystore; log `device.security_level=TEE`.
3. **Keychain access denied** → check `kSecAttrAccessControl` flags; verify app entitlement `keychain-access-groups`.

## Test Strategy Stub

- Unit: store → retrieve → assert identical value; assert value absent after `SecItemDelete`.
- Security: root emulator; assert token not retrievable from file system paths.
- Jailbreak detection: assert app refuses to run in jailbroken environment (see MOB-003).

## Related Patterns

- [MOB-003 Mobile Biometric Auth](mobile-biometric-auth.md) — biometric gates Keychain key access.
- [MOB-001 Mobile Offline Queue](mobile-offline-queue.md) — queue encrypted using Keychain-derived key.
- [SEC-007 Secrets Rotation](../security/secrets-rotation.md) — server-side secret rotation pairs with client refresh token rotation.

## References

- Apple Keychain Services documentation.
- Android EncryptedSharedPreferences documentation (Jetpack Security).

## Key Takeaway

Use `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` on iOS and
`setRequestStrongBoxBacked(true)` on Android — never use `UserDefaults` or
`SharedPreferences` for tokens or PII.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/mobile/mobile-secure-storage.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `mobile-secure-storage.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/mobile/mobile-secure-storage.md
git commit -m "feat(catalog): MOB-002 Mobile Secure Storage — Wave 3C"
```

---

### Task 32: MOB-003 Mobile Biometric Auth

**Files:**
- Modify: `knowledge-base/patterns/mobile/mobile-biometric-auth.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/mobile/mobile-biometric-auth.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Mobile Biometric Authentication

Status: Draft | Catalog ID: MOB-003 | Owner: @tech-lead-mobile
Tier Applicability: T0, T1

## Problem Statement

- 6-digit PIN authentication is shoulder-surfable; biometric adds a second factor
  without friction but current implementation stores biometric-derived key in
  software-only keystore (not Secure Enclave), defeating the hardware guarantee.
- iOS Face ID result not cryptographically bound to the payment request — attacker
  with physical access can accept a Face ID prompt intended for a different action.
- Biometric enrolment not tied to a server-side attestation; bank cannot verify
  the device has genuine biometric hardware vs. software emulation.
- Fall-back to PIN is mandatory (biometric can fail); fallback flow not implemented.

## Context

NIST SP 800-63B §5.1.9 defines biometric authenticator requirements. On iOS,
`LocalAuthentication` gates access to a Secure Enclave private key. On Android,
`BiometricPrompt` gates access to a Keystore key. The private key signs a
server-issued challenge — the server verifies the signature against the stored
public key (challenge-response, not biometric template transfer).

## Solution

```mermaid
sequenceDiagram
    participant USER
    participant APP
    participant SE as Secure Enclave\n/ Android Keystore
    participant API
    Note over APP,SE: Enrolment (once)
    APP->>SE: Generate key pair (biometric-gated)
    SE-->>APP: Public key
    APP->>API: POST /biometric/enroll {publicKey, deviceId}
    API-->>APP: 200 OK (public key stored server-side)
    Note over APP,API: Authentication
    APP->>API: GET /biometric/challenge
    API-->>APP: {challenge: "abc123", expiresIn: 30}
    APP->>SE: Sign(challenge) — requires biometric
    USER->>APP: Authenticates with Face ID / Fingerprint
    SE-->>APP: signature
    APP->>API: POST /biometric/verify {signature, deviceId}
    API-->>APP: JWT access token
```

## Implementation Guidelines

```swift
// iOS: biometric-gated private key signing
import LocalAuthentication
import CryptoKit

class BiometricAuthManager {
    private let keyTag = "com.bank.biometric.private-key"

    func generateKey() throws -> P256.Signing.PublicKey {
        var error: Unmanaged<CFError>?
        let access = SecAccessControlCreateWithFlags(
            nil,
            kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            [.privateKeyUsage, .biometryCurrentSet],  // invalidate on biometric change
            &error
        )!
        let attrs: [String: Any] = [
            kSecAttrKeyType as String:        kSecAttrKeyTypeECSECPrimeRandom,
            kSecAttrKeySizeInBits as String:  256,
            kSecAttrTokenID as String:        kSecAttrTokenIDSecureEnclave,
            kSecPrivateKeyAttrs as String: [
                kSecAttrIsPermanent as String:    true,
                kSecAttrApplicationTag as String: keyTag.data(using: .utf8)!,
                kSecAttrAccessControl as String:  access,
            ],
        ]
        guard let privateKey = SecKeyCreateRandomKey(attrs as CFDictionary, &error) else {
            throw BiometricError.keyGenerationFailed(error!.takeRetainedValue())
        }
        let publicKey = SecKeyCopyPublicKey(privateKey)!
        return publicKey
    }

    func sign(challenge: Data) async throws -> Data {
        let context = LAContext()
        context.localizedReason = "Authenticate to confirm payment"
        guard try await context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics,
                                               localizedReason: "Authenticate") else {
            throw BiometricError.authenticationFailed
        }
        // retrieve key and sign challenge
        let privateKey = try retrievePrivateKey()
        return try sign(challenge, with: privateKey)
    }
}
```

## When to Use

- Re-authentication for high-value payment flows.
- App unlock as an alternative to PIN.
- NIST AAL2 multi-factor authentication (possession + biometric).

## When NOT to Use

- Biometric not available or not enrolled (mandatory PIN fallback).
- Shared/kiosk devices where biometric is inappropriate.

## Variants

- **FIDO2/WebAuthn**: standard protocol for cross-platform biometric auth;
  iOS Passkeys and Android FIDO2 API are the preferred implementation.
- **App-level biometric**: `LAContext.evaluatePolicy` only — simpler but does not
  cryptographically bind the biometric to the specific transaction.

## NFR Acceptance Criteria

| Metric | T0 | T1 |
|--------|----|----|
| Biometric prompt latency | ≤ 300 ms | ≤ 500 ms |
| Challenge-response round trip p99 | ≤ 500 ms | ≤ 1 s |
| Hardware-backed key requirement | Mandatory (Secure Enclave / StrongBox) | Mandatory |
| Fallback to PIN | Mandatory after 3 biometric failures | Mandatory |
| Availability | ≥ 99.99 % | ≥ 99.9 % |
| RTO | ≤ 4 h | ≤ 8 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | NIST SP 800-63B §5.1.9 | Biometric matched on device; private key hardware-bound; challenge signed server-verifiable |
| Ring 1 | PCI-DSS 4.0 §8.3.2 | Multi-factor authentication for payment confirmation: possession (device) + inherence (biometric) |
| Ring 2 | SBV Circ. 09/2020 §III.2 | Banking mobile app biometric authentication meets SBV mobile security requirements ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

LocalAuthentication / BiometricPrompt: on-device, zero cost. Challenge API endpoint:
one GET + one POST per authentication — negligible compute cost at 100,000 auth/day.

## Threat Model

- **Biometric spoofing**: Secure Enclave and StrongBox certified to evaluate
  spoof-resistance per FIDO Alliance standards.
- **Challenge replay**: challenge has 30-second expiry; server tracks used challenges
  in Redis for the expiry window.
- **Biometric change (face surgery)**: `.biometryCurrentSet` key access flag
  invalidates key when biometric template changes; triggers re-enrolment.

## Operational Runbook Stub

1. **User cannot authenticate after Face ID update** → biometric key invalidated; prompt re-enrolment (re-generate key pair).
2. **Server challenge endpoint returning 5xx** → biometric login degraded; fall back to PIN/OTP.
3. **Signature verification failing** → check key pair: device may have re-generated Secure Enclave key after device restore; re-enrol.

## Test Strategy Stub

- Unit (iOS): mock `LAContext`; assert `sign()` returns data of non-zero length.
- Integration: enrol device; fetch challenge; sign; verify — assert JWT token returned.
- Security: replay signed challenge after expiry; assert server rejects with 401.

## Related Patterns

- [MOB-002 Mobile Secure Storage](mobile-secure-storage.md) — Keychain stores biometric key.
- [SEC-006 JWT Best Practices](../security/jwt-best-practices.md) — JWT issued after biometric verification.
- [SEC-011 Session Revocation](../security/session-revocation.md) — revoke biometric session on device loss.

## References

- NIST SP 800-63B: Digital Identity Guidelines, Section 5.1.9.
- Apple LocalAuthentication framework documentation.
- Android BiometricPrompt documentation.

## Key Takeaway

Generate the key pair on device with `.biometryCurrentSet` access control; the
server verifies a signed challenge — never transmit biometric templates off-device.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/mobile/mobile-biometric-auth.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `mobile-biometric-auth.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/mobile/mobile-biometric-auth.md
git commit -m "feat(catalog): MOB-003 Mobile Biometric Auth — Wave 3C"
```

---

### Task 33: MOB-004 Mobile Push Notification (Secure)

**Files:**
- Modify: `knowledge-base/patterns/mobile/mobile-push-notification-secure.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/mobile/mobile-push-notification-secure.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Mobile Push Notification (Secure)

Status: Draft | Catalog ID: MOB-004 | Owner: @tech-lead-mobile
Tier Applicability: T1, T2

## Problem Statement

- Push notifications containing "Your transfer of 5,000,000 VND to Nguyen Van A
  is confirmed" expose transaction amounts and beneficiary names on the device
  lock screen, visible to anyone nearby.
- Decree 13/2023 Article 13 requires minimising personal data shared; current
  push payloads contain full name, account number, and amount.
- APNs/FCM deliver push messages to all devices registered for a user; a stolen
  phone receives the same notification as the legitimate device.
- Silent push for transaction confirmation is delayed 30–60 s by iOS power
  management, making the fetch-on-wake approach unreliable for real-time alerts.

## Context

The secure push pattern sends an empty-payload "data push" (APNs `content-available`
/ FCM `data` message) that wakes the app. The app fetches the notification payload
on wake using the authenticated session. Only then is a local notification
displayed — the full payload never transits APNs/FCM.

## Solution

```mermaid
sequenceDiagram
    participant SERVER as Notification Service
    participant APNS as APNs / FCM
    participant APP as Mobile App
    participant API
    SERVER->>APNS: Silent push\n{content-available: 1, data: {notifId: "x1"}}
    APNS->>APP: Wake app (background fetch)
    APP->>API: GET /notifications/x1\n(Authorization: Bearer JWT)
    API-->>APP: {type: "TRANSFER", amount: 5000000, beneficiary: "Nguyen Van A"}
    APP->>APP: Display local notification\nwith fetched payload
```

## Implementation Guidelines

```swift
// iOS: handle silent push and fetch payload
func application(_ application: UIApplication,
                 didReceiveRemoteNotification userInfo: [AnyHashable: Any],
                 fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
    guard let notifId = userInfo["notifId"] as? String else {
        completionHandler(.noData)
        return
    }
    Task {
        do {
            let notification = try await NotificationAPI.fetch(id: notifId)
            await displayLocalNotification(notification)
            completionHandler(.newData)
        } catch {
            completionHandler(.failed)
        }
    }
}

func displayLocalNotification(_ notification: BankNotification) async {
    let content = UNMutableNotificationContent()
    content.title = notification.title   // e.g., "Transfer Confirmed"
    content.body  = notification.body    // e.g., "5,000,000 ₫ → Nguyen Van A"
    content.sound = .default
    let request = UNNotificationRequest(
        identifier: notification.id,
        content: content,
        trigger: nil)
    try? await UNUserNotificationCenter.current().add(request)
}
```

```kotlin
// Android: FCM data message handler
class BankFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val notifId = message.data["notifId"] ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notification = NotificationApi.fetch(notifId, getAccessToken())
                showLocalNotification(notification)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch notification $notifId", e)
            }
        }
    }
}
```

## When to Use

- All transaction confirmation, balance change, and security alert notifications.
- Any notification containing personal data (name, amount, account number).

## When NOT to Use

- Marketing notifications with no personal data — full-payload push is fine.
- Notifications requiring immediate foreground display before app launch.

## Variants

- **Notification Service Extension (iOS)**: intercept notification in extension,
  fetch payload, modify content before display — does not require app wake.
- **Encrypted payload**: encrypt push payload with device public key; app decrypts
  locally — allows full payload delivery without server round-trip.

## NFR Acceptance Criteria

| Metric | T1 | T2 |
|--------|----|----|
| Silent push to local notification display | ≤ 3 s (foreground) | ≤ 10 s (background) |
| Fetch API latency p99 | ≤ 500 ms | ≤ 1 s |
| Push delivery success rate | ≥ 99 % | ≥ 95 % |
| Availability | ≥ 99.9 % | ≥ 99.5 % |
| RTO | ≤ 8 h | ≤ 24 h |
| RPO | ≤ 1 h | ≤ 4 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.13.2.1 | Push payload not transmitted via APNs/FCM; fetched over TLS-secured authenticated API |
| Ring 1 | PCI-DSS 4.0 §4.2 | Transaction data (amount, beneficiary) never included in push payload traversing third-party (Apple/Google) infrastructure |
| Ring 2 | Decree 13/2023 Art. 13 | Personal data minimised in push: only notification ID sent via APNs/FCM; full data fetched on authenticated channel ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

APNs/FCM: free for up to 1M messages/day. Notification fetch API: 1 GET per
push ≈ 100M GETs/month at 1M daily active users — at $0.001/1000 requests = $100/month.

## Threat Model

- **Notification ID enumeration**: notification IDs are UUIDs (128-bit entropy);
  fetch API validates JWT ownership of notification.
- **APNs token hijack**: APNs device tokens invalidated on device wipe and app
  uninstall; server removes stale tokens on APNs feedback.

## Operational Runbook Stub

1. **Push not delivered** → check APNs/FCM delivery report; verify device token valid; check notification service logs.
2. **App not waking on silent push** → check iOS Background App Refresh enabled; APNs `apns-priority: 5` (low) required for silent push.
3. **Notification fetch failing** → check notification API health; app falls back to polling on next foreground.

## Test Strategy Stub

- Integration: send silent push to simulator; assert app wakes and displays local notification within 3 s.
- Security: fetch notification with JWT from different user; assert 403.
- Load: 10,000 concurrent notification fetches; assert p99 ≤ 500 ms.

## Related Patterns

- [MOB-002 Mobile Secure Storage](mobile-secure-storage.md) — access token stored in Keychain for fetch API auth.
- [SEC-013 PII Tokenization](../security/pii-tokenization-format-preserving.md) — beneficiary name tokenized in push trigger.
- [INT-011 CloudEvents Envelope](../integration/cloudevents-envelope.md) — notification trigger published as CloudEvent.

## References

- Apple documentation: Pushing Background Updates to Your App.
- Google Firebase Cloud Messaging: Data Messages.

## Key Takeaway

Send only `notifId` via APNs/FCM; fetch the full payload over an authenticated
API after wake — transaction amounts and beneficiary names must never transit
Apple or Google infrastructure.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/mobile/mobile-push-notification-secure.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `mobile-push-notification-secure.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/mobile/mobile-push-notification-secure.md
git commit -m "feat(catalog): MOB-004 Mobile Push Notification Secure — Wave 3C"
```

---

### Task 34: MOB-005 Mobile Deep Link Attestation

**Files:**
- Modify: `knowledge-base/patterns/mobile/mobile-deep-link-attestation.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/mobile/mobile-deep-link-attestation.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Mobile Deep Link Attestation

Status: Draft | Catalog ID: MOB-005 | Owner: @tech-lead-mobile
Tier Applicability: T0, T1

## Problem Statement

- Banking payment deep links (`bank://pay?amount=100&to=0987654321`) use a
  custom URI scheme; malicious apps register the same scheme and intercept
  payment initiation links, redirecting funds.
- QR-code-based payment links passed via email or SMS are intercepted by
  malicious apps that claim to handle the custom scheme.
- iOS Universal Links / Android App Links require server-side JSON file
  hosting; current implementation uses custom scheme only with no attestation.
- Deep link parameters (amount, beneficiary) accepted without signature
  verification; attackers craft malicious QR codes with tampered amounts.

## Context

iOS Universal Links require the bank's server to host
`/.well-known/apple-app-site-association` (AASA) with the app's team ID and bundle ID.
Android App Links require `/.well-known/assetlinks.json` with the app's SHA-256
fingerprint. The OS validates these files before routing the deep link — malicious
apps cannot claim HTTPS deep links.

## Solution

```mermaid
sequenceDiagram
    participant QR as QR Code / Link\nhttps://bank.internal/pay?token=xyz
    participant OS as iOS / Android
    participant SERVER as bank.internal
    participant APP as Bank App
    OS->>SERVER: GET /.well-known/apple-app-site-association
    SERVER-->>OS: {apps: ["TEAMID.com.bank.app"]}
    OS->>APP: Route deep link (verified)
    APP->>SERVER: GET /pay/resolve?token=xyz
    SERVER-->>APP: {amount: 100000, beneficiary: "...", signature: "..."}
    APP->>APP: Verify server signature on payload
    APP-->>USER: Confirm payment screen
```

## Implementation Guidelines

```json
// /.well-known/apple-app-site-association (AASA)
{
  "applinks": {
    "substitutionVariables": {},
    "details": [
      {
        "appIDs": ["A1B2C3D4E5.com.bank.app"],
        "components": [
          { "/": "/pay/*", "comment": "Payment deep links" },
          { "/": "/transfer/*", "comment": "Transfer deep links" }
        ]
      }
    ]
  }
}
```

```json
// /.well-known/assetlinks.json (Android)
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.bank.app",
    "sha256_cert_fingerprints": ["AB:CD:EF:..."]
  }
}]
```

```swift
// iOS: handle verified Universal Link
func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
    guard userActivity.activityType == NSUserActivityTypeBrowsingWeb,
          let url = userActivity.webpageURL,
          let token = URLComponents(url: url, resolvingAgainstBaseURL: true)?
              .queryItems?.first(where: { $0.name == "token" })?.value else { return }

    Task {
        let payload = try await PaymentAPI.resolve(token: token)
        // Verify server signature to prevent parameter tampering
        guard try verifySignature(payload) else {
            showError("Payment link is invalid or expired.")
            return
        }
        navigator.presentPaymentConfirmation(payload)
    }
}
```

## When to Use

- QR code payment initiation.
- In-app payment links from SMS/email/notifications.
- Any deep link carrying financial parameters.

## When NOT to Use

- Internal app navigation with no external link source.

## Variants

- **Signed QR codes**: server signs the deep link URL with ECDSA; app verifies
  signature before processing — prevents QR code tampering even if AASA is bypassed.
- **Short-lived tokens**: deep link carries opaque token (UUID), resolved server-side;
  amount/beneficiary never in URL — prevents parameter manipulation entirely.

## NFR Acceptance Criteria

| Metric | T0 | T1 |
|--------|----|----|
| AASA / assetlinks.json response time | ≤ 200 ms | ≤ 500 ms |
| Deep link resolution p99 | ≤ 500 ms | ≤ 1 s |
| Custom URI scheme usage | Forbidden (HTTPS only) | Forbidden |
| Availability | ≥ 99.99 % | ≥ 99.9 % |
| RTO | ≤ 4 h | ≤ 8 h |
| RPO | ≤ 15 min | ≤ 1 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.14.1.3 | Deep link parameters cryptographically signed; unsigned links rejected before display |
| Ring 1 | PCI-DSS 4.0 §6.2 | Payment deep links use HTTPS Universal Links / App Links; no custom URI scheme for financial transactions |
| Ring 2 | SBV Circ. 09/2020 §III | QR code payment links to banking app comply with SBV QR code payment standards ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

AASA and assetlinks.json: static JSON files on CDN, negligible cost. Deep link
resolution API: low volume (payment initiations), negligible compute.

## Threat Model

- **AASA file hijack**: hosted on `bank.internal` with HSTS + CAA DNS records; TLS
  certificate pinned in app for the AASA host.
- **Token replay**: tokens are single-use with 5-minute expiry; server marks token
  consumed on first resolution.

## Operational Runbook Stub

1. **AASA not recognised by iOS** → verify `Content-Type: application/json` header; no redirect on AASA URL (iOS requires direct 200).
2. **App Links not working on Android** → run `adb shell pm verify-app-links --re-verify com.bank.app`; check assetlinks.json fingerprint matches release keystore.
3. **Token expired on scan** → QR code display shows 5-minute countdown; refresh server-side token on expiry.

## Test Strategy Stub

- Integration: scan AASA via `curl https://bank.internal/.well-known/apple-app-site-association`; validate JSON schema.
- Device test: open Universal Link on iOS; assert bank app opens (not Safari).
- Security: modify `amount` parameter in deep link; assert signature verification fails and payment blocked.

## Related Patterns

- [MOB-004 Mobile Push Notification Secure](mobile-push-notification-secure.md) — push triggers deep link for payment confirmation.
- [SEC-006 JWT Best Practices](../security/jwt-best-practices.md) — token in deep link signed with RS256.
- [BSP-002 Idempotent Payment Key](../banking-solutions/idempotent-payment-key.md) — deep link token maps to idempotency key.

## References

- Apple documentation: Allowing Apps and Websites to Link to Your Content.
- Android documentation: Android App Links.

## Key Takeaway

Host AASA/assetlinks.json for all payment paths; use opaque server-resolved tokens
(not amounts in URL) — custom URI schemes (`bank://`) must be replaced with HTTPS
Universal Links for all financial deep links.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/mobile/mobile-deep-link-attestation.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `mobile-deep-link-attestation.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/mobile/mobile-deep-link-attestation.md
git commit -m "feat(catalog): MOB-005 Mobile Deep Link Attestation — Wave 3C"
```

---

### Task 35: MOB-006 Mobile Force-Upgrade

**Files:**
- Modify: `knowledge-base/patterns/mobile/mobile-force-upgrade.md`

- [ ] **Step 1: Verify stub**

```bash
head -4 knowledge-base/patterns/mobile/mobile-force-upgrade.md
```
Expected: `Status: Proposed`

- [ ] **Step 2: Write full document**

```markdown
# Mobile Force-Upgrade

Status: Draft | Catalog ID: MOB-006 | Owner: @tech-lead-mobile
Tier Applicability: T0, T1, T2

## Problem Statement

- A critical security vulnerability patched in v2.5.0 still affects 15% of users
  on v2.4.x; bank cannot enforce the patch because force-upgrade is not implemented.
- PCI-DSS §6.3 requires that all payment-handling software addresses critical
  vulnerabilities within defined timeframes; unpatched app versions violate this.
- Users on deprecated API versions (banking app v1.x) hit API errors because the
  old API was removed; bank has no mechanism to block them from transacting.
- Soft prompts ("Update available") are dismissed; hard block on minimum version
  is necessary for security-critical patches.

## Context

The force-upgrade service exposes a version-check API. The app calls this endpoint
on launch and compares its current version against the minimum supported and
recommended versions. Below minimum: show blocking screen with App Store deep link.
Below recommended: show dismissible banner.

## Solution

```mermaid
flowchart TB
    LAUNCH[App Launch] --> CHECK[GET /version-check\n{platform, currentVersion}]
    CHECK --> RESP{Response}
    RESP -->|currentVersion < minimumVersion| BLOCK[Blocking Screen\nForce Update Required\nApp Store link]
    RESP -->|currentVersion < recommendedVersion| BANNER[Dismissible Banner\nUpdate Available]
    RESP -->|currentVersion >= recommendedVersion| CONTINUE[Normal App Flow]
    BLOCK --> STORE[Open App Store /\nPlay Store]
```

## Implementation Guidelines

```swift
// iOS: version check on app launch
struct VersionCheckResponse: Decodable {
    let minimumVersion: String   // e.g., "2.5.0"
    let recommendedVersion: String
    let forceUpgradeMessage: String
    let appStoreUrl: String
}

class VersionGate {
    func checkOnLaunch() async {
        let currentVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as! String
        let platform = "iOS"
        guard let url = URL(string: "https://api.bank.internal/v1/version-check?platform=\(platform)") else { return }

        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let response = try JSONDecoder().decode(VersionCheckResponse.self, from: data)
            if currentVersion.compare(response.minimumVersion, options: .numeric) == .orderedAscending {
                showForceUpgradeScreen(message: response.forceUpgradeMessage, url: response.appStoreUrl)
            } else if currentVersion.compare(response.recommendedVersion, options: .numeric) == .orderedAscending {
                showSoftUpgradeBanner()
            }
        } catch {
            // Network error: allow app to proceed (fail open for upgrades)
            // Log to analytics for monitoring
        }
    }

    func showForceUpgradeScreen(message: String, url: String) {
        // Present non-dismissible UIViewController covering entire screen
        // Only action: "Update Now" button deep linking to App Store
    }
}
```

```kotlin
// Android: version check via coroutine on app startup
class VersionGateViewModel(private val api: VersionCheckApi) : ViewModel() {
    val upgradeState = MutableStateFlow<UpgradeState>(UpgradeState.Loading)

    fun checkVersion() {
        viewModelScope.launch {
            try {
                val current = BuildConfig.VERSION_NAME
                val response = api.checkVersion(platform = "Android", currentVersion = current)
                upgradeState.value = when {
                    isOlderThan(current, response.minimumVersion) ->
                        UpgradeState.ForceUpgrade(response.forceUpgradeMessage, response.playStoreUrl)
                    isOlderThan(current, response.recommendedVersion) ->
                        UpgradeState.SoftUpgrade
                    else -> UpgradeState.UpToDate
                }
            } catch (e: Exception) {
                upgradeState.value = UpgradeState.UpToDate // fail open
            }
        }
    }
}
```

Version check API (server-side): YAML config file in Git; update via CI/CD without
full API deployment:
```yaml
# version-matrix.yml
ios:
  minimumVersion: "2.5.0"
  recommendedVersion: "2.6.1"
  forceUpgradeMessage: "A critical security update is required. Please update the Techcombank app to continue."
  appStoreUrl: "https://apps.apple.com/app/id123456789"
android:
  minimumVersion: "2.5.0"
  recommendedVersion: "2.6.1"
  forceUpgradeMessage: "A critical security update is required."
  playStoreUrl: "https://play.google.com/store/apps/details?id=com.bank.app"
```

## When to Use

- Any security patch that must be enforced across all users.
- API version deprecation requiring client app upgrades.
- Regulatory requirements mandating all users run a certified app version.

## When NOT to Use

- Minor feature releases where old version is fully compatible.

## Variants

- **Firebase Remote Config**: stores version matrix in Firebase; updates without
  API deployment; works offline via cached config.
- **App Store Connect / Play Console**: built-in staged rollout but no force block
  without a custom version-check endpoint.

## NFR Acceptance Criteria

| Metric | T0 | T1 | T2 |
|--------|----|----|-----|
| Version check latency p99 | ≤ 500 ms | ≤ 1 s | ≤ 2 s |
| Force-upgrade enforcement delay after config change | ≤ 5 min | ≤ 30 min | ≤ 1 h |
| Force-upgrade screen dismissibility | Never | Never | Never |
| Fail-open (network error) | Yes | Yes | Yes |
| Availability | ≥ 99.99 % | ≥ 99.9 % | ≥ 99.5 % |
| RTO | ≤ 4 h | ≤ 8 h | ≤ 24 h |

## Compliance Mapping

| Ring | Standard | Control |
|------|----------|---------|
| Ring 0 | ISO 27001 A.12.6.1 | Force-upgrade ensures all users run software with known vulnerabilities patched |
| Ring 1 | PCI-DSS 4.0 §6.3.3 | Critical security patches enforced on all payment-handling app instances within defined timeframe |
| Ring 2 | SBV Circ. 09/2020 §IV.1 | Banking mobile app version management ensures users run SBV-approved versions with current security controls ⚠️ (working summary — pending Legal review) |

## Cost / FinOps

Version check API: 2 replicas × 0.25 vCPU / 256 MB ≈ $15/month (very low traffic;
called once per app launch). Version matrix YAML in Git: zero additional cost.

## Threat Model

- **Version check endpoint unavailable**: app fails open (proceeds normally) to
  avoid blocking legitimate users; monitor endpoint availability ≥ 99.99%.
- **Version matrix misconfiguration**: setting minimumVersion too high blocks all
  users; require two-engineer approval (4-eyes) for minimumVersion changes.

## Operational Runbook Stub

1. **Emergency force-upgrade** → update `minimumVersion` in `version-matrix.yml` → PR with 4-eyes approval → merge → CI deploys in < 5 min.
2. **Version check endpoint 5xx** → app proceeds normally; monitor and fix within 30 min; users not blocked.
3. **App Store deployment delayed** → do not set `minimumVersion` to new version until App Store review completes and ≥ 90% of users have updated.

## Test Strategy Stub

- Unit (iOS): mock version-check response with `minimumVersion > currentVersion`; assert force-upgrade screen shown.
- Unit: mock `minimumVersion ≤ currentVersion ≤ recommendedVersion`; assert soft banner shown.
- Unit: simulate network error; assert normal app flow (fail open).
- E2E: Espresso/XCUITest — assert force-upgrade screen has no dismiss mechanism.

## Related Patterns

- [FE-006 Web i18n RTL](../frontend/web-i18n-rtl.md) — force-upgrade messages localised in Vietnamese.
- [FE-004 Web Feature Flags](../frontend/web-feature-flags.md) — feature flags and force-upgrade complement each other for release safety.
- [SEC-007 Secrets Rotation](../security/secrets-rotation.md) — forced upgrade may be required after server-side key rotation.

## References

- Apple App Store Guidelines §2.5.4.
- Google Play Developer Policy: Updates.

## Key Takeaway

Never set `minimumVersion` until the new version is live in the App Store and
available to all users — configure `fail open` on network error so the version
check endpoint outage never blocks legitimate users.
```

- [ ] **Step 3: Lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/mobile/mobile-force-upgrade.md
```
Expected: exits 0.

- [ ] **Step 4: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for `mobile-force-upgrade.md`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/patterns/mobile/mobile-force-upgrade.md
git commit -m "feat(catalog): MOB-006 Mobile Force-Upgrade — Wave 3C"
```

---

## Wave 3C Gate — Frontend + Mobile (12 docs)

- [ ] **Step 1: Batch lint**

```bash
for f in \
  knowledge-base/patterns/frontend/web-performance-budgets.md \
  knowledge-base/patterns/frontend/web-resilience-offline-first.md \
  knowledge-base/patterns/frontend/web-csp-hardening.md \
  knowledge-base/patterns/frontend/web-feature-flags.md \
  knowledge-base/patterns/frontend/web-error-boundary.md \
  knowledge-base/patterns/frontend/web-i18n-rtl.md \
  knowledge-base/patterns/mobile/mobile-offline-queue.md \
  knowledge-base/patterns/mobile/mobile-secure-storage.md \
  knowledge-base/patterns/mobile/mobile-biometric-auth.md \
  knowledge-base/patterns/mobile/mobile-push-notification-secure.md \
  knowledge-base/patterns/mobile/mobile-deep-link-attestation.md \
  knowledge-base/patterns/mobile/mobile-force-upgrade.md; do
  bash scripts/mermaid-lint-doc.sh "$f" || echo "FAIL: $f"
done
```
Expected: 12 files, 0 FAIL lines.

- [ ] **Step 2: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for any Wave 3C file.

- [ ] **Step 3: Update catalog — FE-001–006, MOB-001–006 → Draft**

In `governance/standards/enterprise-architecture-catalog.md`, change all 12 rows from `Proposed` → `Draft`.

```bash
grep -E "(FE-00[1-6]|MOB-00[1-6])" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |"
```
Expected: 12 lines.

- [ ] **Step 4: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md
git commit -m "chore(catalog): Wave 3C gate — FE-001–006 + MOB-001–006 promoted to Draft"
```

---

## Wave 3D — Banking Solutions + Reference Architectures (13 docs)

### Task 36: BSP-001 Double-Entry Ledger

**Files:**
- Create: `knowledge-base/patterns/banking-solutions/double-entry-ledger.md`
- Modify: `governance/standards/enterprise-architecture-catalog.md` (at Wave 3D Gate)

- [ ] **Step 1: Write the document**

```markdown
---
id: BSP-001
title: "Double-Entry Ledger Pattern"
domain: banking-solutions
status: Draft
tier: Tier 1
owner: Core Banking Team
compliance: [BCBS-239, SBV-09-2020]
date: 2026-05-10
wave: Wave 3D
---

# Double-Entry Ledger Pattern

## Problem Statement
Every financial transaction must produce a balanced accounting record:
every debit must have an equal and opposite credit. Ad-hoc balance
calculations over raw transaction rows are fragile, slow, and
audit-unfriendly.

## Context
Core banking systems maintain current and savings accounts. Regulatory
reports (SBV Circular 09/2020, BCBS 239) require reconcilable ledger
balances with full audit trail. The pattern applies to any monetary
movement: deposits, withdrawals, transfers, fee postings.

## Solution

```mermaid
sequenceDiagram
    participant API as Payments API
    participant SVC as LedgerService
    participant DB  as PostgreSQL
    API->>SVC: postTransaction(txn)
    SVC->>DB: BEGIN
    SVC->>DB: INSERT ledger_entry (debit side)
    SVC->>DB: INSERT ledger_entry (credit side)
    SVC->>DB: UPDATE account_balance (debit account)
    SVC->>DB: UPDATE account_balance (credit account)
    SVC->>DB: ASSERT SUM(amount) = 0
    SVC->>DB: COMMIT
    DB-->>SVC: ok
    SVC-->>API: TransactionReceipt
```

### Core Schema

```sql
CREATE TABLE ledger_entry (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    txn_ref       VARCHAR(36)   NOT NULL,
    account_id    VARCHAR(20)   NOT NULL,
    entry_type    CHAR(1)       NOT NULL CHECK (entry_type IN ('D','C')),
    amount        NUMERIC(20,2) NOT NULL CHECK (amount > 0),
    currency      CHAR(3)       NOT NULL,
    value_date    DATE          NOT NULL,
    booking_date  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    narrative     VARCHAR(255),
    created_by    VARCHAR(50)   NOT NULL
);

CREATE TABLE account_balance (
    account_id    VARCHAR(20)   PRIMARY KEY,
    currency      CHAR(3)       NOT NULL,
    balance       NUMERIC(20,2) NOT NULL DEFAULT 0,
    version       BIGINT        NOT NULL DEFAULT 0,
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);
```

### Java 21 LedgerService

```java
@Service
@Transactional
public class LedgerService {

    private final LedgerEntryRepository entryRepo;
    private final AccountBalanceRepository balanceRepo;

    public TransactionReceipt post(LedgerPost post) {
        validateBalanced(post);
        String txnRef = post.getTxnRef();

        for (LedgerLine line : post.getLines()) {
            LedgerEntry entry = LedgerEntry.builder()
                .txnRef(txnRef)
                .accountId(line.getAccountId())
                .entryType(line.getType())
                .amount(line.getAmount())
                .currency(line.getCurrency())
                .valueDate(line.getValueDate())
                .narrative(post.getNarrative())
                .createdBy(SecurityContextHolder.getContext()
                    .getAuthentication().getName())
                .build();
            entryRepo.save(entry);

            int sign = line.getType() == 'D' ? -1 : 1;
            balanceRepo.incrementBalance(
                line.getAccountId(), sign * line.getAmount());
        }
        return new TransactionReceipt(txnRef, Instant.now());
    }

    private void validateBalanced(LedgerPost post) {
        BigDecimal sum = post.getLines().stream()
            .map(l -> l.getType() == 'D'
                ? l.getAmount().negate() : l.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(BigDecimal.ZERO) != 0) {
            throw new UnbalancedLedgerException(
                "Entries sum to " + sum + ", must be 0");
        }
    }
}
```

## When to Use
- Any monetary movement in core banking (deposits, transfers, fees)
- Regulatory-reportable positions requiring reconcilable trail

## When Not to Use
- Non-monetary counters (event counts, inventory) — plain increment/decrement suffices
- Read-only reporting queries — use account_balance table directly

## Variants
- **Memo posting**: entries written with `status='MEMO'`, confirmed on clearing
- **Multi-currency**: add `base_amount` + `fx_rate` columns; assertions check base currency sum

## NFR Acceptance Criteria
- Ledger post p99 latency ≤ 50 ms under 500 TPS
- Balance drift = 0 (assertion enforced in DB constraint)
- Audit trail retained ≥ 7 years (per SBV Circular 09/2020)
- Recovery point objective ≤ 1 min (WAL streaming to standby)

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.12.4 | Immutable audit log of all ledger writes |
| Ring 1 | BCBS 239 Principle 2 | Accurate, complete position data |
| Ring 2 | SBV Circular 09/2020 Article 8 | Ledger balances reconciled daily ⚠️ (working summary — pending Legal review) |

## Cost / FinOps
- PostgreSQL storage: ~500 bytes/entry × 10 M entries/day = ~5 GB/day; partition by `booking_date`
- Archive entries > 90 days to cold storage (S3 Glacier) via pg_partman

## Threat Model
| Threat | Control |
|--------|---------|
| Fraudulent balance manipulation | DB assertion + HMAC audit chain (SEC-012) |
| Replayed transaction | Idempotency key on `txn_ref` (UNIQUE constraint) |
| Privilege escalation | `created_by` captured from JWT subject, not request body |

## Runbook Stub
- **Imbalance alert**: query `SELECT txn_ref, SUM(CASE WHEN entry_type='D' THEN -amount ELSE amount END) FROM ledger_entry GROUP BY txn_ref HAVING ABS(SUM(...)) > 0`
- **Balance drift**: compare `account_balance.balance` vs recalculated sum from `ledger_entry`

## Test Strategy Stub

```java
@Test void postTransferMustBalanceLedger() {
    LedgerPost post = LedgerPost.builder()
        .txnRef("TXN-001")
        .lines(List.of(
            LedgerLine.of("ACC-001", 'D', new BigDecimal("100.00"), "VND"),
            LedgerLine.of("ACC-002", 'C', new BigDecimal("100.00"), "VND")))
        .build();
    ledgerService.post(post);
    assertThat(entryRepo.findByTxnRef("TXN-001")).hasSize(2);
    assertThat(balanceRepo.findById("ACC-001").get().getBalance())
        .isEqualByComparingTo("-100.00");
}
```

## Related Patterns
- BSP-002 Idempotent Payment Key — prevents duplicate postings
- BSP-003 Sanction Screening — pre-posting AML check
- SEC-012 Immutable Audit Log — HMAC-chains ledger writes

## References
- BCBS 239 Principles for effective risk data aggregation
- SBV Circular 09/2020 on banking information systems

## Key Takeaway
Every monetary movement writes two (or more) balanced ledger entries
within a single database transaction; balance assertions enforce
correctness at the database layer.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/banking-solutions/double-entry-ledger.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/patterns/banking-solutions/double-entry-ledger.md
git commit -m "feat(catalog): BSP-001 Double-Entry Ledger — Wave 3D"
```

---

### Task 37: BSP-002 Idempotent Payment Key

**Files:**
- Create: `knowledge-base/patterns/banking-solutions/idempotent-payment-key.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: BSP-002
title: "Idempotent Payment Key Pattern"
domain: banking-solutions
status: Draft
tier: Tier 1
owner: Payments Team
compliance: [PCI-DSS, SBV-09-2020]
date: 2026-05-10
wave: Wave 3D
---

# Idempotent Payment Key Pattern

## Problem Statement
Mobile networks, retries, and load-balancer timeouts mean a payment
request may arrive more than once. Duplicate processing causes double
charges or double credits.

## Context
REST APIs handling payment initiation must deduplicate identical
re-submissions. Idempotency keys provide exactly-once semantics
without requiring distributed transactions.

## Solution

```mermaid
flowchart TB
    Client -->|POST /payments\nIdempotency-Key: k| API
    API --> Cache{Key in\nRedis?}
    Cache -->|yes| Return[Return cached response]
    Cache -->|no| Lock[Acquire advisory lock on key]
    Lock --> Process[Process payment]
    Process --> Store[Store result in Redis TTL 24h]
    Store --> Respond[Return 200 + PaymentReceipt]
```

### Java 21 Idempotency Filter

```java
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redis;
    private final ObjectMapper mapper;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
            HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String key = req.getHeader("Idempotency-Key");
        if (key == null || !req.getMethod().equals("POST")) {
            chain.doFilter(req, res); return;
        }

        String cacheKey = "idem:" + key;
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            res.setStatus(200);
            res.setContentType("application/json");
            res.getWriter().write(cached);
            return;
        }

        CachedBodyHttpServletResponse wrappedRes =
            new CachedBodyHttpServletResponse(res);
        chain.doFilter(req, wrappedRes);

        String body = wrappedRes.getCachedBody();
        if (res.getStatus() == 200) {
            redis.opsForValue().set(cacheKey, body,
                Duration.ofHours(24));
        }
    }
}
```

## When to Use
- POST endpoints that initiate payments, transfers, or ledger postings
- Any operation that must not be executed twice

## When Not to Use
- GET/read endpoints — inherently idempotent
- Idempotency windows > 24 h — use database-persisted idempotency table instead

## NFR Acceptance Criteria
- Duplicate detection rate = 100% within 24 h window
- Additional latency from idempotency check ≤ 2 ms (Redis p99)
- Key storage: Redis Cluster with AOF persistence, replication factor 3

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.12.1 | Controlled processing prevents duplicate charges |
| Ring 1 | PCI-DSS 6.4 | Payment integrity — no duplicate authorisations |
| Ring 2 | SBV Circular 09/2020 Article 12 | Transaction accuracy requirements ⚠️ (working summary — pending Legal review) |

## Threat Model
| Threat | Control |
|--------|---------|
| Key collision attack | Keys scoped to client_id + key value |
| Cache poisoning | Redis AUTH + TLS in transit |
| Replay beyond TTL | 24 h window; after expiry, new processing is correct |

## Related Patterns
- BSP-001 Double-Entry Ledger — idempotency prevents duplicate ledger entries
- BSP-005 Reversal and Chargeback — reversal itself must be idempotent

## Key Takeaway
Cache the response keyed on `Idempotency-Key` header in Redis for 24 h;
return the cached response on any re-submission without reprocessing.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/banking-solutions/idempotent-payment-key.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/patterns/banking-solutions/idempotent-payment-key.md
git commit -m "feat(catalog): BSP-002 Idempotent Payment Key — Wave 3D"
```

---

### Task 38: BSP-003 Sanction Screening Pipeline

**Files:**
- Create: `knowledge-base/patterns/banking-solutions/sanction-screening-pipeline.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: BSP-003
title: "Sanction Screening Pipeline"
domain: banking-solutions
status: Draft
tier: Tier 1
owner: AML/Compliance Team
compliance: [ISO-27001, SBV-09-2020, SWIFT-CSP]
date: 2026-05-10
wave: Wave 3D
---

# Sanction Screening Pipeline

## Problem Statement
Every outgoing and incoming payment must be screened against OFAC, UN,
EU, and local MPS sanction lists before settlement. Manual or
synchronous screening at posting time creates latency and blocking
failures.

## Context
The pattern applies to wire transfers, SWIFT messages, and ACH batches.
It runs pre-posting (synchronous for high-value) or post-booking
(asynchronous for bulk), with a hold queue for manual review.

## Solution

```mermaid
sequenceDiagram
    participant Pay as Payment API
    participant Screen as ScreeningService
    participant List as SanctionListCache(Redis)
    participant Queue as HoldQueue(Kafka)
    participant Ops as Ops Dashboard

    Pay->>Screen: screen(paymentDetails)
    Screen->>List: fuzzyMatch(name, country)
    List-->>Screen: matchScore
    alt score >= 80
        Screen->>Queue: publish(HOLD, payment)
        Queue-->>Ops: alert
        Screen-->>Pay: HOLD (202)
    else score < 80
        Screen-->>Pay: CLEAR (200)
    end
```

### Java 21 Screening Service

```java
@Service
public class SanctionScreeningService {

    private static final int HOLD_THRESHOLD = 80;
    private final SanctionListRepository listRepo;

    public ScreeningResult screen(PaymentParty party) {
        List<SanctionEntry> candidates =
            listRepo.findByCountry(party.getCountry());
        int maxScore = candidates.stream()
            .mapToInt(e -> fuzzyScore(e.getName(), party.getName()))
            .max().orElse(0);

        if (maxScore >= HOLD_THRESHOLD) {
            return ScreeningResult.hold(maxScore);
        }
        return ScreeningResult.clear(maxScore);
    }

    private int fuzzyScore(String a, String b) {
        double jw = JaroWinklerSimilarity.apply(
            a.toUpperCase(), b.toUpperCase());
        return (int) Math.round(jw * 100);
    }
}
```

## When to Use
- All cross-border payment flows (SWIFT MT103, ISO 20022 pacs.008)
- Domestic high-value transfers above regulatory threshold

## When Not to Use
- Internal ledger movements between same-bank accounts — no external counterparty

## NFR Acceptance Criteria
- Screening p99 latency ≤ 200 ms (Redis-cached lists)
- False negative rate < 0.01% (validated quarterly against OFAC test set)
- List refresh: every 4 h via scheduled pull from authoritative source
- Hold queue consumer SLA: manual review completed within 2 business hours

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.18.1 | Compliance with legal/regulatory obligations |
| Ring 1 | SWIFT CSP v2024 Control 2.1 | Mandatory transaction screening |
| Ring 2 | SBV Circular 09/2020 AML provisions | Sanction list screening before settlement ⚠️ (working summary — pending Legal review) |

## Threat Model
| Threat | Control |
|--------|---------|
| Stale sanction list | Automated refresh + staleness alert if list > 6 h old |
| Name transliteration evasion | Romanised + original-script variants stored in list |
| Volume-based bypass attempt | Rate limiter on payment API upstream |

## Related Patterns
- BSP-001 Double-Entry Ledger — posting only after CLEAR result
- REF-007 Fraud Screening Platform — complementary real-time fraud signals

## Key Takeaway
Fuzzy-match payment parties against a Redis-cached sanction list;
scores ≥ 80 route to a manual hold queue; scores < 80 return CLEAR
synchronously before ledger posting.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/banking-solutions/sanction-screening-pipeline.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/patterns/banking-solutions/sanction-screening-pipeline.md
git commit -m "feat(catalog): BSP-003 Sanction Screening Pipeline — Wave 3D"
```

---

### Task 39: BSP-004 End-of-Day Batch Window

**Files:**
- Create: `knowledge-base/patterns/banking-solutions/end-of-day-batch-window.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: BSP-004
title: "End-of-Day Batch Window"
domain: banking-solutions
status: Draft
tier: Tier 2
owner: Core Banking Team
compliance: [BCBS-239, SBV-09-2020]
date: 2026-05-10
wave: Wave 3D
---

# End-of-Day Batch Window

## Problem Statement
Core banking systems must close the business day, calculate interest,
generate regulatory reports, and roll the value date — all within a
constrained batch window (typically 22:00–04:00 VNT) without blocking
real-time channels.

## Context
Spring Batch partitions the EOD workload across multiple worker nodes.
Each partition processes one account range. Kafka events notify
downstream systems (reporting, statements) when each phase completes.

## Solution

```mermaid
graph TB
    Trigger[Scheduler 22:00 VNT] --> Phase1[Phase 1: Close Day\nLock value date]
    Phase1 --> Phase2[Phase 2: Interest Accrual\nPartitioned by account range]
    Phase2 --> Phase3[Phase 3: Fee Posting\nBatch ledger entries]
    Phase3 --> Phase4[Phase 4: Statement Generation\nAsync PDF/S3]
    Phase4 --> Phase5[Phase 5: Regulatory Extract\nSBV BAIMS feed]
    Phase5 --> Phase6[Phase 6: Open Next Day\nUnlock value date]
    Phase6 --> Notify[Kafka: EOD_COMPLETE]
```

### Spring Batch Partitioned Step (Java 21)

```java
@Bean
public Step interestAccrualStep(JobRepository jobRepo,
        PlatformTransactionManager txMgr,
        DataSource ds) {

    return new StepBuilder("interestAccrual", jobRepo)
        .partitioner("interestWorker",
            new AccountRangePartitioner(ds, 20))
        .<Account, InterestEntry>chunk(500, txMgr)
        .reader(accountItemReader(ds))
        .processor(new InterestCalculationProcessor())
        .writer(ledgerEntryWriter(ds))
        .taskExecutor(new SimpleAsyncTaskExecutor("eod-"))
        .throttleLimit(8)
        .build();
}
```

### AccountRangePartitioner

```java
public class AccountRangePartitioner implements Partitioner {
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM account WHERE active = true", Long.class);
        long rangeSize = (total / gridSize) + 1;
        Map<String, ExecutionContext> result = new LinkedHashMap<>();
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext ctx = new ExecutionContext();
            ctx.putLong("offset", (long) i * rangeSize);
            ctx.putLong("limit", rangeSize);
            result.put("partition" + i, ctx);
        }
        return result;
    }
}
```

## When to Use
- Nightly interest accrual, fee posting, regulatory extracts
- Any operation that must run outside trading hours on the full account book

## When Not to Use
- Real-time individual account queries — use online API
- Micro-batch streaming — use Kafka Streams with tumbling windows instead

## NFR Acceptance Criteria
- Full EOD cycle completes within 4 h for 10 M accounts
- Restart-from-checkpoint: any failed partition restarts without reprocessing completed partitions
- Regulatory extract delivered to BAIMS SFTP by 04:00 VNT

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.12.1 | Controlled, scheduled processing |
| Ring 1 | BCBS 239 Principle 3 | Timely regulatory data delivery |
| Ring 2 | SBV Circular 09/2020 Article 15 | EOD regulatory reporting obligations ⚠️ (working summary — pending Legal review) |

## Threat Model
| Threat | Control |
|--------|---------|
| Batch overrun blocking next day open | Hard SLA alert at T+3 h; escalation playbook |
| Partial failure leaving inconsistent state | Spring Batch restart-from-checkpoint; no re-processing of completed partitions |
| Unauthorised regulatory extract access | SFTP with client certificates; file encrypted with SBV PGP key |

## Related Patterns
- BSP-001 Double-Entry Ledger — interest accrual posts via ledger service
- DATA-008 Change Data Capture — captures balance changes for downstream

## Key Takeaway
Partition the EOD workload by account range across 8–20 Spring Batch
worker threads; each phase publishes a Kafka event on completion so
downstream systems can chain without polling.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/banking-solutions/end-of-day-batch-window.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/patterns/banking-solutions/end-of-day-batch-window.md
git commit -m "feat(catalog): BSP-004 End-of-Day Batch Window — Wave 3D"
```

---

### Task 40: BSP-005 Reversal and Chargeback

**Files:**
- Create: `knowledge-base/patterns/banking-solutions/reversal-and-chargeback.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: BSP-005
title: "Reversal and Chargeback Pattern"
domain: banking-solutions
status: Draft
tier: Tier 1
owner: Payments Team
compliance: [PCI-DSS, SBV-09-2020]
date: 2026-05-10
wave: Wave 3D
---

# Reversal and Chargeback Pattern

## Problem Statement
Reversals and chargebacks must mirror the original ledger entries exactly
(inverted signs) without duplicating the originating transaction or
creating dangling references.

## Context
A reversal cancels an authorised-but-not-settled transaction. A
chargeback reverses a settled payment after dispute resolution. Both
operations must be idempotent and traceable back to the original
transaction.

## Solution

```mermaid
sequenceDiagram
    participant Client
    participant RevSvc as ReversalService
    participant Ledger as LedgerService
    participant Audit  as AuditLog

    Client->>RevSvc: reverse(originalTxnRef, reason)
    RevSvc->>Ledger: findByTxnRef(originalTxnRef)
    Ledger-->>RevSvc: [debitEntry, creditEntry]
    RevSvc->>RevSvc: invertEntries(entries)
    RevSvc->>Ledger: post(reversalLines, reversalRef)
    Ledger-->>RevSvc: receipt
    RevSvc->>Audit: log(REVERSAL, originalTxnRef, reversalRef)
    RevSvc-->>Client: ReversalReceipt
```

### Java 21 ReversalService

```java
@Service
@Transactional
public class ReversalService {

    private final LedgerService ledger;
    private final LedgerEntryRepository entryRepo;
    private final ReversalRepository reversalRepo;

    public ReversalReceipt reverse(String originalTxnRef, String reason) {
        if (reversalRepo.existsByOriginalTxnRef(originalTxnRef)) {
            return reversalRepo.findByOriginalTxnRef(originalTxnRef)
                .toReceipt();
        }

        List<LedgerEntry> original =
            entryRepo.findByTxnRef(originalTxnRef);
        if (original.isEmpty()) {
            throw new TransactionNotFoundException(originalTxnRef);
        }

        String reversalRef = "REV-" + UUID.randomUUID();
        List<LedgerLine> lines = original.stream()
            .map(e -> LedgerLine.builder()
                .accountId(e.getAccountId())
                .type(e.getEntryType() == 'D' ? 'C' : 'D')
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .valueDate(LocalDate.now())
                .build())
            .toList();

        ledger.post(LedgerPost.builder()
            .txnRef(reversalRef)
            .lines(lines)
            .narrative("Reversal of " + originalTxnRef + ": " + reason)
            .build());

        Reversal rev = reversalRepo.save(Reversal.builder()
            .reversalRef(reversalRef)
            .originalTxnRef(originalTxnRef)
            .reason(reason)
            .reversedAt(Instant.now())
            .build());

        return rev.toReceipt();
    }
}
```

## When to Use
- Customer-initiated payment cancellation (before settlement)
- Acquirer-initiated chargeback after dispute resolution
- System-initiated reversal on downstream settlement failure

## When Not to Use
- Correcting data errors in historical records — use adjustment entries with explicit narrative
- Partial reversals of split transactions — reverse each leg individually

## NFR Acceptance Criteria
- Reversal processing p99 ≤ 100 ms
- Idempotency: identical reversal request returns same receipt within 48 h
- Audit record created for every reversal (immutable, SEC-012 HMAC chain)

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.12.4 | Audit trail for all financial adjustments |
| Ring 1 | PCI-DSS 10.2 | Logging of all chargeback operations |
| Ring 2 | SBV Circular 09/2020 Article 13 | Transaction correction traceability ⚠️ (working summary — pending Legal review) |

## Threat Model
| Threat | Control |
|--------|---------|
| Double reversal | Idempotency check on `originalTxnRef` in `reversal` table |
| Reversal of non-existent transaction | 404 guard before inversion |
| Unauthorised chargeback | RBAC: only `DISPUTE_AGENT` role may initiate |

## Related Patterns
- BSP-001 Double-Entry Ledger — reversal posts through the same ledger service
- BSP-002 Idempotent Payment Key — reversals are themselves idempotent

## Key Takeaway
Invert the sign of every original ledger entry and post them as a new
balanced reversal transaction; guard idempotency with a `reversal` table
keyed on the original transaction reference.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/banking-solutions/reversal-and-chargeback.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/patterns/banking-solutions/reversal-and-chargeback.md
git commit -m "feat(catalog): BSP-005 Reversal and Chargeback — Wave 3D"
```

---

### Task 41: REF-005 SWIFT MT/MX Wire Transfer

**Files:**
- Create: `knowledge-base/reference-architectures/swift-mt-mx-wire-transfer.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: REF-005
title: "SWIFT MT/MX Wire Transfer Reference Architecture"
domain: reference-architectures
status: Draft
tier: Tier 1
owner: Payments Architecture Team
compliance: [SWIFT-CSP, ISO-20022, SBV-09-2020]
date: 2026-05-10
wave: Wave 3D
---

# SWIFT MT/MX Wire Transfer Reference Architecture

## Problem Statement
Outgoing and incoming SWIFT messages must be validated, screened,
translated (MT↔MX), and posted to the ledger with full audit trail —
within the SWIFT daily cut-off windows.

## Context
ISO 20022 migration mandates parallel MT103 + pacs.008 support through
2025. The architecture handles both formats via Apache Camel routing with
format-detection.

## Solution

```mermaid
graph TB
    subgraph Inbound
        SWIFTIn[SWIFT Alliance\nGateway] -->|MT/MX file| Camel[Apache Camel\nRouter]
        Camel -->|detect format| Parser{MT or MX?}
        Parser -->|MT103| MTAdapter[MT to Domain\nAdapter]
        Parser -->|pacs.008| MXAdapter[MX to Domain\nAdapter]
        MTAdapter --> Screen[Sanction Screening\nBSP-003]
        MXAdapter --> Screen
        Screen -->|CLEAR| Ledger[LedgerService\nBSP-001]
        Screen -->|HOLD| HoldQ[Hold Queue]
    end
    subgraph Outbound
        API[Payments API] --> Builder[MessageBuilder]
        Builder -->|MT103| MtOut[MT Serialiser]
        Builder -->|pacs.008| MxOut[MX Serialiser]
        MtOut --> SWIFTOut[SWIFT Alliance\nGateway]
        MxOut --> SWIFTOut
    end
```

### Apache Camel Route (Java 21)

```java
@Component
public class SwiftInboundRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("sftp://swift-gw/inbound?delete=true&scheduler=quartz"
           + "&scheduler.cron=0+*/5+*+*+*+?")
            .routeId("swift-inbound")
            .choice()
                .when(header("CamelFileName").endsWith(".fin"))
                    .to("direct:parseMT")
                .when(header("CamelFileName").endsWith(".xml"))
                    .to("direct:parseMX")
                .otherwise()
                    .to("direct:swiftDLQ")
            .end();

        from("direct:parseMT")
            .bean(MtMessageParser.class, "parse")
            .bean(SanctionScreeningService.class, "screen")
            .choice()
                .when(simple("${body.screeningResult} == 'CLEAR'"))
                    .bean(LedgerService.class, "post")
                .otherwise()
                    .to("kafka:swift-hold-queue")
            .end();
    }
}
```

## When to Use
- All cross-border wire transfers via SWIFT network
- ISO 20022 migration scenarios requiring MT/MX coexistence

## When Not to Use
- Domestic instant payments (Napas 247) — use domestic Napas adapter instead
- Internal ledger transfers — direct ledger API call, no SWIFT message needed

## NFR Acceptance Criteria
- Inbound message processing p99 ≤ 500 ms (excluding SWIFT network latency)
- MT↔MX translation accuracy 100% (validated against SWIFT conformance test suite)
- Cut-off compliance: all outbound messages queued by 15:00 UTC for same-day settlement

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.14.2 | Secure development of payment message handlers |
| Ring 1 | SWIFT CSP v2024 Control 1.1 | Mandatory use of SWIFT Alliance secure channel |
| Ring 2 | SBV Circular 09/2020 Chapter III | Cross-border payment processing obligations ⚠️ (working summary — pending Legal review) |

## Threat Model
| Threat | Control |
|--------|---------|
| Message tampering in transit | SWIFT PKI + TLS 1.3 on SFTP channel |
| Fraudulent SWIFT message injection | MAC validation on every inbound message |
| Settlement cut-off miss | Outbound queue monitor; alert at T-60 min |

## Related Patterns
- BSP-003 Sanction Screening — mandatory pre-posting screen
- COMP-007 ISO 20022 Messaging — message schema governance

## Key Takeaway
Route inbound SWIFT files via Apache Camel with format detection
(MT/MX), mandatory sanction screening, then ledger posting; outbound
messages are serialised from the domain model to MT103 or pacs.008
based on correspondent capability.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/reference-architectures/swift-mt-mx-wire-transfer.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/reference-architectures/swift-mt-mx-wire-transfer.md
git commit -m "feat(catalog): REF-005 SWIFT MT/MX Wire Transfer — Wave 3D"
```

---

### Task 42: REF-006 Loan Origination

**Files:**
- Create: `knowledge-base/reference-architectures/loan-origination.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: REF-006
title: "Loan Origination Reference Architecture"
domain: reference-architectures
status: Draft
tier: Tier 2
owner: Retail Lending Team
compliance: [BCBS-239, SBV-09-2020]
date: 2026-05-10
wave: Wave 3D
---

# Loan Origination Reference Architecture

## Problem Statement
Loan applications pass through multiple stages — KYC, credit scoring,
collateral valuation, approval workflow, disbursement — each owned by
different systems. Without a central orchestration layer, state becomes
fragmented across teams.

## Context
The architecture uses a Saga orchestrator (Spring State Machine) to
coordinate the loan lifecycle. Each stage is a microservice; failures
trigger compensating actions. The applicant status is visible in
real-time via Server-Sent Events.

## Solution

```mermaid
stateDiagram-v2
    [*] --> ApplicationReceived
    ApplicationReceived --> KYCInProgress
    KYCInProgress --> CreditScoring : KYC passed
    KYCInProgress --> Rejected : KYC failed
    CreditScoring --> CollateralValuation : score >= 600
    CreditScoring --> Rejected : score < 600
    CollateralValuation --> PendingApproval
    PendingApproval --> Approved : RM approves
    PendingApproval --> Rejected : RM rejects
    Approved --> Disbursed : disbursement posted
    Disbursed --> [*]
    Rejected --> [*]
```

### Spring State Machine Configuration (Java 21)

```java
@Configuration
@EnableStateMachine
public class LoanStateMachineConfig
        extends StateMachineConfigurerAdapter<LoanState, LoanEvent> {

    @Override
    public void configure(
            StateMachineStateConfigurer<LoanState, LoanEvent> states)
            throws Exception {
        states.withStates()
            .initial(LoanState.APPLICATION_RECEIVED)
            .states(EnumSet.allOf(LoanState.class))
            .end(LoanState.DISBURSED)
            .end(LoanState.REJECTED);
    }

    @Override
    public void configure(
            StateMachineTransitionConfigurer<LoanState, LoanEvent> transitions)
            throws Exception {
        transitions
            .withExternal()
                .source(APPLICATION_RECEIVED).target(KYC_IN_PROGRESS)
                .event(LoanEvent.START_KYC).and()
            .withExternal()
                .source(KYC_IN_PROGRESS).target(CREDIT_SCORING)
                .event(LoanEvent.KYC_PASSED).and()
            .withExternal()
                .source(CREDIT_SCORING).target(COLLATERAL_VALUATION)
                .event(LoanEvent.SCORE_ACCEPTED).and()
            .withExternal()
                .source(COLLATERAL_VALUATION).target(PENDING_APPROVAL)
                .event(LoanEvent.VALUATION_DONE).and()
            .withExternal()
                .source(PENDING_APPROVAL).target(APPROVED)
                .event(LoanEvent.RM_APPROVED).and()
            .withExternal()
                .source(APPROVED).target(DISBURSED)
                .event(LoanEvent.DISBURSED);
    }
}
```

## When to Use
- Retail and SME loan origination with multi-stage approval
- Any multi-party workflow requiring compensating transactions on failure

## When Not to Use
- Simple overdraft authorisation — synchronous credit-limit check suffices
- Pre-approved instant loans — skip state machine, direct disbursement

## NFR Acceptance Criteria
- Application-to-decision p95 ≤ 5 business days (SLA driven, not latency)
- State machine checkpoint persisted to PostgreSQL; survives pod restart
- Applicant real-time status via SSE with ≤ 1 s update lag

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.12.4 | Audit trail of all state transitions |
| Ring 1 | BCBS 239 Principle 6 | Adaptable reporting — loan lifecycle data |
| Ring 2 | SBV Circular 09/2020 Article 20 | Loan documentation and approval obligations ⚠️ (working summary — pending Legal review) |

## Related Patterns
- BSP-001 Double-Entry Ledger — disbursement posts through ledger service
- BSP-003 Sanction Screening — applicant screened before KYC

## Key Takeaway
Orchestrate loan lifecycle stages with a Spring State Machine persisted
to PostgreSQL; each transition is driven by domain events so stages are
decoupled and individually compensatable.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/reference-architectures/loan-origination.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/reference-architectures/loan-origination.md
git commit -m "feat(catalog): REF-006 Loan Origination — Wave 3D"
```

---

### Task 43: REF-007 Fraud Screening Platform

**Files:**
- Create: `knowledge-base/reference-architectures/fraud-screening-platform.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: REF-007
title: "Fraud Screening Platform Reference Architecture"
domain: reference-architectures
status: Draft
tier: Tier 1
owner: Fraud and Risk Team
compliance: [PCI-DSS, SBV-09-2020]
date: 2026-05-10
wave: Wave 3D
---

# Fraud Screening Platform Reference Architecture

## Problem Statement
Real-time payment fraud must be detected and blocked within the
authorisation window (< 300 ms) before funds leave the bank. Batch
overnight rules miss in-session fraud and are insufficient.

## Context
The platform combines a rules engine (Drools) for deterministic rules
with an ML scoring service for behavioural anomalies. CloudEvents carry
fraud signals between services. High-risk transactions are held for
step-up authentication.

## Solution

```mermaid
graph TB
    PayAPI[Payments API] -->|CloudEvent: PaymentInitiated| FraudGW[Fraud Gateway]
    FraudGW --> Rules[Drools Rules Engine\nvelocity and geo-anomaly]
    FraudGW --> ML[ML Scoring Service\nbehavioural model]
    Rules --> Aggregator[Score Aggregator]
    ML --> Aggregator
    Aggregator -->|combined score| Decision{Score >= 75?}
    Decision -->|yes| StepUp[Step-Up Auth\nOTP or Biometric]
    Decision -->|no| Allow[Allow Payment]
    StepUp -->|passed| Allow
    StepUp -->|failed| Block[Block and Alert]
```

### Drools Velocity Rule

```java
// velocity-rules.drl
rule "High Velocity — 5 txns in 60 seconds"
    when
        $e : PaymentEvent($accountId : accountId, $ts : timestamp)
        Number(intValue >= 5) from accumulate(
            PaymentEvent(accountId == $accountId,
                         timestamp > $ts - 60000),
            count(1))
    then
        $e.addRiskFlag("HIGH_VELOCITY", 40);
end
```

### ML Score Integration (Java 21)

```java
@Service
public class MlFraudScoringService {

    private final WebClient mlClient;

    public Mono<Integer> score(PaymentEvent event) {
        return mlClient.post().uri("/score")
            .bodyValue(FraudFeatures.from(event))
            .retrieve()
            .bodyToMono(FraudScoreResponse.class)
            .map(FraudScoreResponse::getScore)
            .timeout(Duration.ofMillis(80))
            .onErrorReturn(0);
    }
}
```

## When to Use
- Real-time card authorisation and payment initiation fraud screening
- Step-up authentication triggering based on risk score

## When Not to Use
- Batch retrospective fraud investigation — use data warehouse queries
- Internal ledger movements — no external counterparty risk

## NFR Acceptance Criteria
- Fraud decision p99 ≤ 250 ms (within authorisation window)
- False positive rate < 0.5% (measured weekly against confirmed legitimate transactions)
- ML model refresh: weekly retrain, A/B shadow scoring for 48 h before promotion

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.16.1 | Incident detection and response |
| Ring 1 | PCI-DSS 10.6 | Fraud monitoring of payment card environments |
| Ring 2 | SBV Circular 09/2020 AML Article 18 | Real-time transaction monitoring ⚠️ (working summary — pending Legal review) |

## Threat Model
| Threat | Control |
|--------|---------|
| Rule bypass via low-velocity attacks | ML behavioural model catches sub-threshold patterns |
| ML model poisoning | Training pipeline integrity checks; anomaly detection on feature distribution |
| Fail-open abuse | ML timeout treated as score=0 (not block); Drools rules still run |

## Related Patterns
- BSP-003 Sanction Screening — pre-posting AML check
- INT-011 CloudEvents — fraud signals use CloudEvents envelope

## Key Takeaway
Combine deterministic Drools velocity rules with an ML behavioural
scoring model; aggregate scores within 250 ms and trigger step-up auth
for high-risk transactions, blocking only on step-up failure.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/reference-architectures/fraud-screening-platform.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/reference-architectures/fraud-screening-platform.md
git commit -m "feat(catalog): REF-007 Fraud Screening Platform — Wave 3D"
```

---

### Task 44: REF-008 Regulatory Reporting

**Files:**
- Create: `knowledge-base/reference-architectures/regulatory-reporting.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: REF-008
title: "Regulatory Reporting Reference Architecture"
domain: reference-architectures
status: Draft
tier: Tier 1
owner: Finance and Regulatory Team
compliance: [BCBS-239, SBV-09-2020]
date: 2026-05-10
wave: Wave 3D
---

# Regulatory Reporting Reference Architecture

## Problem Statement
Multiple regulators (SBV, FATCA/CRS tax authority, AML unit) require
periodic reports in specific formats and submission channels. Building
bespoke extract logic for each report creates maintenance debt and
data inconsistency.

## Context
A single Regulatory Data Hub (Trino over Delta Lake) serves as the
authoritative source. Report generators query the hub via SQL and
produce regulator-specific formats (XML, XBRL, flat file). OpenLineage
tracks data provenance from source system to filed report.

## Solution

```mermaid
graph TB
    subgraph Sources
        CBS[Core Banking] --> CDC[CDC via Debezium]
        ODS[ODS and Data Vault] --> CDC
    end
    subgraph RegDataHub
        CDC --> DeltaLake[Delta Lake\nraw and curated layers]
        DeltaLake --> Trino[Trino\nfederated query]
    end
    subgraph Reporting
        Trino --> SBV[SBV BAIMS\nFlat file]
        Trino --> FATCA[FATCA and CRS\nXML IGA]
        Trino --> AML[AML Suspicious\nActivity XML]
        Trino --> BCBS[BCBS 239\nRisk Aggregate]
    end
    subgraph Delivery
        SBV --> SFTP[SBV SFTP]
        FATCA --> IDES[IRS IDES Gateway]
        AML --> FIU[FIU Portal]
    end
```

### Trino Regulatory Query Example

```sql
SELECT
    account_id,
    currency,
    SUM(CASE WHEN entry_type = 'C' THEN amount ELSE -amount END) AS balance,
    MAX(booking_date) AS as_of_date
FROM delta.curated.ledger_entry
WHERE booking_date = DATE '{{ run_date }}'
GROUP BY account_id, currency
ORDER BY account_id;
```

### Report Scheduler (Java 21)

```java
@Component
public class SbvDailyReportJob {

    private final TrinoTemplate trino;
    private final SftpGateway sftp;

    @Scheduled(cron = "0 0 5 * * *", zone = "Asia/Ho_Chi_Minh")
    public void generateAndSubmit() {
        LocalDate reportDate = LocalDate.now().minusDays(1);
        List<PositionRow> rows = trino.query(
            "sbv-daily-position.sql",
            Map.of("run_date", reportDate.toString()),
            PositionRow.class);

        byte[] flatFile = SbvFlatFileSerializer.serialize(rows, reportDate);
        sftp.upload("sbv-inbound/pos_" + reportDate + ".dat", flatFile);
    }
}
```

## When to Use
- Any periodic regulatory submission (daily, monthly, quarterly)
- Multi-regulator environments requiring the same source data in different formats

## When Not to Use
- Ad-hoc internal management reports — use BI tool directly against ODS
- Real-time regulatory feeds — use streaming pipeline, not batch report job

## NFR Acceptance Criteria
- SBV daily report delivered to SFTP by 05:30 VNT (T+1)
- Data lineage traceable from report cell to source ledger entry via OpenLineage
- Report re-run idempotent: same inputs produce identical output

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.18.1 | Compliance with legal and regulatory obligations |
| Ring 1 | BCBS 239 Principle 11 | Remediation; accurate, timely regulatory data |
| Ring 2 | SBV Circular 09/2020 Chapter IV | Daily/monthly regulatory reporting to SBV BAIMS ⚠️ (working summary — pending Legal review) |

## Related Patterns
- DATA-008 Change Data Capture — feeds raw data into Delta Lake
- DATA-012 OpenLineage Data Lineage — tracks report provenance

## Key Takeaway
Centralise regulatory source data in a Trino-fronted Delta Lake hub;
each regulator gets a dedicated report generator that queries the hub
via parameterised SQL and delivers in the required format and channel.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/reference-architectures/regulatory-reporting.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/reference-architectures/regulatory-reporting.md
git commit -m "feat(catalog): REF-008 Regulatory Reporting — Wave 3D"
```

---

### Task 45: REF-009 Account Opening Omnichannel

**Files:**
- Create: `knowledge-base/reference-architectures/account-opening-omnichannel.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: REF-009
title: "Account Opening Omnichannel Reference Architecture"
domain: reference-architectures
status: Draft
tier: Tier 2
owner: Digital Banking Team
compliance: [SBV-09-2020, Decree-13-2023]
date: 2026-05-10
wave: Wave 3D
---

# Account Opening Omnichannel Reference Architecture

## Problem Statement
Customers begin account opening on mobile, switch to web, or visit a
branch — all expecting to resume their application without re-entering
data. Without shared state, each channel starts a new application.

## Context
A BFF (Backend for Frontend) layer maintains a channel-agnostic
application session in Redis. The core onboarding service owns KYC,
identity verification (eKYC via VNeID), and T24 account creation.

## Solution

```mermaid
graph TB
    Mobile[Mobile App] --> BFF[BFF Layer\nApigee]
    Web[Web App] --> BFF
    Branch[Branch Teller\nApp] --> BFF
    BFF --> Session[Session Store\nRedis]
    BFF --> OnboardSvc[Onboarding Service]
    OnboardSvc --> eKYC[eKYC Service\nVNeID]
    OnboardSvc --> AML[AML and Sanction\nBSP-003]
    OnboardSvc --> T24[Core Banking\nT24 Account Create]
    OnboardSvc --> Notify[Notification\nSMS and Email]
```

### Session Resume (Java 21)

```java
@RestController
@RequestMapping("/v1/onboarding")
public class OnboardingController {

    private final OnboardingSessionService sessionSvc;

    @GetMapping("/{sessionId}/resume")
    public OnboardingSession resumeSession(
            @PathVariable String sessionId,
            @RequestHeader("X-Channel") String channel) {

        OnboardingSession session = sessionSvc.load(sessionId);
        session.setLastChannel(channel);
        session.setLastActiveAt(Instant.now());
        sessionSvc.save(session);
        return session;
    }
}
```

### eKYC Integration

```java
@Service
public class EKycService {

    private final WebClient vneidClient;

    public EKycResult verify(String nationalId, byte[] selfieJpeg) {
        return vneidClient.post().uri("/verify")
            .bodyValue(EKycRequest.of(nationalId,
                Base64.getEncoder().encodeToString(selfieJpeg)))
            .retrieve()
            .bodyToMono(EKycResult.class)
            .timeout(Duration.ofSeconds(10))
            .block();
    }
}
```

## When to Use
- Retail account opening across mobile, web, and branch channels
- Any multi-channel onboarding flow requiring resumable state

## When Not to Use
- Internal staff account provisioning — no customer KYC required
- Instant pre-approved account issuance — skip state machine, synchronous path

## NFR Acceptance Criteria
- Session resume latency p99 ≤ 200 ms (Redis-backed)
- Channel switch: zero data re-entry required
- eKYC verification p95 ≤ 8 s (VNeID SLA dependent)

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.9.2 | User registration and identity verification |
| Ring 1 | — | N/A for this pattern |
| Ring 2 | Decree 13/2023 Article 9 | Lawful basis for collecting personal data during KYC ⚠️ (working summary — pending Legal review) |

## Threat Model
| Threat | Control |
|--------|---------|
| Session hijacking across channels | Session token in HttpOnly cookie + Redis IP binding |
| Identity fraud in eKYC | VNeID liveness detection; manual review fallback |
| PII leakage in BFF logs | PII fields masked per SEC-008 Data Masking |

## Related Patterns
- BSP-003 Sanction Screening — applicant screened post-eKYC
- SEC-008 Data Masking — mask national ID in application logs

## Key Takeaway
Store channel-agnostic onboarding state in Redis via the BFF; any
channel can resume by session ID, allowing customers to switch between
mobile, web, and branch without restarting the application.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/reference-architectures/account-opening-omnichannel.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/reference-architectures/account-opening-omnichannel.md
git commit -m "feat(catalog): REF-009 Account Opening Omnichannel — Wave 3D"
```

---

### Task 46: REF-010 Ledger Posting Engine

**Files:**
- Create: `knowledge-base/reference-architectures/ledger-posting-engine.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: REF-010
title: "Ledger Posting Engine Reference Architecture"
domain: reference-architectures
status: Draft
tier: Tier 1
owner: Core Banking Team
compliance: [BCBS-239, SBV-09-2020]
date: 2026-05-10
wave: Wave 3D
---

# Ledger Posting Engine Reference Architecture

## Problem Statement
Multiple upstream systems (payments, loans, treasury) need to post
ledger entries. Without a single entry point, each team reimplements
debit/credit logic with varying validation quality.

## Context
The Ledger Posting Engine is the single gateway for all ledger writes.
It enforces balanced-entry validation, idempotency, and routing to the
correct chart-of-accounts partition. Kafka delivers posting receipts to
upstream systems.

## Solution

```mermaid
graph TB
    Payments[Payments\nService] -->|PostRequest| Engine[Ledger Posting Engine]
    Loans[Loans\nService] -->|PostRequest| Engine
    Treasury[Treasury\nService] -->|PostRequest| Engine
    Engine --> Validate[Validate:\nbalance and currency]
    Validate --> Idem[Idempotency\nCheck BSP-002]
    Idem --> CoA[Chart-of-Accounts\nRouter]
    CoA --> DB[(PostgreSQL\nledger_entry)]
    DB --> Kafka[Kafka:\nledger.posted]
    Kafka --> Payments
    Kafka --> Reporting[Reporting\nService]
```

### Posting Engine Controller (Java 21)

```java
@RestController
@RequestMapping("/v1/ledger")
public class LedgerPostingController {

    private final LedgerService ledgerService;

    @PostMapping("/post")
    @PreAuthorize("hasRole('LEDGER_POSTER')")
    public ResponseEntity<TransactionReceipt> post(
            @RequestBody @Valid LedgerPostRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        TransactionReceipt receipt = ledgerService.post(
            LedgerPost.builder()
                .txnRef(idempotencyKey)
                .lines(request.getLines())
                .narrative(request.getNarrative())
                .build());

        return ResponseEntity.ok(receipt);
    }
}
```

## When to Use
- Any service needing to write monetary entries (payments, loans, fees)
- Systems requiring async confirmation of posting via Kafka event

## When Not to Use
- Read-only balance queries — use the balance query API
- Non-monetary operational events — no need to go through the ledger engine

## NFR Acceptance Criteria
- Posting p99 ≤ 50 ms
- Kafka receipt delivery within 100 ms of commit
- Throughput: ≥ 2 000 TPS sustained during EOD batch window

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.12.1 | Controlled financial processing |
| Ring 1 | BCBS 239 Principle 2 | Single authoritative source for balance data |
| Ring 2 | SBV Circular 09/2020 Article 8 | Centralised ledger with reconcilable balances ⚠️ (working summary — pending Legal review) |

## Related Patterns
- BSP-001 Double-Entry Ledger — schema and balance logic
- BSP-002 Idempotent Payment Key — prevents duplicate posts

## Key Takeaway
All ledger writes flow through a single engine that enforces balanced
entries and idempotency; Kafka receipts allow upstream services to react
to posting confirmation asynchronously.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/reference-architectures/ledger-posting-engine.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/reference-architectures/ledger-posting-engine.md
git commit -m "feat(catalog): REF-010 Ledger Posting Engine — Wave 3D"
```

---

### Task 47: REF-011 Open Banking PSD2

**Files:**
- Create: `knowledge-base/reference-architectures/open-banking-psd2.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: REF-011
title: "Open Banking PSD2 Reference Architecture"
domain: reference-architectures
status: Draft
tier: Tier 2
owner: Open Banking Team
compliance: [ISO-27001, SBV-09-2020]
date: 2026-05-10
wave: Wave 3D
---

# Open Banking PSD2 Reference Architecture

## Problem Statement
Third-party providers (TPPs) need secure, consented access to account
data and payment initiation without direct access to core banking
credentials.

## Context
The architecture follows PSD2/Open Banking principles adapted for the
Vietnamese regulatory context. OAuth 2.0 + PKCE with dynamic client
registration authenticates TPPs. Consent management tracks granular
permissions per account per TPP.

## Solution

```mermaid
sequenceDiagram
    participant TPP
    participant Consent as Consent Portal
    participant AuthSrv as Auth Server
    participant API as Open Banking API
    participant Core as Core Banking

    TPP->>AuthSrv: Authorization request (PKCE)
    AuthSrv->>Consent: Redirect — grant permissions
    Consent-->>AuthSrv: User grants consent
    AuthSrv-->>TPP: Authorization code
    TPP->>AuthSrv: Token exchange (code + verifier)
    AuthSrv-->>TPP: access_token (scopes: accounts:read)
    TPP->>API: GET /accounts (Bearer token)
    API->>AuthSrv: Introspect token
    API->>Core: Fetch account data
    Core-->>API: AccountData
    API-->>TPP: AccountData (consented fields only)
```

### Consent Enforcement (Java 21 + OPA)

```java
@Component
public class ConsentEnforcementFilter extends OncePerRequestFilter {

    private final OpaClient opa;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
            HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String token = extractBearer(req);
        OpaInput input = OpaInput.builder()
            .token(token)
            .resource(req.getRequestURI())
            .action(req.getMethod())
            .build();

        OpaResult result = opa.evaluate("bank/open_banking/allow", input);
        if (!result.isAllow()) {
            res.sendError(403, "Consent not granted for this resource");
            return;
        }
        chain.doFilter(req, res);
    }
}
```

## When to Use
- TPP integration for account information (AISP) and payment initiation (PISP)
- Any scenario requiring consented, scoped third-party data access

## When Not to Use
- Internal service-to-service calls — mTLS + service account, no OAuth needed
- Customer-facing first-party mobile app — use direct session auth, not open banking flow

## NFR Acceptance Criteria
- Token introspection p99 ≤ 50 ms
- Consent revocation takes effect within 60 s across all API instances
- TPP onboarding (dynamic client registration) completes within 24 h

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.9.4 | Access control enforcement via consent |
| Ring 1 | — | N/A (PSD2 is EU; Vietnamese equivalent evolving) |
| Ring 2 | SBV Circular 09/2020 API access provisions | Third-party data access with customer consent ⚠️ (working summary — pending Legal review) |

## Related Patterns
- SEC-006 JWT Best Practices — access tokens signed RS256
- SEC-010 ABAC with OPA — consent enforcement policy

## Key Takeaway
Authenticate TPPs with OAuth 2.0 PKCE and enforce granular consent
policies via OPA; consent state is the authoritative gate for every
API resource request from third parties.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/reference-architectures/open-banking-psd2.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/reference-architectures/open-banking-psd2.md
git commit -m "feat(catalog): REF-011 Open Banking PSD2 — Wave 3D"
```

---

### Task 48: REF-012 Dispute Management

**Files:**
- Create: `knowledge-base/reference-architectures/dispute-management.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: REF-012
title: "Dispute Management Reference Architecture"
domain: reference-architectures
status: Draft
tier: Tier 2
owner: Operations Team
compliance: [PCI-DSS, SBV-09-2020]
date: 2026-05-10
wave: Wave 3D
---

# Dispute Management Reference Architecture

## Problem Statement
Payment disputes (chargebacks, erroneous transfers) require coordinated
evidence collection, timeline tracking, and communication with
correspondents — all within regulatory deadlines.

## Context
A dispute lifecycle service orchestrates the workflow from customer
claim through evidence gathering, correspondent communication (SWIFT
camt.056), and resolution (chargeback or uphold). SLA timers enforce
regulatory deadlines.

## Solution

```mermaid
stateDiagram-v2
    [*] --> Opened
    Opened --> EvidenceCollection : auto-gather txn records
    EvidenceCollection --> SentToCorrespondent : SWIFT camt.056
    SentToCorrespondent --> AwaitingResponse : SLA timer started
    AwaitingResponse --> Resolved : correspondent accepts
    AwaitingResponse --> Escalated : SLA breached
    Resolved --> Chargeback : decision = refund
    Resolved --> Upheld : decision = no refund
    Chargeback --> [*]
    Upheld --> [*]
    Escalated --> [*]
```

### SLA Timer (Java 21 + Quartz)

```java
@Service
public class DisputeSlaService {

    private final Scheduler quartz;

    public void startSlaTimer(String disputeId, int slaDays) {
        JobDetail job = JobBuilder.newJob(DisputeSlaJob.class)
            .withIdentity("sla-" + disputeId)
            .usingJobData("disputeId", disputeId)
            .build();

        Trigger trigger = TriggerBuilder.newTrigger()
            .startAt(DateBuilder.futureDate(
                slaDays, DateBuilder.IntervalUnit.DAY))
            .build();

        quartz.scheduleJob(job, trigger);
    }
}

public class DisputeSlaJob implements Job {
    @Override
    public void execute(JobExecutionContext ctx) {
        String disputeId = ctx.getJobDetail()
            .getJobDataMap().getString("disputeId");
        disputeService.escalateIfOpen(disputeId);
    }
}
```

## When to Use
- Customer-initiated disputes on card transactions and wire transfers
- Correspondent bank chargeback workflows (SWIFT camt.056)

## When Not to Use
- Internal accounting corrections — use reversal pattern (BSP-005) directly
- Fraud prevention — this is post-dispute resolution, not real-time fraud blocking

## NFR Acceptance Criteria
- SLA breach rate < 1% (escalation fires before regulatory deadline)
- Dispute audit trail retained ≥ 5 years
- Correspondent message delivery p99 ≤ 2 min via SWIFT

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.16.1 | Formal dispute and incident management |
| Ring 1 | PCI-DSS 12.10 | Chargeback handling procedures |
| Ring 2 | SBV Circular 09/2020 Article 14 | Customer complaint handling obligations ⚠️ (working summary — pending Legal review) |

## Related Patterns
- BSP-005 Reversal and Chargeback — executes the chargeback ledger entries
- REF-005 SWIFT MT/MX — correspondent communication channel

## Key Takeaway
Orchestrate dispute lifecycle with a state machine and Quartz SLA
timers; correspondent messages use SWIFT camt.056; chargebacks post
through BSP-005 Reversal.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/reference-architectures/dispute-management.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/reference-architectures/dispute-management.md
git commit -m "feat(catalog): REF-012 Dispute Management — Wave 3D"
```

---

## Wave 3D Gate — Banking Solutions + Reference Architectures (13 docs)

- [ ] **Step 1: Batch lint**

```bash
for f in \
  knowledge-base/patterns/banking-solutions/double-entry-ledger.md \
  knowledge-base/patterns/banking-solutions/idempotent-payment-key.md \
  knowledge-base/patterns/banking-solutions/sanction-screening-pipeline.md \
  knowledge-base/patterns/banking-solutions/end-of-day-batch-window.md \
  knowledge-base/patterns/banking-solutions/reversal-and-chargeback.md \
  knowledge-base/reference-architectures/swift-mt-mx-wire-transfer.md \
  knowledge-base/reference-architectures/loan-origination.md \
  knowledge-base/reference-architectures/fraud-screening-platform.md \
  knowledge-base/reference-architectures/regulatory-reporting.md \
  knowledge-base/reference-architectures/account-opening-omnichannel.md \
  knowledge-base/reference-architectures/ledger-posting-engine.md \
  knowledge-base/reference-architectures/open-banking-psd2.md \
  knowledge-base/reference-architectures/dispute-management.md; do
  bash scripts/mermaid-lint-doc.sh "$f" || echo "FAIL: $f"
done
```
Expected: 13 files, 0 FAIL lines.

- [ ] **Step 2: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for any Wave 3D file.

- [ ] **Step 3: Update catalog — BSP-001–005, REF-005–012 → Draft**

In `governance/standards/enterprise-architecture-catalog.md`, change all 13 rows from `Proposed` → `Draft`.

```bash
grep -E "(BSP-00[1-5]|REF-00[5-9]|REF-01[012])" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |"
```
Expected: 13 lines.

- [ ] **Step 4: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md
git commit -m "chore(catalog): Wave 3D gate — BSP-001–005 + REF-005–012 promoted to Draft"
```

---

## Wave 3E — Compliance Deep Dives (7 docs)

### Task 49: COMP-002 SBV Circular 09/2020

**Files:**
- Create: `knowledge-base/compliance/sbv-circular-09-2020.md`
- Modify: `governance/standards/enterprise-architecture-catalog.md` (at Wave 3E Gate)

- [ ] **Step 1: Write the document**

```markdown
---
id: COMP-002
title: "SBV Circular 09/2020 — Banking Information System Security"
domain: compliance
status: Draft
tier: Tier 1
owner: Compliance and Legal Team
compliance: [SBV-09-2020]
date: 2026-05-10
wave: Wave 3E
---

# SBV Circular 09/2020 — Banking Information System Security

## Problem Statement
Circular 09/2020/TT-NHNN mandates specific technical and organisational
security controls for banking information systems operating in Vietnam.
Architecture teams need a concise control mapping to verify compliance
before system go-live.

## Context
The circular applies to all credit institutions and foreign bank
branches operating in Vietnam. It covers information system
classification, access control, data backup, incident response, and
third-party risk. The circular is supplemented by Decree 13/2023 for
personal data.

## Solution

```mermaid
graph TB
    subgraph Circular09Controls
        Art5[Article 5\nIS Classification]
        Art8[Article 8\nAccess Control]
        Art10[Article 10\nData Backup]
        Art13[Article 13\nIncident Response]
        Art15[Article 15\nRegulatory Reporting]
        Art18[Article 18\nTransaction Monitoring]
        Art20[Article 20\nLoan Documentation]
    end
    subgraph ArchPatterns
        Art5 --> DATA004[DATA-004 Data Vault]
        Art8 --> SEC010[SEC-010 ABAC OPA]
        Art10 --> DATA008[DATA-008 CDC Backup]
        Art13 --> REF007[REF-007 Fraud Platform]
        Art15 --> REF008[REF-008 Reg Reporting]
        Art18 --> REF007
        Art20 --> REF006[REF-006 Loan Origination]
    end
```

## Key Controls Reference

### Article 5 — Information System Classification
- Systems classified as Critical, Important, or Normal based on impact assessment
- Critical systems require HA deployment (N+1 minimum), RTO ≤ 4 h, RPO ≤ 1 h
- Annual classification review mandatory

**Architecture implication**: all Tier 1 catalog patterns target Critical classification; NFR Acceptance Criteria rows in each pattern reflect the RTO/RPO thresholds.

### Article 8 — Access Control
- Role-based access with principle of least privilege
- Privileged access management with session recording for admin accounts
- MFA mandatory for remote access and privileged operations

**Architecture implication**: SEC-006 JWT + SEC-010 OPA/ABAC implement the access control framework; privileged access is governed by SEC-007 Secrets Rotation.

### Article 10 — Data Backup and Recovery
- Daily incremental backup, weekly full backup
- Off-site backup copy required
- Backup restoration tested quarterly

**Architecture implication**: DATA-008 CDC streams to off-site Delta Lake; PostgreSQL WAL streaming to standby satisfies the backup requirements.

### Article 13 — Transaction Correction Traceability
- All transaction corrections must be logged with original + corrected values
- Corrections require dual authorisation above threshold amounts

**Architecture implication**: BSP-005 Reversal and Chargeback enforces dual-auth via RBAC; SEC-012 Immutable Audit Log captures original + reversal pair.

### Article 15 — Regulatory Reporting
- Daily position reports submitted to SBV BAIMS by 05:30 VNT
- Monthly aggregated risk reports

**Architecture implication**: REF-008 Regulatory Reporting implements the scheduled submission pipeline.

### Article 18 — Transaction Monitoring (AML)
- Real-time monitoring for suspicious transactions
- Automated alert to FIU for transactions above threshold

**Architecture implication**: REF-007 Fraud Screening Platform + BSP-003 Sanction Screening implement the monitoring requirements.

## NFR Acceptance Criteria
- Critical system RTO ≤ 4 h, RPO ≤ 1 h
- Privileged session recording retained ≥ 12 months
- Backup restoration drill: quarterly, documented

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.18.2 | Compliance review against SBV Circular 09 |
| Ring 1 | BCBS 239 Principle 11 | Regulatory data delivery timelines |
| Ring 2 | SBV Circular 09/2020 (all articles) | Primary regulatory instrument for this document ⚠️ (working summary — pending Legal review) |

## Threat Model
| Threat | Control |
|--------|---------|
| Non-compliant system go-live | Pre-launch compliance checklist gate in CI/CD |
| Control drift post-go-live | Annual SBV audit + internal quarterly review |
| Undeclared system changes | Change management process with compliance sign-off |

## Related Patterns
- SEC-010 ABAC with OPA — Article 8 access control
- REF-008 Regulatory Reporting — Article 15 reporting obligations
- BSP-003 Sanction Screening — Article 18 AML monitoring

## References
- Circular 09/2020/TT-NHNN dated 21 August 2020
- SBV BAIMS technical integration guide (internal, restricted)

## Key Takeaway
Map each Circular 09/2020 article to the catalog pattern(s) that
implement its controls; use this document as the compliance checklist
gate for every new system classified as Critical or Important.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/compliance/sbv-circular-09-2020.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/compliance/sbv-circular-09-2020.md
git commit -m "feat(catalog): COMP-002 SBV Circular 09/2020 — Wave 3E"
```

---

### Task 50: COMP-003 Decree 13/2023 Personal Data

**Files:**
- Create: `knowledge-base/compliance/decree-13-2023-personal-data.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: COMP-003
title: "Decree 13/2023 — Personal Data Protection"
domain: compliance
status: Draft
tier: Tier 1
owner: Compliance and Legal Team
compliance: [Decree-13-2023]
date: 2026-05-10
wave: Wave 3E
---

# Decree 13/2023 — Personal Data Protection

## Problem Statement
Decree 13/2023/ND-CP establishes the Vietnamese personal data protection
framework (analogous to GDPR). Engineering teams need to know which
patterns and controls satisfy the Decree's requirements for lawful
processing, data subject rights, and cross-border transfers.

## Context
The Decree applies to any organisation processing personal data of
Vietnamese citizens. It introduces explicit consent requirements,
data subject rights (access, correction, deletion, portability), and
mandatory Data Protection Impact Assessments (DPIA) for high-risk
processing.

## Solution

```mermaid
graph TB
    subgraph Decree13Articles
        Art9[Article 9\nLawful Basis + Consent]
        Art16[Article 16\nData Subject Rights]
        Art23[Article 23\nCross-Border Transfer]
        Art25[Article 25\nDPIA Requirement]
    end
    subgraph Controls
        Art9 --> ConsentMgmt[Consent Management\nREF-009 Account Opening]
        Art16 --> DSR[DSR Workflow\nSEC-008 Data Masking]
        Art23 --> CBDT[Cross-Border\nData Transfer Assessment]
        Art25 --> DPIA[DPIA Process\nSEC-013 PII Tokenization]
    end
```

## Key Controls Reference

### Article 9 — Lawful Basis and Consent
- Six lawful bases; consent is most common for banking retail customers
- Consent must be explicit, granular, and withdrawable
- Consent records retained for the duration of processing + 5 years

**Architecture implication**: REF-009 Account Opening Omnichannel captures granular consent per processing purpose; consent records stored in an append-only consent ledger.

### Article 16 — Data Subject Rights
- Right of access: provide copy of personal data within 30 days
- Right of correction: update inaccurate data within 72 h
- Right of deletion: erase data within 30 days (subject to retention obligations)
- Right of portability: export in machine-readable format

**Architecture implication**: A DSR (Data Subject Request) workflow API orchestrates fulfillment. SEC-008 Data Masking ensures PII is masked in logs before any export.

### Article 23 — Cross-Border Data Transfer
- Transfer requires DPPA approval or Standard Contractual Clauses equivalent
- Data localisation: certain categories (biometrics, health) must remain in Vietnam

**Architecture implication**: Cloud region selection for PII workloads must be `ap-southeast-1` (Singapore) at minimum with data residency controls; biometric data (MOB-003) stored on-device only.

### Article 25 — DPIA Requirement
- Mandatory for large-scale processing, profiling, or processing sensitive categories
- DPIA must be filed with DPPA before processing begins

**Architecture implication**: SEC-013 PII Tokenization reduces DPIA scope by minimising raw PII in downstream systems.

## DSR Fulfillment Flow (Java 21)

```java
@RestController
@RequestMapping("/v1/dsr")
public class DataSubjectRequestController {

    private final DsrOrchestrationService dsrSvc;

    @PostMapping("/access")
    @PreAuthorize("hasRole('DSR_AGENT')")
    public ResponseEntity<DsrTicket> requestAccess(
            @RequestBody DsrAccessRequest request) {
        DsrTicket ticket = dsrSvc.initiateAccess(
            request.getSubjectId(), request.getRequestedCategories());
        return ResponseEntity.accepted().body(ticket);
    }
}
```

## NFR Acceptance Criteria
- DSR access fulfillment: data package delivered within 30 calendar days
- DSR deletion: PII erased from all live systems within 30 days (audit log retains pseudonymised reference)
- Consent withdrawal: processing stopped within 24 h of withdrawal

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.18.1 | Legal and regulatory compliance for personal data |
| Ring 1 | — | N/A (no direct Ring 1 overlap) |
| Ring 2 | Decree 13/2023/ND-CP (all articles) | Primary regulatory instrument ⚠️ (working summary — pending Legal review) |

## Threat Model
| Threat | Control |
|--------|---------|
| Consent record tampering | Consent ledger is append-only; HMAC-chained (SEC-012) |
| Unauthorised DSR fulfillment | DSR agent role + dual-auth for deletion requests |
| Data exfiltration via DSR export | Export file encrypted; delivered via authenticated portal only |

## Related Patterns
- SEC-008 Data Masking — masks PII in logs and exports
- SEC-013 PII Tokenization — reduces raw PII surface area
- REF-009 Account Opening — captures consent at onboarding

## References
- Decree 13/2023/ND-CP on Personal Data Protection (effective 1 July 2023)
- DPPA (Department of Personal Data Protection) guidance notes

## Key Takeaway
Map Decree 13/2023 articles to catalog patterns: Article 9 consent →
REF-009; Article 16 DSR rights → DSR workflow API + SEC-008; Article 23
cross-border → data residency controls; Article 25 DPIA → SEC-013
tokenization to reduce PII scope.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/compliance/decree-13-2023-personal-data.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/compliance/decree-13-2023-personal-data.md
git commit -m "feat(catalog): COMP-003 Decree 13/2023 Personal Data — Wave 3E"
```

---

### Task 51: COMP-004 PCI-DSS 4.0

**Files:**
- Create: `knowledge-base/compliance/pci-dss-4-0.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: COMP-004
title: "PCI-DSS 4.0 Control Mapping"
domain: compliance
status: Draft
tier: Tier 1
owner: Compliance and Security Team
compliance: [PCI-DSS]
date: 2026-05-10
wave: Wave 3E
---

# PCI-DSS 4.0 Control Mapping

## Problem Statement
Payment Card Industry Data Security Standard v4.0 mandates specific
controls for any system that stores, processes, or transmits cardholder
data (CHD). Architecture teams need a control-to-pattern mapping to
scope the cardholder data environment (CDE) correctly.

## Context
PCI-DSS 4.0 introduces customised implementation (CI) as an alternative
to the defined approach, allowing teams to demonstrate equivalent
security through compensating controls. The bank's CDE is scoped to the
card processing microservices cluster and the ledger posting engine.

## Solution

```mermaid
graph TB
    subgraph CDE
        CardAPI[Card API] --> Tokenizer[SEC-013\nPCI Point-to-Point\nTokenization]
        Tokenizer --> LedgerEngine[REF-010\nLedger Posting Engine]
        LedgerEngine --> DB[(PCI Segmented\nPostgreSQL)]
    end
    subgraph OutsideCDE
        Tokenizer -->|token only| Analytics[Analytics\nData Warehouse]
        Tokenizer -->|token only| Reporting[REF-008\nRegulatory Reporting]
    end
    FW[Network Segmentation\nFirewall + mTLS] --> CDE
```

## Key Requirements Reference

### Requirement 1 — Network Security Controls
- CDE network segment isolated via firewall + Kubernetes NetworkPolicy
- Only explicitly permitted traffic flows (deny-all default)

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: cde-isolate
  namespace: card-processing
spec:
  podSelector: {}
  policyTypes: [Ingress, Egress]
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: api-gateway
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: card-processing
```

### Requirement 3 — Protect Stored Account Data
- PANs must not be stored in plaintext; FF3-1 format-preserving encryption (SEC-013) or tokenization
- SAD (CVV, PIN) must never be stored post-authorisation

**Architecture implication**: SEC-013 PII Tokenization implements FF3-1 FPE; post-authorisation SAD purge is enforced by application logic with DB-level trigger verification.

### Requirement 6 — Secure Systems and Software
- All CDE dependencies scanned via OWASP Dependency-Check in CI/CD
- Web application firewall (WAF) in front of Card API

**Architecture implication**: Apigee WAF policy applied to `/v1/card/**` endpoints; OWASP scan gate in GitHub Actions pipeline.

### Requirement 10 — Log and Monitor All Access
- All CDE access logged; logs retained ≥ 12 months (3 months immediately accessible)
- Automated log review for anomalies

**Architecture implication**: SEC-012 Immutable Audit Log covers all CDE writes; SIEM ingests logs with 90-day hot storage, 12-month cold storage.

### Requirement 12.10 — Chargeback Handling
- Documented chargeback procedure with defined roles and timelines

**Architecture implication**: REF-012 Dispute Management implements the procedure.

## NFR Acceptance Criteria
- CDE network segmentation: zero lateral movement from non-CDE namespace (verified quarterly by pen test)
- PAN storage: 0 plaintext PAN records in any database (verified by automated scanner)
- Log retention: 12 months; hot access ≤ 3 months old

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.13.1 | Network segregation for sensitive systems |
| Ring 1 | PCI-DSS 4.0 (all requirements) | Primary regulatory instrument |
| Ring 2 | SBV Circular 09/2020 Article 8 | Card data processing obligations ⚠️ (working summary — pending Legal review) |

## Threat Model
| Threat | Control |
|--------|---------|
| CDE scope creep | Token-only data flows outside CDE; PAN never leaves tokenizer |
| Insider PAN exfiltration | DLP on database exports; audit log on all SELECT queries against card tables |
| Dependency vulnerability | OWASP Dependency-Check blocks CI on CVSS ≥ 7 findings |

## Related Patterns
- SEC-013 PII Tokenization — PAN tokenization (Point-to-Point Encryption equivalent)
- REF-012 Dispute Management — Requirement 12.10 chargeback procedure
- SEC-012 Immutable Audit Log — Requirement 10 log integrity

## References
- PCI Security Standards Council: PCI DSS v4.0 (March 2022)
- PCI SSC Guidance: Customised Implementation Overview

## Key Takeaway
Minimise CDE scope by tokenising PANs at ingress (SEC-013); enforce
network isolation via Kubernetes NetworkPolicy; all CDE access is logged
via SEC-012 with 12-month retention.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/compliance/pci-dss-4-0.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/compliance/pci-dss-4-0.md
git commit -m "feat(catalog): COMP-004 PCI-DSS 4.0 — Wave 3E"
```

---

### Task 52: COMP-005 Basel BCBS 239

**Files:**
- Create: `knowledge-base/compliance/basel-bcbs-239.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: COMP-005
title: "Basel BCBS 239 — Risk Data Aggregation and Reporting"
domain: compliance
status: Draft
tier: Tier 1
owner: Finance and Risk Team
compliance: [BCBS-239]
date: 2026-05-10
wave: Wave 3E
---

# Basel BCBS 239 — Risk Data Aggregation and Reporting

## Problem Statement
BCBS 239 requires globally systemically important banks (G-SIBs) to
produce accurate, complete, and timely risk data and reports. Ad-hoc
data extraction from operational systems produces inconsistent risk
figures across departments.

## Context
The 14 principles of BCBS 239 span governance, data architecture, and
reporting capabilities. The bank's data architecture (Data Vault 2.0 +
Trino) directly implements the data aggregation principles. This
document maps each principle to catalog patterns.

## Solution

```mermaid
graph TB
    subgraph BCBS239Principles
        P1[P1: Governance]
        P2[P2: Data Architecture]
        P3[P3: Accuracy and Integrity]
        P6[P6: Adaptability]
        P11[P11: Remediation]
    end
    subgraph Patterns
        P1 --> PRIN001[PRIN-001 API First Design]
        P2 --> DATA004[DATA-004 Data Vault 2.0]
        P2 --> DATA012[DATA-012 OpenLineage]
        P3 --> DATA009[DATA-009 DQ Great Expectations]
        P6 --> REF008[REF-008 Reg Reporting]
        P11 --> REF008
    end
```

## Principle-to-Pattern Mapping

### Principle 1 — Governance
Establish clear ownership and accountability for risk data quality.

**Implementation**: Data steward roles defined in `governance/standards/enterprise-architecture-catalog.md`; each pattern's `owner` field designates the accountable team. PRIN-001 API First Design mandates OpenAPI contracts that serve as the governance boundary.

### Principle 2 — Data Architecture and IT Infrastructure
Risk data must be accurate and maintain integrity throughout its lifecycle.

**Implementation**: DATA-004 Data Vault 2.0 provides a single version of truth with satellite history; DATA-012 OpenLineage tracks lineage from source to risk report cell.

### Principle 3 — Accuracy and Integrity
Risk data must be accurate and reliable.

**Implementation**: DATA-009 Data Quality (Great Expectations) validates critical risk datasets at ingestion; failed expectations block pipeline promotion.

```python
# Great Expectations suite for daily position data
suite.add_expectation(
    ExpectColumnValuesToBeBetween(
        column="balance",
        min_value=-1_000_000_000_000,
        max_value=1_000_000_000_000
    ))
suite.add_expectation(
    ExpectColumnValuesToNotBeNull(column="account_id"))
suite.add_expectation(
    ExpectColumnValuesToMatchRegex(column="currency", regex="^[A-Z]{3}$"))
```

### Principle 6 — Adaptability
Risk data and reports must be adaptable to meet ad-hoc requests.

**Implementation**: REF-008 Regulatory Reporting uses Trino's federated SQL over Delta Lake; new reports are new SQL queries, not new ETL pipelines.

### Principle 11 — Remediation
Supervisors expect timely remediation of deficiencies.

**Implementation**: REF-008 SLA monitoring alerts when submission is late; automated retry + escalation within the report scheduler.

## NFR Acceptance Criteria
- Risk data lineage: every report cell traceable to source transaction in OpenLineage
- Data quality gate: zero Critical DQ failures in daily position data
- Report delivery: ≤ 1 h after close of business (target; regulatory deadline may vary)

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.18.2 | Compliance review of risk data management |
| Ring 1 | BCBS 239 (all 14 principles) | Primary regulatory instrument |
| Ring 2 | SBV Circular 09/2020 Article 15 | Timely submission of risk position data ⚠️ (working summary — pending Legal review) |

## Related Patterns
- DATA-004 Data Vault 2.0 — single authoritative risk data store
- DATA-009 Data Quality — Principle 3 accuracy controls
- DATA-012 OpenLineage — Principle 2 data lineage
- REF-008 Regulatory Reporting — Principles 6 and 11 delivery

## References
- BCBS 239: Principles for effective risk data aggregation and risk reporting (January 2013)
- BIS Implementation progress report (annual)

## Key Takeaway
Implement BCBS 239 Principle 2 (data architecture) via Data Vault 2.0
+ OpenLineage; Principle 3 (accuracy) via Great Expectations gates;
Principles 6 and 11 (adaptability + remediation) via the Trino-backed
Regulatory Reporting hub.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/compliance/basel-bcbs-239.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/compliance/basel-bcbs-239.md
git commit -m "feat(catalog): COMP-005 Basel BCBS 239 — Wave 3E"
```

---

### Task 53: COMP-006 Basel BCBS 230

**Files:**
- Create: `knowledge-base/compliance/basel-bcbs-230.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: COMP-006
title: "Basel BCBS 230 — Operational Risk Principles"
domain: compliance
status: Draft
tier: Tier 2
owner: Risk Management Team
compliance: [BCBS-239]
date: 2026-05-10
wave: Wave 3E
---

# Basel BCBS 230 — Operational Risk Principles (PSMOR)

## Problem Statement
BCBS 230 (Principles for the Sound Management of Operational Risk)
requires banks to embed operational risk management into system design.
Architecture decisions that increase operational risk — single points of
failure, manual processes, opaque dependencies — must be identified and
mitigated.

## Context
BCBS 230's 11 principles span governance, risk culture, change
management, business continuity, and third-party risk. This document
maps the principles relevant to software architecture decisions.

## Solution

```mermaid
graph TB
    subgraph BCBS230Relevant
        P4[Principle 4\nChange Management]
        P6[Principle 6\nBusiness Continuity]
        P8[Principle 8\nIT Risk]
        P9[Principle 9\nThird-Party Risk]
    end
    subgraph ArchControls
        P4 --> CI[CI/CD Pipeline\nwith compliance gates]
        P6 --> HA[HA Deployment\nN+1, Multi-AZ]
        P8 --> OBS[Observability\nSEC-012 Audit Log]
        P9 --> ACL[INT-005\nAnti-Corruption Layer]
    end
```

## Principle-to-Control Mapping

### Principle 4 — Change Management
All significant changes to systems must follow a documented change process that includes risk assessment.

**Architecture implication**: Every catalog pattern promotion (Proposed → Draft → Approved) requires compliance sign-off in the enterprise architecture catalog. CI/CD pipelines include compliance gate checks (`python3 scripts/check-compliance-rows.py`).

### Principle 6 — Business Continuity Planning
Systems must have documented BCP with tested recovery procedures.

**Architecture implication**: NFR Acceptance Criteria in every Tier 1 pattern specify RTO and RPO. Runbook stubs are the seed for full BCP documentation. Critical systems deploy N+1 across Multi-AZ with automated failover.

### Principle 8 — IT Risk
Information technology risk must be managed as a component of operational risk.

**Architecture implication**: SEC-012 Immutable Audit Log provides the operational risk event trail. Threat Model sections in every pattern document residual IT risks and their controls.

### Principle 9 — Third-Party Risk
Third-party arrangements must be subject to appropriate due diligence.

**Architecture implication**: INT-005 Anti-Corruption Layer isolates the domain model from third-party (T24, SWIFT, VNeID) contract changes. Vendor SLAs are referenced in NFR Acceptance Criteria rows.

## Operational Risk Event Taxonomy (Java 21 Enum)

```java
public enum OperationalRiskCategory {
    INTERNAL_FRAUD,           // SEC-012 audit chain
    EXTERNAL_FRAUD,           // REF-007 fraud screening
    EMPLOYMENT_PRACTICES,     // SEC-010 ABAC
    CLIENTS_PRODUCTS_BUSINESS_PRACTICES,  // COMP-003 Decree 13
    DAMAGE_TO_PHYSICAL_ASSETS,
    BUSINESS_DISRUPTION_SYSTEMS, // BCP / Principle 6
    EXECUTION_DELIVERY_MANAGEMENT // BSP-001 ledger + BSP-002 idempotency
}
```

## NFR Acceptance Criteria
- Change management: 100% of Tier 1 system changes pass compliance gate before production
- BCP test: annual DR drill with documented RTO/RPO achievement results
- Third-party SLA breach: automated alert within 5 min of SLA threshold exceeded

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.17.1 | Business continuity planning |
| Ring 1 | BCBS 230 Principles 4, 6, 8, 9 | Operational risk management principles |
| Ring 2 | SBV Circular 09/2020 Article 13 | Incident response and business continuity ⚠️ (working summary — pending Legal review) |

## Related Patterns
- INT-005 Anti-Corruption Layer — Principle 9 third-party isolation
- SEC-012 Immutable Audit Log — Principle 8 IT risk event trail
- REF-007 Fraud Screening — Principle 8 external fraud detection

## References
- BCBS 230: Principles for the Sound Management of Operational Risk (June 2011)
- Basel Committee: Operational Resilience guidance (March 2021)

## Key Takeaway
Map BCBS 230 Principles 4/6/8/9 to catalog controls: change gates in
CI/CD (P4), N+1 HA + runbook stubs (P6), SEC-012 audit log (P8),
INT-005 ACL for third-party isolation (P9).
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/compliance/basel-bcbs-230.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/compliance/basel-bcbs-230.md
git commit -m "feat(catalog): COMP-006 Basel BCBS 230 — Wave 3E"
```

---

### Task 54: COMP-007 ISO 20022 Messaging

**Files:**
- Create: `knowledge-base/compliance/iso-20022-messaging.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: COMP-007
title: "ISO 20022 Messaging Standard"
domain: compliance
status: Draft
tier: Tier 1
owner: Payments Architecture Team
compliance: [ISO-20022, SWIFT-CSP]
date: 2026-05-10
wave: Wave 3E
---

# ISO 20022 Messaging Standard

## Problem Statement
SWIFT's ISO 20022 migration requires banks to support pacs.008
(credit transfer), pacs.004 (return), and camt.056 (cancellation)
alongside legacy MT messages through the coexistence period. Teams
building payment systems need a reference for message construction and
validation.

## Context
ISO 20022 uses XML with strict schema validation. The bank must support
MX messages natively in new systems while maintaining MT translation for
legacy correspondent banks. Apache Camel + SWIFT SDK handles
transformation.

## Solution

```mermaid
sequenceDiagram
    participant Sender as Payments API
    participant Builder as ISO20022 Builder
    participant Validator as Schema Validator
    participant Gateway as SWIFT Alliance

    Sender->>Builder: buildCreditTransfer(payment)
    Builder->>Builder: populate pacs.008 fields
    Builder->>Validator: validate(pacs008Xml)
    Validator-->>Builder: valid / errors
    Builder-->>Sender: pacs008Xml
    Sender->>Gateway: submit(pacs008Xml)
```

### pacs.008 Message Builder (Java 21)

```java
@Service
public class Pacs008Builder {

    public Document buildCreditTransfer(Payment payment) {
        FIToFICustomerCreditTransferV11 pacs008 =
            new FIToFICustomerCreditTransferV11();

        GroupHeader113 grpHdr = new GroupHeader113();
        grpHdr.setMsgId(payment.getMessageId());
        grpHdr.setCreDtTm(XMLGregorianCalendarUtil.now());
        grpHdr.setNbOfTxs("1");
        grpHdr.setTtlIntrBkSttlmAmt(
            ActiveCurrencyAndAmount.of(
                payment.getAmount(), payment.getCurrency()));
        pacs008.setGrpHdr(grpHdr);

        CreditTransferTransaction64 txInfo =
            new CreditTransferTransaction64();
        txInfo.setPmtId(buildPaymentId(payment));
        txInfo.setIntrBkSttlmAmt(
            ActiveCurrencyAndAmount.of(
                payment.getAmount(), payment.getCurrency()));
        txInfo.setDbtr(buildParty(payment.getDebtor()));
        txInfo.setCdtr(buildParty(payment.getCreditor()));
        pacs008.getCdtTrfTxInf().add(txInfo);

        Document doc = new Document();
        doc.setFIToFICstmrCdtTrf(pacs008);
        return doc;
    }
}
```

### Schema Validation

```java
@Component
public class Iso20022Validator {

    private final Schema pacs008Schema;

    public ValidationResult validate(String xmlPayload) {
        try {
            Validator validator = pacs008Schema.newValidator();
            validator.validate(new StreamSource(
                new StringReader(xmlPayload)));
            return ValidationResult.valid();
        } catch (SAXException e) {
            return ValidationResult.invalid(e.getMessage());
        }
    }
}
```

## Message Types Reference

| MX Message | MT Equivalent | Purpose |
|-----------|---------------|---------|
| pacs.008  | MT103         | Credit transfer |
| pacs.004  | MT202 RTN     | Payment return |
| camt.056  | MT199         | Cancellation request |
| camt.054  | MT910/950     | Account notification |

## When to Use
- New payment system development targeting SWIFT ISO 20022
- Correspondent bank integrations that support MX natively

## When Not to Use
- Domestic Napas real-time payments — use Napas proprietary format
- Legacy correspondent banks on MT only — use MT serialiser in REF-005

## NFR Acceptance Criteria
- pacs.008 schema validation: 100% pass before submission to SWIFT
- MT↔MX round-trip accuracy: validated against SWIFT conformance test set
- Message construction p99 ≤ 20 ms

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.14.2 | Secure software development for payment messages |
| Ring 1 | ISO 20022 + SWIFT CSP v2024 Control 1.1 | Mandatory MX message format compliance |
| Ring 2 | SBV Circular 09/2020 Chapter III | Cross-border payment message standards ⚠️ (working summary — pending Legal review) |

## Threat Model
| Threat | Control |
|--------|---------|
| Malformed message injection | Schema validator rejects non-compliant XML before submission |
| Man-in-the-middle on SWIFT channel | SWIFT PKI mutual authentication; TLS 1.3 |
| Message replay | SWIFT message ID + timestamp uniqueness enforced by network |

## Related Patterns
- REF-005 SWIFT MT/MX Wire Transfer — uses this builder and validator
- REF-012 Dispute Management — camt.056 cancellation messages

## References
- ISO 20022 Universal financial industry message scheme (iso20022.org)
- SWIFT MX adoption programme documentation

## Key Takeaway
Build pacs.008/camt.056 messages using a strongly-typed Java builder
against the ISO 20022 XSD schema; validate before submission; maintain
MT equivalents in the Apache Camel route (REF-005) for legacy
correspondents.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/compliance/iso-20022-messaging.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/compliance/iso-20022-messaging.md
git commit -m "feat(catalog): COMP-007 ISO 20022 Messaging — Wave 3E"
```

---

### Task 55: COMP-008 SWIFT CSP v2024

**Files:**
- Create: `knowledge-base/compliance/swift-csp-2024.md`

- [ ] **Step 1: Write the document**

```markdown
---
id: COMP-008
title: "SWIFT Customer Security Programme v2024"
domain: compliance
status: Draft
tier: Tier 1
owner: Security Architecture Team
compliance: [SWIFT-CSP]
date: 2026-05-10
wave: Wave 3E
---

# SWIFT Customer Security Programme v2024

## Problem Statement
SWIFT CSP v2024 mandates 32 security controls (23 mandatory + 9
advisory) for all SWIFT-connected institutions. Non-compliance blocks
connectivity to the SWIFT network. Architecture teams need a mapping of
CSP controls to implemented catalog patterns.

## Context
CSP controls are assessed annually via the Customer Security Controls
Framework (CSCF). The bank's SWIFT environment consists of the Alliance
Gateway, the messaging microservices, and the ledger posting engine.
Controls cover access management, malware protection, software integrity,
and transaction monitoring.

## Solution

```mermaid
graph TB
    subgraph SWIFTEnvironment
        AG[Alliance Gateway] --> MessagingSvc[Messaging\nMicroservices]
        MessagingSvc --> LedgerEngine[REF-010\nLedger Posting]
    end
    subgraph MandatoryControls
        C11[1.1 SWIFT Environment\nProtection — Network Seg]
        C21[2.1 Internal Data Flow\nSecurity — mTLS]
        C22[2.2 Security Updates\n— Patch Management]
        C52[5.2 Token Auth\n— MFA]
        C61[6.1 Operator\nSession Confidentiality]
        C72[7.2 Transaction\nBusiness Controls]
    end
    subgraph Patterns
        C11 --> COMP004[COMP-004 PCI-DSS\nNetwork Segmentation]
        C21 --> INT007[INT-007 Service Mesh\nmTLS]
        C22 --> DevSecOps[CI/CD OWASP\nDependency-Check]
        C52 --> SEC006[SEC-006 JWT\n+ MFA Gate]
        C61 --> SEC007[SEC-007 Secrets\nRotation — PAM]
        C72 --> BSP003[BSP-003 Sanction\nScreening]
    end
```

## Mandatory Control Mapping

| CSP Control | Description | Implementing Pattern |
|-------------|-------------|---------------------|
| 1.1 | SWIFT Environment Protection | COMP-004 PCI-DSS network segmentation |
| 1.2 | Privileged Access Control | SEC-007 Secrets Rotation (PAM sessions) |
| 1.3 | Virtual Platform Security | K8s Pod Security Standards (restricted) |
| 2.1 | Internal Data Flow Security | INT-007 Service Mesh mTLS |
| 2.2 | Security Updates | CI/CD: OWASP Dependency-Check gate (CVSS ≥ 7 blocks) |
| 2.3 | System Hardening | CIS Kubernetes Benchmark Level 2 |
| 2.4A | Back-Office Data Flow Security | Kafka TLS + SCRAM-SHA-512 auth |
| 5.1 | Logical Access Controls | SEC-010 OPA/ABAC (least privilege) |
| 5.2 | Token Management | SEC-006 JWT + TOTP MFA for operators |
| 5.4 | Physical and Logical Password Storage | SEC-007 HashiCorp Vault dynamic secrets |
| 6.1 | Operator Session Confidentiality | Bastion host with session recording |
| 6.2 | Software Integrity | SLSA Level 2 provenance attestation on all SWIFT-path images |
| 7.1 | Cyber Incident Response | COMP-006 BCBS 230 BCP + incident runbook |
| 7.2 | Security Training Awareness | Annual CSP training (HR process, not architecture) |
| 7.3A | Penetration Testing | Quarterly CDE pen test; annual SWIFT environment test |
| 7.4A | Scenario Risk Assessment | Annual threat model review per pattern |

### PAM Session Recording (Mandatory 1.2)

```java
@Component
public class PrivilegedSessionAuditLogger {

    private final AuditLogService auditLog;

    @Around("@annotation(RequiresPrivilegedAccess)")
    public Object logPrivilegedSession(ProceedingJoinPoint pjp)
            throws Throwable {
        String operator = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        String operation = pjp.getSignature().toShortString();
        auditLog.record(AuditEvent.privileged(operator, operation,
            "START", Instant.now()));
        try {
            Object result = pjp.proceed();
            auditLog.record(AuditEvent.privileged(operator, operation,
                "SUCCESS", Instant.now()));
            return result;
        } catch (Exception e) {
            auditLog.record(AuditEvent.privileged(operator, operation,
                "FAILURE: " + e.getMessage(), Instant.now()));
            throw e;
        }
    }
}
```

### Transaction Business Controls (Mandatory 7.2)

```java
@Component
public class SwiftTransactionBusinessControls {

    private static final BigDecimal HIGH_VALUE_THRESHOLD =
        new BigDecimal("1000000"); // 1M USD equivalent

    public void enforce(Payment payment) {
        if (payment.getAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            requireDualAuthorisation(payment);
        }
        if (isUnusualCounterparty(payment.getCreditorBic())) {
            requireManualReview(payment);
        }
    }
}
```

## NFR Acceptance Criteria
- CSP assessment: 100% mandatory controls attested annually
- Privileged session recording: retained ≥ 12 months in WORM storage
- Patch management: critical CVEs remediated within 30 days of disclosure
- Pen test: SWIFT environment tested annually; findings remediated within 90 days

## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | ISO 27001 A.6.1 | Information security roles aligned with CSP governance |
| Ring 1 | SWIFT CSP v2024 CSCF (all 23 mandatory controls) | Primary regulatory instrument |
| Ring 2 | SBV Circular 09/2020 Chapter III | SWIFT connectivity obligations ⚠️ (working summary — pending Legal review) |

## Threat Model
| Threat | Control |
|--------|---------|
| Compromised SWIFT operator credential | MFA (CSP 5.2) + session recording (CSP 6.1) |
| SWIFT message tampering | mTLS + SWIFT PKI (CSP 2.1) |
| Unauthorised high-value transfer | Dual authorisation + sanction screening (CSP 7.2) |
| Software supply chain attack | SLSA Level 2 provenance + OWASP scan (CSP 2.2 + 6.2) |

## Related Patterns
- INT-007 Service Mesh — CSP 2.1 internal data flow security
- SEC-006 JWT Best Practices — CSP 5.2 token management
- SEC-007 Secrets Rotation — CSP 1.2 privileged access
- BSP-003 Sanction Screening — CSP 7.2 transaction business controls

## References
- SWIFT Customer Security Controls Framework (CSCF) v2024
- SWIFT Security Guidance for Financial Institutions

## Key Takeaway
All 23 mandatory CSP v2024 controls are implemented by existing catalog
patterns; this document is the attestation mapping used for annual
SWIFT self-assessment and KYC-SA submission.
```

- [ ] **Step 2: Run Mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/compliance/swift-csp-2024.md
```
Expected: `All Mermaid blocks passed.`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/compliance/swift-csp-2024.md
git commit -m "feat(catalog): COMP-008 SWIFT CSP v2024 — Wave 3E"
```

---

## Wave 3E Gate — Compliance Deep Dives (7 docs)

- [ ] **Step 1: Batch lint**

```bash
for f in \
  knowledge-base/compliance/sbv-circular-09-2020.md \
  knowledge-base/compliance/decree-13-2023-personal-data.md \
  knowledge-base/compliance/pci-dss-4-0.md \
  knowledge-base/compliance/basel-bcbs-239.md \
  knowledge-base/compliance/basel-bcbs-230.md \
  knowledge-base/compliance/iso-20022-messaging.md \
  knowledge-base/compliance/swift-csp-2024.md; do
  bash scripts/mermaid-lint-doc.sh "$f" || echo "FAIL: $f"
done
```
Expected: 7 files, 0 FAIL lines.

- [ ] **Step 2: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: no FAIL for any Wave 3E file.

- [ ] **Step 3: Update catalog — COMP-002–008 → Draft**

In `governance/standards/enterprise-architecture-catalog.md`, change all 7 rows from `Proposed` → `Draft`.

```bash
grep -E "COMP-00[2-8]" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |"
```
Expected: 7 lines.

- [ ] **Step 4: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md
git commit -m "chore(catalog): Wave 3E gate — COMP-002–008 promoted to Draft"
```

---

## Final Verification — All 55 Docs

- [ ] **Step 1: Full compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: 0 FAIL lines across all 55 newly drafted documents.

- [ ] **Step 2: Full catalog count**

```bash
grep "| Draft |" governance/standards/enterprise-architecture-catalog.md | wc -l
```
Expected: count includes all 55 Wave 3 documents (plus any pre-existing Draft entries).

- [ ] **Step 3: Verify all Wave 3 entries are Draft**

```bash
grep -E "(DATA-00[4-9]|DATA-01[0-3]|INT-00[5-9]|SEC-00[6-9]|SEC-01[0-3]|FE-00[1-6]|MOB-00[1-6]|BSP-00[1-5]|REF-00[5-9]|REF-01[012]|COMP-00[2-8])" \
  governance/standards/enterprise-architecture-catalog.md | grep "| Proposed |"
```
Expected: 0 lines (all promoted to Draft).

- [ ] **Step 4: Commit the plan file itself**

```bash
git add docs/superpowers/plans/2026-05-10-wave-3-stub-authoring.md
git commit -m "docs(plans): Wave 3 stub-authoring plan complete — 55 tasks across 5 sub-waves"
```
