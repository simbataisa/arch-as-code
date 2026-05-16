# Wave 4 Stub Promotion (48 docs) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Promote all 48 Wave 1 stubs (EIP-001–023, PRIN-008–013, RES-006–012, BP-006–011, NFR-003–005, TPL-002–004) from Proposed → Draft by filling the two known content gaps, running validation scripts, and bulk-updating the enterprise-architecture-catalog.

**Architecture:** Unlike Wave 3 (authoring from scratch), Wave 4 is a quality-gate and catalog-promotion wave. Pre-audit confirmed most stubs are already 275–550 lines with all required sections. Two targeted fixes are needed: (1) TPL-003 has `## Compliance Hooks` instead of `## Compliance Mapping`; (2) PRIN-008, PRIN-009, and PRIN-011 are missing explicit `## When to Use / When Not to Use` sections. After fixes, each sub-wave runs mermaid lint, compliance check, and catalog row updates.

**Tech Stack:** Bash, Python 3 (scripts), `mmdc` (Mermaid CLI), `grep`, `git`

**Ring 2 rule:** Any Ring 2 (SBV/Decree) compliance cell must end with `⚠️ (working summary — pending Legal review)`

**Sub-wave map (48 docs):**
| Sub-wave | Domain | IDs | Docs |
|----------|--------|-----|------|
| 4A | EIP patterns (first half) | EIP-001–012 | 12 |
| 4B | EIP patterns (second half) | EIP-013–023 | 11 |
| 4C | Architecture Principles | PRIN-008–013 | 6 |
| 4D | Resilience Patterns | RES-006–012 | 7 |
| 4E | Best Practices + NFR + Templates | BP-006–011, NFR-003–005, TPL-002–004 | 12 |

---

## Task 0: Pre-flight Verification

**Files:**
- Read: `governance/standards/enterprise-architecture-catalog.md`
- Run: `scripts/mermaid-lint-doc.sh`, `scripts/check-compliance-rows.py`

- [ ] **Step 1: Confirm Proposed count**

```bash
grep "| Proposed |" governance/standards/enterprise-architecture-catalog.md | wc -l
```
Expected: a number ≥ 48 (may include Wave 2/3 docs not yet executed from the Wave 3 plan).

- [ ] **Step 2: Confirm compliance failures**

```bash
python3 scripts/check-compliance-rows.py 2>&1
```
Expected output: exactly `FAIL TPL-003 (knowledge-base/templates/stub-doc-template.md): missing '## Compliance Mapping' heading`

If additional FAILs appear, fix them before proceeding.

- [ ] **Step 3: Confirm git is clean on main**

```bash
git status
```
Expected: `nothing to commit, working tree clean`

---

## Task 1: Fix TPL-003 — Compliance Mapping Heading

**Files:**
- Modify: `knowledge-base/templates/stub-doc-template.md`

- [ ] **Step 1: Replace the Compliance Hooks section**

Open `knowledge-base/templates/stub-doc-template.md` and replace the `## Compliance Hooks` section with the correct `## Compliance Mapping` heading and a 3-ring placeholder table that matches the pattern doc standard.

Current content to replace:
```
## Compliance Hooks

- Ring 0 (generic): TBD
- Ring 1 (international banking): TBD
- Ring 2 (Vietnam / SBV): TBD
```

Replace with:
```markdown
## Compliance Mapping

| Ring | Framework | Control |
|------|-----------|---------|
| Ring 0 | [e.g. ISO 27001 A.x.x] | [control description — replace with actual] |
| Ring 1 | [e.g. BCBS 239 Principle N] | [control description — replace with actual] |
| Ring 2 | [e.g. SBV Circular 09/2020 Article N] | [control description] ⚠️ (working summary — pending Legal review) |
```

- [ ] **Step 2: Add When to Use / When Not to Use and Key Takeaway placeholders**

The stub template should also carry placeholder headings so authors know every required section. After the `## NFR Hooks` section and before `## References`, add:

```markdown
## When to Use

- TBD — list the scenarios where this pattern/principle is the right choice.

## When Not to Use

- TBD — list the anti-patterns or scenarios where this document does not apply.

## Key Takeaway

TBD — one sentence that captures the essential insight of this document.
```

- [ ] **Step 3: Verify compliance check now passes**

```bash
python3 scripts/check-compliance-rows.py 2>&1
```
Expected: `Done: checked=84, failures=0`

- [ ] **Step 4: Commit**

```bash
git add knowledge-base/templates/stub-doc-template.md
git commit -m "fix(templates): TPL-003 — rename Compliance Hooks to Compliance Mapping heading"
```

---

## Task 2: Add When to Use/Not Use to PRIN-008 (Defense-in-Depth)

**Files:**
- Modify: `knowledge-base/principles/defense-in-depth.md` (before `## References`, line ~476)

- [ ] **Step 1: Insert the section before References**

In `knowledge-base/principles/defense-in-depth.md`, insert the following immediately before the `## References` heading:

```markdown
## When to Use

- **Tier 0 and Tier 1 internet-facing services**: every service that receives external traffic (payment APIs, mobile BFFs, open-banking endpoints) must implement all six layers.
- **PCI Cardholder Data Environment (CDE)**: any system that stores, processes, or transmits card data must apply Layer 5 (data-layer tokenisation) and Layer 6 (tamper-evident audit log) as hard requirements.
- **Systems handling personal data under Decree 13/2023**: PII stores require Layer 3 (ABAC consent enforcement) and Layer 5 (field-level masking) as mandatory controls.
- **Multi-tenant SaaS integrations**: where a single service hosts data for multiple tenants, Layer 3 ABAC must include `tenant_id` as a mandatory context attribute.

## When Not to Use

- **Isolated developer tooling with no production data**: internal CLI tools, seed-data generators, and local dev environments operated by vetted engineers need not implement the full six-layer stack. Apply Layer 1 (network restriction to corporate VPN) and Layer 3 (SSO) only.
- **Batch jobs with no external interface**: nightly ETL jobs that run inside a VPC with no inbound traffic surface require Layers 1, 5, and 6 but not Layers 2–4 (no inter-service mTLS or application-layer auth needed).
- **As a substitute for threat modelling**: Defense-in-Depth is a structural principle, not a threat-modelling exercise. Run STRIDE against each layer separately; this principle tells you where to place controls, not which controls to choose.

```

- [ ] **Step 2: Run mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/principles/defense-in-depth.md
```
Expected: `OK knowledge-base/principles/defense-in-depth.md (N block(s))`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/principles/defense-in-depth.md
git commit -m "feat(principles): PRIN-008 add When to Use/Not Use sections"
```

---

## Task 3: Add When to Use/Not Use to PRIN-009 (Observability-First)

**Files:**
- Modify: `knowledge-base/principles/observability-first.md` (before `## References`, line ~484)

- [ ] **Step 1: Insert the section before References**

In `knowledge-base/principles/observability-first.md`, insert the following immediately before the `## References` heading:

```markdown
## When to Use

- **All production services, Tier 0–3**: every service that runs in production must emit the four Golden Signals (Latency, Traffic, Errors, Saturation) from day one. "Add it later" is explicitly disallowed by this principle — observability is a day-1 requirement.
- **Pre-GA staging environments**: staging must be instrumented identically to production; discrepancies between staging and production dashboards indicate instrumentation drift and must be resolved before GA.
- **During incident response**: any service without golden-signal dashboards delays incident resolution; this principle pre-empts that situation by requiring instrumentation before the first production deployment.
- **Greenfield and brownfield services**: both new services and services undergoing modernisation (Strangler Fig, ACL extraction) must implement OTEL tracing, Micrometer metrics, and structured logging as the first engineering tasks — before feature logic.

## When Not to Use

- **One-off administrative scripts that run once and are discarded**: a migration script run once by an engineer does not need a Prometheus metric. Add a progress log line to stdout; that is sufficient.
- **Developer-local tooling with no production footprint**: local mock servers, seed-data generators, and integration-test helpers that never run in a shared environment are exempt from full instrumentation.
- **As a replacement for alerting design**: Observability-First ensures the signals exist; the SRE team still needs to design SLOs and alert thresholds per NFR-005. Metrics without alerts are necessary but not sufficient.

```

- [ ] **Step 2: Run mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/principles/observability-first.md
```
Expected: `OK knowledge-base/principles/observability-first.md (N block(s))`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/principles/observability-first.md
git commit -m "feat(principles): PRIN-009 add When to Use/Not Use sections"
```

---

## Task 4: Add When to Use/Not Use to PRIN-011 (Least-Privilege)

**Files:**
- Modify: `knowledge-base/principles/least-privilege.md` (before `## References`, line ~466)

- [ ] **Step 1: Insert the section before References**

In `knowledge-base/principles/least-privilege.md`, insert the following immediately before the `## References` heading:

```markdown
## When to Use

- **All production IAM roles and service accounts**: every AWS IAM role, Kubernetes ServiceAccount, and database user must be scoped to the minimum permissions required for its specific function. No wildcard (`*`) actions in production IAM policies.
- **Database access**: each microservice has its own database user with SELECT/INSERT/UPDATE on its own schema only; no service uses a DBA-level credential for routine operations.
- **Kubernetes RBAC**: each service's ServiceAccount has a Role bound only to the namespace resources it needs; ClusterRoles are reserved for infrastructure controllers.
- **HashiCorp Vault policies**: each service's AppRole policy grants read access only to the secret paths it explicitly needs; no service has `path "*"` policy.
- **Human access in production**: production console access is time-limited (JIT via Vault) with dual-approval for Tier 0 systems; no standing admin access to production.

## When Not to Use

- **Developer sandbox environments with no customer data**: in isolated personal sandboxes used for experimentation, broader permissions accelerate development velocity without increasing risk. Broad permissions must never reach production.
- **As a substitute for network segmentation**: Least-Privilege governs identity and permission scope; it does not replace network segmentation (PRIN-008 Defense-in-Depth Layer 1). Both controls are required in Tier 0/1.
- **Applied retroactively without impact analysis**: restricting an existing service account's permissions in production without first running `simulate-policy` (AWS) or equivalent is a change risk. Always use the pre-flight impact-analysis step in the runbook before tightening existing policies.

```

- [ ] **Step 2: Run mermaid lint**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/principles/least-privilege.md
```
Expected: `OK knowledge-base/principles/least-privilege.md (N block(s))`

- [ ] **Step 3: Commit**

```bash
git add knowledge-base/principles/least-privilege.md
git commit -m "feat(principles): PRIN-011 add When to Use/Not Use sections"
```

---

## Wave 4A Gate — EIP-001–012 (12 docs)

**Files:**
- `knowledge-base/patterns/eip/message-channel.md` (EIP-001)
- `knowledge-base/patterns/eip/point-to-point-channel.md` (EIP-002)
- `knowledge-base/patterns/eip/publish-subscribe-channel.md` (EIP-003)
- `knowledge-base/patterns/eip/message-router.md` (EIP-004)
- `knowledge-base/patterns/eip/content-based-router.md` (EIP-005)
- `knowledge-base/patterns/eip/message-translator.md` (EIP-006)
- `knowledge-base/patterns/eip/content-enricher.md` (EIP-007)
- `knowledge-base/patterns/eip/content-filter.md` (EIP-008)
- `knowledge-base/patterns/eip/claim-check.md` (EIP-009)
- `knowledge-base/patterns/eip/normalizer.md` (EIP-010)
- `knowledge-base/patterns/eip/aggregator.md` (EIP-011)
- `knowledge-base/patterns/eip/splitter.md` (EIP-012)

- [ ] **Step 1: Batch mermaid lint — EIP-001–012**

```bash
for f in \
  knowledge-base/patterns/eip/message-channel.md \
  knowledge-base/patterns/eip/point-to-point-channel.md \
  knowledge-base/patterns/eip/publish-subscribe-channel.md \
  knowledge-base/patterns/eip/message-router.md \
  knowledge-base/patterns/eip/content-based-router.md \
  knowledge-base/patterns/eip/message-translator.md \
  knowledge-base/patterns/eip/content-enricher.md \
  knowledge-base/patterns/eip/content-filter.md \
  knowledge-base/patterns/eip/claim-check.md \
  knowledge-base/patterns/eip/normalizer.md \
  knowledge-base/patterns/eip/aggregator.md \
  knowledge-base/patterns/eip/splitter.md; do
  bash scripts/mermaid-lint-doc.sh "$f" || echo "FAIL: $f"
done
```
Expected: 12 files, 0 FAIL lines. If any fail, fix the Mermaid syntax in that file and re-run.

- [ ] **Step 2: Update catalog — EIP-001–012 → Draft**

In `governance/standards/enterprise-architecture-catalog.md`, change the Status column from `Proposed` to `Draft` for EIP-001 through EIP-012.

```bash
grep -E "EIP-00[1-9]|EIP-01[012]" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |"
```
Expected: 12 lines.

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md
git commit -m "chore(catalog): Wave 4A gate — EIP-001–012 promoted to Draft"
```

---

## Wave 4B Gate — EIP-013–023 (11 docs)

**Files:**
- `knowledge-base/patterns/eip/resequencer.md` (EIP-013)
- `knowledge-base/patterns/eip/composed-message-processor.md` (EIP-014)
- `knowledge-base/patterns/eip/scatter-gather.md` (EIP-015)
- `knowledge-base/patterns/eip/routing-slip.md` (EIP-016)
- `knowledge-base/patterns/eip/process-manager.md` (EIP-017)
- `knowledge-base/patterns/eip/message-store.md` (EIP-018)
- `knowledge-base/patterns/eip/smart-proxy.md` (EIP-019)
- `knowledge-base/patterns/eip/test-message.md` (EIP-020)
- `knowledge-base/patterns/eip/channel-purger.md` (EIP-021)
- `knowledge-base/patterns/eip/durable-subscriber.md` (EIP-022)
- `knowledge-base/patterns/eip/guaranteed-delivery.md` (EIP-023)

- [ ] **Step 1: Batch mermaid lint — EIP-013–023**

```bash
for f in \
  knowledge-base/patterns/eip/resequencer.md \
  knowledge-base/patterns/eip/composed-message-processor.md \
  knowledge-base/patterns/eip/scatter-gather.md \
  knowledge-base/patterns/eip/routing-slip.md \
  knowledge-base/patterns/eip/process-manager.md \
  knowledge-base/patterns/eip/message-store.md \
  knowledge-base/patterns/eip/smart-proxy.md \
  knowledge-base/patterns/eip/test-message.md \
  knowledge-base/patterns/eip/channel-purger.md \
  knowledge-base/patterns/eip/durable-subscriber.md \
  knowledge-base/patterns/eip/guaranteed-delivery.md; do
  bash scripts/mermaid-lint-doc.sh "$f" || echo "FAIL: $f"
done
```
Expected: 11 files, 0 FAIL lines.

- [ ] **Step 2: Update catalog — EIP-013–023 → Draft**

In `governance/standards/enterprise-architecture-catalog.md`, change the Status column from `Proposed` to `Draft` for EIP-013 through EIP-023.

```bash
grep -E "EIP-01[3-9]|EIP-02[0123]" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |"
```
Expected: 11 lines.

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md
git commit -m "chore(catalog): Wave 4B gate — EIP-013–023 promoted to Draft"
```

---

## Wave 4C Gate — Architecture Principles (6 docs)

**Files:**
- `knowledge-base/principles/defense-in-depth.md` (PRIN-008) — gap-filled in Task 2
- `knowledge-base/principles/observability-first.md` (PRIN-009) — gap-filled in Task 3
- `knowledge-base/principles/fail-safe-defaults.md` (PRIN-010)
- `knowledge-base/principles/least-privilege.md` (PRIN-011) — gap-filled in Task 4
- `knowledge-base/principles/async-by-default.md` (PRIN-012)
- `knowledge-base/principles/modular-monolith-preference.md` (PRIN-013)

- [ ] **Step 1: Batch mermaid lint — PRIN-008–013**

```bash
for f in \
  knowledge-base/principles/defense-in-depth.md \
  knowledge-base/principles/observability-first.md \
  knowledge-base/principles/fail-safe-defaults.md \
  knowledge-base/principles/least-privilege.md \
  knowledge-base/principles/async-by-default.md \
  knowledge-base/principles/modular-monolith-preference.md; do
  bash scripts/mermaid-lint-doc.sh "$f" || echo "FAIL: $f"
done
```
Expected: 6 files, 0 FAIL lines.

- [ ] **Step 2: Update catalog — PRIN-008–013 → Draft**

In `governance/standards/enterprise-architecture-catalog.md`, change the Status column from `Proposed` to `Draft` for PRIN-008 through PRIN-013.

```bash
grep -E "PRIN-0(08|09|10|11|12|13)" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |"
```
Expected: 6 lines.

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md
git commit -m "chore(catalog): Wave 4C gate — PRIN-008–013 promoted to Draft"
```

---

## Wave 4D Gate — Resilience Patterns (7 docs)

**Files:**
- `knowledge-base/patterns/resilience/timeout-budget.md` (RES-006)
- `knowledge-base/patterns/resilience/fallback-strategies.md` (RES-007)
- `knowledge-base/patterns/resilience/throttling-rate-limiting.md` (RES-008)
- `knowledge-base/patterns/resilience/load-shedding.md` (RES-009)
- `knowledge-base/patterns/resilience/leader-election.md` (RES-010)
- `knowledge-base/patterns/resilience/queue-based-load-levelling.md` (RES-011)
- `knowledge-base/patterns/resilience/health-check-aggregation.md` (RES-012)

- [ ] **Step 1: Batch mermaid lint — RES-006–012**

```bash
for f in \
  knowledge-base/patterns/resilience/timeout-budget.md \
  knowledge-base/patterns/resilience/fallback-strategies.md \
  knowledge-base/patterns/resilience/throttling-rate-limiting.md \
  knowledge-base/patterns/resilience/load-shedding.md \
  knowledge-base/patterns/resilience/leader-election.md \
  knowledge-base/patterns/resilience/queue-based-load-levelling.md \
  knowledge-base/patterns/resilience/health-check-aggregation.md; do
  bash scripts/mermaid-lint-doc.sh "$f" || echo "FAIL: $f"
done
```
Expected: 7 files, 0 FAIL lines.

- [ ] **Step 2: Update catalog — RES-006–012 → Draft**

In `governance/standards/enterprise-architecture-catalog.md`, change the Status column from `Proposed` to `Draft` for RES-006 through RES-012.

```bash
grep -E "RES-0(06|07|08|09|10|11|12)" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |"
```
Expected: 7 lines.

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md
git commit -m "chore(catalog): Wave 4D gate — RES-006–012 promoted to Draft"
```

---

## Wave 4E Gate — Best Practices + NFR + Templates (12 docs)

**Files:**
- `knowledge-base/best-practices/capacity-planning.md` (BP-006)
- `knowledge-base/best-practices/golden-signals-sre.md` (BP-007)
- `knowledge-base/best-practices/error-budgets.md` (BP-008)
- `knowledge-base/best-practices/runbook-authoring.md` (BP-009)
- `knowledge-base/best-practices/incident-postmortem.md` (BP-010)
- `knowledge-base/best-practices/blameless-culture.md` (BP-011)
- `knowledge-base/nfr/capacity-planning-model.md` (NFR-003)
- `knowledge-base/nfr/throughput-model.md` (NFR-004)
- `knowledge-base/nfr/error-budget-policy.md` (NFR-005)
- `knowledge-base/templates/pattern-doc-template.md` (TPL-002)
- `knowledge-base/templates/stub-doc-template.md` (TPL-003) — fixed in Task 1
- `knowledge-base/templates/ref-arch-doc-template.md` (TPL-004)

- [ ] **Step 1: Batch mermaid lint — BP + NFR + TPL**

```bash
for f in \
  knowledge-base/best-practices/capacity-planning.md \
  knowledge-base/best-practices/golden-signals-sre.md \
  knowledge-base/best-practices/error-budgets.md \
  knowledge-base/best-practices/runbook-authoring.md \
  knowledge-base/best-practices/incident-postmortem.md \
  knowledge-base/best-practices/blameless-culture.md \
  knowledge-base/nfr/capacity-planning-model.md \
  knowledge-base/nfr/throughput-model.md \
  knowledge-base/nfr/error-budget-policy.md \
  knowledge-base/templates/pattern-doc-template.md \
  knowledge-base/templates/stub-doc-template.md \
  knowledge-base/templates/ref-arch-doc-template.md; do
  bash scripts/mermaid-lint-doc.sh "$f" || echo "FAIL: $f"
done
```
Expected: 12 files, 0 FAIL lines.

- [ ] **Step 2: Full compliance check — all 48 Wave 4 files**

```bash
python3 scripts/check-compliance-rows.py 2>&1
```
Expected: `Done: checked=84, failures=0`

If any failure appears, fix the specific file before continuing.

- [ ] **Step 3: Update catalog — BP-006–011, NFR-003–005, TPL-002–004 → Draft**

In `governance/standards/enterprise-architecture-catalog.md`, change the Status column from `Proposed` to `Draft` for all 12 entries.

```bash
grep -E "(BP-0(06|07|08|09|10|11)|NFR-00[345]|TPL-00[234])" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |"
```
Expected: 12 lines.

- [ ] **Step 4: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md
git commit -m "chore(catalog): Wave 4E gate — BP-006–011 + NFR-003–005 + TPL-002–004 promoted to Draft"
```

---

## Final Verification — All 48 Wave 4 docs

- [ ] **Step 1: Confirm zero Proposed entries for Wave 4 IDs**

```bash
grep -E "(EIP-0(0[1-9]|1[0-9]|2[0-3])|PRIN-0(08|09|10|11|12|13)|RES-0(06|07|08|09|10|11|12)|BP-0(06|07|08|09|10|11)|NFR-00[345]|TPL-00[234])" \
  governance/standards/enterprise-architecture-catalog.md | grep "| Proposed |"
```
Expected: 0 lines.

- [ ] **Step 2: Confirm all 48 are now Draft**

```bash
grep -E "(EIP-0(0[1-9]|1[0-9]|2[0-3])|PRIN-0(08|09|10|11|12|13)|RES-0(06|07|08|09|10|11|12)|BP-0(06|07|08|09|10|11)|NFR-00[345]|TPL-00[234])" \
  governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | wc -l
```
Expected: 48

- [ ] **Step 3: Final compliance check**

```bash
python3 scripts/check-compliance-rows.py 2>&1
```
Expected: `Done: checked=N, failures=0`

- [ ] **Step 4: Commit the plan file**

```bash
git add docs/superpowers/plans/2026-05-10-wave-4-stub-promotion.md
git commit -m "docs(plans): Wave 4 stub-promotion plan — 48 Wave 1 docs to Draft"
```
