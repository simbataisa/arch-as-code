package com.techcombank.qe.sut.capability.authz;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jws;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * TST-040 token-lifecycle HTTP surface: issue, refresh (rotate), revoke.
 * Authorization enforcement itself lives in {@link SecurityConfig} /
 * {@link JwtAuthenticationFilter} against {@link ProtectedController}'s
 * endpoints -- this controller only manages the tokens those endpoints
 * consume.
 */
@RestController
@RequestMapping("/auth")
public class TokenController {

    private final JwtService jwtService;

    public TokenController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public record TokenRequest(String role) {}

    public record RefreshRequest(String refreshToken) {}

    public record RevokeRequest(String token) {}

    public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresIn) {}

    /** POST /auth/token {"role": "reader"} -> 200 with a fresh access + refresh
     *  token pair, or 400 for an unrecognised role. */
    @PostMapping("/token")
    public ResponseEntity<?> issue(@RequestBody TokenRequest request) {
        if (!JwtService.isKnownRole(request.role())) {
            return ResponseEntity.badRequest().body(Map.of("error", "unknown role: " + request.role()));
        }
        String role = request.role().toLowerCase(java.util.Locale.ROOT);
        String subject = "user-" + role;
        return ResponseEntity.ok(new TokenResponse(
            jwtService.mintAccessToken(subject, role),
            jwtService.mintRefreshToken(subject, role),
            "Bearer",
            jwtService.accessTokenTtlSeconds()));
    }

    /** POST /auth/refresh {"refreshToken": "..."} -> 200 with a rotated pair, or
     *  401 if the refresh token is invalid, expired, of the wrong type, or
     *  already revoked/rotated. Rotation: presenting the same refresh token a
     *  second time is rejected, since the first use revokes it. */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        try {
            Jws<Claims> jws = jwtService.parseAndValidate(request.refreshToken());
            Claims claims = jws.getPayload();
            if (!JwtService.TOKEN_TYPE_REFRESH.equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            String subject = claims.getSubject();
            String role = claims.get(JwtService.CLAIM_ROLE, String.class);
            jwtService.revoke(request.refreshToken());
            return ResponseEntity.ok(new TokenResponse(
                jwtService.mintAccessToken(subject, role),
                jwtService.mintRefreshToken(subject, role),
                "Bearer",
                jwtService.accessTokenTtlSeconds()));
        } catch (JwtException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /** POST /auth/revoke {"token": "..."} -> 204, always -- even for an
     *  already-expired token, per TST-040's negative-path requirement that
     *  revoking a non-existent/expired token is never a silent no-op -- or 400
     *  if the token cannot be parsed at all (bad signature or malformed). */
    @PostMapping("/revoke")
    public ResponseEntity<Void> revoke(@RequestBody RevokeRequest request) {
        try {
            jwtService.revoke(request.token());
        } catch (JwtException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.noContent().build();
    }
}
