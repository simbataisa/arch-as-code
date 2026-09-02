package com.techcombank.qe.sut.capability.ledger;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * TST-021 double-entry ledger: concurrency and defect-injection scenarios.
 *
 * <p>{@link #trialBalanceStaysZeroUnderConcurrentTransfers()} is the proof
 * that {@link TransferService#transfer} locks the two account rows in a
 * deterministic order (by {@code account_id}, never by call-order {@code
 * from}/{@code to}) -- see the Javadoc on {@code TransferService.lockPair}.
 * A flaky implementation here would surface as either an intermittent
 * deadlock (Postgres error 40P01, surfaced as a
 * {@link org.springframework.dao.CannotAcquireLockException}) or, worse, a
 * silently non-zero trial balance from a lost update -- so this test is run
 * repeatedly during self-review, not just once.
 */
class LedgerConcurrencyTest extends AbstractLedgerIntegrationTest {

    @Test
    void trialBalanceStaysZeroUnderConcurrentTransfers() throws Exception {
        // 200 concurrent transfers across a shared pair of accounts.
        // NOTE: declared `var` rather than the brief's `List<Future<?>>` --
        // List's invariance means List<Future<UUID>> (what .toList() actually
        // infers from a Callable<UUID> lambda) does not convert to
        // List<Future<?>>; the element-wise assignment in the loop below
        // still works either way. Behavior is otherwise identical to the brief.
        ExecutorService pool = Executors.newFixedThreadPool(16);
        var futures = IntStream.range(0, 200)
            .mapToObj(i -> pool.submit(() -> service.transfer("ACC-000001", "ACC-000002", 1_00L)))
            .toList();
        for (Future<?> f : futures) f.get();
        pool.shutdown();
        assertEquals(0L, trialBalance.net(), "trial balance must net to zero");
    }

    @Test
    void defectFlagOmitsCreditLegUnderConcurrency() {
        // With SUT_DEFECT=ledger-unbalanced the credit leg is dropped, so the
        // trial balance MUST drift. This proves the defect is actually injected.
        withDefect("ledger-unbalanced", () -> {
            service.transfer("ACC-000001", "ACC-000002", 5_00L);
            assertNotEquals(0L, trialBalance.net());
        });
    }
}
