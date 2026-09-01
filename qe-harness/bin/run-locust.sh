#!/usr/bin/env bash
# usage: run-locust.sh <ARCHETYPE> <ABSOLUTE_MODULE_DIR>
#
# Invoked only via run-module.sh's dispatch (never directly by a test or by
# `make run`). <ABSOLUTE_MODULE_DIR> is modules.yml's declared path for this
# archetype (e.g. qe-harness/harness/locust/tst_039_recon), containing
# locustfile.py + recompute.py. This tool-runner script did not exist for
# any prior task -- run-module.sh resolves "locust" as the basename of this
# module's own tool directory (qe-harness/harness/locust) and execs
# "bin/run-${tool}.sh" by that same convention run-jmeter.sh/
# run-gatling-karate.sh already follow, so ./bin/run-module.sh TST-039
# cannot work without this file existing.
#
# Unlike run-jmeter.sh/run-gatling-karate.sh (Maven goal invocations against
# an already-built JVM reactor), this module is a standalone Python project
# entirely outside the Maven reactor -- see qe-harness/harness/locust/
# pyproject.toml/requirements.txt. This script owns creating/reusing a venv
# for it (qe-harness/harness/locust/.venv, .gitignore'd) and running the
# pinned dependency set from requirements.txt into it, exactly analogous to
# run-jmeter.sh's `mvn -N install` / `mvn -pl common install` bootstrap step
# for the JVM modules' shared dependency.
set -euo pipefail

ARCH="${1:?usage: run-locust.sh <ARCHETYPE> <MODULE_DIR>}"
MODULE_DIR="${2:?usage: run-locust.sh <ARCHETYPE> <MODULE_DIR>}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"   # qe-harness/bin
QE_HARNESS_ROOT="$(dirname "$SCRIPT_DIR")"                    # qe-harness
RUNS_DIR="$QE_HARNESS_ROOT/traceability/runs"

if [ ! -d "$MODULE_DIR" ]; then
    echo "run-locust.sh: no module directory at $MODULE_DIR" >&2
    exit 1
fi
LOCUSTFILE="$MODULE_DIR/locustfile.py"
if [ ! -f "$LOCUSTFILE" ]; then
    echo "run-locust.sh: no locustfile.py under $MODULE_DIR" >&2
    exit 1
fi

# MODULE_DIR is this archetype's own subdirectory (tst_039_recon/); the
# shared emitter.py/requirements.txt/venv live one level up, at
# harness/locust/ -- the same "one tool dir, many archetype subdirs" layout
# run-jmeter.sh's own comment describes for harness/jmeter/.
LOCUST_DIR="$(dirname "$MODULE_DIR")"
VENV_DIR="$LOCUST_DIR/.venv"
REQUIREMENTS_FILE="$LOCUST_DIR/requirements.txt"

# Resolve a real Python 3 interpreter rather than trusting whatever `python3`
# happens to be first on PATH: Homebrew's default python3 on this class of
# host resolves to the very latest CPython (3.14 at the time this script was
# written), which is newer than locust's own gevent/greenlet dependency has
# published wheels for on some platforms. python3.13 (confirmed installed
# via Homebrew and confirmed, empirically, to install/run this module's
# entire pinned dependency set cleanly) is preferred when present; fall back
# to plain python3 otherwise.
PYTHON_BIN="python3"
for candidate in python3.13 python3.12 python3.11; do
    if command -v "$candidate" >/dev/null 2>&1; then
        PYTHON_BIN="$candidate"
        break
    fi
done

if [ ! -x "$VENV_DIR/bin/python" ]; then
    echo "run-locust.sh: creating venv at $VENV_DIR with $PYTHON_BIN" >&2
    "$PYTHON_BIN" -m venv "$VENV_DIR"
fi

# Cheap idempotency check so a re-run doesn't reinstall on every invocation:
# a stamp file records the requirements.txt content hash it was last
# installed from.
STAMP_FILE="$VENV_DIR/.requirements.sha256"
REQUIREMENTS_HASH="$(shasum -a 256 "$REQUIREMENTS_FILE" | awk '{print $1}')"
if [ ! -f "$STAMP_FILE" ] || [ "$(cat "$STAMP_FILE")" != "$REQUIREMENTS_HASH" ]; then
    echo "run-locust.sh: installing $REQUIREMENTS_FILE into $VENV_DIR" >&2
    "$VENV_DIR/bin/pip" install -q --upgrade pip
    "$VENV_DIR/bin/pip" install -q -r "$REQUIREMENTS_FILE"
    echo "$REQUIREMENTS_HASH" > "$STAMP_FILE"
fi

mkdir -p "$RUNS_DIR"

# Configuration handed to locustfile.py's on_test_start/ReconUser/on_test_stop
# via plain process environment variables -- same mechanism (and same
# variable names, where they overlap) as run-jmeter.sh/run-gatling-karate.sh
# use for their own Groovy/Java code, so a defect-injection run's shell
# invocation looks identical regardless of which tool actually runs it.
export SUT_BASE_URL="${SUT_BASE_URL:-http://localhost:8080}"
# Standard libpq environment variables -- psycopg2.connect() with no
# arguments reads these directly (recompute.py's own connect()). Same
# postgres/5432/sut/sut/sut defaults run-jmeter.sh's LEDGER_JDBC_* variables
# use, since both connect to the exact same qe-harness/docker-compose.yml
# `postgres` service, published to the host on the same port for the same
# reason (see that file's own comment on why).
export PGHOST="${PGHOST:-localhost}"
export PGPORT="${PGPORT:-5432}"
export PGDATABASE="${PGDATABASE:-sut}"
export PGUSER="${PGUSER:-sut}"
export PGPASSWORD="${PGPASSWORD:-sut}"
export EVIDENCE_OUTPUT_DIR="$RUNS_DIR"
export QE_ARCHETYPE="$ARCH"
export QE_ENVIRONMENT="${QE_ENVIRONMENT:-local-compose}"

# Headless: a handful of concurrent simulated users repeatedly polling
# GET /recon/report for a short, fixed duration -- long enough for several
# concurrent polls per user, short enough to keep a manual/CI invocation
# fast. --host wires HttpUser.client's base URL (ReconUser's self.client
# calls) to the exact same reference-sut $SUT_BASE_URL every other module
# targets.
"$VENV_DIR/bin/locust" -f "$LOCUSTFILE" \
    --headless \
    --users 5 \
    --spawn-rate 5 \
    --run-time 10s \
    --host "$SUT_BASE_URL" \
    --only-summary

# locustfile.py's own on_test_stop is the source of truth for pass/fail --
# not locust's own exit code, which reflects "did every HTTP request the
# tool itself made succeed", not "did every reconciliation invariant hold"
# (the same app-level-assertion-over-tool-report choice run-jmeter.sh/
# run-gatling-karate.sh already make for their own tools).
FRAGMENT_FILE="$(ls -t "$RUNS_DIR"/*-"$ARCH".json 2>/dev/null | head -n1 || true)"
if [ -z "$FRAGMENT_FILE" ]; then
    echo "run-locust.sh: no evidence fragment written for $ARCH under $RUNS_DIR" >&2
    exit 1
fi

RESULT="$("$VENV_DIR/bin/python" -c "import json,sys; print(json.load(open(sys.argv[1]))['result'])" "$FRAGMENT_FILE")"
echo "run-locust.sh: $ARCH -> $RESULT ($FRAGMENT_FILE)"
[ "$RESULT" != "failed" ]
