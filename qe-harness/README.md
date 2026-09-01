# QE Harness Reference Implementation

Status: Approved | Last Reviewed: 2026-08-24 | Owner: @qe-lead
Catalog ID: TST-016 | Radii
Tier Applicability: T0, T1, T2, T3

## Problem Statement

The Wave 15 testing corpus defines 24 archetypes, four oracle types, and eight performance
profiles, and states plainly that it ships no harness. A QE team could read all of it and still
have nothing to run. This directory is the runnable counterpart: a synthetic reference service
and seven harness modules, one per archetype family, each in the tool
[TST-010](../knowledge-base/testing/tooling/tool-selection-matrix.md) names as its best fit.

## Quick Start

    make up          # start the SUT and the infrastructure its modules need
    make verify      # run the gates — no containers required
    make run ARCH=TST-021
    make run-all     # all seven modules against the clean SUT
    make run-defects # all seven modules against their injected defects; each MUST fail

## Scope

Seven of the 24 archetypes are implemented. `GET /_capabilities` lists all 24; the other 17
answer `501` with their archetype ID. Waves 17+ fill those in.

`TST-043` is **partial**: it covers perf budget, cache correctness, conditional requests, and
compression. Its offline-sync invariants need a client application, which this repository does
not contain.

## What the Threshold Gate Does Not Prove

Performance targets are never hardcoded here. Each cites an `NFR-*` row and anchor in
`profiles/_nfr-thresholds.yml`. The gate proves **the citation resolves** — that the row and
anchor exist. It does **not** prove the number matches the NFR document's prose, because that
would mean parsing Markdown for numeric values. A human owns number accuracy; the gate owns the
citation.

## Smoke Mode

In CI, `TST-031` and `TST-035` run in smoke mode: correctness invariants are asserted, and every
performance threshold is recorded `not-evaluated`. A shared CI runner cannot produce meaningful
latency figures, and a green pipeline must never imply that performance was validated. Full-load
runs happen on a dedicated environment via the nightly or manual job.

## Pinned Versions

Resolved from Maven Central on 2026-08-24 (`mvn -q -N help:effective-pom`, plus
`maven-metadata.xml` lookups for each coordinate below — see Task 1's report for the exact
commands). Every subsequent task uses these exact versions; do not float them without updating
this table.

| Tool | Version | Lockfile |
| --- | --- | --- |
| Java (compiler release) | 21 (`maven.compiler.release`, pinned regardless of installed JDK) | `qe-harness/harness/pom.xml` |
| Spring Boot | 3.5.16 (`org.springframework.boot:spring-boot-starter-parent`, latest 3.x GA — pinned to the 3.x line over newer 4.x for its long, unambiguous compatibility record with Resilience4j, Testcontainers, springdoc-openapi, Flyway, and Spring Security resource-server, all needed by later tasks) | `qe-harness/reference-sut/pom.xml` (Task 5) |
| Apache JMeter | 5.6.2 engine (`org.apache.jmeter:ApacheJMeter`) via `jmeter-maven-plugin` 3.8.0's own default (`com.lazerycode.jmeter:jmeter-maven-plugin`) — confirmed against the actually-resolved artifacts in `~/.m2`, not the plugin's own docs, which reference 5.6.3 | `qe-harness/harness/jmeter/pom.xml` (Task 16) |
| Gatling | **3.9.5** engine (`io.gatling.highcharts:gatling-charts-highcharts`), NOT the true latest release (`3.15.1`, re-checked on 2026-09-01 as still Maven Central's `<release>`/`<latest>`) — `karate-gatling:1.4.1` (below) is genuinely BINARY-INCOMPATIBLE with 3.15.1 (confirmed empirically, not merely a dependency-mediation issue: `ProtocolComponentsRegistry.components`'s erased return type changed between the two engine releases, so karate-gatling 1.4.1's compiled bytecode throws `NoSuchMethodError` at the first real Gatling run against 3.15.1, even after fixing a separate, real netty-version-mediation issue along the way — see "Known Issues" below for the full story). 3.9.5 is karate-gatling 1.4.1's own originally-tested transitive default, and is the newest version confirmed by an actual successful load run (`OK=2 KO=0` against the reference SUT, including correctly detecting the `schema-drift` defect). Run via `gatling-maven-plugin` **4.21.11** (`io.gatling:gatling-maven-plugin`) — re-checked on 2026-09-01: Maven Central's `<release>`/`<latest>` (`4.21.10` was current at Task 1's original resolution but a newer patch shipped since); the plugin version is decoupled from the engine version in Gatling's own numbering and 4.21.11 drives the 3.9.5 engine successfully. | `qe-harness/harness/gatling-karate/pom.xml` (Task 20) |
| Karate | 1.4.1 (`com.intuit.karate:karate-junit5` and `com.intuit.karate:karate-gatling`, matched pair) — re-checked on 2026-09-01: still Maven Central's `<release>`/`<latest>` for both artifacts. See "Known Issues" below for a real JDK-version constraint this pairing has on the forked test JVM, and for karate-gatling 1.4.1's own binary-incompatibility ceiling on the Gatling engine version. | `qe-harness/harness/gatling-karate/pom.xml` (Task 20) |
| Testcontainers | 1.21.4 (`org.testcontainers:junit-jupiter`, `org.testcontainers:postgresql`) — resolved from `maven-metadata.xml` on 2026-08-25. The true latest on Maven Central is `2.0.5`, but that major renamed its module artifacts (`org.testcontainers:testcontainers-postgresql`, `org.testcontainers:testcontainers-junit-jupiter`) and was not evaluated for compatibility with the pinned Spring Boot 3.5.16 line. 1.21.4 is the newest version on the old artifact coordinates and is also the exact version Spring Boot 3.5.16's own `spring-boot-dependencies` BOM manages (`testcontainers.version`), so no explicit `<version>` is set in the reference SUT's POM — it comes transitively from the parent, the same pattern already used for `spring-boot-starter-web`/`-test`. | `qe-harness/reference-sut/pom.xml` (Task 6) |
| JJWT | 0.13.0 (`io.jsonwebtoken:jjwt-api`/`-impl`/`-jackson`) — resolved from `maven-metadata.xml` on 2026-08-25 as the current latest release. Not managed by Spring Boot's `spring-boot-dependencies` BOM (unlike `spring-boot-starter-security`, which needs no explicit version), so all three artifacts pin this version explicitly, the same pattern already used for `org.postgresql:postgresql`. | `qe-harness/reference-sut/pom.xml` (Task 9) |
| springdoc-openapi | 2.9.0 (`org.springdoc:springdoc-openapi-starter-webmvc-api`) — resolved from `maven-metadata.xml` on 2026-08-25 as the newest release on the 2.x line; the true latest, `3.1.0`, targets Spring Boot 4 and was not evaluated against this repo's pinned 3.5.16. Not managed by the `spring-boot-dependencies` BOM, so pinned explicitly, same pattern as JJWT. | `qe-harness/reference-sut/pom.xml` (Task 10) |
| json-schema-validator | 1.5.9 (`com.networknt:json-schema-validator`, test scope), NOT the true latest release (`3.0.7`, resolved from `maven-metadata.xml` on 2026-08-25) — `2.0.0` replaced the `JsonSchemaFactory`/`JsonSchema`/`ValidationMessage` API `SchemaCompatibilityTest` needs with an incompatible `Schema`/`Error`-based rewrite (confirmed directly: neither the `3.0.7` nor the `2.0.7` jar contains any of those four classes). `1.5.9` is the newest release still on the 1.x line, and the newest release that actually has the API this test code calls. | `qe-harness/reference-sut/pom.xml` (Task 10) |
| resilience4j | 2.4.0 (`io.github.resilience4j:resilience4j-spring-boot3`) — resolved from `maven-metadata.xml` on 2026-08-25 as the current latest release. Not managed by the `spring-boot-dependencies` BOM, so pinned explicitly, same pattern as JJWT/springdoc. Pulls in `resilience4j-spring6` (the `@CircuitBreaker` annotation + Spring Boot config binding) transitively at the same version. `spring-boot-starter-aop` (needed for the AOP proxy the annotation is woven through) IS managed by the BOM (verified via `mvn -N help:effective-pom`: `3.5.16`), so it carries no explicit version. | `qe-harness/reference-sut/pom.xml` (Task 11) |
| Python (interpreter) | 3.13 (Homebrew `python@3.13`), NOT the true latest available (`3.14`, the default `python3` on PATH on this host) — confirmed empirically that this module's entire pinned dependency set (below) installs and runs cleanly under 3.13, and `bin/run-locust.sh` explicitly prefers an installed 3.13/3.12/3.11 over plain `python3` for exactly this reason (locust's own gevent/greenlet dependency lags behind CPython's very latest release on some platforms). | `qe-harness/harness/locust/` (Task 21) |
| Locust | 2.46.4 (`locust`) — resolved via `pip index versions locust` on 2026-09-01 against a fresh Python 3.13 venv, as the current latest release. | `qe-harness/harness/locust/requirements.txt` (Task 21) |
| jsonschema | 4.26.0 (`jsonschema`) — resolved via `pip index versions jsonschema` on 2026-09-01, current latest release; supports Draft 2020-12, the draft `evidence.schema.json` (Task 2) itself declares. | `qe-harness/harness/locust/requirements.txt` (Task 21) |
| requests | 2.34.2 (`requests`) — resolved via `pip index versions requests` on 2026-09-01, current latest release. | `qe-harness/harness/locust/requirements.txt` (Task 21) |
| psycopg2-binary | 2.9.12 (`psycopg2-binary`) — resolved via `pip index versions psycopg2-binary` on 2026-09-01, current latest release. Used only for `tst_039_recon/recompute.py`'s direct-to-Postgres independent recomputation (the same database `qe-harness/docker-compose.yml`'s `postgres` service publishes to the host, for the same reason `LEDGER_JDBC_URL` already does for the jmeter modules). | `qe-harness/harness/locust/requirements.txt` (Task 21) |
| pytest (Python) | 8.4.2 (`pytest`) — resolved via `pip index versions pytest` on 2026-09-01; the true latest, `9.1.1`, was not evaluated for compatibility with the rest of this pinned set, so the newest 8.x release was chosen instead (same "not the true latest, pinned for a compatibility reason" pattern this table already uses for Gatling/Testcontainers/springdoc/json-schema-validator above). | `qe-harness/harness/locust/requirements.txt` (Task 21) |

## Known Issues

### gatling-karate (TST-030): Karate 1.4.1's `Suite#run` hangs forever on JDK 25

`mvn -pl gatling-karate test` (Task 20, TST-030) hangs **indefinitely, with no error and no
CPU use**, when the JDK actually forking the Surefire test JVM is as new as JDK 25 — confirmed
empirically: identical test code, only the JDK invoking Maven changed, reproduces/fixes this
deterministically. `jstack` on the hung process shows the main thread permanently parked in
`CompletableFuture.join()` inside `com.intuit.karate.Suite#run` (called from
`Runner.Builder#parallel`, which `Tst030ContractRunner` calls directly), with no Karate worker
thread ever scheduled. This is the same shape of problem `run-jmeter.sh` already documents for
JMeter's bundled Groovy vs. too-new a JDK — Karate 1.4.1 (2023) was never tested against a JDK
this new — just surfacing as a silent hang here instead of a loud "Unsupported class file major
version" error, which makes it easy to mistake for the test suite (or the SUT) being stuck.

**JDK 21 is confirmed working.** Point Maven at it, either by launching Maven itself under JDK 21:

    JAVA_HOME="$(/usr/libexec/java_home -v 21)" mvn -pl gatling-karate test

or, if Maven itself must stay on a newer JDK, by overriding the forked test JVM only via the
module's own escape-hatch property (see `qe.gatlingKarate.javaRuntime` in
`gatling-karate/pom.xml`):

    mvn -pl gatling-karate test "-Dqe.gatlingKarate.javaRuntime=$(/usr/libexec/java_home -v 21)/bin/java"

Both `Tst030ContractRunner`'s three tests pass cleanly (`Tests run: 3, Failures: 0, Errors: 0`
— the brief's two given proof tests plus one added clean-SUT baseline; see the module's own
README) under JDK 21; neither the reference SUT nor the harness code itself is
JDK-25-incompatible, only Karate 1.4.1's own `Suite#run` on the JVM actually running the test.
`bin/run-gatling-karate.sh` resolves this automatically (same `/usr/libexec/java_home -v
21`/`17` fallback `bin/run-jmeter.sh` uses for its own, unrelated JDK constraint), so
`./bin/run-module.sh TST-030` needs no manual `JAVA_HOME` juggling.

### gatling-karate (TST-030): karate-gatling 1.4.1 is binary-incompatible with Gatling 3.15.1

Running the actual Gatling load run (`mvn -pl gatling-karate
io.gatling:gatling-maven-plugin:4.21.11:test
-Dgatling.simulationClass=...Tst030Simulation`) surfaced two *separate* problems, only the
first of which a dependency-management fix can paper over:

1. **A real netty-version-mediation conflict** (now fixed, kept fixed even though it stopped
   being strictly necessary once the engine itself was downgraded — see below — because it is
   good hygiene regardless): `karate-junit5`'s transitive `com.linecorp.armeria:armeria`
   pulls `io.netty:netty-transport:4.1.96.Final`, which Maven's nearest-wins mediation
   resolves ahead of Gatling's own `4.2.14.Final`, while several *other* netty-* artifacts
   resolve to `4.2.14.Final` — a genuinely incompatible mixed-version classpath. At runtime
   this produced `NoClassDefFoundError: io/netty/channel/IoOps` inside
   `io.gatling.netty.util.Transports`'s static initializer (`IoOps` exists only in the 4.2.x
   netty-transport jar). **This is the true root cause** — an earlier version of this
   writeup incorrectly blamed suppressed `netty_tcnative` native-library warnings logged just
   before that error (real, but non-fatal noise from an unrelated native-SSL fallback path,
   not OS/arch-specific as first assumed).
2. **A genuine binary incompatibility between karate-gatling 1.4.1 and Gatling 3.15.1**,
   confirmed only after fixing (1): the run still crashed with `NoSuchMethodError:
   'java.lang.Object io.gatling.core.protocol.ProtocolComponentsRegistry.components(...)'`
   inside `com.intuit.karate.gatling.KarateFeatureActionBuilder.build`. Gatling's own
   (internal, non-DSL) `ProtocolComponentsRegistry.components` method's erased return type
   changed between whatever engine version karate-gatling 1.4.1 was built against
   (~3.9.x-4.3.x, per its own reference POM, last published ~2023) and 3.15.1 — karate-gatling
   1.4.1's compiled bytecode still calls the old erasure, and no newer karate-gatling release
   exists on Maven Central to fix this from the other side.

**Resolution:** `gatling-karate/pom.xml` pins the Gatling engine
(`io.gatling.highcharts:gatling-charts-highcharts`) to **3.9.5** — karate-gatling 1.4.1's own
originally-tested pairing — rather than the newer 3.15.1 (see the Pinned Versions table
above). Confirmed by an actual successful load run: both scenarios execute against the real
reference SUT (`POST /v1` and `/v2/transfers`, `OK=2 KO=0`), and with the `schema-drift`
defect active beforehand, the v2 scenario correctly reports `KO` while v1 stays `OK` — the
same defect proof the Karate side demonstrates, now demonstrated on the Gatling side too, with
an `oracle: contract-schema` evidence fragment emitted either way. `gatling-maven-plugin`
itself stays at the newer 4.21.11 (see the Pinned Versions table): the plugin and engine
versions are decoupled in Gatling's own numbering, and 4.21.11 drives the 3.9.5 engine
successfully.

If a future task needs the newer 3.15.1 engine for gatling-karate specifically, check Maven
Central for a `karate-gatling` release newer than 1.4.1 first — bumping the engine alone
without one will reproduce this exact `NoSuchMethodError`.

### Running the Testcontainers-backed tests on a non-Docker-Desktop engine

`SyntheticDataSeederTest` (Task 6) needs a real Docker API endpoint. Testcontainers'
auto-detection assumes Docker Desktop's default socket; on an engine registered as a different
Docker context (e.g. Rancher Desktop), point it at that context's socket explicitly:

    export DOCKER_HOST="$(docker context inspect <context-name> --format '{{.Endpoints.docker.Host}}')"
    export TESTCONTAINERS_RYUK_DISABLED=true   # see below

Without `DOCKER_HOST` set, Testcontainers fails immediately with "Could not find a valid Docker
environment" even though `docker info` succeeds, because it only probes the default Docker
Desktop socket path and context, not whatever `docker context show` currently reports.

`TESTCONTAINERS_RYUK_DISABLED` is needed because Ryuk (Testcontainers' container reaper) bind-mounts
the host's Docker socket into its own container to watch for JVM death; on an engine that runs
containers inside a VM (Rancher Desktop's Lima/QEMU VM, colima, etc.) that host-path bind-mount
doesn't resolve the same way it does for Docker Desktop, and Ryuk's own container fails to start.
Disabling it means containers aren't auto-reaped if the JVM crashes mid-test — acceptable for a
local run, worth reconsidering for a CI runner on the same kind of engine.

Neither of these is set in this repo (both are host/engine-specific, not project config); the
reference SUT's own `pom.xml` does carry one related, engine-agnostic fix: the surefire plugin
sets `-Djava.net.preferIPv4Stack=true`, plus the test itself sets `Flyway`'s `connectRetries`, to
absorb a real observed race on this kind of engine — see the comments in `pom.xml` and
`SyntheticDataSeederTest.java` for the failure mode each one fixes.

### `docker compose down -v` matches nothing without `--profile`

Found during Task 14's compose verification and confirmed deterministically reproducible (not an
environment flake): every service in `qe-harness/docker-compose.yml` is profile-gated -- none is
profile-less. Compose resolves which services `down` applies to the same way it resolves `up`'s
scope: from `--profile`/`COMPOSE_PROFILES`, not from "whatever containers are currently running".
A bare `docker compose down -v` therefore enables zero profiles, matches zero services, and exits
`0` having removed nothing -- indistinguishable from success by exit code alone. Confirmed by
direct A/B: with `qe-harness-postgres-1`/`qe-harness-reference-sut-1` running, a bare `down -v`
left both up (three times in a row); `docker compose --profile core --profile resilience
--profile observability --profile messaging down -v` removed both containers plus the volume and
network on the first try, every time.

`make down` (Task 14) always passes every profile this file declares
(`ALL_PROFILES` in the Makefile) to `down`, so it matches everything `up` could have started
regardless of which `PROFILES` value `up` was originally given. As a last-resort safety net for
any other reason `down` might still leave something behind, it also force-removes anything still
carrying the `com.docker.compose.project=qe-harness` label afterward:

    docker rm -f $(docker ps -aq --filter label=com.docker.compose.project=qe-harness)
    docker volume rm $(docker volume ls -q --filter label=com.docker.compose.project=qe-harness)
    docker network rm $(docker network ls -q --filter label=com.docker.compose.project=qe-harness)

This matters beyond `make down` itself: Task 23's `run-defects.sh` needs a clean SUT between the
baseline and defect-injection runs, and Task 27's CI job needs a clean runner between pipeline
stages -- either calling `docker compose down -v` directly without `--profile` would silently
inherit this same no-op.

## TST-040 Clock-Skew Tolerance

`app.authz.clock-skew-seconds` (`qe-harness/reference-sut/src/main/resources/application.properties`)
is the *declared* tolerance the reference SUT's token validator (`JwtService`, Task 9) is actually
configured with — it is wired straight into JJWT's own `JwtParserBuilder#clockSkewSeconds(...)`, so
the running validator enforces exactly this number, never a different one. `TokenLifecycleTest`'s
`expiredTokenIsNotAcceptedBeyondDeclaredSkew` measures the SUT's real maximum accepted `exp` offset
by presenting progressively staler tokens and asserts that *measured* value against this property —
never the other way around, and never a hardcoded literal in the test — per this corpus's rule that
performance/tolerance thresholds are asserted against declared configuration, not duplicated as
literals. This one is not a performance threshold (it does not gate on latency or throughput), so
unlike the `NFR-*`-cited thresholds elsewhere in this harness it carries no `NFR-*` citation.

Task 19's `harness/jmeter/tst-040-authz` module runs the equivalent measurement out-of-process,
over real HTTP, via a small test-control door (`TokenExpiryTestController`,
`POST /_test/token/expired`) added to `reference-sut` for exactly this purpose — see that
module's own README for why an HTTP door was needed at all (`JwtService#mintExpiredAccessToken`
has no HTTP surface of its own, deliberately) and for the small, latency-driven margin below `5`s
that measurement typically reports.

## TST-035 Circuit Breaker Configuration

`resilience4j.circuitbreaker.instances.downstream.*`
(`qe-harness/reference-sut/src/main/resources/application.yml`) declares the breaker that guards
`DownstreamClient.fetch` (Task 11): `slidingWindowType: COUNT_BASED`, `slidingWindowSize: 5`,
`minimumNumberOfCalls: 5`, `failureRateThreshold: 50`, `waitDurationInOpenState: 2s`,
`permittedNumberOfCallsInHalfOpenState: 2`, `automaticTransitionFromOpenToHalfOpenEnabled: true`.
Same rule as the clock-skew tolerance above: these are resilience configuration, not performance
thresholds, so they carry no `NFR-*` citation, and `BreakerBehaviourTest` never duplicates them as
literals — it reads `minimumNumberOfCalls` straight off the live `CircuitBreakerRegistry` bean and
asserts the *measured* number of calls-to-open against that. `waitDurationInOpenState` is
deliberately short (2s, not a tuned production value) so the breaker-closes-after-fault-removed test
observes a real state transition well within its 20-second budget rather than needing a
test-specific override profile.

On a real downstream failure, `DownstreamClient.fetch`'s Resilience4j `fallbackMethod` returns a
declared `{"degraded": true, "source": "cache"}` response at a plain `200` — `GET /quotes/{id}`
never surfaces a downstream failure as a `5xx`. The `breaker-disabled` defect flag is the one
exception: with it active, the fallback itself rethrows instead of masking the failure, so the
failure propagates as a genuine `500` — this is the capability's proof that it fails for the right
reason. `qe-harness/downstream-stub/` is a small, static-responder Docker image for `docker compose`
(Task 14) to run as a real, network-reachable downstream service; Toxiproxy (also wired in Task 14)
is what actually black-holes/restores traffic to it in that setup — `BreakerBehaviourTest` itself
needs neither, using a hand-rolled in-process JDK `HttpServer` stub instead (see that test's
Javadoc).

## Related

- [TST-001 Test Strategy Standard](../knowledge-base/testing/strategy/test-strategy-standard.md)
- [TST-002 Performance Test Standard](../knowledge-base/testing/strategy/performance-test-standard.md)
- [TST-010 Tool Selection Matrix](../knowledge-base/testing/tooling/tool-selection-matrix.md)
- [Testing Coverage Matrix](../knowledge-base/testing/coverage/coverage-matrix.md)
