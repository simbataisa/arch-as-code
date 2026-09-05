# TST-023 -- Concurrent Limit & Counter (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.

| ID | Invariant |
|---|---|
| I1 | Admitted count equals min(N, L) under a genuine concurrency burst |
| I2 | Sampled utilisation never exceeds the declared limit at any instant |
| I3 | Releasing a reservation returns exactly its own amount |
| I4 | A second release of the same reservation is rejected |
| I5 | The window boundary uses the account's declared timezone |

Defect proof: with the `reservation-overcommit` defect active this module MUST report I1 and I2
failed, and I3/I4 still passed.

The declared limit `L` is a per-account fixture value read from `account_limit` -- a business
limit owned by the SUT's own data, not a service SLO. That is why this module carries no
`threshold_ref`: citing an NFR row for it would be a fabricated provenance.

## What this module drives

`plan.jmx` runs three phases against the reservation capability:

1. **setUp Thread Group** (`Reset and Seed Limit`, 1 thread, 1 loop) truncates
   `reservation`/`account_limit`/`ledger_entry`/`account` with the same
   `TRUNCATE ... RESTART IDENTITY CASCADE` the SUT's own
   `AbstractReservationIntegrationTest` uses, then inserts `ACC-000001` and its
   `account_limit` row. The reset is necessary, not tidy: `GET /reservations/utilisation`
   sums every held row for the account with no scoping to this run, so one defect-active
   run's overcommitted holds would otherwise poison every later clean run's I1/I2.
2. **Main Thread Group** (`Reservation Burst`, 16 threads x 1 loop) fires
   `POST /reservations` with a **Synchronizing Timer** (group size 16) so all sixteen
   threads are released together. Genuine simultaneity is the whole point: the
   `SELECT ... FOR UPDATE` serialisation this exercises only fails to hold under real
   contention, and threads trickling in under ramp-up alone would each see uncontended
   capacity. A `JSR223 PostProcessor` tallies 201s and 409s into JMeter's `props` --
   the same cross-thread aggregation pattern `tst-031-ratelimit` uses -- and samples
   `GET /reservations/utilisation` after each admission, keeping the running maximum,
   because I2 demands a continuous sample rather than a start/end pair.
3. **TearDown Thread Group** (`Verify Reservation`, 1 thread, 1 loop) runs only after every
   burst thread has finished. It exercises I3 and I4 directly -- reserve, release, assert the
   amount came back, release again and require rejection -- then `assert-reservation.groovy`
   evaluates I1-I5 and writes one fragment.

## Running it

```
make up PROFILES=core           # from qe-harness/
./bin/run-module.sh TST-023     # from qe-harness/
```

Exits non-zero exactly when the emitted fragment's result is `failed` (see `bin/run-jmeter.sh`).

## Defect proof

```
curl -X POST http://localhost:8080/_test/defect/reservation-overcommit   # 204
./bin/run-module.sh TST-023                                              # must report I1+I2 FAILED
curl -X DELETE http://localhost:8080/_test/defect                        # 204, always clears it
```

With `reservation-overcommit` active, `ReservationService.reserve` skips the capacity
comparison entirely -- the hold is still inserted and still counted, so utilisation provably
exceeds `L`. I3 and I4 are untouched by that branch and must still pass, which is what makes
the defect proof specific rather than merely sensitive. `Tst023ModuleTest`'s
`reportsCapacityFailureAgainstTheOvercommitDefect` drives this exact sequence via
`ModuleRunner`, which performs the HTTP activate/clear itself.
