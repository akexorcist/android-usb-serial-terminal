@file:Suppress("AvoidDuplicateDependencies")

import java.time.Duration
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

val releaseVersionName: String? = System.getenv("RELEASE_VERSION_NAME")
val releaseRunNumber: String? = System.getenv("GITHUB_RUN_NUMBER")

if (releaseVersionName != null) {
    require(Regex("""^\d+\.\d+\.\d+$""").matches(releaseVersionName)) {
        "RELEASE_VERSION_NAME must be in X.Y.Z format, got: $releaseVersionName"
    }
}

android {
    namespace = "dev.akexorcist.terminal.usbspp"
    compileSdk = 37
    defaultConfig {
        applicationId = "dev.akexorcist.terminal.usbspp"
        minSdk = 26
        targetSdk = 37
        versionCode = releaseRunNumber?.toInt() ?: 1
        versionName = releaseVersionName ?: "1.0"
    }

    val keystoreProperties = Properties()
    val keystorePropertiesFile = rootProject.file("local.properties")
    if (keystorePropertiesFile.exists()) {
        keystoreProperties.load(keystorePropertiesFile.inputStream())
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("keystore_path", "release.keystore"))
            storePassword = keystoreProperties.getProperty("keystore_password", "")
            keyAlias = keystoreProperties.getProperty("keystore_key_alias", "")
            keyPassword = keystoreProperties.getProperty("keystore_key_password", "")
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    testOptions {
        unitTests {
            // Without this, SerialInputOutputManager's unstubbed Process.setThreadPriority() call
            // throws and is swallowed, so its startup latch never counts down and start() hangs.
            isReturnDefaultValues = true
            all { test -> test.timeout.set(Duration.ofSeconds(30)) }
        }
    }
    buildFeatures {
        compose = true
        aidl = false
        buildConfig = false
        shaders = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Arch Components
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    // Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Instrumented tests
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Local tests: jUnit, coroutines, Android runner
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)

    // Instrumented tests: jUnit rules and runners
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)

    // Navigation
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    // DI
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)

    // USB serial communication
    implementation(libs.usb.serial.android)

    // Date and time
    implementation(libs.kotlinx.datetime)
}
