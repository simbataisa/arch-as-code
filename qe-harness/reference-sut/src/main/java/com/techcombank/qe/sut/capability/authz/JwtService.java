package com.techcombank.qe.sut.capability.authz;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TST-040 JWT issuance, validation, and revocation for the reference SUT.
 *
 * <p>Backed by a single process-wide HMAC key generated at construction --
 * there is no cross-process token portability requirement here (Task 19's
 * JMeter module always mints its tokens by calling this SUT's own
 * {@code POST /auth/token}, never by holding a shared secret itself), so a
 * random per-process key is simpler and safer than a checked-in secret
 * string.
 *
 * <p>{@link #clockSkewSeconds} is wired straight into JJWT's own
 * {@code JwtParserBuilder.clockSkewSeconds(...)}, so the *declared* tolerance
 * ({@code app.authz.clock-skew-seconds}) is also the tolerance the running
 * validator actually enforces -- {@code TokenLifecycleTest}'s clock-skew
 * sweep measures the real accepted offset against this same property, never
 * a duplicated literal.
 */
@Component
public class JwtService {

    static final String CLAIM_ROLE = "role";
    static final String CLAIM_TYPE = "type";
    static final String TOKEN_TYPE_ACCESS = "access";
    static final String TOKEN_TYPE_REFRESH = "refresh";

    private static final Set<String> KNOWN_ROLES = Set.of("reader", "writer", "admin");

    private final SecretKey key = Jwts.SIG.HS256.key().build();
    private final Set<String> revokedJti = ConcurrentHashMap.newKeySet();

    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final long clockSkewSeconds;

    public JwtService(
            @Value("${app.authz.access-token-ttl-seconds:300}") long accessTokenTtlSeconds,
            @Value("${app.authz.refresh-token-ttl-seconds:3600}") long refreshTokenTtlSeconds,
            @Value("${app.authz.clock-skew-seconds:5}") long clockSkewSeconds) {
        this.accessTokenTtl = Duration.ofSeconds(accessTokenTtlSeconds);
        this.refreshTokenTtl = Duration.ofSeconds(refreshTokenTtlSeconds);
        this.clockSkewSeconds = clockSkewSeconds;
    }

    public static boolean isKnownRole(String role) {
        return role != null && KNOWN_ROLES.contains(role.toLowerCase(java.util.Locale.ROOT));
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtl.getSeconds();
    }

    public String mintAccessToken(String subject, String role) {
        return mint(subject, role, TOKEN_TYPE_ACCESS, Instant.now().plus(accessTokenTtl));
    }

    public String mintRefreshToken(String subject, String role) {
        return mint(subject, role, TOKEN_TYPE_REFRESH, Instant.now().plus(refreshTokenTtl));
    }

    /** Test-only hook for {@code TokenLifecycleTest}'s clock-skew sweep: mints an
     *  access token whose {@code exp} is already {@code secondsPastExpiry} seconds
     *  in the past, so the sweep can measure the real accepted offset rather than
     *  asserting a configured number. Not reachable over HTTP -- there is no
     *  {@code exp} override on {@code POST /auth/token}, deliberately, since a real
     *  client must never be able to ask this SUT to mint a token with an arbitrary
     *  expiry. */
    public String mintExpiredAccessToken(String subject, String role, long secondsPastExpiry) {
        return mint(subject, role, TOKEN_TYPE_ACCESS, Instant.now().minusSeconds(secondsPastExpiry));
    }

    private String mint(String subject, String role, String tokenType, Instant expiry) {
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
            .subject(subject)
            .id(jti)
            .claim(CLAIM_ROLE, role)
            .claim(CLAIM_TYPE, tokenType)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact();
    }

    /** Parses and validates {@code token}: signature, expiry (within the configured
     *  clock-skew tolerance), and revocation status. Throws {@link io.jsonwebtoken.JwtException}
     *  (or {@link IllegalArgumentException} for a blank/null token) on any failure --
     *  callers must not authenticate the caller on any exception path. */
    public Jws<Claims> parseAndValidate(String token) {
        Jws<Claims> jws = Jwts.parser()
            .clockSkewSeconds(clockSkewSeconds)
            .verifyWith(key)
            .build()
            .parseSignedClaims(token);
        String jti = jws.getPayload().getId();
        if (isRevoked(jti)) {
            throw new io.jsonwebtoken.JwtException("token revoked: " + jti);
        }
        return jws;
    }

    /** Marks {@code token}'s {@code jti} revoked -- including a token that is
     *  already expired, per TST-040's negative-path requirement that revoking a
     *  non-existent/expired token must never be a silent no-op that could mask a
     *  real revocation failure. Throws {@link io.jsonwebtoken.JwtException} only if
     *  the token cannot be parsed at all (bad signature or malformed). */
    public void revoke(String token) {
        String jti;
        try {
            jti = Jwts.parser().clockSkewSeconds(clockSkewSeconds).verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getId();
        } catch (ExpiredJwtException e) {
            jti = e.getClaims().getId();
        }
        revokedJti.add(jti);
    }

    public boolean isRevoked(String jti) {
        return jti != null && revokedJti.contains(jti);
    }
}
