#!/usr/bin/env bash
# usage: run-all.sh
#
# Runs every module traceability/modules.yml declares -- the 7 real,
# implemented archetypes (TST-021, TST-030, TST-031, TST-035, TST-039,
# TST-040, TST-043), NOT the 17 archetypes TST-010 declares but this wave
# never implements -- against the currently-running, clean (no injected
# defect) SUT, then merges the resulting evidence fragments into
# traceability/test_acceptance_criteria.yml.
#
# Reads the archetype list from modules.yml itself (not a hardcoded list
# here) for the same reason run-module.sh dispatches through modules.yml
# rather than a hardcoded path: a module can never become runnable here
# without also being declared there, or vice versa.
#
# Exits non-zero if ANY module fails, or if the merge step itself fails.
# Deliberately does NOT use `set -e` for the run loop: one module's failure
# must not skip the other six, or a single flaky/broken module would hide
# every other module's own pass/fail result.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"   # qe-harness/bin
QE_HARNESS_ROOT="$(dirname "$SCRIPT_DIR")"                    # qe-harness

cd "$QE_HARNESS_ROOT"

# Built-in `mapfile`/`readarray` (bash 4+) is unavailable under macOS's
# shipped `/bin/bash` (3.2, Apple ships it frozen at the last GPLv2 release)
# -- read the modules.yml-declared archetype list via a `while read` loop
# over process substitution instead, which works on both bash 3.2 and 4+.
ARCHETYPES=()
count=0
while IFS= read -r arch; do
    ARCHETYPES+=("$arch")
    count=$((count + 1))
done < <(python3 - traceability/modules.yml <<'PY'
import sys, yaml, pathlib
mods = yaml.safe_load(pathlib.Path(sys.argv[1]).read_text())["modules"]
for m in mods:
    print(m["archetype"])
PY
)

if [ "$count" -eq 0 ]; then
    echo "run-all.sh: modules.yml declared zero modules -- nothing to run" >&2
    exit 1
fi

status=0
for arch in "${ARCHETYPES[@]}"; do
    echo "=== run-all: $arch ==="
    if "$SCRIPT_DIR/run-module.sh" "$arch"; then
        echo "ok: $arch passed"
    else
        echo "FAILED: $arch (see output above)" >&2
        status=1
    fi
done

echo "=== run-all: merging $count evidence fragments ==="
if ! python3 "$SCRIPT_DIR/merge-fragments.py"; then
    echo "run-all.sh: merge-fragments.py reported a failure" >&2
    status=1
fi

if [ "$status" -eq 0 ]; then
    echo "run-all: all ${#ARCHETYPES[@]} modules passed"
else
    echo "run-all: FAILED -- see above" >&2
fi

exit "$status"
