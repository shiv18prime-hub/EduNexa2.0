# EduNexa 2.0 status

## Implemented in source

- Google/email authentication and profile creation/editing for student, teacher and school accounts.
- Android and web dashboard reads, private personal result history, and the subject calculator with optional-subject handling and local sharing.
- Spark-compatible school workflows: admin approval and code rotation, student/teacher connection requests, school review/cancel/disconnect, and connected-member roster reads. Firestore transactions pair membership documents with user profile state.
- School result, attendance and notice publishing through Firestore. Rules restrict these writes to approved schools and connected teachers, verify active student membership, validate result totals/grades and check that issued identity matches the student profile.
- Callable timed MCQ publishing/scoring, private answer keys, server deadlines, attempt limits and idempotent scoring. These callable routes still require Cloud Functions.
- Cloud Storage profile photos; owner/type/size rules are in place, but the feature still requires Storage to be activated.
- School-scoped conversations, server membership checks and rate limits; chat still requires Cloud Functions.
- Server-owned coupon redemption, Pro grants, Razorpay order/payment verification, webhook replay protection, refunds, marksheet registration and verification. These operations still require Cloud Functions and configured payment credentials where applicable.
- Native image and PDF utilities with bounded file sizes, plus a responsive web application.

## Validation and remaining activation

The current PR branch passed 47 automated tests, the web bundle build, Android APK compilation and Android lint in GitHub Actions on 2026-10-15. Tests include Firestore rules checks for membership and school record writes. Google OAuth registration is checked on trusted push/manual CI runs; the latest PR run skipped that check and did not upload an APK artifact.

The owner confirmed Google sign-in worked on the verified 2.0.1 APK on 2026-10-13. Browser and physical-device acceptance of the newest source changes are still needed.

Production Firebase administration access is absent here. The owner must deploy the updated Firestore rules/indexes from Cloud Shell with `bash scripts/deploy-spark.sh`, then update the APK/web client and verify the account flows. This command stays on Firebase Spark and does not deploy Functions or Storage. Features listed above as Cloud Functions/Storage dependent remain unavailable until a suitable backend is activated. Keep the selected zero-budget constraint; do not link billing.

The production dashboard returned HTTP 404 in a check on 2026-10-13. The web Site remains a separate hosted project, so merging GitHub source does not by itself publish the updated web client. See DEPLOYMENT.md for owner activation and acceptance steps.
