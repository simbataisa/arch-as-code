package com.techcombank.qe.sut.capability.messaging;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Properties;

/** Observability surface the four messaging modules read as ground truth. */
@RestController
public class MessagingObservabilityController {

    private final MessageLog log;
    private final RabbitAdmin admin;
    private final MessagingTopology topology;
    private final long dlqAlertDepth;

    public MessagingObservabilityController(MessageLog log, RabbitAdmin admin,
                                            MessagingTopology topology,
                                            @Value("${app.messaging.dlq-alert-depth}") long dlqAlertDepth) {
        this.log = log;
        this.admin = admin;
        this.topology = topology;
        this.dlqAlertDepth = dlqAlertDepth;
    }

    /** GET /messaging/published-log -> every publication this SUT recorded. */
    @GetMapping("/messaging/published-log")
    public List<MessageLog.Published> publishedLog() {
        return log.published();
    }

    /** GET /messaging/emissions -> emission order, for TST-027's I1/I3. */
    @GetMapping("/messaging/emissions")
    public List<MessageLog.Emitted> emissions() {
        return log.emissions();
    }

    /** GET /messaging/dlq/depth -> {depth, alertDepth, alertFiring, exported}.
     *  `exported` is literally TST-029 I5's first clause: the metric must be
     *  observable at all, not merely correct. */
    @GetMapping("/messaging/dlq/depth")
    public DlqDepthResponse dlqDepth() {
        topology.declareTopology();
        long depth = depthOf(MessagingTopology.Q_DLQ);
        return new DlqDepthResponse(depth, dlqAlertDepth, dlqAlertFiring(depth), true);
    }

    /** I5's alert predicate, kept public so the SUT's own test can assert the
     *  boundary without going through HTTP. */
    public boolean dlqAlertFiring(long depth) {
        return depth > dlqAlertDepth;
    }

    private long depthOf(String queue) {
        Properties props = admin.getQueueProperties(queue);
        return props == null ? 0L : ((Number) props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).longValue();
    }

    public record DlqDepthResponse(long depth, long alertDepth, boolean alertFiring, boolean exported) {}
}
