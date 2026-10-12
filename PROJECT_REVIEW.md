# EduNexa 2.0 status

## Implemented

- Google/email authentication foundation with profile preservation; teacher signup and dashboards; school approval refresh and logout; server-owned school membership, cancellation, approval/rejection, disconnect and code rotation.
- Subject result calculator with max/passing marks, optional subjects, validation, shareable text card, refreshed/deletable history and server-computed dashboard aggregates.
- Callable MCQ publish/list/start/submit with private answer keys, server deadlines, attempt limits and idempotent scoring. Legacy tests require the provided migration.
- Server-validated school result/attendance/notice publication and school/teacher access scopes.
- Cloud Storage profile photos shared across devices, with owner/type/size storage rules.
- School-scoped text conversations, server membership checks and rate limits, listener cleanup and FCM notification trigger.
- Trusted server-owned approval, membership and Pro entitlement proof records; legacy client-editable profile flags grant no privileges.
- Server-owned coupon redemption and Pro grants; administrator pricing; Razorpay order creation, verified captured payment, webhook replay handling and full-refund reversal.
- Trusted issuer marksheet registration, unguessable verification IDs, minimal verification summary, revocation and labelled Pro practice records.
- Native image resizing, portrait background removal, compression/merge/photo-signature tools and PDF operations with bounded files/background processing; system and keyboard insets.
- Responsive web app using the same Firebase project and service contracts, with local image/PDF tools; owner-private hosting.

## Validation and remaining activation

Automated backend/security/domain tests and Android unit/build/lint checks are recorded in the PR and Actions run. Web bundling is validated; browser QA is unavailable in this workspace. Physical-device acceptance of the feature upgrade remains necessary.

Production Firebase administration access is absent here. Core functions, rules/indexes, Storage setup, web authorized domain and first-admin bootstrap must be activated by the project owner. Paid checkout additionally needs merchant credentials and a provider sandbox check. Release signing and public distribution remain owner-controlled.

See DEPLOYMENT.md for exact activation commands and the full device acceptance sequence. Build success does not establish that production Firebase is deployed or that real payments have been tested.
