package com.techcombank.qe.harness.gatlingkarate

import com.intuit.karate.gatling.PreDef._
import com.techcombank.qe.harness.evidence.{EvidenceEmitter, RunFragment}
import io.gatling.core.Predef._

import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

/**
 * TST-030 contract compatibility module (Task 20): the Gatling side of
 * TST-012's headline claim, that one .feature file drives both a
 * functional run and a performance run. `karateFeature` below points at
 * the EXACT same classpath resource com/techcombank/qe/harness/gatlingkarate's
 * own `Tst030ContractRunner` (a plain Karate JUnit runner, in
 * src/test/java alongside this file) drives -- not a duplicated or
 * hand-copied scenario. `Tst030ContractRunner#sameFeatureDrivesTheGatlingSimulation`
 * asserts this literal path appears in this very file, which is what stops
 * the two runners from silently drifting into two separate suites.
 *
 * Run directly (not bound to `mvn test` -- see this module's pom.xml, the
 * same "no lifecycle binding" choice the jmeter module makes for exactly
 * the same reason: a plain `mvn test` must never also fire a real load run
 * against whatever SUT happens to be reachable):
 *
 * {{{
 * mvn -pl gatling-karate io.gatling:gatling-maven-plugin:4.21.11:test \
 *     "-Dgatling.simulationClass=com.techcombank.qe.harness.gatlingkarate.Tst030Simulation"
 * }}}
 *
 * Verified actually completing end-to-end (JDK 21): both scenarios run
 * against the real reference SUT (POST /v1 and /v2/transfers, OK=2 KO=0),
 * and with the `schema-drift` defect active beforehand (`POST
 * /_test/defect/schema-drift`), the v2 scenario correctly reports KO while
 * v1 stays OK -- the same defect proof
 * Tst030ContractRunner#featureFailsAgainstTheSchemaDriftDefect demonstrates
 * on the Karate side. Getting there required pinning the Gatling engine
 * (gatling-charts-highcharts, this module's pom.xml) to 3.9.5 rather than
 * the newer 3.15.1: karate-gatling 1.4.1 is genuinely BINARY-INCOMPATIBLE
 * with 3.15.1 (a NoSuchMethodError in Gatling's own internal
 * ProtocolComponentsRegistry.components, not fixable via a dependency
 * exclusion or version override) -- see gatling-karate/pom.xml's own long
 * comment beside the karate-gatling dependency, and
 * qe-harness/README.md's "Known Issues" section, for the full writeup.
 */
class Tst030Simulation extends Simulation {

  private val totalIterations = new AtomicInteger(0)
  private val failedIterations = new AtomicInteger(0)

  private val protocol = karateProtocol()

  private val contractScenario = scenario("TST-030 transfer contract compatibility")
    .exec(karateFeature("classpath:tst-030-contract/transfer-contract.feature"))
    .exec(session => {
      totalIterations.incrementAndGet()
      if (session.isFailed) failedIterations.incrementAndGet()
      session
    })

  setUp(contractScenario.inject(atOnceUsers(1))).protocols(protocol)

  after {
    // Same SUT_BASE_URL/EVIDENCE_OUTPUT_DIR convention every other harness
    // module uses (see ModuleRunner.java and run-jmeter.sh): plain process
    // environment variables inherited by whichever JVM gatling-maven-plugin
    // forks to actually run this simulation.
    val outputDir = Path.of(
      Option(System.getenv("EVIDENCE_OUTPUT_DIR")).getOrElse("../../traceability/runs")
    )

    val total = totalIterations.get()
    val failed = failedIterations.get()
    val scenarioResult =
      if (failed > 0) RunFragment.Result.FAILED
      else if (total > 0) RunFragment.Result.PASSED
      else RunFragment.Result.NOT_EVALUATED

    val fragment = RunFragment.builder()
      .archetype("TST-030")
      .module("gatling-karate")
      .serviceName("reference-sut")
      .tier("T0")
      .oracle("contract-schema")
      .environment(Option(System.getenv("QE_ENVIRONMENT")).getOrElse("local-compose"))
      .sutDefect(System.getenv("QE_SUT_DEFECT"))
      .invariant(
        // evidence.schema.json requires every invariant id to match ^I[0-9]+$ --
        // "perf-contract" (this field's previous form) never actually satisfied
        // that, the same defect Tst030ContractRunner.scenarioId() had (see its
        // own comment) until validate-harness-coverage.py's check 7 (I3) started
        // validating real fragments against the real schema.
        "I1",
        "Gatling load run drives the shared transfer-contract.feature via karateFeature without a scenario failure",
        scenarioResult
      )
      .build()

    new EvidenceEmitter(outputDir).emit(fragment)
  }
}
