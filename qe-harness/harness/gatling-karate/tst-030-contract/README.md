# TST-030 -- Transfer Contract Compatibility (Karate + Gatling)

Oracle: contract-schema. Best-fit tool per TST-010: Karate + Gatling (`gatling-karate`).

This is the first non-JMeter harness module, and its whole point is to prove `TST-012`'s
headline claim: **one `.feature` file drives both a functional run and a performance run.**
Both halves are verified to actually run end-to-end against the real reference SUT (see
"Running the Gatling side" below) -- not just to compile or reference the right classpath
string.

## The shared feature

`../src/test/resources/tst-030-contract/transfer-contract.feature` has two scenarios:

| Scenario | Asserts |
|---|---|
| v2 response satisfies its published schema | `POST /v2/transfers` returns exactly `{transferRef, status, settledAt}` |
| v1 remains backward compatible | `POST /v1/transfers` still returns (at least) `{transferRef, status}` |

Both runners drive this exact classpath resource, not a copy:

- **Karate** (`Tst030ContractRunner`, a plain JUnit 5 runner): `Runner.path("classpath:tst-030-contract/transfer-contract.feature")`.
- **Gatling** (`Tst030Simulation`): `karateFeature("classpath:tst-030-contract/transfer-contract.feature")` (`com.intuit.karate.gatling.PreDef`), wrapped in a single-user scenario.

`Tst030ContractRunner#sameFeatureDrivesTheGatlingSimulation` reads `Tst030Simulation.scala`'s
own source and asserts that literal path string appears in it. This is the test that stops
the two runners silently drifting into two separate, hand-maintained suites -- a drift that
would quietly falsify `TST-012`'s claim without ever failing loudly on its own.

Defect proof: with the `schema-drift` defect active (see below), the v2 scenario MUST fail
(the v1 scenario, which never reads the defect flag, keeps passing) -- confirmed on **both**
runners: `Tst030ContractRunner#featureFailsAgainstTheSchemaDriftDefect` (Karate) and a manual
Gatling run with the defect active (see "Defect proof (manual)" below), which reports
`POST /v2/transfers (OK=0 KO=1)` / `POST /v1/transfers (OK=1 KO=0)`.

## Running everything at once

```
cd qe-harness && docker compose --profile core up -d --wait   # postgres + reference-sut
./bin/run-module.sh TST-030
```

Runs the Karate functional check, then the Gatling load run, against the SUT's current
(clean) state; exits non-zero if either reports `failed`. See `bin/run-gatling-karate.sh`.

## Running the Karate side directly

```
cd qe-harness/harness && mvn -pl gatling-karate test
```

Runs all three of `Tst030ContractRunner`'s tests (the brief's two given proof tests, plus an
added `passesAgainstTheCleanSut` baseline); each of the non-string-check tests writes one
`oracle: contract-schema` evidence fragment to `traceability/runs/` via the shared JVM
`EvidenceEmitter`.

**JDK caveat:** Karate 1.4.1's `Suite#run` hangs indefinitely (no error, no CPU) when the
Surefire-forked test JVM itself is JDK 25. Launch Maven under JDK 21 or older
(`JAVA_HOME="$(/usr/libexec/java_home -v 21)" mvn ...`), or override just the forked test JVM
via `-Dqe.gatlingKarate.javaRuntime=/path/to/jdk21/bin/java`. `bin/run-gatling-karate.sh`
resolves this automatically (same `/usr/libexec/java_home -v 21`/`17` fallback
`bin/run-jmeter.sh` uses); see `qe-harness/README.md`'s "Known Issues" section and this
module's `pom.xml` for the full writeup.

## Running the Gatling side directly

Not bound to `mvn test` (same reasoning as the jmeter module never binding
`jmeter-maven-plugin` to a lifecycle phase: a plain `mvn test` must never also fire a real
load run). Invoke it directly:

```
mvn -pl gatling-karate io.gatling:gatling-maven-plugin:4.21.11:test \
    "-Dgatling.simulationClass=com.techcombank.qe.harness.gatlingkarate.Tst030Simulation"
```

**Verified actually running end-to-end** (JDK 21; this module's Gatling engine is pinned to
3.9.5, not the newer 3.15.1 -- see `qe-harness/README.md`'s "Known Issues" section for why):
both scenarios fire against the real reference SUT (`POST /v1` and `/v2/transfers`, `OK=2
KO=0`), and `Tst030Simulation`'s `after { }` hook emits an `oracle: contract-schema` fragment
(`result: passed`) to `traceability/runs/` via the shared JVM `EvidenceEmitter`, based on
whether any Gatling `Session` ended up `isFailed` after the `karateFeature(...)` step --
deliberately not by parsing Gatling's own aggregate stats/report, the same
app-level-assertion-over-tool-report choice `assert-trial-balance.groovy` makes for TST-021.

## Defect proof (manual)

```
curl -X POST http://localhost:8080/_test/defect/schema-drift   # 204

# Karate side:
mvn -pl gatling-karate test -Dtest=Tst030ContractRunner#featureFailsAgainstTheSchemaDriftDefect

# Gatling side (same defect, same feature, same SUT):
mvn -pl gatling-karate io.gatling:gatling-maven-plugin:4.21.11:test \
    "-Dgatling.simulationClass=com.techcombank.qe.harness.gatlingkarate.Tst030Simulation"
# -> POST /v2/transfers (OK=0 KO=1), POST /v1/transfers (OK=1 KO=0); emitted fragment's
#    result is "failed" with evidence.sut_defect = "schema-drift"

curl -X DELETE http://localhost:8080/_test/defect               # 204, always clears it
```

With `schema-drift` active, `TransferV2Controller` renames the `transferRef` field to
`transfer_id` in `POST /v2/transfers`'s response -- a field *rename*, not a removal,
specifically because that is the failure mode a naive field-count check would miss while a
real schema-validation contract test catches immediately (the v2 scenario's
`match response == {...}` requires the `transferRef` key by name). `v1/transfers` never reads
the defect flag, so the v1 scenario keeps passing throughout.

`Tst030ContractRunner` activates/clears the defect itself over HTTP
(`POST /_test/defect/schema-drift`, `DELETE /_test/defect`) in a try/finally around the Karate
run -- the SUT is a separate, already-running container; a JVM system property on the test
process has no effect on it. `bin/run-gatling-karate.sh` (and `./bin/run-module.sh TST-030`)
never toggle the defect themselves -- that stays the JUnit suite's own job, run separately via
`mvn test`, exactly like `ModuleRunner.java` (not `bin/run-jmeter.sh`) owns defect toggling for
the jmeter modules.
