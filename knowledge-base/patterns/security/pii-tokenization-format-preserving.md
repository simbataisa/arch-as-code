# PII Tokenization (Format-Preserving)

Status: Draft | Last Reviewed: 2026-05-09 | Owner: @ciso-delegate
Catalog ID: SEC-013 | Radii
Tier Applicability: T0, T1

## Problem Statement

Standard random tokenisation replaces a value with an opaque surrogate that breaks downstream systems expecting a specific format:

- **Legacy schema rigidity**: T24 core banking and partner fixed-format files expect a 12-digit account number, a 16-digit PAN, or a 12-digit CCCD national ID. Random UUIDs break field-length constraints and character-class validations.
- **Analytics and reporting pipeline breakage**: SQL column types, ETL transformations, and regulatory report templates are built for a specific field shape. Opaque tokens force expensive schema migrations across dozens of dependent systems.
- **PAN in transit to partners**: card scheme partners (Visa, Mastercard) and payment processors require PAN in its canonical 16-digit form during certain protocol handshakes. Random tokens cannot substitute.
- **Full masking is not enough**: masking (SEC-008) is irreversible — it destroys data. Tokenisation must be reversible by authorised callers (fraud operations, account reconciliation) while remaining indistinguishable from noise to unauthorised parties.
- **Key management complexity**: ad hoc encryption solutions scatter keys in application config. Format-preserving encryption (FPE) backed by an HSM provides NIST-standardised security without per-application key management.

## Solution

Replace PII with a format-preserving token produced by AES-FF1 (NIST SP 800-38G). The token is the same length and character class as the plaintext. The token vault stores the mapping in an HSM-protected key-value store. Downstream systems — analytics, T24, partners — see only tokens and require no schema changes. De-tokenisation is gated by ABAC (SEC-010) and logged for audit.

```mermaid
flowchart TD
    subgraph INGEST["Ingestion / API Boundary"]
        RAW[Raw PII\nCCCD: 501234567890\nPhone: +84901234567\nPAN: 4111111111111111]
        FPE_SVC["FPE Service\n(AES-FF1 / NIST SP 800-38G)"]
    end

    subgraph VAULT["HashiCorp Vault — Transform Secret Engine"]
        HSM["HSM-protected\nFPE Keys\n(per data class)"]
        TOK_STORE[Token → Plaintext mapping\nencrypted at rest]
    end

    subgraph DOWNSTREAM["Downstream Consumers"]
        T24[T24 Core Banking\n(sees token, same format)]
        ANALYTICS[Analytics / DWH\n(sees token, no schema change)]
        REPORTING[Regulatory Reports\n(sees token, correct shape)]
    end

    subgraph PRESENT["Presentation / Authorised Lookup"]
        FRAUD[Fraud Operations\n(ABAC: role=FRAUD_INVESTIGATOR)]
        DETOKEN["De-tokenise API\n/internal/v1/detoken\n(audit logged)"]
        PLAIN[Plaintext returned\nto authorised caller only]
    end

    RAW -->|tokenise request| FPE_SVC
    FPE_SVC <-->|AES-FF1 encrypt/decrypt| HSM
    FPE_SVC -->|store mapping| TOK_STORE
    FPE_SVC -->|format-preserving token| T24
    FPE_SVC -->|format-preserving token| ANALYTICS
    FPE_SVC -->|format-preserving token| REPORTING
    FRAUD -->|authorised lookup| DETOKEN
    DETOKEN <-->|decrypt| HSM
    DETOKEN --> PLAIN

    classDef raw fill:#fdecea,stroke:#c62828
    classDef safe fill:#e8f5e9,stroke:#2e7d32
    classDef vault fill:#fff8e1,stroke:#f9a825
    classDef svc fill:#e3f2fd,stroke:#1565c0
    class RAW raw
    class T24,ANALYTICS,REPORTING,PLAIN safe
    class HSM,TOK_STORE vault
    class FPE_SVC,DETOKEN svc
```

### FPE Format Preservation Reference

| PII Class | Plaintext Example | Token Example | Preserved Properties |
|---|---|---|---|
| CCCD National ID | `501234567890` | `728391045621` | 12 digits, no leading zero guarantee |
| VN Phone (domestic) | `0901234567` | `0734829164` | 10 digits, `0` prefix preserved |
| VN Phone (intl) | `+84901234567` | `+84734829164` | `+84` prefix preserved, 11 digits |
| Card PAN | `4111111111111111` | `4729384710293847` | 16 digits, BIN (first 6) preserved by tweak |
| Account Number | `000012345678` | `000073829164` | 12 digits, leading zeros preserved |

## Implementation Guidelines

### 1. HashiCorp Vault Transform Secret Engine (FPE Backend)

The Transform secret engine in HashiCorp Vault implements AES-FF1 natively. Configure one transformation per PII class.

```hcl
# Vault policy: allow FPE service account to encode/decode
path "transform/encode/cccd" {
  capabilities = ["update"]
}
path "transform/decode/cccd" {
  capabilities = ["update"]
}
path "transform/encode/phone" {
  capabilities = ["update"]
}
path "transform/decode/phone" {
  capabilities = ["update"]
}
path "transform/encode/pan" {
  capabilities = ["update"]
}
# decode/pan gated to FRAUD_INVESTIGATOR role only
path "transform/decode/pan" {
  capabilities = ["update"]
  required_parameters = ["role_id"]
}
```

```bash
# One-time Vault setup (run by platform team, not application)
vault secrets enable transform

# CCCD: 12-digit numeric
vault write transform/transformations/fpe/cccd \
  template="builtin/numeric" \
  tweak_source=internal \
  allowed_roles="fpe-service"

# Phone: 10-digit numeric (strip +84 prefix before tokenising)
vault write transform/transformations/fpe/phone \
  template="builtin/numeric" \
  tweak_source=internal \
  allowed_roles="fpe-service"

# PAN: 16-digit numeric, BIN-preserving via tweak
vault write transform/transformations/fpe/pan \
  template="builtin/creditcardnumber" \
  tweak_source=supplied \
  allowed_roles="fpe-service"
```

### 2. Java Spring Boot FPE Service

```java
@Service
@Slf4j
public class FpeTokenizationService {

    private final VaultTemplate vaultTemplate;
    private final AuditEventPublisher auditPublisher;

    public FpeTokenizationService(VaultTemplate vaultTemplate,
                                  AuditEventPublisher auditPublisher) {
        this.vaultTemplate  = vaultTemplate;
        this.auditPublisher = auditPublisher;
    }

    /**
     * Tokenise a PII value using AES-FF1 via Vault Transform engine.
     * The returned token is the same length and character class as the input.
     */
    public String tokenize(PiiClass piiClass, String plaintext) {
        Objects.requireNonNull(plaintext, "plaintext must not be null");
        String normalized = normalize(piiClass, plaintext);

        VaultTransformEncodeRequest request = VaultTransformEncodeRequest.builder()
            .value(normalized)
            .transformation(piiClass.vaultTransformation())
            .build();

        VaultTransformEncodeResponse response = vaultTemplate
            .opsForTransform()
            .encode("fpe-service", request);

        String token = denormalize(piiClass, plaintext, response.getEncodedValue());
        log.info("tokenize piiClass={} correlationId={}", piiClass,
                 MDC.get("correlationId")); // never log plaintext or token
        return token;
    }

    /**
     * De-tokenise — only callable by authorised roles (enforced by Vault policy
     * and Spring Security @PreAuthorize at the controller layer).
     */
    public String detokenize(PiiClass piiClass, String token, String requestorRole) {
        auditPublisher.publish(DetokenizeAuditEvent.of(piiClass, requestorRole,
                               MDC.get("correlationId")));
        String normalized = normalize(piiClass, token);

        VaultTransformDecodeRequest request = VaultTransformDecodeRequest.builder()
            .value(normalized)
            .transformation(piiClass.vaultTransformation())
            .build();

        VaultTransformDecodeResponse response = vaultTemplate
            .opsForTransform()
            .decode("fpe-service", request);

        return denormalize(piiClass, token, response.getDecodedValue());
    }

    /** Strip non-numeric prefix (+84) before sending to Vault; restore after. */
    private String normalize(PiiClass cls, String value) {
        return switch (cls) {
            case PHONE_INTL -> value.startsWith("+84")
                ? value.substring(3) : value;
            default -> value;
        };
    }

    private String denormalize(PiiClass cls, String original, String vaultResult) {
        return switch (cls) {
            case PHONE_INTL -> original.startsWith("+84")
                ? "+84" + vaultResult : vaultResult;
            default -> vaultResult;
        };
    }
}
```

```java
public enum PiiClass {
    CCCD("cccd"),
    PHONE_DOMESTIC("phone"),
    PHONE_INTL("phone"),
    CARD_PAN("pan"),
    ACCOUNT_NUMBER("account");

    private final String vaultTransformation;

    PiiClass(String vaultTransformation) {
        this.vaultTransformation = vaultTransformation;
    }

    public String vaultTransformation() { return vaultTransformation; }
}
```

### 3. `@FpeToken` Annotation for Automatic Tokenisation at the Service Boundary

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FpeToken {
    PiiClass piiClass();
}
```

```java
// AOP interceptor applies tokenisation on fields annotated @FpeToken
// before the object is passed to downstream adapters (T24 gateway, analytics export)
@Aspect
@Component
public class FpeTokenizationAspect {

    @Autowired private FpeTokenizationService fpeService;

    @Around("@annotation(com.techcombank.security.FpeTokenizeArgs)")
    public Object tokenizeArgs(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        for (int i = 0; i < args.length; i++) {
            args[i] = applyFpeTokens(args[i]);
        }
        return pjp.proceed(args);
    }

    private Object applyFpeTokens(Object obj) throws IllegalAccessException {
        if (obj == null) return null;
        for (Field field : obj.getClass().getDeclaredFields()) {
            FpeToken ann = field.getAnnotation(FpeToken.class);
            if (ann == null) continue;
            field.setAccessible(true);
            String val = (String) field.get(obj);
            if (val != null) field.set(obj, fpeService.tokenize(ann.piiClass(), val));
        }
        return obj;
    }
}
```

### 4. De-tokenisation API (Privileged, Audit-Logged)

```java
@RestController
@RequestMapping("/internal/v1/detoken")
public class DetokenizationController {

    @Autowired private FpeTokenizationService fpeService;

    @PostMapping
    @PreAuthorize("hasAnyRole('FRAUD_INVESTIGATOR', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<DetokenizeResponse> detokenize(
            @RequestBody @Valid DetokenizeRequest request,
            @AuthenticationPrincipal Jwt principal) {

        String plaintext = fpeService.detokenize(
            request.getPiiClass(),
            request.getToken(),
            principal.getClaimAsString("role")
        );
        return ResponseEntity.ok(new DetokenizeResponse(plaintext));
    }
}
```

### 5. iOS Swift — Tokenisation at API Boundary

```swift
// On iOS, PII is tokenised server-side before reaching the device.
// The app receives a token; display is masked per SEC-008.
// For de-tokenisation (e.g., full account number for fund transfer confirmation),
// the app calls the privileged endpoint with the user's JWT.

struct TokenizedField: Codable {
    let token: String          // format-preserving token, same length as plaintext
    let piiClass: String       // "CCCD", "PHONE", "ACCOUNT_NUMBER"
    let displayMasked: String  // pre-masked display value from server, e.g. "••••••••4567"
}

// NetworkService wraps the de-tokenise call; result shown only in a
// secured UIViewController with biometric authentication gate.
func detokenize(token: String, piiClass: String) async throws -> String {
    let request = DetokenizeRequest(token: token, piiClass: piiClass)
    let response: DetokenizeResponse = try await apiClient.post(
        path: "/internal/v1/detoken",
        body: request,
        requiresBiometric: true
    )
    return response.plaintext
}
```

### 6. Android Kotlin — Tokenisation Pattern

```kotlin
// Tokens arrive from the server in TokenizedField objects.
// De-tokenisation requires biometric confirmation and a valid JWT
// with FRAUD_INVESTIGATOR or CUSTOMER_VERIFIED scope.

data class TokenizedField(
    val token: String,
    val piiClass: String,
    val displayMasked: String   // "••••••••4567" — rendered directly in the UI
)

class AccountRepository(private val api: AccountApi,
                        private val biometricGate: BiometricAuthGate) {

    suspend fun detokenize(field: TokenizedField): Result<String> {
        val authed = biometricGate.authenticate() ?: return Result.failure(
            SecurityException("Biometric authentication required for de-tokenisation"))

        return runCatching {
            api.detokenize(DetokenizeRequest(field.token, field.piiClass))
                .plaintext
        }
    }
}
```

## Compliance Mapping

| Ring | Regulation | Provision | How this pattern satisfies |
|---|---|---|---|
| Ring 0 | NIST SP 800-38G | FF1 and FF3-1 Mode Specifications | The Vault Transform engine implements AES-FF1 per NIST SP 800-38G; no custom cryptography |
| Ring 0 | NIST SP 800-53 | SC-28 (Protection of Information at Rest); SC-12 (Cryptographic Key Establishment) | HSM-backed keys managed by Vault; key material never in application memory |
| Ring 0 | ISO 27001 | A.8.24 Use of Cryptography | FPE keys generated in HSM, rotated per key-lifecycle policy; algorithm selection documented in crypto register |
| Ring 1 | PCI-DSS v4.0 | §3.3 PAN protection; §3.5 Key management | PAN tokenised before storage and at ingestion boundary; Vault manages key rotation; de-tokenise access logged and restricted |
| Ring 1 | BCBS 239 | §3 Data Accuracy; §4 Data Granularity | Tokens are deterministic per (plaintext, key); analytics results remain consistent across reporting periods without exposing PII |
| Ring 2 | Decree 13/2023 | Art. 9 — Processing of Sensitive Personal Data; Art. 6 — Data Minimisation | CCCD national IDs and phone numbers are tokenised at ingestion; downstream systems process tokens only; plaintext access requires explicit authorisation and audit trail ⚠️ (working summary — pending Legal review) |
| Ring 2 | SBV Circular 09/2020 | §III — Cryptographic controls and key management | AES-FF1 with HSM-managed keys satisfies SBV cryptographic strength requirements; key rotation documented and automated ⚠️ (working summary — pending Legal review) |

## NFR Acceptance Criteria

```yaml
nfr_acceptance_criteria:
  catalog_id: SEC-013
  pattern: PII Tokenization (Format-Preserving)

  performance:
    - id: SEC-013-HP-01
      description: >
        FPE tokenise operation (single field, Vault round-trip) must complete
        within 10ms P95 under production concurrency (500 concurrent tokenise calls).
      measurement: micrometer timer on FpeTokenizationService.tokenize()
      threshold: p95 < 10ms

    - id: SEC-013-HP-02
      description: >
        Vault Transform engine must sustain 2,000 tokenise operations/second
        without queue depth exceeding 50 requests.
      measurement: Vault telemetry (vault.core.handle_request duration)
      threshold: throughput >= 2000 ops/sec; queue_depth <= 50

  security:
    - id: SEC-013-SEC-01
      description: >
        No plaintext PII (CCCD, phone, PAN, account number) may be stored in
        any database table not gated by the token vault. Verified by schema audit.
      measurement: automated schema scan for columns matching PII field names
        that are not foreign-keyed to the token vault
      threshold: 0 unprotected PII columns

    - id: SEC-013-SEC-02
      description: >
        Every de-tokenise call must produce an audit event visible in the
        central SIEM within 60 seconds.
      measurement: inject a test de-tokenise call; measure SIEM ingestion latency
      threshold: 100% of de-tokenise calls audited within 60s

  availability:
    - id: SEC-013-HA-01
      description: >
        Vault cluster must be active-active across 2 AZs; FPE service must
        handle a single Vault node failure transparently with no request errors.
      measurement: Chaos Engineering drill — kill one Vault node; observe
        FPE service error rate
      threshold: 0% error rate increase during single-node Vault failure
```

## Cost / FinOps

- **HashiCorp Vault licensing**: Vault Enterprise (required for Transform secret engine in production) is priced per cluster node. Budget approximately USD 15,000–25,000/year for a 3-node HA cluster. The Transform engine is included in Vault Enterprise; no separate per-operation charge.
- **HSM hardware**: if using an on-premises HSM (Thales Luna, Utimaco), budget USD 30,000–60,000 capital plus USD 5,000/year maintenance per device. AWS CloudHSM alternative: ~USD 1.60/hour per HSM = ~USD 14,000/year — often more cost-effective for cloud-native deployments.
- **FPE service compute**: the tokenisation service is stateless and horizontally scalable. At 2,000 ops/sec, 2 × t3.medium instances handle the load with headroom. Cost: ~USD 150/month.
- **Storage — token vault mapping**: each token-to-plaintext mapping entry is ~200 bytes. At 50 million unique PII values, the mapping store is approximately 10 GB — negligible storage cost.
- **Cost of non-compliance**: a CCCD or PAN breach triggers Decree 13/2023 penalties (up to 5% of annual revenue in Vietnam) plus mandatory regulator notification. FPE converts a breach of analytics storage into exposure of tokens with no plaintext value.

## Threat Model

STRIDE analysis against the FPE tokenisation pattern:

- **Spoofing — token oracle attack**: an attacker who can submit arbitrary inputs to the tokenise endpoint can build a plaintext-to-token dictionary. Mitigation: the tokenise endpoint is not exposed externally; it is an internal service-to-service API gated by mTLS and service-account ABAC. Rate limiting (100 tokenise ops/sec per service account) constrains dictionary building.
- **Tampering — token substitution in transit**: an attacker modifies a token in a message to redirect a payment to a different account. Mitigation: all inter-service messages are signed (HMAC-SHA256 message integrity per INT-003); token values are part of the signed payload.
- **Repudiation — de-tokenise without audit trail**: an insider calls the de-tokenise API and denies having accessed PII. Mitigation: every de-tokenise call produces an immutable audit event (SIEM, S3 WORM) that includes caller identity, token reference, and timestamp. The de-tokenise endpoint is excluded from local log suppression rules.
- **Information Disclosure — FPE key compromise**: if the AES-FF1 key is leaked, all tokens can be reversed en masse. Mitigation: keys are generated and stored in the HSM; they never leave the HSM boundary in plaintext. Key rotation is automated (quarterly); rotation produces new tokens for all stored values without service disruption (rotation job).
- **Information Disclosure — FPE cryptanalysis (FF3-1 vulnerability)**: NIST SP 800-38G FF3-1 is known to have reduced security margin for certain radix/tweak combinations. Mitigation: Techcombank uses FF1 (not FF3-1) as the primary algorithm; FF3-1 is not deployed. Algorithm choice documented in the crypto register.
- **Denial of Service — Vault outage blocks tokenisation**: if Vault is unavailable, the FPE service cannot tokenise and ingestion pipelines stall. Mitigation: Vault is deployed active-active across 2 AZs; circuit breaker (RES-002) around Vault calls with a `fail-closed` policy (reject ingestion rather than store plaintext).
- **Elevation of Privilege — analytics job accesses de-tokenise API**: an analytics pipeline is modified to call the de-tokenise endpoint and exfiltrate PII. Mitigation: the de-tokenise endpoint's ABAC policy (SEC-010) grants access only to `FRAUD_INVESTIGATOR` and `COMPLIANCE_OFFICER` roles; analytics service accounts hold only `TOKENIZE` permission.

## Operational Runbook

1. **Key rotation (quarterly)**: trigger Vault key rotation via `vault write -f transform/keys/cccd/rotate`. The FPE service auto-detects the new key version on the next tokenise call (Vault handles versioned encryption). Schedule a background job to re-tokenise existing records with the new key version; run during off-peak (02:00–04:00 ICT). Monitor re-tokenisation progress via Prometheus job metric `fpe_retoken_progress_ratio`.
2. **Alert: `fpe_vault_error_rate > 1%`**: check Vault cluster health via `vault operator members`; verify all nodes are unsealed. If a node is sealed, unseal using the Shamir key ceremony (requires 3 of 5 keyholders). Escalate to Platform Engineering if the cluster is degraded; activate the Vault DR replica if the primary cluster is unavailable.
3. **Alert: `fpe_tokenize_p95_latency > 15ms`**: check Vault performance metrics in Grafana `SEC-013` panel; look for HSM saturation (HSM operations queue depth). If HSM is saturated, scale the HSM cluster (add a node) or reduce non-critical tokenisation traffic. If Vault CPU is the bottleneck, scale Vault pods.
4. **De-tokenise access review (monthly)**: export de-tokenise audit events from SIEM for the prior month; review for anomalous patterns (high volume from a single user, off-hours access, access to PII classes outside the user's documented business need). Escalate anomalies to CISO.
5. **New PII class onboarding**: to tokenise a new PII class, (a) define the Vault transformation and template, (b) update `PiiClass` enum, (c) add the field to the `@FpeToken` annotation registry, (d) run integration tests, (e) perform a data migration to tokenise existing plaintext values, (f) update `governance/standards/data-classification.md`.
6. **Incident: suspected token leak**: if token values are observed in public channels (paste sites, logs), assess the blast radius — tokens without the FPE key are cryptographically opaque. Rotate the affected key immediately. Notify the CISO; open a P1 incident. Tokens generated with the old key are invalidated after rotation.
7. **T24 OFS integration verification**: after any T24 upgrade, run `T24FpeIntegrationIT` to confirm that OFS fields carry tokens (not plaintext) and that T24 processes them correctly within its format constraints. T24 field length validation is the most common integration failure.

## Test Strategy

### Unit Tests
- `FpeTokenizationServiceTest`: mock `VaultTemplate`; assert tokenise returns a value of the same length and character class as the input for each `PiiClass`; assert detokenize is the inverse of tokenize; assert `+84` prefix is preserved for `PHONE_INTL`.
- `FpeTokenizationAspectTest`: apply aspect to a test DTO with `@FpeToken` fields; assert all annotated fields are replaced with tokens of matching format.
- `NormalizeTest`: edge cases — empty string, null, single-character, maximum field length.

### Integration Tests
- `FpeVaultIntegrationIT`: use a Vault dev-mode Testcontainer; configure Transform engine; run tokenise + detokenise round-trip for all `PiiClass` values; assert plaintext is recovered exactly.
- `DetokenizationControllerIT`: call `/internal/v1/detoken` with a `FRAUD_INVESTIGATOR` JWT; assert 200 with plaintext. Call with a `CUSTOMER_SUPPORT` JWT; assert 403. Call with an expired JWT; assert 401.
- `T24FpeIntegrationIT`: simulate OFS message with tokenised CCCD; assert T24 gateway accepts the token (correct format) and returns a valid OFS response.

### Compliance Tests
- `NoPiiInAnalyticsDbTest`: query the analytics database for any CCCD, phone, or PAN columns; assert all values match the token pattern (digits only, correct length) and do not match known plaintext PII patterns.
- `AuditTrailCompletenessTest`: invoke detokenize 100 times; query SIEM audit index; assert 100 corresponding audit events are present within 60 seconds.

### Chaos Tests
- Kill one Vault node during a tokenise load test; assert FPE service error rate remains 0% (Vault HA takes over) and P95 latency stays below 15ms.
- Inject a Vault timeout (500ms artificial delay); assert circuit breaker (RES-002) opens and the ingestion pipeline fails-closed (rejects the record, does not store plaintext).

## References

- NIST SP 800-38G — Recommendation for Block Cipher Modes of Operation: Format-Preserving Encryption (FF1 and FF3-1)
- HashiCorp Vault — Transform Secret Engine documentation
- PCI-DSS v4.0 §3 — Protection of Stored Account Data
- Decree 13/2023/ND-CP — Personal Data Protection (Vietnam)
- SBV Circular 09/2020/TT-NHNN §III — Cryptographic Controls
- [SEC-008 Data Masking](data-masking.md) — irreversible masking complement
- [SEC-004 Tokenization + HSM](tokenization-hsm.md) — random tokenisation baseline
- [SEC-010 ABAC](../security/attribute-based-access-control.md) — gates de-tokenise access
- [RES-002 Circuit Breaker](../resilience/circuit-breaker.md) — protects against Vault outage
- [INT-005 ACL / T24 Gateway](../../patterns/integration/t24-acl-gateway.md) — FPE applied before T24 OFS write

---

**Key Takeaway**: Format-preserving encryption (AES-FF1) lets every downstream system keep its existing schema while all PII at rest is cryptographically protected and reversible only through an HSM-gated, audit-logged de-tokenisation path.
