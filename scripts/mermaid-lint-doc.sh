#!/usr/bin/env bash
# Extracts mermaid blocks from a markdown file and validates each with mmdc.
# Usage: mermaid-lint-doc.sh <file.md>
# Exit 0 if all blocks render; non-zero if any fail.

set -euo pipefail

FILE="$1"
TMPDIR_BASE=$(mktemp -d)
trap 'rm -rf "$TMPDIR_BASE"' EXIT

BLOCK=0
IN_BLOCK=0
BUFFER=""
FAILURES=0

while IFS= read -r line || [[ -n "$line" ]]; do
  if [[ "$line" == '```mermaid' ]]; then
    IN_BLOCK=1
    BUFFER=""
    continue
  fi
  if [[ $IN_BLOCK -eq 1 && "$line" == '```' ]]; then
    IN_BLOCK=0
    BLOCK=$((BLOCK + 1))
    TMPFILE="$TMPDIR_BASE/block_${BLOCK}.mmd"
    OUTFILE="$TMPDIR_BASE/block_${BLOCK}.svg"
    printf '%s\n' "$BUFFER" > "$TMPFILE"
    # Skip blocks that are init-only or comment-only (no renderable diagram type)
    if ! grep -qE '^[[:space:]]*(graph|flowchart|sequenceDiagram|classDiagram|stateDiagram|erDiagram|gantt|pie|gitGraph|mindmap|timeline|xychart|block|sankey|quadrantChart|requirementDiagram|zenuml)' "$TMPFILE" 2>/dev/null; then
      BLOCK=$((BLOCK - 1))  # don't count skipped blocks
      continue
    fi
    if ! npx @mermaid-js/mermaid-cli -i "$TMPFILE" -o "$OUTFILE" 2>/dev/null; then
      echo "FAIL $FILE block $BLOCK"
      FAILURES=$((FAILURES + 1))
    fi
    continue
  fi
  if [[ $IN_BLOCK -eq 1 ]]; then
    BUFFER="${BUFFER}${line}
"
  fi
done < "$FILE"

if [[ $BLOCK -eq 0 ]]; then
  exit 0
fi

if [[ $FAILURES -gt 0 ]]; then
  exit 1
fi
echo "OK $FILE ($BLOCK block(s))"
