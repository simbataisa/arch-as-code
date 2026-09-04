# TST-029 -- Delivery Guarantee, Retry, DLQ (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.
Coverage: **full** -- every invariant is implemented. I2's restart path is
**run-gated**, not unimplemented; see below.

| ID | Invariant |
|---|---|
| I1 | Every published message produced one state change or is in the DLQ |
| I2 | A broker restart loses nothing acked-persisted |
| I3 | A poison message reaches the DLQ inside the declared attempts, without blocking others |
| I4 | Retry intervals match the declared backoff, `distinct_intervals > 1` |
| I5 | DLQ depth is exported and an alert fires past its declared threshold |
| I6 | A permanent error stops retrying at the declared ceiling |

**I2 and CI.** Proving nothing acked-persisted is lost requires restarting the broker process:
every queue in this topology is `durable: true` precisely so that promise is testable. Toxiproxy
severance -- which this harness already uses for TST-035 -- would only prove the client
reconnects, not that the queue survived, so using it here would be a weaker check wearing a
stronger one's name. This module therefore runs `docker compose restart broker` on a full run,
and in CI (`HARNESS_SMOKE_MODE=true`) emits I2 as `not-evaluated` with the reason
`"restart path exercised in full runs only"`. That is an honest gap in a run, not a gap in the
implementation -- which is why coverage stays `full`.

I4 reads the declared ladder from `GET /messaging/delivery/state` rather than inferring
intervals from observed timings, which would be flaky under load. `distinct_intervals > 1` is
checked against `app.messaging.retry-intervals-ms` at the source.

## What this module drives

1. **setUp Thread Group** (`Reset Delivery State`, 1 thread, 1 loop) calls
   `POST /messaging/delivery/reset`, which purges both `qe.q.work` and `qe.q.dlq`.
2. **Main Thread Group** (`Submit Work and Poison`, 6 threads x 3 loops) posts to
   `POST /messaging/work`, mixing ordinary jobs with poison ones -- enough poison to drive DLQ
   depth past `dlqAlertDepth` for I5, and at least one ordinary job queued behind a poison one
   for I3's non-blocking clause.
3. **TearDown Thread Group** (`Verify Delivery`, 1 thread, 1 loop) polls
   `GET /messaging/delivery/state` to a **bounded** deadline until submitted ==
   stateChanges + dlqCount, then -- on a full run only -- restarts the broker and re-reads the
   DLQ depth for I2. `assert-dlq.groovy` then evaluates I1, I3-I6, and I2 or its
   `not-evaluated` reason.

## Running it

```
make up PROFILES="core messaging"
HARNESS_SMOKE_MODE=true ./bin/run-module.sh TST-029   # I2 not-evaluated
./bin/run-module.sh TST-029                           # I2 exercised: restarts the broker
```

## Defect proof

```
curl -X POST http://localhost:8080/_test/defect/dlq-bypass-drop   # 204
./bin/run-module.sh TST-029                                       # must report I1 FAILED
curl -X DELETE http://localhost:8080/_test/defect                 # 204
```

With the defect active, `DeliveryService.consume` acknowledges a poison message without
processing it and without dead-lettering it -- the message simply vanishes, so
`submitted > stateChanges + dlqCount` and I1's conservation law breaks. The retry ladder and the
alert threshold are untouched, so I4 and I5 still pass.
