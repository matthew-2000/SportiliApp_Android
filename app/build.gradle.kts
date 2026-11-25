plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlin.compose)
}

// Optional CI-only settings; Android Studio keeps the existing local defaults.
val ciRelease = providers.environmentVariable("SPORTILI_CI_RELEASE").orNull == "true"
fun requiredReleaseValue(name: String): String =
    providers.environmentVariable(name).orNull?.takeIf { it.isNotBlank() }
        ?: error("Missing CI release setting: $name")

android {
    namespace = "com.matthew.sportiliapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.matthew.sportiliapp"
        minSdk = 27
        targetSdk = 36
        versionCode = if (ciRelease) requiredReleaseValue("SPORTILI_VERSION_CODE").toInt() else 36
        versionName = if (ciRelease) requiredReleaseValue("SPORTILI_VERSION_NAME") else "2.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    if (ciRelease) {
        signingConfigs {
            create("ciUpload") {
                storeFile = file(requiredReleaseValue("SPORTILI_KEYSTORE_PATH"))
                storePassword = requiredReleaseValue("SPORTILI_KEYSTORE_PASSWORD")
                keyAlias = requiredReleaseValue("SPORTILI_KEY_ALIAS")
                keyPassword = requiredReleaseValue("SPORTILI_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            if (ciRelease) signingConfig = signingConfigs.getByName("ciUpload")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom.v20250300))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.protolite.well.known.types)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.gson)
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(platform(libs.firebase.bom))
    implementation(libs.coil.compose)
}
