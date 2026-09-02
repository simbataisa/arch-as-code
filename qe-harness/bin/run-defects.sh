#!/usr/bin/env bash
# usage: run-defects.sh
#
# The defect-pair proof: for every module traceability/modules.yml declares,
# activates that module's own `defect_flag` on the already-running reference
# SUT, then runs the module and INVERTS the expectation -- a module that
# still passes against its own declared defect is the failure here, not the
# success.
#
# Correction to the brief's own pseudocode (see Task 23 brief + report):
# `SUT_DEFECT=<flag> ./bin/run-module.sh <arch>` has ZERO effect on the SUT.
# The SUT is a separate, already-running Docker container; setting a shell
# env var on this script (or on run-module.sh's subprocess) never reaches
# it. Every module that already proves its own defect detection (TST-021,
# TST-030, TST-031, TST-035, TST-039, TST-040, TST-043's own module tests,
# and Tasks 16/20/21's ModuleRunner test fixture) does it the same way this
# script does: POST/DELETE against the SUT's own HTTP control endpoint,
# `/_test/defect/{flag}`.
#
# Cleanup guarantee: DELETE /_test/defect happens after EVERY module,
# regardless of whether that module's activation, run, or expectation-check
# succeeded, failed, or errored -- so one module's injected defect can never
# leak into the next module's "clean-SUT-except-for-my-own-defect"
# expectation. Belt-and-suspenders: an explicit clear_defect call after each
# iteration (the case that matters for correctness -- module N+1 must never
# see module N's defect) PLUS a `trap ... EXIT` (insurance against this
# script itself dying unexpectedly mid-loop, e.g. a signal or an unbound
# variable under `set -u`).
set -uo pipefail

SUT_URL="${SUT_URL:-http://localhost:8080}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"   # qe-harness/bin
QE_HARNESS_ROOT="$(dirname "$SCRIPT_DIR")"                    # qe-harness

cd "$QE_HARNESS_ROOT"

clear_defect() {
    # Best-effort: cleanup must never itself abort the script (still `|| true`
    # at the end), and must never mask an already-recorded proof failure with
    # a cleanup failure. But best-effort must not mean silent: if DELETE
    # itself genuinely fails (SUT temporarily unreachable, a network blip),
    # that is exactly the "defect leaks into the next module" scenario this
    # whole mechanism exists to prevent -- make it observable instead of a
    # quiet no-op.
    if ! curl -fsS -X DELETE "$SUT_URL/_test/defect" -o /dev/null 2>/dev/null; then
        echo "WARNING: clear_defect failed -- DELETE /_test/defect did not succeed; a defect may still be active on the SUT" >&2
    fi
    true
}
trap clear_defect EXIT

# Built-in `mapfile`/`readarray` (bash 4+) is unavailable under macOS's
# shipped `/bin/bash` (3.2, Apple ships it frozen at the last GPLv2
# release) -- read modules.yml's (archetype, defect_flag) pairs via a
# `while read` loop over process substitution instead, which works on both
# bash 3.2 and 4+. Two-column `read arch flag` also sidesteps having to
# split a single joined string back apart.
ARCHS=()
FLAGS=()
count=0
while IFS=' ' read -r arch flag; do
    ARCHS+=("$arch")
    FLAGS+=("$flag")
    count=$((count + 1))
done < <(python3 - traceability/modules.yml <<'PY'
import sys, yaml, pathlib
mods = yaml.safe_load(pathlib.Path(sys.argv[1]).read_text())["modules"]
for m in mods:
    print(f"{m['archetype']} {m['defect_flag']}")
PY
)

if [ "$count" -eq 0 ]; then
    echo "run-defects.sh: modules.yml declared zero modules -- nothing to prove" >&2
    exit 1
fi

status=0
i=0
while [ "$i" -lt "$count" ]; do
    arch="${ARCHS[$i]}"
    flag="${FLAGS[$i]}"
    i=$((i + 1))

    echo "=== run-defects: $arch against defect '$flag' ==="

    if ! curl -fsS -X POST "$SUT_URL/_test/defect/$flag" -o /dev/null; then
        echo "DEFECT PROOF FAILED: could not activate defect '$flag' for $arch" >&2
        status=1
        clear_defect
        continue
    fi

    # Export QE_SUT_DEFECT so every module can tag its own emitted fragment's
    # evidence.sut_defect with the flag actually active for this iteration --
    # otherwise a defect-proof `failed` fragment is indistinguishable, in the
    # evidence trail, from a genuine regression (I4). Scoped to this one
    # command via the inline assignment, so it never leaks into a later
    # iteration or this script's own environment.
    if QE_SUT_DEFECT="$flag" "$SCRIPT_DIR/run-module.sh" "$arch"; then
        echo "DEFECT PROOF FAILED: $arch passed against SUT_DEFECT=$flag" >&2
        status=1
    else
        echo "ok: $arch correctly failed against $flag"
    fi

    # Explicit per-iteration cleanup -- module N+1 must run against a clean
    # SUT (apart from ITS OWN defect_flag, activated on its own iteration),
    # never against module N's leftover defect. Runs on every path above:
    # the passed-when-it-shouldn't-have branch, the correctly-failed branch,
    # and (via the `continue` above) the activation-failed branch too.
    clear_defect
done

exit "$status"
