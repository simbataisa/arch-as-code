package com.techcombank.qe.sut.capability.messaging;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** TST-028's HTTP surface. */
@RestController
public class FanoutController {

    private final AggregatorService aggregator;

    public FanoutController(AggregatorService aggregator) {
        this.aggregator = aggregator;
    }

    /** POST /messaging/fanout -> 201 {correlationId}. */
    @PostMapping("/messaging/fanout")
    public ResponseEntity<FanoutResponse> fanOut(
            @RequestParam(required = false) String correlationId) {
        String corr = aggregator.fanOut(correlationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new FanoutResponse(corr));
    }

    /** POST /messaging/fanout/reply?correlationId=corr-0001&branch=a -> 204. */
    @PostMapping("/messaging/fanout/reply")
    public ResponseEntity<Void> reply(@RequestParam String correlationId,
                                      @RequestParam String branch) {
        aggregator.branchReply(correlationId, branch, "reply-" + branch);
        return ResponseEntity.noContent().build();
    }

    /** POST /messaging/fanout/reset -> 204. */
    @PostMapping("/messaging/fanout/reset")
    public ResponseEntity<Void> reset() {
        aggregator.reset();
        return ResponseEntity.noContent().build();
    }

    /** GET /messaging/aggregate?correlationId=corr-0001 -> the aggregate's
     *  state, or 404 while the window is still open. */
    @GetMapping("/messaging/aggregate")
    public ResponseEntity<?> aggregate(@RequestParam String correlationId) {
        return aggregator.aggregateFor(correlationId)
            .<ResponseEntity<?>>map(a -> ResponseEntity.ok(new AggregateResponse(
                a.correlationId(), a.parts().size(), a.partial(), aggregator.branchCount(),
                aggregator.aggregateTimeoutMs())))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    public record FanoutResponse(String correlationId) {}

    public record AggregateResponse(String correlationId, int partCount, boolean partial,
                                    int branchCount, long aggregateTimeoutMs) {}
}
