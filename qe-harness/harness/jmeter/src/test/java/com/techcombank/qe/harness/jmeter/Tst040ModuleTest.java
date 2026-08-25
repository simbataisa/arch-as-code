package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-040 authorization-matrix and token-lifecycle module (Task 19). Drives
 * real HTTP traffic against the reference SUT's authz capability (Task 9,
 * {@code /auth/*}/{@code /protected/*}) -- requires {@code make up
 * PROFILES=core} to already be running (see qe-harness/README.md): this
 * capability needs only {@code core} (postgres + reference-sut), the same
 * as TST-021/TST-031, not the {@code resilience} profile TST-035 needs.
 *
 * <p>These three tests are given verbatim by the task brief.
 * {@link #classifiesBareForbiddenAsErrorNotDeny()} is this module's single
 * most important correctness property: the {@code authz-missing-marker}
 * defect (Task 9) strips {@code X-Authz-Decision} from every response, so a
 * denial-shaped {@code 401}/{@code 403} that carries no decision marker must
 * be classified {@code error}, never quietly accepted as a correct
 * {@code deny} -- see {@code AuthzDecisionFilter}'s own Javadoc in
 * {@code reference-sut} and {@code assert-authz.groovy}'s {@code classify()}
 * function, which implements exactly this rule.
 */
class Tst040ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void classifiesBareForbiddenAsErrorNotDeny() throws Exception {
        // The authz-missing-marker defect returns 403 with no decision marker.
        // TST-040 requires that be classified 'error', so the module must FAIL,
        // not quietly accept it as a correct denial.
        ModuleResult r = runner.run("TST-040", Map.of("SUT_DEFECT", "authz-missing-marker"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
    }

    @Test
    void passesEveryMatrixCellAgainstTheCleanSut() throws Exception {
        ModuleResult r = runner.run("TST-040", Map.of());
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void measuresRatherThanAssertsClockSkewTolerance() throws Exception {
        ModuleResult r = runner.run("TST-040", Map.of());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.description().contains("accepted exp offset")),
            "clock-skew invariant must report a measured offset");
    }
}
