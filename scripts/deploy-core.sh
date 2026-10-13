#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
blaze=false
installed=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --blaze) blaze=true; shift ;;
    --installed) installed=true; shift ;;
    *) echo 'Usage: bash scripts/deploy-core.sh --blaze [--installed]' >&2; exit 1 ;;
  esac
done
if [[ "$blaze" != true ]]; then
  echo 'Core deployment requires an existing Blaze project and can incur usage charges.' >&2
  echo 'For a no-billing setup run: bash scripts/activate-project.sh --spark' >&2
  echo 'An owner choosing the billed backend must explicitly pass --blaze.' >&2
  exit 1
fi
if [[ "$installed" != true ]]; then
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
