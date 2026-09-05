# TST-020 -- Idempotency & Replay (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.

| ID | Invariant |
|---|---|
| I1 | N same-key requests produce exactly one state change |
| I2 | A replay returns a byte-identical status and body |
| I3 | N distinct keys produce N state changes |
| I4 | The same key with a different payload is a conflict |
| I5 | Under true concurrency one wins and the other is served the stored response |
| I6 | Key TTL is at least the declared client max retry window |
| I7 | Deduplication survives broker redelivery |

Defect proof: with `idempotency-key-ignored` active this module MUST report I1 failed. `execute()`
skips the key lookup (and the reservation row it writes) entirely, so I2 and I5 -- which rest on
that same reservation row -- fail alongside it, and so does I4, since `PayloadConflict` detection
is reached only via that same bypassed path. What stays specific is I3 (distinct keys never
collide, reservation or not), I6 (reads static configuration, not the reservation table) and I7
(a wholly separate capability, `DeliveryService`) -- all three MUST still pass.

**Why this module is last in the wave.** I7 requires a broker that redelivers, which did not
exist in this repository until Wave 17's Phase 2. Running TST-020 before the broker landed
would have meant shipping `coverage: partial` with I7 unreached; sequencing it after buys the
full set. That ordering was a design decision, not an accident -- see the design spec's
decision 2.

I5 rests on the `idempotency_key` table's UNIQUE constraint, not on application-level
check-then-insert: under a synchronised burst two callers race, exactly one insert survives, and
the loser catches the violation and serves the winner's stored response. I2 compares the replay
**byte for byte**, which is why the response body is stored verbatim rather than re-serialised.

## Why I7 is NOT a `POST /messaging/work` redelivery through `ledger_entry`

An earlier draft of this module's task brief asked for I7 to be checked by submitting a job via
`POST /messaging/work`, submitting the same job id again, then polling `ledger_entry`'s row
count. That mechanic is unimplementable as stated: `POST /messaging/work` is served by
`DeliveryController`/`DeliveryService` (TST-029's own capability, Task 23), which has no
relationship whatsoever to `ledger_entry` -- that table belongs entirely to
`TransferService`/`IdempotencyService`, reached only via `POST /transfers`. Polling
`ledger_entry` after a `/messaging/work` call could never detect anything, pass or fail,
regardless of what the SUT does.

This module instead does what the canonical archetype (`idempotency-replay.md`, I7) actually
asks for: "consumer-side deduplication survives broker redelivery -- `assert state_change_count
== 1` after the broker redelivers the same message under at-least-once semantics." That
invariant is genuinely about `DeliveryService`'s own AMQP consumer, not the ledger -- which is
why Task 26 added a small, additive dedup gate to `DeliveryService.consume()` (a
`ConcurrentHashMap`-backed `seenJobIds` set): before this task, a non-poison message
unconditionally incremented `stateChanges` on every delivery, so a broker redelivery of an
already-processed job would have (incorrectly) counted as a second state change -- exactly the
gap this archetype's own Failure Taxonomy names ("idempotency enforced at the API gateway layer
but not at the downstream message consumer, so a second broker delivery bypasses the control
entirely"). `attempts` still counts every delivery, for observability; only `stateChanges` is
deduplicated.

This module drives I7 the same way I1/I2/I5's own same-key burst simulates a client-side
duplicate: a second, identical `POST /messaging/work?jobId=...` call stands in for a real
broker-level redelivery, per this repository's established convention (see how I1/I2's own
burst step in this same module works) of testing replay-style invariants via a repeated
identical call rather than orchestrating a real AMQP-level nack/requeue.

## What this module drives

1. **setUp Thread Group** (`Reset Idempotency Fixture`, 1 thread, 1 loop) truncates
   `idempotency_key` explicitly -- it has no FK to `account`, so a `CASCADE` misses it -- plus
   `ledger_entry`/`account`, seeds the two fixture accounts, and calls
   `POST /messaging/delivery/reset` to zero `DeliveryService`'s own in-memory counters, which
   otherwise persist across module runs in the same long-lived SUT process and would leak
   TST-029's own module's (Task 24) state into this one's I7 check.
2. **Main Thread Group** (`Same-Key Burst`, 12 threads x 1 loop) fires
   `POST /transfers` with one shared `Idempotency-Key` behind a **Synchronizing Timer** (group
   size 12) so the race in I5 is genuine rather than sequential. A `JSR223PostProcessor` tallies
   `201` versus `200` responses and collects the distinct response bodies.
3. **TearDown Thread Group** (`Verify Idempotency`, 1 thread, 1 loop) exercises the remaining
   invariants directly: three distinct keys for I3; the same key with a changed amount for I4,
   requiring `409`; `GET /transfers/idempotency/{key}` for I6's declared-configuration check;
   and, for I7, a `POST /messaging/work` submission followed by a bounded poll for its (async)
   delivery, then a second, identical submission of the same job id -- standing in for a broker
   redelivery, per the note above -- and a fixed settling wait before a final read of
   `GET /messaging/delivery/state` confirming `stateChanges == 1`, not merely unchanged from an
   arbitrary baseline. `assert-idempotency.groovy` then evaluates I1-I7.

## Running it

```
make up PROFILES="core messaging"     # I7 needs the broker
./bin/run-module.sh TST-020
```

## Defect proof

```
curl -X POST http://localhost:8080/_test/defect/idempotency-key-ignored   # 204
./bin/run-module.sh TST-020                                              # must report I1 FAILED
curl -X DELETE http://localhost:8080/_test/defect                        # 204
```

With the defect active, `IdempotencyService.execute` skips the key lookup entirely and always
executes, so twelve same-key requests write twelve balanced pairs (I1 fails: 24 rows, not 2).
Every invariant resting on that same bypassed reservation row fails with it -- I2 (no replay ever
happens, so the distinct-body count is 12, not 1), I4 (`PayloadConflict` is only ever detected via
that same reservation lookup, so the reused-key-changed-payload request just executes normally
and returns `201`, not `409`) and I5 (twelve independent winners, not one). The ledger stays
balanced throughout regardless, so TST-021's own invariants (a different module entirely) are
untouched. Within this module, what stays specific is I3 (distinct keys never collide, whether or
not the reservation path runs), I6 (reads static configuration, never the reservation table) and
I7 (`DeliveryService`'s own dedup gate, a wholly separate capability) -- all three still pass with
the defect active.
