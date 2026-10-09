# GoodGradle

A reusable Android/Compose build foundation with production release optimization,
fast incremental builds, secure defaults and repeatable quality checks. This is a
starter project: a passing build is a foundation for production, not proof that a
finished product meets every release requirement.

The app namespace and application ID are `com.lyheang.goodgradle`. Replace them
with your own identifiers when using this template.

Licensed under [Apache License 2.0](LICENSE).

## Toolchain

Versions checked on **2026-10-09** against official release notes and Google Maven
metadata. Stable versions are pinned in `gradle/libs.versions.toml`.

| Component | Version | Reason |
| --- | --- | --- |
| Android Gradle Plugin | 9.4.1 | Latest stable patch verified in Google Maven |
| Gradle wrapper | 9.8.0 | Above AGP 9.4's 9.6.0 minimum; matching official distribution SHA-256 pinned |
| Build JVM / Java & Kotlin bytecode | 17 | AGP's supported baseline; use a patched JDK 17 |
| Kotlin / Compose compiler | 2.4.20 | Same version for both; explicitly upgrades AGP's built-in Kotlin |
| Compile / target SDK | 37 | Android 17; exercise its behavior changes before release |
| Minimum SDK | 24 | Existing project minimum retained |
| NDK | 30.0.16248370 (r30 LTS) | Current stable LTS, also handles native AndroidX libraries |
| Compose BOM | 2026.09.00 | Stable coordinated Compose dependency versions |
| Core / Activity / Lifecycle | 1.19.1 / 1.13.0 / 2.11.0 | Stable AndroidX updates |
| Test JUnit extension / Espresso / runner | 1.3.0 / 3.7.0 / 1.7.0 | Stable device test stack |
| ProfileInstaller | 1.4.1 | Installs packaged library/app profiles on supported devices |

Install an Android Studio release supporting AGP 9.4, a current patched JDK 17,
SDK platform `platforms;android-37.0`, `build-tools;36.0.0`, and NDK
`ndk;30.0.16248370`. Set `JAVA_HOME`
and Android Studio's Gradle JDK to JDK 17. The committed daemon criteria also
select Java 17. `local.properties` remains machine-specific and ignored.
Build Tools are selected by AGP; they do not need to match the target SDK number.

With the new Android CLI, install the NDK using its side-by-side identifier:
`android --no-metrics sdk install ndk/30.0.16248370`. Legacy `sdkmanager` uses
`sdkmanager "ndk;30.0.16248370"`; CI pins command-line tools 22.0 for that syntax.

AGP 9 includes Kotlin support. Do **not** apply `org.jetbrains.kotlin.android` or
turn off `android.builtInKotlin` / `android.newDsl`. The root `buildscript`
dependency upgrades AGP's default Kotlin compiler using Google's documented
mechanism, and the Compose compiler uses that same catalog version.

## Commands

PowerShell examples; on Linux/macOS substitute `./gradlew` for `.\gradlew.bat`.

```powershell
# Lint debug/release, run JVM tests, compile device tests, build APK and release AAB.
.\gradlew.bat productionCheck

# Run twice with the same tasks to verify configuration cache reuse.
.\gradlew.bat productionCheck

# Requires a connected device or running emulator; includes a Compose launch test.
.\gradlew.bat :app:connectedDebugAndroidTest

# Optimized, non-debuggable, profileable APK signed with the debug key.
.\gradlew.bat :app:assembleBenchmark
.\gradlew.bat :app:installBenchmark

# Optional Compose compiler reports, written under app/build/.
.\gradlew.bat :app:assembleRelease -PcomposeReports=true

# Local build timing report; inspect build/reports/profile/.
.\gradlew.bat :app:assembleDebug --profile
```

`productionCheck` compiles device tests but does not run them. The existing JVM
test is only a template smoke test; add tests for real business behavior as the
app grows. CI repeats the verification command to exercise configuration cache
reuse. The GitHub workflow uses pinned action commits, a read-only token, and no
artifact publishing. It becomes active only after this folder is placed in a
GitHub repository. Dependabot proposes weekly dependency/action updates; review
stability, compatibility and behavior before merging. Never auto-merge prereleases.

## Release optimization and signing

Release uses AGP 9.3+'s `optimization { enable = true }`, which enables R8 code
and resource optimization plus Android's default optimized keep rules. There is
no package scope restriction: the app and its dependencies are optimized.
Add only necessary reflection/JNI rules to `app/src/main/keepRules/*.keep`.
Do not use blanket package keeps, suppress all missing classes, or strip all
logging indiscriminately. Test optimized releases for reflection, serialization,
deep links and third-party SDK behavior.

Set all four variables through your local environment or CI secret store:

| Variable | Value |
| --- | --- |
| `RELEASE_STORE_FILE` | Absolute keystore path, or path relative to this root |
| `RELEASE_STORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Upload key alias |
| `RELEASE_KEY_PASSWORD` | Key password |

Partial signing configuration fails with a clear message. With no signing
variables, verification produces an **unsigned** AAB. No release ever silently
uses a debug key. For a bundle intended for upload, require signing explicitly:

```powershell
.\gradlew.bat :app:bundleRelease -PrequireReleaseSigning=true -PappVersionCode=2 -PappVersionName=1.0.1 --no-configuration-cache
```

Use a monotonically increasing version code for every Play upload. Keep upload
keys outside the checkout and configure Play App Signing. Avoid caching or sharing
signed-build configuration caches because Gradle may serialize signing inputs.
Never put credentials in `gradle.properties`, command-line password arguments,
source code, build scans, CI logs or artifacts.

Outputs:

- Debug APK: `app/build/outputs/apk/debug/`
- Unsigned/signed release AAB: `app/build/outputs/bundle/release/`
- R8 mapping and usage reports: `app/build/outputs/mapping/release/`
- Lint reports: `app/build/reports/`
- JVM test results: `app/build/test-results/`

Retain the exact `mapping.txt` privately for each released version and upload it
to your crash reporting service/Play as appropriate. The `.benchmark` package is
debug-signed for local measurement only; never publish it.

## Build performance

Build cache, configuration cache and parallel project execution are enabled.
Configuration-cache compatibility problems fail instead of being hidden. The
3 GiB daemon heap is a starting point: tune it and workers against machine RAM,
Build Analyzer and measured build times. Parallel execution mostly helps after
modules are added. Do not routinely run `clean`; it discards incremental outputs.

AGP 9 already enables AndroidX, modern R-class behavior and optimized resource
shrinking. Avoid copying obsolete flags into `gradle.properties`. Do not cap
workers globally, enable configuration-on-demand, filter supported languages/ABIs,
or add experimental optimizations without measuring a real bottleneck.

Use the version catalog, centralized repositories and fixed versions. Prefer KSP
over kapt when an added library needs code generation. Introduce included-build
convention plugins when several modules repeat configuration; a single-module
template does not need custom build infrastructure yet.

For supply-chain verification, generate SHA-256 metadata only on a trusted
network, inspect the result against authoritative checksums, and commit it:

```powershell
.\gradlew.bat productionCheck :app:assembleBenchmark --write-verification-metadata sha256
```

Generating metadata trusts the currently resolved bytes; it is not independent
proof of authenticity. Review changes whenever dependencies change. Dependency
locking is optional for larger graphs; pinning direct versions alone does not
freeze every transitive dependency. Keep the wrapper checksum when updating Gradle.

When upgrading Gradle, update `distributionUrl` and `distributionSha256Sum`
together. Use the checksum for the exact version and archive type (`-bin` versus
`-all`) from [Gradle's official checksums](https://gradle.org/release-checksums/).
A checksum from an earlier version will reject a valid new distribution.

## Runtime performance and optional add-ons

Measure startup, frame timing, allocations and battery usage on representative
physical devices with the optimized benchmark variant. Gradle settings cannot
prove runtime improvements. Library Baseline Profiles are packaged by AGP and
ProfileInstaller is included. Once real user journeys exist, add the Android
Studio Baseline Profile generator module and generate both baseline and startup
profiles for cold launch and important flows. Compare Macrobenchmark results
with/without profile compilation; do not hand-write fake profile rules.

Choose libraries when the product needs them:

| Need | Integration approach |
| --- | --- |
| Stateful screens | ViewModel, immutable UI state, lifecycle-aware state collection |
| Multiple screens | Stable AndroidX navigation; adaptive layouts for large screens |
| Structured local storage | Room with KSP, committed schemas and migration tests |
| Small preferences | DataStore; keep credentials out of unencrypted preferences |
| Deferrable background work | WorkManager with appropriate constraints and retry behavior |
| HTTP APIs | One shared HTTP client, timeouts, cancellation, TLS, typed errors; debug-only inspection |
| Dependency injection | Constructor injection first; add Hilt/KSP if graph complexity justifies it |
| Images and large collections | Image loader caching/downsampling; Paging when required |
| Modern Java library APIs | Core-library desugaring only for APIs that need it on minSdk 24 |
| Crash/ANR monitoring | Approved service, consent/data review, exact release mapping files |

The template has no backend, secrets or external monitoring accounts, so those
integrations are not activated by default. Keep network/disk work off the main
thread, use structured concurrency, avoid eagerly initializing unused SDKs and
profile Compose recomposition before tuning it.

## Security and release checklist

Cleartext traffic is disabled. Backup is opt-in: all credential-encrypted and
device-encrypted data domains are excluded from cloud backup/device transfer by
default. If the product needs backup, design an allowlist of safe restorable data
and update both XML policies plus the manifest. Never restore tokens as if they
were a valid authenticated session. No unnecessary permissions are requested.

Before distributing a real app:

- Replace the template application ID, name, icons and sample screen.
- Run lint/JVM/device tests; smoke-test the signed, optimized release on minSdk
  and the latest Android version, including offline/error paths and process death.
- Exercise target SDK 37 behavior changes, edge-to-edge insets, predictive back,
  accessibility, large fonts, RTL, dark mode and large-screen layouts.
- Generate profiles for real user journeys and measure startup/jank on physical devices.
- Confirm Play signing, versioning, backup policy, permissions, privacy policy,
  data safety disclosures and monitoring match the product.
- Use AAB delivery, review installed/download size, and use staged rollout with
  crash/ANR monitoring. Native SDKs require separate 16 KiB page-size checks.
- Retain mappings and symbols per release. Validate dependency licenses.

## Windows socket troubleshooting

If Gradle fails before configuration with `Unable to establish loopback connection`
and `UnixDomainSockets ... Invalid argument: connect`, test a project-local
directory for Java socket files. This machine's normal TEMP directory triggers
that failure independently of the project. A session-only workaround is:

```powershell
New-Item -ItemType Directory -Force build/java-sockets | Out-Null
$taskSocketDirectory = Join-Path (Get-Location) 'build/java-sockets'
$taskPreviousJavaOptions = $env:JAVA_TOOL_OPTIONS
try {
    $env:JAVA_TOOL_OPTIONS = ($taskPreviousJavaOptions + ' -Djdk.net.unixdomain.tmpdir="' + $taskSocketDirectory + '"').Trim()
    .\gradlew.bat productionCheck
} finally {
    $env:JAVA_TOOL_OPTIONS = $taskPreviousJavaOptions
}
```

Use this only for affected local sessions; preserve any pre-existing
`JAVA_TOOL_OPTIONS` when applying it. Do not put a machine-specific socket path
into committed Gradle settings. Remove the session variable after validation if
you added it solely for this workaround.

## Official references

- [AGP 9.4 compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes)
- [NDK LTS downloads](https://developer.android.com/ndk/downloads)
- [AGP built-in Kotlin and compiler upgrades](https://developer.android.com/build/releases/agp-9-0-0-release-notes#kotlin)
- [Kotlin stable releases](https://kotlinlang.org/docs/releases.html)
- [AndroidX stable versions](https://developer.android.com/jetpack/androidx/versions)
- [Compose BOM](https://developer.android.com/develop/ui/compose/bom)
- [Google Maven metadata](https://dl.google.com/dl/android/maven2/master-index.xml)
- [Build performance](https://developer.android.com/build/optimize-your-build)
- [R8 optimization](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization)
- [Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/overview)
- [App signing](https://developer.android.com/studio/publish/app-signing)
- [Backup policies](https://developer.android.com/identity/data/autobackup)
- [Dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html)
