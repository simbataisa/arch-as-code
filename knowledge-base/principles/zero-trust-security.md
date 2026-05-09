# Zero-Trust Security

Status: Approved | Last Reviewed: 2026-02-01 | Owner: @ea-board
Catalog ID: PRIN-003 | Radii
Tier Applicability: T0, T1, T2, T3

## Problem Statement

Legacy perimeter-based security assumes internal network is safe:
- Compromised internal service can freely access others (lateral movement)
- Lost credentials are not detected
- No verification between services
- Compliance violations (PCI-DSS, ISO 27001 require per-hop authentication)
- Cannot implement least-privilege access

## Solution

Verify every request, every connection, every time—regardless of source or network location.

### Core Principles

1. **Assume Breach**: Any endpoint could be compromised; verify everything
2. **Verify at Every Hop**: mTLS for service-to-service, OAuth for user access
3. **Least Privilege**: Grant minimum required permissions; revoke unused access
4. **Explicit Allow**: Default deny; only allow explicitly approved access
5. **Continuous Monitoring**: Detect anomalies and revoke access immediately

## Implementation Guidelines

1. **Service-to-Service (mTLS)**
   - Mutual TLS authentication between all services
   - Certificate rotation every 90 days (automated via service mesh)
   - Istio/Linkerd handles certificate lifecycle automatically
   - Network policies restrict communication to approved pairs

2. **Token Validation at Every Hop**
   - Validate JWT/OAuth tokens at service entry point
   - Extract subject (user ID) and scopes
   - Verify token signature and expiration
   - Check token against revocation list (if revocation needed)
   - Example validation:
     ```java
     @Configuration
     public class SecurityConfig {
       @Bean
       public SecurityFilterChain filterChain(HttpSecurity http) {
         return http
           .authorizeHttpRequests(auth -> auth
             .requestMatchers("/api/public/**").permitAll()
             .requestMatchers("/api/accounts/**").hasScope("accounts:read")
             .anyRequest().authenticated()
           )
           .oauth2ResourceServer(oauth2 -> oauth2
             .jwt(jwt -> jwt.decoder(jwtDecoder()))
           )
           .build();
       }
     }
     ```

3. **API Authentication**
   - Users: OAuth2/OIDC (delegated identity provider)
   - Service accounts: mTLS certificates or signed JWTs
   - External APIs: API keys (short-lived, rotatable)
   - Require authentication for all endpoints (no anonymous access)

4. **Network Segmentation**
   - Service A cannot directly reach Service C unless explicitly allowed
   - Use NetworkPolicy (Kubernetes) or security groups
   - Implement zero-trust proxy (mTLS termination)
   - Database access only from app servers (network ACLs)

5. **Secrets Management**
   - Never hardcode credentials (database passwords, API keys, certificates)
   - Use HashiCorp Vault or cloud-native secrets manager
   - Rotate secrets quarterly (or automatically via dynamic secrets)
   - Audit all secret access

6. **Audit and Monitoring**
   - Log all authentication/authorization decisions
   - Alert on failed login attempts, permission denials
   - Monitor for privilege escalation
   - Implement centralized log aggregation (ELK, Splunk)

## Techcombank Zero-Trust Checklist

- [ ] All services use mTLS for inter-service communication
- [ ] All user-facing APIs require OAuth2/OIDC token
- [ ] Network policies restrict service-to-service communication
- [ ] Secrets stored in vault, not in code or environment
- [ ] Certificate rotation automated (90-day cycle)
- [ ] All authentication/authorization logged with audit trail
- [ ] Admin access requires multi-factor authentication
- [ ] Quarterly access reviews to remove unused permissions

## When to Apply

- All Techcombank production systems (mandatory)
- All integrations with external partners
- Compliance-regulated domains (payments, customer data)
- Internal systems handling PII

## References

- [NIST Zero Trust Architecture](https://csrc.nist.gov/publications/detail/sp/800-207/final)
- [Istio Security](https://istio.io/latest/docs/concepts/security/)
- [OAuth 2.0 Authorization Framework](https://tools.ietf.org/html/rfc6749)
- [HashiCorp Vault](https://www.vaultproject.io/)

---

**Key Takeaway**: Implement mTLS for service-to-service, OAuth for user access, network policies for segmentation, and Vault for secrets. Verify and audit every request.
