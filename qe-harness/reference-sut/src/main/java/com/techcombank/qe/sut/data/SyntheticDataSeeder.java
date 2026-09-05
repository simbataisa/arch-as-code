package com.techcombank.qe.sut.data;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Seeds the {@code account} and {@code ledger_entry} tables (see
 * {@code V1__accounts_and_ledger.sql}) with synthetic, non-PII data.
 *
 * <p>Determinism: the only source of entropy is {@link Random}, constructed
 * fresh from the caller's {@code seed} on every call to {@link #seed(long)}.
 * The same seed against an empty schema always produces the same number of
 * accounts and the same number of ledger entries.
 *
 * <p>Balance: every ledger entry is written as one leg of a debit/credit pair
 * that shares a single {@code transfer_ref} and carries equal, opposite
 * {@code amount_minor} values. The sum of {@code amount_minor} across the
 * whole table is therefore always zero immediately after seeding, regardless
 * of the seed.
 *
 * <p>Identifiers: account references are generated as {@code ACC-%06d}, a
 * shape the {@code account_ref_format} CHECK constraint enforces at the
 * database level -- this class has no way to produce a PAN-shaped
 * identifier even if it tried.
 */
@Component
public class SyntheticDataSeeder {

    /** One account per invented organisation name. */
    private static final int ACCOUNT_COUNT = SyntheticNames.NAMES.length;

    /** Each transfer writes exactly two ledger entries (debit + credit). */
    private static final int TRANSFER_COUNT = 30;

    private static final long MIN_AMOUNT_MINOR = 100L;        // 1.00 in minor units
    private static final long MAX_AMOUNT_MINOR = 1_000_000L;  // 10,000.00 in minor units

    private final JdbcTemplate jdbc;

    public SyntheticDataSeeder(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Seeds with one account per synthetic name -- the original contract. */
    public SeedSummary seed(long seed) {
        return seed(seed, ACCOUNT_COUNT);
    }

    /** Seeds {@code accountCount} accounts. A blended workload (TST-034) needs
     *  more contention surface than the two-account ledger fixture provides,
     *  but can never exceed the fixed synthetic-name pool -- requesting more
     *  throws rather than inventing a name, since a generated party name could
     *  not be guaranteed non-PII. */
    public SeedSummary seed(long seed, int accountCount) {
        if (accountCount < 1 || accountCount > SyntheticNames.NAMES.length) {
            throw new IllegalArgumentException(
                "accountCount must be 1.." + SyntheticNames.NAMES.length + ", got " + accountCount);
        }
        Random random = new Random(seed);

        List<Long> accountIds = seedAccounts(random, accountCount);
        int entries = seedLedger(random, accountIds);

        return new SeedSummary(accountIds.size(), entries);
    }

    private List<Long> seedAccounts(Random random, int accountCount) {
        List<String> shuffledNames = new ArrayList<>(List.of(SyntheticNames.NAMES));
        Collections.shuffle(shuffledNames, random);

        List<Long> accountIds = new ArrayList<>(accountCount);
        for (int i = 0; i < accountCount; i++) {
            String accountRef = "ACC-%06d".formatted(i + 1);
            Long id = jdbc.queryForObject(
                "INSERT INTO account (account_ref, party_name) VALUES (?, ?) RETURNING id",
                Long.class, accountRef, shuffledNames.get(i));
            accountIds.add(id);
        }
        return accountIds;
    }

    private int seedLedger(Random random, List<Long> accountIds) {
        int entries = 0;
        for (int t = 0; t < TRANSFER_COUNT; t++) {
            UUID transferRef = new UUID(random.nextLong(), random.nextLong());
            long amountMinor = random.nextLong(MIN_AMOUNT_MINOR, MAX_AMOUNT_MINOR);

            int debtorIndex = random.nextInt(accountIds.size());
            int creditorIndex = nextDistinctIndex(random, accountIds.size(), debtorIndex);

            insertLedgerEntry(transferRef, accountIds.get(debtorIndex), -amountMinor);
            insertLedgerEntry(transferRef, accountIds.get(creditorIndex), amountMinor);
            entries += 2;
        }
        return entries;
    }

    private static int nextDistinctIndex(Random random, int bound, int exclude) {
        int candidate;
        do {
            candidate = random.nextInt(bound);
        } while (candidate == exclude);
        return candidate;
    }

    private void insertLedgerEntry(UUID transferRef, long accountId, long amountMinor) {
        jdbc.update(
            "INSERT INTO ledger_entry (transfer_ref, account_id, amount_minor) VALUES (?, ?, ?)",
            transferRef, accountId, amountMinor);
    }
}
