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

The latest main workflow (#166) succeeded on 2026-10-16. Android build, unit tests, lint and Firestore rules tests passed. Its OAuth signing verifier warned and withheld an APK because the workflow cache used an unregistered signing key. This working tree now adds Spark chat and bounded Firestore profile photos; these changes must pass CI and be merged before a new APK can include them.

The owner confirmed Google sign-in worked on the verified 2.0.1 APK on 2026-10-13. Browser and physical-device acceptance of the newest source changes are still needed.

The owner has deployed the Spark baseline f