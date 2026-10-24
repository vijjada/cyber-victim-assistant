plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.cybervictim.assistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cybervictim.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        val geminiKey: String = (project.findProperty("GEMINI_API_KEY") as? String) ?: ""
        manifestPlaceholders["geminiKey"] = geminiKey
        buildConfigField("String", "GEMINI_API_KEY", "\"" + geminiKey + "\"")
        val exposureBase: String = (project.findProperty("EXPOSURE_BASE_URL") as? String) ?: ""
        buildConfigField("String", "EXPOSURE_BASE_URL", "\"$exposureBase\"")
    }


    signingConfigs {
        create("release") {
            storeFile = file("../cva-release.jks")
            storePassword = (project.findProperty("CVA_RELEASE_STORE_PASSWORD") as? String)
                ?: System.getenv("CVA_RELEASE_STORE_PASSWORD") ?: "cva2026"
            keyAlias = "cva"
            keyPassword = (project.findProperty("CVA_RELEASE_KEY_PASSWORD") as? String)
                ?: System.getenv("CVA_RELEASE_KEY_PASSWORD") ?: "cva2026"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.core:core-ktx:1.13.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.security:security-crypto:1.1.0-alpha06") // Keystore-backed credential storage

    implementation("com.google.mlkit:text-recognition:16.0.1") // on-device OCR — images never leave the phone
    implementation("org.json:json:20240303")

    testImplementation("junit:junit:4.13.2")
}
