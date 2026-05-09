# Architecture Automation Scripts

This directory contains Python and Bash scripts for automating DAB validation, reviewer assignment, and API documentation generation. All scripts are designed for both local development and CI/CD execution.

## Table of Contents

- [validate-dab-structure.sh](#validate-dab-structuresh) — Validate DAB folder structure
- [generate-dab-index.py](#generate-dab-indexpy) — Generate DAB registry and status dashboard
- [assign-reviewers.py](#assign-reviewerspy) — Auto-assign reviewers to MR (runs in CI/CD)
- [openapi-to-markdown.py](#openapi-to-markdownpy) — Convert OpenAPI specs to Markdown docs

## validate-dab-structure.sh

Validates that a DAB submission folder contains all required files and has valid structure.

### Purpose

Ensures DAB submissions meet structural requirements before review:
- All required documents exist (01-09 for full DAB, or light subset)
- Each document has at least a heading
- `reviewers.yml` is present and valid YAML
- Mermaid diagrams have valid syntax
- Internal links are not broken

### Usage

```bash
# Validate a full DAB
./scripts/validate-dab-structure.sh domains/payments/dab/2026/payment-saga-platform/

# Run on a light DAB (auto-detects based on files present)
./scripts/validate-dab-structure.sh domains/lending/dab/2026/credit-scoring-v2/

# Use in CI/CD (returns exit code 0 or 1)
if ./scripts/validate-dab-structure.sh "$DAB_PATH"; then
  echo "✅ DAB is valid"
else
  echo "❌ DAB has errors"
  exit 1
fi
```

### Arguments

| Argument | Required | Description |
|----------|----------|-------------|
| `{path}` | Yes | Path to DAB folder (e.g., `domains/payments/dab/2026/payment-saga-platform/`) |

### Output

```
📁 Validating: domains/payments/dab/2026/payment-saga-platform/
  ✅ File exists: 01-business-context.md (234 bytes, has heading)
  ✅ File exists: 02-key-design-concerns.md (456 bytes, has heading)
  ...
  ✅ reviewers.yml is valid YAML
  ✅ All Mermaid diagrams have valid syntax
  ⚠️  1 broken link found: link to missing file
  ✅ Validation complete: 9/9 documents valid
```

### Exit Codes

| Code | Meaning |
|------|---------|
| **0** | Validation passed |
| **1** | Validation failed (missing files, broken YAML, broken links) |

### Requirements

- Bash 4.0+
- No external dependencies (uses only `grep`, `find`, standard utilities)

### How It Works

1. **File check**: Verifies all required documents (01-09) or light subset exist
2. **Heading check**: Each file must have at least one `#` heading
3. **YAML validation**: `reviewers.yml` is parsed as valid YAML
4. **Mermaid check**: Searches for mermaid code blocks and validates syntax
5. **Link check**: Searches for internal links (`[text](path)`) and verifies target files exist
6. **Report**: Outputs detailed results with exit code

---

## generate-dab-index.py

Scans all DAB folders and generates a master registry with status dashboard and active submissions list.

### Purpose

Maintains authoritative records of:
- All DABs organized by year and domain
- Current status (Submitted, In Review, Approved, Archived)
- Submission and approval dates
- MR links and implementation tracking

### Usage

```bash
# Generate or update the registry (scans entire domains/ tree)
python3 scripts/generate-dab-index.py

# Specify custom output paths
python3 scripts/generate-dab-index.py \
  --index-file registry/dab-index.md \
  --active-file registry/active-submissions.md \
  --approved-file registry/approved-dabs.md

# Dry-run: show what would be generated without writing files
python3 scripts/generate-dab-index.py --dry-run

# Use in CI/CD
python3 scripts/generate-dab-index.py && git add registry/
```

### Arguments

| Argument | Required | Default | Description |
|----------|----------|---------|-------------|
| `--index-file` | No | `registry/dab-index.md` | Output file for master DAB index |
| `--active-file` | No | `registry/active-submissions.md` | Output file for active submissions |
| `--approved-file` | No | `registry/approved-dabs.md` | Output file for approved DABs |
| `--dry-run` | No | False | Preview output without writing files |
| `-v, --verbose` | No | False | Show detailed scanning progress |

### Output Files

#### registry/dab-index.md

Master index organized by year and domain:

```markdown
# DAB Registry

## 2026

### Payments

| Project | Status | Submitted | Approved | MR |
|---------|--------|-----------|----------|-----|
| 🟢 Payment SAGA Platform | Approved | 2026-03-08 | 2026-03-15 | !123 |
| 🟡 Installment Plans | In Review | 2026-03-10 | — | !124 |

### Lending

...
```

**Status indicators:**
- 🟢 **Approved**: Has approval date, MR is merged
- 🟡 **In Review**: No approval date, MR is open
- 🔴 **Submitted**: New submission, awaiting initial review
- ⚪ **Archived**: Old DAB, no longer active

#### registry/active-submissions.md

List of DABs currently in review (open MRs):

```markdown
# Active DAB Submissions

| Domain | Project | Submitted By | Date | MR | Status |
|--------|---------|--------------|------|----|---------
| Payments | Payment SAGA Platform | @architect-lead | 2026-03-08 | !123 | Under EA Review |
| Lending | Credit Scoring v2 | @lending-arch | 2026-03-10 | !124 | Awaiting Security |
```

#### registry/approved-dabs.md

Historical record of approved DABs with implementation status:

```markdown
# Approved DABs

| Domain | Project | Approved | Implemented | MR |
|--------|---------|----------|-------------|----
| Payments | Payment SAGA Platform | 2026-03-15 | 60% | !123 |
| ...
```

### Requirements

- Python 3.7+
- Standard library only: `os`, `re`, `datetime`, `pathlib`, `argparse`
- No pip dependencies

### How It Works

1. **Scan**: Walks `domains/*/dab/{year}/*` directory tree
2. **Extract metadata**: Reads each project's `README.md` for:
   - Status (submitted, in-review, approved)
   - Submitted date
   - Approved date
   - MR link
3. **Organize**: Groups by year, then by domain, sorted chronologically
4. **Generate**: Creates three output files with formatted tables
5. **Report**: Shows summary (e.g., "Generated index for 15 DABs across 7 domains")

### Metadata Format

The script extracts structured data from DAB `README.md` files. Use this format:

```markdown
# Payment SAGA Platform

> Core DAB for distributed payment orchestration

- **Status**: Approved
- **Submitted**: 2026-03-08
- **Approved**: 2026-03-15
- **MR**: !123
- **Implementation**: 60%

## Overview

...
```

The script looks for these fields anywhere in the README using regex patterns.

---

## assign-reviewers.py

Automatically assigns DAB reviewers to merge requests based on `reviewers.yml` configuration.

### Purpose

Runs in CI/CD pipeline when a DAB merge request is created or updated, reading the project's `reviewers.yml` and creating MR-level approval rules in GitLab.

**Why automatic assignment?**
- Ensures consistent review coverage
- Reduces manual configuration burden
- Allows rules to evolve per-project (via reviewers.yml)
- Integrates with GitLab's native approval system

### Usage

#### In CI/CD (Automatic)

The pipeline runs automatically on DAB branches:

```bash
# In .gitlab-ci.yml (assign:reviewers job):
python3 scripts/assign-reviewers.py
```

GitLab provides these environment variables:
- `CI_SERVER_URL` — GitLab instance
- `CI_PROJECT_ID` — Project ID
- `CI_MERGE_REQUEST_IID` — MR number
- `CI_JOB_TOKEN` — Authentication token

#### Local Testing

```bash
# Dry-run: preview approval rules without calling GitLab API
python3 scripts/assign-reviewers.py --dry-run

# Specify a custom reviewers.yml for testing
python3 scripts/assign-reviewers.py \
  --dry-run \
  --reviewers-file domains/payments/dab/2026/payment-saga-platform/reviewers.yml
```

### Arguments

| Argument | Required | Description |
|----------|----------|-------------|
| `--dry-run` | No | Preview rules without API calls |
| `--reviewers-file {path}` | No | Path to reviewers.yml (auto-detected if not specified) |

### Configuration: reviewers.yml

Each DAB project includes a `reviewers.yml` that defines its reviewers:

```yaml
project:
  name: "Payment SAGA Platform"
  domain: "Payments"
  submitted_by: "@architect-lead"
  submission_date: "2026-03-08"

reviewers:
  solution_architecture:
    files:
      - "02-key-design-concerns.md"
      - "03-high-level-architecture.md"
      - "05-detailed-design.md"
    approvers:
      - "@sa-john"
      - "@sa-jane"
    required: 1  # Min 1 SA must approve

  data_architecture:
    files:
      - "04-data-design.md"
    approvers:
      - "@data-arch-1"
    required: 1

  integration_architecture:
    files:
      - "06-integration-design.md"
      - "openapi.yaml"
    approvers:
      - "@sa-alex"
    required: 1

  infrastructure:
    files:
      - "07-infrastructure-design.md"
    approvers:
      - "@infra-arch-1"
    required: 1

  business_review:
    files:
      - "01-business-context.md"
    approvers:
      - "@ba-lead"
    required: 1
```

**Fixed reviewers** (enforced via CODEOWNERS, not in reviewers.yml):
- EA Board (1 approval)
- Security Board (1 approval)
- EA Directors (assessment sign-off)

### Output Example

```
📖 Loading: domains/payments/dab/2026/payment-saga-platform/reviewers.yml
📋 Project: Payment SAGA Platform
   Domain:  Payments
   Author:  @architect-lead
   Date:    2026-03-08

🔧 Approval rules that would be created:
   ————————————————————————————————————————————

   📌 Solution Architecture
      Required approvals: 1
      Approvers: @sa-john, @sa-jane
      Files: 02-key-design-concerns.md, 03-high-level-architecture.md, ...

   📌 Data Architecture
      Required approvals: 1
      Approvers: @data-arch-1
      Files: 04-data-design.md

   ————————————————————————————————————————————
   Total: 5 rules, 8 approvers

   ℹ️  Fixed DAB chairs are managed via CODEOWNERS
```

### Requirements

- Python 3.7+
- Dependencies: `pyyaml`, `requests` (optional for dry-run)
- GitLab instance with API access

### How It Works

1. **Find reviewers.yml**: Searches dab/ directories for `reviewers.yml`
2. **Parse configuration**: Loads and validates YAML structure
3. **Resolve usernames**: Converts `@username` to GitLab user IDs via API
4. **Create approval rules**: Calls GitLab API to create MR-level rules
5. **Clean up**: Removes old DAB rules to handle re-runs
6. **Report**: Shows success/failure for each rule

### Environment Variables

| Variable | Source | Purpose |
|----------|--------|---------|
| `CI_SERVER_URL` | GitLab | API base URL (default: https://gitlab.com) |
| `CI_PROJECT_ID` | GitLab | Project ID for API calls |
| `CI_MERGE_REQUEST_IID` | GitLab | MR number for approval rules |
| `CI_JOB_TOKEN` | GitLab | Authentication token (preferred) |
| `GITLAB_TOKEN` | Manual | Fallback token for local testing |

---

## openapi-to-markdown.py

Converts OpenAPI 3.0 specifications to readable Markdown documentation.

### Purpose

Auto-generates API reference documentation from OpenAPI specs, making them:
- Human-readable in Markdown
- Searchable in MkDocs
- Version-controlled alongside DABs
- Auto-updating when specs change

### Usage

```bash
# Generate Markdown from OpenAPI spec
python3 scripts/openapi-to-markdown.py openapi.yaml -o docs/api-reference.md

# Process DAB-specific OpenAPI specs
python3 scripts/openapi-to-markdown.py \
  domains/payments/dab/2026/payment-saga-platform/openapi.yaml \
  -o domains/payments/dab/2026/payment-saga-platform/openapi-reference.md

# Generate for all DABs
find domains -name "openapi.yaml" | while read spec; do
  output_dir=$(dirname "$spec")
  python3 scripts/openapi-to-markdown.py "$spec" -o "$output_dir/openapi-reference.md"
done

# Use in CI/CD (build:openapi-docs job)
python3 scripts/openapi-to-markdown.py domains/payments/dab/2026/payment-saga-platform/openapi.yaml \
  -o registry/openapi-reference.md
```

### Arguments

| Argument | Required | Description |
|----------|----------|-------------|
| `{spec}` | Yes | Path to OpenAPI spec file (YAML or JSON) |
| `-o, --output {file}` | Yes | Output Markdown file path |
| `--title {title}` | No | Custom document title |
| `--include-schemas` | No | Include detailed schema definitions |
| `--max-depth {n}` | No | Max nesting depth for schemas (default: 3) |

### Output Example

The generated Markdown includes:

```markdown
# Payment Service API Reference

**Version**: 1.0.0
**Base URL**: https://api.example.com/v1

## Overview

Brief description from OpenAPI `description` field.

## Authentication

- OAuth2 (with scopes)
- API Key (header/query)

## Endpoints

### POST /payments/initiate

**Summary**: Initiate a new payment

**Request**

```json
{
  "amount": 1000,
  "currency": "VND",
  "recipient": {...}
}
```

**Parameters**

| Name | In | Type | Required | Description |
|------|----|----|----------|-------------|
| `amount` | body | number | Yes | Payment amount in minor units |
| `currency` | body | string | Yes | ISO 4217 currency code |

**Response** (200 OK)

```json
{
  "transaction_id": "txn_123456",
  "status": "pending",
  "created_at": "2026-03-08T10:30:00Z"
}
```

| Field | Type | Description |
|-------|------|-------------|
| `transaction_id` | string | Unique transaction identifier |
| `status` | string enum | Payment status (pending, processing, completed, failed) |

## Schemas

### Payment

```
amount: number (required)
currency: string (required, ISO 4217)
recipient: PaymentRecipient (required)
idempotency_key: string (optional, UUID)
```

## Error Codes

| Code | Status | Description |
|------|--------|-------------|
| `INVALID_AMOUNT` | 400 | Amount must be positive |
| `CURRENCY_NOT_SUPPORTED` | 400 | Currency code is not supported |
| `INSUFFICIENT_FUNDS` | 402 | Account has insufficient funds |
```

### Requirements

- Python 3.7+
- Dependencies: `pyyaml` (for YAML parsing)
- Standard library only for output generation

### How It Works

1. **Parse OpenAPI**: Loads YAML or JSON spec file
2. **Extract info**: Gets title, version, base URL, description
3. **Process security**: Extracts auth methods (OAuth, API Key, etc.)
4. **Generate endpoints**: Iterates paths and methods, extracts:
   - Summary and description
   - Parameters (path, query, header, body)
   - Request/response examples
   - Status codes and error descriptions
5. **Format Markdown**: Creates readable tables, code blocks, and navigation
6. **Write output**: Saves to specified Markdown file

---

## Development

### Running Tests

```bash
# Validate all scripts work
bash scripts/validate-dab-structure.sh domains/payments/dab/2026/payment-saga-platform/
python3 scripts/generate-dab-index.py --dry-run
python3 scripts/assign-reviewers.py --dry-run
python3 scripts/openapi-to-markdown.py openapi.yaml -o /tmp/test-api-docs.md
```

### Adding New Scripts

1. Use proper shebangs:
   - Bash: `#!/usr/bin/env bash` + `set -euo pipefail`
   - Python: `#!/usr/bin/env python3` + `argparse` for CLI
2. Document in this README with examples
3. Include error handling and informative messages
4. Make executable: `chmod +x scripts/my-script.sh`

### CI/CD Integration

All scripts are integrated in `.gitlab-ci.yml`:

- **validate**: Runs validation scripts before build
- **build**: Runs generation scripts (generate-dab-index, openapi-to-markdown)
- **assign**: Runs assign-reviewers on DAB branches
- **publish**: Deploys generated docs to GitLab Pages

---

## Troubleshooting

### validate-dab-structure.sh

**Problem**: "File exists but has no heading"
- **Solution**: Add a top-level `# Heading` to each document

**Problem**: "reviewers.yml is not valid YAML"
- **Solution**: Check YAML syntax (indentation, quotes, special characters)

### generate-dab-index.py

**Problem**: "No DABs found"
- **Solution**: Ensure DABs are in `domains/{domain}/dab/{year}/{project}/` structure

**Problem**: "Missing metadata fields"
- **Solution**: Add status, submitted date, etc. to DAB README.md following the format

### assign-reviewers.py

**Problem**: "User not found: @username"
- **Solution**: Verify username exists on GitLab instance, or add manually

**Problem**: "GITLAB_TOKEN not set"
- **Solution**: Set `GITLAB_TOKEN` env var or run with `--dry-run` for preview

### openapi-to-markdown.py

**Problem**: "Invalid OpenAPI spec"
- **Solution**: Validate spec at https://editor.swagger.io before processing

---

## Support

For issues or questions about scripts:
1. Check this README
2. Run with `--help` flag for command options
3. Ask in #architecture Slack channel
4. File an issue in the repository
