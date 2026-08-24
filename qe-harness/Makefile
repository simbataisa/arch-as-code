COMPOSE ?= docker compose
PROFILES ?= core
ROOT := $(shell cd .. && pwd)

.PHONY: up down verify run run-all run-defects

# Start the SUT and the infrastructure its modules need (docker-compose.yml
# and profile wiring land in Task 14).
up:

# Tear down what `make up` started (Task 14).
down:

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
