# Dead Letter Channel

Status: Draft | Last Reviewed: 2026-05-09 | Owner: @tech-lead-backend
Catalog ID: EIP-025 | Radii
Tier Applicability: T0, T1

## Problem Statement

Some messages cannot be processed: malformed payload, schema mismatch, business-rule rejection, downstream that returns a permanent (4xx) error. Without a Dead Letter Channel, these messages either block the partition (head-of-line blocking) until manual intervention, or are silently skipped (data loss, regulatory exposure). A Dead Letter Channel is a separate destination for un-processable messages — they accumulate there, observability surfaces the build-up, and an explicit triage / replay process recovers them.

## Context

Reach for this pattern when:

- Building any production Kafka / RabbitMQ / Solace consumer.
- Designing a saga step that may legitimately reject ([INT-001](../integration/saga-orchestration.md)).
- Operating EOD batch flows where one bad record must not stop the rest.
- Debugging "lost message" tickets — first check is "is it in DLT?".

## Solution

```mermaid
graph LR
    Producer --> Topic[Main Topic<br/>payment-events]
    Topic --> Consumer[Consumer<br/>ledger-poster]
    Consumer -->|"transient failure<br/>(retry up to N)"| Backoff[Backoff Retry<br/>(in-process)]
    Backoff --> Consumer
    Consumer -->|"permanent failure<br/>or N exhausted"| DLT[Dead Letter Topic<br/>payment-events-dlt]
    DLT --> Triage[Triage UI / Dashboard]
    Triage --> Replay[Replay back to main]
    Triage --> Reject[Mark rejected<br/>+ business workflow]
```

### Categorisation

| Class | Definition | Routing |
| --- | --- | --- |
| **Transient** | Network blip, downstream 5xx, timeout | In-process retry with exponential backoff (RES-003) |
| **Permanent** | Schema violation, business rejection, 4xx auth/perm | Direct to DLT, no retry |
| **Poison** | Crashes the consumer (bad payload, uncaught exception) | Caught at handler boundary; routed to DLT |

The consumer must classify each error and route accordingly. Misclassifying a permanent failure as transient causes infinite retry; misclassifying transient as permanent causes false DLT entries.

### DLT lifecycle

```
new → triaged → replayed (back to main) | rejected (closed) | quarantined (manual)
```

Triage SLA per tier:
- T0: 4 hours
- T1: 1 business day
- T2: 1 week

## Implementation Guidelines

### Spring Kafka — DefaultErrorHandler with DLT

```java
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<?, ?> template) {
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(record.topic() + "-dlt", record.partition()));

        ExponentialBackOff backoff = new ExponentialBackOff(1000L, 2.0);
        backoff.setMaxInterval(60_000L);
        backoff.setMaxElapsedTime(5 * 60_000L);  // give up after 5 min cumulative

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backoff);

        // permanent failures — no retry, straight to DLT
        handler.addNotRetryableExceptions(
            BusinessRejectionException.class,
            SchemaValidationException.class,
            org.springframework.security.access.AccessDeniedException.class
        );
        return handler;
    }
}
```

### DLT topic conventions

| Convention | Value |
| --- | --- |
| Naming | `<source-topic>-dlt` (e.g., `payment-events-dlt`) |
| Partitioning | Same partition count as source (preserves key locality) |
| Retention | ≥ 7 days; ≥ 30 days for T0 financial-relevant |
| Replication | Same as source |
| Headers | Stamp `kafka_dlt-original-topic`, `kafka_dlt-exception-class`, `kafka_dlt-exception-message`, `kafka_dlt-original-offset`, `kafka_dlt-original-timestamp` |

Spring Kafka's `DeadLetterPublishingRecoverer` adds these headers automatically.

### Triage UI

A minimal triage UI lists DLT entries with the original payload, the exception, and three actions per entry:

- **Replay** — re-publish to source topic (idempotent receiver protects against duplicates)
- **Reject** — close the entry; emit business event (e.g., "payment rejected" notification)
- **Quarantine** — mark for manual investigation

This is typically a small internal tool (Grafana panel + linked button, or a dedicated React app).

### T24 / legacy

T24 OFS calls that fail with permanent errors (account closed, insufficient funds beyond limit) should not be retried; they go to DLT for business-team triage. Transient T24 failures (timeout, broker hiccup) follow the in-process retry path.

## Variants & Trade-offs

| Variant | When | Trade-off |
| --- | --- | --- |
| **Per-topic DLT** (default) | Standard | Simple; one DLT per source |
| **Shared DLT** | Many low-volume topics | Easier ops; harder to filter by source |
| **In-database DLT** | Compliance requires a transactional record | Loses Kafka tooling; richer query |
| **Multi-attempt DLT chain** | Different retry policies | DLT-1 → wait → DLT-2 → permanent reject |

## NFR Acceptance Criteria

- **HA**: DLT topic is HA per the source topic's tier. T0 → cross-region replication via MirrorMaker 2.
- **HP**: DLT publish adds < 5ms P95 (one extra Kafka publish on failure path; not on success path).
- **HR**: prevents head-of-line blocking; explicit triage SLA per tier; no silent message loss.

## Compliance Mapping

| Layer | Reference | Section/Control | How this satisfies |
| --- | --- | --- | --- |
| Ring 0 | EIP §10.5 (Hohpe/Woolf) | Messaging Channels — Dead Letter Channel | Canonical pattern |
| Ring 0 | Microsoft Cloud Patterns — Publisher-Subscriber | Reliable async messaging | Underlying reliability assumption |
| Ring 1 | Basel BCBS 239 — Principle 6 (Accuracy) | "All material risk exposures must be captured" | DLT prevents silent message loss → accuracy preserved |
| Ring 2 | SBV Circular 09/2020 §IV.3 (UNOFFICIAL TRANSLATION pending Legal) | Incident logging requirement | DLT entries are observable incident records |

## Cost / FinOps Notes

| Item | Driver | Order of magnitude |
|---|---|---|
| DLT topic storage | Failure rate × retention | At 0.1% failure / 30d retention on a 10M/day topic: ~3 GB |
| Triage labour | Failure rate × human time per entry | ~5 min / entry; budget 0.1 FTE per high-volume topic |
| Cross-region replication | DLT × egress | Same as source topic; usually negligible |

**Cost of NOT having DLT**: head-of-line blocking on a single bad record halts ALL processing on that partition. Outage duration = mean-time-to-incident-detection (often hours) + mean-time-to-manual-skip. Far higher than the DLT infrastructure cost.

## Threat Model Summary

STRIDE: primarily **Denial of Service** (head-of-line blocking) and **Repudiation** (silent message loss).

- **Top 3 threats addressed**:
  1. *Poison-pill DoS* — single malformed message blocking a partition. DLT routes it out.
  2. *Silent message loss* — DLT makes lost messages observable.
  3. *Cascade from one bad upstream* — one downstream's outage doesn't stop other consumers if errors route to DLT after retry.
- **Top 3 residual threats**:
  1. *DLT itself overflows* — alert on DLT depth growth; auto-pause source topic if DLT > N% of source rate.
  2. *Replay loops* — if a DLT'd message is replayed but still fails the same way, it lands in DLT again. Mitigation: add `kafka_dlt-attempt-count` header; after 3 attempts mark for manual quarantine.
  3. *PII in DLT entries* — DLT entries may contain customer data; retention windows must match data-protection rules per [PRIN-007 Data Residency](../../principles/data-residency.md).

## Operational Runbook (stub)

- **Alerts**:
  - `DLT_Depth_T0`: T0 source topic's DLT > 0.1% of source rate over 5 min. Severity: High.
  - `DLT_Depth_Sustained`: any DLT > 100 entries unprocessed for 4 hours. Severity: Warning escalating to High.
  - `DLT_Triage_SLA_Breach`: oldest DLT entry > tier SLA. Severity: tier-dependent.
- **Dashboards**: Grafana `dlt-overview` per topic (depth, ingest rate, oldest entry age, exception-class breakdown).
- **Triage procedure**:
  1. Open triage UI for the DLT.
  2. Pick first entry; inspect exception-class header.
  3. Decide: replay (transient), reject (business-final), quarantine (needs investigation).
  4. Document any new exception classes that should join `addNotRetryableExceptions(...)`.

## Test Strategy (stub)

- **Unit**: error-handler config — verify retry vs not-retry classification per exception type.
- **Integration**: Testcontainer Kafka, send a malformed message, verify it lands in DLT with correct headers; send a transient-fail message, verify retry then success.
- **Chaos**: inject transient downstream failures at varying rates; verify DLT only catches messages that exceed retry budget.
- **Replay test**: pull from DLT, replay to source, verify idempotent receiver de-dupes correctly.

## When to Use

- **Mandatory** for every production Kafka / RabbitMQ / Solace consumer.
- Even on T2 services — silent message loss is rarely acceptable in banking.

## When NOT to Use

- Telemetry pipelines where a small loss rate is genuinely acceptable (T3) — but explicit business sign-off required.

## Related Patterns

- [EIP-024 Idempotent Receiver](idempotent-receiver.md) — partner pattern; DLT replay relies on receiver idempotency
- [EIP-023 Guaranteed Delivery](guaranteed-delivery.md) — DLT preserves the delivery guarantee
- [RES-003 Retry with Backoff](../resilience/retry-with-backoff.md) — in-process retry layer before DLT
- [BP-010 Incident Postmortem](../../best-practices/incident-postmortem.md) — DLT spike often triggers postmortem

## References

- Hohpe, G. & Woolf, B. — EIP §10 (Messaging Channels — Dead Letter Channel)
- Spring Kafka `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`
- Confluent — Error Handling and Dead Letter Queues whitepaper

---

**Key Takeaway**: Every consumer has a DLT. Classify errors (transient → retry; permanent → DLT). Stamp DLT entries with original-topic / exception headers. Define triage SLA per tier. Replay is safe because EIP-024 deduplicates.
