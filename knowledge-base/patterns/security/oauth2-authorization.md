# OAuth2 & OIDC Authorization Pattern

Status: Approved | Last Reviewed: 2026-02-18 | Owner: @ea-board
Catalog ID: SEC-002 | Radii
Tier Applicability: T0, T1, T2

## Problem Statement

Traditional username/password authentication has issues:
- Direct credential storage in applications (exposure risk)
- Password reuse across services
- Difficult to revoke access without password change
- No audit trail of who accessed what
- Managing permissions across multiple services is complex
- Third-party integrations require sharing passwords (insecure)

## Solution

Use OAuth2 for authorization and OpenID Connect (OIDC) for authentication. Delegate to centralized identity provider.

```
┌──────────┐        ┌─────────────────────┐       ┌──────────┐
│ Resource │        │ Authorization       │       │ Resource │
│ Owner    │        │ Server              │       │ Server   │
│(User)    │        │(Keycloak/Okta)      │       │(API)     │
└────┬─────┘        └──────────┬──────────┘       └────┬─────┘
     │                         │                      │
     │ 1. Login                │                      │
     ├────────────────────────>│                      │
     │                         │                      │
     │ 2. Authorization Code   │                      │
     │<────────────────────────┤                      │
     │                         │                      │
     │           3. Exchange Code + Secret with Authorization Server
     ├────────────────────────────────────────────────────────────>│
     │                         │                      │
     │                         │  4. Access Token    │
     │<──────────────────────────────────────────────────────────┤
     │                         │                      │
     │     5. Call API with Access Token              │
     ├─────────────────────────────────────────────────────────────>│
     │                         │                      │
     │                         │  6. Validate Token  │
     │                         │<─────────────────────┤
     │                         │                      │
     │                         │  7. Token Valid     │
     │                         ├─────────────────────>│
     │                         │                      │
     │                   8. Protected Resource      │
     │<───────────────────────────────────────────────┤
```

## Implementation Guidelines

1. **OAuth2 Grant Types**

   **Authorization Code Flow** (Web apps, most secure)
   ```
   1. User clicks "Login with Auth Server"
   2. Redirected to: https://auth-server/authorize?
        client_id=app123&
        redirect_uri=https://myapp/callback&
        response_type=code&
        scope=openid profile email&
        state=random123
   3. User authenticates
   4. Auth server redirects: https://myapp/callback?code=abc123&state=random123
   5. Backend exchanges: POST /token
        grant_type=authorization_code&
        code=abc123&
        client_id=app123&
        client_secret=secret&
        redirect_uri=https://myapp/callback
   6. Receive access_token, id_token, refresh_token
   ```

   **Client Credentials Flow** (Service-to-service)
   ```
   POST https://auth-server/token
   grant_type=client_credentials&
   client_id=service-a&
   client_secret=secret&
   scope=api:read api:write

   Response:
   {
     "access_token": "eyJhbGc...",
     "token_type": "Bearer",
     "expires_in": 3600
   }
   ```

   **Resource Owner Password** (Legacy, not recommended)
   ```
   POST /token
   grant_type=password&
   username=user@example.com&
   password=userpassword&
   client_id=app123&
   client_secret=secret
   ```

2. **Spring Security OAuth2 Configuration**
   ```java
   @Configuration
   @EnableWebSecurity
   public class SecurityConfig {

     @Bean
     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
       return http
         .authorizeHttpRequests(authz -> authz
           .requestMatchers("/public/**").permitAll()
           .requestMatchers("/api/accounts/**").hasAuthority("SCOPE_accounts:read")
           .requestMatchers("/api/payments/**").hasAuthority("SCOPE_payments:write")
           .anyRequest().authenticated()
         )
         .oauth2ResourceServer(oauth2 -> oauth2
           .jwt(jwt -> jwt.decoder(jwtDecoder()))
           .bearerTokenResolver(bearerTokenResolver())
         )
         .cors().and()
         .csrf().disable()
         .build();
     }

     @Bean
     public JwtDecoder jwtDecoder() {
       return NimbusJwtDecoder
         .withJwkSetUri("https://auth-server/oauth2/authorize/.well-known/jwks.json")
         .build();
     }

     @Bean
     public BearerTokenResolver bearerTokenResolver() {
       DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
       resolver.setBearerTokenHeaderName(HttpHeaders.AUTHORIZATION);
       return resolver;
     }
   }
   ```

3. **Scope Design** (Least Privilege)
   ```
   accounts:read       - Read account data
   accounts:write      - Create/update accounts
   payments:read       - Read payment history
   payments:write      - Initiate payments
   transfers:read      - Read transfer history
   transfers:write     - Initiate transfers
   admin:manage-users  - Manage other users
   admin:audit-logs    - Access audit logs
   ```

   Assign minimal scopes to each client:
   ```java
   // OAuth2 Token with specific scopes
   {
     "sub": "user_123",
     "username": "john.doe@techcombank.com",
     "scope": "accounts:read transfers:read",
     "iss": "https://auth-server",
     "exp": 1678000000,
     "iat": 1677996400
   }
   ```

4. **Token Management**
   ```java
   @Service
   public class TokenService {

     @Autowired
     private OAuth2RestOperations oauth2RestTemplate;

     // Request access token
     public AccessToken getAccessToken(String clientId, String clientSecret) {
       MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
       params.add("grant_type", "client_credentials");
       params.add("client_id", clientId);
       params.add("client_secret", clientSecret);
       params.add("scope", "api:read api:write");

       try {
         ResponseEntity<AccessToken> response = oauth2RestTemplate
           .postForEntity(
             "https://auth-server/oauth2/token",
             new HttpEntity<>(params),
             AccessToken.class
           );
         return response.getBody();
       } catch (OAuth2Exception e) {
         log.error("Failed to obtain token", e);
         throw new TokenException("Token request failed");
       }
     }

     // Validate token
     public boolean isTokenValid(String token) {
       try {
         JwtClaimsSet claims = JwtDecoder.decode(token);
         return claims.getExpiresAt().isAfter(Instant.now());
       } catch (JwtException e) {
         return false;
       }
     }

     // Refresh token
     public AccessToken refreshToken(String refreshToken) {
       MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
       params.add("grant_type", "refresh_token");
       params.add("refresh_token", refreshToken);
       params.add("client_id", clientId);
       params.add("client_secret", clientSecret);

       return oauth2RestTemplate.postForObject(
         "https://auth-server/oauth2/token",
         new HttpEntity<>(params),
         AccessToken.class
       );
     }
   }
   ```

5. **OIDC OpenID Connect** (Authentication layer on top of OAuth2)
   ```java
   @Configuration
   public class OidcSecurityConfig {

     @Bean
     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
       return http
         .authorizeHttpRequests(authz -> authz
           .requestMatchers("/public/**").permitAll()
           .anyRequest().authenticated()
         )
         .oauth2Login(oauth2 -> oauth2
           .clientRegistrationRepository(clientRegistrationRepository())
           .authorizedClientRepository(authorizedClientRepository())
         )
         .oauth2ResourceServer(oauth2 -> oauth2
           .jwt(jwt -> jwt.decoder(jwtDecoder()))
         )
         .logout(logout -> logout
           .logoutSuccessUrl("https://auth-server/logout")
         )
         .build();
     }

     @Bean
     public ClientRegistrationRepository clientRegistrationRepository() {
       return new InMemoryClientRegistrationRepository(
         ClientRegistration.withRegistrationId("auth-server")
           .clientId("my-app")
           .clientSecret("secret")
           .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
           .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
           .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
           .scope("openid", "profile", "email")
           .authorizationUri("https://auth-server/oauth2/authorize")
           .tokenUri("https://auth-server/oauth2/token")
           .userInfoUri("https://auth-server/oauth2/userinfo")
           .userNameAttributeName(IdTokenClaimNames.SUB)
           .jwkSetUri("https://auth-server/oauth2/.well-known/jwks.json")
           .build()
       );
     }
   }
   ```

6. **Best Practices**
   - **Always use HTTPS**: Tokens in URLs/headers must be encrypted
   - **Short-lived access tokens**: 15-60 minutes (OAuth2 standard)
   - **Long-lived refresh tokens**: 7-30 days, rotatable
   - **Revocation list**: Maintain revoked tokens (in Redis for performance)
   - **Scope minimization**: Grant only necessary scopes
   - **Token rotation**: Refresh tokens after use

## Supported Identity Providers

| Provider | Recommendation |
|----------|---|
| **Keycloak** | Open-source, on-premises |
| **Okta** | SaaS, enterprise-grade |
| **Auth0** | SaaS, developer-friendly |
| **Azure AD** | Enterprise Microsoft shops |
| **AWS Cognito** | AWS-native |

## OpenID Connect Discovery

```
GET /.well-known/openid-configuration

Response:
{
  "issuer": "https://auth-server",
  "authorization_endpoint": "https://auth-server/oauth2/authorize",
  "token_endpoint": "https://auth-server/oauth2/token",
  "userinfo_endpoint": "https://auth-server/oauth2/userinfo",
  "jwks_uri": "https://auth-server/oauth2/.well-known/jwks.json",
  "scopes_supported": ["openid", "profile", "email"],
  "response_types_supported": ["code", "token"],
  "grant_types_supported": ["authorization_code", "refresh_token", "client_credentials"]
}
```

## When to Use

- User authentication (use OIDC)
- API authorization (use OAuth2)
- Third-party integrations
- Mobile apps (browser-based login)
- Single Sign-On (SSO)

## References

- [OAuth 2.0 RFC 6749](https://tools.ietf.org/html/rfc6749)
- [OpenID Connect](https://openid.net/connect/)
- [Spring Security OAuth2](https://spring.io/guides/topicals/spring-security-and-angular-js/)
- [Keycloak](https://www.keycloak.org/)

---

**Key Takeaway**: Use OAuth2 for authorization, OIDC for authentication. Centralize identity in auth server. Grant minimal scopes. Use short-lived access tokens.
