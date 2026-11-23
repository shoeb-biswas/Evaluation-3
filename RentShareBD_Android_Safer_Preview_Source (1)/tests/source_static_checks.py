"""Simple release guard assertions. NOT a replacement for Android build/runtime security tests."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
def src(rel):return (root/'app/src/main/java/com/example'/rel).read_text()
checks={
    'demo seeding debug-only': 'if (BuildConfig.DEBUG) DatabaseSeeder.seedIfEmpty(db)' in src('data/repository/RentShareRepository.kt'),
    'release blocks legacy demo/admin logins': 'Local demo/admin accounts cannot sign in to a release build.' in src('data/repository/RentShareRepository.kt'),
    'one-tap admin debug-only': 'if (!BuildConfig.DEBUG) return Result.failure(SecurityException' in src('data/repository/RentShareRepository.kt'),
    'new password salted hash': 'passwordHash = LocalPasswordHasher.hash(password)' in src('data/repository/RentShareRepository.kt'),
    'password recovery disabled': 'Password recovery is disabled until verified email authentication' in src('data/repository/RentShareRepository.kt'),
    'phone not self-verified': 'phoneVerified = false' in src('data/repository/RentShareRepository.kt'),
    'autopay disabled': 'autoPayEnabled = false' in src('data/repository/FinanceLivingRepository.kt'),
    'wallet preview explicitly not live': 'No coins have been issued.' in src('ui/screens/public/AssistantAndWalletScreens.kt'),
    'AI chat route present': 'composable<AssistantRoute>' in src('ui/RentShareApp.kt'),
    'cleartext blocked': 'android:usesCleartextTraffic="false"' in (root/'app/src/main/AndroidManifest.xml').read_text(),
    'backup blocked': 'android:allowBackup="false"' in (root/'app/src/main/AndroidManifest.xml').read_text(),
}
for name, success in checks.items():print(('PASS' if success else 'FAIL')+': '+name)
assert all(checks.values())
print(f'{len(checks)} static checks passed')
