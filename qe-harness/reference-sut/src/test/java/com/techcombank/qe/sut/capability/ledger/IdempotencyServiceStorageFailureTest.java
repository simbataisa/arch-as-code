package com.techcombank.qe.sut.capability.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproduces the critical bug caught in Task 25's review: {@code
 * IdempotencyService}'s original fix deleted the reservation row whenever
 * ANYTHING thrown after it was created -- including a failure of the
 * fill-in {@code UPDATE} that runs AFTER {@code action} (a real transfer)
 * has already committed. Deleting the reservation in that case would let a
 * client's retry -- exactly what an idempotent-endpoint client is told to do
 * on an ambiguous error -- run {@code action} a second time, silently
 * duplicating a real transfer.
 *
 * <p>{@link FlakyJdbcTemplate} is the most direct way available to force
 * that specific {@code UPDATE} to fail without touching production code:
 * {@code JdbcTemplate.update} is a plain overridable method, so a thin
 * subclass wrapping the SAME real, Postgres-backed {@link DataSource} can
 * intercept only the SQL statement that fills in the response and delegate
 * everything else (the reservation {@code INSERT}, the lookups via {@code
 * SELECT}) to the genuine implementation. No mocking framework is needed,
 * and every other read this test makes goes through the real database, so
 * the assertions below (ledger entries actually persisted, the reservation
 * row actually still present) are checking real state, not a stub's
 * bookkeeping.
 */
class IdempotencyServiceStorageFailureTest extends AbstractLedgerIntegrationTest {

    @Test
    void aTransientPersistFailureIsRetriedAndEventuallySucceeds() {
        // Fails the fill-in UPDATE exactly twice; storeResponse retries up
        // to three times, so the third attempt must succeed.
        IdempotencyService flaky = new IdempotencyService(
            new FlakyJdbcTemplate(jdbc.getDataSource(), 2),
            idempotency.keyTtlSeconds(), idempotency.clientMaxRetryWindowSeconds());

        String key = "idem-flaky1";
        IdempotencyService.Outcome outcome = flaky.execute(key, requestBody(500L),
            () -> service.transfer("ACC-000001", "ACC-000002", 500L));

        assertFalse(outcome.replayed());
        assertEquals(2, ledgerEntryCount(), "action committed exactly once, despite the transient hiccups");
        assertTrue(idempotency.isPresent(key), "the reservation survived the transient failures");
    }

    @Test
    void aPersistFailureThatExhaustsRetriesLeavesTheReservationPendingRatherThanDeletingIt() {
        // Fails the fill-in UPDATE every time -- more failures than
        // storeResponse's retry budget. action has ALREADY committed a real
        // transfer by the time this UPDATE is even attempted, so that
        // transfer must not be silently duplicated or hidden by deleting
        // the reservation.
        IdempotencyService alwaysFlaky = new IdempotencyService(
            new FlakyJdbcTemplate(jdbc.getDataSource(), Integer.MAX_VALUE),
            idempotency.keyTtlSeconds(), idempotency.clientMaxRetryWindowSeconds());

        String key = "idem-flaky2";
        String body = requestBody(500L);

        assertThrows(IdempotencyService.ResponsePersistenceFailure.class, () ->
            alwaysFlaky.execute(key, body, () -> service.transfer("ACC-000001", "ACC-000002", 500L)));

        assertEquals(2, ledgerEntryCount(),
            "action already committed the real transfer before persistence failed -- it must not be undone");
        assertTrue(idempotency.isPresent(key),
            "the reservation must be left pending, not deleted, so a client retry cannot re-run action");
    }

    @Test
    void whenActionItselfFailsTheReservationIsDeletedSoARetryCanStartFresh() {
        // Contrast case, proving the catch really is narrowed to action's
        // own failure: nothing committed, so it IS safe to drop the
        // reservation and let a genuine retry with the same key succeed.
        String key = "idem-flaky3";
        String body = requestBody(500L);

        assertThrows(IllegalStateException.class, () ->
            idempotency.execute(key, body, () -> {
                throw new IllegalStateException("action boom");
            }));

        assertEquals(0, ledgerEntryCount(), "action never committed anything");
        assertFalse(idempotency.isPresent(key), "the reservation must be removed so a retry starts fresh");

        IdempotencyService.Outcome retry = idempotency.execute(key, body,
            () -> service.transfer("ACC-000001", "ACC-000002", 500L));
        assertFalse(retry.replayed());
        assertEquals(2, ledgerEntryCount(), "the retry ran action exactly once, as a fresh key would");
    }

    private String requestBody(long amountMinor) {
        return "{\"from\":\"ACC-000001\",\"to\":\"ACC-000002\",\"amountMinor\":" + amountMinor + "}";
    }

    private long ledgerEntryCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM ledger_entry", Long.class);
    }

    /** Delegates every statement to the real, Postgres-backed {@link
     *  JdbcTemplate} EXCEPT the idempotency response fill-in {@code UPDATE},
     *  which fails with a transient {@link TransientDataAccessResourceException}
     *  for the first {@code updateFailuresRemaining} calls. This is what lets
     *  these tests force the exact failure window the review found, using the
     *  same real database and real {@code TransferService} transaction as
     *  every other test in this suite. */
    private static final class FlakyJdbcTemplate extends JdbcTemplate {
        private final AtomicInteger updateFailuresRemaining;

        FlakyJdbcTemplate(DataSource dataSource, int updateFailuresRemaining) {
            super(dataSource);
            this.updateFailuresRemaining = new AtomicInteger(updateFailuresRemaining);
        }

        @Override
        public int update(String sql, Object... args) {
            if (sql.startsWith("UPDATE idempotency_key") && updateFailuresRemaining.getAndDecrement() > 0) {
                throw new TransientDataAccessResourceException(
                    "simulated transient failure persisting the idempotency response");
            }
            return super.update(sql, args);
        }
    }
}
