package com.techcombank.qe.sut.capability.reservation;

import com.techcombank.qe.sut.DefectFlags;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TST-023 concurrent limit and counter capability.
 *
 * <p><b>Why the row lock:</b> reserve() must read utilisation and insert a hold
 * as one atomic step, or two concurrent callers each see capacity for the last
 * unit and both take it -- admitting L+1, which is exactly invariant I1/I2's
 * failure mode. SELECT ... FOR UPDATE on the account_limit row serialises
 * every reservation for that account, so contention (not overcommit) is what
 * concurrency produces. This mirrors TransferService's lock-ordering rationale;
 * here there is only ever one row to lock, so no ordering rule is needed.
 *
 * <p><b>Defect injection:</b> when {@code reservation-overcommit} is active the
 * capacity comparison is skipped entirely -- the hold is still inserted and
 * still counted, so utilisation provably exceeds the declared limit and I1/I2
 * fail while I3/I4 stay structurally intact.
 */
@Service
public class ReservationService {

    /** Thrown when a reservation would exceed the account's declared limit. */
    public static class LimitExceeded extends RuntimeException {
        public LimitExceeded(String accountRef) {
            super("declared limit exceeded for " + accountRef);
        }
    }

    /** Thrown when releasing a reservation that is not currently held. */
    public static class NotReleasable extends RuntimeException {
        public NotReleasable(long id) {
            super("reservation " + id + " is not in state 'held'");
        }
    }

    private final JdbcTemplate jdbc;

    public ReservationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public long reserve(String accountRef, long amount) {
        long accountId = idOf(accountRef);
        Long limit = jdbc.queryForObject(
            "SELECT declared_limit FROM account_limit WHERE account_id = ? FOR UPDATE",
            Long.class, accountId);
        long ttlSeconds = jdbc.queryForObject(
            "SELECT ttl_seconds FROM account_limit WHERE account_id = ?", Long.class, accountId);

        if (!DefectFlags.isActive("reservation-overcommit")) {
            long held = heldFor(accountId);
            if (held + amount > limit) {
                throw new LimitExceeded(accountRef);
            }
        }

        return jdbc.queryForObject(
            "INSERT INTO reservation (account_id, amount, expires_at) "
                + "VALUES (?, ?, now() + make_interval(secs => ?)) RETURNING id",
            Long.class, accountId, amount, (double) ttlSeconds);
    }

    /** Releases a held reservation, returning exactly its own amount (I3).
     *  Rejects anything not currently held (I4) -- the state check is inside
     *  the UPDATE, so two concurrent releases cannot both see 'held'. */
    @Transactional
    public void release(long reservationId) {
        int updated = jdbc.update(
            "UPDATE reservation SET state = 'released' WHERE id = ? AND state = 'held'",
            reservationId);
        if (updated != 1) {
            throw new NotReleasable(reservationId);
        }
    }

    /** Sum of currently-held amounts. Expired holds are excluded even before
     *  the sweeper transitions them, so I6 holds continuously rather than only
     *  after a sweep. */
    public long utilisation(String accountRef) {
        return heldFor(idOf(accountRef));
    }

    public long declaredLimit(String accountRef) {
        return jdbc.queryForObject(
            "SELECT declared_limit FROM account_limit WHERE account_id = ?",
            Long.class, idOf(accountRef));
    }

    public String windowTimezone(String accountRef) {
        return jdbc.queryForObject(
            "SELECT window_tz FROM account_limit WHERE account_id = ?",
            String.class, idOf(accountRef));
    }

    private long heldFor(long accountId) {
        Long held = jdbc.queryForObject(
            "SELECT COALESCE(SUM(amount), 0) FROM reservation "
                + "WHERE account_id = ? AND state = 'held' AND expires_at > now()",
            Long.class, accountId);
        return held;
    }

    private long idOf(String accountRef) {
        return jdbc.queryForObject(
            "SELECT id FROM account WHERE account_ref = ?", Long.class, accountRef);
    }
}
