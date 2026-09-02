package com.techcombank.qe.sut.capability.ledger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * TST-021 double-entry ledger capability's HTTP surface. Task 16's JMeter
 * module drives these two endpoints.
 */
@RestController
public class LedgerController {

    private final TransferService transferService;
    private final TrialBalanceService trialBalanceService;

    public LedgerController(TransferService transferService, TrialBalanceService trialBalanceService) {
        this.transferService = transferService;
        this.trialBalanceService = trialBalanceService;
    }

    /** POST /transfers {from, to, amountMinor} -> 201 {transferRef} */
    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(@RequestBody TransferRequest request) {
        UUID ref = transferService.transfer(request.from(), request.to(), request.amountMinor());
        return ResponseEntity.status(HttpStatus.CREATED).body(new TransferResponse(ref));
    }

    /** GET /ledger/trial-balance -> {netMinor, entryCount} */
    @GetMapping("/ledger/trial-balance")
    public TrialBalanceResponse trialBalance() {
        return new TrialBalanceResponse(trialBalanceService.net(), trialBalanceService.entryCount());
    }

    public record TransferRequest(String from, String to, long amountMinor) {}

    public record TransferResponse(UUID transferRef) {}

    public record TrialBalanceResponse(long netMinor, long entryCount) {}
}
