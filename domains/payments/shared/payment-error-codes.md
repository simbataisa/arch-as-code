# Payment Error Codes

Standard error taxonomy for the Payments domain. All payment errors must use these codes for consistent handling, monitoring, and customer communication.

---

## Error Code Format

```
PAY-XXX
```

- **PAY** — Domain prefix (Payments)
- **XXX** — Three-digit error code (001-999)

Example: `PAY-001` = Invalid account number

---

## Error Codes Reference

### 001-010: Input Validation Errors

| Code | HTTP Status | Description | Retry Policy | Customer Message |
|------|-------------|-------------|--------------|------------------|
| **PAY-001** | 400 | Invalid account number format | No | "Please check the beneficiary account number." |
| **PAY-002** | 400 | Invalid IFSC/Bank code | No | "Bank code is invalid. Please verify." |
| **PAY-003** | 400 | Invalid amount (zero or negative) | No | "Please enter a valid amount." |
| **PAY-004** | 400 | Missing required field | No | "Required field is missing." |
| **PAY-005** | 400 | Invalid currency code | No | "Currency not supported." |
| **PAY-006** | 400 | Amount exceeds maximum limit | No | "Amount exceeds transaction limit." |
| **PAY-007** | 400 | Amount below minimum limit | No | "Amount is below minimum required." |
| **PAY-008** | 400 | Invalid beneficiary name | No | "Beneficiary name contains invalid characters." |
| **PAY-009** | 400 | Duplicate transaction (within 5 minutes) | No | "This payment was recently submitted. Please try again later." |
| **PAY-010** | 400 | Invalid payment reference | No | "Payment reference contains invalid characters." |

---

### 011-020: Authentication and Authorization Errors

| Code | HTTP Status | Description | Retry Policy | Customer Message |
|------|-------------|-------------|--------------|------------------|
| **PAY-011** | 401 | Invalid authentication token | No | "Session expired. Please login again." |
| **PAY-012** | 403 | Insufficient permissions | No | "You don't have permission to perform this action." |
| **PAY-013** | 403 | Transaction limit exceeded | No | "Transaction exceeds your daily limit." |
| **PAY-014** | 403 | Unauthorized payment channel | No | "Payment not allowed from this channel." |
| **PAY-015** | 403 | Device not registered | No | "Please register your device first." |

---

### 021-030: Account and Customer Errors

| Code | HTTP Status | Description | Retry Policy | Customer Message |
|------|-------------|-------------|--------------|------------------|
| **PAY-021** | 404 | Payer account not found | No | "Your account could not be found." |
| **PAY-022** | 404 | Beneficiary account not found | No | "Beneficiary account not found." |
| **PAY-023** | 400 | Payer account is inactive | No | "Your account is inactive. Please contact support." |
| **PAY-024** | 400 | Beneficiary account is inactive | No | "Beneficiary account is inactive." |
| **PAY-025** | 400 | Insufficient balance | No | "Insufficient funds in your account." |
| **PAY-026** | 400 | Account blocked or frozen | No | "Your account is temporarily blocked. Contact support." |
| **PAY-027** | 400 | Same account transfer not allowed | No | "Cannot transfer to the same account." |
| **PAY-028** | 400 | Account type not eligible | No | "This account type cannot initiate payments." |

---

### 031-040: Fraud and Risk Errors

| Code | HTTP Status | Description | Retry Policy | Customer Message |
|------|-------------|-------------|--------------|------------------|
| **PAY-031** | 403 | Fraud detected - blocked | No | "Transaction blocked for security reasons. Please contact support." |
| **PAY-032** | 403 | Risk score too high | No | "Transaction requires additional verification." |
| **PAY-033** | 403 | Beneficiary blacklisted | No | "This beneficiary cannot receive payments at this time." |
| **PAY-034** | 403 | AML screening failed | No | "Transaction blocked due to compliance checks." |
| **PAY-035** | 403 | KYC verification required | No | "Please complete KYC verification to continue." |
| **PAY-036** | 429 | Too many failed attempts | No | "Too many failed attempts. Try again later." |
| **PAY-037** | 403 | Unusual transaction pattern | No | "Transaction requires additional verification." |

---

### 041-050: Payment Processing Errors

| Code | HTTP Status | Description | Retry Policy | Customer Message |
|------|-------------|-------------|--------------|------------------|
| **PAY-041** | 503 | Payment gateway unavailable | Exponential | "Temporarily unable to process. Please try again." |
| **PAY-042** | 503 | Payment network timeout | Exponential | "Network busy. Please try again." |
| **PAY-043** | 500 | Payment processing failed | Exponential | "Payment processing failed. Retrying..." |
| **PAY-044** | 400 | Payment already processed | No | "This payment has already been processed." |
| **PAY-045** | 503 | Routing service unavailable | Exponential | "Cannot route payment. Please try again." |
| **PAY-046** | 400 | Invalid routing path | No | "Payment cannot be routed through the requested network." |
| **PAY-047** | 500 | Fee calculation error | Linear | "Fee calculation failed. Please try again." |
| **PAY-048** | 503 | Settlement system unavailable | Exponential | "Settlement system is temporarily unavailable." |

---

### 051-060: External Network Errors

| Code | HTTP Status | Description | Retry Policy | Customer Message |
|------|-------------|-------------|--------------|------------------|
| **PAY-051** | 503 | NAPAS network unavailable | Exponential | "NAPAS network temporarily unavailable." |
| **PAY-052** | 503 | SWIFT network unavailable | Exponential | "SWIFT network temporarily unavailable." |
| **PAY-053** | 503 | VietQR network unavailable | Exponential | "QR network temporarily unavailable." |
| **PAY-054** | 402 | Insufficient liquidity at destination bank | No | "Destination bank has insufficient liquidity." |
| **PAY-055** | 400 | Correspondent bank error | No | "Payment cannot be routed to destination." |
| **PAY-056** | 503 | PSP (Stripe/PayPal) unavailable | Exponential | "Payment provider temporarily unavailable." |
| **PAY-057** | 400 | Network rejected payment | No | "Payment network rejected this transaction." |
| **PAY-058** | 503 | Settlement delay expected | No | "Settlement may be delayed due to network congestion." |

---

### 061-070: Reconciliation Errors

| Code | HTTP Status | Description | Retry Policy | Customer Message |
|------|-------------|-------------|--------------|------------------|
| **PAY-061** | 500 | Reconciliation mismatch | No | "Internal accounting error detected. Under investigation." |
| **PAY-062** | 500 | GL posting failed | Linear | "Accounting entry failed. Retrying..." |
| **PAY-063** | 404 | Transaction record not found | No | "Transaction record not found in system." |
| **PAY-064** | 400 | Duplicate settlement detected | No | "Duplicate settlement detected. Investigating." |
| **PAY-065** | 500 | Reversal processing failed | Linear | "Payment reversal failed. Please contact support." |

---

### 071-080: System and Integration Errors

| Code | HTTP Status | Description | Retry Policy | Customer Message |
|------|-------------|-------------|--------------|------------------|
| **PAY-071** | 500 | Database connection error | Exponential | "System error. Please try again." |
| **PAY-072** | 500 | Service unavailable | Exponential | "Service temporarily unavailable." |
| **PAY-073** | 500 | Internal server error | Exponential | "An unexpected error occurred." |
| **PAY-074** | 503 | Core Banking (T24) unavailable | Exponential | "Core system temporarily unavailable." |
| **PAY-075** | 503 | Risk Management service unavailable | Exponential | "Risk check service unavailable." |
| **PAY-076** | 503 | Data Platform service unavailable | Exponential | "Analytics service unavailable." |
| **PAY-077** | 500 | Event publishing failed | Linear | "Notification system error. Retrying..." |
| **PAY-078** | 504 | Request timeout | Exponential | "Request timed out. Please try again." |

---

### 081-090: Regulatory and Compliance Errors

| Code | HTTP Status | Description | Retry Policy | Customer Message |
|------|-------------|-------------|--------------|------------------|
| **PAY-081** | 403 | Sanctions list match | No | "Transaction blocked due to regulatory compliance." |
| **PAY-082** | 403 | Large transaction reporting required | No | "Large transaction requires additional verification." |
| **PAY-083** | 403 | Cross-border restriction | No | "Cross-border payment not allowed from your account." |
| **PAY-084** | 400 | Invalid SWIFT purpose code | No | "Invalid payment purpose code." |
| **PAY-085** | 403 | SBV Circular 64 violation | No | "Payment violates regulatory requirements." |

---

### 091-100: Notification and Communication Errors

| Code | HTTP Status | Description | Retry Policy | Customer Message |
|------|-------------|-------------|--------------|------------------|
| **PAY-091** | 500 | Notification delivery failed | Linear | "Payment confirmation could not be delivered." |
| **PAY-092** | 400 | Invalid phone number | No | "Please update your phone number." |
| **PAY-093** | 400 | Invalid email address | No | "Please update your email address." |
| **PAY-094** | 503 | SMS gateway unavailable | Exponential | "SMS service temporarily unavailable." |
| **PAY-095** | 503 | Email service unavailable | Exponential | "Email service temporarily unavailable." |

---

## Retry Policies

### No Retry
Do not automatically retry. Return error to customer immediately.
- Validation errors
- Authorization errors
- Fraud blocks

### Exponential Backoff
Retry with exponentially increasing delays: 1s, 2s, 4s, 8s, 16s (max 5 retries)
- Network timeouts
- Service unavailability
- Database connection errors
- Maximum total wait: ~31 seconds

### Linear Backoff
Retry with constant delay: 2s between each retry (max 3 retries)
- Fee calculation errors
- GL posting failures
- Notification delivery

---

## Error Response Format

All error responses must follow this standard format:

```json
{
  "error": {
    "code": "PAY-025",
    "message": "Insufficient funds in your account.",
    "customer_message": "Insufficient funds in your account.",
    "technical_detail": "Account 1234567890 has balance VND 500,000 but withdrawal of VND 1,000,000 was requested.",
    "timestamp": "2026-03-08T10:30:45Z",
    "request_id": "req_abc123xyz789",
    "retryable": false
  }
}
```

| Field | Description |
|-------|-------------|
| **code** | Error code (e.g., PAY-025) |
| **message** | Technical message for developers |
| **customer_message** | Message to display to end customers |
| **technical_detail** | Additional debugging information (for support logs) |
| **timestamp** | When error occurred (ISO 8601) |
| **request_id** | Unique request identifier for tracing |
| **retryable** | Whether client can safely retry |

---

## Monitoring and Alerting

Error codes are automatically monitored for trends:

- **Critical Alerts** — PAY-041 through PAY-048 (payment processing errors)
- **High Priority** — PAY-031 through PAY-037 (fraud/risk blocks)
- **Medium Priority** — PAY-021 through PAY-030 (account errors)
- **Info** — PAY-001 through PAY-010 (validation errors, usually customer mistakes)

Thresholds:
- **Critical errors > 1% of volume** → Page on-call engineer
- **High priority errors > 0.5% of volume** → Create incident
- **Service unavailability (PAY-041-060) > 15 minutes** → Declare SEV-1 incident

---

## Integration with Observability

All payment errors are logged with:
- Error code
- Request ID (for tracing)
- Affected customer ID
- Amount and direction
- Timestamp
- Service that generated error
- Retry status

Logs are indexed in DataDog and searchable by error code:
```
datadog: service:payments-api AND error_code:PAY-025
```

---

## See Also

- [Payment Glossary](./payment-glossary.md)
- [Techcombank Error Handling Guide](https://confluence.techcombank.io/error-handling)
- [SRE Runbooks](https://runbook.techcombank.io/payments)

---

Last Updated: March 8, 2026 | Domain: Payments
