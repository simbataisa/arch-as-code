#!/usr/bin/env python3
"""
DAB Reviewer Assignment Script
================================
Reads dab/reviewers.yml and creates MR approval rules via GitLab API.
Runs automatically in CI/CD when a DAB merge request is created or updated.

Usage:
    # In CI/CD (automatic):
    python scripts/assign-reviewers.py

    # Local dry-run (preview only):
    python scripts/assign-reviewers.py --dry-run --reviewers-file dab/reviewers.yml

Environment Variables (set by GitLab CI):
    CI_SERVER_URL          - GitLab instance URL
    CI_PROJECT_ID          - Project ID
    CI_MERGE_REQUEST_IID   - Merge Request IID
    CI_JOB_TOKEN           - Authentication token
"""

import yaml
import os
import sys
import argparse

try:
    import requests
except ImportError:
    requests = None


GITLAB_URL = os.environ.get("CI_SERVER_URL", "https://gitlab.com")
PROJECT_ID = os.environ.get("CI_PROJECT_ID", "")
MR_IID = os.environ.get("CI_MERGE_REQUEST_IID", "")
TOKEN = os.environ.get("CI_JOB_TOKEN", os.environ.get("GITLAB_TOKEN", ""))

API_BASE = f"{GITLAB_URL}/api/v4/projects/{PROJECT_ID}"
HEADERS = {"PRIVATE-TOKEN": TOKEN}


def find_reviewers_file(start_path="."):
    """Find reviewers.yml in any dab/ directory."""
    for root, dirs, files in os.walk(start_path):
        if "reviewers.yml" in files and "dab" in root:
            return os.path.join(root, "reviewers.yml")
    return None


def load_reviewers(filepath):
    """Load and validate reviewers.yml."""
    with open(filepath, "r") as f:
        config = yaml.safe_load(f)

    if not config:
        raise ValueError(f"Empty reviewers file: {filepath}")

    if "project" not in config:
        raise ValueError("Missing 'project' section in reviewers.yml")

    if "reviewers" not in config:
        raise ValueError("Missing 'reviewers' section in reviewers.yml")

    return config


def get_user_id(username):
    """Resolve @username to GitLab user ID."""
    if requests is None:
        return None

    username = username.lstrip("@")
    resp = requests.get(
        f"{GITLAB_URL}/api/v4/users",
        headers=HEADERS,
        params={"username": username},
    )
    resp.raise_for_status()
    users = resp.json()
    return users[0]["id"] if users else None


def get_existing_rules():
    """Get existing MR-level approval rules."""
    if requests is None:
        return []

    resp = requests.get(
        f"{API_BASE}/merge_requests/{MR_IID}/approval_rules",
        headers=HEADERS,
    )
    resp.raise_for_status()
    return resp.json()


def delete_rule(rule_id):
    """Delete an existing approval rule."""
    resp = requests.delete(
        f"{API_BASE}/merge_requests/{MR_IID}/approval_rules/{rule_id}",
        headers=HEADERS,
    )
    return resp.status_code == 204


def create_approval_rule(name, approver_ids, approvals_required):
    """Create an MR-level approval rule via GitLab API."""
    if requests is None:
        return False

    resp = requests.post(
        f"{API_BASE}/merge_requests/{MR_IID}/approval_rules",
        headers=HEADERS,
        json={
            "name": name,
            "approvals_required": approvals_required,
            "user_ids": approver_ids,
        },
    )
    if resp.status_code == 201:
        print(f"  ✅ Rule created: {name} ({approvals_required} required)")
        return True
    else:
        print(f"  ❌ Failed: {name} — {resp.status_code}: {resp.text}")
        return False


def add_reviewers(approver_usernames):
    """Add users as MR reviewers (for visibility in the MR sidebar)."""
    if requests is None:
        return

    for username in approver_usernames:
        uid = get_user_id(username)
        if uid:
            requests.put(
                f"{API_BASE}/merge_requests/{MR_IID}",
                headers=HEADERS,
                json={"reviewer_ids": [uid]},
            )


def run_dry(config):
    """Dry-run: print what would be configured without calling API."""
    project = config["project"]
    print(f"\n📋 Project: {project.get('name', 'Unknown')}")
    print(f"   Domain:  {project.get('domain', 'Unknown')}")
    print(f"   Author:  {project.get('submitted_by', 'Unknown')}")
    print(f"   Date:    {project.get('submission_date', 'Unknown')}")
    print()

    print("🔧 Approval rules that would be created:")
    print("   " + "-" * 55)

    total_approvers = 0
    for role, details in config.get("reviewers", {}).items():
        rule_name = role.replace("_", " ").title()
        approvers = details.get("approvers", [])
        required = details.get("required", 1)
        files = details.get("files", [])

        print(f"\n   📌 DAB: {rule_name}")
        print(f"      Required approvals: {required}")
        print(f"      Approvers: {', '.join(approvers)}")
        print(f"      Files: {', '.join(files)}")
        total_approvers += len(approvers)

    print(f"\n   " + "-" * 55)
    print(f"   Total: {len(config.get('reviewers', {}))} rules, "
          f"{total_approvers} approvers")
    print()
    print("   ℹ️  Fixed DAB chairs are managed via CODEOWNERS (not shown here)")


def run_live(config):
    """Live run: create approval rules via GitLab API."""
    if not PROJECT_ID or not MR_IID:
        print("❌ Missing CI_PROJECT_ID or CI_MERGE_REQUEST_IID")
        sys.exit(1)

    if requests is None:
        print("❌ 'requests' library not installed. Run: pip install requests")
        sys.exit(1)

    project = config["project"]
    print(f"📋 Project: {project.get('name', 'Unknown')}")
    print(f"🔧 Setting up approval rules for MR !{MR_IID}...")
    print()

    # Clean up existing DAB rules (to handle re-runs)
    existing = get_existing_rules()
    for rule in existing:
        if rule.get("name", "").startswith("DAB:"):
            delete_rule(rule["id"])
            print(f"  🗑️  Removed existing rule: {rule['name']}")

    # Create new rules from reviewers.yml
    success_count = 0
    for role, details in config.get("reviewers", {}).items():
        approver_ids = []
        for username in details.get("approvers", []):
            uid = get_user_id(username)
            if uid:
                approver_ids.append(uid)
            else:
                print(f"  ⚠️  User not found: {username}")

        if approver_ids:
            rule_name = role.replace("_", " ").title()
            ok = create_approval_rule(
                name=f"DAB: {rule_name}",
                approver_ids=approver_ids,
                approvals_required=details.get("required", 1),
            )
            if ok:
                success_count += 1

    print()
    print(f"✅ {success_count} approval rules configured for MR !{MR_IID}")


def main():
    parser = argparse.ArgumentParser(
        description="Assign DAB reviewers from reviewers.yml via GitLab API"
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Preview approval rules without calling GitLab API",
    )
    parser.add_argument(
        "--reviewers-file",
        help="Path to reviewers.yml (auto-detected if not specified)",
    )
    args = parser.parse_args()

    # Find reviewers file
    filepath = args.reviewers_file or find_reviewers_file()
    if not filepath or not os.path.exists(filepath):
        print("⚠️  No reviewers.yml found in dab/ — skipping")
        return

    print(f"📖 Loading: {filepath}")
    config = load_reviewers(filepath)

    if args.dry_run:
        run_dry(config)
    else:
        run_live(config)


if __name__ == "__main__":
    main()
