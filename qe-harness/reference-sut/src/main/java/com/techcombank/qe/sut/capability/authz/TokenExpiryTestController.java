package com.techcombank.qe.sut.capability.authz;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;

/**
 * Meta/test-control endpoint for TST-040's clock-skew measurement (Task 19
 * follow-up addition to {@code reference-sut}) -- same {@code _test}-prefixed
 * convention as {@code DefectController} and {@link
 * com.techcombank.qe.sut.capability.ratelimit.RateLimitResetController}: this
 * exists only because this SUT's purpose is to be deliberately
 * test-controllable, not because a real production service would expose it.
 *
 * <p>{@link JwtService#mintExpiredAccessToken} is a plain Java method, not
 * itself reachable over HTTP -- deliberately, per its own Javadoc, since a
 * real client must never be able to ask this SUT to mint a token with an
 * arbitrary expiry. Task 19's JMeter module (Tst040ModuleTest /
 * {@code assert-authz.groovy}) runs as a separate process against an
 * already-running {@code docker compose up} container, the same "harness
 * modules can only reach in-process state over HTTP" constraint {@code
 * RateLimitResetController}'s own Javadoc documents for TST-031 -- so it has
 * no way to call {@code mintExpiredAccessToken} directly the way the
 * in-process {@code TokenLifecycleTest} does. This controller is that HTTP
 * door, scoped narrowly to exactly the one capability the sweep needs (mint
 * an otherwise-valid, already-expired token) rather than exposing the
 * signing key or a general-purpose token-minting override.
 *
 * <p>{@code @Profile("!prod")} -- see {@link com.techcombank.qe.sut.DefectController}'s
 * Javadoc for why: unauthenticated by design, a no-op restriction in every
 * environment this harness runs in today, and a real guard the moment a
 * copy of this reference implementation deploys with {@code prod} active.
 * This one is the sharpest instance of the risk this annotation exists to
 * cut off: an unauthenticated endpoint that mints a signed, valid token for
 * ANY role, including {@code admin}.
 */
@RestController
@RequestMapping("/_test/token")
@Profile("!prod")
public class TokenExpiryTestController {

    private final JwtService jwtService;

    public TokenExpiryTestController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public record ExpiredTokenRequest(String role, long secondsPastExpiry) {}

    public record ExpiredTokenResponse(String accessToken) {}

    /** POST /_test/token/expired {"role": "reader", "secondsPastExpiry": 3} -> 200
     *  with an access token whose {@code exp} is already 3 seconds in the past, or
     *  400 for an unrecognised role. {@code secondsPastExpiry} may be 0 (a token
     *  that expired the instant it was minted) -- the clock-skew sweep starts its
     *  measurement there, per {@code assert-authz.groovy}.
     *
     *  <p>{@code secondsPastExpiry} must be {@code >= 0} -- 400 otherwise. A negative
     *  value would mint an {@code exp} in the FUTURE, i.e. a fully valid, non-expired
     *  token for any role, which contradicts this endpoint's entire reason to exist
     *  ("mint an already-expired token"). That a real client can already obtain a
     *  fully valid token for any role via the legitimate, unauthenticated
     *  {@code POST /auth/token} (Task 9's {@code SecurityConfig} permits everything
     *  except {@code /protected/**} by this SUT's own deliberate design) does not
     *  excuse this endpoint from enforcing its own documented contract -- this
     *  validation is about {@code TokenExpiryTestController} doing only what its own
     *  Javadoc and README claim it does, not about this reference SUT's separate,
     *  already-accepted "issuance itself is unauthenticated" property. */
    @PostMapping("/expired")
    public ResponseEntity<?> mintExpired(@RequestBody ExpiredTokenRequest request) {
        if (!JwtService.isKnownRole(request.role())) {
            return ResponseEntity.badRequest().body(Map.of("error", "unknown role: " + request.role()));
        }
        if (request.secondsPastExpiry() < 0) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "secondsPastExpiry must be >= 0 (a negative offset would mint a "
                    + "non-expired token): " + request.secondsPastExpiry()));
        }
        String role = request.role().toLowerCase(Locale.ROOT);
        String subject = "skew-probe-" + role;
        String token = jwtService.mintExpiredAccessToken(subject, role, request.secondsPastExpiry());
        return ResponseEntity.ok(new ExpiredTokenResponse(token));
    }
}
