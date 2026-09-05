package com.techcombank.qe.sut.capability.ledger;

import com.techcombank.qe.sut.DefectFlags;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared Postgres-via-Testcontainers fixture for the ledger capability
 * (TST-021) test suite. This is the second Wave 16 task needing a live
 * Postgres instance -- {@code SyntheticDataSeederTest} (Task 6) wires its own
 * {@link org.springframework.jdbc.datasource.DriverManagerDataSource} and
 * runs Flyway directly, deliberately without a Spring context, because it
 * only ever needs a {@link JdbcTemplate}.
 *
 * <p>This suite differs: {@link TransferService#transfer} is
 * {@code @Transactional}, and Spring's declarative transaction support only
 * applies through an AOP proxy around a bean managed by a real
 * {@code ApplicationContext}. So this base class boots a full
 * {@code @SpringBootTest} context against the same Testcontainers Postgres
 * pattern -- {@code -Djava.net.preferIPv4Stack=true} (pom.xml) and generous
 * connect-retries (here via the {@code spring.flyway.connect-retries}
 * property, the Spring Boot auto-configured equivalent of Task 6's
 * {@code Flyway.configure().connectRetries(10)}) -- rather than duplicating
 * this task's own DataSource/Flyway wiring.
 *
 * <p><b>Deliberate singleton-container pattern (not {@code @Testcontainers}/
 * {@code @Container}):</b> {@code @SpringBootTest} caches its
 * {@code ApplicationContext} by configuration signature, and both
 * {@code TransferServiceTest} and {@code LedgerConcurrencyTest} share an
 * identical signature (same base class, no per-subclass config) -- so Spring
 * reuses a single cached context, and its {@code DataSource}/HikariPool bean,
 * across both test classes. A per-class {@code @Container} lifecycle (start
 * before the class, stop after) would tear down the first class's Postgres
 * container while the second class's tests still ran against the cached, now
 * stale {@code DataSource} pointing at the dead container's port -- this was
 * observed directly: the second test class failed with "Connection to
 * localhost:&lt;first container's port&gt; refused" even though a second,
 * healthy container had already started on a different port. Starting one
 * container for the whole JVM and never stopping it (Testcontainers' own
 * documented "singleton container" pattern, needed here specifically because
 * of Spring's context caching) keeps the container's lifetime at least as
 * long as the cached context's.
 *
 * <p>The Hikari pool is widened past its 10-connection default so that the
 * concurrency test's 16-thread pool never blocks on connection checkout
 * (as opposed to blocking on the deliberate row lock, which is the only
 * contention this suite means to exercise).
 */
@SpringBootTest
abstract class AbstractLedgerIntegrationTest {

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
    protected TransferService service;

    @Autowired
    protected TrialBalanceService trialBalance;

    @Autowired
    protected IdempotencyService idempotency;

    @BeforeEach
    void resetLedgerFixture() {
        DefectFlags.clear();
        // idempotency_key has no FK to account, so CASCADE does not reach it --
        // truncate it explicitly or keys leak into the next test.
        jdbc.execute("TRUNCATE TABLE idempotency_key RESTART IDENTITY");
        jdbc.execute("TRUNCATE TABLE ledger_entry, account RESTART IDENTITY CASCADE");
        jdbc.update("INSERT INTO account (account_ref, party_name) VALUES (?, ?)",
            "ACC-000001", "Test Fixture Debtor Co");
        jdbc.update("INSERT INTO account (account_ref, party_name) VALUES (?, ?)",
            "ACC-000002", "Test Fixture Creditor Co");
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
