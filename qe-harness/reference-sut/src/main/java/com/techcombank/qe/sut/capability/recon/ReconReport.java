package com.techcombank.qe.sut.capability.recon;

/**
 * {@code GET /recon/report}'s response body: one {@link DimensionResult}
 * per reconciliation dimension. Serialises to exactly
 * {@code {"completeness": {...}, "accuracy": {...}, "timeliness": {...}}}
 * (see the task brief).
 */
public record ReconReport(
    DimensionResult completeness,
    DimensionResult accuracy,
    DimensionResult timeliness) {
}
