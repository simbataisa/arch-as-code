package com.techcombank.qe.sut.capability.reporting;

import com.techcombank.qe.sut.DefectFlags;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Postgres-via-Testcontainers fixture for the reporting capability (TST-037).
 * Singleton container in a static initialiser -- see
 * AbstractLedgerIntegrationTest's javadoc for why not @Container.
 */
@SpringBootTest
abstract class AbstractReportingIntegrationTest {

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
    protected ReportingService service;

    /** Read from the declared property, never duplicated as a literal. */
    @Value("${app.readmodel.convergence-bound-ms}")
    private long convergenceBoundMs;

    protected long convergenceBoundMs() {
        return convergenceBoundMs;
    }

    @BeforeEach
    void resetReportingFixture() {
        DefectFlags.clear();
        // outbox has no FK to account, so CASCADE does not reach it -- truncate
        // it explicitly or published rows leak into the next test.
        jdbc.execute("TRUNCATE TABLE outbox RESTART IDENTITY");
        jdbc.execute("TRUNCATE TABLE report_refresh_timestamp, ledger_entry, account "
            + "RESTART IDENTITY CASCADE");
        jdbc.update("INSERT INTO account (account_ref, party_name) VALUES (?, ?)",
            "ACC-000001", "Test Fixture Reporting Co");
        jdbc.update("INSERT INTO ledger_entry (transfer_ref, account_id, amount_minor) "
            + "SELECT gen_random_uuid(), id, 500 FROM account WHERE account_ref = ?", "ACC-000001");
    }

    protected void withDefect(String flag, Runnable action) {
        DefectFlags.activate(flag);
        try {
            action.run();
        } finally {
            DefectFlags.clear();
        }
    }
}
