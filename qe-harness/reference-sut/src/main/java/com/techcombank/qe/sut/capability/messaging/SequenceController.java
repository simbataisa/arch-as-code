package com.techcombank.qe.sut.capability.messaging;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** TST-027's HTTP surface. */
@RestController
public class SequenceController {

    private final ResequencerService resequencer;

    public SequenceController(ResequencerService resequencer) {
        this.resequencer = resequencer;
    }

    /** POST /messaging/sequence/publish?key=key-a&sequence=3 -> 202. */
    @PostMapping("/messaging/sequence/publish")
    public ResponseEntity<Void> publish(@RequestParam String key,
                                        @RequestParam long sequence,
                                        @RequestBody(required = false) String payload) {
        resequencer.accept(key, sequence, payload == null ? "" : payload);
        return ResponseEntity.accepted().build();
    }

    /** POST /messaging/sequence/reset -> 204. */
    @PostMapping("/messaging/sequence/reset")
    public ResponseEntity<Void> reset() {
        resequencer.reset();
        return ResponseEntity.noContent().build();
    }

    /** GET /messaging/sequence/state?key=key-a -> the module's whole verdict in
     *  one call: emitted order, overflow flag, escalation, declared scope. */
    @GetMapping("/messaging/sequence/state")
    public StateResponse state(@RequestParam String key) {
        return new StateResponse(
            resequencer.emittedSequences(key),
            resequencer.overflowSignalled(key),
            resequencer.silentlyDropped(key),
            resequencer.escalated(key),
            resequencer.declaredScope(),
            resequencer.bufferBound(),
            resequencer.gapTimeoutMs());
    }

    public record StateResponse(List<Long> emitted, boolean overflowSignalled, long silentlyDropped,
                                boolean escalated, String declaredScope, long bufferBound,
                                long gapTimeoutMs) {}
}
