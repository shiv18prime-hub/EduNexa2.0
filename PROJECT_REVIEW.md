# EduNexa 2.0 status

## Android 2.0.3 submission edition

Android school result, attendance and notice publishers now use the existing restrictive Firestore contracts instead of undeployed callables. Approved schools can access these actions; teacher access checks active membership. Original offline practice quizzes, per-account device-local history and explicitly unverified personal PDF study reports are available without sign-in. The teacher workspace and login recovery have been refreshed. Hosted examination scoring, paid membership, registered verification and notification delivery default to disabled in this build. Deploy the updated rules once for fractional school percentages; see SUBMISSION.md.

## Implemented in source

- Google/email authentication and profile creation/editing for student, teacher and school accounts.
- Android and web dashboard reads, private personal result history, and the subject calculator with optional-subject handling and local sharing.
- Spark-compatible school workflows: admin approval and code rotation, student/teacher connection requests, school review/cancel/disconnect, and connected-member roster reads. Firestore transactions pair membership documents with user profile state.
- School result, attendance and notice publishing through Firestore. Rules restrict these writes to approved schools and connected teachers, verify active student membership, validate result totals/grades and check that issued identity matches the student profile.
- Callable timed MCQ publishing/scoring, private answer keys, server deadlines, attempt limits and idempotent scoring. These callable routes still require Cloud Functions.
- Compressed profile photos (maximum 180 KB) stored in the owner’s Firestore profile, within the Firebase Spark backend.
- School-scoped Firestore chat with two-party membership checks, sender validation and a 2,000-character cap. Spark chat has no push delivery or server-side rate limiting.
- Server-owned coupon redemption, Pro grants, Razorpay order/payment verification, webhook replay protection, refunds, marksheet registration and verification. These operations still require Cloud Functions and configured payment credentials where applicable.
- Native image and PDF utilities with bounded file sizes, plus a responsive web application.

## Validation and remaining activation

Spark chat and bounded Firestore profile photos were merged in PR #4. The Firebase Android OAuth config was refreshed in PR #6 and merged to `main`. GitHub Actions run #175 (2026-10-16) passed Android build, Google OAuth signature/client-ID verification, lint, Firestore rules tests and web build; it produced the verified `EduNexa-debug-apk` artifact.

The owner reports installing this APK and reaching the dashboard. Browser and physical-device acceptance of the remaining school features are still needed.

The owner deployed the Spark baseline from Cloud Shell. Deploy the updated Firestore rules/indexes with `bash scripts/deploy-spark.sh`, then verify chat and profile photos on the installed APK and current web client. This command stays on Firebase Spark and does not deploy Functions or Storage. Timed test scoring, Pro grants/redemption, paid membership and marksheet operations remain dependent on Cloud Functions and are unavailable on the current Spark setup. Keep the selected zero-budget constraint; do not link billing.

The production dashboard returned HTTP 404 in a check on 2026-10-13. The web Site remains a separate hosted project, so merging GitHub source does not by itself publish the updated web client. See DEPLOYMENT.md for owner activation and acceptance steps.
