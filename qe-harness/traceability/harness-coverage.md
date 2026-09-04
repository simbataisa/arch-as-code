| Archetype | Family | Tool | Module | Coverage | Defect Flag |
|---|---|---|---|---|---|
| TST-020 | A — Correctness & State | — | — | declared | — |
| TST-021 | A — Correctness & State | jmeter | qe-harness/harness/jmeter/tst-021-ledger | full | ledger-unbalanced |
| TST-022 | A — Correctness & State | — | — | declared | — |
| TST-023 | A — Correctness & State | jmeter | qe-harness/harness/jmeter/tst-023-reservation | full | reservation-overcommit |
| TST-024 | A — Correctness & State | — | — | declared | — |
| TST-025 | A — Correctness & State | — | — | declared | — |
| TST-026 | B — Messaging & Integration | jmeter | qe-harness/harness/jmeter/tst-026-routing | full | route-default-fallthrough |
| TST-027 | B — Messaging & Integration | jmeter | qe-harness/harness/jmeter/tst-027-ordering | partial — I5's per_partition and global scopes cannot be exercised against RabbitMQ, which has no partitions; the declared scope is per_key and only that scope is asserted. I3's post-restart clause belongs to TST-029, which owns the broker-restart path. | resequencer-emits-on-arrival |
| TST-028 | B — Messaging & Integration | — | — | declared | — |
| TST-029 | B — Messaging & Integration | — | — | declared | — |
| TST-030 | B — Messaging & Integration | gatling-karate | qe-harness/harness/gatling-karate/tst-030-contract | full | schema-drift |
| TST-031 | C — Load & Capacity | jmeter | qe-harness/harness/jmeter/tst-031-ratelimit | full | ratelimit-leaky |
| TST-032 | C — Load & Capacity | — | — | declared | — |
| TST-033 | C — Load & Capacity | — | — | declared | — |
| TST-034 | C — Load & Capacity | jmeter | qe-harness/harness/jmeter/tst-034-blend | full | journey-starved |
| TST-035 | D — Resilience | jmeter | qe-harness/harness/jmeter/tst-035-faultinjection | full | breaker-disabled |
| TST-036 | D — Resilience | — | — | declared | — |
| TST-037 | E — Data | jmeter | qe-harness/harness/jmeter/tst-037-readmodel | partial — I5 (no loss or duplication across a connector restart) requires a CDC connector this repository does not contain. I1-I4 and I6 are asserted; I5 is reported not-evaluated rather than substituted. | outbox-published-count-stale |
| TST-038 | E — Data | — | — | declared | — |
| TST-039 | E — Data | locust | qe-harness/harness/locust/tst_039_recon | full | recon-false-clean |
| TST-040 | F — Security | jmeter | qe-harness/harness/jmeter/tst-040-authz | full | authz-missing-marker |
| TST-041 | F — Security | — | — | declared | — |
| TST-042 | G — Observability & Client | — | — | declared | — |
| TST-043 | G — Observability & Client | k6 | qe-harness/harness/k6/tst-043-clientexp | partial — None of the archetype's own I1-I6 are implemented: I1/I2/I6 need an offline client, I3/I4 a rendered DOM, and I5 k6/browser against a real page - no such application exists in this repository. This module ships four substitute server-side HTTP invariants (perf budget, cache correctness, conditional requests, compression) which are renumbered I1-I4 and are NOT the archetype's I1-I4. | cache-headers-absent |

12 of 24 archetypes implemented · 12 declared · 3 partial
