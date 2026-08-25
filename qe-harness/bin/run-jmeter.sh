#!/usr/bin/env bash
# usage: run-jmeter.sh <ARCHETYPE> <ABSOLUTE_MODULE_DIR>
#
# Invoked only via run-module.sh's dispatch (never directly by a test or by
# `make run`). <ABSOLUTE_MODULE_DIR> is one JMeter archetype module's own
# directory (e.g. qe-harness/harness/jmeter/tst-021-ledger), containing
# plan.jmx and assert-*.groovy.
#
# Runs jmeter-maven-plugin's `configure` and `jmeter` goals as DIRECT goal
# invocations (full groupId:artifactId:version:goal coordinates) rather than
# lifecycle-bound <executions> in harness/jmeter/pom.xml -- see that pom's
# module-level comment for why: binding them to a lifecycle phase would make
# `mvn -pl jmeter test` (the JUnit-test command every Tst0NN-ModuleTest
# uses) ALSO trigger a real JMeter run, duplicating and racing the one this
# script itself starts.
#
# `-D` overrides the qe.jmeter.* properties harness/jmeter/pom.xml's plugin
# <configuration> references, scoping this invocation to exactly one
# module's plan.jmx instead of jmeter-maven-plugin's default directory scan
# picking up every archetype module under harness/jmeter/ (only this one
# exists yet, but Tasks 17-19 add siblings, e.g. tst-031-ratelimit/plan.jmx,
# under the same Maven module -- a defect-injection run against TST-021 must
# never also fire TST-031/TST-035/TST-040's plans against the same
# defect-active SUT).
set -euo pipefail

ARCH="${1:?usage: run-jmeter.sh <ARCHETYPE> <MODULE_DIR>}"
MODULE_DIR="${2:?usage: run-jmeter.sh <ARCHETYPE> <MODULE_DIR>}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"   # qe-harness/bin
QE_HARNESS_ROOT="$(dirname "$SCRIPT_DIR")"                    # qe-harness
HARNESS_POM="$QE_HARNESS_ROOT/harness/pom.xml"
RUNS_DIR="$QE_HARNESS_ROOT/traceability/runs"

PLAN_FILE="$MODULE_DIR/plan.jmx"
if [ ! -f "$PLAN_FILE" ]; then
    echo "run-jmeter.sh: no plan.jmx under $MODULE_DIR" >&2
    exit 1
fi
# Every JMeter archetype module names its assertion script differently
# (this module's is assert-trial-balance.groovy; TST-031/TST-035/TST-040 --
# Tasks 17-19 -- will each ship their own assert-*.groovy under this same
# shared script). Glob rather than hardcode a filename here, or this
# dispatcher breaks the moment a second module exists.
ASSERT_SCRIPTS=("$MODULE_DIR"/assert-*.groovy)
if [ ! -f "${ASSERT_SCRIPTS[0]}" ]; then
    echo "run-jmeter.sh: no assert-*.groovy under $MODULE_DIR" >&2
    exit 1
fi
if [ "${#ASSERT_SCRIPTS[@]}" -gt 1 ]; then
    echo "run-jmeter.sh: expected exactly one assert-*.groovy under $MODULE_DIR, found ${#ASSERT_SCRIPTS[@]}: ${ASSERT_SCRIPTS[*]}" >&2
    exit 1
fi
ASSERT_SCRIPT="${ASSERT_SCRIPTS[0]}"

mkdir -p "$RUNS_DIR"

# jmeter-maven-plugin's testPlanLibraries (qe-harness-common + its Jackson/
# postgres deps) resolve via Aether from the LOCAL repo, not the reactor --
# make sure the reactor's own jars are installed there first. Fast/no-op
# once already cached from a previous run.
mvn -q -f "$HARNESS_POM" -N install
mvn -q -f "$HARNESS_POM" -pl common install

# Configuration handed to the plan/assertion script. Plain process
# environment variables, not JMeter -J properties: the plugin runs JMeter
# in-process inside this same forked Maven JVM, so System.getenv() in the
# JSR223 assertion script sees these directly -- no jmeter-maven-plugin
# <propertiesUser> map-config indirection needed.
export SUT_BASE_URL="${SUT_BASE_URL:-http://localhost:8080}"
export LEDGER_JDBC_URL="${LEDGER_JDBC_URL:-jdbc:postgresql://localhost:5432/sut}"
export LEDGER_JDBC_USER="${LEDGER_JDBC_USER:-sut}"
export LEDGER_JDBC_PASSWORD="${LEDGER_JDBC_PASSWORD:-sut}"
export EVIDENCE_OUTPUT_DIR="$RUNS_DIR"
export QE_ARCHETYPE="$ARCH"
export ASSERT_SCRIPT_PATH="$ASSERT_SCRIPT"

# A direct (unbound) goal invocation gets Maven's synthetic "default-cli"
# executionId. `configure` writes its generated JMeter installation/config
# under that id in target/config.json; `jmeter` (like `results`) looks it
# up via -DselectConfiguration, which otherwise defaults to "configuration"
# (the id the plugin's own README examples use for a POM-bound <execution>)
# -- without this override the two goals silently fail to find each
# other's config ("No results for path: $[0]").
# JMeter 5.6.2 (what jmeter-maven-plugin 3.8.0 actually resolves) bundles
# Groovy 3.0.17, whose ASM-based compiler cannot read class files newer
# than roughly Java 17/18. Every JSR223 element in plan.jmx fails
# immediately with "Unsupported class file major version 69" if the forked
# JMeter JVM itself runs on a JDK as new as 25 (confirmed empirically).
# Prefer an installed JDK 21, then 17, over whatever `java` resolves to on
# PATH; fall back to plain "java" if neither is found (matches
# harness/jmeter/pom.xml's own qe.jmeter.javaRuntime default).
JMETER_JAVA_RUNTIME="java"
if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    for candidate_version in 21 17; do
        candidate_home="$(/usr/libexec/java_home -v "$candidate_version" 2>/dev/null || true)"
        if [ -n "$candidate_home" ] && [ -x "$candidate_home/bin/java" ]; then
            JMETER_JAVA_RUNTIME="$candidate_home/bin/java"
            break
        fi
    done
fi
if [ "$JMETER_JAVA_RUNTIME" = "java" ]; then
    echo "run-jmeter.sh: WARNING: no JDK 21/17 found via /usr/libexec/java_home;" \
         "falling back to 'java' on PATH, which may be too new for JMeter's bundled Groovy" >&2
fi

mvn -q -f "$HARNESS_POM" -pl jmeter \
    com.lazerycode.jmeter:jmeter-maven-plugin:3.8.0:configure \
    com.lazerycode.jmeter:jmeter-maven-plugin:3.8.0:jmeter \
    "-Dqe.jmeter.testFilesDirectory=$MODULE_DIR" \
    -Dqe.jmeter.testFilesIncluded=plan.jmx \
    -DselectConfiguration=default-cli \
    "-Dqe.jmeter.javaRuntime=$JMETER_JAVA_RUNTIME"

# The fragment assert-trial-balance.groovy just wrote is the source of
# truth for pass/fail -- not jmeter-maven-plugin's own exit code, which
# reflects only "did the JMeter JVM run without crashing", not "did every
# invariant hold". Re-read it to decide this script's own exit status.
FRAGMENT_FILE="$(ls -t "$RUNS_DIR"/*-"$ARCH".json 2>/dev/null | head -n1 || true)"
if [ -z "$FRAGMENT_FILE" ]; then
    echo "run-jmeter.sh: no evidence fragment written for $ARCH under $RUNS_DIR" >&2
    exit 1
fi

RESULT="$(python3 -c "import json,sys; print(json.load(open(sys.argv[1]))['result'])" "$FRAGMENT_FILE")"
echo "run-jmeter.sh: $ARCH -> $RESULT ($FRAGMENT_FILE)"
[ "$RESULT" != "failed" ]
