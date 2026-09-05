package com.techcombank.qe.sut.capability.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The topology's shape is itself load-bearing for two invariants, so it is
 * asserted directly rather than only implied by the modules.
 */
class MessagingTopologyTest extends AbstractMessagingIntegrationTest {

    @Test
    void declaresEveryObjectTheFourArchetypesNeed() {
        topology.declareTopology();
        for (String queue : new String[] {
                "qe.q.route.domestic", "qe.q.route.intl", "qe.q.unroutable",
                "qe.q.sequence", "qe.q.branch.a", "qe.q.branch.b", "qe.q.branch.c",
                "qe.q.aggregate", "qe.q.work", "qe.q.dlq" }) {
            assertNotNull(admin.getQueueProperties(queue), "missing queue: " + queue);
        }
    }

    @Test
    void theRouteExchangeHasNoCatchAllBinding() {
        topology.declareTopology();
        // TST-026 I2 asserts zero messages reach a default route. A '#' binding
        // would make that trivially true and the invariant worthless, so its
        // absence is asserted here rather than left to reviewer vigilance.
        // Published to qe.route, the exchange whose bindings the invariant is
        // actually about -- publishing to qe.in would exercise a different
        // exchange and pass for the wrong reason.
        rabbit.convertAndSend(MessagingTopology.ROUTE_EXCHANGE, "pay.unknown.type", "probe");
        assertTrue(awaitQueueDepth("qe.q.unroutable", 1),
            "an unmatched key must divert to quarantine, not vanish and not match a catch-all");
        assertEquals(0L, queueDepth("qe.q.route.domestic"));
        assertEquals(0L, queueDepth("qe.q.route.intl"));
    }

    @Test
    void theRetryLadderHasDistinctIntervals() {
        // TST-029 I4 asserts distinct_intervals > 1. A flat ladder fails that
        // invariant against the SUT's own declared backoff, so the declared
        // property is checked here at the source.
        assertTrue(retryIntervalsMs().size() > 1);
        assertEquals(retryIntervalsMs().size(), retryIntervalsMs().stream().distinct().count(),
            "every configured retry interval must differ");
    }

    @Test
    void theDlqBindingMatchesTheRoutingKeyDeadLetteredMessagesActuallyCarry() {
        topology.declareTopology();
        // qe.q.work has no x-dead-letter-routing-key override, so a message
        // dead-lettered from it arrives at qe.dlx carrying its ORIGINAL
        // routing key -- "work" (from bind(work).to(in).with("work")) -- not
        // the queue's own name "qe.q.work". qe.dlx is a DirectExchange
        // (exact-match), so this proves the qe.q.dlq binding is declared on
        // the key a dead-lettered message will actually carry, before any
        // later task's real publish/consume/reject logic exists to exercise
        // the path end-to-end.
        rabbit.convertAndSend(MessagingTopology.DLX, "work", "probe");
        assertTrue(awaitQueueDepth("qe.q.dlq", 1),
            "a message routed with key \"work\" must land in qe.q.dlq");
    }

    @Test
    void everyQueueIsDurable() {
        topology.declareTopology();
        // TST-029 I2: nothing acked-persisted may be lost across a broker
        // restart, which a transient queue cannot promise.
        assertTrue(topology.declarables().getDeclarablesByType(org.springframework.amqp.core.Queue.class)
            .stream().allMatch(org.springframework.amqp.core.Queue::isDurable));
    }
}
