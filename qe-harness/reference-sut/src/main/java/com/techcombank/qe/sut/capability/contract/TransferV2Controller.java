package com.techcombank.qe.sut.capability.contract;

import com.techcombank.qe.sut.DefectFlags;
import com.techcombank.qe.sut.capability.ledger.TransferService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * TST-030 versioned API and breaking-change fixture (Task 10): v2's
 * surface. {@code POST /v2/transfers} returns
 * {@code {transferRef, status, settledAt}} -- a strictly additive change
 * over v1's {@code {transferRef, status}} (see {@link TransferV1Controller}),
 * published in {@code src/main/resources/contracts/transfer-v2.schema.json}
 * and asserted by
 * {@code SchemaCompatibilityTest#v2ResponseSatisfiesItsPublishedSchema}.
 *
 * <p><b>Defect injection:</b> when
 * {@code DefectFlags.isActive("schema-drift")} is true, this response
 * renames {@code transferRef} to {@code transfer_id} -- a rename, not a
 * removal, because a rename is exactly the failure mode a naive field-count
 * check would miss, while a real schema-validation contract test (the
 * required property {@code transferRef} is simply absent from the drifted
 * response) still catches it -- see
 * {@code SchemaCompatibilityTest#defectFlagRenamesAFieldAndBreaksTheContract}.
 * v1 is deliberately untouched by this flag: the defect is v2-only, which is
 * also what lets the two versions be validated independently of one
 * another.
 */
@RestController
public class TransferV2Controller {

    private final TransferService transferService;

    public TransferV2Controller(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/v2/transfers")
    public ResponseEntity<Map<String, Object>> transfer(@RequestBody TransferRequest request) {
        UUID ref = transferService.transfer(request.from(), request.to(), request.amountMinor());
        Instant settledAt = Instant.now();

        String transferRefFieldName = DefectFlags.isActive("schema-drift") ? "transfer_id" : "transferRef";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put(transferRefFieldName, ref.toString());
        body.put("status", "SETTLED");
        body.put("settledAt", settledAt.toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    public record TransferRequest(String from, String to, long amountMinor) {}
}
