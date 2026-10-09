# Validation — 2026-10-09

## Package rename validation with Gradle 9.8.0

Renamed the namespace, application ID, Kotlin sources and tests to
`com.lyheang.goodgradle`, with matching source directory paths.
`assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` passed.
The generated APK metadata confirms `com.lyheang.goodgradle` for the app and
`com.lyheang.goodgradle.test` for instrumentation. The JVM test report records
one test with zero failures or errors. Device tests were compiled, not executed.

## Wrapper update to Gradle 9.8.0

The wrapper URL was changed to 9.8.0 while the SHA-256 remained at 9.6.0.
Updated `distributionSha256Sum` to the official 9.8.0 binary ZIP checksum:
`bafd5ce9cfaea0fbccfdc8439a1ac42fbd4cd9c89dc9a988228d8a2639a58e6c`.
The distribution downloaded, verified and started successfully. Debug APK and
device-test APK compilation and debug/release lint completed during validation.
The full production check was stopped after a validation tool interruption;
optimized release/benchmark completion under 9.8.0 remains unverified.

## Original baseline validation with Gradle 9.6.0

Validated on Windows with JDK 17, AGP 9.4.1, Gradle 9.6.0, SDK 37 and NDK
30.0.16248370. A session-only Java socket directory override was required for
this machine; see the README troubleshooting section.

| Check | Result |
| --- | --- |
| `productionCheck :app:assembleBenchmark` | Passed |
| Repeat of the same command | Passed; configuration cache reused |
| Debug and release lint | Zero issues in both XML reports |
| JVM unit test | 1 test, zero failures/errors; template smoke test only |
| Debug and Compose device-test APK compilation | Passed |
| Full R8 optimized release bundle and benchmark APK | Passed |
| Packaged Baseline Profile | `baseline.prof` and `baseline.profm` present in AAB |
| Missing signing with `requireReleaseSigning=true` | Rejected with expected error |
| Partial signing environment | Rejected with expected error |
| Release bundle signature | Unsigned as intended for verification without keys |
| Gradle distribution SHA-256 | Matches official published checksum |
| Native stripping with NDK r30 | Passed without strip warnings |
| Optimized APK ZIP alignment | `zipalign -c -P 16 4` passed |
| Bundled arm64-v8a/x86_64 ELF load segments | All have `0x4000` (16 KiB) alignment |
| CI pipeline error propagation | Explicit Bash pipefail preserved simulated exit 42 |

Device tests were compiled but not executed: `adb devices` showed no device or
emulator. Signed release validation requires the product's upload key. CI has
been reviewed locally but has not run on GitHub because this folder is not yet
a Git repository. Runtime startup/jank/battery performance has not been measured.
The included profiles originate from libraries; generate app-specific profiles
after real user journeys are implemented.

Local verification logs and reports are ignored under `build/` and `app/build/`.
Re-run checks after any version, code, signing or configuration change.
