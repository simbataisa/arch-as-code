package com.techcombank.qe.sut.capability.ledger;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TST-021 double-entry ledger: single-transfer correctness. See
 * {@link LedgerConcurrencyTest} for the concurrent-transfer and
 * defect-injection scenarios.
 */
class TransferServiceTest extends AbstractLedgerIntegrationTest {

    @Test
    void transferWritesBalancedPairInOneTransaction() {
        UUID ref = service.transfer("ACC-000001", "ACC-000002", 5_00L);
        List<Long> amounts = jdbc.queryForList(
            "SELECT amount_minor FROM ledger_entry WHERE transfer_ref = ?", Long.class, ref);
        assertEquals(2, amounts.size());
        assertEquals(0L, amounts.stream().mapToLong(Long::longValue).sum());
    }
}
