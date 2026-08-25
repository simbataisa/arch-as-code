package com.techcombank.qe.sut.capability.contract;

import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-030 versioned API and breaking-change fixture (Task 10): proves
 * {@code /v1/transfers} and {@code /v2/transfers} each satisfy their own
 * published JSON Schema (versioned independently), that v1 stays backward
 * compatible as v2 evolves, and that the {@code schema-drift} defect (a
 * field rename, not a removal -- see {@link TransferV2Controller}) is
 * actually caught by schema validation.
 */
class SchemaCompatibilityTest extends AbstractContractIntegrationTest {

    @Test
    void v2ResponseSatisfiesItsPublishedSchema() throws Exception {
        String body = post("/v2/transfers", validRequest());
        Set<ValidationMessage> errors = schema("transfer-v2.schema.json").validate(json(body));
        assertTrue(errors.isEmpty(), "v2 response violates its own schema: " + errors);
    }

    @Test
    void v1ResponseRemainsBackwardCompatible() throws Exception {
        // BACKWARD compatibility: every field v1 declared is still present.
        String body = post("/v1/transfers", validRequest());
        Set<ValidationMessage> errors = schema("transfer-v1.schema.json").validate(json(body));
        assertTrue(errors.isEmpty(), "v1 contract broken: " + errors);
    }

    @Test
    void defectFlagRenamesAFieldAndBreaksTheContract() throws Exception {
        withDefect("schema-drift", () -> {
            String body = post("/v2/transfers", validRequest());
            Set<ValidationMessage> errors = schema("transfer-v2.schema.json").validate(json(body));
            assertFalse(errors.isEmpty(), "schema-drift defect must break the published contract");
        });
    }
}
