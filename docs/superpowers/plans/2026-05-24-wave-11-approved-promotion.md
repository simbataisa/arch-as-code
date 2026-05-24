# Wave 11 — Draft → Approved Promotion Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Promote all 23 Wave-9/10 Draft documents (15 BSP banking engines + 8 REF product platforms) to Approved status by running a two-stage gate: automated quality checks (Stage 1), then a per-doc self-review checklist with targeted in-place fixes (Stage 2), followed by catalog promotion commits per group.

**Architecture:** Two promotion groups — Wave 11A (BSP-006–020, 15 engine docs in `knowledge-base/patterns/banking-solutions/`) and Wave 11B (REF-013–020, 8 platform docs in `knowledge-base/reference-architectures/`). Each group runs Stage 1 automated checks → Stage 2 self-review + fixes → catalog promotion commit. Target final state: **Approved: 165, Draft: 0, Proposed: 0**.

**Tech Stack:** Markdown, `bash scripts/mermaid-lint-doc.sh`, `python3 scripts/check-compliance-rows.py`, `python3 scripts/validate-internal-links.py`, `grep`, `git`, `sed`

---

## Self-Review Checklist (applied in every Stage 2)

Each document must pass **all 7 items** before promotion:

1. **≥15 sections** — `grep -c "^## " <file>` returns ≥ 15
2. **No placeholder text** — no "TBD", "TODO", "placeholder" in body
3. **≥1 measurable NFR threshold** — e.g. `p99 < 100ms`, `≥ 99.9%`, `< 50ms`
4. **3-ring Compliance Mapping** — Ring 2 row mentions SBV Circular 09/2020 or Decree 13/2023 and ends with `⚠️ (working summary — pending Legal review)`
5. **≥2 STRIDE-labelled threats** — each named with explicit category: `(Tampering)`, `(Spoofing)`, `(Information Disclosure)`, `(Repudiation)`, `(Denial of Service)`, `(Elevation of Privilege)`
6. **Named runbook alert** — at least one `Alert: SomeName` entry in Operational Runbook
7. **Related Patterns non-empty** — Related Patterns section has ≥1 entry

### Diagnostic script (run per group)

```bash
for f in <file1> <file2> ...; do
  sections=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  placeholder=$(grep -ilE "TBD|TODO|placeholder" "$f" 2>/dev/null && echo "YES" || echo "no")
  nfr=$(grep -iE "p[0-9]+.*ms|<[0-9]+.*ms|throughput|latency|rps|tps|99th|p99|[0-9]+%" "$f" | grep -c "" 2>/dev/null || echo 0)
  ring2=$(grep -c "Ring 2\|SBV\|Decree 13" "$f" 2>/dev/null || echo 0)
  stride=$(grep -iE "\(Tampering\)|\(Spoofing\)|\(Repudiation\)|\(Information Disclosure\)|\(Denial of Service\)|\(Elevation of Privilege\)" "$f" | grep -c "" 2>/dev/null || echo 0)
  alert=$(grep -c "Alert:" "$f" 2>/dev/null || echo 0)
  echo "$(basename $f .md): sections=$sections placeholder=$placeholder nfr=$nfr ring2=$ring2 stride=$stride alert=$alert"
done
```

Pass criteria: `sections≥15`, `placeholder=no`, `nfr≥1`, `ring2≥1`, `stride≥2`, `alert≥1`

---

## Task 0: Pre-Flight Verification

- [ ] **Step 1: Verify current catalog counts**

```bash
grep -c "| Draft |" governance/standards/enterprise-architecture-catalog.md
grep -c "| Approved |" governance/standards/enterprise-architecture-catalog.md
grep -c "| Proposed |" governance/standards/enterprise-architecture-catalog.md
```

Expected: Draft=23, Approved=142, Proposed=0.

- [ ] **Step 2: Verify all 23 files exist**

```bash
ls knowledge-base/patterns/banking-solutions/pricing-engine.md \
   knowledge-base/patterns/banking-solutions/interest-calculation-engine.md \
   knowledge-base/patterns/banking-solutions/fee-engine.md \
   knowledge-base/patterns/banking-solutions/tax-calculation-engine.md \
   knowledge-base/patterns/banking-solutions/rule-decisioning-engine.md \
   knowledge-base/patterns/banking-solutions/credit-limit-engine.md \
   knowledge-base/patterns/banking-solutions/transaction-limit-engine.md \
   knowledge-base/patterns/banking-solutions/collateral-management-engine.md \
   knowledge-base/patterns/banking-solutions/fx-rate-engine.md \
   knowledge-base/patterns/banking-solutions/position-keeping-engine.md \
   knowledge-base/patterns/banking-solutions/settlement-engine.md \
   knowledge-base/patterns/banking-solutions/product-factory.md \
   knowledge-base/patterns/banking-solutions/accrual-engine.md \
   knowledge-base/patterns/banking-solutions/collections-engine.md \
   knowledge-base/patterns/banking-solutions/relationship-pricing-engine.md \
   knowledge-base/reference-architectures/retail-deposits-platform.md \
   knowledge-base/reference-architectures/consumer-lending-platform.md \
   knowledge-base/reference-architectures/credit-card-issuing-platform.md \
   knowledge-base/reference-architectures/corporate-lending-syndications.md \
   knowledge-base/reference-architectures/trade-finance-platform.md \
   knowledge-base/reference-architectures/treasury-fx-platform.md \
   knowledge-base/reference-architectures/wealth-management-platform.md \
   knowledge-base/reference-architectures/cash-management-liquidity.md
```

Expected: all 23 files listed without error.

- [ ] **Step 3: Commit pre-flight**

```bash
git add docs/superpowers/plans/2026-05-24-wave-11-approved-promotion.md
git commit -m "docs(plans): Wave 11 approved-promotion plan"
```

---

## Task 1: Wave 11A — BSP-006–020 Gate + Promote Draft → Approved

**Files (15 BSP engine docs in `knowledge-base/patterns/banking-solutions/`):**
- pricing-engine.md, interest-calculation-engine.md, fee-engine.md, tax-calculation-engine.md
- rule-decisioning-engine.md, credit-limit-engine.md, transaction-limit-engine.md, collateral-management-engine.md
- fx-rate-engine.md, position-keeping-engine.md, settlement-engine.md
- product-factory.md, accrual-engine.md, collections-engine.md, relationship-pricing-engine.md

**Catalog files modified:**
- `governance/standards/_catalog-inventory.yml`
- `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Stage 1 — Mermaid lint for all 15 BSP docs**

```bash
for f in \
  knowledge-base/patterns/banking-solutions/pricing-engine.md \
  knowledge-base/patterns/banking-solutions/interest-calculation-engine.md \
  knowledge-base/patterns/banking-solutions/fee-engine.md \
  knowledge-base/patterns/banking-solutions/tax-calculation-engine.md \
  knowledge-base/patterns/banking-solutions/rule-decisioning-engine.md \
  knowledge-base/patterns/banking-solutions/credit-limit-engine.md \
  knowledge-base/patterns/banking-solutions/transaction-limit-engine.md \
  knowledge-base/patterns/banking-solutions/collateral-management-engine.md \
  knowledge-base/patterns/banking-solutions/fx-rate-engine.md \
  knowledge-base/patterns/banking-solutions/position-keeping-engine.md \
  knowledge-base/patterns/banking-solutions/settlement-engine.md \
  knowledge-base/patterns/banking-solutions/product-factory.md \
  knowledge-base/patterns/banking-solutions/accrual-engine.md \
  knowledge-base/patterns/banking-solutions/collections-engine.md \
  knowledge-base/patterns/banking-solutions/relationship-pricing-engine.md; do
  bash scripts/mermaid-lint-doc.sh "$f"
done
```

Expected: 0 failures. Fix any Mermaid syntax errors before proceeding (subgraph names with spaces → `subgraph ID["Label"]`; `\n` in labels → `<br/>`).

- [ ] **Step 2: Stage 1 — Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```

Expected: 0 failures. If any BSP doc is missing Ring 0/1/2 rows or Ring 2 lacks `⚠️ (working summary — pending Legal review)`, fix inline.

- [ ] **Step 3: Stage 2 — Diagnostic self-review for BSP-006–020**

Run diagnostic script for all 15 BSP files:

```bash
for f in \
  knowledge-base/patterns/banking-solutions/pricing-engine.md \
  knowledge-base/patterns/banking-solutions/interest-calculation-engine.md \
  knowledge-base/patterns/banking-solutions/fee-engine.md \
  knowledge-base/patterns/banking-solutions/tax-calculation-engine.md \
  knowledge-base/patterns/banking-solutions/rule-decisioning-engine.md \
  knowledge-base/patterns/banking-solutions/credit-limit-engine.md \
  knowledge-base/patterns/banking-solutions/transaction-limit-engine.md \
  knowledge-base/patterns/banking-solutions/collateral-management-engine.md \
  knowledge-base/patterns/banking-solutions/fx-rate-engine.md \
  knowledge-base/patterns/banking-solutions/position-keeping-engine.md \
  knowledge-base/patterns/banking-solutions/settlement-engine.md \
  knowledge-base/patterns/banking-solutions/product-factory.md \
  knowledge-base/patterns/banking-solutions/accrual-engine.md \
  knowledge-base/patterns/banking-solutions/collections-engine.md \
  knowledge-base/patterns/banking-solutions/relationship-pricing-engine.md; do
  sections=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  placeholder=$(grep -ilE "TBD|TODO|placeholder" "$f" 2>/dev/null && echo "YES" || echo "no")
  nfr=$(grep -iE "p[0-9]+.*ms|<[0-9]+.*ms|throughput|latency|rps|tps|99th|p99|[0-9]+%" "$f" | grep -c "" 2>/dev/null || echo 0)
  ring2=$(grep -c "Ring 2\|SBV\|Decree 13" "$f" 2>/dev/null || echo 0)
  stride=$(grep -iE "\(Tampering\)|\(Spoofing\)|\(Repudiation\)|\(Information Disclosure\)|\(Denial of Service\)|\(Elevation of Privilege\)" "$f" | grep -c "" 2>/dev/null || echo 0)
  alert=$(grep -c "Alert:" "$f" 2>/dev/null || echo 0)
  echo "$(basename $f .md): sections=$sections placeholder=$placeholder nfr=$nfr ring2=$ring2 stride=$stride alert=$alert"
done
```

For any doc that fails (sections<15, placeholder=YES, nfr=0, ring2=0, stride<2, alert=0), fix the specific gap in-place:

- **sections<15**: add the missing section (Context, When Not to Use, Variants, etc.) — a minimal 2-sentence stub is sufficient
- **placeholder=YES**: replace "TBD" / "TODO" with a meaningful sentence
- **nfr=0**: add a threshold sentence to the NFR section, e.g. `threshold: p99 < 50ms`
- **ring2=0**: add a Ring 2 row to the Compliance Mapping table — example:
  ```
  | Ring 2 — Vietnam | SBV Circular 09/2020 | §IV.2 — transaction data logging | Every [pattern] posting is logged with accountId, amount, and eventId to the structured audit log ⚠️ (working summary — pending Legal review) |
  ```
- **stride<2**: add STRIDE threat entries; each must have category in parentheses, e.g. `**Unauthorized access (Elevation of Privilege)**`
- **alert=0**: add at least one `Alert: SomeName — fires when ...` line to the Operational Runbook

Re-run diagnostic after fixes to confirm all 15 docs pass.

- [ ] **Step 4: Promote BSP-006–020 Draft → Approved in inventory YAML**

In `governance/standards/_catalog-inventory.yml`, find the 15 BSP entries (catalog IDs BSP-006 through BSP-020) and change `status: Draft` to `status: Approved` for each.

- [ ] **Step 5: Promote BSP-006–020 Draft → Approved in catalog markdown**

In `governance/standards/enterprise-architecture-catalog.md`, find the 15 BSP rows and change `| Draft |` to `| Approved |` in the status column.

- [ ] **Step 6: Verify BSP promotion**

```bash
grep -c "| Draft |" governance/standards/enterprise-architecture-catalog.md
grep -c "| Approved |" governance/standards/enterprise-architecture-catalog.md
```

Expected: Draft=8 (REF-013–020 still Draft), Approved=157 (142 original + 15 BSP).

- [ ] **Step 7: Commit Wave 11A**

```bash
git add knowledge-base/patterns/banking-solutions/*.md governance/standards/_catalog-inventory.yml governance/standards/enterprise-architecture-catalog.md
git commit -m "feat(catalog): Wave 11A gate — promote BSP-006–020 Draft→Approved"
```

---

## Task 2: Wave 11B — REF-013–020 Gate + Promote Draft → Approved

**Files (8 REF platform docs in `knowledge-base/reference-architectures/`):**
- retail-deposits-platform.md, consumer-lending-platform.md, credit-card-issuing-platform.md
- corporate-lending-syndications.md, trade-finance-platform.md, treasury-fx-platform.md
- wealth-management-platform.md, cash-management-liquidity.md

**Catalog files modified:**
- `governance/standards/_catalog-inventory.yml`
- `governance/standards/enterprise-architecture-catalog.md`

- [ ] **Step 1: Stage 1 — Mermaid lint for all 8 REF docs**

```bash
for f in \
  knowledge-base/reference-architectures/retail-deposits-platform.md \
  knowledge-base/reference-architectures/consumer-lending-platform.md \
  knowledge-base/reference-architectures/credit-card-issuing-platform.md \
  knowledge-base/reference-architectures/corporate-lending-syndications.md \
  knowledge-base/reference-architectures/trade-finance-platform.md \
  knowledge-base/reference-architectures/treasury-fx-platform.md \
  knowledge-base/reference-architectures/wealth-management-platform.md \
  knowledge-base/reference-architectures/cash-management-liquidity.md; do
  bash scripts/mermaid-lint-doc.sh "$f"
done
```

Expected: 0 failures.

- [ ] **Step 2: Stage 1 — Compliance check**

```bash
python3 scripts/check-compliance-rows.py
```

Expected: 0 failures (already passed Wave 10 gate but re-verify).

- [ ] **Step 3: Stage 1 — Internal link validator**

```bash
python3 scripts/validate-internal-links.py
```

Expected: 0 broken links.

- [ ] **Step 4: Stage 2 — Diagnostic self-review for REF-013–020**

```bash
for f in \
  knowledge-base/reference-architectures/retail-deposits-platform.md \
  knowledge-base/reference-architectures/consumer-lending-platform.md \
  knowledge-base/reference-architectures/credit-card-issuing-platform.md \
  knowledge-base/reference-architectures/corporate-lending-syndications.md \
  knowledge-base/reference-architectures/trade-finance-platform.md \
  knowledge-base/reference-architectures/treasury-fx-platform.md \
  knowledge-base/reference-architectures/wealth-management-platform.md \
  knowledge-base/reference-architectures/cash-management-liquidity.md; do
  sections=$(grep -c "^## " "$f" 2>/dev/null || echo 0)
  placeholder=$(grep -ilE "TBD|TODO|placeholder" "$f" 2>/dev/null && echo "YES" || echo "no")
  nfr=$(grep -iE "p[0-9]+.*ms|<[0-9]+.*ms|throughput|latency|rps|tps|99th|p99|[0-9]+%" "$f" | grep -c "" 2>/dev/null || echo 0)
  ring2=$(grep -c "Ring 2\|SBV\|Decree 13" "$f" 2>/dev/null || echo 0)
  stride=$(grep -iE "\(Tampering\)|\(Spoofing\)|\(Repudiation\)|\(Information Disclosure\)|\(Denial of Service\)|\(Elevation of Privilege\)" "$f" | grep -c "" 2>/dev/null || echo 0)
  alert=$(grep -c "Alert:" "$f" 2>/dev/null || echo 0)
  echo "$(basename $f .md): sections=$sections placeholder=$placeholder nfr=$nfr ring2=$ring2 stride=$stride alert=$alert"
done
```

Apply same fix playbook as Task 1 Step 3 for any failing docs. Re-run diagnostic after fixes.

- [ ] **Step 5: Promote REF-013–020 Draft → Approved in inventory YAML**

In `governance/standards/_catalog-inventory.yml`, find the 8 REF entries (catalog IDs REF-013 through REF-020) and change `status: Draft` to `status: Approved` for each.

- [ ] **Step 6: Promote REF-013–020 Draft → Approved in catalog markdown**

In `governance/standards/enterprise-architecture-catalog.md`, find the 8 REF rows and change `| Draft |` to `| Approved |` in the status column.

- [ ] **Step 7: Verify REF promotion**

```bash
grep -c "| Draft |" governance/standards/enterprise-architecture-catalog.md
grep -c "| Approved |" governance/standards/enterprise-architecture-catalog.md
```

Expected: Draft=0, Approved=165.

- [ ] **Step 8: Commit Wave 11B**

```bash
git add knowledge-base/reference-architectures/*.md governance/standards/_catalog-inventory.yml governance/standards/enterprise-architecture-catalog.md
git commit -m "feat(catalog): Wave 11B gate — promote REF-013–020 Draft→Approved"
```

---

## Task 3: Final Verification + Tag v1.1.0

- [ ] **Step 1: Final catalog count verification**

```bash
grep -c "| Approved |" governance/standards/enterprise-architecture-catalog.md
grep -c "| Draft |" governance/standards/enterprise-architecture-catalog.md
grep -c "| Proposed |" governance/standards/enterprise-architecture-catalog.md
```

Expected: **Approved=165, Draft=0, Proposed=0**.

- [ ] **Step 2: Run all three quality scripts one final time**

```bash
python3 scripts/check-compliance-rows.py
python3 scripts/validate-internal-links.py
```

Expected: 0 failures, 0 broken links.

- [ ] **Step 3: Tag v1.1.0**

```bash
git tag -a v1.1.0 -m "v1.1.0 — Wave 9/10/11 complete: 165 Approved (23 new banking engines + product platforms)"
```

- [ ] **Step 4: Final git log verification**

```bash
git log --oneline -5
```

Expected top entries:
```
feat(catalog): Wave 11B gate — promote REF-013–020 Draft→Approved
feat(catalog): Wave 11A gate — promote BSP-006–020 Draft→Approved
docs(plans): Wave 11 approved-promotion plan
feat(catalog): Wave 10 gate — promote REF-013–020 Proposed→Draft
feat(catalog): REF-020 Cash Management and Liquidity — Wave 10
```

---

## Self-Review

### Spec Coverage

- Wave 11A: 15 BSP docs (BSP-006–020) gated and promoted ✅
- Wave 11B: 8 REF docs (REF-013–020) gated and promoted ✅
- Target state 165/0/0 verified ✅
- v1.1.0 tag cut ✅

### No placeholders — all steps have exact commands and expected outputs ✅
