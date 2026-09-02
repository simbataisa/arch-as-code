package com.techcombank.qe.harness.jmeter.support;

import com.techcombank.qe.harness.evidence.RunFragment;

/**
 * Result of one {@link ModuleRunner#run(String, java.util.Map)} invocation:
 * the exit code of the {@code run-module.sh} subprocess plus the
 * {@link RunFragment} it wrote to {@code traceability/runs/}.
 *
 * <p>Tests generally assert against {@link #fragment()} — the evidence is
 * the source of truth for pass/fail, not the shell exit code — but
 * {@code exitCode} is kept alongside it so a caller can also confirm the
 * module's own script agrees with what it emitted (see run-jmeter.sh, which
 * exits non-zero exactly when the fragment's result is FAILED).
 */
public record ModuleResult(int exitCode, RunFragment fragment) {
}
