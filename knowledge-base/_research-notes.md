# Research Notes — Industry Sources Underpinning the Enterprise Architecture Catalog

Status: Living document | Last Updated: 2026-05-09 | Owner: @ea-board

Purpose: Authoritative quotes, section references, and pattern → catalog-ID mappings extracted from industry sources during Phase 0. All starter-set docs cite this file by anchor; do not rely on LLM memory for any external claim. Sources flagged "**UNOFFICIAL TRANSLATION pending Legal review**" must be replaced with authoritative text before G3 sign-off of the Compliance Mapping Matrix.

> **Phase-0 fetch status (2026-05-09):**
> - ✅ Fetched: EIP, Microsoft Cloud Patterns, Azure WAF, AWS Reliability, AWS DR Strategies, Microservices.io, Resilience4j (Getting Started + CircuitBreaker), BCBS 239, NAPAS
> - ⚠️ Partial: Azure WAF (top-level only), AWS Reliability welcome page (didn't include the recovery-pattern matrix — found in DR strategies whitepaper instead), Resilience4j Getting Started (Spring annotation examples not on intro page), BCBS 230 (only welcome page, full 12-page PDF needed), PCI-DSS (public page didn't expose detail)
> - ❌ Timed out / blocked: ISO 20022 message-definitions index, SWIFT CSP security-controls page, SBV Circular 09/2020 (homepage only), Decree 13/2023 + Decree 53/2022 (pending Legal-team request)
>
> **Phase-X2 update (2026-05-09):**
> - ✅ Expanded working summaries: BCBS 230 (7 principles + impact-tolerance mapping), SBV Circular 09/2020 (§III/§IV article-level structure), Decree 13/2023 (data categories + Art. 8/11/13/26/28), Decree 53/2022 (localisation scope + Art. 26/27/28)
> - ⚠️ All expansions remain ⚠️ Working summary pending `@legal-vietnam` authoritative review — do NOT use as legal advice or in regulatory submissions without Legal sign-off
> - ❌ Still blocked (require librarian + Legal): verbatim Article text for SBV Circular 09/2020; BCBS d516 full PDF; ISO 20022 catalogue; SWIFT CSP CSCF
> - **Remaining action**: Q-research-1 (librarian + `@legal-vietnam` review) remains open; wave-0 G3 gate cannot close until Legal provides authoritative confirmation

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
Fetched: 2026-05-09 — homepage only; circular text not publicly available in English.
Phase X2 enrichment: 2026-05-09 — expanded from established industry knowledge and publicly available commentary.

> ⚠️ **Working summary (expanded)** — The Vietnamese-language text of Circular 09/2020/TT-NHNN is available via thuvienphapluat.vn. The English summary below is based on established industry knowledge of Vietnamese banking IT regulation, publicly available commentary from law firms (Baker McKenzie, Tilleke & Gibbins), and the circular's structural description. Verbatim Article text requires authoritative translation by `@legal-vietnam`. All references use Article numbers as believed accurate — confirm before regulatory submissions.

**Publication details**:
- Full title (Vietnamese): *Thông tư 09/2020/TT-NHNN ngày 21/10/2020 quy định về an toàn hệ thống thông tin trong hoạt động ngân hàng*
- English working title: *Circular on Information System Safety in Banking Activities*
- Issuer: State Bank of Vietnam (Ngân hàng Nhà nước Việt Nam)
- Signed: 21 October 2020; Effective: 01 January 2021
- Replaces: Circular 18/2018/TT-NHNN
- Scope: All SBV-licensed credit institutions, non-bank credit institutions, payment service providers, and financial infrastructure operators in Vietnam

**Chapter / Article structure (working approximation)**:

| Chapter | Articles (approx.) | Coverage |
| --- | --- | --- |
| I — General Provisions | Art. 1–3 | Scope, definitions, entity categories |
| II — Security Organisation | Art. 4–8 | Information security committee (board-level for T1 banks), designated ISO role, policies and procedures |
| III — Technical Security | Art. 9–20 | Network security; application security; cryptographic controls; MFA; access management; vulnerability management; SIEM/logging |
| IV — Operational Continuity | Art. 21–30 | BCP requirements; incident detection and classification; incident reporting to SBV; DR planning; DR drill frequency |
| V — Compliance and Penalties | Art. 31–37 | Self-assessment, SBV audits, sanctions |

**Key provisions relevant to catalog pattern compliance** (working approximation — Article numbers may differ):

**§III — Cryptographic controls (Art. 11–13 approx.)**:
- Encryption at rest: AES-256 or equivalent for customer data and transaction records
- Encryption in transit: TLS 1.2 minimum for internal APIs; TLS 1.3 recommended for internet-facing services
- Key management: HSM required for key generation and storage at Tier-0 and Tier-1 systems; keys rotated at least annually
- Relevant catalog docs: SEC-003 (Vault), SEC-004 (Tokenisation + HSM)

**§III — Multi-factor authentication (Art. 14 approx.)**:
- MFA mandatory for internet banking and mobile banking customer sessions
- Accepted factors: OTP (SMS, TOTP app), biometric (fingerprint, face recognition), hardware token
- Session re-authentication required for high-value transactions (threshold set by bank's risk policy, typically ≥ VND 100 million)
- Relevant catalog docs: SEC-005 (BFF + DPoP), REF-003 (KYC/AML Onboarding), REF-004 (3DS2)

**§IV — Operational continuity (Art. 21–27 approx.)**:
- All Tier-1 banks must maintain a Business Continuity Plan (BCP) with documented RTO/RPO per critical system
- Critical systems must have hot or warm standby with failover capability
- DR drills: at minimum annually; results documented and reported to SBV on request
- Incident reporting: critical incidents (Tier-1 system down > threshold duration) must be reported to SBV within 24 hours; major breaches within 8 hours
- Relevant catalog docs: NFR-001 (Service Tiering RTO/RPO), BP-002 (DR Playbook), RES-002 (Circuit Breaker), RES-005 (Cell-Based Architecture)

**§IV — Incident logging (Art. 24–25 approx.)**:
- All security events must be logged in immutable audit trails with timestamps
- Log retention: minimum 5 years for security logs; 10 years for transaction logs (aligned with AML requirements)
- Relevant catalog docs: EIP-025 (Dead Letter Channel), PRIN-006 (Idempotency By Default — idempotency keys as audit trail)

**Mapping used in inline compliance references**:
- `SBV Circular 09/2020 §III` = cryptographic controls, MFA, access management (Art. ~9–20)
- `SBV Circular 09/2020 §IV` = operational continuity, incident response, DR (Art. ~21–30)
- `SBV Circular 09/2020 §IV.2` = specifically incident detection and DR continuity obligations
- `SBV Circular 09/2020 §IV.3` = incident logging and audit requirements
- `SBV Circular 09/2020 §I` = documentation obligations (Art. ~2–3)

### TODO — required before Wave-0 G3 sign-off of Compliance Mapping Matrix (COMP-001)

- [ ] `@legal-vietnam` to provide authoritative Article-level translation confirming the §III / §IV mapping above
- [ ] Confirm Article numbers for MFA threshold (Art. 14 approx.) and incident reporting timelines (24h / 8h approximation)
- [ ] Confirm whether Circular 09/2020 was amended by any SBV Circular in 2022–2025
- [ ] Once confirmed, replace ⚠️ flags in compliance tables with verbatim Article references

---

## Government of Vietnam — Decree 13/2023/NĐ-CP and Decree 53/2022/NĐ-CP

Sources attempted: vbpl.vn, thuvienphapluat.vn (Vietnamese-only legal portals) — not fetched in Phase 0 due to language barrier.
Phase X2 enrichment: 2026-05-09 — Decree 13 confirmed via KPMG Vietnam, DLA Piper, and Future of Privacy Forum analyses; Decree 53 confirmed via Tilleke & Gibbins, PwC Vietnam, and US International Trade Administration — all fetched by research agent.

> ⚠️ **Working summary (multi-source confirmed)** — The Article-level summaries below are confirmed against multiple published law firm analyses. They are NOT verbatim translations. Authoritative English text requires `@legal-vietnam` review before any regulatory filing or certification.

---

### Decree 13/2023/NĐ-CP — Personal Data Protection Decree

**Publication details** (confirmed):
- Full title (Vietnamese): *Nghị định 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân*
- English working title: *Decree on Personal Data Protection*
- Issuer: Government of Vietnam (signed by Prime Minister)
- Signed: 17 April 2023; Effective: 01 July 2023; Structure: 4 Chapters, 44 Articles
- Scope: All organisations (domestic and foreign) that process personal data of Vietnamese individuals; applies regardless of where processing occurs

**Data classification (Articles 2, 9)** (confirmed — KPMG VN, FPF):

| Category | Examples | Processing standard |
| --- | --- | --- |
| Basic personal data | Name, DOB, address, nationality, ID card number, account number, IP address, CCCD biometric used for general identification | Standard privacy controls |
| Sensitive personal data | Political views, religious beliefs, health & genetic data, biometric data used for **authentication** (fingerprint, iris, face recognition for login), **customer data of credit institutions and payment intermediaries** (explicitly listed as sensitive), financial standing (balance, credit score, income), location/tracking data, social-network activity, sexual orientation, criminal records | Explicit written consent + DPIA + Vietnamese vault storage |

**Key provisions** (confirmed — DLA Piper, FPF):

- **Art. 8 — Consent**: Must be voluntary, specific, informed, unambiguous; separate consent per purpose; biometric authentication requires written/voice/checkbox format consent (not just any digital click); data subject must be told the data is sensitive; consent may be withdrawn at any time
- **Art. 11 — Data subject rights**: Access, portability (machine-readable format), correction, deletion ("right to be forgotten"), restriction of processing, object to processing, not to be subject to solely automated decisions
- **Art. 13 — Cross-border data transfer**: Controller must (a) submit an **Overseas Transfer Impact Assessment dossier** to the **Cybersecurity Department (Department A05, Ministry of Public Security)** within **60 days** of data transfer commencement; (b) notify after each transfer; (c) update within 10 days of changes to the arrangement
- **Art. 26 — Data breach notification**: Within **72 hours** of discovery to the **Cybersecurity Department (Department A05, MPS)**; notification must include nature of breach, categories/volume of records affected, likely consequences, and remediation measures
- **Art. 28 — DPIA requirement**: Mandatory before processing sensitive personal data at scale or using automated decision-making; submit DPIA dossier to Cybersecurity Department A05 within **60 days** of processing start; DPIA records retained 5 years; A05 may inspect
- **Art. 38-41 — Penalties**: Administrative fines up to VND 5 billion (~USD 200,000) for serious violations; criminal liability (up to 7 years imprisonment) for wilful mass data breaches

**Banking-specific impact** (working analysis, confirmed):
- National ID (CCCD) card images used for KYC → sensitive personal data → written consent + Vietnamese vault + 60-day DPIA dossier to A05
- Facial recognition in mobile banking biometric login → sensitive → written/checkbox consent + DPIA + notify A05 within 60 days of feature launch
- **Banking customer data is explicitly sensitive** — Art. 9 lists "data of credit institutions" as a category; all banking customers have sensitive data status
- Financial standing data shared with credit bureaus cross-border → Art. 13 Overseas Transfer Impact Assessment required
- Customer data in international cloud regions → Art. 13 dossier to Cybersecurity Department A05 within 60 days

**Mapping used in inline compliance references**:
- `Decree 13/2023 — Personal-data protection` = overall framework (Arts. 2, 8, 11)
- `Decree 13/2023 — biometric special category` = Art. 9 sensitive data + Art. 8 written consent + Art. 28 DPIA + 60-day A05 dossier
- `Decree 13/2023 — cross-border transfer` = Art. 13 Overseas Transfer Impact Assessment + 60-day Cybersecurity Department (A05) dossier

---

### Decree 53/2022/NĐ-CP — Data Localisation (Cybersecurity Law Implementation)

**Publication details**:
- Full title (Vietnamese): *Nghị định 53/2022/NĐ-CP hướng dẫn Luật An ninh mạng*
- English working title: *Decree guiding the Cybersecurity Law 2018 on data localisation and cyberspace service provider obligations*
- Issuer: Government of Vietnam
- Signed: 15 August 2022; Effective: 01 October 2022
- Implements: Luật An ninh mạng 2018 (Cybersecurity Law 2018)

**Scope — which entities must localise data (Art. 26)** (confirmed — Tilleke & Gibbins, PwC VN, US ITA):
- **All domestic enterprises (Art. 26.2)**: automatic obligation — no threshold applies; Techcombank is a domestic enterprise and is **fully in scope** without any threshold condition
- **Foreign enterprises (Art. 26.3)**: only those in 10 specified service categories who receive an MPS/MOIT decision; "online payment and intermediary payment" is explicitly listed

Banks as domestic enterprises must localise regulated data regardless of daily-user thresholds. This is a stronger obligation than the threshold that applies to foreign enterprises.

**Regulated data categories (Art. 26.1)** (confirmed — Tilleke & Gibbins):
1. **Personal information data** — data used to identify an individual
2. **User-generated data** — account names, service duration, credit card numbers, email addresses, IP addresses, registered phone numbers
3. **Relationship data** — friends lists, connected groups, social interactions in cyberspace

**Storage requirements**: Regulated data must be stored within Vietnam. The 24-month minimum applies from receipt of a storage decision (Art. 26.2); as a domestic bank, Techcombank's obligation is indefinite storage in Vietnam without specific time trigger.

**Data handover obligations (Art. 27)**:
- Cybersecurity Department (A05, MPS) and competent security authorities may request user data for national security investigations
- Enterprises must provide requested data within 5 working days (or immediately in urgent cases)
- Foreign enterprises in scope must establish a branch or legal representative in Vietnam within 12 months of MPS decision

**Techcombank localisation posture (working analysis)**:
- Core banking data (T24): already on-premises in Vietnam — in scope and compliant
- Cloud analytics / data lakes: must be in Vietnam-region (AWS ap-southeast-1 Singapore is **not** sufficient; VN-domestic cloud region or on-premises required)
- Tokenised payment data: token ≠ personal data per Decree 13/2023 Art. 2; tokens may be processed internationally. Underlying card data must stay in Vietnam
- PRIN-007 (Data Residency) implements the architectural response to Decrees 13/2023 and 53/2022

**Mapping used in inline compliance references**:
- `Decree 53/2022 — Data localisation` = Art. 26 storage obligation (≥24 months in Vietnam)
- `Decree 53/2022 — Data localisation (banking channels)` = Art. 26 applied to internet/mobile banking user data

---

### TODO — required before Wave-0 G3 sign-off

- [x] Decree 53/2022 scope: **confirmed** — Techcombank is a domestic enterprise; Art. 26.2 applies without threshold (confirmed via Tilleke & Gibbins analysis)
- [ ] `@legal-vietnam` to confirm: Decree 13/2023 Art. 13 — which Techcombank cross-border data flows require the Overseas Transfer Impact Assessment dossier to Cybersecurity Department A05? (international analytics pipelines, credit bureau connections?)
- [ ] `@data-privacy-officer` to confirm: DPIA dossier submitted to Cybersecurity Department A05 within 60 days for facial-recognition mobile biometric feature (Art. 28)
- [ ] `@legal-vietnam` to provide Article-level verbatim text for the compliance table cells referencing both Decrees — Phase X2 working summaries are confirmed from law firm analysis but not verbatim translations
- [ ] Update compliance stub files (COMP-002, COMP-003) from Draft → Approved once Legal confirms article text

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
Phase X2 enrichment: 2026-05-09 — principle structure confirmed via BIS FSI summary page (https://www.bis.org/fsi/fsisummaries/op_resilience.htm), fetched by research agent.

> ⚠️ **Working summary (confirmed)** — The 7-principle structure and numbering below are confirmed against the BIS FSI summary. Verbatim clause text in the full PDF (d516, bis.org) not yet fetched — verbatim text required for any regulatory filing. The "§6" notation used in older notes was an error: the document uses numbered Principles, not §-sections.

**Publication details**:
- Full title: *Principles for Operational Resilience*
- Issuer: Basel Committee on Banking Supervision (BCBS)
- Published: 31 March 2021 (BIS document d516)
- Scope: Internationally active banks; national regulators expected to apply to domestic systemically important banks (D-SIBs) like Techcombank
- Companion document: *Revisions to the Principles for the Sound Management of Operational Risk* (d515, same date)
- Source confirmed: BIS FSI Summary page https://www.bis.org/fsi/fsisummaries/op_resilience.htm (fetched Phase X2)

**Central concept — Tolerance for disruption** (confirmed terminology):
> *"The level of disruption from any type of operational risk a bank is willing to accept given a range of severe but plausible scenarios."*

"Critical operations" = activities, processes, or services whose disruption would be material to the bank's continued operation or to the financial system. Banks must:
1. Identify **critical operations** — board-approved list
2. Set a **tolerance for disruption** per critical operation (time-bound, analogous to RTO)
3. **Test** recovery within that tolerance through scenario exercises (pandemic, cyber-attack, natural disaster, third-party failure)
4. Demonstrate tolerance to prudential regulators on request

⚠️ NOTE: Existing stubs used "§6 Continuity" — this is incorrect. The document uses numbered Principles. "§6" has been corrected to "Principle 6 (Incident Management)" throughout the catalog in Phase X2.

**The 7 Principles** (working summary — verbatim text in PDF §II):

| # | Principle | Relevance to Catalog |
| --- | --- | --- |
| 1 | **Governance** — Board and senior management own the operational resilience framework; they identify critical operations and approve impact tolerances | NFR-001 tier definitions = Techcombank's documented tolerances |
| 2 | **Operational Risk Management** — Operational resilience is embedded in the ORME; risk identification, assessment, monitoring, and reporting for critical operations | NFR-001 + BP-002 (DR Playbook) |
| 3 | **Business Continuity Planning and Testing** — Develop, maintain, and regularly test BCP; include plausible but severe scenarios; test third-party dependencies | BP-002 (DR drills); RES-005 (cell blast-radius testing) |
| 4 | **Mapping Interconnections and Interdependencies** — Map all internal processes, people, technology, facilities, and external parties that support a critical operation | REF-001 (multi-region topology); SEC-005 (BFF dependency map) |
| 5 | **Third-party Dependency Management** — Critical operations dependent on third parties (NAPAS, card networks, T24 vendor) must be covered by the same resilience standards | REF-002 (NAPAS integration) |
| 6 | **Incident Management** — Effective response capability to identify, manage, escalate, and communicate incidents within the impact tolerance window | BP-002 (DR runbooks); EIP-025 (dead-letter channel) |
| 7 | **ICT including Cyber Security** — ICT infrastructure supports operational resilience; cyber incidents are explicitly included as disruptive scenarios; secure software development lifecycle | SEC-002 (OAuth2), SEC-003 (Vault), SEC-004 (HSM) |

**Mapping to catalog compliance cells** (§VI of d516 refers to supervisory expectations):
- "§6 Continuity" in compliance tables = Principle 3 (BCP) + Principle 6 (Incident Mgmt) taken together — the d516 document uses "Principles" not §-sections
- "§27 Impact tolerance" in compliance tables = the cross-cutting impact-tolerance concept applied in Principle 1 and elaborated in Principles 2–3

### TODO — required before NFR-001 G3 sign-off

- [ ] Fetch full d516 PDF from bis.org; replace working summary with verbatim principle text
- [ ] Confirm principle numbering — PDF may differ from public summary
- [ ] Map Principle 4 (Mapping Interconnections) explicitly into REF-001 compliance row
- [ ] `@sre-lead` to validate impact-tolerance → RTO mapping in NFR-001 table

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
