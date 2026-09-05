# TST-026 -- Message Transformation & Routing (JMeter)

Oracle: **contract-schema**. Best-fit tool per TST-010: JMeter.

| ID | Invariant |
|---|---|
| I1 | Every source field maps, or is a documented discard |
| I2 | Zero messages reach a default or fallback route |
| I3 | An unmapped enum is rejected, never defaulted |
| I4 | Splitter output count equals the declared element count |
| I5 | Round trip preserves amount scale and currency (BigDecimal compareTo == 0) |
| I6 | Vietnamese diacritics survive byte-identically |
| I7 | An enricher failure yields an error and zero partial messages |

Defect proof: with `route-default-fallthrough` active this module MUST report I2 failed and
I1/I5/I6 still passed.

This is the **only** module in the harness using the `contract-schema` oracle, and the first
caller of `ContractSchema` anywhere in this repository. Every routed message is validated
against `GET /messaging/contract` -- the schema the SUT itself publishes -- rather than against
a copy pasted into this module, so the contract cannot drift from what the service serves.

`amount` travels as a **scaled string**, not a JSON number: a JSON number lets a parser
normalise `1500.00` to `1500`, destroying exactly the scale I5 exists to protect. I5 compares
with `BigDecimal.compareTo`, never `equals`.

Task 17's SUT surface has no batch/splitter or enricher stage of its own -- `RoutingService`
transforms one message at a time and validates `kind` inline, and its own `RoutingServiceTest`
only exercises I2/I3/I5/I6. I4 and I7 are still asserted against real SUT behaviour rather than
marked partial: I4 checks that a declared batch of independent `POST /messaging/transform` calls
comes back 1:1 (no call silently drops or duplicates its own output), and I7 checks that every
rejected (`422`) publish -- the closest thing this SUT has to an enricher-style validation failure
-- leaves the routed queues untouched, which `RoutingService.publish` guarantees structurally by
validating before it ever calls `RabbitTemplate.convertAndSend`.

## What this module drives

1. **setUp Thread Group** (`Reset Messaging Fixture`, 1 thread, 1 loop) purges the routing
   queues via the broker's own management API (there is no SUT reset endpoint for either the
   queues or the in-memory published log, and none of I1-I7 read that log, so the queues are
   what actually needs resetting for a repeatable run), computes I4's and I7's tallies against
   the freshly-purged queues, then fetches `GET /messaging/contract` once and stores it for the
   assertion.
2. **Main Thread Group** (`Publish Mixed Keys`, 6 threads x 4 loops) posts to
   `POST /messaging/publish` with a deliberate mix: matched `pay.domestic.*` and `pay.intl.*`
   keys, unmatched keys that must quarantine (I2), a payload carrying `Nguyễn Thị Hoà` for I6,
   a two-place `amount` for I5, and one `kind` outside the declared domain that must come back
   `422` for I3.
3. **TearDown Thread Group** (`Verify Routing`, 1 thread, 1 loop) reads
   `GET /messaging/routed` for I2's verdict, replays one message through
   `POST /messaging/transform` for I1/I5/I6, then `assert-routing.groovy` validates the
   transformed message against the published schema with `ContractSchema` and evaluates I1-I7.

## Running it

```
make up PROFILES="core messaging"     # the broker is NOT in the core profile
./bin/run-module.sh TST-026
```

## Defect proof

```
curl -X POST http://localhost:8080/_test/defect/route-default-fallthrough   # 204
./bin/run-module.sh TST-026                                                 # must report I2 FAILED
curl -X DELETE http://localhost:8080/_test/defect                           # 204
```

With the defect active, `RoutingService.publish` rewrites an unmatched routing key to a real
one, so the message reaches `qe.q.route.domestic` and the quarantine queue stays empty. The
transform itself is untouched, so I1, I5 and I6 still pass -- which is what makes the proof
specific rather than merely sensitive.
