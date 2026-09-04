package com.techcombank.qe.sut.capability.reporting;

import com.techcombank.qe.sut.DefectFlags;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * TST-037 read-model convergence and CDC lag capability.
 *
 * <p><b>Percentiles, not the mean:</b> {@link #lag()} returns p95 and p99 and
 * deliberately exposes no mean at all. Invariant I2 fails a run that asserts
 * only the mean regardless of what the mean shows, so offering one here would
 * be offering a footgun.
 *
 * <p><b>Defect injection:</b> {@code outbox-published-count-stale} publishes
 * the row (setting published_at) but never increments published_count, so I4
 * alone fails -- convergence and the percentile shape are untouched.
 */
@Service
public class ReportingService {

    /** Read-model staleness at the tail. No mean is exposed, by design (I2). */
    public record Lag(long p95Ms, long p99Ms, long accountsCovered) {}

    private final JdbcTemplate jdbc;

    public ReportingService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Rebuilds the matview and upserts per-account freshness, using the same
     *  ON CONFLICT ... DO UPDATE idiom DefectSeeder.refresh() established. */
    @Transactional
    public void refresh() {
        jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY account_balance_report");
        jdbc.update(
            "INSERT INTO report_refresh_timestamp (account_id, refreshed_at) "
                + "SELECT account_id, now() FROM account_balance_report "
                + "ON CONFLICT (account_id) DO UPDATE SET refreshed_at = EXCLUDED.refreshed_at");
    }

    /** Tail-percentile staleness across every tracked account. Computed in
     *  Postgres via percentile_disc so the SUT, not the harness, owns the
     *  definition of the percentile. */
    public Lag lag() {
        List<Lag> rows = jdbc.query(
            "SELECT "
                + "  COALESCE(CAST(percentile_disc(0.95) WITHIN GROUP "
                + "    (ORDER BY EXTRACT(EPOCH FROM (now() - refreshed_at)) * 1000) AS BIGINT), 0) AS p95, "
                + "  COALESCE(CAST(percentile_disc(0.99) WITHIN GROUP "
                + "    (ORDER BY EXTRACT(EPOCH FROM (now() - refreshed_at)) * 1000) AS BIGINT), 0) AS p99, "
                + "  COUNT(*) AS covered "
                + "FROM report_refresh_timestamp",
            (rs, n) -> new Lag(rs.getLong("p95"), rs.getLong("p99"), rs.getLong("covered")));
        return rows.get(0);
    }

    @Transactional
    public long enqueue(String eventType, String aggregateRef) {
        return jdbc.queryForObject(
            "INSERT INTO outbox (event_type, aggregate_ref) VALUES (?, ?) RETURNING id",
            Long.class, eventType, aggregateRef);
    }

    /** Publishes every pending row. The count increment is what the defect
     *  skips -- publication itself still happens, so the failure is a
     *  miscount, not a silent drop. */
    @Transactional
    public int publishPending() {
        if (DefectFlags.isActive("outbox-published-count-stale")) {
            return jdbc.update(
                "UPDATE outbox SET published_at = now() WHERE published_at IS NULL");
        }
        return jdbc.update(
            "UPDATE outbox SET published_at = now(), published_count = published_count + 1 "
                + "WHERE published_at IS NULL");
    }

    /** I4's evidence: rows that were published but not counted exactly once. */
    public long outboxMiscountedRows() {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM outbox WHERE published_at IS NOT NULL AND published_count <> 1",
            Long.class);
    }
}
