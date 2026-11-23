# RentShare BD — Android Source Revision (SAFE PREVIEW)

**Find your place. Feel at home.**

This project is a **native Kotlin + Jetpack Compose Android application** based on the user-supplied `rentsharebd-2.zip`. It has been revised to improve its home-like palette, add a conversational AI assistant screen, add a wallet **preview**, and reduce several risks in the original on-device demonstration code.

> **NOT A PRODUCTION RELEASE. NOT A LIVE PAYMENT SYSTEM.** This application currently uses an on-device Room/SQLite database and cannot sync users, listings, chat, or payments across Bangladesh. Do not distribute to the general public as a fully functional rental/payment platform. See `RELEASE_READINESS.md` before any distribution.

## Included screens and local feature flows

- House discovery, listing filters and detail pages, renter/landlord workspaces
- Roommate matching and roommate requests
- Roommate-group and direct chat **on the same device only**
- Expense splitting, expense charts, utility bill *records*, meal planning, grocery lists, maintenance tickets, and PDF/CSV exports
- Demo admin moderation **only in debug builds**
- Firebase AI Logic rental-query parser and financial coach (requires live Firebase configuration)
- **New RentShare AI Chat:** an interactive assistant for renting, roommates, home living and budgeting (requires Firebase AI and App Check)
- **New Wallet Preview:** proposed 1 coin = ৳1 pricing, but recharge, transfer and payment features are explicitly disabled. No funds are collected or transferred.

## Changes from uploaded version

- Warmer forest-green, sage, terracotta and ivory theme and a welcoming home message
- Debug-only demo admin accounts and sample listing seeding; production build no longer automatically seeds them
- Local prototype password records use salted PBKDF2 instead of directly storing new plaintext passwords; legacy demo records migrate on successful login
- Removed unauthenticated password reset (no email verification service was present)
- Disabled fake email/phone verification shortcuts and guarded ID-reference demo UI
- Disabled falsely implied utility auto-pay; manual payment records are marked unverified
- Updated Android Firebase App Check release provider to Play Integrity; debug provider remains debug-only
- Disabled Android automatic backup, cleartext network access; removed missing debug keystore reference

## Build on your own computer

1. Install **Android Studio** and Android SDK 36, with a JDK compatible with your Gradle/AGP version.
2. Open the project folder containing `settings.gradle.kts` and sync Gradle. The uploaded archive does **not** contain a Gradle wrapper, and this revision does not fabricate one.
3. Confirm `app/google-services.json` belongs to the Firebase project under your control; enable Firebase AI Logic and configure App Check. Configure the Android package ID and SHA certificates accordingly.
4. Build and run the **debug variant** on real Android phones for a *local demonstration only*.
5. Validate navigation, images, permissions, Room migration, AI calls, crashes and accessibility. Test mobile data/Wi-Fi with real devices.
6. Do **not** publish a production Android package until the remaining blockers in `RELEASE_READINESS.md` are resolved and a release signing key is configured.

## Tests performed here

- 10 deterministic Kotlin domain smoke tests: **passed**
- 5 local password-hashing checks: **passed** (using a JVM stub for `android.util.Base64`)
- Full Android compilation, emulator testing, Firebase production calls, multi-device sync and financial integration: **not performed**. No Android SDK or Android Gradle build environment was available in this workspace.

## Folder guide

- `app/src/main/java/com/example/ui/` — Compose UI and screens
- `app/src/main/java/com/example/data/local/` — Room entities, DAOs, local auth helpers
- `app/src/main/java/com/example/data/ai/` — Firebase AI Logic calls and App Check
- `app/src/main/java/com/example/domain/` — compatibility, utility bill and expense calculations
- `tests/` — JVM smoke tests and test-only stubs
- `RELEASE_READINESS.md` — critical audit and staged launch conditions
- `WALLET_ARCHITECTURE.md` — payment design and compliance gates

**No real bKash/Nagad personal-account number or payment credentials have been embedded in the app.**
