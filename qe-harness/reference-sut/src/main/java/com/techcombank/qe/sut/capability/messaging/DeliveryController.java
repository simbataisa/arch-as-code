package com.techcombank.qe.sut.capability.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** TST-029's HTTP surface. */
@RestController
public class DeliveryController {

    private final DeliveryService delivery;
    private final List<Long> retryIntervalsMs;
    private final long dlqAlertDepth;

    public DeliveryController(DeliveryService delivery,
                              @Value("${app.messaging.retry-intervals-ms}") List<Long> retryIntervalsMs,
                              @Value("${app.messaging.dlq-alert-depth}") long dlqAlertDepth) {
        this.delivery = delivery;
        this.retryIntervalsMs = List.copyOf(retryIntervalsMs);
        this.dlqAlertDepth = dlqAlertDepth;
    }

    /** POST /messaging/work?jobId=job-0001&poison=false -> 202. */
    @PostMapping("/messaging/work")
    public ResponseEntity<Void> submit(@RequestParam String jobId,
                                       @RequestParam(defaultValue = "false") boolean poison) {
        delivery.submit(jobId, poison);
        return ResponseEntity.accepted().build();
    }

    /** POST /messaging/delivery/reset -> 204. */
    @PostMapping("/messaging/delivery/reset")
    public ResponseEntity<Void> reset() {
        delivery.reset();
        return ResponseEntity.noContent().build();
    }

    /** GET /messaging/delivery/state -> the whole verdict in one call: the
     *  conservation-law counters, the declared ceiling, the declared ladder and
     *  the alert threshold. Every declared value is returned so the harness
     *  asserts against configuration rather than literals of its own. */
    @GetMapping("/messaging/delivery/state")
    public StateResponse state() {
        return new StateResponse(
            delivery.submitted(), delivery.stateChanges(), delivery.dlqCount(),
            delivery.maxDeliveryAttempts(), retryIntervalsMs,
            retryIntervalsMs.stream().distinct().count(), dlqAlertDepth,
            delivery.dlqCount() > dlqAlertDepth, true);
    }

    public record StateResponse(long submitted, long stateChanges, long dlqCount,
                                int maxDeliveryAttempts, List<Long> retryIntervalsMs,
                                long distinctIntervals, long dlqAlertDepth,
                                boolean alertFiring, boolean dlqDepthExported) {}
}
