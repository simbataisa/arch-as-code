# TST-021 -- Ledger & Monetary Invariant (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.

| ID | Invariant |
|---|---|
| I1 | Trial balance nets to zero after every transfer batch |
| I2 | Every transfer_ref has exactly two ledger entries |
| I3 | No ledger entry has amount_minor = 0 |

Defect proof: with the `ledger-unbalanced` defect active (see below) this module MUST report
I1 failed.

## What this module drives

`plan.jmx` runs three phases against the reference SUT's ledger capability
(`POST /transfers`, `GET /ledger/trial-balance`):

1. **setUp Thread Group** (`Reset and Seed Accounts`, 1 thread, 1 loop) truncates
   `ledger_entry`/`account` (same `TRUNCATE ... RESTART IDENTITY CASCADE` pattern
   `AbstractLedgerIntegrationTest#resetLedgerFixture()` uses in the reference SUT's own test
   suite) and inserts the two synthetic accounts (`ACC-000001`, `ACC-000002`) the load below
   transfers between. This reset is necessary, not just tidy: `GET /ledger/trial-balance`
   nets *every* `ledger_entry` row ever written, with no scoping to "this run's transfers"
   and no reset endpoint of its own -- without it, one defect-active run's unbalanced
   entries permanently poison every later clean run's I1/I2 result (confirmed empirically).
   The reference SUT also does not seed the `account`/`ledger_entry` tables at application
   startup (see `SyntheticDataSeeder`'s own javadoc), so this is also what makes a
   freshly-started `make up PROFILES=core` stack's empty ledger runnable at all.
2. **Main Thread Group** (`Transfer Load`, 8 threads x 5 loops) fires `POST /transfers`
   concurrently, alternating direction each iteration. A **Synchronizing Timer** (group
   size 8) blocks every thread until all eight have arrived, then releases them together --
   this is what makes the run genuinely concurrent rather than threads trickling in
   one-by-one under ramp-up alone, which matters because `TransferService`'s deadlock-
   avoidance lock ordering only gets exercised under real contention.
3. **TearDown Thread Group** (`Verify Ledger`, 1 thread, 1 loop) runs only after every
   `Transfer Load` thread has finished -- JMeter guarantees this ordering. It calls
   `GET /ledger/trial-balance` once, then `assert-trial-balance.groovy` evaluates I1-I3 and
   calls the JVM `EvidenceEmitter` (from `qe-harness-common`, on JMeter's classpath via this
   module's `testPlanLibraries`) to write one fragment to `traceability/runs/`.

   Asserting the invariants **after** the load has fully drained, not mid-run, is
   deliberate: mid-run, a transfer that is still in its own transaction can make the trial
   balance transiently non-zero even with a correctly-functioning ledger -- that would be a
   false failure, not evidence of a real invariant violation.

I2/I3 are evaluated by a direct `java.sql.DriverManager` query against Postgres inside
`assert-trial-balance.groovy`, not a JMeter-native "JDBC Request" sampler: `GET
/ledger/trial-balance` only returns the aggregate net and row count (see
`LedgerController`), so confirming "every transfer_ref has exactly two entries" and "no
entry has amount_minor = 0" needs a query the SUT's HTTP surface does not expose.

## Running it

```
make up PROFILES=core          # from qe-harness/, brings up postgres + reference-sut
./bin/run-module.sh TST-021    # from qe-harness/
```

Exits non-zero exactly when the emitted fragment's result is `failed` (see
`bin/run-jmeter.sh`).

## Defect proof

The defect is injected on the running SUT over HTTP, not via a process environment
variable (the reference SUT runs in a separate, already-running container -- see
`DefectController`/`DefectFlags` in `reference-sut`):

```
curl -X POST http://localhost:8080/_test/defect/ledger-unbalanced   # 204
./bin/run-module.sh TST-021                                          # must report I1 FAILED
curl -X DELETE http://localhost:8080/_test/defect                    # 204, always clears it
```

With `ledger-unbalanced` active, `TransferService` skips writing the credit leg of every
transfer -- the debit leg alone is still written and counted, so the trial balance
provably drifts non-zero and I1 fails. `Tst021ModuleTest`'s
`reportsInvariantFailureAgainstTheUnbalancedDefect` test drives this exact sequence via
`ModuleRunner`, which performs the HTTP activate/clear itself (see that class's javadoc).
