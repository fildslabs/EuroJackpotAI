import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
    id("com.google.devtools.ksp")
}

// Reads LOTTO_API_KEY from local.properties (git-ignored) so the real key never gets
// committed to source control. Falls back to an empty string, which makes the remote
// data source no-op and the app run entirely on local/sample data.
//
// This used to be MAGAYO_API_KEY — magayo's free tier capped out at 10 requests/month,
// which was impractical, so the remote data source was switched to Poland's official
// LOTTO OpenAPI (developers.lotto.pl). If local.properties still has a MAGAYO_API_KEY
// line from before, it's simply unused now; add LOTTO_API_KEY alongside or in its place.
val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(FileInputStream(f))
}
val lottoApiKey: String = localProperties.getProperty("LOTTO_API_KEY", "")

android {
    namespace = "com.eurojackpotai.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.eurojackpotai.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0-phase4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "LOTTO_API_KEY", "\"$lottoApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Kotlin Gradle plugin 2.x removed the old `android { kotlinOptions { jvmTarget = "17" } }`
// String-based API (it's now a hard error, not just a warning) in favor of this
// top-level `kotlin { compilerOptions { ... } }` DSL, which takes a typed JvmTarget enum.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Compose BOM keeps all Compose artifact versions in sync. Pinned to 2025.12.01
    // (compose-ui 1.11.4) rather than a newer BOM on purpose: BOMs from 2026 onward
    // pull in compose-ui 1.12.x, whose AARs declare a minimum compileSdk of 37 and
    // Android Gradle Plugin 9.1.0 — both newer than what this project uses (compileSdk
    // 36, AGP 8.13.0, the max AGP 8.13.0 actually supports). Bump this once the project
    // itself moves to AGP 9.1+/compileSdk 37.
    val composeBom = platform("androidx.compose:compose-bom:2025.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-process:2.9.0")
    implementation("androidx.activity:activity-compose:1.10.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.navigation:navigation-compose:2.9.0")

    // Room (local database)
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Networking (LOTTO OpenAPI — developers.lotto.pl) — kept minimal and swappable
    // behind domain/repository interfaces so the remote source can be replaced again
    // if it ever needs to be (as it already was once, from magayo).
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Ticket scanning ("Scan Ticket" in My Numbers) — Google ML Kit's on-device Latin
    // text recognizer. Runs fully on the phone (the ticket photo never leaves it, no
    // API key, no per-scan cost); the recognition model is fetched once via Google
    // Play services the first time it's used and cached after that. See
    // data/ocr/TicketTextRecognizer.kt and domain/usecase/ParseTicketTextUseCase.kt.
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    // Instrumented tests
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.room:room-testing:2.7.0")
}
