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

Install APK artifacts only from successful push/manual runs. CI checks the finished APK's actual signature and Firebase OAuth binding before upload. PR runs publish lint reports only because PR caches may use a different debug certificate. The signing patch is version 2.0.1.

## Activate

[DEPLOYMENT.md](DEPLOYMENT.md) describes owner login, core deployment, first-admin bootstrap, legacy test migration, web authorization, payment setup and device acceptance. [PROJECT_REVIEW.md](PROJECT_REVIEW.md) distinguishes implemented code from production activation and device verification.

[Open the prepared Cloud Shell setup](https://shell.cloud.google.com/?cloudshell_git_repo=https://github.com/shiv18prime-hub/EduNexa2.0.git&cloudshell_git_branch=fix/auth-school-access&cloudshell_workspace=.&show=terminal&cloudshell_print=scripts/cloud-shell-start.txt) and run `bash scripts/activate-project.sh --spark` in the owner's Google environment. The owner has chosen a zero budget. Default setup deploys only Firestore rules/indexes, authorizes the web domain and grants a selected verified account the admin role. It does not deploy Functions/Storage or change billing.

This prepares the free foundation; it does **not** activate the complete app. The current branch adds Spark-native dashboard reads, personal calculator history and transactional school membership/approval. Deploy the updated branch and Firestore rules before using those changes. Publishing school results/attendance/notices, timed test scoring, chat sending, Pro grants/redemptions and marksheet registration/verification still require Cloud Functions. Cloud photo upload still uses Storage. See the feature-status table in [DEPLOYMENT.md](DEPLOYMENT.md). Existing privileged-write restrictions remain in place.

Private web address: https://edunexa-school-hub.shiv18prime.chatgpt.site

New feature clients need the matching backend deployment before school services become available. On 2026-10-13 the owner confirmed Google login works on the verified 2.0.1 APK; the signing certificate remains registered in google-services.json.
