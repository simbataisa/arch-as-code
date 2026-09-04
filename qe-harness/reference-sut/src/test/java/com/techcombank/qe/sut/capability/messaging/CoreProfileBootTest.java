package com.techcombank.qe.sut.capability.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The application context MUST start with no broker reachable.
 *
 * <p>This is the guard for Wave 17's most dangerous change. reference-sut is in
 * compose profile ["core"]; broker is in ["messaging"]. Its container
 * healthcheck hits /_capabilities, so if a missing broker failed the context at
 * startup, `make up PROFILES=core` would report an unhealthy SUT and every one
 * of the seven pre-existing modules would break. The AMQP connection is
 * therefore lazy: beans exist, no socket is opened until first use.
 *
 * <p>No RabbitMQ container is started here, deliberately -- the absence is the
 * test.
 */
@SpringBootTest
class CoreProfileBootTest {

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
        // Point at a port nothing is listening on: proof the context does not
        // need the broker to exist.
        registry.add("spring.rabbitmq.host", () -> "127.0.0.1");
        registry.add("spring.rabbitmq.port", () -> 1);
    }

    @Autowired
    private ConnectionFactory connectionFactory;

    @Test
    void contextStartsWithNoBrokerReachable() {
        assertNotNull(connectionFactory, "the bean must exist without a live connection");
    }
}
