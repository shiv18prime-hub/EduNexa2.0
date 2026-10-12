# EduNexa 2.0

Android + web student and school hub. Kotlin Android SDK 35, JDK 17, Gradle 8.9; Firebase Authentication, Firestore, Storage, FCM and callable functions in asia-south1.

## Validate

```bash
npm ci
npm ci --prefix functions
npm test
npm run build:web
gradle testDebugUnitTest lintDebug assembleDebug
```

Emulator tests use demo projects, not production data. Java is required. GitHub Actions builds the APK and runs checks.

## Activate

[DEPLOYMENT.md](DEPLOYMENT.md) describes owner login, core deployment, first-admin bootstrap, legacy test migration, web authorization, payment setup and device acceptance. [PROJECT_REVIEW.md](PROJECT_REVIEW.md) distinguishes implemented code from production activation and device verification.

Private web address: https://edunexa-school-hub.shiv18prime.chatgpt.site

New feature clients need the matching backend deployment before school services become available. Existing Google login has been confirmed on the owner's phone; the signing certificate remains registered in google-services.json.
