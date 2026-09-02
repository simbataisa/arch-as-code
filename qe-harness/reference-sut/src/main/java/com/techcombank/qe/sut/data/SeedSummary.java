package com.techcombank.qe.sut.data;

/**
 * Result of one {@link SyntheticDataSeeder#seed(long)} call: how many
 * {@code account} rows and how many {@code ledger_entry} rows were written.
 */
public record SeedSummary(int accounts, int entries) {
}
