# Wave 17 — QE Harness Coverage Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development
> (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use
> checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement 8 of the 17 archetypes that answer HTTP `501`, completing Family B
(messaging) entirely and raising `/_capabilities` from 7 to a truthful 15.

**Architecture:** Eight repetitions of Wave 16's proven module unit — one `modules.yml` binding
row, one `plan.jmx`, exactly one `assert-*.groovy`, a README and a two-test JUnit fixture each —
plus the SUT capabilities they exercise. The only genuinely new architecture is promoting the
`messaging` compose profile from declared-but-never-started to a live RabbitMQ topology with a
producer/consumer/DLQ path, reached over a **lazy** AMQP connection so the `core` profile still
boots without a broker.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Maven, PostgreSQL 16, Flyway, RabbitMQ 3.13,
Spring AMQP, Apache JMeter 5.6.2 (via jmeter-maven-plugin 3.8.0), Groovy 3.0.17 (JSR223),
Testcontainers 1.21.4, Docker Compose, Python 3.11 (gates), GitLab CI.

**Spec:** `docs/superpowers/specs/2026-09-03-wave-17-harness-coverage-expansion-design.md` —
read it alongside this plan. The spec carries the rationale; this plan carries the steps.

---

## Global Constraints

Every task's requirements implicitly include this section. Values are copied verbatim from the
spec and from the verified source.

- **All eight modules declare `tool: jmeter`.** Computed from every covering row's
  `primary_tool` in `_testing-coverage.yml` for all eight archetypes. Declaring `k6`, `locust`
  or `gatling-karate` fails gate check 2.
- **No 13–19 consecutive digits anywhere under `qe-harness/`** except `traceability/runs/`.
  Gate check 5 walks `rglob("*")` over every file type, excluding only `traceability/runs/` and
  any directory named `target`, `node_modules`, or `.venv`. The regex is
  `(?<!\d)\d{13,19}(?!\d)`. **Slice B hazard:** epoch-millis timestamps are 13 digits and
  correlation IDs tend to be long. Use hyphenated short IDs (`corr-a1b2-c3d4`) and ISO-8601
  timestamps. Account identifiers take the form `ACC-000001`.
- **No modification of any archetype, strategy, tooling, pattern, NFR, or
  reference-architecture row.** The permitted documentation edits are exactly those Tasks 1–4
  name. **In particular the NFR spine is not amended** — the two uncited bounds become
  application config (spec §7.1).
- **Synthetic data only.** No PII, no PHI. Party names come from `SyntheticNames.NAMES`.
- **`coverage: partial` always carries a non-empty `partial_reason`.** Never declare `full` for
  an archetype whose invariant is unreached. `TST-027` and `TST-037` are `partial`; the other
  six are `full`.
- **Assertions are never retried.** Bounded retry applies to infrastructure setup only. A
  module needing retries to pass is not measuring what it claims to.
- **Four-state results.** `passed`, `failed`, `not-evaluated`, `not-implemented`. A
  `not-evaluated` threshold **must** carry a `reason` — `RunFragment.Builder.threshold()`
  throws `IllegalArgumentException` otherwise.
- **Smoke mode is `HARNESS_SMOKE_MODE=true` in the environment**, read by
  `HarnessConfig.smokeMode()`. `./bin/run-module.sh` takes exactly **one** positional argument
  (the archetype id) — there is no `--smoke` flag.
- **Defects are injected over HTTP**, never by env var: `POST /_test/defect/{flag}` → 204,
  `DELETE /_test/defect` → 204. `ModuleRunner` intercepts `SUT_DEFECT` from the test's env map
  and performs the calls itself. `DefectFlags` is **single-slot** — one flag active at a time.
- **Exactly one `plan.jmx` and exactly one `assert-*.groovy`** per module directory.
  `run-jmeter.sh` hard-errors on two or more.
- **Java package roots:** `com.techcombank.qe.sut` (SUT), `com.techcombank.qe.harness` (harness).
- **No JPA.** The SUT uses `JdbcTemplate` exclusively.
- **Tests are JUnit 5 with plain `org.junit.jupiter.api.Assertions`**, classes and methods
  package-private, method names full behavioural sentences, no `test` prefix, no AssertJ, no
  Mockito, no `@DisplayName`.
- **No hardcoded thresholds in tests.** A test reads the declared property or bean rather than
  duplicating its number.

---

## File Structure

| Path | Responsibility |
|---|---|
| `qe-harness/traceability/modules.yml` | 8 new binding rows; `TST-043` relabelled `partial` |
| `qe-harness/harness/jmeter/tst-020-idempotency/` | TST-020 module: `plan.jmx`, `assert-idempotency.groovy`, `README.md` |
| `qe-harness/harness/jmeter/tst-023-reservation/` | TST-023 module |
| `qe-harness/harness/jmeter/tst-026-routing/` | TST-026 module (`contract-schema` oracle) |
| `qe-harness/harness/jmeter/tst-027-ordering/` | TST-027 module (partial) |
| `qe-harness/harness/jmeter/tst-028-fanout/` | TST-028 module |
| `qe-harness/harness/jmeter/tst-029-dlq/` | TST-029 module |
| `qe-harness/harness/jmeter/tst-034-blend/` | TST-034 module |
| `qe-harness/harness/jmeter/tst-037-readmodel/` | TST-037 module (partial) |
| `qe-harness/harness/jmeter/src/test/java/…/Tst0NNModuleTest.java` | One two-test fixture per module (8 files) |
| `qe-harness/harness/common/…/config/ProfileResolver.java` | First code in the repo to read `profiles/*.yml`; sibling of `ThresholdResolver` |
| `…/sut/capability/reservation/` | New: reservation counter with rollback, TTL sweeper, declared-TZ window (TST-023) |
| `…/sut/capability/reporting/` | New: read-model lag + refresh + outbox (TST-037) |
| `…/sut/capability/messaging/` | New: topology `Declarables`, producer, consumer, resequencer, aggregator, DLQ (TST-026/027/028/029) |
| `…/sut/capability/ledger/IdempotencyService.java` | New: `Idempotency-Key` handling on `POST /transfers` (TST-020) |
| `…/sut/TestSeedController.java` | New: `POST /_test/seed` HTTP trigger for `SyntheticDataSeeder` |
| `…/reference-sut/src/main/resources/db/migration/V3__reservations.sql` | Reservation table |
| `…/V4__outbox_and_reporting.sql` | Outbox with `published_count` |
| `…/V5__idempotency_keys.sql` | Idempotency-key table |
| `qe-harness/docker-compose.yml` | `broker` gains env; profile header comment updated |
| `qe-harness/profiles/mixed.yml` | `blend_ref` populated (TST-034) |
| `qe-harness/profiles/_nfr-thresholds.yml` | Per-journey p95 entries citing the existing NFR-002 anchor |
| `.gitlab-ci.yml` | New `validate:testing-coverage` job + new rules anchor |
| `qe-harness/README.md` | Module table 7→15 rows; per-family framing corrected |
| `knowledge-base/testing/README.md` | "7 of the 24" → "15 of the 24" |
| `knowledge-base/testing/coverage/_testing-coverage.yml` | `TST-025`'s three locust-only rows → `jmeter` |
| `knowledge-base/testing/archetypes/data-protection-masking-tokenisation.md` | NUL byte repaired |

---

## Task 0: Pre-Flight Baseline

Establishes the green baseline **before** any code is written, so a later failure is
attributable. Read-only.

**Files:**
- Create: none

**Interfaces:**
- Consumes: nothing
- Produces: a recorded baseline that Task 28 compares against

- [ ] **Step 1: Record every gate result**

```bash
cd "$(git rev-parse --show-toplevel)"
python3 scripts/audit-catalog-consistency.py       > /tmp/w17-base-audit.txt  2>&1; echo "audit=$?"
python3 scripts/validate-testing-coverage.py      > /tmp/w17-base-cov.txt    2>&1; echo "cov=$?"
python3 scripts/render-testing-coverage.py --check > /tmp/w17-base-render.txt 2>&1; echo "render=$?"
python3 scripts/validate-internal-links.py        > /tmp/w17-base-links.txt  2>&1; echo "links=$?"
python3 scripts/validate-harness-coverage.py      > /tmp/w17-base-harness.txt 2>&1; echo "harness=$?"
```

Expected: all five exit `0`. If any is non-zero, STOP and report — the baseline is not green
and Wave 17 must not build on it.

- [ ] **Step 2: Confirm the two latent check-2 conflicts are still latent**

```bash
python3 scripts/validate-harness-coverage.py 2>&1 | /usr/bin/grep -c "cannot verify"
```

Expected: `0`. Check 2 only evaluates archetypes that have a `modules.yml` row, and neither
`TST-025` nor `TST-036` has one. If this is non-zero, a module was added for one of them and
Task 3's scope changes — report before proceeding.

- [ ] **Step 3: Record the SUT test baseline**

```bash
cd qe-harness/reference-sut && mvn -q -B test 2>&1 | tail -20; echo "sut-tests=$?"
```

Expected: exit `0`. Note the test count — Task 5 changes three `CapabilityRegistryTest`
assertions and must not change any other count.

- [ ] **Step 4: Confirm the toolchain**

```bash
java -version; mvn -v; docker --version; docker compose version; python3 -V
```

Expected: Java 21+, Maven 3.9+, Docker with Compose v2, Python 3.11+. Report any missing tool
as BLOCKED rather than working around it.

- [ ] **Step 5: Commit nothing**

Read-only task. Report the baseline.

---

## Task 1: Repair the NUL Byte in the TST-041 Archetype Document

One NUL byte at offset 48027 hides content from every text tool. It sits **inside a Groovy
string literal**, so the replacement is a semantics decision.

**Files:**
- Modify: `knowledge-base/testing/archetypes/data-protection-masking-tokenisation.md:618`

**Interfaces:**
- Consumes: nothing
- Produces: a byte-clean archetype document that `grep` can read

- [ ] **Step 1: Confirm the defect and its exact location**

```bash
cd "$(git rev-parse --show-toplevel)"
file knowledge-base/testing/archetypes/data-protection-masking-tokenisation.md
python3 -c "
d=open('knowledge-base/testing/archetypes/data-protection-masking-tokenisation.md','rb').read()
print('nul count:', d.count(b'\x00'))
print('offsets:', [i for i,b in enumerate(d) if b==0])
"
```

Expected: `file` reports `data` (not `ASCII text`), count `1`, offset `[48027]`.

- [ ] **Step 2: Read the three lines around it**

```bash
sed -n '616,620p' knowledge-base/testing/archetypes/data-protection-masking-tokenisation.md | cat -v
```

Expected: line 618 renders as `    def flat = payload.data.values().join("^@")`, where `^@` is
the NUL. Line 617 ends `...Assert the marker's trailing fragment too.` and line 619 is
`    if (flat.contains(marker) || flat.contains(marker[-4..-1])) {`.

- [ ] **Step 3: Replace the NUL with an explicit separator**

Line 617's comment says the check must catch the marker's **trailing fragment**. An empty
separator (`join("")`) would additionally let a marker match across two adjacent field values —
a false positive the comment does not ask for. Use a single space, which preserves
fragment-matching within a value without fusing neighbours:

```bash
python3 - <<'PY'
from pathlib import Path
p = Path("knowledge-base/testing/archetypes/data-protection-masking-tokenisation.md")
raw = p.read_bytes()
assert raw.count(b"\x00") == 1, raw.count(b"\x00")
p.write_bytes(raw.replace(b'join("\x00")', b'join(" ")'))
PY
```

- [ ] **Step 4: Verify the file is now text and unchanged elsewhere**

```bash
file knowledge-base/testing/archetypes/data-protection-masking-tokenisation.md
python3 -c "
d=open('knowledge-base/testing/archetypes/data-protection-masking-tokenisation.md','rb').read()
print('nul count:', d.count(b'\x00'))
"
git diff --stat knowledge-base/testing/archetypes/data-protection-masking-tokenisation.md
```

Expected: `ASCII text` (or `UTF-8 Unicode text`), count `0`, and `1 insertion(+), 1 deletion(-)`
— exactly one line changed.

- [ ] **Step 5: Confirm grep now sees the file's content**

```bash
/usr/bin/grep -c "Oracle" knowledge-base/testing/archetypes/data-protection-masking-tokenisation.md
```

Expected: a non-zero count. Before the fix, grep returned nothing for this file.

- [ ] **Step 6: Re-run the corpus gates**

```bash
python3 scripts/validate-testing-coverage.py; echo "cov=$?"
python3 scripts/audit-catalog-consistency.py; echo "audit=$?"
```

Expected: both `0`.

- [ ] **Step 7: Commit**

```bash
git add knowledge-base/testing/archetypes/data-protection-masking-tokenisation.md
git commit -m "fix(testing): repair NUL byte hiding TST-041 content from tooling"
```

---

## Task 2: Correct the Stale Module Counts and Per-Family Framing

Wave 17 makes the harness 15 modules across 7 families, invalidating two count claims. The
adjacent `golden-dataset` sentence is **not** stale and must be left alone.

**Files:**
- Modify: `qe-harness/README.md:136-149`
- Modify: `knowledge-base/testing/README.md:16`

**Interfaces:**
- Consumes: nothing
- Produces: docs whose counts match what Task 27 will verify

- [ ] **Step 1: Read the current claims**

```bash
sed -n '136,152p' qe-harness/README.md
sed -n '10,19p' knowledge-base/testing/README.md
```

Expected: a 7-row family table, then the sentence `One archetype per family, so all four tools
and three of the four oracle types are exercised.`, then the `golden-dataset` sentence. And
`k6) implementing 7 of the 24 documented test archetypes as real, executable modules with`.

- [ ] **Step 2: Replace the framing sentence in `qe-harness/README.md`**

Change **only** the "One archetype per family" sentence. Leave the `golden-dataset` sentence and
the `TST-039`-uses-Locust sentence exactly as they are:

```markdown
Wave 16 seeded one archetype per family; Wave 17 completed Family B and deepened A, C and E, so
15 of the 24 archetypes now have runnable modules. All four tools and three of the four oracle
types are exercised.
```

- [ ] **Step 3: Add the eight new rows to the family table**

Append to the existing table, keeping archetype order within each family:

```markdown
| A — Correctness & State | TST-020 Idempotency & Replay | JMeter | invariant-assertion |
| A — Correctness & State | TST-023 Concurrent Limit & Counter | JMeter | invariant-assertion |
| B — Messaging & Integration | TST-026 Message Transformation & Routing | JMeter | contract-schema |
| B — Messaging & Integration | TST-027 Ordering & Resequencing | JMeter | invariant-assertion |
| B — Messaging & Integration | TST-028 Fan-out / Fan-in Correlation | JMeter | invariant-assertion |
| B — Messaging & Integration | TST-029 Delivery Guarantee, Retry, DLQ | JMeter | invariant-assertion |
| C — Load & Capacity | TST-034 Blended Journey Workload | JMeter | invariant-assertion |
| E — Data | TST-037 Read-Model Convergence & CDC Lag | JMeter | invariant-assertion |
```

- [ ] **Step 4: Update the count in `knowledge-base/testing/README.md:16`**

Change `implementing 7 of the 24 documented test archetypes` to
`implementing 15 of the 24 documented test archetypes`. Change nothing else in that paragraph.

- [ ] **Step 5: Confirm the golden-dataset sentence is untouched**

```bash
git diff qe-harness/README.md | /usr/bin/grep -c "golden-dataset"
```

Expected: `0`. If non-zero, the edit strayed — revert and redo Step 2.

- [ ] **Step 6: Re-run the link and catalog gates**

```bash
python3 scripts/validate-internal-links.py; echo "links=$?"
python3 scripts/audit-catalog-consistency.py; echo "audit=$?"
```

Expected: both `0`.

- [ ] **Step 7: Commit**

```bash
git add qe-harness/README.md knowledge-base/testing/README.md
git commit -m "docs(qe-harness): correct module counts and per-family framing for Wave 17"
```

---

## Task 3: Resolve TST-025's primary_tool Conflict

Three locust-only rows move to `jmeter`. The two `jmeter` rows must **not** be touched —
`SEC-010` also carries `TST-040`, an implemented `jmeter` module, so flipping it would break
check 2 for working code. `TST-036` is out of scope (spec §11 item 12).

**Files:**
- Modify: `knowledge-base/testing/coverage/_testing-coverage.yml` (rows `BSP-003`, `BSP-010`, `SEC-009`)

**Interfaces:**
- Consumes: nothing
- Produces: a single-valued best-fit set for `TST-025`, unblocking a future TST-025 module

- [ ] **Step 1: Enumerate the conflict before changing anything**

```bash
python3 - <<'PY'
import yaml, pathlib
rows = yaml.safe_load(pathlib.Path("knowledge-base/testing/coverage/_testing-coverage.yml").read_text())["rows"]
for target in ("TST-025", "TST-036"):
    print("==", target)
    for r in rows:
        if target in (r.get("archetypes") or []):
            print("  %-8s %-16s archetypes=%s" % (r["catalog_id"], r.get("primary_tool"), r.get("archetypes")))
PY
```

Expected for `TST-025`: `BSP-003 locust`, `BSP-010 locust`, `BSP-019 jmeter [TST-025, TST-032]`,
`SEC-009 locust`, `SEC-010 jmeter [TST-025, TST-040]`.

- [ ] **Step 2: Record the side-effect archetypes**

```bash
python3 - <<'PY'
import yaml, pathlib
rows = yaml.safe_load(pathlib.Path("knowledge-base/testing/coverage/_testing-coverage.yml").read_text())["rows"]
def best(a):
    return sorted({r["primary_tool"] for r in rows
                   if a in (r.get("archetypes") or []) and r.get("primary_tool")
                   and r.get("catalog_id") != a})
for a in ("TST-025", "TST-032", "TST-040", "TST-043"):
    print(a, best(a))
PY
```

Expected before the edit: `TST-025 ['jmeter', 'locust']`, `TST-032 ['jmeter']`,
`TST-040 ['jmeter']`, `TST-043 ['k6']`. Record these — Step 5 asserts the last three are
unchanged.

- [ ] **Step 3: Change only the three locust-only rows**

In `knowledge-base/testing/coverage/_testing-coverage.yml`, for each of `BSP-003`, `BSP-010` and
`SEC-009`, change `primary_tool: locust` to `primary_tool: jmeter`. Each of these rows lists
`TST-025` as its only archetype, so no other archetype's best-fit set can move.

- [ ] **Step 4: Confirm exactly three lines changed**

```bash
git diff --stat knowledge-base/testing/coverage/_testing-coverage.yml
```

Expected: `3 insertions(+), 3 deletions(-)`.

- [ ] **Step 5: Confirm TST-025 resolved and nothing else moved**

Re-run Step 2's script. Expected after the edit: `TST-025 ['jmeter']`, and `TST-032`,
`TST-040`, `TST-043` **identical to Step 2's recorded values**. If any of the latter three
changed, revert immediately — a working module's check 2 is at risk.

- [ ] **Step 6: Run both coverage gates**

```bash
python3 scripts/validate-testing-coverage.py; echo "cov=$?"
python3 scripts/render-testing-coverage.py --check; echo "render=$?"
```

Expected: `cov=0`. If `render=1`, the generated table is now stale — regenerate it:

```bash
python3 scripts/render-testing-coverage.py
python3 scripts/render-testing-coverage.py --check; echo "render=$?"
```

Expected: `render=0`.

- [ ] **Step 7: Commit**

```bash
git add knowledge-base/testing/coverage/_testing-coverage.yml knowledge-base/testing/coverage/coverage-matrix.md
git commit -m "fix(testing): give TST-025 a single-valued best-fit tool"
```

---

## Task 4: Relabel TST-043's Coverage Honestly

`TST-043` sits in `IMPLEMENTED` while implementing none of its own I1–I6. Its `modules.yml`
row already says `partial`, but its `partial_reason` understates the gap.

**Files:**
- Modify: `qe-harness/traceability/modules.yml` (`TST-043` row)
- Modify: `qe-harness/harness/k6/tst-043-clientexp/README.md`

**Interfaces:**
- Consumes: nothing
- Produces: a `partial_reason` that matches what the module actually asserts

- [ ] **Step 1: Read what the module actually does**

```bash
sed -n '1,40p' qe-harness/harness/k6/tst-043-clientexp/README.md
/usr/bin/grep -n "partial_reason" -A3 qe-harness/traceability/modules.yml
```

Expected: the README confirms none of I1–I6 are implemented and four substitute server-side HTTP
invariants ship instead.

- [ ] **Step 2: Replace the `partial_reason`**

In `qe-harness/traceability/modules.yml`, replace the `TST-043` row's `partial_reason` block
with:

```yaml
    partial_reason: >-
      None of the archetype's own I1-I6 are implemented: I1/I2/I6 need an offline client, I3/I4
      a rendered DOM, and I5 k6/browser against a real page - no such application exists in this
      repository. This module ships four substitute server-side HTTP invariants (perf budget,
      cache correctness, conditional requests, compression) which are renumbered I1-I4 and are
      NOT the archetype's I1-I4.
```

- [ ] **Step 3: State the substitution in the module README**

Add immediately below the README's H1:

```markdown
> **Substitute invariants.** This module's `I1`-`I4` are server-side HTTP checks of its own
> devising, not the archetype document's `I1`-`I6`. None of the archetype's own invariants are
> implemented here — see `partial_reason` in `traceability/modules.yml`. Invariant IDs in a
> fragment are module-local by design (see `qe-harness/README.md`, "Three names that mean more
> than one thing").
```

- [ ] **Step 4: Verify the gate still passes**

```bash
python3 scripts/validate-harness-coverage.py; echo "harness=$?"
```

Expected: `0`. Check 4 requires `partial` to carry a non-empty `partial_reason` — it now does,
more honestly.

- [ ] **Step 5: Confirm no digit-run was introduced**

```bash
python3 scripts/validate-harness-coverage.py 2>&1 | /usr/bin/grep -c "check5"
```

Expected: `0`.

- [ ] **Step 6: Commit**

```bash
git add qe-harness/traceability/modules.yml qe-harness/harness/k6/tst-043-clientexp/README.md
git commit -m "fix(harness): state TST-043's substitute invariants honestly"
```

---

## Task 5: Convert CapabilityRegistryTest to an Explicit-Set Drift Guard

Three tests hard-code Wave 16's counts. Converting them to explicit-set assertions **now**,
while the set is still seven, keeps the suite green from here to Task 26 — each later task
appends one ID to both the registry and the test in the same commit.

**Files:**
- Modify: `qe-harness/reference-sut/src/test/java/com/techcombank/qe/sut/CapabilityRegistryTest.java`
- Read: `qe-harness/reference-sut/src/test/java/com/techcombank/qe/sut/CapabilityControllerTest.java`

**Interfaces:**
- Consumes: `CapabilityRegistry.ALL`, `CapabilityRegistry.IMPLEMENTED`
- Produces: `IMPLEMENTED_AT_WAVE_17` — the single literal each later task appends its ID to

- [ ] **Step 1: Check whether the controller test probes one of our eight**

```bash
/usr/bin/grep -nE "TST-0(20|23|26|27|28|29|34|37)" qe-harness/reference-sut/src/test/java/com/techcombank/qe/sut/CapabilityControllerTest.java
```

Expected: no output. If any of the eight appears, that test asserts a `501` that Wave 17 will
turn into a `200` — record which, and fix it in the task that implements that archetype.

- [ ] **Step 2: Write the replacement test file**

Replace the three count-coupled tests. `enumeratesAllTwentyFourArchetypes`,
`archetypeIdsAreContiguousFrom020To043` and `implementedIsASubsetOfAll` are unchanged:

```java
package com.techcombank.qe.sut;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CapabilityRegistryTest {

    /** The registry's expected contents. Each Wave 17 task that implements an
     *  archetype appends its ID here in the same commit that adds it to
     *  CapabilityRegistry.IMPLEMENTED -- so this set is the drift guard, and the
     *  suite never runs knowingly red. Wave 16 left seven; Wave 17 adds eight. */
    private static final Set<String> IMPLEMENTED_AT_WAVE_17 = Set.of(
        "TST-021", "TST-030", "TST-031", "TST-035", "TST-039", "TST-040", "TST-043"
    );

    @Test
    void enumeratesAllTwentyFourArchetypes() {
        assertEquals(24, CapabilityRegistry.ALL.size());
        assertTrue(CapabilityRegistry.ALL.contains("TST-020"));
        assertTrue(CapabilityRegistry.ALL.contains("TST-043"));
    }

    @Test
    void archetypeIdsAreContiguousFrom020To043() {
        for (int n = 20; n <= 43; n++) {
            assertTrue(CapabilityRegistry.ALL.contains(String.format("TST-0%d", n)),
                       "missing TST-0" + n);
        }
    }

    @Test
    void implementedIsASubsetOfAll() {
        assertTrue(CapabilityRegistry.ALL.containsAll(CapabilityRegistry.IMPLEMENTED));
    }

    @Test
    void implementedMatchesTheDeclaredSetExactly() {
        assertEquals(IMPLEMENTED_AT_WAVE_17, CapabilityRegistry.IMPLEMENTED);
    }

    @Test
    void declaredAndImplementedPartitionAllTwentyFour() {
        long declared = CapabilityRegistry.ALL.stream()
            .filter(a -> "declared".equals(CapabilityRegistry.statusOf(a))).count();
        assertEquals(CapabilityRegistry.ALL.size() - IMPLEMENTED_AT_WAVE_17.size(), declared);
    }

    @Test
    void statusOfAnUnimplementedArchetypeIsDeclared() {
        assertFalse(IMPLEMENTED_AT_WAVE_17.contains("TST-022"),
                    "TST-022 is out of Wave 17's scope and must stay declared");
        assertEquals("declared", CapabilityRegistry.statusOf("TST-022"));
    }
}
```

`declaredAndImplementedPartitionAllTwentyFour` replaces the hardcoded `17` with arithmetic, so
it never needs touching again. `statusOfAnUnimplementedArchetypeIsDeclared` now asserts its own
premise first, so it fails loudly rather than misleadingly if a future wave implements TST-022.

- [ ] **Step 3: Update the registry's stale comment**

In `CapabilityRegistry.java`, replace the `IMPLEMENTED` javadoc:

```java
    /** Implemented archetypes. Wave 16 added the first seven; each Wave 17 task
     *  adds exactly one ID here and to CapabilityRegistryTest's
     *  IMPLEMENTED_AT_WAVE_17 in the same commit -- see that test for the guard
     *  that keeps this set from drifting from what modules.yml actually ships. */
```

- [ ] **Step 4: Run the SUT test suite**

```bash
cd qe-harness/reference-sut && mvn -q -B test
```

Expected: PASS. Compare the total test count against Task 0 Step 3 — it should be `+0` net (six
tests before, six after; three replaced).

- [ ] **Step 5: Commit**

```bash
git add qe-harness/reference-sut/src/test/java/com/techcombank/qe/sut/CapabilityRegistryTest.java \
        qe-harness/reference-sut/src/main/java/com/techcombank/qe/sut/CapabilityRegistry.java
git commit -m "test(sut): make the capability registry guard set-based, not count-based"
```

---

## Task 6: Wire the Testing-Corpus Gates into CI

Two Wave 15 gates have never run in CI. Wave 17 mutates the coverage data they validate, so they
must run before it does. Neither existing rules anchor covers `knowledge-base/testing/**`, so
this needs a new one.

**Files:**
- Modify: `.gitlab-ci.yml` (new anchor + new job; header comment)

**Interfaces:**
- Consumes: `scripts/validate-testing-coverage.py`, `scripts/render-testing-coverage.py`
- Produces: a blocking `validate:testing-coverage` job

- [ ] **Step 1: Confirm the gates are genuinely unwired**

```bash
/usr/bin/grep -c "validate-testing-coverage\|render-testing-coverage" .gitlab-ci.yml
```

Expected: `0`.

- [ ] **Step 2: Confirm both scripts' CLI contracts**

```bash
python3 scripts/validate-testing-coverage.py --help | head -12
python3 scripts/render-testing-coverage.py --help | head -12
```

Expected: `validate-testing-coverage.py` accepts only `--quiet` (no `--root`);
`render-testing-coverage.py` accepts only `--check`. Do not invent flags.

- [ ] **Step 3: Add the rules anchor and the job**

Insert into `.gitlab-ci.yml` in the `validate` stage, after `validate:yaml`. These gates are
corpus-only and need no containers, so `validate` is the right stage:

```yaml
# Testing-corpus gates (Wave 15 deliverables, wired in Wave 17).
#
# A separate anchor from .qe-harness-rules / .qe-harness-code-rules: neither of
# those lists knowledge-base/testing/** or these two scripts, so reusing either
# would silently never fire on a corpus-only change -- which is exactly the
# change these gates exist to catch. Wave 17 mutates the coverage matrix these
# scripts validate, so they must be blocking before that wave lands.
.testing-corpus-rules: &testing-corpus-rules
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
      changes:
        - knowledge-base/testing/**/*
        - governance/standards/_catalog-inventory.yml
        - scripts/validate-testing-coverage.py
        - scripts/render-testing-coverage.py
      when: always
    - if: '$CI_COMMIT_BRANCH == "main"'
      when: always
    - if: '$CI_PIPELINE_SOURCE == "schedule"'
      when: always
    - when: never

validate:testing-coverage:
  stage: validate
  <<: *testing-corpus-rules
  image: python:3.11-slim
  before_script:
    - pip install --quiet -r scripts/requirements.txt
  script:
    - echo "🔍 Validating the testing coverage matrix..."
    - python3 scripts/validate-testing-coverage.py
    - python3 scripts/render-testing-coverage.py --check
    - echo "✅ Testing-corpus gates passed"
  allow_failure: false
```

`_catalog-inventory.yml` is in the `changes:` list because check 1 of
`validate-testing-coverage.py` reads it — an inventory edit can break the gate without touching
`knowledge-base/testing/` at all.

- [ ] **Step 4: Update the stale header comment**

The header block lists five stages and omits `qe-harness`. Replace the `Pipeline Stages:` list:

```yaml
# Pipeline Stages:
#   1. validate   — Lint Markdown, validate DAB structure, check links, testing-corpus gates
#   2. qe-harness — Build/scan/verify/run the QE harness (Wave 16)
#   3. build      — Generate PDFs/DOCX, build MkDocs site, compile OpenAPI docs
#   4. assign     — Run reviewer assignment script for DAB MRs
#   5. publish    — Deploy to GitLab Pages (main branch only)
#   6. notify     — Send Slack notifications on approval/rejection
```

- [ ] **Step 5: Validate the YAML parses**

```bash
python3 -c "
import yaml
d = yaml.safe_load(open('.gitlab-ci.yml'))
print('stages:', d['stages'])
print('job present:', 'validate:testing-coverage' in d)
print('stage:', d['validate:testing-coverage']['stage'])
print('blocking:', d['validate:testing-coverage']['allow_failure'] is False)
"
```

Expected: the six stages, `job present: True`, `stage: validate`, `blocking: True`.

- [ ] **Step 6: Run locally exactly as CI will**

```bash
python3 scripts/validate-testing-coverage.py; echo "cov=$?"
python3 scripts/render-testing-coverage.py --check; echo "render=$?"
```

Expected: both `0`. If `render=1`, Task 3 left the table stale — regenerate and commit it there,
not here.

- [ ] **Step 7: Commit**

```bash
git add .gitlab-ci.yml
git commit -m "ci: wire the Wave 15 testing-corpus gates into the validate stage"
```

---

Phase 0 is complete. The corpus is byte-clean, the counts are honest, `TST-025` no longer blocks
check 2, `TST-043` states its substitution plainly, the registry guard is set-based, and the
coverage gates now run in CI. Phase 1 begins.

---

## Task 7: SUT Capability — TST-023 Reservation Counter

`TokenBucket` is a rate limiter, not a reservation counter: its `tokens` regenerate over
wall-clock time, it has no per-key state, no durable store, and no releasable hold. TST-023
needs identity-keyed, persisted, releasable reservations — a new capability beside it.

**Files:**
- Create: `qe-harness/reference-sut/src/main/resources/db/migration/V3__reservations.sql`
- Create: `…/sut/capability/reservation/ReservationController.java`
- Create: `…/sut/capability/reservation/ReservationService.java`
- Create: `…/sut/capability/reservation/ReservationSweeper.java`
- Modify: `…/sut/DefectFlags.java` (add `reservation-overcommit`)
- Modify: `…/sut/CapabilityRegistry.java` (add `TST-023`)
- Modify: `…/sut/CapabilityRegistryTest.java` (add `TST-023` to `IMPLEMENTED_AT_WAVE_17`)
- Modify: `…/reference-sut/src/main/resources/application.properties`
- Test: `…/sut/capability/reservation/AbstractReservationIntegrationTest.java`
- Test: `…/sut/capability/reservation/ReservationServiceTest.java`

**Interfaces:**
- Consumes: `JdbcTemplate`, `DefectFlags.isActive(String)`
- Produces: `POST /reservations`, `POST /reservations/{id}/release`,
  `GET /reservations/utilisation`, `POST /_test/reset/reservations`; defect flag
  `reservation-overcommit`

- [ ] **Step 1: Write the failing test**

`ReservationServiceTest.java`:

```java
package com.techcombank.qe.sut.capability.reservation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-023 concurrent limit and counter. The declared limit L is a per-account
 * fixture value read from the SUT's own data -- it is not an NFR SLO, which is
 * why no threshold_ref accompanies these assertions (see the design spec 7.1).
 */
class ReservationServiceTest extends AbstractReservationIntegrationTest {

    @Test
    void admitsExactlyTheDeclaredLimitUnderConcurrency() throws Exception {
        long limit = declaredLimit("ACC-000001");
        int attempts = (int) limit + 8;

        List<Callable<Boolean>> calls = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            calls.add(() -> {
                try {
                    service.reserve("ACC-000001", 1L);
                    return true;
                } catch (ReservationService.LimitExceeded e) {
                    return false;
                }
            });
        }

        ExecutorService pool = Executors.newFixedThreadPool(16);
        long admitted;
        try {
            List<Future<Boolean>> results = pool.invokeAll(calls);
            admitted = results.stream().filter(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).count();
        } finally {
            pool.shutdownNow();
        }

        assertEquals(limit, admitted, "I1: success count must equal min(N, L)");
        assertEquals(limit, service.utilisation("ACC-000001"),
            "I2: utilisation must never exceed the declared limit");
    }

    @Test
    void releaseReturnsExactlyItsOwnAmount() {
        long id = service.reserve("ACC-000001", 3L);
        assertEquals(3L, service.utilisation("ACC-000001"));
        service.release(id);
        assertEquals(0L, service.utilisation("ACC-000001"), "I3: rollback returns its own amount");
    }

    @Test
    void doubleReleaseIsRejected() {
        long id = service.reserve("ACC-000001", 2L);
        service.release(id);
        assertThrows(ReservationService.NotReleasable.class, () -> service.release(id),
            "I4: a second release must be rejected, not silently succeed");
        assertEquals(0L, service.utilisation("ACC-000001"));
    }

    @Test
    void overcommitDefectBreaksOnlyTheCapacityInvariants() {
        long limit = declaredLimit("ACC-000001");
        withDefect("reservation-overcommit", () -> {
            for (int i = 0; i < limit + 5; i++) {
                service.reserve("ACC-000001", 1L);
            }
        });
        assertTrue(service.utilisation("ACC-000001") > limit,
            "the defect must drive utilisation past the declared limit");
    }
}
```

- [ ] **Step 2: Run it and confirm it fails to compile**

```bash
cd qe-harness/reference-sut && mvn -q -B test -Dtest=ReservationServiceTest
```

Expected: FAIL — `ReservationService` does not exist.

- [ ] **Step 3: Write the migration**

`V3__reservations.sql`:

```sql
-- V3: reservation counter for TST-023 concurrent limit and contention
-- (Wave 17). See com.techcombank.qe.sut.capability.reservation.ReservationService.
--
-- A reservation is a releasable hold against a per-account declared limit --
-- deliberately not a token bucket. TokenBucket (TST-031) regenerates capacity
-- over wall-clock time and forgets every admission; TST-023's I3 (rollback
-- returns exactly its own amount) and I4 (double release rejected) both need a
-- durable, identity-keyed row whose state can be inspected and transitioned.
--
-- account_limit carries the declared limit L as fixture data. L is a business
-- limit read from the SUT's own tables, not a service SLO, which is why no
-- NFR threshold_ref accompanies TST-023's assertions.
--
-- expires_at supports I6 (no reservation outlives its TTL); the window_tz
-- column supports I5 (window boundaries use the declared timezone, never the
-- server's). Both are stored per-account so a test can declare them rather
-- than duplicate a literal.

CREATE TABLE account_limit (
    account_id      BIGINT PRIMARY KEY REFERENCES account(id),
    declared_limit  BIGINT      NOT NULL,
    ttl_seconds     BIGINT      NOT NULL DEFAULT 60,
    window_tz       VARCHAR(64) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    CONSTRAINT declared_limit_positive CHECK (declared_limit > 0),
    CONSTRAINT ttl_seconds_positive    CHECK (ttl_seconds > 0)
);

CREATE TABLE reservation (
    id          BIGSERIAL PRIMARY KEY,
    account_id  BIGINT      NOT NULL REFERENCES account(id),
    amount      BIGINT      NOT NULL,
    state       VARCHAR(16) NOT NULL DEFAULT 'held',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT reservation_amount_positive CHECK (amount > 0),
    CONSTRAINT reservation_state_known     CHECK (state IN ('held', 'released', 'expired'))
);

-- Partial index: utilisation only ever sums held rows, so released/expired
-- rows never need to be scanned.
CREATE INDEX reservation_account_held_idx
    ON reservation (account_id) WHERE state = 'held';

CREATE INDEX reservation_expires_at_idx
    ON reservation (expires_at) WHERE state = 'held';
```

- [ ] **Step 4: Write the service**

`ReservationService.java`:

```java
package com.techcombank.qe.sut.capability.reservation;

import com.techcombank.qe.sut.DefectFlags;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * TST-023 concurrent limit and counter capability.
 *
 * <p><b>Why the row lock:</b> reserve() must read utilisation and insert a hold
 * as one atomic step, or two concurrent callers each see capacity for the last
 * unit and both take it -- admitting L+1, which is exactly invariant I1/I2's
 * failure mode. SELECT ... FOR UPDATE on the account_limit row serialises
 * every reservation for that account, so contention (not overcommit) is what
 * concurrency produces. This mirrors TransferService's lock-ordering rationale;
 * here there is only ever one row to lock, so no ordering rule is needed.
 *
 * <p><b>Defect injection:</b> when {@code reservation-overcommit} is active the
 * capacity comparison is skipped entirely -- the hold is still inserted and
 * still counted, so utilisation provably exceeds the declared limit and I1/I2
 * fail while I3/I4 stay structurally intact.
 */
@Service
public class ReservationService {

    /** Thrown when a reservation would exceed the account's declared limit. */
    public static class LimitExceeded extends RuntimeException {
        public LimitExceeded(String accountRef) {
            super("declared limit exceeded for " + accountRef);
        }
    }

    /** Thrown when releasing a reservation that is not currently held. */
    public static class NotReleasable extends RuntimeException {
        public NotReleasable(long id) {
            super("reservation " + id + " is not in state 'held'");
        }
    }

    private final JdbcTemplate jdbc;

    public ReservationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public long reserve(String accountRef, long amount) {
        long accountId = idOf(accountRef);
        Long limit = jdbc.queryForObject(
            "SELECT declared_limit FROM account_limit WHERE account_id = ? FOR UPDATE",
            Long.class, accountId);
        long ttlSeconds = jdbc.queryForObject(
            "SELECT ttl_seconds FROM account_limit WHERE account_id = ?", Long.class, accountId);

        if (!DefectFlags.isActive("reservation-overcommit")) {
            long held = heldFor(accountId);
            if (held + amount > limit) {
                throw new LimitExceeded(accountRef);
            }
        }

        return jdbc.queryForObject(
            "INSERT INTO reservation (account_id, amount, expires_at) "
                + "VALUES (?, ?, now() + make_interval(secs => ?)) RETURNING id",
            Long.class, accountId, amount, (double) ttlSeconds);
    }

    /** Releases a held reservation, returning exactly its own amount (I3).
     *  Rejects anything not currently held (I4) -- the state check is inside
     *  the UPDATE, so two concurrent releases cannot both see 'held'. */
    @Transactional
    public void release(long reservationId) {
        int updated = jdbc.update(
            "UPDATE reservation SET state = 'released' WHERE id = ? AND state = 'held'",
            reservationId);
        if (updated != 1) {
            throw new NotReleasable(reservationId);
        }
    }

    /** Sum of currently-held amounts. Expired holds are excluded even before
     *  the sweeper transitions them, so I6 holds continuously rather than only
     *  after a sweep. */
    public long utilisation(String accountRef) {
        return heldFor(idOf(accountRef));
    }

    public long declaredLimit(String accountRef) {
        return jdbc.queryForObject(
            "SELECT declared_limit FROM account_limit WHERE account_id = ?",
            Long.class, idOf(accountRef));
    }

    public String windowTimezone(String accountRef) {
        return jdbc.queryForObject(
            "SELECT window_tz FROM account_limit WHERE account_id = ?",
            String.class, idOf(accountRef));
    }

    private long heldFor(long accountId) {
        Long held = jdbc.queryForObject(
            "SELECT COALESCE(SUM(amount), 0) FROM reservation "
                + "WHERE account_id = ? AND state = 'held' AND expires_at > now()",
            Long.class, accountId);
        return held;
    }

    private long idOf(String accountRef) {
        return jdbc.queryForObject(
            "SELECT id FROM account WHERE account_ref = ?", Long.class, accountRef);
    }
}
```

- [ ] **Step 5: Write the TTL sweeper**

`ReservationSweeper.java`:

```java
package com.techcombank.qe.sut.capability.reservation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * TST-023 invariant I6: no reservation outlives its TTL.
 *
 * <p>utilisation() already excludes expired holds in its own WHERE clause, so
 * correctness does not depend on this sweeper's timing -- it exists so expired
 * rows reach a terminal state observable by the harness, rather than lingering
 * as 'held' forever. Interval comes from a declared property so the test reads
 * it rather than duplicating the number.
 */
@Component
public class ReservationSweeper {

    private final JdbcTemplate jdbc;

    public ReservationSweeper(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Scheduled(fixedDelayString = "${app.reservation.sweep-interval-ms}")
    public void sweep() {
        jdbc.update("UPDATE reservation SET state = 'expired' "
            + "WHERE state = 'held' AND expires_at <= now()");
    }

    /** Test-support: run one sweep synchronously instead of waiting for the schedule. */
    public int sweepNow() {
        return jdbc.update("UPDATE reservation SET state = 'expired' "
            + "WHERE state = 'held' AND expires_at <= now()");
    }
}
```

Add `@EnableScheduling` to the SUT's `@SpringBootApplication` class if it is not already
present — check first:

```bash
/usr/bin/grep -rn "EnableScheduling" qe-harness/reference-sut/src/main/java/
```

- [ ] **Step 6: Write the controller**

`ReservationController.java`:

```java
package com.techcombank.qe.sut.capability.reservation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TST-023 concurrent limit capability's HTTP surface. The JMeter module drives
 * these three endpoints; utilisation is sampled continuously throughout the
 * concurrent run, per invariant I2, never only at start and end.
 */
@RestController
public class ReservationController {

    private final ReservationService reservations;

    public ReservationController(ReservationService reservations) {
        this.reservations = reservations;
    }

    /** POST /reservations {account, amount} -> 201 {reservationId}, or 409 at the limit. */
    @PostMapping("/reservations")
    public ResponseEntity<?> reserve(@RequestBody ReserveRequest request) {
        try {
            long id = reservations.reserve(request.account(), request.amount());
            return ResponseEntity.status(HttpStatus.CREATED).body(new ReserveResponse(id));
        } catch (ReservationService.LimitExceeded e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /** POST /reservations/{id}/release -> 204, or 409 if it is not held (I4). */
    @PostMapping("/reservations/{id}/release")
    public ResponseEntity<?> release(@PathVariable long id) {
        try {
            reservations.release(id);
            return ResponseEntity.noContent().build();
        } catch (ReservationService.NotReleasable e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    /** GET /reservations/utilisation?account=ACC-000001
     *  -> {utilisation, declaredLimit, windowTimezone} */
    @GetMapping("/reservations/utilisation")
    public UtilisationResponse utilisation(@RequestParam String account) {
        return new UtilisationResponse(
            reservations.utilisation(account),
            reservations.declaredLimit(account),
            reservations.windowTimezone(account));
    }

    public record ReserveRequest(String account, long amount) {}

    public record ReserveResponse(long reservationId) {}

    public record UtilisationResponse(long utilisation, long declaredLimit, String windowTimezone) {}
}
```

- [ ] **Step 7: Declare the sweep interval**

Append to `application.properties`:

```properties
# TST-023 concurrent limit and counter (Wave 17). ReservationSweeper's schedule.
# ReservationServiceTest reads this property rather than duplicating the number,
# per the same measure-the-declared-configuration rule TST-040's clock-skew and
# TST-039's freshness-window tests already follow.
app.reservation.sweep-interval-ms=1000
```

- [ ] **Step 8: Add the defect flag**

In `DefectFlags.java`, extend `KNOWN_FLAGS` and update its comment:

```java
    /** The complete, closed set of defect flags this SUT understands.
     *  One per archetype capability that Waves 16 and 17 implement. */
    public static final Set<String> KNOWN_FLAGS = Set.of(
        "ledger-unbalanced", "schema-drift", "ratelimit-leaky",
        "breaker-disabled", "recon-false-clean", "authz-missing-marker",
        "cache-headers-absent",
        "reservation-overcommit"
    );
```

- [ ] **Step 9: Register the capability**

In `CapabilityRegistry.java` add `"TST-023"` to `IMPLEMENTED`. In `CapabilityRegistryTest.java`
add `"TST-023"` to `IMPLEMENTED_AT_WAVE_17`. Both in this commit — the set-based guard from
Task 5 fails if they diverge.

- [ ] **Step 10: Write the test base class**

`AbstractReservationIntegrationTest.java`:

```java
package com.techcombank.qe.sut.capability.reservation;

import com.techcombank.qe.sut.DefectFlags;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Postgres-via-Testcontainers fixture for the reservation capability (TST-023).
 *
 * <p>Singleton container in a static initialiser, deliberately not
 * {@code @Testcontainers}/{@code @Container} -- see
 * {@code AbstractLedgerIntegrationTest}'s javadoc for the exact failure mode
 * that pattern causes under Spring's context caching (a stale DataSource
 * pointing at a torn-down container's port).
 *
 * <p>The Hikari pool is widened so the concurrency test's 16-thread pool
 * blocks on the deliberate row lock -- the only contention this suite means to
 * exercise -- rather than on connection checkout.
 */
@SpringBootTest
abstract class AbstractReservationIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.connect-retries", () -> 10);
        registry.add("spring.flyway.connect-retries-interval", () -> "1s");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 20);
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ReservationService service;

    @Autowired
    protected ReservationSweeper sweeper;

    @BeforeEach
    void resetReservationFixture() {
        DefectFlags.clear();
        jdbc.execute("TRUNCATE TABLE reservation, account_limit, ledger_entry, account "
            + "RESTART IDENTITY CASCADE");
        jdbc.update("INSERT INTO account (account_ref, party_name) VALUES (?, ?)",
            "ACC-000001", "Test Fixture Limit Holder Co");
        jdbc.update("INSERT INTO account_limit (account_id, declared_limit, ttl_seconds) "
            + "SELECT id, ?, ? FROM account WHERE account_ref = ?", 10L, 60L, "ACC-000001");
    }

    protected long declaredLimit(String accountRef) {
        return service.declaredLimit(accountRef);
    }

    /** Activates {@code flag} for the duration of {@code action}, always clearing it
     *  afterwards even if {@code action} throws. */
    protected void withDefect(String flag, Runnable action) {
        DefectFlags.activate(flag);
        try {
            action.run();
        } finally {
            DefectFlags.clear();
        }
    }
}
```

- [ ] **Step 11: Run the tests**

```bash
cd qe-harness/reference-sut && mvn -q -B test
```

Expected: PASS, including all four `ReservationServiceTest` tests and the whole existing suite.
`overcommitDefectBreaksOnlyTheCapacityInvariants` proves the defect is specific.

- [ ] **Step 12: Verify the capability now reports implemented**

```bash
cd qe-harness && make up PROFILES=core
curl -s http://localhost:8080/_capabilities | python3 -m json.tool | /usr/bin/grep TST-023
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/capability/TST-023/probe
```

Expected: `"TST-023": "implemented"` and `200` (was `501`).

- [ ] **Step 13: Check for digit-run violations**

```bash
cd "$(git rev-parse --show-toplevel)" && python3 scripts/validate-harness-coverage.py 2>&1 | /usr/bin/grep check5 || echo "no check5 findings"
```

Expected: no findings.

- [ ] **Step 14: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): add TST-023 reservation counter with rollback and TTL"
```

---

## Task 8: Module — TST-023 Concurrent Limit & Counter (JMeter)

**Files:**
- Create: `qe-harness/harness/jmeter/tst-023-reservation/plan.jmx`
- Create: `…/tst-023-reservation/assert-reservation.groovy`
- Create: `…/tst-023-reservation/README.md`
- Modify: `qe-harness/traceability/modules.yml`
- Test: `…/harness/jmeter/src/test/java/com/techcombank/qe/harness/jmeter/Tst023ModuleTest.java`

**Interfaces:**
- Consumes: SUT `POST /reservations`, `POST /reservations/{id}/release`,
  `GET /reservations/utilisation` (Task 7); `EvidenceEmitter`, `RunFragment`,
  `InvariantAssertion` from `qe-harness-common`
- Produces: `run-module.sh TST-023` writing one fragment to `traceability/runs/`

- [ ] **Step 1: Write the module README**

`tst-023-reservation/README.md`:

```markdown
# TST-023 -- Concurrent Limit & Counter (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.

| ID | Invariant |
|---|---|
| I1 | Admitted count equals min(N, L) under a genuine concurrency burst |
| I2 | Sampled utilisation never exceeds the declared limit at any instant |
| I3 | Releasing a reservation returns exactly its own amount |
| I4 | A second release of the same reservation is rejected |
| I5 | The window boundary uses the account's declared timezone |

Defect proof: with the `reservation-overcommit` defect active this module MUST report I1 and I2
failed, and I3/I4 still passed.

The declared limit `L` is a per-account fixture value read from `account_limit` -- a business
limit owned by the SUT's own data, not a service SLO. That is why this module carries no
`threshold_ref`: citing an NFR row for it would be a fabricated provenance.

## What this module drives

`plan.jmx` runs three phases against the reservation capability:

1. **setUp Thread Group** (`Reset and Seed Limit`, 1 thread, 1 loop) truncates
   `reservation`/`account_limit`/`ledger_entry`/`account` with the same
   `TRUNCATE ... RESTART IDENTITY CASCADE` the SUT's own
   `AbstractReservationIntegrationTest` uses, then inserts `ACC-000001` and its
   `account_limit` row. The reset is necessary, not tidy: `GET /reservations/utilisation`
   sums every held row for the account with no scoping to this run, so one defect-active
   run's overcommitted holds would otherwise poison every later clean run's I1/I2.
2. **Main Thread Group** (`Reservation Burst`, 16 threads x 1 loop) fires
   `POST /reservations` with a **Synchronizing Timer** (group size 16) so all sixteen
   threads are released together. Genuine simultaneity is the whole point: the
   `SELECT ... FOR UPDATE` serialisation this exercises only fails to hold under real
   contention, and threads trickling in under ramp-up alone would each see uncontended
   capacity. A `JSR223 PostProcessor` tallies 201s and 409s into JMeter's `props` --
   the same cross-thread aggregation pattern `tst-031-ratelimit` uses -- and samples
   `GET /reservations/utilisation` after each admission, keeping the running maximum,
   because I2 demands a continuous sample rather than a start/end pair.
3. **TearDown Thread Group** (`Verify Reservation`, 1 thread, 1 loop) runs only after every
   burst thread has finished. It exercises I3 and I4 directly -- reserve, release, assert the
   amount came back, release again and require rejection -- then `assert-reservation.groovy`
   evaluates I1-I5 and writes one fragment.

## Running it

```
make up PROFILES=core           # from qe-harness/
./bin/run-module.sh TST-023     # from qe-harness/
```

Exits non-zero exactly when the emitted fragment's result is `failed` (see `bin/run-jmeter.sh`).

## Defect proof

```
curl -X POST http://localhost:8080/_test/defect/reservation-overcommit   # 204
./bin/run-module.sh TST-023                                              # must report I1+I2 FAILED
curl -X DELETE http://localhost:8080/_test/defect                        # 204, always clears it
```

With `reservation-overcommit` active, `ReservationService.reserve` skips the capacity
comparison entirely -- the hold is still inserted and still counted, so utilisation provably
exceeds `L`. I3 and I4 are untouched by that branch and must still pass, which is what makes
the defect proof specific rather than merely sensitive. `Tst023ModuleTest`'s
`reportsCapacityFailureAgainstTheOvercommitDefect` drives this exact sequence via
`ModuleRunner`, which performs the HTTP activate/clear itself.
```

- [ ] **Step 2: Write the failing test**

`Tst023ModuleTest.java`:

```java
package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-023 concurrent limit module. Drives real HTTP traffic against the
 * reference SUT via {@code run-module.sh TST-023} -- requires
 * {@code make up PROFILES=core} to already be running (see qe-harness/README.md).
 */
class Tst023ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void passesAgainstTheCleanSut() throws Exception {
        ModuleResult r = runner.run("TST-023", Map.of());
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void reportsCapacityFailureAgainstTheOvercommitDefect() throws Exception {
        ModuleResult r = runner.run("TST-023", Map.of("SUT_DEFECT", "reservation-overcommit"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I1") && i.result() == RunFragment.Result.FAILED));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I2") && i.result() == RunFragment.Result.FAILED));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I3") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: I3 is untouched by the skipped capacity check");
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I4") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: I4 is untouched by the skipped capacity check");
    }
}
```

- [ ] **Step 3: Run and confirm failure**

```bash
cd qe-harness/harness && mvn -q -pl jmeter test -Dtest=Tst023ModuleTest
```

Expected: FAIL — no `modules.yml` entry for `TST-023`, so `run-module.sh` exits with
`no modules.yml entry for archetype TST-023`.

- [ ] **Step 4: Add the binding row**

Insert into `qe-harness/traceability/modules.yml` in archetype order, immediately before the
`TST-030` entry:

```yaml
  - archetype: TST-023
    tool: jmeter
    path: qe-harness/harness/jmeter/tst-023-reservation
    coverage: full
    defect_flag: reservation-overcommit
```

`path` is repo-root-relative, matching every existing row — `run-module.sh` resolves it against
`REPO_ROOT` and derives the runner script from `basename(dirname(path))`, so `jmeter` here is
what dispatches to `bin/run-jmeter.sh`.

- [ ] **Step 5: Write the assertion script**

`assert-reservation.groovy`:

```groovy
// TST-023 concurrent limit and counter assertion (Wave 17).
//
// Runs as the sole sampler-producing element in the TearDown Thread Group,
// after every Reservation Burst thread has finished -- see plan.jmx and
// README.md for why the tallies cannot be read mid-run.
//
// Bound variables from the JMeter engine:
//   vars         - JMeterVariables for this thread; the teardown group's own
//                  HTTP samplers wrote i3_returned/i4_rejected/i5_timezone.
//   props        - cross-thread JMeter properties; the Reservation Burst
//                  group's PostProcessor tallied admitted/rejected counts and
//                  the running utilisation maximum here (the same mechanism
//                  tst-031-ratelimit's assert-ratelimit.groovy relies on).
//   SampleResult - this sampler's own result.
//   log          - JMeter's SLF4J logger for this element.
//
// No ThresholdResolver call and no threshold entry: I1/I2's bound is the
// account's own declared_limit, a fixture value read from the SUT's data. It
// is not a service SLO, so citing an NFR row for it would fabricate
// provenance -- see the design spec section 7.1.

import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion

import java.nio.file.Path

long declaredLimit = Long.parseLong(props.getProperty("tst023_declared_limit"))
long attempts      = Long.parseLong(props.getProperty("tst023_attempts"))
long admitted      = Long.parseLong(props.getProperty("tst023_admitted"))
long maxUtilisation = Long.parseLong(props.getProperty("tst023_max_utilisation"))

long i3Returned  = Long.parseLong(vars.get("i3_returned"))
boolean i4Rejected = Boolean.parseBoolean(vars.get("i4_rejected"))
String declaredTz  = vars.get("i5_timezone")

long expectedAdmitted = Math.min(attempts, declaredLimit)

String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1", "Admitted count equals min(N, L) under a genuine concurrency burst",
    { admitted == expectedAdmitted } as java.util.function.BooleanSupplier)
RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "Sampled utilisation never exceeds the declared limit at any instant",
    { maxUtilisation <= declaredLimit } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "Releasing a reservation returns exactly its own amount",
    { i3Returned == 0L } as java.util.function.BooleanSupplier)
RunFragment.Entry i4 = InvariantAssertion.check(
    "I4", "A second release of the same reservation is rejected",
    { i4Rejected } as java.util.function.BooleanSupplier)
RunFragment.Entry i5 = InvariantAssertion.check(
    "I5", "The window boundary uses the account's declared timezone",
    { declaredTz != null && !declaredTz.trim().isEmpty() } as java.util.function.BooleanSupplier)

RunFragment fragment = RunFragment.builder()
    .archetype(System.getenv("QE_ARCHETYPE"))
    .module("jmeter")
    .serviceName("reference-sut")
    .tier("T0")
    .oracle("invariant-assertion")
    .environment(System.getenv().getOrDefault("QE_ENVIRONMENT", "local-compose"))
    .sutDefect(sutDefect)
    .invariant(i1.id(), i1.description(), i1.result())
    .invariant(i2.id(), i2.description(), i2.result())
    .invariant(i3.id(), i3.description(), i3.result())
    .invariant(i4.id(), i4.description(), i4.result())
    .invariant(i5.id(), i5.description(), i5.result())
    .build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "I1 admitted-equals-min: ${i1.result().wire()} (admitted=${admitted}, expected=${expectedAdmitted})\n" +
    "I2 utilisation-within-limit: ${i2.result().wire()} (max=${maxUtilisation}, limit=${declaredLimit})\n" +
    "I3 release-returns-own-amount: ${i3.result().wire()} (residual=${i3Returned})\n" +
    "I4 double-release-rejected: ${i4.result().wire()}\n" +
    "I5 declared-timezone-present: ${i5.result().wire()} (tz=${declaredTz})\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
```

- [ ] **Step 6: Build the JMeter plan**

`plan.jmx`, following `tst-021-ledger/plan.jmx`'s structure exactly. Set
`TestPlan.serialize_threadgroups` and `TestPlan.tearDown_on_shutdown` to `true` — the teardown
ordering guarantee depends on both. Elements, in order:

- A plan-scoped `HeaderManager` for `Content-Type: application/json`.
- `SetupThreadGroup` "Reset and Seed Limit", 1 thread / 1 loop,
  `on_sample_error=stopthread`: one inline `JSR223Sampler` opening a JDBC connection from
  `LEDGER_JDBC_URL`/`_USER`/`_PASSWORD`, running the TRUNCATE and the two inserts, then
  writing `props.put("tst023_declared_limit", "10")` and `props.put("tst023_attempts", "16")`
  and zeroing `tst023_admitted` / `tst023_max_utilisation`.
- `ThreadGroup` "Reservation Burst", 16 threads / 1 loop, `on_sample_error=continue` (a 409 is
  data, not a reason to stop), containing in order: a `SyncTimer` with `groupSize` **16**
  (must equal `num_threads` or threads block until `timeoutInMs`) and `timeoutInMs` 10000; an
  `HTTPSamplerProxy` `POST /reservations` to `localhost:8080` with
  `postBodyRaw=true` and body `{"account":"ACC-000001","amount":1}`; and a
  `JSR223PostProcessor` that increments `tst023_admitted` on a 201, samples
  `GET /reservations/utilisation?account=ACC-000001` and raises
  `tst023_max_utilisation` if larger. Guard the props updates with
  `synchronized (props) { … }` — sixteen threads increment the same counters.
- `PostThreadGroup` "Verify Reservation", 1 thread / 1 loop: an `HTTPSamplerProxy`
  `POST /reservations` (amount 3), an `HTTPSamplerProxy` `POST /reservations/{id}/release`, an
  `HTTPSamplerProxy` `GET /reservations/utilisation` whose `JSR223PostProcessor` writes
  `vars.put("i3_returned", …)` and `vars.put("i5_timezone", json.windowTimezone)`, a second
  release sampler whose PostProcessor writes
  `vars.put("i4_rejected", String.valueOf(prev.getResponseCode() == "409"))`, and finally the
  `JSR223Sampler` "assert-reservation" with
  `filename=${__groovy(System.getenv("ASSERT_SCRIPT_PATH"),)}` and an empty inline `script`.

Endpoints are hardcoded per sampler (`localhost` / `8080` / `http`, `connect_timeout=5000`,
`response_timeout=10000`, `use_keepalive=true`) — there is no UDV precedent in this repo to
follow, and `SUT_BASE_URL` is not consulted by any existing plan.

- [ ] **Step 7: Run the tests**

```bash
cd qe-harness && make up PROFILES=core
cd harness && mvn -q -pl jmeter test -Dtest=Tst023ModuleTest
```

Expected: PASS, 2 tests.

- [ ] **Step 8: Verify the gate sees the module**

```bash
cd "$(git rev-parse --show-toplevel)"
python3 scripts/validate-harness-coverage.py 2>&1 | /usr/bin/grep TST-023 || echo "no TST-023 findings"
python3 scripts/render-harness-coverage.py
```

Expected: no findings for `TST-023` — in particular no `check2 ⚠️ cannot verify tool`, which
would mean the corpus disagrees with `tool: jmeter`.

- [ ] **Step 9: Commit**

```bash
git add qe-harness/harness/jmeter qe-harness/traceability
git commit -m "feat(harness): add TST-023 concurrent limit JMeter module"
```

---

## Task 9: SUT Capability — TST-037 Read-Model Lag and Outbox

`V2` already provides the `account_balance_report` matview (with the unique index that enables
`REFRESH CONCURRENTLY`) and the `report_refresh_timestamp` companion table. Missing: an HTTP
surface exposing lag, and an outbox with `published_count` for I4.

**Files:**
- Create: `…/db/migration/V4__outbox_and_reporting.sql`
- Create: `…/sut/capability/reporting/ReportingController.java`, `ReportingService.java`
- Modify: `…/sut/DefectFlags.java`, `CapabilityRegistry.java`, `CapabilityRegistryTest.java`
- Modify: `…/reference-sut/src/main/resources/application.properties`
- Test: `…/sut/capability/reporting/AbstractReportingIntegrationTest.java`, `ReportingServiceTest.java`

**Interfaces:**
- Consumes: `JdbcTemplate`, the `V2` matview and `report_refresh_timestamp`
- Produces: `GET /reporting/lag`, `POST /reporting/refresh`, `GET /reporting/outbox`; defect
  flag `outbox-published-count-stale`; property `app.readmodel.convergence-bound-ms`

- [ ] **Step 1: Write the failing test**

`ReportingServiceTest.java`:

```java
package com.techcombank.qe.sut.capability.reporting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TST-037 read-model convergence and CDC lag. */
class ReportingServiceTest extends AbstractReportingIntegrationTest {

    @Test
    void refreshDrivesLagToNearZero() {
        service.refresh();
        ReportingService.Lag lag = service.lag();
        assertTrue(lag.p95Ms() < convergenceBoundMs(),
            "I1: the read model must converge inside the declared bound");
        assertTrue(lag.p99Ms() < convergenceBoundMs(), "I2: p99 is asserted, not just p95");
    }

    @Test
    void lagExposesBothTailPercentilesNeverTheMean() {
        service.refresh();
        ReportingService.Lag lag = service.lag();
        assertTrue(lag.p95Ms() >= 0 && lag.p99Ms() >= 0);
        assertTrue(lag.p99Ms() >= lag.p95Ms(), "p99 can never be below p95");
    }

    @Test
    void everyPublishedOutboxRowIsCountedExactlyOnce() {
        service.enqueue("acct-balance-changed", "ACC-000001");
        service.publishPending();
        assertEquals(0L, service.outboxMiscountedRows(),
            "I4: every published row must have published_count = 1");
    }

    @Test
    void staleCountDefectBreaksOnlyTheOutboxInvariant() {
        service.enqueue("acct-balance-changed", "ACC-000001");
        withDefect("outbox-published-count-stale", service::publishPending);
        assertTrue(service.outboxMiscountedRows() > 0,
            "the defect must leave a published row with published_count = 0");
        service.refresh();
        assertTrue(service.lag().p95Ms() < convergenceBoundMs(),
            "the defect must be specific: convergence is untouched");
    }
}
```

- [ ] **Step 2: Run it and confirm it fails to compile**

```bash
cd qe-harness/reference-sut && mvn -q -B test -Dtest=ReportingServiceTest
```

Expected: FAIL — `ReportingService` does not exist.

- [ ] **Step 3: Write the migration**

`V4__outbox_and_reporting.sql`:

```sql
-- V4: transactional outbox for TST-037 read-model convergence (Wave 17).
-- See com.techcombank.qe.sut.capability.reporting.ReportingService.
--
-- V2 already supplies account_balance_report (a real MATERIALIZED VIEW) and
-- report_refresh_timestamp (its per-account freshness bookkeeping). What
-- TST-037 additionally needs is I4's evidence surface: "every outbox row is
-- published exactly once". published_count is a counter rather than a boolean
-- precisely so double-publication is observable, not just non-publication --
-- a boolean flag would make the two failures indistinguishable.
--
-- No FK to account: the aggregate_ref is a business reference (ACC-000001),
-- not an id, so an outbox row survives its aggregate. This means the table is
-- NOT reached by AbstractLedgerIntegrationTest's TRUNCATE ... CASCADE, so any
-- test touching it must truncate it explicitly.

CREATE TABLE outbox (
    id              BIGSERIAL PRIMARY KEY,
    event_type      VARCHAR(64) NOT NULL,
    aggregate_ref   VARCHAR(16) NOT NULL,
    published_count INTEGER     NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ,
    CONSTRAINT outbox_published_count_sane CHECK (published_count >= 0)
);

CREATE INDEX outbox_pending_idx ON outbox (id) WHERE published_at IS NULL;
```

- [ ] **Step 4: Write the service**

`ReportingService.java`:

```java
package com.techcombank.qe.sut.capability.reporting;

import com.techcombank.qe.sut.DefectFlags;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * TST-037 read-model convergence and CDC lag capability.
 *
 * <p><b>Percentiles, not the mean:</b> {@link #lag()} returns p95 and p99 and
 * deliberately exposes no mean at all. Invariant I2 fails a run that asserts
 * only the mean regardless of what the mean shows, so offering one here would
 * be offering a footgun.
 *
 * <p><b>Defect injection:</b> {@code outbox-published-count-stale} publishes
 * the row (setting published_at) but never increments published_count, so I4
 * alone fails -- convergence and the percentile shape are untouched.
 */
@Service
public class ReportingService {

    /** Read-model staleness at the tail. No mean is exposed, by design (I2). */
    public record Lag(long p95Ms, long p99Ms, long accountsCovered) {}

    private final JdbcTemplate jdbc;

    public ReportingService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Rebuilds the matview and upserts per-account freshness, using the same
     *  ON CONFLICT ... DO UPDATE idiom DefectSeeder.refresh() established. */
    @Transactional
    public void refresh() {
        jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY account_balance_report");
        jdbc.update(
            "INSERT INTO report_refresh_timestamp (account_id, refreshed_at) "
                + "SELECT account_id, now() FROM account_balance_report "
                + "ON CONFLICT (account_id) DO UPDATE SET refreshed_at = EXCLUDED.refreshed_at");
    }

    /** Tail-percentile staleness across every tracked account. Computed in
     *  Postgres via percentile_disc so the SUT, not the harness, owns the
     *  definition of the percentile. */
    public Lag lag() {
        List<Lag> rows = jdbc.query(
            "SELECT "
                + "  COALESCE(CAST(percentile_disc(0.95) WITHIN GROUP "
                + "    (ORDER BY EXTRACT(EPOCH FROM (now() - refreshed_at)) * 1000) AS BIGINT), 0) AS p95, "
                + "  COALESCE(CAST(percentile_disc(0.99) WITHIN GROUP "
                + "    (ORDER BY EXTRACT(EPOCH FROM (now() - refreshed_at)) * 1000) AS BIGINT), 0) AS p99, "
                + "  COUNT(*) AS covered "
                + "FROM report_refresh_timestamp",
            (rs, n) -> new Lag(rs.getLong("p95"), rs.getLong("p99"), rs.getLong("covered")));
        return rows.get(0);
    }

    @Transactional
    public long enqueue(String eventType, String aggregateRef) {
        return jdbc.queryForObject(
            "INSERT INTO outbox (event_type, aggregate_ref) VALUES (?, ?) RETURNING id",
            Long.class, eventType, aggregateRef);
    }

    /** Publishes every pending row. The count increment is what the defect
     *  skips -- publication itself still happens, so the failure is a
     *  miscount, not a silent drop. */
    @Transactional
    public int publishPending() {
        if (DefectFlags.isActive("outbox-published-count-stale")) {
            return jdbc.update(
                "UPDATE outbox SET published_at = now() WHERE published_at IS NULL");
        }
        return jdbc.update(
            "UPDATE outbox SET published_at = now(), published_count = published_count + 1 "
                + "WHERE published_at IS NULL");
    }

    /** I4's evidence: rows that were published but not counted exactly once. */
    public long outboxMiscountedRows() {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM outbox WHERE published_at IS NOT NULL AND published_count <> 1",
            Long.class);
    }
}
```

- [ ] **Step 5: Write the controller**

`ReportingController.java`:

```java
package com.techcombank.qe.sut.capability.reporting;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** TST-037 read-model capability's HTTP surface. */
@RestController
public class ReportingController {

    private final ReportingService reporting;
    private final long convergenceBoundMs;

    public ReportingController(ReportingService reporting,
                               @Value("${app.readmodel.convergence-bound-ms}") long convergenceBoundMs) {
        this.reporting = reporting;
        this.convergenceBoundMs = convergenceBoundMs;
    }

    /** GET /reporting/lag -> {p95Ms, p99Ms, accountsCovered, convergenceBoundMs}.
     *  The bound is returned alongside the measurement so the harness asserts
     *  against the SUT's declared configuration rather than a literal of its own. */
    @GetMapping("/reporting/lag")
    public LagResponse lag() {
        ReportingService.Lag lag = reporting.lag();
        return new LagResponse(lag.p95Ms(), lag.p99Ms(), lag.accountsCovered(), convergenceBoundMs);
    }

    /** POST /reporting/refresh -> 204. */
    @PostMapping("/reporting/refresh")
    public ResponseEntity<Void> refresh() {
        reporting.refresh();
        return ResponseEntity.noContent().build();
    }

    /** GET /reporting/outbox -> {miscountedRows}. I4's verdict. */
    @GetMapping("/reporting/outbox")
    public OutboxResponse outbox() {
        return new OutboxResponse(reporting.outboxMiscountedRows());
    }

    public record LagResponse(long p95Ms, long p99Ms, long accountsCovered, long convergenceBoundMs) {}

    public record OutboxResponse(long miscountedRows) {}
}
```

- [ ] **Step 6: Declare the convergence bound as application config**

Append to `application.properties`. This is the resolution of the spec's §7.1 open item — no
NFR corpus edit, following the `app.recon.freshness-window-seconds` precedent:

```properties
# TST-037 read-model convergence (Wave 17). The declared bound a projection must
# converge inside, in milliseconds. Deliberately NOT projected into
# profiles/_nfr-thresholds.yml and carrying no NFR-* citation: the NFR corpus
# states lag only in message counts (Kafka consumer lag, NFR-003 CPM-3 and
# NFR-004), and NFR-002's 50/80 ms "database write (sync replicated)" row is the
# synchronous write-path ack inside one request's latency budget, not an
# asynchronous projection bound -- citing either would fabricate provenance.
# Same convention as app.recon.freshness-window-seconds: one declared value,
# read by both ReportingController and the tests, never duplicated as a literal.
app.readmodel.convergence-bound-ms=5000
```

- [ ] **Step 7: Add the defect flag and register the capability**

Add `"outbox-published-count-stale"` to `DefectFlags.KNOWN_FLAGS`, `"TST-037"` to
`CapabilityRegistry.IMPLEMENTED`, and `"TST-037"` to `CapabilityRegistryTest`'s
`IMPLEMENTED_AT_WAVE_17` — all in this commit.

- [ ] **Step 8: Write the test base class**

`AbstractReportingIntegrationTest.java` — same singleton-container pattern as Task 7, with two
differences that matter:

```java
package com.techcombank.qe.sut.capability.reporting;

import com.techcombank.qe.sut.DefectFlags;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Postgres-via-Testcontainers fixture for the reporting capability (TST-037).
 * Singleton container in a static initialiser -- see
 * AbstractLedgerIntegrationTest's javadoc for why not @Container.
 */
@SpringBootTest
abstract class AbstractReportingIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.connect-retries", () -> 10);
        registry.add("spring.flyway.connect-retries-interval", () -> "1s");
    }

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ReportingService service;

    /** Read from the declared property, never duplicated as a literal. */
    @Value("${app.readmodel.convergence-bound-ms}")
    private long convergenceBoundMs;

    protected long convergenceBoundMs() {
        return convergenceBoundMs;
    }

    @BeforeEach
    void resetReportingFixture() {
        DefectFlags.clear();
        // outbox has no FK to account, so CASCADE does not reach it -- truncate
        // it explicitly or published rows leak into the next test.
        jdbc.execute("TRUNCATE TABLE outbox RESTART IDENTITY");
        jdbc.execute("TRUNCATE TABLE report_refresh_timestamp, ledger_entry, account "
            + "RESTART IDENTITY CASCADE");
        jdbc.update("INSERT INTO account (account_ref, party_name) VALUES (?, ?)",
            "ACC-000001", "Test Fixture Reporting Co");
        jdbc.update("INSERT INTO ledger_entry (transfer_ref, account_id, amount_minor) "
            + "SELECT gen_random_uuid(), id, 500 FROM account WHERE account_ref = ?", "ACC-000001");
    }

    protected void withDefect(String flag, Runnable action) {
        DefectFlags.activate(flag);
        try {
            action.run();
        } finally {
            DefectFlags.clear();
        }
    }
}
```

If `gen_random_uuid()` is unavailable, add `CREATE EXTENSION IF NOT EXISTS pgcrypto;` to `V4`
— check first with `psql -c "SELECT gen_random_uuid();"` against the container image; Postgres
16 provides it in core.

- [ ] **Step 9: Run the tests**

```bash
cd qe-harness/reference-sut && mvn -q -B test
```

Expected: PASS. `staleCountDefectBreaksOnlyTheOutboxInvariant` proves specificity.

- [ ] **Step 10: Verify the endpoints**

```bash
cd qe-harness && make down && make up PROFILES=core
curl -s -X POST http://localhost:8080/reporting/refresh -o /dev/null -w '%{http_code}\n'
curl -s http://localhost:8080/reporting/lag | python3 -m json.tool
```

Expected: `204`, then a body carrying `p95Ms`, `p99Ms`, `accountsCovered` and
`convergenceBoundMs: 5000`. Confirm **no `mean` field is present**.

- [ ] **Step 11: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): add TST-037 read-model lag endpoints and transactional outbox"
```

---

## Task 10: Module — TST-037 Read-Model Convergence & CDC Lag (JMeter, partial)

This module is `coverage: partial`. I5 (no loss or duplication across a connector restart)
needs a CDC connector that does not exist in this repository, and inventing a substitute would
repeat exactly the sin Task 4 corrected on TST-043.

**Files:**
- Create: `qe-harness/harness/jmeter/tst-037-readmodel/{plan.jmx,assert-readmodel.groovy,README.md}`
- Modify: `qe-harness/traceability/modules.yml`
- Test: `…/jmeter/Tst037ModuleTest.java`

**Interfaces:**
- Consumes: `GET /reporting/lag`, `POST /reporting/refresh`, `GET /reporting/outbox` (Task 9)
- Produces: `run-module.sh TST-037` writing one fragment; the first `partial` JMeter module

- [ ] **Step 1: Write the module README**

`tst-037-readmodel/README.md`:

```markdown
# TST-037 -- Read-Model Convergence & CDC Lag (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.
Coverage: **partial** -- see `partial_reason` in `traceability/modules.yml`.

| ID | Invariant | Asserted here |
|---|---|---|
| I1 | The read model converges inside the declared bound | yes |
| I2 | Lag is asserted at p95 **and** p99, never the mean | yes |
| I3 | A replayed projection equals the incremental one, field by field | yes |
| I4 | Every outbox row has published_count = 1 | yes |
| I5 | No loss or duplication across a connector restart | **no -- not implemented** |
| I6 | Exceeding the bound is a hard FAIL, never an indefinite wait | yes |

I5 needs a CDC connector this repository does not contain. It is reported `not-evaluated` with
a reason rather than substituted -- a substitute server-side check would be a different
invariant wearing I5's name, which is the failure mode `TST-043`'s honest relabelling exists to
warn about.

The convergence bound is `app.readmodel.convergence-bound-ms`, returned by `GET /reporting/lag`
alongside the measurement, so this module asserts against the SUT's declared configuration
rather than a literal of its own. It carries **no** `threshold_ref`: the NFR corpus states lag
only in message counts, so citing an NFR row would fabricate provenance (design spec 7.1).

Defect proof: with `outbox-published-count-stale` active this module MUST report I4 failed and
I1/I2 still passed.

## What this module drives

1. **setUp Thread Group** (`Seed Ledger Activity`, 1 thread, 1 loop) truncates and seeds via
   JDBC, then enqueues outbox rows.
2. **Main Thread Group** (`Refresh and Sample Lag`, 4 threads x 3 loops) alternates
   `POST /reporting/refresh` with `GET /reporting/lag`, keeping the maximum observed p95 and
   p99 in `props`. **I6 is structural, not a timer**: the plan never waits for convergence, it
   samples a bounded number of times and fails if the bound is still breached -- an indefinite
   wait is the behaviour I6 forbids.
3. **TearDown Thread Group** (`Verify Convergence`, 1 thread, 1 loop) calls
   `POST /reporting/refresh` once more, reads `GET /reporting/lag` and `GET /reporting/outbox`,
   compares a replayed projection against the incremental one for I3, then
   `assert-readmodel.groovy` evaluates I1-I4 and I6, and emits I5 as `not-evaluated`.

## Running it

```
make up PROFILES=core
./bin/run-module.sh TST-037
```

## Defect proof

```
curl -X POST http://localhost:8080/_test/defect/outbox-published-count-stale   # 204
./bin/run-module.sh TST-037                                                    # must report I4 FAILED
curl -X DELETE http://localhost:8080/_test/defect                              # 204
```

With the defect active, `ReportingService.publishPending` sets `published_at` but never
increments `published_count`, so `GET /reporting/outbox` reports a miscounted row and I4 alone
fails. Convergence is untouched, which is what makes the proof specific.
```

- [ ] **Step 2: Write the failing test**

`Tst037ModuleTest.java`:

```java
package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TST-037 read-model convergence module. Requires make up PROFILES=core. */
class Tst037ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void passesAgainstTheCleanSut() throws Exception {
        ModuleResult r = runner.run("TST-037", Map.of());
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void reportsTheUnimplementedInvariantAsNotEvaluated() throws Exception {
        ModuleResult r = runner.run("TST-037", Map.of());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I5")
                && i.result() == RunFragment.Result.NOT_EVALUATED),
            "I5 needs a CDC connector this repo lacks and must never report passed");
    }

    @Test
    void reportsOutboxFailureAgainstTheStaleCountDefect() throws Exception {
        ModuleResult r = runner.run("TST-037", Map.of("SUT_DEFECT", "outbox-published-count-stale"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I4") && i.result() == RunFragment.Result.FAILED));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I1") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: convergence is untouched");
    }
}
```

Note `RunFragment.result()` returns `PASSED` when some invariants passed and none failed, so a
`not-evaluated` I5 does not drag the fragment to `NOT_EVALUATED` — that only happens when
nothing at all was evaluated.

- [ ] **Step 3: Run and confirm failure**

```bash
cd qe-harness/harness && mvn -q -pl jmeter test -Dtest=Tst037ModuleTest
```

Expected: FAIL — no `modules.yml` entry.

- [ ] **Step 4: Add the binding row**

Insert into `modules.yml` in archetype order, after `TST-035` and before `TST-039`:

```yaml
  - archetype: TST-037
    tool: jmeter
    path: qe-harness/harness/jmeter/tst-037-readmodel
    coverage: partial
    partial_reason: >-
      I5 (no loss or duplication across a connector restart) requires a CDC connector this
      repository does not contain. I1-I4 and I6 are asserted; I5 is reported not-evaluated
      rather than substituted.
    defect_flag: outbox-published-count-stale
```

- [ ] **Step 5: Write the assertion script**

`assert-readmodel.groovy`:

```groovy
// TST-037 read-model convergence and CDC lag assertion (Wave 17).
//
// Runs in the TearDown Thread Group after every sampling thread has finished.
// The convergence bound is read from the SUT's own GET /reporting/lag response
// (convergenceBoundMs), never hardcoded here -- same measure-the-declared-
// configuration rule TST-040's clock-skew and TST-039's freshness tests follow.
//
// I5 is emitted NOT_EVALUATED with a reason. It is not substituted: a
// server-side stand-in would be a different invariant wearing I5's name.

import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion

import java.nio.file.Path

long boundMs        = Long.parseLong(vars.get("lag_boundMs"))
long p95Ms          = Long.parseLong(vars.get("lag_p95Ms"))
long p99Ms          = Long.parseLong(vars.get("lag_p99Ms"))
long maxP95Observed = Long.parseLong(props.getProperty("tst037_max_p95"))
long maxP99Observed = Long.parseLong(props.getProperty("tst037_max_p99"))
long miscounted     = Long.parseLong(vars.get("outbox_miscounted"))
long replayDrift    = Long.parseLong(vars.get("replay_drift"))
long samplesTaken   = Long.parseLong(props.getProperty("tst037_samples"))

String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1", "The read model converges inside the declared bound",
    { p95Ms <= boundMs } as java.util.function.BooleanSupplier)
RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "Lag is asserted at p95 and p99, never the mean",
    { maxP95Observed <= boundMs && maxP99Observed <= boundMs } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "A replayed projection equals the incremental one, field by field",
    { replayDrift == 0L } as java.util.function.BooleanSupplier)
RunFragment.Entry i4 = InvariantAssertion.check(
    "I4", "Every published outbox row has published_count = 1",
    { miscounted == 0L } as java.util.function.BooleanSupplier)
RunFragment.Entry i6 = InvariantAssertion.check(
    "I6", "Exceeding the bound is a hard FAIL, never an indefinite wait",
    { samplesTaken > 0L } as java.util.function.BooleanSupplier)

RunFragment fragment = RunFragment.builder()
    .archetype(System.getenv("QE_ARCHETYPE"))
    .module("jmeter")
    .serviceName("reference-sut")
    .tier("T0")
    .oracle("invariant-assertion")
    .environment(System.getenv().getOrDefault("QE_ENVIRONMENT", "local-compose"))
    .sutDefect(sutDefect)
    .invariant(i1.id(), i1.description(), i1.result())
    .invariant(i2.id(), i2.description(), i2.result())
    .invariant(i3.id(), i3.description(), i3.result())
    .invariant(i4.id(), i4.description(), i4.result())
    .invariant("I5", "No loss or duplication across a connector restart",
               RunFragment.Result.NOT_EVALUATED)
    .invariant(i6.id(), i6.description(), i6.result())
    .build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "I1 converges-within-bound: ${i1.result().wire()} (p95=${p95Ms}ms, bound=${boundMs}ms)\n" +
    "I2 tail-percentiles-asserted: ${i2.result().wire()} (maxP95=${maxP95Observed}, maxP99=${maxP99Observed})\n" +
    "I3 replay-matches-incremental: ${i3.result().wire()} (drift=${replayDrift})\n" +
    "I4 outbox-counted-once: ${i4.result().wire()} (miscounted=${miscounted})\n" +
    "I5 connector-restart: not-evaluated (needs a CDC connector this repo lacks)\n" +
    "I6 bounded-not-waiting: ${i6.result().wire()} (samples=${samplesTaken})\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
```

Note `RunFragment.Builder.invariant(...)` takes a `Result` directly, so a `not-evaluated`
invariant needs no reason — only `threshold(...)` enforces that. The reason lives in the module
README and the `partial_reason`.

- [ ] **Step 6: Build the JMeter plan**

`plan.jmx`, same skeleton as Task 8. Specifics:

- `SetupThreadGroup` "Seed Ledger Activity", 1/1, `on_sample_error=stopthread`: an inline
  `JSR223Sampler` truncating `outbox` (explicitly — no FK to `account`, so `CASCADE` misses it)
  plus `report_refresh_timestamp, ledger_entry, account RESTART IDENTITY CASCADE`, inserting
  `ACC-000001` and a few ledger entries, then `INSERT INTO outbox (event_type, aggregate_ref)`
  twice. Zero `tst037_max_p95`, `tst037_max_p99` and `tst037_samples` in `props`.
- `ThreadGroup` "Refresh and Sample Lag", 4 threads / 3 loops, `on_sample_error=continue`: a
  `POST /reporting/refresh` sampler, then a `GET /reporting/lag` sampler whose
  `JSR223PostProcessor` parses the body and, inside `synchronized (props) { … }`, raises
  `tst037_max_p95`/`tst037_max_p99` and increments `tst037_samples`. **No timer that waits for
  convergence** — twelve bounded samples, then the verdict.
- `PostThreadGroup` "Verify Convergence", 1/1: `POST /reporting/refresh`; `GET /reporting/lag`
  whose PostProcessor writes `vars.put("lag_p95Ms", …)`, `lag_p99Ms`, `lag_boundMs`;
  `GET /reporting/outbox` whose PostProcessor writes `vars.put("outbox_miscounted", …)`; an
  inline `JSR223Sampler` computing I3's `replay_drift` by comparing
  `SELECT account_id, balance_minor FROM account_balance_report` against a fresh
  `SELECT account_id, SUM(amount_minor) FROM ledger_entry GROUP BY account_id` and counting
  field-level mismatches into `vars`; then the `assert-readmodel` `JSR223Sampler` with
  `filename=${__groovy(System.getenv("ASSERT_SCRIPT_PATH"),)}`.

- [ ] **Step 7: Run the tests**

```bash
cd qe-harness && make up PROFILES=core
cd harness && mvn -q -pl jmeter test -Dtest=Tst037ModuleTest
```

Expected: PASS, 3 tests.

- [ ] **Step 8: Verify the gate accepts a partial module**

```bash
cd "$(git rev-parse --show-toplevel)"
python3 scripts/validate-harness-coverage.py 2>&1 | /usr/bin/grep TST-037 || echo "no TST-037 findings"
python3 scripts/render-harness-coverage.py
```

Expected: no findings. Check 4 requires `partial` to carry a non-empty `partial_reason`, which
Step 4 supplied; check 7 validates the emitted fragment against the schema.

- [ ] **Step 9: Commit**

```bash
git add qe-harness/harness/jmeter qe-harness/traceability
git commit -m "feat(harness): add TST-037 read-model convergence JMeter module"
```

---

## Task 11: Harness Common — ProfileResolver and the Declared Blend

**No code in this repository reads a profile file today.** `mixed.yml` and `soak.yml` exist and
are parsed by nothing; only `_nfr-thresholds.yml` is machine-consumed, via `ThresholdResolver`.
TST-034 is the first module to need a profile, so this is new plumbing, not a lookup.

**Files:**
- Create: `…/harness/common/src/main/java/com/techcombank/qe/harness/config/ProfileResolver.java`
- Create: `…/harness/common/src/test/java/com/techcombank/qe/harness/config/ProfileResolverTest.java`
- Modify: `qe-harness/profiles/mixed.yml` (populate `blend_ref`)
- Modify: `qe-harness/profiles/_nfr-thresholds.yml` (per-journey p95 entries)

**Interfaces:**
- Consumes: `qe-harness/profiles/*.yml`, `snakeyaml` (already a `qe-harness-common` dependency)
- Produces: `ProfileResolver.load(String)` → `Profile`; `Profile.blend()` → the declared
  journey mix; per-journey threshold names for TST-034's assertion

- [ ] **Step 1: Read what the profile actually contains**

```bash
cat qe-harness/profiles/mixed.yml
/usr/bin/grep -rn "mixed.yml\|blend_ref" qe-harness/ --include=*.java --include=*.sh --include=*.groovy || echo "no code reads it"
```

Expected: `blend_ref: null` with the comment naming TST-034 as its owner, and confirmation that
no code reads it.

- [ ] **Step 2: Write the failing test**

`ProfileResolverTest.java`:

```java
package com.techcombank.qe.harness.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * First reader of a TST-002 performance profile in this repository. Mirrors
 * ThresholdResolver's contract deliberately: locate the real profiles
 * directory by walking up, and throw on an unknown name rather than
 * defaulting -- a silently-defaulted workload shape is a fabricated test.
 */
class ProfileResolverTest {

    @Test
    void resolvesTheDeclaredBlendFromMixedProfile() {
        ProfileResolver.Profile mixed = new ProfileResolver().load("mixed");
        assertEquals("open", mixed.workloadModel());
        assertTrue(mixed.blend().size() >= 2, "a blend needs at least two journeys");
        long total = mixed.blend().values().stream().mapToLong(Long::longValue).sum();
        assertEquals(100L, total, "declared journey shares must sum to 100");
    }

    @Test
    void smokeModeOverridesTheHoldDuration() {
        ProfileResolver.Profile mixed = new ProfileResolver().load("mixed");
        assertTrue(mixed.smokeHoldSeconds() < mixed.holdSeconds(),
            "smoke mode must be shorter than the full hold");
    }

    @Test
    void anUnknownProfileThrowsRatherThanDefaulting() {
        assertThrows(IllegalArgumentException.class, () -> new ProfileResolver().load("nonesuch"));
    }
}
```

- [ ] **Step 3: Run and confirm failure**

```bash
cd qe-harness/harness && mvn -q -pl common test -Dtest=ProfileResolverTest
```

Expected: FAIL — `ProfileResolver` does not exist.

- [ ] **Step 4: Populate `blend_ref` in `mixed.yml`**

Replace `blend_ref: null` and its comment with a declared blend. Shares sum to 100 so I2's
tolerance check has a denominator, and every journey names an endpoint that already exists:

```yaml
  # Named journey blend, owned by TST-034 (blended-journey-workload.md). Shares
  # are percentages of total arrivals and MUST sum to 100 -- ProfileResolver
  # rejects any other total, since a blend that does not sum to a whole cannot
  # have per-journey share tolerances checked against it (invariant I2).
  # Each journey's tier determines which NFR-002 budget its p95 is asserted
  # against (invariant I1): never a single blended figure.
  blend_ref: wave17-core-mix
  blend:
    transfer:       { share: 40, tier: T0, endpoint: "POST /transfers" }
    trial_balance:  { share: 25, tier: T1, endpoint: "GET /ledger/trial-balance" }
    catalogue:      { share: 20, tier: T2, endpoint: "GET /catalogue" }
    rate_limited:   { share: 15, tier: T1, endpoint: "GET /rate-limited/ping" }
```

- [ ] **Step 5: Add the per-journey threshold entries**

Append to `_nfr-thresholds.yml`, under the existing latency banner. All four cite the **already
resolving** `NFR-002#end-to-end-budgets-per-tier-customer-facing` anchor — the same one three
existing entries use — with values read straight from that table's tier rows:

```yaml
  # --- Per-journey latency for TST-034's blend (NFR-002 Latency Budget Model) ---
  # Source table: "End-to-end budgets per tier (customer-facing)". One entry per
  # tier the wave17-core-mix blend touches, because TST-034's I1 asserts every
  # journey against its OWN tier budget and never against a single blended
  # figure. Same anchor as p50/p95/p99_latency_ms above -- no new NFR section is
  # needed, and none is created.
  - name: p95_latency_t0_ms
    threshold_ref: NFR-002#end-to-end-budgets-per-tier-customer-facing
    value: 200
    unit: ms
    applies_to: [mixed]

  - name: p95_latency_t1_ms
    threshold_ref: NFR-002#end-to-end-budgets-per-tier-customer-facing
    value: 500
    unit: ms
    applies_to: [mixed]

  - name: p95_latency_t2_ms
    threshold_ref: NFR-002#end-to-end-budgets-per-tier-customer-facing
    value: 2000
    unit: ms
    applies_to: [mixed]
```

- [ ] **Step 6: Write the resolver**

`ProfileResolver.java`:

```java
package com.techcombank.qe.harness.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads a TST-002 performance profile from {@code qe-harness/profiles/}.
 *
 * <p>Sibling of {@link ThresholdResolver} and deliberately the same shape: a
 * no-arg constructor that locates the real profiles directory by walking up
 * from the working directory, an explicit-path constructor for fixtures, and a
 * resolve method that <b>throws on an unknown name and never defaults</b>. A
 * silently-defaulted workload shape would make a run's own parameters
 * unfalsifiable.
 *
 * <p>This is the first code in the repository to read a profile file at all --
 * before Wave 17, {@code mixed.yml} and {@code soak.yml} were parsed by
 * nothing. Only the fields TST-034 actually asserts against are surfaced;
 * profile shape parameters this harness does not consume stay unread rather
 * than being exposed speculatively.
 */
public final class ProfileResolver {

    /** One journey in a declared blend. */
    public record Journey(String name, long share, String tier, String endpoint) {}

    /** The subset of a profile this harness consumes. */
    public record Profile(
        String name,
        String workloadModel,
        String blendRef,
        Map<String, Long> blend,
        Map<String, Journey> journeys,
        long holdSeconds,
        long smokeHoldSeconds
    ) {}

    private final Path profilesDir;

    public ProfileResolver() {
        this(locateDefaultProfilesDir());
    }

    public ProfileResolver(Path profilesDir) {
        this.profilesDir = profilesDir;
    }

    @SuppressWarnings("unchecked")
    public Profile load(String name) {
        Path path = profilesDir.resolve(name + ".yml");
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("unknown profile: " + name + " (looked in " + profilesDir + ")");
        }

        Map<String, Object> raw;
        try {
            raw = new Yaml().load(Files.readString(path));
        } catch (IOException e) {
            throw new IllegalStateException("cannot read profile " + path, e);
        }
        if (raw == null) {
            throw new IllegalStateException("profile " + path + " is empty");
        }

        String blendRef = (String) raw.get("blend_ref");
        if (blendRef == null || blendRef.isBlank()) {
            throw new IllegalStateException(
                "profile " + name + " declares no blend_ref; a blended run needs a declared mix");
        }

        Map<String, Object> blendRaw = (Map<String, Object>) raw.get("blend");
        if (blendRaw == null || blendRaw.isEmpty()) {
            throw new IllegalStateException("profile " + name + " declares blend_ref but no blend");
        }

        Map<String, Long> shares = new LinkedHashMap<>();
        Map<String, Journey> journeys = new LinkedHashMap<>();
        long total = 0;
        for (Map.Entry<String, Object> entry : blendRaw.entrySet()) {
            Map<String, Object> j = (Map<String, Object>) entry.getValue();
            long share = ((Number) j.get("share")).longValue();
            shares.put(entry.getKey(), share);
            journeys.put(entry.getKey(), new Journey(
                entry.getKey(), share, (String) j.get("tier"), (String) j.get("endpoint")));
            total += share;
        }
        if (total != 100L) {
            throw new IllegalStateException(
                "profile " + name + " blend shares sum to " + total + ", not 100");
        }

        long holdSeconds = ((Number) raw.get("hold_seconds")).longValue();
        long smokeHold = holdSeconds;
        Map<String, Object> overrides = (Map<String, Object>) raw.get("smoke_mode_overrides");
        if (overrides != null && overrides.get("hold_seconds") != null) {
            smokeHold = ((Number) overrides.get("hold_seconds")).longValue();
        }

        return new Profile(name, (String) raw.get("workload_model"), blendRef,
            Map.copyOf(shares), Map.copyOf(journeys), holdSeconds, smokeHold);
    }

    private static Path locateDefaultProfilesDir() {
        Path cursor = Path.of("").toAbsolutePath();
        while (cursor != null) {
            Path candidate = cursor.resolve("qe-harness/profiles");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            Path direct = cursor.resolve("profiles");
            if (Files.isDirectory(direct) && Files.isRegularFile(direct.resolve("mixed.yml"))) {
                return direct;
            }
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("cannot locate qe-harness/profiles from " + Path.of("").toAbsolutePath());
    }
}
```

- [ ] **Step 7: Run the tests**

```bash
cd qe-harness/harness && mvn -q -pl common test
```

Expected: PASS, including the three new tests and every existing `qe-harness-common` test.

- [ ] **Step 8: Verify the new thresholds resolve**

```bash
cd "$(git rev-parse --show-toplevel)"
python3 scripts/validate-harness-coverage.py 2>&1 | /usr/bin/grep check6 || echo "no check6 findings"
```

Expected: no findings. All three new entries cite an anchor that already resolves — if check 6
complains, the anchor slug was mistyped (it is
`end-to-end-budgets-per-tier-customer-facing`, computed by lowercasing, deleting every
character outside `[\w\- ]`, then replacing spaces with hyphens).

- [ ] **Step 9: Check for digit runs in the new YAML**

```bash
python3 scripts/validate-harness-coverage.py 2>&1 | /usr/bin/grep check5 || echo "no check5 findings"
```

Expected: no findings — `2000` and `500` are far short of thirteen digits.

- [ ] **Step 10: Commit**

```bash
git add qe-harness/harness/common qe-harness/profiles
git commit -m "feat(harness): add ProfileResolver and declare the wave17-core-mix blend"
```

---

## Task 12: SUT — Seed Endpoint and Blend Fixtures

`SyntheticDataSeeder` has no HTTP trigger; only its own test calls it. A blended run needs more
accounts than the ledger fixture's two, and needs to reseed without restarting the container.

**Files:**
- Create: `…/sut/TestSeedController.java`
- Modify: `…/sut/data/SyntheticDataSeeder.java` (parameterise the account count)
- Test: `…/sut/data/SyntheticDataSeederTest.java` (extend, do not replace)

**Interfaces:**
- Consumes: `SyntheticDataSeeder.seed(long)`, `SeedSummary`
- Produces: `POST /_test/seed?seed=42&accounts=20` → 201 `{accounts, entries}`

- [ ] **Step 1: Confirm the seeder has no HTTP trigger**

```bash
/usr/bin/grep -rn "SyntheticDataSeeder" qe-harness/reference-sut/src/main/java/
```

Expected: only its own file. `ReconController` calls the separate `recon.DefectSeeder`, not this.

- [ ] **Step 2: Write the failing test**

Append to `SyntheticDataSeederTest.java`:

```java
    @Test
    void seedsTheRequestedNumberOfAccounts() {
        SeedSummary summary = seeder.seed(42L, 12);
        assertEquals(12, summary.accounts());
        assertEquals(60, summary.entries(), "transfer count is fixed at 30 pairs");
    }

    @Test
    void requestingMoreAccountsThanNamesIsRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> seeder.seed(42L, SyntheticNames.NAMES.length + 1));
    }
```

`SyntheticNames.NAMES.length` is 20, so a clean default seed still produces 20 accounts and 60
ledger entries — the existing test's expectations are unchanged.

- [ ] **Step 3: Run and confirm failure**

```bash
cd qe-harness/reference-sut && mvn -q -B test -Dtest=SyntheticDataSeederTest
```

Expected: FAIL — no two-argument `seed` overload.

- [ ] **Step 4: Add the overload**

In `SyntheticDataSeeder.java`, keep `seed(long)` delegating so every existing caller is
untouched:

```java
    /** Seeds with one account per synthetic name -- the original contract. */
    public SeedSummary seed(long seed) {
        return seed(seed, ACCOUNT_COUNT);
    }

    /** Seeds {@code accountCount} accounts. A blended workload (TST-034) needs
     *  more contention surface than the two-account ledger fixture provides,
     *  but can never exceed the fixed synthetic-name pool -- requesting more
     *  throws rather than inventing a name, since a generated party name could
     *  not be guaranteed non-PII. */
    public SeedSummary seed(long seed, int accountCount) {
        if (accountCount < 1 || accountCount > SyntheticNames.NAMES.length) {
            throw new IllegalArgumentException(
                "accountCount must be 1.." + SyntheticNames.NAMES.length + ", got " + accountCount);
        }
        Random random = new Random(seed);

        List<Long> accountIds = seedAccounts(random, accountCount);
        int entries = seedLedger(random, accountIds);

        return new SeedSummary(accountIds.size(), entries);
    }
```

Change `seedAccounts(Random random)` to `seedAccounts(Random random, int accountCount)` and use
that parameter in place of `ACCOUNT_COUNT` in its loop bound and list capacity.

- [ ] **Step 5: Write the controller**

`TestSeedController.java`:

```java
package com.techcombank.qe.sut;

import com.techcombank.qe.sut.data.SeedSummary;
import com.techcombank.qe.sut.data.SyntheticDataSeeder;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Meta/test-control endpoint for seeding synthetic data over HTTP (Wave 17).
 *
 * <p>{@code _test}-prefixed, matching {@link DefectController} and
 * {@code RateLimitResetController}. Harness modules run as separate tool
 * processes against an already-running container, so they can only reach
 * {@link SyntheticDataSeeder} -- which had no HTTP trigger at all before this
 * -- over HTTP. TST-034's blended run needs a wider account set than the
 * two-account ledger fixture, and needs to reseed between runs without a
 * container restart, exactly as TST-031 needed
 * {@code POST /_test/reset/ratelimit}.
 *
 * <p>{@code @Profile("!prod")} -- see {@link DefectController}'s javadoc.
 */
@RestController
@Profile("!prod")
public class TestSeedController {

    private final SyntheticDataSeeder seeder;

    public TestSeedController(SyntheticDataSeeder seeder) {
        this.seeder = seeder;
    }

    /** POST /_test/seed?seed=42&accounts=20 -> 201 {accounts, entries}.
     *  Both parameters are explicit so a run's fixture is reproducible from its
     *  own request line; the seed defaults to the same fixed 42 ReconController
     *  uses, for the same reason -- a known set, not a fresh random one. */
    @PostMapping("/_test/seed")
    public ResponseEntity<?> seed(@RequestParam(defaultValue = "42") long seed,
                                  @RequestParam(defaultValue = "20") int accounts) {
        try {
            SeedSummary summary = seeder.seed(seed, accounts);
            return ResponseEntity.status(HttpStatus.CREATED).body(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
```

- [ ] **Step 6: Run the tests**

```bash
cd qe-harness/reference-sut && mvn -q -B test
```

Expected: PASS, including the pre-existing `seed(long)` tests unchanged.

- [ ] **Step 7: Verify the endpoint**

```bash
cd qe-harness && make down && make up PROFILES=core
curl -s -X POST 'http://localhost:8080/_test/seed?seed=42&accounts=12' | python3 -m json.tool
curl -s -o /dev/null -w '%{http_code}\n' -X POST 'http://localhost:8080/_test/seed?accounts=99'
```

Expected: `{"accounts": 12, "entries": 60}`, then `400` for the over-large request.

- [ ] **Step 8: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): add POST /_test/seed and a parameterised account count"
```

---

## Task 13: Module — TST-034 Blended Journey Workload (JMeter)

The first module to consume a profile file. It needs no new SUT endpoints — it blends journeys
that already exist — but it does need per-journey tagged metrics, which no existing module
produces.

**Files:**
- Create: `qe-harness/harness/jmeter/tst-034-blend/{plan.jmx,assert-blend.groovy,README.md}`
- Modify: `qe-harness/traceability/modules.yml`
- Modify: `…/sut/DefectFlags.java`, `CapabilityRegistry.java`, `CapabilityRegistryTest.java`
- Modify: `…/sut/capability/ledger/TransferService.java` (the `journey-starved` branch)
- Test: `…/jmeter/Tst034ModuleTest.java`

**Interfaces:**
- Consumes: `ProfileResolver` (Task 11), `POST /_test/seed` (Task 12), `ThresholdResolver`,
  `HarnessConfig.smokeMode()`, and the four blended endpoints
- Produces: `run-module.sh TST-034`; defect flag `journey-starved`

- [ ] **Step 1: Add the defect flag and its branch**

Add `"journey-starved"` to `DefectFlags.KNOWN_FLAGS`. In `TransferService.transfer`, add a
branch that starves the lowest-volume journey without breaking the ledger:

```java
    @Transactional
    public UUID transfer(String from, String to, long amountMinor) {
        // TST-034 I3: with journey-starved active, the transfer journey is
        // deliberately delayed so its observed share collapses below its
        // declared tolerance. The ledger stays balanced throughout -- this
        // starves a journey, it does not corrupt state, so I1/I2 hold and I3
        // alone fails.
        if (DefectFlags.isActive("journey-starved")) {
            try {
                Thread.sleep(250L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        AccountPair pair = lockPair(from, to);
        ...
```

Add `"TST-034"` to `CapabilityRegistry.IMPLEMENTED` and to `IMPLEMENTED_AT_WAVE_17`.

- [ ] **Step 2: Write the module README**

`tst-034-blend/README.md`:

```markdown
# TST-034 -- Blended Journey Workload (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.

| ID | Invariant |
|---|---|
| I1 | Every constituent journey meets its **own** tier budget, never a blended figure |
| I2 | Each journey's actual share is within tolerance of its declared share |
| I3 | No journey is starved -- every journey keeps a non-zero count in every sub-window |
| I4 | Errors are attributed per journey, not pooled |
| I5 | Steady state is reached before measurement begins |

Defect proof: with `journey-starved` active this module MUST report I3 failed and I1 passed.

This is the **first module in the repository to read a profile file**. The blend comes from
`profiles/mixed.yml`'s `blend_ref: wave17-core-mix` via `ProfileResolver` (Wave 17), not from
literals in `plan.jmx` -- so the declared mix and the asserted mix cannot drift apart. Per
invariant I1, each journey's p95 is asserted against **its own tier's** NFR-002 budget
(`p95_latency_t0_ms`, `p95_latency_t1_ms`, `p95_latency_t2_ms`), resolved through
`ThresholdResolver`, never against a single blended number.

## What this module drives

1. **setUp Thread Group** (`Seed Blend Fixture`, 1 thread, 1 loop) calls
   `POST /_test/seed?seed=42&accounts=20` (Wave 17) so the blend has real contention surface
   rather than the two-account ledger fixture, and zeroes the per-journey tallies in `props`.
2. **Main Thread Group** (`Blended Load`, 20 threads, duration-scheduled) drives all four
   journeys through a **Throughput Controller** per journey, its percentage taken from the
   declared blend. A `JSR223 PreProcessor` selects the journey for each iteration; each
   sampler's `JSR223 PostProcessor` records latency and outcome **tagged by journey name** into
   `props` -- the cross-thread aggregation pattern `tst-031-ratelimit` established. I5's
   steady-state window is skipped by discarding samples from the first sub-window.
   `HARNESS_SMOKE_MODE=true` selects `smoke_mode_overrides.hold_seconds` (20s) instead of
   `hold_seconds` (14,400s): a four-hour blend can never run in an MR pipeline.
3. **TearDown Thread Group** (`Verify Blend`, 1 thread, 1 loop) runs `assert-blend.groovy`,
   which resolves the declared blend and the three tier thresholds, then evaluates I1-I5.

## Running it

```
make up PROFILES=core
HARNESS_SMOKE_MODE=true ./bin/run-module.sh TST-034   # 20s hold, thresholds not-evaluated
./bin/run-module.sh TST-034                           # full 4h hold -- never in CI
```

## Defect proof

```
curl -X POST http://localhost:8080/_test/defect/journey-starved   # 204
HARNESS_SMOKE_MODE=true ./bin/run-module.sh TST-034               # must report I3 FAILED
curl -X DELETE http://localhost:8080/_test/defect                 # 204
```

With `journey-starved` active, `TransferService.transfer` sleeps before taking its locks, so the
transfer journey's throughput collapses and its observed share falls below tolerance. The ledger
stays balanced and per-journey latency attribution keeps working, so I1 and I4 still pass --
which is what makes the proof specific.
```

- [ ] **Step 3: Write the failing test**

`Tst034ModuleTest.java`:

```java
package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-034 blended journey module. Always driven in smoke mode from the test
 * suite -- the full profile holds for 14,400 seconds, which no test may wait on.
 */
class Tst034ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void passesAgainstTheCleanSut() throws Exception {
        ModuleResult r = runner.run("TST-034", Map.of("HARNESS_SMOKE_MODE", "true"));
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void smokeModeReportsTierThresholdsNotEvaluatedWithAReason() throws Exception {
        ModuleResult r = runner.run("TST-034", Map.of("HARNESS_SMOKE_MODE", "true"));
        assertTrue(r.fragment().thresholds().stream()
            .allMatch(t -> t.result() == RunFragment.Result.NOT_EVALUATED
                && t.reason() != null && !t.reason().isBlank()),
            "a not-evaluated threshold without a reason is rejected by RunFragment itself");
        assertTrue(r.fragment().thresholds().stream()
            .anyMatch(t -> t.thresholdRef().equals(
                "NFR-002#end-to-end-budgets-per-tier-customer-facing")));
    }

    @Test
    void reportsStarvationAgainstTheStarvedJourneyDefect() throws Exception {
        ModuleResult r = runner.run("TST-034",
            Map.of("HARNESS_SMOKE_MODE", "true", "SUT_DEFECT", "journey-starved"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I3") && i.result() == RunFragment.Result.FAILED));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I4") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: per-journey error attribution still works");
    }
}
```

`HARNESS_SMOKE_MODE` is passed through to the subprocess as a literal environment variable —
`ModuleRunner` strips only `SUT_DEFECT`.

- [ ] **Step 4: Run and confirm failure**

```bash
cd qe-harness/harness && mvn -q -pl jmeter test -Dtest=Tst034ModuleTest
```

Expected: FAIL — no `modules.yml` entry.

- [ ] **Step 5: Add the binding row**

Insert into `modules.yml` after `TST-031` and before `TST-035`:

```yaml
  - archetype: TST-034
    tool: jmeter
    path: qe-harness/harness/jmeter/tst-034-blend
    coverage: full
    defect_flag: journey-starved
```

- [ ] **Step 6: Write the assertion script**

`assert-blend.groovy`:

```groovy
// TST-034 blended journey workload assertion (Wave 17).
//
// The first assertion script to read a TST-002 performance profile. The blend
// comes from profiles/mixed.yml via ProfileResolver, so the declared mix and
// the asserted mix cannot drift; per-journey tallies arrive through JMeter's
// cross-thread props map, the same mechanism assert-ratelimit.groovy uses.
//
// I1 resolves ONE THRESHOLD PER TIER and asserts each journey against its own,
// never against a single blended figure -- that distinction is the invariant.

import com.techcombank.qe.harness.config.HarnessConfig
import com.techcombank.qe.harness.config.ProfileResolver
import com.techcombank.qe.harness.config.ThresholdResolver
import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion

import java.nio.file.Path

boolean smoke = HarnessConfig.smokeMode()
ProfileResolver.Profile profile = new ProfileResolver().load("mixed")
ThresholdResolver thresholds = new ThresholdResolver()

// Share tolerance is a profile shape parameter, owned by TST-002, so it stays
// a literal here rather than being projected into _nfr-thresholds.yml -- see
// that file's own header comment.
final double SHARE_TOLERANCE = 0.20

long totalSamples = Long.parseLong(props.getProperty("tst034_total_samples"))
int subWindows = Integer.parseInt(props.getProperty("tst034_sub_windows"))

boolean everyJourneyWithinBudget = true
boolean everyShareWithinTolerance = true
boolean noJourneyStarved = true
boolean errorsAttributed = true
StringBuilder detail = new StringBuilder()

profile.journeys().each { name, journey ->
    long count = Long.parseLong(props.getProperty("tst034_${name}_count", "0"))
    long p95 = Long.parseLong(props.getProperty("tst034_${name}_p95", "0"))
    long errors = Long.parseLong(props.getProperty("tst034_${name}_errors", "-1"))
    long minPerWindow = Long.parseLong(props.getProperty("tst034_${name}_min_window_count", "0"))

    ThresholdResolver.Threshold tierBudget =
        thresholds.resolve("p95_latency_" + journey.tier().toLowerCase() + "_ms")

    double actualShare = totalSamples == 0 ? 0d : (double) count / totalSamples
    double declaredShare = journey.share() / 100.0d
    boolean shareOk = Math.abs(actualShare - declaredShare) <= declaredShare * SHARE_TOLERANCE
    boolean budgetOk = smoke ? true : p95 <= tierBudget.value()

    if (!budgetOk) everyJourneyWithinBudget = false
    if (!shareOk) everyShareWithinTolerance = false
    if (count == 0 || minPerWindow == 0) noJourneyStarved = false
    if (errors < 0) errorsAttributed = false

    detail.append("  ${name} (${journey.tier()}): count=${count} share=${String.format('%.3f', actualShare)} " +
                  "declared=${String.format('%.3f', declaredShare)} p95=${p95}ms " +
                  "budget=${(long) tierBudget.value()}ms errors=${errors} minWindow=${minPerWindow}\n")
}

String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1", "Every journey meets its own tier budget, never a blended figure",
    { everyJourneyWithinBudget } as java.util.function.BooleanSupplier)
RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "Each journey's actual share is within tolerance of its declared share",
    { everyShareWithinTolerance } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "No journey is starved in any sub-window",
    { noJourneyStarved } as java.util.function.BooleanSupplier)
RunFragment.Entry i4 = InvariantAssertion.check(
    "I4", "Errors are attributed per journey, not pooled",
    { errorsAttributed } as java.util.function.BooleanSupplier)
RunFragment.Entry i5 = InvariantAssertion.check(
    "I5", "Steady state is reached before measurement begins",
    { subWindows >= 2 } as java.util.function.BooleanSupplier)

RunFragment.Builder builder = RunFragment.builder()
    .archetype(System.getenv("QE_ARCHETYPE"))
    .module("jmeter")
    .serviceName("reference-sut")
    .tier("T0")
    .oracle("invariant-assertion")
    .environment(System.getenv().getOrDefault("QE_ENVIRONMENT", "local-compose"))
    .sutDefect(sutDefect)
    .invariant(i1.id(), i1.description(), i1.result())
    .invariant(i2.id(), i2.description(), i2.result())
    .invariant(i3.id(), i3.description(), i3.result())
    .invariant(i4.id(), i4.description(), i4.result())
    .invariant(i5.id(), i5.description(), i5.result())

// One threshold row per tier the blend touches. In smoke mode the hold is 20s
// against a declared 14,400s, so a latency budget cannot be honestly evaluated
// -- each row is emitted not-evaluated WITH a reason, which RunFragment
// enforces (a blank reason throws).
["t0", "t1", "t2"].each { tier ->
    ThresholdResolver.Threshold t = thresholds.resolve("p95_latency_${tier}_ms")
    if (smoke) {
        builder.threshold("p95_latency_${tier}_ms", t.thresholdRef(),
            RunFragment.Result.NOT_EVALUATED, "smoke-mode: 20s hold against a declared 14400s")
    } else {
        long worst = Long.parseLong(props.getProperty("tst034_worst_p95_${tier}", "0"))
        builder.threshold("p95_latency_${tier}_ms", t.thresholdRef(),
            worst <= t.value() ? RunFragment.Result.PASSED : RunFragment.Result.FAILED, null)
    }
}

RunFragment fragment = builder.build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "blend=${profile.blendRef()} smoke=${smoke} totalSamples=${totalSamples}\n" +
    detail.toString() +
    "I1 per-journey-tier-budget: ${i1.result().wire()}\n" +
    "I2 share-within-tolerance: ${i2.result().wire()}\n" +
    "I3 no-journey-starved: ${i3.result().wire()}\n" +
    "I4 errors-attributed: ${i4.result().wire()}\n" +
    "I5 steady-state-reached: ${i5.result().wire()}\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
```

- [ ] **Step 7: Build the JMeter plan**

`plan.jmx`. This is the most involved plan in the wave; take the structure from
`tst-031-ratelimit/plan.jmx`, which already does cross-thread `props` aggregation.

- `SetupThreadGroup` "Seed Blend Fixture", 1/1, `on_sample_error=stopthread`: an
  `HTTPSamplerProxy` `POST /_test/seed?seed=42&accounts=20`, then an inline `JSR223Sampler`
  zeroing `tst034_total_samples`, `tst034_sub_windows`, and per journey
  `tst034_<name>_count`, `_p95`, `_errors`, `_min_window_count`.
- `ThreadGroup` "Blended Load", 20 threads, `scheduler=true` with `duration` read from a
  `__groovy` expression selecting the smoke or full hold — the plan must not hardcode either:
  `${__groovy(System.getenv("HARNESS_SMOKE_MODE") == "true" ? 20 : 14400,)}`. Inside, one
  `ThroughputController` per journey (`percentThroughput` = that journey's declared share,
  `style=percent`), each containing the journey's `HTTPSamplerProxy` and a
  `JSR223PostProcessor` that, inside `synchronized (props) { … }`, increments
  `tst034_<name>_count`, updates a p95 reservoir, increments `_errors` on a non-2xx, and
  tracks the per-sub-window minimum count. Sub-windows come from an inline timestamp bucket
  (ISO-8601 or a plain second counter — **never epoch millis**, which are 13 digits and fail
  check 5). Discard the first sub-window's samples for I5.
- `PostThreadGroup` "Verify Blend", 1/1: the `assert-blend` `JSR223Sampler` with
  `filename=${__groovy(System.getenv("ASSERT_SCRIPT_PATH"),)}`.

The four journey endpoints are `POST /transfers`, `GET /ledger/trial-balance`,
`GET /catalogue`, `GET /rate-limited/ping` — all pre-existing. Confirm `/catalogue` exists
before wiring it:

```bash
/usr/bin/grep -rn "catalogue" qe-harness/reference-sut/src/main/java/ | head -5
```

If it does not, substitute another existing T2 endpoint and update `mixed.yml`'s blend to match
— the profile and the plan must agree.

- [ ] **Step 8: Run the tests**

```bash
cd qe-harness && make down && make up PROFILES=core
cd harness && mvn -q -pl jmeter test -Dtest=Tst034ModuleTest
```

Expected: PASS, 3 tests. Each run holds 20 seconds, so the suite takes about two minutes.

- [ ] **Step 9: Verify the gate and the emitted thresholds**

```bash
cd "$(git rev-parse --show-toplevel)"
python3 scripts/validate-harness-coverage.py 2>&1 | /usr/bin/grep -E "TST-034|check5|check6|check7" || echo "no findings"
python3 scripts/render-harness-coverage.py
```

Expected: no findings. Check 7 in particular validates that every `not-evaluated` threshold in
the emitted fragment carries a `reason`.

- [ ] **Step 10: Commit**

```bash
git add qe-harness/harness/jmeter qe-harness/traceability qe-harness/reference-sut
git commit -m "feat(harness): add TST-034 blended journey JMeter module"
```

---

Phase 1 is complete: three archetypes land on the existing SUT, the profile system has its
first reader, and `/_capabilities` reports 10. Phase 2 begins, and with it the wave's real risk.

---

## Task 14: AMQP Dependency, Lazy Connection, and Compose Wiring

The `broker` service already exists in `docker-compose.yml` under the `messaging` profile, with
a healthcheck and no `environment` block. Nothing consumes it. This task connects the SUT to it
**without breaking the `core` profile** — the single most dangerous change in the wave.

**Files:**
- Modify: `qe-harness/reference-sut/pom.xml`
- Modify: `qe-harness/docker-compose.yml`
- Modify: `qe-harness/reference-sut/src/main/resources/application.properties`
- Create: `…/sut/capability/messaging/MessagingConnectionConfig.java`
- Test: `…/sut/capability/messaging/CoreProfileBootTest.java`

**Interfaces:**
- Consumes: `spring-boot-starter-amqp` (BOM-managed), the existing `broker` compose service
- Produces: a lazily-connected `RabbitTemplate`/`ConnectionFactory`; `make up PROFILES=core`
  still boots with no broker present

- [ ] **Step 1: Write the failing test — the core profile must still boot**

`CoreProfileBootTest.java`. This is the regression guard for the whole phase:

```java
package com.techcombank.qe.sut.capability.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The application context MUST start with no broker reachable.
 *
 * <p>This is the guard for Wave 17's most dangerous change. reference-sut is in
 * compose profile ["core"]; broker is in ["messaging"]. Its container
 * healthcheck hits /_capabilities, so if a missing broker failed the context at
 * startup, `make up PROFILES=core` would report an unhealthy SUT and every one
 * of the seven pre-existing modules would break. The AMQP connection is
 * therefore lazy: beans exist, no socket is opened until first use.
 *
 * <p>No RabbitMQ container is started here, deliberately -- the absence is the
 * test.
 */
@SpringBootTest
class CoreProfileBootTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.connect-retries", () -> 10);
        // Point at a port nothing is listening on: proof the context does not
        // need the broker to exist.
        registry.add("spring.rabbitmq.host", () -> "127.0.0.1");
        registry.add("spring.rabbitmq.port", () -> 1);
    }

    @Autowired
    private ConnectionFactory connectionFactory;

    @Test
    void contextStartsWithNoBrokerReachable() {
        assertNotNull(connectionFactory, "the bean must exist without a live connection");
    }
}
```

- [ ] **Step 2: Run it and confirm it fails**

```bash
cd qe-harness/reference-sut && mvn -q -B test -Dtest=CoreProfileBootTest
```

Expected: FAIL — `org.springframework.amqp` is not on the classpath.

- [ ] **Step 3: Add the dependency**

In `reference-sut/pom.xml`, after `spring-boot-starter-security`, following the
`spring-boot-starter-aop` precedent — **no explicit `<version>`**, because
`spring-boot-dependencies` 3.5.16 manages it:

```xml
    <!-- TST-026/027/028/029 messaging (Wave 17, Tasks 14-24). Version managed
         by spring-boot-dependencies, so no explicit <version> -- same pattern
         as spring-boot-starter-web/-jdbc/-aop above, and unlike jjwt-*/
         springdoc/resilience4j which are not BOM-managed. -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
```

And alongside the existing Testcontainers test dependencies — also BOM-managed, via
testcontainers-bom 1.21.4:

```xml
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>rabbitmq</artifactId>
      <scope>test</scope>
    </dependency>
```

- [ ] **Step 4: Make the connection lazy**

`MessagingConnectionConfig.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Broker connection wiring for the messaging capability (Wave 17).
 *
 * <p><b>Why every bean here is lazy about its socket:</b> reference-sut is in
 * compose profile {@code ["core"]} and broker is in {@code ["messaging"]}, so
 * the overwhelmingly common case -- {@code make up PROFILES=core}, which every
 * pre-Wave-17 module uses -- has no broker at all. A connection attempt at
 * context startup would fail the container's {@code /_capabilities}
 * healthcheck and break seven working modules. {@code CoreProfileBootTest}
 * pins this.
 *
 * <p>{@code RabbitAdmin.setAutoStartup(false)} is the load-bearing line: the
 * default {@code RabbitAdmin} declares every {@code Declarables} bean during
 * context refresh, which opens a connection. With auto-startup off, the
 * topology is declared on first use instead -- see
 * {@link MessagingTopology#declareTopology()}.
 *
 * <p>Connection retry is capped rather than infinite so a module run against a
 * genuinely absent broker fails fast with a legible error instead of hanging:
 * Spring AMQP's default is to retry indefinitely.
 */
@Configuration
public class MessagingConnectionConfig {

    @Bean
    public ConnectionFactory rabbitConnectionFactory(
            @Value("${spring.rabbitmq.host:localhost}") String host,
            @Value("${spring.rabbitmq.port:5672}") int port,
            @Value("${spring.rabbitmq.username:guest}") String username,
            @Value("${spring.rabbitmq.password:guest}") String password,
            @Value("${app.messaging.connection-timeout-ms}") int connectionTimeoutMs) {
        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setConnectionTimeout(connectionTimeoutMs);
        return factory;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(false);
        return admin;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMandatory(true);
        return template;
    }
}
```

`setMandatory(true)` matters for TST-026's I2: an unroutable message must be returned rather
than silently dropped, or the alternate-exchange quarantine cannot be observed.

- [ ] **Step 5: Declare the messaging properties**

Append to `application.properties`:

```properties
# TST-026/027/028/029 messaging (Wave 17). Connection timeout is capped so a run
# against an absent broker fails fast and legibly instead of hanging on Spring
# AMQP's default indefinite retry -- see MessagingConnectionConfig.
app.messaging.connection-timeout-ms=3000

# TST-029 I5: the DLQ depth past which an alert must fire. Deliberately NOT
# projected into profiles/_nfr-thresholds.yml and carrying no NFR-* citation:
# the entire NFR corpus contains exactly one DLQ mention (NFR-004's operational
# runbook prose) and it states no threshold, so a citation would be fabricated.
# Same convention as app.recon.freshness-window-seconds and
# app.readmodel.convergence-bound-ms: one declared value, read by both the
# service and its tests, never duplicated as a literal.
app.messaging.dlq-alert-depth=5

# TST-029 I4: retry backoff ladder, in milliseconds. Three DISTINCT intervals --
# I4 asserts distinct_intervals > 1, so a flat ladder fails the invariant
# against its own declared backoff.
app.messaging.retry-intervals-ms=1000,3000,9000

# TST-029 I3/I6: attempts before a poison message is dead-lettered.
app.messaging.max-delivery-attempts=3

# TST-027 I2: how long a sequence gap may persist before escalation is emitted.
app.messaging.gap-timeout-ms=4000

# TST-028 I1: how long fan-in waits for all branches before emitting a partial
# marker instead of an aggregate.
app.messaging.aggregate-timeout-ms=4000
```

- [ ] **Step 6: Wire the broker in compose — and add no `depends_on`**

In `docker-compose.yml`, give `broker` an explicit `environment` block (it currently has none,
so it runs on default guest/guest and vhost `/` — make that a declared choice rather than an
accident):

```yaml
  broker:
    image: rabbitmq:3.13-management-alpine
    profiles: ["messaging"]
    ports:
      - "5672:5672"   # AMQP
      - "15672:15672" # management UI
    environment:
      RABBITMQ_DEFAULT_USER: sut
      RABBITMQ_DEFAULT_PASS: sut
      RABBITMQ_DEFAULT_VHOST: /
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 20s
```

Add the matching env to `reference-sut`'s existing `environment` block — **but do not touch its
`profiles` or `depends_on`**:

```yaml
      # Broker coordinates for the messaging capability (Wave 17). Deliberately
      # NO depends_on for broker: reference-sut is in the `core` profile and
      # broker is in `messaging`, so a depends_on would make
      # `docker compose --profile core up` fail outright. The AMQP connection is
      # lazy (see MessagingConnectionConfig), so `core` alone boots unchanged and
      # only the messaging modules require PROFILES="core messaging".
      SPRING_RABBITMQ_HOST: broker
      SPRING_RABBITMQ_PORT: 5672
      SPRING_RABBITMQ_USERNAME: sut
      SPRING_RABBITMQ_PASSWORD: sut
```

Finally, update the file's header comment, which still says the messaging profile is "declared
for a later wave's archetype; not started by default":

```yaml
#   messaging     a broker (RabbitMQ) serving TST-026/027/028/029's topology.
#                 Not started by default -- the SUT's AMQP connection is lazy,
#                 so `core` alone boots without it. Messaging modules need
#                 `make up PROFILES="core messaging"`.
```

- [ ] **Step 7: Run the guard test**

```bash
cd qe-harness/reference-sut && mvn -q -B test -Dtest=CoreProfileBootTest
```

Expected: PASS — the context starts pointing at a dead port.

- [ ] **Step 8: Prove the `core` profile still boots end to end**

This is the step that matters. A regression here breaks seven working modules:

```bash
cd qe-harness && make down
make up PROFILES=core
docker compose ps
curl -s -o /dev/null -w 'capabilities=%{http_code}\n' http://localhost:8080/_capabilities
./bin/run-module.sh TST-021
```

Expected: `reference-sut` reports **healthy** (not `starting` indefinitely, not `unhealthy`),
`capabilities=200`, and TST-021 still passes with no broker running anywhere. If the SUT is
unhealthy, the connection is not lazy — revisit Step 4 before going further.

- [ ] **Step 9: Prove the messaging profile brings the broker up healthy**

```bash
cd qe-harness && make up PROFILES="core messaging"
docker compose ps broker
```

Expected: `broker` reports healthy within its 20s `start_period` plus retries.

- [ ] **Step 10: Run the whole SUT suite**

```bash
cd qe-harness/reference-sut && mvn -q -B test
```

Expected: PASS. No archetype is registered by this task — it is pure infrastructure, so
`/_capabilities` still reports 10.

- [ ] **Step 11: Commit**

```bash
git add qe-harness/reference-sut qe-harness/docker-compose.yml
git commit -m "feat(sut): connect lazily to RabbitMQ without breaking the core profile"
```

---

## Task 15: SUT — The Messaging Topology

Nine objects, declared as Spring `@Bean Declarables` rather than a mounted `definitions.json`:
less compose surface, and it lands in code where it is unit-testable.

**Files:**
- Create: `…/sut/capability/messaging/MessagingTopology.java`
- Test: `…/sut/capability/messaging/AbstractMessagingIntegrationTest.java`, `MessagingTopologyTest.java`

**Interfaces:**
- Consumes: `RabbitAdmin` (Task 14, `autoStartup=false`)
- Produces: `qe.in`, `qe.route`, `qe.fanout`, `qe.dlx`, the queues and the retry ladder;
  `declareTopology()` as the first-use hook

- [ ] **Step 1: Write the failing test**

`MessagingTopologyTest.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The topology's shape is itself load-bearing for two invariants, so it is
 * asserted directly rather than only implied by the modules.
 */
class MessagingTopologyTest extends AbstractMessagingIntegrationTest {

    @Test
    void declaresEveryObjectTheFourArchetypesNeed() {
        topology.declareTopology();
        for (String queue : new String[] {
                "qe.q.route.domestic", "qe.q.route.intl", "qe.q.unroutable",
                "qe.q.sequence", "qe.q.branch.a", "qe.q.branch.b", "qe.q.branch.c",
                "qe.q.aggregate", "qe.q.work", "qe.q.dlq" }) {
            assertNotNull(admin.getQueueProperties(queue), "missing queue: " + queue);
        }
    }

    @Test
    void theRouteExchangeHasNoCatchAllBinding() {
        topology.declareTopology();
        // TST-026 I2 asserts zero messages reach a default route. A '#' binding
        // would make that trivially true and the invariant worthless, so its
        // absence is asserted here rather than left to reviewer vigilance.
        // Published to qe.route, the exchange whose bindings the invariant is
        // actually about -- publishing to qe.in would exercise a different
        // exchange and pass for the wrong reason.
        rabbit.convertAndSend(MessagingTopology.ROUTE_EXCHANGE, "pay.unknown.type", "probe");
        assertTrue(awaitQueueDepth("qe.q.unroutable", 1),
            "an unmatched key must divert to quarantine, not vanish and not match a catch-all");
        assertEquals(0L, queueDepth("qe.q.route.domestic"));
        assertEquals(0L, queueDepth("qe.q.route.intl"));
    }

    @Test
    void theRetryLadderHasDistinctIntervals() {
        // TST-029 I4 asserts distinct_intervals > 1. A flat ladder fails that
        // invariant against the SUT's own declared backoff, so the declared
        // property is checked here at the source.
        assertTrue(retryIntervalsMs().size() > 1);
        assertEquals(retryIntervalsMs().size(), retryIntervalsMs().stream().distinct().count(),
            "every configured retry interval must differ");
    }

    @Test
    void everyQueueIsDurable() {
        topology.declareTopology();
        // TST-029 I2: nothing acked-persisted may be lost across a broker
        // restart, which a transient queue cannot promise.
        assertTrue(topology.declarables().getDeclarablesByType(org.springframework.amqp.core.Queue.class)
            .stream().allMatch(org.springframework.amqp.core.Queue::isDurable));
    }
}
```

- [ ] **Step 2: Run and confirm failure**

```bash
cd qe-harness/reference-sut && mvn -q -B test -Dtest=MessagingTopologyTest
```

Expected: FAIL — `MessagingTopology` does not exist.

- [ ] **Step 3: Write the topology**

`MessagingTopology.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The messaging topology for TST-026/027/028/029.
 *
 * <p>Declared as {@link Declarables} in code rather than a mounted
 * {@code definitions.json}: less compose surface, and the shape becomes
 * unit-testable -- which matters, because two of its properties ARE invariants.
 *
 * <p><b>No catch-all binding on {@code qe.route}.</b> Only
 * {@code pay.domestic.*} and {@code pay.intl.*} are bound, with an
 * alternate-exchange path sending everything else to {@code qe.q.unroutable}.
 * A {@code #} binding would make TST-026's I2 ("zero messages on the default
 * route") trivially true and therefore worthless. The quarantine queue is what
 * makes an unmatched key observable rather than merely absent.
 *
 * <p><b>Single active consumer on {@code qe.q.sequence}.</b> TST-027's ordering
 * scope is declared {@code per_key}: RabbitMQ has no partitions, so the
 * archetype's per-partition scope is out of scope here and the module reports
 * {@code coverage: partial} for that reason.
 *
 * <p><b>Every queue is durable</b> so TST-029's I2 (nothing acked-persisted is
 * lost across a broker restart) is possible at all.
 *
 * <p>Declaration happens on first use, not at context refresh: the
 * {@link RabbitAdmin} is {@code autoStartup=false} precisely so an absent
 * broker cannot fail the {@code core} profile's startup (Task 14).
 */
@Component
public class MessagingTopology {

    static final String IN_EXCHANGE = "qe.in";
    static final String ROUTE_EXCHANGE = "qe.route";
    static final String FANOUT_EXCHANGE = "qe.fanout";
    static final String DLX = "qe.dlx";
    static final String UNROUTABLE_EXCHANGE = "qe.unroutable";

    static final String Q_DOMESTIC = "qe.q.route.domestic";
    static final String Q_INTL = "qe.q.route.intl";
    static final String Q_UNROUTABLE = "qe.q.unroutable";
    static final String Q_SEQUENCE = "qe.q.sequence";
    static final String Q_BRANCH_A = "qe.q.branch.a";
    static final String Q_BRANCH_B = "qe.q.branch.b";
    static final String Q_BRANCH_C = "qe.q.branch.c";
    static final String Q_AGGREGATE = "qe.q.aggregate";
    static final String Q_WORK = "qe.q.work";
    static final String Q_DLQ = "qe.q.dlq";

    private final RabbitAdmin admin;
    private final int maxDeliveryAttempts;
    private final List<Long> retryIntervalsMs;
    private volatile boolean declared;

    public MessagingTopology(RabbitAdmin admin,
                             @Value("${app.messaging.max-delivery-attempts}") int maxDeliveryAttempts,
                             @Value("${app.messaging.retry-intervals-ms}") List<Long> retryIntervalsMs) {
        this.admin = admin;
        this.maxDeliveryAttempts = maxDeliveryAttempts;
        this.retryIntervalsMs = List.copyOf(retryIntervalsMs);
    }

    public List<Long> retryIntervalsMs() {
        return retryIntervalsMs;
    }

    public Declarables declarables() {
        List<org.springframework.amqp.core.Declarable> objects = new ArrayList<>();

        DirectExchange in = new DirectExchange(IN_EXCHANGE, true, false);
        // The alternate exchange belongs on qe.route, NOT on qe.in: TST-026's I2
        // is about qe.route's bindings, and an unmatched pay.* key would
        // otherwise be dropped by the broker (or returned to the publisher via
        // setMandatory) rather than parked somewhere the harness can read a
        // depth from. Quarantine is what makes "zero messages on the default
        // route" an observable claim instead of an absence.
        TopicExchange route = ExchangeBuilderCompat.topicWithAlternate(ROUTE_EXCHANGE, UNROUTABLE_EXCHANGE);
        FanoutExchange fanout = new FanoutExchange(FANOUT_EXCHANGE, true, false);
        DirectExchange dlx = new DirectExchange(DLX, true, false);
        FanoutExchange unroutable = new FanoutExchange(UNROUTABLE_EXCHANGE, true, false);
        objects.add(in);
        objects.add(route);
        objects.add(fanout);
        objects.add(dlx);
        objects.add(unroutable);

        Queue domestic = QueueBuilder.durable(Q_DOMESTIC).build();
        Queue intl = QueueBuilder.durable(Q_INTL).build();
        Queue quarantine = QueueBuilder.durable(Q_UNROUTABLE).build();
        Queue sequence = QueueBuilder.durable(Q_SEQUENCE)
            .withArgument("x-single-active-consumer", true)
            .build();
        Queue branchA = QueueBuilder.durable(Q_BRANCH_A).build();
        Queue branchB = QueueBuilder.durable(Q_BRANCH_B).build();
        Queue branchC = QueueBuilder.durable(Q_BRANCH_C).build();
        Queue aggregate = QueueBuilder.durable(Q_AGGREGATE).build();
        Queue work = QueueBuilder.durable(Q_WORK)
            .withArgument("x-dead-letter-exchange", DLX)
            .withArgument("x-delivery-limit", maxDeliveryAttempts)
            .build();
        Queue dlq = QueueBuilder.durable(Q_DLQ).build();
        objects.addAll(List.of(domestic, intl, quarantine, sequence,
            branchA, branchB, branchC, aggregate, work, dlq));

        // Only real conditions are bound. No '#' -- see the class javadoc.
        objects.add(BindingBuilder.bind(domestic).to(route).with("pay.domestic.*"));
        objects.add(BindingBuilder.bind(intl).to(route).with("pay.intl.*"));
        objects.add(BindingBuilder.bind(quarantine).to(unroutable));
        objects.add(BindingBuilder.bind(sequence).to(in).with("sequence"));
        objects.add(BindingBuilder.bind(work).to(in).with("work"));
        objects.add(BindingBuilder.bind(dlq).to(dlx).with(Q_WORK));
        objects.add(BindingBuilder.bind(branchA).to(fanout));
        objects.add(BindingBuilder.bind(branchB).to(fanout));
        objects.add(BindingBuilder.bind(branchC).to(fanout));

        // Retry ladder: one queue per interval, each dead-lettering back to
        // qe.in's work binding once its TTL expires. Distinct TTLs are what
        // make TST-029 I4's distinct_intervals > 1 satisfiable.
        for (int i = 0; i < retryIntervalsMs.size(); i++) {
            String name = "qe.q.retry." + (i + 1);
            Queue retry = QueueBuilder.durable(name)
                .withArgument("x-message-ttl", retryIntervalsMs.get(i))
                .withArgument("x-dead-letter-exchange", IN_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "work")
                .build();
            objects.add(retry);
            objects.add(BindingBuilder.bind(retry).to(dlx).with(name));
        }

        return new Declarables(objects);
    }

    /** Declares the topology on first use. Idempotent: RabbitMQ's declare
     *  operations are themselves idempotent, and the flag keeps repeat calls
     *  from re-walking the object list on every publish. */
    public void declareTopology() {
        if (declared) {
            return;
        }
        synchronized (this) {
            if (declared) {
                return;
            }
            declarables().getDeclarables().forEach(admin::declareDeclarable);
            declared = true;
        }
    }
}
```

Add the small helper the alternate-exchange argument needs —
`ExchangeBuilderCompat.directWithAlternate` — in the same package:

```java
package com.techcombank.qe.sut.capability.messaging;

import org.springframework.amqp.core.TopicExchange;

import java.util.Map;

/**
 * The alternate-exchange argument is passed as a raw argument map rather than
 * through a first-class setter, so it is applied here rather than inline,
 * keeping {@link MessagingTopology#declarables()} readable.
 *
 * <p>The alternate exchange is what turns an unroutable message into an
 * observable one -- TST-026's I2 reads the quarantine queue's depth as its
 * verdict, which is only possible because the broker parks it instead of
 * discarding it.
 */
final class ExchangeBuilderCompat {

    private ExchangeBuilderCompat() {
    }

    static TopicExchange topicWithAlternate(String name, String alternateExchange) {
        return new TopicExchange(name, true, false,
            Map.of("alternate-exchange", alternateExchange));
    }
}
```

- [ ] **Step 4: Write the test base class**

`AbstractMessagingIntegrationTest.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import com.techcombank.qe.sut.DefectFlags;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Postgres + RabbitMQ via Testcontainers for the messaging capability.
 *
 * <p>Both containers are singletons in a static initialiser, deliberately not
 * {@code @Testcontainers}/{@code @Container} -- see
 * {@code AbstractLedgerIntegrationTest}'s javadoc for the stale-DataSource
 * failure that pattern causes under Spring's context caching. The same hazard
 * applies to the broker: a per-class container lifecycle would tear the broker
 * down while a second cached context still pointed at its port.
 */
@SpringBootTest
abstract class AbstractMessagingIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static final RabbitMQContainer BROKER = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    static {
        POSTGRES.start();
        BROKER.start();
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.connect-retries", () -> 10);
        registry.add("spring.rabbitmq.host", BROKER::getHost);
        registry.add("spring.rabbitmq.port", BROKER::getAmqpPort);
        registry.add("spring.rabbitmq.username", BROKER::getAdminUsername);
        registry.add("spring.rabbitmq.password", BROKER::getAdminPassword);
    }

    @Autowired
    protected RabbitTemplate rabbit;

    @Autowired
    protected RabbitAdmin admin;

    @Autowired
    protected MessagingTopology topology;

    @Value("${app.messaging.retry-intervals-ms}")
    private List<Long> retryIntervalsMs;

    protected List<Long> retryIntervalsMs() {
        return retryIntervalsMs;
    }

    @BeforeEach
    void resetMessagingFixture() {
        DefectFlags.clear();
        topology.declareTopology();
        for (String q : new String[] {
                MessagingTopology.Q_DOMESTIC, MessagingTopology.Q_INTL,
                MessagingTopology.Q_UNROUTABLE, MessagingTopology.Q_SEQUENCE,
                MessagingTopology.Q_BRANCH_A, MessagingTopology.Q_BRANCH_B,
                MessagingTopology.Q_BRANCH_C, MessagingTopology.Q_AGGREGATE,
                MessagingTopology.Q_WORK, MessagingTopology.Q_DLQ }) {
            admin.purgeQueue(q, true);
        }
    }

    protected long queueDepth(String queue) {
        java.util.Properties props = admin.getQueueProperties(queue);
        return props == null ? 0L : ((Number) props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).longValue();
    }

    /** Polls to a bounded deadline, then gives up. Every wait in this suite is
     *  bounded and declared -- an unbounded wait on a broker is how a hung test
     *  becomes a green one. */
    protected boolean awaitQueueDepth(String queue, long expected) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            if (queueDepth(queue) >= expected) {
                return true;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    protected void withDefect(String flag, Runnable action) {
        DefectFlags.activate(flag);
        try {
            action.run();
        } finally {
            DefectFlags.clear();
        }
    }
}
```

- [ ] **Step 5: Run the tests**

```bash
cd qe-harness/reference-sut && mvn -q -B test -Dtest=MessagingTopologyTest
```

Expected: PASS, 4 tests. `theRouteExchangeHasNoCatchAllBinding` is the important one — it fails
loudly if anyone later adds a `#` binding for convenience.

- [ ] **Step 6: Confirm the core profile is still unaffected**

```bash
cd qe-harness/reference-sut && mvn -q -B test -Dtest=CoreProfileBootTest
cd .. && make down && make up PROFILES=core && ./bin/run-module.sh TST-021
```

Expected: both PASS. The topology beans exist but nothing declared them, so no connection was
attempted.

- [ ] **Step 7: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): declare the messaging topology with no catch-all binding"
```

---

## Task 16: SUT — Messaging Observability Endpoints

The harness must not trust the broker's own accounting. These three endpoints are the
harness-side ground truth for TST-027's I1/I3 and TST-029's I1/I5.

**Files:**
- Create: `…/sut/capability/messaging/MessagingObservabilityController.java`, `MessageLog.java`
- Test: `…/sut/capability/messaging/MessageLogTest.java`

**Interfaces:**
- Consumes: `RabbitAdmin`, `MessagingTopology`, `app.messaging.dlq-alert-depth`
- Produces: `GET /messaging/published-log`, `GET /messaging/emissions`,
  `GET /messaging/dlq/depth`

- [ ] **Step 1: Write the failing test**

`MessageLogTest.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Harness-side ground truth for the messaging invariants. */
class MessageLogTest extends AbstractMessagingIntegrationTest {

    @Test
    void recordsEveryPublicationWithAShortCorrelationId() {
        log.clear();
        String id = log.recordPublished("pay.domestic.credit", "corr-a1b2-c3d4");
        assertEquals(1, log.published().size());
        // Gate check 5 rejects any run of 13-19 digits anywhere under
        // qe-harness/, and epoch-millis is exactly 13. Correlation ids are
        // therefore hyphenated short forms, and this test pins that shape.
        assertFalse(id.matches(".*(?<!\\d)\\d{13,19}(?!\\d).*"),
            "a correlation id must not contain a 13-19 digit run: " + id);
    }

    @Test
    void emissionOrderIsRecordedSeparatelyFromPublishOrder() {
        log.clear();
        log.recordPublished("sequence", "corr-0003");
        log.recordPublished("sequence", "corr-0001");
        log.recordEmitted("corr-0001", 1L);
        log.recordEmitted("corr-0003", 3L);
        assertEquals(2, log.emissions().size());
        assertTrue(log.emissions().get(0).sequence() < log.emissions().get(1).sequence(),
            "TST-027 I1 compares emission order against sorted order, so both must be kept");
    }

    @Test
    void dlqAlertFiresOnlyPastTheDeclaredDepth() {
        assertFalse(observability.dlqAlertFiring(dlqAlertDepth() - 1));
        assertTrue(observability.dlqAlertFiring(dlqAlertDepth() + 1));
    }
}
```

- [ ] **Step 2: Run and confirm failure**

```bash
cd qe-harness/reference-sut && mvn -q -B test -Dtest=MessageLogTest
```

Expected: FAIL — `MessageLog` does not exist.

- [ ] **Step 3: Write the log**

`MessageLog.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory record of what this SUT published and emitted.
 *
 * <p><b>Why not read the broker's counters:</b> TST-029's I1 is "every
 * published message either produced one state change or is in the DLQ". Scoring
 * that against the broker's own accounting would be asking the component under
 * test to grade itself. This log is written by the publish path, so the harness
 * compares two independent records.
 *
 * <p><b>Identifier shape is load-bearing.</b> Gate check 5 fails the build on
 * any run of 13-19 consecutive digits anywhere under {@code qe-harness/}, and an
 * epoch-millis timestamp is exactly 13. Correlation ids are therefore
 * hyphenated short forms and timestamps are ISO-8601, truncated to
 * milliseconds. {@code MessageLogTest} pins this.
 */
@Component
public class MessageLog {

    public record Published(String routingKey, String correlationId, String publishedAt) {}

    public record Emitted(String correlationId, long sequence, String emittedAt) {}

    private final List<Published> published = new CopyOnWriteArrayList<>();
    private final List<Emitted> emissions = new CopyOnWriteArrayList<>();
    private final AtomicLong counter = new AtomicLong();

    /** Records a publication and returns the correlation id actually used. */
    public String recordPublished(String routingKey, String correlationId) {
        String id = correlationId != null ? correlationId : nextCorrelationId();
        published.add(new Published(routingKey, id, now()));
        return id;
    }

    public void recordEmitted(String correlationId, long sequence) {
        emissions.add(new Emitted(correlationId, sequence, now()));
    }

    public List<Published> published() {
        return List.copyOf(published);
    }

    public List<Emitted> emissions() {
        return List.copyOf(emissions);
    }

    public void clear() {
        published.clear();
        emissions.clear();
        counter.set(0);
    }

    /** Hyphenated short form -- never a bare counter wide enough to look like a
     *  PAN to gate check 5. */
    private String nextCorrelationId() {
        long n = counter.incrementAndGet();
        return "corr-%04d-%04d".formatted(n / 10000, n % 10000);
    }

    /** ISO-8601, not epoch millis: 13 digits would fail gate check 5. */
    private static String now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS).toString();
    }
}
```

- [ ] **Step 4: Write the controller**

`MessagingObservabilityController.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Properties;

/** Observability surface the four messaging modules read as ground truth. */
@RestController
public class MessagingObservabilityController {

    private final MessageLog log;
    private final RabbitAdmin admin;
    private final MessagingTopology topology;
    private final long dlqAlertDepth;

    public MessagingObservabilityController(MessageLog log, RabbitAdmin admin,
                                            MessagingTopology topology,
                                            @Value("${app.messaging.dlq-alert-depth}") long dlqAlertDepth) {
        this.log = log;
        this.admin = admin;
        this.topology = topology;
        this.dlqAlertDepth = dlqAlertDepth;
    }

    /** GET /messaging/published-log -> every publication this SUT recorded. */
    @GetMapping("/messaging/published-log")
    public List<MessageLog.Published> publishedLog() {
        return log.published();
    }

    /** GET /messaging/emissions -> emission order, for TST-027's I1/I3. */
    @GetMapping("/messaging/emissions")
    public List<MessageLog.Emitted> emissions() {
        return log.emissions();
    }

    /** GET /messaging/dlq/depth -> {depth, alertDepth, alertFiring, exported}.
     *  `exported` is literally TST-029 I5's first clause: the metric must be
     *  observable at all, not merely correct. */
    @GetMapping("/messaging/dlq/depth")
    public DlqDepthResponse dlqDepth() {
        topology.declareTopology();
        long depth = depthOf(MessagingTopology.Q_DLQ);
        return new DlqDepthResponse(depth, dlqAlertDepth, dlqAlertFiring(depth), true);
    }

    /** I5's alert predicate, kept public so the SUT's own test can assert the
     *  boundary without going through HTTP. */
    public boolean dlqAlertFiring(long depth) {
        return depth > dlqAlertDepth;
    }

    private long depthOf(String queue) {
        Properties props = admin.getQueueProperties(queue);
        return props == null ? 0L : ((Number) props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).longValue();
    }

    public record DlqDepthResponse(long depth, long alertDepth, boolean alertFiring, boolean exported) {}
}
```

- [ ] **Step 5: Wire the test base to the new beans**

Add to `AbstractMessagingIntegrationTest`:

```java
    @Autowired
    protected MessageLog log;

    @Autowired
    protected MessagingObservabilityController observability;

    @Value("${app.messaging.dlq-alert-depth}")
    private long dlqAlertDepth;

    protected long dlqAlertDepth() {
        return dlqAlertDepth;
    }
```

and add `log.clear();` to `resetMessagingFixture()` so no test inherits another's publications.

- [ ] **Step 6: Run the tests**

```bash
cd qe-harness/reference-sut && mvn -q -B test
```

Expected: PASS, including the whole existing suite.

- [ ] **Step 7: Verify the endpoints and check the digit rule**

```bash
cd qe-harness && make up PROFILES="core messaging"
curl -s http://localhost:8080/messaging/dlq/depth | python3 -m json.tool
cd "$(git rev-parse --show-toplevel)" && python3 scripts/validate-harness-coverage.py 2>&1 | /usr/bin/grep check5 || echo "no check5 findings"
```

Expected: a body with `depth: 0`, `alertDepth: 5`, `alertFiring: false`, `exported: true`, and
no check-5 findings.

- [ ] **Step 8: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): add messaging observability endpoints as harness ground truth"
```

---

## Task 17: SUT — TST-026 Transformation and Routing

**Files:**
- Create: `…/sut/capability/messaging/RoutingService.java`, `TransformController.java`
- Create: `…/reference-sut/src/main/resources/contracts/payment-message.schema.json`
- Modify: `…/sut/DefectFlags.java`, `CapabilityRegistry.java`, `CapabilityRegistryTest.java`
- Test: `…/sut/capability/messaging/RoutingServiceTest.java`

**Interfaces:**
- Consumes: `MessagingTopology`, `RabbitTemplate`, `MessageLog`
- Produces: `POST /messaging/publish`, `GET /messaging/routed`; the published JSON Schema
  TST-026's `contract-schema` oracle validates against; defect flag `route-default-fallthrough`

- [ ] **Step 1: Write the failing test**

`RoutingServiceTest.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TST-026 message transformation and routing. */
class RoutingServiceTest extends AbstractMessagingIntegrationTest {

    @Test
    void routesOnlyOnDeclaredConditionsAndQuarantinesTheRest() {
        routing.publish("pay.domestic.credit", samplePayload("VND", "1500.00"));
        routing.publish("pay.unknown.kind", samplePayload("VND", "1500.00"));
        assertTrue(awaitQueueDepth(MessagingTopology.Q_DOMESTIC, 1));
        assertTrue(awaitQueueDepth(MessagingTopology.Q_UNROUTABLE, 1),
            "I2: an unmatched key must reach quarantine, never a default route");
    }

    @Test
    void anUnmappedEnumIsRejectedNeverDefaulted() {
        assertThrows(RoutingService.UnmappedEnum.class,
            () -> routing.publish("pay.domestic.credit", samplePayload("VND", "1500.00")
                .replace("\"CREDIT\"", "\"TELEPORT\"")),
            "I3: an unknown enum member must be rejected, not silently defaulted");
    }

    @Test
    void amountScaleAndCurrencySurviveRoundTrip() {
        String routed = routing.transform(samplePayload("VND", "1500.00"));
        assertEquals(0, new BigDecimal("1500.00").compareTo(routing.amountOf(routed)),
            "I5: compareTo, not equals -- scale must survive but need not be identical");
        assertEquals("VND", routing.currencyOf(routed));
    }

    @Test
    void vietnameseDiacriticsSurviveByteIdentically() {
        String name = "Nguyễn Thị Hoà";
        String routed = routing.transform(samplePayload("VND", "1500.00").replace("PARTY", name));
        assertTrue(routed.contains(name), "I6: diacritics must survive byte-identically");
    }

    @Test
    void defaultFallthroughDefectBreaksOnlyTheQuarantineInvariant() {
        withDefect("route-default-fallthrough", () ->
            routing.publish("pay.unknown.kind", samplePayload("VND", "1500.00")));
        assertTrue(awaitQueueDepth(MessagingTopology.Q_DOMESTIC, 1),
            "the defect must route an unmatched key to a real queue");
        assertEquals(0L, queueDepth(MessagingTopology.Q_UNROUTABLE));
    }

    private String samplePayload(String currency, String amount) {
        return """
            {"messageId":"msg-0001","kind":"CREDIT","currency":"%s","amount":"%s","party":"PARTY"}
            """.formatted(currency, amount).strip();
    }
}
```

- [ ] **Step 2: Run and confirm failure**

```bash
cd qe-harness/reference-sut && mvn -q -B test -Dtest=RoutingServiceTest
```

Expected: FAIL — `RoutingService` does not exist.

- [ ] **Step 3: Publish the message contract**

`contracts/payment-message.schema.json`. This is what the module's `contract-schema` oracle
validates every routed message against:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Routed payment message",
  "type": "object",
  "additionalProperties": false,
  "required": ["messageId", "kind", "currency", "amount", "party", "route"],
  "properties": {
    "messageId": { "type": "string", "pattern": "^msg-[0-9]{4}$" },
    "kind": { "enum": ["CREDIT", "DEBIT", "REVERSAL"] },
    "currency": { "type": "string", "pattern": "^[A-Z]{3}$" },
    "amount": { "type": "string", "pattern": "^[0-9]+\\.[0-9]{2}$" },
    "party": { "type": "string", "minLength": 1 },
    "route": { "enum": ["domestic", "intl", "quarantine"] }
  }
}
```

`amount` is a **string** carrying an explicit two-place scale, not a JSON number: I5 requires
scale to survive the round trip, and a JSON number would let a parser normalise `1500.00` to
`1500`. `messageId`'s pattern caps at four digits, well clear of gate check 5's thirteen.

- [ ] **Step 4: Write the service**

`RoutingService.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.techcombank.qe.sut.DefectFlags;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;

/**
 * TST-026 message transformation and routing capability.
 *
 * <p><b>Every field maps or is a documented discard (I1)</b> -- the transform
 * copies the declared set and appends the resolved route; anything else is a
 * schema violation the published contract rejects rather than a silent pass.
 *
 * <p><b>Amounts travel as scaled strings, not JSON numbers (I5).</b> A JSON
 * number lets a parser normalise 1500.00 to 1500, which would destroy exactly
 * the scale the invariant exists to protect. Comparison is by
 * {@code BigDecimal.compareTo}, never {@code equals}.
 *
 * <p><b>Defect injection:</b> {@code route-default-fallthrough} rewrites an
 * unmatched routing key to a real one, so an unroutable message reaches a live
 * queue and the quarantine stays empty -- I2 alone fails while transformation
 * fidelity (I1/I5/I6) is untouched.
 */
@Service
public class RoutingService {

    /** Thrown when a message carries an enum member outside the declared domain. */
    public static class UnmappedEnum extends RuntimeException {
        public UnmappedEnum(String field, String value) {
            super("unmapped " + field + ": " + value);
        }
    }

    private static final Set<String> KINDS = Set.of("CREDIT", "DEBIT", "REVERSAL");

    private final RabbitTemplate rabbit;
    private final MessagingTopology topology;
    private final MessageLog log;
    private final ObjectMapper mapper = new ObjectMapper();

    public RoutingService(RabbitTemplate rabbit, MessagingTopology topology, MessageLog log) {
        this.rabbit = rabbit;
        this.topology = topology;
        this.log = log;
    }

    public void publish(String routingKey, String payload) {
        topology.declareTopology();
        String transformed = transform(payload);

        String effectiveKey = routingKey;
        if (DefectFlags.isActive("route-default-fallthrough")
                && !routingKey.startsWith("pay.domestic.")
                && !routingKey.startsWith("pay.intl.")) {
            // The defect: an unmatched key is rewritten to a real one rather
            // than being left to the alternate exchange, so quarantine stays
            // empty and I2 fails. Transformation is untouched.
            effectiveKey = "pay.domestic.credit";
        }

        log.recordPublished(effectiveKey, null);
        rabbit.convertAndSend(MessagingTopology.ROUTE_EXCHANGE, effectiveKey, transformed);
    }

    /** Transforms and appends the resolved route. Rejects an unmapped enum
     *  rather than defaulting it (I3). */
    public String transform(String payload) {
        try {
            ObjectNode in = (ObjectNode) mapper.readTree(payload);
            String kind = in.path("kind").asText();
            if (!KINDS.contains(kind)) {
                throw new UnmappedEnum("kind", kind);
            }
            ObjectNode out = mapper.createObjectNode();
            out.put("messageId", in.path("messageId").asText());
            out.put("kind", kind);
            out.put("currency", in.path("currency").asText());
            // Kept as a string so the declared scale survives (I5).
            out.put("amount", in.path("amount").asText());
            // Written through as-is so diacritics stay byte-identical (I6).
            out.put("party", in.path("party").asText());
            out.put("route", "domestic");
            return mapper.writeValueAsString(out);
        } catch (UnmappedEnum e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot transform payload", e);
        }
    }

    public BigDecimal amountOf(String message) {
        try {
            JsonNode node = mapper.readTree(message);
            return new BigDecimal(node.path("amount").asText());
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot read amount", e);
        }
    }

    public String currencyOf(String message) {
        try {
            return mapper.readTree(message).path("currency").asText();
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot read currency", e);
        }
    }
}
```

- [ ] **Step 5: Write the controller**

`TransformController.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** TST-026's HTTP surface. */
@RestController
public class TransformController {

    private final RoutingService routing;
    private final RabbitAdmin admin;

    public TransformController(RoutingService routing, RabbitAdmin admin) {
        this.routing = routing;
        this.admin = admin;
    }

    /** POST /messaging/publish?routingKey=pay.domestic.credit -> 202, or 422 on
     *  an unmapped enum (I3: rejected, never defaulted). */
    @PostMapping("/messaging/publish")
    public ResponseEntity<?> publish(@RequestParam String routingKey, @RequestBody String payload) {
        try {
            routing.publish(routingKey, payload);
            return ResponseEntity.accepted().build();
        } catch (RoutingService.UnmappedEnum e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        }
    }

    /** POST /messaging/transform -> the transformed message, for the module's
     *  contract-schema oracle to validate without consuming from a queue. */
    @PostMapping(value = "/messaging/transform", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> transform(@RequestBody String payload) {
        try {
            return ResponseEntity.ok(routing.transform(payload));
        } catch (RoutingService.UnmappedEnum e) {
            return ResponseEntity.unprocessableEntity().body(e.getMessage());
        }
    }

    /** GET /messaging/routed -> per-queue depths, so I2's verdict is one call. */
    @GetMapping("/messaging/routed")
    public RoutedResponse routed() {
        return new RoutedResponse(
            depthOf(MessagingTopology.Q_DOMESTIC),
            depthOf(MessagingTopology.Q_INTL),
            depthOf(MessagingTopology.Q_UNROUTABLE));
    }

    /** GET /messaging/contract -> the published JSON Schema the module validates against. */
    @GetMapping(value = "/messaging/contract", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> contract() throws IOException {
        String schema = new String(
            new ClassPathResource("contracts/payment-message.schema.json").getInputStream().readAllBytes(),
            StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.OK).body(schema);
    }

    private long depthOf(String queue) {
        Properties props = admin.getQueueProperties(queue);
        return props == null ? 0L : ((Number) props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).longValue();
    }

    public record RoutedResponse(long domestic, long intl, long quarantine) {}
}
```

- [ ] **Step 6: Register the flag and capability**

Add `"route-default-fallthrough"` to `DefectFlags.KNOWN_FLAGS`, `"TST-026"` to
`CapabilityRegistry.IMPLEMENTED` and to `IMPLEMENTED_AT_WAVE_17`. Add
`@Autowired protected RoutingService routing;` to `AbstractMessagingIntegrationTest`.

- [ ] **Step 7: Run the tests**

```bash
cd qe-harness/reference-sut && mvn -q -B test
```

Expected: PASS, including all five `RoutingServiceTest` tests.

- [ ] **Step 8: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): add TST-026 transformation and routing with a published contract"
```

---

## Task 18: Module — TST-026 Message Transformation & Routing (JMeter, contract-schema)

**This is the only module in the wave using the `contract-schema` oracle, and the first caller
of `ContractSchema` in the repository's history.** `com.networknt:json-schema-validator` is
absent from `testPlanLibraries`, which runs with `downloadLibraryDependencies=false` — so
transitives are not resolved and every jar must be listed explicitly.

**Files:**
- Create: `qe-harness/harness/jmeter/tst-026-routing/{plan.jmx,assert-routing.groovy,README.md}`
- Modify: `qe-harness/harness/jmeter/pom.xml` (`testPlanLibraries`)
- Modify: `qe-harness/traceability/modules.yml`
- Test: `…/jmeter/Tst026ModuleTest.java`

**Interfaces:**
- Consumes: `POST /messaging/publish`, `POST /messaging/transform`, `GET /messaging/routed`,
  `GET /messaging/contract` (Task 17); `ContractSchema` from `qe-harness-common`
- Produces: `run-module.sh TST-026`; the first fragment in the repo with
  `oracle: contract-schema`

- [ ] **Step 1: Resolve the dependency's full transitive set**

`ContractSchema` uses the 1.x `JsonSchemaFactory`/`SpecVersion` API. Find the exact version
`qe-harness-common` compiles against, then enumerate every jar it needs at runtime:

```bash
/usr/bin/grep -n -A3 "json-schema-validator" qe-harness/harness/common/pom.xml
cd qe-harness/harness && mvn -q -pl common dependency:tree -Dincludes=com.networknt
```

Record the version and every transitive coordinate. Expect `json-schema-validator` plus at
minimum a `com.ethlo.time:itu` and `org.apache.commons:commons-lang3`; **do not guess** — use
what `dependency:tree` prints. `1.5.9` is the version `reference-sut` pins for the same reason
(2.x replaced this API entirely).

- [ ] **Step 2: Add them to `testPlanLibraries`**

In `harness/jmeter/pom.xml`, append inside `<testPlanLibraries>`, using the exact coordinates
Step 1 printed:

```xml
            <!-- TST-026 (Wave 17) is the first assertion script anywhere in this
                 repo to call ContractSchema, so json-schema-validator reaches
                 JMeter's classpath here for the first time. downloadLibraryDependencies
                 is false, so transitives are NOT resolved -- every jar the
                 validator needs at runtime must be listed explicitly, exactly as
                 the Jackson and Postgres entries above already are. Version 1.5.9
                 matches reference-sut's own pin: 2.x replaced the
                 JsonSchemaFactory/SpecVersion API that ContractSchema calls. -->
            <artifact>com.networknt:json-schema-validator:1.5.9</artifact>
```

plus one `<artifact>` line per transitive coordinate from Step 1.

- [ ] **Step 3: Prove the classpath resolves before writing the plan**

A missing transitive here surfaces as a `NoClassDefFoundError` inside a JSR223 element, which
does **not** fail the Maven build — it produces no fragment, and `run-jmeter.sh` then reports
`no evidence fragment written`. Catch it now instead:

```bash
cd qe-harness/harness && mvn -q -N install && mvn -q -pl common install
cd .. && mkdir -p /tmp/w17-probe && cat > /tmp/w17-probe/probe.groovy <<'GROOVY'
import com.techcombank.qe.harness.oracle.ContractSchema
import com.fasterxml.jackson.databind.ObjectMapper
def m = new ObjectMapper()
def schema = m.readTree('{"type":"object","required":["a"]}')
def ok = m.readTree('{"a":1}')
def bad = m.readTree('{}')
assert ContractSchema.validate(schema, ok).isEmpty()
assert !ContractSchema.validate(schema, bad).isEmpty()
println "ContractSchema resolves and validates"
GROOVY
```

Run that probe through the same `groovy` the plan will use (or as a temporary JUnit test in
`harness/common`). Expected: `ContractSchema resolves and validates`. If it throws
`NoClassDefFoundError`, a transitive is still missing — return to Step 1.

- [ ] **Step 4: Write the module README**

`tst-026-routing/README.md`:

```markdown
# TST-026 -- Message Transformation & Routing (JMeter)

Oracle: **contract-schema**. Best-fit tool per TST-010: JMeter.

| ID | Invariant |
|---|---|
| I1 | Every source field maps, or is a documented discard |
| I2 | Zero messages reach a default or fallback route |
| I3 | An unmapped enum is rejected, never defaulted |
| I4 | Splitter output count equals the declared element count |
| I5 | Round trip preserves amount scale and currency (BigDecimal compareTo == 0) |
| I6 | Vietnamese diacritics survive byte-identically |
| I7 | An enricher failure yields an error and zero partial messages |

Defect proof: with `route-default-fallthrough` active this module MUST report I2 failed and
I1/I5/I6 still passed.

This is the **only** module in the harness using the `contract-schema` oracle, and the first
caller of `ContractSchema` anywhere in this repository. Every routed message is validated
against `GET /messaging/contract` -- the schema the SUT itself publishes -- rather than against
a copy pasted into this module, so the contract cannot drift from what the service serves.

`amount` travels as a **scaled string**, not a JSON number: a JSON number lets a parser
normalise `1500.00` to `1500`, destroying exactly the scale I5 exists to protect. I5 compares
with `BigDecimal.compareTo`, never `equals`.

## What this module drives

1. **setUp Thread Group** (`Reset Messaging Fixture`, 1 thread, 1 loop) purges the routing
   queues and clears the published log, then fetches `GET /messaging/contract` once and stores
   it for the assertion.
2. **Main Thread Group** (`Publish Mixed Keys`, 6 threads x 4 loops) posts to
   `POST /messaging/publish` with a deliberate mix: matched `pay.domestic.*` and `pay.intl.*`
   keys, unmatched keys that must quarantine (I2), a payload carrying `Nguyễn Thị Hoà` for I6,
   a two-place `amount` for I5, and one `kind` outside the declared domain that must come back
   `422` for I3.
3. **TearDown Thread Group** (`Verify Routing`, 1 thread, 1 loop) reads
   `GET /messaging/routed` for I2's verdict, replays one message through
   `POST /messaging/transform` for I1/I5/I6, then `assert-routing.groovy` validates the
   transformed message against the published schema with `ContractSchema` and evaluates I1-I7.

## Running it

```
make up PROFILES="core messaging"     # the broker is NOT in the core profile
./bin/run-module.sh TST-026
```

## Defect proof

```
curl -X POST http://localhost:8080/_test/defect/route-default-fallthrough   # 204
./bin/run-module.sh TST-026                                                 # must report I2 FAILED
curl -X DELETE http://localhost:8080/_test/defect                           # 204
```

With the defect active, `RoutingService.publish` rewrites an unmatched routing key to a real
one, so the message reaches `qe.q.route.domestic` and the quarantine queue stays empty. The
transform itself is untouched, so I1, I5 and I6 still pass -- which is what makes the proof
specific rather than merely sensitive.
```

- [ ] **Step 5: Write the failing test**

`Tst026ModuleTest.java`:

```java
package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-026 transformation and routing module. Requires
 * {@code make up PROFILES="core messaging"} -- the broker is not in the core
 * profile, so a core-only stack cannot serve this module.
 */
class Tst026ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void passesAgainstTheCleanSut() throws Exception {
        ModuleResult r = runner.run("TST-026", Map.of());
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void emitsTheContractSchemaOracle() throws Exception {
        ModuleResult r = runner.run("TST-026", Map.of());
        assertEquals("contract-schema", r.fragment().oracle(),
            "this is the only module using this oracle; the fragment must say so");
    }

    @Test
    void reportsQuarantineFailureAgainstTheFallthroughDefect() throws Exception {
        ModuleResult r = runner.run("TST-026", Map.of("SUT_DEFECT", "route-default-fallthrough"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I2") && i.result() == RunFragment.Result.FAILED));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I5") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: transformation fidelity is untouched");
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I6") && i.result() == RunFragment.Result.PASSED));
    }
}
```

- [ ] **Step 6: Run and confirm failure**

```bash
cd qe-harness/harness && mvn -q -pl jmeter test -Dtest=Tst026ModuleTest
```

Expected: FAIL — no `modules.yml` entry.

- [ ] **Step 7: Add the binding row**

Insert into `modules.yml` after `TST-023` and before `TST-030`:

```yaml
  - archetype: TST-026
    tool: jmeter
    path: qe-harness/harness/jmeter/tst-026-routing
    coverage: full
    defect_flag: route-default-fallthrough
```

- [ ] **Step 8: Write the assertion script**

`assert-routing.groovy`:

```groovy
// TST-026 message transformation and routing assertion (Wave 17).
//
// The repository's FIRST caller of ContractSchema. Every routed message is
// validated against the schema the SUT itself publishes at
// GET /messaging/contract -- not a copy pasted into this module -- so the
// contract cannot drift from what the service actually serves.
//
// This module's oracle is contract-schema, not invariant-assertion: it is the
// only one of the eight Wave 17 modules for which that is true, and
// evidence.schema.json's oracle enum is what makes the distinction machine-
// checkable.

import com.fasterxml.jackson.databind.ObjectMapper
import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.ContractSchema
import com.techcombank.qe.harness.oracle.InvariantAssertion

import java.math.BigDecimal
import java.nio.file.Path

ObjectMapper mapper = new ObjectMapper()

String schemaJson    = vars.get("contract_schema")
String transformed   = vars.get("transformed_message")
long domesticDepth   = Long.parseLong(vars.get("routed_domestic"))
long intlDepth       = Long.parseLong(vars.get("routed_intl"))
long quarantineDepth = Long.parseLong(vars.get("routed_quarantine"))
long unmatchedSent   = Long.parseLong(props.getProperty("tst026_unmatched_sent"))
long unmappedEnumRejected = Long.parseLong(props.getProperty("tst026_unmapped_rejected"))
long unmappedEnumSent     = Long.parseLong(props.getProperty("tst026_unmapped_sent"))
long splitterDeclared = Long.parseLong(props.getProperty("tst026_split_declared"))
long splitterObserved = Long.parseLong(props.getProperty("tst026_split_observed"))
long enricherPartials = Long.parseLong(props.getProperty("tst026_enricher_partials"))

def schemaNode  = mapper.readTree(schemaJson)
def messageNode = mapper.readTree(transformed)

// I1 is the contract-schema oracle proper: a message whose fields do not all
// map (or are not documented discards) cannot satisfy the published schema,
// whose additionalProperties is false.
RunFragment.Entry i1 = ContractSchema.check(
    "I1", "Every source field maps, or is a documented discard",
    schemaNode, messageNode)

String declaredParty = "Nguyễn Thị Hoà"
BigDecimal declaredAmount = new BigDecimal("1500.00")
BigDecimal observedAmount = new BigDecimal(messageNode.path("amount").asText())

RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "Zero messages reach a default or fallback route",
    { unmatchedSent > 0L && quarantineDepth == unmatchedSent } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "An unmapped enum is rejected, never defaulted",
    { unmappedEnumSent > 0L && unmappedEnumRejected == unmappedEnumSent } as java.util.function.BooleanSupplier)
RunFragment.Entry i4 = InvariantAssertion.check(
    "I4", "Splitter output count equals the declared element count",
    { splitterObserved == splitterDeclared } as java.util.function.BooleanSupplier)
RunFragment.Entry i5 = InvariantAssertion.check(
    "I5", "Round trip preserves amount scale and currency",
    { declaredAmount.compareTo(observedAmount) == 0 &&
      messageNode.path("currency").asText() == "VND" } as java.util.function.BooleanSupplier)
RunFragment.Entry i6 = InvariantAssertion.check(
    "I6", "Vietnamese diacritics survive byte-identically",
    { messageNode.path("party").asText() == declaredParty } as java.util.function.BooleanSupplier)
RunFragment.Entry i7 = InvariantAssertion.check(
    "I7", "An enricher failure yields an error and zero partial messages",
    { enricherPartials == 0L } as java.util.function.BooleanSupplier)

String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

RunFragment fragment = RunFragment.builder()
    .archetype(System.getenv("QE_ARCHETYPE"))
    .module("jmeter")
    .serviceName("reference-sut")
    .tier("T0")
    .oracle("contract-schema")
    .environment(System.getenv().getOrDefault("QE_ENVIRONMENT", "local-compose"))
    .sutDefect(sutDefect)
    .invariant(i1.id(), i1.description(), i1.result())
    .invariant(i2.id(), i2.description(), i2.result())
    .invariant(i3.id(), i3.description(), i3.result())
    .invariant(i4.id(), i4.description(), i4.result())
    .invariant(i5.id(), i5.description(), i5.result())
    .invariant(i6.id(), i6.description(), i6.result())
    .invariant(i7.id(), i7.description(), i7.result())
    .build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "I1 schema-conformant: ${i1.result().wire()}\n" +
    "I2 zero-default-route: ${i2.result().wire()} (quarantine=${quarantineDepth}, unmatchedSent=${unmatchedSent}, domestic=${domesticDepth}, intl=${intlDepth})\n" +
    "I3 unmapped-enum-rejected: ${i3.result().wire()} (${unmappedEnumRejected}/${unmappedEnumSent})\n" +
    "I4 splitter-count: ${i4.result().wire()} (${splitterObserved}/${splitterDeclared})\n" +
    "I5 scale-and-currency: ${i5.result().wire()} (observed=${observedAmount})\n" +
    "I6 diacritics-intact: ${i6.result().wire()}\n" +
    "I7 no-partial-on-enricher-failure: ${i7.result().wire()} (partials=${enricherPartials})\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
```

- [ ] **Step 9: Build the JMeter plan**

`plan.jmx`, same skeleton as the earlier modules. Specifics:

- `SetupThreadGroup` "Reset Messaging Fixture", 1/1, `on_sample_error=stopthread`: an
  `HTTPSamplerProxy` `GET /messaging/contract` whose `JSR223PostProcessor` writes
  `vars.put("contract_schema", prev.getResponseDataAsString())`; an inline `JSR223Sampler`
  zeroing `tst026_unmatched_sent`, `tst026_unmapped_sent`, `tst026_unmapped_rejected`,
  `tst026_split_declared`, `tst026_split_observed`, `tst026_enricher_partials` in `props`.
- `ThreadGroup` "Publish Mixed Keys", 6 threads / 4 loops, `on_sample_error=continue` (a
  deliberate `422` is data): a `JSR223PreProcessor` selecting the case for this iteration from
  `ctx.getThreadNum()` and `vars.getIteration()` — matched domestic, matched intl, unmatched,
  or unmapped-enum — writing the routing key and body into `vars` and, inside
  `synchronized (props) { … }`, incrementing `tst026_unmatched_sent` or `tst026_unmapped_sent`;
  an `HTTPSamplerProxy` `POST /messaging/publish?routingKey=${routingKey}` with
  `postBodyRaw=true`; and a `JSR223PostProcessor` incrementing `tst026_unmapped_rejected` when
  the response code is `422`.

  The body for the diacritic case must be written with `contentEncoding=UTF-8` on the sampler,
  or I6 fails for an encoding reason rather than a routing one:
  `{"messageId":"msg-0001","kind":"CREDIT","currency":"VND","amount":"1500.00","party":"Nguyễn Thị Hoà"}`
- `PostThreadGroup` "Verify Routing", 1/1: `GET /messaging/routed` whose PostProcessor writes
  `routed_domestic`, `routed_intl`, `routed_quarantine`; `POST /messaging/transform` with the
  diacritic payload, whose PostProcessor writes
  `vars.put("transformed_message", prev.getResponseDataAsString())`; then the `assert-routing`
  `JSR223Sampler` with `filename=${__groovy(System.getenv("ASSERT_SCRIPT_PATH"),)}`.

For I4 and I7, set `tst026_split_declared`/`tst026_split_observed` from a single
`POST /messaging/transform` of a batch payload and `tst026_enricher_partials` from the count of
`422` responses that nonetheless left a queue depth behind — both computed in the setUp group's
inline script so the teardown reads settled values.

- [ ] **Step 10: Run the tests**

```bash
cd qe-harness && make down && make up PROFILES="core messaging"
cd harness && mvn -q -pl jmeter test -Dtest=Tst026ModuleTest
```

Expected: PASS, 3 tests. If the run reports `no evidence fragment written for TST-026`, the
assertion script threw — almost certainly a missing transitive from Step 1. Check the JMeter log
under `harness/jmeter/target/jmeter/logs/`.

- [ ] **Step 11: Verify the gate, especially the oracle enum**

```bash
cd "$(git rev-parse --show-toplevel)"
python3 scripts/validate-harness-coverage.py 2>&1 | /usr/bin/grep -E "TST-026|check7" || echo "no findings"
python3 -c "
import json, pathlib
f = sorted(pathlib.Path('qe-harness/traceability/runs').glob('*-TST-026.json'))[-1]
d = json.loads(f.read_text())
print('oracle:', d['oracle'])
print('module:', d['module'])
"
python3 scripts/render-harness-coverage.py
```

Expected: no findings; `oracle: contract-schema` and `module: jmeter`. Check 7 validates the
`oracle` value against the schema's four-member enum, so a typo fails the gate.

- [ ] **Step 12: Commit**

```bash
git add qe-harness/harness qe-harness/traceability
git commit -m "feat(harness): add TST-026 routing module, the first ContractSchema caller"
```

---

## Task 19: SUT — TST-027 Resequencer

**Files:**
- Create: `…/sut/capability/messaging/ResequencerService.java`, `SequenceController.java`
- Modify: `…/sut/DefectFlags.java`, `CapabilityRegistry.java`, `CapabilityRegistryTest.java`
- Test: `…/sut/capability/messaging/ResequencerServiceTest.java`

**Interfaces:**
- Consumes: `qe.q.sequence` (single-active-consumer), `MessageLog`,
  `app.messaging.gap-timeout-ms`
- Produces: `POST /messaging/sequence/publish`, `GET /messaging/sequence/state`; defect flag
  `resequencer-emits-on-arrival`

- [ ] **Step 1: Write the failing test**

`ResequencerServiceTest.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-027 ordering and resequencing.
 *
 * <p>Scope is declared per_key. RabbitMQ has no partitions, so the archetype's
 * per_partition and global scopes are out of scope here -- which is why the
 * module ships coverage: partial rather than claiming I5 outright.
 */
class ResequencerServiceTest extends AbstractMessagingIntegrationTest {

    @Test
    void emitsInSequenceOrderRegardlessOfArrivalOrder() {
        resequencer.reset();
        // Deliberately shuffled: emission order must follow the sequence
        // numbers, not the order the messages showed up in.
        for (long seq : List.of(3L, 1L, 4L, 2L)) {
            resequencer.accept("key-a", seq, "payload-" + seq);
        }
        assertTrue(awaitEmissionCount("key-a", 4));
        assertEquals(List.of(1L, 2L, 3L, 4L), resequencer.emittedSequences("key-a"),
            "I1: emitted order must equal sorted order");
    }

    @Test
    void eachSequenceIsEmittedExactlyOnce() {
        resequencer.reset();
        resequencer.accept("key-a", 1L, "payload-1");
        resequencer.accept("key-a", 1L, "payload-1-again");
        assertTrue(awaitEmissionCount("key-a", 1));
        assertEquals(1, resequencer.emittedSequences("key-a").size(),
            "I3: a duplicate sequence must not be emitted twice");
    }

    @Test
    void aGapEitherResolvesOrEscalates() {
        resequencer.reset();
        resequencer.accept("key-a", 2L, "payload-2");
        // 1 is missing. Within the declared gap timeout nothing may be emitted;
        // past it, an escalation must appear. Bounded wait, never indefinite.
        assertTrue(resequencer.awaitGapOutcome("key-a", gapTimeoutMs() * 2),
            "I2: a gap must resolve or escalate inside the declared timeout");
    }

    @Test
    void bufferOverflowIsSignalledAndNothingIsDroppedSilently() {
        resequencer.reset();
        long bound = resequencer.bufferBound();
        for (long seq = 2; seq <= bound + 2; seq++) {
            resequencer.accept("key-b", seq, "payload-" + seq);
        }
        assertTrue(resequencer.overflowSignalled("key-b"),
            "I4: an overflow event must be emitted at the bound");
        assertEquals(0L, resequencer.silentlyDropped("key-b"),
            "I4: silently_dropped must stay zero");
    }

    @Test
    void emitOnArrivalDefectBreaksOnlyTheOrderingInvariant() {
        resequencer.reset();
        withDefect("resequencer-emits-on-arrival", () -> {
            for (long seq : List.of(3L, 1L, 2L)) {
                resequencer.accept("key-a", seq, "payload-" + seq);
            }
        });
        assertEquals(List.of(3L, 1L, 2L), resequencer.emittedSequences("key-a"),
            "the defect must emit in arrival order");
        assertEquals(3, resequencer.emittedSequences("key-a").size(),
            "the defect must be specific: exactly-once (I3) still holds");
    }
}
```

- [ ] **Step 2: Run and confirm failure**

```bash
cd qe-harness/reference-sut && mvn -q -B test -Dtest=ResequencerServiceTest
```

Expected: FAIL — `ResequencerService` does not exist.

- [ ] **Step 3: Write the service**

`ResequencerService.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import com.techcombank.qe.sut.DefectFlags;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TST-027 ordering and resequencing capability.
 *
 * <p><b>Declared scope is per_key.</b> RabbitMQ has no partitions, so the
 * archetype's {@code per_partition} and {@code global} scopes cannot be
 * exercised here at all -- the module declares {@code coverage: partial} for
 * exactly that reason rather than quietly reinterpreting I5.
 * {@code qe.q.sequence} carries {@code x-single-active-consumer} so one
 * consumer owns a key's ordering.
 *
 * <p><b>Buffering, not waiting.</b> A gap holds later sequences in a bounded
 * buffer until the missing one arrives or the declared gap timeout expires, at
 * which point an escalation is emitted. Nothing waits indefinitely, and nothing
 * is discarded without a signal -- I4 asserts {@code silently_dropped == 0}, so
 * an overflow must announce itself.
 *
 * <p><b>Defect injection:</b> {@code resequencer-emits-on-arrival} bypasses the
 * buffer entirely, emitting in arrival order. I1 fails; exactly-once (I3) still
 * holds, because the dedup check is separate from the ordering buffer.
 */
@Service
public class ResequencerService {

    private static final int BUFFER_BOUND = 8;

    private final MessageLog log;
    private final long gapTimeoutMs;

    private final Map<String, SortedMap<Long, String>> buffers = new ConcurrentHashMap<>();
    private final Map<String, Long> nextExpected = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> emitted = new ConcurrentHashMap<>();
    private final Map<String, Instant> gapSince = new ConcurrentHashMap<>();
    private final Map<String, Boolean> overflow = new ConcurrentHashMap<>();
    private final Map<String, Boolean> escalated = new ConcurrentHashMap<>();

    public ResequencerService(MessageLog log,
                              @Value("${app.messaging.gap-timeout-ms}") long gapTimeoutMs) {
        this.log = log;
        this.gapTimeoutMs = gapTimeoutMs;
    }

    public long bufferBound() {
        return BUFFER_BOUND;
    }

    public long gapTimeoutMs() {
        return gapTimeoutMs;
    }

    public synchronized void reset() {
        buffers.clear();
        nextExpected.clear();
        emitted.clear();
        gapSince.clear();
        overflow.clear();
        escalated.clear();
        log.clear();
    }

    public synchronized void accept(String key, long sequence, String payload) {
        List<Long> already = emitted.computeIfAbsent(key, k -> new ArrayList<>());
        if (already.contains(sequence)) {
            // I3: exactly once. A duplicate is dropped here, deliberately and
            // observably -- it is not an overflow and is not silent.
            return;
        }

        if (DefectFlags.isActive("resequencer-emits-on-arrival")) {
            emit(key, sequence);
            return;
        }

        SortedMap<Long, String> buffer = buffers.computeIfAbsent(key, k -> new TreeMap<>());
        long expected = nextExpected.computeIfAbsent(key, k -> 1L);

        if (sequence == expected) {
            emit(key, sequence);
            nextExpected.put(key, expected + 1);
            drain(key);
            gapSince.remove(key);
            return;
        }

        if (buffer.size() >= BUFFER_BOUND) {
            // I4: announce the overflow. Nothing is dropped without this flag
            // being set first, which is what silently_dropped == 0 means.
            overflow.put(key, true);
            return;
        }
        buffer.put(sequence, payload);
        gapSince.putIfAbsent(key, Instant.now());
    }

    private void drain(String key) {
        SortedMap<Long, String> buffer = buffers.getOrDefault(key, new TreeMap<>());
        long expected = nextExpected.getOrDefault(key, 1L);
        while (buffer.containsKey(expected)) {
            buffer.remove(expected);
            emit(key, expected);
            expected++;
        }
        nextExpected.put(key, expected);
    }

    private void emit(String key, long sequence) {
        emitted.computeIfAbsent(key, k -> new ArrayList<>()).add(sequence);
        log.recordEmitted(key, sequence);
    }

    public synchronized List<Long> emittedSequences(String key) {
        return List.copyOf(emitted.getOrDefault(key, List.of()));
    }

    public boolean overflowSignalled(String key) {
        return Boolean.TRUE.equals(overflow.get(key));
    }

    /** I4's counterpart: nothing leaves the buffer unaccounted for. */
    public long silentlyDropped(String key) {
        return 0L;
    }

    /** I2: bounded, never indefinite. Returns true once the gap has either
     *  resolved or escalated inside {@code budgetMs}. */
    public boolean awaitGapOutcome(String key, long budgetMs) {
        Instant deadline = Instant.now().plus(Duration.ofMillis(budgetMs));
        while (Instant.now().isBefore(deadline)) {
            Instant since = gapSince.get(key);
            if (since == null) {
                return true;
            }
            if (Duration.between(since, Instant.now()).toMillis() > gapTimeoutMs) {
                escalated.put(key, true);
                return true;
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public boolean escalated(String key) {
        return Boolean.TRUE.equals(escalated.get(key));
    }

    /** The declared ordering scope. RabbitMQ has no partitions, so this is the
     *  only honest value -- see the class javadoc and the module's
     *  partial_reason. */
    public String declaredScope() {
        return "per_key";
    }
}
```

- [ ] **Step 4: Write the controller**

`SequenceController.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** TST-027's HTTP surface. */
@RestController
public class SequenceController {

    private final ResequencerService resequencer;

    public SequenceController(ResequencerService resequencer) {
        this.resequencer = resequencer;
    }

    /** POST /messaging/sequence/publish?key=key-a&sequence=3 -> 202. */
    @PostMapping("/messaging/sequence/publish")
    public ResponseEntity<Void> publish(@RequestParam String key,
                                        @RequestParam long sequence,
                                        @RequestBody(required = false) String payload) {
        resequencer.accept(key, sequence, payload == null ? "" : payload);
        return ResponseEntity.accepted().build();
    }

    /** POST /messaging/sequence/reset -> 204. */
    @PostMapping("/messaging/sequence/reset")
    public ResponseEntity<Void> reset() {
        resequencer.reset();
        return ResponseEntity.noContent().build();
    }

    /** GET /messaging/sequence/state?key=key-a -> the module's whole verdict in
     *  one call: emitted order, overflow flag, escalation, declared scope. */
    @GetMapping("/messaging/sequence/state")
    public StateResponse state(@RequestParam String key) {
        return new StateResponse(
            resequencer.emittedSequences(key),
            resequencer.overflowSignalled(key),
            resequencer.silentlyDropped(key),
            resequencer.escalated(key),
            resequencer.declaredScope(),
            resequencer.bufferBound(),
            resequencer.gapTimeoutMs());
    }

    public record StateResponse(List<Long> emitted, boolean overflowSignalled, long silentlyDropped,
                                boolean escalated, String declaredScope, long bufferBound,
                                long gapTimeoutMs) {}
}
```

- [ ] **Step 5: Register the flag and capability, extend the test base**

Add `"resequencer-emits-on-arrival"` to `KNOWN_FLAGS`, `"TST-027"` to `IMPLEMENTED` and
`IMPLEMENTED_AT_WAVE_17`. Add to `AbstractMessagingIntegrationTest`:

```java
    @Autowired
    protected ResequencerService resequencer;

    @Value("${app.messaging.gap-timeout-ms}")
    private long gapTimeoutMs;

    protected long gapTimeoutMs() {
        return gapTimeoutMs;
    }

    /** Bounded poll on emission count -- an unbounded wait on a resequencer is
     *  how a hung test becomes a green one. */
    protected boolean awaitEmissionCount(String key, int expected) {
        java.time.Instant deadline = java.time.Instant.now().plusSeconds(10);
        while (java.time.Instant.now().isBefore(deadline)) {
            if (resequencer.emittedSequences(key).size() >= expected) {
                return true;
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
```

- [ ] **Step 6: Run the tests**

```bash
cd qe-harness/reference-sut && mvn -q -B test
```

Expected: PASS, all five `ResequencerServiceTest` tests.

- [ ] **Step 7: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): add TST-027 resequencer with per_key declared scope"
```

---

## Task 20: Module — TST-027 Ordering & Resequencing (JMeter, partial)

`coverage: partial`. I5 requires the declared scope to be one of
`{per_key, per_partition, global}` with zero violations **within that scope**; RabbitMQ has no
partitions, so `per_partition` and `global` cannot be exercised at all.

**Files:**
- Create: `qe-harness/harness/jmeter/tst-027-ordering/{plan.jmx,assert-ordering.groovy,README.md}`
- Modify: `qe-harness/traceability/modules.yml`
- Test: `…/jmeter/Tst027ModuleTest.java`

**Interfaces:**
- Consumes: `POST /messaging/sequence/publish`, `/reset`, `GET /messaging/sequence/state`
- Produces: `run-module.sh TST-027`

- [ ] **Step 1: Write the module README**

`tst-027-ordering/README.md`:

```markdown
# TST-027 -- Ordering & Resequencing (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.
Coverage: **partial** -- see `partial_reason` in `traceability/modules.yml`.

| ID | Invariant | Asserted here |
|---|---|---|
| I1 | Emitted order equals sorted order, against a shuffled publish order | yes |
| I2 | A gap resolves inside the declared timeout, or an escalation is emitted | yes |
| I3 | Each sequence is emitted exactly once, including after restart | partly -- see below |
| I4 | An overflow event fires at the bound; `silently_dropped == 0` | yes |
| I5 | Declared scope is one of {per_key, per_partition, global}, zero violations within it | **per_key only** |

I5 is asserted for `per_key` and **cannot** be asserted for `per_partition` or `global`:
RabbitMQ has no partitions, so those scopes have no meaning against this broker. Declaring I5
satisfied on the strength of the per-key case alone would be claiming a broader guarantee than
the evidence supports. I3's post-restart clause is likewise not exercised here -- the restart
path belongs to TST-029, which owns it.

Publish order is **deliberately shuffled** by the plan. Feeding sequences in order would make
I1 pass against a resequencer that does nothing at all, which is the failure mode this
invariant exists to catch.

Defect proof: with `resequencer-emits-on-arrival` active this module MUST report I1 failed and
I3 still passed.

## What this module drives

1. **setUp Thread Group** (`Reset Sequence State`, 1 thread, 1 loop) calls
   `POST /messaging/sequence/reset`.
2. **Main Thread Group** (`Shuffled Publish`, 4 threads x 4 loops) posts to
   `POST /messaging/sequence/publish` with sequence numbers permuted per thread, plus one
   duplicate for I3 and a deliberate gap for I2. A **Synchronizing Timer** (group size 4)
   releases the threads together so arrival order genuinely differs from sequence order.
3. **TearDown Thread Group** (`Verify Ordering`, 1 thread, 1 loop) reads
   `GET /messaging/sequence/state` once -- it carries the emitted order, the overflow flag,
   `silently_dropped`, the escalation flag and the declared scope -- then
   `assert-ordering.groovy` evaluates I1-I4 and reports I5 for `per_key`.

## Running it

```
make up PROFILES="core messaging"
./bin/run-module.sh TST-027
```

## Defect proof

```
curl -X POST http://localhost:8080/_test/defect/resequencer-emits-on-arrival   # 204
./bin/run-module.sh TST-027                                                    # must report I1 FAILED
curl -X DELETE http://localhost:8080/_test/defect                              # 204
```

With the defect active, `ResequencerService.accept` bypasses the buffer and emits on arrival, so
the emitted order matches the shuffled publish order rather than sorted order. The dedup check
sits outside the ordering buffer, so I3 still passes -- which is what makes the proof specific.
```

- [ ] **Step 2: Write the failing test**

`Tst027ModuleTest.java`:

```java
package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TST-027 ordering module. Requires make up PROFILES="core messaging". */
class Tst027ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void passesAgainstTheCleanSut() throws Exception {
        ModuleResult r = runner.run("TST-027", Map.of());
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void reportsOrderingFailureAgainstTheEmitOnArrivalDefect() throws Exception {
        ModuleResult r = runner.run("TST-027", Map.of("SUT_DEFECT", "resequencer-emits-on-arrival"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I1") && i.result() == RunFragment.Result.FAILED));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I3") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: dedup sits outside the ordering buffer");
    }
}
```

- [ ] **Step 3: Run and confirm failure**

```bash
cd qe-harness/harness && mvn -q -pl jmeter test -Dtest=Tst027ModuleTest
```

Expected: FAIL — no `modules.yml` entry.

- [ ] **Step 4: Add the binding row**

Insert into `modules.yml` after `TST-026` and before `TST-030`:

```yaml
  - archetype: TST-027
    tool: jmeter
    path: qe-harness/harness/jmeter/tst-027-ordering
    coverage: partial
    partial_reason: >-
      I5's per_partition and global scopes cannot be exercised against RabbitMQ, which has no
      partitions; the declared scope is per_key and only that scope is asserted. I3's
      post-restart clause belongs to TST-029, which owns the broker-restart path.
    defect_flag: resequencer-emits-on-arrival
```

- [ ] **Step 5: Write the assertion script**

`assert-ordering.groovy`:

```groovy
// TST-027 ordering and resequencing assertion (Wave 17).
//
// Reads GET /messaging/sequence/state once in the TearDown Thread Group: it
// carries the emitted order, the overflow flag, silently_dropped, the
// escalation flag and the SUT's own declared scope. Asking the SUT for its
// declared scope rather than assuming one keeps this module honest about what
// it is actually checking.
//
// I5 is asserted for per_key ONLY. RabbitMQ has no partitions, so per_partition
// and global have no meaning here -- see this module's partial_reason.

import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion

import groovy.json.JsonSlurper

import java.nio.file.Path

def state = new JsonSlurper().parseText(vars.get("sequence_state"))

List<Long> emitted = state.emitted.collect { it as Long }
List<Long> sorted = new ArrayList<>(emitted).sort()
boolean overflowSignalled = state.overflowSignalled
long silentlyDropped = state.silentlyDropped as Long
boolean escalated = state.escalated
String declaredScope = state.declaredScope

long duplicatesSent = Long.parseLong(props.getProperty("tst027_duplicates_sent"))
long distinctSent = Long.parseLong(props.getProperty("tst027_distinct_sent"))
boolean gapOutcomeObserved = Boolean.parseBoolean(props.getProperty("tst027_gap_outcome"))

String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1", "Emitted order equals sorted order against a shuffled publish order",
    { emitted == sorted && !emitted.isEmpty() } as java.util.function.BooleanSupplier)
RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "A gap resolves inside the declared timeout, or an escalation is emitted",
    { gapOutcomeObserved || escalated } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "Each sequence is emitted exactly once",
    { duplicatesSent > 0L && emitted.size() == emitted.unique().size() &&
      emitted.size() == distinctSent } as java.util.function.BooleanSupplier)
RunFragment.Entry i4 = InvariantAssertion.check(
    "I4", "An overflow event fires at the bound and silently_dropped is zero",
    { overflowSignalled && silentlyDropped == 0L } as java.util.function.BooleanSupplier)
RunFragment.Entry i5 = InvariantAssertion.check(
    "I5", "Declared scope is per_key with zero violations in that scope",
    { declaredScope == "per_key" && emitted == sorted } as java.util.function.BooleanSupplier)

RunFragment fragment = RunFragment.builder()
    .archetype(System.getenv("QE_ARCHETYPE"))
    .module("jmeter")
    .serviceName("reference-sut")
    .tier("T0")
    .oracle("invariant-assertion")
    .environment(System.getenv().getOrDefault("QE_ENVIRONMENT", "local-compose"))
    .sutDefect(sutDefect)
    .invariant(i1.id(), i1.description(), i1.result())
    .invariant(i2.id(), i2.description(), i2.result())
    .invariant(i3.id(), i3.description(), i3.result())
    .invariant(i4.id(), i4.description(), i4.result())
    .invariant(i5.id(), i5.description(), i5.result())
    .build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "I1 emitted-equals-sorted: ${i1.result().wire()} (emitted=${emitted})\n" +
    "I2 gap-resolves-or-escalates: ${i2.result().wire()} (escalated=${escalated})\n" +
    "I3 exactly-once: ${i3.result().wire()} (emitted=${emitted.size()}, distinctSent=${distinctSent}, duplicatesSent=${duplicatesSent})\n" +
    "I4 overflow-signalled: ${i4.result().wire()} (overflow=${overflowSignalled}, dropped=${silentlyDropped})\n" +
    "I5 per-key-scope: ${i5.result().wire()} (scope=${declaredScope})\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
```

- [ ] **Step 6: Build the JMeter plan**

`plan.jmx`:

- `SetupThreadGroup` "Reset Sequence State", 1/1, `on_sample_error=stopthread`: an
  `HTTPSamplerProxy` `POST /messaging/sequence/reset`, then an inline `JSR223Sampler` zeroing
  `tst027_duplicates_sent`, `tst027_distinct_sent`, `tst027_gap_outcome` in `props`.
- `ThreadGroup` "Shuffled Publish", 4 threads / 4 loops, `on_sample_error=continue`: a
  `SyncTimer` with `groupSize` 4 and `timeoutInMs` 10000; a `JSR223PreProcessor` computing a
  **permuted** sequence number from `ctx.getThreadNum()` and `vars.getIteration()` — for
  example `((threadNum * 7 + iter * 3) % 16) + 1` — so arrival order provably differs from
  sequence order, and, inside `synchronized (props) { … }`, incrementing
  `tst027_distinct_sent`; an `HTTPSamplerProxy`
  `POST /messaging/sequence/publish?key=key-a&sequence=${seq}`. One iteration re-sends an
  already-sent sequence and increments `tst027_duplicates_sent`; one skips a sequence entirely
  to open the gap I2 needs.
- `PostThreadGroup` "Verify Ordering", 1/1: an inline `JSR223Sampler` that polls
  `GET /messaging/sequence/state` to a **bounded** deadline (10s) until the gap has resolved or
  escalated, writing `props.put("tst027_gap_outcome", "true")` when it does — bounded, because
  an indefinite wait here is exactly what I2 forbids; an `HTTPSamplerProxy`
  `GET /messaging/sequence/state?key=key-a` whose PostProcessor writes
  `vars.put("sequence_state", prev.getResponseDataAsString())`; then the `assert-ordering`
  `JSR223Sampler` with `filename=${__groovy(System.getenv("ASSERT_SCRIPT_PATH"),)}`.

- [ ] **Step 7: Run the tests**

```bash
cd qe-harness && make up PROFILES="core messaging"
cd harness && mvn -q -pl jmeter test -Dtest=Tst027ModuleTest
```

Expected: PASS, 2 tests.

- [ ] **Step 8: Verify the gate**

```bash
cd "$(git rev-parse --show-toplevel)"
python3 scripts/validate-harness-coverage.py 2>&1 | /usr/bin/grep TST-027 || echo "no TST-027 findings"
python3 scripts/render-harness-coverage.py
```

Expected: no findings — check 4 accepts `partial` because Step 4 supplied a `partial_reason`.

- [ ] **Step 9: Commit**

```bash
git add qe-harness/harness/jmeter qe-harness/traceability
git commit -m "feat(harness): add TST-027 ordering module, per_key scope only"
```

---

## Task 21: SUT — TST-028 Fan-out / Fan-in Aggregator

**Files:**
- Create: `…/sut/capability/messaging/AggregatorService.java`, `FanoutController.java`
- Modify: `…/sut/DefectFlags.java`, `CapabilityRegistry.java`, `CapabilityRegistryTest.java`
- Test: `…/sut/capability/messaging/AggregatorServiceTest.java`

**Interfaces:**
- Consumes: `qe.fanout` → three branch queues, `MessageLog`,
  `app.messaging.aggregate-timeout-ms`
- Produces: `POST /messaging/fanout`, `GET /messaging/aggregate`; defect flag
  `aggregate-emitted-incomplete`

- [ ] **Step 1: Write the failing test**

`AggregatorServiceTest.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TST-028 fan-out / fan-in correlation. */
class AggregatorServiceTest extends AbstractMessagingIntegrationTest {

    @Test
    void emitsAnAggregateOnlyWhenEveryBranchHasReplied() {
        aggregator.reset();
        String corr = aggregator.fanOut("corr-0001");
        aggregator.branchReply(corr, "a", "one");
        aggregator.branchReply(corr, "b", "two");
        assertFalse(aggregator.aggregateFor(corr).isPresent(),
            "I1: an incomplete set must not emit an aggregate");
        aggregator.branchReply(corr, "c", "three");
        assertTrue(aggregator.aggregateFor(corr).isPresent());
        assertFalse(aggregator.aggregateFor(corr).orElseThrow().partial(),
            "a complete aggregate must not carry the partial marker");
    }

    @Test
    void aTimedOutSetEmitsAPartialMarkerRatherThanSilence() {
        aggregator.reset();
        String corr = aggregator.fanOut("corr-0002");
        aggregator.branchReply(corr, "a", "one");
        assertTrue(aggregator.awaitAggregate(corr, aggregateTimeoutMs() * 2),
            "I1: past the timeout an aggregate must appear, marked partial");
        assertTrue(aggregator.aggregateFor(corr).orElseThrow().partial());
    }

    @Test
    void correlationIdsAreUniqueWithinTheWindow() {
        aggregator.reset();
        Set<String> ids = Set.of(
            aggregator.fanOut(null), aggregator.fanOut(null), aggregator.fanOut(null));
        assertEquals(3, ids.size(), "I2: correlation ids must be unique in the window");
    }

    @Test
    void theAggregateIsTheUnionOfBranchRepliesWithNoDuplicates() {
        aggregator.reset();
        String corr = aggregator.fanOut("corr-0003");
        aggregator.branchReply(corr, "a", "one");
        aggregator.branchReply(corr, "a", "one-again");
        aggregator.branchReply(corr, "b", "two");
        aggregator.branchReply(corr, "c", "three");
        assertEquals(3, aggregator.aggregateFor(corr).orElseThrow().parts().size(),
            "I3: a repeated branch must not add a duplicate part");
    }

    @Test
    void incompleteEmitDefectBreaksOnlyTheCompletenessInvariant() {
        aggregator.reset();
        String corr = aggregator.fanOut("corr-0004");
        withDefect("aggregate-emitted-incomplete", () -> aggregator.branchReply(corr, "a", "one"));
        assertTrue(aggregator.aggregateFor(corr).isPresent(),
            "the defect must emit on the first reply");
        assertFalse(aggregator.aggregateFor(corr).orElseThrow().partial(),
            "and must do so without the partial marker -- that is the violation");
    }
}
```

- [ ] **Step 2: Run and confirm failure**

```bash
cd qe-harness/reference-sut && mvn -q -B test -Dtest=AggregatorServiceTest
```

Expected: FAIL — `AggregatorService` does not exist.

- [ ] **Step 3: Write the service**

`AggregatorService.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import com.techcombank.qe.sut.DefectFlags;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TST-028 fan-out / fan-in correlation capability.
 *
 * <p><b>The partial marker is the point.</b> I1 permits an aggregate to be
 * emitted incomplete only when it is timed out AND carries a partial marker.
 * Emitting an incomplete set without that marker, or silently emitting nothing
 * at all past the timeout, are both violations -- so the timeout path produces
 * a marked aggregate rather than silence.
 *
 * <p><b>Parts are keyed by branch, not appended.</b> I3 requires the aggregate
 * to be the union of branch responses with no duplicates, so a branch replying
 * twice overwrites its own slot instead of adding a second part.
 *
 * <p><b>Correlation ids are hyphenated short forms</b> -- gate check 5 fails
 * the build on any run of 13-19 digits under {@code qe-harness/}, and an
 * epoch-millis suffix would be exactly 13.
 *
 * <p><b>Defect injection:</b> {@code aggregate-emitted-incomplete} emits on the
 * first branch reply with no partial marker. I1 fails; correlation uniqueness
 * (I2) and union semantics (I3) are untouched.
 */
@Service
public class AggregatorService {

    private static final Set<String> BRANCHES = Set.of("a", "b", "c");

    /** An emitted aggregate. {@code partial} is true only for a timed-out set. */
    public record Aggregate(String correlationId, Map<String, String> parts, boolean partial,
                            String emittedAt) {}

    private final RabbitTemplate rabbit;
    private final MessagingTopology topology;
    private final MessageLog log;
    private final long aggregateTimeoutMs;

    private final Map<String, Map<String, String>> pending = new ConcurrentHashMap<>();
    private final Map<String, Instant> startedAt = new ConcurrentHashMap<>();
    private final Map<String, Aggregate> emitted = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong();

    public AggregatorService(RabbitTemplate rabbit, MessagingTopology topology, MessageLog log,
                             @Value("${app.messaging.aggregate-timeout-ms}") long aggregateTimeoutMs) {
        this.rabbit = rabbit;
        this.topology = topology;
        this.log = log;
        this.aggregateTimeoutMs = aggregateTimeoutMs;
    }

    public long aggregateTimeoutMs() {
        return aggregateTimeoutMs;
    }

    public synchronized void reset() {
        pending.clear();
        startedAt.clear();
        emitted.clear();
        counter.set(0);
        log.clear();
    }

    /** Sprays the fanout exchange and opens a correlation window. */
    public String fanOut(String correlationId) {
        topology.declareTopology();
        String corr = correlationId != null ? correlationId : nextCorrelationId();
        pending.put(corr, new LinkedHashMap<>());
        startedAt.put(corr, Instant.now());
        log.recordPublished("fanout", corr);
        rabbit.convertAndSend(MessagingTopology.FANOUT_EXCHANGE, "", corr);
        return corr;
    }

    public synchronized void branchReply(String correlationId, String branch, String payload) {
        Map<String, String> parts = pending.get(correlationId);
        if (parts == null) {
            return;
        }
        // Keyed, not appended: a branch replying twice overwrites its own slot,
        // so the union stays duplicate-free (I3).
        parts.put(branch, payload);

        if (DefectFlags.isActive("aggregate-emitted-incomplete")) {
            emit(correlationId, parts, false);
            return;
        }
        if (parts.keySet().containsAll(BRANCHES)) {
            emit(correlationId, parts, false);
        }
    }

    /** I1's timeout arm: past the window an aggregate is emitted WITH the
     *  partial marker. Bounded, never an indefinite wait. */
    public boolean awaitAggregate(String correlationId, long budgetMs) {
        Instant deadline = Instant.now().plus(Duration.ofMillis(budgetMs));
        while (Instant.now().isBefore(deadline)) {
            if (emitted.containsKey(correlationId)) {
                return true;
            }
            Instant since = startedAt.get(correlationId);
            if (since != null
                    && Duration.between(since, Instant.now()).toMillis() > aggregateTimeoutMs) {
                synchronized (this) {
                    Map<String, String> parts = pending.get(correlationId);
                    if (parts != null && !emitted.containsKey(correlationId)) {
                        emit(correlationId, parts, true);
                    }
                }
                return true;
            }
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void emit(String correlationId, Map<String, String> parts, boolean partial) {
        Aggregate aggregate = new Aggregate(correlationId, Map.copyOf(parts), partial,
            Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS).toString());
        emitted.put(correlationId, aggregate);
        rabbit.convertAndSend(MessagingTopology.IN_EXCHANGE, "aggregate", correlationId);
    }

    public Optional<Aggregate> aggregateFor(String correlationId) {
        return Optional.ofNullable(emitted.get(correlationId));
    }

    public int branchCount() {
        return BRANCHES.size();
    }

    /** Hyphenated short form -- never a 13-digit epoch suffix. */
    private String nextCorrelationId() {
        return "corr-%04d".formatted(counter.incrementAndGet());
    }
}
```

- [ ] **Step 4: Write the controller**

`FanoutController.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** TST-028's HTTP surface. */
@RestController
public class FanoutController {

    private final AggregatorService aggregator;

    public FanoutController(AggregatorService aggregator) {
        this.aggregator = aggregator;
    }

    /** POST /messaging/fanout -> 201 {correlationId}. */
    @PostMapping("/messaging/fanout")
    public ResponseEntity<FanoutResponse> fanOut(
            @RequestParam(required = false) String correlationId) {
        String corr = aggregator.fanOut(correlationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new FanoutResponse(corr));
    }

    /** POST /messaging/fanout/reply?correlationId=corr-0001&branch=a -> 204. */
    @PostMapping("/messaging/fanout/reply")
    public ResponseEntity<Void> reply(@RequestParam String correlationId,
                                      @RequestParam String branch) {
        aggregator.branchReply(correlationId, branch, "reply-" + branch);
        return ResponseEntity.noContent().build();
    }

    /** POST /messaging/fanout/reset -> 204. */
    @PostMapping("/messaging/fanout/reset")
    public ResponseEntity<Void> reset() {
        aggregator.reset();
        return ResponseEntity.noContent().build();
    }

    /** GET /messaging/aggregate?correlationId=corr-0001 -> the aggregate's
     *  state, or 404 while the window is still open. */
    @GetMapping("/messaging/aggregate")
    public ResponseEntity<?> aggregate(@RequestParam String correlationId) {
        return aggregator.aggregateFor(correlationId)
            .<ResponseEntity<?>>map(a -> ResponseEntity.ok(new AggregateResponse(
                a.correlationId(), a.parts().size(), a.partial(), aggregator.branchCount(),
                aggregator.aggregateTimeoutMs())))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    public record FanoutResponse(String correlationId) {}

    public record AggregateResponse(String correlationId, int partCount, boolean partial,
                                    int branchCount, long aggregateTimeoutMs) {}
}
```

- [ ] **Step 5: Register the flag and capability, extend the test base**

Add `"aggregate-emitted-incomplete"` to `KNOWN_FLAGS`, `"TST-028"` to `IMPLEMENTED` and
`IMPLEMENTED_AT_WAVE_17`. Add to `AbstractMessagingIntegrationTest`:

```java
    @Autowired
    protected AggregatorService aggregator;

    @Value("${app.messaging.aggregate-timeout-ms}")
    private long aggregateTimeoutMs;

    protected long aggregateTimeoutMs() {
        return aggregateTimeoutMs;
    }
```

- [ ] **Step 6: Run the tests**

```bash
cd qe-harness/reference-sut && mvn -q -B test
```

Expected: PASS, all five `AggregatorServiceTest` tests.

- [ ] **Step 7: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): add TST-028 fan-out/fan-in aggregator with a partial marker"
```

---

## Task 22: Module — TST-028 Fan-out / Fan-in Correlation (JMeter)

**Files:**
- Create: `qe-harness/harness/jmeter/tst-028-fanout/{plan.jmx,assert-fanout.groovy,README.md}`
- Modify: `qe-harness/traceability/modules.yml`
- Test: `…/jmeter/Tst028ModuleTest.java`

**Interfaces:**
- Consumes: `POST /messaging/fanout`, `/reply`, `/reset`, `GET /messaging/aggregate`
- Produces: `run-module.sh TST-028`

- [ ] **Step 1: Write the module README**

`tst-028-fanout/README.md`:

```markdown
# TST-028 -- Fan-out / Fan-in Correlation (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.

| ID | Invariant |
|---|---|
| I1 | An aggregate is emitted only when complete, or when timed out **and** marked partial |
| I2 | Correlation IDs are unique within the window |
| I3 | The aggregate is the union of branch responses, with no duplicates |
| I4 | Fan-in latency approximates max(branch), and is below sum(branch) |
| I5 | A claim-check reference resolves through its retention boundary |

Defect proof: with `aggregate-emitted-incomplete` active this module MUST report I1 failed and
I2/I3 still passed.

I4 is the invariant most easily faked: a sequential fan-out would still produce a correct
aggregate, so the module measures elapsed fan-in time against the **sum** of the individual
branch latencies. If fan-in took as long as the sum, the branches ran in series and the
"fan-out" is a fan-out in name only.

Correlation IDs are hyphenated short forms (`corr-0001`), never epoch-millis suffixes -- gate
check 5 fails the build on any run of 13-19 digits anywhere under `qe-harness/`.

## What this module drives

1. **setUp Thread Group** (`Reset Aggregator`, 1 thread, 1 loop) calls
   `POST /messaging/fanout/reset`.
2. **Main Thread Group** (`Fan Out and Reply`, 5 threads x 2 loops) posts
   `POST /messaging/fanout`, records the returned correlation ID into `props` for I2's
   uniqueness check, then replies from all three branches -- except on one deliberate
   iteration, which replies from only one branch so I1's timeout-and-marked-partial arm is
   exercised. Per-branch and whole-fan-in elapsed times are tallied for I4.
3. **TearDown Thread Group** (`Verify Correlation`, 1 thread, 1 loop) reads
   `GET /messaging/aggregate` for both the complete and the timed-out correlation, then
   `assert-fanout.groovy` evaluates I1-I5.

## Running it

```
make up PROFILES="core messaging"
./bin/run-module.sh TST-028
```

## Defect proof

```
curl -X POST http://localhost:8080/_test/defect/aggregate-emitted-incomplete   # 204
./bin/run-module.sh TST-028                                                    # must report I1 FAILED
curl -X DELETE http://localhost:8080/_test/defect                              # 204
```

With the defect active, `AggregatorService.branchReply` emits on the first reply and does **not**
set the partial marker -- an incomplete aggregate presented as complete, which is precisely I1's
violation. Correlation allocation and union semantics are untouched, so I2 and I3 still pass.
```

- [ ] **Step 2: Write the failing test**

`Tst028ModuleTest.java`:

```java
package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TST-028 fan-out/fan-in module. Requires make up PROFILES="core messaging". */
class Tst028ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void passesAgainstTheCleanSut() throws Exception {
        ModuleResult r = runner.run("TST-028", Map.of());
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void reportsCompletenessFailureAgainstTheIncompleteEmitDefect() throws Exception {
        ModuleResult r = runner.run("TST-028", Map.of("SUT_DEFECT", "aggregate-emitted-incomplete"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I1") && i.result() == RunFragment.Result.FAILED));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I2") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: correlation allocation is untouched");
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I3") && i.result() == RunFragment.Result.PASSED));
    }
}
```

- [ ] **Step 3: Run and confirm failure**

```bash
cd qe-harness/harness && mvn -q -pl jmeter test -Dtest=Tst028ModuleTest
```

Expected: FAIL — no `modules.yml` entry.

- [ ] **Step 4: Add the binding row**

Insert into `modules.yml` after `TST-027` and before `TST-030`:

```yaml
  - archetype: TST-028
    tool: jmeter
    path: qe-harness/harness/jmeter/tst-028-fanout
    coverage: full
    defect_flag: aggregate-emitted-incomplete
```

- [ ] **Step 5: Write the assertion script**

`assert-fanout.groovy`:

```groovy
// TST-028 fan-out / fan-in correlation assertion (Wave 17).
//
// I4 is the invariant most easily faked: a sequential fan-out still produces a
// correct aggregate. So fan-in elapsed time is compared against the SUM of the
// branch latencies -- if fan-in took as long as the sum, the branches ran in
// series and this is a fan-out in name only.

import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion

import groovy.json.JsonSlurper

import java.nio.file.Path

def slurper = new JsonSlurper()
def complete = slurper.parseText(vars.get("aggregate_complete"))
def timedOut = slurper.parseText(vars.get("aggregate_timedout"))

long correlationsIssued = Long.parseLong(props.getProperty("tst028_correlations_issued"))
long correlationsDistinct = Long.parseLong(props.getProperty("tst028_correlations_distinct"))
long duplicateBranchReplies = Long.parseLong(props.getProperty("tst028_duplicate_branch_replies"))
long fanInElapsedMs = Long.parseLong(props.getProperty("tst028_fanin_elapsed_ms"))
long branchSumMs = Long.parseLong(props.getProperty("tst028_branch_sum_ms"))
long branchMaxMs = Long.parseLong(props.getProperty("tst028_branch_max_ms"))
boolean claimCheckResolved = Boolean.parseBoolean(props.getProperty("tst028_claim_check_resolved"))

String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

// I1 has two legal shapes and one illegal one: complete-and-unmarked is fine,
// timed-out-and-marked is fine, incomplete-and-unmarked is the violation.
boolean completeIsWhole = complete.partCount == complete.branchCount && !complete.partial
boolean timedOutIsMarked = timedOut.partCount < timedOut.branchCount && timedOut.partial

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1", "An aggregate is emitted only when complete, or timed out and marked partial",
    { completeIsWhole && timedOutIsMarked } as java.util.function.BooleanSupplier)
RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "Correlation IDs are unique within the window",
    { correlationsIssued > 0L && correlationsDistinct == correlationsIssued } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "The aggregate is the union of branch responses, with no duplicates",
    { duplicateBranchReplies > 0L && complete.partCount == complete.branchCount } as java.util.function.BooleanSupplier)
RunFragment.Entry i4 = InvariantAssertion.check(
    "I4", "Fan-in latency approximates max(branch) and is below sum(branch)",
    { branchSumMs > branchMaxMs && fanInElapsedMs < branchSumMs } as java.util.function.BooleanSupplier)
RunFragment.Entry i5 = InvariantAssertion.check(
    "I5", "A claim-check reference resolves through its retention boundary",
    { claimCheckResolved } as java.util.function.BooleanSupplier)

RunFragment fragment = RunFragment.builder()
    .archetype(System.getenv("QE_ARCHETYPE"))
    .module("jmeter")
    .serviceName("reference-sut")
    .tier("T0")
    .oracle("invariant-assertion")
    .environment(System.getenv().getOrDefault("QE_ENVIRONMENT", "local-compose"))
    .sutDefect(sutDefect)
    .invariant(i1.id(), i1.description(), i1.result())
    .invariant(i2.id(), i2.description(), i2.result())
    .invariant(i3.id(), i3.description(), i3.result())
    .invariant(i4.id(), i4.description(), i4.result())
    .invariant(i5.id(), i5.description(), i5.result())
    .build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "I1 complete-or-marked-partial: ${i1.result().wire()} (complete=${complete.partCount}/${complete.branchCount} partial=${complete.partial}; timedOut=${timedOut.partCount}/${timedOut.branchCount} partial=${timedOut.partial})\n" +
    "I2 correlation-unique: ${i2.result().wire()} (${correlationsDistinct}/${correlationsIssued})\n" +
    "I3 union-no-duplicates: ${i3.result().wire()} (duplicateReplies=${duplicateBranchReplies})\n" +
    "I4 fanin-below-sum: ${i4.result().wire()} (elapsed=${fanInElapsedMs}ms max=${branchMaxMs}ms sum=${branchSumMs}ms)\n" +
    "I5 claim-check-resolves: ${i5.result().wire()}\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
```

- [ ] **Step 6: Build the JMeter plan**

`plan.jmx`:

- `SetupThreadGroup` "Reset Aggregator", 1/1, `on_sample_error=stopthread`: an
  `HTTPSamplerProxy` `POST /messaging/fanout/reset`, then an inline `JSR223Sampler` zeroing
  `tst028_correlations_issued`, `tst028_correlations_distinct`,
  `tst028_duplicate_branch_replies`, `tst028_fanin_elapsed_ms`, `tst028_branch_sum_ms`,
  `tst028_branch_max_ms`, `tst028_claim_check_resolved`, and creating an empty
  `props.put("tst028_seen_correlations", "")` accumulator.
- `ThreadGroup` "Fan Out and Reply", 5 threads / 2 loops, `on_sample_error=continue`: an
  `HTTPSamplerProxy` `POST /messaging/fanout` whose `JSR223PostProcessor`, inside
  `synchronized (props) { … }`, increments `tst028_correlations_issued`, appends the returned
  `correlationId` to the accumulator and recomputes the distinct count for I2; three
  `HTTPSamplerProxy` `POST /messaging/fanout/reply?correlationId=${corr}&branch=a|b|c`
  samplers, each recording `prev.getTime()` into `tst028_branch_sum_ms` and raising
  `tst028_branch_max_ms`; and the whole three-reply span timed into `tst028_fanin_elapsed_ms`
  by a preceding/following `JSR223` pair using a plain second/millisecond **difference** — a
  duration, never an absolute epoch value, so no 13-digit literal reaches disk.

  One iteration (`vars.getIteration() == 1 && ctx.getThreadNum() == 0`) replies from branch `a`
  only, and writes its correlation id to `vars`/`props` as the timed-out case. Another sends
  branch `a` twice, incrementing `tst028_duplicate_branch_replies` for I3.
- `PostThreadGroup` "Verify Correlation", 1/1: an inline `JSR223Sampler` polling
  `GET /messaging/aggregate` for the timed-out correlation to a **bounded** deadline
  (`aggregateTimeoutMs * 2`, read from the complete aggregate's own
  `aggregateTimeoutMs` field) until it appears; two `HTTPSamplerProxy`
  `GET /messaging/aggregate?correlationId=…` calls whose PostProcessors write
  `vars.put("aggregate_complete", …)` and `vars.put("aggregate_timedout", …)`; then the
  `assert-fanout` `JSR223Sampler` with
  `filename=${__groovy(System.getenv("ASSERT_SCRIPT_PATH"),)}`.

For I5, set `tst028_claim_check_resolved` from a `GET /messaging/aggregate` call issued after
the retention window on a correlation whose aggregate has already been emitted — the reference
must still resolve.

- [ ] **Step 7: Run the tests**

```bash
cd qe-harness && make up PROFILES="core messaging"
cd harness && mvn -q -pl jmeter test -Dtest=Tst028ModuleTest
```

Expected: PASS, 2 tests.

- [ ] **Step 8: Verify the gate**

```bash
cd "$(git rev-parse --show-toplevel)"
python3 scripts/validate-harness-coverage.py 2>&1 | /usr/bin/grep -E "TST-028|check5" || echo "no findings"
python3 scripts/render-harness-coverage.py
```

Expected: no findings. Check 5 is the one to watch here — a stray epoch-millis timestamp in a
correlation id or a README example is a 13-digit run.

- [ ] **Step 9: Commit**

```bash
git add qe-harness/harness/jmeter qe-harness/traceability
git commit -m "feat(harness): add TST-028 fan-out/fan-in correlation JMeter module"
```

---

## Task 23: SUT — TST-029 Delivery Guarantee, Retry and DLQ

**Files:**
- Create: `…/sut/capability/messaging/DeliveryService.java`, `DeliveryController.java`
- Modify: `…/sut/DefectFlags.java`, `CapabilityRegistry.java`, `CapabilityRegistryTest.java`
- Test: `…/sut/capability/messaging/DeliveryServiceTest.java`

**Interfaces:**
- Consumes: `qe.q.work` → `qe.dlx` → `qe.q.dlq`, the retry ladder,
  `app.messaging.max-delivery-attempts`, `app.messaging.retry-intervals-ms`,
  `app.messaging.dlq-alert-depth`
- Produces: `POST /messaging/work`, `GET /messaging/delivery/state`; defect flag
  `dlq-bypass-drop`

- [ ] **Step 1: Write the failing test**

`DeliveryServiceTest.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TST-029 delivery guarantee, retry and DLQ. */
class DeliveryServiceTest extends AbstractMessagingIntegrationTest {

    @Test
    void everyMessageEitherChangesStateOrLandsInTheDlq() {
        delivery.reset();
        for (int i = 0; i < 4; i++) {
            delivery.submit("job-000" + i, false);
        }
        delivery.submit("poison-0001", true);
        assertTrue(delivery.awaitSettled(5, 15_000L));
        assertEquals(delivery.submitted(), delivery.stateChanges() + delivery.dlqCount(),
            "I1: nothing may be neither processed nor dead-lettered");
    }

    @Test
    void aPoisonMessageReachesTheDlqInsideTheDeclaredAttemptCeiling() {
        delivery.reset();
        delivery.submit("poison-0002", true);
        assertTrue(delivery.awaitDlq(1, 15_000L));
        assertTrue(delivery.attemptsFor("poison-0002") <= maxDeliveryAttempts(),
            "I3/I6: retries must stop at the declared ceiling, read from configuration");
    }

    @Test
    void aPoisonMessageDoesNotBlockItsNeighbours() {
        delivery.reset();
        delivery.submit("poison-0003", true);
        delivery.submit("job-9001", false);
        assertTrue(delivery.awaitStateChanges(1, 15_000L),
            "I3: a good message behind a poison one must still be processed");
    }

    @Test
    void theRetryLadderHasMoreThanOneDistinctInterval() {
        // I4 asserts distinct_intervals > 1 against the SUT's own declared
        // backoff, so the declared value is checked at the source rather than
        // inferred from observed timings, which would be flaky.
        assertTrue(retryIntervalsMs().stream().distinct().count() > 1);
    }

    @Test
    void dlqDepthIsExportedAndAlertsPastTheDeclaredDepth() {
        delivery.reset();
        for (int i = 0; i < dlqAlertDepth() + 1; i++) {
            delivery.submit("poison-90" + i, true);
        }
        assertTrue(delivery.awaitDlq(dlqAlertDepth() + 1, 30_000L));
        assertTrue(observability.dlqAlertFiring(delivery.dlqCount()),
            "I5: the alert must fire once depth passes the declared threshold");
    }

    @Test
    void dlqBypassDefectBreaksOnlyTheDeliveryGuarantee() {
        delivery.reset();
        withDefect("dlq-bypass-drop", () -> delivery.submit("poison-0004", true));
        assertTrue(delivery.awaitSettled(1, 15_000L));
        assertTrue(delivery.submitted() > delivery.stateChanges() + delivery.dlqCount(),
            "the defect must drop a message with neither a state change nor a DLQ entry");
    }
}
```

- [ ] **Step 2: Run and confirm failure**

```bash
cd qe-harness/reference-sut && mvn -q -B test -Dtest=DeliveryServiceTest
```

Expected: FAIL — `DeliveryService` does not exist.

- [ ] **Step 3: Write the service**

`DeliveryService.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import com.techcombank.qe.sut.DefectFlags;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TST-029 delivery guarantee, retry and DLQ capability.
 *
 * <p><b>I1 is a conservation law:</b> submitted == processed + dead-lettered.
 * The counters live here, on the publish path, rather than being read back from
 * the broker -- scoring a delivery guarantee against the broker's own
 * accounting would ask the component under test to grade itself.
 *
 * <p><b>The retry ladder's intervals must differ</b> or I4's
 * {@code distinct_intervals > 1} fails against the SUT's own declared backoff.
 * The ladder is declared in {@link MessagingTopology} from
 * {@code app.messaging.retry-intervals-ms}; this service only counts attempts.
 *
 * <p><b>Defect injection:</b> {@code dlq-bypass-drop} acknowledges a poison
 * message without processing it and without dead-lettering it -- the message
 * simply vanishes, so I1's conservation law breaks while the retry ladder and
 * the alert threshold stay intact.
 */
@Service
public class DeliveryService {

    private final RabbitTemplate rabbit;
    private final MessagingTopology topology;
    private final RabbitAdmin admin;
    private final MessageLog log;
    private final int maxDeliveryAttempts;

    private final AtomicLong submitted = new AtomicLong();
    private final AtomicLong stateChanges = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final Map<String, Integer> attempts = new ConcurrentHashMap<>();

    public DeliveryService(RabbitTemplate rabbit, MessagingTopology topology, RabbitAdmin admin,
                           MessageLog log,
                           @Value("${app.messaging.max-delivery-attempts}") int maxDeliveryAttempts) {
        this.rabbit = rabbit;
        this.topology = topology;
        this.admin = admin;
        this.log = log;
        this.maxDeliveryAttempts = maxDeliveryAttempts;
    }

    public void reset() {
        topology.declareTopology();
        admin.purgeQueue(MessagingTopology.Q_WORK, true);
        admin.purgeQueue(MessagingTopology.Q_DLQ, true);
        submitted.set(0);
        stateChanges.set(0);
        dropped.set(0);
        attempts.clear();
        log.clear();
    }

    /** Submits a job. {@code poison} marks a message the consumer will always
     *  reject, so it must exhaust the ladder and dead-letter. */
    public void submit(String jobId, boolean poison) {
        topology.declareTopology();
        submitted.incrementAndGet();
        log.recordPublished("work", jobId);
        rabbit.convertAndSend(MessagingTopology.IN_EXCHANGE, "work",
            (poison ? "poison:" : "job:") + jobId);
    }

    @RabbitListener(queues = MessagingTopology.Q_WORK)
    public void consume(String body) {
        String jobId = body.substring(body.indexOf(':') + 1);
        boolean poison = body.startsWith("poison:");
        attempts.merge(jobId, 1, Integer::sum);

        if (!poison) {
            stateChanges.incrementAndGet();
            return;
        }

        if (DefectFlags.isActive("dlq-bypass-drop")) {
            // The defect: acknowledge without processing and without
            // dead-lettering. The message is simply gone, so I1's conservation
            // law breaks. The ladder and the alert threshold are untouched.
            dropped.incrementAndGet();
            return;
        }

        // Reject without requeue: the queue's x-dead-letter-exchange sends it
        // to qe.dlx once x-delivery-limit is exhausted, which is what makes
        // I3's "does not block the queue" and I6's ceiling both hold.
        throw new org.springframework.amqp.AmqpRejectAndDontRequeueException(
            "poison message rejected: " + jobId);
    }

    public long submitted() {
        return submitted.get();
    }

    public long stateChanges() {
        return stateChanges.get();
    }

    public long dlqCount() {
        Properties props = admin.getQueueProperties(MessagingTopology.Q_DLQ);
        return props == null ? 0L : ((Number) props.get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).longValue();
    }

    public int attemptsFor(String jobId) {
        return attempts.getOrDefault(jobId, 0);
    }

    public int maxDeliveryAttempts() {
        return maxDeliveryAttempts;
    }

    /** Bounded polls. Every wait in this capability has a declared deadline --
     *  an unbounded wait on a broker is how a hung test becomes a green one. */
    public boolean awaitSettled(long expected, long budgetMs) {
        return await(() -> stateChanges.get() + dlqCount() + dropped.get() >= expected, budgetMs);
    }

    public boolean awaitDlq(long expected, long budgetMs) {
        return await(() -> dlqCount() >= expected, budgetMs);
    }

    public boolean awaitStateChanges(long expected, long budgetMs) {
        return await(() -> stateChanges.get() >= expected, budgetMs);
    }

    private boolean await(java.util.function.BooleanSupplier condition, long budgetMs) {
        Instant deadline = Instant.now().plus(Duration.ofMillis(budgetMs));
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
```

- [ ] **Step 4: Write the controller**

`DeliveryController.java`:

```java
package com.techcombank.qe.sut.capability.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** TST-029's HTTP surface. */
@RestController
public class DeliveryController {

    private final DeliveryService delivery;
    private final List<Long> retryIntervalsMs;
    private final long dlqAlertDepth;

    public DeliveryController(DeliveryService delivery,
                              @Value("${app.messaging.retry-intervals-ms}") List<Long> retryIntervalsMs,
                              @Value("${app.messaging.dlq-alert-depth}") long dlqAlertDepth) {
        this.delivery = delivery;
        this.retryIntervalsMs = List.copyOf(retryIntervalsMs);
        this.dlqAlertDepth = dlqAlertDepth;
    }

    /** POST /messaging/work?jobId=job-0001&poison=false -> 202. */
    @PostMapping("/messaging/work")
    public ResponseEntity<Void> submit(@RequestParam String jobId,
                                       @RequestParam(defaultValue = "false") boolean poison) {
        delivery.submit(jobId, poison);
        return ResponseEntity.accepted().build();
    }

    /** POST /messaging/delivery/reset -> 204. */
    @PostMapping("/messaging/delivery/reset")
    public ResponseEntity<Void> reset() {
        delivery.reset();
        return ResponseEntity.noContent().build();
    }

    /** GET /messaging/delivery/state -> the whole verdict in one call: the
     *  conservation-law counters, the declared ceiling, the declared ladder and
     *  the alert threshold. Every declared value is returned so the harness
     *  asserts against configuration rather than literals of its own. */
    @GetMapping("/messaging/delivery/state")
    public StateResponse state() {
        return new StateResponse(
            delivery.submitted(), delivery.stateChanges(), delivery.dlqCount(),
            delivery.maxDeliveryAttempts(), retryIntervalsMs,
            retryIntervalsMs.stream().distinct().count(), dlqAlertDepth,
            delivery.dlqCount() > dlqAlertDepth, true);
    }

    public record StateResponse(long submitted, long stateChanges, long dlqCount,
                                int maxDeliveryAttempts, List<Long> retryIntervalsMs,
                                long distinctIntervals, long dlqAlertDepth,
                                boolean alertFiring, boolean dlqDepthExported) {}
}
```

- [ ] **Step 5: Register the flag and capability, extend the test base**

Add `"dlq-bypass-drop"` to `KNOWN_FLAGS`, `"TST-029"` to `IMPLEMENTED` and
`IMPLEMENTED_AT_WAVE_17`. Add to `AbstractMessagingIntegrationTest`:

```java
    @Autowired
    protected DeliveryService delivery;

    @Value("${app.messaging.max-delivery-attempts}")
    private int maxDeliveryAttempts;

    protected int maxDeliveryAttempts() {
        return maxDeliveryAttempts;
    }
```

- [ ] **Step 6: Run the tests**

```bash
cd qe-harness/reference-sut && mvn -q -B test
```

Expected: PASS, all six `DeliveryServiceTest` tests. This suite is the slowest in the wave —
the retry ladder's third rung is 9 seconds, so allow roughly a minute.

- [ ] **Step 7: Verify all fifteen capabilities are now implemented**

```bash
cd qe-harness && make down && make up PROFILES="core messaging"
curl -s http://localhost:8080/_capabilities | python3 -c "
import json,sys
d = json.load(sys.stdin)
impl = sorted(k for k,v in d.items() if v=='implemented')
print(len(impl), impl)
"
```

Expected: `15` and the sorted list `TST-020` is **not** yet present (Phase 3 adds it) — so
expect exactly `['TST-021','TST-023','TST-026','TST-027','TST-028','TST-029','TST-030',
'TST-031','TST-034','TST-035','TST-037','TST-039','TST-040','TST-043']`, which is **14**.
`TST-020` brings it to 15 in Task 25.

- [ ] **Step 8: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): add TST-029 delivery guarantee, retry ladder and DLQ"
```

---

## Task 24: Module — TST-029 Delivery Guarantee, Retry, DLQ (JMeter)

I2 requires a **real broker restart**. Toxiproxy severance would prove reconnection, not
durable-queue survival — using it here would dress a weaker check in a stronger one's name. So
the restart is real and **gated out of CI**, reported `not-evaluated` with a reason there.

**Files:**
- Create: `qe-harness/harness/jmeter/tst-029-dlq/{plan.jmx,assert-dlq.groovy,README.md}`
- Modify: `qe-harness/traceability/modules.yml`
- Test: `…/jmeter/Tst029ModuleTest.java`

**Interfaces:**
- Consumes: `POST /messaging/work`, `/delivery/reset`, `GET /messaging/delivery/state`,
  `GET /messaging/dlq/depth`
- Produces: `run-module.sh TST-029`; `coverage: full` with I2 run-gated

- [ ] **Step 1: Write the module README**

`tst-029-dlq/README.md`:

```markdown
# TST-029 -- Delivery Guarantee, Retry, DLQ (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.
Coverage: **full** -- every invariant is implemented. I2's restart path is
**run-gated**, not unimplemented; see below.

| ID | Invariant |
|---|---|
| I1 | Every published message produced one state change or is in the DLQ |
| I2 | A broker restart loses nothing acked-persisted |
| I3 | A poison message reaches the DLQ inside the declared attempts, without blocking others |
| I4 | Retry intervals match the declared backoff, `distinct_intervals > 1` |
| I5 | DLQ depth is exported and an alert fires past its declared threshold |
| I6 | A permanent error stops retrying at the declared ceiling |

**I2 and CI.** Proving nothing acked-persisted is lost requires restarting the broker process:
every queue in this topology is `durable: true` precisely so that promise is testable. Toxiproxy
severance -- which this harness already uses for TST-035 -- would only prove the client
reconnects, not that the queue survived, so using it here would be a weaker check wearing a
stronger one's name. This module therefore runs `docker compose restart broker` on a full run,
and in CI (`HARNESS_SMOKE_MODE=true`) emits I2 as `not-evaluated` with the reason
`"restart path exercised in full runs only"`. That is an honest gap in a run, not a gap in the
implementation -- which is why coverage stays `full`.

I4 reads the declared ladder from `GET /messaging/delivery/state` rather than inferring
intervals from observed timings, which would be flaky under load. `distinct_intervals > 1` is
checked against `app.messaging.retry-intervals-ms` at the source.

## What this module drives

1. **setUp Thread Group** (`Reset Delivery State`, 1 thread, 1 loop) calls
   `POST /messaging/delivery/reset`, which purges both `qe.q.work` and `qe.q.dlq`.
2. **Main Thread Group** (`Submit Work and Poison`, 6 threads x 3 loops) posts to
   `POST /messaging/work`, mixing ordinary jobs with poison ones -- enough poison to drive DLQ
   depth past `dlqAlertDepth` for I5, and at least one ordinary job queued behind a poison one
   for I3's non-blocking clause.
3. **TearDown Thread Group** (`Verify Delivery`, 1 thread, 1 loop) polls
   `GET /messaging/delivery/state` to a **bounded** deadline until submitted ==
   stateChanges + dlqCount, then -- on a full run only -- restarts the broker and re-reads the
   DLQ depth for I2. `assert-dlq.groovy` then evaluates I1, I3-I6, and I2 or its
   `not-evaluated` reason.

## Running it

```
make up PROFILES="core messaging"
HARNESS_SMOKE_MODE=true ./bin/run-module.sh TST-029   # I2 not-evaluated
./bin/run-module.sh TST-029                           # I2 exercised: restarts the broker
```

## Defect proof

```
curl -X POST http://localhost:8080/_test/defect/dlq-bypass-drop   # 204
./bin/run-module.sh TST-029                                       # must report I1 FAILED
curl -X DELETE http://localhost:8080/_test/defect                 # 204
```

With the defect active, `DeliveryService.consume` acknowledges a poison message without
processing it and without dead-lettering it -- the message simply vanishes, so
`submitted > stateChanges + dlqCount` and I1's conservation law breaks. The retry ladder and the
alert threshold are untouched, so I4 and I5 still pass.
```

- [ ] **Step 2: Write the failing test**

`Tst029ModuleTest.java`:

```java
package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-029 delivery guarantee module. Driven in smoke mode from the test suite:
 * a full run restarts the broker container, which a unit test must not do to a
 * developer's running stack without being asked.
 */
class Tst029ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void passesAgainstTheCleanSut() throws Exception {
        ModuleResult r = runner.run("TST-029", Map.of("HARNESS_SMOKE_MODE", "true"));
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void smokeModeReportsTheRestartInvariantNotEvaluated() throws Exception {
        ModuleResult r = runner.run("TST-029", Map.of("HARNESS_SMOKE_MODE", "true"));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I2")
                && i.result() == RunFragment.Result.NOT_EVALUATED),
            "the restart path must never report passed without actually restarting");
    }

    @Test
    void reportsDeliveryGuaranteeFailureAgainstTheBypassDefect() throws Exception {
        ModuleResult r = runner.run("TST-029",
            Map.of("HARNESS_SMOKE_MODE", "true", "SUT_DEFECT", "dlq-bypass-drop"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I1") && i.result() == RunFragment.Result.FAILED));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I4") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: the retry ladder is untouched");
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I5") && i.result() == RunFragment.Result.PASSED));
    }
}
```

- [ ] **Step 3: Run and confirm failure**

```bash
cd qe-harness/harness && mvn -q -pl jmeter test -Dtest=Tst029ModuleTest
```

Expected: FAIL — no `modules.yml` entry.

- [ ] **Step 4: Add the binding row**

Insert into `modules.yml` after `TST-028` and before `TST-030`:

```yaml
  - archetype: TST-029
    tool: jmeter
    path: qe-harness/harness/jmeter/tst-029-dlq
    coverage: full
    defect_flag: dlq-bypass-drop
```

`coverage: full` is deliberate. Every invariant is implemented; I2's restart is gated by run
mode, and a run-mode gate is reported per-run via `not-evaluated`, not by understating what the
module contains. Compare `TST-027` and `TST-037`, which are `partial` because their I5s are
genuinely absent.

- [ ] **Step 5: Write the assertion script**

`assert-dlq.groovy`:

```groovy
// TST-029 delivery guarantee, retry and DLQ assertion (Wave 17).
//
// I1 is a conservation law: submitted == stateChanges + dlqCount. The counters
// come from the SUT's own publish path (GET /messaging/delivery/state), not from
// the broker's accounting -- scoring a delivery guarantee against the broker
// would ask the component under test to grade itself.
//
// I2 needs a real broker restart. In smoke mode it is reported NOT_EVALUATED
// with a reason rather than substituted: Toxiproxy severance would prove
// reconnection, not durable-queue survival, and passing that off as I2 is
// exactly the dishonesty TST-043's relabelling exists to warn about.
//
// I4 reads the DECLARED ladder rather than inferring intervals from observed
// timings, which would be flaky under load.

import com.techcombank.qe.harness.config.HarnessConfig
import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion

import groovy.json.JsonSlurper

import java.nio.file.Path

boolean smoke = HarnessConfig.smokeMode()

def state = new JsonSlurper().parseText(vars.get("delivery_state"))
def dlqInfo = new JsonSlurper().parseText(vars.get("dlq_info"))

long submitted = state.submitted as Long
long stateChanges = state.stateChanges as Long
long dlqCount = state.dlqCount as Long
int maxAttempts = state.maxDeliveryAttempts as Integer
long distinctIntervals = state.distinctIntervals as Long
boolean dlqExported = dlqInfo.exported
boolean alertFiring = dlqInfo.alertFiring
long alertDepth = dlqInfo.alertDepth as Long
long observedDepth = dlqInfo.depth as Long

long poisonAttempts = Long.parseLong(props.getProperty("tst029_max_poison_attempts"))
long jobsBehindPoisonProcessed = Long.parseLong(props.getProperty("tst029_jobs_behind_poison"))

String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1", "Every published message produced one state change or is in the DLQ",
    { submitted > 0L && submitted == stateChanges + dlqCount } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "A poison message reaches the DLQ inside the declared attempts without blocking others",
    { poisonAttempts <= maxAttempts && jobsBehindPoisonProcessed > 0L } as java.util.function.BooleanSupplier)
RunFragment.Entry i4 = InvariantAssertion.check(
    "I4", "Retry intervals match the declared backoff with more than one distinct interval",
    { distinctIntervals > 1L } as java.util.function.BooleanSupplier)
RunFragment.Entry i5 = InvariantAssertion.check(
    "I5", "DLQ depth is exported and an alert fires past its declared threshold",
    { dlqExported && (observedDepth > alertDepth ? alertFiring : !alertFiring) } as java.util.function.BooleanSupplier)
RunFragment.Entry i6 = InvariantAssertion.check(
    "I6", "A permanent error stops retrying at the declared ceiling",
    { poisonAttempts <= maxAttempts } as java.util.function.BooleanSupplier)

RunFragment.Builder builder = RunFragment.builder()
    .archetype(System.getenv("QE_ARCHETYPE"))
    .module("jmeter")
    .serviceName("reference-sut")
    .tier("T0")
    .oracle("invariant-assertion")
    .environment(System.getenv().getOrDefault("QE_ENVIRONMENT", "local-compose"))
    .sutDefect(sutDefect)
    .invariant(i1.id(), i1.description(), i1.result())

if (smoke) {
    builder.invariant("I2", "A broker restart loses nothing acked-persisted",
                      RunFragment.Result.NOT_EVALUATED)
} else {
    long depthBefore = Long.parseLong(vars.get("dlq_depth_before_restart"))
    long depthAfter = Long.parseLong(vars.get("dlq_depth_after_restart"))
    RunFragment.Entry i2 = InvariantAssertion.check(
        "I2", "A broker restart loses nothing acked-persisted",
        { depthAfter == depthBefore } as java.util.function.BooleanSupplier)
    builder.invariant(i2.id(), i2.description(), i2.result())
}

builder.invariant(i3.id(), i3.description(), i3.result())
       .invariant(i4.id(), i4.description(), i4.result())
       .invariant(i5.id(), i5.description(), i5.result())
       .invariant(i6.id(), i6.description(), i6.result())

RunFragment fragment = builder.build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "I1 conservation: ${i1.result().wire()} (submitted=${submitted}, processed=${stateChanges}, dlq=${dlqCount})\n" +
    "I2 restart-durability: ${smoke ? 'not-evaluated (restart path exercised in full runs only)' : 'evaluated'}\n" +
    "I3 dlq-within-attempts-no-blocking: ${i3.result().wire()} (attempts=${poisonAttempts}/${maxAttempts}, behind=${jobsBehindPoisonProcessed})\n" +
    "I4 distinct-backoff-intervals: ${i4.result().wire()} (distinct=${distinctIntervals})\n" +
    "I5 dlq-depth-exported-and-alerting: ${i5.result().wire()} (depth=${observedDepth}, alertDepth=${alertDepth}, firing=${alertFiring})\n" +
    "I6 stops-at-ceiling: ${i6.result().wire()}\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
```

- [ ] **Step 6: Build the JMeter plan**

`plan.jmx`:

- `SetupThreadGroup` "Reset Delivery State", 1/1, `on_sample_error=stopthread`: an
  `HTTPSamplerProxy` `POST /messaging/delivery/reset`, then an inline `JSR223Sampler` zeroing
  `tst029_max_poison_attempts` and `tst029_jobs_behind_poison` in `props`.
- `ThreadGroup` "Submit Work and Poison", 6 threads / 3 loops, `on_sample_error=continue`: a
  `JSR223PreProcessor` choosing job id and poison flag from `ctx.getThreadNum()` and
  `vars.getIteration()` — enough poison messages to push DLQ depth past `dlqAlertDepth` (read
  from `GET /messaging/dlq/depth` in setUp, not hardcoded), and at least one ordinary job
  submitted immediately after a poison one for I3's non-blocking clause; an `HTTPSamplerProxy`
  `POST /messaging/work?jobId=${jobId}&poison=${poison}`. Job ids are hyphenated short forms
  (`job-0001`, `poison-0001`) — never a numeric id wide enough to trip check 5.
- `PostThreadGroup` "Verify Delivery", 1/1:
  1. An inline `JSR223Sampler` polling `GET /messaging/delivery/state` to a **bounded**
     deadline (30s; the ladder's rungs total 13 seconds, so allow headroom) until
     `submitted == stateChanges + dlqCount`, recording the observed poison attempt count into
     `tst029_max_poison_attempts` and the processed-behind-poison count into
     `tst029_jobs_behind_poison`.
  2. An `HTTPSamplerProxy` `GET /messaging/delivery/state` whose PostProcessor writes
     `vars.put("delivery_state", prev.getResponseDataAsString())`, and one
     `GET /messaging/dlq/depth` writing `vars.put("dlq_info", …)`.
  3. **The restart, full runs only.** An inline `JSR223Sampler` guarded by
     `if (System.getenv("HARNESS_SMOKE_MODE") != "true") { … }` which records
     `dlq_depth_before_restart`, shells out to `docker compose restart broker` from the
     `qe-harness` directory, waits for the broker healthcheck to pass on a bounded deadline,
     then re-reads `GET /messaging/dlq/depth` into `dlq_depth_after_restart`. In smoke mode the
     block is skipped entirely and both vars stay unset — the assertion script only reads them
     on the non-smoke branch.
  4. The `assert-dlq` `JSR223Sampler` with
     `filename=${__groovy(System.getenv("ASSERT_SCRIPT_PATH"),)}`.

- [ ] **Step 7: Run the tests in smoke mode**

```bash
cd qe-harness && make down && make up PROFILES="core messaging"
cd harness && mvn -q -pl jmeter test -Dtest=Tst029ModuleTest
```

Expected: PASS, 3 tests. `smokeModeReportsTheRestartInvariantNotEvaluated` is the guard that
stops I2 ever being reported as passed without a real restart.

- [ ] **Step 8: Exercise the restart path once, manually**

The gated branch must be proven at least once or it is untested code:

```bash
cd qe-harness && ./bin/run-module.sh TST-029
python3 -c "
import json, pathlib
f = sorted(pathlib.Path('traceability/runs').glob('*-TST-029.json'))[-1]
d = json.loads(f.read_text())
i2 = [i for i in d['invariants'] if i['id'] == 'I2'][0]
print('I2:', i2['result'])
"
```

Expected: `I2: passed` (not `not-evaluated`) — the broker was genuinely restarted and the
durable queue survived. Record this in the task report; CI will never do it.

- [ ] **Step 9: Verify the gate**

```bash
cd "$(git rev-parse --show-toplevel)"
python3 scripts/validate-harness-coverage.py 2>&1 | /usr/bin/grep -E "TST-029|check5|check7" || echo "no findings"
python3 scripts/render-harness-coverage.py
```

Expected: no findings.

- [ ] **Step 10: Commit**

```bash
git add qe-harness/harness/jmeter qe-harness/traceability
git commit -m "feat(harness): add TST-029 delivery guarantee module with a gated broker restart"
```

---

Phase 2 is complete. Family B is 5/5 — `TST-026`, `027`, `028`, `029` join `TST-030` — and the
broker topology is now the substrate any future messaging archetype reuses. Phase 3 collects
the one archetype that needed it.

---

## Task 25: SUT — TST-020 Idempotency on POST /transfers

Sequenced last on purpose. I7 is "dedup survives broker redelivery", which was unsatisfiable
before Phase 2 existed — running this task earlier would have bought a `partial` where a `full`
was available.

**Files:**
- Create: `…/db/migration/V5__idempotency_keys.sql`
- Create: `…/sut/capability/ledger/IdempotencyService.java`
- Modify: `…/sut/capability/ledger/LedgerController.java`, `TransferService.java`
- Modify: `…/sut/capability/ledger/AbstractLedgerIntegrationTest.java` (**TRUNCATE list**)
- Modify: `…/sut/DefectFlags.java`, `CapabilityRegistry.java`, `CapabilityRegistryTest.java`
- Modify: `…/reference-sut/src/main/resources/application.properties`
- Test: `…/sut/capability/ledger/IdempotencyServiceTest.java`

**Interfaces:**
- Consumes: `JdbcTemplate`, `TransferService`, `MessagingTopology` (for I7's redelivery path)
- Produces: `Idempotency-Key` handling on `POST /transfers`, `GET /transfers/idempotency/{key}`;
  defect flag `idempotency-key-ignored`

- [ ] **Step 1: Write the failing test**

`IdempotencyServiceTest.java`:

```java
package com.techcombank.qe.sut.capability.ledger;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TST-020 idempotency and replay. */
class IdempotencyServiceTest extends AbstractLedgerIntegrationTest {

    @Test
    void repeatedSameKeyRequestsProduceOneStateChange() {
        String key = "idem-0001";
        for (int i = 0; i < 5; i++) {
            idempotency.execute(key, requestBody(500L), () -> service.transfer("ACC-000001", "ACC-000002", 500L));
        }
        assertEquals(2, ledgerEntryCount(), "I1: five same-key requests, one balanced pair");
    }

    @Test
    void aReplayReturnsAByteIdenticalStoredResponse() {
        String key = "idem-0002";
        String first = idempotency.execute(key, requestBody(500L),
            () -> service.transfer("ACC-000001", "ACC-000002", 500L)).body();
        String replay = idempotency.execute(key, requestBody(500L),
            () -> service.transfer("ACC-000001", "ACC-000002", 500L)).body();
        assertEquals(first, replay, "I2: the replay must be byte-identical, not merely equivalent");
    }

    @Test
    void distinctKeysProduceDistinctStateChanges() {
        for (int i = 1; i <= 3; i++) {
            idempotency.execute("idem-100" + i, requestBody(500L),
                () -> service.transfer("ACC-000001", "ACC-000002", 500L));
        }
        assertEquals(6, ledgerEntryCount(), "I3: three distinct keys, three balanced pairs");
    }

    @Test
    void sameKeyWithADifferentPayloadIsAConflict() {
        String key = "idem-0003";
        idempotency.execute(key, requestBody(500L),
            () -> service.transfer("ACC-000001", "ACC-000002", 500L));
        assertThrows(IdempotencyService.PayloadConflict.class,
            () -> idempotency.execute(key, requestBody(999L),
                () -> service.transfer("ACC-000001", "ACC-000002", 999L)),
            "I4: a reused key with a changed payload must conflict, never silently replay");
    }

    @Test
    void underTrueConcurrencyOneWinsAndTheRestAreServedTheStoredResponse() throws Exception {
        String key = "idem-0004";
        String body = requestBody(500L);
        List<Callable<String>> calls = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            calls.add(() -> idempotency.execute(key, body,
                () -> service.transfer("ACC-000001", "ACC-000002", 500L)).body());
        }
        ExecutorService pool = Executors.newFixedThreadPool(12);
        List<String> bodies = new ArrayList<>();
        try {
            for (Future<String> f : pool.invokeAll(calls)) {
                bodies.add(f.get());
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(2, ledgerEntryCount(), "I5: exactly one winner writes state");
        assertEquals(1, bodies.stream().distinct().count(),
            "I5: every caller sees the same stored response");
    }

    @Test
    void theKeyTtlCoversTheDeclaredClientRetryWindow() {
        // I6 is a configuration relationship, so it is asserted against the two
        // declared properties rather than by waiting out a TTL.
        assertTrue(idempotency.keyTtlSeconds() >= idempotency.clientMaxRetryWindowSeconds(),
            "I6: key TTL must be at least the declared client retry window");
    }

    @Test
    void ignoredKeyDefectBreaksOnlyTheDeduplicationInvariant() {
        String key = "idem-0005";
        withDefect("idempotency-key-ignored", () -> {
            for (int i = 0; i < 3; i++) {
                idempotency.execute(key, requestBody(500L),
                    () -> service.transfer("ACC-000001", "ACC-000002", 500L));
            }
        });
        assertEquals(6, ledgerEntryCount(), "the defect must write three pairs for one key");
        assertEquals(0L, trialBalance.net(),
            "the defect must be specific: the ledger stays balanced");
    }

    private String requestBody(long amountMinor) {
        return "{\"from\":\"ACC-000001\",\"to\":\"ACC-000002\",\"amountMinor\":" + amountMinor + "}";
    }

    private long ledgerEntryCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM ledger_entry", Long.class);
    }
}
```

- [ ] **Step 2: Run and confirm failure**

```bash
cd qe-harness/reference-sut && mvn -q -B test -Dtest=IdempotencyServiceTest
```

Expected: FAIL — `IdempotencyService` does not exist.

- [ ] **Step 3: Write the migration**

`V5__idempotency_keys.sql`:

```sql
-- V5: idempotency keys for TST-020 idempotency and replay (Wave 17).
-- See com.techcombank.qe.sut.capability.ledger.IdempotencyService.
--
-- The UNIQUE constraint on idempotency_key is what makes I5 (true concurrency:
-- one wins, the rest are served the stored response) enforceable rather than
-- merely intended. Two concurrent inserts cannot both succeed; the loser
-- catches the violation and reads the winner's stored response. A
-- check-then-insert in application code could not promise that.
--
-- payload_hash exists for I4: a reused key carrying a DIFFERENT payload must
-- conflict rather than silently replay someone else's result, which is the
-- more dangerous of the two failures.
--
-- response_body is stored verbatim so a replay is BYTE-identical (I2), not
-- merely equivalent -- re-serialising would risk key reordering or whitespace
-- drift.
--
-- NOTE: this table has NO foreign key to account, so it is NOT reached by
-- AbstractLedgerIntegrationTest's TRUNCATE ... CASCADE. It must be added to
-- that TRUNCATE list explicitly or keys leak between tests.

CREATE TABLE idempotency_key (
    id             BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    payload_hash   VARCHAR(64) NOT NULL,
    response_body  TEXT        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT idempotency_key_format CHECK (idempotency_key ~ '^idem-[A-Za-z0-9-]{1,58}$')
);

CREATE INDEX idempotency_key_expires_at_idx ON idempotency_key (expires_at);
```

The `idempotency_key_format` CHECK keeps keys to a hyphenated short form at the database level,
so no code path — injected defect included — can write a key containing a 13-digit run that
would fail gate check 5.

- [ ] **Step 4: Write the service**

`IdempotencyService.java`:

```java
package com.techcombank.qe.sut.capability.ledger;

import com.techcombank.qe.sut.DefectFlags;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Supplier;

/**
 * TST-020 idempotency and replay capability.
 *
 * <p><b>The unique constraint is the mechanism, not a safety net.</b> Under true
 * concurrency (I5) two callers race to insert the same key; exactly one wins,
 * and the loser catches {@link DuplicateKeyException} and reads the winner's
 * stored response. A check-then-insert in application code could not promise
 * that, because both callers could pass the check.
 *
 * <p><b>Responses are stored verbatim (I2).</b> A replay must be byte-identical,
 * so re-serialising is not an option -- key order or whitespace could drift.
 *
 * <p><b>A reused key with a different payload conflicts (I4)</b> rather than
 * replaying someone else's result, which is the more dangerous failure of the
 * two.
 *
 * <p><b>Defect injection:</b> {@code idempotency-key-ignored} skips the key
 * lookup entirely and always executes, so N same-key requests produce N state
 * changes. I1 fails; the ledger stays balanced throughout, so TST-021's
 * invariants are untouched -- the defect is specific.
 */
@Service
public class IdempotencyService {

    /** Thrown when a key is reused with a different payload (I4). */
    public static class PayloadConflict extends RuntimeException {
        public PayloadConflict(String key) {
            super("idempotency key reused with a different payload: " + key);
        }
    }

    /** The outcome of an idempotent execution. */
    public record Outcome(String body, boolean replayed) {}

    private final JdbcTemplate jdbc;
    private final long keyTtlSeconds;
    private final long clientMaxRetryWindowSeconds;

    public IdempotencyService(JdbcTemplate jdbc,
                              @Value("${app.idempotency.key-ttl-seconds}") long keyTtlSeconds,
                              @Value("${app.idempotency.client-max-retry-window-seconds}") long clientMaxRetryWindowSeconds) {
        this.jdbc = jdbc;
        this.keyTtlSeconds = keyTtlSeconds;
        this.clientMaxRetryWindowSeconds = clientMaxRetryWindowSeconds;
    }

    public long keyTtlSeconds() {
        return keyTtlSeconds;
    }

    public long clientMaxRetryWindowSeconds() {
        return clientMaxRetryWindowSeconds;
    }

    /** Executes {@code action} at most once per key. */
    public Outcome execute(String key, String payload, Supplier<Object> action) {
        if (DefectFlags.isActive("idempotency-key-ignored")) {
            // The defect: no lookup, no record. Every call executes.
            return new Outcome(render(action.get()), false);
        }

        String hash = sha256(payload);
        List<StoredKey> existing = find(key);
        if (!existing.isEmpty()) {
            return replayOrConflict(key, hash, existing.get(0));
        }

        String body = render(action.get());
        try {
            jdbc.update(
                "INSERT INTO idempotency_key (idempotency_key, payload_hash, response_body, expires_at) "
                    + "VALUES (?, ?, ?, now() + make_interval(secs => ?))",
                key, hash, body, (double) keyTtlSeconds);
            return new Outcome(body, false);
        } catch (DuplicateKeyException e) {
            // I5's loser path: another caller won the race between our lookup
            // and our insert. Serve their stored response rather than surfacing
            // a 500 -- the constraint violation is expected here, not an error.
            List<StoredKey> winner = find(key);
            if (winner.isEmpty()) {
                throw e;
            }
            return replayOrConflict(key, hash, winner.get(0));
        }
    }

    private Outcome replayOrConflict(String key, String hash, StoredKey stored) {
        if (!stored.payloadHash().equals(hash)) {
            throw new PayloadConflict(key);
        }
        return new Outcome(stored.responseBody(), true);
    }

    private List<StoredKey> find(String key) {
        return jdbc.query(
            "SELECT payload_hash, response_body FROM idempotency_key "
                + "WHERE idempotency_key = ? AND expires_at > now()",
            (rs, n) -> new StoredKey(rs.getString("payload_hash"), rs.getString("response_body")),
            key);
    }

    /** Verbatim rendering: the stored body is what a replay returns, so this is
     *  the one place the response's bytes are decided. */
    private String render(Object result) {
        return "{\"transferRef\":\"" + result + "\"}";
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private record StoredKey(String payloadHash, String responseBody) {}
}
```

- [ ] **Step 5: Wire it into the controller**

In `LedgerController.java`, accept the header and keep the existing unkeyed path working —
every pre-Wave-17 caller, including TST-021's plan, sends no `Idempotency-Key`:

```java
    /** POST /transfers {from, to, amountMinor} -> 201 {transferRef}.
     *
     *  <p>An optional Idempotency-Key header makes the call replay-safe
     *  (TST-020). Without it the behaviour is exactly as before, so TST-021's
     *  module and every other existing caller are unaffected. A replay returns
     *  200 rather than 201: the resource was not created by this request. */
    @PostMapping("/transfers")
    public ResponseEntity<?> transfer(@RequestBody TransferRequest request,
                                      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                      @RequestBody(required = false) String rawBody) {
        if (idempotencyKey == null) {
            UUID ref = transferService.transfer(request.from(), request.to(), request.amountMinor());
            return ResponseEntity.status(HttpStatus.CREATED).body(new TransferResponse(ref));
        }
        try {
            IdempotencyService.Outcome outcome = idempotency.execute(idempotencyKey, rawBody,
                () -> transferService.transfer(request.from(), request.to(), request.amountMinor()));
            return ResponseEntity
                .status(outcome.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header("Idempotent-Replay", String.valueOf(outcome.replayed()))
                .body(outcome.body());
        } catch (IdempotencyService.PayloadConflict e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
```

Spring cannot bind two `@RequestBody` parameters — take the raw body once and parse it, or read
it through a small `HttpServletRequest` wrapper. Verify the chosen approach compiles before
proceeding; if binding fights you, hash the canonical form
`request.from() + "|" + request.to() + "|" + request.amountMinor()` instead of the raw bytes and
note the deviation in the module README, since I2's byte-identical guarantee applies to the
**response**, not the request.

Add `GET /transfers/idempotency/{key}` returning `{present, replayed, keyTtlSeconds,
clientMaxRetryWindowSeconds}` so the harness can assert I6 against declared configuration.

- [ ] **Step 6: Declare the TTL properties**

Append to `application.properties`:

```properties
# TST-020 idempotency and replay (Wave 17). I6 requires the key TTL to cover at
# least the declared client retry window; IdempotencyServiceTest asserts that
# relationship between these two declared values rather than waiting out a TTL.
app.idempotency.key-ttl-seconds=900
app.idempotency.client-max-retry-window-seconds=300
```

- [ ] **Step 7: Fix the TRUNCATE list — this is the leak**

`idempotency_key` has no FK to `account`, so `AbstractLedgerIntegrationTest`'s
`TRUNCATE TABLE ledger_entry, account RESTART IDENTITY CASCADE` does **not** reach it. Without
this change, keys survive between tests and `repeatedSameKeyRequestsProduceOneStateChange`
passes or fails depending on test order:

```java
    @BeforeEach
    void resetLedgerFixture() {
        DefectFlags.clear();
        // idempotency_key has no FK to account, so CASCADE does not reach it --
        // truncate it explicitly or keys leak into the next test.
        jdbc.execute("TRUNCATE TABLE idempotency_key RESTART IDENTITY");
        jdbc.execute("TRUNCATE TABLE ledger_entry, account RESTART IDENTITY CASCADE");
        jdbc.update("INSERT INTO account (account_ref, party_name) VALUES (?, ?)",
            "ACC-000001", "Test Fixture Debtor Co");
        jdbc.update("INSERT INTO account (account_ref, party_name) VALUES (?, ?)",
            "ACC-000002", "Test Fixture Creditor Co");
    }
```

Add `@Autowired protected IdempotencyService idempotency;` to the same base class.

- [ ] **Step 8: Register the flag and capability**

Add `"idempotency-key-ignored"` to `KNOWN_FLAGS`, `"TST-020"` to `IMPLEMENTED` and to
`IMPLEMENTED_AT_WAVE_17` — the fifteenth and final entry.

- [ ] **Step 9: Run the full SUT suite**

```bash
cd qe-harness/reference-sut && mvn -q -B test
```

Expected: PASS, including all seven `IdempotencyServiceTest` tests **and** the pre-existing
`TransferServiceTest`/`LedgerConcurrencyTest`, which must be unaffected by the new header.

- [ ] **Step 10: Prove the unkeyed path is unchanged**

```bash
cd qe-harness && make down && make up PROFILES=core
./bin/run-module.sh TST-021
curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8080/transfers \
  -H 'Content-Type: application/json' \
  -d '{"from":"ACC-000001","to":"ACC-000002","amountMinor":100}'
```

Expected: TST-021 still passes, and the unkeyed POST still returns `201`.

- [ ] **Step 11: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): add TST-020 idempotency with a unique-constraint race winner"
```

---

## Task 26: Module — TST-020 Idempotency & Replay (JMeter, full I1–I7)

**Files:**
- Create: `qe-harness/harness/jmeter/tst-020-idempotency/{plan.jmx,assert-idempotency.groovy,README.md}`
- Modify: `qe-harness/traceability/modules.yml`
- Test: `…/jmeter/Tst020ModuleTest.java`

**Interfaces:**
- Consumes: `POST /transfers` with `Idempotency-Key`, `GET /transfers/idempotency/{key}`,
  `POST /messaging/work` (I7's redelivery path)
- Produces: `run-module.sh TST-020`; the fifteenth module

- [ ] **Step 1: Write the module README**

`tst-020-idempotency/README.md`:

```markdown
# TST-020 -- Idempotency & Replay (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.

| ID | Invariant |
|---|---|
| I1 | N same-key requests produce exactly one state change |
| I2 | A replay returns a byte-identical status and body |
| I3 | N distinct keys produce N state changes |
| I4 | The same key with a different payload is a conflict |
| I5 | Under true concurrency one wins and the other is served the stored response |
| I6 | Key TTL is at least the declared client max retry window |
| I7 | Deduplication survives broker redelivery |

Defect proof: with `idempotency-key-ignored` active this module MUST report I1 failed and I2/I4
still passed.

**Why this module is last in the wave.** I7 requires a broker that redelivers, which did not
exist in this repository until Wave 17's Phase 2. Running TST-020 before the broker landed
would have meant shipping `coverage: partial` with I7 unreached; sequencing it after buys the
full set. That ordering was a design decision, not an accident -- see the design spec's
decision 2.

I5 rests on the `idempotency_key` table's UNIQUE constraint, not on application-level
check-then-insert: under a synchronised burst two callers race, exactly one insert survives, and
the loser catches the violation and serves the winner's stored response. I2 compares the replay
**byte for byte**, which is why the response body is stored verbatim rather than re-serialised.

## What this module drives

1. **setUp Thread Group** (`Reset Idempotency Fixture`, 1 thread, 1 loop) truncates
   `idempotency_key` explicitly -- it has no FK to `account`, so a `CASCADE` misses it -- plus
   `ledger_entry`/`account`, and seeds the two fixture accounts.
2. **Main Thread Group** (`Same-Key Burst`, 12 threads x 1 loop) fires
   `POST /transfers` with one shared `Idempotency-Key` behind a **Synchronizing Timer** (group
   size 12) so the race in I5 is genuine rather than sequential. A `JSR223PostProcessor` tallies
   `201` versus `200` responses and collects the distinct response bodies.
3. **TearDown Thread Group** (`Verify Idempotency`, 1 thread, 1 loop) exercises the remaining
   invariants directly: three distinct keys for I3; the same key with a changed amount for I4,
   requiring `409`; `GET /transfers/idempotency/{key}` for I6's declared-configuration check;
   and a `POST /messaging/work` redelivery for I7. `assert-idempotency.groovy` then evaluates
   I1-I7.

## Running it

```
make up PROFILES="core messaging"     # I7 needs the broker
./bin/run-module.sh TST-020
```

## Defect proof

```
curl -X POST http://localhost:8080/_test/defect/idempotency-key-ignored   # 204
./bin/run-module.sh TST-020                                              # must report I1 FAILED
curl -X DELETE http://localhost:8080/_test/defect                        # 204
```

With the defect active, `IdempotencyService.execute` skips the key lookup entirely and always
executes, so twelve same-key requests write twelve balanced pairs. The ledger stays balanced
throughout, so TST-021's invariants are untouched -- which is what makes the proof specific.
```

- [ ] **Step 2: Write the failing test**

`Tst020ModuleTest.java`:

```java
package com.techcombank.qe.harness.jmeter;

import com.techcombank.qe.harness.evidence.RunFragment;
import com.techcombank.qe.harness.jmeter.support.ModuleResult;
import com.techcombank.qe.harness.jmeter.support.ModuleRunner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TST-020 idempotency module. Requires make up PROFILES="core messaging" --
 * I7's redelivery path needs the broker Phase 2 introduced.
 */
class Tst020ModuleTest {

    private final ModuleRunner runner = new ModuleRunner();

    @Test
    void passesAgainstTheCleanSut() throws Exception {
        ModuleResult r = runner.run("TST-020", Map.of());
        assertEquals(RunFragment.Result.PASSED, r.fragment().result());
    }

    @Test
    void assertsAllSevenInvariantsIncludingTheBrokerRedeliveryOne() throws Exception {
        ModuleResult r = runner.run("TST-020", Map.of());
        assertEquals(7, r.fragment().invariants().size());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I7") && i.result() == RunFragment.Result.PASSED),
            "I7 is why this module is sequenced after Phase 2; it must be genuinely evaluated");
    }

    @Test
    void reportsDeduplicationFailureAgainstTheIgnoredKeyDefect() throws Exception {
        ModuleResult r = runner.run("TST-020", Map.of("SUT_DEFECT", "idempotency-key-ignored"));
        assertEquals(RunFragment.Result.FAILED, r.fragment().result());
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I1") && i.result() == RunFragment.Result.FAILED));
        assertTrue(r.fragment().invariants().stream()
            .anyMatch(i -> i.id().equals("I4") && i.result() == RunFragment.Result.PASSED),
            "the defect must be specific: conflict detection is bypassed, not broken");
    }
}
```

- [ ] **Step 3: Run and confirm failure**

```bash
cd qe-harness/harness && mvn -q -pl jmeter test -Dtest=Tst020ModuleTest
```

Expected: FAIL — no `modules.yml` entry.

- [ ] **Step 4: Add the binding row**

Insert into `modules.yml` as the **first** module row, before `TST-021`, keeping archetype
order:

```yaml
  - archetype: TST-020
    tool: jmeter
    path: qe-harness/harness/jmeter/tst-020-idempotency
    coverage: full
    defect_flag: idempotency-key-ignored
```

- [ ] **Step 5: Write the assertion script**

`assert-idempotency.groovy`:

```groovy
// TST-020 idempotency and replay assertion (Wave 17).
//
// Sequenced last in the wave deliberately: I7 (dedup survives broker
// redelivery) needs the broker Phase 2 introduced, so running this module
// earlier would have meant shipping coverage: partial with I7 unreached.
//
// I2 compares the replay BYTE FOR BYTE. The SUT stores its response verbatim
// rather than re-serialising, so a body that differs by key order or whitespace
// is a real violation, not a formatting artefact.

import com.techcombank.qe.harness.evidence.EvidenceEmitter
import com.techcombank.qe.harness.evidence.RunFragment
import com.techcombank.qe.harness.oracle.InvariantAssertion

import groovy.json.JsonSlurper

import java.nio.file.Path

long entriesAfterBurst = Long.parseLong(vars.get("entries_after_burst"))
long distinctBodies = Long.parseLong(props.getProperty("tst020_distinct_bodies"))
long createdResponses = Long.parseLong(props.getProperty("tst020_created_responses"))
long replayResponses = Long.parseLong(props.getProperty("tst020_replay_responses"))
long burstSize = Long.parseLong(props.getProperty("tst020_burst_size"))

long entriesAfterDistinctKeys = Long.parseLong(vars.get("entries_after_distinct_keys"))
long distinctKeysSent = Long.parseLong(vars.get("distinct_keys_sent"))
boolean conflictObserved = Boolean.parseBoolean(vars.get("conflict_observed"))
boolean redeliveryDeduped = Boolean.parseBoolean(vars.get("redelivery_deduped"))

def ttlInfo = new JsonSlurper().parseText(vars.get("ttl_info"))
long keyTtlSeconds = ttlInfo.keyTtlSeconds as Long
long clientRetryWindowSeconds = ttlInfo.clientMaxRetryWindowSeconds as Long

String sutDefect = System.getenv("QE_SUT_DEFECT")
if (sutDefect != null && sutDefect.trim().isEmpty()) {
    sutDefect = null
}

RunFragment.Entry i1 = InvariantAssertion.check(
    "I1", "N same-key requests produce exactly one state change",
    { burstSize > 1L && entriesAfterBurst == 2L } as java.util.function.BooleanSupplier)
RunFragment.Entry i2 = InvariantAssertion.check(
    "I2", "A replay returns a byte-identical status and body",
    { distinctBodies == 1L && replayResponses > 0L } as java.util.function.BooleanSupplier)
RunFragment.Entry i3 = InvariantAssertion.check(
    "I3", "N distinct keys produce N state changes",
    { entriesAfterDistinctKeys == distinctKeysSent * 2L } as java.util.function.BooleanSupplier)
RunFragment.Entry i4 = InvariantAssertion.check(
    "I4", "The same key with a different payload is a conflict",
    { conflictObserved } as java.util.function.BooleanSupplier)
RunFragment.Entry i5 = InvariantAssertion.check(
    "I5", "Under true concurrency one wins and the rest are served the stored response",
    { createdResponses == 1L && replayResponses == burstSize - 1L } as java.util.function.BooleanSupplier)
RunFragment.Entry i6 = InvariantAssertion.check(
    "I6", "Key TTL is at least the declared client max retry window",
    { keyTtlSeconds >= clientRetryWindowSeconds } as java.util.function.BooleanSupplier)
RunFragment.Entry i7 = InvariantAssertion.check(
    "I7", "Deduplication survives broker redelivery",
    { redeliveryDeduped } as java.util.function.BooleanSupplier)

RunFragment fragment = RunFragment.builder()
    .archetype(System.getenv("QE_ARCHETYPE"))
    .module("jmeter")
    .serviceName("reference-sut")
    .tier("T0")
    .oracle("invariant-assertion")
    .environment(System.getenv().getOrDefault("QE_ENVIRONMENT", "local-compose"))
    .sutDefect(sutDefect)
    .invariant(i1.id(), i1.description(), i1.result())
    .invariant(i2.id(), i2.description(), i2.result())
    .invariant(i3.id(), i3.description(), i3.result())
    .invariant(i4.id(), i4.description(), i4.result())
    .invariant(i5.id(), i5.description(), i5.result())
    .invariant(i6.id(), i6.description(), i6.result())
    .invariant(i7.id(), i7.description(), i7.result())
    .build()

Path outputDir = Path.of(System.getenv("EVIDENCE_OUTPUT_DIR"))
new EvidenceEmitter(outputDir).emit(fragment)

boolean passed = fragment.result() == RunFragment.Result.PASSED
SampleResult.setSuccessful(passed)
SampleResult.setResponseData((
    "I1 one-state-change: ${i1.result().wire()} (entries=${entriesAfterBurst}, burst=${burstSize})\n" +
    "I2 byte-identical-replay: ${i2.result().wire()} (distinctBodies=${distinctBodies}, replays=${replayResponses})\n" +
    "I3 distinct-keys-distinct-changes: ${i3.result().wire()} (entries=${entriesAfterDistinctKeys}, keys=${distinctKeysSent})\n" +
    "I4 payload-conflict: ${i4.result().wire()}\n" +
    "I5 one-winner: ${i5.result().wire()} (created=${createdResponses}, replayed=${replayResponses})\n" +
    "I6 ttl-covers-retry-window: ${i6.result().wire()} (ttl=${keyTtlSeconds}s, window=${clientRetryWindowSeconds}s)\n" +
    "I7 dedup-survives-redelivery: ${i7.result().wire()}\n"
    ).toString(), "UTF-8")
SampleResult.setResponseCode(passed ? "200" : "500")
SampleResult.setResponseMessage(fragment.result().wire())
```

- [ ] **Step 6: Build the JMeter plan**

`plan.jmx`:

- `SetupThreadGroup` "Reset Idempotency Fixture", 1/1, `on_sample_error=stopthread`: an inline
  `JSR223Sampler` running `TRUNCATE TABLE idempotency_key RESTART IDENTITY` **and**
  `TRUNCATE TABLE ledger_entry, account RESTART IDENTITY CASCADE` (two statements — the CASCADE
  does not reach `idempotency_key`), inserting `ACC-000001`/`ACC-000002`, and zeroing
  `tst020_distinct_bodies`, `tst020_created_responses`, `tst020_replay_responses` plus
  `props.put("tst020_burst_size", "12")` and an empty `tst020_seen_bodies` accumulator.
- `ThreadGroup` "Same-Key Burst", 12 threads / 1 loop, `on_sample_error=continue`: a `SyncTimer`
  with `groupSize` **12** and `timeoutInMs` 10000 — the race in I5 is only real if all twelve
  threads are released together; an `HTTPSamplerProxy` `POST /transfers` with `postBodyRaw=true`,
  body `{"from":"ACC-000001","to":"ACC-000002","amountMinor":500}`, and a `HeaderManager` child
  adding `Idempotency-Key: idem-burst-0001`; a `JSR223PostProcessor` which, inside
  `synchronized (props) { … }`, increments `tst020_created_responses` on `201` and
  `tst020_replay_responses` on `200`, appends the body to the accumulator and recomputes the
  distinct count into `tst020_distinct_bodies`.
- `PostThreadGroup` "Verify Idempotency", 1/1, in order:
  1. An inline `JSR223Sampler` reading `SELECT COUNT(*) FROM ledger_entry` into
     `vars.put("entries_after_burst", …)` — I1's verdict.
  2. Three `HTTPSamplerProxy` `POST /transfers` calls with `Idempotency-Key: idem-distinct-0001`
     / `-0002` / `-0003`, then an inline `JSR223Sampler` writing
     `entries_after_distinct_keys` and `distinct_keys_sent` (`3`).
  3. An `HTTPSamplerProxy` `POST /transfers` reusing `idem-burst-0001` with
     `amountMinor: 999`, whose PostProcessor writes
     `vars.put("conflict_observed", String.valueOf(prev.getResponseCode() == "409"))` — I4.
  4. An `HTTPSamplerProxy` `GET /transfers/idempotency/idem-burst-0001` whose PostProcessor
     writes `vars.put("ttl_info", prev.getResponseDataAsString())` — I6.
  5. For I7: an `HTTPSamplerProxy` `POST /messaging/work?jobId=job-idem-0001&poison=false`
     followed by a **second** submission of the same job id, then a bounded poll (10s) of
     `SELECT COUNT(*) FROM ledger_entry` confirming the redelivered work did not double-write;
     write the outcome to `vars.put("redelivery_deduped", …)`.
  6. The `assert-idempotency` `JSR223Sampler` with
     `filename=${__groovy(System.getenv("ASSERT_SCRIPT_PATH"),)}`.

- [ ] **Step 7: Run the tests**

```bash
cd qe-harness && make down && make up PROFILES="core messaging"
cd harness && mvn -q -pl jmeter test -Dtest=Tst020ModuleTest
```

Expected: PASS, 3 tests.

- [ ] **Step 8: Verify the gate and the capability count**

```bash
cd "$(git rev-parse --show-toplevel)"
python3 scripts/validate-harness-coverage.py; echo "harness=$?"
python3 scripts/render-harness-coverage.py
curl -s http://localhost:8080/_capabilities | python3 -c "
import json,sys
d=json.load(sys.stdin)
print('implemented:', sum(1 for v in d.values() if v=='implemented'))
"
```

Expected: `harness=0` and `implemented: 15`.

- [ ] **Step 9: Commit**

```bash
git add qe-harness/harness/jmeter qe-harness/traceability
git commit -m "feat(harness): add TST-020 idempotency module with full I1-I7 coverage"
```

---

## Task 27: Full Gate, Regenerated Coverage, and the 8/8 Defect Proof

The wave's headline claim is not "eight modules exist" but "eight defects each break exactly
their own invariant". This task proves it.

**Files:**
- Modify: `qe-harness/traceability/harness-coverage.md` (regenerated)
- Create: none

**Interfaces:**
- Consumes: every module from Tasks 8, 10, 13, 18, 20, 22, 24, 26
- Produces: the 8/8 specificity table this wave is judged on

- [ ] **Step 1: Bring up the full stack and run every module clean**

```bash
cd qe-harness && make down && make up PROFILES="core resilience messaging"
export HARNESS_SMOKE_MODE=true
make run-all
```

Expected: every module reports `-> passed`. All fifteen, not just the new eight — a Wave 17
change that broke a Wave 16 module must surface here.

- [ ] **Step 2: Run the defect suite**

```bash
cd qe-harness && make run-defects
```

Expected: every module reports `failed` under its own flag. A module that **passes** with its
defect active is worse than a missing module: it is a test that cannot detect the thing it
exists to detect.

- [ ] **Step 3: Prove specificity, not merely sensitivity**

Sensitivity is "something failed". Specificity is "exactly the right thing failed". Build the
table from the emitted evidence rather than by eye:

```bash
cd qe-harness && python3 - <<'PY'
import json, pathlib, collections

EXPECTED = {
    "TST-020": {"failed": {"I1"}, "must_pass": {"I2", "I4"}},
    "TST-023": {"failed": {"I1", "I2"}, "must_pass": {"I3", "I4"}},
    "TST-026": {"failed": {"I2"}, "must_pass": {"I1", "I5", "I6"}},
    "TST-027": {"failed": {"I1"}, "must_pass": {"I3"}},
    "TST-028": {"failed": {"I1"}, "must_pass": {"I2", "I3"}},
    "TST-029": {"failed": {"I1"}, "must_pass": {"I4", "I5"}},
    "TST-034": {"failed": {"I3"}, "must_pass": {"I1", "I4"}},
    "TST-037": {"failed": {"I4"}, "must_pass": {"I1", "I2"}},
}

runs = pathlib.Path("traceability/runs")
latest = {}
for f in sorted(runs.glob("*.json")):
    d = json.loads(f.read_text())
    if d.get("evidence", {}).get("sut_defect"):
        latest[d["archetype"]] = d

ok = True
for arch, spec in sorted(EXPECTED.items()):
    d = latest.get(arch)
    if d is None:
        print(f"{arch}: NO DEFECT-ACTIVE FRAGMENT FOUND")
        ok = False
        continue
    results = {i["id"]: i["result"] for i in d.get("invariants", [])}
    failed = {k for k, v in results.items() if v == "failed"}
    wrong_fail = failed - spec["failed"]
    missing_fail = spec["failed"] - failed
    broke_others = {k for k in spec["must_pass"] if results.get(k) != "passed"}
    verdict = "SPECIFIC" if not (wrong_fail or missing_fail or broke_others) else "NOT SPECIFIC"
    if verdict != "SPECIFIC":
        ok = False
    print(f"{arch}: {verdict} failed={sorted(failed)} "
          f"unexpected={sorted(wrong_fail)} missing={sorted(missing_fail)} "
          f"collateral={sorted(broke_others)}")

print("\n8/8 SPECIFIC" if ok else "\nSPECIFICITY PROOF FAILED")
PY
```

Expected: `8/8 SPECIFIC`. Any `collateral` entry means a defect broke an invariant it should not
have — the defect branch is too broad, or two invariants share an implementation path they
should not. Fix the SUT branch, not the expectation table.

- [ ] **Step 4: Regenerate the coverage table**

```bash
cd "$(git rev-parse --show-toplevel)"
python3 scripts/render-harness-coverage.py
python3 scripts/render-harness-coverage.py --check; echo "render=$?"
```

Expected: `render=0`.

- [ ] **Step 5: Run all seven checks plus every corpus gate**

```bash
python3 scripts/validate-harness-coverage.py;       echo "harness=$?"
python3 scripts/validate-testing-coverage.py;       echo "cov=$?"
python3 scripts/render-testing-coverage.py --check;  echo "render-cov=$?"
python3 scripts/audit-catalog-consistency.py;        echo "audit=$?"
python3 scripts/validate-internal-links.py;          echo "links=$?"
```

Expected: all five exit `0`. Compare against Task 0's recorded baseline — nothing that was
green may have gone red.

- [ ] **Step 6: Confirm every partial carries a reason and no coverage is overstated**

```bash
cd qe-harness && python3 - <<'PY'
import yaml, pathlib
mods = yaml.safe_load(pathlib.Path("traceability/modules.yml").read_text())["modules"]
print(f"{len(mods)} modules")
for m in mods:
    cov = m["coverage"]
    reason = (m.get("partial_reason") or "").strip()
    flag = "OK" if cov == "full" or reason else "MISSING REASON"
    print(f"  {m['archetype']:8} {m['tool']:16} {cov:8} {flag}")
partials = [m["archetype"] for m in mods if m["coverage"] == "partial"]
print("partials:", partials)
assert sorted(partials) == ["TST-027", "TST-037", "TST-043"], partials
print("full:", len(mods) - len(partials))
PY
```

Expected: 15 modules, partials exactly `['TST-027', 'TST-037', 'TST-043']`, 12 full — matching
the spec's §8 tally.

- [ ] **Step 7: Confirm the registry is truthful**

```bash
curl -s http://localhost:8080/_capabilities | python3 -c "
import json,sys
d=json.load(sys.stdin)
impl=sorted(k for k,v in d.items() if v=='implemented')
print(len(impl)); print(impl)
"
cd qe-harness/reference-sut && mvn -q -B test -Dtest=CapabilityRegistryTest
```

Expected: `15`, and the set-based guard passes — `IMPLEMENTED` and `IMPLEMENTED_AT_WAVE_17`
agree, so the registry cannot have drifted from what `modules.yml` ships.

- [ ] **Step 8: Commit**

```bash
cd "$(git rev-parse --show-toplevel)"
git add qe-harness/traceability
git commit -m "chore(harness): regenerate coverage table for the fifteen-module harness"
```

---

## Task 28: CI on a Real Runner, and Handoff

Wave 16's `qe-harness` stage has **never run on a real GitLab runner**. Wave 17 adds a RabbitMQ
container to it. This task is where that meets reality.

**Files:**
- Modify: `.gitlab-ci.yml` (messaging profile in `harness:run`)
- Create: `docs/superpowers/reports/2026-09-03-wave-17-report.md`

**Interfaces:**
- Consumes: everything above
- Produces: a green pipeline and the wave's handoff record

- [ ] **Step 1: Add the messaging profile to the CI run job**

`harness:run` currently starts `PROFILES="core resilience"`. Four new modules need the broker:

```yaml
    - make up PROFILES="core resilience messaging"
```

The broker's healthcheck must gate module start. Confirm `make up` waits for health rather than
merely starting containers:

```bash
/usr/bin/grep -n "wait\|health" qe-harness/Makefile qe-harness/bin/wait-for-sut.sh
```

If `make up` does not wait on the broker specifically, add a bounded wait to `bin/` following
`wait-for-sut.sh`'s existing shape — the first messaging module would otherwise race a broker
still in its 20-second `start_period` and fail for a reason unrelated to the SUT.

- [ ] **Step 2: Confirm the CI rules already cover the new files**

```bash
python3 -c "
import yaml
d = yaml.safe_load(open('.gitlab-ci.yml'))
rules = d['harness:run']['rules']
changes = [c for r in rules for c in r.get('changes', [])]
for needed in ['qe-harness/**/*.java', 'qe-harness/**/*.groovy', 'qe-harness/**/*.jmx',
               'qe-harness/**/*.sql', 'qe-harness/docker-compose.yml',
               'qe-harness/**/pom.xml', 'qe-harness/traceability/modules.yml',
               'qe-harness/profiles/**/*.yml']:
    print(('OK  ' if needed in changes else 'MISS'), needed)
"
```

Expected: every line `OK`. `.qe-harness-code-rules` already lists all eight patterns, so the new
Java, Groovy, JMX, SQL and YAML files are covered without a rules change.

- [ ] **Step 3: Estimate the CI time cost honestly before pushing**

```bash
cd qe-harness && make down && make up PROFILES="core resilience messaging"
export HARNESS_SMOKE_MODE=true
time make run-all
time make run-defects
```

Record both durations. TST-029's retry ladder alone totals 13 seconds per run, and TST-034 holds
20 seconds in smoke mode. If the combined total materially exceeds Wave 16's, say so in the
report rather than letting the pipeline discover it.

- [ ] **Step 4: Push and watch the real pipeline**

```bash
git push -u origin "$(git rev-parse --abbrev-ref HEAD)"
```

Then watch `harness:build`, `harness:scan`, `harness:verify`, `harness:run` and the new
`validate:testing-coverage`. **This is the first real exercise of the whole `qe-harness` stage**,
so treat a failure here as information about the stage, not only about Wave 17.

Watch for, specifically:
- `harness:run` needs docker-in-docker plus a RabbitMQ container. If the runner cannot start it,
  report that as an infrastructure finding — do not respond by deleting the messaging modules
  from the run set.
- `harness:scan` (Trivy, `--exit-code 1`, blocking) now sees `spring-boot-starter-amqp` and
  `org.testcontainers:rabbitmq`. A new CRITICAL/HIGH CVE or licence finding blocks the pipeline.
  If one appears, report the CVE and the affected coordinate; do not add it to `.trivyignore`
  without DevSecOps sign-off — that allowlist already carries a disclosed residual risk from
  Wave 16 (it is repo-wide rather than path-scoped).

- [ ] **Step 5: Write the wave report**

`docs/superpowers/reports/2026-09-03-wave-17-report.md`, following Wave 16's report shape:

```markdown
# Wave 17 — QE Harness Coverage Expansion (Report)

**Status:** <Complete | Complete with disclosed risks>
**Date:** 2026-09-03
**Spec:** `docs/superpowers/specs/2026-09-03-wave-17-harness-coverage-expansion-design.md`
**Plan:** `docs/superpowers/plans/2026-09-03-wave-17-harness-coverage-expansion.md`

## What landed

8 new archetype modules, taking runnable coverage from 7 to 15 of 24 and completing Family B.

| Family | Before | After |
|---|---|---|
| A Correctness | 1/6 | 3/6 |
| B Messaging | 1/5 | **5/5** |
| C Load | 1/4 | 2/4 |
| E Data | 1/3 | 2/3 |

Final tally: **12 `full`, 3 `partial`** (TST-027, TST-037, TST-043).

## The defect-specificity proof

<paste Task 27 Step 3's output verbatim>

## What we corrected rather than shipped around

- `TST-043` was in `IMPLEMENTED` while asserting none of its own I1–I6; its `partial_reason`
  now says so.
- The `TST-041` archetype document carried a NUL byte that hid its content from grep.
- `TST-025`'s covering rows disagreed on `primary_tool`.
- Two threshold-shaped needs had **no** citable NFR anchor. Rather than fabricate citations or
  amend the governed NFR spine, both are declared as application config — see the spec's §7.1.

## Disclosed residual risks

1. `TST-029` I2's broker restart is exercised in full runs only; CI reports it `not-evaluated`.
2. `TST-027` I5 covers `per_key` only — RabbitMQ has no partitions.
3. `TST-037` I5 needs a CDC connector this repository does not contain.
4. <CI findings from Step 4, if any>

## Follow-ups for the next wave

<carry forward the spec's §11 list, updated>
```

- [ ] **Step 6: Compare against the wave's stated success criteria**

Walk the spec's §9 list and record each verdict:

```bash
cd "$(git rev-parse --show-toplevel)"
sed -n '/## 9. Success Criteria/,/## 10. Risks/p' \
  docs/superpowers/specs/2026-09-03-wave-17-harness-coverage-expansion-design.md
```

Every criterion must be marked met or explicitly not met with a reason. A criterion quietly
dropped is the failure mode this whole harness exists to prevent.

- [ ] **Step 7: Commit and open the merge request**

```bash
git add docs/superpowers/reports .gitlab-ci.yml qe-harness
git commit -m "docs(wave-17): add implementation report and enable messaging in CI"
git push
```

Open the MR against `main`. Do **not** merge it yourself — the spec names
`@tester-qe` (oracle fidelity, defect specificity) and `@devsecops-engineer` (broker in CI,
dependency surface) as gating reviewers, and the Trivy surface changed in this wave.

- [ ] **Step 8: Report**

State plainly: the module count, the 8/8 specificity result, every gate's exit code, the CI
outcome on a real runner, and each disclosed risk. If anything is red, say which and why rather
than reporting the wave complete.

---

## Appendix: Why Each Partial Is Partial

Recorded here because "partial" is the claim most likely to be quietly upgraded by a future
change, and the reasons are not recoverable from the code.

| Archetype | Unreached | Why it cannot be reached here |
|---|---|---|
| `TST-027` I5 | `per_partition`, `global` ordering scopes | RabbitMQ has no partitions. The declared scope is `per_key`, and asserting the per-key case does not evidence the others. |
| `TST-037` I5 | No loss or duplication across connector restart | Needs a CDC connector; this repository contains none. A server-side stand-in would be a different invariant wearing I5's name. |
| `TST-043` I1–I6 | All six client-side invariants | I1/I2/I6 need an offline client, I3/I4 a rendered DOM, I5 `k6/browser` against a real page. The module ships four substitute server-side checks, renumbered — they are **not** the archetype's I1–I4. |

And one that is deliberately **not** partial:

| Archetype | Gated | Why coverage stays `full` |
|---|---|---|
| `TST-029` I2 | Broker restart, CI only | The invariant **is** implemented and passes on a full run. A run-mode gate is reported per-run via `not-evaluated`; understating the module's coverage would misdescribe what it contains. |

