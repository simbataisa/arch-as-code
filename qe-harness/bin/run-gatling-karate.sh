#!/usr/bin/env bash
# usage: run-gatling-karate.sh <ARCHETYPE> <ABSOLUTE_MODULE_DIR>
#
# Invoked only via run-module.sh's dispatch (never directly by a test or by
# `make run`). <ABSOLUTE_MODULE_DIR> is modules.yml's declared path for this
# archetype (e.g. qe-harness/harness/gatling-karate/tst-030-contract) --
# accepted for the same calling convention every run-<tool>.sh script
# shares, but unlike run-jmeter.sh's per-plan.jmx scoping this module has
# exactly one Maven module and one JUnit test class total, so MODULE_DIR
# itself scopes nothing here; it is only checked for existence.
#
# Runs BOTH halves of TST-030's dual-runner proof against the SUT's
# CURRENT defect state. This script never toggles a defect itself -- that
# is the JUnit test suite's own job (Tst030ContractRunner's
# featureFailsAgainstTheSchemaDriftDefect activates/clears schema-drift
# over HTTP around itself), exercised only via a plain `mvn test`, a
# separate path from this script, exactly mirroring how ModuleRunner.java
# (not run-jmeter.sh) owns defect toggling for the jmeter modules:
#   1. The Karate functional check (Tst030ContractRunner's own
#      passesAgainstTheCleanSut) -- narrowed to exactly this one test
#      method via -Dtest so this script's fragment lookup below is
#      unambiguous (the schema-drift proof test, which only runs under a
#      plain `mvn test`, would otherwise also fire and leave its own
#      FAILED fragment as the "newest" one). This also seeds the two
#      fixture accounts (see that test's own @BeforeAll) step 2 depends on.
#   2. The Gatling load run (Tst030Simulation), invoked directly via
#      gatling-maven-plugin (see gatling-karate/pom.xml's own comment for
#      why it is registered without a lifecycle binding).
# Each writes its own oracle: contract-schema evidence fragment; this
# script fails if EITHER result is "failed".
set -euo pipefail

ARCH="${1:?usage: run-gatling-karate.sh <ARCHETYPE> <MODULE_DIR>}"
MODULE_DIR="${2:?usage: run-gatling-karate.sh <ARCHETYPE> <MODULE_DIR>}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"   # qe-harness/bin
QE_HARNESS_ROOT="$(dirname "$SCRIPT_DIR")"                    # qe-harness
HARNESS_POM="$QE_HARNESS_ROOT/harness/pom.xml"
RUNS_DIR="$QE_HARNESS_ROOT/traceability/runs"
GATLING_MAVEN_PLUGIN_VERSION="4.21.11"   # keep in sync with gatling-karate/pom.xml's own comment/README's Pinned Versions table

if [ ! -d "$MODULE_DIR" ]; then
    echo "run-gatling-karate.sh: no module directory at $MODULE_DIR" >&2
    exit 1
fi

mkdir -p "$RUNS_DIR"

# qe-harness-common (RunFragment/EvidenceEmitter) resolves as a test-scope
# dependency from the LOCAL repo, not the reactor -- same requirement (and
# same fix) as run-jmeter.sh's own install-first step. Fast/no-op once
# already cached from a previous run.
mvn -q -f "$HARNESS_POM" -N install
mvn -q -f "$HARNESS_POM" -pl common install

export SUT_BASE_URL="${SUT_BASE_URL:-http://localhost:8080}"
export LEDGER_JDBC_URL="${LEDGER_JDBC_URL:-jdbc:postgresql://localhost:5432/sut}"
export LEDGER_JDBC_USER="${LEDGER_JDBC_USER:-sut}"
export LEDGER_JDBC_PASSWORD="${LEDGER_JDBC_PASSWORD:-sut}"
export EVIDENCE_OUTPUT_DIR="$RUNS_DIR"
export QE_ARCHETYPE="$ARCH"

# Karate 1.4.1's com.intuit.karate.Suite#run hangs indefinitely -- zero CPU,
# no exception -- when the JVM actually running it is JDK 25 (see
# gatling-karate/pom.xml's qe.gatlingKarate.javaRuntime property and
# qe-harness/README.md's "Running the gatling-karate module's tests"
# section for the full writeup: confirmed via jstack, the main thread sits
# permanently parked in CompletableFuture.join() with no Karate worker
# thread ever scheduled). This is the exact same shape of problem
# run-jmeter.sh already works around for JMeter's bundled Groovy vs. a too-
# new JDK, just surfacing as a silent hang here rather than a loud
# "Unsupported class file major version" error. Unlike jmeter-maven-plugin
# (one javaRuntime config knob covering its single forked JVM), this
# module forks a JVM in at least two different ways across its two steps
# below (Surefire for step 1, gatling-maven-plugin's own ForkMain for step
# 2) with no single shared plugin property covering both -- so, exactly
# like run-jmeter.sh, resolve an installed JDK 21 or 17 via
# /usr/libexec/java_home and point JAVA_HOME/PATH at it for this whole
# script's `mvn` invocations, which covers every fork uniformly.
GATLING_KARATE_JAVA_HOME=""
if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    for candidate_version in 21 17; do
        candidate_home="$(/usr/libexec/java_home -v "$candidate_version" 2>/dev/null || true)"
        if [ -n "$candidate_home" ] && [ -x "$candidate_home/bin/java" ]; then
            GATLING_KARATE_JAVA_HOME="$candidate_home"
            break
        fi
    done
fi
if [ -n "$GATLING_KARATE_JAVA_HOME" ]; then
    export JAVA_HOME="$GATLING_KARATE_JAVA_HOME"
    export PATH="$JAVA_HOME/bin:$PATH"
else
    echo "run-gatling-karate.sh: WARNING: no JDK 21/17 found via /usr/libexec/java_home;" \
         "falling back to whatever JVM is already on PATH, which may be too new for" \
         "Karate 1.4.1's Suite#run (hangs indefinitely, with no error at all)" >&2
fi

# Step 1: Karate functional check against the SUT's current state.
mvn -f "$HARNESS_POM" -pl gatling-karate test \
    -Dtest=Tst030ContractRunner#passesAgainstTheCleanSut

KARATE_FRAGMENT="$(ls -t "$RUNS_DIR"/*-"$ARCH".json 2>/dev/null | head -n1 || true)"
if [ -z "$KARATE_FRAGMENT" ]; then
    echo "run-gatling-karate.sh: no evidence fragment written for $ARCH's Karate run under $RUNS_DIR" >&2
    exit 1
fi
KARATE_RESULT="$(python3 -c "import json,sys; print(json.load(open(sys.argv[1]))['result'])" "$KARATE_FRAGMENT")"
echo "run-gatling-karate.sh: $ARCH (karate)  -> $KARATE_RESULT ($KARATE_FRAGMENT)"

# Step 2: Gatling load run, driving the SAME shared .feature file via
# karateFeature(...) -- see Tst030Simulation.scala. Depends on step 1
# having already seeded the fixture accounts (Tst030ContractRunner's own
# @BeforeAll); this script always runs step 1 first for exactly that
# reason.
mvn -f "$HARNESS_POM" -pl gatling-karate \
    "io.gatling:gatling-maven-plugin:${GATLING_MAVEN_PLUGIN_VERSION}:test" \
    "-Dgatling.simulationClass=com.techcombank.qe.harness.gatlingkarate.Tst030Simulation"

GATLING_FRAGMENT="$(ls -t "$RUNS_DIR"/*-"$ARCH".json 2>/dev/null | head -n1 || true)"
if [ -z "$GATLING_FRAGMENT" ] || [ "$GATLING_FRAGMENT" = "$KARATE_FRAGMENT" ]; then
    echo "run-gatling-karate.sh: no NEW evidence fragment written for $ARCH's Gatling run under $RUNS_DIR" >&2
    exit 1
fi
GATLING_RESULT="$(python3 -c "import json,sys; print(json.load(open(sys.argv[1]))['result'])" "$GATLING_FRAGMENT")"
echo "run-gatling-karate.sh: $ARCH (gatling) -> $GATLING_RESULT ($GATLING_FRAGMENT)"

[ "$KARATE_RESULT" != "failed" ] && [ "$GATLING_RESULT" != "failed" ]
