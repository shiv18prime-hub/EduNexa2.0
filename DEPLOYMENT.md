# EduNexa activation

All code targets the existing `student-result-pro` Firebase project and Android package `com.edunexa.app`. The current APK signing SHA-1 is already in the owner-provided google-services.json. On 2026-10-13 the owner confirmed that Google login works with the verified 2.0.1 APK.

## Choose the verified APK

Download `EduNexa-debug-apk` from a successful **push** or manual workflow run. Pull-request runs validate compilation/tests/lint and publish lint reports only. Earlier PR APKs used a different cached key: `06:49:68:1F:55:D7:2D:EC:5D:F7:EE:AC:0A:35:CF:F2:54:AC:00:9A`, which has no Android OAuth registration in the supplied Firebase config. Earlier PR and push APKs both displayed version 2.0.0.

The 2.0.1 patch verifies the finished APK's signature with Android SDK `apksigner`, checks its actual package and SHA-1 against the supplied Android OAuth client, and checks its embedded web client ID before permitting an APK upload. This prevents an unregistered cached key from silently producing a downloadable APK. Current push signing SHA-1: `A3:87:0E:8A:F3:1D:BE:0F:A9:F0:2E:B5:5A:66:BF:C6:BB:2A:38:EA`.

Android will reject an in-place update between APKs signed by different keys. If the installed copy used the old PR key, replacing it with the verified push APK requires removing that copy; this removes local app data. Export any local files first. Firebase account records remain in the project. Production Google OAuth settings still need owner verification if sign-in fails with the verified APK.

## What requires the project owner

This workspace has no authenticated Firebase administration connection. Production functions, Firestore/Storage rules, indexes and web authorized domains have not been deployed from this branch. The configured dashboard endpoint returned HTTP 404 on 2026-10-13. Working Android Google login does not provide administrative deployment credentials.

### Zero-budget setup (current owner choice)

The owner confirmed a zero budget on 2026-10-13. Keep `student-result-pro` on **Spark**; do not attach a billing account or upgrade to Blaze. Firebase Authentication (Google/email) and the Firestore Standard free quota can be used without a payment method. Current limits and exclusions are documented at [Firebase pricing](https://firebase.google.com/pricing) and [Firestore quotas](https://firebase.google.com/docs/firestore/quotas). Quota exhaustion can interrupt service; this is not an unlimited free backend.

Default guided setup now deploys **only Firestore rules and indexes**, authorizes the existing web address, and grants an existing verified account the admin role. It never deploys Functions or Storage and never links billing. The Firebase Admin SDK used by the local owner scripts is a client library, not a deployed Cloud Function.

[Open the prepared repository in Google Cloud Shell](https://shell.cloud.google.com/?cloudshell_git_repo=https://github.com/shiv18prime-hub/EduNexa2.0.git&cloudshell_git_branch=fix/auth-school-access&cloudshell_workspace=.&show=terminal&cloudshell_print=scripts/cloud-shell-start.txt), then run:

```bash
bash scripts/activate-project.sh --spark
```

This requires Node.js 22 or later. If Cloud Shell has an older default, run `nvm install 22` and `nvm use 22` first. Sign in with the Google account that owns `student-result-pro` when asked. Enter any authorization code in your own Cloud Shell terminal. Do not send passwords, tokens or authorization codes to chat. Third-party repository launch links start an isolated Cloud Shell environment without existing credentials, so an owner sign-in is expected.

When prompted, enter the existing EduNexa email that should become administrator. The script checks that this is an enabled Firebase Authentication account with a verified email, shows its UID, and runs a dry configuration check before deployment. Use your own Google-login account. That account's app role will become administrator; other profile fields are preserved.

If dependencies and ADC have already been prepared in this checkout, resume without reinstalling packages. Replace the placeholder with the existing verified EduNexa account selected for administrator:

```bash
bash scripts/activate-project.sh --spark --installed --email YOUR_EDUNEXA_EMAIL
```

For a Cloud Shell checkout whose plain `git pull` reports `From .`, update from GitHub explicitly before running setup:

```bash
git pull --ff-only https://github.com/shiv18prime-hub/EduNexa2.0.git fix/auth-school-access
```

Setup validates the selected account and web configuration before deploying. Admin role and domain changes are applied only after successful Firestore deployment. Errors stop the sequence. It does not probe undeployed Functions or claim full application activation.

| Feature in the current APK/web | Status with this Spark setup |
| --- | --- |
| Google/email login, profile creation and text edits | Uses Authentication and Firestore directly; verify after rules deployment |
| Basic calculator and local image/PDF processing | No hosted compute required; verify on the device |
| Selected account's admin role, user list and coupon definition editing | Direct Firestore/owner setup; available after setup, subject to access rules |
| Android and web dashboard reads and personal calculator result history | Migrated to private Firestore reads/writes in this branch; deploy updated clients and rules before use |
| Android and web school approval/code rotation, join requests, review/cancel, active memberships and disconnect | Migrated to admin-only or paired Firestore transactions in this branch; deploy updated clients and rules before use |
| School result/attendance/notice publishing, timed test scoring, chat sending, Pro grants/redemptions and marksheet registration/verification | Still call Cloud Functions; not activated by Spark setup |
| Profile photo uploads | Still use Firebase Storage; need a no-billing replacement |
| Chat push delivery | Still depends on a deployed notification function |

The complete online app is **not yet available on Spark**. The migrations above are source changes and have not yet been deployed to the owner’s Firebase project or installed as an updated APK/web client. Existing access rules remain restrictive: protected school results, memberships, answer keys and Pro grants are not made freely client-writable to bypass the missing server. The remaining free-backend work must retain role boundaries and trusted grading. Login success or an admin role grant does not mean every school feature has been activated.

### Billed backend (optional, not used for the zero-budget setup)

The existing full backend requires the [Blaze plan for Functions deployment](https://firebase.google.com/docs/functions/get-started) and [Storage](https://firebase.google.com/docs/storage/web/start), with a default Storage bucket initialized. This is not a no-card free plan, even where no-cost usage quotas apply. Do not use it for the owner's current zero-budget setup.

An owner who separately chooses billed hosting can explicitly run `bash scripts/activate-project.sh --blaze`. That path deploys core Functions and rules/indexes, authorizes the web domain, grants the chosen administrator, and checks five protected callable endpoints. These probes establish reachability/authentication enforcement, not signed-in acceptance. The script itself never upgrades billing.

### Individual commands

In an authenticated terminal or Google Cloud Shell, clone this repository and check out `fix/auth-school-access`. Run:

```bash
bash scripts/deploy-spark.sh
```

This deploys only Firestore rules/indexes. The Firebase CLI reuses an existing login or Application Default Credentials; otherwise it asks you to sign in. Use the Google account that owns `student-result-pro`. Firebase credentials stay in the owner's environment. The billed core equivalent is `bash scripts/deploy-core.sh --blaze` and should not be used on the current Spark project.

Choose an existing verified Firebase Authentication account for the first administrator. Preview the selected UID before applying:

```bash
node scripts/set-admin.cjs --email YOUR_EDUNEXA_EMAIL
node scripts/set-admin.cjs --email YOUR_EDUNEXA_EMAIL --apply
```

`--uid EXISTING_UID` is also supported. For the Admin SDK helper scripts, authenticate Application Default Credentials in your own environment if they are unavailable (`gcloud auth application-default login --no-launch-browser`). Sign out and sign in again after granting the role.

Only after separately deploying the billed backend, check endpoint reachability without accessing account data:

```bash
node scripts/check-backend.cjs
```

Once the full backend is available, sign in as that account to approve schools, create coupons, grant Pro and configure a paid plan. Signups cannot create an administrator themselves. Existing schools must be verified once through Admin → Users → Verify / Rotate Code (or the web Admin school list), even if an old profile says approved. Existing student/teacher connections should send a request again and be approved by the verified school. Server-owned proof records now determine access; legacy approval or Pro profile flags are not trusted. Restore any legitimate existing Pro memberships with an administrator grant.

If legacy MCQ tests exist, move their answer keys out of student-readable documents:

```bash
node scripts/migrate-mcq.cjs
node scripts/migrate-mcq.cjs --apply
```

The first command is a dry run. The scripts require Firebase Admin credentials in your authenticated environment (for example Application Default Credentials in Cloud Shell). New tests already use separate answer keys.

## Web login

Private web address: https://edunexa-school-hub.shiv18prime.chatgpt.site

Add `edunexa-school-hub.shiv18prime.chatgpt.site` in Firebase Authentication settings → Authorized domains. Email/password sign-in uses the existing project provider. Google popup login requires this domain authorization. The hosting audience remains owner-private; sharing with schools is a separate deliberate hosting decision.

The guided setup adds this domain automatically. For a separate preview/apply using owner credentials:

```bash
node scripts/authorize-web.cjs
node scripts/authorize-web.cjs --apply
```

## Payments

The paid plan starts disabled. Free/discount coupons and administrator Pro grants use the core backend. For paid checkout, use a Razorpay test account first and keep credentials in Firebase Secret Manager:

```bash
firebase functions:secrets:set RAZORPAY_KEY_ID --project student-result-pro
firebase functions:secrets:set RAZORPAY_KEY_SECRET --project student-result-pro
firebase functions:secrets:set RAZORPAY_WEBHOOK_SECRET --project student-result-pro
firebase deploy --project student-result-pro --only functions:createProOrder,functions:verifyProPayment,functions:paymentWebhook
```

Configure Razorpay to send `payment.captured` and `payment.refunded` events to the deployed paymentWebhook URL. Enable automatic capture. Only captured payments with a matching order, currency, amount and owner activate Pro. Repeated callbacks do not extend membership again. Full refunds reverse unused purchased membership time. Do a real provider sandbox test before setting Live credentials and enabling the paid plan in Admin.

No price has been invented or enabled. The administrator chooses rupee price and membership duration in Admin. The default 100% coupon grants 30 days and each account can redeem it once; creation can configure duration and limits.

## Release

Current GitHub artifact is a debug APK for owner testing. A distributable signed release requires an owner-controlled release keystore, backup, SHA registration and a Play policy review for digital subscriptions. Do not publish the debug APK as a production store release.

## Acceptance test

1. Register a student, teacher and school using distinct accounts.
2. Verify pending/rejected schools cannot publish; approve one as administrator.
3. Join student and teacher using the active school code; approve or cancel/reject their requests.
4. Publish results, attendance, notices and a test as school/teacher. Student sees their own academic records and current school's notices.
5. Submit a timed test; test cross-school denial, retry, attempt limit and late submission.
6. Upload a profile photo and reopen the same account on another device/web.
7. Exchange a school message; remove the member and verify message access is revoked.
8. Redeem a 100% coupon; confirm duplicate redemption fails. Use the provider sandbox for paid Pro and refund.
9. Register a school marksheet; verify lookup shows only minimal academic summary. Student practice records require Pro and stay clearly labelled practice records.
10. Test image/PDF export, portrait edges and navigation/keyboard insets on a physical Android phone.
