#!/usr/bin/env python3
"""Phase 4 P4.1 — Cross-link existing 18 docs to spine catalog IDs.

For each existing doc, insert two lines after the `Status:` line:
    Catalog ID: <ID> | Radii
    Tier Applicability: <tiers>

Idempotent: skips files that already have a `Catalog ID:` line.
"""
import re
from pathlib import Path

# id, path, tiers
DOCS = [
    ("PRIN-001", "knowledge-base/principles/api-first-design.md", "T0, T1, T2, T3"),
    ("PRIN-002", "knowledge-base/principles/event-driven-architecture.md", "T0, T1, T2, T3"),
    ("PRIN-003", "knowledge-base/principles/zero-trust-security.md", "T0, T1, T2, T3"),
    ("PRIN-004", "knowledge-base/principles/database-per-service.md", "T0, T1, T2, T3"),
    ("PRIN-005", "knowledge-base/principles/cloud-native-first.md", "T0, T1, T2, T3"),
    ("DATA-002", "knowledge-base/patterns/data/data-mesh-ownership.md", "T1, T2"),
    ("DATA-003", "knowledge-base/patterns/data/temporal-tables.md", "T1, T2"),
    ("INT-003", "knowledge-base/patterns/integration/api-gateway-routing.md", "T0, T1, T2, T3"),
    ("INT-004", "knowledge-base/patterns/integration/event-sourcing.md", "T0, T1"),
    ("RES-001", "knowledge-base/patterns/resilience/bulkhead-isolation.md", "T0, T1, T2"),
    ("RES-003", "knowledge-base/patterns/resilience/retry-with-backoff.md", "T0, T1, T2"),
    ("SEC-001", "knowledge-base/patterns/security/mtls-service-mesh.md", "T0, T1, T2"),
    ("SEC-002", "knowledge-base/patterns/security/oauth2-authorization.md", "T0, T1, T2"),
    ("SEC-003", "knowledge-base/patterns/security/vault-secret-management.md", "T0, T1, T2"),
    ("BP-001", "knowledge-base/best-practices/ci-cd-pipeline-design.md", "T0, T1, T2, T3"),
    ("BP-002", "knowledge-base/best-practices/disaster-recovery-playbook.md", "T0, T1"),
    ("BP-003", "knowledge-base/best-practices/microservice-decomposition.md", "T0, T1, T2, T3"),
    ("BP-004", "knowledge-base/best-practices/observability-standards.md", "T0, T1, T2, T3"),
]


def main() -> None:
    root = Path("/Users/dennis.dao/Documents/Arch-As-Code")
    updated = skipped = 0
    for cid, rel, tiers in DOCS:
        path = root / rel
        text = path.read_text()
        if "Catalog ID:" in text:
            skipped += 1
            continue
        # Insert after the first Status: line
        new_lines = f"\nCatalog ID: {cid} | Radii\nTier Applicability: {tiers}"
        text2, n = re.subn(
            r"^(Status: [^\n]+)$",
            r"\1" + new_lines,
            text,
            count=1,
            flags=re.MULTILINE,
        )
        if n != 1:
            print(f"WARN: no Status line in {rel}")
            continue
        path.write_text(text2)
        updated += 1
        print(f"Updated {cid}: {rel}")
    print(f"\nDone: updated={updated}, skipped={skipped}")


if __name__ == "__main__":
    main()
