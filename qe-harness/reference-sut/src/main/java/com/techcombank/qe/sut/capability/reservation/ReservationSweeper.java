package com.techcombank.qe.sut.capability.reservation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * TST-023 invariant I6: no reservation outlives its TTL.
 *
 * <p>utilisation() already excludes expired holds in its own WHERE clause, so
 * correctness does not depend on this sweeper's timing -- it exists so expired
 * rows reach a terminal state observable by the harness, rather than lingering
 * as 'held' forever. Interval comes from a declared property so the test reads
 * it rather than duplicating the number.
 */
@Component
public class ReservationSweeper {

    private final JdbcTemplate jdbc;

    public ReservationSweeper(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelayString = "${app.reservation.sweep-interval-ms}")
    public void sweep() {
        jdbc.update("UPDATE reservation SET state = 'expired' "
            + "WHERE state = 'held' AND expires_at <= now()");
    }

    /** Test-support: run one sweep synchronously instead of waiting for the schedule. */
    public int sweepNow() {
        return jdbc.update("UPDATE reservation SET state = 'expired' "
            + "WHERE state = 'held' AND expires_at <= now()");
    }
}
