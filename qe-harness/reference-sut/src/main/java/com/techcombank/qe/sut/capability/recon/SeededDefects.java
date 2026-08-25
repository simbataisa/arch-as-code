package com.techcombank.qe.sut.capability.recon;

import java.util.List;

/**
 * The ground truth returned by one {@link DefectSeeder#seed(long)} call:
 * exactly which {@code account_ref}s that call made defective in each
 * dimension. {@link ReconService} never sees this object -- it is
 * consumed only by the tests (and, later, Task 21's Locust module) that
 * score {@link ReconService#report()}'s independently-detected defects
 * against this known-true set.
 */
public record SeededDefects(
    List<String> completeness,
    List<String> accuracy,
    List<String> timeliness) {
}
