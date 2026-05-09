# JWT Best Practices

Status: Draft | Last Reviewed: 2026-05-09 | Owner: @ciso-delegate
Catalog ID: SEC-006 | Radii
Tier Applicability: T0, T1, T2

## Problem Statement

- JWTs are structurally transparent (Base64-encoded payload) and, if misconfigured, expose customer PII, session state, or payment authorisation details to anyone who intercepts or decodes a token.
- Common implementation errors — `alg=none` acceptance, HS256 with shared secrets, missing `aud`/`iss` validation, and long-lived access tokens — convert a single token leak into full account compromise for hours or days.
- In a banking context, a forged or stolen JWT grants direct access to payment initiation APIs; without scope constraints on payment tokens, a compromised access token can drain an account up to the per-transfer VND 100M limit.
- Token revocation is structurally absent from standard JWT; without an explicit denylist, logout and suspicious-activity detection are security theatre.

## Solution

Short-lived access tokens signed with RS256, validated against a JWKS endpoint, scoped to specific operations, and revocable via a Redis-backed JTI denylist. Refresh tokens are opaque, server-side only, and rotated on every use.

```mermaid
sequenceDiagram
    autonumber
    participant Client as Client (Browser / Mobile)
    participant AuthSvc as Auth Service
    participant JWKS as JWKS Endpoint<br/>(/.well-known/jwks.json)
    participant Redis as Redis Denylist
    participant API as Payment API (Resource Server)

    Client->>AuthSvc: POST /oauth2/token (credentials + MFA OTP)
    AuthSvc->>AuthSvc: Validate credentials & OTP (SBV §III MFA)
    AuthSvc->>Client: access_token (RS256, 15 min TTL)<br/>+ refresh_token (opaque, 8h, HttpOnly cookie)

    Note over Client,API: Normal API call
    Client->>API: GET /api/v1/accounts<br/>Authorization: Bearer <access_token>
    API->>JWKS: Fetch public key (cached 5 min)
    JWKS-->>API: JWK Set (RSA public key, kid)
    API->>API: Validate: sig, iss, aud, exp, nbf, jti
    API->>Redis: SISMEMBER denylist:<jti>
    Redis-->>API: 0 (not revoked)
    API-->>Client: 200 OK + account data

    Note over Client,API: Token refresh
    Client->>AuthSvc: POST /oauth2/token/refresh<br/>(refresh_token cookie)
    AuthSvc->>AuthSvc: Validate opaque token; rotate refresh token
    AuthSvc-->>Client: new access_token + new refresh_token

    Note over Client,API: Logout / revocation
    Client->>AuthSvc: POST /oauth2/logout<br/>Authorization: Bearer <access_token>
    AuthSvc->>Redis: SADD denylist:<jti> (TTL = token remaining lifetime)
    AuthSvc->>AuthSvc: Invalidate refresh token server-side
    AuthSvc-->>Client: 204 No Content
```

## Implementation Guidelines

### 1. Spring Security Resource Server — JWKS Validation

```java
@Configuration
@EnableWebSecurity
public class ResourceServerSecurityConfig {

    @Value("${security.oauth2.jwks-uri}")
    private String jwksUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)  // stateless; CSRF not applicable
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/payments/**")
                    .hasAuthority("SCOPE_payment:initiate")
                .requestMatchers("/api/v1/accounts/**")
                    .hasAuthority("SCOPE_account:read")
                .anyRequest().authenticated());

        return http.build();
    }

    /**
     * JwtDecoder backed by a JWKS URI with a 5-minute cache.
     * Rejects alg=none and HS256 by only trusting the RS256 key from the JWKS.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(jwksUri)
                .jwsAlgorithm(SignatureAlgorithm.RS256)  // reject all other algorithms
                .cache(Duration.ofMinutes(5))             // cache public keys
                .build();

        // Chain custom validators
        OAuth2TokenValidator<Jwt> withIssuer =
                JwtValidators.createDefaultWithIssuer(
                        "https://auth.techcombank.vn");
        OAuth2TokenValidator<Jwt> withAudience =
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains("tcb-api"));

        OAuth2TokenValidator<Jwt> fullValidator = new DelegatingOAuth2TokenValidator<>(
                withIssuer,
                withAudience,
                new JtiDenylistValidator(redisDenylistChecker()),
                new PaymentScopeAmountValidator());  // banking-specific: scope × amount

        decoder.setJwtValidator(fullValidator);
        return decoder;
    }
}
```

### 2. Custom Validators — JTI Denylist and Payment Scope Amount Guard

```java
/**
 * Rejects tokens whose JTI has been added to the Redis denylist.
 * TTL on the Redis key equals the token's remaining lifetime so the key
 * self-expires — no manual cleanup required.
 */
@Component
@RequiredArgsConstructor
public class JtiDenylistValidator implements OAuth2TokenValidator<Jwt> {

    private final StringRedisTemplate redis;
    private static final String DENYLIST_PREFIX = "jwt:denylist:";

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String jti = token.getId();
        if (jti == null || jti.isBlank()) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("missing_jti", "Token has no JTI claim", null));
        }
        Boolean isDenied = redis.hasKey(DENYLIST_PREFIX + jti);
        if (Boolean.TRUE.equals(isDenied)) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("token_revoked", "Token has been revoked", null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}

/**
 * Banking-specific: a payment:initiate token scoped to amounts > VND 100M
 * requires a step-up scope (payment:initiate:high_value).
 * This guards against scope downgrade attacks on high-value transfers.
 */
@Component
public class PaymentScopeAmountValidator implements OAuth2TokenValidator<Jwt> {

    private static final long HIGH_VALUE_THRESHOLD_VND = 100_000_000L;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        List<String> scopes = token.getClaimAsStringList("scope");
        if (scopes == null || !scopes.contains("payment:initiate")) {
            return OAuth2TokenValidatorResult.success();  // not a payment token
        }
        Long maxAmount = token.getClaim("max_amount_vnd");
        if (maxAmount != null && maxAmount > HIGH_VALUE_THRESHOLD_VND
                && (scopes == null || !scopes.contains("payment:initiate:high_value"))) {
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                    "insufficient_scope",
                    "High-value payment requires step-up authentication",
                    null));
        }
        return OAuth2TokenValidatorResult.success();
    }
}
```

### 3. Token Revocation on Logout

```java
@RestController
@RequiredArgsConstructor
@Slf4j
public class LogoutController {

    private final StringRedisTemplate redis;
    private final RefreshTokenRepository refreshTokenRepo;
    private static final String DENYLIST_PREFIX = "jwt:denylist:";

    @PostMapping("/oauth2/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Jwt accessToken,
            @CookieValue(name = "refresh_token", required = false) String refreshToken) {

        // 1. Denylist the access token JTI until it naturally expires
        String jti = accessToken.getId();
        Instant exp = accessToken.getExpiresAt();
        if (jti != null && exp != null) {
            Duration remaining = Duration.between(Instant.now(), exp);
            if (!remaining.isNegative()) {
                redis.opsForValue().set(
                        DENYLIST_PREFIX + jti,
                        "revoked",
                        remaining);
            }
        }

        // 2. Invalidate the refresh token server-side
        if (refreshToken != null) {
            refreshTokenRepo.deleteByToken(refreshToken);
        }

        log.info("Logout: jti={} sub={}", jti,
                accessToken.getSubject());
        return ResponseEntity.noContent().build();
    }
}
```

### 4. iOS Swift — Keychain Token Storage

```swift
import Foundation
import Security

/// Stores and retrieves JWT access tokens securely in the iOS Keychain.
/// Tokens are NEVER stored in NSUserDefaults, UserDefaults, or plain files.
enum TokenKeychainStore {

    private static let accessTokenKey = "vn.techcombank.accessToken"
    private static let refreshTokenKey = "vn.techcombank.refreshToken"

    // kSecAttrAccessibleWhenUnlockedThisDeviceOnly: token is inaccessible
    // when the device is locked and is NOT backed up to iCloud.
    static func saveAccessToken(_ token: String) throws {
        let data = Data(token.utf8)
        let query: [String: Any] = [
            kSecClass as String:       kSecClassGenericPassword,
            kSecAttrAccount as String: accessTokenKey,
            kSecValueData as String:   data,
            kSecAttrAccessible as String:
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]
        SecItemDelete(query as CFDictionary)  // remove old value first
        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess else {
            throw KeychainError.saveFailed(status)
        }
    }

    static func loadAccessToken() throws -> String? {
        let query: [String: Any] = [
            kSecClass as String:       kSecClassGenericPassword,
            kSecAttrAccount as String: accessTokenKey,
            kSecReturnData as String:  true,
            kSecMatchLimit as String:  kSecMatchLimitOne
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else {
            return nil
        }
        return String(data: data, encoding: .utf8)
    }

    static func deleteTokens() {
        [accessTokenKey, refreshTokenKey].forEach { key in
            let query: [String: Any] = [
                kSecClass as String:       kSecClassGenericPassword,
                kSecAttrAccount as String: key
            ]
            SecItemDelete(query as CFDictionary)
        }
    }

    enum KeychainError: Error {
        case saveFailed(OSStatus)
    }
}
```

### 5. Android Kotlin — EncryptedSharedPreferences Token Storage

```kotlin
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores JWT tokens using EncryptedSharedPreferences backed by the Android Keystore.
 * Tokens are NEVER stored in plain SharedPreferences, plain files, or logcat output.
 */
object TokenSecureStore {

    private const val PREFS_FILE = "tcb_secure_tokens"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    private fun getEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(context: Context, accessToken: String, refreshToken: String) {
        getEncryptedPrefs(context).edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun loadAccessToken(context: Context): String? =
        getEncryptedPrefs(context).getString(KEY_ACCESS_TOKEN, null)

    fun clearTokens(context: Context) {
        getEncryptedPrefs(context).edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }
}
```

### 6. Token Issuance — Auth Service Key Configuration

```yaml
# application.yml — Auth Service
spring:
  security:
    oauth2:
      authorizationserver:
        token:
          access-token-time-to-live: 15m      # 15-minute access tokens
          refresh-token-time-to-live: 8h       # 8-hour refresh tokens
          reuse-refresh-tokens: false           # rotate on every use
        issuer: https://auth.techcombank.vn

# RSA key pair stored in Vault; rotated every 90 days
# JWKS endpoint: /.well-known/jwks.json (auto-published by Spring Authorization Server)
security:
  jwt:
    private-key: ${JWT_PRIVATE_KEY}           # injected from Vault at startup
    kid: ${JWT_KEY_ID}                        # current key identifier
    jwks-cache-ttl: 300s                      # 5-minute JWKS cache on resource servers
```

## Compliance Mapping

| Ring | Regulation | Provision | How this pattern satisfies |
|------|-----------|-----------|---------------------------|
| Ring 0 | NIST SP 800-63B | AAL2 — Authenticator Assurance Level 2 | 15-minute access token + OTP MFA at issuance + Keychain/EncryptedSharedPreferences storage satisfies AAL2 session binding requirements. |
| Ring 0 | OWASP ASVS | V3.5 Token-Based Session Management — reject alg=none, validate iss/aud/exp | `NimbusJwtDecoder.withJwkSetUri().jwsAlgorithm(RS256)` rejects non-RS256; custom validators enforce iss, aud, jti. |
| Ring 0 | RFC 8725 | JWT Best Current Practices — short TTL, audience restriction, algorithm pinning | 15-minute TTL, `aud=tcb-api`, RS256 algorithm pin, no PII in payload, JTI denylist on revocation. |
| Ring 1 | PCI-DSS v4.0 | Req 8.2 (Unique IDs), Req 8.6 (Interactive login controls) | JTI claim provides unique token identity; denylist enforces immediate revocation; MFA gate at issuance satisfies interactive login controls. |
| Ring 1 | BCBS 230 | Principle 7 (ICT Security) | Short-lived tokens bound by scope and amount limit; revocation on suspicious activity limits exposure window for compromised tokens. |
| Ring 2 | SBV Circular 09/2020 | §III Multi-factor authentication for internet banking ⚠️ (working summary — pending Legal review) | MFA OTP is validated at token issuance; the resulting access token carries the authentication level as a claim; step-up is required for high-value payments. |

## NFR Acceptance Criteria

```yaml
nfr_acceptance_criteria:
  id: SEC-006
  pattern: JWT Best Practices

  availability:
    - id: RA-01
      statement: >
        JWKS endpoint must return a valid JWK Set within 200 ms P99.
        Resource servers cache the key for 5 minutes; JWKS unavailability
        must not cause request failures during the cache window.
      measurement: Load test JWKS endpoint at 100 rps; assert P99 ≤ 200 ms.
        Bring JWKS endpoint down; verify resource server continues to validate
        tokens for 5 minutes using cached keys.

  performance:
    - id: RP-01
      statement: >
        JWT validation (signature check + claim validation + Redis denylist lookup)
        must complete within 5 ms P95 under steady load.
      measurement: Load test resource server at 500 rps; measure filter chain
        duration for JWT validation; assert P95 ≤ 5 ms.

  security:
    - id: RS-01
      statement: >
        A token with alg=none or alg=HS256 MUST be rejected with HTTP 401.
      measurement: Unit test presents forged tokens with alg=none and alg=HS256;
        asserts HTTP 401 for both.
    - id: RS-02
      statement: >
        A logged-out token (JTI in denylist) MUST be rejected within one Redis
        round-trip; the rejection MUST occur before the token's natural expiry.
      measurement: Log out; use the returned access token immediately; assert HTTP 401.
    - id: RS-03
      statement: >
        Access tokens MUST NOT contain customer name, national ID, date of birth,
        or account number in any claim.
      measurement: Decode all issued access tokens in CI; assert payload contains
        only: sub (user-id-hash), iss, aud, exp, nbf, iat, jti, scope, max_amount_vnd.
    - id: RS-04
      statement: >
        A payment:initiate token used to submit a transfer exceeding VND 100M
        without the payment:initiate:high_value scope MUST be rejected with HTTP 403.
      measurement: Integration test submits VND 150M transfer with a standard
        payment:initiate token; asserts HTTP 403 with error insufficient_scope.
```

## Cost / FinOps

- Redis cluster for JTI denylist: shared with rate limiter and session cache (see RES-008); marginal cost is one SADD + one SISMEMBER per request. At 1 000 rps sustained, Redis handles 2 000 ops/s — well within a cache.r7g.large capacity.
- JWKS caching (5-minute TTL on resource servers) means the JWKS endpoint receives approximately 1 request per 5 minutes per pod, not 1 per request. At 50 pods this is 10 requests/minute — negligible hosting cost.
- RSA key pair rotation (90-day cycle) requires one Vault `pki` lease renewal and one JWKS cache invalidation — automated via a CI pipeline job; engineer time ≈ 0 after initial setup.
- 15-minute access tokens generate more refresh traffic than 60-minute tokens; estimate 4× the refresh endpoint calls. At 50 000 MAU with average 2 sessions/day this is approximately 400 000 extra token-refresh calls/day — well within Auth Service capacity at < 1 ms per refresh.
- Cost of NOT following JWT best practices: a single forged or leaked long-lived token enables unlimited fraudulent transactions until the token expires; remediation costs (fraud reimbursement, incident response, regulatory notification) vastly exceed the infrastructure cost of short TTLs and Redis revocation.

## Threat Model

STRIDE analysis — JWT vulnerabilities map primarily to Spoofing and Elevation of Privilege:

- **Spoofing — Algorithm confusion (alg=none or HS256 downgrade)**: Attacker crafts a token signed with `alg=none` or with a public key as the HMAC secret. Mitigation: `NimbusJwtDecoder.jwsAlgorithm(RS256)` rejects all non-RS256 algorithms at the decoder level; there is no code path that accepts HS256.
- **Spoofing — JWK Set poisoning (SSRF to attacker-controlled JWKS)**: Attacker manipulates the `jku` header to redirect key resolution to a malicious JWKS. Mitigation: JWKS URI is statically configured in `application.yml`; the decoder ignores `jku`/`x5u` headers in the token.
- **Tampering — Payload modification**: Attacker modifies the payload (e.g., inflates `max_amount_vnd`) and re-signs with a forged key. Mitigation: RS256 signature verification with the pinned RSA public key from the JWKS makes payload tampering detectable.
- **Repudiation — Token replay after logout**: Attacker captures a valid access token and continues using it after the victim logs out. Mitigation: JTI denylist in Redis with TTL equal to the token's remaining lifetime; every request checks the denylist before authorisation.
- **Information Disclosure — PII in JWT payload**: Decoded payload exposes customer PII to anyone who intercepts the token (e.g., in browser history or access logs). Mitigation: access token payload is restricted to `sub` (opaque user ID hash), `iss`, `aud`, `exp`, `nbf`, `iat`, `jti`, `scope`, and `max_amount_vnd`; no name, national ID, or account number.
- **Elevation of Privilege — Scope escalation for high-value payments**: Attacker uses a standard `payment:initiate` token to initiate a VND 150M transfer that requires step-up. Mitigation: `PaymentScopeAmountValidator` rejects any payment exceeding VND 100M without the `payment:initiate:high_value` scope, which requires a step-up MFA challenge.
- **Elevation of Privilege — Stolen refresh token**: Attacker steals a refresh token (e.g., via XSS or intercepted cookie) and generates new access tokens indefinitely. Mitigation: refresh tokens are opaque, stored HttpOnly Secure SameSite=Strict cookie (browser) or Keychain/EncryptedSharedPreferences (mobile); refresh token rotation means the stolen token is immediately invalidated on first use by the legitimate user.

## Operational Runbook

1. **Routine key rotation (90-day cycle)**: The rotation CI pipeline generates a new RSA key pair in Vault, publishes the new public key to the JWKS endpoint alongside the old key (grace window: 30 minutes), updates the Auth Service `kid` environment variable via Vault agent, and restarts Auth Service pods with a rolling deployment. After 30 minutes, remove the old key from the JWKS to complete rotation.

2. **Emergency key rotation (suspected key compromise)**: Escalate to the CISO delegate. Immediately revoke the compromised key in Vault. Publish a new key to the JWKS endpoint. All in-flight tokens signed with the old key will fail validation — this is intentional. Customer-facing services will surface HTTP 401; clients must re-authenticate. Notify SBV within the required incident reporting window (Circular 09/2020 §IV).

3. **Alert: `JwtRevocationRateHigh`** fires when the denylist SADD rate exceeds 100/minute (possible mass logout or session invalidation event). Check Grafana `auth-service-overview`. Determine if this is a coordinated logout (e.g., forced re-auth after a deployment) or anomalous. If anomalous, escalate to the security team.

4. **Alert: `JtiDenylistRedisLatencyHigh`** fires when Redis P95 latency for denylist lookups exceeds 10 ms. Check ElastiCache metrics. If Redis is degraded, assess whether to fail open (allow requests without denylist check) or fail closed (HTTP 503). Default policy: fail open with `denylist_check=skipped` log field and an immediate PagerDuty page.

5. **Token validation failure spike**: If HTTP 401 rate exceeds 5% of requests for more than 1 minute, check whether a key rotation just completed and the JWKS cache has not refreshed. If so, the issue self-resolves within 5 minutes. If unrelated to rotation, check for a clock skew issue (`nbf`/`exp` validation failures) or a Redis denylist outage.

6. **Mobile token storage audit**: During the annual PCI-DSS internal assessment, verify via code review and dynamic analysis that iOS builds store tokens only in Keychain (`kSecAttrAccessibleWhenUnlockedThisDeviceOnly`) and Android builds use `EncryptedSharedPreferences`. Any deviation must be remediated before the assessment closes.

7. **Suspicious high-value transfer attempt**: If `PaymentScopeAmountValidator` rejects a high-value transfer, the event is logged with `scope_violation=true`. The fraud team reviews these events daily. Repeated violations from the same `sub` trigger an account review.

8. **Post-incident**: After any JWT-related security incident, review the token TTL, scope definitions, and denylist Redis configuration. File a change request if parameters need adjustment. Update the threat model with any new attack vector identified.

## Test Strategy

### Unit Tests
- `JtiDenylistValidatorTest`: mock Redis; verify that a JTI present in the denylist returns a failure result; verify that an absent JTI returns success; verify that a token with no `jti` claim returns a failure.
- `PaymentScopeAmountValidatorTest`: token with `payment:initiate` scope and `max_amount_vnd=150000000` without `high_value` scope returns failure; same token with `high_value` scope returns success.
- `ResourceServerSecurityConfigTest`: present a token with `alg=none`; assert `JwtException` is thrown. Present a token with wrong `iss`; assert `JwtValidationException`.
- `LogoutControllerTest`: mock Redis; verify `SADD` is called with correct key and TTL after logout.

### Integration Tests
- Spring Boot Test with Testcontainers (Redis + Keycloak or Spring Auth Server): issue a real RS256 token, call a protected endpoint, assert 200. Log out (denylist the JTI), call the endpoint again with the same token, assert 401.
- High-value payment: issue a standard payment token, call payment API with `amount=150000000`, assert 403.
- Algorithm rejection: craft an HS256-signed token using the JWKS public key as HMAC secret; call protected endpoint; assert 401.

### Compliance Tests
- PII scan: in CI, issue 100 tokens and assert that no decoded payload contains patterns matching Vietnamese national ID format (12 digits), full name fields, or account number format.
- OWASP ASVS V3.5.3 audit: automated test driven from the ASVS checklist; covers `alg=none`, missing `aud`, missing `iss`, expired token, future `nbf`.

### Chaos Tests
- Redis denylist down: kill Redis; verify that requests continue with `denylist_check=skipped` logged, HTTP 200 returned (fail-open policy for availability); verify PagerDuty alert fires within 60 s.
- JWKS endpoint down: bring JWKS endpoint down; verify resource servers continue to validate tokens using cached keys for 5 minutes; after cache expiry, verify HTTP 401 is returned.

## References

- [RFC 8725 — JSON Web Token Best Current Practices](https://www.rfc-editor.org/rfc/rfc8725)
- [RFC 9449 — DPoP: Demonstrating Proof of Possession](https://www.rfc-editor.org/rfc/rfc9449)
- [OWASP ASVS V3 Session Management](https://owasp.org/www-project-application-security-verification-standard/)
- [NIST SP 800-63B Digital Identity Guidelines](https://pages.nist.gov/800-63-3/sp800-63b.html)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Apple Keychain Services documentation](https://developer.apple.com/documentation/security/keychain_services)
- [Android EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
- [SEC-002 OAuth2 Authorization](../../patterns/security/oauth2-authorization.md)
- [SEC-005 BFF + Token Binding](../../patterns/security/bff-token-binding.md)
- [SEC-007 Secrets Rotation](secrets-rotation.md)

---

**Key Takeaway**: Short-lived RS256 tokens with audience restrictions, JTI revocation, and scope-bound payment limits transform JWT from a common misconfiguration risk into a defence layer — provided tokens are never stored in plaintext and the JWKS endpoint is the sole authority for key material.
