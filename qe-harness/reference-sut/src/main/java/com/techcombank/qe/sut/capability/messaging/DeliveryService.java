package com.techcombank.qe.sut.capability.messaging;

import com.techcombank.qe.sut.DefectFlags;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TST-029 delivery guarantee, retry and DLQ capability.
 *
 * <p><b>I1 is a conservation law:</b> submitted == processed + dead-lettered.
 * The counters live here, on the publish path, rather than being read back from
 * the broker -- scoring a delivery guarantee against the broker's own
 * accounting would ask the component under test to grade itself.
 *
 * <p><b>The retry ladder's intervals must differ</b> or I4's
 * {@code distinct_intervals > 1} fails against the SUT's own declared backoff.
 * The ladder is declared in {@link MessagingTopology} from
 * {@code app.messaging.retry-intervals-ms}; this service only counts attempts.
 *
 * <p><b>Defect injection:</b> {@code dlq-bypass-drop} acknowledges a poison
 * message without processing it and without dead-lettering it -- the message
 * simply vanishes, so I1's conservation law breaks while the retry ladder and
 * the alert threshold stay intact.
 *
 * <p><b>The defect decision is captured at publish time, not re-read live in
 * the consumer.</b> {@link #consume(String)} runs on the AMQP listener's own
 * thread, asynchronously and with no ordering guarantee relative to the
 * publishing thread. {@code AbstractMessagingIntegrationTest#withDefect}
 * clears the flag in a {@code finally} immediately after the (synchronous)
 * {@code submit()} call returns -- which races the consumer thread and, in
 * practice, wins almost every time, since a publish-then-clear round trip on
 * the calling thread is faster than a broker delivering to a separate
 * consumer channel. Re-checking {@code DefectFlags.isActive(...)} inside
 * {@code consume()} would therefore almost always see the flag already
 * cleared and take the normal reject path instead of the defect path, making
 * the defect test fail (not flake -- fail every time) even though the flag
 * genuinely was active for the entire synchronous extent of {@code submit()}.
 * Baking the decision into the message body at publish time -- while the flag
 * is still guaranteed active -- makes the defect deterministic instead of a
 * race with the test's cleanup.
 *
 * <p><b>The listener container is lazy too, exactly like {@link RabbitAdmin}
 * (Task 14).</b> {@code @RabbitListener}'s container is a Spring
 * {@code SmartLifecycle} bean that, by default, connects to the broker and
 * starts consuming during context refresh -- unconditionally, regardless of
 * {@code RabbitAdmin.setAutoStartup(false)}, which only governs topology
 * declaration. reference-sut runs in compose profile {@code ["core"]} with
 * the broker in {@code ["messaging"]} (see {@code MessagingConnectionConfig},
 * pinned by {@code CoreProfileBootTest}), so an eagerly-starting container
 * would try to authenticate against whatever happens to be listening on the
 * default AMQP port at context-refresh time -- in a shared dev/CI host this
 * can be an unrelated, already-running broker with different credentials,
 * which Spring AMQP treats as a FATAL startup exception (unlike a merely
 * unreachable broker, which it retries in the background) and would fail the
 * context for every {@code @SpringBootTest} in the whole application, not
 * just messaging ones. {@code spring.rabbitmq.listener.simple.auto-startup=
 * false} keeps the container from starting on context refresh; this class
 * starts it explicitly, once, on first use -- after {@link
 * MessagingTopology#declareTopology()} has ensured {@code qe.q.work} exists. */
@Service
public class DeliveryService {

    private final RabbitTemplate rabbit;
    private final MessagingTopology topology;
    private final RabbitAdmin admin;
    private final MessageLog log;
    private final RabbitListenerEndpointRegistry listenerRegistry;
    private final int maxDeliveryAttempts;
    private volatile boolean listenerStarted;

    private final AtomicLong submitted = new AtomicLong();
    private final AtomicLong stateChanges = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();

    public DeliveryService(RabbitTemplate rabbit, MessagingTopology topology, RabbitAdmin admin,
                           MessageLog log, RabbitListenerEndpointRegistry listenerRegistry,
                           @Value("${app.messaging.max-delivery-attempts}") int maxDeliveryAttempts) {
        this.rabbit = rabbit;
        this.topology = topology;
        this.admin = admin;
        this.log = log;
        this.listenerRegistry = listenerRegistry;
        this.maxDeliveryAttempts = maxDeliveryAttempts;
    }

    /** Declares the topology, then starts the (otherwise lazy) listener
     *  container -- idempotent, and always in that order, so the container
     *  never attaches before {@code qe.q.work} exists. */
    private void ensureReady() {
        topology.declareTopology();
        if (!listenerStarted) {
            synchronized (this) {
                if (!listenerStarted) {
                    listenerRegistry.start();
                    listenerStarted = true;
                }
            }
        }
    }

    public void reset() {
        ensureReady();
        admin.purgeQueue(MessagingTopology.Q_WORK, true);
        admin.purgeQueue(MessagingTopology.Q_DLQ, true);
        submitted.set(0);
        stateChanges.set(0);
        dropped.set(0);
        attempts.clear();
        log.clear();
    }

    /** Submits a job. {@code poison} marks a message the consumer will always
     *  reject, so it must exhaust the ladder and dead-letter.
     *
     *  <p>Whether the {@code dlq-bypass-drop} defect applies to THIS message
     *  is decided here, while the flag is still guaranteed active for the
     *  caller (see the class javadoc), and baked into the body the async
     *  consumer later reads -- not re-read live from the flag in
     *  {@link #consume(String)}. */
    public void submit(String jobId, boolean poison) {
        ensureReady();
        submitted.incrementAndGet();
        log.recordPublished("work", jobId);
        boolean bypass = poison && DefectFlags.isActive("dlq-bypass-drop");
        String prefix = bypass ? "bypass:" : (poison ? "poison:" : "job:");
        rabbit.convertAndSend(MessagingTopology.IN_EXCHANGE, "work", prefix + jobId);
    }

    @RabbitListener(queues = MessagingTopology.Q_WORK)
    public void consume(String body) {
        String jobId = body.substring(body.indexOf(':') + 1);
        boolean poison = body.startsWith("poison:");
        boolean bypass = body.startsWith("bypass:");
        attempts.merge(jobId, 1, Integer::sum);

        if (!poison && !bypass) {
            stateChanges.incrementAndGet();
            return;
        }

        if (bypass) {
            // The defect: acknowledge without processing and without
            // dead-lettering. The message is simply gone, so I1's conservation
            // law breaks. The ladder and the alert threshold are untouched.
            dropped.incrementAndGet();
            return;
        }

        // Reject without requeue: the queue's x-dead-letter-exchange sends it
        // to qe.dlx once x-delivery-limit is exhausted, which is what makes
        // I3's "does not block the queue" and I6's ceiling both hold.
        throw new org.springframework.amqp.AmqpRejectAndDontRequeueException(
            "poison message rejected: " + jobId);
    }

    public long submitted() {
        return submitted.get();
    }

    public long stateChanges() {
        return stateChanges.get();
    }

    public long dlqCount() {
        Properties props = admin.getQueueProperties(MessagingTopology.Q_DLQ);
        return props == null ? 0L : ((Number) props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).longValue();
    }

    public int attemptsFor(String jobId) {
        return attempts.getOrDefault(jobId, 0);
    }

    public int maxDeliveryAttempts() {
        return maxDeliveryAttempts;
    }

    /** Bounded polls. Every wait in this capability has a declared deadline --
     *  an unbounded wait on a broker is how a hung test becomes a green one. */
    public boolean awaitSettled(long expected, long budgetMs) {
        return await(() -> stateChanges.get() + dlqCount() + dropped.get() >= expected, budgetMs);
    }

    public boolean awaitDlq(long expected, long budgetMs) {
        return await(() -> dlqCount() >= expected, budgetMs);
    }

    public boolean awaitStateChanges(long expected, long budgetMs) {
        return await(() -> stateChanges.get() >= expected, budgetMs);
    }

    private boolean await(java.util.function.BooleanSupplier condition, long budgetMs) {
        Instant deadline = Instant.now().plus(Duration.ofMillis(budgetMs));
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
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
}
