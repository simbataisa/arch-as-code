# HashiCorp Vault for Secrets Management Pattern

Status: Approved | Last Reviewed: 2026-02-22 | Owner: @ea-board
Catalog ID: SEC-003 | Radii
Tier Applicability: T0, T1, T2

## Problem Statement

Managing secrets (passwords, API keys, certificates) is error-prone:
- Hardcoded secrets in source code (exposed in Git history)
- Secrets in config files (exposed if file leaked)
- No audit trail of who accessed what secret
- Cannot rotate secrets without redeployment
- Secret sharing between services is insecure
- No centralized control or revocation

## Solution

Use HashiCorp Vault for centralized secret management. Single source of truth with access control, audit logging, and automatic rotation.

```
┌──────────────┐
│ Application  │
├──────────────┤
│ Requests DB  │
│ Credentials  │
└────────┬─────┘
         │
         ↓ (mTLS)
    ┌─────────────────────┐
    │ HashiCorp Vault     │
    ├─────────────────────┤
    │ ✓ Encrypt secrets   │
    │ ✓ Audit access      │
    │ ✓ Rotate credentials│
    │ ✓ Generate tokens   │
    └─────────────────────┘
         │
         ↓ (Dynamic secret generation)
    ┌─────────────────┐
    │ PostgreSQL      │
    │ (temporary user │
    │  created by     │
    │  Vault)         │
    └─────────────────┘
```

## Implementation Guidelines

1. **Install Vault**
   ```bash
   # Download and install
   wget https://releases.hashicorp.com/vault/1.15.0/vault_1.15.0_linux_amd64.zip
   unzip vault_1.15.0_linux_amd64.zip
   sudo mv vault /usr/local/bin/

   # Start dev server (not for production)
   vault server -dev

   # For production, use Kubernetes or managed service
   # AWS: AWS Secrets Manager (alternative)
   # GCP: Secret Manager (alternative)
   ```

2. **Initialize and Unseal Vault**
   ```bash
   # Initialize (generates keys)
   vault operator init \
     -key-shares=5 \
     -key-threshold=3

   # Output:
   # Unseal Key 1: ...
   # Unseal Key 2: ...
   # ...
   # Root Token: hvs.xxxxx

   # Unseal with 3 of 5 keys
   vault operator unseal <key1>
   vault operator unseal <key2>
   vault operator unseal <key3>

   # Check status
   vault status
   ```

3. **Configure Secret Engines**
   ```bash
   # Enable KV v2 secrets engine
   vault secrets enable -path=secret kv-v2

   # Enable database secrets engine (dynamic credentials)
   vault secrets enable database

   # Enable transit engine (encryption as a service)
   vault secrets enable transit
   ```

4. **Store Static Secrets** (KV Engine)
   ```bash
   # Write a secret
   vault kv put secret/database/postgres \
     username="admin" \
     password="SecurePassword123!" \
     host="postgres-prod.internal" \
     port="5432" \
     database="orders"

   # Read a secret
   vault kv get secret/database/postgres

   # Output:
   # ===== Secret Path =====
   # secret/data/database/postgres
   # ===== Data =====
   # Key        Value
   # ---        -----
   # database   orders
   # host       postgres-prod.internal
   # password   SecurePassword123!
   # port       5432
   # username   admin

   # List secrets
   vault kv list secret/database/
   ```

5. **Dynamic Database Credentials**
   ```bash
   # Configure database connection
   vault write database/config/postgres \
     plugin_name=postgresql-database-plugin \
     allowed_roles="order-service,payment-service" \
     connection_url="postgresql://{{username}}:{{password}}@postgres-prod.internal:5432/postgres" \
     username="vault_admin" \
     password="VaultAdminPassword123!"

   # Create role (order-service gets temporary credentials)
   vault write database/roles/order-service \
     db_name=postgres \
     creation_statements="CREATE USER \"{{name}}\" WITH PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
       GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO \"{{name}}\"; \
       GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO \"{{name}}\";" \
     default_ttl="1h" \
     max_ttl="24h"

   # Generate temporary credentials (automatic)
   vault read database/creds/order-service

   # Output:
   # Key                Value
   # ---                -----
   # lease_duration     3600s
   # lease_id           database/creds/order-service/...
   # password           A1a-2b3c4d5e6f7g8h9i0j
   # username           v-root-order-service-...
   ```

6. **Java Application Integration**
   ```java
   @Configuration
   public class VaultConfig {

     @Value("${spring.cloud.vault.host}")
     private String vaultHost;

     @Value("${spring.cloud.vault.port}")
     private int vaultPort;

     @Value("${spring.cloud.vault.token}")
     private String vaultToken;

     @Bean
     public VaultTemplate vaultTemplate() {
       ClientAuthentication auth = new TokenAuthentication(vaultToken);
       VaultEndpoint endpoint = new VaultEndpoint();
       endpoint.setHost(vaultHost);
       endpoint.setPort(vaultPort);
       endpoint.setScheme("https");

       RestOperations restOperations = new RestTemplate();
       return new VaultTemplate(endpoint, restOperations, auth);
     }

     // Access secrets
     @Bean
     public DataSource dataSource(VaultTemplate vaultTemplate) {
       VaultResponseSupport<Map<String, Object>> vaultResponse = vaultTemplate
         .read("secret/data/database/postgres");

       Map<String, Object> data = vaultResponse.getData().get("data");
       String username = (String) data.get("username");
       String password = (String) data.get("password");
       String host = (String) data.get("host");
       String database = (String) data.get("database");

       return DataSourceBuilder.create()
         .driverClassName("org.postgresql.Driver")
         .url(String.format("jdbc:postgresql://%s:5432/%s", host, database))
         .username(username)
         .password(password)
         .build();
     }
   }
   ```

7. **Spring Cloud Config with Vault**
   ```yaml
   # application.yml
   spring:
     cloud:
       vault:
         host: vault.internal
         port: 8200
         scheme: https
         authentication: TOKEN
         token: ${VAULT_TOKEN}  # From environment or Kubernetes
         kv-version: 2

       config:
         import: vault://secret/application/

   # Vault secrets
   # secret/data/application/orders:
   #   spring.datasource.url: jdbc:postgresql://...
   #   spring.datasource.username: vault{database/creds/order-service/username}
   #   spring.datasource.password: vault{database/creds/order-service/password}
   ```

8. **Kubernetes Auth** (Automatic pod authentication)
   ```bash
   # Enable Kubernetes authentication
   vault auth enable kubernetes

   # Configure with cluster details
   vault write auth/kubernetes/config \
     kubernetes_host=https://kubernetes.default.svc \
     kubernetes_ca_cert=@/var/run/secrets/kubernetes.io/serviceaccount/ca.crt \
     token_reviewer_jwt=@/var/run/secrets/kubernetes.io/serviceaccount/token

   # Create policy for order-service
   vault policy write order-service - <<EOF
   path "secret/data/database/*" {
     capabilities = ["read"]
   }
   path "database/creds/order-service" {
     capabilities = ["read"]
   }
   EOF

   # Create role for order-service namespace/serviceaccount
   vault write auth/kubernetes/role/order-service \
     bound_service_account_names=order-service \
     bound_service_account_namespaces=default \
     policies=order-service \
     ttl=24h
   ```

   **Kubernetes Deployment with Vault**
   ```yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     name: order-service
   spec:
     template:
       spec:
         serviceAccountName: order-service
         containers:
         - name: order-service
           image: order-service:1.0.0
           env:
           - name: VAULT_ADDR
             value: "https://vault.internal:8200"
           - name: VAULT_SKIP_VERIFY
             value: "false"
           volumeMounts:
           - name: vault-token
             mountPath: /var/run/secrets/vault
         initContainers:
         - name: vault-init
           image: vault:latest
           env:
           - name: VAULT_ADDR
             value: "https://vault.internal:8200"
           command:
           - sh
           - -c
           - |
             KUBE_TOKEN=$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)
             VAULT_TOKEN=$(vault write -field=token auth/kubernetes/login role=order-service jwt=$KUBE_TOKEN)
             echo $VAULT_TOKEN > /var/run/secrets/vault/token
           volumeMounts:
           - name: vault-token
             mountPath: /var/run/secrets/vault
         volumes:
         - name: vault-token
           emptyDir: {}
   ```

9. **Secret Rotation**
   ```bash
   # Vault automatically rotates database credentials
   # TTL = time to live before credential expires

   # Configure rotation policy
   vault write database/roles/order-service \
     default_ttl="1h" \
     max_ttl="24h"

   # Application must handle credential refresh:
   # 1. Application requests credential 1hr before TTL
   # 2. Gets new temporary credential from Vault
   # 3. Closes old DB connection, opens new one
   # 4. Old credential expires automatically
   ```

10. **Transit Engine** (Encryption as a Service)
    ```bash
    # Enable transit secrets engine
    vault secrets enable transit

    # Create encryption key
    vault write -f transit/keys/application

    # Encrypt data
    vault write transit/encrypt/application plaintext=$(base64 <<< "sensitive data")

    # Decrypt data
    vault write transit/decrypt/application ciphertext="vault:v1:8SDd3DBxQd..."
    ```

## Audit Logging

```bash
# Enable file audit logging
vault audit enable file file_path=/var/log/vault/audit.log

# View audit logs
cat /var/log/vault/audit.log | jq

# Output shows who accessed what and when:
# {
#   "time": "2026-03-08T10:30:00Z",
#   "type": "request",
#   "auth": {
#     "client_token": "hvs.xxxxx",
#     "accessor": "k8s_1234"
#   },
#   "request": {
#     "operation": "read",
#     "path": "secret/data/database/postgres"
#   }
# }
```

## Best Practices

- **Never hardcode Vault tokens**: Use Kubernetes auth or AppRole
- **Short TTLs**: Set secret TTL to minimum viable duration
- **Rotation**: Rotate database credentials automatically
- **Encryption in transit**: Always use HTTPS/mTLS to Vault
- **Audit everything**: Enable audit logging for compliance

## When to Use

- Database credentials (use dynamic secrets)
- API keys and tokens
- TLS certificates
- Encryption keys
- Regulatory compliance (audit trail required)

## When NOT to Use

- Application-level secrets (use config management)
- Non-sensitive configuration (use ConfigMap)

## Cloud Alternatives

| Cloud | Alternative |
|-------|---|
| **AWS** | AWS Secrets Manager |
| **GCP** | Secret Manager |
| **Azure** | Key Vault |

## References

- [HashiCorp Vault](https://www.vaultproject.io/)
- [Vault Documentation](https://www.vaultproject.io/docs)
- [Dynamic Database Credentials](https://www.vaultproject.io/docs/secrets/databases)
- [Kubernetes Auth](https://www.vaultproject.io/docs/auth/kubernetes)

---

**Key Takeaway**: Use Vault for centralized secret management. Leverage dynamic credentials for databases, mTLS for Vault communication, Kubernetes auth for pods, and audit logging for compliance.
