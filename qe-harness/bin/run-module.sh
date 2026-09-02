#!/usr/bin/env bash
# usage: run-module.sh <ARCHETYPE>
#
# Dispatches to the tool-specific runner script for whichever module
# traceability/modules.yml declares for <ARCHETYPE>, e.g. TST-021 ->
# qe-harness/harness/jmeter/tst-021-ledger -> bin/run-jmeter.sh.
#
# Dispatching through modules.yml (rather than each caller hardcoding a
# path) means the traceability gate (scripts/validate-harness-coverage.py)
# and this runner read the exact same declaration, so a module can never be
# runnable but untraceable, or traceable but unrunnable.
#
# Self-locating via BASH_SOURCE so this script (and the tool-specific script
# it execs) behaves the same regardless of the caller's own working
# directory -- ModuleRunner (a JVM test fixture) invokes it with cwd set to
# qe-harness/, `make run` (from within qe-harness/) invokes it as
# ./bin/run-module.sh, and a developer might invoke it from the repo root.
set -euo pipefail

ARCH="${1:?usage: run-module.sh <ARCHETYPE>}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"   # qe-harness/bin
QE_HARNESS_ROOT="$(dirname "$SCRIPT_DIR")"                    # qe-harness
REPO_ROOT="$(dirname "$QE_HARNESS_ROOT")"                     # repo root

"$SCRIPT_DIR/wait-for-sut.sh"

MODULE_PATH="$(python3 - "$ARCH" "$QE_HARNESS_ROOT/traceability/modules.yml" <<'PY'
import sys, yaml, pathlib
archetype, modules_file = sys.argv[1], sys.argv[2]
mods = yaml.safe_load(pathlib.Path(modules_file).read_text())["modules"]
try:
    print(next(m["path"] for m in mods if m["archetype"] == archetype))
except StopIteration:
    sys.exit(f"no modules.yml entry for archetype {archetype}")
PY
)"

# MODULE_PATH is repo-root-relative (e.g. "qe-harness/harness/jmeter/tst-021-ledger"),
# matching modules.yml's own declared paths -- resolve it against REPO_ROOT so this
# script works regardless of its own caller's cwd.
TOOL_DIR="$(basename "$(dirname "$MODULE_PATH")")"   # e.g. "jmeter"
TOOL_RUNNER="$SCRIPT_DIR/run-${TOOL_DIR}.sh"

if [ ! -x "$TOOL_RUNNER" ]; then
    echo "run-module.sh: no runner script for tool '$TOOL_DIR' (expected $TOOL_RUNNER)" >&2
    exit 1
fi

exec "$TOOL_RUNNER" "$ARCH" "$REPO_ROOT/$MODULE_PATH"
