plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Secrets come from the environment or CI secret store. Never commit keys.
// Unsigned bundles are allowed for verification, never publishing.
val releaseStoreFile = providers.environmentVariable("RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
val signingValues = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
val hasReleaseSigning = signingValues.all { !it.isNullOrBlank() }
require(signingValues.all { it.isNullOrBlank() } || hasReleaseSigning) {
    "Release signing requires all four RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS and RELEASE_KEY_PASSWORD variables."
}
if (providers.gradleProperty("requireReleaseSigning").orNull == "true") {
    require(hasReleaseSigning) { "Publishing requires release signing. See README.md for the four required environment variables." }
}

android {
    namespace = "com.lyheang.goodgradle"
    // Current NDK LTS; used to strip/package native libraries shipped by AndroidX.
    ndkVersion = "30.0.16248370"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.lyheang.goodgradle"
        minSdk = 24
        targetSdk = 37
        // Override from CI for each upload; defaults keep local builds simple.
        versionCode = providers.gradleProperty("appVersionCode").orNull?.toInt() ?: 1
        versionName = providers.gradleProperty("appVersionName").orNull ?: "1.0.0"
        require(versionCode!! > 0) { "appVersionCode must be positive." }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            optimization {
                enable = true
            }
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
        // Installable, optimized build for Profiler / Macrobenchmark, never publishing.
        create("benchmark") {
            initWith(getByName("release"))
            applicationIdSuffix = ".benchmark"
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            isProfileable = true
            matchingFallbacks += "release"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        checkDependencies = true
        warningsAsErrors = true
        // Versions are reviewed via the catalog and Dependabot instead.
        disable += "NewerVersionAvailable"
    }
    testOptions {
        animationsDisabled = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

// Opt in when investigating recomposition; avoid reports on every build.
composeCompiler {
    if (providers.gradleProperty("composeReports").orNull == "true") {
        reportsDestination = layout.buildDirectory.dir("compose-reports")
        metricsDestination = layout.buildDirectory.dir("compose-metrics")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Install packaged AndroidX profiles on supported devices.
    implementation(libs.androidx.profileinstaller)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
