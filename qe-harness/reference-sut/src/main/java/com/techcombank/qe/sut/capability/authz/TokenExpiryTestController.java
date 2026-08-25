package com.techcombank.qe.sut.capability.authz;

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
 */
@RestController
@RequestMapping("/_test/token")
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
     *  measurement there, per {@code assert-authz.groovy}. */
    @PostMapping("/expired")
    public ResponseEntity<?> mintExpired(@RequestBody ExpiredTokenRequest request) {
        if (!JwtService.isKnownRole(request.role())) {
            return ResponseEntity.badRequest().body(Map.of("error", "unknown role: " + request.role()));
        }
        String role = request.role().toLowerCase(Locale.ROOT);
        String subject = "skew-probe-" + role;
        String token = jwtService.mintExpiredAccessToken(subject, role, request.secondsPastExpiry());
        return ResponseEntity.ok(new ExpiredTokenResponse(token));
    }
}
