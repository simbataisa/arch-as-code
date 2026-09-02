package com.techcombank.qe.sut.capability.recon;

import com.techcombank.qe.sut.DefectFlags;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared Postgres-via-Testcontainers fixture for the TST-039 reconciliation
 * capability's test suite ({@link ReconServiceTest}).
 *
 * <p>{@link ReconService} and {@link DefectSeeder} both read/write the real
 * {@code account_balance_report} materialized view and
 * {@code report_refresh_timestamp} table introduced by
 * {@code V2__reporting_view.sql} -- there is no in-memory fixture for a
 * materialized view, so this suite needs a full {@code @SpringBootTest}
 * against a real Postgres, exactly like the ledger (Task 7) and contract
 * (Task 10) capabilities before it.
 *
 * <p><b>Why this duplicates {@code AbstractLedgerIntegrationTest} instead of
 * extending it:</b> that class is package-private in
 * {@code com.techcombank.qe.sut.capability.ledger} (Task 7's own review
 * already flagged this exact cross-package limitation, and Task 10's
 * {@code AbstractContractIntegrationTest} replicated the pattern for the
 * same reason), so it cannot be {@code extends}ed from this package. This
 * class replicates its documented singleton-container pattern directly:
 * {@code @SpringBootTest} caches its {@code ApplicationContext} by
 * configuration signature, and this suite's signature (no MockMvc, its own
 * package) differs from both prior suites', so it gets its own cached
 * context and therefore its own Postgres container, started once in a
 * {@code static {}} block and never explicitly stopped, per Testcontainers'
 * own documented "singleton container" pattern. See
 * {@code AbstractLedgerIntegrationTest}'s javadoc for the full account of
 * the context-caching hazard a per-class {@code @Container} lifecycle would
 * otherwise hit.
 */
@SpringBootTest
abstract class AbstractReconIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.connect-retries", () -> 10);
        registry.add("spring.flyway.connect-retries-interval", () -> "1s");
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ReconService service;

    @Autowired
    protected DefectSeeder defectSeeder;

    /** Resets both the source-of-truth tables and the reporting view/
     *  bookkeeping table so every test starts from a genuinely empty,
     *  in-sync state -- not just an empty {@code ledger_entry}. */
    @BeforeEach
    void resetReconFixture() {
        DefectFlags.clear();
        jdbc.execute("TRUNCATE TABLE report_refresh_timestamp");
        jdbc.execute("TRUNCATE TABLE ledger_entry, account RESTART IDENTITY CASCADE");
        jdbc.execute("REFRESH MATERIALIZED VIEW account_balance_report");
    }

    /** Activates {@code flag} for the duration of {@code action}, always
     *  clearing it afterwards even if {@code action} throws -- same pattern
     *  as the ledger, contract, rate-limiter, authz, and resilience
     *  capabilities' own {@code withDefect} helpers. */
    protected void withDefect(String flag, Runnable action) {
        DefectFlags.activate(flag);
        try {
            action.run();
        } finally {
            DefectFlags.clear();
        }
    }
}
