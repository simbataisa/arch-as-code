// TST-021 ledger invariant assertion (Task 16).
//
// Runs once, as the sole sampler-producing element after the load's own
// HTTP Request in the TearDown Thread Group, once every virtual user from
// the main Thread Group has finished -- see plan.jmx and its README.md for
// why the check has to happen there, not mid-run.
//
// Bound variables this JSR223 Sampler receives from the JMeter engine:
//   vars        - JMeterVariables for this thread; netMinor/entryCount were
//                 set by the "extract-trial-balance" JSR223 PostProcessor
//                 attached to the preceding "GET trial-balance" HTTP sampler.
//   SampleResult - this sampler's own result; its success/failure and
//                 response data become what JMeter's own .jtl reports for
//                 this element, independent of the EvidenceEmitter fragment
//                 below (which is the harness's real source of truth).
//   log         - JMeter's SLF4J logger for this element.
//
// I2/I3 are evaluated here, against Postgres directly, rather than via a
// JMeter-native "JDBC Request" sampler: GET /ledger/trial-balance only
// returns the aggregate net and row count (see LedgerController), so
// confirming "every transfer_ref has exactly two ledger entries" and "no
// entry has amount_minor = 0" needs a query the SUT's HTTP surface does not
// expose. A JSR223 Sampler reaching Postgres directly via java.sql keeps
// that logic in one reviewable Groovy file instead of a JMeter JDBC Request
// element's XML-only configuration.

import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion

import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement

long netMinor = Long.parseLong(vars.get("ledger_netMinor"))

String jdbcUrl = System.getenv("LEDGER_JDBC_URL")
String jdbcUser = System.getenv("LEDGER_JDBC_USER")
String jdbcPassword = System.getenv("LEDGER_JDBC_PASSWORD")

long badTransferPairs
long zeroAmountEntries

Connection conn = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword)
try {
    Statement st = conn.createStatement()
    try {
        ResultSet rs = st.executeQuery(
            "SELECT COUNT(*) FROM (" +
            "  SELECT transfer_ref FROM ledger_entry GROUP BY transfer_ref HAVING COUNT(*) <> 2" +
            ") mismatched_pairs")
        rs.next()
        badTransferPairs = rs.getLong(1)
        rs.close()

        rs = st.executeQuery("SELECT COUNT(*) FROM ledger_entry WHERE amount_minor = 0")
        rs.next()
        zeroAmountEntries = rs.getLong(1)
        rs.close()
    } finally {
        st.close()
    }
} finally {
    conn.close()
}

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1", "Trial balance nets to zero after every transfer batch",
    { netMinor == 0L } as java.util.function.BooleanSupplier)
RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "Every transfer_ref has exactly two ledger entries",
    { badTransferPairs == 0L } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "No ledger entry has amount_minor = 0",
    { zeroAmountEntries == 0L } as java.util.function.BooleanSupplier)

RunFragment fragment = RunFragment.builder()
    .archetype(System.getenv("QE_ARCHETYPE"))
    .module("jmeter")
    .serviceName("reference-sut")
    .tier("T0")
    .oracle("invariant-assertion")
    .environment(System.getenv().getOrDefault("QE_ENVIRONMENT", "local-compose"))
    .invariant(i1.id(), i1.description(), i1.result())
    .invariant(i2.id(), i2.description(), i2.result())
    .invariant(i3.id(), i3.description(), i3.result())
    .build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "I1 trial-balance-zero: ${i1.result().wire()} (netMinor=${netMinor})\n" +
    "I2 transfer-pairs-complete: ${i2.result().wire()} (badTransferPairs=${badTransferPairs})\n" +
    "I3 no-zero-amount-entries: ${i3.result().wire()} (zeroAmountEntries=${zeroAmountEntries})\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
