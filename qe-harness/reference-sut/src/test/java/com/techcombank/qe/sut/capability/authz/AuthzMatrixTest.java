package com.techcombank.qe.sut.capability.authz;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-040 authorization-matrix sweep. See {@link AuthzDecisionFilter}'s Javadoc
 * for why a bare 401/403 with no marker must never be scored as a deny.
 */
class AuthzMatrixTest extends AbstractAuthzTest {

    @Test
    void everyMatrixCellCarriesADecisionMarker() throws Exception {
        for (String role : List.of("reader", "writer", "admin", "anonymous")) {
            for (String ep : List.of("/protected/read", "/protected/write", "/protected/admin")) {
                MockHttpServletResponse r = call(role, ep);
                assertTrue(r.containsHeader("X-Authz-Decision"),
                    "no decision marker for " + role + " -> " + ep
                    + " (a bare " + r.getStatus() + " is an error, not a deny)");
                assertTrue(Set.of("allow", "deny").contains(r.getHeader("X-Authz-Decision")));
            }
        }
    }

    @Test
    void defectFlagStripsTheDecisionMarker() throws Exception {
        withDefect("authz-missing-marker", () -> {
            MockHttpServletResponse r = call("anonymous", "/protected/admin");
            assertFalse(r.containsHeader("X-Authz-Decision"));
        });
    }

    /** Not one of TST-040's four given tests, but verifies the exact property the
     *  whole decision-marker design exists to protect: a genuine server error must
     *  never carry the marker. Exercised directly against {@link AuthzDecisionFilter}
     *  with a stub {@code FilterChain} that raises a plain 500 -- no controller or
     *  Spring context needed to force one, so this stays a fast, isolated unit test
     *  rather than duplicating the full MockMvc slice for one status code. */
    @Test
    void genuineServerErrorDoesNotCarryADecisionMarker() throws Exception {
        AuthzDecisionFilter filter = new AuthzDecisionFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/protected/read");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(500));

        assertFalse(response.containsHeader("X-Authz-Decision"),
            "a genuine 500 must never be marked allow or deny -- it is neither, it is unknown");
    }
}
