#!/usr/bin/env bash
# usage: run-k6.sh <ARCHETYPE> <ABSOLUTE_MODULE_DIR>
#
# Invoked only via run-module.sh's dispatch (never directly by a test or by
# `make run`). <ABSOLUTE_MODULE_DIR> is modules.yml's declared path for this
# archetype (e.g. qe-harness/harness/k6/tst-043-clientexp), containing
# script.js. This tool-runner script did not exist for any prior task --
# run-module.sh resolves "k6" as the basename of this module's own tool
# directory (qe-harness/harness/k6) and execs "bin/run-${tool}.sh" by that
# same convention run-jmeter.sh/run-gatling-karate.sh/run-locust.sh already
# follow, so ./bin/run-module.sh TST-043 cannot work without this file
# existing -- exactly the gap Tasks 20 and 21 each already hit and fixed for
# their own tool.
#
# Unlike every other run-<tool>.sh (a JVM Maven goal, or a Python venv),
# this module is a standalone Node/k6 project entirely outside both the
# Maven reactor and Python -- a third, independent toolchain. It runs in two
# separate processes, not one:
#   1. The real `k6` binary (a Go program, not Node -- see qe-harness/
#      README.md's Pinned Versions table) executes tst-043-clientexp/
#      script.js, which asserts I1-I4 against the real, running SUT and
#      writes a RAW report (check results + measured payload size) to a
#      temp file via its own handleSummary. k6 scripts run inside a
#      sandboxed JS engine (goja) that cannot load ajv -- see harness/k6/
#      emitter.js's own comment for the confirmed, empirical reason -- so
#      this step deliberately does NOT build or schema-validate the
#      evidence fragment itself.
#   2. A plain `node` invocation of harness/k6/write-fragment.js reads that
#      raw report and calls emitFragment (real Node, real ajv) to build and
#      schema-validate the actual fragment, then writes it to
#      traceability/runs/ -- exactly what step 1 could not safely do
#      in-process.
set -euo pipefail

ARCH="${1:?usage: run-k6.sh <ARCHETYPE> <MODULE_DIR>}"
MODULE_DIR="${2:?usage: run-k6.sh <ARCHETYPE> <MODULE_DIR>}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"   # qe-harness/bin
QE_HARNESS_ROOT="$(dirname "$SCRIPT_DIR")"                    # qe-harness
RUNS_DIR="$QE_HARNESS_ROOT/traceability/runs"
THRESHOLDS_FILE="$QE_HARNESS_ROOT/profiles/_nfr-thresholds.yml"

if [ ! -d "$MODULE_DIR" ]; then
    echo "run-k6.sh: no module directory at $MODULE_DIR" >&2
    exit 1
fi
SCRIPT_JS="$MODULE_DIR/script.js"
if [ ! -f "$SCRIPT_JS" ]; then
    echo "run-k6.sh: no script.js under $MODULE_DIR" >&2
    exit 1
fi

if ! command -v k6 >/dev/null 2>&1; then
    echo "run-k6.sh: k6 binary not found on PATH (brew install k6; see" \
         "qe-harness/README.md's Pinned Versions table for the version this" \
         "was built/verified against)" >&2
    exit 1
fi

# MODULE_DIR is this archetype's own subdirectory (tst-043-clientexp/); the
# shared emitter.js/write-fragment.js/package.json/node_modules live one
# level up, at harness/k6/ -- the same "one tool dir, many archetype
# subdirs" layout run-jmeter.sh/run-locust.sh document for their own tools.
K6_DIR="$(dirname "$MODULE_DIR")"

if [ ! -d "$K6_DIR/node_modules" ]; then
    echo "run-k6.sh: installing npm dependencies into $K6_DIR" >&2
    ( cd "$K6_DIR" && npm install --no-audit --no-fund --silent )
fi

mkdir -p "$RUNS_DIR"

# Resolve payload_budget_bytes from profiles/_nfr-thresholds.yml the same
# way ThresholdResolver.java resolves a threshold for the JVM modules:
# never hardcode the number, read it from the one file allowed to declare
# one, and throw loudly if it is missing rather than defaulting. k6's own
# JS runtime (goja) cannot load a YAML-parsing npm package the way a plain
# Node/Python process can, so this resolution happens here, in bash/python,
# BEFORE k6 starts, and the resolved value+citation are handed across the
# process boundary as plain environment variables (the same mechanism
# run-locust.sh/run-jmeter.sh already use to configure their own tool's
# process from this script).
THRESHOLD_JSON="$(python3 - "$THRESHOLDS_FILE" <<'PY'
import sys, yaml, json, pathlib
data = yaml.safe_load(pathlib.Path(sys.argv[1]).read_text())
try:
    entry = next(t for t in data["thresholds"] if t["name"] == "payload_budget_bytes")
except StopIteration:
    sys.exit(
        "run-k6.sh: no 'payload_budget_bytes' entry in " + sys.argv[1] +
        " -- refusing to default a threshold value, add the entry with a real NFR-* citation instead"
    )
print(json.dumps({"value": entry["value"], "threshold_ref": entry["threshold_ref"]}))
PY
)"
export PAYLOAD_BUDGET_BYTES="$(python3 -c "import json,sys; print(json.loads(sys.argv[1])['value'])" "$THRESHOLD_JSON")"
export PAYLOAD_BUDGET_REF="$(python3 -c "import json,sys; print(json.loads(sys.argv[1])['threshold_ref'])" "$THRESHOLD_JSON")"

export SUT_BASE_URL="${SUT_BASE_URL:-http://localhost:8080}"
export QE_ARCHETYPE="$ARCH"
export QE_ENVIRONMENT="${QE_ENVIRONMENT:-local-compose}"

# mktemp -t k6-raw-report only creates /tmp/k6-raw-report.XXXXXX; appending
# ".json" by string concatenation (the previous form of this line) names a
# DIFFERENT path that mktemp never actually created, so the real file leaked
# on every run and the `trap` below removed nothing. GNU mktemp's --suffix
# isn't available in macOS/BSD mktemp, so keep the template itself the only
# thing mktemp needs to see -- k6's own handleSummary writes to whatever
# path K6_RAW_REPORT_PATH names, with no extension requirement of its own.
RAW_REPORT="$(mktemp -t k6-raw-report.XXXXXX)"
trap 'rm -f "$RAW_REPORT"' EXIT
export K6_RAW_REPORT_PATH="$RAW_REPORT"

# Step 1: k6 runs script.js against the real, running SUT and writes the
# raw report (never the schema-validated fragment -- see this file's own
# header comment).
( cd "$K6_DIR" && k6 run --quiet "$SCRIPT_JS" )

if [ ! -s "$RAW_REPORT" ]; then
    echo "run-k6.sh: k6 did not write a raw report to $RAW_REPORT" >&2
    exit 1
fi

# Step 2: a plain Node process builds + schema-validates the real evidence
# fragment from that raw report, and writes it to traceability/runs/.
FRAGMENT_FILE="$(node "$K6_DIR/write-fragment.js" "$RAW_REPORT" "$RUNS_DIR")"

RESULT="$(node -e "const fs=require('fs'); console.log(JSON.parse(fs.readFileSync(process.argv[1],'utf8')).result)" "$FRAGMENT_FILE")"
echo "run-k6.sh: $ARCH -> $RESULT ($FRAGMENT_FILE)"
[ "$RESULT" != "failed" ]
