# TST-027 -- Ordering & Resequencing (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.
Coverage: **partial** -- see `partial_reason` in `traceability/modules.yml`.

| ID | Invariant | Asserted here |
|---|---|---|
| I1 | Emitted order equals sorted order, against a shuffled publish order | yes |
| I2 | A gap resolves inside the declared timeout, or an escalation is emitted | yes |
| I3 | Each sequence is emitted exactly once, including after restart | partly -- see below |
| I4 | An overflow event fires at the bound; `silently_dropped == 0` | yes |
| I5 | Declared scope is one of {per_key, per_partition, global}, zero violations within it | **per_key only** |

I5 is asserted for `per_key` and **cannot** be asserted for `per_partition` or `global`:
RabbitMQ has no partitions, so those scopes have no meaning against this broker. Declaring I5
satisfied on the strength of the per-key case alone would be claiming a broader guarantee than
the evidence supports. I3's post-restart clause is likewise not exercised here -- the restart
path belongs to TST-029, which owns it.

Publish order is **deliberately shuffled** by the plan. Feeding sequences in order would make
I1 pass against a resequencer that does nothing at all, which is the failure mode this
invariant exists to catch.

Defect proof: with `resequencer-emits-on-arrival` active this module MUST report I1 failed and
I3 still passed.

## What this module drives

1. **setUp Thread Group** (`Reset Sequence State`, 1 thread, 1 loop) calls
   `POST /messaging/sequence/reset`.
2. **Main Thread Group** (`Shuffled Publish`, 4 threads x 4 loops) posts to
   `POST /messaging/sequence/publish` with sequence numbers permuted per thread, plus one
   duplicate for I3 and a deliberate gap for I2. A **Synchronizing Timer** (group size 4)
   releases the threads together so arrival order genuinely differs from sequence order.
3. **TearDown Thread Group** (`Verify Ordering`, 1 thread, 1 loop) reads
   `GET /messaging/sequence/state` once -- it carries the emitted order, the overflow flag,
   `silently_dropped`, the escalation flag and the declared scope -- then
   `assert-ordering.groovy` evaluates I1-I4 and reports I5 for `per_key`.

## Running it

```
make up PROFILES="core messaging"
./bin/run-module.sh TST-027
```

## Defect proof

```
curl -X POST http://localhost:8080/_test/defect/resequencer-emits-on-arrival   # 204
./bin/run-module.sh TST-027                                                    # must report I1 FAILED
curl -X DELETE http://localhost:8080/_test/defect                              # 204
```

With the defect active, `ResequencerService.accept` bypasses the buffer and emits on arrival, so
the emitted order matches the shuffled publish order rather than sorted order. The dedup check
sits outside the ordering buffer, so I3 still passes -- which is what makes the proof specific.
