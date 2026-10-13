#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ $# -gt 1 || ($# -eq 1 && "$1" != '--installed') ]]; then
  echo 'Usage: bash scripts/deploy-core.sh [--installed]' >&2
  exit 1
fi
if [[ "${1:-}" != '--installed' ]]; then
  npm ci
  npm ci --prefix functions
fi
# Reuse owner CLI login or Application Default Credentials. Do not ask for a second login.
if ! ./node_modules/.bin/firebase projects:list --json >/dev/null; then
  ./node_modules/.bin/firebase login --no-localhost
fi
core_names="joinSchool,reviewJoin,cancelJoin,disconnectMember,rotateCode,publishMcq,studentTests,startMcq,submitMcq,saveResult,publishResult,schoolStudents,saveAttendance,publishNotice,dashboard,createMarksheet,verifyMarksheet,revokeMarksheet,contacts,conversation,sendMessage,grantPro,redeemCoupon,chatNotification"
core_targets="firestore,storage"
IFS=',' read -r -a selected_functions <<< "$core_names"
for function_name in "${selected_functions[@]}"; do
  core_targets+=",functions:$function_name"
done
./node_modules/.bin/firebase deploy --project student-result-pro --only "$core_targets"
