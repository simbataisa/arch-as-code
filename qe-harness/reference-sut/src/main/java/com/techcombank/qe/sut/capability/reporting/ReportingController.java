package com.techcombank.qe.sut.capability.reporting;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** TST-037 read-model capability's HTTP surface. */
@RestController
public class ReportingController {

    private final ReportingService reporting;
    private final long convergenceBoundMs;

    public ReportingController(ReportingService reporting,
                               @Value("${app.readmodel.convergence-bound-ms}") long convergenceBoundMs) {
        this.reporting = reporting;
        this.convergenceBoundMs = convergenceBoundMs;
    }

    /** GET /reporting/lag -> {p95Ms, p99Ms, accountsCovered, convergenceBoundMs}.
     *  The bound is returned alongside the measurement so the harness asserts
     *  against the SUT's declared configuration rather than a literal of its own. */
    @GetMapping("/reporting/lag")
    public LagResponse lag() {
        ReportingService.Lag lag = reporting.lag();
        return new LagResponse(lag.p95Ms(), lag.p99Ms(), lag.accountsCovered(), convergenceBoundMs);
    }

    /** POST /reporting/refresh -> 204. */
    @PostMapping("/reporting/refresh")
    public ResponseEntity<Void> refresh() {
        reporting.refresh();
        return ResponseEntity.noContent().build();
    }

    /** GET /reporting/outbox -> {miscountedRows}. I4's verdict. */
    @GetMapping("/reporting/outbox")
    public OutboxResponse outbox() {
        return new OutboxResponse(reporting.outboxMiscountedRows());
    }

    public record LagResponse(long p95Ms, long p99Ms, long accountsCovered, long convergenceBoundMs) {}

    public record OutboxResponse(long miscountedRows) {}
}
