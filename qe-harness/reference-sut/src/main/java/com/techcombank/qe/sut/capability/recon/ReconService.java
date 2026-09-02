package com.techcombank.qe.sut.capability.recon;

import com.techcombank.qe.sut.DefectFlags;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * TST-039 reconciliation capability: independently recomputes each of three
 * dimensions of {@code account_balance_report} (see
 * {@code V2__reporting_view.sql}) against {@code ledger_entry} -- its
 * source of truth -- and against {@code report_refresh_timestamp}'s
 * freshness bookkeeping.
 *
 * <p><b>Genuinely detects; never echoes:</b> every dimension below queries
 * {@code account}/{@code ledger_entry}/{@code account_balance_report}/
 * {@code report_refresh_timestamp} directly. None of them call
 * {@link DefectSeeder} or read anything it returns -- {@link
 * DefectSeeder#seed} and this class are wired together only by both
 * operating on the same tables, exactly like a real seed-then-detect
 * reconciliation workflow. This is what lets Task 21's Locust module build
 * a genuine confusion matrix: a false positive or false negative here would
 * be a real detection failure, not an artefact of the check trusting the
 * seeder's own bookkeeping.
 *
 * <ul>
 *   <li><b>Completeness</b> -- every {@code account} with at least one
 *       {@code ledger_entry} row must have a matching
 *       {@code account_balance_report} row. A missing row is a defect.
 *   <li><b>Accuracy</b> -- every account with an {@code
 *       account_balance_report} row must have {@code balance_minor} equal
 *       to {@code ledger_entry}'s true, freshly-recomputed sum for that
 *       account. A mismatch is a defect.
 *   <li><b>Timeliness</b> -- every account with a {@code
 *       report_refresh_timestamp} row must have been refreshed within
 *       {@link #freshnessWindow()}. An older timestamp is a defect.
 * </ul>
 *
 * <p><b>Defect injection:</b> {@code
 * DefectFlags.isActive("recon-false-clean")} true, every dimension's
 * defects are still computed (so {@code checked} counts stay honest) and
 * then discarded -- the report claims a clean SUT no matter what {@link
 * DefectSeeder} actually seeded. See {@link com.techcombank.qe.sut.DefectFlags}.
 */
@Service
public class ReconService {

    private final JdbcTemplate jdbc;
    private final long freshnessWindowSeconds;

    public ReconService(JdbcTemplate jdbc,
                         @Value("${app.recon.freshness-window-seconds}") long freshnessWindowSeconds) {
        this.jdbc = jdbc;
        this.freshnessWindowSeconds = freshnessWindowSeconds;
    }

    public ReconReport report() {
        DimensionResult completeness = completeness();
        DimensionResult accuracy = accuracy();
        DimensionResult timeliness = timeliness();

        if (DefectFlags.isActive("recon-false-clean")) {
            return new ReconReport(
                completeness.withNoDefects(),
                accuracy.withNoDefects(),
                timeliness.withNoDefects());
        }
        return new ReconReport(completeness, accuracy, timeliness);
    }

    /** Declared freshness SLA -- see {@code app.recon.freshness-window-seconds}
     *  in {@code application.properties}. */
    public Duration freshnessWindow() {
        return Duration.ofSeconds(freshnessWindowSeconds);
    }

    private DimensionResult completeness() {
        List<String> checked = jdbc.queryForList(
            "SELECT DISTINCT a.account_ref FROM account a " +
            "JOIN ledger_entry l ON l.account_id = a.id " +
            "ORDER BY a.account_ref",
            String.class);

        List<String> defects = jdbc.queryForList(
            "SELECT DISTINCT a.account_ref FROM account a " +
            "JOIN ledger_entry l ON l.account_id = a.id " +
            "LEFT JOIN account_balance_report r ON r.account_id = a.id " +
            "WHERE r.account_id IS NULL " +
            "ORDER BY a.account_ref",
            String.class);

        return new DimensionResult(checked.size(), defects);
    }

    private DimensionResult accuracy() {
        List<AccuracyRow> rows = jdbc.query(
            "SELECT a.account_ref AS ref, r.balance_minor AS reported, " +
            "  CAST(COALESCE((SELECT SUM(l.amount_minor) FROM ledger_entry l " +
            "                 WHERE l.account_id = a.id), 0) AS BIGINT) AS actual " +
            "FROM account a " +
            "JOIN account_balance_report r ON r.account_id = a.id " +
            "ORDER BY a.account_ref",
            (rs, rowNum) -> new AccuracyRow(
                rs.getString("ref"), rs.getLong("reported"), rs.getLong("actual")));

        List<String> defects = rows.stream()
            .filter(row -> row.reported() != row.actual())
            .map(AccuracyRow::ref)
            .toList();

        return new DimensionResult(rows.size(), defects);
    }

    private DimensionResult timeliness() {
        Instant threshold = Instant.now().minus(freshnessWindow());

        List<TimelinessRow> rows = jdbc.query(
            "SELECT a.account_ref AS ref, t.refreshed_at AS refreshed_at " +
            "FROM account a " +
            "JOIN report_refresh_timestamp t ON t.account_id = a.id " +
            "ORDER BY a.account_ref",
            (rs, rowNum) -> new TimelinessRow(
                rs.getString("ref"), rs.getTimestamp("refreshed_at").toInstant()));

        List<String> defects = rows.stream()
            .filter(row -> row.refreshedAt().isBefore(threshold))
            .map(TimelinessRow::ref)
            .toList();

        return new DimensionResult(rows.size(), defects);
    }

    private record AccuracyRow(String ref, long reported, long actual) {}

    private record TimelinessRow(String ref, Instant refreshedAt) {}
}
