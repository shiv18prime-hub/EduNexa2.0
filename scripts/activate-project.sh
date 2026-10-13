#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if ! command -v node >/dev/null || ! node -e 'process.exit(Number(process.versions.node.split(".")[0]) >= 22 ? 0 : 1)'; then
  echo 'Node.js 22 or later is required. In Cloud Shell run: nvm install 22; nvm use 22; then rerun this script.' >&2
  exit 1
fi
if ! command -v gcloud >/dev/null; then
  echo 'Run this script in Google Cloud Shell, which includes gcloud.' >&2
  exit 1
fi
echo 'Activating the core EduNexa backend in student-result-pro.'
echo 'Use the Google account that owns this Firebase project. Credentials stay in this terminal environment.'
npm ci
npm ci --prefix functions
if ! gcloud auth application-default print-access-token >/dev/null 2>&1; then
  gcloud auth application-default login --no-launch-browser
fi
read -r -p 'Existing verified EduNexa email to use as administrator: ' admin_email
node scripts/set-admin.cjs --email "$admin_email"
node scripts/authorize-web.cjs
# Dependencies are already installed; the deploy helper also supports standalone use.
bash scripts/deploy-core.sh --installed
node scripts/authorize-web.cjs --apply
node scripts/set-admin.cjs --email "$admin_email" --apply
node scripts/check-backend.cjs
echo 'Core activation completed. Sign out/sign in as the chosen admin, then verify a school and test a student/teacher connection.'
