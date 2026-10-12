# Code review and completion backlog

Reviewed baseline `f0abd3b18f62e157b060fa5875e8942a13e0a484`.

## Fixed in this branch

- Google signup no longer replaces an existing user document (role, approval and connection preserved). Removed lifecycle routing that could race with a Google result. Added cancellation, missing-token and OAuth error handling.
- User creation cannot self-approve a school or inject membership/Pro fields. Self-edit allowlist protects role, approval and membership.
- Added missing schoolJoinRequests rules. Connected-student queries are allowed only for their approved school. Students use school code display data instead of reading a school's private profile.
- Connection requests and school decisions use atomic writes; school profile updates require a matching pending request and batched decision.
- Attendance updates cannot move another school's record to the current school.
- Pending schools open tools; they do not enter the student dashboard through Continue.
- CI runs on pull requests, runs lint/unit checks and builds an APK. Debug keystore path now matches Android's default path.

## Remaining work before production

1. Firebase console: confirm providers, actual signing fingerprints and deployed rules. A physical-device test must verify Google login, email registration and returning sessions. No production Firebase credentials are available in this workspace.
2. Marksheet verification: marksheetVerifications has no rules in the baseline, so writes/reads fail. Design a minimal public verification record and trusted issuer verification before enabling it; exposing entire student documents is inappropriate.
3. MCQ integrity: answers and scores are supplied/read on the client. Move answer keys and scoring to a trusted backend, add attempt limits and duration enforcement.
4. Membership/coupons: coupon administration exists; billing, redemption and authoritative Pro entitlement are not implemented. Do not represent this as a working paid subscription.
5. Results: basic total-marks calculator/history exists; configurable subjects, passing marks, share cards, refreshed history and proper dashboard aggregates remain.
6. Profile photos: content URIs are local to a device, so storing them in Firestore does not implement cross-device photo sync. Add authenticated storage upload and per-user local caching.
7. Tools: test large images/PDFs, cancellation, memory limits, background work and exported-file sharing on Android 23 and 35.
8. School code rotation: standalone code creation should use a transaction to prevent collisions and define revocation/rotation semantics.
9. Android target 35: verify edge-to-edge insets and accessibility on all screens. Establish stable release signing, versioning, privacy disclosures and backup policy.
10. Teacher roles/chat and a web application are absent. Agree the data permissions and implement after the student/school core passes testing.

## Acceptance sequence

Register student and school → confirm pending school cannot publish → admin approves school → student joins by active code → school approves/rejects → connected student sees only their school's notices/tests and their own results/attendance → logout and login → test denied cross-school operations → test real APK tools and account switching.

Changes in this branch do not automatically deploy Firebase rules or configure OAuth. CI and device validation must be reported separately from static review.
