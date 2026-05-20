# Wave 7 — Draft → Approved Promotion Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Promote all 55 Wave-5 Draft documents to Approved status by running a two-stage gate: automated Mermaid lint + compliance check (Stage 1), then a per-doc self-review checklist with targeted in-place fixes (Stage 2), followed by a catalog promotion commit per sub-wave.

**Architecture:** Same two-stage gate pattern as Wave 6. Wave-5 docs were authored with the full 15-section template and are expected to be in better shape than Wave-4 stubs — most gaps will be STRIDE labelling, `Alert:` format, and Ring 2 `⚠️` markers rather than missing sections. Each sub-wave follows: Stage 1 automated checks → Stage 2 self-review + fixes → catalog promotion commit.

**Tech Stack:** Markdown, `bash scripts/mermaid-lint-doc.sh`, `python3 scripts/check-compliance-rows.py`, `grep`, `git`

---

## Sub-Wave Map

| Wave | IDs | Count |
|------|-----|-------|
| 7A | BSP-001–005 | 5 |
| 7B | INT-005–009 | 5 |
| 7C | MOB-001–006 | 6 |
| 7D | FE-001–006 | 6 |
| 7E | COMP-002–008 | 7 |
| 7F | SEC-006–013 | 8 |
| 7G | REF-005–012 | 8 |
| 7H | DATA-004–013 | 10 |
| **Total** | | **55** |

---

## Self-Review Checklist (applied in every Stage 2)

Each document must pass **all 7 items** before promotion:

1. **≥15 sections** — `grep -c "^## " <file>` returns ≥ 15
2. **No placeholder text** — no "TBD", "TODO", "placeholder" in body
3. **≥1 measurable NFR threshold** — e.g. `p99 < 100 ms`, `throughput ≥ 5 000 TPS`, `≥ 99.9%`
4. **3-ring Compliance Mapping** — Ring 2 row mentions SBV / Decree 13 and ends with `⚠️ (working summary — pending Legal review)`
5. **≥2 STRIDE-labelled threats** — each named with explicit category: `(Tampering)`, `(Spoofing)`, `(Information Disclosure)`, `(Repudiation)`, `(Denial of Service)`, `(Elevation of Privilege)`
6. **Named runbook alert** — at least one line in Operational Runbook formatted as `Alert: SomeName`
7. **Related Patterns non-empty** — Related Patterns section has ≥1 entry

### Diagnostic script (run for each sub-wave)

```bash
for f in <file1> <file2> ...; do
  sections=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  placeholder=$(grep -ilE "TBD|TODO|placeholder" "$f" 2>/dev/null && echo "YES" || echo "no")
  nfr=$(grep -iE "p[0-9]+.*ms|<[0-9]+.*ms|throughput|latency|rps|tps|99th|p99|[0-9]+%" "$f" | grep -c "" 2>/dev/null || echo 0)
  ring2=$(grep -c "Ring 2\|SBV\|Decree 13" "$f" 2>/dev/null || echo 0)
  stride=$(grep -iE "Tampering|Spoofing|Repudiation|Information Disclosure|Denial of Service|Elevation of Privilege" "$f" | grep -c "" 2>/dev/null || echo 0)
  alert=$(grep -c "Alert:" "$f" 2>/dev/null || echo 0)
  echo "$(basename $f .md): sections=$sections placeholder=$placeholder nfr=$nfr ring2=$ring2 stride=$stride alert=$alert"
done
```

Pass criteria: `sections≥15`, `placeholder=no`, `nfr≥1`, `ring2≥1`, `stride≥2`, `alert≥1`

---

## Task 0: Pre-Flight Verification

**Files:** none modified

- [ ] **Step 1: Count Draft docs**

```bash
grep -c "| Draft |" governance/standards/enterprise-architecture-catalog.md
```
Expected: 55

- [ ] **Step 2: Confirm tooling works**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/banking-solutions/double-entry-ledger.md
python3 scripts/check-compliance-rows.py
```
Expected: lint exits 0; compliance reports 0 failures.

- [ ] **Step 3: Confirm clean working tree**

```bash
git status --short
```
Expected: empty output.

---

## Wave 7A — BSP-001–005

### Task 1: Wave 7A — Stage 1 Automated Checks

**Files:** none modified

- [ ] **Step 1: Mermaid lint all BSP files**

```bash
for f in \
  knowledge-base/patterns/banking-solutions/double-entry-ledger.md \
  knowledge-base/patterns/banking-solutions/idempotent-payment-key.md \
  knowledge-base/patterns/banking-solutions/sanction-screening-pipeline.md \
  knowledge-base/patterns/banking-solutions/end-of-day-batch-window.md \
  knowledge-base/patterns/banking-solutions/reversal-and-chargeback.md; do
  result=$(bash scripts/mermaid-lint-doc.sh "$f" 2>&1)
  code=$?
  if [ $code -ne 0 ]; then echo "FAIL: $(basename $f)"; echo "$result" | tail -3; else echo "PASS: $(basename $f)"; fi
done
```
Expected: all PASS.

- [ ] **Step 2: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: 0 failures.

### Task 2: Wave 7A — Stage 2 Self-Review (BSP-001–005)

**Files:** Fix any doc with open items in-place before proceeding.

- [ ] **Step 1: Run diagnostic on all 5 BSP files**

```bash
for f in \
  knowledge-base/patterns/banking-solutions/double-entry-ledger.md \
  knowledge-base/patterns/banking-solutions/idempotent-payment-key.md \
  knowledge-base/patterns/banking-solutions/sanction-screening-pipeline.md \
  knowledge-base/patterns/banking-solutions/end-of-day-batch-window.md \
  knowledge-base/patterns/banking-solutions/reversal-and-chargeback.md; do
  sections=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  placeholder=$(grep -ilE "TBD|TODO|placeholder" "$f" 2>/dev/null && echo "YES" || echo "no")
  nfr=$(grep -iE "p[0-9]+.*ms|<[0-9]+.*ms|throughput|latency|rps|tps|99th|p99|[0-9]+%" "$f" | grep -c "" 2>/dev/null || echo 0)
  ring2=$(grep -c "Ring 2\|SBV\|Decree 13" "$f" 2>/dev/null || echo 0)
  stride=$(grep -iE "Tampering|Spoofing|Repudiation|Information Disclosure|Denial of Service|Elevation of Privilege" "$f" | grep -c "" 2>/dev/null || echo 0)
  alert=$(grep -c "Alert:" "$f" 2>/dev/null || echo 0)
  echo "$(basename $f .md): sections=$sections placeholder=$placeholder nfr=$nfr ring2=$ring2 stride=$stride alert=$alert"
done
```

- [ ] **Step 2: For each file with open items, fix in-place**

Common fixes (apply only what's missing — do not add content that already passes):

**Adding STRIDE labels** — in Threat Model section, add category in parens to each threat:
```markdown
- Duplicate payment submitted by compromised client (Tampering)
- Settlement netting bypass via injected batch entry (Elevation of Privilege)
```

**Adding `Alert:` format** — in Operational Runbook section:
```markdown
**Alert: DoubleEntryImbalanceDetected** — fires when ledger debit/credit totals diverge by > 0.
```

**Fixing Ring 2 row** — Compliance Mapping must have:
```markdown
| Ring 2 | SBV Circular 09/2020; Decree 13/2023 | [requirement] | ⚠️ (working summary — pending Legal review) |
```

- [ ] **Step 3: Re-run diagnostic — confirm all 5 files pass all 7 checklist items**

Re-run the Step 1 diagnostic. All lines must show `sections≥15 placeholder=no nfr≥1 ring2≥1 stride≥2 alert≥1`.

### Task 3: Wave 7A — Promote BSP-001–005 Draft → Approved

**Files:**
- Modify: `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Update catalog for BSP-001–005**

In `governance/standards/enterprise-architecture-catalog.md`, change the 5 BSP rows:
- `| Draft |` → `| Approved |`
- Last Reviewed date → `2026-05-18` (or current date)
- Notes → `Wave 7A — self-review complete`

- [ ] **Step 2: Verify**

```bash
grep "| BSP-0" governance/standards/enterprise-architecture-catalog.md | grep -c "| Approved |"
```
Expected: 5

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md \
  knowledge-base/patterns/banking-solutions/double-entry-ledger.md \
  knowledge-base/patterns/banking-solutions/idempotent-payment-key.md \
  knowledge-base/patterns/banking-solutions/sanction-screening-pipeline.md \
  knowledge-base/patterns/banking-solutions/end-of-day-batch-window.md \
  knowledge-base/patterns/banking-solutions/reversal-and-chargeback.md
git commit -m "feat(catalog): Wave 7A gate — promote BSP-001–005 Draft→Approved"
```

---

## Wave 7B — INT-005–009

### Task 4: Wave 7B — Stage 1 Automated Checks

**Files:** none modified

- [ ] **Step 1: Mermaid lint all INT files**

```bash
for f in \
  knowledge-base/patterns/integration/anti-corruption-layer.md \
  knowledge-base/patterns/integration/strangler-fig.md \
  knowledge-base/patterns/integration/sidecar-ambassador.md \
  knowledge-base/patterns/integration/backend-for-frontend-routing.md \
  knowledge-base/patterns/integration/content-based-router.md; do
  result=$(bash scripts/mermaid-lint-doc.sh "$f" 2>&1)
  code=$?
  if [ $code -ne 0 ]; then echo "FAIL: $(basename $f)"; echo "$result" | tail -3; else echo "PASS: $(basename $f)"; fi
done
```
Expected: all PASS.

- [ ] **Step 2: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: 0 failures.

### Task 5: Wave 7B — Stage 2 Self-Review (INT-005–009)

**Files:** Fix any doc with open items in-place.

- [ ] **Step 1: Run diagnostic on all 5 INT files**

```bash
for f in \
  knowledge-base/patterns/integration/anti-corruption-layer.md \
  knowledge-base/patterns/integration/strangler-fig.md \
  knowledge-base/patterns/integration/sidecar-ambassador.md \
  knowledge-base/patterns/integration/backend-for-frontend-routing.md \
  knowledge-base/patterns/integration/content-based-router.md; do
  sections=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  placeholder=$(grep -ilE "TBD|TODO|placeholder" "$f" 2>/dev/null && echo "YES" || echo "no")
  nfr=$(grep -iE "p[0-9]+.*ms|<[0-9]+.*ms|throughput|latency|rps|tps|99th|p99|[0-9]+%" "$f" | grep -c "" 2>/dev/null || echo 0)
  ring2=$(grep -c "Ring 2\|SBV\|Decree 13" "$f" 2>/dev/null || echo 0)
  stride=$(grep -iE "Tampering|Spoofing|Repudiation|Information Disclosure|Denial of Service|Elevation of Privilege" "$f" | grep -c "" 2>/dev/null || echo 0)
  alert=$(grep -c "Alert:" "$f" 2>/dev/null || echo 0)
  echo "$(basename $f .md): sections=$sections placeholder=$placeholder nfr=$nfr ring2=$ring2 stride=$stride alert=$alert"
done
```

- [ ] **Step 2: Fix open items; re-run diagnostic to confirm all 5 pass**

Apply the same fix patterns as Task 2 Step 2 for any items that fail.

### Task 6: Wave 7B — Promote INT-005–009 Draft → Approved

**Files:**
- Modify: `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Update catalog for INT-005–009** — change `| Draft |` → `| Approved |`, date → current, notes → `Wave 7B — self-review complete`

- [ ] **Step 2: Verify**

```bash
grep "| INT-00[5-9]" governance/standards/enterprise-architecture-catalog.md | grep -c "| Approved |"
```
Expected: 5

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md \
  knowledge-base/patterns/integration/anti-corruption-layer.md \
  knowledge-base/patterns/integration/strangler-fig.md \
  knowledge-base/patterns/integration/sidecar-ambassador.md \
  knowledge-base/patterns/integration/backend-for-frontend-routing.md \
  knowledge-base/patterns/integration/content-based-router.md
git commit -m "feat(catalog): Wave 7B gate — promote INT-005–009 Draft→Approved"
```

---

## Wave 7C — MOB-001–006

### Task 7: Wave 7C — Stage 1 Automated Checks

**Files:** none modified

- [ ] **Step 1: Mermaid lint all MOB files**

```bash
for f in \
  knowledge-base/patterns/mobile/mobile-offline-queue.md \
  knowledge-base/patterns/mobile/mobile-secure-storage.md \
  knowledge-base/patterns/mobile/mobile-biometric-auth.md \
  knowledge-base/patterns/mobile/mobile-push-notification-secure.md \
  knowledge-base/patterns/mobile/mobile-deep-link-attestation.md \
  knowledge-base/patterns/mobile/mobile-force-upgrade.md; do
  result=$(bash scripts/mermaid-lint-doc.sh "$f" 2>&1)
  code=$?
  if [ $code -ne 0 ]; then echo "FAIL: $(basename $f)"; echo "$result" | tail -3; else echo "PASS: $(basename $f)"; fi
done
```
Expected: all PASS.

- [ ] **Step 2: Compliance check** — `python3 scripts/check-compliance-rows.py` Expected: 0 failures.

### Task 8: Wave 7C — Stage 2 Self-Review (MOB-001–006)

**Files:** Fix any doc with open items in-place.

- [ ] **Step 1: Run diagnostic on all 6 MOB files**

```bash
for f in \
  knowledge-base/patterns/mobile/mobile-offline-queue.md \
  knowledge-base/patterns/mobile/mobile-secure-storage.md \
  knowledge-base/patterns/mobile/mobile-biometric-auth.md \
  knowledge-base/patterns/mobile/mobile-push-notification-secure.md \
  knowledge-base/patterns/mobile/mobile-deep-link-attestation.md \
  knowledge-base/patterns/mobile/mobile-force-upgrade.md; do
  sections=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  placeholder=$(grep -ilE "TBD|TODO|placeholder" "$f" 2>/dev/null && echo "YES" || echo "no")
  nfr=$(grep -iE "p[0-9]+.*ms|<[0-9]+.*ms|throughput|latency|rps|tps|99th|p99|[0-9]+%" "$f" | grep -c "" 2>/dev/null || echo 0)
  ring2=$(grep -c "Ring 2\|SBV\|Decree 13" "$f" 2>/dev/null || echo 0)
  stride=$(grep -iE "Tampering|Spoofing|Repudiation|Information Disclosure|Denial of Service|Elevation of Privilege" "$f" | grep -c "" 2>/dev/null || echo 0)
  alert=$(grep -c "Alert:" "$f" 2>/dev/null || echo 0)
  echo "$(basename $f .md): sections=$sections placeholder=$placeholder nfr=$nfr ring2=$ring2 stride=$stride alert=$alert"
done
```

- [ ] **Step 2: Fix open items; re-run diagnostic to confirm all 6 pass**

### Task 9: Wave 7C — Promote MOB-001–006 Draft → Approved

**Files:**
- Modify: `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Update catalog for MOB-001–006** — change `| Draft |` → `| Approved |`, date → current, notes → `Wave 7C — self-review complete`

- [ ] **Step 2: Verify**

```bash
grep "| MOB-0" governance/standards/enterprise-architecture-catalog.md | grep -c "| Approved |"
```
Expected: 6

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md \
  knowledge-base/patterns/mobile/mobile-offline-queue.md \
  knowledge-base/patterns/mobile/mobile-secure-storage.md \
  knowledge-base/patterns/mobile/mobile-biometric-auth.md \
  knowledge-base/patterns/mobile/mobile-push-notification-secure.md \
  knowledge-base/patterns/mobile/mobile-deep-link-attestation.md \
  knowledge-base/patterns/mobile/mobile-force-upgrade.md
git commit -m "feat(catalog): Wave 7C gate — promote MOB-001–006 Draft→Approved"
```

---

## Wave 7D — FE-001–006

### Task 10: Wave 7D — Stage 1 Automated Checks

**Files:** none modified

- [ ] **Step 1: Mermaid lint all FE files**

```bash
for f in \
  knowledge-base/patterns/frontend/web-performance-budgets.md \
  knowledge-base/patterns/frontend/web-resilience-offline-first.md \
  knowledge-base/patterns/frontend/web-csp-hardening.md \
  knowledge-base/patterns/frontend/web-feature-flags.md \
  knowledge-base/patterns/frontend/web-error-boundary.md \
  knowledge-base/patterns/frontend/web-i18n-rtl.md; do
  result=$(bash scripts/mermaid-lint-doc.sh "$f" 2>&1)
  code=$?
  if [ $code -ne 0 ]; then echo "FAIL: $(basename $f)"; echo "$result" | tail -3; else echo "PASS: $(basename $f)"; fi
done
```
Expected: all PASS.

- [ ] **Step 2: Compliance check** — `python3 scripts/check-compliance-rows.py` Expected: 0 failures.

### Task 11: Wave 7D — Stage 2 Self-Review (FE-001–006)

**Files:** Fix any doc with open items in-place.

- [ ] **Step 1: Run diagnostic on all 6 FE files**

```bash
for f in \
  knowledge-base/patterns/frontend/web-performance-budgets.md \
  knowledge-base/patterns/frontend/web-resilience-offline-first.md \
  knowledge-base/patterns/frontend/web-csp-hardening.md \
  knowledge-base/patterns/frontend/web-feature-flags.md \
  knowledge-base/patterns/frontend/web-error-boundary.md \
  knowledge-base/patterns/frontend/web-i18n-rtl.md; do
  sections=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  placeholder=$(grep -ilE "TBD|TODO|placeholder" "$f" 2>/dev/null && echo "YES" || echo "no")
  nfr=$(grep -iE "p[0-9]+.*ms|<[0-9]+.*ms|throughput|latency|rps|tps|99th|p99|[0-9]+%|LCP|INP|CLS|FID" "$f" | grep -c "" 2>/dev/null || echo 0)
  ring2=$(grep -c "Ring 2\|SBV\|Decree 13" "$f" 2>/dev/null || echo 0)
  stride=$(grep -iE "Tampering|Spoofing|Repudiation|Information Disclosure|Denial of Service|Elevation of Privilege" "$f" | grep -c "" 2>/dev/null || echo 0)
  alert=$(grep -c "Alert:" "$f" 2>/dev/null || echo 0)
  echo "$(basename $f .md): sections=$sections placeholder=$placeholder nfr=$nfr ring2=$ring2 stride=$stride alert=$alert"
done
```

Note: The FE NFR grep also checks LCP/INP/CLS/FID (Core Web Vitals metrics) which are measurable thresholds specific to frontend docs.

- [ ] **Step 2: Fix open items; re-run diagnostic to confirm all 6 pass**

### Task 12: Wave 7D — Promote FE-001–006 Draft → Approved

**Files:**
- Modify: `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Update catalog for FE-001–006** — change `| Draft |` → `| Approved |`, date → current, notes → `Wave 7D — self-review complete`

- [ ] **Step 2: Verify**

```bash
grep "| FE-0" governance/standards/enterprise-architecture-catalog.md | grep -c "| Approved |"
```
Expected: 6

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md \
  knowledge-base/patterns/frontend/web-performance-budgets.md \
  knowledge-base/patterns/frontend/web-resilience-offline-first.md \
  knowledge-base/patterns/frontend/web-csp-hardening.md \
  knowledge-base/patterns/frontend/web-feature-flags.md \
  knowledge-base/patterns/frontend/web-error-boundary.md \
  knowledge-base/patterns/frontend/web-i18n-rtl.md
git commit -m "feat(catalog): Wave 7D gate — promote FE-001–006 Draft→Approved"
```

---

## Wave 7E — COMP-002–008

### Task 13: Wave 7E — Stage 1 Automated Checks

**Files:** none modified

- [ ] **Step 1: Mermaid lint all COMP files**

```bash
for f in \
  knowledge-base/compliance/sbv-circular-09-2020.md \
  knowledge-base/compliance/decree-13-2023-personal-data.md \
  knowledge-base/compliance/pci-dss-4-0.md \
  knowledge-base/compliance/basel-bcbs-239.md \
  knowledge-base/compliance/basel-bcbs-230.md \
  knowledge-base/compliance/iso-20022-messaging.md \
  knowledge-base/compliance/swift-csp-2024.md; do
  result=$(bash scripts/mermaid-lint-doc.sh "$f" 2>&1)
  code=$?
  if [ $code -ne 0 ]; then echo "FAIL: $(basename $f)"; echo "$result" | tail -3; else echo "PASS: $(basename $f)"; fi
done
```
Expected: all PASS.

- [ ] **Step 2: Compliance check** — `python3 scripts/check-compliance-rows.py` Expected: 0 failures.

### Task 14: Wave 7E — Stage 2 Self-Review (COMP-002–008)

**Files:** Fix any doc with open items in-place.

- [ ] **Step 1: Run diagnostic on all 7 COMP files**

```bash
for f in \
  knowledge-base/compliance/sbv-circular-09-2020.md \
  knowledge-base/compliance/decree-13-2023-personal-data.md \
  knowledge-base/compliance/pci-dss-4-0.md \
  knowledge-base/compliance/basel-bcbs-239.md \
  knowledge-base/compliance/basel-bcbs-230.md \
  knowledge-base/compliance/iso-20022-messaging.md \
  knowledge-base/compliance/swift-csp-2024.md; do
  sections=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  placeholder=$(grep -ilE "TBD|TODO|placeholder" "$f" 2>/dev/null && echo "YES" || echo "no")
  nfr=$(grep -iE "p[0-9]+.*ms|<[0-9]+.*ms|throughput|latency|rps|tps|99th|p99|[0-9]+%|RTO|RPO" "$f" | grep -c "" 2>/dev/null || echo 0)
  ring2=$(grep -c "Ring 2\|SBV\|Decree 13" "$f" 2>/dev/null || echo 0)
  stride=$(grep -iE "Tampering|Spoofing|Repudiation|Information Disclosure|Denial of Service|Elevation of Privilege" "$f" | grep -c "" 2>/dev/null || echo 0)
  alert=$(grep -c "Alert:" "$f" 2>/dev/null || echo 0)
  echo "$(basename $f .md): sections=$sections placeholder=$placeholder nfr=$nfr ring2=$ring2 stride=$stride alert=$alert"
done
```

- [ ] **Step 2: Fix open items; re-run diagnostic to confirm all 7 pass**

### Task 15: Wave 7E — Promote COMP-002–008 Draft → Approved

**Files:**
- Modify: `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Update catalog for COMP-002–008** — change `| Draft |` → `| Approved |`, date → current, notes → `Wave 7E — self-review complete`

- [ ] **Step 2: Verify**

```bash
grep "| COMP-0" governance/standards/enterprise-architecture-catalog.md | grep -c "| Approved |"
```
Expected: 9 (COMP-001 already Approved + 8 new = 9)

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md \
  knowledge-base/compliance/sbv-circular-09-2020.md \
  knowledge-base/compliance/decree-13-2023-personal-data.md \
  knowledge-base/compliance/pci-dss-4-0.md \
  knowledge-base/compliance/basel-bcbs-239.md \
  knowledge-base/compliance/basel-bcbs-230.md \
  knowledge-base/compliance/iso-20022-messaging.md \
  knowledge-base/compliance/swift-csp-2024.md
git commit -m "feat(catalog): Wave 7E gate — promote COMP-002–008 Draft→Approved"
```

---

## Wave 7F — SEC-006–013

### Task 16: Wave 7F — Stage 1 Automated Checks

**Files:** none modified

- [ ] **Step 1: Mermaid lint all SEC files**

```bash
for f in \
  knowledge-base/patterns/security/jwt-best-practices.md \
  knowledge-base/patterns/security/secrets-rotation.md \
  knowledge-base/patterns/security/data-masking.md \
  knowledge-base/patterns/security/fraud-signal-collection.md \
  knowledge-base/patterns/security/attribute-based-access-control.md \
  knowledge-base/patterns/security/session-revocation.md \
  knowledge-base/patterns/security/audit-logging-tamper-evident.md \
  knowledge-base/patterns/security/pii-tokenization-format-preserving.md; do
  result=$(bash scripts/mermaid-lint-doc.sh "$f" 2>&1)
  code=$?
  if [ $code -ne 0 ]; then echo "FAIL: $(basename $f)"; echo "$result" | tail -3; else echo "PASS: $(basename $f)"; fi
done
```
Expected: all PASS.

- [ ] **Step 2: Compliance check** — `python3 scripts/check-compliance-rows.py` Expected: 0 failures.

### Task 17: Wave 7F — Stage 2 Self-Review (SEC-006–013)

**Files:** Fix any doc with open items in-place.

- [ ] **Step 1: Run diagnostic on all 8 SEC files**

```bash
for f in \
  knowledge-base/patterns/security/jwt-best-practices.md \
  knowledge-base/patterns/security/secrets-rotation.md \
  knowledge-base/patterns/security/data-masking.md \
  knowledge-base/patterns/security/fraud-signal-collection.md \
  knowledge-base/patterns/security/attribute-based-access-control.md \
  knowledge-base/patterns/security/session-revocation.md \
  knowledge-base/patterns/security/audit-logging-tamper-evident.md \
  knowledge-base/patterns/security/pii-tokenization-format-preserving.md; do
  sections=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  placeholder=$(grep -ilE "TBD|TODO|placeholder" "$f" 2>/dev/null && echo "YES" || echo "no")
  nfr=$(grep -iE "p[0-9]+.*ms|<[0-9]+.*ms|throughput|latency|rps|tps|99th|p99|[0-9]+%" "$f" | grep -c "" 2>/dev/null || echo 0)
  ring2=$(grep -c "Ring 2\|SBV\|Decree 13" "$f" 2>/dev/null || echo 0)
  stride=$(grep -iE "Tampering|Spoofing|Repudiation|Information Disclosure|Denial of Service|Elevation of Privilege" "$f" | grep -c "" 2>/dev/null || echo 0)
  alert=$(grep -c "Alert:" "$f" 2>/dev/null || echo 0)
  echo "$(basename $f .md): sections=$sections placeholder=$placeholder nfr=$nfr ring2=$ring2 stride=$stride alert=$alert"
done
```

- [ ] **Step 2: Fix open items; re-run diagnostic to confirm all 8 pass**

### Task 18: Wave 7F — Promote SEC-006–013 Draft → Approved

**Files:**
- Modify: `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Update catalog for SEC-006–013** — change `| Draft |` → `| Approved |`, date → current, notes → `Wave 7F — self-review complete`

- [ ] **Step 2: Verify**

```bash
grep "| SEC-0" governance/standards/enterprise-architecture-catalog.md | grep -c "| Approved |"
```
Expected: 13 (SEC-001–005 already Approved + 8 new = 13)

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md \
  knowledge-base/patterns/security/jwt-best-practices.md \
  knowledge-base/patterns/security/secrets-rotation.md \
  knowledge-base/patterns/security/data-masking.md \
  knowledge-base/patterns/security/fraud-signal-collection.md \
  knowledge-base/patterns/security/attribute-based-access-control.md \
  knowledge-base/patterns/security/session-revocation.md \
  knowledge-base/patterns/security/audit-logging-tamper-evident.md \
  knowledge-base/patterns/security/pii-tokenization-format-preserving.md
git commit -m "feat(catalog): Wave 7F gate — promote SEC-006–013 Draft→Approved"
```

---

## Wave 7G — REF-005–012

### Task 19: Wave 7G — Stage 1 Automated Checks

**Files:** none modified

- [ ] **Step 1: Mermaid lint all REF files**

```bash
for f in \
  knowledge-base/reference-architectures/swift-mt-mx-wire-transfer.md \
  knowledge-base/reference-architectures/loan-origination.md \
  knowledge-base/reference-architectures/fraud-screening-platform.md \
  knowledge-base/reference-architectures/regulatory-reporting.md \
  knowledge-base/reference-architectures/account-opening-omnichannel.md \
  knowledge-base/reference-architectures/ledger-posting-engine.md \
  knowledge-base/reference-architectures/open-banking-psd2.md \
  knowledge-base/reference-architectures/dispute-management.md; do
  result=$(bash scripts/mermaid-lint-doc.sh "$f" 2>&1)
  code=$?
  if [ $code -ne 0 ]; then echo "FAIL: $(basename $f)"; echo "$result" | tail -3; else echo "PASS: $(basename $f)"; fi
done
```
Expected: all PASS.

- [ ] **Step 2: Compliance check** — `python3 scripts/check-compliance-rows.py` Expected: 0 failures.

### Task 20: Wave 7G — Stage 2 Self-Review (REF-005–012)

**Files:** Fix any doc with open items in-place.

- [ ] **Step 1: Run diagnostic on all 8 REF files**

```bash
for f in \
  knowledge-base/reference-architectures/swift-mt-mx-wire-transfer.md \
  knowledge-base/reference-architectures/loan-origination.md \
  knowledge-base/reference-architectures/fraud-screening-platform.md \
  knowledge-base/reference-architectures/regulatory-reporting.md \
  knowledge-base/reference-architectures/account-opening-omnichannel.md \
  knowledge-base/reference-architectures/ledger-posting-engine.md \
  knowledge-base/reference-architectures/open-banking-psd2.md \
  knowledge-base/reference-architectures/dispute-management.md; do
  sections=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  placeholder=$(grep -ilE "TBD|TODO|placeholder" "$f" 2>/dev/null && echo "YES" || echo "no")
  nfr=$(grep -iE "p[0-9]+.*ms|<[0-9]+.*ms|throughput|latency|rps|tps|99th|p99|[0-9]+%" "$f" | grep -c "" 2>/dev/null || echo 0)
  ring2=$(grep -c "Ring 2\|SBV\|Decree 13" "$f" 2>/dev/null || echo 0)
  stride=$(grep -iE "Tampering|Spoofing|Repudiation|Information Disclosure|Denial of Service|Elevation of Privilege" "$f" | grep -c "" 2>/dev/null || echo 0)
  alert=$(grep -c "Alert:" "$f" 2>/dev/null || echo 0)
  echo "$(basename $f .md): sections=$sections placeholder=$placeholder nfr=$nfr ring2=$ring2 stride=$stride alert=$alert"
done
```

- [ ] **Step 2: Fix open items; re-run diagnostic to confirm all 8 pass**

### Task 21: Wave 7G — Promote REF-005–012 Draft → Approved

**Files:**
- Modify: `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Update catalog for REF-005–012** — change `| Draft |` → `| Approved |`, date → current, notes → `Wave 7G — self-review complete`

- [ ] **Step 2: Verify**

```bash
grep "| REF-0" governance/standards/enterprise-architecture-catalog.md | grep -c "| Approved |"
```
Expected: 12 (REF-001–004 already Approved + 8 new = 12)

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md \
  knowledge-base/reference-architectures/swift-mt-mx-wire-transfer.md \
  knowledge-base/reference-architectures/loan-origination.md \
  knowledge-base/reference-architectures/fraud-screening-platform.md \
  knowledge-base/reference-architectures/regulatory-reporting.md \
  knowledge-base/reference-architectures/account-opening-omnichannel.md \
  knowledge-base/reference-architectures/ledger-posting-engine.md \
  knowledge-base/reference-architectures/open-banking-psd2.md \
  knowledge-base/reference-architectures/dispute-management.md
git commit -m "feat(catalog): Wave 7G gate — promote REF-005–012 Draft→Approved"
```

---

## Wave 7H — DATA-004–013

### Task 22: Wave 7H — Stage 1 Automated Checks

**Files:** none modified

- [ ] **Step 1: Mermaid lint all DATA files**

```bash
for f in \
  knowledge-base/patterns/data/data-vault-2.md \
  knowledge-base/patterns/data/slowly-changing-dimensions.md \
  knowledge-base/patterns/data/lambda-architecture.md \
  knowledge-base/patterns/data/kappa-architecture.md \
  knowledge-base/patterns/data/change-data-capture.md \
  knowledge-base/patterns/data/data-lineage.md \
  knowledge-base/patterns/data/time-series-modelling.md \
  knowledge-base/patterns/data/data-quality-rules.md \
  knowledge-base/patterns/data/data-virtualization.md \
  knowledge-base/patterns/data/reference-data-master.md; do
  result=$(bash scripts/mermaid-lint-doc.sh "$f" 2>&1)
  code=$?
  if [ $code -ne 0 ]; then echo "FAIL: $(basename $f)"; echo "$result" | tail -3; else echo "PASS: $(basename $f)"; fi
done
```
Expected: all PASS.

- [ ] **Step 2: Compliance check** — `python3 scripts/check-compliance-rows.py` Expected: 0 failures.

### Task 23: Wave 7H — Stage 2 Self-Review (DATA-004–013)

**Files:** Fix any doc with open items in-place.

- [ ] **Step 1: Run diagnostic on all 10 DATA files**

```bash
for f in \
  knowledge-base/patterns/data/data-vault-2.md \
  knowledge-base/patterns/data/slowly-changing-dimensions.md \
  knowledge-base/patterns/data/lambda-architecture.md \
  knowledge-base/patterns/data/kappa-architecture.md \
  knowledge-base/patterns/data/change-data-capture.md \
  knowledge-base/patterns/data/data-lineage.md \
  knowledge-base/patterns/data/time-series-modelling.md \
  knowledge-base/patterns/data/data-quality-rules.md \
  knowledge-base/patterns/data/data-virtualization.md \
  knowledge-base/patterns/data/reference-data-master.md; do
  sections=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  placeholder=$(grep -ilE "TBD|TODO|placeholder" "$f" 2>/dev/null && echo "YES" || echo "no")
  nfr=$(grep -iE "p[0-9]+.*ms|<[0-9]+.*ms|throughput|latency|rps|tps|99th|p99|[0-9]+%" "$f" | grep -c "" 2>/dev/null || echo 0)
  ring2=$(grep -c "Ring 2\|SBV\|Decree 13" "$f" 2>/dev/null || echo 0)
  stride=$(grep -iE "Tampering|Spoofing|Repudiation|Information Disclosure|Denial of Service|Elevation of Privilege" "$f" | grep -c "" 2>/dev/null || echo 0)
  alert=$(grep -c "Alert:" "$f" 2>/dev/null || echo 0)
  echo "$(basename $f .md): sections=$sections placeholder=$placeholder nfr=$nfr ring2=$ring2 stride=$stride alert=$alert"
done
```

- [ ] **Step 2: Fix open items; re-run diagnostic to confirm all 10 pass**

### Task 24: Wave 7H — Promote DATA-004–013 Draft → Approved

**Files:**
- Modify: `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Update catalog for DATA-004–013** — change `| Draft |` → `| Approved |`, date → current, notes → `Wave 7H — self-review complete`

- [ ] **Step 2: Verify**

```bash
grep "| DATA-0" governance/standards/enterprise-architecture-catalog.md | grep -c "| Approved |"
```
Expected: 13 (DATA-001–003 already Approved + 10 new = 13)

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md \
  knowledge-base/patterns/data/data-vault-2.md \
  knowledge-base/patterns/data/slowly-changing-dimensions.md \
  knowledge-base/patterns/data/lambda-architecture.md \
  knowledge-base/patterns/data/kappa-architecture.md \
  knowledge-base/patterns/data/change-data-capture.md \
  knowledge-base/patterns/data/data-lineage.md \
  knowledge-base/patterns/data/time-series-modelling.md \
  knowledge-base/patterns/data/data-quality-rules.md \
  knowledge-base/patterns/data/data-virtualization.md \
  knowledge-base/patterns/data/reference-data-master.md
git commit -m "feat(catalog): Wave 7H gate — promote DATA-004–013 Draft→Approved"
```

---

## Task 25: Final Verification

**Files:** none modified

- [ ] **Step 1: Count remaining Draft entries**

```bash
grep -c "| Draft |" governance/standards/enterprise-architecture-catalog.md
```
Expected: 0 (all 55 Wave-5 docs now Approved)

- [ ] **Step 2: Count Approved entries**

```bash
grep -c "| Approved |" governance/standards/enterprise-architecture-catalog.md
```
Expected: 142 (87 from Wave 6 + 55 newly promoted)

- [ ] **Step 3: Count Proposed entries**

```bash
grep -c "| Proposed |" governance/standards/enterprise-architecture-catalog.md
```
Expected: 0

- [ ] **Step 4: Final compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: 0 failures.

- [ ] **Step 5: Confirm git log**

```bash
git log --oneline -10
```
Expected: 8 Wave-7 gate commits visible (7A through 7H).
