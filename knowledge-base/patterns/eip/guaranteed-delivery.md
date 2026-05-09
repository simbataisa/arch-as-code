# Guaranteed Delivery

Status: Draft | Last Reviewed: 2026-05-09 | Owner: @tech-lead-backend
Catalog ID: EIP-023 | Radii
Tier Applicability: T0, T1

## Problem Statement

- A payment event that disappears during a broker hiccup, a producer crash, or a consumer restart is not a recoverable error — it is a missing transaction. In banking, message loss manifests as a ledger gap, an unreconciled settlement, a customer not notified of a credit, or a fraud alert that never reached the screening engine. There is no acceptable message-loss rate for T0 financial channels.
- Default Kafka producer settings (`acks=1`, `enable.idempotence=false`) acknowledge the message the moment the partition leader writes it, before followers have replicated. A leader failure between write and replication causes the acknowledged message to be lost — silently, because the producer received a success response. This is the most common source of "lost messages in production" incidents.
- Consumer `enable.auto.commit=true` commits the offset on a background timer regardless of whether the side-effect (ledger post, T24 OFS call) succeeded. A JVM crash between offset commit and side-effect completion causes the message to appear processed but its effect to be absent — a silent loss indistinguishable from a successful process.
- Without Guaranteed Delivery as an explicit architectural concern, each team configures Kafka independently. One team uses `acks=all`; another inherits the default `acks=1`; a third uses auto-commit. The result is inconsistent durability guarantees across a platform where every component must be consistent.

## Solution

Guaranteed Delivery ensures every message published by a producer is received by at least one consumer exactly once (from the consumer's perspective), even in the presence of broker failures, network partitions, consumer restarts, and DR failovers. The pattern combines four complementary mechanisms that together form a layered durability guarantee:

```mermaid
graph TD
    subgraph Producer Side
        APP[Application Service]
        OUTBOX[(Transactional Outbox\nPostgreSQL\nINT-002)]
        RELAY[Outbox Relay\nCDC / Debezium]
    end

    subgraph Kafka Broker - RF=3
        direction LR
        L["Partition Leader\n(Broker 1)"]
        F1["Follower\n(Broker 2)"]
        F2["Follower\n(Broker 3)"]
        L -->|"replicate"| F1
        L -->|"replicate"| F2
    end

    subgraph Consumer Side
        CONSUMER[Consumer Pod]
        SIDEEFFECT[(Side-Effect Target\nLedger / T24 OFS)]
        DEDUPE[(Dedupe Store\nEIP-024)]
        DLT[Dead Letter Topic\nEIP-025]
    end

    APP -->|"1. write outbox row\n(same DB txn as\nbusiness write)"| OUTBOX
    OUTBOX -->|"2. CDC event"| RELAY
    RELAY -->|"3. produce\nacks=all"| L
    L -->|"4. ack after\nISR sync"| RELAY

    L --> CONSUMER
    CONSUMER -->|"5. check dedupe"| DEDUPE
    CONSUMER -->|"6. apply\nside-effect"| SIDEEFFECT
    CONSUMER -->|"7. commit offset\nafter side-effect"| L
    CONSUMER -->|"failure after\nN retries"| DLT

    style OUTBOX fill:#d4edda,stroke:#28a745
    style DLT fill:#f8d7da,stroke:#dc3545
    style DEDUPE fill:#cce5ff,stroke:#004085
```

### Delivery guarantee layers

| Layer | Mechanism | Failure it covers |
|---|---|---|
| L1: Outbox | DB transaction includes event row (INT-002) | Application crash before Kafka produce |
| L2: Broker durability | `acks=all`, RF=3, min.insync.replicas=2 | Broker leader failure before replication |
| L3: Producer idempotence | `enable.idempotence=true`, `retries=MAX_VALUE` | Producer retry producing duplicates |
| L4: Consumer commit-after-effect | Manual offset commit after side-effect | Consumer crash between effect and commit |
| L5: Idempotent consumer | Dedupe store (EIP-024) | At-least-once delivery producing duplicates |
| L6: Dead Letter Channel | DLT after N retries (EIP-025) | Permanent processing failures |

## Implementation Guidelines

1. **Configure Kafka producers for maximum durability.** Apply these settings to every `ProducerFactory` in the codebase. Enforce via a shared `KafkaProducerDefaults` configuration module that all teams extend — never let teams configure producers ad hoc.

   ```java
   @Configuration
   public class GuaranteedDeliveryProducerConfig {

       @Bean
       public ProducerFactory<String, Object> durableProducerFactory(
               KafkaProperties props) {
           Map<String, Object> config = props.buildProducerProperties();

           // Durability — acks=all ensures ISR has written before ack
           config.put(ProducerConfig.ACKS_CONFIG, "all");

           // Idempotence — prevents duplicate produce on retry
           config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

           // Retries — retry indefinitely within delivery.timeout.ms
           config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
           config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000);

           // Batching — balance throughput with latency
           config.put(ProducerConfig.LINGER_MS_CONFIG, 5);
           config.put(ProducerConfig.BATCH_SIZE_CONFIG, 65_536);

           // In-flight limit — required when enable.idempotence=true
           config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

           return new DefaultKafkaProducerFactory<>(config);
       }
   }
   ```

2. **Configure the Kafka broker topic for T0 durability.** These settings are enforced via GitOps IaC (Terraform or Ansible) — no manual console configuration.

   ```yaml
   # Terraform: kafka_topic resource for T0 financial channels
   topic_config:
     replication.factor: 3
     min.insync.replicas: 2
     unclean.leader.election.enable: "false"   # never elect an out-of-sync replica
     retention.ms: "2592000000"                # 30 days for T0
     cleanup.policy: "delete"
     compression.type: "lz4"
   ```

   `unclean.leader.election.enable=false` is critical: allowing an out-of-sync replica to become leader trades availability for durability — in banking, durability wins. Accept a brief producer error rather than silently losing messages.

3. **Configure consumers with manual offset commit.** Never use `enable.auto.commit=true` on financial channels. Commit the offset only after the side-effect is durably written.

   ```java
   @Configuration
   public class GuaranteedDeliveryConsumerConfig {

       @Bean
       public ConsumerFactory<String, Object> durableConsumerFactory(
               KafkaProperties props) {
           Map<String, Object> config = props.buildConsumerProperties();

           // Manual offset commit — we control when "processed" means
           config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

           // Fetch small batches for faster per-message commit
           config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 50);

           // Fail fast if consumer is too slow (avoid session timeout)
           config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300_000);

           return new DefaultKafkaConsumerFactory<>(config);
       }

       @Bean
       public ConcurrentKafkaListenerContainerFactory<String, Object>
               manualAckContainerFactory(
               ConsumerFactory<String, Object> cf,
               DefaultErrorHandler errorHandler) {
           var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
           factory.setConsumerFactory(cf);
           factory.getContainerProperties()
               .setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
           factory.setCommonErrorHandler(errorHandler);
           return factory;
       }
   }
   ```

4. **Use `@RetryableTopic` for automated retry with Dead Letter Channel integration.** Spring Kafka's `@RetryableTopic` creates intermediate retry topics with exponential backoff, transparently forwarding to the DLT after exhausting attempts. This is the recommended approach for T1 channels; for T0 channels, use the Transactional Outbox (INT-002) pattern to ensure at-least-once from the database perspective.

   ```java
   @Component
   @RequiredArgsConstructor
   @Slf4j
   public class PaymentEventConsumer {

       private final LedgerService ledger;
       private final IdempotentReceiverService dedupe;  // EIP-024

       @RetryableTopic(
           attempts = "4",
           backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 30_000),
           dltTopicSuffix = "-dlt",
           autoCreateTopics = "false",  // topics pre-created via IaC
           include = {TransientInfrastructureException.class}
       )
       @KafkaListener(
           topics = "com.techcombank.payments.transaction.created",
           groupId = "ledger-poster",
           containerFactory = "manualAckContainerFactory"
       )
       public void onPaymentCreated(
               @Payload TransactionCreatedEvent event,
               @Header("messageId") String messageId,
               Acknowledgment ack) {

           // EIP-024: deduplicate before applying side-effect
           if (dedupe.alreadyProcessed(messageId, "ledger-poster")) {
               log.debug("Duplicate payment event {} absorbed", messageId);
               ack.acknowledge();
               return;
           }

           ledger.post(event);
           dedupe.markProcessed(messageId, "ledger-poster");
           ack.acknowledge();  // commit offset AFTER side-effect + dedupe record

           log.info("Payment event processed: messageId={} transactionId={} "
               + "correlationId={}",
               messageId, event.getTransactionId(), MDC.get("correlationId"));
       }

       @DltHandler
       public void onDeadLetter(
               @Payload TransactionCreatedEvent event,
               @Header("messageId") String messageId,
               @Header(KafkaHeaders.RECEIVED_TOPIC) String sourceTopic) {
           log.error("Payment event reached DLT after all retries: "
               + "messageId={} sourceTopic={} transactionId={}",
               messageId, sourceTopic, event.getTransactionId());
           // Alert + triage workflow triggered by DLT consumer (EIP-025)
       }
   }
   ```

5. **Use the Transactional Outbox (INT-002) for T0 producers that must never lose a message between the business write and the Kafka publish.** The outbox pattern writes the event to a PostgreSQL table within the same database transaction as the business entity update. A CDC relay (Debezium or a polling relay) reads the outbox table and publishes to Kafka. This guarantees that if the application crashes after the business write, the event is still published on restart — the event's existence is tied to the database transaction, not the JVM's in-memory state.

   ```java
   @Service
   @RequiredArgsConstructor
   @Transactional
   public class PaymentCommandService {

       private final PaymentRepository payments;
       private final OutboxEventRepository outbox;  // INT-002

       public void processPayment(CreatePaymentCommand cmd) {
           Payment payment = Payment.from(cmd);
           payments.save(payment);

           // Write to outbox IN THE SAME transaction as the payment save
           // If this transaction commits, the event WILL be published (by CDC relay)
           // If this transaction rolls back, the event is NOT published
           outbox.save(OutboxEvent.builder()
               .aggregateType("Payment")
               .aggregateId(payment.getId().toString())
               .eventType("TransactionCreated")
               .payload(serialize(TransactionCreatedEvent.from(payment)))
               .correlationId(MDC.get("correlationId"))
               .build());
       }
   }
   ```

6. **Configure cross-region replication for T0 channels using MirrorMaker 2.** A T0 channel that is lost entirely in a region-level failure must be recoverable from the DR region. MirrorMaker 2 replicates with a configurable lag target (< 5 seconds under normal conditions).

   ```yaml
   # MirrorMaker 2 configuration (mm2.properties)
   clusters = primary, dr
   primary.bootstrap.servers = kafka-primary:9092
   dr.bootstrap.servers = kafka-dr:9092
   primary->dr.enabled = true
   primary->dr.topics = com.techcombank.payments.*,com.techcombank.kyc.*
   replication.factor = 3
   checkpoints.topic.replication.factor = 3
   heartbeats.topic.replication.factor = 3
   ```

7. **Integrate T24 OFS calls within the manual-commit consumer pattern.** The OFS call must complete before the Kafka offset is committed. If the OFS call fails with a transient error (T24 timeout), the consumer exception causes the message to remain on the topic and be retried. If the OFS call fails with a permanent error (account closed), the message routes to the DLT for human triage — the ledger entry is NOT posted, and the customer's account remains consistent.

## Banking Use Cases

1. **Payment event guaranteed delivery end-to-end** — A NAPAS credit transfer is initiated. The Payment Service writes the business record AND an outbox event in one PostgreSQL transaction. The CDC relay picks up the outbox event and publishes to `com.techcombank.payments.transaction.created` with `acks=all`. The Ledger Poster consumer deduplicates (EIP-024), posts to T24, commits the offset, and releases the outbox row. If T24 is slow, the Kafka offset is not committed; the message is retried up to 4 times with backoff. If T24 permanently fails, the message goes to the DLT (EIP-025) for manual posting. Zero message loss at every failure point.

2. **SWIFT outbound message guaranteed delivery** — A USD wire transfer command must reach the SWIFT adapter without loss. The Process Manager (EIP-017) writes the SWIFT submission command to the outbox. The CDC relay publishes it to `com.techcombank.rails.swift.payment.queued`. The SWIFT adapter is the consumer: it takes the message, submits to SWIFT gpi, and commits the Kafka offset only after SWIFT returns a successful acknowledgement (UETR confirmed). If SWIFT times out, the message is retried. If SWIFT rejects permanently (invalid BIC), the message goes to the DLT for the SWIFT operations team.

3. **Fraud alert guaranteed delivery** — A fraud engine raises an alert. The alert must be delivered to the Card Management service (to block the card) without fail. The fraud engine publishes to `com.techcombank.fraud.alert.raised` with `acks=all`. The Card Management consumer applies manual-commit: block the card in T24, then commit the offset. A consumer crash between card-block and offset-commit causes a redeliver; the Idempotent Receiver (EIP-024) absorbs the duplicate. No card blocking is missed; no card is blocked twice.

4. **KYC event guaranteed delivery during DR failover** — During a DR failover, Techcombank switches from the primary region to the DR region. MirrorMaker 2 has been replicating KYC events with < 5 seconds lag. Consumers in the DR region pick up from the replicated offset (or the last committed offset in the DR consumer group). Tail events from the primary that have not yet been replicated are replayed by the upstream service on reconnection. Idempotent receivers absorb any duplicates from the replay.

5. **EOD batch settlement guaranteed delivery** — The EOD settlement batch aggregator (EIP-011) releases a `SettlementBatch` event. This must reach the Reconciliation Service without loss — a missing EOD batch means an unreconciled day. The settlement channel uses T0 configuration (RF=3, acks=all, 30-day retention). The Reconciliation Service uses manual offset commit. The outbox pattern ensures the batch event is co-committed with the aggregator's MongoDB group deletion, so the event exists in Kafka if and only if the aggregation was finalised.

## Compliance Mapping

| Ring | Regulation | Provision | How this pattern satisfies |
|---|---|---|---|
| Ring 0 | EIP Book (Hohpe & Woolf) | Chapter 3 — Guaranteed Delivery | Canonical pattern; this doc applies all four mechanical layers to Techcombank's Kafka stack |
| Ring 0 | NIST SP 800-53 | SI-12 (Information Management), CP-10 (Information System Recovery) | Durable retention + cross-region replication constitutes the messaging layer of the system recovery plan |
| Ring 0 | ISO 27001 | A.12.3.1 (Information Backup) | Broker replication (RF=3) + MirrorMaker 2 + outbox persistence = three independent copies of every financial event |
| Ring 1 | BCBS 239 §6 | Completeness — "data should include all material information and should not be lost during aggregation" | Guaranteed Delivery is the technical control that makes §6 Completeness achievable; without it, the data pipeline is inherently leaky |
| Ring 1 | ISO 20022 | Settlement Finality — a confirmed ISO 20022 message is legally binding | `acks=all` + idempotent producer ensures the confirmed payment message in Kafka is exactly the message the downstream system processes |
| Ring 1 | SWIFT CSP 2024 | Control 1.2 — Secure the Infrastructure | SWIFT messages in transit on internal Kafka channels are protected by the same durability controls as all T0 channels; mTLS (SEC-001) adds confidentiality |
| Ring 2 | SBV Circular 09/2020 §IV.2 | Operational continuity — systems must continue processing during disruptions ⚠️ (working summary — pending Legal review) | MirrorMaker 2 cross-region replication + consumer manual-commit + DLT ensures that a single-region outage does not cause message loss; processing resumes in DR with the replicated message stream |

## NFR Acceptance Criteria

```yaml
nfr:
  catalog_id: EIP-023
  pattern: Guaranteed Delivery

  availability:
    target: 99.999%  # five nines for T0 financial channels
    broker_replication_factor: 3
    min_in_sync_replicas: 2
    unclean_leader_election: false   # never sacrifice durability for availability
    cross_region_replication: required_for_T0

  durability:
    message_loss_tolerance: 0        # zero for T0 and T1 financial channels
    producer_acks: all
    enable_idempotence: true
    consumer_auto_commit: false
    offset_commit_timing: "after side-effect durably persisted"
    outbox_required_for: T0          # optional but recommended for T1

  performance:
    produce_latency_p95_ms: 8        # acks=all adds ~3ms vs acks=1 on local cluster
    produce_latency_p99_ms: 20
    consumer_redelivery_latency_ms:  # time from failure to redelivery
      attempt_1: 1000
      attempt_2: 2000
      attempt_3: 4000
      attempt_4_dlt: immediate

  resilience:
    max_retry_attempts: 4
    backoff_initial_ms: 1000
    backoff_multiplier: 2.0
    backoff_max_ms: 30000
    dlt_required: true               # EIP-025 mandatory on all channels

  observability:
    required_metrics:
      - kafka_producer_record_error_rate
      - kafka_producer_request_latency_avg
      - kafka_consumer_records_lag_max
      - kafka_consumer_fetch_rate
      - outbox_relay_pending_events_total
      - dlt_depth_by_topic
    alerts:
      - name: GD_Producer_Error_Rate
        condition: "kafka_producer_record_error_rate > 0.001 over 5min"
        severity: Critical
      - name: GD_Consumer_Lag_High
        condition: "kafka_consumer_records_lag_max > 10000 over 5min"
        severity: High
      - name: GD_Outbox_Relay_Lag
        condition: "outbox_relay_pending_events_total > 1000 over 2min"
        severity: High
      - name: GD_MirrorMaker_Lag
        condition: "mirrormaker_replication_lag_seconds > 60"
        severity: Warning
```

## Cost/FinOps

- **Replication storage overhead** — RF=3 means every byte of data is stored 3× across brokers. For a 10M transactions/day T0 channel at 2KB/message × 30-day retention: 10M × 2KB × 30 × 3 = 1.8TB. At USD 0.023/GB-month, this is approximately USD 41/month per T0 topic. Compression (`lz4`) typically reduces this by 3–5×, bringing the effective cost to USD 8–14/month per T0 topic.
- **Outbox relay infrastructure** — A Debezium connector running in Kafka Connect adds approximately 0.5 vCPU and 512MB RAM overhead per database. For Techcombank's expected 3–5 outbox databases, total Debezium cost is approximately USD 50/month in pod compute. This is the price of the zero-message-loss guarantee between the database transaction and Kafka.
- **MirrorMaker 2 cross-region egress** — T0 channel replication to the DR region at 1.8TB/month incurs approximately USD 145/month in inter-region data transfer fees. This is a fixed, predictable cost for the DR durability guarantee required by SBV §IV.2 — budget it as a compliance cost.
- **`acks=all` latency premium** — Compared to `acks=1`, `acks=all` adds approximately 2–3ms to produce latency (time for ISR followers to acknowledge). This is within the T0 latency budget (< 8ms P95). The 2–3ms cost is non-negotiable: it is the cost of the durability guarantee. Any attempt to reduce this by switching to `acks=1` trades a small latency win for a real message-loss risk.
- **DLT triage labour** — Messages that exhaust retries and land in the DLT require human triage. At 0.01% failure rate on 10M transactions/day = 1,000 DLT entries/day. At 5 minutes per entry and a VND 300,000/hour triage engineer cost, this is approximately VND 250,000/day. The primary cost reduction lever is improving upstream data quality to reduce the failure rate — not loosening durability.

## Threat Model

- **Unclean leader election data loss** — With `unclean.leader.election.enable=true` (the Kafka default), a broker elected as leader without being in-sync may not have all messages, causing silent loss. Mitigation: `unclean.leader.election.enable=false` on all T0/T1 topics (IaC-enforced). Accept brief producer unavailability during leader election over silent message loss.
- **Producer send-and-forget pattern** — A developer uses `kafkaTemplate.send(topic, payload)` without checking the `CompletableFuture` return value, silently swallowing send failures. Mitigation: a custom `KafkaTemplate` wrapper that wraps `send()` and throws if the future completes exceptionally; enforced via ArchUnit rule that bans `kafkaTemplate.send(...)` calls that discard the return value.
- **Consumer auto-commit race** — `enable.auto.commit=true` (sometimes accidentally enabled by copying configuration from non-financial services) commits offsets before side-effects complete. Mitigation: ArchUnit rule bans `enable.auto.commit=true` in any configuration class annotated with `@KafkaConsumerConfig`; automated configuration scan in CI pipeline reports any broker-level auto-commit setting.
- **Outbox relay stall** — The CDC relay (Debezium) stalls due to a Kafka Connect worker crash. Outbox rows accumulate in PostgreSQL indefinitely; no events are published. Mitigation: `GD_Outbox_Relay_Lag` alert fires within 2 minutes; Debezium workers run in a 3-node Kafka Connect cluster (HA); the alert runbook includes steps to restart the connector and verify the backlog clears.
- **Replay without idempotency** — Guaranteed Delivery is at-least-once; without [EIP-024 Idempotent Receiver](idempotent-receiver.md) on every consumer, a redelivered payment event becomes a duplicate ledger entry. Mitigation: architecture review gate requires EIP-024 to be paired with EIP-023 on every financial channel consumer; ArchUnit rule asserts that any `@KafkaListener` on a financial-channel topic also references the `IdempotentReceiverService`.
- **MirrorMaker 2 replication lag during failover** — At the moment of a DR failover, the last 5 seconds of messages on the primary may not yet be replicated. These messages are "in-flight" between primary and DR. Mitigation: the outbox pattern ensures these messages exist in the source database; the upstream service re-publishes from the outbox on reconnection; idempotent consumers in DR absorb the duplicates. Effective message loss = 0.
- **Storage exhaustion causing message deletion before consumption** — If a consumer group falls behind by more than the retention window, Kafka begins deleting messages before the consumer reads them. Mitigation: `GD_Consumer_Lag_High` alert fires at 10,000 messages of lag (well within the retention window); automatic consumer scaling via KEDA; T0 30-day retention provides a wide window before deletion risk.

## Operational Runbook

1. **Alert: GD_Producer_Error_Rate** — A non-zero producer error rate on a T0 channel is a P1 incident. Producers receive `NotEnoughReplicasException` when ISR falls below `min.insync.replicas=2` (typically from a broker being down). Check broker health: `kubectl get pods -n kafka`. Identify the affected broker and investigate. While the broker is restarting, producers will retry (`retries=MAX_VALUE`) and eventually succeed when ISR recovers. Do not change `min.insync.replicas` to resolve the alert — fix the broker.

2. **Alert: GD_Consumer_Lag_High** — Consumer lag > 10,000 messages on a T0 channel. Check the consumer group: `kafka-consumer-groups.sh --describe --group <group>`. Identify the lagging partition. Check consumer pod health. If healthy but slow: scale pods (`kubectl scale deployment <consumer> --replicas=N`, max = partition count). If pods are crashing: check DLT for error pattern. If a single partition is hot: investigate partition key distribution.

3. **Alert: GD_Outbox_Relay_Lag** — The CDC relay is not publishing from the outbox. Check Debezium connector status: `GET /connectors/<connector>/status`. If `FAILED`, check the Kafka Connect worker logs. Common causes: Kafka Connect worker crash (restart the pod), PostgreSQL WAL slot advancing failure (check PostgreSQL replication slots), Kafka broker unavailable (check broker health). Restart the connector: `POST /connectors/<connector>/restart`.

4. **Broker failure recovery** — One of three brokers in the cluster fails. Kafka automatically elects new partition leaders from the remaining ISR. Producers experience brief `NotEnoughReplicasException` bursts (< 30 seconds for typical leader election). After the failed broker restarts and rejoins, it catches up via log replication. Verify: `kafka-topics.sh --describe --topics-with-overrides | grep UnderReplicated` returns empty within 5 minutes of broker restart.

5. **DR failover — switch to DR region** — Execute the DR runbook (REF-001). Kafka consumers in the DR region are already running against the MirrorMaker 2 replica topics. On failover, update the consumer group offsets to the last committed DR offset. Source services republish any outbox events that were in-flight at failover time. After failover, monitor `GD_Consumer_Lag_High` for the DR consumer groups; lag should normalise within 5 minutes. Run the idempotency validation test to confirm no duplicate side-effects.

6. **Verifying zero message loss after an incident** — After any incident that involved Kafka broker instability, run the message-continuity audit: query the outbox table for `status = 'PENDING'` rows older than 5 minutes (these were not relayed); check the DLT for messages with `kafka_dlt-original-offset` values in the incident window; cross-reference with the downstream side-effect target (ledger, T24) for gaps. Document findings in the incident postmortem per BP-010.

7. **Adding a new T0 financial channel** — New T0 channels must be provisioned via IaC (Terraform) with the mandated configuration (`acks=all`, RF=3, `min.insync.replicas=2`, `unclean.leader.election.enable=false`, 30-day retention). Submit a Channel Design Record (CDR). The CDR must explicitly confirm that EIP-023 (this document), EIP-024 (Idempotent Receiver), and EIP-025 (Dead Letter Channel) are applied on both the producer and consumer sides. Architecture review is mandatory before CDR approval for T0 channels.

8. **Testing message loss under broker failure** — Quarterly: execute the message-loss chaos test in the staging environment. Procedure: (a) start a producer sending 10,000 messages to a T0 topic; (b) kill the partition leader mid-send; (c) wait for leader election and producer retry; (d) count the messages received by the consumer; (e) verify count = 10,000 with no duplicates. This test provides ongoing evidence that the durability configuration is effective.

## Test Strategy

**Unit tests** — Verify `GuaranteedDeliveryProducerConfig` sets `acks=all`, `enable.idempotence=true`, `retries=Integer.MAX_VALUE`, and `enable.auto.commit=false` on the consumer factory. These are simple property-assertion tests that catch configuration regressions before they reach staging.

**Integration tests** — Testcontainers (3-node Kafka cluster configured with RF=3). Test: (a) publish 100 messages with `acks=all`; verify all 100 are consumed; (b) kill the partition leader mid-publish; verify all messages are eventually delivered after leader election (no loss); (c) kill the consumer pod after applying the side-effect but before committing the offset; restart the consumer; verify the message is redelivered and the [EIP-024](idempotent-receiver.md) deduplicate absorbs the duplicate.

**Outbox integration tests** — Testcontainers (PostgreSQL + Debezium + Kafka). Publish a payment event via the `PaymentCommandService` (writes to outbox in same transaction). Verify the event appears on the Kafka topic within 5 seconds. Kill the Debezium worker after the outbox write; restart it; verify the event still appears on the Kafka topic (relay-from-outbox on restart).

**Chaos tests** — Use Chaos Mesh or a Kubernetes network policy to: (a) drop 50% of packets between the producer and one Kafka broker; verify no message loss (producer retries succeed via the other brokers); (b) simultaneously kill two of three Kafka brokers; verify producers receive `NotEnoughReplicasException` (min ISR not met) and message publishing halts rather than data loss occurring; (c) pause the consumer for 2 minutes, then resume; verify all messages are consumed from the correct offset with no loss or duplication.

**Compliance tests** — Automated test asserts that every Kafka topic configuration in the production cluster matches the NFR YAML for its tier. Run as a daily CI job using `kafka-topics.sh --describe` output parsed against expected configuration. Alert on any drift (e.g., someone manually changed `min.insync.replicas` via console).

## References

- Hohpe, G. & Woolf, B. — Enterprise Integration Patterns (Addison-Wesley), Chapter 3: Guaranteed Delivery
- Apache Kafka documentation — Producer Configuration, Topic Configuration, Transactions
- Spring Kafka reference — `@RetryableTopic`, `DefaultErrorHandler`, `DeadLetterPublishingRecoverer`
- Debezium documentation — Outbox Event Router
- Related catalog IDs: [EIP-001 Message Channel](message-channel.md), [EIP-024 Idempotent Receiver](idempotent-receiver.md), [EIP-025 Dead Letter Channel](dead-letter-channel.md), [INT-002 Transactional Outbox + CDC](../integration/cdc-outbox-pattern.md), [REF-001 Multi-Region Active-Active](../../reference-architectures/multi-region-active-active.md), [SEC-001 mTLS](../../security/mtls.md)

---

**Key Takeaway**: Guaranteed Delivery in banking is a layered contract — outbox pattern atomises the event with the business write; `acks=all` + RF=3 ensures broker durability; manual offset commit ties processing confirmation to side-effect success; and the Idempotent Receiver (EIP-024) makes the resulting at-least-once delivery safe — together achieving zero message loss on T0 financial channels.
