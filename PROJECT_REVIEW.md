# EduNexa 2.0 status

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

Spark chat and bounded Firestore profile photos were merged in PR #4. Main workflow #169 passed Android build, unit tests, lint, Firestore rules tests and web build on 2026-10-16. The OAuth signing verifier withheld an APK because SHA-1 `C8:C4:4B:42:FD:D2:AC:05:6B:10:4B:ED:59:72:28:18:7C:DF:49:A1` is not registered in the checked-in Firebase config. Add its SHA-1/SHA-256, refresh `google-services.json`, and push the file to get the updated APK artifact.

The owner confirmed Google sign-in worked on the verified 2.0.1 APK on 2026-10-13. Browser and physical-device acceptance of the newest source changes are still needed.

The owner deployed the Spark baseline from Cloud Shell. Deploy the updated Firestore rules/indexes with `bash scripts/deploy-spark.sh`, then install the new APK or republish the web client and verify chat and profile photos. This command stays on Firebase Spark and does not deploy Functions or Storage. Timed test scoring, Pro grants/redemption, paid membership and marksheet operations remain dependent on Cloud Functions and are unavailable on the current Spark setup. Keep the selected zero-budget constraint; do not link billing.

The production dashboard returned HTTP 404 in a check on 2026-10-13. The web Site remains a separate hosted project, so merging GitHub source does not by itself publish the updated web client. See DEPLOYMENT.md for owner activation and acceptance steps.
