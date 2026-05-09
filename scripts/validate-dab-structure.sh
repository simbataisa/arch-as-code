#!/usr/bin/env bash
# ============================================================
# DAB Structure Validation Script
# ============================================================
# Validates that a DAB submission folder meets structural
# requirements: all documents present, valid YAML, syntax checks.
#
# Usage:
#   ./scripts/validate-dab-structure.sh domains/payments/dab/2026/payment-saga-platform/
#
# Exit codes:
#   0 = Valid
#   1 = Invalid (missing files, broken YAML, etc)
# ============================================================

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Track validation status
VALIDATION_ERRORS=0
VALIDATION_WARNINGS=0

# ============================================================
# Utility Functions
# ============================================================

print_header() {
  printf "%b\n" "${BLUE}📁 Validating DAB: $1${NC}"
}

print_success() {
  printf "%b\n" "${GREEN}  ✅ $1${NC}"
  ((VALIDATION_ERRORS += 0)) || true
}

print_error() {
  printf "%b\n" "${RED}  ❌ $1${NC}"
  ((VALIDATION_ERRORS += 1)) || true
}

print_warning() {
  printf "%b\n" "${YELLOW}  ⚠️  $1${NC}"
  ((VALIDATION_WARNINGS += 1)) || true
}

print_info() {
  printf "%b\n" "${BLUE}  ℹ️  $1${NC}"
}

# ============================================================
# Main Validation Logic
# ============================================================

validate_dab_structure() {
  local dab_path="$1"

  # Check if path exists
  if [ ! -d "$dab_path" ]; then
    print_error "Path does not exist: $dab_path"
    return 1
  fi

  print_header "$dab_path"

  # Auto-detect full vs light DAB based on files present
  local full_dab=1
  local light_dab=0

  # Check which documents exist
  local doc_count=0
  for i in {01..09}; do
    if [ -f "$dab_path/${i}-"*.md ]; then
      ((doc_count += 1))
    fi
  done

  # If we have fewer than 9 docs, it might be a light DAB
  if [ "$doc_count" -lt 9 ]; then
    light_dab=1
    full_dab=0
  fi

  if [ "$full_dab" -eq 1 ]; then
    print_info "Full DAB detected (9 documents expected)"
    validate_full_dab "$dab_path"
  else
    print_info "Light DAB detected ($doc_count documents found)"
    validate_light_dab "$dab_path"
  fi

  # Validate reviewers.yml
  validate_reviewers_yml "$dab_path"

  # Validate YAML in openapi.yaml if present
  validate_openapi_yaml "$dab_path"

  # Check for Mermaid diagrams
  validate_mermaid_diagrams "$dab_path"

  # Check for broken links
  validate_internal_links "$dab_path"

  # Print summary
  print_validation_summary "$dab_path"

  # Return status
  if [ "$VALIDATION_ERRORS" -gt 0 ]; then
    return 1
  fi
  return 0
}

validate_full_dab() {
  local dab_path="$1"
  printf "\n  %b\n" "${BLUE}Checking documents (Full DAB: 01-09):${NC}"

  local required_docs=(
    "01-business-context.md"
    "02-key-design-concerns.md"
    "03-high-level-architecture.md"
    "04-data-design.md"
    "05-detailed-design.md"
    "06-integration-design.md"
    "07-infrastructure-design.md"
    "08-security-design.md"
    "09-dab-light-assessment.md"
  )

  local found_docs=0
  for doc in "${required_docs[@]}"; do
    # Allow flexibility in doc naming (e.g., 01-business-context.md or similar)
    local pattern="${doc%-*}"
    local files=($(find "$dab_path" -maxdepth 1 -name "${pattern}*.md" 2>/dev/null || echo))

    if [ ${#files[@]} -gt 0 ]; then
      # Check if file has at least one heading
      if grep -q "^#" "${files[0]}" 2>/dev/null; then
        local size=$(stat -f%z "${files[0]}" 2>/dev/null || stat -c%s "${files[0]}" 2>/dev/null || echo "0")
        print_success "File exists: $doc ($size bytes, has heading)"
        ((found_docs += 1))
      else
        print_error "File exists but has no heading: $doc"
      fi
    else
      print_error "File missing: $doc"
    fi
  done

  if [ "$found_docs" -eq 9 ]; then
    print_success "All 9 documents present"
  fi
}

validate_light_dab() {
  local dab_path="$1"
  printf "\n  %b\n" "${BLUE}Checking documents (Light DAB subset):${NC}"

  local required_light=(
    "01-business-context.md"
    "02-key-design-concerns.md"
    "03-high-level-architecture.md"
    "09-dab-light-assessment.md"
  )

  local found_count=0
  for doc in "${required_light[@]}"; do
    local pattern="${doc%-*}"
    local files=($(find "$dab_path" -maxdepth 1 -name "${pattern}*.md" 2>/dev/null || echo))

    if [ ${#files[@]} -gt 0 ]; then
      if grep -q "^#" "${files[0]}" 2>/dev/null; then
        local size=$(stat -f%z "${files[0]}" 2>/dev/null || stat -c%s "${files[0]}" 2>/dev/null || echo "0")
        print_success "File exists: $doc ($size bytes)"
        ((found_count += 1))
      else
        print_error "File exists but has no heading: $doc"
      fi
    else
      print_warning "Optional document missing: $doc"
    fi
  done

  if [ "$found_count" -ge 4 ]; then
    print_success "Light DAB minimum documents present"
  else
    print_error "Light DAB missing required documents (need at least 4 core docs)"
  fi
}

validate_reviewers_yml() {
  local dab_path="$1"
  printf "\n  %b\n" "${BLUE}Checking reviewers.yml:${NC}"

  local reviewers_file="$dab_path/reviewers.yml"
  if [ ! -f "$reviewers_file" ]; then
    print_error "reviewers.yml not found"
    return 1
  fi

  # Validate YAML syntax using Python
  if python3 -c "import yaml; yaml.safe_load(open('$reviewers_file'))" 2>/dev/null; then
    print_success "reviewers.yml is valid YAML"

    # Check for required sections
    if grep -q "^project:" "$reviewers_file" && grep -q "^reviewers:" "$reviewers_file"; then
      print_success "reviewers.yml has 'project' and 'reviewers' sections"
    else
      print_error "reviewers.yml missing 'project' or 'reviewers' section"
    fi
  else
    print_error "reviewers.yml has YAML syntax errors"
    return 1
  fi
}

validate_openapi_yaml() {
  local dab_path="$1"
  printf "\n  %b\n" "${BLUE}Checking OpenAPI specification:${NC}"

  local openapi_files=()
  if [ -f "$dab_path/openapi.yaml" ]; then
    openapi_files+=("$dab_path/openapi.yaml")
  elif [ -f "$dab_path/openapi.yml" ]; then
    openapi_files+=("$dab_path/openapi.yml")
  fi

  if [ ${#openapi_files[@]} -eq 0 ]; then
    print_info "No OpenAPI spec found (optional)"
    return 0
  fi

  for openapi_file in "${openapi_files[@]}"; do
    if python3 -c "import yaml; yaml.safe_load(open('$openapi_file'))" 2>/dev/null; then
      print_success "$(basename "$openapi_file") is valid YAML"
    else
      print_error "$(basename "$openapi_file") has YAML syntax errors"
    fi
  done
}

validate_mermaid_diagrams() {
  local dab_path="$1"
  printf "\n  %b\n" "${BLUE}Checking Mermaid diagrams:${NC}"

  # Find all markdown files with mermaid blocks
  local md_files=($(find "$dab_path" -maxdepth 1 -name "*.md" -type f 2>/dev/null || echo))

  if [ ${#md_files[@]} -eq 0 ]; then
    print_info "No markdown files found"
    return 0
  fi

  local mermaid_count=0
  local errors=0

  for md_file in "${md_files[@]}"; do
    # Extract mermaid blocks using grep
    if grep -q '```mermaid' "$md_file" 2>/dev/null; then
      ((mermaid_count += 1))

      # Basic syntax checks for mermaid
      local has_graph=0
      if grep -A 10 '```mermaid' "$md_file" | grep -qE '^\s*(graph|flowchart|sequenceDiagram|classDiagram|stateDiagram)'; then
        has_graph=1
      fi

      if [ "$has_graph" -eq 1 ]; then
        print_success "$(basename "$md_file") has valid Mermaid diagram syntax"
      else
        print_warning "$(basename "$md_file") has Mermaid block but could not verify full syntax"
      fi
    fi
  done

  if [ "$mermaid_count" -eq 0 ]; then
    print_info "No Mermaid diagrams found (recommended to include at least one)"
  else
    print_success "Found $mermaid_count Mermaid diagram(s)"
  fi
}

validate_internal_links() {
  local dab_path="$1"
  printf "\n  %b\n" "${BLUE}Checking internal links:${NC}"

  local md_files=($(find "$dab_path" -maxdepth 1 -name "*.md" -type f 2>/dev/null || echo))

  if [ ${#md_files[@]} -eq 0 ]; then
    print_info "No markdown files found"
    return 0
  fi

  local broken_links=0
  local checked_links=0

  for md_file in "${md_files[@]}"; do
    # Extract links using grep/sed: [text](path)
    local links=$(grep -o '\[.*\]([^)]*\.md[^)]*)' "$md_file" 2>/dev/null | sed 's/.*(\(.*\))/\1/' | sort -u || echo)

    while IFS= read -r link; do
      [ -z "$link" ] && continue
      # Skip external URLs and anchors
      if [[ "$link" =~ ^(http|https|#) ]]; then
        continue
      fi

      ((checked_links += 1))

      # Resolve relative path
      local target_dir=$(dirname "$md_file")
      local target_file="$target_dir/$link"
      local resolved_target=$(cd "$(dirname "$target_file")" 2>/dev/null && pwd -P && echo "$(basename "$target_file")" || echo "")

      if [ ! -f "$target_file" ]; then
        print_error "Broken link in $(basename "$md_file"): $link"
        ((broken_links += 1))
      fi
    done <<< "$links"
  done

  if [ "$checked_links" -eq 0 ]; then
    print_info "No internal links found"
  elif [ "$broken_links" -eq 0 ]; then
    print_success "All $checked_links internal links are valid"
  else
    print_warning "$broken_links broken link(s) found out of $checked_links checked"
  fi
}

print_validation_summary() {
  local dab_path="$1"
  printf "\n  %b\n" "${BLUE}Validation Summary:${NC}"

  local total_checks=$((VALIDATION_ERRORS + VALIDATION_WARNINGS))
  if [ "$VALIDATION_ERRORS" -eq 0 ]; then
    printf "%b\n" "${GREEN}  ✅ DAB validation passed${NC}"
    if [ "$VALIDATION_WARNINGS" -gt 0 ]; then
      printf "%b\n" "${YELLOW}     ($VALIDATION_WARNINGS warning(s) found)${NC}"
    fi
  else
    printf "%b\n" "${RED}  ❌ DAB validation failed${NC}"
    printf "%b\n" "${RED}     $VALIDATION_ERRORS error(s), $VALIDATION_WARNINGS warning(s)${NC}"
  fi
  printf "\n"
}

# ============================================================
# Main Entry Point
# ============================================================

main() {
  if [ $# -eq 0 ]; then
    printf "Usage: %s <path-to-dab-folder>\n" "$(basename "$0")"
    printf "Example: %s domains/payments/dab/2026/payment-saga-platform/\n" "$(basename "$0")"
    exit 1
  fi

  local dab_path="$1"

  # Normalize path (remove trailing slash)
  dab_path="${dab_path%/}"

  validate_dab_structure "$dab_path"
  local result=$?

  exit "$result"
}

main "$@"
