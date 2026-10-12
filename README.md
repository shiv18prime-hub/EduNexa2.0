# EduNexa 2.0

Native Android student and school hub (`com.edunexa.app`). Kotlin, Android SDK 35, JDK 17, Gradle 8.9; Firebase Authentication, Firestore and FCM.

## Build and validate

Install Android SDK 35 and Gradle 8.9. Run `gradle testDebugUnitTest lintDebug assembleDebug`.
GitHub Actions builds pull requests and main; download `EduNexa-debug-apk` from the successful run.

Rules tests: `npm ci`, then `npm test` (Java required). These use a demo project and never touch production.
Deploy rules separately with `firebase deploy --only firestore:rules --project YOUR_PROJECT_ID` after checking tests and account ownership. Committing rules does not deploy them.

## Firebase setup

Enable Email/Password and Google providers. Register the actual APK signing SHA-1 and SHA-256 from `gradle signingReport` in Firebase, then download the updated `google-services.json`. Google error 10 indicates OAuth/signing configuration; code changes alone cannot repair a missing fingerprint. Debug APK signing is cached in GitHub Actions but is not a production release identity.

Create the initial admin using a trusted Firebase Admin SDK/console operation; clients cannot assign their own admin role. Approve school accounts through the admin screen. New school codes include the display name; existing codes can optionally be backfilled by an administrator. Students request connection with a code; school approval/rejection updates the request and profile in a single batch.

## Completion status

This is an existing prototype, not a production-complete app. See [PROJECT_REVIEW.md](PROJECT_REVIEW.md) for verified gaps and release checks.
