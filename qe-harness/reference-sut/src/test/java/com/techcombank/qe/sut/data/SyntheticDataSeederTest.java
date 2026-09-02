package com.techcombank.qe.sut.data;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link SyntheticDataSeeder} against a real Postgres instance via
 * Testcontainers -- not H2 or another compatibility-mode database. The
 * {@code account_ref_format} and {@code amount_nonzero} CHECK constraints in
 * {@code V1__accounts_and_ledger.sql} use Postgres-specific regex-operator
 * ({@code ~}) syntax, and this suite's whole point is proving they hold
 * against the real engine, not an emulation of it.
 */
@Testcontainers
class SyntheticDataSeederTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static JdbcTemplate jdbc;
    private static SyntheticDataSeeder seeder;

    @BeforeAll
    static void migrateAndConnect() {
        DataSource dataSource = new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());

        // connectRetries: the Testcontainers wait strategy confirms Postgres is
        // listening *inside* the container as soon as its log shows "ready to
        // accept connections", but on engines that proxy container ports through
        // a VM (observed on Rancher Desktop; not needed on Docker Desktop) the
        // host-side port-forward can lag a moment behind that signal, so the
        // very first external connection attempt can see "connection refused"
        // even though the container is genuinely up. Retrying absorbs that race
        // without weakening what the test actually proves.
        Flyway.configure()
            .dataSource(dataSource)
            .connectRetries(10)
            .connectRetriesInterval(1)
            .load()
            .migrate();

        jdbc = new JdbcTemplate(dataSource);
        seeder = new SyntheticDataSeeder(jdbc);
    }

    @BeforeEach
    void truncate() {
        jdbc.execute("TRUNCATE TABLE ledger_entry, account RESTART IDENTITY CASCADE");
    }

    @Test
    void seedIsDeterministicForTheSameSeed() {
        SeedSummary a = seeder.seed(42L);
        truncate();
        SeedSummary b = seeder.seed(42L);
        assertEquals(a, b);
    }

    @Test
    void noAccountIdentifierIsPanShaped() {
        seeder.seed(42L);
        List<String> ids = jdbc.queryForList("SELECT account_ref FROM account", String.class);
        Pattern pan = Pattern.compile("(?<!\\d)\\d{13,19}(?!\\d)");
        ids.forEach(id -> assertFalse(pan.matcher(id).find(), "PAN-shaped: " + id));
        ids.forEach(id -> assertTrue(id.matches("^ACC-\\d{6}$"), "bad format: " + id));
    }

    @Test
    void seededLedgerIsBalanced() {
        seeder.seed(42L);
        Long net = jdbc.queryForObject(
            "SELECT COALESCE(SUM(amount_minor), 0) FROM ledger_entry", Long.class);
        assertEquals(0L, net, "seed must not start the ledger out of balance");
    }

    /**
     * Not one of the brief's three given tests, but exercises the exact
     * mechanism this task exists to provide: {@code account_ref_format} is a
     * database-level CHECK constraint, so even a hand-crafted insert that
     * tries to write a PAN-shaped (or otherwise malformed) account_ref is
     * rejected by Postgres itself -- no future code path, including an
     * injected defect, can bypass it by skipping application code.
     */
    @Test
    void malformedAccountRefIsRejectedByCheckConstraint() {
        assertThrows(DataIntegrityViolationException.class, () ->
            jdbc.update(
                "INSERT INTO account (account_ref, party_name) VALUES (?, ?)",
                "NOT-A-VALID-ACCOUNT-REF", "Malformed Test Account"));
    }
}
