package com.techcombank.qe.sut.capability.ledger;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * TST-021 double-entry ledger capability: the trial balance is the sum of
 * {@code amount_minor} across every {@code ledger_entry} row. A correctly
 * functioning ledger keeps this at zero forever, no matter how many
 * transfers are written -- every transfer is a debit/credit pair of equal,
 * opposite magnitude. A non-zero net is the invariant violation the harness
 * (a later task) asserts against, and is exactly what
 * {@code DefectFlags.isActive("ledger-unbalanced")} is designed to produce.
 */
@Service
public class TrialBalanceService {

    private final JdbcTemplate jdbc;

    public TrialBalanceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Sum of amount_minor across every ledger_entry row. Zero iff balanced. */
    public long net() {
        Long net = jdbc.queryForObject(
            "SELECT COALESCE(SUM(amount_minor), 0) FROM ledger_entry", Long.class);
        return net;
    }

    public long entryCount() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM ledger_entry", Long.class);
        return count;
    }
}
