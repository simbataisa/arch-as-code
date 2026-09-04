# TST-043 -- Client Experience Budget (k6)

> **Substitute invariants.** This module's `I1`-`I4` are server-side HTTP checks of its own
> devising, not the archetype document's `I1`-`I6`. None of the archetype's own invariants are
> implemented here — see `partial_reason` in `traceability/modules.yml`. Invariant IDs in a
> fragment are module-local by design (see `qe-harness/README.md`, "Three names that mean more
> than one thing").

Oracle: invariant-assertion. Best-fit tool per TST-010: k6. Canonical archetype:
[client-experience-offline-perf.md](../../../../knowledge-base/testing/archetypes/client-experience-offline-perf.md)
(TST-043, catalog IDs FE-005/FE-006/FE-001/FE-002/MOB-001/MOB-006).

`traceability/modules.yml` declares this module `coverage: partial`:

> None of the archetype's own I1-I6 are implemented: I1/I2/I6 need an offline client, I3/I4
> a rendered DOM, and I5 k6/browser against a real page - no such application exists in this
> repository. This module ships four substitute server-side HTTP invariants (perf budget,
> cache correctness, conditional requests, compression) which are renumbered I1-I4 and are
> NOT the archetype's I1-I4.

This is the third and last non-JMeter module (a third, independent toolchain: Node/k6, entirely
outside both the Maven reactor and Python), and the last of Wave 16's seven harness modules.

## Why this module's scope is narrower than the archetype's own I1-I6

Read the canonical archetype closely before assuming this module implements any of its six named
invariants -- it does not implement **any** of them as written:

- **I1** (offline queue replay order/exactly-once), **I2** (queue bound/overflow), **I6**
  (force-upgrade preserving in-progress state) are all offline-queue/mobile-client invariants --
  there is no offline queue, no mobile client, in this repository at all.
- **I3** (error boundary containment) and **I4** (i18n/RTL layout) are React/rendered-DOM
  invariants -- there is no frontend application to render.
- **I5** (Core Web Vitals -- LCP/INP/CLS, measured via the `k6/browser` module against a real,
  throttled Chromium context navigating a real page) is the archetype's own *defining*
  capability (§6 Tool Fit: "I5 is the reason this archetype exists as a distinct document ... k6
  is the only one of the four tools capable of exercising it at all") -- and it is exactly as
  unreachable as I1-I4/I6: `k6/browser` needs a real page to navigate and paint; this repository
  has a reference *backend* (Task 5), not a frontend.

What this module DOES do -- and what `partial_reason` above actually names -- is exercise the
reference SUT's own **server-side prerequisites** for a good client experience: HTTP caching
semantics, conditional requests, and compression on `GET /catalogue` (Task 13's
`CatalogueController`/`CachePolicyFilter`), plus a payload-size budget. None of these four checks
is literally one of the archetype's own I1-I6; they are real, protocol-level HTTP invariants that
a client experience budget depends on, proven with the one tool this corpus already commits to
for this archetype (k6), using its plain `k6/http` module rather than `k6/browser` -- there being
nothing for `k6/browser` to navigate to here.

## This module's own invariants/threshold

| ID | Check | Assertion |
|---|---|---|
| I1 | Cache headers present | `GET /catalogue` carries both `Cache-Control` and `ETag` |
| I2 | Conditional request | presenting the just-observed `ETag` via `If-None-Match` yields `304` with an empty body |
| I3 | Compression | `Accept-Encoding: gzip` yields a response with `Content-Encoding: gzip` |
| I4 | Payload budget | the plain (uncompressed) response body stays within `payload_budget_bytes` (`profiles/_nfr-thresholds.yml`) |

## The payload budget (I4): what NFR-003 does and does not say

No NFR document in this corpus states a client-response-payload-size ceiling directly (checked:
[NFR-002 Latency Budget Model](../../../../knowledge-base/nfr/latency-budget-model.md),
[NFR-004 Throughput Model](../../../../knowledge-base/nfr/throughput-model.md) -- neither mentions
payload size as a bound on anything a client receives).
[NFR-003 Capacity Planning Model](../../../../knowledge-base/nfr/capacity-planning-model.md)'s own
`### Input Parameters` table lists `avg_payload_bytes: 2,048` -- but read in context, that is a
**mean** request+response payload for one worked capacity-sizing example (the NAPAS gateway at
Tết peak), an input to Little's Law/Kafka partition sizing, not a stated maximum for any client
response. Treating it as a ceiling outright would misrepresent what the document says.

`profiles/_nfr-thresholds.yml`'s own `payload_budget_bytes` entry instead **derives** a ceiling
from that same value, using NFR-003's own margin convention: its "Worked Example" table applies a
1.5x "Headroom" multiplier to convert a normal/peak figure into a provisioned ceiling ("absorbs
unexpected spikes ... without breaching SLOs"). `2,048 x 1.5 = 3,072` bytes. This is Task 22's own
derived interpretation, not a number NFR-003 states as a payload budget outright -- see that
entry's own comment in `_nfr-thresholds.yml`, and `qe-harness/README.md`'s "What the Threshold
Gate Does Not Prove": a human still owns confirming this derivation is the right one for this
module's actual scope. The gate (`scripts/validate-harness-coverage.py`, check 6) only proves the
citation `NFR-003#input-parameters` resolves to a real row and a real heading anchor -- it does
not, and cannot, prove 3,072 is the right number.

## Why emission is a separate Node step

Every other module builds and schema-validates its evidence fragment **in-process**, inside the
same tool that ran the oracle (JMeter's Groovy calls the JVM `EvidenceEmitter` directly; Locust's
Python calls `emitter.py` directly). This module cannot do that: `script.js` runs inside k6's own
sandboxed JS engine (goja), which has no Node module-resolution algorithm behind it. Confirmed
empirically (Task 22): `ajv`'s own internal module graph (relative, extension-less `require`s
resolved by Node's algorithm) does not resolve under k6's much simpler loader --

```
GoError: The moduleSpecifier "./core" couldn't be found on local disk.
```

-- so `emitFragment` (in `../emitter.js`) cannot be called from inside `script.js` at all. Emission
is instead split across two processes, both driven by `bin/run-k6.sh`:

1. **k6** runs `script.js` against the real, running SUT. `handleSummary` writes a RAW report
   (each check's pass/fail, from k6's own `data.root_group.checks[]`, plus the measured payload
   size) to a temp file -- no schema validation happens here.
2. **Node** (`../write-fragment.js`) reads that raw report, calls `emitFragment` (real Node, real
   `ajv`, against the exact same `evidence.schema.json` the JVM/Python emitters target) to build
   and validate the real fragment, and writes it to `traceability/runs/`.

This means `script.js` itself never touches disk beyond `handleSummary`'s own raw-report write --
consistent with every other module's `run-<tool>.sh` owning the actual evidence-file write, not
the tool's own in-process script.

## Running it

```bash
cd qe-harness && docker compose --profile core up -d --wait   # postgres + reference-sut
./bin/run-module.sh TST-043
```

`bin/run-k6.sh` (Task 22's own addition -- this tool-runner script did not exist before, the same
gap Tasks 20/21 each already hit and fixed for their own tool) installs this directory's pinned
`package.json` dependencies into `node_modules/` if missing, resolves `payload_budget_bytes` from
`profiles/_nfr-thresholds.yml` (via `python3`, the same mechanism `run-module.sh` itself already
uses to resolve `modules.yml`), runs k6 headlessly (`vus: 1, iterations: 1` -- see `script.js`'s
own comment for why a single, deterministic run is the right shape for these four checks), then
runs `write-fragment.js` to build and write the fragment. Exits non-zero if that fragment's
`result` is `failed`.

## Running the Jest suite directly

```bash
cd qe-harness/harness/k6
npm install
npm test
```

Pure unit tests of `emitter.js` (`../tests/emitter.test.js`) -- no live SUT, no k6 binary needed.

## Defect proof (manual)

```bash
curl -X POST http://localhost:8080/_test/defect/cache-headers-absent   # 204
cd qe-harness && ./bin/run-module.sh TST-043; echo "exit=$?"
# -> I1 failed (no Cache-Control/ETag at all)
# -> I2 failed too (no ETag to present via If-None-Match in the first place, so the
#    "conditional" request just gets a fresh 200 back, never a 304)
# -> exit=1
curl -X DELETE http://localhost:8080/_test/defect                      # 204, always clears it
```

I3 and I4 are **not** affected by this defect, confirmed directly against the running SUT --
worth stating plainly rather than assuming a defect flips every check:

- I3 stays `passed`: `cache-headers-absent` bypasses `CachePolicyFilter` entirely (its own
  Javadoc/`doFilterInternal`: `chain.doFilter(request, response); return;`), which is where the
  filter's own app-level gzip lives -- but Tomcat's connector-level compression
  (`server.compression.enabled=true`, `application.properties`) independently gzips this response
  anyway once the filter steps aside, because the response is written without an up-front
  `Content-Length` (`Transfer-Encoding: chunked`), which bypasses Spring Boot's
  `min-response-size` size-skip. `curl -H "Accept-Encoding: gzip"` confirms `Content-Encoding:
  gzip` present with the defect active.
- I4 stays `passed`: the payload-size budget is a property of `CatalogueController`'s own fixed
  response body, which this defect never touches (it only removes headers `CachePolicyFilter`
  would otherwise add).

## Files

- `../package.json` -- pinned npm dependencies (`ajv`, `jest`); see `qe-harness/README.md`'s
  "Pinned Versions" table for how/when each was resolved.
- `../emitter.js` -- the shared JS evidence emitter (`emitFragment(obj) -> string`), mirroring the
  JVM `EvidenceEmitter`/`RunFragment` and Python `emit_fragment` field-for-field.
- `../write-fragment.js` -- the plain-Node CLI `bin/run-k6.sh` invokes after k6 finishes, turning
  k6's raw report into a real, schema-validated fragment via `emitFragment`.
- `../tests/emitter.test.js` -- Task 22's two given Jest tests for `emitter.js`.
- `script.js` -- the k6 script itself: I1-I4 above, plus `handleSummary`'s raw-report write.
