package com.techcombank.qe.sut.capability.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * TST-021 double-entry ledger capability's HTTP surface. Task 16's JMeter
 * module drives these two endpoints.
 *
 * <p>TST-020 idempotency and replay (Wave 17) is layered onto
 * {@code POST /transfers} via an optional {@code Idempotency-Key} header.
 * Spring cannot bind two {@code @RequestBody} parameters on one handler
 * method, so the single {@code @RequestBody} accepted here is the raw JSON
 * string; it is parsed once (via the shared {@link ObjectMapper} bean) into a
 * {@link TransferRequest} for both branches. The unkeyed branch below is
 * otherwise byte-identical to the pre-Wave-17 behaviour, so every caller that
 * sends no header -- TST-021's own JMeter module and TST-034's
 * blended-journey module -- sees exactly the same 201/{@code transferRef}
 * response as before this task.
 */
@RestController
public class LedgerController {

    private final TransferService transferService;
    private final TrialBalanceService trialBalanceService;
    private final IdempotencyService idempotency;
    private final ObjectMapper objectMapper;

    public LedgerController(TransferService transferService, TrialBalanceService trialBalanceService,
                             IdempotencyService idempotency, ObjectMapper objectMapper) {
        this.transferService = transferService;
        this.trialBalanceService = trialBalanceService;
        this.idempotency = idempotency;
        this.objectMapper = objectMapper;
    }

    /** POST /transfers {from, to, amountMinor} -> 201 {transferRef}.
     *
     *  <p>An optional Idempotency-Key header makes the call replay-safe
     *  (TST-020). Without it the behaviour is exactly as before, so TST-021's
     *  module and every other existing caller are unaffected. A replay returns
     *  200 rather than 201: the resource was not created by this request. */
    @PostMapping("/transfers")
    public ResponseEntity<?> transfer(@RequestBody String rawBody,
                                      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        TransferRequest request = parse(rawBody);
        if (idempotencyKey == null) {
            UUID ref = transferService.transfer(request.from(), request.to(), request.amountMinor());
            return ResponseEntity.status(HttpStatus.CREATED).body(new TransferResponse(ref));
        }
        try {
            IdempotencyService.Outcome outcome = idempotency.execute(idempotencyKey, rawBody,
                () -> transferService.transfer(request.from(), request.to(), request.amountMinor()));
            return ResponseEntity
                .status(outcome.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header("Idempotent-Replay", String.valueOf(outcome.replayed()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(outcome.body());
        } catch (IdempotencyService.PayloadConflict e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /** GET /transfers/idempotency/{key} -> {present, replayed, keyTtlSeconds,
     *  clientMaxRetryWindowSeconds}, so the harness can assert I6 (the key TTL
     *  covers the declared client retry window) against declared
     *  configuration rather than by waiting out a TTL. {@code present}/
     *  {@code replayed} report whether a non-expired record exists for
     *  {@code key} -- if it does, any subsequent request carrying that key
     *  would be served as a replay. */
    @GetMapping("/transfers/idempotency/{key}")
    public IdempotencyStatusResponse idempotencyStatus(@PathVariable String key) {
        boolean present = idempotency.isPresent(key);
        return new IdempotencyStatusResponse(present, present,
            idempotency.keyTtlSeconds(), idempotency.clientMaxRetryWindowSeconds());
    }

    /** GET /ledger/trial-balance -> {netMinor, entryCount} */
    @GetMapping("/ledger/trial-balance")
    public TrialBalanceResponse trialBalance() {
        return new TrialBalanceResponse(trialBalanceService.net(), trialBalanceService.entryCount());
    }

    /** Parses the raw request body once. A malformed body yields the same
     *  400 Bad Request a Spring-bound {@code @RequestBody TransferRequest}
     *  parameter produced before this task. */
    private TransferRequest parse(String rawBody) {
        try {
            return objectMapper.readValue(rawBody, TransferRequest.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "malformed transfer request body", e);
        }
    }

    public record TransferRequest(String from, String to, long amountMinor) {}

    public record TransferResponse(UUID transferRef) {}

    public record TrialBalanceResponse(long netMinor, long entryCount) {}

    public record IdempotencyStatusResponse(boolean present, boolean replayed,
                                             long keyTtlSeconds, long clientMaxRetryWindowSeconds) {}
}
