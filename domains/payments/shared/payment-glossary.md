# Payment Domain Glossary

A comprehensive glossary of key terms, concepts, and acronyms used throughout the Payments domain at Techcombank.

---

## Core Payment Concepts

### Payment
A financial transaction transferring funds from a payer (debtor) account to a payee (creditor) account, typically initiated by the payer or an authorized third party.

### Payment Order
A formal request to execute a payment containing:
- Payer account and customer identification
- Payee account and identification
- Transaction amount in specified currency
- Instruction date and value date
- Optional: purpose, reference, special instructions

### Payment Status
The current state of a payment throughout its lifecycle:
- **Initiated** — Payment order received and validated
- **Routing** — Payment being sent to payment network
- **Processing** — Payment network is processing
- **Settled** — Payment completed and funds transferred
- **Failed** — Payment could not be completed
- **Reversed** — Payment was reversed after settlement

### Settlement
The final confirmation and posting of a payment in both the payer's and payee's accounts by the respective banks. Settlement typically occurs at T+0 (same-day) for NAPAS and T+1 (next-day) for some international transfers.

---

## Payment Networks and Standards

### NAPAS
National Payments Switch. Vietnam's national automated clearing house for domestic interbank payments and QR code transfers. NAPAS facilitates:
- Real-time domestic transfers (via 247 system)
- Batch transfers
- Card transactions
- VietQR payments

### SWIFT
Society for Worldwide Interbank Financial Telecommunication. International messaging standard for cross-border payments and financial transactions. SWIFT uses FIN messaging (e.g., MT103 for international transfers).

### VietQR
Vietnam QR code payment infrastructure operated by NAPAS. Enables customers to initiate payments by scanning QR codes using mobile banking apps.

### PSP
Payment Service Provider. Third-party companies providing payment processing services:
- **Stripe** — Card payment processor
- **PayPal** — Digital wallet and payment platform
- **Momo** — Mobile wallet in Vietnam

### SBV
State Bank of Vietnam. Vietnam's central bank; regulates banking operations and payment systems.

### SBV Circular 64
Regulatory guidance from SBV regarding electronic payment and payment transaction service provision (dated November 2013). Key requirements:
- Large transaction reporting (> VND 1 billion)
- AML/KYC compliance
- Payment fraud prevention
- Incident reporting

---

## Distributed Transaction Patterns

### Saga
A distributed transaction pattern coordinating multiple microservices to complete a business transaction. Two types:

**Orchestration Saga** (used in Payments):
- Central orchestrator (Temporal) coordinates all participants
- Orchestrator sends commands to each service
- Example: Payment Saga orchestrates debit → fraud check → credit → fee calculation → notification

**Choreography Saga**:
- Each service publishes events; other services subscribe
- No central orchestrator
- Complex to debug and scale

### Compensation
In a saga, if a transaction fails, compensation logic undoes previous steps. Example:
- Debit account → Fraud check fails → Compensation: reverse debit
- Used to maintain consistency across distributed systems

### Idempotency Key
A unique identifier (typically UUID) attached to a payment request ensuring the same payment is not processed twice if the request is retried. Example:
- First request with key `abc-123` → debits account, returns confirmation
- Retry with same key `abc-123` → returns same confirmation without re-debiting

Essential for reliability in distributed systems where network failures can cause retries.

### 2PC (Two-Phase Commit)
Distributed transaction protocol where a coordinator asks all participants to prepare (phase 1) and then commit (phase 2). **Not used in Payments** because:
- Requires distributed locks (poor performance)
- Blocking (reduces throughput)
- Sensitive to failure timing
- Saga pattern is preferred for distributed payments

---

## Reconciliation and Matching

### Reconciliation
Process of matching payment records from multiple sources to ensure accuracy and completeness:
- **Network Reconciliation** — Match payments sent vs. confirmations received from NAPAS/SWIFT
- **GL Reconciliation** — Match payment postings with General Ledger entries
- **Break** — A payment in one system but not the other (e.g., payment sent to NAPAS but not confirmed)

### Break Resolution
Process of investigating and resolving reconciliation breaks:
- **Duplicate** — Same payment processed twice (catch with idempotency)
- **In-flight** — Payment still in process (wait for next day)
- **Lost Payment** — Payment sent but not delivered (escalate and reverse)
- **Late Settlement** — Payment will settle but delayed (monitor and wait)

### Outbox Pattern
Ensures reliable event publishing when data and events must be atomically written:
1. Write payment record and event to outbox table in same transaction
2. Separate process reads outbox and publishes events to Kafka
3. If publishing fails, retry until successful
4. Prevents lost events and ensures exactly-once semantics

Example table:
```sql
CREATE TABLE payments_outbox (
  id UUID PRIMARY KEY,
  aggregate_id UUID,
  event_type VARCHAR,
  event_data JSONB,
  created_at TIMESTAMP,
  published_at TIMESTAMP
)
```

---

## Risk and Compliance

### Fraud Screening
Real-time verification process checking if a payment is fraudulent:
- **Rule-based** — Check against predefined rules (e.g., amount > limit)
- **Behavioral** — Check against customer's historical patterns
- **Network-based** — Check payee account history for fraud signals
- **Machine Learning** — ML model scoring transaction risk (0-100)

### Risk Score
Numerical rating (0-100) indicating payment fraud risk:
- 0-20: Low risk (approve)
- 20-50: Medium risk (flag for review)
- 50-80: High risk (likely fraudulent)
- 80-100: Very high risk (block automatically)

### AML/KYC
- **AML (Anti-Money Laundering)** — Compliance to prevent money laundering and terrorism financing
- **KYC (Know Your Customer)** — Customer verification and screening process

Payment system must verify customer identity, check against blacklists, and monitor for suspicious transaction patterns.

### Transaction Limit
Maximum amount a customer can transfer in a single transaction or time period:
- Retail customers: VND 50 million/transaction, VND 200 million/day
- Corporate customers: VND 5 billion/transaction (configurable)
- Determined by customer risk profile, account type, and regulatory requirements

---

## Technical Patterns

### CDC (Change Data Capture)
Technique detecting and capturing data changes in a database:
- **Query-based** — Periodically query database for changed records
- **Log-based** — Read database transaction logs (e.g., PostgreSQL WAL)
- **Trigger-based** — Use database triggers to capture changes

Used in Payments for:
- Publishing payment events when records change
- Feeding data warehouse with payment data
- Audit trail generation

### Event Sourcing
Architectural pattern storing all changes to application state as immutable events:
- Instead of storing current balance, store all debit/credit events
- Current state calculated by replaying events
- Perfect for payments: create audit trail and enable debugging

Example:
```
Payment(account_id=123, amount=1000000)
├─ PaymentInitiatedEvent(2026-03-08T10:00:00Z, 1000000)
├─ PaymentValidatedEvent(2026-03-08T10:00:01Z)
├─ FraudCheckPassedEvent(2026-03-08T10:00:02Z)
├─ PaymentRoutedEvent(2026-03-08T10:00:03Z, NAPAS)
├─ SettledEvent(2026-03-08T10:00:05Z)
└─ NotificationSentEvent(2026-03-08T10:00:06Z)
```

### Webhook
HTTP callback mechanism for real-time notifications:
- When payment is settled, Payments system calls external webhook (e.g., `POST https://partner.io/payments/settled`)
- Partner system receives real-time update without polling
- Enables integration with third-party systems

---

## Banking Standards

### ISO 20022
International standard for financial data messaging. Replaces older formats (SWIFT FIN, ACH):
- **XML-based** — Structured data
- **Richer** — More detailed transaction information
- **Standardized** — Same format globally for payments

Example ISO 20022 message for domestic transfer:
```xml
<CstmrCdtTrfInitn>
  <GrpHdr>
    <MsgId>20260308001</MsgId>
    <CreDtTm>2026-03-08T10:30:00</CreDtTm>
  </GrpHdr>
  <PmtInf>
    <PmtInfId>001</PmtInfId>
    <Dbtr>
      <Nm>John Doe</Nm>
      <Id>9704......</Id>
    </Dbtr>
    <CdtTrfTxInf>
      <Amt>1000000</Amt>
      <Cdtr>
        <Nm>Jane Smith</Nm>
        <Id>9703......</Id>
      </Cdtr>
    </CdtTrfTxInf>
  </PmtInf>
</CstmrCdtTrfInitn>
```

### SWIFT FIN
Format for international banking messages:
- **MT103** — Single customer credit transfer (international)
- **MT202** — General financial institution transfer
- Older format; being replaced by ISO 20022

### ACH (Automated Clearing House)
Batch payment processing system used in some countries for lower-value transfers. Not primary in Vietnam (NAPAS is primary).

---

## Performance and Reliability

### Latency
Time required to complete a payment operation:
- **P50** — Median latency (50th percentile)
- **P95** — 95th percentile (95 out of 100 requests)
- **P99** — 99th percentile (very fast requests only)

Techcombank SLAs:
- Payment validation: < 500ms (P95)
- Fraud screening: < 100ms (P99)
- Full processing: < 2 seconds (P99)

### Throughput
Number of payments processed per unit time:
- Domestic transfers: 100K tx/minute
- International transfers: 10K tx/minute
- Total payment events (including notifications): 500K/minute

### High Availability
System availability targets:
- **99.95%** — Payment core systems (maximum 2 hours downtime/month)
- **99.90%** — External networks (NAPAS, SWIFT)
- **99.99%** — Payment processing (RTO: 5 minutes, RPO: 0)

---

## See Also

- [Payments Domain Model](../domain-model.md)
- [Context Map](../context-map.md)
- [Error Codes](./payment-error-codes.md)
- [Payment Flow Template](./diagrams/payment-flow-template.md)

---

Last Updated: March 8, 2026 | Domain: Payments
