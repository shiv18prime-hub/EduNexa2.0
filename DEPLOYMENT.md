# EduNexa activation

All code targets the existing `student-result-pro` Firebase project and Android package `com.edunexa.app`. The current APK signing SHA-1 is already in the owner-provided google-services.json. Google login was confirmed on the owner's phone before this feature upgrade.

## What requires the project owner

This workspace has no authenticated Firebase administration connection. Production functions, Firestore/Storage rules, indexes and web authorized domains have not been deployed. The new Android and web clients call these functions; install the new feature APK after activating the core backend. Existing installed login builds are separate from this branch.

In an authenticated terminal or Google Cloud Shell, clone this repository and check out `fix/auth-school-access`. Run:

```bash
bash scripts/deploy-core.sh
```

The Firebase CLI will ask you to sign in. Use the Google account that owns `student-result-pro`. Cloud Functions and Storage may require the project's supported billing plan. This script deploys the core features; paid checkout functions are excluded until payment credentials are configured. Firebase credentials stay in the owner's environment.

Choose an existing Firebase Authentication UID for the first administrator, then run:

```bash
node scripts/set-admin.cjs --uid EXISTING_UID --apply
```

For the Admin SDK helper scripts, authenticate Application Default Credentials in your own environment if they are unavailable (`gcloud auth application-default login --no-launch-browser`).

Sign in as that account to approve schools, create coupons, grant Pro and configure a paid plan. Signups cannot create an administrator themselves. Existing schools must be verified once through Admin → Users → Verify / Rotate Code (or the web Admin school list), even if an old profile says approved. Existing student/teacher connections should send a request again and be approved by the verified school. Server-owned proof records now determine access; legacy approval or Pro profile flags are not trusted. Restore any legitimate existing Pro memberships with an administrator grant.

If legacy MCQ tests exist, move their answer keys out of student-readable documents:

```bash
node scripts/migrate-mcq.cjs
node scripts/migrate-mcq.cjs --apply
```

The first command is a dry run. The scripts require Firebase Admin credentials in your authenticated environment (for example Application Default Credentials in Cloud Shell). New tests already use separate answer keys.

## Web login

Private web address: https://edunexa-school-hub.shiv18prime.chatgpt.site

Add `edunexa-school-hub.shiv18prime.chatgpt.site` in Firebase Authentication settings → Authorized domains. Email/password sign-in uses the existing project provider. Google popup login requires this domain authorization. The hosting audience remains owner-private; sharing with schools is a separate deliberate hosting decision.

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
