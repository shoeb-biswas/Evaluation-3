# RentShare BD — Release Readiness and Tomorrow's Decision

**Audit date:** 23 November 2026 (Bangladesh time)  
**Requested public release:** 24 November 2026  
**Audit basis:** the actual uploaded `rentsharebd-2.zip` Android source, not the previously described website stack.

## Recommendation: NO-GO for a nationwide public launch on 24 November 2026

A public rental platform requires connected user accounts, synchronized listings and chat, reliable moderation, privacy protection, back-end authorization, and verified production infrastructure. The ZIP is currently an attractive native Android **on-device prototype**. Its Room/SQLite database is the primary source of users, listings, messages, rent applications, bills and financial records. There is **no functioning shared rental-service backend in this ZIP**. Firebase AI Logic integration exists but does not provide a shared marketplace backend.

A signed tester APK may be prepared and evaluated on physical devices. That is **not** equivalent to nationwide production availability.

## Source-specific findings

| Area | Evidence | Assessment |
|---|---|---|
| Android stack | `app/build.gradle.kts`; Compose, Room, DataStore, Firebase AI | Native Android project, not the prior React/Express/MySQL ZIP |
| Database | `data/local/AppDatabase.kt`, `data/repository/RentShareRepository.kt`, `data/RentShareContainer.kt` | All marketplace and account data lives on each individual device; users cannot see each other's local data |
| Auth | Original `RentShareRepository.login/register` | Previously compared/stored plaintext passwords; patched only for local demo, **not** replaced by server identity |
| Admin | Original README and `DatabaseSeeder` | Universal local debug admin and demo accounts; moved seeding and one-tap access behind `BuildConfig.DEBUG` |
| Listing claims | Seed data in `AppDatabase.kt` includes approved/verified sample flats | These are illustrative samples, not proof of actual verification or availability |
| Verification | Original `verifyEmailForUser` and `submitVerification` | Previously allowed self-verification by a tap/string; neutralized in revised source, real OTP/document workflow still missing |
| Chat | `Room` MessageEntity, DAOs | Messages are stored locally, not synchronized between separate users' devices |
| Expenses/bills | `FinanceLivingRepository`, local BillPaymentEntity | Records are not merchant payments, payment confirmations or utility provider settlement |
| Auto-pay | Local `toggleBillAutoPay` | Disabled; provider authorization and server ledger absent |
| Wallet | Absent from uploaded source | This revision adds a **visually disabled preview**; no coins issued and no real BDT transfers |
| AI | `data/ai/AiRepository.kt` uses Firebase AI Logic with model `gemini-3.8-flash` | AI code exists and new chat UI was added, but network, quota, App Check and actual model responses were not tested |
| Publishing | `app/build.gradle.kts`; target SDK 36 and source code | No Play Store publication, Android device testing, or signed release output in this review |
| Tests | `tests/` in revision | 15 JVM smoke checks passed; these cannot substitute for end-to-end Android testing |

## Verified modifications in this source revision

- Safer local hashed passwords (PBKDF2) and legacy compatibility migration
- Debug-only demo seeding/demo role switching
- Removed nonverified password reset and self-verified account markers
- Disabled alleged auto-payment, emphasized manual *record keeping* only
- AI assistant conversation screen and wallet preview navigable from the app
- Warm, welcoming UI palette with transparent sample-content notices
- Firebase App Check release provider changed to Play Integrity (console configuration pending)

## Requirements for true production release — all mandatory

- [ ] Backend/API deployed with TLS, logs, monitoring, encrypted backups and tested restore
- [ ] Managed identity such as Firebase Authentication or server-side auth; phone/email verification and account deletion
- [ ] Per-user/role security rules or server authorization on **all** users, listings, messages, media and admin actions
- [ ] Synchronized listings and roommate data; duplicate/stale listing protections; search by real Bangladesh districts
- [ ] Realtime cross-device conversations, sender/participant authorization and abuse reporting
- [ ] Reliable image uploads with content checks, storage quota, URL signing or access controls as needed
- [ ] User-generated content moderation, report-and-block, enforcement history and admin audit trail
- [ ] Privacy notice, terms, support contact, consent rules, retention/deletion processes
- [ ] Verified identity/document processing, or disable and remove it entirely until a provider/process exists
- [ ] Real user-tested onboarding, landlord listing, tenant searching, matches, chatting, rental requests and moderation on two or more **different phones**
- [ ] Test mobile data and low-bandwidth connections across Bangladesh; measure crash rate and API response times
- [ ] Real device Android compatibility matrix, permissions, navigation/accessibility, CI build and regression tests
- [ ] Firebase App Check enforcement, quotas, abuse protection, AI safety and model availability confirmed from Firebase console
- [ ] Release signing key securely stored; signed AAB/APK, store listing, privacy/data disclosures and appropriate review/testing

### Additional obligations for real payments — HARD STOP

- [ ] Confirm with an appropriately regulated provider and local qualified counsel whether the wallet/credit design is permitted; obtain approvals or use a licensed wallet-as-a-service partnership
- [ ] Authorized bKash/Nagad merchant/payment gateway onboarding and verified payout/settlement instructions
- [ ] Payment server performs signed provider verification, idempotent event processing, double-entry immutable ledger, reversal/refund/dispute processing
- [ ] User balance is never computed or credited solely by Android code, a pasted transaction ID, or a message to a personal wallet number
- [ ] Rent payee consent, utility biller integration, clear fees, cash-out/redemption rules, transaction history, customer support and AML/KYC controls as required
- [ ] Webhook failure, retries, duplicates, incorrect amounts, chargebacks, refunds and partial outages have automated tests

## What can happen tomorrow (24 November)

**Safe milestone:** Developer/import Gradle on own computer; build a debug APK; test with a few consenting testers using explicitly fake records and wallet-preview mode; fix any crashes; document findings. Do not ask testers for real personal NID information, payment PINs, or real deposits.

**Do not:** Distribute this APK nationwide claiming that listings/chat sync, that real rent/utility payments are processed, or that the balance is redeemable. Avoid collecting real user deposits on a personal bKash/Nagad number against unissued RentShareCoins.

## Google Play distribution

For personal Play Console accounts created after 13 November 2023, Google currently requires a closed test with at least 12 opted-in testers for 14 continuous days before an application for production access. Review and verification can add time. New Android apps are submitted as signed Android App Bundles (AABs). Directly sharing a signed APK is a distinct distribution method, not Play Store approval, and should not be used to bypass security tests.

References: Google Play testing requirement — https://support.google.com/googleplay/android-developer/answer/14151465 ; Android app signing — https://developer.android.com/studio/publish/app-signing

## Testing completed in this workspace

**Passed:** 10 domain logic tests (cost, splits, settlements, bill due status, roommate score), 5 password hashing tests. See `tests/`.

**Not performed:** full Android Gradle build/compile, Compose runtime tests, instrumented tests, AI cloud requests, Android app signing, end-to-end multi-device interactions, property moderation on cloud, security penetration test, backend load tests, payment/merchant tests, public launch. No Android SDK was present in this environment. The revised project must still be compiled, installed and reviewed for regressions before even a limited beta.

## Recommended implementation order after the private demo

1. Choose **one** authoritative backend (Firebase Auth + Firestore/Storage/Cloud Functions, or existing Node/Express/MySQL if a separate verified project exists). Do not assume the uploaded ZIP contains a web backend.
2. Move account, listings, conversations, chat, rental applications, and moderation to the backend with server-side authorization.
3. Add security/privacy protections and cross-device test accounts; eliminate demo privileges from actual production builds.
4. Verify AI provider and app integrity on real release builds.
5. Add merchant checkout through an authorized integration. Only after review consider any stored-value wallet under a regulated partner.
6. Perform release testing and obtain Play Console production access.
