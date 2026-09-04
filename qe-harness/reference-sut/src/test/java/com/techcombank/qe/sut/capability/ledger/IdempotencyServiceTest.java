package com.techcombank.qe.sut.capability.ledger;

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

/** TST-020 idempotency and replay. */
class IdempotencyServiceTest extends AbstractLedgerIntegrationTest {

    @Test
    void repeatedSameKeyRequestsProduceOneStateChange() {
        String key = "idem-0001";
        for (int i = 0; i < 5; i++) {
            idempotency.execute(key, requestBody(500L), () -> service.transfer("ACC-000001", "ACC-000002", 500L));
        }
        assertEquals(2, ledgerEntryCount(), "I1: five same-key requests, one balanced pair");
    }

    @Test
    void aReplayReturnsAByteIdenticalStoredResponse() {
        String key = "idem-0002";
        String first = idempotency.execute(key, requestBody(500L),
            () -> service.transfer("ACC-000001", "ACC-000002", 500L)).body();
        String replay = idempotency.execute(key, requestBody(500L),
            () -> service.transfer("ACC-000001", "ACC-000002", 500L)).body();
        assertEquals(first, replay, "I2: the replay must be byte-identical, not merely equivalent");
    }

    @Test
    void distinctKeysProduceDistinctStateChanges() {
        for (int i = 1; i <= 3; i++) {
            idempotency.execute("idem-100" + i, requestBody(500L),
                () -> service.transfer("ACC-000001", "ACC-000002", 500L));
        }
        assertEquals(6, ledgerEntryCount(), "I3: three distinct keys, three balanced pairs");
    }

    @Test
    void sameKeyWithADifferentPayloadIsAConflict() {
        String key = "idem-0003";
        idempotency.execute(key, requestBody(500L),
            () -> service.transfer("ACC-000001", "ACC-000002", 500L));
        assertThrows(IdempotencyService.PayloadConflict.class,
            () -> idempotency.execute(key, requestBody(999L),
                () -> service.transfer("ACC-000001", "ACC-000002", 999L)),
            "I4: a reused key with a changed payload must conflict, never silently replay");
    }

    @Test
    void underTrueConcurrencyOneWinsAndTheRestAreServedTheStoredResponse() throws Exception {
        String key = "idem-0004";
        String body = requestBody(500L);
        List<Callable<String>> calls = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            calls.add(() -> idempotency.execute(key, body,
                () -> service.transfer("ACC-000001", "ACC-000002", 500L)).body());
        }
        ExecutorService pool = Executors.newFixedThreadPool(12);
        List<String> bodies = new ArrayList<>();
        try {
            for (Future<String> f : pool.invokeAll(calls)) {
                bodies.add(f.get());
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(2, ledgerEntryCount(), "I5: exactly one winner writes state");
        assertEquals(1, bodies.stream().distinct().count(),
            "I5: every caller sees the same stored response");
    }

    @Test
    void theKeyTtlCoversTheDeclaredClientRetryWindow() {
        // I6 is a configuration relationship, so it is asserted against the two
        // declared properties rather than by waiting out a TTL.
        assertTrue(idempotency.keyTtlSeconds() >= idempotency.clientMaxRetryWindowSeconds(),
            "I6: key TTL must be at least the declared client retry window");
    }

    @Test
    void ignoredKeyDefectBreaksOnlyTheDeduplicationInvariant() {
        String key = "idem-0005";
        withDefect("idempotency-key-ignored", () -> {
            for (int i = 0; i < 3; i++) {
                idempotency.execute(key, requestBody(500L),
                    () -> service.transfer("ACC-000001", "ACC-000002", 500L));
            }
        });
        assertEquals(6, ledgerEntryCount(), "the defect must write three pairs for one key");
        assertEquals(0L, trialBalance.net(),
            "the defect must be specific: the ledger stays balanced");
    }

    private String requestBody(long amountMinor) {
        return "{\"from\":\"ACC-000001\",\"to\":\"ACC-000002\",\"amountMinor\":" + amountMinor + "}";
    }

    private long ledgerEntryCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM ledger_entry", Long.class);
    }
}
