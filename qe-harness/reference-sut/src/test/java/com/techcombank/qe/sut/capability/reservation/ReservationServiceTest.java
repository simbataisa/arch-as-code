package com.techcombank.qe.sut.capability.reservation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-023 concurrent limit and counter. The declared limit L is a per-account
 * fixture value read from the SUT's own data -- it is not an NFR SLO, which is
 * why no threshold_ref accompanies these assertions (see the design spec 7.1).
 */
class ReservationServiceTest extends AbstractReservationIntegrationTest {

    @Test
    void admitsExactlyTheDeclaredLimitUnderConcurrency() throws Exception {
        long limit = declaredLimit("ACC-000001");
        int attempts = (int) limit + 8;

        List<Callable<Boolean>> calls = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            calls.add(() -> {
                try {
                    service.reserve("ACC-000001", 1L);
                    return true;
                } catch (ReservationService.LimitExceeded e) {
                    return false;
                }
            });
        }

        ExecutorService pool = Executors.newFixedThreadPool(16);
        long admitted;
        try {
            List<Future<Boolean>> results = pool.invokeAll(calls);
            admitted = results.stream().filter(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).count();
        } finally {
            pool.shutdownNow();
        }

        assertEquals(limit, admitted, "I1: success count must equal min(N, L)");
        assertEquals(limit, service.utilisation("ACC-000001"),
            "I2: utilisation must never exceed the declared limit");
    }

    @Test
    void releaseReturnsExactlyItsOwnAmount() {
        long id = service.reserve("ACC-000001", 3L);
        assertEquals(3L, service.utilisation("ACC-000001"));
        service.release(id);
        assertEquals(0L, service.utilisation("ACC-000001"), "I3: rollback returns its own amount");
    }

    @Test
    void doubleReleaseIsRejected() {
        long id = service.reserve("ACC-000001", 2L);
        service.release(id);
        assertThrows(ReservationService.NotReleasable.class, () -> service.release(id),
            "I4: a second release must be rejected, not silently succeed");
        assertEquals(0L, service.utilisation("ACC-000001"));
    }

    @Test
    void overcommitDefectBreaksOnlyTheCapacityInvariants() {
        long limit = declaredLimit("ACC-000001");
        withDefect("reservation-overcommit", () -> {
            for (int i = 0; i < limit + 5; i++) {
                service.reserve("ACC-000001", 1L);
            }
        });
        assertTrue(service.utilisation("ACC-000001") > limit,
            "the defect must drive utilisation past the declared limit");
    }
}
