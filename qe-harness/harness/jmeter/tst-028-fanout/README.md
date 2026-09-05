# TST-028 -- Fan-out / Fan-in Correlation (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.

| ID | Invariant |
|---|---|
| I1 | An aggregate is emitted only when complete, or when timed out **and** marked partial |
| I2 | Correlation IDs are unique within the window |
| I3 | The aggregate is the union of branch responses, with no duplicates |
| I4 | Fan-in latency approximates max(branch), and is below sum(branch) |
| I5 | A claim-check reference resolves through its retention boundary |

Defect proof: with `aggregate-emitted-incomplete` active this module MUST report I1 failed and
I2/I3 still passed.

I4 is the invariant most easily faked: a sequential fan-out would still produce a correct
aggregate, so the module measures elapsed fan-in time against the **sum** of the individual
branch latencies. If fan-in took as long as the sum, the branches ran in series and the
"fan-out" is a fan-out in name only.

Correlation IDs are hyphenated short forms (`corr-0001`), never epoch-millis suffixes -- gate
check 5 fails the build on any run of 13-19 digits anywhere under `qe-harness/`.

## What this module drives

1. **setUp Thread Group** (`Reset Aggregator`, 1 thread, 1 loop) calls
   `POST /messaging/fanout/reset`.
2. **Main Thread Group** (`Fan Out and Reply`, 5 threads x 2 loops) posts
   `POST /messaging/fanout`, records the returned correlation ID into `props` for I2's
   uniqueness check, then replies from all three branches -- except on one deliberate
   iteration, which replies from only one branch so I1's timeout-and-marked-partial arm is
   exercised. Per-branch and whole-fan-in elapsed times are tallied for I4.
3. **TearDown Thread Group** (`Verify Correlation`, 1 thread, 1 loop) reads
   `GET /messaging/aggregate` for both the complete and the timed-out correlation, then
   `assert-fanout.groovy` evaluates I1-I5.

## Running it

```
make up PROFILES="core messaging"
./bin/run-module.sh TST-028
```

## Defect proof

```
curl -X POST http://localhost:8080/_test/defect/aggregate-emitted-incomplete   # 204
./bin/run-module.sh TST-028                                                    # must report I1 FAILED
curl -X DELETE http://localhost:8080/_test/defect                              # 204
```

With the defect active, `AggregatorService.branchReply` emits on the first reply and does **not**
set the partial marker -- an incomplete aggregate presented as complete, which is precisely I1's
violation. Correlation allocation and union semantics are untouched, so I2 and I3 still pass.
