# Chaos Engineering

Status: Draft | Last Reviewed: 2026-05-09 | Owner: @sre-lead
Catalog ID: BP-005 | Radii
Tier Applicability: T0 (mandatory monthly drills), T1 (quarterly drills)

## Problem Statement

Resilience claims that aren't tested are aspirational. A T0 service may declare RTO < 5 min, multi-region active-active, cell-based blast radius — and yet, on the day of a real incident, no team has actually verified the failover path under pressure. Banking platforms cannot accept that risk. Chaos Engineering is the discipline of *deliberately injecting failures into production systems* to verify the resilience claims continuously, with bounded blast radius.

## Context

Reach for this practice when:

- Onboarding a new T0 / T1 service — declare its first chaos hypothesis on day 1.
- Validating that [REF-001 Multi-Region Active-Active](../reference-architectures/multi-region-active-active.md), [RES-005 Cell-Based Architecture](../patterns/resilience/cell-based-architecture.md), [RES-002 Circuit Breaker](../patterns/resilience/circuit-breaker.md) actually behave as designed.
- Closing the loop on an incident postmortem ([BP-010](incident-postmortem.md)) — the fix becomes a recurring chaos experiment.
- Pre-Tet readiness drills — verify the platform survives the annual peak before it arrives.

## Solution — the chaos cycle

```mermaid
graph LR
    Hypothesis[1. State a hypothesis<br/>"Service X tolerates Y failure<br/>without exceeding budget Z"]
    --> Scope[2. Define blast radius<br/>(start small: 1 pod / 1 cell / staging)]
    --> Plan[3. Plan rollback &amp; abort criteria]
    --> Run[4. Inject the failure]
    --> Measure[5. Measure golden signals<br/>against baseline]
    --> Learn[6. Learn — confirm or refute hypothesis]
    --> Automate[7. Automate the experiment<br/>add to recurring schedule]
    Automate --> Hypothesis
```

### The five principles (Principles of Chaos Engineering)

1. Build a hypothesis around steady-state behaviour.
2. Vary real-world events.
3. Run experiments in production.
4. Automate experiments to run continuously.
5. Minimise blast radius.

### Drill cadence per tier

| Tier | Cadence | Examples |
| --- | --- | --- |
| T0 | Monthly cell-level | Drain one cell; kill a pod; inject downstream latency |
| T0 | Quarterly region-level | Failover Region A → Region B (REF-001); Aurora Global promotion |
| T0 | Annually | Full-region simulated outage including DNS / Global Accelerator failover |
| T1 | Quarterly | Cell-level + downstream-failure scenarios |
| T1 | Annually | Region failover |
| T2 | Annually | Backup-restore drill (BP-002) |

## Implementation Guidelines

### Tool stack

| Layer | Tool | Use |
|---|---|---|
| Kubernetes pod / network | Chaos Mesh / LitmusChaos | Pod kill, network partition, latency injection, DNS failure |
| Application-level | Spring Boot fault-injection (custom annotation or AOP) | Inject business-logic faults (e.g., 5% of payments fail) |
| AWS infrastructure | AWS Fault Injection Simulator (FIS) | EC2 termination, AZ disruption, AWS API throttling |
| Game-day orchestration | Gremlin or custom runbook | Coordinate human-in-the-loop drills |

### Java / Spring — application-level fault injection

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ChaosFault {
    /** Probability 0.0..1.0 that the fault is injected. */
    double probability() default 0.0;
    String featureFlag();   // wired to Unleash / LaunchDarkly
    Class<? extends Throwable> exception() default RuntimeException.class;
}

@Aspect
@Component
@RequiredArgsConstructor
public class ChaosFaultAspect {
    private final FeatureFlagClient flags;

    @Around("@annotation(fault)")
    public Object inject(ProceedingJoinPoint pjp, ChaosFault fault) throws Throwable {
        if (flags.isEnabled(fault.featureFlag())
                && ThreadLocalRandom.current().nextDouble() < fault.probability()) {
            throw fault.exception().getDeclaredConstructor(String.class)
                .newInstance("Injected by chaos: " + fault.featureFlag());
        }
        return pjp.proceed();
    }
}

// usage — only triggers in environments where the feature flag is enabled
@ChaosFault(probability = 0.05, featureFlag = "chaos.payment-auth.fault")
public AuthResult authorise(AuthRequest req) { /* ... */ }
```

Production safety: the fault is gated by a feature flag with explicit allow-list of environments and a monitored kill-switch.

### Kubernetes — Chaos Mesh CRD

```yaml
# Cell-drain drill: kill all pods in cell-A every 30 days at 10:00 UTC+7 (post-peak hour)
apiVersion: chaos-mesh.org/v1alpha1
kind: PodChaos
metadata:
  name: cell-a-drain-drill
  namespace: payment-auth-cell-a
spec:
  action: pod-kill
  mode: all
  selector:
    namespaces: [payment-auth-cell-a]
    labelSelectors:
      app: payment-auth
  scheduler:
    cron: "0 10 1 * *"   # 1st of each month, 10:00
  duration: "5m"
```

### Game-day playbook (template)

A game-day is a coordinated, human-in-the-loop drill — typically annual, regional-scale.

```markdown
# Game-Day: Payment Auth Region-A Outage Drill

**Date**: 2026-09-15  **Duration**: 2 hours
**Hypothesis**: T0 payment-auth survives loss of Region vn-south-1 with RTO < 5 min, RPO = 0.

## Pre-drill (T-7 days)
- [ ] Notify stakeholders (Customer Service, Compliance, Communications)
- [ ] Verify current RTO/RPO metrics baseline
- [ ] Identify abort criteria (e.g., > 5% sustained customer-facing 5xx for > 1 min)

## Drill (T+0)
- [ ] T+0:  Disable Region A health checks; observe Global Accelerator failover
- [ ] T+1m: Confirm traffic to Region B (verify by Grafana panel + smoke test)
- [ ] T+2m: Aurora Global promotion verified
- [ ] T+5m: Customer-facing flows verified (test transaction)

## Post-drill
- [ ] Restore Region A; reverse-direction failover
- [ ] Document RTO achieved, deviations
- [ ] Update runbooks with any gaps

## Communication
- [ ] T-1h: notify SRE on-call channels
- [ ] T+0:  status page banner ("planned maintenance")
- [ ] T+5m: status page update on completion
```

### Frontend / Mobile

Client-side chaos: simulate poor network conditions on a percentage of customer cohorts (using build-flag controlled HTTP-client middleware) to verify graceful degradation. Particularly important for [REF-002 Real-Time Payments](../reference-architectures/real-time-payments-napas.md) where mobile users on flaky connections must still see correct retry / status messages.

## Variants & Trade-offs

| Variant | When | Trade-off |
| --- | --- | --- |
| **Staging-only chaos** | Service maturity is low | Safe but doesn't catch real prod issues |
| **Production chaos with bounded blast** (default for T0/T1) | Mature services | Catches real issues; requires discipline |
| **Continuous "always-on" chaos** | Highly mature services | Maximum signal; minimum operational overhead per drill |
| **Game-days only** | Regional / annual drills | Higher coordination cost; lower frequency; broader scope |

## NFR Acceptance Criteria

- **HA**: every T0 service has at least one production chaos experiment on a monthly schedule that verifies its declared RTO. No T0 ships without this.
- **HP**: chaos drills measure latency P95/P99 against [NFR-002 budget](../nfr/latency-budget-model.md) during the drill. Drill aborted if P95 exceeds 1.5× budget for > 1 minute.
- **HR**: drill-coverage matrix mapped against the service's documented `failure_modes` (per [TPL-001](../templates/nfr-acceptance-criteria-dab.md)). 100% coverage of declared failure modes is the target.

## Compliance Mapping

| Layer | Reference | Section/Control | How this satisfies |
|---|---|---|---|
| Ring 0 | Principles of Chaos Engineering (principlesofchaos.org) | 5 principles | Practice anchored in industry standard |
| Ring 0 | NIST SP 800-53 CP-4 (Contingency Plan Testing) | "Test the contingency plan" | Chaos drills are the active verification of contingency plans |
| Ring 0 | Google SRE Book Chapter 17 (Testing for Reliability) | "Disaster role-playing" / DiRT | Same idea; same intent |
| Ring 1 | Basel BCBS 230 Principle 3 (BCP and Testing) | Operational resilience requires regular continuity testing ⚠️ (working summary — pending Legal review) | Drill cadence directly satisfies the testing requirement |
| Ring 2 | SBV Circular 09/2020 §IV.2 | Operational continuity ⚠️ (working summary — pending Legal review) | Drill artefacts evidence Techcombank exercises continuity |

## Cost / FinOps Notes

| Item | Cost driver | Order of magnitude |
|---|---|---|
| Tooling (Chaos Mesh / FIS) | Compute for the chaos infra itself | ~$200/month |
| Engineering time per drill | 1–2 person-days per cell-level drill; 5 person-days per game-day | $X/year |
| Production traffic absorbed by drill | Bounded — design intent is no customer impact | Trace-level only |
| Failed drills causing real impact | Drill plan minimises this with abort criteria | If it happens, it's a real incident — $$$ |

**Cost of NOT doing chaos**: the resilience investments (multi-region, cell-based, circuit breakers) are unverified — meaning the spend is a guess. A real-incident "first time the failover ran" costs more than the entire annual chaos programme.

## Threat Model Summary

STRIDE: chaos engineering is itself a threat *vector* — a misused chaos tool can cause a real outage. Therefore the practice has its own threat model.

- **Top 3 threats addressed (by chaos, against the platform)**:
  1. *Untested resilience claims* — drill confirms or refutes.
  2. *Atrophied muscle memory* — drills keep on-call playbooks current.
  3. *Hidden coupling* — drills surface unexpected cross-cell or cross-region dependencies.
- **Top 3 residual threats (introduced by chaos itself)**:
  1. *Drill mis-targets production accidentally* — mitigation: feature-flag gating; environment guards; explicit drill identifiers in tooling.
  2. *Insufficient blast-radius bounding* — mitigation: 5-minute hard-stop on every drill; on-call observer authority to abort.
  3. *Drills that don't reflect real failure modes* — mitigation: drill scenarios derived from real incidents (BP-010 postmortems) and from declared failure modes (TPL-001).

## Operational Runbook

- **Drill calendar**: published quarterly in `governance/dab-process/` with all T0/T1 drill dates.
- **Pre-drill checklist** (per drill):
  1. Stakeholders notified ≥ 24 h ahead.
  2. Baseline metrics captured.
  3. Abort criteria documented and on-call observer present.
  4. Status-page banner prepared (don't publish; have it ready).
- **During-drill**:
  1. Inject failure per CRD / FIS / playbook.
  2. Observer monitors abort criteria (golden signals + business KPIs).
  3. Abort and rollback if thresholds breached.
- **Post-drill**:
  1. Document achieved RTO/RPO; deviations from hypothesis.
  2. File any new findings as bugs / runbook updates.
  3. Schedule the next drill.
- **Alerts**:
  - `Chaos_Drill_Scheduled`: 24 h before; PagerDuty informational.
  - `Chaos_Drill_Active`: drill is running; mute non-critical alerts in the affected scope.
  - `Chaos_AbortCriteria_Triggered`: drill auto-aborted; severity High (suggests real fragility).

## Test Strategy

- **Self-test**: chaos tooling has its own unit tests (the fault-injection annotation aspect; the CRD schema validation).
- **Drill efficacy test**: periodic table-top exercise where the drill is *not* injected but the on-call team is asked to walk through their response — verifies understanding even when tool fails.

## When to Use

- **Mandatory** for T0 services (monthly cell-level minimum).
- **Recommended** for T1 services.
- Recommended for T2 services when capacity planning or DR validation matters.

## When NOT to Use

- Services where production blast-radius cannot be bounded — fix the architecture first ([RES-005 Cell-Based](../patterns/resilience/cell-based-architecture.md)).
- During regulatory blackout windows (e.g., last 3 business days of Tet).
- During declared incidents — focus on recovery, schedule drills afterwards.

## Related Patterns

- [RES-001 Bulkhead](../patterns/resilience/bulkhead-isolation.md) / [RES-005 Cell-Based](../patterns/resilience/cell-based-architecture.md) — bound the chaos blast radius
- [RES-002 Circuit Breaker](../patterns/resilience/circuit-breaker.md) — chaos drills exercise CB behaviour
- [REF-001 Multi-Region Active-Active](../reference-architectures/multi-region-active-active.md) — annual game-day target
- [BP-002 Disaster Recovery Playbook](disaster-recovery-playbook.md) — DR drills are a chaos subset
- [BP-007 Golden Signals (SRE)](golden-signals-sre.md) — drill measurement basis
- [BP-010 Incident Postmortem](incident-postmortem.md) — postmortems generate new chaos hypotheses
- [TPL-001 NFR Acceptance Criteria DAB Template](../templates/nfr-acceptance-criteria-dab.md) — `failure_modes` field drives drill coverage

## References

- Principles of Chaos Engineering — principlesofchaos.org
- Google SRE Book Chapter 17 (Testing for Reliability)
- "Chaos Engineering" — O'Reilly book by Casey Rosenthal & Nora Jones
- Netflix Tech Blog — Chaos Monkey origins
- AWS Fault Injection Simulator
- Chaos Mesh / LitmusChaos documentation

---

**Key Takeaway**: Resilience untested is resilience aspirational. T0 = monthly cell-level + quarterly region-level + annual game-day. Bounded blast radius. Hypothesis-driven. Automated where possible. Pair with cell-based architecture so production drills are safe.
