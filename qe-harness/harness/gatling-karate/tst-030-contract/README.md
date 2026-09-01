# TST-030 -- Transfer Contract Compatibility (Karate + Gatling)

Oracle: contract-schema. Best-fit tool per TST-010: Karate + Gatling (`gatling-karate`).

This is the first non-JMeter harness module, and its whole point is to prove `TST-012`'s
headline claim: **one `.feature` file drives both a functional run and a performance run.**

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
(the v1 scenario, which never reads the defect flag, keeps passing) -- confirmed by
`Tst030ContractRunner#featureFailsAgainstTheSchemaDriftDefect`.

## Running the Karate side

```
cd qe-harness && docker compose --profile core up -d --wait   # postgres + reference-sut
cd harness && mvn -pl gatling-karate test
```

Both the Karate JUnit runner's proof tests and its own clean-SUT baseline pass; each of the
non-string-check tests writes one `oracle: contract-schema` evidence fragment to
`traceability/runs/` via the shared JVM `EvidenceEmitter`.

**JDK caveat:** Karate 1.4.1's `Suite#run` hangs indefinitely (no error, no CPU) when the
Surefire-forked test JVM itself is JDK 25. Launch Maven under JDK 21 or older
(`JAVA_HOME="$(/usr/libexec/java_home -v 21)" mvn ...`), or override just the forked test JVM
via `-Dqe.gatlingKarate.javaRuntime=/path/to/jdk21/bin/java`. See `qe-harness/README.md` and
this module's `pom.xml` for the full writeup.

## Running the Gatling side

Not bound to `mvn test` (same reasoning as the jmeter module never binding
`jmeter-maven-plugin` to a lifecycle phase: a plain `mvn test` must never also fire a real
load run). Invoke it directly:

```
mvn -pl gatling-karate io.gatling:gatling-maven-plugin:4.21.11:test \
    "-Dgatling.simulationClass=com.techcombank.qe.harness.gatlingkarate.Tst030Simulation"
```

`Tst030Simulation` also emits an `oracle: contract-schema` fragment (via `after { }`), based on
whether any Gatling `Session` in the run ended up `isFailed` after the `karateFeature(...)`
step -- deliberately not by parsing Gatling's own aggregate stats/report, the same
app-level-assertion-over-tool-report choice `assert-trial-balance.groovy` makes for TST-021.

**Known issue (not yet resolved):** on this task's own dev environment (macOS/aarch64, JDK 21)
the command above fails before any scenario runs, with
`NoClassDefFoundError: io/netty/channel/IoOps` inside `io.gatling.netty.util.Transports`,
preceded by suppressed `netty_tcnative` native-library load failures for the
`osx-aarch_64`/`aarch_64` classifiers. This is a forked-classpath/native-library mismatch in
the `gatling-maven-plugin` 4.21.11 + Gatling 3.15.1 + Netty 4.2.14.Final combination on this
platform, not a bug in `Tst030Simulation` itself: the class compiles cleanly
(`mvn -pl gatling-karate test-compile`), and its `karateFeature(...)` call is independently
verified to reference the shared feature (see above). Left registered in `pom.xml` rather than
removed, since it may work on a different OS/arch (e.g. a Linux CI runner).

## Defect proof (manual)

```
curl -X POST http://localhost:8080/_test/defect/schema-drift   # 204
mvn -pl gatling-karate test -Dtest=Tst030ContractRunner#featureFailsAgainstTheSchemaDriftDefect
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
process has no effect on it.

## Not yet wired: `bin/run-module.sh TST-030`

Unlike the jmeter modules, this module has no `bin/run-gatling-karate.sh` yet, so
`./bin/run-module.sh TST-030` / `make run ARCH=TST-030` are not runnable end-to-end. Task 20's
brief scopes this module to the shared feature file, the two runners, and their own JUnit
proof tests; a `run-module.sh` dispatch script is not one of its listed deliverables.
