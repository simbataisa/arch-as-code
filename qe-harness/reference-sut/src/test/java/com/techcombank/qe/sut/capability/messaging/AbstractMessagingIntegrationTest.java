package com.techcombank.qe.sut.capability.messaging;

import com.techcombank.qe.sut.DefectFlags;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Postgres + RabbitMQ via Testcontainers for the messaging capability.
 *
 * <p>Both containers are singletons in a static initialiser, deliberately not
 * {@code @Testcontainers}/{@code @Container} -- see
 * {@code AbstractLedgerIntegrationTest}'s javadoc for the stale-DataSource
 * failure that pattern causes under Spring's context caching. The same hazard
 * applies to the broker: a per-class container lifecycle would tear the broker
 * down while a second cached context still pointed at its port.
 */
@SpringBootTest
abstract class AbstractMessagingIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static final RabbitMQContainer BROKER = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    static {
        POSTGRES.start();
        BROKER.start();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.connect-retries", () -> 10);
        registry.add("spring.rabbitmq.host", BROKER::getHost);
        registry.add("spring.rabbitmq.port", BROKER::getAmqpPort);
        registry.add("spring.rabbitmq.username", BROKER::getAdminUsername);
        registry.add("spring.rabbitmq.password", BROKER::getAdminPassword);
    }

    @Autowired
    protected RabbitTemplate rabbit;

    @Autowired
    protected RabbitAdmin admin;

    @Autowired
    protected MessagingTopology topology;

    @Autowired
    protected MessageLog log;

    @Autowired
    protected MessagingObservabilityController observability;

    @Autowired
    protected RoutingService routing;

    @Value("${app.messaging.retry-intervals-ms}")
    private List<Long> retryIntervalsMs;

    @Value("${app.messaging.dlq-alert-depth}")
    private long dlqAlertDepth;

    protected List<Long> retryIntervalsMs() {
        return retryIntervalsMs;
    }

    protected long dlqAlertDepth() {
        return dlqAlertDepth;
    }

    @BeforeEach
    void resetMessagingFixture() {
        DefectFlags.clear();
        log.clear();
        topology.declareTopology();
        for (String q : new String[] {
                MessagingTopology.Q_DOMESTIC, MessagingTopology.Q_INTL,
                MessagingTopology.Q_UNROUTABLE, MessagingTopology.Q_SEQUENCE,
                MessagingTopology.Q_BRANCH_A, MessagingTopology.Q_BRANCH_B,
                MessagingTopology.Q_BRANCH_C, MessagingTopology.Q_AGGREGATE,
                MessagingTopology.Q_WORK, MessagingTopology.Q_DLQ }) {
            admin.purgeQueue(q, true);
        }
    }

    protected long queueDepth(String queue) {
        java.util.Properties props = admin.getQueueProperties(queue);
        return props == null ? 0L : ((Number) props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).longValue();
    }

    /** Polls to a bounded deadline, then gives up. Every wait in this suite is
     *  bounded and declared -- an unbounded wait on a broker is how a hung test
     *  becomes a green one. */
    protected boolean awaitQueueDepth(String queue, long expected) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            if (queueDepth(queue) >= expected) {
                return true;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
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
