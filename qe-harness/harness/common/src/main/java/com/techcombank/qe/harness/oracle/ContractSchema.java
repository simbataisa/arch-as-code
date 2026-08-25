package com.techcombank.qe.harness.oracle;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.techcombank.qe.harness.evidence.RunFragment;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contract-schema oracle: validates an instance (e.g. a captured API response body) against
 * a JSON Schema, or checks that an old schema and a new schema remain compatible.
 *
 * <p>Used by TST-030 (Contract &amp; Schema Compatibility). Same
 * {@code com.networknt:json-schema-validator} library reference-sut's own contract tests
 * already use for the {@code /v1} vs {@code /v2} transfer API schemas, kept consistent
 * across the reactor rather than introducing a second JSON Schema implementation.
 */
public final class ContractSchema {

    private static final JsonSchemaFactory FACTORY =
        JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    private ContractSchema() {
    }

    /** A schema violation is reported with the validator's own message. */
    public record Violation(String message) {
    }

    /**
     * Validates {@code instance} against {@code schema}.
     *
     * @param id          stable identifier for this check within its archetype run
     * @param description human-readable statement of what is being validated
     */
    public static RunFragment.Entry check(String id, String description, JsonNode schema, JsonNode instance) {
        Set<Violation> violations = validate(schema, instance);
        return new RunFragment.Entry(id, description,
            violations.isEmpty() ? RunFragment.Result.PASSED : RunFragment.Result.FAILED);
    }

    /**
     * Validates {@code instance} against {@code schema} and returns every violation found
     * (empty set means the instance conforms).
     */
    public static Set<Violation> validate(JsonNode schema, JsonNode instance) {
        JsonSchema jsonSchema = FACTORY.getSchema(schema);
        Set<ValidationMessage> messages = jsonSchema.validate(instance);
        return messages.stream()
            .map(m -> new Violation(m.getMessage()))
            .collect(Collectors.toUnmodifiableSet());
    }
}
