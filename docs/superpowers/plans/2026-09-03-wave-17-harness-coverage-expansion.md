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
