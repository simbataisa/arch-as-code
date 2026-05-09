# Data Classification Framework

Data classification defines sensitivity levels and handling requirements for all data processed by Techcombank systems. Every data type must be classified, and architecture designs must implement controls matching the classification tier.

---

## Classification Overview

Data is classified into four tiers based on sensitivity and business impact if disclosed:

| Tier | Sensitivity | Examples | Access | Storage | Transmission | Retention |
|------|---|---|---|---|---|---|
| **Public** | None | Marketing content, public APIs, docs | Unrestricted | Any | Any | Unrestricted |
| **Internal** | Low | Policies, org charts, roadmaps | Employees | Secure | Encrypted preferred | 1 year |
| **Confidential** | High | Customer PII, account data, transactions | Role-restricted | Encrypted | TLS required | 3-7 years |
| **Restricted** | Critical | Passwords, credentials, card numbers, keys | Minimal (audit) | HSM/Vault | Encrypted always | 1 year (destroy after use) |

---

## Tier 1: Public

### Definition
Information with no sensitivity. Disclosure poses no business or legal risk. Intended for external consumption.

### Examples
- Marketing materials and advertisements
- Public website content
- General company information
- Press releases
- Public API documentation
- Community contributions
- Blog posts

### Handling Rules

**Storage:**
- Any storage system acceptable
- No encryption required
- No access control needed
- CDN distribution acceptable

**Transmission:**
- HTTP acceptable (HTTPS preferred for performance)
- Public channels acceptable
- No confidentiality required

**Retention:**
- No specific retention requirement
- Can be indefinite
- Archival acceptable

**Access Control:**
- Anyone may access
- No authentication required
- Public read by default

**Handling Example:**
```markdown
## Public Data Handling

Product brochure ("Top 5 Reasons to Bank with Techcombank"):
- Stored on public website and CDN
- No encryption
- No access logs needed
- Anyone can download and share
```

---

## Tier 2: Internal

### Definition
Business-sensitive information intended for employees and authorized partners only. Not customer-facing. Disclosure could cause competitive harm or operational disruption.

### Examples
- Internal policy documentation
- Organizational structure and reporting lines
- Internal roadmap and strategic plans
- Business process documentation
- Internal training materials
- Employee directory
- Budget and financial forecasts
- Internal communications
- Code repositories (source code)
- System architecture diagrams (non-sensitive)

### Handling Rules

**Storage:**
- Secure file storage (Confluence, SharePoint, GitLab)
- Authentication required
- Encryption recommended
- Access logs recommended
- Backup with retention

**Transmission:**
- Company network only (not public internet)
- TLS encryption recommended
- VPN required if remote access
- No file sharing with public links

**Retention:**
- 1 year standard
- Longer if regulatory requirement
- Secure deletion after retention period
- No indefinite storage

**Access Control:**
- Employees only by default
- Department-level permissions
- Contractors with signed NDAs only
- No public access
- Access reviewed annually

**Handling Example:**
```markdown
## Internal Data Handling

Techcombank 2026 Strategic Roadmap:
- Stored in internal Confluence
- Access: VP and above only
- Transmission: Internal network + TLS
- Encryption: AES-256 at rest recommended
- Retention: 1 year after fiscal year end
- Access logs: Monthly review
- Destruction: Secure wipe after retention
```

---

## Tier 3: Confidential

### Definition
Customer-sensitive and business-critical information. Disclosure could result in legal liability, customer harm, or financial loss. Requires strong controls.

### Examples
- Customer personally identifiable information (PII):
  - Name, address, phone, email
  - Social Security Number (SSN)
  - Government-issued ID numbers
  - Date of birth
  - Employment history

- Financial data:
  - Account balances and transactions
  - Loan details and terms
  - Credit scores
  - Payment history
  - Account opening documents

- Customer communications:
  - Customer service conversations
  - Transaction details
  - Investment advice
  - Loan applications
  - Support tickets with personal data

- Business contracts:
  - Customer agreements
  - Vendor contracts
  - Partnership agreements
  - Non-disclosure agreements

- Internal audit data:
  - Compliance reports (not regulatory filings)
  - Risk assessments
  - Internal audit findings
  - Security assessments

### Handling Rules

**Storage:**
- Encrypted at AES-256 (database encryption)
- Isolated in secure databases or data lakes
- Access control by role/department
- No uncontrolled backups
- Backup encryption required
- Access logs maintained and audited
- Geographic restrictions: Vietnam only (or per regulation)

**Transmission:**
- TLS 1.2+ required for all channels
- Encrypted protocols: HTTPS, gRPC with mTLS
- No unencrypted email or file sharing
- Secure file transfer systems (not WeTransfer, public cloud)
- VPN required for remote access

**Retention:**
- 3-7 years depending on business use
- Financial data: 7 years (regulatory)
- Customer service data: 3 years
- Loan/credit files: 7 years
- Secure deletion after retention
- Quarterly purge of expired data

**Access Control:**
- Need-to-know basis: only employees with job function requiring access
- Role-based access control (RBAC)
- Multi-factor authentication for sensitive systems
- Quarterly access reviews
- Audit logs of all access (who, what, when)
- Manager approval for new access

**PII Masking:**
- In development/test environments: PII must be masked
- In logs: mask before storage
- In error messages: never expose PII
- Display last 4 digits only when possible (e.g., SSN: ***-**-1234)

**Handling Example:**
```markdown
## Confidential Data Handling

Customer Account Balances:
- Stored in: PostgreSQL with transparent encryption (AES-256)
- Encrypted backups: Daily backups, encrypted separate keys
- Transmission: HTTPS only, TLS 1.2+
- Access: Tellers and above, by account owner
- Access control: RBAC + MFA
- Audit logs: All access logged, SSN masked
- Retention: 7 years (regulatory requirement)
- Deletion: Secure wipe per NIST guidelines
- Disaster recovery: Encrypted offsite backup, tested quarterly

Customer SSN:
- Encrypted at DB field level: AES-256
- Never logged in plaintext
- Never in error messages
- Display only: ***-**-1234
- Key management: HSM-backed KMS
- Rotation: Annual key rotation with re-encryption
```

---

## Tier 4: Restricted

### Definition
Highly sensitive information critical to information security infrastructure. Unauthorized access would cause severe harm. Requires maximum controls: encryption, HSM storage, minimal access, audit logging.

### Examples
- Cryptographic keys:
  - Encryption keys (master keys, data keys)
  - Private keys for certificates
  - API signing keys
  - Session keys

- Credentials:
  - Database passwords
  - Admin account passwords
  - API keys for external services
  - OAuth refresh tokens
  - SSH private keys
  - Service account credentials

- Sensitive payment data:
  - Full credit/debit card numbers (PAN)
  - Card security codes (CVV/CVC)
  - PIN blocks
  - Magnetic stripe data (Track 1/2)
  - Any cardholder data (PCI-DSS Tier 4)

- Biometric data:
  - Fingerprints
  - Facial recognition data
  - Iris scans
  - Voice signatures

- Regulatory/legal sensitive:
  - Litigation documents
  - Settlement agreements
  - Government investigation records
  - Evidence of security breaches (initial phase)

### Handling Rules

**Storage:**
- Hardware Security Module (HSM) or Vault only
- Never in databases, files, or environment variables
- Encrypted with master key held in HSM
- No backup of keys (keys are generated, not stored)
- Access: Minimal (service accounts via Vault only)
- Audit logging: Every access logged and retained indefinitely
- Location: On-premise or highly restricted cloud region

**Transmission:**
- mTLS with certificate pinning (no MITM possible)
- Short-lived tokens (TTL < 1 hour)
- Never in email, Slack, chat, or communication tools
- Physical delivery for initial setup (hand-off only)
- Never logged or printed

**Retention:**
- Minimum retention: Only as long as needed
- Destroy immediately after use
- No archival: Once rotated, old key destroyed
- Rotation frequency: 90-180 days
- Destruction certified: Independent verification

**Access Control:**
- Absolute minimum necessary access
- No human access (automated systems only)
- Service-to-service access via OAuth2 Client Credentials
- Administrative access: 2-person rule (two admins required to enable)
- Audit: Every access logged with full context
- Breach notification: Immediate if unauthorized access detected

**Restricted Data Policies:**
- Never log: Credentials, keys, tokens in logs
- Never display: Secrets in error messages, stack traces
- Never commit: To Git or version control
- Never cache: In browser, client, or shared cache
- Destruction: Secure wipe (NIST 800-88)

**Handling Example:**
```markdown
## Restricted Data Handling

Database Master Key (PostgreSQL):
- Stored in: AWS KMS or HashiCorp Vault (HSM-backed)
- Encryption: Master Key Encryption Key (MKEK) in HSM
- Access: Only via Vault API, not human access
- Service access: PostgreSQL service authenticates to Vault
- Audit: Every retrieval logged: timestamp, service, purpose
- Rotation: Annual rotation
- Destruction: HSM purges old key material
- Disaster Recovery: Backup in separate HSM, tested quarterly
- Failure mode: Service cannot start if key unavailable (fail-safe)

API Key for Payment Gateway:
- Stored in: HashiCorp Vault secret engine
- Encryption: AES-256 at rest in Vault
- Access: Only payment-service can read via mTLS
- TTL: 90 days (automatic rotation)
- Rotation: Automated, new key before old key revoked
- Audit: Access logged, rotation logged
- Breach procedure: Immediate revocation, new key generated
- Never: In code, config, env vars, logs, Slack, email

Credit Card Numbers (PAN):
- Compliance: PCI-DSS Level 1
- Storage: Tokenization primary (never store if possible)
- If stored: Network-isolated HSM only
- Encryption: Format-preserving encryption (retains last 4 digits)
- Access: Zero human access (automated systems only)
- Transmission: Encrypted always, P2P encrypted channels
- Audit: All access logged, monitored for anomalies
- Retention: Shortest possible, destroy on transaction completion
```

---

## Classification Decision Tree

Use this to classify data correctly:

```
Is this data intended for public consumption?
├─ Yes → PUBLIC
└─ No ↓

Is this customer personally identifiable information (PII)?
├─ Yes → CONFIDENTIAL
└─ No ↓

Is this a cryptographic key, password, API key, or credential?
├─ Yes → RESTRICTED
└─ No ↓

Would unauthorized access cause significant customer harm or legal liability?
├─ Yes → CONFIDENTIAL
└─ No ↓

Would unauthorized access cause operational disruption or competitive harm?
├─ Yes → INTERNAL
└─ No ↓

Default: INTERNAL
```

---

## Data Classification Mapping for DAB

Every DAB submission must include a Data Classification section in Section 5 (Security Assessment):

```markdown
## Data Classification Mapping

### Input Data
| Data Field | Classification | Rationale | Storage Tier |
|---|---|---|---|
| Customer Name | Confidential | PII | Encrypted DB |
| Account Number | Confidential | Financial identifier | Encrypted DB |
| Transfer Amount | Confidential | Financial data | Encrypted DB |
| Merchant ID | Internal | Business data, no PII | Standard DB |
| Card Number | Restricted | PCI-DSS requirement | HSM/Tokenization |

### Processing & Output
| Data | Classification | Handling |
|---|---|---|
| In-Memory Transfer Object | Confidential | Never logged, encrypted if cached |
| API Response (transfer ID, status) | Confidential | TLS only, short-lived cache |
| Audit Logs | Confidential | PII masked, 7-year retention |
| Error Messages | Internal | No PII exposure, generic messages to client |

### Data at Rest
| System | Data Classified | Encryption | Access Control |
|---|---|---|---|
| PostgreSQL | Confidential | AES-256 | RBAC, MFA |
| Redis Cache | Confidential | TLS + DB encryption | Service identity |
| Kafka Topics | Confidential | TLS + AES-256 | Service ACLs |
| S3 Backups | Confidential | AES-256 with separate key | IAM policies |

### Data in Transit
| Channel | Data Classified | Encryption |
|---|---|---|
| API (HTTPS) | Confidential | TLS 1.2+ |
| gRPC (mTLS) | Confidential | TLS 1.2+ + client cert |
| Kafka | Confidential | SASL/SSL |
| Database | Confidential | TLS + local socket |

### Regulatory Alignment
- PCI-DSS: Card numbers classified Restricted, tokenized
- GDPR: Customer PII (Confidential), right to deletion honored
- Vietnam Data Protection Law: Confidential data stored in-country
```

---

## Special Cases & Scenarios

### Development & Testing Data

**Rule:** Never use real customer data in development/test environments.

**Alternatives:**
- **Synthetic data:** Generated test records (valid format, no real PII)
- **Masked production data:** Real data with PII masked
- **Dummy data:** Hardcoded test values

**Masking Rules for Test Data:**
```
Customer Name: "Test Customer 001"
SSN: "***-**-1234"
Email: "test001@testing.local"
Phone: "+84-***-**-1234"
Account Number: "****5678"
```

### Data Retention & Destruction

**Secure Deletion Methods:**
- **Database:** DELETE statement with VACUUM (PostgreSQL)
- **File Systems:** shred, wipe (overwrite 3+ times)
- **SSD:** TRIM command or AES key destruction (if encrypted)
- **HSM Keys:** Direct key purge from HSM
- **Physical:** Shredding or incineration (certified)

**Certification Required:** Destruction must be documented with:
- What data destroyed
- When destroyed
- Method used
- Who performed destruction
- Independent verification

### Cross-Border Data Transfer

**Restriction:** Confidential and Restricted data must NOT leave Vietnam unless:
- Regulatory requirement (e.g., disaster recovery)
- Explicit customer consent
- Data processing agreement in place
- Equivalent security controls in destination country

**Implementation:**
```sql
-- Database geo-constraint
CREATE TABLE customers (
  id UUID PRIMARY KEY,
  name TEXT,
  -- Geo-constraint: Must stay in Vietnam region
  region_constraint: 'VN'  -- Checked by application
);

-- Application validation
if (customer.region_constraint != "VN") {
  throw new Exception("Cannot transfer PII outside Vietnam")
}
```

### Audit & Compliance Verification

**Quarterly Review:**
1. Scan databases for unclassified data
2. Verify access logs are complete
3. Confirm encryption is active
4. Test data retention/destruction procedures
5. Review incidents and access anomalies

**Tools:**
- `SELECT * FROM pg_stat_user_tables` (PostgreSQL data discovery)
- DLP (Data Loss Prevention) tools
- SIEM (Security Information & Event Management)
- Manual code review for hardcoded PII

---

## Related Documents
- [Security Baseline](./security-baseline.md)
- [API Standards — Authentication](./api-standards.md)
- [DAB Full Process — Section 5 (Security Assessment)](../dab-process/dab-full-process.md#section-5-security--compliance-assessment)
- Vietnam Data Protection Law (national regulation)
- PCI-DSS Standard (for payment card data)
- GDPR Compliance (if EU customers)
