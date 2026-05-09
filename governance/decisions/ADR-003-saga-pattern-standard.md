# ADR-003: Adopt SAGA Pattern as Standard for Distributed Transactions

**Status:** Accepted

**Date:** 2026-01-15

**Authors:** Enterprise Architecture Technology Team, Payment & Settlements Domain

**Stakeholders:** Payments, Ledger, Settlements teams; Database architects; DevOps/SRE

---

## Context

Techcombank's transition to microservices architecture has created a critical technical challenge: **distributed transactions across multiple services and databases**. The legacy monolith used ACID transactions with a single database. Now:

- **Payment Service** writes to its database
- **Ledger Service** writes to its database
- **Settlement Service** writes to its database
- All three need to succeed atomically, or all must rollback

**Current State:**
- Some teams use **Two-Phase Commit (2PC)**, causing deadlocks and latency spikes (5+ minutes in worst case)
- Some teams use **ad-hoc distributed locking**, prone to race conditions
- Some teams use **eventual consistency** without compensating transactions, causing silent data corruption
- No standardized approach; each domain invents its own solution

**The Problem with 2PC:**
- Payment system waits for lock on all 3 services (Ledger, Settlement)
- If Ledger is slow (cascading failure from upstream), entire payment stalls
- 2PC coordinators become bottleneck; locks held for long periods
- Causes cascading timeouts across platform
- Network partition → coordinator unavailable → everything fails

**Business Requirement:**
- Payment must eventually settle (within 24 hours guaranteed)
- Inconsistent state is not acceptable (silent data loss unacceptable)
- But payment doesn't need to be ACID-immediate (eventual consistency acceptable if guaranteed)
- SLA: Payment notification < 2 seconds; ledger update < 5 minutes is acceptable

---

## Decision Drivers

1. **Scale** — 10K+ concurrent transactions/sec requires lock-free architecture
2. **Reliability** — Cascade failures from 2PC must be eliminated
3. **Latency** — Sub-second payment confirmation needed, not 5-minute locks
4. **Operational Simplicity** — Reduce on-call incidents from deadlock resolution
5. **Developer Velocity** — Standardized pattern reduces context-switching
6. **Data Integrity** — Guaranteed eventual consistency, no silent failures
7. **Regulatory** — Financial records must be reconcilable; 2PC is not mandatory

---

## Considered Options

### Option 1: Two-Phase Commit (2PC) Everywhere
**Approach:**
- Global transaction coordinator (e.g., XA protocol)
- All services lock resources
- Coordinator collects votes from all participants
- If all agree: Commit phase; else: Rollback phase

**Pros:**
- Immediate ACID consistency (strong guarantee)
- Familiar from monolithic databases
- Database-native support (SQL transactions)

**Cons:**
- **Blocking:** All services locked until commit completes
- **Latency:** High (wait for slowest participant)
- **Failure cascades:** If any participant unavailable → entire transaction fails
- **Deadlock risk:** Circular lock dependencies across services
- **Network partition vulnerability:** Coordinator unreachable → everything fails
- **Throughput bottleneck:** Coordinator becomes single point of contention
- **Operational complexity:** Resolving stuck transactions requires manual intervention

**Verdict:** Rejected for distributed system at Techcombank scale.

---

### Option 2: Eventual Consistency (Fire-and-Forget)
**Approach:**
- Service A writes to its DB and returns immediately
- Service A publishes event asynchronously
- Service B receives event, writes to its DB
- If Service B fails: Event queued indefinitely, no compensation

**Pros:**
- Fast (non-blocking, low latency)
- Scalable (no coordination overhead)
- Decoupled (services don't know about each other)

**Cons:**
- **Data inconsistency:** If Service B never processes event, data diverges
- **Silent failures:** No automatic rollback; operator must manually reconcile
- **Complexity:** Business logic must handle partial failures
- **Customer experience:** Payment confirmed but ledger not updated (confusion)
- **Auditing nightmare:** Tracing failures across services is hard
- **Regulatory risk:** Financial records not guaranteed to be consistent

**Verdict:** Rejected for payment system. Cannot accept silent inconsistency.

---

### Option 3: SAGA Pattern (Orchestration Model)
**Approach:**
- **SAGA Orchestrator** (Temporal, Step Functions, or custom) coordinates steps
- Service A writes to DB + publishes event
- Orchestrator waits for event, then requests Service B
- Service B writes to DB + publishes event
- Orchestrator waits for event, then requests Service C
- Service C writes to DB + publishes final event
- **If any step fails:** Orchestrator triggers compensating transactions in reverse order

**Pros:**
- **Eventual consistency with guarantees:** All steps eventually succeed or all compensate (no partial state)
- **Non-blocking:** Services don't lock each other; can process concurrently
- **Failure recovery:** Orchestrator retries failed steps automatically
- **Visibility:** Orchestrator logs entire saga (who did what, when, why)
- **Scalability:** No global lock; scales to 10K+ concurrent sagas
- **Testability:** Can mock services and test failure scenarios
- **Operational simplicity:** Orchestrator handles recovery; operators don't need to intervene

**Cons:**
- **Eventual consistency:** Not immediate ACID (accepts small window of inconsistency)
- **Complexity:** Must implement compensating transactions (e.g., "reverse payment")
- **New runtime:** Orchestrator itself is new component to operate
- **Learning curve:** Teams must understand saga semantics

**Verdict:** Selected. Best fit for distributed payment system.

---

### Option 4: Hybrid: SAGA for Cross-Domain, 2PC for Single-Domain
**Approach:**
- **Saga pattern** for cross-domain transactions (e.g., Payment → Ledger → Settlement)
- **2PC (Single DB Transaction)** for within-domain operations (e.g., multiple tables same DB)
- Example: Payment service writes to payment_requests + payment_history in same DB with 2PC; then saga for ledger update

**Pros:**
- **Best of both:** Strong ACID where possible (single DB), eventually consistent across domains
- **Reduces scope:** 2PC doesn't span services (avoids cascade failures)
- **Developer intuition:** SQL transactions for simple cases, saga for complex cases

**Cons:**
- **Dual approach:** Teams must know when to use which pattern
- **Transition complexity:** Moving from single DB to saga when requirements change

**Verdict:** Partial. Use as secondary pattern (see decision below).

---

## Decision

**Primary: SAGA Pattern for distributed transactions across services.**

**Secondary: 2PC allowed ONLY within single service/database (no cross-service 2PC).**

### Saga Model Selection: Orchestration (vs. Choreography)

**Orchestration** (recommended):
- Central SAGA Orchestrator coordinates steps
- Explicit state machine (steps, transitions, compensations)
- Easy to visualize and debug
- Single point of control (orchestrator)

**Choreography** (not recommended for Techcombank):
- Services publish events, other services subscribe
- No central coordinator
- More resilient (no single point of failure)
- Harder to debug (implicit dependencies)

**Rationale for Orchestration:** Techcombank scale and regulatory requirements demand explicit visibility and control. Choreography is more resilient but harder to trace for audit purposes.

### Saga Orchestration Framework

**Recommended:** Temporal (temporalio/temporal)
- Open source workflow orchestration
- Language-agnostic SDKs (Go, Java, Python, Node.js)
- Built-in retry, timeout, compensation
- Full execution history for audit
- Community: Netflix, Uber, others use Temporal

**Alternative:** AWS Step Functions (if AWS-native preferred)
- Managed service (no ops burden)
- JSON state machine definition
- Limited language support
- Lower cost at small scale; higher at large scale

### Saga Pattern: Standard Components

Every saga submission must define:

1. **Saga Steps:** Ordered sequence of service calls
   ```
   Step 1: Payment Service → Create Payment Record
   Step 2: Ledger Service → Debit Customer Account
   Step 3: Settlement Service → Submit to Clearing
   ```

2. **Success Path:** Happy path, all steps succeed
   ```
   All 3 steps complete → Return "SETTLED" status
   ```

3. **Compensating Transactions:** Reverse operations if step fails
   ```
   If Step 3 fails → Compensate Step 2 (reverse ledger debit)
   If Step 2 fails → Compensate Step 1 (reverse payment record)
   ```

4. **Retry Policy:** How many times + exponential backoff
   ```
   Max retries: 3
   Backoff: 1s, 2s, 4s
   Give up → Dead-letter queue for manual reconciliation
   ```

5. **Timeout:** How long to wait for each step
   ```
   Step timeout: 30 seconds
   Total saga timeout: 5 minutes
   Exceed → Trigger compensation
   ```

---

## Implementation

### Temporal SAGA Example (Go)

```go
// Define saga steps
func PaymentSaga(ctx workflow.Context, paymentRequest PaymentRequest) error {
    options := workflow.ActivityOptions{
        ScheduleToStartTimeout: time.Hour,
        StartToCloseTimeout:    time.Hour,
    }
    ctx = workflow.WithActivityOptions(ctx, options)

    var paymentID string
    var ledgerID string

    // Step 1: Create Payment
    err := workflow.ExecuteActivity(ctx, CreatePaymentActivity, paymentRequest).Get(ctx, &paymentID)
    if err != nil {
        return err
    }

    // Step 2: Debit Ledger
    err = workflow.ExecuteActivity(ctx, DebitLedgerActivity, ledgerRequest).Get(ctx, &ledgerID)
    if err != nil {
        // Compensate Step 1
        workflow.ExecuteActivity(ctx, ReversePaymentActivity, paymentID)
        return err
    }

    // Step 3: Submit for Settlement
    err = workflow.ExecuteActivity(ctx, SubmitSettlementActivity, settlementRequest).Get(ctx, nil)
    if err != nil {
        // Compensate Step 2, then Step 1
        workflow.ExecuteActivity(ctx, ReverseLedgerActivity, ledgerID)
        workflow.ExecuteActivity(ctx, ReversePaymentActivity, paymentID)
        return err
    }

    return nil
}

// Activity: Create Payment
func CreatePaymentActivity(ctx context.Context, req PaymentRequest) (string, error) {
    // Insert into payment service DB
    paymentID := uuid.New().String()
    db.CreatePayment(ctx, paymentID, req)
    return paymentID, nil
}

// Compensating activity: Reverse Payment
func ReversePaymentActivity(ctx context.Context, paymentID string) error {
    // Mark payment as REVERSED in DB
    db.UpdatePaymentStatus(ctx, paymentID, "REVERSED")
    return nil
}
```

### Timeout & Retry Configuration

```yaml
# Payment saga: 5-minute end-to-end SLA
payment_saga:
  total_timeout: 5m
  steps:
    - name: "create_payment"
      timeout: 30s
      retries: 3
      backoff: exponential(1s, 2s, 4s)
    - name: "debit_ledger"
      timeout: 30s
      retries: 5          # More retries for critical ledger
      backoff: exponential(1s, 2s, 4s, 8s, 16s)
    - name: "submit_settlement"
      timeout: 2m         # Settlement may take longer
      retries: 3
      backoff: exponential(2s, 4s, 8s)
  compensation:
    - target: "create_payment"
      activity: "reverse_payment"
      timeout: 30s
      retries: 3
```

---

## When to Use Each Pattern

### Use SAGA Pattern When:
- Transaction spans multiple services/databases
- Services may fail independently
- Eventual consistency acceptable (within hours)
- Long-running processes (minutes to days)
- Async compensation acceptable

**Example:** Payment → Ledger → Settlement (3 services)

### Use 2PC (Single DB Transaction) When:
- All changes in same database
- Different tables but same RDBMS
- True ACID consistency required
- Short operations (seconds)
- Sync compensation required

**Example:** Payment service: `INSERT payment_requests; UPDATE customer_balance` in PostgreSQL

### Use Fire-and-Forget (No Pattern) When:
- No need to guarantee consistency
- Failures are acceptable (e.g., analytics events)
- No compensation needed

**Example:** Write analytics event (fire-and-forget is OK)

---

## Consequences

### Positive

1. **Scale:** Supports 10K+/sec without bottlenecks
2. **Reliability:** Automatic compensation; no manual reconciliation
3. **Latency:** Saga steps execute in parallel where possible; total latency predictable
4. **Visibility:** Full execution history in Temporal (debuggable)
5. **Resilience:** Cascade failures isolated to single saga; don't affect others
6. **Developer Experience:** Clear saga definition; explicit steps and compensation
7. **Testing:** Mock activities; test failure scenarios (impossible with 2PC)
8. **Operational Simplicity:** Retries, timeouts, compensation automated

### Negative

1. **Eventual Consistency:** Not immediate ACID (inconsistency window acceptable but exists)
   - Mitigation: Customer sees "Pending" status during saga; final confirmation when complete
2. **Compensation Complexity:** Must implement reverse operation for each forward step
   - Mitigation: Compensation is typically just status updates; no actual reversal
3. **New Runtime:** Temporal adds operational complexity
   - Mitigation: Temporal is production-proven (Netflix, Uber scale)
4. **Data Visibility:** Intermediate states visible if queried during saga
   - Mitigation: Return provisional status to customer; final status when saga completes
5. **Learning Curve:** Teams must understand saga semantics
   - Mitigation: Training, templates, examples provided

### Risks & Mitigations

| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| **Saga orchestrator unavailable** | Low | High | HA setup; 3+ Temporal replicas |
| **Step always fails (poison pill)** | Low | Medium | Dead-letter queue; manual investigation |
| **Compensation fails** | Low | High | Compensate compensation; alerting |
| **Data inconsistency during saga** | Medium | Low | Customer sees "Pending" status; acceptable |
| **Long saga timeout leads to overdue payment** | Low | High | SLA: Max 5 minutes; reconciliation daily |

---

## Validation & Success Criteria

**Rollout Success (Months 1-2):**
- [ ] Temporal cluster deployed and operational
- [ ] 5+ cross-domain sagas implemented and tested
- [ ] Saga patterns documented with examples
- [ ] All teams trained on SAGA approach
- [ ] Zero unresolved sagas (100% eventual completion)

**Long-term Success (6 months):**
- [ ] 100% of cross-domain transactions using SAGA
- [ ] No 2PC deadlocks (eliminated)
- [ ] Payment latency p99 < 2 seconds (saga doesn't add latency)
- [ ] Saga visibility + observability high (Temporal dashboards)
- [ ] Zero silent data inconsistencies

**Metrics:**
- Saga success rate: Target > 99.9% (will complete eventually)
- Saga latency: p50 < 500ms, p99 < 5s
- Compensation rate: < 0.1% (sagas rarely fail)
- Manual reconciliation: < 1 per week (rarely needed)

---

## Migration Plan

### Phase 1: Pilot (Weeks 1-3)
- Deploy Temporal cluster (HA setup)
- Implement payment saga (Payment → Ledger only)
- Load test: 1K saga/sec
- Team training

### Phase 2: Expansion (Weeks 4-6)
- Full payment saga (Payment → Ledger → Settlement)
- Implement 5+ other cross-domain sagas
- Production migration (gradual traffic shift)

### Phase 3: Standardization (Weeks 7+)
- Convert remaining 2PC transactions to SAGA
- Retire any 2PC coordinator
- Operational runbooks, alerting

---

## Comparison: SAGA vs 2PC vs Fire-and-Forget

| Aspect | SAGA | 2PC | Fire & Forget |
|---|---|---|---|
| **Consistency** | Eventual | Immediate ACID | Eventual (best-effort) |
| **Latency** | Medium (~500ms) | High (locks) | Low (async) |
| **Scalability** | High (10K+/sec) | Low (bottleneck) | High (no coordination) |
| **Failure recovery** | Automatic | Manual | None |
| **Visibility** | Excellent (logs) | Good (DB logs) | None |
| **Cross-service** | Yes (ideal) | No (deadlock risk) | Yes (unreliable) |
| **Operational complexity** | Medium | Low | High |
| **Data integrity guarantee** | Strong (compensated) | Strongest (locked) | Weakest (best-effort) |

---

## Example Sagas at Techcombank

### Saga 1: Domestic Transfer
```
Step 1: Payment Service → Create Payment (PENDING)
Step 2: Ledger Service → Debit Source Account
Step 3: Ledger Service → Credit Dest Account
Step 4: Settlement Service → Settle in Clearing
Step 5: Notification Service → Send SMS receipt

Compensation (reverse order):
  - Cancel settlement
  - Reverse credit
  - Reverse debit
  - Cancel payment
```

### Saga 2: Loan Origination
```
Step 1: Credit Service → Check Credit Score
Step 2: Loan Service → Create Loan Record
Step 3: Funding Service → Fund to Customer Account
Step 4: Ledger Service → Record Loan Liability
Step 5: Notification Service → Send Approval Letter

Compensation:
  - Reverse Ledger entry
  - Reverse Funding
  - Cancel Loan Record
```

---

## Related Decisions

- **[DAB Process: Section 3 (Detailed Design)](../dab-process/dab-full-process.md#section-3-detailed-design):** All distributed transaction DABs must include saga pattern documentation

---

## References

- Temporal Documentation: https://docs.temporal.io/
- SAGA Pattern (Microservices Patterns): https://microservices.io/patterns/data/saga.html
- AWS Step Functions: https://docs.aws.amazon.com/step-functions/
- Compensation-Based Sagas: https://www.microsoft.com/en-us/research/publication/compensation-based-sagas/

---

## Sign-Off

**Enterprise Architecture Director:** _______________ **Date:** ___________

**Payments Domain Lead:** _______________ **Date:** ___________

**CTO Approval:** _______________ **Date:** ___________
