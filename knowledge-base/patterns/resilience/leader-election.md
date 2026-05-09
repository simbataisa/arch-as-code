# Leader Election

Status: Draft | Last Reviewed: 2026-05-09 | Owner: @sre-lead
Catalog ID: RES-010 | Radii
Tier Applicability: T0, T1

## Problem Statement

Multi-instance deployments introduce split-brain and duplicate-execution hazards for operations that must run exactly once across the cluster:

- NAPAS requires a single active TCP session per bank connection; two payment-service instances simultaneously establishing sessions cause authentication conflicts and message-ordering failures on the NAPAS switch.
- The T24 end-of-day (EOD) batch window must be triggered by exactly one instance; duplicate triggers cause double-posting of accruals, interest calculations, and ledger reconciliation errors — a direct BCBS 239 data accuracy violation.
- Kafka consumer group rebalance coordination, if left to multiple competing instances, produces duplicate partition assignments during rolling restarts, leading to double-processing of payment events.
- Payment deduplication windows (idempotency locks) held by multiple instances simultaneously allow the same NAPAS credit transfer to be submitted twice, creating a duplicate payment liability.
- Kubernetes pod autoscaling and rolling deploys mean the number of active instances varies continuously; a hard-coded "instance 0 is leader" approach breaks during deploys and scale-out events.
- Without leader election observability, SREs cannot determine which instance is performing singleton operations at any given moment, making incident triage and audit log correlation slow and error-prone.

## Solution

Implement distributed leader election so exactly one instance at a time holds a renewable lease for each singleton operation; followers monitor the leader and automatically campaign to take over if the lease expires.

```mermaid
sequenceDiagram
    autonumber
    participant Pod1 as Pod-1 (Leader)
    participant Pod2 as Pod-2 (Follower)
    participant Pod3 as Pod-3 (Follower)
    participant Redis as Redis (Lease Store)
    participant NAPAS as NAPAS Gateway
    participant T24 as T24 EOD Scheduler

    Note over Pod1,Redis: Normal operation — Pod-1 holds lease
    Pod1->>Redis: SET napas-session-leader pod-1 PX 10000 NX
    Redis-->>Pod1: OK (lease acquired)
    Pod1->>NAPAS: Establish TCP session
    NAPAS-->>Pod1: Session active

    loop Every 3 s — lease renewal
        Pod1->>Redis: PEXPIRE napas-session-leader 10000
        Redis-->>Pod1: 1 (renewed)
    end

    Pod2->>Redis: SET napas-session-leader pod-2 PX 10000 NX
    Redis-->>Pod2: nil (lease held by pod-1; stand by)

    Note over Pod1,NAPAS: Pod-1 crashes / OOMKilled
    Pod1-xPod1: Pod-1 stops renewing

    Note over Pod2,Redis: Lease TTL expires (10 s)
    Pod2->>Redis: SET napas-session-leader pod-2 PX 10000 NX
    Redis-->>Pod2: OK (new leader)
    Pod2->>NAPAS: Re-establish TCP session
    NAPAS-->>Pod2: Session active

    Pod3->>Redis: SET napas-session-leader pod-3 PX 10000 NX
    Redis-->>Pod3: nil (pod-2 is now leader)

    Note over Pod2,T24: End-of-day trigger (leader only)
    Pod2->>T24: Trigger EOD batch window
    T24-->>Pod2: Batch accepted
```

## Implementation Guidelines

1. **Spring Integration `LeaderInitiator` with Redis** — use Spring Integration's leader election abstraction backed by a Redis `LockRegistry`. The `LeaderInitiator` manages campaign, grant, and revoke lifecycle events.

   ```java
   @Configuration
   @Slf4j
   public class LeaderElectionConfig {

       @Bean
       public RedisLockRegistry redisLockRegistry(RedisConnectionFactory connectionFactory) {
           // Lock key per singleton operation; TTL 15 s
           return new RedisLockRegistry(connectionFactory, "tcb-leader-election", 15_000L);
       }

       @Bean
       public LeaderInitiator napasSessionLeaderInitiator(
               RedisLockRegistry lockRegistry,
               NapasSessionLeaderCandidate candidate) {
           return new LeaderInitiator(lockRegistry, candidate, "napas-session-leader");
       }

       @Bean
       public LeaderInitiator eodBatchLeaderInitiator(
               RedisLockRegistry lockRegistry,
               EodBatchLeaderCandidate candidate) {
           return new LeaderInitiator(lockRegistry, candidate, "t24-eod-batch-leader");
       }
   }
   ```

2. **NAPAS session leader candidate** — implements `Candidate` to react to role grants and revocations; only the elected leader maintains the active NAPAS TCP session.

   ```java
   @Component
   @Slf4j
   public class NapasSessionLeaderCandidate implements Candidate {

       private final NapasSessionManager sessionManager;
       private final MeterRegistry meterRegistry;
       private volatile Context leaderContext;

       @Override
       public void onGranted(Context ctx) {
           this.leaderContext = ctx;
           log.info("correlationId=leader-election role=napas-session-leader event=GRANTED pod={}", podId());
           meterRegistry.counter("leader_election.role_granted_total", "role", "napas-session").increment();
           sessionManager.establishSession();
       }

       @Override
       public void onRevoked(Context ctx) {
           log.warn("correlationId=leader-election role=napas-session-leader event=REVOKED pod={}", podId());
           meterRegistry.counter("leader_election.role_revoked_total", "role", "napas-session").increment();
           sessionManager.closeSession();
       }

       @Override
       public String getRole() { return "napas-session-leader"; }

       private String podId() {
           return System.getenv().getOrDefault("POD_NAME", "unknown");
       }

       public boolean isLeader() {
           return leaderContext != null && leaderContext.isLeader();
       }
   }
   ```

3. **T24 EOD batch leader candidate** — guards the end-of-day trigger; a non-leader instance that receives an EOD schedule event skips execution and logs.

   ```java
   @Component
   @Slf4j
   public class EodBatchLeaderCandidate implements Candidate {

       private volatile boolean isLeader = false;

       @Override
       public void onGranted(Context ctx) {
           isLeader = true;
           log.info("correlationId=leader-election role=t24-eod-batch event=GRANTED pod={}", podId());
       }

       @Override
       public void onRevoked(Context ctx) {
           isLeader = false;
           log.warn("correlationId=leader-election role=t24-eod-batch event=REVOKED pod={}", podId());
       }

       @Override
       public String getRole() { return "t24-eod-batch-leader"; }

       public boolean isLeader() { return isLeader; }

       private String podId() {
           return System.getenv().getOrDefault("POD_NAME", "unknown");
       }
   }

   @Component
   @Slf4j
   public class EodBatchScheduler {

       private final EodBatchLeaderCandidate leaderCandidate;
       private final T24OfsClient t24Client;

       @Scheduled(cron = "0 55 23 * * *", zone = "Asia/Ho_Chi_Minh")
       public void triggerEodWindow() {
           if (!leaderCandidate.isLeader()) {
               log.debug("Not EOD leader; skipping trigger on pod={}", podName());
               return;
           }
           log.info("correlationId=eod-batch event=TRIGGER pod={}", podName());
           t24Client.triggerEodBatch();
       }
   }
   ```

4. **Kubernetes ConfigMap-based leader election** — for environments where Redis is unavailable or for Kubernetes-native tooling, use the `kubernetes-client` leader election with ConfigMap annotation locks (the same mechanism used by `kube-scheduler`).

   ```java
   @Configuration
   @ConditionalOnProperty("leader.election.backend", havingValue = "k8s")
   @Slf4j
   public class KubernetesLeaderElectionConfig {

       @Bean
       public LeaderElectionConfig k8sLeaderElectionConfig() {
           return new LeaderElectionConfigBuilder()
               .withName("napas-session-leader")
               .withNamespace(System.getenv("POD_NAMESPACE"))
               .withLeaseDuration(Duration.ofSeconds(15))
               .withRenewDeadline(Duration.ofSeconds(10))
               .withRetryPeriod(Duration.ofSeconds(3))
               .withLeaderCallbacks(new LeaderCallbacksBuilder()
                   .withOnStartedLeading(ctx -> log.info("K8s leader role granted"))
                   .withOnStoppedLeading(() -> log.warn("K8s leader role lost"))
                   .withOnNewLeader(newLeader -> log.info("New leader elected: {}", newLeader))
                   .build())
               .build();
       }
   }
   ```

5. **Distributed idempotency lock** — for payment deduplication, a short-lived distributed lock ensures only one instance processes the deduplication window for a given idempotency key.

   ```java
   @Service
   @Slf4j
   public class PaymentDeduplicationService {

       private final RedisLockRegistry lockRegistry;

       public PaymentResult processWithDedup(CreditTransferRequest request, String correlationId) {
           String lockKey = "dedup:" + request.getIdempotencyKey();
           Lock lock = lockRegistry.obtain(lockKey);
           try {
               if (!lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                   log.warn("correlationId={} Duplicate payment detected key={}", correlationId, request.getIdempotencyKey());
                   return PaymentResult.duplicate(request.getIdempotencyKey());
               }
               try {
                   return doProcessPayment(request, correlationId);
               } finally {
                   lock.unlock();
               }
           } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               throw new PaymentProcessingException("Lock interrupted", e);
           }
       }
   }
   ```

6. **YAML configuration and observability** — expose leader status via Spring Boot Actuator; emit structured metrics.

   ```yaml
   spring:
     integration:
       leader:
         heartbeat-time: 3000        # Renewal interval ms
         candidate-timeout: 10000    # Lease TTL ms

   management:
     endpoints:
       web:
         exposure:
           include: health,metrics,leader
     health:
       leader:
         enabled: true

   leader:
     election:
       backend: redis   # or k8s
       roles:
         - napas-session-leader
         - t24-eod-batch-leader
   ```

## When to Use / When NOT to Use

**Use when:**
- An operation must execute on exactly one instance at a time: maintaining a stateful external connection (NAPAS TCP), triggering a batch job, or holding a coordination lock.
- The cluster size is dynamic (Kubernetes autoscaling, rolling deploys) and a static "primary pod" designation is not reliable.
- Split-brain execution would cause data integrity problems (double EOD batch trigger, duplicate NAPAS session, duplicate payment).
- The operation's singleton constraint must survive pod failure with automatic failover within a bounded time window.

**Do NOT use when:**
- All instances can safely execute the operation concurrently (stateless REST handlers, idempotent Kafka consumers with partition assignment).
- The coordination overhead (Redis round-trip, lease renewal) exceeds the execution time of the protected operation.
- Extremely short-lived operations (sub-millisecond) — use a local synchronization primitive instead.
- The cluster runs a single instance (no contention to resolve) — add leader election when scaling to multiple instances, not before.

## Variants & Trade-offs

| Variant | When | Trade-off |
|---|---|---|
| **Redis SET NX + TTL** | Redis already in stack; low operational overhead | Redis single-node failure can cause leader election stall; use Redis Sentinel or Cluster for HA |
| **Zookeeper ephemeral node** | ZooKeeper already managed (Kafka cluster); strongest consistency guarantee | Higher operational complexity; Zookeeper itself needs HA; added latency |
| **Kubernetes ConfigMap lock** | Kubernetes-native deployment; want to avoid additional dependencies | Requires Kubernetes API access from pod; RBAC configuration needed; lease granularity is coarser |
| **Spring Integration `LeaderInitiator`** | Spring Boot service; want abstraction over the lock backend | Abstraction hides backend specifics; easier to swap from Redis to K8s without changing business logic |
| **Etcd distributed lock** | Service mesh with etcd available; very high reliability requirement | Etcd adds another dependency; overkill if Redis Sentinel is already HA |

## NFR Acceptance Criteria

```yaml
service_name: "payment-service-leader-election-compliance"
tier: T0
acceptance_criteria:
  - id: LE-1
    description: "Leader failover time"
    requirement: "After the current leader pod is terminated, a new leader must be elected and active within 20 seconds (lease TTL 15s + campaign time <= 5s); NAPAS session must be re-established within the same 20s window"
    measurement: "Chaos test: kubectl delete pod {leader}; measure time from pod termination to NapasSessionLeaderCandidate.onGranted on successor; alert if > 20s"
  - id: LE-2
    description: "No split-brain under partition"
    requirement: "Under a simulated Redis network partition (one Redis node unreachable), no two pods may simultaneously report isLeader()=true for the same role; verified by injecting partition and polling all pod /actuator/health endpoints"
    measurement: "Toxiproxy Redis partition for 30s; scrape leader status from all pods; assert at most one leader per role at any snapshot"
  - id: LE-3
    description: "EOD batch single-trigger guarantee"
    requirement: "The T24 EOD batch trigger must fire exactly once per scheduled window across a 3-pod deployment; zero duplicate triggers in 30 consecutive EOD cycles"
    measurement: "T24 audit log correlation: count EOD trigger events per day; alert if count != 1"
  - id: LE-4
    description: "Leader election observability"
    requirement: "Every leader role grant and revocation must emit a structured log entry (correlationId, role, pod name, event=GRANTED|REVOKED) and increment a labelled Prometheus counter; Grafana dashboard shows current leader pod per role"
    measurement: "Integration test: trigger leadership change; assert log entry and counter increment; verify Grafana panel data"
```

## Compliance Mapping

| Layer | Reference | Section/Control | How |
|---|---|---|---|
| Ring 0 | AWS Well-Architected Reliability — Single point of failure | REL 9: Deploy redundantly | Leader election enables active-passive redundancy for singleton operations without a single hard-coded primary |
| Ring 0 | Microsoft Cloud Patterns — Leader Election | "Coordinate distributed services by electing a leader" | Pattern definition; implementation follows Candidate/Context lifecycle |
| Ring 0 | Kubernetes documentation — Leader Election | ConfigMap lease-based election | K8s variant uses the same mechanism as control plane components |
| Ring 1 | BCBS 239 §6 Accuracy — Single authoritative processor | Data aggregation accuracy; no duplicate reporting | Leader election prevents double EOD batch triggers that would create duplicate accrual entries, violating the single-authoritative-processor principle |
| Ring 2 | SBV Circular 09/2020 §IV.2 ⚠️ (working summary — pending Legal review) | Payment system continuity and integrity | NAPAS session leader election ensures uninterrupted payment session continuity during pod failures; automatic failover meets continuity obligations |

## Cost / FinOps Notes

| Item | Driver | Order of magnitude |
|---|---|---|
| Redis lease operations | SET NX + PEXPIRE every 3 s per role per pod | ~60 Redis ops/min for 3 pods × 2 roles; negligible versus payment traffic |
| Lease TTL memory | One small string key per role | Bytes; effectively free |
| Kubernetes API (K8s variant) | ConfigMap PATCH every renewal period | ~20 API calls/min; within kube-apiserver rate limits |
| Spring Integration overhead | LeaderInitiator heartbeat thread | 1 thread per role; negligible on JVM with virtual threads (Java 21) |

**Cost of NOT having leader election**: a duplicate NAPAS session costs one authentication rejection and session teardown per deployment; a duplicate EOD batch trigger in T24 requires manual journal reversal — estimated 2–4 engineering hours per incident plus audit documentation.

## Threat Model Summary

STRIDE focus: **Spoofing** and **Tampering** via lock store manipulation.

- **Top 3 threats addressed:**
  1. *Duplicate singleton execution (split-brain)* — distributed lock with TTL ensures at most one leader holds the lock at any moment.
  2. *NAPAS session conflict* — only the elected leader establishes and renews the TCP session; followers listen but do not transmit.
  3. *EOD double-trigger* — leader check at `triggerEodWindow()` entry point prevents follower execution even if the scheduled method fires on all pods.
- **Top 3 residual threats:**
  1. *Redis lock store compromise* — an attacker who can write to Redis could forge a lock grant for any identity. Mitigation: Redis ACL restricts write access to payment-service service accounts only; mTLS between service and Redis; Vault-managed credentials.
  2. *GC pause causing false lease expiry* — a long JVM GC pause on the leader pod may prevent lease renewal, causing a new leader to be elected while the old leader is still running but paused. Mitigation: Java 21 ZGC (sub-millisecond pause); lease TTL set at 5× renewal interval to tolerate transient pauses.
  3. *Leader starvation* — if a pod repeatedly wins election but crashes before completing the singleton operation, followers cycle without progress. Mitigation: crash-loop detection via Kubernetes restartPolicy and pod disruption budget; alert on > 3 leadership changes in 5 min.

## Operational Runbook (stub)

- **Alerts:**
  - `LeaderElectionFlapping`: more than 3 leader role changes in 5 minutes for any role. Severity: Warning — investigate pod stability.
  - `LeaderAbsent`: no pod reports `isLeader()=true` for a role for > 30 s. Severity: Critical — PagerDuty; NAPAS session may be inactive.
  - `EodDuplicateTrigger`: T24 audit log shows > 1 EOD trigger in a calendar day. Severity: Critical — immediate data integrity review.
- **Dashboards:** Grafana — `leader-election-overview`: current leader pod per role, role grant/revoke events over time, lease renewal latency, Redis lock operation latency.
- **On-call playbook:**
  1. Check `leader_election.role_granted_total` and `leader_election.role_revoked_total` counters for flap frequency.
  2. Identify the current leader pod from `/actuator/health` or the Grafana dashboard.
  3. If `LeaderAbsent`: check Redis connectivity from all pods (`redis-cli ping`); restart the pod if it is stuck in campaign loop.
  4. If `EodDuplicateTrigger`: immediately freeze further T24 batch triggers via feature flag; engage T24 team for journal reversal; file data integrity incident.
  5. Document in `governance/decisions/REVIEW-LOG-{date}-incident.md`.

## Test Strategy (stub)

- **Unit:** Assert `onGranted` triggers `sessionManager.establishSession()`; assert `onRevoked` triggers `sessionManager.closeSession()`; assert non-leader `EodBatchScheduler.triggerEodWindow()` skips execution.
- **Integration:** Two Spring Boot Testcontainers instances sharing a Redis container; assert exactly one `onGranted` fires; kill the leader container; assert the follower becomes leader within 20 s.
- **Chaos (pod kill):** Deploy 3 replicas; `kubectl delete pod {leader}` repeatedly; assert NAPAS session is re-established within 20 s each time; assert zero duplicate EOD triggers over 10 cycles.
- **Split-brain test:** Use Toxiproxy to partition Redis; assert no two pods simultaneously report `isLeader()=true`; assert system recovers automatically when partition heals.
- **Performance:** Measure Redis SET NX latency under load; assert lease renewal P99 < 50 ms (well inside the 3 s renewal interval).

## Related Patterns

- [RES-002 Circuit Breaker](circuit-breaker.md) — pair with leader election for the NAPAS session client; CB protects the session from flapping on NAPAS errors
- [RES-007 Fallback Strategies](fallback-strategies.md) — if leader election cannot determine a leader within the lease window, a fallback policy (queue-and-hold) may be needed for in-flight payments
- [RES-012 Health Check Aggregation](health-check-aggregation.md) — include leader role status as a custom health indicator; a service that is a candidate but never wins election is degraded
- [PRIN-006 Idempotency-by-default](../../principles/idempotency-by-default.md) — idempotency keys are the application-layer complement to leader election; both prevent duplicate processing
- [NFR-001 Service Tiering + RTO/RPO](../../nfr/service-tiering-rto-rpo.md) — T0 RTO drives the 20 s failover requirement for NAPAS session leader
- [BP-005 Chaos Engineering](../../best-practices/chaos-engineering.md) — pod-kill chaos drills are the primary verification mechanism for leader election correctness

## References

- [Spring Integration Leader Election](https://docs.spring.io/spring-integration/reference/leader-election.html)
- [Redis Distributed Locks (Redlock)](https://redis.io/docs/manual/patterns/distributed-locks/)
- [Kubernetes Leader Election](https://kubernetes.io/blog/2016/01/simple-leader-election-with-kubernetes/)
- [Martin Kleppmann — Designing Data-Intensive Applications, Ch. 8](https://dataintensive.net/) — distributed systems guarantees and split-brain

---
**Key Takeaway**: Leader election ensures exactly one instance at a time manages the NAPAS TCP session and triggers the T24 EOD batch — automatic failover within 20 seconds prevents both service outages and data integrity violations.
