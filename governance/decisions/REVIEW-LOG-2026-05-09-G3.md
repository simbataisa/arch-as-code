# Review Log — Gate G3 (Phase 2: Spine Docs ×6)

Status: Queued for async review (batched)
Gate: G3
Phase: Phase 2 (Spine docs sequential)
Date queued: 2026-05-09
SLA: 1–2 business days per doc; batched into one MR for review efficiency

## Review scope

Six spine docs:

1. `knowledge-base/nfr/service-tiering-rto-rpo.md` — NFR-001
2. `knowledge-base/nfr/latency-budget-model.md` — NFR-002
3. `knowledge-base/principles/idempotency-by-default.md` — PRIN-006
4. `knowledge-base/templates/nfr-acceptance-criteria-dab.md` — TPL-001
5. `knowledge-base/compliance/compliance-mapping-matrix.md` — COMP-001 (+ `_compliance-matrix.yml`)
6. `knowledge-base/reference-architectures/multi-region-active-active.md` — REF-001

## Reviewers per doc

| Doc | Required reviewers |
|---|---|
| NFR-001 | @ea-board, @sre-lead |
| NFR-002 | @ea-board, @sre-lead, @tech-lead-backend |
| PRIN-006 | @ea-board, @tech-lead-backend |
| TPL-001 | @ea-board, @dab-chair |
| COMP-001 | @ea-board, @head-of-compliance, @ciso-delegate |
| REF-001 | @ea-board, @sre-lead, @payments-domain-owner |

## Reject criteria (verbatim from Spec §5)

- NFR targets unrealistic
- Code samples wrong (Spring 3.x / Resilience4j 2.x / current Techcombank conventions)
- Compliance refs missing or wrong section
- Spine concepts contradicting each other (e.g., NFR-002 latency budget exceeds NFR-001 RTO budget consideration)

## Cross-doc consistency check (already performed inline; reviewer to verify)

- ✅ NFR-001 tier definitions referenced consistently in NFR-002, PRIN-006, TPL-001, REF-001
- ✅ NFR-002 latency budgets referenced in TPL-001 sample YAML
- ✅ PRIN-006 idempotency referenced as prerequisite in REF-001 and EIP-024 (yet to be authored)
- ✅ COMP-001 cells consistent with each spine doc's `## Compliance Mapping` section
- ✅ REF-001 inherits NFR-001 T0 numbers without contradiction

## Outstanding items affecting G3

1. Cells in `_compliance-matrix.yml` for stub patterns (~100 rows) are intentionally omitted — populated when each stub is authored to Approved.
2. Vietnamese-source citations (SBV Circ. 09, Decree 13, Decree 53, BCBS 230 §27) all flagged "(UNOFFICIAL)" pending Legal-team translations. Reviewers should verify the working summaries are consistent with their understanding of these regulations; final cell text is replaced post-Legal sign-off.
3. The `lint-nfr-ac.py` script in TPL-001 is a working-spec; the actual CI implementation (Phase 4 follow-up) may refine it.

## Action items if approved

1. Proceed to Phase 2.5 (stub generation).
2. Concurrently prepare Phase 3 sub-wave dispatches.
3. Engage Legal team (`@legal-vietnam`) to backfill UNOFFICIAL citations.

## Reviewer sign-off (one box per doc per role)

NFR-001:
- [ ] @ea-board
- [ ] @sre-lead

NFR-002:
- [ ] @ea-board
- [ ] @sre-lead
- [ ] @tech-lead-backend

PRIN-006:
- [ ] @ea-board
- [ ] @tech-lead-backend

TPL-001:
- [ ] @ea-board
- [ ] @dab-chair

COMP-001:
- [ ] @ea-board
- [ ] @head-of-compliance
- [ ] @ciso-delegate

REF-001:
- [ ] @ea-board
- [ ] @sre-lead
- [ ] @payments-domain-owner

## Reviewer feedback

(empty — to be filled by reviewers)
