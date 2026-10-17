plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val shortcutDefaults = providers.environmentVariable("MATHAPELO_SHORTCUTS_B64").orElse("").get()
require(shortcutDefaults.matches(Regex("[A-Za-z0-9+/=]*"))) {
    "MATHAPELO_SHORTCUTS_B64 must contain base64 characters only"
}

android {
    namespace = "com.mathapelo.keyboard"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mathapelo.keyboard"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "DEFAULT_DOT_SHORTCUTS_B64", "\"$shortcutDefaults\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core:1.13.1")
    testImplementation("junit:junit:4.13.2")
}
