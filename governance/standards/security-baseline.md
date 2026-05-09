# Security Baseline for DAB Submissions

Mandatory minimum security controls that must be implemented in all Design Approval Board submissions. These baseline controls are non-negotiable and apply to every architecture, service, integration, and data pipeline.

---

## Security Control Framework

All DAB submissions must demonstrate compliance with these baseline controls. See [Data Classification](./data-classification.md) for data tier definitions.

---

## 1. Authentication (Mandatory)

### Requirement
All user-facing and inter-service APIs must use modern authentication mechanisms.

### For User/Customer APIs
- **OAuth 2.0** (recommended) with Authorization Code or Device Code flows
- **OpenID Connect (OIDC)** for identity + authentication
- **Multi-factor authentication (MFA)** required for financial transactions and admin access

**Not acceptable:**
- Basic auth (username/password) over HTTP
- Custom authentication schemes
- Session-only authentication without token refresh

### For Service-to-Service APIs
- **OAuth 2.0 Client Credentials flow** for system accounts
- **mTLS (mutual TLS)** with client certificate validation
- **API Key rotation** every 90 days (if used for low-risk internal APIs only)

**Not acceptable:**
- Hardcoded credentials in code or config
- Shared passwords across services
- API keys without rotation policy

### Implementation Example (DAB Section 5 must include)
```markdown
## Authentication Design

**User APIs:**
- OAuth 2.0 Authorization Code flow via auth.techcombank.local
- Token endpoint: https://auth.techcombank.local/oauth2/token
- Token lifetime: 1 hour
- Refresh tokens valid for 30 days (must be stored securely on client)
- MFA via TOTP or SMS for transactions > VND 10,000,000

**Service APIs:**
- mTLS with x509 certificates, 2-year rotation
- Service identity via SPIFFE (Secure Production Identity Framework)
- Certificate revocation checked via OCSP stapling

**Credential Storage:**
- Service secrets stored in HashiCorp Vault
- Access logged and audited
- Automatic rotation: 180 days
```

---

## 2. Authorization (Mandatory)

### Requirement
Every API must enforce role-based or attribute-based access control. No API should trust all authenticated users equally.

### Minimum Standard: Role-Based Access Control (RBAC)
- Define roles based on job function (Admin, Operator, User, Read-Only)
- Assign users to roles
- Enforce role checks on every API endpoint
- Log authorization decisions

**Example:**
```yaml
endpoints:
  /transfers:
    POST: [Role: Teller, Role: Customer]           # Can create transfers
    GET: [Role: Teller, Role: Customer]            # Can view own transfers
  /transfers/{id}/approve:
    POST: [Role: Manager, Role: Compliance]        # Can approve transfers
  /audit-logs:
    GET: [Role: Admin, Role: Auditor]              # Can view audit logs
```

### Advanced Standard: Attribute-Based Access Control (ABAC)
For complex policies, use attributes (user.department, transaction.amount, time.hour):

```
Rule: Teller can transfer up to VND 100,000,000 per day
If user.role = "Teller" AND transaction.amount <= 100,000,000 AND transaction.daily_total <= 100,000,000
Then ALLOW
Else DENY
```

### Implementation Checklist (DAB Section 5)
- [ ] Authorization model defined (RBAC or ABAC)
- [ ] All API endpoints have role/permission checks
- [ ] Default deny policy (no implicit allow)
- [ ] Authorization decisions logged
- [ ] Privilege escalation prevention (no admin flag in JWT)
- [ ] Regular access reviews (quarterly minimum)

---

## 3. Encryption in Transit (Mandatory)

### Requirement
All data in transit must be encrypted using industry-standard TLS.

### Minimum Standard
- **TLS 1.2 or higher** (TLS 1.3 preferred)
- **All connections** over HTTPS or mTLS
- **Strong cipher suites** (no weak ciphers like DES, RC4)
- **Certificate validation** (hostname verification, chain validation)

### TLS Configuration

**Acceptable Cipher Suites (TLS 1.2):**
```
TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
```

**Not acceptable:**
```
TLS_RSA_WITH_AES_128_CBC_SHA         ❌ No ECDHE (forward secrecy)
TLS_DES_CBC3_SHA                     ❌ Deprecated algorithm
```

### Certificate Management
- Certificates from trusted CAs (Techcombank PKI or public CA)
- Certificate validity period: max 1 year
- Certificate renewal: 30 days before expiration
- OCSP stapling or CRL checks enabled
- Automated alerts for expiring certificates

### DAB Section 5 Template
```markdown
## Encryption in Transit

**Protocol:** HTTPS only, TLS 1.2+
**Cipher Suites:** TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384, TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
**Certificate Authority:** [Internal PKI or Let's Encrypt]
**Certificate Validity:** 1 year
**Renewal Process:** Automated 30 days before expiration
**HSTS Policy:** max-age=31536000, includeSubDomains

## Encryption at Rest

**Database:** AES-256-GCM (default encryption)
**Backup:** AES-256-GCM with separate key
**Cache (Redis):** TLS_PORT, encryption at rest via redis-cli CONFIG
**Logs:** Sensitive fields masked before storage
```

---

## 4. Encryption at Rest (Mandatory)

### Requirement
Sensitive data stored in databases, caches, and backups must be encrypted using strong symmetric encryption.

### Minimum Standard
- **AES-256** encryption (or equivalent, e.g., ChaCha20-Poly1305)
- **Authenticated encryption** (GCM, ChaCha20-Poly1305 with auth tag)
- **Key management** via Techcombank Key Management Service (KMS) or HashiCorp Vault
- **Key rotation** every 365 days

### Database Encryption
```sql
-- PostgreSQL with pgcrypto
CREATE EXTENSION pgcrypto;

-- Encrypt PII columns
CREATE TABLE customers (
  id UUID PRIMARY KEY,
  name TEXT,  -- Consider encryption if PII
  email TEXT ENCRYPTED WITH AES_256,  -- Encrypted at DB level
  ssn TEXT ENCRYPTED WITH AES_256,    -- Highly sensitive
  created_at TIMESTAMP
);

-- Application-level encryption (preferred)
-- Encrypt before INSERT, decrypt after SELECT
ENCRYPTED_SSN = AES_256_ENCRYPT(ssn, kms.get_key('customer-ssn'))
```

### Cache Encryption (Redis)
```yaml
# Redis configuration for encrypted cache
requirepass: "[strong-password-from-vault]"
tls-port: 6380
tls-cert-file: "/etc/redis/certs/redis.crt"
tls-key-file: "/etc/redis/certs/redis.key"
tls-ca-cert-file: "/etc/redis/certs/ca.crt"

# Encryption at rest (optional, via RDB encryption plugin)
modules:
  - /usr/lib/redis/modules/redis-crypt.so
```

### Key Management
```markdown
## Key Management

**KMS Provider:** HashiCorp Vault (or AWS KMS)
**Key Storage:** HSM-backed (hardware security module)
**Key Rotation:** Automated every 365 days
**Key Access:**
  - Only services with explicit permissions can access keys
  - All key access logged
  - Keys never logged, printed, or exposed in errors

**Encryption Key Hierarchy:**
1. Master Key (in HSM, never leaves hardware)
2. Data Encryption Key (DEK, encrypted with master key)
3. Field-level keys (for PII: encrypted with DEK)
```

---

## 5. Logging & Audit Trail (Mandatory)

### Requirement
All access to sensitive operations and data must be logged with sufficient detail for forensic analysis. All logs must have PII masked to prevent data exposure.

### What Must Be Logged

**Access & Authentication:**
- Every login attempt (success and failure)
- API access by user/service
- Authorization checks (approved and denied)
- Token generation and revocation

**Data Operations:**
- CREATE: Who, what, when (for sensitive data)
- UPDATE/DELETE: Before/after values (masked), timestamp, user
- Data export/download
- Database backups

**Security Events:**
- Failed authentication attempts (threshold-based alerts)
- Authorization denials
- Rate limit violations
- Suspicious patterns (e.g., bulk data export)

### Log Format & Structure

```json
{
  "timestamp": "2026-03-08T10:30:45.123Z",
  "requestId": "req-20260308-abc123",
  "userId": "user-12345",
  "action": "TRANSFER_CREATED",
  "resource": "/v1/transfers",
  "method": "POST",
  "result": "SUCCESS",
  "statusCode": 201,
  "sourceIp": "203.0.113.42",
  "userAgent": "Mozilla/5.0...",
  "details": {
    "transferAmount": 1000.50,
    "destinationAccountId": "[MASKED_ACCOUNT]",  ← PII MASKED
    "duration_ms": 145
  }
}
```

### PII Masking Rules (Mandatory)

**Account numbers:** `[MASKED_ACCOUNT]` or `****5678`
**SSN/National ID:** `***-**-1234` (last 4 visible)
**Email:** `user@[MASKED]`
**Phone:** `+84-***-**-1234`
**Card numbers:** `4111-****-****-1111`
**Passwords/Tokens:** NEVER log, EVER

### Log Retention

| Log Type | Retention | Searchability |
|----------|-----------|---|
| **Authentication/Access** | 1 year | Indexed, queryable |
| **Data modifications (PII)** | 7 years | Indexed, queryable |
| **Error logs** | 90 days | Indexed |
| **Debug logs** | 7 days | Not indexed (performance) |

### Implementation Checklist (DAB Section 5)
- [ ] Logging library configured (e.g., SLF4J, winston)
- [ ] PII masked in all logs
- [ ] Unique request ID in every log entry
- [ ] Log aggregation: ELK, Splunk, or CloudWatch
- [ ] Log retention policy documented
- [ ] Access logs searchable and auditable
- [ ] Alerts configured for security events

---

## 6. Data Classification Alignment (Mandatory)

### Requirement
All data handled by the system must be classified per [Data Classification](./data-classification.md) standard, and storage/transmission must match classification tier.

### Classification Tiers

| Tier | Examples | Storage | Encryption | Access Control | Retention |
|------|----------|---------|-----------|---|---|
| **Public** | Marketing materials, public APIs | Any | Not required | Public read | Unrestricted |
| **Internal** | Business docs, org charts, policies | Secure | Recommended | Employees only | 1 year |
| **Confidential** | Customer PII, financial data, contracts | Restricted | Required (AES-256) | Role-based | 3 years |
| **Restricted** | Credentials, keys, card numbers | HSM/Vault | Required (AES-256 + HSM) | Minimal (audit) | 1 year |

### DAB Section 5 Requirement: Data Classification Mapping

```markdown
## Data Classification

**Input Data:**
- Customer Name: Confidential (PII)
- Account Number: Restricted (financial identifier)
- Transfer Amount: Confidential (financial)
- Merchant ID: Internal (business data)

**Output/Response Data:**
- Transfer ID: Internal
- Transfer Status: Confidential
- Timestamp: Internal
- Error messages: Internal (never expose system details)

**Data at Rest:**
- PostgreSQL backup: Encrypted at AES-256, Restricted tier
- Redis cache: TLS encrypted, Confidential tier
- Kafka topics: Encrypted at AES-256, Confidential tier

**Data in Transit:**
- All HTTPS: TLS 1.2+
- All gRPC: mTLS with certificates
- No unencrypted channels allowed
```

---

## 7. Vulnerability Scanning & Assessment (Mandatory)

### Requirement
All code, dependencies, and infrastructure must be scanned for known vulnerabilities before deployment. Critical vulnerabilities must be remediated.

### Code Scanning
- **SAST (Static Application Security Testing):** SonarQube or similar
  - Scan for SQL injection, XSS, hardcoded secrets
  - Dependency scanning for known CVEs
  - Run on every commit in CI/CD

- **Penetration Testing:** For customer-facing APIs
  - Test for OWASP Top 10 vulnerabilities
  - Annual or before major release
  - Include business logic flaws

### Dependency Management
```bash
# NPM: Check for vulnerable packages
npm audit

# Python: Safety check
pip install safety
safety check

# Java: OWASP Dependency Check
dependency-check --scan .

# Go: Nancy
nancy sleuth --quiet
```

### Infrastructure Scanning
- Container image scanning: Trivy, Clair
- Kubernetes RBAC audit
- IAM permission audit
- Network security group rules audit

### DAB Section 5 Requirement: Security Assessment Results

```markdown
## Vulnerability Assessment

**Code Scanning (SonarQube):**
- Critical issues: 0
- High issues: 0
- Coverage: >80% (unit tests)
- No hardcoded secrets found

**Dependency Scan:**
- npm audit: 0 vulnerabilities
- Maven dependency-check: 0 high-risk CVEs
- Last scan: 2026-03-08

**OWASP Top 10 Review:**
✅ A01:2021 - Broken Access Control: Mitigated (RBAC enforced)
✅ A02:2021 - Cryptographic Failures: Mitigated (AES-256)
✅ A03:2021 - Injection: Mitigated (parameterized queries)
✅ A04:2021 - Insecure Design: N/A
✅ A05:2021 - Security Misconfiguration: Mitigated (IaC validated)
✅ A06:2021 - Vulnerable Components: 0 known CVEs
✅ A07:2021 - Identification & Auth Failures: OAuth2 implemented
✅ A08:2021 - Data Integrity Failures: Checksums, signatures
✅ A09:2021 - Logging & Monitoring: ELK configured
✅ A10:2021 - SSRF: Not applicable (no URL-based requests)

**Penetration Test Results:**
- Scheduled: Q2 2026
- Status: Planned before production release
```

---

## 8. Network Segmentation & Zero Trust (Recommended for Full DAB)

### Requirement for Financial Transactions
- Services communicate over encrypted channels only
- No trust based on network location
- Every service authenticates to every other service

### Implementation
```yaml
# Kubernetes NetworkPolicy
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: payment-service-policy
spec:
  podSelector:
    matchLabels:
      app: payment-service
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: api-gateway
    ports:
    - protocol: TCP
      port: 8080
  egress:
  - to:
    - namespaceSelector:
        matchLabels:
          name: databases
    ports:
    - protocol: TCP
      port: 5432
  - to:
    - namespaceSelector:
        matchLabels:
          name: kafka
    ports:
    - protocol: TCP
      port: 9093
```

---

## 9. Secrets Management (Mandatory)

### Requirement
No secrets (passwords, API keys, certificates) in code, configs, or environment variables. Secrets must be managed centrally.

### Acceptable Secret Stores
- **HashiCorp Vault** (recommended)
- **AWS Secrets Manager**
- **Kubernetes Secrets** (with encryption at rest enabled)
- **Azure Key Vault**

**Not acceptable:**
- `.env` files in Git
- Environment variables in deployment specs
- Secrets in application config files
- Hardcoded credentials in code

### Secret Rotation Policy
| Secret Type | Rotation Frequency | Automated |
|---|---|---|
| Database passwords | 180 days | Yes |
| API keys | 90 days | Yes |
| Service certificates | 365 days | Yes |
| SSH keys | 1 year | Manual review |
| TLS certificates | 365 days | Yes (automated renewal) |

### Vault Integration Example
```yaml
# Kubernetes Pod injecting secrets from Vault
apiVersion: v1
kind: Pod
metadata:
  name: payment-service
  annotations:
    vault.hashicorp.com/agent-inject: "true"
    vault.hashicorp.com/agent-inject-secret-database: "secret/data/payment-db"
    vault.hashicorp.com/agent-inject-template-database: |
      {{ with secret "secret/data/payment-db" -}}
      export DB_PASSWORD="{{ .Data.data.password }}"
      {{- end }}
    vault.hashicorp.com/role: "payment-service"
spec:
  containers:
  - name: payment-service
    image: payment-service:v1
    envFrom:
    - secretRef:
        name: payment-secrets  # Populated by Vault agent
```

---

## Security Baseline Checklist (Required for DAB Section 5)

Copy and complete this checklist in every DAB submission:

```markdown
## Security Baseline Certification

### Authentication
- [ ] Authentication mechanism chosen: [OAuth2 | OIDC | mTLS]
- [ ] Token endpoint defined: ___________
- [ ] MFA required for admin/financial operations
- [ ] No hardcoded credentials in code/config

### Authorization
- [ ] Authorization model defined: [RBAC | ABAC]
- [ ] All endpoints have access control
- [ ] Default deny policy enforced
- [ ] Authorization decisions logged

### Encryption in Transit
- [ ] TLS 1.2+ on all endpoints
- [ ] Strong cipher suites configured
- [ ] Certificate validation enabled
- [ ] HSTS header set (if HTTP)
- [ ] No cleartext communication

### Encryption at Rest
- [ ] AES-256 for sensitive data
- [ ] Encryption keys managed by KMS/Vault
- [ ] Key rotation every 365 days
- [ ] Database, backups, cache all encrypted

### Logging & Audit
- [ ] Access logging enabled
- [ ] PII masked in logs
- [ ] Unique request IDs in logs
- [ ] Log retention: _____ years
- [ ] Security events alerted

### Data Classification
- [ ] All data classified per standard
- [ ] Classification mapped to controls
- [ ] Sensitive data encrypted
- [ ] Access control matches tier

### Vulnerability Scanning
- [ ] Code scanning (SAST): _____ tool
- [ ] Dependency scan: _____ date
- [ ] Critical vulnerabilities: 0
- [ ] High vulnerabilities: 0
- [ ] Penetration test planned: [Yes | No]

### Secrets Management
- [ ] Secret store chosen: [Vault | AWS Secrets | K8s Secrets]
- [ ] No secrets in code/config/env
- [ ] Secret rotation automated: [Yes | No]
- [ ] Rotation frequency: _____ days

### Network Security
- [ ] Network policies defined: [Yes | No]
- [ ] Service-to-service mTLS: [Yes | No]
- [ ] Zero trust implemented: [Yes | No]

## Signature

**Security Architect:** ___________ **Date:** ___________

**Status:** [✅ COMPLIANT | ⚠️ CONDITIONAL | ❌ NON-COMPLIANT]

**Notes:** _______________________________________________
```

---

## Compliance & Audit

### Annual Security Assessment
- Review all systems for baseline compliance
- Update baseline if industry standards change
- Audit log access patterns
- Test incident response procedures

### Incident Response
- Security incident triggers immediate investigation
- Root cause analysis within 24 hours
- Notification to stakeholders per policy
- Post-incident review within 5 days

---

## Related Documents
- [Data Classification](./data-classification.md)
- [DAB Full Process — Section 5 (Security Assessment)](../dab-process/dab-full-process.md#section-5-security--compliance-assessment)
- Techcombank IT Security Policy (internal)
- OWASP Top 10: https://owasp.org/Top10/
