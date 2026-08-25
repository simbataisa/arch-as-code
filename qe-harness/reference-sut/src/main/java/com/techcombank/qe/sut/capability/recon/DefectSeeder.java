package com.techcombank.qe.sut.capability.recon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * TST-039 reconciliation capability: seeds a deterministic, independently
 * re-derivable defect set against the real {@code account_balance_report}
 * materialized view and its companion {@code report_refresh_timestamp}
 * table (see {@code V2__reporting_view.sql}). {@link ReconService} never
 * calls this class or reads anything it returns -- see that class's
 * javadoc for why that separation is the whole point.
 *
 * <p>{@link #seed(long)} writes four accounts:
 * <ul>
 *   <li><b>Control</b> -- gets balanced ledger activity before the one
 *       {@code REFRESH MATERIALIZED VIEW} call below, and is never touched
 *       again. It must never appear in any defect list: {@link
 *       ReconService} has no way to tell it apart from the three defect
 *       accounts below except by genuinely recomputing/comparing, since it
 *       is never told which accounts are "the seeded ones".
 *   <li><b>Completeness defect</b> -- gets its only ledger activity
 *       strictly <em>after</em> the refresh, so
 *       {@code account_balance_report} genuinely never saw it: no row
 *       exists for it at all.
 *   <li><b>Accuracy defect</b> -- gets balanced ledger activity before the
 *       refresh (so it has a correct report row as of that snapshot), then
 *       one more, unmatched ledger entry afterwards -- {@code
 *       ledger_entry}'s true sum for it has genuinely moved past what
 *       {@code account_balance_report.balance_minor} still holds.
 *   <li><b>Timeliness defect</b> -- gets balanced ledger activity before
 *       the refresh and nothing afterwards, so its balance stays correct;
 *       only its {@code report_refresh_timestamp} row is backdated past
 *       {@code app.recon.freshness-window-seconds}, simulating a refresh
 *       job that silently stopped running for this one account even though
 *       the values it last wrote are still accurate.
 * </ul>
 *
 * <p>Every account_ref is namespaced {@code ACC-99xxxx} to stay clear of
 * whatever a test's own fixture (or {@code SyntheticDataSeeder}) already
 * inserted.
 */
@Component
public class DefectSeeder {

    private final JdbcTemplate jdbc;
    private final long freshnessWindowSeconds;

    public DefectSeeder(JdbcTemplate jdbc,
                         @Value("${app.recon.freshness-window-seconds}") long freshnessWindowSeconds) {
        this.jdbc = jdbc;
        this.freshnessWindowSeconds = freshnessWindowSeconds;
    }

    public SeededDefects seed(long seed) {
        Random random = new Random(seed);

        long controlId       = insertAccount("ACC-990001", "Recon Control Co");
        long completenessId  = insertAccount("ACC-990002", "Recon Completeness Defect Co");
        long accuracyId      = insertAccount("ACC-990003", "Recon Accuracy Defect Co");
        long timelinessId    = insertAccount("ACC-990004", "Recon Timeliness Defect Co");

        // Pre-refresh ledger activity: control/accuracy/timeliness accounts
        // all have genuine, balanced ledger history at the moment of the
        // one refresh below. The completeness account has none yet.
        writeBalancedPair(random, accuracyId, controlId);
        writeBalancedPair(random, timelinessId, controlId);

        // The one and only refresh: account_balance_report and
        // report_refresh_timestamp now correctly reflect exactly the
        // control/accuracy/timeliness accounts' true balances, as of now.
        refresh();

        // Completeness defect: this account's only ledger activity happens
        // strictly after the refresh above, so it never gets a report row.
        insertLedgerEntry(UUID.randomUUID(), completenessId, 1_000L + random.nextInt(9_000));

        // Accuracy defect: one more, unmatched entry for an
        // already-refreshed account, written after the refresh, so its
        // stored balance_minor is now genuinely stale.
        insertLedgerEntry(UUID.randomUUID(), accuracyId, 100L + random.nextInt(900));

        // Timeliness defect: ledger data for this account is never touched
        // again (its balance stays correct); only the bookkeeping row is
        // backdated, well past the declared freshness window.
        Instant stale = Instant.now()
            .minus(Duration.ofSeconds(freshnessWindowSeconds))
            .minus(Duration.ofMinutes(10));
        jdbc.update("UPDATE report_refresh_timestamp SET refreshed_at = ? WHERE account_id = ?",
            Timestamp.from(stale), timelinessId);

        return new SeededDefects(
            List.of(accountRef(completenessId)),
            List.of(accountRef(accuracyId)),
            List.of(accountRef(timelinessId)));
    }

    private long insertAccount(String accountRef, String partyName) {
        return jdbc.queryForObject(
            "INSERT INTO account (account_ref, party_name) VALUES (?, ?) RETURNING id",
            Long.class, accountRef, partyName);
    }

    private void writeBalancedPair(Random random, long debtorId, long creditorId) {
        UUID transferRef = new UUID(random.nextLong(), random.nextLong());
        long amountMinor = 1_000L + random.nextInt(9_000);
        insertLedgerEntry(transferRef, debtorId, -amountMinor);
        insertLedgerEntry(transferRef, creditorId, amountMinor);
    }

    private void insertLedgerEntry(UUID transferRef, long accountId, long amountMinor) {
        jdbc.update(
            "INSERT INTO ledger_entry (transfer_ref, account_id, amount_minor) VALUES (?, ?, ?)",
            transferRef, accountId, amountMinor);
    }

    /** Rebuilds account_balance_report from ledger_entry as it stands right
     *  now, then upserts report_refresh_timestamp for every account the
     *  refreshed view now contains. */
    private void refresh() {
        jdbc.execute("REFRESH MATERIALIZED VIEW account_balance_report");
        jdbc.update(
            "INSERT INTO report_refresh_timestamp (account_id, refreshed_at) " +
            "SELECT account_id, now() FROM account_balance_report " +
            "ON CONFLICT (account_id) DO UPDATE SET refreshed_at = EXCLUDED.refreshed_at");
    }

    private String accountRef(long accountId) {
        return jdbc.queryForObject(
            "SELECT account_ref FROM account WHERE id = ?", String.class, accountId);
    }
}
