package com.techcombank.qe.sut.capability.recon;

import java.util.List;

/**
 * One reconciliation dimension's result: how many rows {@link ReconService}
 * checked, and the {@code account_ref}s it found to be defective. Serialises
 * to exactly {@code {"checked": n, "defects": [ids]}} (see the task brief).
 */
public record DimensionResult(int checked, List<String> defects) {

    /** Same population, defects hidden -- used by {@code
     *  DefectFlags.isActive("recon-false-clean")} to mask genuinely detected
     *  defects without pretending they were never checked for. */
    DimensionResult withNoDefects() {
        return new DimensionResult(checked, List.of());
    }
}
