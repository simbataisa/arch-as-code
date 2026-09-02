package com.techcombank.qe.sut.capability.authz;

import com.techcombank.qe.sut.DefectFlags;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TST-040's decision marker -- the single most load-bearing detail in this
 * capability. The archetype (see {@code authn-authz-token-lifecycle.md} §3)
 * classifies a bare 401/403 that carries no explicit decision marker as
 * {@code error}, not {@code deny}: a genuine server-side failure must never
 * be mistaken for a correct, deliberate authorization denial. This filter is
 * why every authorization outcome from this SUT instead carries an explicit
 * {@code X-Authz-Decision: allow | deny} header.
 *
 * <p>Registered (see {@link SecurityConfig}) to run immediately before
 * Spring Security's {@code ExceptionTranslationFilter}, so that
 * {@link #doFilterInternal}'s {@code chain.doFilter(...)} call delegates all
 * the way down through the rest of Spring Security's chain -- authentication,
 * the authorization decision, and (on denial) the entry point / access-denied
 * handler that actually sets the 401/403 status -- and back up, before this
 * filter ever inspects {@code response.getStatus()}. That is what "runs after
 * Spring Security's filter chain has resolved the outcome" means here:
 * structurally this filter sits early in the chain, but the code that reads
 * the status only executes once the real result is already decided.
 * ({@link SecurityConfig}'s entry point / access-denied handler deliberately
 * call {@code response.setStatus(...)}, never {@code sendError(...)} --
 * {@code sendError} commits the response immediately, which would make the
 * header this filter adds afterwards silently no-op.)
 *
 * <p>Sets {@code allow} on a 2xx outcome, {@code deny} on a 401 or 403 -- the
 * only two codes anything downstream of this filter in this SUT ever produces
 * via Spring Security's own entry point / access-denied handler -- and
 * deliberately sets nothing else: a genuine 500 (or a 404 from an unmapped
 * path) stays visibly distinct from a deliberate denial.
 *
 * <p>The {@code authz-missing-marker} defect disables this filter outright:
 * every response then carries no marker at all, reproducing exactly the "bare
 * 403 is an error, not a deny" failure mode the archetype exists to catch.
 *
 * <p>Not a {@code @Component}: see {@link JwtAuthenticationFilter}'s Javadoc
 * for why these filters are constructed directly by {@link SecurityConfig}
 * instead.
 */
public class AuthzDecisionFilter extends OncePerRequestFilter {

    public static final String DECISION_HEADER = "X-Authz-Decision";
    private static final String DEFECT_FLAG = "authz-missing-marker";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(request, response);

        if (DefectFlags.isActive(DEFECT_FLAG)) {
            return;
        }

        int status = response.getStatus();
        if (status >= 200 && status < 300) {
            response.setHeader(DECISION_HEADER, "allow");
        } else if (status == 401 || status == 403) {
            response.setHeader(DECISION_HEADER, "deny");
        }
        // Anything else -- 5xx, 404, ... -- gets no header, deliberately: the
        // decision is unknown, and an unknown outcome must never read as a deny.
    }
}
