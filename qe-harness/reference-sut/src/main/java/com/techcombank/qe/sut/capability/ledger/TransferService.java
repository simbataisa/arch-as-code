package com.techcombank.qe.sut.capability.ledger;

import com.techcombank.qe.sut.DefectFlags;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * TST-021 double-entry ledger capability: writes a balanced debit/credit
 * pair of {@code ledger_entry} rows for a transfer between two accounts, in
 * one transaction.
 *
 * <p><b>Deadlock avoidance under concurrency:</b> {@link #lockPair} resolves
 * both account ids and locks their rows with {@code SELECT ... FOR UPDATE}
 * in ascending {@code account_id} order -- never in {@code from}/{@code to}
 * call order. Without this, two concurrent transfers moving in opposite
 * directions between the same pair of accounts (A-&gt;B and B-&gt;A) could each
 * lock their own "from" row first and then block forever waiting for the
 * other's "to" row -- a classic transfer deadlock. Locking by a single,
 * global, id-ascending order means every transaction touching a given pair
 * of rows contends for the same first lock, so at most one proceeds at a
 * time: contention, not deadlock.
 *
 * <p><b>Defect injection:</b> when {@code DefectFlags.isActive("ledger-unbalanced")}
 * is true, the credit leg is skipped entirely -- the debit leg is still
 * written (and still counts against the locked rows), so the trial balance
 * provably drifts. See {@link com.techcombank.qe.sut.DefectFlags}.
 */
@Service
public class TransferService {

    private final JdbcTemplate jdbc;

    public TransferService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public UUID transfer(String from, String to, long amountMinor) {
        // TST-034 I3: with journey-starved active, the transfer journey is
        // deliberately delayed so its observed share collapses below its
        // declared tolerance. The ledger stays balanced throughout -- this
        // starves a journey, it does not corrupt state, so I1/I2 hold and I3
        // alone fails.
        //
        // 4000ms, not the originally-drafted 250ms: this magnitude was never
        // pinned by the design spec or by blended-journey-workload.md -- it
        // was an arbitrary detail invented while first writing this branch.
        // Empirically (Task 13), 250ms sat too close to this test
        // environment's own scheduling jitter (observed up to ~220ms on a
        // clean run, no defect involved at all) for plan.jmx's per-sub-window
        // starvation check to ever separate "defect" from "noisy but
        // healthy" reliably. 4000ms gives a wide, dependable margin over
        // that jitter without changing what I3 asserts.
        if (DefectFlags.isActive("journey-starved")) {
            try {
                Thread.sleep(4000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        AccountPair pair = lockPair(from, to);

        UUID ref = UUID.randomUUID();
        insertEntry(ref, pair.fromId(), -amountMinor);
        if (!DefectFlags.isActive("ledger-unbalanced")) {
            insertEntry(ref, pair.toId(), amountMinor);
        }
        return ref;
    }

    private AccountPair lockPair(String from, String to) {
        long fromId = idOf(from);
        long toId = idOf(to);

        long first = Math.min(fromId, toId);
        long second = Math.max(fromId, toId);

        lockRow(first);
        if (second != first) {
            lockRow(second);
        }

        return new AccountPair(fromId, toId);
    }

    private long idOf(String accountRef) {
        return jdbc.queryForObject(
            "SELECT id FROM account WHERE account_ref = ?", Long.class, accountRef);
    }

    private void lockRow(long accountId) {
        jdbc.queryForObject("SELECT id FROM account WHERE id = ? FOR UPDATE", Long.class, accountId);
    }

    private void insertEntry(UUID transferRef, long accountId, long amountMinor) {
        jdbc.update(
            "INSERT INTO ledger_entry (transfer_ref, account_id, amount_minor) VALUES (?, ?, ?)",
            transferRef, accountId, amountMinor);
    }

    private record AccountPair(long fromId, long toId) {}
}
