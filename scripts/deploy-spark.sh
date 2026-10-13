#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ $# -gt 1 || ($# -eq 1 && "$1" != '--installed') ]]; then
  echo 'Usage: bash scripts/deploy-spark.sh [--installed]' >&2
  exit 1
fi
if [[ "${1:-}" != '--installed' ]]; then
  npm ci
fi
if ! ./node_modules/.bin/firebase projects:list --json >/dev/null; then
  ./node_modules/.bin/firebase login --no-localhost
fi
# An explicit allow-list keeps Firebase Functions and Storage out of this path.
# This command never changes the project's billing plan.
./node_modules/.bin/firebase deploy --project student-result-pro --only firestore
