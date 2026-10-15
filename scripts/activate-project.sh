#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
plan=spark
plan_selected=false
installed=false
admin_email=''
usage() {
  echo 'Usage: bash scripts/activate-project.sh [--spark|--blaze] [--installed] [--email VERIFIED_EMAIL]' >&2
}
while [[ $# -gt 0 ]]; do
  case "$1" in
    --spark|--blaze)
      if [[ "$plan_selected" == true ]]; then usage; exit 1; fi
      plan="${1#--}"
      plan_selected=true
      shift ;;
    --installed)
      if [[ "$installed" == true ]]; then usage; exit 1; fi
      installed=true
      shift ;;
    --email)
      if [[ $# -lt 2 || -n "$admin_email" || -z "$2" || "$2" == --* ]]; then usage; exit 1; fi
      admin_email="$2"
      shift 2 ;;
    *) usage; exit 1 ;;
  esac
done
if ! command -v node >/dev/null || ! node -e 'process.exit(Number(process.versions.node.split(".")[0]) >= 22 ? 0 : 1)'; then
  echo 'Node.js 22 or later is required. In Cloud Shell run: nvm install 22; nvm use 22; then rerun this script.' >&2
  exit 1
fi
if ! command -v gcloud >/dev/null; then
  echo 'Run this script in Google Cloud Shell, which includes gcloud.' >&2
  exit 1
fi
if [[ "$plan" == spark ]]; then
  echo 'Preparing EduNexa on Firebase Spark: Firestore rules/indexes, web login and administrator setup only.'
  echo 'This path does not deploy Functions or Storage, link a billing account, or change your pricing plan.'
else
  echo 'Activating the core EduNexa backend in student-result-pro. Existing Blaze billing and a Storage bucket are required.'
fi
echo 'Use the Google account that owns this Firebase project. Credentials stay in this terminal environment.'
if [[ "$installed" == false ]]; then
  npm ci
  npm ci --prefix functions
fi
# Cloud Shell metadata can return a token even when no owner ADC file exists.
# Validate owner ADC and its quota project together before using Identity Toolkit.
if ! gcloud auth application-default set-quota-project student-result-pro; then
  gcloud auth application-default login --no-launch-browser
  gcloud auth application-default set-quota-project student-result-pro
fi
if [[ -z "$admin_email" ]]; then
  read -r -p 'Existing verified EduNexa email to use as administrator: ' admin_email
fi
node scripts/set-admin.cjs --email "$admin_email"
node scripts/authorize-web.cjs
# Dependencies are already installed; the deploy helper also supports standalone use.
if [[ "$plan" == spark ]]; then
  bash scripts/deploy-spark.sh --installed
else
  bash scripts/deploy-core.sh --blaze --installed
fi
node scripts/authorize-web.cjs --apply
node scripts/set-admin.cjs --email "$admin_email" --apply
if [[ "$plan" == spark ]]; then
  echo 'Spark foundation setup completed. Sign out/sign in as the chosen administrator.'
  echo 'The current APK/web still need backend changes for dashboard, saving results, school workflows, tests, chat, coupons and registered marksheets.'
  echo 'Cloud photo uploads and push delivery also remain unavailable. This is not full app activation. See DEPLOYMENT.md.'
else
  node scripts/check-backend.cjs
  echo 'Core activation completed. Sign out/sign in as the chosen admin, then verify a school and test a student/teacher connection.'
fi
