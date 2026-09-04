package com.techcombank.qe.sut.capability.ledger;

import com.techcombank.qe.sut.DefectFlags;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;

/**
 * TST-020 idempotency and replay capability.
 *
 * <p><b>The unique constraint is the mechanism, not a safety net.</b> Under true
 * concurrency (I5) two callers race to <em>reserve</em> the same key, before
 * either runs {@code action}: {@link #execute} inserts a placeholder row
 * (empty {@code response_body}) first, and only the caller whose {@code
 * INSERT} actually succeeds goes on to run {@code action} and fill in the
 * real response. Every other racer catches {@link DuplicateKeyException} on
 * that same {@code INSERT} and polls the row until it stops being a
 * placeholder, then serves the winner's stored response.
 *
 * <p>This ordering matters: an earlier design that ran {@code action} first
 * and only attempted the {@code INSERT} afterwards let every racer pass the
 * initial "is this key known yet?" check before anyone had written anything,
 * so all of them executed {@code action} -- the unique constraint only
 * arbitrated who got to <em>record</em> a row, not who got to <em>act</em>.
 * Reserving the key with an empty-body placeholder before running {@code
 * action} makes the constraint the actual arbiter of execution, not just of
 * bookkeeping.
 *
 * <p><b>Responses are stored verbatim (I2).</b> A replay must be byte-identical,
 * so re-serialising is not an option -- key order or whitespace could drift.
 *
 * <p><b>A reused key with a different payload conflicts (I4)</b> rather than
 * replaying someone else's result, which is the more dangerous failure of the
 * two.
 *
 * <p><b>Defect injection:</b> {@code idempotency-key-ignored} skips the key
 * lookup entirely and always executes, so N same-key requests produce N state
 * changes. I1 fails; the ledger stays balanced throughout, so TST-021's
 * invariants are untouched -- the defect is specific.
 */
@Service
public class IdempotencyService {

    /** Marks a just-reserved row whose winner has not yet stored a real
     *  response. A real response (see {@link #render}) is never empty, so
     *  this cannot collide with a genuine stored body. */
    private static final String PENDING = "";

    private static final long AWAIT_TIMEOUT_NANOS = 10_000_000_000L;
    private static final long AWAIT_POLL_MILLIS = 2L;

    /** Thrown when a key is reused with a different payload (I4). */
    public static class PayloadConflict extends RuntimeException {
        public PayloadConflict(String key) {
            super("idempotency key reused with a different payload: " + key);
        }
    }

    /** The outcome of an idempotent execution. */
    public record Outcome(String body, boolean replayed) {}

    private final JdbcTemplate jdbc;
    private final long keyTtlSeconds;
    private final long clientMaxRetryWindowSeconds;

    public IdempotencyService(JdbcTemplate jdbc,
                              @Value("${app.idempotency.key-ttl-seconds}") long keyTtlSeconds,
                              @Value("${app.idempotency.client-max-retry-window-seconds}") long clientMaxRetryWindowSeconds) {
        this.jdbc = jdbc;
        this.keyTtlSeconds = keyTtlSeconds;
        this.clientMaxRetryWindowSeconds = clientMaxRetryWindowSeconds;
    }

    public long keyTtlSeconds() {
        return keyTtlSeconds;
    }

    public long clientMaxRetryWindowSeconds() {
        return clientMaxRetryWindowSeconds;
    }

    /** For the {@code GET /transfers/idempotency/{key}} status endpoint
     *  (LedgerController): whether a non-expired record exists for this key. */
    public boolean isPresent(String key) {
        return !find(key).isEmpty();
    }

    /** Executes {@code action} at most once per key. */
    public Outcome execute(String key, String payload, Supplier<Object> action) {
        if (DefectFlags.isActive("idempotency-key-ignored")) {
            // The defect: no lookup, no record. Every call executes.
            return new Outcome(render(action.get()), false);
        }

        return attempt(key, sha256(payload), action);
    }

    /** One try at reserving {@code key} and, if reservation succeeds, running
     *  {@code action}. Recurses (once, in the ordinary case) if the row this
     *  caller was waiting on turns out to have been rolled back by its
     *  winner -- see {@link #awaitWinner}. */
    private Outcome attempt(String key, String hash, Supplier<Object> action) {
        List<StoredKey> existing = find(key);
        if (!existing.isEmpty()) {
            StoredKey stored = existing.get(0);
            return isPending(stored) ? awaitWinner(key, hash, action) : replayOrConflict(key, hash, stored);
        }

        try {
            // The reservation: an empty response_body placeholder, inserted
            // BEFORE action runs. Exactly one concurrent caller's INSERT can
            // succeed for a given key -- that caller, and only that caller,
            // goes on to run action below. This is what makes the unique
            // constraint the arbiter of execution, not merely of bookkeeping.
            jdbc.update(
                "INSERT INTO idempotency_key (idempotency_key, payload_hash, response_body, expires_at) "
                    + "VALUES (?, ?, ?, now() + make_interval(secs => ?))",
                key, hash, PENDING, (double) keyTtlSeconds);
        } catch (DuplicateKeyException e) {
            // I5's loser path: another caller's INSERT won the race to
            // reserve this key. Wait for them to finish and serve their
            // stored response rather than surfacing a 500 -- the constraint
            // violation is expected here, not an error, and it happened
            // before action ran, not after.
            return awaitWinner(key, hash, action);
        }

        return runReservedAction(key, hash, action);
    }

    /** Runs {@code action} for the caller that just won the reservation, and
     *  stores its rendered result verbatim (I2). If action throws, the
     *  reservation is removed rather than left permanently pending, so a
     *  later retry with the same key is not stuck waiting forever. */
    private Outcome runReservedAction(String key, String hash, Supplier<Object> action) {
        try {
            String body = render(action.get());
            jdbc.update("UPDATE idempotency_key SET response_body = ? WHERE idempotency_key = ?", body, key);
            return new Outcome(body, false);
        } catch (RuntimeException e) {
            jdbc.update("DELETE FROM idempotency_key WHERE idempotency_key = ?", key);
            throw e;
        }
    }

    /** Polls the reserved row until its winner fills in a real response (or
     *  removes it on failure), then replays -- or, if the winner rolled back,
     *  re-attempts the reservation as if this were the first caller. */
    private Outcome awaitWinner(String key, String hash, Supplier<Object> action) {
        long deadline = System.nanoTime() + AWAIT_TIMEOUT_NANOS;
        while (System.nanoTime() < deadline) {
            List<StoredKey> found = find(key);
            if (found.isEmpty()) {
                return attempt(key, hash, action);
            }
            StoredKey stored = found.get(0);
            if (!isPending(stored)) {
                return replayOrConflict(key, hash, stored);
            }
            try {
                Thread.sleep(AWAIT_POLL_MILLIS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while awaiting idempotency winner: " + key, ie);
            }
        }
        throw new IllegalStateException("timed out awaiting idempotency winner: " + key);
    }

    private boolean isPending(StoredKey stored) {
        return PENDING.equals(stored.responseBody());
    }

    private Outcome replayOrConflict(String key, String hash, StoredKey stored) {
        if (!stored.payloadHash().equals(hash)) {
            throw new PayloadConflict(key);
        }
        return new Outcome(stored.responseBody(), true);
    }

    private List<StoredKey> find(String key) {
        return jdbc.query(
            "SELECT payload_hash, response_body FROM idempotency_key "
                + "WHERE idempotency_key = ? AND expires_at > now()",
            (rs, n) -> new StoredKey(rs.getString("payload_hash"), rs.getString("response_body")),
            key);
    }

    /** Verbatim rendering: the stored body is what a replay returns, so this is
     *  the one place the response's bytes are decided. */
    private String render(Object result) {
        return "{\"transferRef\":\"" + result + "\"}";
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private record StoredKey(String payloadHash, String responseBody) {}
}
