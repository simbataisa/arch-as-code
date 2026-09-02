# Wave 16 — QE Harness Reference Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development
> (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use
> checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a runnable QE harness plus a bundled synthetic reference service, so the Wave 15
testing corpus stops being doctrine the QE team can only read.

**Architecture:** One new top-level `qe-harness/` holds a Java 21 + Spring Boot reference SUT
whose capability registry enumerates all 24 archetypes (7 implemented, 17 answering `501`), and
seven harness modules — one per archetype family — each written in the tool the corpus names as
that archetype's best fit. A Python traceability gate proves the harness and the corpus agree; a
new CI stage builds, scans, verifies, and runs it.

**Tech Stack:** Java 21, Spring Boot 3, Maven, PostgreSQL, Resilience4j, Toxiproxy, Apache
JMeter, Gatling + Karate, k6 (npm), Locust (pip), Docker Compose, Python 3 (gates), GitLab CI.

**Spec:** `docs/superpowers/specs/2026-08-24-wave-16-qe-harness-design.md` — read it alongside
this plan. The spec carries the rationale; this plan carries the steps.

---

## Global Constraints

Every task's requirements implicitly include this section. Values are copied verbatim from the
spec.

- **Synthetic data only.** No PII, no PHI, and **no 13–19 digit numeric strings** anywhere in
  the tree — seed data, fixtures, test names, or documentation. Account identifiers take the
  form `ACC-000001`. Amounts are in minor units. Party names come from a fixed synthetic list.
- **No hardcoded performance thresholds.** Every numeric performance target carries a
  `threshold_ref` citing an existing `NFR-*` row and anchor, e.g. `NFR-003#p99-latency`.
- **No modification** of the 24 archetype documents, 9 strategy documents, 5 tooling documents,
  or any pattern, NFR, or reference-architecture row. Wave 16 implements the corpus; it does not
  amend it. The single permitted documentation edit is rescoping the documentation-only claim in
  `knowledge-base/testing/README.md` (Task 25).
- **Assertions are never retried.** Bounded retry applies to infrastructure setup only.
- **Three-state results.** Every result is `passed`, `failed`, or `not-evaluated`. Capability
  stubs additionally report `not-implemented`. Two-state reporting is a defect.
- **Each module's tool must match** the archetype's declared best fit in `TST-010`:
  `TST-021` JMeter, `TST-030` gatling-karate, `TST-031` JMeter, `TST-035` JMeter, `TST-039`
  locust, `TST-040` JMeter, `TST-043` k6.
- **Markdown-only merge requests must not incur harness CI time.** All `qe-harness` jobs are
  gated on `changes: [qe-harness/**, scripts/validate-harness-coverage.py]` plus schedules.
- **Java package root:** `com.techcombank.qe`. **Defect flag env var:** `SUT_DEFECT`.
- **Dependency versions are pinned in lockfiles**, committed, and scanned. Task 1 resolves and
  records the pinned set; no task may introduce an unpinned dependency.

---

## File Structure

| Path | Responsibility |
|---|---|
| `qe-harness/README.md` | TST-016 governed entry doc; clone-and-run instructions; states the threshold-gate limit |
| `qe-harness/Makefile` | Single façade over three build systems: `up`, `down`, `verify`, `run`, `run-all`, `run-defects` |
| `qe-harness/docker-compose.yml` | SUT + infra, profile-gated (`core`, `resilience`, `observability`, `messaging`) |
| `qe-harness/reference-sut/pom.xml` | Spring Boot app build |
| `…/sut/CapabilityRegistry.java` | Single source of the 24 archetype IDs and their implemented/declared state |
| `…/sut/CapabilityController.java` | `GET /_capabilities`; `501` responder for declared-not-implemented |
| `…/sut/DefectFlags.java` | Reads `SUT_DEFECT`; one enum constant per injected defect |
| `…/sut/capability/<archetype>/` | One package per implemented capability — ledger, ratelimit, authz, contract, resilience, recon, clientexp |
| `qe-harness/reference-sut/src/main/resources/db/migration/` | Flyway schema |
| `qe-harness/reference-sut/…/SyntheticDataSeeder.java` | Deterministic seed; the only writer of test data |
| `qe-harness/harness/pom.xml` | JVM reactor: `common`, `jmeter`, `gatling-karate` |
| `qe-harness/harness/common/…/oracle/` | `InvariantAssertion`, `GoldenDataset`, `ConfusionMatrix`, `ContractSchema` |
| `qe-harness/harness/common/…/evidence/EvidenceEmitter.java` | Writes one run fragment; JVM emitter |
| `qe-harness/harness/locust/emitter.py` | Python evidence emitter |
| `qe-harness/harness/k6/emitter.js` | JavaScript evidence emitter |
| `qe-harness/profiles/_nfr-thresholds.yml` | Machine-readable NFR projection; every entry cites a row + anchor |
| `qe-harness/traceability/evidence.schema.json` | Language-neutral run-fragment schema; the one contract all three emitters obey |
| `qe-harness/traceability/harness-coverage.md` | Generated module ↔ archetype ↔ tool ↔ status table |
| `scripts/validate-harness-coverage.py` | The traceability gate — six checks |
| `scripts/render-harness-coverage.py` | Renders `harness-coverage.md`; `--check` mode for CI |
| `.gitlab-ci.yml` | New `qe-harness` stage, four jobs |

**Decomposition note.** One package per capability, and one harness module directory per tool.
Capabilities never import each other — a defect injected into the ledger must not be able to
change rate-limiter behaviour, or the defect-injection proof in Task 27 means nothing.

---

## Evidence Run-Fragment Contract

Defined once here; Tasks 2, 21, 22 all implement it and Task 3's gate validates it.

`TST-001` defines `test_acceptance_criteria` as a **per-service** block whose `archetypes` field
is a list. A single harness module cannot emit that block — it knows only its own archetype. So
each module emits a **run fragment**, and `make run-all` merges fragments into one block.

```json
{
  "archetype": "TST-021",
  "module": "jmeter",
  "service_name": "reference-sut",
  "tier": "T0",
  "oracle": "invariant-assertion",
  "result": "passed",
  "invariants": [
    {"id": "I1", "description": "trial balance nets to zero", "result": "passed"}
  ],
  "thresholds": [
    {"name": "p99_latency_ms", "threshold_ref": "NFR-003#p99-latency", "result": "not-evaluated", "reason": "smoke-mode"}
  ],
  "evidence": {
    "executed_on": "2026-08-24",
    "environment": "ci-smoke",
    "sut_defect": null,
    "report_path": "traceability/runs/2026-08-24T101500Z-TST-021.json"
  }
}
```

`result` ∈ `passed | failed | not-evaluated | not-implemented`. Same domain for each entry in
`invariants[]` and `thresholds[]`. A `thresholds[]` entry whose `result` is `not-evaluated`
**must** carry a non-empty `reason`.

---

## Task 0: Pre-Flight Baseline

Establishes the green baseline and resolves the spec's §14 open risk **before** any code is
written, so a later failure is attributable.

**Files:**
- Create: none (read-only task; writes one scratch file)

**Interfaces:**
- Consumes: nothing
- Produces: a recorded baseline the final task (Task 28) compares against

- [ ] **Step 1: Record the four Wave 15 gate results**

```bash
cd "$(git rev-parse --show-toplevel)"
python3 scripts/audit-catalog-consistency.py            > /tmp/w16-base-audit.txt 2>&1; echo "audit=$?"
python3 scripts/validate-testing-coverage.py            > /tmp/w16-base-cov.txt   2>&1; echo "cov=$?"
python3 scripts/render-testing-coverage.py --check      > /tmp/w16-base-render.txt 2>&1; echo "render=$?"
python3 scripts/validate-internal-links.py              > /tmp/w16-base-links.txt 2>&1; echo "links=$?"
```

Expected: all four exit `0`. If any is non-zero, STOP and report — the baseline is not green and
Wave 16 must not build on it.

- [ ] **Step 2: Resolve the catalog-path risk (spec §11, §14)**

The spec flags that `TST-016`'s `path` would be the first catalog row outside `knowledge-base/`.
Confirm the audit script tolerates that:

```bash
sed -n '110,125p' scripts/audit-catalog-consistency.py
```

Expected: the row's path is resolved as `ROOT / inv["path"]` and only checked for existence —
no `knowledge-base/` prefix constraint. Also confirm it parses a document header:

```bash
/usr/bin/grep -n "STATUS_RE\|CATALOG_ID_RE" scripts/audit-catalog-consistency.py
```

Expected: `Status:` and `Catalog ID:` regexes. **This means `qe-harness/README.md` MUST carry the
same header block every catalog document carries** (Task 1, Step 2). Record this finding.

- [ ] **Step 3: Confirm the toolchain is available**

```bash
java -version; mvn -v; docker --version; docker compose version; node -v; python3 -V
```

Expected: Java 21+, Maven 3.9+, Docker with Compose v2, Node 18+, Python 3.11+. Report any
missing tool as BLOCKED rather than working around it — the whole wave assumes these.

- [ ] **Step 4: Commit nothing**

This task produces no repository change. Report the baseline and the Step 2 finding.

---

## Task 1: Harness Skeleton, README (TST-016), and Pinned Dependencies

**Files:**
- Create: `qe-harness/README.md`, `qe-harness/Makefile`, `qe-harness/.gitignore`
- Create: `qe-harness/reference-sut/pom.xml`, `qe-harness/harness/pom.xml`
- Create: `qe-harness/harness/common/pom.xml`

**Interfaces:**
- Consumes: Task 0's finding that the README needs a catalog header
- Produces: Maven coordinates `com.techcombank.qe:qe-harness-parent:1.0.0-SNAPSHOT` with modules
  `common`, `jmeter`, `gatling-karate`; Make targets `up`, `down`, `verify`, `run`, `run-all`,
  `run-defects`

- [ ] **Step 1: Create the directory skeleton**

```bash
cd "$(git rev-parse --show-toplevel)"
mkdir -p qe-harness/reference-sut/src/main/java/com/techcombank/qe/sut
mkdir -p qe-harness/reference-sut/src/main/resources/db/migration
mkdir -p qe-harness/reference-sut/src/test/java/com/techcombank/qe/sut
mkdir -p qe-harness/harness/common/src/main/java/com/techcombank/qe/harness
mkdir -p qe-harness/harness/common/src/test/java/com/techcombank/qe/harness
mkdir -p qe-harness/harness/jmeter qe-harness/harness/gatling-karate
mkdir -p qe-harness/harness/k6 qe-harness/harness/locust
mkdir -p qe-harness/profiles qe-harness/traceability/runs
```

- [ ] **Step 2: Write `qe-harness/README.md` with the catalog header**

The header block is mandatory — `scripts/audit-catalog-consistency.py` parses `Status:` and
`Catalog ID:` and Task 26 will fail without them.

```markdown
# QE Harness Reference Implementation

Status: Approved | Last Reviewed: 2026-08-24 | Owner: @qe-lead
Catalog ID: TST-016 | Radii
Tier Applicability: T0, T1, T2, T3

## Problem Statement

The Wave 15 testing corpus defines 24 archetypes, four oracle types, and eight performance
profiles, and states plainly that it ships no harness. A QE team could read all of it and still
have nothing to run. This directory is the runnable counterpart: a synthetic reference service
and seven harness modules, one per archetype family, each in the tool
[TST-010](../knowledge-base/testing/tooling/tool-selection-matrix.md) names as its best fit.

## Quick Start

    make up          # start the SUT and the infrastructure its modules need
    make verify      # run the gates — no containers required
    make run ARCH=TST-021
    make run-all     # all seven modules against the clean SUT
    make run-defects # all seven modules against their injected defects; each MUST fail

## Scope

Seven of the 24 archetypes are implemented. `GET /_capabilities` lists all 24; the other 17
answer `501` with their archetype ID. Waves 17+ fill those in.

`TST-043` is **partial**: it covers perf budget, cache correctness, conditional requests, and
compression. Its offline-sync invariants need a client application, which this repository does
not contain.

## What the Threshold Gate Does Not Prove

Performance targets are never hardcoded here. Each cites an `NFR-*` row and anchor in
`profiles/_nfr-thresholds.yml`. The gate proves **the citation resolves** — that the row and
anchor exist. It does **not** prove the number matches the NFR document's prose, because that
would mean parsing Markdown for numeric values. A human owns number accuracy; the gate owns the
citation.

## Smoke Mode

In CI, `TST-031` and `TST-035` run in smoke mode: correctness invariants are asserted, and every
performance threshold is recorded `not-evaluated`. A shared CI runner cannot produce meaningful
latency figures, and a green pipeline must never imply that performance was validated. Full-load
runs happen on a dedicated environment via the nightly or manual job.

## Related

- [TST-001 Test Strategy Standard](../knowledge-base/testing/strategy/test-strategy-standard.md)
- [TST-002 Performance Test Standard](../knowledge-base/testing/strategy/performance-test-standard.md)
- [TST-010 Tool Selection Matrix](../knowledge-base/testing/tooling/tool-selection-matrix.md)
- [Testing Coverage Matrix](../knowledge-base/testing/coverage/coverage-matrix.md)
```

- [ ] **Step 3: Write the parent POM**

`qe-harness/harness/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.techcombank.qe</groupId>
  <artifactId>qe-harness-parent</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <modules>
    <module>common</module>
    <module>jmeter</module>
    <module>gatling-karate</module>
  </modules>
  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>
</project>
```

- [ ] **Step 4: Write the Makefile**

```makefile
COMPOSE ?= docker compose
PROFILES ?= core
ROOT := $(shell cd .. && pwd)

.PHONY: up down verify run run-all run-defects

up:
	$(COMPOSE) --profile $(PROFILES) up -d --wait

down:
	$(COMPOSE) --profile core --profile resilience down -v

verify:
	python3 $(ROOT)/scripts/validate-harness-coverage.py
	python3 $(ROOT)/scripts/render-harness-coverage.py --check

run:
	@test -n "$(ARCH)" || (echo "usage: make run ARCH=TST-021" && exit 2)
	./bin/run-module.sh "$(ARCH)"

run-all:
	./bin/run-all.sh

run-defects:
	./bin/run-defects.sh
```

- [ ] **Step 5: Resolve and pin dependency versions**

Do not invent versions. Resolve what actually exists, then commit the result:

```bash
cd qe-harness/harness && mvn -q -N help:effective-pom > /dev/null && echo "parent POM valid"
```

Record the resolved Spring Boot, JMeter, Gatling, and Karate versions in
`qe-harness/README.md` under a new `## Pinned Versions` section as a Markdown table with columns
`Tool | Version | Lockfile`. Every subsequent task uses these exact versions.

- [ ] **Step 6: Verify the skeleton builds**

Run: `cd qe-harness/harness && mvn -q validate`
Expected: SUCCESS. Module directories `common`, `jmeter`, `gatling-karate` may warn about
missing POMs — those arrive in Tasks 15, 16, 20. If Maven hard-fails on the missing modules,
comment the `<module>` entries and uncomment each in the task that creates it.

- [ ] **Step 7: Commit**

```bash
git add qe-harness/
git commit -m "feat(qe-harness): scaffold harness skeleton and TST-016 entry doc"
```

---

## Task 2: Evidence Schema and JVM Emitter

**Files:**
- Create: `qe-harness/traceability/evidence.schema.json`
- Create: `qe-harness/harness/common/pom.xml`
- Create: `…/harness/common/src/main/java/com/techcombank/qe/harness/evidence/RunFragment.java`
- Create: `…/evidence/EvidenceEmitter.java`
- Test: `…/harness/common/src/test/java/com/techcombank/qe/harness/evidence/EvidenceEmitterTest.java`

**Interfaces:**
- Consumes: Maven parent from Task 1
- Produces: `EvidenceEmitter.emit(RunFragment) -> Path`; `RunFragment.Result` enum with
  `PASSED, FAILED, NOT_EVALUATED, NOT_IMPLEMENTED` serialising to `passed`, `failed`,
  `not-evaluated`, `not-implemented`. Tasks 16–22 all call `emit`. Task 3's gate validates its
  output.

- [ ] **Step 1: Write the failing test**

`EvidenceEmitterTest.java`:

```java
package com.techcombank.qe.harness.evidence;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class EvidenceEmitterTest {

    @Test
    void emitsFragmentValidatingAgainstSchema(@TempDir Path dir) throws Exception {
        RunFragment f = RunFragment.builder()
            .archetype("TST-021").module("jmeter").serviceName("reference-sut")
            .tier("T0").oracle("invariant-assertion")
            .invariant("I1", "trial balance nets to zero", RunFragment.Result.PASSED)
            .environment("ci-smoke")
            .build();

        Path out = new EvidenceEmitter(dir).emit(f);

        assertTrue(Files.exists(out));
        String json = Files.readString(out);
        assertTrue(json.contains("\"archetype\": \"TST-021\""));
        assertTrue(json.contains("\"result\": \"passed\""));
    }

    @Test
    void rejectsNotEvaluatedThresholdWithoutReason() {
        assertThrows(IllegalArgumentException.class, () ->
            RunFragment.builder()
                .archetype("TST-031").module("jmeter").serviceName("reference-sut")
                .tier("T0").oracle("invariant-assertion").environment("ci-smoke")
                .threshold("p99_latency_ms", "NFR-003#p99-latency",
                           RunFragment.Result.NOT_EVALUATED, null)
                .build());
    }

    @Test
    void overallResultIsFailedIfAnyInvariantFailed() {
        RunFragment f = RunFragment.builder()
            .archetype("TST-021").module("jmeter").serviceName("reference-sut")
            .tier("T0").oracle("invariant-assertion").environment("ci-smoke")
            .invariant("I1", "a", RunFragment.Result.PASSED)
            .invariant("I2", "b", RunFragment.Result.FAILED)
            .build();
        assertEquals(RunFragment.Result.FAILED, f.result());
    }
}
```

- [ ] **Step 2: Run it and confirm it fails**

Run: `cd qe-harness/harness && mvn -q -pl common test`
Expected: FAIL — `RunFragment` and `EvidenceEmitter` do not exist.

- [ ] **Step 3: Write `evidence.schema.json`**

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "QE harness run fragment",
  "type": "object",
  "additionalProperties": false,
  "required": ["archetype", "module", "service_name", "tier", "oracle", "result", "evidence"],
  "properties": {
    "archetype": {"type": "string", "pattern": "^TST-0[2-4][0-9]$"},
    "module": {"enum": ["jmeter", "gatling-karate", "k6", "locust"]},
    "service_name": {"type": "string", "minLength": 1},
    "tier": {"enum": ["T0", "T1", "T2", "T3"]},
    "oracle": {"enum": ["invariant-assertion", "golden-dataset", "confusion-matrix", "contract-schema"]},
    "result": {"$ref": "#/$defs/result"},
    "invariants": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "required": ["id", "description", "result"],
        "properties": {
          "id": {"type": "string", "pattern": "^I[0-9]+$"},
          "description": {"type": "string", "minLength": 1},
          "result": {"$ref": "#/$defs/result"}
        }
      }
    },
    "thresholds": {
      "type": "array",
      "items": {
        "type": "object",
        "additionalProperties": false,
        "required": ["name", "threshold_ref", "result"],
        "properties": {
          "name": {"type": "string", "minLength": 1},
          "threshold_ref": {"type": "string", "pattern": "^NFR-[0-9]{3}#[a-z0-9-]+$"},
          "result": {"$ref": "#/$defs/result"},
          "reason": {"type": "string", "minLength": 1}
        },
        "allOf": [{
          "if": {"properties": {"result": {"const": "not-evaluated"}}},
          "then": {"required": ["reason"]}
        }]
      }
    },
    "evidence": {
      "type": "object",
      "additionalProperties": false,
      "required": ["executed_on", "environment", "report_path"],
      "properties": {
        "executed_on": {"type": "string", "format": "date"},
        "environment": {"type": "string", "minLength": 1},
        "sut_defect": {"type": ["string", "null"]},
        "report_path": {"type": "string", "minLength": 1}
      }
    }
  },
  "$defs": {
    "result": {"enum": ["passed", "failed", "not-evaluated", "not-implemented"]}
  }
}
```

- [ ] **Step 4: Write `RunFragment` and `EvidenceEmitter`**

`RunFragment.java` — a record with a builder. Key logic, which the third test pins:

```java
package com.techcombank.qe.harness.evidence;

import java.time.LocalDate;
import java.util.*;

public record RunFragment(
    String archetype, String module, String serviceName, String tier, String oracle,
    List<Entry> invariants, List<Threshold> thresholds,
    String environment, String sutDefect, LocalDate executedOn
) {
    public enum Result {
        PASSED("passed"), FAILED("failed"),
        NOT_EVALUATED("not-evaluated"), NOT_IMPLEMENTED("not-implemented");
        private final String wire;
        Result(String w) { this.wire = w; }
        public String wire() { return wire; }
    }

    public record Entry(String id, String description, Result result) {}
    public record Threshold(String name, String thresholdRef, Result result, String reason) {}

    /** FAILED if any invariant or threshold failed; else NOT_EVALUATED if nothing was
     *  evaluated at all; else PASSED. Never silently PASSED on an empty run. */
    public Result result() {
        boolean anyFailed = invariants.stream().anyMatch(i -> i.result() == Result.FAILED)
            || thresholds.stream().anyMatch(t -> t.result() == Result.FAILED);
        if (anyFailed) return Result.FAILED;
        boolean anyEvaluated = invariants.stream().anyMatch(i -> i.result() == Result.PASSED)
            || thresholds.stream().anyMatch(t -> t.result() == Result.PASSED);
        return anyEvaluated ? Result.PASSED : Result.NOT_EVALUATED;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String archetype, module, serviceName, tier, oracle, environment, sutDefect;
        private final List<Entry> invariants = new ArrayList<>();
        private final List<Threshold> thresholds = new ArrayList<>();

        public Builder archetype(String v) { this.archetype = v; return this; }
        public Builder module(String v) { this.module = v; return this; }
        public Builder serviceName(String v) { this.serviceName = v; return this; }
        public Builder tier(String v) { this.tier = v; return this; }
        public Builder oracle(String v) { this.oracle = v; return this; }
        public Builder environment(String v) { this.environment = v; return this; }
        public Builder sutDefect(String v) { this.sutDefect = v; return this; }

        public Builder invariant(String id, String desc, Result r) {
            invariants.add(new Entry(id, desc, r)); return this;
        }

        public Builder threshold(String name, String ref, Result r, String reason) {
            if (r == Result.NOT_EVALUATED && (reason == null || reason.isBlank())) {
                throw new IllegalArgumentException(
                    "threshold '" + name + "' is not-evaluated and must carry a reason");
            }
            thresholds.add(new Threshold(name, ref, r, reason)); return this;
        }

        public RunFragment build() {
            Objects.requireNonNull(archetype, "archetype");
            Objects.requireNonNull(oracle, "oracle");
            return new RunFragment(archetype, module, serviceName, tier, oracle,
                List.copyOf(invariants), List.copyOf(thresholds),
                environment, sutDefect, LocalDate.now());
        }
    }
}
```

`EvidenceEmitter.java` writes `<dir>/<ISO-instant>-<archetype>.json` using Jackson, serialising
`Result` via `wire()` and setting `report_path` to the file's repo-relative path.

- [ ] **Step 5: Run the tests**

Run: `cd qe-harness/harness && mvn -q -pl common test`
Expected: PASS, 3 tests.

- [ ] **Step 6: Commit**

```bash
git add qe-harness/traceability/evidence.schema.json qe-harness/harness/common
git commit -m "feat(qe-harness): add evidence run-fragment schema and JVM emitter"
```

---

## Task 3: The Traceability Gate

The gate that justifies in-repo placement (spec §4.1). Six checks.

**Files:**
- Create: `scripts/validate-harness-coverage.py`
- Create: `qe-harness/traceability/modules.yml`
- Test: `scripts/tests/test_validate_harness_coverage.py`
- Modify: `scripts/requirements.txt`

**Interfaces:**
- Consumes: `evidence.schema.json` from Task 2
- Produces: `modules.yml` as the single declaration of which module implements which archetype;
  Tasks 16–22 each append one entry. Exit `0` clean, `1` on findings, `2` on malformed input.

- [ ] **Step 1: Add `jsonschema` to requirements**

`scripts/requirements.txt` currently has `jsonschema` commented out under "Optional". Move it to
Core:

```text
jsonschema>=4.20,<5.0          # evidence run-fragment validation (validate-harness-coverage)
```

- [ ] **Step 2: Write `modules.yml` with the seven declared modules**

```yaml
# Which harness module implements which archetype. The traceability gate reads this.
# tool MUST equal the archetype's declared best fit in TST-010.
version: 1
modules:
  - archetype: TST-021
    tool: jmeter
    path: qe-harness/harness/jmeter/tst-021-ledger
    coverage: full
    defect_flag: ledger-unbalanced
  - archetype: TST-030
    tool: gatling-karate
    path: qe-harness/harness/gatling-karate/tst-030-contract
    coverage: full
    defect_flag: schema-drift
  - archetype: TST-031
    tool: jmeter
    path: qe-harness/harness/jmeter/tst-031-ratelimit
    coverage: full
    defect_flag: ratelimit-leaky
  - archetype: TST-035
    tool: jmeter
    path: qe-harness/harness/jmeter/tst-035-faultinjection
    coverage: full
    defect_flag: breaker-disabled
  - archetype: TST-039
    tool: locust
    path: qe-harness/harness/locust/tst_039_recon
    coverage: full
    defect_flag: recon-false-clean
  - archetype: TST-040
    tool: jmeter
    path: qe-harness/harness/jmeter/tst-040-authz
    coverage: full
    defect_flag: authz-missing-marker
  - archetype: TST-043
    tool: k6
    path: qe-harness/harness/k6/tst-043-clientexp
    coverage: partial
    partial_reason: >-
      Offline-sync invariants require a client application, which this repository does not
      contain. Perf budget, cache correctness, conditional requests, and compression only.
    defect_flag: cache-headers-absent
```

- [ ] **Step 3: Write the failing test**

`scripts/tests/test_validate_harness_coverage.py`:

```python
import subprocess, sys, textwrap, pathlib

SCRIPT = pathlib.Path(__file__).resolve().parents[1] / "validate-harness-coverage.py"

def run(tmp_root):
    return subprocess.run([sys.executable, str(SCRIPT), "--root", str(tmp_root)],
                          capture_output=True, text=True)

def test_flags_tool_mismatch(tmp_path, monkeypatch):
    # A module claiming locust for TST-021, whose declared best fit is jmeter.
    mods = tmp_path / "qe-harness/traceability"
    mods.mkdir(parents=True)
    (mods / "modules.yml").write_text(textwrap.dedent("""
        version: 1
        modules:
          - archetype: TST-021
            tool: locust
            path: qe-harness/harness/locust/tst_021
            coverage: full
            defect_flag: ledger-unbalanced
    """))
    result = run(tmp_path)
    assert result.returncode == 1
    assert "tool mismatch" in result.stdout

def test_flags_pan_shaped_string(tmp_path):
    bad = tmp_path / "qe-harness/harness/jmeter/seed.csv"
    bad.parent.mkdir(parents=True)
    bad.write_text("account,pan\nACC-000001,4111111111111111\n")
    result = run(tmp_path)
    assert result.returncode == 1
    assert "PAN-shaped" in result.stdout

def test_flags_partial_without_reason(tmp_path):
    mods = tmp_path / "qe-harness/traceability"
    mods.mkdir(parents=True)
    (mods / "modules.yml").write_text(textwrap.dedent("""
        version: 1
        modules:
          - archetype: TST-043
            tool: k6
            path: qe-harness/harness/k6/tst-043
            coverage: partial
            defect_flag: cache-headers-absent
    """))
    result = run(tmp_path)
    assert result.returncode == 1
    assert "partial coverage without partial_reason" in result.stdout
```

- [ ] **Step 4: Run it and confirm it fails**

Run: `python3 -m pytest scripts/tests/test_validate_harness_coverage.py -v`
Expected: FAIL — script does not exist.

- [ ] **Step 5: Write the gate**

`scripts/validate-harness-coverage.py`, following the house style of
`scripts/validate-testing-coverage.py` (module docstring listing checks, `ROOT` constant,
`--quiet`, exit `2` on malformed input):

```python
#!/usr/bin/env python3
"""Validate the QE harness against the Wave 15 testing corpus.

Six checks:
  1. Every modules.yml archetype exists as an archetype document.
  2. Every module's tool equals that archetype's declared best fit in TST-010.
  3. Every module's path exists on disk.
  4. coverage: partial requires a non-empty partial_reason.
  5. No PAN-shaped string (13-19 consecutive digits) anywhere under qe-harness/.
  6. Every threshold_ref in profiles/_nfr-thresholds.yml cites an existing
     NFR-* row and a heading anchor that resolves in that document.

Usage:
    python3 scripts/validate-harness-coverage.py
    python3 scripts/validate-harness-coverage.py --quiet
    python3 scripts/validate-harness-coverage.py --root /path/to/tree
"""
```

Implementation notes the implementer must honour:

- Check 2 reads best fit from `knowledge-base/testing/tooling/tool-selection-matrix.md`. If that
  document does not express per-archetype best fit in a machine-readable form, **do not guess and
  do not edit the document** (Global Constraints forbid amending the corpus). Instead read it
  from `knowledge-base/testing/coverage/_testing-coverage.yml`'s `primary_tool` for the rows the
  archetype covers, and if that is still ambiguous, report `⚠️ cannot verify` for that module and
  escalate — a silently-skipped check is worse than a reported one.
- Check 5 regex: `re.compile(r"(?<!\d)\d{13,19}(?!\d)")`. Skip `qe-harness/traceability/runs/`
  and any `target/`, `node_modules/`, `.venv/` directory. A hit prints file, line, and the
  matched span's length — never the matched digits themselves.
- Check 6 reuses anchor logic from `scripts/validate-internal-links.py`; import it rather than
  reimplementing slugification.

- [ ] **Step 6: Run the tests**

Run: `python3 -m pytest scripts/tests/test_validate_harness_coverage.py -v`
Expected: PASS, 3 tests.

- [ ] **Step 7: Run the gate against the real tree**

Run: `python3 scripts/validate-harness-coverage.py`
Expected: exit `1` with findings that every module path is missing — Tasks 16–22 have not run
yet. That is correct behaviour. Record the finding count as the starting deficit.

- [ ] **Step 8: Commit**

```bash
git add scripts/validate-harness-coverage.py scripts/tests/ scripts/requirements.txt \
        qe-harness/traceability/modules.yml
git commit -m "feat(qe-harness): add traceability gate with six checks"
```

---

## Task 4: NFR Threshold Projection

**Files:**
- Create: `qe-harness/profiles/_nfr-thresholds.yml`
- Create: `qe-harness/profiles/baseline.yml`, `load.yml`, `stress.yml`, `spike.yml`,
  `soak.yml`, `mixed.yml`, `scalability.yml`, `failover-under-load.yml`

**Interfaces:**
- Consumes: Task 3's check 6
- Produces: `threshold_ref` keys that Tasks 16–22 cite by name; the eight profile files
  `TST-002` defines

- [ ] **Step 1: Find the real NFR rows and anchors**

```bash
ls knowledge-base/nfr/
/usr/bin/grep -n "^## " knowledge-base/nfr/*.md | head -40
/usr/bin/grep -n "^- id: NFR-" -A 3 governance/standards/_catalog-inventory.yml | head -40
```

Use only anchors that actually appear. **Do not invent an `NFR-*` ID or a heading.** If no NFR
row carries a suitable latency or throughput heading, report that as a finding and escalate
rather than fabricating a citation — a resolvable-but-wrong citation is the exact failure the
gate exists to prevent.

- [ ] **Step 2: Write `_nfr-thresholds.yml` from what you found**

Shape, with the values replaced by real ones from Step 1:

```yaml
# Machine-readable projection of NFR numeric targets.
#
# The gate proves each threshold_ref resolves to a real NFR row and heading anchor.
# It does NOT prove the value below matches that document's prose — see qe-harness/README.md.
# A human owns value accuracy. Update this file whenever the cited NFR row changes.
version: 1
thresholds:
  - name: p99_latency_ms
    threshold_ref: NFR-001#latency-targets
    value: 250
    unit: ms
    applies_to: [TST-031, TST-035, TST-043]
```

- [ ] **Step 3: Write the eight profile files**

Each profile file declares only shape, never a hardcoded number — numbers come from
`_nfr-thresholds.yml` by name. `baseline.yml`:

```yaml
profile: baseline
workload_model: closed        # closed | open — see TST-003
duration_seconds: 300
ramp_seconds: 60
threshold_names: [p99_latency_ms, error_rate_pct]
smoke_mode_overrides:
  duration_seconds: 20
  ramp_seconds: 5
  thresholds: not-evaluated   # correctness invariants still assert; perf does not
```

Repeat with the shape appropriate to each of `load`, `stress`, `spike`, `soak`, `mixed`,
`scalability`, `failover-under-load`, taking each profile's intent from
`knowledge-base/testing/strategy/performance-test-standard.md`. Read that document; do not
improvise the profile semantics.

- [ ] **Step 4: Verify check 6 passes**

Run: `python3 scripts/validate-harness-coverage.py 2>&1 | /usr/bin/grep -i threshold`
Expected: no threshold findings. Module-path findings remain until Tasks 16–22.

- [ ] **Step 5: Commit**

```bash
git add qe-harness/profiles/
git commit -m "feat(qe-harness): add NFR threshold projection and eight TST-002 profiles"
```

---

## Task 5: Reference SUT Skeleton and Capability Registry

**Files:**
- Create: `qe-harness/reference-sut/pom.xml`
- Create: `…/sut/ReferenceSutApplication.java`, `…/sut/CapabilityRegistry.java`,
  `…/sut/CapabilityController.java`, `…/sut/DefectFlags.java`
- Test: `…/sut/CapabilityRegistryTest.java`, `…/sut/CapabilityControllerTest.java`

**Interfaces:**
- Consumes: nothing from earlier tasks (the SUT is independent of the harness)
- Produces: `GET /_capabilities` → `{"TST-020": "declared", "TST-021": "implemented", …}` for all
  24 IDs; `DefectFlags.active()` → `Optional<String>`; `CapabilityRegistry.IMPLEMENTED` set that
  Tasks 6–13 each add to

- [ ] **Step 1: Write the failing tests**

```java
package com.techcombank.qe.sut;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CapabilityRegistryTest {

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
    void statusOfDeclaredButUnimplementedIsDeclared() {
        assertEquals("declared", CapabilityRegistry.statusOf("TST-022"));
    }
}
```

`CapabilityControllerTest` is a `@WebMvcTest` asserting `GET /_capabilities` returns 200 with 24
keys, and that `GET /capability/TST-022/probe` returns **501** with a body containing `TST-022`.

- [ ] **Step 2: Run and confirm failure**

Run: `cd qe-harness/reference-sut && mvn -q test`
Expected: FAIL — classes absent.

- [ ] **Step 3: Implement**

```java
package com.techcombank.qe.sut;

import java.util.*;
import java.util.stream.*;

public final class CapabilityRegistry {

    /** All 24 archetypes, TST-020..TST-043 contiguous. Wave 15 closed every ID gap. */
    public static final Set<String> ALL = IntStream.rangeClosed(20, 43)
        .mapToObj(n -> "TST-0" + n)
        .collect(Collectors.toUnmodifiableSet());

    /** Implemented in Wave 16. Tasks 6-13 each add exactly one ID here. */
    public static final Set<String> IMPLEMENTED = Set.of();

    private CapabilityRegistry() {}

    public static String statusOf(String archetype) {
        if (!ALL.contains(archetype)) {
            throw new IllegalArgumentException("unknown archetype: " + archetype);
        }
        return IMPLEMENTED.contains(archetype) ? "implemented" : "declared";
    }

    public static Map<String, String> statusMap() {
        return ALL.stream().sorted()
            .collect(Collectors.toMap(a -> a, CapabilityRegistry::statusOf,
                                      (a, b) -> a, LinkedHashMap::new));
    }
}
```

`DefectFlags.java` reads `SUT_DEFECT` once at startup, validates it against a known set, and
**fails startup on an unrecognised value** — a typo'd defect flag must not silently produce a
clean SUT, or Task 27's defect proof would pass vacuously.

`CapabilityController.java` exposes `GET /_capabilities` returning `statusMap()`, and a
`/capability/{archetype}/probe` endpoint returning `501 Not Implemented` with body
`{"archetype": "...", "status": "declared", "wave": "17+"}` for anything not in `IMPLEMENTED`.

- [ ] **Step 4: Run the tests**

Run: `cd qe-harness/reference-sut && mvn -q test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): add capability registry enumerating all 24 archetypes"
```

---

## Task 6: Postgres Schema and Synthetic Data Seeder

**Files:**
- Create: `…/reference-sut/src/main/resources/db/migration/V1__accounts_and_ledger.sql`
- Create: `…/sut/data/SyntheticDataSeeder.java`, `…/sut/data/SyntheticNames.java`
- Test: `…/sut/data/SyntheticDataSeederTest.java`

**Interfaces:**
- Consumes: `ReferenceSutApplication` from Task 5
- Produces: tables `account`, `ledger_entry`; `SyntheticDataSeeder.seed(long seed)` →
  `SeedSummary(int accounts, int entries)`. Tasks 7 and 12 read these tables.

- [ ] **Step 1: Write the failing test**

```java
@Test
void seedIsDeterministicForTheSameSeed() {
    SeedSummary a = seeder.seed(42L);
    truncate();
    SeedSummary b = seeder.seed(42L);
    assertEquals(a, b);
}

@Test
void noAccountIdentifierIsPanShaped() {
    seeder.seed(42L);
    List<String> ids = jdbc.queryForList("SELECT account_ref FROM account", String.class);
    Pattern pan = Pattern.compile("(?<!\\d)\\d{13,19}(?!\\d)");
    ids.forEach(id -> assertFalse(pan.matcher(id).find(), "PAN-shaped: " + id));
    ids.forEach(id -> assertTrue(id.matches("^ACC-\\d{6}$"), "bad format: " + id));
}

@Test
void seededLedgerIsBalanced() {
    seeder.seed(42L);
    Long net = jdbc.queryForObject(
        "SELECT COALESCE(SUM(amount_minor), 0) FROM ledger_entry", Long.class);
    assertEquals(0L, net, "seed must not start the ledger out of balance");
}
```

- [ ] **Step 2: Run and confirm failure**

Run: `cd qe-harness/reference-sut && mvn -q test -Dtest=SyntheticDataSeederTest`
Expected: FAIL.

- [ ] **Step 3: Write the migration**

```sql
CREATE TABLE account (
    id           BIGSERIAL PRIMARY KEY,
    account_ref  VARCHAR(16) NOT NULL UNIQUE,
    party_name   VARCHAR(64) NOT NULL,
    CONSTRAINT account_ref_format CHECK (account_ref ~ '^ACC-[0-9]{6}$')
);

CREATE TABLE ledger_entry (
    id            BIGSERIAL PRIMARY KEY,
    transfer_ref  UUID        NOT NULL,
    account_id    BIGINT      NOT NULL REFERENCES account(id),
    amount_minor  BIGINT      NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT amount_nonzero CHECK (amount_minor <> 0)
);

CREATE INDEX ledger_entry_transfer_ref_idx ON ledger_entry (transfer_ref);
```

The `account_ref_format` CHECK constraint enforces the synthetic-identifier rule at the database
level, so no code path — including an injected defect — can write a PAN-shaped identifier.

- [ ] **Step 4: Implement the seeder**

`SyntheticNames` holds a fixed array of 20 invented party names (`"Aurora Trading"`,
`"Beacon Holdings"`, …) — invented organisations, never person names. `SyntheticDataSeeder` uses
`new Random(seed)` so runs are reproducible, generates `ACC-%06d` refs, and writes ledger entries
in balanced debit/credit pairs sharing one `transfer_ref`.

- [ ] **Step 5: Run the tests**

Run: `cd qe-harness/reference-sut && mvn -q test -Dtest=SyntheticDataSeederTest`
Expected: PASS, 3 tests.

- [ ] **Step 6: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): add ledger schema and deterministic synthetic seeder"
```

---

## Task 7: SUT Capability — TST-021 Double-Entry Ledger

**Files:**
- Create: `…/sut/capability/ledger/LedgerController.java`, `TransferService.java`,
  `TrialBalanceService.java`
- Modify: `…/sut/CapabilityRegistry.java` — add `TST-021` to `IMPLEMENTED`
- Test: `…/sut/capability/ledger/TransferServiceTest.java`,
  `LedgerConcurrencyTest.java`

**Interfaces:**
- Consumes: `account`, `ledger_entry` from Task 6; `DefectFlags` from Task 5
- Produces: `POST /transfers` `{from, to, amountMinor}` → `201 {transferRef}`;
  `GET /ledger/trial-balance` → `{netMinor, entryCount}`. Task 16's JMeter module drives these.

- [ ] **Step 1: Write the failing tests**

```java
@Test
void transferWritesBalancedPairInOneTransaction() {
    UUID ref = service.transfer("ACC-000001", "ACC-000002", 5_00L);
    List<Long> amounts = jdbc.queryForList(
        "SELECT amount_minor FROM ledger_entry WHERE transfer_ref = ?", Long.class, ref);
    assertEquals(2, amounts.size());
    assertEquals(0L, amounts.stream().mapToLong(Long::longValue).sum());
}

@Test
void trialBalanceStaysZeroUnderConcurrentTransfers() throws Exception {
    // 200 concurrent transfers across a shared pair of accounts.
    ExecutorService pool = Executors.newFixedThreadPool(16);
    List<Future<?>> futures = IntStream.range(0, 200)
        .mapToObj(i -> pool.submit(() -> service.transfer("ACC-000001", "ACC-000002", 1_00L)))
        .toList();
    for (Future<?> f : futures) f.get();
    pool.shutdown();
    assertEquals(0L, trialBalance.net(), "trial balance must net to zero");
}

@Test
void defectFlagOmitsCreditLegUnderConcurrency() {
    // With SUT_DEFECT=ledger-unbalanced the credit leg is dropped, so the
    // trial balance MUST drift. This proves the defect is actually injected.
    withDefect("ledger-unbalanced", () -> {
        service.transfer("ACC-000001", "ACC-000002", 5_00L);
        assertNotEquals(0L, trialBalance.net());
    });
}
```

- [ ] **Step 2: Run and confirm failure**

Run: `cd qe-harness/reference-sut && mvn -q test -Dtest='*Ledger*,TransferServiceTest'`
Expected: FAIL.

- [ ] **Step 3: Implement**

`TransferService.transfer` is `@Transactional`, inserts the debit and credit rows under one
transaction, and takes the two account rows in a **deterministic order by `account_id`** using
`SELECT … FOR UPDATE` to avoid deadlock under the concurrency test.

The defect path, guarded by `DefectFlags`:

```java
@Transactional
public UUID transfer(String from, String to, long amountMinor) {
    UUID ref = UUID.randomUUID();
    long fromId = lockAccount(from), toId = lockAccount(to);   // ordered inside lockPair
    insertEntry(ref, fromId, -amountMinor);
    if (!DefectFlags.isActive("ledger-unbalanced")) {
        insertEntry(ref, toId, amountMinor);
    }
    return ref;
}
```

- [ ] **Step 4: Run the tests**

Run: `cd qe-harness/reference-sut && mvn -q test -Dtest='*Ledger*,TransferServiceTest'`
Expected: PASS, 3 tests.

- [ ] **Step 5: Register the capability**

In `CapabilityRegistry`, change `IMPLEMENTED` to `Set.of("TST-021")`. Re-run
`CapabilityRegistryTest` — `statusOf("TST-021")` must now be `implemented`.

- [ ] **Step 6: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): implement TST-021 double-entry ledger capability"
```

---

## Task 8: SUT Capability — TST-031 Token-Bucket Rate Limiter

**Files:**
- Create: `…/sut/capability/ratelimit/RateLimitFilter.java`, `TokenBucket.java`,
  `RateLimitedController.java`
- Modify: `…/sut/CapabilityRegistry.java` — add `TST-031`
- Test: `…/sut/capability/ratelimit/TokenBucketTest.java`

**Interfaces:**
- Consumes: `DefectFlags`
- Produces: `GET /rate-limited/ping` → `200` under the configured rate, `429` with a
  `Retry-After` header above it. Configured rate from `app.ratelimit.permits-per-second`.
  Task 17's JMeter module drives it.

- [ ] **Step 1: Write the failing tests**

```java
@Test
void admitsNoMoreThanConfiguredRate() {
    TokenBucket bucket = new TokenBucket(10, Duration.ofSeconds(1), clock);
    int admitted = 0;
    for (int i = 0; i < 100; i++) if (bucket.tryAcquire()) admitted++;
    assertEquals(10, admitted, "bucket must not admit above its configured rate");
}

@Test
void rejectionCarriesRetryAfter() throws Exception {
    exhaustBucket();
    mvc.perform(get("/rate-limited/ping"))
       .andExpect(status().isTooManyRequests())
       .andExpect(header().exists("Retry-After"));
}

@Test
void neverReturnsServerErrorUnderOverload() throws Exception {
    for (int i = 0; i < 500; i++) {
        int sc = mvc.perform(get("/rate-limited/ping")).andReturn().getResponse().getStatus();
        assertTrue(sc == 200 || sc == 429, "unexpected status under overload: " + sc);
    }
}

@Test
void defectFlagAdmitsAboveConfiguredRate() {
    withDefect("ratelimit-leaky", () -> {
        TokenBucket bucket = new TokenBucket(10, Duration.ofSeconds(1), clock);
        int admitted = 0;
        for (int i = 0; i < 100; i++) if (bucket.tryAcquire()) admitted++;
        assertTrue(admitted > 10, "leaky defect must admit above the rate");
    });
}
```

- [ ] **Step 2: Run and confirm failure**

Run: `cd qe-harness/reference-sut && mvn -q test -Dtest='TokenBucketTest,RateLimit*'`
Expected: FAIL.

- [ ] **Step 3: Implement**

`TokenBucket` refills lazily from an injected `Clock` (injected so the test is deterministic
rather than sleeping). `tryAcquire()` is synchronised on the bucket. The `ratelimit-leaky` defect
skips the capacity check. `RateLimitFilter` returns `429` with `Retry-After` set to the whole
seconds until the next refill.

- [ ] **Step 4: Run the tests**

Run: `cd qe-harness/reference-sut && mvn -q test -Dtest='TokenBucketTest,RateLimit*'`
Expected: PASS, 4 tests.

- [ ] **Step 5: Register the capability**

`IMPLEMENTED` becomes `Set.of("TST-021", "TST-031")`.

- [ ] **Step 6: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): implement TST-031 token-bucket rate limiter capability"
```

---

## Task 9: SUT Capability — TST-040 AuthN/AuthZ with Decision Marker

The decision marker is load-bearing: `TST-040` classifies a bare `403` carrying no marker as
`error`, **not** `deny`. Without the marker the harness cannot tell a correct denial from an
unhandled failure.

**Files:**
- Create: `…/sut/capability/authz/SecurityConfig.java`, `AuthzDecisionFilter.java`,
  `ProtectedController.java`, `TokenController.java`
- Modify: `…/sut/CapabilityRegistry.java` — add `TST-040`
- Test: `…/sut/capability/authz/AuthzMatrixTest.java`, `TokenLifecycleTest.java`

**Interfaces:**
- Consumes: `DefectFlags`
- Produces: `POST /auth/token`, `POST /auth/refresh`, `POST /auth/revoke`; protected endpoints
  `/protected/{read,write,admin}`; **every** authorisation response carries header
  `X-Authz-Decision: allow | deny`. Task 19's JMeter module drives these.

- [ ] **Step 1: Write the failing tests**

```java
@Test
void everyMatrixCellCarriesADecisionMarker() throws Exception {
    for (String role : List.of("reader", "writer", "admin", "anonymous")) {
        for (String ep : List.of("/protected/read", "/protected/write", "/protected/admin")) {
            MockHttpServletResponse r = call(role, ep);
            assertTrue(r.containsHeader("X-Authz-Decision"),
                "no decision marker for " + role + " -> " + ep
                + " (a bare " + r.getStatus() + " is an error, not a deny)");
            assertTrue(Set.of("allow", "deny").contains(r.getHeader("X-Authz-Decision")));
        }
    }
}

@Test
void revokedTokenIsRejectedWithAnExplicitDeny() throws Exception {
    String token = issue("reader");
    revoke(token);
    MockHttpServletResponse r = callWithToken(token, "/protected/read");
    assertEquals(401, r.getStatus());
    assertEquals("deny", r.getHeader("X-Authz-Decision"));
}

@Test
void expiredTokenIsNotAcceptedBeyondDeclaredSkew() throws Exception {
    // Measures the actual accepted exp offset rather than asserting a configured value.
    long maxAccepted = probeMaxAcceptedExpOffsetSeconds();
    assertTrue(maxAccepted <= DECLARED_CLOCK_SKEW_SECONDS,
        "accepted exp offset " + maxAccepted + "s exceeds declared tolerance");
}

@Test
void defectFlagStripsTheDecisionMarker() throws Exception {
    withDefect("authz-missing-marker", () -> {
        MockHttpServletResponse r = call("anonymous", "/protected/admin");
        assertFalse(r.containsHeader("X-Authz-Decision"));
    });
}
```

- [ ] **Step 2: Run and confirm failure**

Run: `cd qe-harness/reference-sut && mvn -q test -Dtest='Authz*,TokenLifecycleTest'`
Expected: FAIL.

- [ ] **Step 3: Implement**

`AuthzDecisionFilter` runs after Spring Security's filter chain and sets `X-Authz-Decision` from
the resolved authentication outcome — `allow` on 2xx, `deny` on a security-originated 401/403.
It deliberately does **not** set the header when the failure came from anywhere else, so a
genuine server error stays distinguishable. The `authz-missing-marker` defect disables the
filter entirely.

`DECLARED_CLOCK_SKEW_SECONDS` is read from `app.authz.clock-skew-seconds` and documented in
`qe-harness/README.md`; it is not a performance threshold, so it does not need an `NFR-*`
citation.

- [ ] **Step 4: Run the tests**

Run: `cd qe-harness/reference-sut && mvn -q test -Dtest='Authz*,TokenLifecycleTest'`
Expected: PASS, 4 tests.

- [ ] **Step 5: Register the capability**

`IMPLEMENTED` becomes `Set.of("TST-021", "TST-031", "TST-040")`.

- [ ] **Step 6: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): implement TST-040 authz matrix with explicit decision marker"
```

---

## Task 10: SUT Capability — TST-030 Versioned API and Breaking-Change Fixture

**Files:**
- Create: `…/sut/capability/contract/TransferV1Controller.java`,
  `TransferV2Controller.java`, `OpenApiConfig.java`
- Create: `qe-harness/reference-sut/src/main/resources/contracts/transfer-v1.schema.json`,
  `transfer-v2.schema.json`, `transfer-v2-breaking.schema.json`
- Modify: `…/sut/CapabilityRegistry.java` — add `TST-030`
- Test: `…/sut/capability/contract/SchemaCompatibilityTest.java`

**Interfaces:**
- Consumes: `DefectFlags`
- Produces: `POST /v1/transfers`, `POST /v2/transfers`, `GET /openapi.json`. Task 20's
  Karate + Gatling module drives these.

- [ ] **Step 1: Write the failing test**

```java
@Test
void v2ResponseSatisfiesItsPublishedSchema() throws Exception {
    String body = post("/v2/transfers", validRequest());
    Set<ValidationMessage> errors = schema("transfer-v2.schema.json").validate(json(body));
    assertTrue(errors.isEmpty(), "v2 response violates its own schema: " + errors);
}

@Test
void v1ResponseRemainsBackwardCompatible() throws Exception {
    // BACKWARD compatibility: every field v1 declared is still present.
    String body = post("/v1/transfers", validRequest());
    Set<ValidationMessage> errors = schema("transfer-v1.schema.json").validate(json(body));
    assertTrue(errors.isEmpty(), "v1 contract broken: " + errors);
}

@Test
void defectFlagRenamesAFieldAndBreaksTheContract() throws Exception {
    withDefect("schema-drift", () -> {
        String body = post("/v2/transfers", validRequest());
        Set<ValidationMessage> errors = schema("transfer-v2.schema.json").validate(json(body));
        assertFalse(errors.isEmpty(), "schema-drift defect must break the published contract");
    });
}
```

- [ ] **Step 2: Run and confirm failure**

Run: `cd qe-harness/reference-sut && mvn -q test -Dtest=SchemaCompatibilityTest`
Expected: FAIL.

- [ ] **Step 3: Implement**

`/v2/transfers` returns `{transferRef, status, settledAt}`; `/v1/transfers` returns
`{transferRef, status}`. The `schema-drift` defect renames `transferRef` to `transfer_id` in the
v2 response only — a rename, not a removal, because a rename is the failure mode a naive
field-count check misses.

- [ ] **Step 4: Run the tests**

Run: `cd qe-harness/reference-sut && mvn -q test -Dtest=SchemaCompatibilityTest`
Expected: PASS, 3 tests.

- [ ] **Step 5: Register the capability**

`IMPLEMENTED` gains `"TST-030"`.

- [ ] **Step 6: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): implement TST-030 versioned API and breaking-change fixture"
```

---

## Task 11: SUT Capability — TST-035 Circuit Breaker and Degraded Response

**Files:**
- Create: `…/sut/capability/resilience/DownstreamClient.java`,
  `QuoteController.java`, `DegradedResponse.java`
- Create: `qe-harness/downstream-stub/` (a 20-line static responder image config)
- Modify: `…/sut/CapabilityRegistry.java` — add `TST-035`
- Test: `…/sut/capability/resilience/BreakerBehaviourTest.java`

**Interfaces:**
- Consumes: `DefectFlags`; a downstream base URL from `app.downstream.base-url` (Toxiproxy in
  compose)
- Produces: `GET /quotes/{id}` → `200` with live data, or `200` with
  `{"degraded": true, "source": "cache"}` when the breaker is open. **Never** `5xx` on downstream
  failure. Task 18's JMeter module drives it.

- [ ] **Step 1: Write the failing tests**

```java
@Test
void downstreamFailureYieldsDeclaredDegradedResponseNotAnError() {
    downstream.blackhole();
    ResponseEntity<Map> r = rest.getForEntity("/quotes/Q1", Map.class);
    assertEquals(200, r.getStatusCode().value(), "downstream failure must not surface as 5xx");
    assertEquals(true, r.getBody().get("degraded"));
}

@Test
void breakerOpensWithinDeclaredThreshold() {
    downstream.blackhole();
    int callsUntilOpen = callUntilBreakerOpen();
    assertTrue(callsUntilOpen <= BREAKER_MINIMUM_CALLS + BREAKER_SLACK,
        "breaker took " + callsUntilOpen + " calls to open");
}

@Test
void breakerClosesAfterFaultRemoved() {
    downstream.blackhole();
    callUntilBreakerOpen();
    downstream.restore();
    awaitClosed(Duration.ofSeconds(20));
    assertEquals(false, rest.getForEntity("/quotes/Q1", Map.class).getBody().get("degraded"));
}

@Test
void defectFlagLetsDownstreamFailureSurfaceAsFiveHundred() {
    withDefect("breaker-disabled", () -> {
        downstream.blackhole();
        assertEquals(500, rest.getForEntity("/quotes/Q1", Map.class).getStatusCode().value());
    });
}
```

- [ ] **Step 2: Run and confirm failure**

Run: `cd qe-harness/reference-sut && mvn -q test -Dtest=BreakerBehaviourTest`
Expected: FAIL.

- [ ] **Step 3: Implement**

Resilience4j `@CircuitBreaker` on `DownstreamClient.fetch`, with a `fallbackMethod` returning
`DegradedResponse`. The `breaker-disabled` defect removes the fallback so the exception
propagates. Breaker config (`minimumNumberOfCalls`, `failureRateThreshold`,
`waitDurationInOpenState`) lives in `application.yml` and is documented in the README — these are
resilience configuration, not performance thresholds, so no `NFR-*` citation is required.

- [ ] **Step 4: Run the tests**

Run: `cd qe-harness/reference-sut && mvn -q test -Dtest=BreakerBehaviourTest`
Expected: PASS, 4 tests.

- [ ] **Step 5: Register the capability**

`IMPLEMENTED` gains `"TST-035"`.

- [ ] **Step 6: Commit**

```bash
git add qe-harness/reference-sut qe-harness/downstream-stub
git commit -m "feat(sut): implement TST-035 breaker with declared degraded response"
```

---

## Task 12: SUT Capability — TST-039 Reconciliation with Seeded Defects

**Files:**
- Create: `…/reference-sut/src/main/resources/db/migration/V2__reporting_view.sql`
- Create: `…/sut/capability/recon/ReconController.java`, `ReconService.java`,
  `DefectSeeder.java`
- Modify: `…/sut/CapabilityRegistry.java` — add `TST-039`
- Test: `…/sut/capability/recon/ReconServiceTest.java`

**Interfaces:**
- Consumes: `account`, `ledger_entry` from Task 6
- Produces: `GET /recon/report` →
  `{"completeness": {...}, "accuracy": {...}, "timeliness": {...}}` each with
  `{"checked": n, "defects": [ids]}`; `POST /recon/seed-defects` seeds a known defect set.
  Task 21's Locust module scores this as a confusion matrix.

- [ ] **Step 1: Write the failing tests**

```java
@Test
void reportFindsEverySeededDefect() {
    SeededDefects seeded = defectSeeder.seed(42L);   // known ids per dimension
    ReconReport report = service.report();
    assertEquals(seeded.completeness(), report.completeness().defects());
    assertEquals(seeded.accuracy(),     report.accuracy().defects());
    assertEquals(seeded.timeliness(),   report.timeliness().defects());
}

@Test
void reportFindsNothingOnCleanData() {
    ReconReport report = service.report();
    assertTrue(report.completeness().defects().isEmpty());
    assertTrue(report.accuracy().defects().isEmpty());
    assertTrue(report.timeliness().defects().isEmpty());
}

@Test
void defectFlagReportsCleanDespiteSeededDefects() {
    withDefect("recon-false-clean", () -> {
        defectSeeder.seed(42L);
        assertTrue(service.report().accuracy().defects().isEmpty(),
            "false-clean defect must hide real defects");
    });
}
```

The first two tests are what let Task 21 build a real confusion matrix: seeded truth is known,
so false positives and false negatives are both measurable.

- [ ] **Step 2: Run and confirm failure**

Run: `cd qe-harness/reference-sut && mvn -q test -Dtest=ReconServiceTest`
Expected: FAIL.

- [ ] **Step 3: Implement**

`V2__reporting_view.sql` creates `account_balance_report` as a materialised view over
`ledger_entry`. `DefectSeeder` introduces exactly three defect classes: a missing report row
(completeness), a report row whose amount diverges (accuracy), and a report row whose
`refreshed_at` is older than the freshness window (timeliness). `recon-false-clean` makes
`ReconService` return empty defect lists.

- [ ] **Step 4: Run the tests**

Run: `cd qe-harness/reference-sut && mvn -q test -Dtest=ReconServiceTest`
Expected: PASS, 3 tests.

- [ ] **Step 5: Register the capability**

`IMPLEMENTED` gains `"TST-039"`.

- [ ] **Step 6: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): implement TST-039 reconciliation with seeded defect classes"
```

---

## Task 13: SUT Capability — TST-043 Cache, ETag and Payload Budget

**Files:**
- Create: `…/sut/capability/clientexp/CatalogueController.java`, `CachePolicyFilter.java`
- Modify: `…/sut/CapabilityRegistry.java` — add `TST-043`
- Test: `…/sut/capability/clientexp/CachePolicyTest.java`

**Interfaces:**
- Consumes: `DefectFlags`
- Produces: `GET /catalogue` → `200` with `Cache-Control`, `ETag`, and gzip when requested;
  `304` on a matching `If-None-Match`. Task 22's k6 module drives it.

- [ ] **Step 1: Write the failing tests**

```java
@Test
void responseCarriesCacheControlAndETag() throws Exception {
    mvc.perform(get("/catalogue"))
       .andExpect(status().isOk())
       .andExpect(header().exists("Cache-Control"))
       .andExpect(header().exists("ETag"));
}

@Test
void matchingIfNoneMatchYieldsNotModifiedWithNoBody() throws Exception {
    String etag = mvc.perform(get("/catalogue")).andReturn()
                     .getResponse().getHeader("ETag");
    mvc.perform(get("/catalogue").header("If-None-Match", etag))
       .andExpect(status().isNotModified())
       .andExpect(content().string(""));
}

@Test
void compressesWhenClientAcceptsGzip() throws Exception {
    mvc.perform(get("/catalogue").header("Accept-Encoding", "gzip"))
       .andExpect(header().string("Content-Encoding", "gzip"));
}

@Test
void defectFlagOmitsCacheHeaders() throws Exception {
    withDefect("cache-headers-absent", () ->
        mvc.perform(get("/catalogue"))
           .andExpect(header().doesNotExist("ETag"))
           .andExpect(header().doesNotExist("Cache-Control")));
}
```

- [ ] **Step 2: Run and confirm failure**

Run: `cd qe-harness/reference-sut && mvn -q test -Dtest=CachePolicyTest`
Expected: FAIL.

- [ ] **Step 3: Implement**

`CachePolicyFilter` computes a strong `ETag` from the response body hash and sets
`Cache-Control: public, max-age=60`. Spring Boot's `server.compression.enabled=true` handles
gzip. The `cache-headers-absent` defect disables the filter.

- [ ] **Step 4: Run the tests**

Run: `cd qe-harness/reference-sut && mvn -q test -Dtest=CachePolicyTest`
Expected: PASS, 4 tests.

- [ ] **Step 5: Register the capability and assert the final count**

`IMPLEMENTED` becomes all seven. Add to `CapabilityRegistryTest`:

```java
@Test
void waveSixteenImplementsExactlySevenCapabilities() {
    assertEquals(7, CapabilityRegistry.IMPLEMENTED.size());
    assertEquals(Set.of("TST-021","TST-030","TST-031","TST-035","TST-039","TST-040","TST-043"),
                 CapabilityRegistry.IMPLEMENTED);
}

@Test
void seventeenArchetypesRemainDeclared() {
    long declared = CapabilityRegistry.ALL.stream()
        .filter(a -> "declared".equals(CapabilityRegistry.statusOf(a))).count();
    assertEquals(17, declared);
}
```

Run: `cd qe-harness/reference-sut && mvn -q test`
Expected: PASS, whole suite.

- [ ] **Step 6: Commit**

```bash
git add qe-harness/reference-sut
git commit -m "feat(sut): implement TST-043 cache policy; complete the seven capabilities"
```

---

## Task 14: Docker Compose with Profiles and Health Gating

**Files:**
- Create: `qe-harness/docker-compose.yml`, `qe-harness/reference-sut/Dockerfile`
- Create: `qe-harness/bin/wait-for-sut.sh`

**Interfaces:**
- Consumes: the built SUT jar from Tasks 5–13
- Produces: `docker compose --profile core up -d --wait` yielding a healthy SUT on
  `http://localhost:8080`; Toxiproxy control API on `:8474` under the `resilience` profile

- [ ] **Step 1: Write the compose file**

Services: `postgres` and `reference-sut` and `oauth2-issuer` in profile `core`; `toxiproxy` and
`downstream-stub` in `resilience`; `otel-collector` and `prometheus` in `observability`; `broker`
in `messaging`. Every service declares a `healthcheck`, and `reference-sut` declares
`depends_on: {postgres: {condition: service_healthy}}`.

`reference-sut` passes `SUT_DEFECT: ${SUT_DEFECT:-}` through, so a defect run needs no compose
edit.

- [ ] **Step 2: Write `wait-for-sut.sh`**

```bash
#!/usr/bin/env bash
# Fail fast and once, rather than letting every assertion report connection-refused.
set -euo pipefail
URL="${1:-http://localhost:8080}/_capabilities"
for i in $(seq 1 60); do
  if curl -fsS "$URL" >/dev/null 2>&1; then echo "SUT ready"; exit 0; fi
  sleep 2
done
echo "SUT did not become ready at $URL after 120s" >&2
exit 1
```

This is infrastructure setup, so the bounded retry is permitted — Global Constraints forbid
retrying *assertions*, not readiness probes.

- [ ] **Step 3: Verify `core` comes up and reports seven capabilities**

```bash
cd qe-harness && make up PROFILES=core
./bin/wait-for-sut.sh
curl -s localhost:8080/_capabilities | python3 -c "import json,sys; d=json.load(sys.stdin); print(len(d), sum(1 for v in d.values() if v=='implemented'))"
```

Expected: `24 7`

- [ ] **Step 4: Verify a declared capability answers 501**

```bash
curl -s -o /dev/null -w '%{http_code}\n' localhost:8080/capability/TST-022/probe
```

Expected: `501`

- [ ] **Step 5: Verify the resilience profile**

```bash
cd qe-harness && make up PROFILES=resilience && curl -fsS localhost:8474/proxies >/dev/null && echo "toxiproxy up"
```

Expected: `toxiproxy up`

- [ ] **Step 6: Commit**

```bash
git add qe-harness/docker-compose.yml qe-harness/reference-sut/Dockerfile qe-harness/bin
git commit -m "feat(qe-harness): add profile-gated compose stack with health gating"
```

---

## Task 15: Harness Common — Oracles and Config

**Files:**
- Create: `…/harness/common/…/oracle/InvariantAssertion.java`, `GoldenDataset.java`,
  `ConfusionMatrix.java`, `ContractSchema.java`
- Create: `…/harness/common/…/config/HarnessConfig.java`,
  `…/config/ThresholdResolver.java`
- Test: `…/harness/common/…/oracle/ConfusionMatrixTest.java`,
  `…/config/ThresholdResolverTest.java`

**Interfaces:**
- Consumes: `RunFragment` from Task 2; `_nfr-thresholds.yml` from Task 4
- Produces: `InvariantAssertion.check(String id, String desc, BooleanSupplier)` →
  `RunFragment.Entry`; `ConfusionMatrix.score(Set expected, Set actual)` →
  `Score(tp, fp, fn, precision, recall)`; `ThresholdResolver.resolve(String name)` →
  `Threshold(value, unit, thresholdRef)`; `HarnessConfig.smokeMode()` → `boolean` from env
  `HARNESS_SMOKE_MODE`

- [ ] **Step 1: Write the failing tests**

```java
class ConfusionMatrixTest {
    @Test
    void scoresFalsePositivesAndNegativesSeparately() {
        var score = ConfusionMatrix.score(Set.of("A", "B", "C"), Set.of("B", "C", "D"));
        assertEquals(2, score.tp());   // B, C
        assertEquals(1, score.fp());   // D reported but not seeded
        assertEquals(1, score.fn());   // A seeded but not reported
    }

    @Test
    void perfectCleanRunScoresZeroEverywhere() {
        var score = ConfusionMatrix.score(Set.of(), Set.of());
        assertEquals(0, score.tp() + score.fp() + score.fn());
        assertTrue(Double.isNaN(score.precision()), "precision is undefined with no predictions");
    }
}

class ThresholdResolverTest {
    @Test
    void resolvesByNameAndCarriesItsCitation() {
        var t = resolver.resolve("p99_latency_ms");
        assertTrue(t.thresholdRef().matches("^NFR-\\d{3}#[a-z0-9-]+$"));
    }

    @Test
    void unknownNameThrowsRatherThanDefaulting() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("made_up_metric"));
    }
}
```

The `precision` NaN test matters: returning `1.0` for a run that predicted nothing would let a
broken reconciliation report look perfect.

- [ ] **Step 2: Run and confirm failure**

Run: `cd qe-harness/harness && mvn -q -pl common test`
Expected: FAIL.

- [ ] **Step 3: Implement**

`ThresholdResolver` loads `_nfr-thresholds.yml` and **throws** on an unknown name — never
defaults, because a silently-defaulted threshold is an invented threshold. `ConfusionMatrix`
returns `Double.NaN` for precision when `tp + fp == 0` and for recall when `tp + fn == 0`.

- [ ] **Step 4: Run the tests**

Run: `cd qe-harness/harness && mvn -q -pl common test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add qe-harness/harness/common
git commit -m "feat(harness): add oracle library and citation-carrying threshold resolver"
```

---

## Task 16: Module — TST-021 Ledger Invariant (JMeter)

**Files:**
- Create: `…/harness/jmeter/pom.xml`
- Create: `…/harness/jmeter/tst-021-ledger/plan.jmx`,
  `…/tst-021-ledger/assert-trial-balance.groovy`, `…/tst-021-ledger/README.md`
- Create: `qe-harness/bin/run-module.sh`
- Test: `…/harness/jmeter/src/test/java/…/Tst021ModuleTest.java`

**Interfaces:**
- Consumes: SUT `POST /transfers`, `GET /ledger/trial-balance` (Task 7); `EvidenceEmitter`
  (Task 2); `HarnessConfig` (Task 15)
- Produces: `run-module.sh TST-021` writing one fragment to `traceability/runs/`; the pattern
  Tasks 17–22 follow

- [ ] **Step 1: Write the module README naming its invariants**

`tst-021-ledger/README.md` lists the invariants this module asserts, each with the ID used in
the fragment:

```markdown
# TST-021 — Ledger & Monetary Invariant (JMeter)

Oracle: invariant-assertion. Best-fit tool per TST-010: JMeter.

| ID | Invariant |
|---|---|
| I1 | Trial balance nets to zero after every transfer batch |
| I2 | Every transfer_ref has exactly two ledger entries |
| I3 | No ledger entry has amount_minor = 0 |

Defect proof: with `SUT_DEFECT=ledger-unbalanced` this module MUST report I1 failed.
```

- [ ] **Step 2: Write the failing test**

```java
@Test
void reportsInvariantFailureAgainstTheUnbalancedDefect() throws Exception {
    ModuleResult r = runner.run("TST-021", Map.of("SUT_DEFECT", "ledger-unbalanced"));
    assertEquals(RunFragment.Result.FAILED, r.fragment().result());
    assertTrue(r.fragment().invariants().stream()
        .anyMatch(i -> i.id().equals("I1") && i.result() == RunFragment.Result.FAILED));
}

@Test
void passesAgainstTheCleanSut() throws Exception {
    ModuleResult r = runner.run("TST-021", Map.of());
    assertEquals(RunFragment.Result.PASSED, r.fragment().result());
}
```

- [ ] **Step 3: Run and confirm failure**

Run: `cd qe-harness/harness && mvn -q -pl jmeter test`
Expected: FAIL.

- [ ] **Step 4: Build the JMeter plan**

`plan.jmx` — a Thread Group driving `POST /transfers` with a Synchronizing Timer to force genuine
concurrency, then a **TearDownThreadGroup** running a single `GET /ledger/trial-balance` plus a
JDBC PostProcessor for I2 and I3. The teardown group is what guarantees the invariant is checked
after all load has drained; asserting mid-run would race.

`assert-trial-balance.groovy` (JSR223) reads the response, evaluates I1–I3, and calls
`EvidenceEmitter`.

- [ ] **Step 5: Write `run-module.sh`**

```bash
#!/usr/bin/env bash
# usage: run-module.sh TST-021
set -euo pipefail
ARCH="$1"
./bin/wait-for-sut.sh
MODULE_PATH="$(python3 - "$ARCH" <<'PY'
import sys, yaml, pathlib
mods = yaml.safe_load(pathlib.Path("traceability/modules.yml").read_text())["modules"]
print(next(m["path"] for m in mods if m["archetype"] == sys.argv[1]))
PY
)"
exec "./bin/run-$(basename "$(dirname "$MODULE_PATH")").sh" "$ARCH" "$MODULE_PATH"
```

Dispatching through `modules.yml` means the gate and the runner read one declaration, so a module
cannot be runnable but untraceable.

- [ ] **Step 6: Run the tests**

Run: `cd qe-harness/harness && mvn -q -pl jmeter test`
Expected: PASS, 2 tests.

- [ ] **Step 7: Verify the gate now sees this module**

Run: `python3 scripts/validate-harness-coverage.py 2>&1 | /usr/bin/grep TST-021`
Expected: no findings for `TST-021`.

- [ ] **Step 8: Commit**

```bash
git add qe-harness/harness/jmeter qe-harness/bin
git commit -m "feat(harness): add TST-021 ledger invariant JMeter module"
```

---

## Task 17: Module — TST-031 Rate Limit Breakpoint (JMeter, smoke-aware)

**Files:**
- Create: `…/harness/jmeter/tst-031-ratelimit/plan.jmx`, `assert-ratelimit.groovy`,
  `README.md`
- Modify: `qe-harness/traceability/modules.yml` — already declared in Task 3; verify path matches
- Test: `…/harness/jmeter/src/test/java/…/Tst031ModuleTest.java`

**Interfaces:**
- Consumes: SUT `GET /rate-limited/ping` (Task 8); `HarnessConfig.smokeMode()` (Task 15)
- Produces: a fragment whose `thresholds[]` entries are `not-evaluated` with
  `reason: "smoke-mode"` when `HARNESS_SMOKE_MODE=true`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void smokeModeRecordsThresholdsNotEvaluatedWithAReason() throws Exception {
    ModuleResult r = runner.run("TST-031", Map.of("HARNESS_SMOKE_MODE", "true"));
    assertFalse(r.fragment().thresholds().isEmpty(), "must still declare its thresholds");
    r.fragment().thresholds().forEach(t -> {
        assertEquals(RunFragment.Result.NOT_EVALUATED, t.result());
        assertEquals("smoke-mode", t.reason());
    });
}

@Test
void smokeModeStillAssertsCorrectnessInvariants() throws Exception {
    ModuleResult r = runner.run("TST-031", Map.of("HARNESS_SMOKE_MODE", "true"));
    assertTrue(r.fragment().invariants().stream()
        .anyMatch(i -> i.result() == RunFragment.Result.PASSED),
        "smoke mode must not skip correctness");
}

@Test
void reportsFailureAgainstTheLeakyDefect() throws Exception {
    ModuleResult r = runner.run("TST-031",
        Map.of("SUT_DEFECT", "ratelimit-leaky", "HARNESS_SMOKE_MODE", "true"));
    assertEquals(RunFragment.Result.FAILED, r.fragment().result());
}
```

The second test is the important one: smoke mode must degrade *what* is measured, never *whether*
correctness is checked.

- [ ] **Step 2: Run and confirm failure**

Run: `cd qe-harness/harness && mvn -q -pl jmeter test -Dtest=Tst031ModuleTest`
Expected: FAIL.

- [ ] **Step 3: Implement**

`plan.jmx` uses a **Throughput Shaping Timer** ramping past the configured rate to locate the
breakpoint. Invariants, per its README: `I1` admitted rate never exceeds configured rate; `I2`
every rejection carries `Retry-After`; `I3` no `5xx` at any load stage.

`assert-ratelimit.groovy` reads `HARNESS_SMOKE_MODE`; in smoke mode it shortens the ramp per
`profiles/stress.yml`'s `smoke_mode_overrides` and emits every threshold as `NOT_EVALUATED` with
reason `smoke-mode`, while still evaluating I1–I3.

- [ ] **Step 4: Run the tests**

Run: `cd qe-harness/harness && mvn -q -pl jmeter test -Dtest=Tst031ModuleTest`
Expected: PASS, 3 tests.

- [ ] **Step 5: Commit**

```bash
git add qe-harness/harness/jmeter
git commit -m "feat(harness): add TST-031 rate-limit breakpoint module with smoke mode"
```

---

## Task 18: Module — TST-035 Fault Injection (JMeter + Toxiproxy)

**Files:**
- Create: `…/harness/jmeter/tst-035-faultinjection/plan.jmx`, `assert-degradation.groovy`,
  `toxic-control.groovy`, `README.md`
- Test: `…/harness/jmeter/src/test/java/…/Tst035ModuleTest.java`

**Interfaces:**
- Consumes: SUT `GET /quotes/{id}` (Task 11); Toxiproxy control API on `:8474` (Task 14)
- Produces: a fragment asserting the degradation invariants

- [ ] **Step 1: Write the failing tests**

```java
@Test
void assertsDegradedResponseRatherThanFiveHundred() throws Exception {
    ModuleResult r = runner.run("TST-035", Map.of("HARNESS_SMOKE_MODE", "true"));
    assertEquals(RunFragment.Result.PASSED, r.fragment().result());
}

@Test
void reportsFailureAgainstTheBreakerDisabledDefect() throws Exception {
    ModuleResult r = runner.run("TST-035",
        Map.of("SUT_DEFECT", "breaker-disabled", "HARNESS_SMOKE_MODE", "true"));
    assertEquals(RunFragment.Result.FAILED, r.fragment().result());
}

@Test
void restoresTheProxyEvenWhenAssertionsFail() throws Exception {
    runner.run("TST-035", Map.of("SUT_DEFECT", "breaker-disabled",
                                 "HARNESS_SMOKE_MODE", "true"));
    assertTrue(toxiproxy.isClean(), "module must not leave the proxy in a faulted state");
}
```

The third test prevents the classic fault-injection bug: a failing run leaving the next module
running against a broken network.

- [ ] **Step 2: Run and confirm failure**

Run: `cd qe-harness/harness && mvn -q -pl jmeter test -Dtest=Tst035ModuleTest`
Expected: FAIL.

- [ ] **Step 3: Implement**

`toxic-control.groovy` adds and removes the Toxiproxy toxic via its HTTP API, invoked from a
setUp Thread Group and — critically — an unconditional **TearDownThreadGroup** so restoration
happens even on assertion failure.

Invariants per README: `I1` downstream failure never yields `5xx`; `I2` degraded response matches
the declared shape; `I3` breaker recovers after the fault is removed.

- [ ] **Step 4: Run the tests**

Run: `cd qe-harness/harness && mvn -q -pl jmeter test -Dtest=Tst035ModuleTest`
Expected: PASS, 3 tests.

- [ ] **Step 5: Commit**

```bash
git add qe-harness/harness/jmeter
git commit -m "feat(harness): add TST-035 fault-injection module with guaranteed restore"
```

---

## Task 19: Module — TST-040 AuthZ Matrix and Token Lifecycle (JMeter)

**Files:**
- Create: `…/harness/jmeter/tst-040-authz/plan.jmx`, `authz-matrix.csv`,
  `assert-authz.groovy`, `README.md`
- Test: `…/harness/jmeter/src/test/java/…/Tst040ModuleTest.java`

**Interfaces:**
- Consumes: SUT `/auth/*` and `/protected/*` with `X-Authz-Decision` (Task 9)
- Produces: a fragment whose `invariants[]` includes one entry per three-outcome classification
  rule

- [ ] **Step 1: Write `authz-matrix.csv` with an expected verdict per cell**

```csv
role,endpoint,expected_verdict
reader,/protected/read,allow
reader,/protected/write,deny
reader,/protected/admin,deny
writer,/protected/read,allow
writer,/protected/write,allow
writer,/protected/admin,deny
admin,/protected/read,allow
admin,/protected/write,allow
admin,/protected/admin,allow
anonymous,/protected/read,deny
anonymous,/protected/write,deny
anonymous,/protected/admin,deny
```

Assertions compare against `expected_verdict`, never against another code path's behaviour —
comparing gateway to direct would pass if both were wrong in the same way.

- [ ] **Step 2: Write the failing tests**

```java
@Test
void classifiesBareForbiddenAsErrorNotDeny() throws Exception {
    // The authz-missing-marker defect returns 403 with no decision marker.
    // TST-040 requires that be classified 'error', so the module must FAIL,
    // not quietly accept it as a correct denial.
    ModuleResult r = runner.run("TST-040", Map.of("SUT_DEFECT", "authz-missing-marker"));
    assertEquals(RunFragment.Result.FAILED, r.fragment().result());
}

@Test
void passesEveryMatrixCellAgainstTheCleanSut() throws Exception {
    ModuleResult r = runner.run("TST-040", Map.of());
    assertEquals(RunFragment.Result.PASSED, r.fragment().result());
}

@Test
void measuresRatherThanAssertsClockSkewTolerance() throws Exception {
    ModuleResult r = runner.run("TST-040", Map.of());
    assertTrue(r.fragment().invariants().stream()
        .anyMatch(i -> i.description().contains("accepted exp offset")),
        "clock-skew invariant must report a measured offset");
}
```

- [ ] **Step 3: Run and confirm failure**

Run: `cd qe-harness/harness && mvn -q -pl jmeter test -Dtest=Tst040ModuleTest`
Expected: FAIL.

- [ ] **Step 4: Implement**

A CSV Data Set Config drives the 12 cells. `assert-authz.groovy` classifies each response into
`allow | deny | error` — **a 401/403 with no `X-Authz-Decision` header is `error`** — and
compares to `expected_verdict`. A separate stage measures the maximum accepted `exp` offset by
presenting progressively staler tokens, and records it as a measured value rather than asserting a
configured one.

- [ ] **Step 5: Run the tests**

Run: `cd qe-harness/harness && mvn -q -pl jmeter test -Dtest=Tst040ModuleTest`
Expected: PASS, 3 tests.

- [ ] **Step 6: Commit**

```bash
git add qe-harness/harness/jmeter
git commit -m "feat(harness): add TST-040 authz matrix module with three-outcome oracle"
```

---

## Task 20: Module — TST-030 Contract Compatibility (Karate + Gatling)

Proves `TST-012`'s headline claim: one `.feature` file drives both functional and performance
runs.

**Files:**
- Create: `…/harness/gatling-karate/pom.xml`
- Create: `…/gatling-karate/src/test/resources/tst-030-contract/transfer-contract.feature`
- Create: `…/gatling-karate/src/test/java/…/Tst030ContractRunner.java` (Karate)
- Create: `…/gatling-karate/src/test/scala/…/Tst030Simulation.scala` (Gatling, same feature)
- Create: `…/gatling-karate/tst-030-contract/README.md`

**Interfaces:**
- Consumes: SUT `/v1/transfers`, `/v2/transfers`, `/openapi.json` (Task 10)
- Produces: a fragment with `oracle: contract-schema`; the same `.feature` executed by both
  runners

- [ ] **Step 1: Write the shared feature file**

```gherkin
Feature: transfer contract compatibility

  Background:
    * url baseUrl

  Scenario: v2 response satisfies its published schema
    Given path 'v2', 'transfers'
    And request { from: 'ACC-000001', to: 'ACC-000002', amountMinor: 500 }
    When method post
    Then status 201
    And match response == { transferRef: '#uuid', status: '#string', settledAt: '#string' }

  Scenario: v1 remains backward compatible
    Given path 'v1', 'transfers'
    And request { from: 'ACC-000001', to: 'ACC-000002', amountMinor: 500 }
    When method post
    Then status 201
    And match response contains { transferRef: '#uuid', status: '#string' }
```

- [ ] **Step 2: Write the failing test**

```java
@Test
void featureFailsAgainstTheSchemaDriftDefect() {
    Results r = Runner.path(FEATURE).systemProperty("sutDefect", "schema-drift").parallel(1);
    assertTrue(r.getFailCount() > 0, "schema-drift must break the contract assertions");
}

@Test
void sameFeatureDrivesTheGatlingSimulation() {
    // The Gatling simulation MUST reference the same .feature path, not a copy.
    String sim = Files.readString(Path.of(SIMULATION_SCALA));
    assertTrue(sim.contains("tst-030-contract/transfer-contract.feature"),
        "Gatling must drive the shared feature, not a duplicated scenario");
}
```

The second test is what stops the two runners silently drifting into two separate test suites —
which is the failure mode that would quietly falsify `TST-012`'s claim.

- [ ] **Step 3: Run and confirm failure**

Run: `cd qe-harness/harness && mvn -q -pl gatling-karate test`
Expected: FAIL.

- [ ] **Step 4: Implement both runners over the one feature**

`Tst030ContractRunner` is a plain Karate JUnit runner. `Tst030Simulation` uses
`karateFeature("classpath:tst-030-contract/transfer-contract.feature")` so the load run executes
the identical scenarios. Both emit a fragment through the JVM `EvidenceEmitter`.

- [ ] **Step 5: Run the tests**

Run: `cd qe-harness/harness && mvn -q -pl gatling-karate test`
Expected: PASS, 2 tests.

- [ ] **Step 6: Commit**

```bash
git add qe-harness/harness/gatling-karate
git commit -m "feat(harness): add TST-030 contract module sharing one feature across both runners"
```

---

## Task 21: Module — TST-039 Data Quality Reconciliation (Locust)

**Files:**
- Create: `…/harness/locust/pyproject.toml`, `requirements.txt`
- Create: `…/harness/locust/emitter.py`
- Create: `…/harness/locust/tst_039_recon/locustfile.py`, `recompute.py`, `README.md`
- Test: `…/harness/locust/tests/test_emitter.py`,
  `…/harness/locust/tests/test_recompute.py`

**Interfaces:**
- Consumes: SUT `GET /recon/report`, `POST /recon/seed-defects` (Task 12);
  `evidence.schema.json` (Task 2)
- Produces: `emit_fragment(dict) -> Path` in Python, writing a fragment that validates against
  the same schema the JVM emitter obeys

- [ ] **Step 1: Write the failing tests**

```python
def test_python_emitter_output_validates_against_the_shared_schema(tmp_path):
    schema = json.loads(SCHEMA_PATH.read_text())
    out = emit_fragment({
        "archetype": "TST-039", "module": "locust", "service_name": "reference-sut",
        "tier": "T0", "oracle": "confusion-matrix",
        "invariants": [{"id": "I1", "description": "no false negatives", "result": "passed"}],
        "environment": "ci-smoke",
    }, tmp_path)
    jsonschema.validate(json.loads(out.read_text()), schema)

def test_recompute_scores_false_negatives_independently():
    # Independent recomputation is why TST-039 uses Locust, not JMeter.
    seeded = {"accuracy": {"ACC-000003"}, "completeness": set(), "timeliness": set()}
    reported = {"accuracy": set(), "completeness": set(), "timeliness": set()}
    score = score_dimensions(seeded, reported)
    assert score["accuracy"]["fn"] == 1
    assert score["accuracy"]["fp"] == 0

def test_emitter_rejects_not_evaluated_threshold_without_reason(tmp_path):
    with pytest.raises(ValueError):
        emit_fragment({
            "archetype": "TST-039", "module": "locust", "service_name": "reference-sut",
            "tier": "T0", "oracle": "confusion-matrix", "environment": "ci-smoke",
            "thresholds": [
                {"name": "freshness_s", "threshold_ref": "NFR-002#freshness",
                 "result": "not-evaluated"},
            ],
        }, tmp_path)
```

The third test mirrors the JVM emitter's guard, so the two languages enforce the same rule.

- [ ] **Step 2: Run and confirm failure**

Run: `cd qe-harness/harness/locust && python3 -m pytest tests/ -v`
Expected: FAIL.

- [ ] **Step 3: Implement**

`emitter.py` validates against `evidence.schema.json` **before** writing, raising `ValueError` on
a schema violation — the emitter must never produce a fragment the gate would reject.
`recompute.py` reads the ledger through the SUT's read API and recomputes expected balances and
freshness independently, then `score_dimensions` produces per-dimension `tp/fp/fn`.

`locustfile.py` drives `POST /recon/seed-defects` then `GET /recon/report` under concurrency, and
scores the result.

- [ ] **Step 4: Run the tests**

Run: `cd qe-harness/harness/locust && python3 -m pytest tests/ -v`
Expected: PASS, 3 tests.

- [ ] **Step 5: Verify the defect proof**

```bash
cd qe-harness && SUT_DEFECT=recon-false-clean ./bin/run-module.sh TST-039; echo "exit=$?"
```

Expected: non-zero exit, and the fragment's `result` is `failed` with a false-negative count above
zero.

- [ ] **Step 6: Commit**

```bash
git add qe-harness/harness/locust
git commit -m "feat(harness): add TST-039 reconciliation module with independent recomputation"
```

---

## Task 22: Module — TST-043 Client Experience Budget (k6)

**Files:**
- Create: `…/harness/k6/package.json`, `…/harness/k6/emitter.js`
- Create: `…/harness/k6/tst-043-clientexp/script.js`, `README.md`
- Test: `…/harness/k6/tests/emitter.test.js`

**Interfaces:**
- Consumes: SUT `GET /catalogue` (Task 13); `evidence.schema.json` (Task 2)
- Produces: `emitFragment(obj) -> string` in JavaScript; a fragment with
  `coverage: partial` reflected in the module README and `modules.yml`

- [ ] **Step 1: Write the failing test**

```javascript
test('js emitter output validates against the shared schema', () => {
  const out = emitFragment({
    archetype: 'TST-043', module: 'k6', service_name: 'reference-sut',
    tier: 'T0', oracle: 'invariant-assertion',
    invariants: [{ id: 'I1', description: 'ETag present', result: 'passed' }],
    environment: 'ci-smoke',
  });
  const validate = ajv.compile(schema);
  expect(validate(JSON.parse(out))).toBe(true);
});

test('js emitter rejects not-evaluated threshold without reason', () => {
  expect(() => emitFragment({
    archetype: 'TST-043', module: 'k6', service_name: 'reference-sut',
    tier: 'T0', oracle: 'invariant-assertion', environment: 'ci-smoke',
    thresholds: [{ name: 'payload_kb', threshold_ref: 'NFR-004#payload-budget',
                   result: 'not-evaluated' }],
  })).toThrow();
});
```

- [ ] **Step 2: Run and confirm failure**

Run: `cd qe-harness/harness/k6 && npm test`
Expected: FAIL.

- [ ] **Step 3: Implement**

`script.js` asserts, per its README: `I1` `Cache-Control` and `ETag` present; `I2` matching
`If-None-Match` yields `304` with an empty body; `I3` gzip applied when accepted; `I4` payload
size within the budget resolved from `_nfr-thresholds.yml`.

The README states the partial scope in the same words as `modules.yml`'s `partial_reason`, and
`emitter.js` mirrors the not-evaluated-needs-reason guard.

- [ ] **Step 4: Run the tests**

Run: `cd qe-harness/harness/k6 && npm test`
Expected: PASS, 2 tests.

- [ ] **Step 5: Verify all three emitters agree**

```bash
cd qe-harness && make run-all
python3 -c "
import json, glob, jsonschema, pathlib
schema = json.loads(pathlib.Path('traceability/evidence.schema.json').read_text())
files = glob.glob('traceability/runs/*.json')
for f in files: jsonschema.validate(json.loads(open(f).read()), schema)
print(f'{len(files)} fragments valid across 3 languages')
"
```

Expected: `7 fragments valid across 3 languages`

- [ ] **Step 6: Commit**

```bash
git add qe-harness/harness/k6
git commit -m "feat(harness): add TST-043 client-experience module, partial by declaration"
```

---

## Task 23: Run-All, Defect-Pair Runner, and Fragment Merge

**Files:**
- Create: `qe-harness/bin/run-all.sh`, `qe-harness/bin/run-defects.sh`,
  `qe-harness/bin/merge-fragments.py`
- Test: `qe-harness/bin/tests/test_merge_fragments.py`

**Interfaces:**
- Consumes: all seven modules; `modules.yml`
- Produces: `traceability/test_acceptance_criteria.yml` — the merged per-service block
  `TST-001` defines; exit non-zero if any module failed

- [ ] **Step 1: Write the failing test**

```python
def test_merge_produces_one_block_listing_every_archetype(tmp_path):
    write_fragments(tmp_path, ["TST-021", "TST-030", "TST-031",
                               "TST-035", "TST-039", "TST-040", "TST-043"])
    block = merge(tmp_path)["test_acceptance_criteria"]
    assert block["archetypes"] == ["TST-021", "TST-030", "TST-031",
                                   "TST-035", "TST-039", "TST-040", "TST-043"]
    assert block["service_name"] == "reference-sut"

def test_merge_marks_partial_coverage_not_passed(tmp_path):
    write_fragments(tmp_path, ["TST-043"])
    block = merge(tmp_path)["test_acceptance_criteria"]
    assert block["coverage"]["TST-043"] == "partial"

def test_merge_preserves_not_evaluated_thresholds(tmp_path):
    write_fragment(tmp_path, "TST-031", threshold_result="not-evaluated",
                   reason="smoke-mode")
    block = merge(tmp_path)["test_acceptance_criteria"]
    assert block["performance"]["thresholds_not_evaluated"] == 1
```

The third test is the guard against the spec's highest-severity risk: a merge that quietly drops
`not-evaluated` would turn a smoke run into an apparent performance pass.

- [ ] **Step 2: Run and confirm failure**

Run: `cd qe-harness && python3 -m pytest bin/tests/ -v`
Expected: FAIL.

- [ ] **Step 3: Implement**

`merge-fragments.py` reads every fragment in `traceability/runs/`, groups by `service_name`, and
emits a `test_acceptance_criteria` block matching `TST-001`'s shape — `archetypes` sorted,
`coverage` per archetype from `modules.yml`, and an explicit
`performance.thresholds_not_evaluated` count.

`run-defects.sh` iterates `modules.yml` and, for each module, runs it with its `defect_flag` and
**inverts the expectation** — a module that passes against its own defect is a failure:

```bash
for arch in $(yq '.modules[].archetype' traceability/modules.yml); do
  flag=$(yq ".modules[] | select(.archetype==\"$arch\") | .defect_flag" traceability/modules.yml)
  if SUT_DEFECT="$flag" ./bin/run-module.sh "$arch"; then
    echo "DEFECT PROOF FAILED: $arch passed against SUT_DEFECT=$flag" >&2
    exit 1
  fi
  echo "ok: $arch correctly failed against $flag"
done
```

- [ ] **Step 4: Run the tests**

Run: `cd qe-harness && python3 -m pytest bin/tests/ -v`
Expected: PASS, 3 tests.

- [ ] **Step 5: Run both suites end to end**

```bash
cd qe-harness && make up PROFILES=resilience && make run-all && make run-defects
```

Expected: `run-all` exits `0` with 7 fragments; `run-defects` prints `ok:` for all seven.

- [ ] **Step 6: Commit**

```bash
git add qe-harness/bin
git commit -m "feat(qe-harness): add run-all, defect-pair proof, and fragment merge"
```

---

## Task 24: Harness Coverage Renderer

**Files:**
- Create: `scripts/render-harness-coverage.py`
- Create: `qe-harness/traceability/harness-coverage.md` (generated)
- Test: `scripts/tests/test_render_harness_coverage.py`

**Interfaces:**
- Consumes: `modules.yml`; `CapabilityRegistry` state via `/_capabilities` is **not** used — the
  renderer is offline and reads only files
- Produces: `harness-coverage.md`; `--check` exits `1` if the file is stale, matching
  `render-testing-coverage.py`'s contract

- [ ] **Step 1: Write the failing test**

```python
def test_check_mode_fails_when_file_is_stale(tmp_path):
    setup_tree(tmp_path)
    (tmp_path / "qe-harness/traceability/harness-coverage.md").write_text("stale\n")
    r = run(["--check", "--root", str(tmp_path)])
    assert r.returncode == 1
    assert "stale" in r.stdout.lower()

def test_render_marks_partial_rows_visibly(tmp_path):
    setup_tree(tmp_path)
    run(["--root", str(tmp_path)])
    text = (tmp_path / "qe-harness/traceability/harness-coverage.md").read_text()
    assert "partial" in text
    assert "offline-sync" in text, "partial rows must carry their reason inline"
```

- [ ] **Step 2: Run and confirm failure**

Run: `python3 -m pytest scripts/tests/test_render_harness_coverage.py -v`
Expected: FAIL.

- [ ] **Step 3: Implement**

Render a table with columns `Archetype | Family | Tool | Module | Coverage | Defect Flag`, plus a
summary line `7 of 24 archetypes implemented · 17 declared · 1 partial`. Partial rows carry the
`partial_reason` inline so nobody reads the table as full coverage.

- [ ] **Step 4: Run the tests and render**

```bash
python3 -m pytest scripts/tests/test_render_harness_coverage.py -v
python3 scripts/render-harness-coverage.py
python3 scripts/render-harness-coverage.py --check; echo "check=$?"
```

Expected: tests PASS; `check=0`.

- [ ] **Step 5: Commit**

```bash
git add scripts/render-harness-coverage.py scripts/tests/ qe-harness/traceability/harness-coverage.md
git commit -m "feat(qe-harness): add harness coverage renderer with --check"
```

---

## Task 25: Rescope the Documentation-Only Claim

The single permitted corpus edit. Keep it surgical.

**Files:**
- Modify: `knowledge-base/testing/README.md` — the documentation-only paragraph

**Interfaces:**
- Consumes: nothing
- Produces: a corpus that points at the harness instead of contradicting it

- [ ] **Step 1: Read the current paragraph**

```bash
/usr/bin/grep -n "not a test harness" -A 8 knowledge-base/testing/README.md
```

- [ ] **Step 2: Rewrite it**

Replace the paragraph so it (a) keeps the claim true for `knowledge-base/`, and (b) points at the
harness. Required content — the runnable counterpart now exists at `qe-harness/` (`TST-016`),
this directory remains documentation-only, and 7 of 24 archetypes have modules today. Do not
delete the distinction; the corpus staying documentation-only is still correct and still load-
bearing for every other category.

- [ ] **Step 3: Verify no other claim now contradicts reality**

```bash
/usr/bin/grep -rn "no Maven\|no package.json\|nothing runs in CI\|documentation-only" knowledge-base/
```

Expected: every remaining hit is scoped to `knowledge-base/` and is still true. Fix any that is
not — but change nothing outside the README's one paragraph without escalating first.

- [ ] **Step 4: Re-run the corpus gates**

```bash
python3 scripts/validate-internal-links.py; echo "links=$?"
python3 scripts/validate-testing-coverage.py; echo "cov=$?"
```

Expected: both `0`.

- [ ] **Step 5: Commit**

```bash
git add knowledge-base/testing/README.md
git commit -m "docs(testing): rescope documentation-only claim, point at the TST-016 harness"
```

---

## Task 26: Register TST-016 in the Catalog

**Files:**
- Modify: `governance/standards/_catalog-inventory.yml` — append the `TST-016` row
- Modify: `governance/standards/enterprise-architecture-catalog.md` — append the table row
- Modify: `knowledge-base/testing/coverage/_testing-coverage.yml` — add `TST-016` as `governs`
- Modify: `knowledge-base/testing/coverage/coverage-matrix.md` (regenerated)

**Interfaces:**
- Consumes: `qe-harness/README.md`'s catalog header from Task 1
- Produces: catalog count 231 → 232

- [ ] **Step 1: Append the inventory row**

```yaml
- id: TST-016
  title: QE Harness Reference Implementation
  category: testing
  status: Approved
  owner: qe-lead
  path: qe-harness/README.md
  tiers:
  - T0
  - T1
  - T2
  - T3
  spine_or_radii: radii
  compliance_refs:
    ring0: []
    ring1: []
    ring2: []
  last_reviewed: '2026-08-24'
  notes: Wave 16 — runnable counterpart to the Wave 15 testing corpus; 7 of 24 archetypes
  target_wave: 16
```

- [ ] **Step 2: Append the catalog table row**

Match the existing column order exactly (`id | title | category | status | spine | owner |
path | … `) — `audit-catalog-consistency.py` parses it positionally, so a shifted column fails
the gate. Copy the shape from the `TST-015` row.

- [ ] **Step 3: Add the coverage row as `governs`**

Every discipline is `governs`; `archetypes` is empty; `perf_profiles` is empty; `primary_tool` is
the uniform `jmeter` schema placeholder that every `governs` row uses — the coverage file's own
header explains this is not a real tool assignment.

- [ ] **Step 4: Regenerate and run every gate**

```bash
python3 scripts/render-testing-coverage.py
python3 scripts/audit-catalog-consistency.py; echo "audit=$?"
python3 scripts/validate-testing-coverage.py; echo "cov=$?"
python3 scripts/render-testing-coverage.py --check; echo "render=$?"
python3 scripts/validate-internal-links.py; echo "links=$?"
python3 scripts/validate-harness-coverage.py; echo "harness=$?"
```

Expected: all `0`. Confirm the count moved to 232:

```bash
/usr/bin/grep -c 'status: Approved' governance/standards/_catalog-inventory.yml
```

Expected: `232`

- [ ] **Step 5: Commit**

```bash
git add governance/ knowledge-base/testing/coverage/
git commit -m "feat(governance): register TST-016 QE harness reference implementation"
```

---

## Task 27: CI Stage — Build, Scan, Verify, Run

**Files:**
- Modify: `.gitlab-ci.yml` — add the `qe-harness` stage and four jobs

**Interfaces:**
- Consumes: everything above
- Produces: four CI jobs, gated on `changes:` so Markdown-only MRs pay nothing

- [ ] **Step 1: Add the stage**

Insert `qe-harness` into the `stages:` list after `validate`.

- [ ] **Step 2: Write the four jobs**

Follow the file's house style — emoji echo, `rules:` with explicit `when: never` fallback,
`allow_failure: false`. All four share this rule block:

```yaml
.qe-harness-rules: &qe-harness-rules
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
      changes:
        - qe-harness/**/*
        - scripts/validate-harness-coverage.py
        - scripts/render-harness-coverage.py
      when: always
    - if: '$CI_PIPELINE_SOURCE == "schedule"'
      when: always
    - when: never
```

`harness:verify` needs no containers and runs first. `harness:scan` runs the CVE and licence
check across all three dependency trees and is **`allow_failure: false`** — the spec makes this
blocking because Wave 16 introduces the repo's first third-party dependency tree.
`harness:run` executes `make run-all` **and** `make run-defects` with `HARNESS_SMOKE_MODE=true`,
publishing `qe-harness/traceability/runs/` as artifacts.

- [ ] **Step 3: Lint the pipeline**

```bash
python3 -c "import yaml,sys; yaml.safe_load(open('.gitlab-ci.yml')); print('yaml ok')"
```

Expected: `yaml ok`. If `glab` is available, also run `glab ci lint`.

- [ ] **Step 4: Prove the docs pipeline is unaffected**

Confirm the `changes:` paths do not match a Markdown-only diff:

```bash
git diff --name-only main -- '*.md' | /usr/bin/grep -c '^qe-harness/' || echo "0 harness md files in a docs-only diff"
```

Expected: the `changes:` block contains no bare `**/*.md` pattern. Verify by reading the rule
block back.

- [ ] **Step 5: Commit**

```bash
git add .gitlab-ci.yml
git commit -m "ci: add qe-harness stage with blocking dependency scan"
```

---

## Task 28: Final Gate and Handoff

**Files:**
- Modify: `.bmad/handoff-log.md` — append the Wave 16 entry

**Interfaces:**
- Consumes: everything
- Produces: a recorded, verified end state

- [ ] **Step 1: Cold-start proof**

```bash
cd qe-harness && make down || true
make up PROFILES=resilience
make verify
make run-all
make run-defects
```

Expected: every step exits `0`. This is success criterion 1 and 2 from the spec.

- [ ] **Step 2: Capability count proof**

```bash
curl -s localhost:8080/_capabilities | python3 -c "
import json,sys
d=json.load(sys.stdin)
impl=[k for k,v in d.items() if v=='implemented']
print(f'total={len(d)} implemented={len(impl)} declared={len(d)-len(impl)}')
assert len(d)==24 and len(impl)==7
"
```

Expected: `total=24 implemented=7 declared=17`

- [ ] **Step 3: Run all six gates**

```bash
python3 scripts/audit-catalog-consistency.py;    echo "audit=$?"
python3 scripts/validate-testing-coverage.py;    echo "cov=$?"
python3 scripts/render-testing-coverage.py --check; echo "render=$?"
python3 scripts/validate-internal-links.py;      echo "links=$?"
python3 scripts/validate-harness-coverage.py;    echo "harness=$?"
python3 scripts/render-harness-coverage.py --check; echo "hrender=$?"
```

Expected: all `0`. Compare against Task 0's baseline — any gate that was green then and is not
now is a regression this wave caused, and must be fixed, not explained.

- [ ] **Step 4: Global-constraint sweep**

```bash
/usr/bin/grep -rEn '(?<!\d)[0-9]{13,19}(?!\d)' qe-harness/ --include='*' 2>/dev/null | /usr/bin/grep -v traceability/runs | head
python3 scripts/validate-harness-coverage.py 2>&1 | /usr/bin/grep -ci 'pan-shaped' || echo "0 PAN findings"
/usr/bin/grep -rn 'retry' qe-harness/harness --include='*.groovy' --include='*.py' --include='*.js' | /usr/bin/grep -i assert || echo "no assertion retries"
```

Expected: no PAN-shaped hits outside generated run output; no assertion retries.

- [ ] **Step 5: Confirm TST-043 is recorded partial, not passed**

```bash
/usr/bin/grep -n 'TST-043' qe-harness/traceability/harness-coverage.md
/usr/bin/grep -n 'TST-043' -A 2 qe-harness/traceability/test_acceptance_criteria.yml
```

Expected: `partial` in both, with the reason present. This is spec success criterion 6 and the
mitigation for a named risk — do not accept a `passed`.

- [ ] **Step 6: Append the handoff entry**

Add one row to `.bmad/handoff-log.md` recording: date `2026-08-24`, agent `tester-qe`, and an
action line stating the file counts, the seven modules and their tools, the gate results with
numbers, the cold-start and defect-pair proofs, the catalog count move 231 → 232, and — stated
explicitly — that `TST-031`/`TST-035` ran in smoke mode with thresholds `not-evaluated`, and that
`TST-043` is partial. Follow the existing entries' level of detail.

- [ ] **Step 7: Commit**

```bash
git add .bmad/handoff-log.md
git commit -m "docs(bmad): record Wave 16 QE harness final gate handoff"
```

---

## Self-Review

**1. Spec coverage.** Every spec section maps to a task:

| Spec § | Requirement | Task |
|---|---|---|
| §4.1 | Placement + traceability gate | 3 |
| §4.2 | Directory layout | 1 |
| §4.3 | Three build trees, Makefile façade | 1 |
| §4.4 | Compose profiles | 14 |
| §5.1 | Seven modules, best-fit tools | 16–22 |
| §5.2 | SUT capabilities, `/_capabilities`, 501 | 5–13 |
| §5.3 | `TST-043` partial | 22, 24, 28 |
| §5.4 | Oracles, evidence, 3 emitters, one schema | 2, 15, 21, 22 |
| §5.5 | Synthetic-data constraints, enforced | 3 (gate), 6 (DB CHECK) |
| §6 | Run flow | 1, 16, 23 |
| §7 | Threshold citation + stated limit | 4, 15, 1 (README) |
| §8 | Four CI jobs, blocking scan, `changes:` rules | 27 |
| §8.1 | Smoke mode | 17, 18, 27 |
| §9 | Three-state results, no assertion retry | 2, 14, 28 |
| §10 | Defect injection, seven pairs | 7–13 (SUT side), 23 (proof) |
| §11 | `TST-016` registration | 26 |
| §12 | Nine success criteria | 28 |

Gap found and closed while reviewing: the spec's §12 criterion 8 ("Markdown-only MRs show no
added pipeline time") had no verification step — added as Task 27 Step 4. The spec's permitted
rescoping of the documentation-only claim was implied by §4.1 but had no task — added as Task 25.

**2. Placeholder scan.** No `TBD`, `TODO`, or "implement later". Two places deliberately instruct
the implementer to *discover* rather than transcribe — Task 1 Step 5 (dependency versions) and
Task 4 Step 1 (real `NFR-*` anchors) — because inventing a version or a citation that does not
exist would be worse than a resolved one, and both steps state the escalation path instead of a
fallback value.

**3. Type consistency.** `RunFragment.Result` has one spelling throughout
(`PASSED/FAILED/NOT_EVALUATED/NOT_IMPLEMENTED`, wire form `passed/failed/not-evaluated/
not-implemented`). `EvidenceEmitter.emit` is the JVM entry point in Tasks 2, 16–20;
`emit_fragment` in Python (21); `emitFragment` in JS (22) — different names because different
languages, all validating the same `evidence.schema.json`. `CapabilityRegistry.IMPLEMENTED` grows
in exactly one place per task (7–13) and is asserted at 7 in Task 13. `modules.yml` is written
once (Task 3) and read by the gate (3), the runner (16), the defect proof (23), and the renderer
(24) — one declaration, four consumers.
