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

[Open the prepared Cloud Shell setup](https://shell.cloud.google.com/?cloudshell_git_repo=https://github.com/shiv18prime-hub/EduNexa2.0.git&cloudshell_git_branch=fix/auth-school-access&cloudshell_workspace=.&show=terminal&cloudshell_print=scripts/cloud-shell-start.txt) and run `bash scripts/activate-project.sh` in the owner's Google environment. The guided setup deploys core services, authorizes the web domain, grants a selected verified account the admin role and checks endpoint reachability.

Private web address: https://edunexa-school-hub.shiv18prime.chatgpt.site

New feature clients need the matching backend deployment before school services become available. On 2026-10-13 the owner confirmed Google login works on the verified 2.0.1 APK; the signing certificate remains registered in google-services.json.
