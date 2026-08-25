COMPOSE ?= docker compose
PROFILES ?= core
ROOT := $(shell cd .. && pwd)

.PHONY: up down verify run run-all run-defects

# Start the SUT and the infrastructure its modules need, e.g.
# `make up PROFILES=core` or `make up PROFILES="core resilience"`. `--wait`
# blocks on each started service's own healthcheck; bin/wait-for-sut.sh is a
# separate plain-HTTP readiness probe for callers (e.g. a harness module's
# `make run`) that don't go through `docker compose up` themselves.
up:
	$(COMPOSE) $(foreach p,$(PROFILES),--profile $(p)) up -d --wait

# Tear down what `make up` started, including volumes (so a stale Postgres
# volume never leaks into the next run).
down:
	$(COMPOSE) down -v

# Run the gates — no containers required (traceability gate: Task 3;
# NFR threshold projection: Task 4).
verify:

# Run one module against the running SUT: `make run ARCH=TST-021`
# (bin/run-module.sh lands in Task 16, alongside the first module).
run:

# Run all seven implemented modules against the clean SUT
# (bin/run-all.sh or equivalent lands alongside the last archetype module).
run-all:

# Run all seven modules against their injected defects; each MUST fail
# (same driver as run-all, pointed at the defect-injection profile).
run-defects:
