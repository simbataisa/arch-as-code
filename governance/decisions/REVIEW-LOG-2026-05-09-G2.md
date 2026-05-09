# Review Log — Gate G2 (Phase 1: Master Catalog Spec) — THE BIG GATE

Status: Queued for async review
Gate: G2
Phase: Phase 1 (Master catalog spec)
Date queued: 2026-05-09
SLA: 5–7 business days (multiple rounds expected)

## Review scope

- `governance/standards/enterprise-architecture-catalog.md` — master catalog (~440 lines, §1–§11)
- `governance/standards/_catalog-inventory.yml` — machine-readable inventory (141 rows)
- `scripts/render-catalog-table.py` — table-rendering script
- `registry/catalog-reviewers.yml` — role-to-human alias registry

## Reviewers (per `registry/catalog-reviewers.yml`)

- @ea-board (full board)
- @dab-chair
- @payments-domain-owner
- @core-banking-domain-owner
- @digital-channels-domain-owner
- @risk-management-domain-owner
- @lending-domain-owner
- @wealth-management-domain-owner
- @data-platform-domain-owner
- @ciso-delegate
- @sre-lead
- @head-of-compliance

## Reject criteria (verbatim from Spec §5)

- Taxonomy disagreement (categories A–K not aligned with how Techcombank structures architecture work)
- Scope (rows missing or over-included)
- Ownership (role-aliases not aligned with real Techcombank org)
- Sequence (Wave 0 / 1 / 2 / 3 ordering disputed)

## Pre-circulation note

Per Spec R1 mitigation: this gate's outcome can be improved by **informally pre-circulating Sections §1, §2, §3 to EA-Board members before formally triggering G2**, to surface taxonomy/spine disagreements early. Recommend a 2-day informal review window before formal G2 trigger.

## Outstanding items affecting G2

1. `registry/catalog-reviewers.yml` has all roles populated but every `primary` and `backup` is `TBD-FILL-IN` — must be filled with actual humans before G2 can complete (otherwise reviewer routing fails).
2. The 4 starter-set "upgrades-of-existing" (DATA-001, INT-001, INT-002, RES-002) currently sit at status=Approved in inventory; their content has not yet been upgraded to full ops-runbook depth. EA-Board may wish to re-classify these as Draft until upgrade happens. Currently treated as Approved-but-pending-upgrade in §5 gap analysis.
3. NFR Framework Summary (§7) is a placeholder; will be backfilled with authoritative numbers from spine docs P2.1 + P2.2 in Phase 2 (Task P2.6.B).

## Action items if approved

1. Proceed to Phase 2 (6 spine docs sequential authoring).
2. Concurrently dispatch Phase 2.5 (stub generation) — parallel-safe with Phase 2.
3. Open ticket with HR / Org-Chart owner to populate `registry/catalog-reviewers.yml` with names.

## Reviewer sign-off

- [ ] @ea-board (full board)
- [ ] @dab-chair
- [ ] @payments-domain-owner
- [ ] @core-banking-domain-owner
- [ ] @digital-channels-domain-owner
- [ ] @risk-management-domain-owner
- [ ] @lending-domain-owner
- [ ] @wealth-management-domain-owner
- [ ] @data-platform-domain-owner
- [ ] @ciso-delegate
- [ ] @sre-lead
- [ ] @head-of-compliance

## Reviewer feedback

(empty — to be filled by reviewers)
