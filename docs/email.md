# Email — to CIO

**To:** [CIO]
**Cc:** Head of Solution Architecture; EA-Board chair; N-1 domain leads; EAT N-2 stakeholders
**Subject:** G2 review pack — Banking Enterprise Architecture Catalog (parallel sign-off requested)
**Attachment:** deck.html (17 slides · ~10-min walkthrough)

---

Dear anh Ngọc,

Ahead of the EA-Board G2 gate, please find attached the review pack for the Banking Enterprise Architecture Catalog. We are requesting **parallel sign-off** — your endorsement at the CIO level, alongside N-1 domain leads and the EAT N-2 stakeholder group — so authoring can begin without serialising the approval chain. The plan has been aligned with Timo (Head of EAT).

## What this is

A consolidated, citable architecture knowledge base for Techcombank Solution Architecture. Today the EAT repository holds 22 reference documents with no DAB-citable normative IDs, no Vietnam-specific compliance mapping, and no formal NFR acceptance template — meaning every design review depends on tribal knowledge and freshly-written compliance text. The catalog closes that gap.

## What is in the deck

- **Framing (slides 1–3).** Today's state, and the three numbers that anchor delivery: **141** target catalog rows (the completeness bar), **20** starter-set documents authored at full ops-runbook depth in Wave 0, and **6** normative spine documents that all radii inherit from.
- **The model (slides 4–6).** Three concentric regulatory rings — generic (NIST, OWASP, Well-Architected) → international banking (PCI-DSS 4.0, BCBS 239/230, SWIFT CSP, ISO 20022) → Vietnam (SBV Circular 09/2020, Decree 13/2023 PDP, Decree 53/2022). The spine-and-radii document model. The taxonomy.
- **The 20 starter patterns (slide 7) and quality bar (slide 8).** What we commit to authoring this wave, and the 12-section template every document must satisfy — three-ring compliance map, NFR acceptance criteria, threat model, runbook stub, multi-stack code samples.
- **A worked example (slides 9–10).** Service tiering with NFR targets, plus a compliance map walked end-to-end so reviewers can see the bar applied.
- **Delivery (slides 11–13).** Seven-phase plan with explicit gates, the dependency graph (spine sequential → radii parallel in three sub-waves → final integration), timeline and effort.
- **Risk, metrics, decision (slides 14–16).** Risk register with mitigations, success metrics for adoption / quality / coverage, and the five specific decisions we are asking each approver to confirm.

## What we need from each approver

1. **Taxonomy** — the spine/radii split and the three-ring compliance model.
2. **Scope of Wave 0** — the 20 starter patterns, with Wave 1 stubs deferred.
3. **Ownership** — N-1 domain leads named as document owners, EAT N-2 named as reviewers.
4. **Sequence** — spine-first sequential, then radii in parallel sub-waves.
5. **Quality bar** — the 12-section template and lint rules as merge-blocking.

A standing decision will be recorded against each item; partial sign-off (e.g. taxonomy + bar approved, scope deferred) is acceptable and lets us begin spine authoring on the approved subset.

## Process and timing

- **Pack circulated:** today.
- **Parallel review window:** 5 working days. Comments via the catalog issue tracker, tagged by slide number.
- **Live walkthrough:** 45-minute session for N-1 + EAT N-2. I will book.
- **G2 close:** end of next week. Phase 2 (spine authoring) opens the following week on whichever items have cleared.

The shortest path through the deck is **slides 3, 11, 13, and 16** — what we are shipping, how, when, and what we need you to approve.

Happy to walk it through live, or to take questions async ahead of G2.

Best,
Dao Tuan Anh (Dennis)
Director, Solution Architecture · Techcombank
