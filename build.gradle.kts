// Upgrade AGP's built-in Kotlin compiler and keep Compose at the same version.
// Do not apply org.jetbrains.kotlin.android with AGP 9.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

tasks.register("productionCheck") {
    group = "verification"
    description = "Runs lint, JVM tests and builds debug, device-test and optimized release artifacts."
    dependsOn(
        ":app:lintDebug",
        ":app:lintRelease",
        ":app:testDebugUnitTest",
        ":app:assembleDebug",
        ":app:assembleDebugAndroidTest",
        ":app:bundleRelease",
    )
}
