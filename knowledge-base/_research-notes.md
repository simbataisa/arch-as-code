# Research Notes — Industry Sources Underpinning the Enterprise Architecture Catalog

Status: Living document | Last Updated: 2026-05-09 | Owner: @ea-board

Purpose: Authoritative quotes, section references, and pattern → catalog-ID mappings extracted from industry sources during Phase 0. All starter-set docs cite this file by anchor; do not rely on LLM memory for any external claim. Sources flagged "**UNOFFICIAL TRANSLATION pending Legal review**" must be replaced with authoritative text before G3 sign-off of the Compliance Mapping Matrix.

> **Phase-0 fetch status (2026-05-09):**
> - ✅ Fetched: EIP, Microsoft Cloud Patterns, Azure WAF, AWS Reliability, AWS DR Strategies, Microservices.io, Resilience4j (Getting Started + CircuitBreaker), BCBS 239, NAPAS
> - ⚠️ Partial: Azure WAF (top-level only), AWS Reliability welcome page (didn't include the recovery-pattern matrix — found in DR strategies whitepaper instead), Resilience4j Getting Started (Spring annotation examples not on intro page), BCBS 230 (only welcome page, full 12-page PDF needed), PCI-DSS (public page didn't expose detail)
> - ❌ Timed out / blocked: ISO 20022 message-definitions index, SWIFT CSP security-controls page, SBV Circular 09/2020 (homepage only), Decree 13/2023 + Decree 53/2022 (pending Legal-team request)
> - **Action**: Open Question Q-research-1 — schedule a librarian to fetch the gapped sources via authoritative copies (BCBS 230 PDF, ISO 20022 catalogue, SWIFT CSP CSCF document, SBV/Decree authoritative translations).

---

## EIP — Enterprise Integration Patterns (Hohpe & Woolf)

Source: https://www.enterpriseintegrationpatterns.com/patterns/messaging/index.html
Reference book: Hohpe, G. & Woolf, B. (2003). *Enterprise Integration Patterns*. Addison-Wesley.
Fetched: 2026-05-09

### All 65 patterns by category

#### Messaging Channels
- Message Channel — Logical conduit through which messages travel
- Point-to-Point Channel — Ensures each message is consumed by exactly one receiver
- Publish-Subscribe Channel — Broadcasts messages to all interested subscribers
- Datatype Channel — Routes messages based on their data type
- Invalid Message Channel — Captures messages that cannot be processed
- Dead Letter Channel — Handles messages that fail delivery
- Guaranteed Delivery — Assures messages are not lost in transit
- Channel Adapter — Bridges messaging system to external applications
- Messaging Bridge — Connects separate messaging systems
- Message Bus — Infrastructure backbone for application communication

#### Message Construction
- Message — Base pattern for message construction
- Command Message — Invokes a procedure in another application
- Document Message — Transmits data between systems
- Event Message — Notifies other applications of state changes
- Request-Reply — Synchronous communication
- Return Address — Routing replies back to original sender
- Correlation Identifier — Match related messages in conversations
- Message Sequence — Ordering related messages
- Message Expiration — How long a message remains valid
- Format Indicator — Metadata identifying message content format

#### Message Routing
- Pipes-and-Filters
- Message Router (base)
- Content-based Router
- Message Filter
- Dynamic Router
- Recipient List
- Splitter
- Aggregator
- Resequencer
- Composed Message Processor
- Scatter-Gather
- Routing Slip
- Process Manager
- Message Broker

#### Message Transformation
- Message Translator
- Envelope Wrapper
- Content Enricher
- Content Filter
- Claim Check
- Normalizer
- Canonical Data Model

#### Messaging Endpoints
- Message Endpoint (base)
- Messaging Gateway
- Messaging Mapper
- Transactional Client
- Polling Consumer
- Event-driven Consumer
- Competing Consumers
- Message Dispatcher
- Selective Consumer
- Durable Subscriber
- **Idempotent Receiver** ← starter-set EIP-024
- Service Activator

#### System Management
- Control Bus
- Detour
- Wire Tap
- Message History
- Message Store
- Smart Proxy
- Test Message
- Channel Purger

### Banking-relevant subset (25 of 65) — Catalog IDs

| # | Pattern | Catalog ID |
| --- | --- | --- |
| 1 | Message Channel | EIP-001 |
| 2 | Point-to-Point Channel | EIP-002 |
| 3 | Publish-Subscribe Channel | EIP-003 |
| 4 | Message Router (base) | EIP-004 |
| 5 | Content-Based Router | EIP-005 |
| 6 | Message Translator | EIP-006 |
| 7 | Content Enricher | EIP-007 |
| 8 | Content Filter | EIP-008 |
| 9 | Claim Check | EIP-009 |
| 10 | Normalizer | EIP-010 |
| 11 | Aggregator | EIP-011 |
| 12 | Splitter | EIP-012 |
| 13 | Resequencer | EIP-013 |
| 14 | Composed Message Processor | EIP-014 |
| 15 | Scatter-Gather | EIP-015 |
| 16 | Routing Slip | EIP-016 |
| 17 | Process Manager | EIP-017 |
| 18 | Message Store | EIP-018 |
| 19 | Smart Proxy | EIP-019 |
| 20 | Test Message | EIP-020 |
| 21 | Channel Purger | EIP-021 |
| 22 | Durable Subscriber | EIP-022 |
| 23 | Guaranteed Delivery | EIP-023 |
| 24 | **Idempotent Receiver** ← Wave 0 starter | EIP-024 |
| 25 | **Dead Letter Channel** ← Wave 0 starter | EIP-025 |

> **Flagged for G2 review**: subset selection reflects Solution Architect judgment per Spec Q6. EA-Board may add or substitute at G2.

---

## Microsoft Cloud Design Patterns

Source: https://learn.microsoft.com/en-us/azure/architecture/patterns/
Fetched: 2026-05-09

### Catalog (42 patterns) with Well-Architected pillar tags

| Pattern | Summary | Pillars |
| --- | --- | --- |
| Ambassador | Helper services that send network requests on behalf of consumer | Reliability, Security |
| Anti-Corruption Layer | Façade between modern and legacy systems | Operational Excellence |
| Asynchronous Request-Reply | Decouple back-end from front-end with async | Performance Efficiency |
| Backends for Frontends | Separate backend per frontend | Reliability, Security, Performance |
| Bulkhead | Isolate elements into pools | Reliability, Security, Performance |
| Cache-Aside | Load data on demand into cache | Reliability, Performance |
| Choreography | Decentralized service orchestration | Operational Excellence, Performance |
| Circuit Breaker | Handle variable-time faults | Reliability, Performance |
| Claim Check | Split large message into claim + payload | Reliability, Security, Cost, Performance |
| Compensating Transaction | Undo a series of steps | Reliability |
| Competing Consumers | Multiple concurrent consumers from one channel | Reliability, Cost, Performance |
| Compute Resource Consolidation | Consolidate tasks into a single unit | Cost, OpEx, Performance |
| CQRS | Separate read from write interfaces | Performance |
| Deployment Stamps | Independent copies of components incl. data | OpEx, Performance |
| Event Sourcing | Append-only event store | Reliability, Performance |
| External Configuration Store | Centralised config | OpEx |
| Federated Identity | Delegate auth to external IdP | Reliability, Security, Performance |
| Gateway Aggregation | Aggregate multiple requests | Reliability, Security, OpEx, Performance |
| Gateway Offloading | Offload shared functions to gateway | All pillars |
| Gateway Routing | Single endpoint to multiple services | Reliability, OpEx, Performance |
| Geode | Geographically distributed nodes (any-to-any) | Reliability, Performance |
| Health Endpoint Monitoring | Functional checks via exposed endpoints | Reliability, OpEx, Performance |
| Index Table | Indexes over frequently queried fields | Reliability, Performance |
| Leader Election | Coordinate via elected leader | Reliability |
| Materialized View | Prepopulated views for poorly-formatted queries | Performance |
| Messaging Bridge | Bridge incompatible messaging systems | Cost, OpEx |
| Pipes and Filters | Decompose complex processing | Reliability |
| Priority Queue | Higher priority requests processed faster | Reliability, Performance |
| Publisher-Subscriber | Async event broadcast | All pillars |
| Quarantine | Quality gate for external assets | Security, OpEx |
| Queue-Based Load Leveling | Buffer between task and service | Reliability, Cost, Performance |
| Rate Limiting | Control resource consumption | Reliability |
| Retry | Retry failed operations on temporary failure | Reliability |
| Saga | Data consistency across microservices | Reliability |
| Scheduler Agent Supervisor | Coordinate distributed actions | Reliability, Performance |
| Sequential Convoy | Ordered processing of related messages | Reliability |
| Sharding | Horizontal data partitions | Reliability, Cost |
| Sidecar | Components in separate process/container | Security, OpEx |
| Static Content Hosting | Static content via cloud storage | Cost |
| Strangler Fig | Incremental legacy migration | Reliability, Cost, OpEx |
| Throttling | Control consumption from apps/tenants | Reliability, Security, Cost, Performance |
| Valet Key | Token for restricted client access | Security, Cost, Performance |

### Mapping to catalog (notable overlaps)

| MS pattern | Catalog ID (existing or planned) |
| --- | --- |
| Saga | INT-001 (existing) |
| CQRS | DATA-001 (existing) |
| Circuit Breaker | RES-002 (existing) |
| Bulkhead | RES-001 (existing) |
| Retry | RES-003 (existing) |
| Sharding | DATA-stub |
| Strangler Fig | INT-stub |
| Anti-Corruption Layer | INT-stub |
| Sidecar | RES-stub or INT-stub |
| Backends for Frontends | SEC-005 (BFF + token-binding starter) |
| Throttling | RES-stub |
| Rate Limiting | RES-stub |
| Health Endpoint Monitoring | covered in BP-004 (observability) |
| Deployment Stamps | similar to RES-005 (cell-based) |

---

## Microsoft Azure Well-Architected Framework

Source: https://learn.microsoft.com/en-us/azure/well-architected/
Fetched: 2026-05-09

### 5 pillars

1. **Reliability** — Workload meets uptime and recovery targets via redundancy and resiliency at scale.
2. **Security** — Protect from attacks while maintaining confidentiality and data integrity.
3. **Cost Optimization** — Optimisation mindset across organizational, architectural, and tactical levels.
4. **Operational Excellence** — Reduce production issues via observability and automation.
5. **Performance Efficiency** — Adjust to demand via horizontal scaling and pre-deployment testing.

> **Gap**: top-5 design principles per pillar not extracted from index page; documented in subordinate `/reliability/`, `/security/`, etc. routes. Backfill in implementation Wave 0 if needed.

---

## AWS Well-Architected — Reliability Pillar + DR Strategies

Source (Reliability welcome): https://docs.aws.amazon.com/wellarchitected/latest/reliability-pillar/welcome.html
Source (DR strategies): https://docs.aws.amazon.com/whitepapers/latest/disaster-recovery-workloads-on-aws/disaster-recovery-options-in-the-cloud.html
Fetched: 2026-05-09

### 6 Well-Architected pillars

Operational Excellence | Security | Reliability | Performance Efficiency | Cost Optimization | Sustainability

### 4 Disaster Recovery strategies (canonical for spine doc NFR-001)

| Strategy | Description | RTO range (typical) | RPO range (typical) | Cost |
| --- | --- | --- | --- | --- |
| **Backup & Restore** | Periodic backups + recovery-region IaC redeployment | Hours | Hours (depends on backup frequency) | Lowest |
| **Pilot Light** | Continuous data replication + scaled-down infra "switched off" | 10s of minutes | Seconds–minutes | Low–medium |
| **Warm Standby** | Scaled-down full functional copy in DR region; scaled up at failover | Minutes | Seconds | Medium–high |
| **Multi-site Active/Active** | Workload runs in 2+ regions simultaneously, all serving traffic | Near zero (data) | Near zero (RPO non-zero only for data corruption) | Highest |

### Key concepts for catalog

- **Data plane vs control plane**: failover operations should rely on data-plane operations only (higher availability design goals).
- **Traffic management**: Route 53 health checks (DNS) and ARC (manual on/off switches) for DNS-based failover; AWS Global Accelerator (AnyCast IP, edge network) for low-latency failover.
- **Write strategies for active/active**: write-global, write-local, write-partitioned. Aurora global database supports write-global with sub-minute promotion of secondary; DynamoDB global tables support write-local with last-writer-wins.

### Pattern catalog mapping

| AWS concept | Catalog ID |
| --- | --- |
| Multi-site Active/Active | REF-001 (spine starter) |
| Backup & Restore | covered in BP-002 (DR playbook, existing) |
| Pilot Light / Warm Standby | covered in BP-002 + REF-001 variants section |
| Cell-based / Deployment stamps | RES-005 (cell-based, starter) |

---

## Microservices.io Patterns

Source: https://microservices.io/patterns/index.html
Fetched: 2026-05-09

### Patterns by category

**Application Architecture**: Monolithic, Microservice
**Decomposition**: Decompose by business capability, by subdomain; Self-contained Service; Service per team
**Data Management**: Database per Service, Shared database, Saga, Command-side replica, API Composition, CQRS, Domain event, Event sourcing, Transactional outbox, Transaction log tailing, Polling publisher
**Communication Style**: RPI, Messaging, Domain-specific protocol, Idempotent Consumer
**External API**: API gateway, Backend for Frontend
**Service Discovery**: Client-side / Server-side / Service registry / Self-registration / 3rd-party registration
**Reliability**: Circuit Breaker
**Security**: Access Token
**Observability**: Log aggregation, Application metrics, Audit logging, Distributed tracing, Exception tracking, Health check API, Log deployments and changes
**UI**: Server-side / Client-side composition
**Testing**: Consumer-driven contract test, Consumer-side contract test, Service component test
**Deployment**: per host / per VM / per Container / Serverless / Service deployment platform
**Cross-cutting**: Microservice chassis, Externalized configuration, Service Template
**Refactoring**: Strangler Application, Anti-corruption layer

### Catalog mapping

Patterns already mapped to existing or planned catalog rows: Database per Service (PRIN-004), Saga (INT-001), CQRS (DATA-001), Event sourcing (INT-004), Transactional outbox (INT-002), API gateway (INT-003), Backend for Frontend (SEC-005), Idempotent Consumer (PRIN-006 + EIP-024), Circuit Breaker (RES-002), Bulkhead (RES-001), Strangler/Anti-corruption (INT-stub).

---

## Resilience4j

Source: https://resilience4j.readme.io/docs/getting-started + /docs/circuitbreaker
Fetched: 2026-05-09

### Core modules (Java 8+ functional library)

| Module | Purpose |
| --- | --- |
| CircuitBreaker | Prevent cascading failures by stopping calls to failing services |
| RateLimiter | Control frequency of requests to protect resources |
| Bulkhead | Isolate resources to prevent exhaustion |
| Retry | Automatic retry (sync and async) of failed operations |
| TimeLimiter | Enforce time constraints on operations |
| Cache | Store and retrieve previous call results |

Spring Boot integration via separate starter modules: `resilience4j-spring-boot3`. Annotation usage (e.g., `@CircuitBreaker(name="...")`, `@Retry(name="...")`, `@Bulkhead(name="...")`) documented in Spring-specific pages — fetch as needed during Wave 3a authoring.

### CircuitBreaker — state machine (canonical for RES-002 upgrade)

**Normal states**:
- **CLOSED**: default; calls proceed. Transitions to OPEN when failure/slow-call thresholds breached.
- **OPEN**: rejects calls with `CallNotPermittedException`. Transitions to HALF_OPEN after `waitDurationInOpenState`.
- **HALF_OPEN**: allows `permittedNumberOfCallsInHalfOpenState` test calls. Transitions to CLOSED if thresholds satisfied; back to OPEN if violated.

**Special states**: METRICS_ONLY (records but never trips), DISABLED (always permits), FORCED_OPEN (always denies).

### CircuitBreaker — config schema (defaults)

| Parameter | Default | Type |
| --- | --- | --- |
| `failureRateThreshold` | 50 | percentage |
| `slowCallRateThreshold` | 100 | percentage |
| `slowCallDurationThreshold` | 60000 | milliseconds |
| `permittedNumberOfCallsInHalfOpenState` | 10 | count |
| `maxWaitDurationInHalfOpenState` | 0 | milliseconds |
| `slidingWindowType` | COUNT_BASED | COUNT_BASED \| TIME_BASED |
| `slidingWindowSize` | 100 | count or seconds |
| `minimumNumberOfCalls` | 100 | count |
| `waitDurationInOpenState` | 60000 | milliseconds |
| `automaticTransitionFromOpenToHalfOpenEnabled` | false | boolean |
| `recordExceptions` | empty | list |
| `ignoreExceptions` | empty | list |

> "When the failure rate is equal or greater than the threshold the CircuitBreaker transitions to open." Calculations require meeting `minimumNumberOfCalls` first.

---

## State Bank of Vietnam — Circular 09/2020/TT-NHNN

Source attempted: https://www.sbv.gov.vn/webcenter/portal/en/home/sbv/legaldoc
Fetched: 2026-05-09 — homepage only; circular text not exposed.

> **UNOFFICIAL TRANSLATION pending Legal review per Spec Q2.**

### Working summary (general public knowledge of Vietnamese banking IT regulation)

Circular 09/2020/TT-NHNN, issued by the State Bank of Vietnam, governs IT security in banks. Believed to cover (subject to Legal verification):

- §I — Scope and definitions
- §II — IT security organisation and responsibilities
- §III — Cryptographic controls, multi-factor authentication, and access management
- §IV — Operational continuity, incident response, business continuity planning
- §V — Data protection and cross-border data handling

### TODO — required before Wave-0 G3 sign-off of Compliance Mapping Matrix (COMP-001)

- [ ] Engage in-house Legal team (`@legal-vietnam` per `registry/catalog-reviewers.yml`) to provide authoritative English translation, or confirm clauses §II–§V mapped above
- [ ] Replace working summary with verbatim clause references
- [ ] Lift "UNOFFICIAL TRANSLATION" badges from compliance mappings on Approved patterns

---

## Government of Vietnam — Decree 13/2023/NĐ-CP and Decree 53/2022/NĐ-CP

Sources attempted: vbpl.vn (Vietnamese-only legal portal) — not fetched in Phase 0 due to language barrier.

> **UNOFFICIAL TRANSLATION pending Legal review per Spec Q2.**

### Working summary

- **Decree 13/2023/NĐ-CP** (Personal Data Protection Decree): defines categories of personal data (including biometric and sensitive data), processor/controller obligations, cross-border transfer rules, DPO requirements, consent and data-subject-rights regime. Effective 2023-07-01.
- **Decree 53/2022/NĐ-CP** (Data Localisation Decree, implementing the Cybersecurity Law 2018): requires certain types of user data and activity logs of Vietnamese users to be stored within Vietnam by certain categories of foreign and domestic services, and imposes data-handover obligations on national security grounds.

### TODO — required before Wave-0 G3 sign-off

- [ ] Legal team to provide authoritative translations of: Decree 13 Articles 1–5 (definitions), 11–13 (cross-border), 28–32 (DPO/penalties); Decree 53 Articles 26–28 (localisation requirements for banks)
- [ ] Confirm scope of localisation: which Techcombank data flows are in vs out of scope?

---

## PCI Security Standards Council — PCI-DSS v4.0

Source attempted: https://www.pcisecuritystandards.org/standards/pci-dss/ — public page lists categories but not the requirements themselves.

### 12 high-level requirements (canonical structure, well-known industry knowledge)

| # | Requirement |
| --- | --- |
| 1 | Install and maintain network security controls |
| 2 | Apply secure configurations to all system components |
| 3 | Protect stored account data |
| 4 | Protect cardholder data with strong cryptography during transmission over open, public networks |
| 5 | Protect all systems and networks from malicious software |
| 6 | Develop and maintain secure systems and software |
| 7 | Restrict access to system components and cardholder data by business need to know |
| 8 | Identify users and authenticate access to system components |
| 9 | Restrict physical access to cardholder data |
| 10 | Log and monitor all access to system components and cardholder data |
| 11 | Test security of systems and networks regularly |
| 12 | Support information security with organizational policies and programs |

### Requirement 3 — sub-requirements relevant to Tokenization + HSM (SEC-004)

- 3.5 Cryptographic keys used to protect stored account data are protected against disclosure and misuse.
- 3.6 Cryptographic keys used to protect stored account data are managed throughout their lifecycle.
- 3.7 Where cryptography is used to protect stored account data, key management policies and procedures, including key generation, distribution, storage, change/destruction, are formally defined and implemented.

### TODO — required before SEC-004 G4 sign-off

- [ ] Acquire and cite the official PCI-DSS v4.0 PDF (paid download from PCI SSC) for verbatim §3.5/3.6/3.7 wording
- [ ] Confirm Requirement 3.4 (PAN protection) wording for tokenization context

---

## Bank for International Settlements — BCBS 239 + BCBS 230

### BCBS 239 — Principles for Effective Risk Data Aggregation and Risk Reporting

Source: https://www.bis.org/publ/bcbs239.htm
Fetched: 2026-05-09

#### Governance & Infrastructure (1–2)

1. **Governance** — Board and senior management oversight of risk-data aggregation infrastructure.
2. **Organizational Integration** — Independence and resourcing of risk-data aggregation functions.

#### Risk Data Aggregation Capabilities (3–6)

3. **Data Architecture** — Comprehensive, documented framework for collecting and integrating risk data.
4. **Granularity & Accuracy** — Capture risk data at sufficiently detailed levels.
5. **Timeliness** — Systems enabling rapid and accurate risk-data aggregation.
6. **Completeness** — Aggregation covers all material risk exposures.

#### Risk Reporting Practices (7–11)

7. **Frequency & Distribution** — Risk reports to relevant stakeholders with appropriate frequency.
8. **Accuracy & Verification** — Validate completeness and accuracy of reports.
9. **Adaptability** — Reporting systems can adjust to changing environments and stress.
10. **Clarity & Usability** — Accessible formats supporting decision-making.
11. **Consistency** — Standardised definitions and methodologies.

#### Supervisory Review (12–14)

12. **Supervisory Assessment** — Enable regulators to evaluate compliance.
13. **Corrective Actions** — Remediate identified deficiencies.
14. **Validation & Testing** — Regular stress testing and validation.

### BCBS 230 — Principles for Operational Resilience (BIS d516)

Source: https://www.bis.org/bcbs/publ/d516.htm
Fetched: 2026-05-09 — welcome page only; full PDF (12 pages) NOT fetched.

Working summary based on webpage abstract:
- Issued 2021-03-31 in response to pandemics, cyber incidents, technology failures, natural disasters
- Builds on operational risk management, corporate governance, outsourcing, and BCM frameworks
- Concept of **impact tolerance** is central — banks must define maximum tolerable level of disruption per critical operation

### TODO — required before NFR-001 (Service Tiering) G3 sign-off

- [ ] Fetch full BCBS d516 PDF; extract the 7 principles and the impact-tolerance / RTO guidance verbatim
- [ ] Map principles to RES-005 (cell-based architecture) and BP-002 (DR playbook)

---

## ISO 20022

Source attempted: https://www.iso20022.org/iso-20022-message-definitions — timed out (twice).

### Working summary (industry standard knowledge)

ISO 20022 is the international standard for financial-services electronic data interchange. Messages are organised into business domains identified by 4-letter abbreviations:

| Domain | Purpose |
| --- | --- |
| `pacs` | Payments Clearing & Settlement (interbank) |
| `pain` | Payments Initiation (customer-to-bank) |
| `camt` | Cash Management (account reporting, liquidity) |
| `acmt` | Account Management |
| `auth` | Authorities (regulatory reporting) |
| `head` | Business Application Header |
| `secl` | Securities Clearing |
| `seev` | Securities Events |
| `setr` | Securities Trade |
| `tsmt` | Trade Services Management |
| `remt` | Remittance |

### Top message types in real-time payments (`pacs`)

- `pacs.008` — FI-to-FI Customer Credit Transfer
- `pacs.002` — FI-to-FI Payment Status Report
- `pacs.004` — Payment Return
- `pacs.007` — FI-to-FI Payment Reversal
- `pacs.028` — FI-to-FI Payment Status Request

### TODO — required before REF-002 (Real-Time Payments NAPAS) G4 sign-off

- [ ] Re-fetch ISO 20022 message-definitions catalogue (off-peak, longer timeout) or use a static archive URL
- [ ] Confirm `pacs.008` schema for NAPAS instant transfer flow

---

## SWIFT — Customer Security Programme v2024

Source attempted: https://www.swift.com/myswift/customer-security-programme-csp/security-controls — timed out.

### Working summary (industry knowledge)

SWIFT CSP requires every SWIFT user to attest annually against the **Customer Security Controls Framework (CSCF)**. CSCF v2024 organises controls into 7 objectives:

1. **Restrict Internet Access** — segregate SWIFT environment from corporate network
2. **Reduce Attack Surface and Vulnerabilities** — patching, hardening
3. **Physically Secure the Environment**
4. **Prevent Compromise of Credentials**
5. **Manage Identities and Segregate Privileges**
6. **Detect Anomalous Activity to Systems or Transaction Records**
7. **Plan for Incident Response and Information Sharing**

Approximately 22 mandatory controls + 9 advisory controls in v2024 (subject to verification).

### TODO — required before REF-005 (SWIFT MT/MX wire-transfer stub-to-Approved) authoring

- [ ] Fetch authoritative CSCF v2024 PDF for verbatim control IDs (1.1, 1.2, 2.1A, etc.)
- [ ] Map mandatory controls to network-segmentation patterns in catalog

---

## NAPAS — Public Technical References

Source: https://napas.com.vn/en/payment-products/
Fetched: 2026-05-09

### Payment products

| Product | Description | Settlement | Format |
| --- | --- | --- | --- |
| **NAPAS 247** | Real-time fund transfer across member banks 24/7 | Immediate | (not public) |
| VietQR Transfer | QR-based transfer, max 500M VND/txn | Real-time, 24/7 | QR code |
| International Switching | Cross-border switching | (not public) | (not public) |
| Domestic ATM/POS Switching | ATM and POS routing | (not public) | (not public) |
| Online Payment Gateway | Web payment processing | (not public) | (not public) |
| Bill Payment Service | Utility / bill payments | (not public) | (not public) |
| E-wallet Top-up/Withdraw | Wallet load/withdraw | (not public) | (not public) |
| QR Payment Service | Merchant QR | (not public) | QR code |
| VietQRCash | QR-based cash withdraw | (not public) | QR code |
| Tap to Phone | NFC acceptance | (not public) | NFC/contactless |
| Apple Pay | Apple wallet integration | (not public) | (not public) |
| NAPAS Tap & Pay | Contactless payment | (not public) | Contactless |

### Catalog usage

- REF-002 (Real-Time Payments NAPAS reference architecture) is the primary consumer of this section
- Vendor-confidential protocol details are out of scope per Spec §8 — reference only the public products listed above

---

## Quick Index — Pattern-to-Source Map

| Catalog ID (target) | Primary source(s) | Sub-section |
| --- | --- | --- |
| PRIN-006 (Idempotency) | EIP §10.1; microservices.io Idempotent Consumer; BCBS 239 §6 | EIP-024 mapping |
| EIP-024 (Idempotent Receiver) | EIP §10 (Hohpe/Woolf); Resilience4j Retry | EIP catalog row |
| EIP-025 (Dead Letter Channel) | EIP §10 (Hohpe/Woolf); MS Cloud "Publisher-Subscriber" | EIP catalog row |
| RES-002 (Circuit Breaker) | Resilience4j /circuitbreaker; MS Cloud Circuit Breaker; microservices.io | Resilience4j config schema |
| RES-005 (Cell-Based Architecture) | AWS DR strategies; MS Cloud Deployment Stamps; AWS Bulkhead | AWS DR strategies |
| NFR-001 (Service Tiering RTO/RPO) | AWS DR Strategies (4-pattern matrix); BCBS 230; Azure WAF Reliability | AWS DR strategies table |
| NFR-002 (Latency Budget) | Azure WAF Performance Efficiency; AWS WA Performance | (TODO: deeper extraction) |
| REF-001 (Multi-Region A/A) | AWS DR Strategies "Multi-site Active/Active" | AWS DR strategies |
| REF-002 (RT Payments NAPAS) | NAPAS public docs; ISO 20022 pacs | (TODO: NAPAS protocol via vendor portal) |
| SEC-004 (Tokenization + HSM) | PCI-DSS §3.5–3.7 | (TODO: official PCI-DSS PDF) |
| COMP-001 (Compliance Matrix) | All sources; SBV Circ. 09; Decrees 13+53 | (TODO: Legal-team translations) |
| BP-005 (Chaos Engineering) | (external: principlesofchaos.org — not yet fetched) | TODO Wave 3a |

---

**Last fetch session**: 2026-05-09. Re-run fetches for timed-out sources before authoring the affected starter-set docs (currently NFR-001, NFR-002, COMP-001, REF-002, SEC-004 are partially blocked).
