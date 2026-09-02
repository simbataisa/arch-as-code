package com.techcombank.qe.sut.capability.authz;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * TST-040 bearer-token authentication. Reads {@code Authorization: Bearer <token>},
 * validates it via {@link JwtService} (signature, expiry within the configured
 * clock-skew tolerance, and revocation), and -- only for a token of type
 * {@code "access"} that passes all three checks -- populates the
 * {@link SecurityContextHolder} with a {@code ROLE_<ROLE>} authority.
 *
 * <p>Deliberately never writes to {@code response} on any failure path: a
 * missing, malformed, expired, revoked, or wrong-type (refresh) token simply
 * leaves the request unauthenticated. {@link SecurityConfig}'s
 * {@code authorizeHttpRequests} + {@code exceptionHandling} are what turn an
 * unauthenticated or under-privileged request into the 401/403
 * {@link AuthzDecisionFilter} later reads -- this filter is not where the
 * SUT's allow/deny decision itself is made.
 *
 * <p>Not a {@code @Component}: it is constructed directly by
 * {@link SecurityConfig} and wired in via
 * {@code HttpSecurity.addFilterBefore(...)}. If it were also component-scanned,
 * Spring Boot's generic servlet-filter auto-registration would register it a
 * second time, running it twice per request.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                Claims claims = jwtService.parseAndValidate(token).getPayload();
                if (JwtService.TOKEN_TYPE_ACCESS.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
                    String role = claims.get(JwtService.CLAIM_ROLE, String.class);
                    List<GrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)));
                    var authentication =
                        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                // A "refresh" token presented here is simply left unauthenticated: refresh
                // tokens are only valid at POST /auth/refresh, never as a bearer credential.
            } catch (JwtException | IllegalArgumentException e) {
                // Invalid signature, expired beyond skew, revoked, or malformed: leave the
                // request unauthenticated. SecurityConfig turns that into 401/403, which
                // AuthzDecisionFilter marks "deny" once the outcome is resolved.
            }
        }
        chain.doFilter(request, response);
    }
}
