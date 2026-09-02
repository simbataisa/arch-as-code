package com.techcombank.qe.sut.capability.authz;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-040 token-lifecycle tests: revocation and the clock-skew boundary.
 *
 * <p>{@link #expiredTokenIsNotAcceptedBeyondDeclaredSkew} is a *measurement*,
 * not an assertion against a hardcoded number, per the archetype's own I3
 * invariant (see {@code authn-authz-token-lifecycle.md} §3): it mints
 * progressively staler tokens via {@link JwtService#mintExpiredAccessToken}
 * until the SUT first rejects one, and asserts that measured ceiling against
 * {@code app.authz.clock-skew-seconds} -- the same property {@link JwtService}
 * itself configures its JJWT parser's {@code clockSkewSeconds} from, so this
 * checks the running validator's real behaviour, never a duplicated literal.
 */
class TokenLifecycleTest extends AbstractAuthzTest {

    /** Sweep bound: comfortably above the configured tolerance so the sweep
     *  always finds a real rejection rather than exhausting the range
     *  inconclusively (see the archetype's own sweep-termination requirement). */
    private static final long SWEEP_MAX_SECONDS = 30;

    @Value("${app.authz.clock-skew-seconds}")
    private long DECLARED_CLOCK_SKEW_SECONDS;

    @Test
    void revokedTokenIsRejectedWithAnExplicitDeny() throws Exception {
        String token = issue("reader");
        revoke(token);
        MockHttpServletResponse r = callWithToken(token, "/protected/read");
        assertEquals(401, r.getStatus());
        assertEquals("deny", r.getHeader("X-Authz-Decision"));
    }

    @Test
    void expiredTokenIsNotAcceptedBeyondDeclaredSkew() throws Exception {
        // Measures the actual accepted exp offset rather than asserting a configured value.
        long maxAccepted = probeMaxAcceptedExpOffsetSeconds();
        assertTrue(maxAccepted <= DECLARED_CLOCK_SKEW_SECONDS,
            "accepted exp offset " + maxAccepted + "s exceeds declared tolerance");
    }

    /** Mints a token already {@code offset} seconds past its own {@code exp} for
     *  offsets {@code 0..SWEEP_MAX_SECONDS}, stopping at the first rejection, and
     *  returns the largest offset still accepted (HTTP 200). Fails loudly if the
     *  sweep runs out without ever finding a rejection -- an inconclusive sweep is
     *  a failed measurement, not a passing one. */
    private long probeMaxAcceptedExpOffsetSeconds() throws Exception {
        long maxAccepted = -1;
        for (long offset = 0; offset <= SWEEP_MAX_SECONDS; offset++) {
            String token = jwtService.mintExpiredAccessToken("skew-probe", "reader", offset);
            int status = callWithToken(token, "/protected/read").getStatus();
            if (status == 200) {
                maxAccepted = offset;
            } else {
                return maxAccepted;
            }
        }
        throw new AssertionError(
            "clock-skew sweep exhausted " + SWEEP_MAX_SECONDS + "s without a rejection -- "
            + "widen SWEEP_MAX_SECONDS, the validator's real leeway was never found");
    }
}
