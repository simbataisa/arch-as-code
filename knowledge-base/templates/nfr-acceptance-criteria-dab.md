# NFR Acceptance Criteria — DAB Submission Template

Status: Draft | Last Reviewed: 2026-05-09 | Owner: @dab-chair
Catalog ID: TPL-001 | **Spine**
Tier Applicability: N/A (template)

## Problem Statement

Without a normative NFR-AC template, DAB submissions report Non-Functional Requirements inconsistently. Some teams give vague targets ("highly available"); others give numbers without regulatory context; reviewers can't compare. This template forces every DAB to declare the same machine-checkable contract: tier, RTO/RPO, latency budget, throughput, failure modes, recovery behaviour, blast radius, plus the catalog references that justify the numbers.

## Context

Used by:

- DAB authors filling in `dab/02-key-design-concerns.md` and `dab/05-detailed-design.md`.
- DAB reviewers verifying that NFRs are declared and feasible.
- CI lint that gates merge of `dab/` content if NFR-AC block is missing or malformed.
- Service code via the `@LatencyBudget` and `@ServiceTier` annotations (see [NFR-001](../nfr/service-tiering-rto-rpo.md), [NFR-002](../nfr/latency-budget-model.md)).

## Solution — the canonical NFR-AC YAML block

Every service in scope of a DAB submission has one block. CI lint rejects merges where the block is missing or fields are empty.

```yaml
# nfr_acceptance_criteria:
nfr_acceptance_criteria:
  service_name: payment-auth-service
  tier: T0                          # one of T0, T1, T2, T3 — see NFR-001

  # === High Availability (HA) ===
  rto_minutes: 5                    # inherited from tier; override only with EA-Board approval
  rpo_seconds: 0
  availability_target_pct: 99.99
  recovery_topology: multi-region-active-active   # see REF-001
  failover_mode: automatic-via-data-plane         # data-plane operations only per AWS WA Reliability

  # === High Performance (HP) ===
  latency:
    p50_ms: 50
    p95_ms: 200
    p99_ms: 500
  throughput_target:
    sustained_rps: 5000
    peak_rps: 15000     # 3x sustained for end-of-month / Tet
  capacity_headroom_pct: 60   # per tier-specific guidance in NFR-002

  # === High Resilience (HR) ===
  failure_modes:
    - id: FM1
      description: Database master loss
      detection: 3 failed health checks within 30 s
      response: automatic standby promotion
      time_to_detect_seconds: 30
      time_to_recover_seconds: 60
    - id: FM2
      description: NAPAS gateway unreachable
      detection: circuit-breaker opens (>50% failure over 60 s)
      response: fail fast, return 503; queue for async retry
      time_to_detect_seconds: 60
      time_to_recover_seconds: variable
    - id: FM3
      description: Region-level network partition
      detection: cross-region health-check fails for 60 s
      response: DNS / Global Accelerator failover to active peer region
      time_to_detect_seconds: 60
      time_to_recover_seconds: 300
  blast_radius: single-cell           # see RES-005
  cell_count: 3
  cells_per_region: 3
  graceful_degradation:
    - "Read-only mode if DB master fails (RES-007)"
    - "Cached response from last successful auth if NAPAS unavailable (with explicit timestamp banner)"

  # === Catalog References (mandatory ≥ 3) ===
  catalog_references:
    - id: NFR-001
      reason: Tier T0 inherits RTO < 5 min, RPO 0
    - id: NFR-002
      reason: Latency budget P95 < 200 ms, P99 < 500 ms
    - id: PRIN-006
      reason: All write endpoints idempotent — Idempotency-Key required
    - id: REF-001
      reason: Multi-region active-active deployment topology
    - id: EIP-024
      reason: Idempotent Receiver pattern on payment-events Kafka topic
    - id: SEC-004
      reason: Card data tokenised pre-storage; HSM-backed keys
    - id: RES-005
      reason: 3-cell deployment for blast-radius limitation

  # === Compliance posture (cross-referenced from each catalog row) ===
  compliance_posture:
    ring_0:
      - source: AWS Well-Architected Reliability
        satisfied_via: REF-001 + NFR-001
    ring_1:
      - source: BCBS 239 §3 (timeliness)
        satisfied_via: NFR-002 P95 enforcement
      - source: BCBS 239 §6 (accuracy)
        satisfied_via: PRIN-006 + EIP-024
      - source: BCBS 230 Principle 6 (Incident Management)
        satisfied_via: NFR-001 T0 tier; REF-001 active-active
      - source: PCI-DSS 4.0 §3.5
        satisfied_via: SEC-004 (if card flows)
    ring_2:
      - source: SBV Circ. 09/2020 §IV.2 ⚠️ (working summary — pending Legal review)
        satisfied_via: NFR-001 + REF-001 + PRIN-006

  # === Testing commitments ===
  test_strategy:
    unit_coverage_pct_min: 80
    integration_coverage:
      - "All catalog-referenced patterns have an integration test exercising them"
    chaos_drills:
      cadence: monthly
      scenarios: ["FM1", "FM2"]
    dr_drills:
      cadence: quarterly
      scenarios: ["FM3 — region failover"]
    load_tests:
      cadence: pre-release
      profile: "peak_rps × 1.2 for 30 minutes"

  # === Sign-off ===
  authored_by: "@solution-architect-handle"
  reviewed_by:
    - "@ea-board"
    - "@sre-lead"
    - "@payments-domain-owner"
  last_updated: 2026-05-09
```

## Implementation Guidelines

### How a DAB author uses this template

1. Copy the YAML block above into `dab/02-key-design-concerns.md` (or `dab/05-detailed-design.md` for service-specific NFRs).
2. Replace every value with your service's actual numbers.
3. Pull the tier-inherited rows from [NFR-001](../nfr/service-tiering-rto-rpo.md) — do not reinvent.
4. List ≥ 3 catalog references in `catalog_references`. Each reference must include a `reason`.
5. Submit MR using the existing template `.gitlab/merge_request_templates/dab-full.md`.

### CI lint rule (Python script for `.gitlab-ci.yml`)

```python
# scripts/lint-nfr-ac.py
import sys, yaml, re
from pathlib import Path

REQUIRED_KEYS = {
    "service_name", "tier", "rto_minutes", "rpo_seconds",
    "availability_target_pct", "recovery_topology",
    "latency", "throughput_target",
    "failure_modes", "blast_radius",
    "catalog_references", "compliance_posture",
}
LATENCY_KEYS = {"p50_ms", "p95_ms", "p99_ms"}

def lint(path: Path) -> list[str]:
    errors = []
    text = path.read_text()
    blocks = re.findall(r"```yaml\n# nfr_acceptance_criteria:.*?\n```", text, re.DOTALL)
    if not blocks:
        return [f"{path}: missing NFR-AC YAML block"]
    for raw in blocks:
        try:
            data = yaml.safe_load(raw.strip("`yaml\n").strip("`"))
        except yaml.YAMLError as e:
            errors.append(f"{path}: invalid YAML — {e}")
            continue
        ac = data.get("nfr_acceptance_criteria", {})
        for k in REQUIRED_KEYS:
            if k not in ac:
                errors.append(f"{path}: missing key 'nfr_acceptance_criteria.{k}'")
        if "latency" in ac:
            for k in LATENCY_KEYS:
                if k not in ac["latency"]:
                    errors.append(f"{path}: missing 'nfr_acceptance_criteria.latency.{k}'")
        refs = ac.get("catalog_references") or []
        if len(refs) < 3:
            errors.append(f"{path}: catalog_references must contain ≥ 3 entries (found {len(refs)})")
    return errors

if __name__ == "__main__":
    fails = []
    for p in Path("dab").rglob("*.md"):
        fails.extend(lint(p))
    for e in fails:
        print(f"FAIL {e}")
    sys.exit(0 if not fails else 1)
```

### How a DAB reviewer uses this block

Mental checklist when reviewing the YAML:

1. Does the tier match the service's actual criticality? Cross-check against tier-assignment rules in [NFR-001](../nfr/service-tiering-rto-rpo.md).
2. Are the latency numbers consistent with the tier? Cross-check [NFR-002](../nfr/latency-budget-model.md).
3. Are failure_modes complete? At minimum: DB master loss, downstream unavailability, region failure.
4. Are ≥ 3 catalog references cited and each with a reason that's coherent?
5. Is compliance_posture filled for all 3 rings (or noted N/A with reason)?
6. Are test_strategy commitments realistic given the team's capacity?

### How a service author uses the template in code

The YAML in the DAB doc is the contract; the code annotations enforce it.

```java
@SpringBootApplication
@ServiceTier(value = ServiceTier.Tier.T0,
             rationale = "On NAPAS 247 hot path; SBV Circ. 09 §IV.2",
             catalogRefs = {"REF-002", "PRIN-006"})
public class PaymentAuthApplication { /* ... */ }

@RestController
public class AuthController {
    @PostMapping("/authorise")
    @Idempotent(ttlHours = 24)
    @LatencyBudget(tier = "T0", p50Millis = 50, p95Millis = 200, p99Millis = 500)
    @CatalogReference("EIP-024")   // optional, for traceability
    public ResponseEntity<AuthResult> authorise(/* ... */) { /* ... */ }
}
```

### How automated checks verify code matches doc

```yaml
# .gitlab-ci.yml — excerpt
nfr-ac-coherence:
  script:
    - python3 scripts/lint-nfr-ac.py
    - python3 scripts/check-annotations-match-dab.py
      --service payment-auth
      --dab dab/05-detailed-design.md
  rules:
    - if: $CI_MERGE_REQUEST_ID
```

## Variants & Trade-offs

| Variant | Use when | Trade-off |
|---|---|---|
| **Full template (this doc)** | Default for any T0/T1 service | Verbose; comprehensive |
| **Slim template** (omit failure_modes details) | T2/T3 services | Faster to author; weaker resilience commitments |
| **External reference** (link to a separate `nfr.yaml` file) | Mono-repo with many services | Cleaner DAB doc; one extra file to maintain |

## NFR Acceptance Criteria

This template *is* the format. Self-application:

- **HA**: forces explicit RTO/RPO commitments per service.
- **HP**: forces explicit latency and throughput budgets.
- **HR**: forces explicit failure-mode enumeration and graceful-degradation behaviours.

## Compliance Mapping

| Layer | Reference | Section/Control | How this satisfies |
|---|---|---|---|
| Ring 0 | NIST SP 800-160 (Systems Security Engineering) | Requirements traceability | YAML block traces every NFR claim back to a catalog ID |
| Ring 1 | Basel BCBS 239 — Principles 1, 2 (governance) | Risk-data infrastructure must have governance | NFR-AC YAML is part of the governance artefact set |
| Ring 1 | Basel BCBS 230 — Principle 1 (Governance) | Operational resilience requires explicit impact-tolerance statements | NFR-AC `rto_minutes` + `failure_modes` are the impact-tolerance statements |
| Ring 2 | SBV Circular 09/2020 — §I (Documentation) | Banks must document IT operational requirements ⚠️ (working summary — pending Legal review) | NFR-AC blocks form the documented requirements set |

## Cost / FinOps Notes

- Template overhead: ~30 minutes for a senior architect to fill in for a new service; ~10 minutes to update for a change.
- CI lint runtime: < 1 second per DAB doc.
- Cost of NOT having: incoherent NFRs across services → over-engineering some, under-engineering others; eventual incident driven by an NFR gap that "everyone assumed someone else owned".

## Threat Model Summary

STRIDE: this template addresses **Repudiation** (recovery commitments are explicit) and **Operational Excellence** (continuous traceability of NFRs to compliance).

- Top threats addressed:
  1. *NFR drift* — values change in code without doc update. Mitigation: CI annotation-vs-doc coherence check.
  2. *Implicit assumptions* — "we'll fix it later". Mitigation: required fields with no defaults.
- Residual:
  1. *Lying* — author writes 99.99% but service genuinely can't deliver. Mitigation: chaos drills (BP-005) and DR drills (BP-002) verify against drill outcomes.

## Operational Runbook (stub)

This is a template not a runtime artefact, so runbook is for the *governance process*:

- **Quarterly NFR review** (per [§11 Maintenance](../../governance/standards/enterprise-architecture-catalog.md#11-maintenance) of the catalog): EA-Board reviews each T0 service's NFR-AC against actual measurements; recommends adjustments.
- **Drift alerts**: nightly job compares declared `latency.p95_ms` to observed P95 over rolling 7d; alerts > 20% drift to FinOps Slack.

## Test Strategy (stub)

- **Unit**: `lint-nfr-ac.py` test fixtures (valid + missing-each-required-key).
- **Integration**: smoke-test merging a DAB MR with an invalid block — must fail CI.
- **End-to-end** (annual): pick 5 random T0 services, verify code annotations match DAB YAML, verify drill outcomes match declared `time_to_recover_seconds`.

## When to Use

- Every DAB submission for a T0, T1, T2 service. Mandatory.
- Every change MR that affects RTO/RPO/latency/throughput numbers must update the NFR-AC block.

## When NOT to Use

- T3 internal tooling that's already opted out of NFR commitments (with explicit EA-Board waiver).

## Related Patterns

- [NFR-001 Service Tiering + RTO/RPO Matrix](../nfr/service-tiering-rto-rpo.md)
- [NFR-002 Latency Budget Model](../nfr/latency-budget-model.md)
- [PRIN-006 Idempotency-by-default](../principles/idempotency-by-default.md)
- [COMP-001 Compliance Mapping Matrix](../compliance/compliance-mapping-matrix.md)
- [REF-001 Multi-Region Active-Active](../reference-architectures/multi-region-active-active.md)
- The DAB template at `templates/dab-full/` consumes this NFR-AC block

## References

- Catalog §9 Acceptance Criteria
- Existing DAB templates at `templates/dab-full/02-key-design-concerns.md`, `templates/dab-full/05-detailed-design.md` — to be updated to embed this template

---

**Key Takeaway**: Every DAB has a single, lint-checked NFR-AC YAML block declaring tier, RTO/RPO, latency, throughput, failure modes, blast radius, ≥3 catalog references, and 3-ring compliance posture. Code annotations enforce the same numbers; CI verifies coherence.
