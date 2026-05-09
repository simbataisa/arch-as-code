# API Standards for DAB Submissions

Technical standards for all APIs defined in Design Approval Board submissions. Covers OpenAPI specifications, versioning, naming conventions, error handling, authentication, and documentation requirements.

---

## API Specification Standard

### OpenAPI Version
- **Minimum required:** OpenAPI 3.0.x
- **Recommended:** OpenAPI 3.1.x
- **Format:** YAML or JSON (YAML preferred for readability)

### Basic API Definition

All APIs must include complete OpenAPI specification following this structure:

```yaml
openapi: 3.1.0
info:
  title: "Payment Service API"
  version: "1.0.0"
  description: |
    API for initiating and managing payment transfers.
    Supports domestic and international transfers.
  contact:
    name: "Payment Architecture Team"
    email: "payments@techcombank.local"
  license:
    name: "Apache 2.0"

servers:
  - url: "https://api.techcombank.local/v1"
    description: "Production API"
  - url: "https://staging-api.techcombank.local/v1"
    description: "Staging API"
  - url: "http://localhost:8080/v1"
    description: "Local Development"

paths:
  /transfers:
    post:
      operationId: "createTransfer"
      summary: "Initiate a payment transfer"
      description: "Creates a new payment transfer between accounts"
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/CreateTransferRequest"
      responses:
        '201':
          description: "Transfer created successfully"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/TransferResponse"
        '400':
          description: "Invalid request"
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/ErrorResponse"
        '401':
          description: "Unauthorized"
        '429':
          description: "Rate limit exceeded"
      security:
        - oauth2:
            - "transfers:write"

components:
  schemas:
    CreateTransferRequest:
      type: "object"
      required:
        - "amount"
        - "currency"
        - "destinationAccount"
      properties:
        amount:
          type: "number"
          format: "double"
          description: "Transfer amount"
          example: 1000.50
        currency:
          type: "string"
          pattern: "^[A-Z]{3}$"
          description: "ISO 4217 currency code"
          example: "VND"
    ErrorResponse:
      type: "object"
      required:
        - "error"
        - "message"
      properties:
        error:
          type: "string"
          enum:
            - "INVALID_REQUEST"
            - "UNAUTHORIZED"
            - "FORBIDDEN"
            - "NOT_FOUND"
            - "RATE_LIMIT_EXCEEDED"
            - "INTERNAL_SERVER_ERROR"
        message:
          type: "string"
          description: "Human-readable error message"
        requestId:
          type: "string"
          description: "Unique request ID for debugging"
          example: "req-20260308-001"

  securitySchemes:
    oauth2:
      type: "oauth2"
      flows:
        clientCredentials:
          tokenUrl: "https://auth.techcombank.local/oauth2/token"
          scopes:
            "transfers:read": "Read transfer data"
            "transfers:write": "Create and manage transfers"
```

---

## Versioning Strategy

### URL Path Versioning (Required)

All APIs must use URL path versioning:

```
GET /v1/transfers
GET /v2/transfers          (if new version released)
```

**Not acceptable:**
- Query parameter versioning: `GET /transfers?version=1`
- Header versioning: `X-API-Version: 1`
- Accept header versioning: `Accept: application/vnd.techcombank.v1+json`

### Version Lifecycle

- **v1** — Initial production release
- **v2** — Released only if breaking changes unavoidable
- **Support window** — At least 12 months for previous major version
- **Deprecation notice** — 6 months prior notice before sunset

**Rationale:** URL path versioning is explicit, discoverable, and prevents client confusion.

### Breaking Changes Definition

Changes that require version bump:
- Removing an endpoint
- Removing a required parameter
- Changing parameter name
- Changing response schema (removing field, changing type)
- Changing HTTP status code meaning

Changes that do NOT require version bump:
- Adding optional parameters
- Adding new fields to response (with backward-compatible defaults)
- Adding new endpoints
- Fixing bugs that broke API contract
- Improving documentation

---

## JSON Naming Conventions

### Request/Response Field Names: camelCase

All JSON fields in request/response bodies use `camelCase`:

```json
{
  "transferId": "trn-20260308-001",
  "sourceAccountId": "acc-12345",
  "destinationAccountId": "acc-67890",
  "transferAmount": 1000.50,
  "transferCurrency": "VND",
  "createdAt": "2026-03-08T10:30:00Z",
  "transferStatus": "PENDING"
}
```

**Not acceptable:**
```json
{
  "transfer_id": "...",         ❌ snake_case
  "TransferId": "...",          ❌ PascalCase
  "transfer-id": "...",         ❌ kebab-case
}
```

### URL Path Segments: kebab-case

URL paths use `kebab-case` for clarity and REST conventions:

```
POST /v1/transfers
GET /v1/transfers/{transfer-id}
POST /v1/transfers/{transfer-id}/approve
GET /v1/customers/{customer-id}/accounts/{account-id}
```

**Not acceptable:**
```
GET /v1/transferS                    ❌ Inconsistent pluralization
GET /v1/Transfers                    ❌ Capitalized path
POST /v1/transfers/confirm           ❌ Not a sub-resource
```

### Resource Names: Plural, lowercase

REST resources are plural, lowercase nouns:

```
/v1/transfers          (not /transfer, /Transfers, /Transfers)
/v1/accounts           (not /account, /accounts)
/v1/customers          (not /customer, /Customers)
/v1/merchants          (not /merchant, /Merchants)
```

### Query Parameters: camelCase

Query parameters follow camelCase convention:

```
GET /v1/transfers?customerId=cus-123
GET /v1/accounts?sortBy=createdAt&sortOrder=DESC
GET /v1/transfers?pageSize=20&pageNumber=2
GET /v1/transfers?filterStatus=PENDING&filterCurrency=VND
```

---

## HTTP Methods & Status Codes

### Standard HTTP Methods

| Method | Semantics | Example |
|--------|-----------|---------|
| **GET** | Retrieve resource, no side effects | `GET /v1/transfers/{id}` |
| **POST** | Create new resource or execute action | `POST /v1/transfers` |
| **PUT** | Replace entire resource (idempotent) | `PUT /v1/transfers/{id}` |
| **PATCH** | Partial update (idempotent) | `PATCH /v1/transfers/{id}` |
| **DELETE** | Remove resource (idempotent) | `DELETE /v1/transfers/{id}` |

### Status Codes

**Success Codes:**
| Code | Meaning | Use Case |
|------|---------|----------|
| **200** | OK | Successful GET, POST (response with body) |
| **201** | Created | Resource successfully created (POST) |
| **202** | Accepted | Long-running operation accepted (async) |
| **204** | No Content | Successful DELETE or operation with no response body |

**Client Error Codes:**
| Code | Meaning | Use Case |
|------|---------|----------|
| **400** | Bad Request | Invalid input, malformed JSON, missing required fields |
| **401** | Unauthorized | Missing or invalid authentication token |
| **403** | Forbidden | Authenticated but insufficient permissions |
| **404** | Not Found | Resource doesn't exist |
| **409** | Conflict | Request conflicts with resource state (e.g., duplicate payment) |
| **429** | Too Many Requests | Rate limit exceeded |

**Server Error Codes:**
| Code | Meaning | Use Case |
|------|---------|----------|
| **500** | Internal Server Error | Unexpected server error |
| **503** | Service Unavailable | Service temporarily down or in maintenance |

---

## Required API Metadata

### Info Section (Required)
```yaml
info:
  title: "Payment Service API"
  version: "1.0.0"                           # Semantic versioning
  description: |
    Comprehensive description of API purpose and capabilities.
    Support multiple paragraphs.
  contact:
    name: "API Support Team"
    email: "api-support@techcombank.local"
    url: "https://wiki.techcombank.local/api-support"
  license:
    name: "Apache 2.0"
    url: "https://opensource.org/licenses/Apache-2.0"
```

### Servers Section (Required)
Define at least production and development servers:

```yaml
servers:
  - url: "https://api.techcombank.local/v1"
    description: "Production"
    variables:
      environment:
        default: "prod"
  - url: "https://staging-api.techcombank.local/v1"
    description: "Staging"
  - url: "http://localhost:8080/v1"
    description: "Development"
```

### Tags & Organization (Recommended)
Group related endpoints:

```yaml
paths:
  /transfers:
    post:
      tags:
        - "Transfers"
      summary: "Create transfer"
  /transfers/{id}:
    get:
      tags:
        - "Transfers"
      summary: "Get transfer details"
  /accounts:
    get:
      tags:
        - "Accounts"
      summary: "List accounts"
```

---

## Authentication & Security

### Required Authentication Schemes

All APIs must implement one of:

#### 1. OAuth 2.0 (Recommended)
```yaml
components:
  securitySchemes:
    oauth2:
      type: "oauth2"
      flows:
        clientCredentials:
          tokenUrl: "https://auth.techcombank.local/oauth2/token"
          scopes:
            "transfers:read": "Read transfer data"
            "transfers:write": "Initiate transfers"
            "accounts:read": "Read account information"
        authorizationCode:
          authorizationUrl: "https://auth.techcombank.local/oauth2/authorize"
          tokenUrl: "https://auth.techcombank.local/oauth2/token"
          scopes:
            "profile": "User profile data"
            "accounts:read": "Read account information"
```

**Usage in endpoint:**
```yaml
paths:
  /transfers:
    post:
      security:
        - oauth2: ["transfers:write"]
      ...
```

#### 2. OpenID Connect (OIDC)
```yaml
components:
  securitySchemes:
    oidc:
      type: "openIdConnect"
      openIdConnectUrl: "https://auth.techcombank.local/.well-known/openid-configuration"
```

#### 3. API Key (Only for Internal/Low-Risk)
```yaml
components:
  securitySchemes:
    apiKey:
      type: "apiKey"
      in: "header"
      name: "X-API-Key"
```

**NOT recommended for customer-facing APIs; use OAuth2 instead.**

### Required Security Attributes

All APIs must specify in documentation:

```markdown
## Security Requirements

- **Authentication:** OAuth 2.0 (Client Credentials flow)
- **Token Endpoint:** https://auth.techcombank.local/oauth2/token
- **Scopes Required:** transfers:write, accounts:read
- **Token Lifetime:** 1 hour
- **Token Refresh:** Refresh token valid for 30 days
- **HTTPS:** Required, TLS 1.2 minimum
- **Rate Limiting:** 1000 requests per minute per API key
- **IP Whitelisting:** If applicable
```

---

## Standard Error Response Schema

All APIs must use this error response format (required in DAB security assessment):

```yaml
components:
  schemas:
    ErrorResponse:
      type: "object"
      required:
        - "error"
        - "message"
        - "requestId"
      properties:
        error:
          type: "string"
          enum:
            - "INVALID_REQUEST"
            - "UNAUTHORIZED"
            - "FORBIDDEN"
            - "NOT_FOUND"
            - "CONFLICT"
            - "RATE_LIMIT_EXCEEDED"
            - "INTERNAL_SERVER_ERROR"
            - "SERVICE_UNAVAILABLE"
          description: "Machine-readable error code"
        message:
          type: "string"
          description: "Human-readable error message"
          example: "Invalid amount: must be > 0"
        requestId:
          type: "string"
          pattern: "^[a-zA-Z0-9-]+$"
          description: "Unique request ID for debugging and auditing"
          example: "req-20260308-abc123"
        details:
          type: "object"
          description: "Additional error context (optional)"
          properties:
            field:
              type: "string"
              example: "amount"
            reason:
              type: "string"
              example: "Must be > 0"
        timestamp:
          type: "string"
          format: "date-time"
          description: "When error occurred (ISO 8601)"
          example: "2026-03-08T10:30:00Z"

    # Specific error responses
    ValidationError:
      allOf:
        - $ref: "#/components/schemas/ErrorResponse"
        - type: "object"
          properties:
            validationErrors:
              type: "array"
              items:
                type: "object"
                properties:
                  field:
                    type: "string"
                  message:
                    type: "string"

    RateLimitError:
      allOf:
        - $ref: "#/components/schemas/ErrorResponse"
        - type: "object"
          properties:
            retryAfter:
              type: "integer"
              description: "Seconds to wait before retrying"
```

**Example Error Response:**
```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "API rate limit exceeded. Please retry after 60 seconds.",
  "requestId": "req-20260308-abc123",
  "retryAfter": 60,
  "timestamp": "2026-03-08T10:30:00Z"
}
```

---

## Pagination Standards

For list endpoints, use standard pagination:

```yaml
paths:
  /transfers:
    get:
      parameters:
        - name: "pageNumber"
          in: "query"
          schema:
            type: "integer"
            default: 1
            minimum: 1
          description: "Page number (1-indexed)"
        - name: "pageSize"
          in: "query"
          schema:
            type: "integer"
            default: 20
            minimum: 1
            maximum: 100
          description: "Items per page"
        - name: "sortBy"
          in: "query"
          schema:
            type: "string"
            enum: ["createdAt", "amount", "status"]
          description: "Field to sort by"
        - name: "sortOrder"
          in: "query"
          schema:
            type: "string"
            enum: ["ASC", "DESC"]
            default: "DESC"
      responses:
        '200':
          description: "List of transfers"
          content:
            application/json:
              schema:
                type: "object"
                properties:
                  data:
                    type: "array"
                    items:
                      $ref: "#/components/schemas/Transfer"
                  pagination:
                    type: "object"
                    properties:
                      pageNumber:
                        type: "integer"
                      pageSize:
                        type: "integer"
                      totalCount:
                        type: "integer"
                      totalPages:
                        type: "integer"
```

---

## Rate Limiting

APIs must implement rate limiting and communicate via standard headers:

```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1646821200
```

**Specification:**
```yaml
paths:
  /transfers:
    get:
      responses:
        '200':
          description: "Success"
          headers:
            X-RateLimit-Limit:
              schema:
                type: "integer"
              description: "Requests allowed per time window"
            X-RateLimit-Remaining:
              schema:
                type: "integer"
              description: "Requests remaining in current window"
            X-RateLimit-Reset:
              schema:
                type: "integer"
              description: "Unix timestamp when limit resets"
```

---

## Documentation Requirements

### Required Sections in DAB Section 3 (Detailed Design)

1. **OpenAPI Specification** — Complete specification file
2. **Authentication Flow** — Detailed auth sequence
3. **Rate Limiting Policy** — Limits per client/API key
4. **Error Handling** — All error codes and meanings
5. **Pagination** — List endpoint pagination strategy
6. **Async Operations** — If applicable, webhook/callback patterns
7. **Idempotency** — How duplicate requests are handled
8. **Field Validation** — Rules for input validation
9. **API Examples** — cURL/SDK examples for key flows

---

## Compliance Checklist for DAB Submissions

- [ ] OpenAPI 3.0+ specification provided
- [ ] Version strategy using URL path versioning (/v1/, /v2/)
- [ ] All field names use camelCase (JSON) and kebab-case (URLs)
- [ ] All endpoints define authentication (OAuth2/OIDC required)
- [ ] Standard error response schema used
- [ ] Rate limiting implemented and documented
- [ ] Pagination rules defined for list endpoints
- [ ] HTTP status codes match REST conventions
- [ ] Required API metadata present (title, version, servers, contact)
- [ ] Security assessment includes auth/encryption/logging
- [ ] API documentation examples provided (cURL, SDK)

---

## Common Mistakes to Avoid

1. **No version prefix** — Using `/transfers` instead of `/v1/transfers`
2. **Inconsistent naming** — mix of camelCase, snake_case, PascalCase
3. **Missing authentication** — Endpoints without OAuth2/OIDC
4. **Vague error messages** — "Error" instead of specific codes like "INVALID_AMOUNT"
5. **No rate limiting** — APIs without documented rate limits
6. **Inconsistent pagination** — Different pagination per endpoint
7. **No request ID** — Errors without unique identifier for debugging
8. **Breaking changes without version bump** — Changing behavior without /v2/
9. **Incomplete OpenAPI spec** — Missing descriptions, examples, schemas
10. **No security metadata** — No mention of TLS, authentication, data classification

---

## Tools & Validation

### API Specification Validation
```bash
# Validate OpenAPI spec
npm install -g openapi-enforcer
openapi-enforcer validate openapi.yaml

# Generate docs
npm install -g redoc-cli
redoc-cli build openapi.yaml -o index.html
```

### Mock Server
```bash
# Local mock server for testing
npm install -g @openapitools/openapi-generator-cli
openapi-generator-cli generate -i openapi.yaml -g nodejs-express-server -o ./mock-server
```

---

## Related Documents
- [Security Baseline](./security-baseline.md)
- [Data Classification](./data-classification.md)
- [DAB Full Process — Section 3 (Detailed Design)](../dab-process/dab-full-process.md#section-3-detailed-design)
