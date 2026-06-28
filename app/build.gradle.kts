plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.dvoraak.greencompressor"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        // Distinct applicationId is THE thing that makes Android (and Obtainium)
        // treat Green Compressor as a separate app from upstream Compressor.
        // Without this, installing v1.0.0 on a phone that already has 1.5.7
        // would either be rejected (signature mismatch) or silently replace it.
        applicationId = "io.github.dvoraak.greencompressor"
        minSdk = 24
        targetSdk = 36
        // Reset to 1 / 1.0.0 — this is the first release of Green Compressor
        // as its own app, not a continuation of upstream's version numbers.
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // STABLE signing key checked into the repo. The previous setup used the
    // AGP-generated debug keystore (~/.android/debug.keystore on whatever
    // machine ran the build), which differs per GitHub Actions runner — so
    // v1.0.0 and v1.0.1 had different signatures and Android refused to
    // upgrade between them ("conflict: green compressor"). With a single
    // committed keystore, every CI build (and every local build on any
    // machine) signs with the same key → upgrades work indefinitely.
    //
    // Security note: the keystore password is in this file on purpose.
    // This is the standard pattern for personal-archive forks where the
    // user is the only consumer of releases; the only thing a keystore
    // leak buys an attacker is the ability to publish a fake APK *that
    // would still need the user to install it from somewhere other than
    // the official GitHub releases page they configured in Obtainium*.
    signingConfigs {
        create("greencompressor") {
            storeFile = file("release.keystore")
            storePassword = "greenpass"
            keyAlias = "greencompressor"
            keyPassword = "greenpass"
        }
    }

    buildTypes {
        getByName("debug") {
            // Use the same key for debug too, so adb-pushed debug builds and
            // Obtainium-pulled release builds are mutually upgradeable —
            // otherwise switching between them on the phone forces an uninstall.
            signingConfig = signingConfigs.getByName("greencompressor")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("greencompressor")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    dependenciesInfo {
        includeInBundle = false
        includeInApk = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}