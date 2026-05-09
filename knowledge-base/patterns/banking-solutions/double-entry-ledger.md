# Double-Entry Ledger

Status: Proposed | Target Wave: 2 | Owner: @payments-domain-owner
Catalog ID: BSP-001
Tier Applicability: T0

> **STUB** — full content authored in Wave 2.
> Catalog: ../../governance/standards/enterprise-architecture-catalog.md

## Problem Statement

Every banking system rests on the double-entry-bookkeeping invariant: every value movement is recorded as a debit on one account and an equal credit on another, and the sum of debits equals the sum of credits at all times. Implementing this correctly under concurrency, retry, and distributed-transaction conditions is one of the highest-stakes engineering challenges in banking. Errors here are visible to regulators (BCBS 239), customers, and the press.

## Sketch of Solution

- Ledger entries are append-only; never updated in place (event-sourcing INT-004 friendly)
- Each posting: `(transaction_id, debit_account, credit_account, amount, currency, timestamp)`; `transaction_id` is the idempotency key (PRIN-006)
- Database constraint enforces sum-zero invariant at transaction commit
- Account balance = aggregation over postings (read-side; can be cached / materialised)
- Reversal: a new posting with negated amounts; never delete or update the original
- Multi-currency: postings store amounts in their native currency; balance reports compose with FX rates as a separate function
- Sharding by `customer_id` or `account_id` for horizontal scale; cross-shard postings via two-phase commit or saga (INT-001)

## Compliance Hooks

- Ring 0: Accounting fundamentals (double-entry bookkeeping; Pacioli 1494)
- Ring 1: IFRS 9; BCBS 239 §3 (timeliness), §6 (accuracy); ISO 20022 reconciliation
- Ring 2: Vietnam Accounting Standards (VAS); SBV Circular 09 §IV (UNOFFICIAL TRANSLATION pending Legal)

## NFR Hooks

- HA: T0 mandatory; sync-replicated ledger across regions (REF-001)
- HP: append-only writes can be batched; balance reads served from materialised views
- HR: append-only design enables full audit and forensic replay

## Authoring Checklist (DoD for moving Status → Approved)

- [ ] Mermaid ER diagram of postings, accounts, and balance views
- [ ] PostgreSQL DDL with constraint enforcing sum-zero
- [ ] Java sample with idempotent posting service
- [ ] T24 integration — handoff to T24 ledger via ACL (INT-005)
- [ ] Compliance Mapping (3 rings) — IFRS 9 explicit
- [ ] NFR Acceptance Criteria
- [ ] Cost/FinOps (storage growth; cold archival)
- [ ] Threat Model (insider tamper; reversal abuse; balance-view staleness)
- [ ] Operational Runbook (reconciliation; balance reset)
- [ ] Test Strategy (property-based tests for sum-zero; concurrent posting)

## References

- "Patterns for the Modern Bank Engineer" — typical references
- Square / Stripe / Modern Treasury ledger architectures (public talks)
- IFRS 9 Financial Instruments
- Catalog: PRIN-006 Idempotency; INT-001 Saga; INT-002 Outbox+CDC; INT-004 Event Sourcing; REF-001 Multi-Region
