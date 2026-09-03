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
        rabbit.convertAndSend("qe.in", "pay.unknown.type", "probe");
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

        DirectExchange in = ExchangeBuilderCompat.directWithAlternate(IN_EXCHANGE, UNROUTABLE_EXCHANGE);
        TopicExchange route = new TopicExchange(ROUTE_EXCHANGE, true, false);
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

import org.springframework.amqp.core.DirectExchange;

import java.util.Map;

/**
 * The alternate-exchange argument has no first-class setter on
 * {@link DirectExchange}'s constructors, so it is applied here rather than
 * inline, keeping {@link MessagingTopology#declarables()} readable.
 *
 * <p>The alternate exchange is what turns an unroutable message into an
 * observable one -- TST-026's I2 reads the quarantine queue's depth as its
 * verdict, which is only possible because the broker parks it instead of
 * discarding it.
 */
final class ExchangeBuilderCompat {

    private ExchangeBuilderCompat() {
    }

    static DirectExchange directWithAlternate(String name, String alternateExchange) {
        DirectExchange exchange = new DirectExchange(name, true, false,
            Map.of("alternate-exchange", alternateExchange));
        return exchange;
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
