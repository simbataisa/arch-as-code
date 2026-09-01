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
 * KNOWN ISSUE (see gatling-karate/pom.xml's own longer note beside the
 * gatling-maven-plugin declaration): on this task's dev environment
 * (macOS/aarch64, JDK 21) that command fails with a netty
 * NoClassDefFoundError before any scenario runs -- a forked-classpath /
 * native-library issue, not a bug in this class. This class's own
 * karateFeature(...) call and compilation are independently verified by
 * Tst030ContractRunner#sameFeatureDrivesTheGatlingSimulation and by
 * `mvn -pl gatling-karate test-compile` succeeding.
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
      .sutDefect(System.getenv("SUT_DEFECT"))
      .invariant(
        "perf-contract",
        "Gatling load run drives the shared transfer-contract.feature via karateFeature without a scenario failure",
        scenarioResult
      )
      .build()

    new EvidenceEmitter(outputDir).emit(fragment)
  }
}
