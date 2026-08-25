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
# volume never leaks into the next run). Every service in docker-compose.yml
# is profile-gated (none is profile-less) -- Compose's `down` resolves which
# services are "in scope" the same way `up` does, from `--profile`/
# COMPOSE_PROFILES, not from "whatever is currently running". A bare
# `docker compose down -v` therefore enables zero profiles, matches zero
# services, and exits 0 having silently done nothing: confirmed
# deterministically reproducible (Task 14), not an environment flake -- see
# qe-harness/README.md's "`docker compose down -v` matches nothing without
# --profile" section. ALL_PROFILES below enumerates every profile this file
# declares so `down` always matches everything `up` could have started,
# regardless of which PROFILES value was passed to `up`. The label-filtered
# cleanup after it is a last-resort safety net for any other reason `down`
# might still leave something behind; it is a no-op when `down` already
# worked, since the filters then match nothing.
ALL_PROFILES := core resilience observability messaging

down:
	$(COMPOSE) $(foreach p,$(ALL_PROFILES),--profile $(p)) down -v || true
	@leftover="$$(docker ps -aq --filter label=com.docker.compose.project=qe-harness)"; \
	if [ -n "$$leftover" ]; then \
		echo "docker compose down still left containers behind -- forcing cleanup" >&2; \
		docker rm -f $$leftover; \
	fi
	@leftover_vol="$$(docker volume ls -q --filter label=com.docker.compose.project=qe-harness)"; \
	if [ -n "$$leftover_vol" ]; then docker volume rm $$leftover_vol; fi
	@leftover_net="$$(docker network ls -q --filter label=com.docker.compose.project=qe-harness)"; \
	if [ -n "$$leftover_net" ]; then docker network rm $$leftover_net; fi

# Run the gates — no containers required (traceability gate: Task 3;
# NFR threshold projection: Task 4).
verify:

# Run one module against the running SUT: `make run ARCH=TST-021`
# (bin/run-module.sh lands in Task 16, alongside the first module).
run:
	./bin/run-module.sh $(ARCH)

# Run all seven implemented modules against the clean SUT
# (bin/run-all.sh or equivalent lands alongside the last archetype module).
run-all:

# Run all seven modules against their injected defects; each MUST fail
# (same driver as run-all, pointed at the defect-injection profile).
run-defects:
