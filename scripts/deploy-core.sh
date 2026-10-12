#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
npm ci
npm ci --prefix functions
./node_modules/.bin/firebase login --no-localhost
core_names="joinSchool,reviewJoin,cancelJoin,disconnectMember,rotateCode,publishMcq,studentTests,startMcq,submitMcq,saveResult,publishResult,schoolStudents,saveAttendance,publishNotice,dashboard,createMarksheet,verifyMarksheet,revokeMarksheet,contacts,conversation,sendMessage,grantPro,redeemCoupon,chatNotification"
core_targets="firestore,storage"
IFS=',' read -r -a selected_functions <<< "$core_names"
for function_name in "${selected_functions[@]}"; do
  core_targets+=",functions:$function_name"
done
./node_modules/.bin/firebase deploy --project student-result-pro --only "$core_targets"
