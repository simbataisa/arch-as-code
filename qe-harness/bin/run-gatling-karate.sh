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
# Runs TST-030's dual-runner proof against the SUT's CURRENT defect state.
# This script never toggles a defect on the SUT itself -- that is
# run-defects.sh's job (POST/DELETE against /_test/defect/{flag}) and/or
# Tst030ContractRunner's own job for its dedicated defect-mode test method
# (which activates/clears schema-drift over HTTP around itself) -- exactly
# mirroring how ModuleRunner.java (not run-jmeter.sh) owns defect toggling
# for the jmeter modules. What DOES branch here is which JUnit test
# method(s) get invoked, based on whether QE_SUT_DEFECT is set in this
# script's own environment (run-defects.sh exports it right before calling
# run-module.sh, so every module -- this one included -- can tell a
# defect-proof invocation apart from an ordinary one):
#   - QE_SUT_DEFECT set (a run-defects.sh invocation): run ONLY
#     Tst030ContractRunner#featureFailsAgainstTheSchemaDriftDefect, which
#     proves the contract genuinely breaks under that defect and tags its
#     own fragment's evidence.sut_defect accordingly. The Gatling load step
#     does not run in this branch (see the code below for why).
#   - QE_SUT_DEFECT unset (the ordinary path): run BOTH halves --
#     1. The Karate functional check (Tst030ContractRunner's own
#        passesAgainstTheCleanSut) -- narrowed to exactly this one test
#        method via -Dtest so this script's fragment lookup below is
#        unambiguous. This also seeds the two fixture accounts (see that
#        test's own @BeforeAll) step 2 depends on.
#     2. The Gatling load run (Tst030Simulation), invoked directly via
#        gatling-maven-plugin (see gatling-karate/pom.xml's own comment for
#        why it is registered without a lifecycle binding).
#     Each writes its own oracle: contract-schema evidence fragment; this
#     script fails if EITHER result is "failed".
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

# Bound every fragment lookup below to files written no earlier than a
# sentinel captured right before the mvn invocation that is supposed to
# write it -- `traceability/runs/` accumulates every fragment any module
# has ever written, so "the newest *-<ARCH>.json in the whole directory"
# (the previous form of these lookups) would silently resolve to a PRIOR
# run's fragment if a step ever completed without writing one of its own. A
# step that produced no fragment must fail loudly instead of reporting
# someone else's old result as its own. `find -newer` (not a timestamp
# string) works identically under GNU and BSD find with no clock-format
# portability concerns.
SENTINEL_KARATE=""
SENTINEL_GATLING=""
cleanup_sentinels() { rm -f "$SENTINEL_KARATE" "$SENTINEL_GATLING"; }
trap cleanup_sentinels EXIT

if [ -n "${QE_SUT_DEFECT:-}" ]; then
    # Defect-proof run, invoked by run-defects.sh: the SUT already has
    # QE_SUT_DEFECT's flag active (POSTed to /_test/defect/{flag} before
    # run-module.sh was ever called). Running passesAgainstTheCleanSut here
    # (this script's normal step 1) would incorrectly assert "the clean SUT
    # must satisfy both contract scenarios" against a SUT that is
    # deliberately not clean -- Tst030ContractRunner's own dedicated
    # defect-mode test, featureFailsAgainstTheSchemaDriftDefect, is the
    # correct thing to run instead: it (re-)activates this exact defect
    # around itself, asserts the contract genuinely breaks, and clears the
    # defect again in its own `finally` -- and (see that class's own
    # comment) it now emits its fragment before asserting, so a JUnit
    # assertion failure can never suppress the evidence the way it used to
    # (M9). The Gatling load step never runs in this branch:
    # Tst030Simulation.scala has no defect-awareness of its own, and by the
    # time it would run, the defect test's own `finally` has already
    # cleared schema-drift -- running it anyway would just leave an
    # unrelated clean "passed" fragment on disk, unused by this script but
    # available to confuse a later merge-fragments.py run.
    SENTINEL_KARATE="$(mktemp "$RUNS_DIR/.fragment-sentinel.XXXXXX")"
    mvn -f "$HARNESS_POM" -pl gatling-karate test \
        -Dtest=Tst030ContractRunner#featureFailsAgainstTheSchemaDriftDefect

    KARATE_FRAGMENT="$(find "$RUNS_DIR" -maxdepth 1 -name "*-$ARCH.json" -newer "$SENTINEL_KARATE" 2>/dev/null | sort | tail -n1)"
    if [ -z "$KARATE_FRAGMENT" ]; then
        echo "run-gatling-karate.sh: no evidence fragment written for $ARCH's defect-proof Karate run under $RUNS_DIR since this run started" >&2
        exit 1
    fi
    KARATE_RESULT="$(python3 -c "import json,sys; print(json.load(open(sys.argv[1]))['result'])" "$KARATE_FRAGMENT")"
    echo "run-gatling-karate.sh: $ARCH (karate, defect=$QE_SUT_DEFECT) -> $KARATE_RESULT ($KARATE_FRAGMENT)"

    [ "$KARATE_RESULT" != "failed" ]
else
    # Step 1: Karate functional check against the SUT's current (clean)
    # state -- narrowed to exactly this one test method via -Dtest so the
    # fragment lookup below is unambiguous. This also seeds the two fixture
    # accounts (see that test's own @BeforeAll) step 2 depends on.
    SENTINEL_KARATE="$(mktemp "$RUNS_DIR/.fragment-sentinel.XXXXXX")"
    mvn -f "$HARNESS_POM" -pl gatling-karate test \
        -Dtest=Tst030ContractRunner#passesAgainstTheCleanSut

    KARATE_FRAGMENT="$(find "$RUNS_DIR" -maxdepth 1 -name "*-$ARCH.json" -newer "$SENTINEL_KARATE" 2>/dev/null | sort | tail -n1)"
    if [ -z "$KARATE_FRAGMENT" ]; then
        echo "run-gatling-karate.sh: no evidence fragment written for $ARCH's Karate run under $RUNS_DIR since this run started" >&2
        exit 1
    fi
    KARATE_RESULT="$(python3 -c "import json,sys; print(json.load(open(sys.argv[1]))['result'])" "$KARATE_FRAGMENT")"
    echo "run-gatling-karate.sh: $ARCH (karate)  -> $KARATE_RESULT ($KARATE_FRAGMENT)"

    # Step 2: Gatling load run, driving the SAME shared .feature file via
    # karateFeature(...) -- see Tst030Simulation.scala. Depends on step 1
    # having already seeded the fixture accounts; this script always runs
    # step 1 first for exactly that reason.
    SENTINEL_GATLING="$(mktemp "$RUNS_DIR/.fragment-sentinel.XXXXXX")"
    mvn -f "$HARNESS_POM" -pl gatling-karate \
        "io.gatling:gatling-maven-plugin:${GATLING_MAVEN_PLUGIN_VERSION}:test" \
        "-Dgatling.simulationClass=com.techcombank.qe.harness.gatlingkarate.Tst030Simulation"

    GATLING_FRAGMENT="$(find "$RUNS_DIR" -maxdepth 1 -name "*-$ARCH.json" -newer "$SENTINEL_GATLING" 2>/dev/null | sort | tail -n1)"
    if [ -z "$GATLING_FRAGMENT" ]; then
        echo "run-gatling-karate.sh: no evidence fragment written for $ARCH's Gatling run under $RUNS_DIR since this run started" >&2
        exit 1
    fi
    GATLING_RESULT="$(python3 -c "import json,sys; print(json.load(open(sys.argv[1]))['result'])" "$GATLING_FRAGMENT")"
    echo "run-gatling-karate.sh: $ARCH (gatling) -> $GATLING_RESULT ($GATLING_FRAGMENT)"

    [ "$KARATE_RESULT" != "failed" ] && [ "$GATLING_RESULT" != "failed" ]
fi
