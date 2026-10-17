# EduNexa Android 2.0.3

The Android submission edition includes an offline study desk, three original practice quizzes with explanations and device-local history, result calculation/sharing, personal PDF study reports, image/PDF utilities, Google/email authentication, profile sync, school approval and connection requests, school chat, and school result/attendance/notice workflows.

## School rules update

Before using online school publishing, the Firebase project owner must deploy the updated Firestore rules once: `bash scripts/deploy-spark.sh --installed` from an authenticated, updated checkout. The update corrects fractional percentage validation (for example, 2/7 = 28.57%) and does not expand role permissions. Offline study tools do not require deployment.

## Demonstration

1. Open **Explore offline study tools** on the welcome screen.
2. Complete a quiz, review answers and reopen the practice history.
3. Calculate a result with custom subjects and share it.
4. Open **PDF Study Report**, enter the details, preview and save a PDF. Reports are clearly labelled personal practice and are not school-issued or verified.
5. Use Image Resizer, Target KB Compressor or PDF Toolkit with a local document.
6. Sign in for the real student, teacher, school or administrator workflows. Schools require administrator approval; teachers/students need an approved school connection. Results, attendance and notices use the deployed restrictive Firestore rules.

This edition defaults to Firebase Spark. Paid membership, registered marksheet verification, secure school examination scoring and push delivery are not enabled. Offline quiz scores never become official school exam attempts. No billing upgrade is performed.

`-PhostedServicesEnabled=true` is reserved for an owner who has separately deployed/configured the trusted backend. It is not required for this submission edition.

## Verification

The Android workflow runs JVM tests, assembles the APK, checks lint, and verifies the actual APK signer/package against Firebase configuration before publishing an artifact. The rules workflow tests Firestore access in an emulator. Run physical-device acceptance for installation, Google sign-in, school publishing, portrait processing and document saving.

The distributed APK is a debug build for project evaluation, not a Play Store release.
