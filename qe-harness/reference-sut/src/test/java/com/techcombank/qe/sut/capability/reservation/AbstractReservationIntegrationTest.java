package com.techcombank.qe.sut.capability.reservation;

import com.techcombank.qe.sut.DefectFlags;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Postgres-via-Testcontainers fixture for the reservation capability (TST-023).
 *
 * <p>Singleton container in a static initialiser, deliberately not
 * {@code @Testcontainers}/{@code @Container} -- see
 * {@code AbstractLedgerIntegrationTest}'s javadoc for the exact failure mode
 * that pattern causes under Spring's context caching (a stale DataSource
 * pointing at a torn-down container's port).
 *
 * <p>The Hikari pool is widened so the concurrency test's 16-thread pool
 * blocks on the deliberate row lock -- the only contention this suite means to
 * exercise -- rather than on connection checkout.
 */
@SpringBootTest
abstract class AbstractReservationIntegrationTest {

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
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 20);
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ReservationService service;

    @Autowired
    protected ReservationSweeper sweeper;

    @BeforeEach
    void resetReservationFixture() {
        DefectFlags.clear();
        jdbc.execute("TRUNCATE TABLE reservation, account_limit, ledger_entry, account "
            + "RESTART IDENTITY CASCADE");
        jdbc.update("INSERT INTO account (account_ref, party_name) VALUES (?, ?)",
            "ACC-000001", "Test Fixture Limit Holder Co");
        jdbc.update("INSERT INTO account_limit (account_id, declared_limit, ttl_seconds) "
            + "SELECT id, ?, ? FROM account WHERE account_ref = ?", 10L, 60L, "ACC-000001");
    }

    protected long declaredLimit(String accountRef) {
        return service.declaredLimit(accountRef);
    }

    /** Activates {@code flag} for the duration of {@code action}, always clearing it
     *  afterwards even if {@code action} throws. */
    protected void withDefect(String flag, Runnable action) {
        DefectFlags.activate(flag);
        try {
            action.run();
        } finally {
            DefectFlags.clear();
        }
    }
}
