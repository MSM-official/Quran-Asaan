plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.shoaib.qurani"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.shoaib.qurani"
        minSdk = 24
        targetSdk = 36
        // GitHub sets VERSION_CODE on every build, so each upload to Play is higher.
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = "1.0." + (System.getenv("VERSION_CODE") ?: "0")
    }

    // Upload key for Google Play. The key file and password come from
    // GitHub secrets (KEYSTORE_BASE64, KEYSTORE_PASSWORD), never from the repo.
    val keystoreFile = System.getenv("UPLOAD_KEYSTORE_FILE")
    val keystorePassword = System.getenv("UPLOAD_KEYSTORE_PASSWORD")
    if (keystoreFile != null && keystorePassword != null) {
        signingConfigs {
            create("upload") {
                storeFile = file(keystoreFile)
                storePassword = keystorePassword
                keyAlias = "upload"
                keyPassword = keystorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystoreFile != null && keystorePassword != null) {
                signingConfig = signingConfigs.getByName("upload")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
