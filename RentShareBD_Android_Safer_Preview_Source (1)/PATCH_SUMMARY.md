# Source Revision Summary

This archive preserves the user's native Android Kotlin/Compose RentShare BD prototype and introduces **limited, auditable improvements**. It is not a production-ready, connected marketplace.

## Added

- `ui/screens/public/AssistantAndWalletScreens.kt`: conversational AI assistant UI, and a wallet preview that explicitly disables money transfers.
- `data/ai/AiRepository.kt` and `ui/viewmodel/MainViewModel.kt`: general AI assistant streaming and message state.
- `data/local/LocalPasswordHasher.kt`: salted PBKDF2 for the **on-device demo only**.
- `tests/DomainLogicSmokeTest.kt`, `tests/PasswordHashSmokeTest.kt`, and test-only stubs.
- `RELEASE_READINESS.md`, `WALLET_ARCHITECTURE.md`, `TEST_STATUS.md`.

## Modified

- `ui/RentShareApp.kt`, `ui/navigation/Routes.kt`, `ExploreHomeScreen.kt`, `TenantDashboardScreen.kt`: wiring AI/chat/wallet routes.
- `ui/theme/Color.kt` and string resources: warm, home-like color palette and welcoming copy.
- `data/repository/RentShareRepository.kt`: demo passwords no longer stored plaintext for new users; local demo seeding and quick admin role locked to debug; no self-verification or unauthenticated password reset; new properties pending moderation.
- `data/local/AppDatabase.kt`: hash seeded demo passwords.
- `TenantCoreScreens.kt`, `AuthScreens.kt`, `TenantLivingAndAiScreens.kt`: hide unsafe demo actions in release and indicate preview-only use.
- `FinanceLivingRepository.kt`, `TenantFinanceScreens.kt`: disable auto-pay and clarify manually recorded bills are not payments.
- `app/build.gradle.kts`, `AppCheckHelper.kt`, version catalog: update Firebase App Check release provider to Play Integrity, remove nonexistent debug signing file reference.
- `AndroidManifest.xml`: turn off backup and cleartext network traffic.

## Verification note

This workspace did **not** have the Android SDK, so full Gradle build, app launch, UI instrumentation, device testing, and Firebase network testing were unavailable. The source-level revision must be compiled in Android Studio and reviewed before use. Fifteen pure Kotlin/JVM checks passed; see `TEST_STATUS.md`.

## Important

No bKash/Nagad funds are collected. The wallet screen is a preview only; it deliberately has no enabled top-up or pay buttons. The project still has no shared user/listing/chat backend. Do not treat it as a nationwide public release.
