package com.techcombank.qe.sut.capability.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.techcombank.qe.sut.DefectFlags;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.InputStream;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared Postgres-via-Testcontainers + MockMvc fixture for the TST-030
 * versioned-API capability's test suite ({@link SchemaCompatibilityTest}).
 *
 * <p>{@code /v1/transfers} and {@code /v2/transfers} both delegate to
 * {@link com.techcombank.qe.sut.capability.ledger.TransferService}
 * (Task 7), whose {@code @Transactional} method only runs through Spring's
 * AOP proxy inside a real {@code ApplicationContext} -- so, exactly like
 * that capability's own {@code AbstractLedgerIntegrationTest}, this suite
 * needs a full {@code @SpringBootTest} against a real Postgres, not a
 * fixture or a mocked {@code JdbcTemplate}.
 *
 * <p><b>Why this duplicates {@code AbstractLedgerIntegrationTest} instead of
 * extending it:</b> that class is package-private in
 * {@code com.techcombank.qe.sut.capability.ledger} (Task 7's own review
 * already flagged this exact cross-package limitation), so it cannot be
 * {@code extends}ed from this package. Rather than promote it to a shared
 * {@code public} location for a second occurrence, this class replicates its
 * documented singleton-container pattern directly: {@code @SpringBootTest}
 * caches its {@code ApplicationContext} by configuration signature, and this
 * class additionally declares {@code @AutoConfigureMockMvc} (the ledger
 * suite does not need MockMvc, since it calls {@code TransferService}
 * directly) -- a different signature, so this suite gets its own cached
 * context and therefore needs its own Postgres container, started once in a
 * {@code static {}} block and never explicitly stopped, per Testcontainers'
 * own documented "singleton container" pattern. See
 * {@code AbstractLedgerIntegrationTest}'s javadoc for the full account of the
 * context-caching hazard a per-class {@code @Container} lifecycle would
 * otherwise hit.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class AbstractContractIntegrationTest {

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
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void resetContractFixture() {
        DefectFlags.clear();
        jdbc.execute("TRUNCATE TABLE ledger_entry, account RESTART IDENTITY CASCADE");
        jdbc.update("INSERT INTO account (account_ref, party_name) VALUES (?, ?)",
            "ACC-000001", "Test Fixture Debtor Co");
        jdbc.update("INSERT INTO account (account_ref, party_name) VALUES (?, ?)",
            "ACC-000002", "Test Fixture Creditor Co");
    }

    /** A valid {@code {from, to, amountMinor}} transfer request body -- the
     *  two accounts {@link #resetContractFixture} seeds before every test. */
    protected record TransferRequestFixture(String from, String to, long amountMinor) {}

    protected TransferRequestFixture validRequest() {
        return new TransferRequestFixture("ACC-000001", "ACC-000002", 5_00L);
    }

    /** POSTs {@code requestBody} as JSON to {@code path}, asserts 201
     *  Created, and returns the raw response body string. */
    protected String post(String path, Object requestBody) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    }

    /** Parses a raw JSON response body for validation against a
     *  {@link JsonSchema}. */
    protected JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    /** Loads a published contract fixture from
     *  {@code src/main/resources/contracts/<fixture>}. */
    protected JsonSchema schema(String fixture) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/contracts/" + fixture)) {
            JsonNode schemaNode = objectMapper.readTree(in);
            return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaNode);
        }
    }

    @FunctionalInterface
    protected interface ThrowingRunnable {
        void run() throws Exception;
    }

    /** Activates {@code flag} for the duration of {@code action}, always
     *  clearing it afterwards even if {@code action} throws -- same pattern
     *  as the ledger, rate-limiter, and authz capabilities' own
     *  {@code withDefect} helpers. */
    protected void withDefect(String flag, ThrowingRunnable action) throws Exception {
        DefectFlags.activate(flag);
        try {
            action.run();
        } finally {
            DefectFlags.clear();
        }
    }
}
