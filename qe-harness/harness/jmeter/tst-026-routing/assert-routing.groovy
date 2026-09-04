// TST-026 message transformation and routing assertion (Wave 17).
//
// The repository's FIRST caller of ContractSchema. Every routed message is
// validated against the schema the SUT itself publishes at
// GET /messaging/contract -- not a copy pasted into this module -- so the
// contract cannot drift from what the service actually serves.
//
// This module's oracle is contract-schema, not invariant-assertion: it is the
// only one of the eight Wave 17 modules for which that is true, and
// evidence.schema.json's oracle enum is what makes the distinction machine-
// checkable.

import com.fasterxml.jackson.databind.ObjectMapper
import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.ContractSchema
import com.techcombank.qe.harness.oracle.InvariantAssertion

import java.math.BigDecimal
import java.nio.file.Path

ObjectMapper mapper = new ObjectMapper()

String schemaJson    = vars.get("contract_schema")
String transformed   = vars.get("transformed_message")
long domesticDepth   = Long.parseLong(vars.get("routed_domestic"))
long intlDepth       = Long.parseLong(vars.get("routed_intl"))
long quarantineDepth = Long.parseLong(vars.get("routed_quarantine"))
long unmatchedSent   = Long.parseLong(props.getProperty("tst026_unmatched_sent"))
long unmappedEnumRejected = Long.parseLong(props.getProperty("tst026_unmapped_rejected"))
long unmappedEnumSent     = Long.parseLong(props.getProperty("tst026_unmapped_sent"))
long splitterDeclared = Long.parseLong(props.getProperty("tst026_split_declared"))
long splitterObserved = Long.parseLong(props.getProperty("tst026_split_observed"))
long enricherPartials = Long.parseLong(props.getProperty("tst026_enricher_partials"))

def schemaNode  = mapper.readTree(schemaJson)
def messageNode = mapper.readTree(transformed)

// I1 is the contract-schema oracle proper: a message whose fields do not all
// map (or are not documented discards) cannot satisfy the published schema,
// whose additionalProperties is false.
RunFragment.Entry i1 = ContractSchema.check(
    "I1", "Every source field maps, or is a documented discard",
    schemaNode, messageNode)

String declaredParty = "Nguyễn Thị Hoà"
BigDecimal declaredAmount = new BigDecimal("1500.00")
BigDecimal observedAmount = new BigDecimal(messageNode.path("amount").asText())

RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "Zero messages reach a default or fallback route",
    { unmatchedSent > 0L && quarantineDepth == unmatchedSent } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "An unmapped enum is rejected, never defaulted",
    { unmappedEnumSent > 0L && unmappedEnumRejected == unmappedEnumSent } as java.util.function.BooleanSupplier)
RunFragment.Entry i4 = InvariantAssertion.check(
    "I4", "Splitter output count equals the declared element count",
    { splitterObserved == splitterDeclared } as java.util.function.BooleanSupplier)
RunFragment.Entry i5 = InvariantAssertion.check(
    "I5", "Round trip preserves amount scale and currency",
    { declaredAmount.compareTo(observedAmount) == 0 &&
      messageNode.path("currency").asText() == "VND" } as java.util.function.BooleanSupplier)
RunFragment.Entry i6 = InvariantAssertion.check(
    "I6", "Vietnamese diacritics survive byte-identically",
    { messageNode.path("party").asText() == declaredParty } as java.util.function.BooleanSupplier)
RunFragment.Entry i7 = InvariantAssertion.check(
    "I7", "An enricher failure yields an error and zero partial messages",
    { enricherPartials == 0L } as java.util.function.BooleanSupplier)

String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

RunFragment fragment = RunFragment.builder()
    .archetype(System.getenv("QE_ARCHETYPE"))
    .module("jmeter")
    .serviceName("reference-sut")
    .tier("T0")
    .oracle("contract-schema")
    .environment(System.getenv().getOrDefault("QE_ENVIRONMENT", "local-compose"))
    .sutDefect(sutDefect)
    .invariant(i1.id(), i1.description(), i1.result())
    .invariant(i2.id(), i2.description(), i2.result())
    .invariant(i3.id(), i3.description(), i3.result())
    .invariant(i4.id(), i4.description(), i4.result())
    .invariant(i5.id(), i5.description(), i5.result())
    .invariant(i6.id(), i6.description(), i6.result())
    .invariant(i7.id(), i7.description(), i7.result())
    .build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "I1 schema-conformant: ${i1.result().wire()}\n" +
    "I2 zero-default-route: ${i2.result().wire()} (quarantine=${quarantineDepth}, unmatchedSent=${unmatchedSent}, domestic=${domesticDepth}, intl=${intlDepth})\n" +
    "I3 unmapped-enum-rejected: ${i3.result().wire()} (${unmappedEnumRejected}/${unmappedEnumSent})\n" +
    "I4 splitter-count: ${i4.result().wire()} (${splitterObserved}/${splitterDeclared})\n" +
    "I5 scale-and-currency: ${i5.result().wire()} (observed=${observedAmount})\n" +
    "I6 diacritics-intact: ${i6.result().wire()}\n" +
    "I7 no-partial-on-enricher-failure: ${i7.result().wire()} (partials=${enricherPartials})\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
