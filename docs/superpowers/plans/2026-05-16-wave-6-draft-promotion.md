# Wave 6 Draft Promotion (64 docs) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Promote all 64 existing Draft documents through a hybrid quality gate (automated checks + per-doc self-review checklist) and advance each from Draft → Approved.

**Architecture:** Two-stage gate per group: Stage 1 runs automated lint/compliance/dead-link checks (all must pass before Stage 2 begins). Stage 2 runs a per-doc self-review checklist; any doc with an open item receives a targeted fix in the same task before the group is promoted. No doc moves to Approved with open items.

**Tech Stack:** Java 21 / Spring Boot 3.x / Resilience4j / Spring Kafka / Apache Camel / PostgreSQL 16 / React 18 + TypeScript 5 / Swift 5.10 / Android Kotlin 2.x / Debezium / OPA/Rego / HashiCorp Vault

**Ring 2 rule:** Any Vietnamese regulation (SBV Circular 09/2020, Decree 13/2023) cited in Ring 2 must end: `⚠️ (working summary — pending Legal review)`

**Self-review checklist (per doc — all 7 must be checked before promotion):**
- [ ] All 15 sections present and non-empty (no "TBD", no placeholder text)
- [ ] Mermaid diagram accurately represents the described solution (no stale/mismatched diagram)
- [ ] NFR Acceptance Criteria contains at least one measurable threshold (p99 latency, RTO, availability %)
- [ ] Compliance Mapping covers all 3 rings; Ring 2 row ends with `⚠️ (working summary — pending Legal review)`
- [ ] Threat Model names at least 2 specific threats (STRIDE category) with concrete mitigations
- [ ] Runbook stub has at least one named alert with threshold values and one named remediation step
- [ ] Related Patterns links are valid and bidirectional (the linked doc also references back)

**Sub-wave map:**
| Wave | IDs | Count |
|------|-----|-------|
| 6A | EIP-001–012 | 12 |
| 6B | EIP-013–025 | 13 |
| 6C | PRIN-006–013 | 8 |
| 6D | RES-005–012 | 8 |
| 6E | BP-005–011, NFR-001–005 | 12 |
| 6F | TPL-001–004, COMP-001 | 5 |
| **Total** | | **58** |

> Note: The design spec targets 64 Draft docs. Count will be confirmed in Task 0 from the live catalog. If the count differs (e.g., additional Wave 4 promotions), additional group entries may exist — process all Draft docs found.

---

## Task 0: Pre-Flight Verification

**Files:** none modified

- [ ] **Step 1: Count Draft docs**

```bash
grep -c "| Draft |" governance/standards/enterprise-architecture-catalog.md
```
Expected: 58–64 (all current Draft entries).

- [ ] **Step 2: Confirm tooling**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/composed-message-processor.md
python3 scripts/check-compliance-rows.py
```
Expected: lint exits 0; compliance script reports PASS or 0 failures.

- [ ] **Step 3: Confirm working tree clean**

```bash
git status --short
```
Expected: empty output (Wave 5 is fully committed before Wave 6 begins).

---

## Wave 6A — EIP-001–012

---

### Task 1: Wave 6A — Stage 1 Automated Checks

**Files:** none modified

- [ ] **Step 1: Mermaid lint all EIP-001–012 files**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/message-channel.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/point-to-point-channel.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/publish-subscribe-channel.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/message-router.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/content-based-router.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/message-translator.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/content-enricher.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/content-filter.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/claim-check.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/normalizer.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/aggregator.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/splitter.md
```
Expected: all exit 0, no FAIL lines.

- [ ] **Step 2: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: 0 failures for any EIP-001–012 file.

- [ ] **Step 3: Dead-link scan**

```bash
grep -rh "\[.*\](\.\./" knowledge-base/patterns/eip/ 2>/dev/null | sed 's/.*](\(.*\))/\1/' | sort -u | while read link; do
  file=$(echo "$link" | sed 's/#.*//')
  [ -f "knowledge-base/patterns/eip/$file" ] || echo "BROKEN: $file"
done
```
Expected: no BROKEN lines.

---

### Task 2: Wave 6A — Stage 2 Self-Review (EIP-001–012)

**Files:** Fix any doc with open self-review items in-place before proceeding.

For each of the 12 files below, run through all 7 checklist items. If any item fails, apply the targeted fix described.

- [ ] **Step 1: Combined diagnostic scan for all EIP-001–012 files**

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
  echo "=== $f ==="
  sections=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  [ "$sections" -lt 15 ] && echo "  SECTIONS: $sections (need 15)"
  grep -il "TBD\|TODO\|placeholder" "$f" 2>/dev/null && echo "  PLACEHOLDER"
  grep -q "p99\|latency\|availability\|RTO" "$f" || echo "  NO NFR THRESHOLD"
  grep -q "pending Legal review" "$f" || echo "  NO RING2 MARKER"
  threats=$(grep -c "STRIDE\|Tampering\|Spoofing\|Repudiation\|Information Disclosure\|Denial of Service\|Elevation" "$f" 2>/dev/null || echo 0)
  [ "$threats" -lt 2 ] && echo "  THREATS: $threats (need ≥2)"
  grep -q "Alert:" "$f" || echo "  NO RUNBOOK ALERT"
done
```
Expected: no warning lines for any file.

- [ ] **Step 5: Fix any open items found in Steps 1–4**

For each file with an open checklist item, apply a targeted fix:
- Missing section: add the section with appropriate content (follow the 15-section template from the Wave 5 spec)
- Placeholder text: replace with concrete content appropriate to the pattern
- Missing NFR threshold: add at least one row with `p99 latency ≤ X ms` or `Availability ≥ XX.XX %`
- Missing Ring 2 marker: append `⚠️ (working summary — pending Legal review)` to the Ring 2 compliance row
- Missing threat: add a STRIDE-categorized threat with mitigation
- Missing runbook alert: add `Alert: [name] — threshold: [value] — remediation: [step]`

After any fix, re-run the lint and compliance check for that specific file:
```bash
bash scripts/mermaid-lint-doc.sh <fixed-file>
python3 scripts/check-compliance-rows.py
```
Expected: exits 0 / 0 failures.

---

### Task 3: Wave 6A — Promote EIP-001–012 Draft → Approved

**Files:**
- Modify: `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Update catalog status for EIP-001–012**

In `governance/standards/enterprise-architecture-catalog.md`, for each of the 12 EIP-001–012 rows, change `| Draft |` to `| Approved |`.

```bash
grep -n "| EIP-0" governance/standards/enterprise-architecture-catalog.md | head -20
```
Verify you can see all 12 EIP-001–012 rows before editing.

- [ ] **Step 2: Verify the changes**

```bash
grep -E "\| EIP-00[1-9] \|| \| EIP-01[0-2] \|" governance/standards/enterprise-architecture-catalog.md | grep -c "| Approved |"
```
Expected: 12

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md
git commit -m "feat(catalog): Wave 6A gate — promote EIP-001–012 Draft→Approved"
```

---

## Wave 6B — EIP-013–025

---

### Task 4: Wave 6B — Stage 1 Automated Checks

**Files:** none modified

- [ ] **Step 1: Mermaid lint all EIP-013–025 files**

```bash
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/resequencer.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/composed-message-processor.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/scatter-gather.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/routing-slip.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/process-manager.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/message-store.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/smart-proxy.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/test-message.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/channel-purger.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/durable-subscriber.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/guaranteed-delivery.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/idempotent-receiver.md
bash scripts/mermaid-lint-doc.sh knowledge-base/patterns/eip/dead-letter-channel.md
```
Expected: all exit 0, no FAIL lines.

- [ ] **Step 2: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: 0 failures for any EIP-013–025 file.

- [ ] **Step 3: Dead-link scan**

```bash
grep -h "| EIP-01[3-9]\|| EIP-02" governance/standards/enterprise-architecture-catalog.md | grep -o '`[^`]*`' | tr -d '`' | while read f; do
  [ -f "$f" ] || echo "MISSING FILE: $f"
done
```
Expected: no MISSING FILE lines.

---

### Task 5: Wave 6B — Stage 2 Self-Review (EIP-013–025)

**Files:** Fix any doc with open self-review items in-place before proceeding.

- [ ] **Step 1: Get the file list for EIP-013–025**

```bash
grep "| EIP-01[3-9]\|| EIP-02" governance/standards/enterprise-architecture-catalog.md | grep -o '`knowledge-base[^`]*`' | tr -d '`'
```

- [ ] **Step 2: Section audit — verify all 15 sections present**

```bash
for f in $(grep "| EIP-01[3-9]\|| EIP-02" governance/standards/enterprise-architecture-catalog.md | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  count=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  echo "$f: $count sections"
done
```
Expected: each file shows 15 sections.

- [ ] **Step 3: Placeholder scan**

```bash
for f in $(grep "| EIP-01[3-9]\|| EIP-02" governance/standards/enterprise-architecture-catalog.md | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  grep -l "TBD\|TODO\|placeholder" "$f" 2>/dev/null && echo "$f has placeholders"
done
```
Expected: no output.

- [ ] **Step 4: NFR threshold verification**

```bash
for f in $(grep "| EIP-01[3-9]\|| EIP-02" governance/standards/enterprise-architecture-catalog.md | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  grep -q "p99\|latency\|availability\|RTO" "$f" || echo "MISSING NFR: $f"
done
```
Expected: no MISSING NFR lines.

- [ ] **Step 5: Ring 2 marker verification**

```bash
for f in $(grep "| EIP-01[3-9]\|| EIP-02" governance/standards/enterprise-architecture-catalog.md | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  grep -q "pending Legal review" "$f" || echo "MISSING Ring2 marker: $f"
done
```
Expected: no MISSING Ring2 lines.

- [ ] **Step 6: Fix any open items; re-lint and re-check after each fix**

Apply targeted fixes per the same rules as Task 2 Step 5. Re-lint after each fix:
```bash
bash scripts/mermaid-lint-doc.sh <fixed-file>
python3 scripts/check-compliance-rows.py
```

---

### Task 6: Wave 6B — Promote EIP-013–025 Draft → Approved

**Files:**
- Modify: `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Update catalog status for EIP-013–025**

In `governance/standards/enterprise-architecture-catalog.md`, for each EIP-013–025 row, change `| Draft |` to `| Approved |`.

- [ ] **Step 2: Verify**

```bash
grep -E "\| EIP-01[3-9] \|| \| EIP-02[0-5] \|" governance/standards/enterprise-architecture-catalog.md | grep -c "| Approved |"
```
Expected: 13

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md
git commit -m "feat(catalog): Wave 6B gate — promote EIP-013–025 Draft→Approved"
```

---

## Wave 6C — PRIN-006–013

---

### Task 7: Wave 6C — Stage 1 Automated Checks

**Files:** none modified

- [ ] **Step 1: Get PRIN-006–013 file paths from catalog**

```bash
grep "| PRIN-0[01][0-9]" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'
```

- [ ] **Step 2: Mermaid lint all PRIN-006–013 files**

```bash
for f in $(grep "| PRIN-0[01][0-9]" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  bash scripts/mermaid-lint-doc.sh "$f"
done
```
Expected: all exit 0.

- [ ] **Step 3: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: 0 failures for PRIN files.

- [ ] **Step 4: Dead-link scan**

```bash
for f in $(grep "| PRIN-0[01][0-9]" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  grep -oh "\[.*\](\.\./[^)]*)" "$f" 2>/dev/null | sed 's/.*](\(.*\))/\1/'
done | sort -u
```
Review output: each relative link should resolve to a file that exists.

---

### Task 8: Wave 6C — Stage 2 Self-Review (PRIN-006–013)

**Files:** Fix any doc with open self-review items in-place.

- [ ] **Step 1: Section audit**

```bash
for f in $(grep "| PRIN-0[01][0-9]" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  count=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  echo "$f: $count sections"
done
```
Expected: each file ≥15 sections.

- [ ] **Step 2: Placeholder scan**

```bash
for f in $(grep "| PRIN-0[01][0-9]" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  grep -il "TBD\|TODO\|placeholder" "$f" 2>/dev/null && echo "$f has placeholders"
done
```
Expected: no output.

- [ ] **Step 3: NFR threshold check**

```bash
for f in $(grep "| PRIN-0[01][0-9]" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  grep -q "p99\|latency\|availability\|RTO" "$f" || echo "MISSING NFR: $f"
done
```
Expected: no MISSING NFR lines.

- [ ] **Step 4: Threat Model check (≥2 threats per doc)**

```bash
for f in $(grep "| PRIN-0[01][0-9]" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  count=$(grep -c "STRIDE\|Spoofing\|Tampering\|Repudiation\|Information Disclosure\|Denial of Service\|Elevation" "$f" 2>/dev/null || echo 0)
  [ "$count" -lt 2 ] && echo "NEEDS MORE THREATS: $f ($count found)"
done
```
Expected: no NEEDS MORE THREATS lines.

- [ ] **Step 5: Ring 2 marker verification**

```bash
for f in $(grep "| PRIN-0[01][0-9]" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  grep -q "pending Legal review" "$f" || echo "MISSING Ring2: $f"
done
```
Expected: no MISSING Ring2 lines.

- [ ] **Step 6: Fix open items; re-lint and re-check after each fix**

---

### Task 9: Wave 6C — Promote PRIN-006–013 Draft → Approved

**Files:**
- Modify: `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Update catalog for PRIN-006–013**

Change `| Draft |` to `| Approved |` for all PRIN-006–013 rows.

- [ ] **Step 2: Verify**

```bash
grep "| PRIN-0[01][0-9]" governance/standards/enterprise-architecture-catalog.md | grep -c "| Approved |"
```
Expected: 8

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md
git commit -m "feat(catalog): Wave 6C gate — promote PRIN-006–013 Draft→Approved"
```

---

## Wave 6D — RES-005–012

---

### Task 10: Wave 6D — Stage 1 Automated Checks

**Files:** none modified

- [ ] **Step 1: Mermaid lint all RES-005–012 files**

```bash
for f in $(grep "| RES-0" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  bash scripts/mermaid-lint-doc.sh "$f"
done
```
Expected: all exit 0.

- [ ] **Step 2: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: 0 failures for RES files.

- [ ] **Step 3: Dead-link scan**

```bash
for f in $(grep "| RES-0" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  grep -oh "\[.*\](\.\./[^)]*\|[^)]*\.md)" "$f" 2>/dev/null
done | grep -v "^$"
```
Review that each linked file exists.

---

### Task 11: Wave 6D — Stage 2 Self-Review (RES-005–012)

**Files:** Fix any doc with open self-review items in-place.

- [ ] **Step 1: Section audit**

```bash
for f in $(grep "| RES-0" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  count=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  echo "$f: $count sections"
done
```
Expected: each file ≥15 sections.

- [ ] **Step 2: Combined diagnostic scan**

```bash
for f in $(grep "| RES-0" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  echo "=== $f ==="
  grep -il "TBD\|TODO" "$f" 2>/dev/null && echo "  PLACEHOLDER"
  grep -q "p99\|RTO" "$f" || echo "  NO NFR THRESHOLD"
  grep -q "pending Legal review" "$f" || echo "  NO RING2 MARKER"
  grep -c "STRIDE\|Tampering\|Spoofing\|Repudiation" "$f" 2>/dev/null | xargs -I{} [ {} -ge 2 ] || echo "  THREAT MODEL < 2 threats"
  grep -q "Alert:" "$f" || echo "  NO RUNBOOK ALERT"
done
```
Expected: no warning lines for any file.

- [ ] **Step 3: Fix open items; re-lint after each fix**

---

### Task 12: Wave 6D — Promote RES-005–012 Draft → Approved

**Files:**
- Modify: `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Update catalog for RES-005–012**

Change `| Draft |` to `| Approved |` for all RES-005–012 rows.

- [ ] **Step 2: Verify**

```bash
grep "| RES-0" governance/standards/enterprise-architecture-catalog.md | grep -c "| Approved |"
```
Expected: ≥8 (includes any previously Approved RES entries + the 8 newly promoted)

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md
git commit -m "feat(catalog): Wave 6D gate — promote RES-005–012 Draft→Approved"
```

---

## Wave 6E — BP-005–011, NFR-001–005

---

### Task 13: Wave 6E — Stage 1 Automated Checks

**Files:** none modified

- [ ] **Step 1: Mermaid lint all BP-005–011 and NFR-001–005 files**

```bash
for f in $(grep "| BP-0\|| NFR-0" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  bash scripts/mermaid-lint-doc.sh "$f"
done
```
Expected: all exit 0.

- [ ] **Step 2: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: 0 failures for BP and NFR files.

- [ ] **Step 3: Dead-link scan**

```bash
for f in $(grep "| BP-0\|| NFR-0" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  grep -oh "\[.*\]([^)]*\.md)" "$f" 2>/dev/null | sed 's/.*](\(.*\))/\1/' | grep -v "^http"
done | sort -u
```
Review each relative link for existence.

---

### Task 14: Wave 6E — Stage 2 Self-Review (BP-005–011, NFR-001–005)

**Files:** Fix any doc with open self-review items in-place.

- [ ] **Step 1: Combined diagnostic scan**

```bash
for f in $(grep "| BP-0\|| NFR-0" governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  echo "=== $f ==="
  sections=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  [ "$sections" -lt 15 ] && echo "  SECTIONS: $sections (need 15)"
  grep -il "TBD\|TODO" "$f" 2>/dev/null && echo "  PLACEHOLDER"
  grep -q "p99\|latency\|RTO\|availability" "$f" || echo "  NO NFR THRESHOLD"
  grep -q "pending Legal review" "$f" || echo "  NO RING2 MARKER"
  threats=$(grep -c "STRIDE\|Tampering\|Spoofing\|Repudiation\|Information Disclosure\|Denial of Service\|Elevation" "$f" 2>/dev/null || echo 0)
  [ "$threats" -lt 2 ] && echo "  THREATS: $threats (need ≥2)"
  grep -q "Alert:" "$f" || echo "  NO RUNBOOK ALERT"
done
```
Expected: no warning lines for any file.

- [ ] **Step 2: Fix open items; re-lint after each fix**

After any fix to a file:
```bash
bash scripts/mermaid-lint-doc.sh <fixed-file>
python3 scripts/check-compliance-rows.py
```
Expected: exits 0 / 0 failures.

---

### Task 15: Wave 6E — Promote BP-005–011, NFR-001–005 Draft → Approved

**Files:**
- Modify: `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Update catalog for BP-005–011 and NFR-001–005**

Change `| Draft |` to `| Approved |` for all BP-005–011 and NFR-001–005 rows.

- [ ] **Step 2: Verify BP promotions**

```bash
grep "| BP-0" governance/standards/enterprise-architecture-catalog.md | grep -c "| Approved |"
```
Expected: ≥7 (BP-005 through BP-011 newly promoted; BP-001–004 already promoted in Wave 3)

- [ ] **Step 3: Verify NFR promotions**

```bash
grep "| NFR-0" governance/standards/enterprise-architecture-catalog.md | grep -c "| Approved |"
```
Expected: ≥5

- [ ] **Step 4: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md
git commit -m "feat(catalog): Wave 6E gate — promote BP-005–011 + NFR-001–005 Draft→Approved"
```

---

## Wave 6F — TPL-001–004, COMP-001

---

### Task 16: Wave 6F — Stage 1 Automated Checks

**Files:** none modified

- [ ] **Step 1: Mermaid lint all TPL-001–004 and COMP-001 files**

```bash
for f in $(grep "| TPL-0\|| COMP-001 " governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  bash scripts/mermaid-lint-doc.sh "$f"
done
```
Expected: all exit 0.

- [ ] **Step 2: Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: 0 failures for TPL and COMP-001 files.

- [ ] **Step 3: Dead-link scan**

```bash
for f in $(grep "| TPL-0\|| COMP-001 " governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  grep -oh "\[.*\]([^)]*\.md)" "$f" 2>/dev/null | sed 's/.*](\(.*\))/\1/' | grep -v "^http"
done | sort -u
```
Review each relative link for existence.

---

### Task 17: Wave 6F — Stage 2 Self-Review (TPL-001–004, COMP-001)

**Files:** Fix any doc with open self-review items in-place.

- [ ] **Step 1: Combined diagnostic scan**

```bash
for f in $(grep "| TPL-0\|| COMP-001 " governance/standards/enterprise-architecture-catalog.md | grep "| Draft |" | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  echo "=== $f ==="
  sections=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  [ "$sections" -lt 15 ] && echo "  SECTIONS: $sections (need 15)"
  grep -il "TBD\|TODO" "$f" 2>/dev/null && echo "  PLACEHOLDER"
  grep -q "p99\|latency\|RTO\|availability" "$f" || echo "  NO NFR THRESHOLD"
  grep -q "pending Legal review" "$f" || echo "  NO RING2 MARKER"
  threats=$(grep -c "STRIDE\|Tampering\|Spoofing\|Repudiation\|Information Disclosure\|Denial of Service\|Elevation" "$f" 2>/dev/null || echo 0)
  [ "$threats" -lt 2 ] && echo "  THREATS: $threats (need ≥2)"
  grep -q "Alert:" "$f" || echo "  NO RUNBOOK ALERT"
done
```
Expected: no warning lines for any file.

- [ ] **Step 2: Fix open items; re-lint after each fix**

---

### Task 18: Wave 6F — Promote TPL-001–004, COMP-001 Draft → Approved

**Files:**
- Modify: `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Update catalog for TPL-001–004 and COMP-001**

Change `| Draft |` to `| Approved |` for all TPL-001–004 and COMP-001 rows.

- [ ] **Step 2: Verify**

```bash
grep "| TPL-0" governance/standards/enterprise-architecture-catalog.md | grep -c "| Approved |"
```
Expected: 4

```bash
grep "| COMP-001 " governance/standards/enterprise-architecture-catalog.md | grep -c "| Approved |"
```
Expected: 1

- [ ] **Step 3: Commit**

```bash
git add governance/standards/enterprise-architecture-catalog.md
git commit -m "feat(catalog): Wave 6F gate — promote TPL-001–004 + COMP-001 Draft→Approved"
```

---

## Task 19: Final Verification

**Files:** none modified

- [ ] **Step 1: Count remaining Draft entries (should only be the 55 Wave 5 docs)**

```bash
grep -c "| Draft |" governance/standards/enterprise-architecture-catalog.md
```
Expected: 55 (the Wave 5 docs authored in Wave 5 — eligible for next Wave 6 cycle; all 64 original Draft docs are now Approved).

- [ ] **Step 2: Count Approved entries**

```bash
grep -c "| Approved |" governance/standards/enterprise-architecture-catalog.md
```
Expected: ≥86 (22 pre-existing + 64 newly promoted).

- [ ] **Step 3: Count Proposed entries**

```bash
grep -c "| Proposed |" governance/standards/enterprise-architecture-catalog.md
```
Expected: 0 (all Proposed stubs authored in Wave 5 and promoted to Draft).

- [ ] **Step 4: Final compliance check**

```bash
python3 scripts/check-compliance-rows.py
```
Expected: 0 failures across all files.

- [ ] **Step 5: Full Mermaid lint on all Wave 6 files**

```bash
for f in $(grep "| Approved |" governance/standards/enterprise-architecture-catalog.md | grep -o '`knowledge-base[^`]*`' | tr -d '`'); do
  bash scripts/mermaid-lint-doc.sh "$f"
done
```
Expected: all pass (exit 0).

- [ ] **Step 6: Catalog state summary**

```bash
echo "=== Final Catalog State ==="
echo "Approved: $(grep -c '| Approved |' governance/standards/enterprise-architecture-catalog.md)"
echo "Draft:    $(grep -c '| Draft |' governance/standards/enterprise-architecture-catalog.md)"
echo "Proposed: $(grep -c '| Proposed |' governance/standards/enterprise-architecture-catalog.md)"
```
Expected:
```
=== Final Catalog State ===
Approved: 86
Draft:    55
Proposed: 0
```
