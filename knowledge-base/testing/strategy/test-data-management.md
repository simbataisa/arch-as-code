# Test Data Management

Status: Approved | Last Reviewed: 2026-08-13 | Owner: @qe-lead
Catalog ID: TST-004 | Radii
Tier Applicability: T0, T1, T2, T3

## Problem Statement

- Production extracts copied into lower environments to avoid the effort of building synthetic
  data create direct regulatory exposure — a regulator does not distinguish who touched a real
  record or why; the disclosure exists the moment the record is readable outside its authorised
  system.
- Masking a production extract reduces but does not eliminate re-identification risk: the
  masked rows still carry the original population's cardinality, transaction pattern, and rare
  value combinations, and a rare combination of otherwise-masked attributes can re-identify a
  customer even after names and account numbers are removed.
- Undersized synthetic datasets make a load test pass for the wrong reason — a table holding a
  few hundred rows produces an unrealistically high cache-hit rate that has nothing to do with
  production's index selectivity, so the test reports a pass that production's real data shape
  would not earn.
- Data generated without a recorded seed makes a failure unreproducible: a run fails once, and
  by the time anyone investigates, the exact dataset that produced the failure no longer exists
  in any recoverable form, so the defect report has nothing to attach.
- Residue left behind by one run silently shifts the next run's baseline — a queue depth, a
  cache population, or an accumulated row count carried over from yesterday's run becomes
  today's starting condition, and a regression introduced by today's code is mistaken for normal
  run-to-run variance.

## Prohibitions

The rules below are absolute. They apply to every environment this corpus governs and every
archetype family in the catalog, regardless of tier, delivery deadline, or tooling limitation.
This section has no waiver process — a request to set aside a rule below is itself a defect to
raise with @qe-lead and @ciso-delegate, not a decision a squad makes on its own.

- Production data — including masked or partially masked production extracts — must not be used
  in any test environment covered by this corpus. Masking reduces risk; it does not convert
  production data into synthetic data, and a masked extract remains production data for the
  purpose of this rule.
- Real customer PII or PHI must never appear in a committed fixture, code snippet, log sample,
  screenshot, or documentation example anywhere in this repository — no real names, no real
  dates of birth, no real national ID numbers, no real member IDs, no real account-holder
  details. This holds even for a "bad example" or a negative-test illustration: a
  realistic-looking value is exactly as disclosive captioned "this is wrong" as it is captioned
  "this is right."
- Card PANs must never be real. Every PAN appearing in test material must come from a designated
  test BIN range (see [Synthetic Generation Strategy](#synthetic-generation-strategy)); a PAN
  that would pass Luhn validation against a live production BIN must not be used, even when it
  was generated synthetically and even when no live transaction was ever attempted against it.
- Account numbers, CIF identifiers, and customer references must be synthetic and visibly marked
  as such — for example `CIF-TEST-00001` or `ACCT-SYN-4821` — never a plausible-looking value
  indistinguishable from a real one.
- A fixture, snippet, or example that violates any rule above is prohibited from merge on sight,
  regardless of author intent, review time pressure, or how close it looks to the real thing;
  "it's just a placeholder" is not a defense a reviewer may accept.

Cross-references: [SEC-008 Data Masking](../../patterns/security/data-masking.md) owns masking
technique for the rare case a Confidential-tier value must be displayed at all; [SEC-013 PII
Tokenization (Format-Preserving)](../../patterns/security/pii-tokenization-format-preserving.md)
owns format-preserving tokenisation for PAN-shaped values used downstream of ingestion;
[`governance/standards/data-classification.md`](../../../governance/standards/data-classification.md)
owns the four sensitivity tiers this document assumes and never restates.

## Synthetic Generation Strategy

No single generation technique fits every archetype family; the right technique follows from
what the archetype is checking, not from generator convenience.

- **Rule-based generation** derives records directly from the domain model's own constraints —
  field types, enumerations, check constraints, and business rules encoded in the schema or the
  pattern documents. Use it when correctness depends on structural validity: a functional
  archetype needs a record that is a valid instance of the domain, nothing more.
- **Distribution-matched generation** reproduces a target statistical shape — value frequency,
  skew, and cardinality — rather than merely valid structure. Use it whenever the thing under
  test is sensitive to *how often* a value recurs, not just whether it is well-formed; see
  [Volume and Cardinality](#volume-and-cardinality) for why this is mandatory for performance
  archetypes.
- **Graph-consistent generation** builds an entire connected entity graph in one pass so that
  every foreign key resolves before any record is written. Use it whenever an archetype's
  correctness depends on a chain of references staying intact; see
  [Referential Integrity](#referential-integrity).

Whichever technique is used, the generated dataset must have three properties:

1. **Deterministic given a seed** — the same seed and the same generator version produce
   byte-for-byte the same dataset, every time, on every machine.
2. **Reproducible across environments** — a dataset generated on a laptop and a dataset
   generated in a CI runner, from the same seed, are the same dataset; no environment-specific
   randomness source (wall-clock time, process ID, unseeded `random()`) may leak into
   generation.
3. **Volume-scalable without regeneration from scratch** — moving from a `baseline` profile's
   small dataset to a `soak` profile's full-volume dataset extends the existing dataset under
   the same seed rather than replacing it with an unrelated one, so that referential integrity
   and cardinality shape survive the scale-up.

Card PANs are generated rule-based, from a designated test BIN range only, expressed with the
trailing digits masked so that no example in this corpus is ever a complete, copyable value: the
Visa test range is written `4111-11xx-xxxx-xxxx` and the Mastercard test range is written
`5105-10xx-xxxx-xxxx`. A generator implementation fills the masked positions at run time from
its own seed; this document never states the filled-in value, because a fully rendered example
number is exactly the kind of realistic-looking artefact the [Prohibitions](#prohibitions)
section exists to keep out of committed material.

## Referential Integrity

Several archetype families depend on a chain of foreign keys staying intact across five entity
types: a customer owns one or more accounts, each account accumulates ledger entries, each
ledger entry is produced by a transaction, and some transactions carry a settlement instruction
that clears the position externally.

```mermaid
graph LR
    Cust["Customer"] --> Acct["Account"]
    Acct --> Ledger["Ledger Entry"]
    Ledger --> Txn["Transaction"]
    Txn --> Settle["Settlement Instruction"]
```

A broken reference anywhere in this chain — an orphaned ledger entry with no owning account, a
transaction with no ledger entry, a settlement instruction referencing a transaction that was
never generated — does not make a dependent test result merely wrong; it makes the result
**meaningless**. [TST-021 Ledger and Monetary Invariant](../archetypes/ledger-monetary-invariant.md)
asserts an invariant across the whole chain (for example, that every ledger entry sums to zero
against its account); an orphaned row is invisible to that join and is silently excluded from
the population the invariant is checked over, so the invariant appears to hold even though the
very rows most likely to expose a defect were never evaluated. The same failure mode applies to
[TST-039 Data Quality and Reconciliation](../archetypes/data-quality-reconciliation.md): a
reconciliation check compares two sides of a chain that must resolve completely to be a
reconciliation at all, and a dangling reference on either side produces a clean-looking match
that reconciles nothing.

Graph-consistent generation is the only technique that guarantees this chain resolves end to
end; rule-based generation of each entity type in isolation is not sufficient, because isolated
generation has no mechanism to prevent a foreign key from pointing at a row that generation
never produced.

## Volume and Cardinality

Row count is not the number that matters. Latency in a production-shaped system is driven by
index selectivity and cache hit rate, both of which are properties of *cardinality and skew* —
how many distinct values a column holds and how unevenly traffic is distributed across
them — not by how many rows a table holds in total. A performance dataset must match
production's cardinality and skew within the ratio declared in
[TST-005](./environments-quality-gates.md), not merely match or approximate production's row
count.

**Failure mode:** a uniformly distributed synthetic dataset — every customer with the same
number of accounts, every account with the same transaction frequency, no hot keys — produces an
unrealistically high cache hit rate, because a uniform distribution has no long tail competing
for cache space. The resulting load test passes cleanly and predicts nothing about production,
where a small number of high-activity accounts dominate cache pressure and index lookups. A
dataset sized to the right row count but generated with the wrong distribution is not a smaller,
conservative version of the real answer; like the closed-model workload failure described in
[TST-003](./workload-modelling.md), it is a different, meaningless number that happens to look
like one.

## Seeding and Reproducibility

Every generated dataset is produced from a recorded seed, and the seed is captured in the run's
evidence artifact alongside the generator version that consumed it — not left in a throwaway
console log. The same seed and the same generator version reproduce the same dataset on any
machine; this is what makes a defect reproducible after the environment that first produced it
has been recycled. A defect report against a data-dependent failure always cites the seed it was
observed under, and a triage session that cannot locate the seed treats the failure as
unconfirmed until it can be regenerated deterministically.

## Teardown and Reset

Every archetype that writes data carries a teardown obligation: the environment a run leaves
behind must be indistinguishable, for testing purposes, from the environment it started in. No
run may leave residue that changes a later run's baseline — a queue depth that never drained, a
cache population that never expired, or an accumulated row count from a prior run all shift the
starting condition the next run measures against, turning a genuine regression into what looks
like normal variance, or the reverse.

Reset is verified, not assumed, against concrete signals:

- **Row counts** for every table the archetype writes to return to their pre-run values (or to a
  documented, intentional delta).
- **Queue depths** return to their pre-run baseline, with no messages left in flight.
- **DLQ depth** returns to its pre-run value; a DLQ that grew during the run and was not
  explicitly asserted on by the archetype is a teardown failure, not a passing run.
- **Cache state** — size and hit-rate counters — returns to baseline, or is explicitly flushed
  before the next run begins.

A run whose teardown cannot be verified against these four signals is not considered complete,
and its dataset must not be reused as the starting point for the next run.

## Data for Each Discipline

Each of the six disciplines in [TST-001](./test-strategy-standard.md) needs data shaped for what
it verifies; using the same generic dataset for every discipline is why coverage gaps go
unnoticed.

| Discipline | Data it needs |
|---|---|
| Functional | Boundary values and deliberately negative cases exercising invalid or edge-of-range input. |
| Performance | Production-matched volume, cardinality, and skew — see [Volume and Cardinality](#volume-and-cardinality). |
| Resilience | In-flight state captured at the exact moment a fault is injected — mid-transaction records, unacknowledged messages, partially applied writes. |
| Contract | Both schema-valid payloads and deliberately schema-invalid payloads, to prove a consumer rejects what it should reject. |
| Security | An identity and entitlement matrix covering every role and permission boundary the archetype exercises. |
| Data quality | Deliberately dirty records with a known, counted number of defects, so a reconciliation or validation check can be graded against a ground truth. |

## Compliance Mapping

| Layer | Reference | Section/Control | How this satisfies |
|---|---|---|---|
| Ring 0 | NIST SP 800-53 | SA-15 (Development Process, Standards, and Tools) | The [Synthetic Generation Strategy](#synthetic-generation-strategy)'s deterministic, seed-based generation is the documented, repeatable data-production process SA-15 requires. |
| Ring 0 | OWASP Testing Guide / CIS Controls | Test-data guidance; CIS control on data handling in non-production | The [Prohibitions](#prohibitions) section operationalises the general "no production data in lower environments" guidance into a checkable, corpus-wide rule. |
| Ring 1 | [PCI-DSS 4.0](../../compliance/pci-dss-4-0.md) | §6.5.5 (live PANs prohibited in test and development); §3 (stored account data protection) | The Card PAN prohibition and the designated-test-BIN-range rule in [Prohibitions](#prohibitions) are exactly §6.5.5's live-PAN ban and §3's stored-account-data protection, made concrete for this corpus. |
| Ring 1 | GDPR | Art. 5(1)(c) — data minimisation | The synthetic-only mandate is a stronger control than minimisation: no real personal data is collected into test material at all, rather than merely limiting how much is retained. |
| Ring 2 | [COMP-003 Decree 13/2023/ND-CP](../../compliance/decree-13-2023-personal-data.md) — personal-data protection ⚠️ (working summary — pending Legal review) | Bounds on what test data may contain | The [Prohibitions](#prohibitions) section's ban on real names, real dates of birth, and real national ID numbers keeps every environment this corpus governs outside Decree 13's scope for personal data entirely, rather than relying on a lawful-basis argument for processing real data. |

## Related

- [TST-001 Test Strategy Standard](./test-strategy-standard.md)
- [TST-002 Performance Test Standard](./performance-test-standard.md)
- [TST-005 Test Environments and Quality Gates](./environments-quality-gates.md)
- [TST-039 Data Quality and Reconciliation](../archetypes/data-quality-reconciliation.md)
- [TST-041 Data Protection, Masking and Tokenisation](../archetypes/data-protection-masking-tokenisation.md)
- [SEC-008 Data Masking](../../patterns/security/data-masking.md)
- [SEC-013 PII Tokenization (Format-Preserving)](../../patterns/security/pii-tokenization-format-preserving.md)
- [COMP-003 Decree 13/2023/ND-CP — Personal Data Protection (VPDP)](../../compliance/decree-13-2023-personal-data.md)
- [COMP-004 PCI-DSS v4.0](../../compliance/pci-dss-4-0.md)
