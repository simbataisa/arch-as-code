#!/usr/bin/env bash
# Fail fast once, letting assertion report connection-refused.
set -euo pipefail
URL="${1:-http://localhost:8080}/_capabilities"
for i in $(seq 1 60); do
  if curl -fsS "$URL" >/dev/null 2>&1; then echo "SUT ready"; exit 0; fi
  sleep 2
done
echo "SUT did not become ready at $URL 120s" >&2
exit 1
