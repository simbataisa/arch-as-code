package com.techcombank.qe.sut.capability.contract;

import com.techcombank.qe.sut.capability.ledger.TransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * TST-030 versioned API and breaking-change fixture (Task 10): v1's frozen
 * surface. {@code POST /v1/transfers} always returns exactly
 * {@code {transferRef, status}} -- the shape published in
 * {@code src/main/resources/contracts/transfer-v1.schema.json} and asserted
 * by {@code SchemaCompatibilityTest#v1ResponseRemainsBackwardCompatible}.
 *
 * <p>v1 never reads {@link com.techcombank.qe.sut.DefectFlags} -- only
 * {@link TransferV2Controller} does. The {@code schema-drift} defect this
 * capability demonstrates is a v2-only field rename, precisely so that
 * {@code SchemaCompatibilityTest} can prove v1 stays backward compatible
 * (every field it ever declared is still present, unaffected by the defect)
 * even while v2's own published contract breaks.
 *
 * <p>Both versions delegate the actual transfer to
 * {@link TransferService} (Task 7's double-entry ledger, TST-021) -- this is
 * a real transfer against a real Postgres, not a fixture, which is why this
 * capability's tests need Testcontainers (see
 * {@code AbstractContractIntegrationTest}).
 */
@RestController
public class TransferV1Controller {

    private final TransferService transferService;

    public TransferV1Controller(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/v1/transfers")
    public ResponseEntity<Map<String, Object>> transfer(@RequestBody TransferRequest request) {
        UUID ref = transferService.transfer(request.from(), request.to(), request.amountMinor());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transferRef", ref.toString());
        body.put("status", "SETTLED");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    public record TransferRequest(String from, String to, long amountMinor) {}
}
